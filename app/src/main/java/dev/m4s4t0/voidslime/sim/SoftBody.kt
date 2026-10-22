package dev.m4s4t0.voidslime.sim

import dev.m4s4t0.voidslime.model.Material
import dev.m4s4t0.voidslime.model.Mold
import dev.m4s4t0.voidslime.model.Plate
import dev.m4s4t0.voidslime.model.Shapes
import dev.m4s4t0.voidslime.model.Vessel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The slime: a closed skin of points (an icosphere) sitting in a dish, held together by
 *  - shape matching: every point is pulled towards where its resting shape says it belongs (relative to the centre
 *    of mass), which is what makes the whole blob wobble back after a poke;
 *  - springs along every edge, so a poke drags the neighbourhood with it and ripples spread across the skin;
 *  - volume: squash it and pressure pushes the sides out;
 *  - plasticity: dents and stretches beyond a small yield flow into the resting shape, which then slowly settles
 *    back into a dome, so slime holds a squish for a moment like the real thing.
 * Tools act through colliders (finger, palm, rolling pin) and grabs (stretching). Sculpting colliders (the rake, the
 * stamp) press their shape straight into the resting shape and it holds a while. Blisters blow air into the skin; the
 * [amount] shrinks as droplets break off and grows back as they return. Units: the dome is about 1 across.
 * Not thread-safe: the renderer steps it on the GL thread.
 */
class SoftBody(subdiv: Int = 4) {
    private val sphere = Icosphere(subdiv)
    val n = sphere.count
    val tris = sphere.tris
    private val edges = sphere.edges()
    private val edgeCount = edges.size / 2

    val pos = FloatArray(n * 3)
    private val vel = FloatArray(n * 3)
    private val frc = FloatArray(n * 3)
    /** The resting shape, relative to its centre. */
    private val rest = FloatArray(n * 3)
    /** The shape it settles into, relative to its centre. */
    private val nat = FloatArray(n * 3)
    private val restLen = FloatArray(edgeCount)
    /** Seconds left that a sculpted point keeps its new shape before slowly settling. */
    private val hold = FloatArray(n)
    /** The blisters' air: offsets added to the natural shape. */
    private val air = FloatArray(n * 3)
    private val airNext = FloatArray(n * 3)
    private val scratch = FloatArray(n * 3)
    private var airDirty = false
    private var natVolume = 1f
    private val areaN = FloatArray(n * 3)
    val normals = FloatArray(n * 3)
    /** Where each point sits on the unit sphere: the slime's swirl and glitter are painted in these coordinates. */
    val pattern: FloatArray = sphere.verts.copyOf()

    var mat: Material = LIQUID
        private set
    private var solid: Material = LIQUID
    /** 0 is a runny puddle of glue, 1 the finished slime (the lab stirs it from one to the other). */
    var form = 1f
        private set

    var cx = 0f; private set
    var cy = 0f; private set
    var cz = 0f; private set
    private var volume0 = 1f
    var volume = 1f; private set

    /** How hard it's being worked right now (0 calm, ~1 squished all over): colours mix in as you knead. */
    var agitation = 0f; private set

    val colliders = ArrayList<Collider>()
    val grabs = ArrayList<Grab>()
    val blisters = ArrayList<Blister>()

    /** How much of the slime is here (1 all of it): droplets that break off take some away until they come back. */
    var amount = 1f
        set(v) {
            val a = v.coerceIn(MIN_AMOUNT, 1f)
            if (abs(a - field) < 1e-6f) return
            val s = cbrt(a / field)
            field = a
            for (i in 0 until n * 3) rest[i] *= s
            shapeNatural()
        }

    /** The volume of all of it (droplets are measured against this). */
    val fullVolume get() = natVolume / amount

    /** The finished slime's material, and how far it's formed. */
    fun setMaterial(m: Material, form: Float) {
        solid = m
        val f = form.coerceIn(0f, 1f)
        val changed = abs(f - this.form) > 0.004f
        this.form = f
        mat = blend(LIQUID, m, smooth(f))
        if (changed) shapeNatural()
    }

    /** Snap straight into the natural shape, at rest. */
    fun reset() {
        blisters.clear()
        air.fill(0f)
        hold.fill(0f)
        volume0 = natVolume
        for (i in 0 until n * 3) {
            rest[i] = nat[i]
            vel[i] = 0f
        }
        val lift = -minY(nat)
        for (i in 0 until n) {
            pos[i * 3] = nat[i * 3]
            pos[i * 3 + 1] = nat[i * 3 + 1] + lift
            pos[i * 3 + 2] = nat[i * 3 + 2]
        }
        updateRestLengths()
        computeNormals()
    }

