package dev.m4s4t0.voidslime.model

import org.json.JSONObject
import kotlin.random.Random

/**
 * A customer at the Slime Shop asking for a slime. Each wish is optional; the more wishes, the bigger the reward.
 * Orders only ever ask for things the player has unlocked.
 */
data class Order(
    val id: String,
    val customer: String,
    val emoji: String,
    val base: Base?,
    val color: String?,
    val addIn: AddIn?,
    val glitter: Boolean,
    val reward: Int,
    val line: String,
) {
    val wishes get() = listOfNotNull(base, color, addIn).size + (if (glitter) 1 else 0)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("customer", customer); put("emoji", emoji)
        base?.let { put("base", it.id) }
        color?.let { put("color", it) }
        addIn?.let { put("addIn", it.id) }
        put("glitter", glitter); put("reward", reward); put("line", line)
    }

    companion object {
        fun fromJson(j: JSONObject) = Order(
            id = j.getString("id"),
            customer = j.optString("customer", "Customer"),
            emoji = j.optString("emoji", "🙂"),
            base = if (j.has("base")) Base.of(j.getString("base")) else null,
            color = j.optString("color").takeIf { id -> id.isNotEmpty() && COLORS.any { it.id == id } },
            addIn = AddIn.of(j.optString("addIn")),
            glitter = j.optBoolean("glitter"),
            reward = j.optInt("reward", 20),
            line = j.optString("line"),
        )
    }
}

object Orders {
    private val customers = listOf(
        "Mia" to "👧", "Leo" to "👦", "Grandma Rose" to "👵", "Captain Zed" to "👨‍🚀", "Pixel" to "🤖", "Nova" to "🧚",
        "Mr. Whiskers" to "🐱", "Bubbles" to "🐙", "Professor Goo" to "🧑‍🔬", "Luna" to "🦄", "Bao" to "🐼", "Ziggy" to "👽",
        "Coach Kim" to "🏋️", "Dr. Fizz" to "🧪", "Penny" to "🐧", "Rex" to "🦖",
    )
    private val greetings = listOf("Hi! Could I get", "Ooh, I'd love", "Hello! I'd like", "Please make me", "My kid wants", "I need", "Surprise me with", "Can you whip up")

    fun generate(rnd: Random, unlocked: (String) -> Boolean, level: Int): Order {
        val (who, emoji) = customers[rnd.nextInt(customers.size)]
        val bases = Base.entries.filter { unlocked(key(it)) }
        val colors = COLORS.filter { unlocked(key(it)) }
        val addIns = AddIn.entries.filter { unlocked(key(it)) }
        // Harder orders as the shop grows: up to two wishes early on, up to four later.
        val maxWishes = (2 + level / 3).coerceAtMost(4)
        val wishes = 1 + rnd.nextInt(maxWishes)
        val pool = mutableListOf("base", "color", "addIn", "glitter").apply { shuffle(rnd) }.take(wishes).toSet()
        val base = if ("base" in pool) bases[rnd.nextInt(bases.size)] else null
        val color = if ("color" in pool) colors[rnd.nextInt(colors.size)].id else null
        val addIn = if ("addIn" in pool && addIns.isNotEmpty()) addIns[rnd.nextInt(addIns.size)] else null
        val glitter = "glitter" in pool
        val reward = 14 + wishes * 12 + rnd.nextInt(10) + level * 2
        val parts = listOfNotNull(
            color?.let { colorOf(it).title.lowercase() },
            base?.title?.lowercase(),
            "slime",
            addIn?.let { "with ${it.title.lowercase()}" },
            if (glitter) (if (addIn != null) "and glitter" else "with glitter") else null,
        )
        val what = parts.joinToString(" ")
        val article = if (what.first().lowercaseChar() in "aeiou") "an" else "a"
        val line = "${greetings[rnd.nextInt(greetings.size)]} $article $what!"
        return Order("o" + rnd.nextLong().toULong().toString(36), who, emoji, base, color, addIn, glitter, reward, line)
    }

    /** How well [r] matches: the share of wishes met (0..1). The first colour counts most, but any of them will do. */
    fun match(o: Order, r: Recipe): Float {
        var want = 0f
        var got = 0f
        o.base?.let { want += 1; if (r.base == it) got += 1 }
        o.color?.let { c -> want += 1; got += when { r.colors.firstOrNull() == c -> 1f; c in r.colors -> 0.7f; else -> 0f } }
        o.addIn?.let { want += 1; if (it in r.addIns) got += 1 }
        if (o.glitter) { want += 1; if (r.glitter != Glitter.NONE) got += 1 }
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

    fun payout(o: Order, stars: Int) = when (stars) {
        3 -> o.reward
        2 -> (o.reward * 0.6f).toInt()
        else -> (o.reward * 0.25f).toInt().coerceAtLeast(3)
    }
}

/** Unlock keys, shared by the supply shop, orders and the lab. Free things are always unlocked. */
fun key(b: Base) = "base:${b.id}"
fun key(c: SlimeColor) = "color:${c.id}"
fun key(a: AddIn) = "addin:${a.id}"
fun key(g: Glitter) = "glitter:${g.id}"
fun key(t: Tray) = "tray:${t.id}"

fun priceOf(k: String): Int {
    val (kind, id) = k.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return when (kind) {
        "base" -> Base.entries.firstOrNull { it.id == id }?.price
        "color" -> COLORS.firstOrNull { it.id == id }?.price
        "addin" -> AddIn.entries.firstOrNull { it.id == id }?.price
        "glitter" -> Glitter.entries.firstOrNull { it.id == id }?.price
        "tray" -> Tray.entries.firstOrNull { it.id == id }?.price
        else -> null
    } ?: 0
}

/** "Cloud base", "Mint", "Fishbowl beads", for a key. */
fun label(k: String): String {
    val (kind, id) = k.split(':', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    return when (kind) {
        "base" -> Base.entries.firstOrNull { it.id == id }?.let { "${it.emoji} ${it.title} base" }
        "color" -> COLORS.firstOrNull { it.id == id }?.let { "🎨 ${it.title}" }
        "addin" -> AddIn.entries.firstOrNull { it.id == id }?.let { "${it.emoji} ${it.title}" }
        "glitter" -> Glitter.entries.firstOrNull { it.id == id }?.let { "✨ ${it.title} glitter" }
        "tray" -> Tray.entries.firstOrNull { it.id == id }?.let { "${it.emoji} ${it.title} dish" }
        else -> null
    } ?: k
}
