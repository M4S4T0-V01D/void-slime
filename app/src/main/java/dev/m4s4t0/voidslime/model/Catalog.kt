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
    /** Dark sky with a nebula and twinkling stars in it. */
    val galaxy: Float = 0f,
    /** Changes colour as it warms up while you play with it, and cools back. */
    val thermo: Float = 0f,
    /** Squishing it crunches by itself (Icee). */
    val crunchy: Boolean = false,
)

enum class Base(val title: String, val blurb: String, val price: Int, val mat: Material, val level: Int = 1) {
    GLOSSY("Glossy", "Wet-look shine, a classic", 0,
        Material(shapeK = 26f, edgeK = 1400f, damping = 3.2f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.6f, maxStretch = 2.3f, alpha = 1f, gloss = 1f)),
    CLEAR("Clear", "Glass-clear and stretchy", 0,
        Material(shapeK = 22f, edgeK = 1300f, damping = 2.8f, plastic = 0.4f, relax = 0.4f, volumeK = 30f, gravity = 1.4f, maxStretch = 2.7f, alpha = 0.32f, gloss = 1f)),
    BUTTER("Butter", "Thick, smooth, holds a shape", 0,
        Material(shapeK = 18f, edgeK = 1800f, damping = 5.5f, plastic = 0.9f, relax = 0.18f, volumeK = 40f, gravity = 1.1f, maxStretch = 1.6f, alpha = 1f, gloss = 0.28f)),
    CLOUD("Cloud", "Fluffy, airy and drizzly", 120,
        Material(shapeK = 16f, edgeK = 1000f, damping = 4.5f, plastic = 0.6f, relax = 0.3f, volumeK = 26f, gravity = 0.9f, maxStretch = 2.0f, alpha = 1f, gloss = 0.12f, fluff = 1f), level = 2),
    JELLY("Jelly", "Bouncy, wobbly, can't stop jiggling", 150,
        Material(shapeK = 46f, edgeK = 2200f, damping = 1.3f, plastic = 0.04f, relax = 1.4f, volumeK = 46f, gravity = 2.8f, maxStretch = 1.3f, alpha = 0.62f, gloss = 1f), level = 2),
    ICEE("Icee", "Frosty, crackly, crunches as you squish", 200,
        Material(shapeK = 24f, edgeK = 1500f, damping = 3.6f, plastic = 0.45f, relax = 0.35f, volumeK = 32f, gravity = 1.5f, maxStretch = 2.0f, alpha = 0.55f, gloss = 0.7f, frost = 1f, crunchy = true), level = 3),
    CHROME("Chrome", "Liquid metal, a mirror you can squish", 250,
        Material(shapeK = 26f, edgeK = 1500f, damping = 3.4f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.6f, maxStretch = 2.1f, alpha = 1f, gloss = 1f, metal = 1f), level = 4),
    HOLO("Holo", "Rainbow sheen that shifts as it moves", 300,
        Material(shapeK = 24f, edgeK = 1400f, damping = 3.0f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.5f, maxStretch = 2.3f, alpha = 1f, gloss = 1f, holo = 1f), level = 4),
    GLOW("Glow", "Glows in the dark: turn the lights off", 350,
        Material(shapeK = 24f, edgeK = 1400f, damping = 3.0f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.5f, maxStretch = 2.3f, alpha = 0.85f, gloss = 0.9f, glow = 1f), level = 3),
    FLOAM("Floam", "Packed with beads, crunches everywhere", 220,
        Material(shapeK = 20f, edgeK = 1600f, damping = 4.8f, plastic = 0.7f, relax = 0.22f, volumeK = 38f, gravity = 1.2f, maxStretch = 1.5f, alpha = 1f, gloss = 0.35f, fluff = 0.45f, crunchy = true), level = 3),
    PEARL("Pearl", "Soft pearly shimmer, silky smooth", 260,
        Material(shapeK = 24f, edgeK = 1500f, damping = 3.4f, plastic = 0.4f, relax = 0.4f, volumeK = 34f, gravity = 1.5f, maxStretch = 2.2f, alpha = 1f, gloss = 0.9f, metal = 0.3f, holo = 0.3f), level = 5),
    MAGNETIC("Magnetic", "Iron slime: the magnet toy loves it", 320,
        Material(shapeK = 20f, edgeK = 1500f, damping = 3.6f, plastic = 0.5f, relax = 0.35f, volumeK = 34f, gravity = 1.7f, maxStretch = 2.4f, alpha = 1f, gloss = 0.85f, metal = 0.55f), level = 5),
    THERMO("Thermo", "Changes colour as you play with it", 380,
        Material(shapeK = 26f, edgeK = 1400f, damping = 3.2f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.6f, maxStretch = 2.3f, alpha = 1f, gloss = 0.8f, thermo = 1f), level = 6),
    GALAXY("Galaxy", "A night sky full of stars", 420,
        Material(shapeK = 24f, edgeK = 1400f, damping = 3.0f, plastic = 0.35f, relax = 0.45f, volumeK = 34f, gravity = 1.5f, maxStretch = 2.4f, alpha = 1f, gloss = 1f, galaxy = 1f), level = 7),
    ;