    /** Fold everything back into a ball-ish dome (the Play screen's "Ball up"): rest returns to natural, softly. */
    fun ballUp() {
        System.arraycopy(nat, 0, rest, 0, n * 3)
        for (i in 0 until n) vel[i * 3 + 1] += 0.6f
    }

    /** The shape it settles into once formed (a dome, a heart, a bunny...). */
    var mold = Mold.DOME
        set(v) { if (v != field) { field = v; shapeNatural() } }

    /** What it sits in: the dish (whose wall follows [plate]) or a round bowl or jar (see [Shapes.vesselR]). */
    var vessel = Vessel.DISH
        set(v) {
            field = v
            vH = Shapes.vesselH(v)
            for (k in 0..V_STEPS) vR[k] = Shapes.vesselR(v, k * vH / V_STEPS)
            for (k in 0..V_STEPS) {
                val a = vR[max(0, k - 1)]; val b = vR[min(V_STEPS, k + 1)]
                vD[k] = ((b - a) / ((min(V_STEPS, k + 1) - max(0, k - 1)) * vH / V_STEPS)).coerceIn(-12f, 12f)
            }
            vMin = vR.min()
        }
    private var vH = 1f
    private val vR = FloatArray(V_STEPS + 1)
    private val vD = FloatArray(V_STEPS + 1)
    private var vMin = 0f

    /** The dish outline the wall follows. */
    var plate = Plate.ROUND
        set(v) {
            field = v
            wallMin = DISH_R * Shapes.plateMin(v)
            for (k in 0 until WALL_STEPS) wall[k] = DISH_R * Shapes.plateR(v, k * 2f * PI.toFloat() / WALL_STEPS)
        }
    private val wall = FloatArray(WALL_STEPS) { DISH_R }
    private var wallMin = DISH_R

    init {
        setMaterial(LIQUID, 1f)
        shapeNatural()
        reset()
    }

    private fun shapeNatural() {
        // Dome: wide and low, nearly flat underneath. Puddle: a thin spread of glue. Molds reshape the formed slime.
        val f = smooth(form)
        val sc = cbrt(amount)
        val radius = lerp(1.5f, 1.05f, f) * sc
        val height = lerp(0.16f, 0.62f, f) * sc
        val under = lerp(0.04f, 0.1f, f) * sc
        val p = FloatArray(3)
        for (i in 0 until n) {
            Shapes.mold(mold, sphere.verts[i * 3], sphere.verts[i * 3 + 1], sphere.verts[i * 3 + 2], radius, height, under, f, p)
            nat[i * 3] = p[0]; nat[i * 3 + 1] = p[1]; nat[i * 3 + 2] = p[2]
        }
        // Relative to its centre of mass.
        var mx = 0f; var my = 0f; var mz = 0f
        for (i in 0 until n) { mx += nat[i * 3]; my += nat[i * 3 + 1]; mz += nat[i * 3 + 2] }
        mx /= n; my /= n; mz /= n
        for (i in 0 until n) { nat[i * 3] -= mx; nat[i * 3 + 1] -= my; nat[i * 3 + 2] -= mz }
        natVolume = volumeOf(nat)
        airDirty = true
        if (blisters.isEmpty()) volume0 = natVolume
    }

    /** The dish wall's radius in the direction of ([x], [z]). */
    fun wallAt(x: Float, z: Float): Float {
        if (plate == Plate.ROUND) return DISH_R
        val a = atan2(z, x)
        val t = ((a / (2f * PI.toFloat()) + 1f) % 1f) * WALL_STEPS
        val k = t.toInt() % WALL_STEPS
        val fr = t - t.toInt()
        return wall[k] + (wall[(k + 1) % WALL_STEPS] - wall[k]) * fr
    }

    private fun minY(a: FloatArray): Float {
        var m = Float.MAX_VALUE
        for (i in 0 until n) m = min(m, a[i * 3 + 1])
        return m
    }

    /** Advances the simulation by [frameDt] seconds in small fixed steps. */
    fun step(frameDt: Float) {
        val dt = 1f / 240f
        val steps = (frameDt / dt).toInt().coerceIn(1, 8)
        if (airDirty) updateAir()
        updateRestLengths()
        var agit = 0f
        repeat(steps) { agit += sub(dt) }
        agitation = agit / steps
        computeNormals()
    }

