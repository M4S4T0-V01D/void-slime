package dev.m4s4t0.voidslime.gl

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** How the hand holds its fingers. Curls are thumb, index, middle, ring, pinky (0 straight, 1 a fist). */
internal enum class Pose(val curl: FloatArray, val spread: Float) {
    REST(floatArrayOf(0.2f, 0.25f, 0.32f, 0.38f, 0.45f), 0.35f),
    POINT(floatArrayOf(0.85f, 0f, 1f, 1f, 1f), 0f),
    OPEN(floatArrayOf(0f, 0.04f, 0.04f, 0.04f, 0.06f), 0.7f),
    PINCH(floatArrayOf(0.5f, 0.5f, 0.85f, 0.95f, 1f), 0f),
    CLAW(floatArrayOf(0.45f, 0.45f, 0.45f, 0.45f, 0.45f), 0.9f),
    HOLD(floatArrayOf(0.75f, 0.95f, 0.95f, 0.95f, 0.95f), 0f),
}

/** Which point of the hand goes where it's aimed. */
internal enum class Grip { TIP, PALM, PINCH, FIST, CLAW }

/**
 * A white cartoon glove that floats by itself and does the poking, slapping and holding. Each frame the scene aims
 * it: a pose, which point of the hand ([Grip]) goes where, and which way the fingers ([fx]) and the back of the hand
 * ([nx]) face. It glides there, blending between poses, and is drawn as rounded rods ([props]).
 */
internal class Hand {
    var visible = false
    private var placed = false

    // Where it's going.
    private val target = FloatArray(3)
    private val tf = floatArrayOf(0f, 0f, -1f)
    private val tn = floatArrayOf(0f, 1f, 0f)
    private var pose = Pose.REST
    private var grip = Grip.PALM
    private var rate = 10f
    /** Extra curl (the claw squeezing). */
    private var squeeze = 0f

    // Where it is.
    private val wrist = FloatArray(3)
    private val f = floatArrayOf(0f, 0f, -1f)
    private val n = floatArrayOf(0f, 1f, 0f)
    private val s = FloatArray(3)
    private val curl = FloatArray(5)
    private var spread = 0.3f

    // Joints, worked out relative to the wrist: per finger, 4 points (knuckle, two joints, tip); palm bases.
    private val joints = Array(5) { Array(4) { FloatArray(3) } }
    private val bases = Array(4) { FloatArray(3) }
    private val anchor = FloatArray(3)

    /** Aims the hand: [grip] of the hand in [pose] goes to (x, y, z), fingers along f, back of the hand along n. */
    fun aim(pose: Pose, grip: Grip, x: Float, y: Float, z: Float, fx: Float, fy: Float, fz: Float, nx: Float, ny: Float, nz: Float, rate: Float = 12f, squeeze: Float = 0f) {
        this.pose = pose; this.grip = grip; this.rate = rate; this.squeeze = squeeze
        target[0] = x; target[1] = y; target[2] = z
        tf[0] = fx; tf[1] = fy; tf[2] = fz; norm(tf)
        tn[0] = nx; tn[1] = ny; tn[2] = nz
        if (!visible) placed = false
        visible = true
    }

    fun hide() { visible = false }

    fun update(dt: Float, out: MutableList<Prop>) {
        if (!visible) return
        val k = if (placed) min(1f, dt * rate) else 1f
        val kp = if (placed) min(1f, dt * 14f) else 1f
        for (i in 0 until 5) curl[i] += (min(1.1f, pose.curl[i] + squeeze * (if (pose == Pose.CLAW) 0.6f else 0f)) - curl[i]) * kp
        spread += (pose.spread - spread) * kp
        for (i in 0 until 3) { f[i] += (tf[i] - f[i]) * k; n[i] += (tn[i] - n[i]) * k }
        norm(f)
        // Keep the back of the hand square to the fingers.
        val d = dot(n, f)
        for (i in 0 until 3) n[i] -= f[i] * d
        if (norm(n) < 1e-4f) { n[0] = 0f; n[1] = 1f; n[2] = 0f }
        cross(n, f, s)
        build()
        when (grip) {
            Grip.TIP -> copy(joints[1][3], anchor)
            Grip.PALM -> for (i in 0 until 3) anchor[i] = f[i] * 0.26f - n[i] * 0.1f
            Grip.PINCH -> for (i in 0 until 3) anchor[i] = (joints[1][3][i] + joints[0][3][i]) * 0.5f
            Grip.FIST -> for (i in 0 until 3) anchor[i] = (joints[2][0][i] + joints[2][2][i]) * 0.5f - n[i] * 0.04f
            Grip.CLAW -> for (i in 0 until 3) anchor[i] = (joints[1][3][i] + joints[2][3][i] + joints[3][3][i] + joints[4][3][i] + joints[0][3][i]) / 5f
        }
        for (i in 0 until 3) {
            val goal = target[i] - anchor[i]
            wrist[i] += (goal - wrist[i]) * k
        }
        placed = true
        for (i in 0 until 3) pos[i] = wrist[i] + anchor[i]
        emit(out)
    }

    /** Where the aimed point of the hand is now (it lags behind the aim). */
    val pos = FloatArray(3)

    /** Fingertip [i] (0 thumb .. 4 pinky) in world space, as of the last [update]. */
    fun tip(i: Int, out: FloatArray) {
        val j = joints[i][3]
        out[0] = wrist[0] + j[0]; out[1] = wrist[1] + j[1]; out[2] = wrist[2] + j[2]
    }

