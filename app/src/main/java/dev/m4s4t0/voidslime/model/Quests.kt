package dev.m4s4t0.voidslime.model

import org.json.JSONObject
import kotlin.random.Random

/** A kind of daily challenge: raise [stat] by one of [goals]. [needs] is an unlock key it needs (a toy). */
class QuestKind(val stat: String, val icon: String, val goals: List<Int>, val perUnit: Float, val needs: String? = null, val text: (Int) -> String)

/** Today's challenge: [goal] more of [stat] than the [start] it had when the day began. */
data class Quest(val stat: String, val goal: Int, val start: Long, val reward: Int, val done: Boolean = false) {
    val kind get() = Quests.kind(stat)
    val text get() = kind?.text?.invoke(goal) ?: stat
    val icon get() = kind?.icon ?: "target"

    fun toJson(): JSONObject = JSONObject().put("stat", stat).put("goal", goal).put("start", start).put("reward", reward).put("done", done)

    companion object {
        fun fromJson(j: JSONObject) = Quest(j.getString("stat"), j.getInt("goal"), j.getLong("start"), j.getInt("reward"), j.optBoolean("done"))
    }
}

object Quests {
    /** Finish all three in a day for this on top. */
    const val ALL_BONUS = 40

    private fun times(n: Int) = if (n == 1) "once" else "$n times"
    private fun s(n: Int, word: String) = if (n == 1) "$n $word" else "$n ${word}s"

    val KINDS = listOf(
        QuestKind(Stat.POKES, "tool_poke", listOf(80, 150, 250), 0.08f) { "Poke the slime ${times(it)}" },
        QuestKind(Stat.STRETCHES, "tool_stretch", listOf(10, 20, 35), 0.8f) { "Stretch it ${times(it)}" },
        QuestKind(Stat.SNAPS, "pic_snap", listOf(3, 5, 8), 3f) { "Stretch until it snaps, ${times(it)}" },
        QuestKind(Stat.SQUISHES, "tool_squish", listOf(10, 20, 30), 0.8f) { "Squish it flat ${times(it)}" },
        QuestKind(Stat.ROLLS, "tool_roll", listOf(5, 10, 15), 1.5f) { "Roll it out ${times(it)}" },
        QuestKind(Stat.POPS, "bubbles", listOf(20, 40, 70), 0.4f) { "Pop ${s(it, "bubble")}" },
        QuestKind(Stat.CRUNCHES, "pic_pretzel", listOf(30, 60), 0.4f) { "Crunch ${s(it, "bead")}" },
        QuestKind(Stat.MADE, "flask", listOf(1, 2, 3), 10f) { "Make ${s(it, "new slime")}" },
        QuestKind(Stat.ORDERS, "bell", listOf(1, 2, 3), 9f) { "Finish ${s(it, "shop order")}" },
        QuestKind(Stat.RUSHES, "timer", listOf(1, 2), 10f) { "Play Bubble rush ${times(it)}" },
        QuestKind(Stat.SLAPS, "tool_slap", listOf(15, 30), 0.8f, "toy:slap") { "Slap it ${times(it)}" },
        QuestKind(Stat.RAKES, "tool_rake", listOf(5, 10), 2f, "toy:rake") { "Rake ${s(it, "groove")} into it" },
        QuestKind(Stat.STAMPS, "tool_stamp", listOf(5, 10), 2f, "toy:stamp") { "Stamp ${s(it, "star")}" },
        QuestKind(Stat.SHAKES, "tool_shaker", listOf(15, 30), 0.8f, "toy:shaker") { "Shake sprinkles for $it seconds" },
        QuestKind(Stat.MAGNET, "tool_magnet", listOf(15, 30), 0.8f, "toy:magnet") { "Hold the magnet over it for $it seconds" },
        QuestKind(Stat.WANDS, "tool_wand", listOf(5, 10), 2f, "toy:wand") { "Wave the bubble wand ${times(it)}" },
        QuestKind(Stat.GOLD_POPS, "coin", listOf(2, 4), 6f, "toy:wand") { "Pop ${s(it, "golden bubble")}" },
        QuestKind(Stat.BLISTERS, "pic_blister", listOf(3, 6, 10), 2f) { "Blow up and pop ${s(it, "slime bubble")}" },
        QuestKind(Stat.SPLATS, "pic_droplet", listOf(10, 20), 0.8f, "toy:slap") { "Knock off ${s(it, "droplet")}" },
        QuestKind(Stat.SQUEEZES, "tool_squeeze", listOf(5, 10), 2f, "toy:squeeze") { "Squeeze it ${times(it)}" },
        QuestKind(Stat.SCOOPS, "tool_scoop", listOf(5, 10), 2f, "toy:scoop") { "Scoop ${s(it, "blob")}" },
        QuestKind(Stat.BONKS, "tool_hammer", listOf(10, 20), 1f, "toy:hammer") { "Bonk it ${times(it)}" },
        QuestKind(Stat.CUTS, "tool_cutter", listOf(4, 8), 2.5f, "toy:cutter") { "Cut ${s(it, "heart")}" },
        QuestKind(Stat.WHISKS, "tool_whisk", listOf(15, 30), 0.8f, "toy:whisk") { "Whisk it for $it seconds" },
        QuestKind(Stat.DRYS, "tool_dryer", listOf(15, 30), 0.8f, "toy:dryer") { "Blow dry it for $it seconds" },
        QuestKind(Stat.RACES, "flag", listOf(1, 2, 3), 8f, RACE_KEY) { "Finish ${s(it, "slime race")}" },
        QuestKind(Stat.RACE_WINS, "trophy", listOf(1, 2), 14f, RACE_KEY) { "Win ${s(it, "slime race")}" },
    )

    fun kind(stat: String) = KINDS.firstOrNull { it.stat == stat }

    /** Three different challenges for a new day, using only toys the player has. */
    fun daily(rnd: Random, unlocked: (String) -> Boolean, level: Int, stats: Map<String, Long>): List<Quest> =
        KINDS.filter { k -> k.needs == null || unlocked(k.needs) }.shuffled(rnd).take(3).map { k ->
            val goal = k.goals[rnd.nextInt(k.goals.size)]
            Quest(k.stat, goal, stats[k.stat] ?: 0, (10 + goal * k.perUnit + level).toInt().coerceIn(10, 60))
        }

    fun progress(q: Quest, stats: Map<String, Long>): Int =
        if (q.done) q.goal else ((stats[q.stat] ?: 0) - q.start).coerceIn(0, q.goal.toLong()).toInt()
}
