package dev.m4s4t0.voidslime.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The outlines of dishes and slime molds, as polar radius functions. Angles are world angles, atan2(z, x); the
 * shapes face [FRONT_YAW], the way the camera starts looking from, so a heart points at you. Plate outlines match
 * art/items.py's plate_r, so the shop icons look like the dish you get.
 */
object Shapes {
    const val FRONT_YAW = 0.5f
    private const val TAU = (2 * PI).toFloat()

    /** World angle to the shape's own angle (+z, PI/2, is its front). */
    fun local(a: Float) = a + FRONT_YAW

    /** The dish wall's radius at world angle [a], relative to a round dish. */
    fun plateR(p: Plate, a: Float): Float {
        val l = local(a)
        return when (p) {
            Plate.ROUND -> 1f
            Plate.SQUARE -> 0.96f * (abs(cos(l)).pow(5f) + abs(sin(l)).pow(5f)).pow(-0.2f)
            Plate.HEXAGON -> {
                val k = mod(l + PI.toFloat() / 6f, PI.toFloat() / 3f) - PI.toFloat() / 6f
                1.06f * cos(PI.toFloat() / 6f) / cos(k) * 0.97f + 0.03f
            }
            Plate.FLOWER -> 1f + 0.11f * cos(6f * l)
            Plate.CLOUD -> 0.94f + 0.12f * abs(cos(3.5f * l + PI.toFloat() / 2f)).pow(0.6f)
            Plate.STAR -> 1f + 0.17f * cos(5f * (l + PI.toFloat() / 2f))
            Plate.HEART -> 0.5f * heart(l) * 1.18f + 0.5f * 0.96f
        }
    }

    /** The narrowest the dish gets (a quick test before the exact wall check). */
    fun plateMin(p: Plate) = minR.getOrPut(p) { (0 until 360).minOf { plateR(p, it / 360f * TAU) } }
    private val minR = HashMap<Plate, Float>()

    /** The widest the dish gets (so the camera can fit it). */
    fun plateMax(p: Plate) = maxR.getOrPut(p) { (0 until 360).maxOf { plateR(p, it / 360f * TAU) } }
    private val maxR = HashMap<Plate, Float>()

    /** How far a mold's footprint reaches at world angle [a], relative to a dome. */
    fun moldR(m: Mold, a: Float): Float {
        val l = local(a)
        return when (m) {
            Mold.HEART -> 0.3f + 0.8f * heart(l)
            Mold.STAR -> 0.9f + 0.3f * cos(5f * (l + PI.toFloat() / 2f))
            Mold.CUBE -> 0.86f * (abs(cos(l + PI.toFloat() / 4f)).pow(4f) + abs(sin(l + PI.toFloat() / 4f)).pow(4f)).pow(-0.25f)
            Mold.FLOWER -> 0.94f + 0.16f * cos(6f * l)
            Mold.PUDDING -> 0.82f
            Mold.DROP -> 0.86f
            Mold.BEAR, Mold.BUNNY, Mold.CAT -> 0.95f
            Mold.DOME -> 1f
        }
    }

