package dev.m4s4t0.voidslime.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.random.Random

/** Something worth celebrating on screen. */
sealed interface GameEvent {
    data class StickerEarned(val sticker: Sticker) : GameEvent
    data class LevelUp(val level: Int) : GameEvent
    data class Coins(val amount: Int) : GameEvent
    data class QuestDone(val quest: Quest, val allDone: Boolean) : GameEvent
}

/**
 * Everything the player has: coins, XP, stats, unlocked supplies, the jar shelf and the shop's orders. Compose state,
 * so the UI follows along; saved to SharedPreferences (stats in batches, purchases and jars straight away).
 */
object Game {
    private lateinit var prefs: SharedPreferences
    private val rnd = Random(System.nanoTime())

    var coins by mutableIntStateOf(0)
        private set
    var xp by mutableLongStateOf(0L)
        private set
    val stats = mutableStateMapOf<String, Long>()
    private val unlocked = mutableStateListOf<String>()
    val jars = mutableStateListOf<Recipe>()
    var currentId by mutableStateOf<String?>(null)
    val earned = mutableStateListOf<String>()
    val orders = mutableStateListOf<Order>()
    val quests = mutableStateListOf<Quest>()
    var tray by mutableStateOf(Tray.VOID)
    var world by mutableStateOf(World.VOID)
    var plate by mutableStateOf(Plate.ROUND)
    var giftReady by mutableStateOf(false)
        private set

    var sounds by mutableStateOf(true)
    var haptics by mutableStateOf(true)
    var volume by mutableStateOf(0.8f)
    var music by mutableStateOf(true)
    var musicVolume by mutableStateOf(0.6f)

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    /** Pokes, stretches and the like since the last coin: every [TRICKLE] earns one, so playing always pays a little. */
    private var sinceCoin = 0
    private var dirty = false