    private fun sub(dt: Float): Float {
        val m = mat
        // Centre of mass.
        var mx = 0f; var my = 0f; var mz = 0f
        for (i in 0 until n) { mx += pos[i * 3]; my += pos[i * 3 + 1]; mz += pos[i * 3 + 2] }
        mx /= n; my /= n; mz /= n
        cx = mx; cy = my; cz = mz

        // Volume and area-weighted normals (one pass over the triangles).
        volume = accumulateAreaNormals()
        val pressure = m.volumeK * ((volume0 - volume) / volume0).coerceIn(-0.6f, 0.6f)

        // Forces: shape matching, gravity, pressure.
        val ks = m.shapeK
        for (i in 0 until n) {
            val o = i * 3
            val anx = areaN[o]; val any = areaN[o + 1]; val anz = areaN[o + 2]
            val al = sqrt(anx * anx + any * any + anz * anz).takeIf { it > 1e-9f } ?: 1f
            frc[o] = (mx + rest[o] - pos[o]) * ks + pressure * anx / al
            frc[o + 1] = (my + rest[o + 1] - pos[o + 1]) * ks - m.gravity + pressure * any / al
            frc[o + 2] = (mz + rest[o + 2] - pos[o + 2]) * ks + pressure * anz / al
        }
        // Edge springs, damped along the edge.
        val ke = m.edgeK
        val ce = 2f * sqrt(ke) * 0.12f
        for (e in 0 until edgeCount) {
            val a = edges[e * 2] * 3
            val b = edges[e * 2 + 1] * 3
            val dx = pos[b] - pos[a]; val dy = pos[b + 1] - pos[a + 1]; val dz = pos[b + 2] - pos[a + 2]
            val l = sqrt(dx * dx + dy * dy + dz * dz)
            if (l < 1e-7f) continue
            val ix = dx / l; val iy = dy / l; val iz = dz / l
            val rv = (vel[b] - vel[a]) * ix + (vel[b + 1] - vel[a + 1]) * iy + (vel[b + 2] - vel[a + 2]) * iz
            val f = ke * (l - restLen[e]) + ce * rv
            frc[a] += f * ix; frc[a + 1] += f * iy; frc[a + 2] += f * iz
            frc[b] -= f * ix; frc[b + 1] -= f * iy; frc[b + 2] -= f * iz
        }
        // Stretching: grabbed points are pulled towards the finger, tapering into a strand the further it goes.
        for (g in grabs) {
            val idx = g.idx
            for (k in idx.indices) {
                val o = idx[k] * 3
                val w = g.w[k] * g.strength
                val tx = g.tx + g.off[k * 3] * g.taper
                val ty = g.ty + g.off[k * 3 + 1] * g.taper
                val tz = g.tz + g.off[k * 3 + 2] * g.taper
                frc[o] += w * (GRAB_K * (tx - pos[o]) - GRAB_C * vel[o])
                frc[o + 1] += w * (GRAB_K * (ty - pos[o + 1]) - GRAB_C * vel[o + 1])
                frc[o + 2] += w * (GRAB_K * (tz - pos[o + 2]) - GRAB_C * vel[o + 2])
            }
        }
        // Integrate (semi-implicit Euler).
        val keep = max(0f, 1f - m.damping * dt)
        for (i in 0 until n * 3) {
            vel[i] = (vel[i] + frc[i] * dt) * keep
            pos[i] += vel[i] * dt
        }
        // Tools.
        for (c in colliders) {
            c.contacts = 0
            c.push = 0f
            if (c.active) c.resolve(this)
        }
        // The dish: a floor that grips, and a wall.
        val grip = max(0f, 1f - 14f * dt)
        if (vessel != Vessel.DISH) {
            container(grip, max(0f, 1f - 5f * dt))
        } else for (i in 0 until n) {
            val o = i * 3
            if (pos[o + 1] < 0f) {
                pos[o + 1] = 0f
                if (vel[o + 1] < 0f) vel[o + 1] = 0f
                vel[o] *= grip; vel[o + 2] *= grip
            } else if (pos[o + 1] < 0.004f) {
                vel[o] *= grip; vel[o + 2] *= grip
            }
            val r2 = pos[o] * pos[o] + pos[o + 2] * pos[o + 2]
            if (r2 > wallMin * wallMin) {
                val r = sqrt(r2)
                val wr = wallAt(pos[o], pos[o + 2])
                if (r <= wr) continue
                val nx = pos[o] / r; val nz = pos[o + 2] / r
                pos[o] = nx * wr; pos[o + 2] = nz * wr
                val vr = vel[o] * nx + vel[o + 2] * nz
                if (vr > 0f) { vel[o] -= vr * nx; vel[o + 2] -= vr * nz }
            }
        }
        // Plasticity past a small yield, and settling back to the natural shape.
        val pf = m.plastic * dt
        val rf = m.relax * dt
        var agit = 0f
        val sf = min(1f, SCULPT_RATE * dt)
        for (i in 0 until n) {
            val o = i * 3
            val dx = pos[o] - mx - rest[o]
            val dy = pos[o + 1] - my - rest[o + 1]
            val dz = pos[o + 2] - mz - rest[o + 2]
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            // Sculpted points take their new shape almost at once and keep it a few seconds.
            val h = hold[i]
            if (h > 0f) hold[i] = max(0f, h - dt)
            val y = if (h > 0f) SCULPT_YIELD else YIELD
            if (d > y) {
                val k = (if (h > HOLD - 0.3f) sf else pf) * (d - y) / d
                rest[o] += dx * k; rest[o + 1] += dy * k; rest[o + 2] += dz * k
                agit += d - y
            }
            val r = if (h > 0f) rf * 0.06f else rf
            rest[o] += (nat[o] + air[o] - rest[o]) * r
            rest[o + 1] += (nat[o + 1] + air[o + 1] - rest[o + 1]) * r
            rest[o + 2] += (nat[o + 2] + air[o + 2] - rest[o + 2]) * r
        }
        return agit / n * 8f
    }