    /**
     * Shapes one point of the unit sphere ([x], [y], [z]) into the mold, in the slime's rest frame: [radius] and
     * [height] are the dome's size, [f] how formed it is (0 a runny puddle, which has no shape yet). Writes to [out].
     */
    fun mold(m: Mold, x: Float, y: Float, z: Float, radius: Float, height: Float, under: Float, f: Float, out: FloatArray) {
        val a = atan2(z, x)
        val rho = hypot(x, z)
        val slump = 1f + 0.06f * (1f - y) * f
        val fr = 1f + (moldR(m, a) - 1f) * f
        var h = height
        var yy = y
        var sr = rho
        when (m) {
            Mold.CUBE -> if (y > 0f) { yy = y.pow(lerp(1f, 0.35f, f)); sr = rho.pow(lerp(1f, 0.45f, f)); h *= 1f + 0.25f * f }
            Mold.PUDDING -> if (y > 0f) { yy = y.pow(lerp(1f, 0.5f, f)); sr = rho.pow(lerp(1f, 0.6f, f)) * (1f - 0.16f * y * f); h *= 1f + 0.55f * f }
            Mold.DROP -> if (y > 0f) { sr = rho * (1f - 0.35f * y * y * f); yy = y * (1f + 0.45f * y.pow(6f) * f); h *= 1f + 0.4f * f }
            Mold.STAR, Mold.HEART, Mold.FLOWER -> h *= 1f - 0.1f * f
            Mold.BEAR, Mold.BUNNY, Mold.CAT -> h *= 1f + 0.18f * f
            Mold.DOME -> Unit
        }
        val s = if (rho > 1e-6f) sr / rho else 0f
        var px = x * s * radius * slump * fr
        var py = if (y >= 0f) yy * h else y * under
        var pz = z * s * radius * slump * fr
        if (m.animal && f > 0f && y > 0f) {
            for (side in -1..1 step 2) {
                val e = ears(m, side)
                val d = x * e[0] + y * e[1] + z * e[2]
                val w = ((d - e[3]) / (1f - e[3])).coerceIn(0f, 1f)
                if (w <= 0f) continue
                val prof = when (m) { Mold.CAT -> w * w; Mold.BUNNY -> w.pow(0.8f); else -> sqrt(w) }
                val push = e[4] * prof * f
                px += e[0] * push * radius; py += e[1] * push * height * 1.6f; pz += e[2] * push * radius
            }
        }
        out[0] = px; out[1] = py; out[2] = pz
    }

    /** An ear on [side]: direction on the unit sphere (rest frame, rotated to face the front), cone cos, length. */
    private fun ears(m: Mold, side: Int): FloatArray = earCache.getOrPut(m.ordinal * 2 + (side + 1) / 2) {
        val (ex, ey, cone, len) = when (m) {
            Mold.BUNNY -> listOf(0.22f, 0.97f, 0.975f, 0.9f)
            Mold.CAT -> listOf(0.5f, 0.85f, 0.965f, 0.55f)
            else -> listOf(0.58f, 0.8f, 0.97f, 0.36f)
        }
        // Slightly back from the face, then turned to the front.
        val lx = ex * side; val lz = -0.12f
        val l = sqrt(lx * lx + ey * ey + lz * lz)
        val (wx, wz) = toWorld(lx / l, lz / l)
        floatArrayOf(wx, ey / l, wz, cone, len)
    }
    private val earCache = HashMap<Int, FloatArray>()

    /** Where a face feature sits on the unit sphere: eyes and cheeks on the front, turned to the world. */
    fun faceSpot(dx: Float, dy: Float): FloatArray {
        val z = sqrt(max(0f, 1f - dx * dx - dy * dy))
        val (wx, wz) = toWorld(dx, z)
        return floatArrayOf(wx, dy, wz)
    }

    /** Rest-frame x, z (front is +z) to world x, z. */
    private fun toWorld(x: Float, z: Float): Pair<Float, Float> {
        val c = cos(FRONT_YAW); val s = sin(FRONT_YAW)
        return Pair(x * c + z * s, -x * s + z * c)
    }

    // The heart's polar radius (point at the front, +z), normalised to 0..1, from the classic heart curve.
    private val heartTable: FloatArray by lazy {
        val pts = Array(720) { i ->
            val t = i / 720f * TAU
            val x = 16f * sin(t).pow(3)
            val y = 13f * cos(t) - 5f * cos(2 * t) - 2f * cos(3 * t) - cos(4 * t)
            floatArrayOf(x / 16.5f, -(y - 2.5f) / 16.5f)
        }
        val angs = FloatArray(720) { atan2(pts[it][1], pts[it][0]) }
        val out = FloatArray(360)
        for (k in 0 until 360) {
            val th = k / 360f * TAU
            var bd = 9f; var best = 1f
            for (i in pts.indices) {
                val d = abs(mod(angs[i] - th + PI.toFloat(), TAU) - PI.toFloat())
                if (d < bd) { bd = d; best = hypot(pts[i][0], pts[i][1]) }
            }
            out[k] = best
        }
        val mx = out.max()
        for (k in 0 until 360) out[k] /= mx
        out
    }

    private fun heart(l: Float) = heartTable[((mod(l, TAU) / TAU) * 360f).toInt().coerceIn(0, 359)]

    private fun mod(a: Float, b: Float) = ((a % b) + b) % b
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
