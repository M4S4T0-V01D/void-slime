package dev.m4s4t0.voidslime

import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.sim.Icosphere
import dev.m4s4t0.voidslime.sim.SoftBody
import dev.m4s4t0.voidslime.sim.SphereCollider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimTest {
    @Test fun icosphereCounts() {
        val s = Icosphere(4)
        assertEquals(2562, s.verts.size / 3)
        assertEquals(5120, s.tris.size / 3)
        assertEquals(7680, s.edges().size / 2)
    }

    private fun finite(b: SoftBody) = b.pos.all { it.isFinite() }

    @Test fun everyBaseSettlesAndStaysWhole() {
        val out = FloatArray(2)
        for (base in Base.entries) {
            val b = SoftBody()
            b.setMaterial(base.mat, 1f)
            repeat(240) { b.step(1 / 60f) }
            assertTrue("$base went NaN", finite(b))
            b.extent(out)
            assertTrue("$base sank or flew: top ${out[0]}", out[0] in 0.15f..3f)
            assertTrue("$base spread too far: ${out[1]}", out[1] < 3f)
            assertTrue("$base lost its volume: ${b.volume}", b.volume > 0f)
            assertTrue("$base fell through the floor", b.pos.filterIndexed { i, _ -> i % 3 == 1 }.min() > -0.05f)
        }
    }

    /** Mean (1 - cos) between the normals at a triangle's corners, over the upper surface: 0 is perfectly smooth. */
    private fun roughness(b: SoftBody): Double {
        var sum = 0.0
        var count = 0
        val nm = b.normals
        for (t in 0 until b.tris.size / 3) {
            val i = b.tris[t * 3] * 3; val j = b.tris[t * 3 + 1] * 3; val k = b.tris[t * 3 + 2] * 3
            if (nm[i + 1] < 0.3f) continue
            for ((p, q) in listOf(i to j, j to k, k to i)) {
                sum += 1.0 - (nm[p] * nm[q] + nm[p + 1] * nm[q + 1] + nm[p + 2] * nm[q + 2])
                count++
            }
        }
        return sum / count
    }

    @Test fun unstirredGlueIsASmoothPuddle() {
        val rough = listOf(0f, 0.5f, 1f).associateWith { form ->
            val b = SoftBody()
            b.setMaterial(Base.GLOSSY.mat, form)
            b.reset()
            repeat(300) { b.step(1 / 60f) }
            roughness(b).also { println("form $form roughness $it") }
        }
        for ((form, r) in rough) assertTrue("form $form is crumpled: $r", r < 0.01)
    }

    @Test fun pokingAndReleasingDoesNotExplode() {
        val b = SoftBody()
        b.setMaterial(Base.JELLY.mat, 1f)
        repeat(120) { b.step(1 / 60f) }
        val finger = SphereCollider(0f, 2f, 0f, 0.17f)
        b.colliders += finger
        repeat(120) { finger.y = 2f - it / 60f; b.step(1 / 60f) }
        b.colliders.clear()
        repeat(240) { b.step(1 / 60f) }
        assertTrue(finite(b))
        val out = FloatArray(2)
        b.extent(out)
        assertTrue("top ${out[0]}", out[0] in 0.15f..3f)
    }

    @Test fun holdingTheMagnetPullsButDoesNotTear() {
        val out = FloatArray(2)
        for (base in listOf(Base.GLOSSY, Base.MAGNETIC, Base.JELLY)) {
            val b = SoftBody()
            b.setMaterial(base.mat, 1f)
            repeat(120) { b.step(1 / 60f) }
            b.extent(out)
            val rest = out[0]
            repeat(180) { b.attract(0f, rest + 0.5f, 0f, 1.3f, 48f / 60f); b.step(1 / 60f) }
            assertTrue("$base went NaN under the magnet", finite(b))
            b.extent(out)
            assertTrue("$base didn't rise to the magnet", out[0] > rest + 0.05f)
            assertTrue("$base flew off: ${out[0]}", out[0] < rest + 1.5f)
            repeat(300) { b.step(1 / 60f) }
            assertTrue("$base went NaN after the magnet", finite(b))
            assertTrue("$base lost its volume", b.volume > 0f)
        }
    }
}