    /**
     * A round bowl or jar: a floor that grips, and a curved wall the slime slides up (and sticks to a little). A point
     * outside the wall at its height goes back in along the wall's normal, in the (radius, height) plane.
     */
    private fun container(grip: Float, stick: Float) {
        val steps = V_STEPS / vH
        for (i in 0 until n) {
            val o = i * 3
            if (pos[o + 1] < 0f) {
                pos[o + 1] = 0f
                if (vel[o + 1] < 0f) vel[o + 1] = 0f
                vel[o] *= grip; vel[o + 2] *= grip
            }
            val r2 = pos[o] * pos[o] + pos[o + 2] * pos[o + 2]
            if (r2 <= vMin * vMin) continue
            val r = sqrt(r2)
            val t = (pos[o + 1] * steps).coerceIn(0f, V_STEPS.toFloat())
            val k = min(t.toInt(), V_STEPS - 1)
            val fr = t - k
            val wr = vR[k] + (vR[k + 1] - vR[k]) * fr
            if (r <= wr) continue
            val dr = vD[k] + (vD[k + 1] - vD[k]) * fr
            val inv = 1f / sqrt(1f + dr * dr)
            // Outward normal (nr, ny) and how far out the point is along it.
            val nr = inv; val ny = -dr * inv
            val out = (r - wr) * inv
            val ex = pos[o] / r; val ez = pos[o + 2] / r
            val nrr = r - out * nr
            pos[o] = ex * nrr; pos[o + 2] = ez * nrr
            pos[o + 1] = max(0f, pos[o + 1] - out * ny)
            val vn = (vel[o] * ex + vel[o + 2] * ez) * nr + vel[o + 1] * ny
            if (vn > 0f) {
                vel[o] -= vn * nr * ex; vel[o + 2] -= vn * nr * ez; vel[o + 1] -= vn * ny
            }
            vel[o] *= stick; vel[o + 1] *= stick; vel[o + 2] *= stick
        }
    }

    /** Recomputes the blisters' air, moving the resting shape along with it, and the volume to hold. */
    private fun updateAir() {
        airDirty = false
        airNext.fill(0f)
        for (b in blisters) {
            if (b.size <= 0f) continue
            for (i in 0 until n) {
                val o = i * 3
                val qx = pattern[o] - b.px; val qy = pattern[o + 1] - b.py; val qz = pattern[o + 2] - b.pz
                val d = sqrt(qx * qx + qy * qy + qz * qz) / Blister.R
                if (d >= 1f) continue
                // A dome rising off the skin, widening a little at its base like a balloon.
                val cap = sqrt(1f - d * d)
                val along = qx * b.nx + qy * b.ny + qz * b.nz
                var tx = qx - b.nx * along; var ty = qy - b.ny * along; var tz = qz - b.nz * along
                val tl = sqrt(tx * tx + ty * ty + tz * tz)
                if (tl > 1e-5f) { tx /= tl; ty /= tl; tz /= tl } else { tx = 0f; ty = 0f; tz = 0f }
                val side = 0.45f * d * cap
                airNext[o] += b.size * (cap * b.nx + side * tx)
                airNext[o + 1] += b.size * (cap * b.ny + side * ty)
                airNext[o + 2] += b.size * (cap * b.nz + side * tz)
            }
        }
        for (i in 0 until n * 3) {
            rest[i] += airNext[i] - air[i]
            air[i] = airNext[i]
            scratch[i] = nat[i] + air[i]
        }
        volume0 = if (blisters.isEmpty()) natVolume else volumeOf(scratch)
    }

