package dev.m4s4t0.voidslime.model

/**
 * How a slime base moves and looks. The physics side ([shapeK] … [maxStretch]) drives sim/SoftBody, the look side
 * ([alpha] … [frost]) the slime shader in gl/Shaders.kt.
 */
data class Material(
    /** Pull back to its resting shape: low is gooey, high is bouncy. */
    val shapeK: Float,
    /** Springs between neighbouring points: how far a poke spreads across the surface. */
    val edgeK: Float,
    /** How quickly a wobble dies away. */
    val damping: Float,
    /** How much of a dent or stretch the slime keeps (per second). */
    val plastic: Float,
    /** How fast it settles back into a dome (per second). */
    val relax: Float,
    /** Holding its volume: squash it and it bulges out the sides. */
    val volumeK: Float,
    val gravity: Float,
    /** Stretch further than this (slime radii) and it snaps back. */
    val maxStretch: Float,
    /** 1 is opaque; below that the slime is see-through and add-ins float inside it. */
    val alpha: Float,
    val gloss: Float,
    val fluff: Float = 0f,
    val metal: Float = 0f,
    val holo: Float = 0f,
    val glow: Float = 0f,
    val frost: Float = 0f,
    /** Squishing it crunches by itself (Icee). */
    val crunchy: Boolean = false,
)

enum class Base(val title: String, val emoji: String, val blurb: String, val price: Int, val mat: Material) {
    GLOSSY("Glossy", "💧", "Wet-look shine, a classic", 0,
        Material(shapeK = 26f, edgeK = 1400f, damping = 3.2f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.6f, maxStretch = 2.3f, alpha = 1f, gloss = 1f)),
    CLEAR("Clear", "🫧", "Glass-clear and stretchy", 0,
        Material(shapeK = 22f, edgeK = 1300f, damping = 2.8f, plastic = 0.4f, relax = 0.4f, volumeK = 30f, gravity = 1.4f, maxStretch = 2.7f, alpha = 0.32f, gloss = 1f)),
    BUTTER("Butter", "🧈", "Thick, smooth, holds a shape", 0,
        Material(shapeK = 18f, edgeK = 1800f, damping = 5.5f, plastic = 0.9f, relax = 0.18f, volumeK = 40f, gravity = 1.1f, maxStretch = 1.6f, alpha = 1f, gloss = 0.28f)),
    CLOUD("Cloud", "☁️", "Fluffy, airy and drizzly", 120,
        Material(shapeK = 16f, edgeK = 1000f, damping = 4.5f, plastic = 0.6f, relax = 0.3f, volumeK = 26f, gravity = 0.9f, maxStretch = 2.0f, alpha = 1f, gloss = 0.12f, fluff = 1f)),
    JELLY("Jelly", "🍮", "Bouncy, wobbly, can't stop jiggling", 150,
        Material(shapeK = 46f, edgeK = 2200f, damping = 1.3f, plastic = 0.04f, relax = 1.4f, volumeK = 46f, gravity = 2.8f, maxStretch = 1.3f, alpha = 0.62f, gloss = 1f)),
    ICEE("Icee", "🧊", "Frosty, crackly, crunches as you squish", 200,
        Material(shapeK = 24f, edgeK = 1500f, damping = 3.6f, plastic = 0.45f, relax = 0.35f, volumeK = 32f, gravity = 1.5f, maxStretch = 2.0f, alpha = 0.55f, gloss = 0.7f, frost = 1f, crunchy = true)),
    CHROME("Chrome", "🪩", "Liquid metal, a mirror you can squish", 250,
        Material(shapeK = 26f, edgeK = 1500f, damping = 3.4f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.6f, maxStretch = 2.1f, alpha = 1f, gloss = 1f, metal = 1f)),
    HOLO("Holo", "🌈", "Rainbow sheen that shifts as it moves", 300,
        Material(shapeK = 24f, edgeK = 1400f, damping = 3.0f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.5f, maxStretch = 2.3f, alpha = 1f, gloss = 1f, holo = 1f)),
    GLOW("Glow", "✨", "Glows in the dark: turn the lights off", 350,
        Material(shapeK = 24f, edgeK = 1400f, damping = 3.0f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.5f, maxStretch = 2.3f, alpha = 0.85f, gloss = 0.9f, glow = 1f)),
    ;

    val id get() = name.lowercase()

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: GLOSSY
    }
}

data class SlimeColor(val id: String, val title: String, val argb: Long, val price: Int)

