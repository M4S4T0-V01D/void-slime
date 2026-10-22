package dev.m4s4t0.voidslime.model

import org.json.JSONObject
import kotlin.random.Random

/**
 * The sorts of customer: regulars; rush orders that leave if you take too long but pay more; VIPs with long wish
 * lists who pay double and bring a prize for a perfect slime; picky ones who only pay well for exactly right.
 */
enum class OrderKind(val title: String, val pay: Float, val level: Int) {
    REGULAR("Order", 1f, 1),
    RUSH("Rush order", 1.6f, 2),
    PICKY("Picky", 1.8f, 3),
    VIP("VIP", 2f, 4),
    ;

    val id get() = name.lowercase()

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: REGULAR
    }
}

/**
 * A customer at the Slime Shop asking for a slime. Each wish is optional; the more wishes, the bigger the reward.
 * Orders only ever ask for things the player has unlocked.
 */
data class Order(
    val id: String,
    val customer: String,
    /** The customer's portrait (art "cust_<face>"). */
    val face: String,
    val base: Base?,
    val color: String?,
    val addIn: AddIn?,
    val glitter: Boolean,
    val reward: Int,
    val line: String,
    /** A second colour somewhere in the mix. */
    val color2: String? = null,
    val mold: Mold? = null,
    /** Wants the slime to have a cute face. */
    val smiley: Boolean = false,
    /** Wants loads of [addIn] (the lab's top amount). */
    val loads: Boolean = false,
    val kind: OrderKind = OrderKind.REGULAR,
    /** A VIP's thank-you for a perfect slime: an unlock key. */
    val prize: String? = null,
    /** When a rush order gives up and leaves (epoch millis; 0 never). */
    val expires: Long = 0L,
) {
    val wishes get() = listOfNotNull(base, color, color2, addIn, mold).size + listOf(glitter, smiley).count { it }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("customer", customer); put("face", face)
        base?.let { put("base", it.id) }
        color?.let { put("color", it) }
        addIn?.let { put("addIn", it.id) }
        put("glitter", glitter); put("reward", reward); put("line", line)
        color2?.let { put("color2", it) }
        mold?.let { put("mold", it.id) }
        if (smiley) put("smiley", true)
        if (loads) put("loads", true)
        if (kind != OrderKind.REGULAR) put("kind", kind.id)
        prize?.let { put("prize", it) }
        if (expires != 0L) put("expires", expires)
    }

    companion object {
        private fun color(j: JSONObject, name: String) = j.optString(name).takeIf { id -> id.isNotEmpty() && COLORS.any { it.id == id } }

        fun fromJson(j: JSONObject) = Order(
            id = j.getString("id"),
            customer = j.optString("customer", "Customer"),
            face = j.optString("face").ifEmpty { Orders.faceOf(j.optString("customer")) },
            base = if (j.has("base")) Base.of(j.getString("base")) else null,
            color = color(j, "color"),
            addIn = AddIn.of(j.optString("addIn")),
            glitter = j.optBoolean("glitter"),
            reward = j.optInt("reward", 20),
            line = j.optString("line"),
            color2 = color(j, "color2"),
            mold = j.optString("mold").takeIf { it.isNotEmpty() }?.let { id -> Mold.entries.firstOrNull { it.id == id } },
            smiley = j.optBoolean("smiley"),
            loads = j.optBoolean("loads"),
            kind = OrderKind.of(j.optString("kind")),
            prize = j.optString("prize").takeIf { it.isNotEmpty() },
            expires = j.optLong("expires", 0L),
        )
    }
}

object Orders {
    private val customers = listOf(
        "Mia" to "mia", "Leo" to "leo", "Grandma Rose" to "rose", "Captain Zed" to "zed", "Pixel" to "pixel", "Nova" to "nova",
        "Mr. Whiskers" to "whiskers", "Bubbles" to "bubbles", "Professor Goo" to "goo", "Luna" to "luna", "Bao" to "bao", "Ziggy" to "ziggy",
        "Coach Kim" to "kim", "Dr. Fizz" to "fizz", "Penny" to "penny", "Rex" to "rex",
        "Sir Wobbles" to "wobbles", "Princess Pip" to "pip", "Robo-7" to "robo", "Chef Mango" to "mango",
        "Skater Sam" to "sam", "Madame Moon" to "moon", "Dino Dan" to "dan", "Aunt Jojo" to "jojo",
    )
    /** The fancy ones who turn up as VIPs. */
    private val vips = setOf("pip", "moon", "zed", "wobbles", "robo")