    /** Starts a blister at the hit point (pattern coordinates [px], [py], [pz]), rising along the normal there. */
    fun addBlister(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float): Blister {
        val pl = sqrt(px * px + py * py + pz * pz).takeIf { it > 1e-6f } ?: 1f
        return Blister(px / pl, py / pl, pz / pl, nx, ny, nz).also { blisters += it }
    }

    /** Blows [d] more air into [b], up to its limit. */
    fun blow(b: Blister, d: Float) {
        b.size = min(Blister.MAX, b.size + d)
        airDirty = true
    }

    /** Lets all the air out of [b] at once: the skin falls back in. */
    fun popBlister(b: Blister) {
        blisters.remove(b)
        airDirty = true
    }

    /** The blister under pattern point (x, y, z), if any (and how close to its middle, 0..1). */
    fun blisterAt(x: Float, y: Float, z: Float): Blister? = blisters.filter { it.size > 0.04f }.minByOrNull {
        sq(x - it.px) + sq(y - it.py) + sq(z - it.pz)
    }?.takeIf { sqrt(sq(x - it.px) + sq(y - it.py) + sq(z - it.pz)) < Blister.R * 0.85f }

    /** The world position of a blister's top. */
    fun blisterTop(b: Blister, out: FloatArray) {
        var best = 0; var bd = Float.MAX_VALUE
        for (i in 0 until n) {
            val d = sq(pattern[i * 3] - b.px) + sq(pattern[i * 3 + 1] - b.py) + sq(pattern[i * 3 + 2] - b.pz)
            if (d < bd) { bd = d; best = i }
        }
        out[0] = pos[best * 3]; out[1] = pos[best * 3 + 1]; out[2] = pos[best * 3 + 2]
    }

    /** Pattern (unit sphere) coordinates at barycentric (u, v) on [tri]. */
    fun patternAt(tri: Int, u: Float, v: Float, out: FloatArray) {
        val a = tris[tri * 3] * 3; val b = tris[tri * 3 + 1] * 3; val c = tris[tri * 3 + 2] * 3
        val w = 1f - u - v
        for (k in 0 until 3) out[k] = pattern[a + k] * w + pattern[b + k] * u + pattern[c + k] * v
    }

    /** A sculpting tool touched point [i]: it keeps the new shape for a while. */
    internal fun sculpt(i: Int) { hold[i] = HOLD }

    private fun updateRestLengths() {
        for (e in 0 until edgeCount) {
            val a = edges[e * 2] * 3
            val b = edges[e * 2 + 1] * 3
            val dx = rest[b] - rest[a]; val dy = rest[b + 1] - rest[a + 1]; val dz = rest[b + 2] - rest[a + 2]
            restLen[e] = sqrt(dx * dx + dy * dy + dz * dz)
        }
    }

    /** Fills [areaN] with each point's area-weighted normal and returns the enclosed volume. */
    private fun accumulateAreaNormals(): Float {
        areaN.fill(0f)
        var v = 0f
        val p = pos
        for (t in tris.indices step 3) {
            val a = tris[t] * 3; val b = tris[t + 1] * 3; val c = tris[t + 2] * 3
            val e1x = p[b] - p[a]; val e1y = p[b + 1] - p[a + 1]; val e1z = p[b + 2] - p[a + 2]
            val e2x = p[c] - p[a]; val e2y = p[c + 1] - p[a + 1]; val e2z = p[c + 2] - p[a + 2]
            val nx = e1y * e2z - e1z * e2y
            val ny = e1z * e2x - e1x * e2z
            val nz = e1x * e2y - e1y * e2x
            v += (p[a] * nx + p[a + 1] * ny + p[a + 2] * nz)
            areaN[a] += nx; areaN[a + 1] += ny; areaN[a + 2] += nz
            areaN[b] += nx; areaN[b + 1] += ny; areaN[b + 2] += nz
            areaN[c] += nx; areaN[c + 1] += ny; areaN[c + 2] += nz
        }
        return v / 6f
    }

    private fun volumeOf(a: FloatArray): Float {
        var v = 0f
        for (t in tris.indices step 3) {
            val i = tris[t] * 3; val j = tris[t + 1] * 3; val k = tris[t + 2] * 3
            v += a[i] * (a[j + 1] * a[k + 2] - a[j + 2] * a[k + 1]) +
                a[i + 1] * (a[j + 2] * a[k] - a[j] * a[k + 2]) +
                a[i + 2] * (a[j] * a[k + 1] - a[j + 1] * a[k])
        }
        return abs(v / 6f).coerceAtLeast(1e-4f)
    }

