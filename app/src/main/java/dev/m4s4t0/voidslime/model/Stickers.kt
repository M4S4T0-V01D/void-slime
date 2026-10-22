package dev.m4s4t0.voidslime.model

/** Things the game counts; stickers are earned on these. */
object Stat {
    const val POKES = "pokes"
    const val STRETCHES = "stretches"
    const val SNAPS = "snaps"
    const val SQUISHES = "squishes"
    const val ROLLS = "rolls"
    const val POPS = "pops"
    const val CRUNCHES = "crunches"
    const val MADE = "made"
    const val ORDERS = "orders"
    const val PERFECT = "perfect"
    const val RUSH_BEST = "rush_best"
    const val STREAK = "streak"
    const val SECONDS = "seconds"
    const val LIGHTS_OFF = "lights_off"
    const val BOUGHT = "bought"
    const val SLAPS = "slaps"
    const val RAKES = "rakes"
    const val STAMPS = "stamps"
    /** Seconds of shaking sprinkles. */
    const val SHAKES = "shakes"
    /** Seconds of holding the magnet. */
    const val MAGNET = "magnet"
    const val WANDS = "wands"
    const val GOLD_POPS = "gold_pops"
    const val RUSHES = "rushes"
    const val QUESTS = "quests"
    /** Days with all three challenges done. */
    const val QUEST_DAYS = "quest_days"
    /** Droplets knocked off the slime (slaps, bonks, squeezes...). */
    const val SPLATS = "splats"
    /** Droplets that found their way home. */
    const val MERGES = "merges"
    /** Air blisters blown into the slime and popped. */
    const val BLISTERS = "blisters"
    const val SQUEEZES = "squeezes"
    const val SCOOPS = "scoops"
    const val BONKS = "bonks"
    const val CUTS = "cuts"
    /** Seconds of whisking. */
    const val WHISKS = "whisks"
    /** Seconds of the hair dryer. */
    const val DRYS = "drys"
    const val RACES = "races"
    const val RACE_WINS = "race_wins"
    const val VIPS = "vips"
    /** Perfect orders in a row right now, and the best run. */
    const val COMBO = "combo"
    const val COMBO_BEST = "combo_best"
}

data class Sticker(val id: String, val title: String, val desc: String, val stat: String, val goal: Int, val reward: Int) {
    val icon get() = "st_" + id.replace('-', '_')
}