    val level get() = Levels.of(xp).first
    val current: Recipe get() = jars.firstOrNull { it.id == currentId } ?: jars.firstOrNull() ?: STARTER_JARS.first()

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("game", Context.MODE_PRIVATE)
        coins = prefs.getInt("coins", 60)
        xp = prefs.getLong("xp", 0)
        sounds = prefs.getBoolean("sounds", true)
        haptics = prefs.getBoolean("haptics", true)
        volume = prefs.getFloat("volume", 0.8f)
        music = prefs.getBoolean("music", true)
        musicVolume = prefs.getFloat("musicVolume", 0.6f)
        tray = Tray.of(prefs.getString("tray", null))
        world = World.of(prefs.getString("world", null))
        plate = Plate.of(prefs.getString("plate", null))
        runCatching { JSONObject(prefs.getString("stats", "{}")!!) }.getOrNull()?.let { j -> j.keys().forEach { stats[it] = j.getLong(it) } }
        unlocked.addAll(prefs.getStringSet("unlocked", emptySet())!!)
        earned.addAll(prefs.getStringSet("earned", emptySet())!!)
        val saved = runCatching { JSONArray(prefs.getString("jars", null) ?: "") }.getOrNull()
        if (saved == null) jars.addAll(STARTER_JARS) else for (i in 0 until saved.length()) runCatching { jars += Recipe.fromJson(saved.getJSONObject(i)) }
        currentId = prefs.getString("current", null)
        val os = runCatching { JSONArray(prefs.getString("orders", "[]")) }.getOrNull()
        if (os != null) for (i in 0 until os.length()) runCatching { orders += Order.fromJson(os.getJSONObject(i)) }
        val qs = runCatching { JSONArray(prefs.getString("quests", "[]")) }.getOrNull()
        if (qs != null) for (i in 0 until qs.length()) runCatching { quests += Quest.fromJson(qs.getJSONObject(i)) }
        topUpOrders()
        visit()
        saveAll()
    }

    // ---- visiting, the daily gift ----

    private fun visit() {
        val today = LocalDate.now()
        val last = prefs.getString("lastVisit", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        when {
            last == null -> stats[Stat.STREAK] = 1
            last == today -> Unit
            last.plusDays(1) == today -> stats[Stat.STREAK] = (stats[Stat.STREAK] ?: 0) + 1
            else -> stats[Stat.STREAK] = 1
        }
        prefs.edit().putString("lastVisit", today.toString()).apply()
        giftReady = prefs.getString("giftDay", null) != today.toString()
        if (prefs.getString("questDay", null) != today.toString() || quests.isEmpty()) {
            quests.clear()
            quests.addAll(Quests.daily(rnd, ::isUnlocked, level, stats))
            prefs.edit().putString("questDay", today.toString()).apply()
        }
        checkStickers()
    }

    /** What's in today's gift. */
    data class Gift(val coins: Int, val unlockedKey: String?, val jar: Recipe)

    fun openGift(): Gift? {
        if (!giftReady) return null
        giftReady = false
        prefs.edit().putString("giftDay", LocalDate.now().toString()).apply()
        val streak = (stats[Stat.STREAK] ?: 1).toInt()
        val amount = 20 + rnd.nextInt(25) + 5 * streak.coerceAtMost(10)
        // A cheap locked supply, free, now and then.
        val locked = allKeys().filter { !isUnlocked(it) }.sortedBy { priceOf(it) }.take(6)
        val freebie = if (locked.isNotEmpty() && rnd.nextFloat() < 0.6f) locked[rnd.nextInt(locked.size)] else null
        freebie?.let { unlocked += it }
        val jar = mysteryRecipe("Gift of ${LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}")
        jars.add(0, jar)
        earn(amount, 15)
        saveAll()
        return Gift(amount, freebie, jar)
    }

    /** A random slime from the unlocked supplies (the daily gift's jar, the lab's "Surprise me"). */
    fun mysteryRecipe(name: String? = null): Recipe {
        val bases = Base.entries.filter { isUnlocked(key(it)) }
        val colors = COLORS.filter { isUnlocked(key(it)) }.shuffled(rnd)
        val addIns = AddIn.entries.filter { isUnlocked(key(it)) }.shuffled(rnd)
        val glitters = Glitter.entries.filter { isUnlocked(key(it)) }
        val molds = Mold.entries.filter { isUnlocked(key(it)) }
        val r = Recipe(
            id = Recipe.newId(rnd),
            name = "",
            base = bases[rnd.nextInt(bases.size)],
            colors = colors.take(1 + rnd.nextInt(3)).map { it.id },
            addIns = addIns.take(rnd.nextInt(3)).associateWith { rnd.nextInt(ADD_IN_LEVELS.size) },
            glitter = if (rnd.nextBoolean()) glitters[rnd.nextInt(glitters.size)] else Glitter.NONE,
            created = System.currentTimeMillis(),
            mold = if (rnd.nextFloat() < 0.4f) molds[rnd.nextInt(molds.size)] else Mold.DOME,
        )
        return r.copy(face = r.mold.animal || rnd.nextFloat() < 0.25f).copy(name = name ?: Names.suggest(r, rnd))
    }

    // ---- coins, XP, stats, stickers ----

    fun earn(amount: Int, xpGain: Int = 0) {
        if (amount != 0) {
            coins += amount
            if (amount > 0) _events.tryEmit(GameEvent.Coins(amount))
        }
        if (xpGain > 0) {
            val before = level
            xp += xpGain
            if (level > before) {
                _events.tryEmit(GameEvent.LevelUp(level))
                coins += 25
            }
        }
        dirty = true
    }

    /** Counts a stat. Play actions also trickle coins and XP. */
    fun add(stat: String, n: Long = 1) {
        stats[stat] = (stats[stat] ?: 0) + n
        // Golden bubbles outside Bubble rush are worth a coin each.
        if (stat == Stat.GOLD_POPS) earn(n.toInt())
        if (stat in PLAY_STATS) {
            sinceCoin += n.toInt()
            if (sinceCoin >= TRICKLE) {
                sinceCoin = 0
                earn(1, 3)
            }
        }
        dirty = true
        checkQuests()
        checkStickers()
    }

    private fun checkQuests() {
        for (i in quests.indices) {
            val q = quests[i]
            if (q.done || Quests.progress(q, stats) < q.goal) continue
            quests[i] = q.copy(done = true)
            stats[Stat.QUESTS] = (stats[Stat.QUESTS] ?: 0) + 1
            earn(q.reward, 15)
            val all = quests.all { it.done }
            if (all) {
                stats[Stat.QUEST_DAYS] = (stats[Stat.QUEST_DAYS] ?: 0) + 1
                earn(Quests.ALL_BONUS, 30)
            }
            _events.tryEmit(GameEvent.QuestDone(q, all))
            dirty = true
        }
    }

    /** A best score: only kept if higher. */
    fun best(stat: String, value: Long) {
        if (value > (stats[stat] ?: 0)) {
            stats[stat] = value
            dirty = true
            checkStickers()
        }
    }

    private fun checkStickers() {
        for (s in STICKERS) {
            if (s.id in earned) continue
            if ((stats[s.stat] ?: 0) >= s.goal) {
                earned += s.id
                earn(s.reward, 20)
                _events.tryEmit(GameEvent.StickerEarned(s))
                dirty = true
            }
        }
    }

    // ---- supplies ----

    fun isUnlocked(k: String) = priceOf(k) == 0 || k in unlocked

    fun buy(k: String): Boolean {
        val price = priceOf(k)
        if (isUnlocked(k) || coins < price) return false
        coins -= price
        unlocked += k
        add(Stat.BOUGHT)
        saveAll()
        return true
    }

    fun allKeys(): List<String> =
        Base.entries.map(::key) + COLORS.map(::key) + AddIn.entries.map(::key) + Glitter.entries.map(::key) + Tray.entries.map(::key) + Toy.entries.map(::key) +
            World.entries.map(::key) + Plate.entries.map(::key) + Mold.entries.map(::key)

    fun chooseTray(t: Tray) {
        if (!isUnlocked(key(t))) return
        tray = t
        prefs.edit().putString("tray", t.id).apply()
    }

    fun chooseWorld(w: World) {
        if (!isUnlocked(key(w))) return
        world = w
        prefs.edit().putString("world", w.id).apply()
    }

    fun choosePlate(p: Plate) {
        if (!isUnlocked(key(p))) return
        plate = p
        prefs.edit().putString("plate", p.id).apply()
    }

    // ---- jars ----

    fun saveJar(r: Recipe) {
        val i = jars.indexOfFirst { it.id == r.id }
        if (i >= 0) jars[i] = r else jars.add(0, r)
        saveAll()
    }

    fun deleteJar(id: String) {
        jars.removeAll { it.id == id }
        if (jars.isEmpty()) jars.addAll(STARTER_JARS)
        if (currentId == id) currentId = jars.first().id
        saveAll()
    }

    fun select(id: String) {
        currentId = id
        val i = jars.indexOfFirst { it.id == id }
        if (i >= 0) jars[i] = jars[i].copy(plays = jars[i].plays + 1)
        saveAll()
    }

    // ---- shop orders ----

    private fun topUpOrders() {
        while (orders.size < 3) orders += Orders.generate(rnd, ::isUnlocked, level)
    }

    /** Hands [r] to the customer: pays by stars, replaces the order with a new one. Returns the stars. */
    fun fulfil(orderId: String, r: Recipe): Pair<Int, Int> {
        val o = orders.firstOrNull { it.id == orderId } ?: return 0 to 0
        val stars = Orders.stars(o, r)
        val pay = Orders.payout(o, stars)
        orders.remove(o)
        topUpOrders()
        add(Stat.ORDERS)
        if (stars == 3) add(Stat.PERFECT)
        earn(pay, 10 * stars)
        saveAll()
        return stars to pay
    }

    fun skipOrder(orderId: String) {
        orders.removeAll { it.id == orderId }
        topUpOrders()
        saveAll()
    }

    // ---- saving ----

    /** Called now and then and when the app goes to the background. */
    fun flush() {
        if (dirty) saveAll()
    }

    fun saveSettings() {
        prefs.edit().putBoolean("sounds", sounds).putBoolean("haptics", haptics).putFloat("volume", volume)
            .putBoolean("music", music).putFloat("musicVolume", musicVolume).apply()
    }

    fun saveAll() {
        dirty = false
        prefs.edit()
            .putInt("coins", coins)
            .putLong("xp", xp)
            .putString("stats", JSONObject(stats.toMap()).toString())
            .putStringSet("unlocked", unlocked.toSet())
            .putStringSet("earned", earned.toSet())
            .putString("jars", JSONArray(jars.map { it.toJson() }).toString())
            .putString("current", currentId)
            .putString("orders", JSONArray(orders.map { it.toJson() }).toString())
            .putString("quests", JSONArray(quests.map { it.toJson() }).toString())
            .apply()
    }

    /** Start over: everything back to a fresh shelf. */
    fun reset() {
        prefs.edit().clear().apply()
        stats.clear(); unlocked.clear(); earned.clear(); jars.clear(); orders.clear(); quests.clear()
        coins = 60; xp = 0; currentId = null; tray = Tray.VOID; world = World.VOID; plate = Plate.ROUND
        jars.addAll(STARTER_JARS)
        topUpOrders()
        visit()
        saveAll()
        saveSettings()
    }

    private const val TRICKLE = 30
    private val PLAY_STATS = setOf(
        Stat.POKES, Stat.STRETCHES, Stat.SQUISHES, Stat.ROLLS, Stat.POPS, Stat.CRUNCHES, Stat.SNAPS,
        Stat.SLAPS, Stat.RAKES, Stat.STAMPS, Stat.SHAKES, Stat.MAGNET, Stat.WANDS,
    )
}