    fun computeNormals() {
        accumulateAreaNormals()
        for (i in 0 until n) {
            val o = i * 3
            val l = sqrt(areaN[o] * areaN[o] + areaN[o + 1] * areaN[o + 1] + areaN[o + 2] * areaN[o + 2]).takeIf { it > 1e-12f } ?: 1f
            normals[o] = areaN[o] / l; normals[o + 1] = areaN[o + 1] / l; normals[o + 2] = areaN[o + 2] / l
        }
    }

    /** A kick to every point (a slap, a celebration bounce). */
    fun impulse(vx: Float, vy: Float, vz: Float) {
        for (i in 0 until n) { vel[i * 3] += vx; vel[i * 3 + 1] += vy; vel[i * 3 + 2] += vz }
    }

    /** A kick to the points near (x, y, z), fading out over [radius]. */
    fun impulseAt(x: Float, y: Float, z: Float, radius: Float, vx: Float, vy: Float, vz: Float) {
        for (i in 0 until n) {
            val o = i * 3
            val dx = pos[o] - x; val dy = pos[o + 1] - y; val dz = pos[o + 2] - z
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (d < radius) {
                val w = (1f - d / radius).let { it * it }
                vel[o] += vx * w; vel[o + 1] += vy * w; vel[o + 2] += vz * w
            }
        }
    }

    /** Pulls the points within [radius] of (x, y, z) towards it (the magnet), harder the closer they are. */
    fun attract(x: Float, y: Float, z: Float, radius: Float, strength: Float) {
        for (i in 0 until n) {
            val o = i * 3
            val dx = x - pos[o]; val dy = y - pos[o + 1]; val dz = z - pos[o + 2]
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (d < radius && d > 0.06f) {
                val w = (1f - d / radius).let { it * it } * strength / d
                vel[o] += dx * w; vel[o + 1] += dy * w; vel[o + 2] += dz * w
            }
        }
    }

    // ---- picking ----

    class Hit(val t: Float, val tri: Int, val u: Float, val v: Float, val x: Float, val y: Float, val z: Float)

    /** The nearest point where the ray (origin o, direction d) meets the skin, or null. */
    fun raycast(ox: Float, oy: Float, oz: Float, dx: Float, dy: Float, dz: Float): Hit? {
        var best = Float.MAX_VALUE
        var bt = -1; var bu = 0f; var bv = 0f
        val p = pos
        for (t in tris.indices step 3) {
            val a = tris[t] * 3; val b = tris[t + 1] * 3; val c = tris[t + 2] * 3
            val e1x = p[b] - p[a]; val e1y = p[b + 1] - p[a + 1]; val e1z = p[b + 2] - p[a + 2]
            val e2x = p[c] - p[a]; val e2y = p[c + 1] - p[a + 1]; val e2z = p[c + 2] - p[a + 2]
            val px = dy * e2z - dz * e2y; val py = dz * e2x - dx * e2z; val pz = dx * e2y - dy * e2x
            val det = e1x * px + e1y * py + e1z * pz
            if (abs(det) < 1e-9f) continue
            val inv = 1f / det
            val sx = ox - p[a]; val sy = oy - p[a + 1]; val sz = oz - p[a + 2]
            val u = (sx * px + sy * py + sz * pz) * inv
            if (u < 0f || u > 1f) continue
            val qx = sy * e1z - sz * e1y; val qy = sz * e1x - sx * e1z; val qz = sx * e1y - sy * e1x
            val v = (dx * qx + dy * qy + dz * qz) * inv
            if (v < 0f || u + v > 1f) continue
            val tt = (e2x * qx + e2y * qy + e2z * qz) * inv
            if (tt > 1e-4f && tt < best) { best = tt; bt = t / 3; bu = u; bv = v }
        }
        if (bt < 0) return null
        return Hit(best, bt, bu, bv, ox + dx * best, oy + dy * best, oz + dz * best)
    }

    /** Position and normal of the skin at barycentric (u, v) on triangle [tri], written into [out] (6 floats). */
    fun surface(tri: Int, u: Float, v: Float, out: FloatArray) {
        val a = tris[tri * 3] * 3; val b = tris[tri * 3 + 1] * 3; val c = tris[tri * 3 + 2] * 3
        val w = 1f - u - v
        for (k in 0 until 3) {
            out[k] = pos[a + k] * w + pos[b + k] * u + pos[c + k] * v
            out[3 + k] = normals[a + k] * w + normals[b + k] * u + normals[c + k] * v
        }
        val l = sqrt(out[3] * out[3] + out[4] * out[4] + out[5] * out[5]).takeIf { it > 1e-9f } ?: 1f
        out[3] /= l; out[4] /= l; out[5] /= l
    }