val STICKERS = listOf(
    Sticker("first-poke", "Boop", "Poke a slime", Stat.POKES, 1, 5),
    Sticker("poke-500", "Poke master", "Poke 500 times", Stat.POKES, 500, 40),
    Sticker("poke-5000", "Unstoppable finger", "Poke 5,000 times", Stat.POKES, 5000, 150),
    Sticker("stretch-50", "Stretchy", "Stretch slime 50 times", Stat.STRETCHES, 50, 30),
    Sticker("snap-10", "Snap!", "Stretch until it snaps, 10 times", Stat.SNAPS, 10, 30),
    Sticker("squish-100", "Squish squad", "Squish slime 100 times", Stat.SQUISHES, 100, 40),
    Sticker("roll-30", "Pastry chef", "Roll slime flat 30 times", Stat.ROLLS, 30, 30),
    Sticker("pop-100", "Bubble popper", "Pop 100 bubbles", Stat.POPS, 100, 40),
    Sticker("pop-1000", "Pop legend", "Pop 1,000 bubbles", Stat.POPS, 1000, 150),
    Sticker("crunch-300", "Crunch time", "Crunch 300 beads", Stat.CRUNCHES, 300, 40),
    Sticker("made-1", "Mad scientist", "Make your first slime", Stat.MADE, 1, 15),
    Sticker("made-10", "Collector", "Make 10 slimes", Stat.MADE, 10, 60),
    Sticker("made-30", "Slime museum", "Make 30 slimes", Stat.MADE, 30, 200),
    Sticker("order-1", "Open for business", "Finish a shop order", Stat.ORDERS, 1, 10),
    Sticker("order-25", "Shopkeeper", "Finish 25 orders", Stat.ORDERS, 25, 120),
    Sticker("perfect-10", "Perfectionist", "Get 3 stars on 10 orders", Stat.PERFECT, 10, 80),
    Sticker("rush-25", "Quick fingers", "Pop 25 in one Bubble Rush", Stat.RUSH_BEST, 25, 40),
    Sticker("rush-60", "Lightning", "Pop 60 in one Bubble Rush", Stat.RUSH_BEST, 60, 120),
    Sticker("streak-3", "Regular", "Visit 3 days in a row", Stat.STREAK, 3, 30),
    Sticker("streak-7", "Slime week", "Visit 7 days in a row", Stat.STREAK, 7, 100),
    Sticker("time-30m", "Zen", "Play for 30 minutes", Stat.SECONDS, 1800, 60),
    Sticker("lights-off", "Night light", "Turn the lights off on a glow slime", Stat.LIGHTS_OFF, 1, 20),
    Sticker("shopper-5", "Big spender", "Buy 5 supplies", Stat.BOUGHT, 5, 40),
    Sticker("shopper-25", "Shopaholic", "Buy 25 things from the shop", Stat.BOUGHT, 25, 150),
    Sticker("slap-50", "High five", "Slap slime 50 times", Stat.SLAPS, 50, 30),
    Sticker("rake-30", "Zen garden", "Rake grooves 30 times", Stat.RAKES, 30, 40),
    Sticker("stamp-25", "Star maker", "Stamp 25 stars", Stat.STAMPS, 25, 40),
    Sticker("shake-60", "Sprinkle chef", "Shake sprinkles for a minute", Stat.SHAKES, 60, 40),
    Sticker("magnet-60", "Magnetic personality", "Hold the magnet for a minute", Stat.MAGNET, 60, 50),
    Sticker("wand-30", "Bubble wizard", "Wave the bubble wand 30 times", Stat.WANDS, 30, 40),
    Sticker("gold-25", "Gold digger", "Pop 25 golden bubbles", Stat.GOLD_POPS, 25, 60),
    Sticker("quest-10", "Go-getter", "Finish 10 daily challenges", Stat.QUESTS, 10, 50),
    Sticker("quest-days-5", "Challenge champ", "Finish all three challenges on 5 days", Stat.QUEST_DAYS, 5, 120),
    Sticker("splat-50", "Splatter", "Knock 50 droplets off a slime", Stat.SPLATS, 50, 40),
    Sticker("merge-100", "Back together", "Watch 100 droplets come home", Stat.MERGES, 100, 50),
    Sticker("blister-25", "Big bubble", "Blow up and pop 25 slime bubbles", Stat.BLISTERS, 25, 40),
    Sticker("squeeze-30", "Squeeze me", "Squeeze slime 30 times", Stat.SQUEEZES, 30, 40),
    Sticker("scoop-25", "Ice cream man", "Scoop 25 blobs", Stat.SCOOPS, 25, 40),
    Sticker("bonk-50", "Bonk!", "Bonk slime with the hammer 50 times", Stat.BONKS, 50, 40),
    Sticker("cut-20", "Heart cutter", "Cut 20 hearts", Stat.CUTS, 20, 40),
    Sticker("whisk-60", "Whisk it", "Whisk for a minute", Stat.WHISKS, 60, 50),
    Sticker("dry-60", "Blow dry", "Blow the hair dryer for a minute", Stat.DRYS, 60, 50),
    Sticker("race-1", "Off the line", "Finish a slime race", Stat.RACES, 1, 15),
    Sticker("race-win-10", "Speed slime", "Win 10 slime races", Stat.RACE_WINS, 10, 120),
    Sticker("vip-5", "Red carpet", "Make 5 VIPs perfectly happy", Stat.VIPS, 5, 100),
    Sticker("combo-5", "On a roll", "Get 5 perfect orders in a row", Stat.COMBO_BEST, 5, 80),
)

/** Levels: each one needs a little more XP than the last. */
object Levels {
    fun need(level: Int) = 60 + 40 * (level - 1)

    /** (level, xp into this level, xp this level needs) for a running XP total. */
    fun of(xp: Long): Triple<Int, Int, Int> {
        var lvl = 1
        var left = xp
        while (left >= need(lvl)) {
            left -= need(lvl)
            lvl++
        }
        return Triple(lvl, left.toInt(), need(lvl))
    }
}