    val id get() = name.lowercase()
    val icon get() = "base_$id"

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
    SlimeColor("sunset", "Sunset", 0xFFFF5E3A, 60),
    SlimeColor("blueberry", "Blueberry", 0xFF4F46E5, 60),
    SlimeColor("ruby", "Ruby", 0xFFE0115F, 80),
    SlimeColor("emerald", "Emerald", 0xFF10B981, 80),
    SlimeColor("aqua", "Aqua", 0xFF00E5D4, 80),
    SlimeColor("lilac", "Lilac", 0xFFE0B0FF, 80),
    SlimeColor("milktea", "Milk tea", 0xFFD2B48C, 100),
    SlimeColor("cocoa", "Cocoa", 0xFF6B4226, 100),
    SlimeColor("pistachio", "Pistachio", 0xFFC5E384, 100),
    SlimeColor("flamingo", "Flamingo", 0xFFFF8FAB, 120),
    SlimeColor("sunflower", "Sunflower", 0xFFFFC233, 120),
    SlimeColor("electric", "Electric blue", 0xFF0066FF, 150),
    SlimeColor("cosmic", "Cosmic purple", 0xFF3B0A6B, 150),
    SlimeColor("toxic", "Toxic green", 0xFF7CFF00, 180),
)

fun colorOf(id: String) = COLORS.firstOrNull { it.id == id } ?: COLORS.first()

/** What the add-in sprite looks like (the point-sprite shader's shape number; SPARK is the particles' glow). */
enum class Shape { BEAD, STAR, HEART, SPRINKLE, SLICE, FLAKE, CRYSTAL, GLASS, DISC, SPARK, FLOWER, MOON }

