package dev.m4s4t0.voidslime.gl

import android.opengl.GLES20
import android.util.Log
import dev.m4s4t0.voidslime.sim.Icosphere
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun floats(n: Int): FloatBuffer = ByteBuffer.allocateDirect(n * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
fun floats(a: FloatArray): FloatBuffer = floats(a.size).apply { put(a); position(0) }
fun shorts(a: ShortArray): ShortBuffer = ByteBuffer.allocateDirect(a.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(a); position(0) }

/** A linked shader program with its attribute and uniform locations looked up once. */
class Program(vs: String, fs: String) {
    val id: Int = GLES20.glCreateProgram()
    private val attribs = HashMap<String, Int>()
    private val uniforms = HashMap<String, Int>()

    init {
        GLES20.glAttachShader(id, compile(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(id, compile(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(id)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(id, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) Log.e(TAG, "link: " + GLES20.glGetProgramInfoLog(id))
    }

    fun use() = GLES20.glUseProgram(id)
    fun a(name: String) = attribs.getOrPut(name) { GLES20.glGetAttribLocation(id, name) }
    fun u(name: String) = uniforms.getOrPut(name) { GLES20.glGetUniformLocation(id, name) }

    fun f(name: String, v: Float) = GLES20.glUniform1f(u(name), v)
    fun v2(name: String, x: Float, y: Float) = GLES20.glUniform2f(u(name), x, y)
    fun v3(name: String, x: Float, y: Float, z: Float) = GLES20.glUniform3f(u(name), x, y, z)
    fun v3(name: String, c: FloatArray) = GLES20.glUniform3f(u(name), c[0], c[1], c[2])
    fun v4(name: String, x: Float, y: Float, z: Float, w: Float) = GLES20.glUniform4f(u(name), x, y, z, w)
    fun m4(name: String, m: FloatArray) = GLES20.glUniformMatrix4fv(u(name), 1, false, m, 0)

    private fun compile(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) Log.e(TAG, "compile: " + GLES20.glGetShaderInfoLog(s))
        return s
    }

    companion object {
        const val TAG = "VoidSlime"
    }
}

/** A static indexed mesh of interleaved position + normal. */
class Mesh(data: FloatArray, index: ShortArray) {
    private val vbo: Int
    private val ibo: Int
    private val count = index.size

    init {
        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        vbo = ids[0]; ibo = ids[1]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, floats(data), GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, index.size * 2, shorts(index), GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    /** Draws with [p]'s aPos (and aNor, when it has one). */
    fun draw(p: Program) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        val ap = p.a("aPos")
        val an = p.a("aNor")
        GLES20.glEnableVertexAttribArray(ap)
        GLES20.glVertexAttribPointer(ap, 3, GLES20.GL_FLOAT, false, 24, 0)
        if (an >= 0) {
            GLES20.glEnableVertexAttribArray(an)
            GLES20.glVertexAttribPointer(an, 3, GLES20.GL_FLOAT, false, 24, 12)
        }
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, count, GLES20.GL_UNSIGNED_SHORT, 0)
        GLES20.glDisableVertexAttribArray(ap)
        if (an >= 0) GLES20.glDisableVertexAttribArray(an)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }
}

object Meshes {
    /**
     * A profile of (radius, height) points spun round the y axis, [seg] times. Normals come from the profile's
     * slope, so a smooth profile shades smoothly.
     */
    fun lathe(profile: List<Pair<Float, Float>>, seg: Int = 96): Mesh {
        val rows = profile.size
        val data = FloatArray((seg + 1) * rows * 6)
        var o = 0
        for (s in 0..seg) {
            val a = s * 2.0 * Math.PI / seg
            val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
            for (k in 0 until rows) {
                val (r, y) = profile[k]
                val (r0, y0) = profile[maxOf(k - 1, 0)]
                val (r1, y1) = profile[minOf(k + 1, rows - 1)]
                // Tangent along the profile; the normal is it turned a quarter to the left, so a profile that runs
                // outwards along a floor faces up.
                val tr = r1 - r0; val ty = y1 - y0
                val l = hypot(tr, ty).takeIf { it > 1e-6f } ?: 1f
                val nr = -ty / l; val ny = tr / l
                data[o++] = r * ca; data[o++] = y; data[o++] = r * sa
                data[o++] = nr * ca; data[o++] = ny; data[o++] = nr * sa
            }
        }
        val idx = ShortArray(seg * (rows - 1) * 6)
        var i = 0
        for (s in 0 until seg) for (k in 0 until rows - 1) {
            val a = (s * rows + k).toShort()
            val b = ((s + 1) * rows + k).toShort()
            val c = (s * rows + k + 1).toShort()
            val d = ((s + 1) * rows + k + 1).toShort()
            idx[i++] = a; idx[i++] = c; idx[i++] = b
            idx[i++] = b; idx[i++] = c; idx[i++] = d
        }
        return Mesh(data, idx)
    }

    /** The dish: a flat floor at y = 0 curving up into a rounded rim. */
    fun tray(): Mesh = lathe(
        listOf(
            0f to -0.003f, 1.0f to -0.003f, 1.88f to -0.003f, 1.96f to 0.02f, 2.03f to 0.09f, 2.07f to 0.17f,
            2.11f to 0.215f, 2.16f to 0.225f, 2.21f to 0.2f, 2.24f to 0.12f, 2.22f to 0.0f, 2.12f to -0.07f, 1.6f to -0.09f, 0f to -0.09f,
        ),
    )

    /** A rolling pin along y (the renderer turns it on its side): handles, a long barrel with rounded ends. Top down. */
    fun rollingPin(): Mesh = lathe(
        listOf(
            0f to 2.35f, 0.06f to 2.33f, 0.085f to 2.25f, 0.085f to 1.85f, 0.11f to 1.78f, 0.2f to 1.72f, 0.22f to 1.6f,
            0.22f to -1.6f, 0.2f to -1.72f, 0.11f to -1.78f, 0.085f to -1.85f, 0.085f to -2.25f, 0.06f to -2.33f, 0f to -2.35f,
        ),
        seg = 40,
    )

    /** A palm-sized glass disc (unit radius, thin), for the squish tool. */
    fun palm(): Mesh = lathe(listOf(0f to 0.12f, 0.8f to 0.12f, 0.96f to 0.1f, 1f to 0.06f, 0.95f to 0.02f, 0.8f to 0f, 0f to 0f), seg = 64)

    /** A unit sphere for bubbles (normals = positions). */
    fun sphere(subdiv: Int = 2): Mesh {
        val s = Icosphere(subdiv)
        val data = FloatArray(s.count * 6)
        for (i in 0 until s.count) for (k in 0 until 3) {
            data[i * 6 + k] = s.verts[i * 3 + k]
            data[i * 6 + 3 + k] = s.verts[i * 3 + k]
        }
        return Mesh(data, ShortArray(s.tris.size) { s.tris[it].toShort() })
    }
}