    /** Every customer's portrait id (for the art check). */
    val faces get() = customers.map { it.second }

    /** A customer's portrait id from their name (orders saved before portraits only had a name). */
    fun faceOf(name: String) = customers.firstOrNull { it.first == name }?.second ?: "mia"

    private val greetings = mapOf(
        OrderKind.REGULAR to listOf("Hi! Could I get", "Ooh, I'd love", "Hello! I'd like", "Please make me", "My kid wants", "I need", "Surprise me with", "Can you whip up"),
        OrderKind.RUSH to listOf("Quick, quick! I need", "Hurry please, I need", "No time to chat! Just", "My bus leaves soon! Can I get"),
        OrderKind.PICKY to listOf("It has to be EXACTLY", "Only perfect will do:", "Don't mess it up! I want", "I'm very particular. Precisely"),
        OrderKind.VIP to listOf("For my royal collection,", "A very special request:", "Only the finest:", "I'll pay handsomely for"),
    )

    /** How long a rush order waits. */
    const val RUSH_MILLIS = 8 * 60 * 1000L

    /** Order slots at the counter: more as the shop grows. */
    fun slots(level: Int) = 3 + (if (level >= 4) 1 else 0) + (if (level >= 8) 1 else 0)

    /**
     * A new order for a player at [level] who has unlocked what [unlocked] says. [hasVip] stops two VIPs at once.
     * [now] is the clock for rush orders.
     */
    fun generate(rnd: Random, unlocked: (String) -> Boolean, level: Int, hasVip: Boolean = false, now: Long = System.currentTimeMillis()): Order {
        val kind = run {
            val roll = rnd.nextFloat()
            when {
                level >= OrderKind.VIP.level && !hasVip && roll < 0.12f -> OrderKind.VIP
                level >= OrderKind.PICKY.level && roll < 0.24f -> OrderKind.PICKY
                level >= OrderKind.RUSH.level && roll < 0.42f -> OrderKind.RUSH
                else -> OrderKind.REGULAR
            }
        }
        val (who, face) = if (kind == OrderKind.VIP) customers.filter { it.second in vips }.let { it[rnd.nextInt(it.size)] } else customers[rnd.nextInt(customers.size)]
        val bases = Base.entries.filter { unlocked(key(it)) }
        val colors = COLORS.filter { unlocked(key(it)) }
        val addIns = AddIn.entries.filter { unlocked(key(it)) }
        val molds = Mold.entries.filter { it != Mold.DOME && unlocked(key(it)) }
        // Harder orders as the shop grows: up to two wishes early on, more later; VIPs always want a lot.
        val maxWishes = (2 + level / 3).coerceAtMost(5)
        val wishes = when (kind) {
            OrderKind.VIP -> 3 + rnd.nextInt(3)
            OrderKind.RUSH -> 1 + rnd.nextInt(2)
            else -> 1 + rnd.nextInt(maxWishes)
        }
        val options = buildList {
            add("base"); add("color"); add("addIn"); add("glitter")
            if (level >= 2) add("smiley")
            if (level >= 3 && colors.size > 1) add("color2")
            if (molds.isNotEmpty()) add("mold")
        }.shuffled(rnd)
        val pool = options.take(wishes).toSet()
        val base = if ("base" in pool) bases[rnd.nextInt(bases.size)] else null
        val color = if ("color" in pool || "color2" in pool) colors[rnd.nextInt(colors.size)].id else null
        val color2 = if ("color2" in pool) colors.filter { it.id != color }.let { it[rnd.nextInt(it.size)].id } else null
        val addIn = if ("addIn" in pool && addIns.isNotEmpty()) addIns[rnd.nextInt(addIns.size)] else null
        val loads = addIn != null && level >= 2 && rnd.nextFloat() < 0.3f
        val mold = if ("mold" in pool) molds[rnd.nextInt(molds.size)] else null
        val glitter = "glitter" in pool
        val smiley = "smiley" in pool
        val count = listOfNotNull(base, color, color2, addIn, mold).size + listOf(glitter, smiley).count { it }
        val reward = ((14 + count * 12 + rnd.nextInt(10) + level * 2) * kind.pay).toInt()
        val prize = if (kind == OrderKind.VIP) {
            allKeys().filter { !unlocked(it) && levelOf(it) <= level + 1 && priceOf(it) in 1..320 }.let { if (it.isEmpty()) null else it[rnd.nextInt(it.size)] }
        } else null
        val line = "${greetings.getValue(kind).let { it[rnd.nextInt(it.size)] }} ${describe(base, color, color2, addIn, loads, mold, glitter, smiley)}!"
        return Order(
            "o" + rnd.nextLong().toULong().toString(36), who, face, base, color, addIn, glitter, reward, line,
            color2 = color2, mold = mold, smiley = smiley, loads = loads, kind = kind, prize = prize,
            expires = if (kind == OrderKind.RUSH) now + RUSH_MILLIS else 0L,
        )
    }