enum class AddIn(
    val title: String,
    val shape: Shape,
    /** Sprite size in slime radii. */
    val size: Float,
    /** Crunches under your finger. */
    val crunchy: Boolean,
    val price: Int,
    /** Fixed colours (ARGB); empty means a mix of bright ones. */
    val tints: List<Long>,
) {
    FOAM("Foam beads", Shape.BEAD, 0.075f, true, 0, listOf(0xFFFFFFFF, 0xFFFFD6EC, 0xFFD8F4FF, 0xFFFFF3B0)),
    SPRINKLES("Sprinkles", Shape.SPRINKLE, 0.09f, false, 0, emptyList()),
    STARS("Stars", Shape.STAR, 0.11f, false, 0, listOf(0xFFFFE066, 0xFFFFFFFF, 0xFF8FE9FF)),
    HEARTS("Hearts", Shape.HEART, 0.11f, false, 60, listOf(0xFFFF4D8D, 0xFFFF9EC7, 0xFFFF2B55)),
    PEARLS("Pearls", Shape.BEAD, 0.085f, false, 80, listOf(0xFFFFF8F0, 0xFFF3E6FF, 0xFFE6FFF9)),
    FISHBOWL("Fishbowl beads", Shape.GLASS, 0.12f, true, 100, emptyList()),
    SUGAR("Sugar crystals", Shape.CRYSTAL, 0.07f, true, 120, listOf(0xFFFFFFFF, 0xFFE0F7FF)),
    FRUIT("Fruit slices", Shape.SLICE, 0.14f, false, 140, listOf(0xFFFF9F1C, 0xFFFFE14D, 0xFFFF4D6D, 0xFF8BE04E)),
    SNOW("Snowflakes", Shape.FLAKE, 0.11f, false, 160, listOf(0xFFFFFFFF, 0xFFDDF3FF)),
    CONFETTI("Confetti", Shape.DISC, 0.07f, false, 180, emptyList()),
    MICRO("Micro beads", Shape.BEAD, 0.045f, true, 130, listOf(0xFFFFFFFF, 0xFFE8F6FF, 0xFFFFEAF6)),
    DAISIES("Daisies", Shape.FLOWER, 0.13f, false, 150, listOf(0xFFFFFFFF, 0xFFFFE066, 0xFFFF9EC7, 0xFFC9A8FF)),
    MOONS("Moons", Shape.MOON, 0.11f, false, 170, listOf(0xFFFFD36B, 0xFFFFF6D8)),
    GOLDLEAF("Gold leaf", Shape.DISC, 0.085f, false, 190, listOf(0xFFFFD36B, 0xFFFFE9A8, 0xFFE8B04B)),
    GEMS("Rhinestones", Shape.CRYSTAL, 0.09f, true, 200, emptyList()),
    GUMMIES("Gummy worms", Shape.SPRINKLE, 0.17f, false, 210, listOf(0xFFFF4D6D, 0xFFFFE14D, 0xFF8BE04E, 0xFFFF8A3D, 0xFF3DA5FF)),
    ;

    val id get() = name.lowercase()
    val icon get() = "addin_$id"

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
    AQUA("Aqua", 0xFF7DF9FF, 70),
    VIOLET("Violet", 0xFFB98CFF, 70),
    EMERALD("Emerald", 0xFF5BF5A0, 90),
    COPPER("Copper", 0xFFE08A4F, 100),
    ;

    val id get() = name.lowercase()
    val icon get() = "glitter_$id"

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** The dish the slime sits in, and the room's light around it. */
enum class Tray(val title: String, val argb: Long, val gloss: Float, val price: Int) {
    VOID("Void glass", 0xFF262046, 1f, 0),
    MARBLE("Marble", 0xFFEDEAF2, 0.7f, 100),
    BUBBLEGUM("Bubblegum", 0xFFFFB3D9, 0.5f, 120),
    MINT("Mint", 0xFFA8F0D0, 0.5f, 120),
    WOOD("Wood", 0xFFB07A4A, 0.25f, 160),
    NEON("Neon", 0xFF14101F, 1f, 220),
    CANDY("Cotton candy", 0xFFD9B8FF, 0.6f, 140),
    SUNSET("Sunset", 0xFFFF9A6B, 0.5f, 160),
    ICE("Ice", 0xFFCFEFFF, 1f, 180),
    STARRY("Starry night", 0xFF0B0820, 1f, 260),
    GOLD("Gold", 0xFFE8C46A, 0.9f, 300),
    ;

    val id get() = name.lowercase()
    val icon get() = "tray_$id"

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: VOID
    }
}

/**
 * Extra play tools from the shop (the free ones, Poke to Bubbles, are always there). Names match gl.Tool. Each
 * unlocks for buying at [level].
 */
enum class Toy(val title: String, val blurb: String, val price: Int, val level: Int = 1) {
    SLAP("Slap", "A big wobbly smack. Slap it fast and it splats into droplets!", 60),
    RAKE("Rake", "Drag deep grooves through it, so satisfying", 120, 2),
    SHAKER("Sprinkle shaker", "Shake sprinkles all over it", 140, 2),
    SQUEEZE("Squeeze", "Grab it and squeeze until it squirts out droplets", 150, 2),
    STAMP("Star stamp", "Press a star into it", 160, 3),
    SCOOP("Scoop", "Scoop out a blob and drop it somewhere else", 170, 3),
    HAMMER("Squeaky hammer", "Bonk! Splashes it everywhere", 180, 3),
    CUTTER("Heart cutter", "Press a heart shape deep into it", 200, 4),
    WAND("Bubble wand", "Tap to blow a cloud of bubbles, some golden", 300, 4),
    MAGNET("Magnet", "Pull the slime up into spikes (Magnetic slime loves it)", 250, 5),
    WHISK("Whisk", "Hold to whip it round and swirl the colours", 260, 5),
    DRYER("Hair dryer", "Blow warm wind across it to make ripples", 320, 6),
    ;

    val id get() = name.lowercase()
    val icon get() = "tool_$id"
}

/** Weather in a world's backdrop, drawn by the back shader (BACK_FS's fx number). */
enum class Fx { TWINKLE, SNOW, PETALS, BUBBLES, SPARKLE, POLLEN }

/**
 * Where you play: a painted backdrop behind the dish, its weather, the sky the slime reflects ([skyTop] and
 * [skyBottom]) and its own little song. Void is the classic procedural space.
 */
