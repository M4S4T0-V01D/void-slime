package dev.m4s4t0.voidslime.model

/** The level Slime racing opens at, and its key (for challenges that need it). */
const val RACE_LEVEL = 3
const val RACE_KEY = "race:cup"

/**
 * A slime racing cup: a track through one of the worlds. Later cups are longer, with quicker rivals, more puddles
 * and bigger prizes. [prizes] are the coins for 1st to 4th.
 */
enum class Cup(
    val title: String,
    val blurb: String,
    val level: Int,
    val world: World,
    /** Track length in lane-heights. */
    val length: Float,
    /** How fast the rivals hop (1 is a steady player). */
    val rivals: Float,
    /** Puddles and boost pads per lane-height of track. */
    val puddles: Float,
    val boosts: Float,
    val prizes: List<Int>,
) {
    MEADOW("Meadow cup", "A sunny sprint through the grass", RACE_LEVEL, World.MEADOW, 60f, 0.82f, 0.05f, 0.05f, listOf(40, 25, 15, 8)),
    CANDY("Candy cup", "Sticky puddles and sugar rushes", 5, World.CANDY, 75f, 0.92f, 0.07f, 0.06f, listOf(60, 35, 20, 10)),
    SPACE("Space cup", "Low gravity, big hops, fast rivals", 8, World.SPACE, 90f, 1.0f, 0.08f, 0.07f, listOf(90, 50, 30, 15)),
    ;

    val id get() = name.lowercase()

    /** XP for finishing in [place] (0 is first). */
    fun xp(place: Int) = (40 - place * 8) + ordinal * 10

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: MEADOW
    }
}

/** The rival slimes on the track: name, colour (ARGB). */
val RIVALS = listOf(
    "Gloopy" to 0xFF6EF2B4, "Wobblina" to 0xFFFF7EC8, "Sir Squish" to 0xFFFFE45C, "Blorp" to 0xFF22E3FF,
    "Jiggles" to 0xFFFF8A3D, "Goo-Goo" to 0xFFC9A8FF, "Splatty" to 0xFFFF4D6D, "Mr. Ooze" to 0xFFB6FF3D,
)