    /** Lays out the joints for the current pose, relative to the wrist. */
    private fun build() {
        val lens = LENGTHS
        for (fi in 1..4) {
            val i = fi - 1
            val side = 0.135f - i * 0.09f
            val reach = KNUCKLE[i]
            val b = bases[i]
            val side0 = 0.08f - i * 0.053f
            for (c in 0 until 3) b[c] = f[c] * 0.06f + s[c] * side0
            val kn = joints[fi][0]
            for (c in 0 until 3) kn[c] = f[c] * reach + s[c] * side
            // Spread fans the fingers out around the back-of-hand axis.
            val a = spread * (1.5f - i) * 0.14f
            val dir = FloatArray(3); val back = FloatArray(3)
            for (c in 0 until 3) { dir[c] = f[c] * cos(a) + s[c] * sin(a); back[c] = n[c] }
            for (j in 0 until 3) {
                val th = curl[fi] * BEND[j]
                val cth = cos(th); val sth = sin(th)
                for (c in 0 until 3) {
                    val d0 = dir[c]; val b0 = back[c]
                    dir[c] = d0 * cth - b0 * sth
                    back[c] = b0 * cth + d0 * sth
                }
                val p0 = joints[fi][j]; val p1 = joints[fi][j + 1]
                for (c in 0 until 3) p1[c] = p0[c] + dir[c] * lens[i][j]
            }
        }
        // Thumb: from the heel of the palm, out to the index side, folding across the palm as it curls.
        val t = curl[0]
        val tb = joints[0][0]
        for (c in 0 until 3) tb[c] = f[c] * 0.13f + s[c] * 0.12f - n[c] * 0.05f
        val dir = FloatArray(3)
        for (c in 0 until 3) dir[c] = f[c] * 0.55f + s[c] * 0.8f - n[c] * 0.35f
        norm(dir)
        val fold = FloatArray(3)
        for (c in 0 until 3) fold[c] = f[c] * 0.55f - s[c] * 0.45f - n[c] * 0.7f
        norm(fold)
        for (j in 0 until 3) {
            val w = (t * (0.45f + j * 0.25f)).coerceIn(0f, 1f)
            val dd = FloatArray(3) { dir[it] * (1f - w) + fold[it] * w }
            norm(dd)
            val p0 = joints[0][j]; val p1 = joints[0][j + 1]
            for (c in 0 until 3) p1[c] = p0[c] + dd[c] * THUMB[j]
        }
    }

    private fun emit(out: MutableList<Prop>) {
        val w = wrist
        fun rod(a: FloatArray, b: FloatArray, r: Float, c: FloatArray) {
            out += Prop(w[0] + a[0], w[1] + a[1], w[2] + a[2], r, c, Prop.Shape.ROD, w[0] + b[0], w[1] + b[1], w[2] + b[2])
        }
        fun ball(a: FloatArray, r: Float, c: FloatArray) { out += Prop(w[0] + a[0], w[1] + a[1], w[2] + a[2], r, c) }
        // Cuff, then the palm as four fat rods side by side.
        val c0 = FloatArray(3) { -f[it] * 0.2f }
        val c1 = FloatArray(3) { f[it] * 0.03f }
        rod(c0, c1, 0.15f, CUFF)
        ball(c1, 0.12f, GLOVE)
        for (i in 0 until 4) {
            rod(bases[i], joints[i + 1][0], 0.085f, GLOVE)
            ball(bases[i], 0.085f, GLOVE)
        }
        for (fi in 0..4) {
            val r = if (fi == 0) 0.075f else if (fi == 4) 0.06f else 0.068f
            ball(joints[fi][0], r * 1.08f, GLOVE)
            for (j in 0 until 3) {
                rod(joints[fi][j], joints[fi][j + 1], r, GLOVE)
                ball(joints[fi][j + 1], r, GLOVE)
            }
        }
    }

    companion object {
        private val GLOVE = floatArrayOf(0.97f, 0.97f, 1f)
        private val CUFF = floatArrayOf(1f, 0.78f, 0.9f)
        private val KNUCKLE = floatArrayOf(0.42f, 0.44f, 0.42f, 0.38f)
        private val LENGTHS = arrayOf(
            floatArrayOf(0.2f, 0.13f, 0.1f), floatArrayOf(0.22f, 0.14f, 0.1f),
            floatArrayOf(0.2f, 0.13f, 0.1f), floatArrayOf(0.15f, 0.1f, 0.08f),
        )
        private val THUMB = floatArrayOf(0.15f, 0.13f, 0.1f)
        private val BEND = floatArrayOf(1.25f, 1.45f, 1.0f)

        fun dot(a: FloatArray, b: FloatArray) = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
        fun cross(a: FloatArray, b: FloatArray, out: FloatArray) {
            val x = a[1] * b[2] - a[2] * b[1]; val y = a[2] * b[0] - a[0] * b[2]; val z = a[0] * b[1] - a[1] * b[0]
            out[0] = x; out[1] = y; out[2] = z
        }
        /** Normalises in place; returns the old length. */
        fun norm(a: FloatArray): Float {
            val l = sqrt(dot(a, a))
            if (l > 1e-6f) { a[0] /= l; a[1] /= l; a[2] /= l }
            return l
        }
        private fun copy(a: FloatArray, b: FloatArray) { b[0] = a[0]; b[1] = a[1]; b[2] = a[2] }
    }
}