    /** "a mint and lemon jelly bear slime with loads of stars, glitter and a cute face" */
    private fun describe(base: Base?, color: String?, color2: String?, addIn: AddIn?, loads: Boolean, mold: Mold?, glitter: Boolean, smiley: Boolean): String {
        val cols = listOfNotNull(color, color2).map { colorOf(it).title.lowercase() }
        val shape = mold?.let { if (it.animal) it.title.lowercase() else "${it.title.lowercase()}-shaped" }
        val head = (listOfNotNull(cols.joinToString(" and ").ifEmpty { null }, base?.title?.lowercase(), shape) + "slime").joinToString(" ")
        val extras = listOfNotNull(
            addIn?.let { (if (loads) "loads of " else "") + it.title.lowercase() },
            if (glitter) "glitter" else null,
            if (smiley) "a cute face" else null,
        )
        val tail = when (extras.size) {
            0 -> ""
            1 -> " with ${extras[0]}"
            else -> " with ${extras.dropLast(1).joinToString(", ")} and ${extras.last()}"
        }
        val what = head + tail
        val article = if (what.first().lowercaseChar() in "aeiou") "an" else "a"
        return "$article $what"
    }

    /** How well [r] matches: the share of wishes met (0..1). The first colour counts most, but any of them will do. */
    fun match(o: Order, r: Recipe): Float {
        var want = 0f
        var got = 0f
        o.base?.let { want += 1; if (r.base == it) got += 1 }
        o.color?.let { c -> want += 1; got += when { r.colors.firstOrNull() == c -> 1f; c in r.colors -> 0.7f; else -> 0f } }
        o.color2?.let { c -> want += 1; if (c in r.colors) got += 1 }
        o.addIn?.let { a ->
            want += 1
            val lvl = r.addIns[a]
            if (lvl != null) got += if (!o.loads || lvl >= ADD_IN_LEVELS.lastIndex) 1f else 0.6f
        }
        o.mold?.let { want += 1; if (r.mold == it) got += 1 }
        if (o.glitter) { want += 1; if (r.glitter != Glitter.NONE) got += 1 }
        if (o.smiley) { want += 1; if (r.face) got += 1 }
        return if (want == 0f) 1f else got / want
    }

    /** 1 to 3 stars: everything right is 3, most of it 2, a try 1. */
    fun stars(o: Order, r: Recipe): Int {
        val m = match(o, r)
        return when {
            m >= 0.999f -> 3
            m >= 0.5f -> 2
            else -> 1
        }
    }

    /** What the customer pays for [stars]. Picky customers barely pay for anything less than perfect. */
    fun payout(o: Order, stars: Int) = when {
        stars >= 3 -> o.reward
        o.kind == OrderKind.PICKY -> (o.reward * 0.15f).toInt().coerceAtLeast(3)
        stars == 2 -> (o.reward * 0.6f).toInt()
        else -> (o.reward * 0.25f).toInt().coerceAtLeast(3)
    }

    /** The bonus share for a run of perfect orders in a row: +10% each, up to +50%. */
    fun comboBonus(combo: Int) = 0.1f * combo.coerceIn(0, 5)
}

/** Unlock keys, shared by the supply shop, orders and the lab. Free things are always unlocked. */
fun key(b: Base) = "base:${b.id}"
fun key(c: SlimeColor) = "color:${c.id}"
fun key(a: AddIn) = "addin:${a.id}"
fun key(g: Glitter) = "glitter:${g.id}"
fun key(t: Tray) = "tray:${t.id}"
fun key(t: Toy) = "toy:${t.id}"
fun key(w: World) = "world:${w.id}"
fun key(p: Plate) = "plate:${p.id}"
fun key(m: Mold) = "mold:${m.id}"
fun key(v: Vessel) = "vessel:${v.id}"

