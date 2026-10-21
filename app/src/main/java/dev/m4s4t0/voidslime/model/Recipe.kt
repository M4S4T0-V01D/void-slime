package dev.m4s4t0.voidslime.model

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/** One slime: what it's made of. Saved in a jar on the shelf. */
data class Recipe(
    val id: String,
    val name: String,
    val base: Base,
    /** One to three colour ids (COLORS), swirled together. */
    val colors: List<String>,
    /** Add-in → how many (ADD_IN_LEVELS index 0..2). */
    val addIns: Map<AddIn, Int> = emptyMap(),
    val glitter: Glitter = Glitter.NONE,
    val created: Long = 0,
    /** Times it's been played with (sorting the shelf by favourites). */
    val plays: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("base", base.id)
        put("colors", JSONArray(colors))
        put("addIns", JSONObject().apply { for ((a, n) in addIns) put(a.id, n) })
        put("glitter", glitter.id)
        put("created", created)
        put("plays", plays)
    }

    val crunchy get() = base.mat.crunchy || addIns.keys.any { it.crunchy }

    companion object {
        fun fromJson(j: JSONObject): Recipe {
            val cs = j.optJSONArray("colors")
            val colors = (0 until (cs?.length() ?: 0)).map { cs!!.getString(it) }.filter { id -> COLORS.any { it.id == id } }.take(3)
            val ai = j.optJSONObject("addIns")
            val addIns = buildMap {
                ai?.keys()?.forEach { k -> AddIn.of(k)?.let { put(it, ai.getInt(k).coerceIn(0, ADD_IN_LEVELS.lastIndex)) } }
            }
            return Recipe(
                id = j.getString("id"),
                name = j.optString("name", "Slime"),
                base = Base.of(j.optString("base")),
                colors = colors.ifEmpty { listOf(COLORS.first().id) },
                addIns = addIns,
                glitter = Glitter.of(j.optString("glitter")),
                created = j.optLong("created"),
                plays = j.optInt("plays"),
            )
        }

        fun newId(rnd: Random = Random.Default) = "s" + System.currentTimeMillis().toString(36) + rnd.nextInt(1 shl 20).toString(36)
    }
}

/** Cute names for a new slime, built from what's in it ("Midnight Marshmallow", "Sparkly Mint Cloud"). */
object Names {
    private val treats = listOf("Marshmallow", "Milkshake", "Jelly Bean", "Cupcake", "Galaxy", "Mochi", "Sorbet", "Soda", "Dream", "Pudding", "Taffy", "Nebula", "Lagoon", "Comet", "Frosting", "Parfait")
    private val moods = listOf("Dreamy", "Cosmic", "Sleepy", "Fizzy", "Sparkly", "Squishy", "Velvet", "Sunny", "Moonlit", "Bubbly", "Starry", "Silky")

    fun suggest(r: Recipe, rnd: Random = Random.Default): String {
        val color = colorOf(r.colors.first()).title.substringBefore(' ').let { if (it == "Void") "Void" else it }
        val baseWord = when (r.base) {
            Base.CLOUD -> "Cloud"
            Base.BUTTER -> "Butter"
            Base.JELLY -> "Jelly"
            Base.ICEE -> "Icee"
            Base.CHROME -> "Chrome"
            Base.HOLO -> "Holo"
            Base.GLOW -> "Glow"
            else -> treats[rnd.nextInt(treats.size)]
        }
        return when (rnd.nextInt(3)) {
            0 -> "$color $baseWord"
            1 -> "${moods[rnd.nextInt(moods.size)]} $color $baseWord"
            else -> "${moods[rnd.nextInt(moods.size)]} $baseWord"
        }
    }
}

/** The jars every new shelf starts with. */
val STARTER_JARS = listOf(
    Recipe("starter-galaxy", "Void Galaxy", Base.CLEAR, listOf("void", "cyan"), mapOf(AddIn.STARS to 1), Glitter.SILVER),
    Recipe("starter-strawberry", "Strawberry Milk", Base.BUTTER, listOf("bubblegum", "snow"), mapOf(AddIn.SPRINKLES to 1)),
    Recipe("starter-mint", "Mint Crunch", Base.GLOSSY, listOf("mint"), mapOf(AddIn.FOAM to 2)),
)