val COLORS = listOf(
    SlimeColor("void", "Void violet", 0xFFA970FF, 0),
    SlimeColor("cyan", "Void cyan", 0xFF22E3FF, 0),
    SlimeColor("bubblegum", "Bubblegum", 0xFFFF7EC8, 0),
    SlimeColor("strawberry", "Strawberry", 0xFFFF4D6D, 0),
    SlimeColor("lemon", "Lemon", 0xFFFFE45C, 0),
    SlimeColor("mint", "Mint", 0xFF6EF2B4, 0),
    SlimeColor("ocean", "Ocean", 0xFF3D7BFF, 0),
    SlimeColor("tangerine", "Tangerine", 0xFFFF8A3D, 0),
    SlimeColor("snow", "Snow", 0xFFF4F1FF, 0),
    SlimeColor("lavender", "Lavender", 0xFFC9A8FF, 0),
    SlimeColor("peach", "Peach", 0xFFFFB896, 40),
    SlimeColor("lime", "Lime", 0xFFB6FF3D, 40),
    SlimeColor("sky", "Sky", 0xFF8FD8FF, 40),
    SlimeColor("cherry", "Cherry", 0xFFC2123B, 60),
    SlimeColor("grape", "Grape", 0xFF7A2BD8, 60),
    SlimeColor("teal", "Teal", 0xFF14B8A6, 60),
    SlimeColor("coral", "Coral", 0xFFFF6F61, 80),
    SlimeColor("rosegold", "Rose gold", 0xFFE8A598, 80),
    SlimeColor("butterscotch", "Butterscotch", 0xFFE7A33E, 80),
    SlimeColor("midnight", "Midnight", 0xFF1B1F5E, 100),
    SlimeColor("matcha", "Matcha", 0xFF8DB255, 100),
    SlimeColor("blush", "Blush", 0xFFFFD1DC, 100),
    SlimeColor("coal", "Coal", 0xFF2A2730, 120),
    SlimeColor("neon", "Neon pink", 0xFFFF2BD6, 150),
)

fun colorOf(id: String) = COLORS.firstOrNull { it.id == id } ?: COLORS.first()

/** What the add-in sprite looks like (the point-sprite shader's shape number). */
enum class Shape { BEAD, STAR, HEART, SPRINKLE, SLICE, FLAKE, CRYSTAL, GLASS, DISC }

enum class AddIn(
    val title: String,
    val emoji: String,
    val shape: Shape,
    /** Sprite size in slime radii. */
    val size: Float,
    /** Crunches under your finger. */
    val crunchy: Boolean,
    val price: Int,
    /** Fixed colours (ARGB); empty means a mix of bright ones. */
    val tints: List<Long>,
) {
    FOAM("Foam beads", "⚪", Shape.BEAD, 0.075f, true, 0, listOf(0xFFFFFFFF, 0xFFFFD6EC, 0xFFD8F4FF, 0xFFFFF3B0)),
    SPRINKLES("Sprinkles", "🍬", Shape.SPRINKLE, 0.09f, false, 0, emptyList()),
    STARS("Stars", "⭐", Shape.STAR, 0.11f, false, 0, listOf(0xFFFFE066, 0xFFFFFFFF, 0xFF8FE9FF)),
    HEARTS("Hearts", "💖", Shape.HEART, 0.11f, false, 60, listOf(0xFFFF4D8D, 0xFFFF9EC7, 0xFFFF2B55)),
    PEARLS("Pearls", "🦪", Shape.BEAD, 0.085f, false, 80, listOf(0xFFFFF8F0, 0xFFF3E6FF, 0xFFE6FFF9)),
    FISHBOWL("Fishbowl beads", "🔮", Shape.GLASS, 0.12f, true, 100, emptyList()),
    SUGAR("Sugar crystals", "💎", Shape.CRYSTAL, 0.07f, true, 120, listOf(0xFFFFFFFF, 0xFFE0F7FF)),
    FRUIT("Fruit slices", "🍊", Shape.SLICE, 0.14f, false, 140, listOf(0xFFFF9F1C, 0xFFFFE14D, 0xFFFF4D6D, 0xFF8BE04E)),
    SNOW("Snowflakes", "❄️", Shape.FLAKE, 0.11f, false, 160, listOf(0xFFFFFFFF, 0xFFDDF3FF)),
    CONFETTI("Confetti", "🎉", Shape.DISC, 0.07f, false, 180, emptyList()),
    ;

    val id get() = name.lowercase()

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id }
        val BRIGHT = listOf(0xFFFF4D8D, 0xFFFFE066, 0xFF22E3FF, 0xFF6EF2B4, 0xFFA970FF, 0xFFFF8A3D, 0xFFFFFFFF)
    }
}

/** How many of an add-in go in. */
val ADD_IN_LEVELS = listOf("A pinch" to 16, "Some" to 36, "Loads" to 70)

enum class Glitter(val title: String, val argb: Long, val price: Int) {
    NONE("None", 0, 0),
    SILVER("Silver", 0xFFFFFFFF, 0),
    GOLD("Gold", 0xFFFFD36B, 60),
    ROSE("Rose", 0xFFFF9ECF, 90),
    RAINBOW("Rainbow", 0, 150),
    ;

    val id get() = name.lowercase()

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** The dish the slime sits in, and the room's light around it. */
enum class Tray(val title: String, val emoji: String, val argb: Long, val gloss: Float, val price: Int) {
    VOID("Void glass", "🌌", 0xFF262046, 1f, 0),
    MARBLE("Marble", "🤍", 0xFFEDEAF2, 0.7f, 100),
    BUBBLEGUM("Bubblegum", "🩷", 0xFFFFB3D9, 0.5f, 120),
    MINT("Mint", "🌿", 0xFFA8F0D0, 0.5f, 120),
    WOOD("Wood", "🪵", 0xFFB07A4A, 0.25f, 160),
    NEON("Neon", "💜", 0xFF14101F, 1f, 220),
    ;

    val id get() = name.lowercase()

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: VOID
    }
}