/** Every unlock key in the game. */
fun allKeys(): List<String> =
    Base.entries.map(::key) + COLORS.map(::key) + AddIn.entries.map(::key) + Glitter.entries.map(::key) + Tray.entries.map(::key) + Toy.entries.map(::key) +
        World.entries.map(::key) + Plate.entries.map(::key) + Mold.entries.map(::key) + Vessel.entries.map(::key)

private fun split(k: String) = k.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }

fun priceOf(k: String): Int {
    val (kind, id) = split(k)
    return when (kind) {
        "base" -> Base.entries.firstOrNull { it.id == id }?.price
        "color" -> COLORS.firstOrNull { it.id == id }?.price
        "addin" -> AddIn.entries.firstOrNull { it.id == id }?.price
        "glitter" -> Glitter.entries.firstOrNull { it.id == id }?.price
        "tray" -> Tray.entries.firstOrNull { it.id == id }?.price
        "toy" -> Toy.entries.firstOrNull { it.id == id }?.price
        "world" -> World.entries.firstOrNull { it.id == id }?.price
        "plate" -> Plate.entries.firstOrNull { it.id == id }?.price
        "mold" -> Mold.entries.firstOrNull { it.id == id }?.price
        "vessel" -> Vessel.entries.firstOrNull { it.id == id }?.price
        else -> null
    } ?: 0
}

/**
 * The level a key can be bought from. Toys, worlds, dishes, molds, containers and bases say so themselves; the small
 * supplies (colours, add-ins, glitter, dish colours) go by price.
 */
fun levelOf(k: String): Int {
    val (kind, id) = split(k)
    return when (kind) {
        "base" -> Base.entries.firstOrNull { it.id == id }?.level
        "toy" -> Toy.entries.firstOrNull { it.id == id }?.level
        "world" -> World.entries.firstOrNull { it.id == id }?.level
        "plate" -> Plate.entries.firstOrNull { it.id == id }?.level
        "mold" -> Mold.entries.firstOrNull { it.id == id }?.level
        "vessel" -> Vessel.entries.firstOrNull { it.id == id }?.level
        "race" -> Cup.entries.firstOrNull { it.id == id }?.level ?: RACE_LEVEL
        else -> priceOf(k).let { p -> when { p < 100 -> 1; p < 150 -> 2; p < 180 -> 3; else -> 4 } }
    } ?: 1
}

/** "Cloud base", "Mint", "Fishbowl beads", for a key. */
fun label(k: String): String {
    val (kind, id) = split(k)
    return when (kind) {
        "base" -> Base.entries.firstOrNull { it.id == id }?.let { "${it.title} base" }
        "color" -> COLORS.firstOrNull { it.id == id }?.title
        "addin" -> AddIn.entries.firstOrNull { it.id == id }?.title
        "glitter" -> Glitter.entries.firstOrNull { it.id == id }?.let { "${it.title} glitter" }
        "tray" -> Tray.entries.firstOrNull { it.id == id }?.let { "${it.title} dish" }
        "toy" -> Toy.entries.firstOrNull { it.id == id }?.title
        "world" -> World.entries.firstOrNull { it.id == id }?.title
        "plate" -> Plate.entries.firstOrNull { it.id == id }?.let { "${it.title} dish shape" }
        "mold" -> Mold.entries.firstOrNull { it.id == id }?.let { "${it.title} mold" }
        "vessel" -> Vessel.entries.firstOrNull { it.id == id }?.title
        else -> null
    } ?: k
}

/** The art for a key (colours have none: they're drawn as a swatch). */
fun iconOf(k: String): String? {
    val (kind, id) = split(k)
    return when (kind) {
        "base" -> "base_$id"
        "addin" -> "addin_$id"
        "glitter" -> "glitter_$id"
        "tray" -> "tray_$id"
        "toy" -> "tool_$id"
        "world" -> "world_$id"
        "plate" -> "plate_$id"
        "mold" -> "mold_$id"
        "vessel" -> "vessel_$id"
        else -> null
    }
}