    /** Starts stretching from the hit point: the points within [radius] follow the target, the nearest most. */
    fun grab(x: Float, y: Float, z: Float, radius: Float): Grab {
        val idx = ArrayList<Int>()
        val ws = ArrayList<Float>()
        for (i in 0 until n) {
            val o = i * 3
            val dx = pos[o] - x; val dy = pos[o + 1] - y; val dz = pos[o + 2] - z
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (d < radius) { idx += i; ws += (1f - d / radius).let { it * it } }
        }
        val off = FloatArray(idx.size * 3)
        for (k in idx.indices) {
            val o = idx[k] * 3
            off[k * 3] = pos[o] - x; off[k * 3 + 1] = pos[o + 1] - y; off[k * 3 + 2] = pos[o + 2] - z
        }
        return Grab(idx.toIntArray(), ws.toFloatArray(), off).also { it.tx = x; it.ty = y; it.tz = z; it.ax = x; it.ay = y; it.az = z }
    }

    /** Top of the slime and how far it spreads from its centre (for the shadow and the camera). */
    fun extent(out: FloatArray) {
        var top = 0f
        var r = 0f
        for (i in 0 until n) {
            val o = i * 3
            top = max(top, pos[o + 1])
            val dx = pos[o] - cx; val dz = pos[o + 2] - cz
            r = max(r, dx * dx + dz * dz)
        }
        out[0] = top
        out[1] = sqrt(r)
    }

    /** Pushes every point within [radius] of (x, y, z) outwards from it at [speed] (a bonk, a splash). */
    fun burstAt(x: Float, y: Float, z: Float, radius: Float, speed: Float) {
        for (i in 0 until n) {
            val o = i * 3
            val dx = pos[o] - x; val dy = pos[o + 1] - y; val dz = pos[o + 2] - z
            val d = sqrt(dx * dx + dy * dy + dz * dz)
            if (d < radius && d > 1e-4f) {
                val w = (1f - d / radius) * speed / d
                vel[o] += dx * w; vel[o + 1] += dy * w * 0.5f; vel[o + 2] += dz * w
            }
        }
    }

    /** The skin point nearest (x, y, z). */
    fun nearest(x: Float, y: Float, z: Float): Int {
        var best = 0; var bd = Float.MAX_VALUE
        for (i in 0 until n) {
            val d = sq(pos[i * 3] - x) + sq(pos[i * 3 + 1] - y) + sq(pos[i * 3 + 2] - z)
            if (d < bd) { bd = d; best = i }
        }
        return best
    }

    /** Rest offset of point [i] (for tests). */
    internal fun restOf(i: Int) = floatArrayOf(rest[i * 3], rest[i * 3 + 1], rest[i * 3 + 2])

    internal fun maxSpeed(): Float {
        var m = 0f
        for (i in 0 until n * 3) m = max(m, abs(vel[i]))
        return m
    }

    internal fun velocity() = vel

    private fun sq(x: Float) = x * x

    companion object {
        const val DISH_R = 1.85f
        private const val WALL_STEPS = 720
        private const val V_STEPS = 256
        const val YIELD = 0.07f
        private const val SCULPT_YIELD = 0.015f
        /** How fast a sculpted point's rest follows it (per second), and how long it holds. */
        private const val SCULPT_RATE = 30f
        const val HOLD = 3.5f
        /** Never less than this much left in the dish. */
        const val MIN_AMOUNT = 0.55f
        const val GRAB_K = 700f
        const val GRAB_C = 26f

        /** Glue before the activator: runny, flows into a puddle. */
        val LIQUID = Material(shapeK = 8f, edgeK = 300f, damping = 6f, plastic = 1.6f, relax = 1.4f, volumeK = 6f, gravity = 0.15f, maxStretch = 0.8f, alpha = 0.8f, gloss = 1f)

        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        fun smooth(t: Float) = t * t * (3f - 2f * t)

        fun blend(a: Material, b: Material, t: Float) = Material(
            shapeK = lerp(a.shapeK, b.shapeK, t), edgeK = lerp(a.edgeK, b.edgeK, t), damping = lerp(a.damping, b.damping, t),
            plastic = lerp(a.plastic, b.plastic, t), relax = lerp(a.relax, b.relax, t), volumeK = lerp(a.volumeK, b.volumeK, t),
            gravity = lerp(a.gravity, b.gravity, t), maxStretch = lerp(a.maxStretch, b.maxStretch, t),
            alpha = lerp(min(a.alpha, b.alpha + 0.3f), b.alpha, t), gloss = lerp(a.gloss, b.gloss, t), fluff = b.fluff * t,
            metal = b.metal * t, holo = b.holo * t, glow = b.glow * t, frost = b.frost * t, galaxy = b.galaxy * t, thermo = b.thermo * t, crunchy = b.crunchy && t > 0.9f,
        )
    }
}

