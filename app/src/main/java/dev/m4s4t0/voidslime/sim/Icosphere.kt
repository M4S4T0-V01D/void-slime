package dev.m4s4t0.voidslime.sim

import kotlin.math.sqrt

/** A unit sphere of near-even triangles: an icosahedron with each face split [subdiv] times. */
class Icosphere(subdiv: Int) {
    val verts: FloatArray
    val tris: IntArray

    init {
        val t = ((1.0 + sqrt(5.0)) / 2.0).toFloat()
        val v = ArrayList<Float>()
        fun add(x: Float, y: Float, z: Float): Int {
            val l = sqrt(x * x + y * y + z * z)
            v += x / l; v += y / l; v += z / l
            return v.size / 3 - 1
        }
        for ((x, y, z) in listOf(
            Triple(-1f, t, 0f), Triple(1f, t, 0f), Triple(-1f, -t, 0f), Triple(1f, -t, 0f),
            Triple(0f, -1f, t), Triple(0f, 1f, t), Triple(0f, -1f, -t), Triple(0f, 1f, -t),
            Triple(t, 0f, -1f), Triple(t, 0f, 1f), Triple(-t, 0f, -1f), Triple(-t, 0f, 1f),
        )) add(x, y, z)
        var f = intArrayOf(
            0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
            1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
            3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
            4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
        )
        repeat(subdiv) {
            val mid = HashMap<Long, Int>()
            fun m(a: Int, b: Int): Int {
                val k = if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
                return mid.getOrPut(k) { add((v[a * 3] + v[b * 3]) / 2, (v[a * 3 + 1] + v[b * 3 + 1]) / 2, (v[a * 3 + 2] + v[b * 3 + 2]) / 2) }
            }
            val nf = IntArray(f.size * 4)
            var o = 0
            for (i in f.indices step 3) {
                val a = f[i]; val b = f[i + 1]; val c = f[i + 2]
                val ab = m(a, b); val bc = m(b, c); val ca = m(c, a)
                for (x in intArrayOf(a, ab, ca, b, bc, ab, c, ca, bc, ab, bc, ca)) nf[o++] = x
            }
            f = nf
        }
        verts = v.toFloatArray()
        tris = f
    }

    val count get() = verts.size / 3

    /** Every edge once, as vertex pairs. */
    fun edges(): IntArray {
        val seen = HashSet<Long>()
        val out = ArrayList<Int>()
        for (i in tris.indices step 3) {
            for (k in 0 until 3) {
                val a = tris[i + k]
                val b = tris[i + (k + 1) % 3]
                val key = if (a < b) (a.toLong() shl 32) or b.toLong() else (b.toLong() shl 32) or a.toLong()
                if (seen.add(key)) { out += a; out += b }
            }
        }
        return out.toIntArray()
    }
}
