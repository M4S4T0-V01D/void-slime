package dev.m4s4t0.voidslime

import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.COLORS
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Levels
import dev.m4s4t0.voidslime.model.Order
import dev.m4s4t0.voidslime.model.Orders
import dev.m4s4t0.voidslime.model.Recipe
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
        val o = Order("o", "Mia", "👧", Base.JELLY, red, addIn, true, 50, "")
        val r = Recipe("s", "x", Base.JELLY, listOf(red), mapOf(addIn to 0), Glitter.entries.last())
        assertEquals(1f, Orders.match(o, r), 1e-6f)
        assertEquals(3, Orders.stars(o, r))
        assertEquals(50, Orders.payout(o, 3))
    }

    @Test fun partialMatchesEarnFewerStars() {
        val o = Order("o", "Mia", "👧", Base.JELLY, red, null, false, 50, "")
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
}