/**
 * Air blown into the skin: a dome around pattern point ([px], [py], [pz]) rising along ([nx], [ny], [nz]), [size]
 * high (world units).
 */
class Blister(val px: Float, val py: Float, val pz: Float, val nx: Float, val ny: Float, val nz: Float) {
    var size = 0f

    companion object {
        /** How wide (a chord on the unit sphere) and how high it can get before it bursts. */
        const val R = 0.62f
        const val MAX = 0.85f
    }
}

/** A finger stretching the slime: grabbed points, how strongly each follows ([w]), and where they were. */
class Grab(val idx: IntArray, val w: FloatArray, val off: FloatArray) {
    var tx = 0f; var ty = 0f; var tz = 0f
    /** Where the stretch started. */
    var ax = 0f; var ay = 0f; var az = 0f
    var taper = 1f
    var strength = 1f
}

/** Something solid pushing into the slime. [contacts]/[push] say how much it touched in the last step (for sounds). */
sealed class Collider {
    var active = true
    /** Presses its shape into the slime's resting shape (rake, stamp, cutter). */
    var sculpt = false
    var contacts = 0
    var push = 0f
    abstract fun resolve(b: SoftBody)
}

/** A fingertip. */
class SphereCollider(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var r: Float = 0.16f) : Collider() {
    override fun resolve(b: SoftBody) {
        val p = b.pos
        val v = b.velocity()
        for (i in 0 until b.n) {
            val o = i * 3
            val dx = p[o] - x; val dy = p[o + 1] - y; val dz = p[o + 2] - z
            val d2 = dx * dx + dy * dy + dz * dz
            if (d2 >= r * r) continue
            val d = sqrt(d2).takeIf { it > 1e-6f } ?: 1e-6f
            val nx = dx / d; val ny = dy / d; val nz = dz / d
            push += r - d
            contacts++
            if (sculpt) b.sculpt(i)
            p[o] = x + nx * r; p[o + 1] = y + ny * r; p[o + 2] = z + nz * r
            val vn = v[o] * nx + v[o + 1] * ny + v[o + 2] * nz
            if (vn < 0f) { v[o] -= vn * nx; v[o + 1] -= vn * ny; v[o + 2] -= vn * nz }
        }
    }
}

/** A palm pressing down: a round flat plate at height [y] with softly rounded edges. */
class PlateCollider(var x: Float = 0f, var z: Float = 0f, var r: Float = 0.75f, var y: Float = 2f) : Collider() {
    override fun resolve(b: SoftBody) {
        val p = b.pos
        val v = b.velocity()
        for (i in 0 until b.n) {
            val o = i * 3
            val dx = p[o] - x; val dz = p[o + 2] - z
            val d = sqrt(dx * dx + dz * dz)
            if (d >= r) continue
            val edge = 0.7f * r
            val lim = if (d > edge) y + ((d - edge) / (r - edge)).let { it * it } * 0.35f else y
            if (p[o + 1] > lim) {
                push += p[o + 1] - lim
                contacts++
                p[o + 1] = lim
                if (v[o + 1] > 0f) v[o + 1] = 0f
            }
        }
    }
}

/** A rolling pin: a horizontal cylinder through (x, y, z) along (ax, az). */
class RollerCollider(var x: Float = 0f, var y: Float = 0.4f, var z: Float = 0f, var ax: Float = 1f, var az: Float = 0f, var r: Float = 0.22f, var half: Float = 1.6f) : Collider() {
    override fun resolve(b: SoftBody) {
        val p = b.pos
        val v = b.velocity()
        for (i in 0 until b.n) {
            val o = i * 3
            val dx = p[o] - x; val dy = p[o + 1] - y; val dz = p[o + 2] - z
            val t = (dx * ax + dz * az).coerceIn(-half, half)
            val qx = dx - ax * t; val qy = dy; val qz = dz - az * t
            val d2 = qx * qx + qy * qy + qz * qz
            if (d2 >= r * r) continue
            val d = sqrt(d2).takeIf { it > 1e-6f } ?: 1e-6f
            val nx = qx / d; val ny = qy / d; val nz = qz / d
            push += r - d
            contacts++
            p[o] += nx * (r - d); p[o + 1] += ny * (r - d); p[o + 2] += nz * (r - d)
            val vn = v[o] * nx + v[o + 1] * ny + v[o + 2] * nz
            if (vn < 0f) { v[o] -= vn * nx; v[o + 1] -= vn * ny; v[o + 2] -= vn * nz }
        }
    }
}