enum class World(val title: String, val blurb: String, val price: Int, val fx: Fx, val skyTop: Long, val skyBottom: Long, val level: Int = 1) {
    VOID("The Void", "Deep space, glowing and calm", 0, Fx.TWINKLE, 0xFF2A1F5C, 0xFF0B0820),
    MEADOW("Sunny meadow", "Rolling hills, flowers and a breeze", 150, Fx.POLLEN, 0xFF8FD8FF, 0xFF9BE07A),
    CANDY("Candy land", "Lollipops and cotton-candy clouds", 200, Fx.SPARKLE, 0xFFFFC2E2, 0xFFC9A8FF, 2),
    OCEAN("Under the sea", "Bubbles, coral and friendly fish", 250, Fx.BUBBLES, 0xFF3DD5FF, 0xFF103A8A, 3),
    SAKURA("Sakura garden", "Cherry blossoms drifting down", 250, Fx.PETALS, 0xFFFFD9E8, 0xFFB98CFF, 3),
    SNOW("Snowy peaks", "A cosy cabin in the snow", 300, Fx.SNOW, 0xFFCFEFFF, 0xFF7FA6E8, 4),
    BEACH("Sunset beach", "Palm trees and a warm sunset", 300, Fx.SPARKLE, 0xFFFFB37A, 0xFFFF6F91, 5),
    SPACE("Planet hop", "Rings, moons and shooting stars", 350, Fx.TWINKLE, 0xFF3B0A6B, 0xFF05030F, 6),
    NEON("Neon city", "Glowing skyline at night", 400, Fx.SPARKLE, 0xFFFF2BD6, 0xFF14101F, 8),
    ;

    val id get() = name.lowercase()
    val icon get() = "world_$id"
    val backdrop get() = "bg_$id"

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: VOID
    }
}

/** The outline of the dish (see [Shapes.plateR]); the slime squishes up against its wall. */
enum class Plate(val title: String, val price: Int, val level: Int = 1) {
    ROUND("Round", 0),
    SQUARE("Square", 120),
    HEXAGON("Hexagon", 150, 2),
    FLOWER("Flower", 180, 2),
    CLOUD("Cloud", 200, 3),
    STAR("Star", 240, 4),
    HEART("Heart", 260, 4),
    ;

    val id get() = name.lowercase()
    val icon get() = "plate_$id"

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: ROUND
    }
}

/**
 * What the slime sits in. The dish is flat with a low wall (and takes the [Plate] outline); bowls and jars are deep
 * and round, so the slime slumps into them and climbs their walls (see [Shapes.vesselR]). [glass] ones are see-through.
 */
enum class Vessel(val title: String, val blurb: String, val price: Int, val level: Int, val glass: Boolean) {
    DISH("Dish", "Flat and roomy, in any dish shape", 0, 1, false),
    BOWL("Bowl", "A deep round bowl to squish into", 150, 2, false),
    JAR("Jar", "A tall glass jar: watch it from every side", 220, 3, true),
    FISHBOWL("Fishbowl", "A round glass bubble with a little mouth", 300, 5, true),
    ;

    val id get() = name.lowercase()
    val icon get() = "vessel_$id"

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: DISH
    }
}

/** The shape a slime settles into (see [Shapes]). Animals get ears and, by default, a cute face. */
enum class Mold(val title: String, val blurb: String, val price: Int, val animal: Boolean = false, val level: Int = 1) {
    DOME("Dome", "The classic squishy blob", 0),
    HEART("Heart", "A big squishy heart", 150),
    STAR("Star", "Five chubby points", 150),
    CUBE("Cube", "A soft wobbly cube", 180, level = 2),
    FLOWER("Flower", "Six round petals", 180, level = 2),
    PUDDING("Pudding", "Tall and jiggly with a flat top", 160, level = 2),
    DROP("Drop", "A droplet with a little tip", 160, level = 3),
    BEAR("Bear", "Round ears, very huggable", 220, animal = true, level = 4),
    BUNNY("Bunny", "Long floppy ears", 220, animal = true, level = 4),
    CAT("Cat", "Pointy ears, purrs when poked", 220, animal = true, level = 5),
    ;

    val id get() = name.lowercase()
    val icon get() = "mold_$id"

    companion object {
        fun of(id: String?) = entries.firstOrNull { it.id == id } ?: DOME
    }
}
