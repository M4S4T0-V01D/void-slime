package dev.m4s4t0.voidslime

import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.COLORS
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Levels
import dev.m4s4t0.voidslime.model.Order
import dev.m4s4t0.voidslime.model.Orders
import dev.m4s4t0.voidslime.model.Quest
import dev.m4s4t0.voidslime.model.Quests
import dev.m4s4t0.voidslime.model.Shape
import dev.m4s4t0.voidslime.model.Toy
import dev.m4s4t0.voidslime.model.Tray
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.Mold
import dev.m4s4t0.voidslime.model.Plate
import dev.m4s4t0.voidslime.model.STICKERS
import dev.m4s4t0.voidslime.model.Shapes
import dev.m4s4t0.voidslime.model.World
import dev.m4s4t0.voidslime.model.Vessel
import dev.m4s4t0.voidslime.model.OrderKind
import dev.m4s4t0.voidslime.model.Cup
import dev.m4s4t0.voidslime.model.RACE_LEVEL
import dev.m4s4t0.voidslime.model.levelOf
import dev.m4s4t0.voidslime.model.iconOf
import dev.m4s4t0.voidslime.audio.Song
import dev.m4s4t0.voidslime.ui.Art
import dev.m4s4t0.voidslime.model.key
import dev.m4s4t0.voidslime.model.label
import dev.m4s4t0.voidslime.model.priceOf
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ModelTest {
    private val red = COLORS[0].id
    private val other = COLORS[1].id
    private val addIn = AddIn.entries.first()

    @Test fun recipeRoundTripsThroughJson() {
        val r = Recipe("s1", "Mint Cloud", Base.CLOUD, listOf(red, other), mapOf(addIn to 2), Glitter.entries.last(), 123L, 7)
        assertEquals(r, Recipe.fromJson(JSONObject(r.toJson().toString())))
    }

    @Test fun recipeFromJsonDropsUnknownsAndClampsLevels() {
        val j = JSONObject("""{"id":"x","base":"nope","colors":["zzz"],"addIns":{"${addIn.id}":99,"bogus":1}}""")
        val r = Recipe.fromJson(j)
        assertEquals(listOf(COLORS.first().id), r.colors)
        assertEquals(setOf(addIn), r.addIns.keys)
        assertTrue(r.addIns.getValue(addIn) <= 2)
    }

    @Test fun perfectRecipeEarnsThreeStarsAndFullPay() {
        val o = Order("o", "Mia", "mia", Base.JELLY, red, addIn, true, 50, "")
        val r = Recipe("s", "x", Base.JELLY, listOf(red), mapOf(addIn to 0), Glitter.entries.last())
        assertEquals(1f, Orders.match(o, r), 1e-6f)
        assertEquals(3, Orders.stars(o, r))
        assertEquals(50, Orders.payout(o, 3))
    }

    @Test fun partialMatchesEarnFewerStars() {
        val o = Order("o", "Mia", "mia", Base.JELLY, red, null, false, 50, "")
        val second = Recipe("s", "x", Base.JELLY, listOf(other, red))
        assertEquals(0.85f, Orders.match(o, second), 1e-6f)
        assertEquals(2, Orders.stars(o, second))
        val wrong = Recipe("s", "x", Base.GLOSSY, listOf(other))
        assertEquals(1, Orders.stars(o, wrong))
        assertTrue(Orders.payout(o, 1) >= 3)
    }

    @Test fun generatedOrdersOnlyAskForUnlockedThings() {
        val rnd = Random(4)
        val free: (String) -> Boolean = { priceOf(it) == 0 }
        repeat(500) {
            val o = Orders.generate(rnd, free, level = it % 12)
            o.base?.let { b -> assertTrue(free(key(b))) }
            o.color?.let { c -> assertTrue(free("color:$c")) }
            o.addIn?.let { a -> assertTrue(free(key(a))) }
            assertTrue(o.reward > 0)
            assertTrue(o.line.isNotBlank())
        }
    }

    @Test fun specialOrdersWaitForTheirLevel() {
        val rnd = Random(9)
        val all: (String) -> Boolean = { true }
        repeat(300) {
            val lvl = 1 + it % 10
            val o = Orders.generate(rnd, all, level = lvl, now = 1000L)
            assertTrue(o.kind.level <= lvl)
            if (o.kind == OrderKind.RUSH) assertTrue(o.expires > 1000L)
            if (o.kind != OrderKind.REGULAR) assertEquals(o, Order.fromJson(JSONObject(o.toJson().toString())))
        }
    }

    @Test fun lockedThingsHaveALevel() {
        assertEquals(1, levelOf(key(Vessel.DISH)))
        assertTrue(Vessel.entries.all { levelOf(key(it)) == it.level })
        assertTrue(Toy.entries.all { levelOf(key(it)) == it.level })
        assertTrue(Cup.entries.all { it.level >= RACE_LEVEL })
    }

    @Test fun everyItemHasALabel() {
        for (b in Base.entries) assertTrue(label(key(b)).isNotBlank())
        for (a in AddIn.entries) assertTrue(label(key(a)).isNotBlank())
    }

    @Test fun levelsAccumulate() {
        assertEquals(Triple(1, 0, 60), Levels.of(0))
        assertEquals(Triple(1, 59, 60), Levels.of(59))
        assertEquals(Triple(2, 0, 100), Levels.of(60))
        assertEquals(3, Levels.of(160).first)
    }

    @Test fun everyShopThingHasAPriceALabelAndAUniqueKey() {
        val keys = Base.entries.map(::key) + COLORS.map(::key) + AddIn.entries.map(::key) + Glitter.entries.map(::key) + Tray.entries.map(::key) + Toy.entries.map(::key) +
            World.entries.map(::key) + Plate.entries.map(::key) + Mold.entries.map(::key)
        assertEquals(keys.size, keys.toSet().size)
        for (k in keys) assertTrue("$k has no label", label(k) != k)
        for (t in Toy.entries) assertTrue("${t.id} should cost something", priceOf(key(t)) > 0)
        // The particles' spark keeps its sprite number; add-ins never use it.
        assertEquals(9, Shape.SPARK.ordinal)
        assertTrue(AddIn.entries.none { it.shape == Shape.SPARK })
    }

    @Test fun dailyChallengesAreDifferentAndOnlyUseOwnedToys() {
        repeat(50) { seed ->
            val qs = Quests.daily(Random(seed), { it == "toy:slap" }, 3, emptyMap())
            assertEquals(3, qs.size)
            assertEquals(3, qs.map { it.stat }.toSet().size)
            for (q in qs) {
                val needs = Quests.kind(q.stat)!!.needs
                assertTrue("${q.stat} needs $needs", needs == null || needs == "toy:slap")
                assertTrue(q.reward in 10..60)
            }
        }
    }

    @Test fun challengeProgressCountsFromTheStartOfTheDay() {
        val q = Quest("pokes", 80, start = 1000, reward = 20)
        assertEquals(0, Quests.progress(q, mapOf("pokes" to 1000L)))
        assertEquals(30, Quests.progress(q, mapOf("pokes" to 1030L)))
        assertEquals(80, Quests.progress(q, mapOf("pokes" to 5000L)))
        assertEquals(q, Quest.fromJson(JSONObject(q.toJson().toString())))
    }

    @Test fun theDefaultsAreFreeAndEverythingElseCosts() {
        assertEquals(0, priceOf(key(World.VOID)))
        assertEquals(0, priceOf(key(Plate.ROUND)))
        assertEquals(0, priceOf(key(Mold.DOME)))
        for (w in World.entries.drop(1)) assertTrue(priceOf(key(w)) > 0)
        for (p in Plate.entries.drop(1)) assertTrue(priceOf(key(p)) > 0)
        for (m in Mold.entries.drop(1)) assertTrue(priceOf(key(m)) > 0)
    }

    @Test fun everyPictureTheGameAsksForExists() {
        val keys = Base.entries.map(::key) + AddIn.entries.map(::key) + Glitter.entries.map(::key) + Tray.entries.map(::key) + Toy.entries.map(::key) +
            World.entries.map(::key) + Plate.entries.map(::key) + Mold.entries.map(::key) + Vessel.entries.map(::key)
        val names = keys.mapNotNull(::iconOf) + STICKERS.map { it.icon } + Quests.KINDS.map { it.icon } + World.entries.map { it.backdrop } + Vessel.entries.map { "vessel_" + it.id } + listOf("race", "flag") +
            listOf("Mia", "Leo", "Nobody").map { "cust_" + Orders.faceOf(it) }
        for (n in names) assertTrue("missing art: $n", Art.ids.containsKey(n))
        assertTrue(COLORS.all { iconOf(key(it)) == null })
    }

    @Test fun everyWorldHasASong() {
        for (w in World.entries) Song.valueOf(w.name)
    }

    @Test fun oldSavedOrdersStillLoad() {
        val j = JSONObject("""{"id":"o1","customer":"Leo","emoji":"x","glitter":true,"reward":30,"line":"Hi"}""")
        val o = Order.fromJson(j)
        assertEquals(Orders.faceOf("Leo"), o.face)
        assertEquals(o, Order.fromJson(JSONObject(o.toJson().toString())))
    }

    @Test fun moldAndFaceRoundTrip() {
        val r = Recipe("s1", "Bun", Base.JELLY, listOf(red), mold = Mold.BUNNY, face = true)
        assertEquals(r, Recipe.fromJson(JSONObject(r.toJson().toString())))
        assertEquals(Mold.DOME, Recipe.fromJson(JSONObject("""{"id":"x","base":"jelly","colors":[]}""")).mold)
    }

    @Test fun dishOutlinesStayDishSized() {
        for (p in Plate.entries) {
            val rs = (0 until 360).map { Shapes.plateR(p, it / 360f * 6.2832f) }
            assertTrue("$p too small: ${rs.min()}", rs.min() > 0.5f)
            assertTrue("$p too big: ${rs.max()}", rs.max() < 1.3f)
            assertTrue(Shapes.plateMin(p) <= rs.min() + 1e-4f)
        }
        for (a in 0 until 36) assertEquals(1f, Shapes.plateR(Plate.ROUND, a * 0.17f), 1e-6f)
    }

    @Test fun anUnformedMoldIsJustTheDome() {
        val out = FloatArray(3)
        val dome = FloatArray(3)
        for (m in Mold.entries) for ((x, y, z) in listOf(Triple(0.6f, 0.8f, 0f), Triple(0f, 0.6f, -0.8f), Triple(0.8f, -0.6f, 0f))) {
            Shapes.mold(m, x, y, z, 1.2f, 0.7f, 0.1f, 0f, out)
            Shapes.mold(Mold.DOME, x, y, z, 1.2f, 0.7f, 0.1f, 0f, dome)
            for (i in 0..2) assertEquals("$m", dome[i], out[i], 1e-5f)
        }
    }

    @Test fun theHeartPointsAtTheCamera() {
        val front = Shapes.moldR(Mold.HEART, Math.PI.toFloat() / 2f - Shapes.FRONT_YAW)
        val back = Shapes.moldR(Mold.HEART, -Math.PI.toFloat() / 2f - Shapes.FRONT_YAW)
        assertTrue("front $front back $back", front > back)
        // Bunny ears stick up well above a dome.
        fun top(m: Mold): Float {
            val out = FloatArray(3)
            var best = 0f
            for (i in 0..40) for (j in 0..80) {
                val th = i / 40f * 1.5708f; val ph = j / 80f * 6.2832f
                Shapes.mold(m, kotlin.math.cos(th) * kotlin.math.cos(ph), kotlin.math.sin(th), kotlin.math.cos(th) * kotlin.math.sin(ph), 1f, 1f, 0.1f, 1f, out)
                best = maxOf(best, out[1])
            }
            return best
        }
        assertTrue(top(Mold.BUNNY) > top(Mold.DOME) * 1.3f)
    }
}
