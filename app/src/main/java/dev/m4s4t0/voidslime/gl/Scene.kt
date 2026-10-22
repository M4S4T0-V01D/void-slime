package dev.m4s4t0.voidslime.gl

import android.opengl.Matrix
import android.os.Handler
import android.os.Looper
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.model.ADD_IN_LEVELS
import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.STARTER_JARS
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidslime.sim.Collider
import dev.m4s4t0.voidslime.sim.PlateCollider
import dev.m4s4t0.voidslime.sim.RollerCollider
import dev.m4s4t0.voidslime.sim.SoftBody
import dev.m4s4t0.voidslime.sim.SphereCollider
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

/**
 * The slime and everything around it: camera, fingers and tools, add-ins, bubbles, particles, and the sounds and
 * haptics that go with them. Lives on the GL thread (SlimeView hands it touches and settings with queueEvent);
 * SlimeRenderer draws it.
 */
class Scene(private val listener: SceneListener) {
    private val main = Handler(Looper.getMainLooper())
    private val rnd = Random(System.nanoTime())
    val body = SoftBody(4)

    var recipe: Recipe = STARTER_JARS.first(); private set
    var form = 1f; private set
    var mix = 0f; private set
    var time = 0f; private set
    var room = 1f; private set
    /** How warm the slime is from being played with (0..1): Thermo slime changes colour with it. */
    var heat = 0f; private set

    var tool = Tool.POKE
        set(v) { if (v != field) { releaseAll(); field = v } }
    var mode = SceneMode.PLAY
        set(v) {
            if (v != field) {
                releaseAll(); field = v
                if (v == SceneMode.RUSH) { bubbles.clear(); rushClock = 0f; nextSpawn = 0.6f }
            }
        }
    var lightsOn = true
    var look = Look.DEFAULT
    var zoom = 1f

    internal val items = ArrayList<Item>()
    internal val bubbles = ArrayList<Bubble>()
    internal val particles = ArrayList<Particle>()
    internal var props: List<Prop> = emptyList(); private set
    private val touches = LinkedHashMap<Int, Touch>()
    val palm = PlateCollider().apply { active = false }
    val pin = RollerCollider().apply { active = false }
    var pinSpin = 0f; private set
    private var stirAcc = 0f
    private var rushClock = 0f
    private var nextSpawn = 0f

    // ---- camera ----
    var width = 1; private set
    var height = 1; private set
    private var yaw = 0.5f
    private var pitch = 0.64f
    private var yawVel = 0f
    val proj = FloatArray(16)
    val view = FloatArray(16)
    val vp = FloatArray(16)
    private val inv = FloatArray(16)
    val eye = FloatArray(3)
    private val fwd = FloatArray(3)
    private val ro = FloatArray(3)
    private val rd = FloatArray(3)
    private val tmp = FloatArray(6)
    private val ext = FloatArray(2)

    init {
        body.colliders += palm
        body.colliders += pin
        applyMaterial()
        body.reset()
    }

    val transparent get() = body.mat.alpha < 0.9f

    fun resize(w: Int, h: Int) { width = max(1, w); height = max(1, h) }

    // ---- settings from the UI ----

    /** Shows [r]. A different slime starts afresh; the same one (edited in the lab) changes in place, with drops. */
    fun setRecipe(r: Recipe, fresh: Boolean = false) {
        val old = recipe
        recipe = r
        if (fresh || old.id != r.id) {
            mix = 0f
            applyMaterial()
            body.reset()
            items.clear()
            buildItems(false)
            return
        }
        if (old.base != r.base) applyMaterial()
        if (old.colors != r.colors && r.colors.size >= old.colors.size) {
            Audio.play(Snd.PLIP, 0.8f, 0.9f + rnd.nextFloat() * 0.3f)
            mix = max(0f, mix - 0.35f)
            body.extent(ext)
            val c = rgb(colorOf(r.colors.last()).argb)
            burst(body.cx, ext[0] + 0.05f, body.cz, 14, 1.4f, 0.07f) { c }
            body.impulseAt(body.cx, ext[0], body.cz, 0.7f, 0f, -1.2f, 0f)
        }
        if (old.addIns != r.addIns) buildItems(true)
        if (old.glitter != r.glitter && r.glitter != Glitter.NONE) {
            Audio.play(Snd.WHOOSH, 0.5f)
            body.extent(ext)
            val g = r.glitter
            repeat(40) {
                val c = if (g.argb == 0L) hue(rnd.nextFloat()) else rgb(g.argb)
                burst(body.cx + (rnd.nextFloat() - 0.5f) * 1.6f, ext[0] + 0.8f + rnd.nextFloat(), body.cz + (rnd.nextFloat() - 0.5f) * 1.6f, 1, 0.3f, 0.05f) { c }
            }
        }
    }

    /** How far the slime has come together (lab: 0 glue puddle, 1 slime). */
    fun setForm(f: Float) {
        form = f.coerceIn(0f, 1f)
        stirAcc = form * STIR_TOTAL
        applyMaterial()
    }

    private fun applyMaterial() = body.setMaterial(recipe.base.mat, form)

    fun ballUp() {
        body.ballUp()
        Audio.play(Snd.SLAP, 0.7f)
        Haptics.buzz(Haptics.Kind.THUD, 0.7f)
    }

    fun resetShape() = body.reset()

    /** A happy hop and a shower of confetti (an order done, a slime finished). */
    fun celebrate() {
        body.impulse(0f, 2.4f, 0f)
        body.extent(ext)
        repeat(50) {
            val p = spawn(body.cx, ext[0] + 0.3f, body.cz, 2.6f, 0.09f)
            val c = hue(rnd.nextFloat())
            p.r = c[0]; p.g = c[1]; p.b = c[2]
            p.shape = listOf(1f, 2f, 8f)[rnd.nextInt(3)]
            p.vy += 2.2f
            p.max = 1.6f; p.life = p.max
        }
    }

    // ---- touch ----

    fun touch(action: Int, id: Int, x: Float, y: Float) {
        when (action) {
            DOWN -> down(id, x, y)
            MOVE -> touches[id]?.let { move(it, x, y) }
            UP -> touches.remove(id)?.let { up(it) }
            else -> releaseAll()
        }
    }

    private fun releaseAll() {
        for (t in touches.values) up(t)
        touches.clear()
    }

    private fun down(id: Int, x: Float, y: Float) {
        ray(x, y)
        val t = Touch(id, x, y, time)
        touches[id] = t
        hitBubble()?.let { pop(it); t.kind = Kind.DONE; return }
        val tl = if (mode == SceneMode.HOME || mode == SceneMode.RUSH) Tool.POKE else tool
        when (tl) {
            Tool.NONE -> Unit
            Tool.POKE -> body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])?.let { h ->
                t.kind = Kind.FINGER
                t.depth = h.t
                val c = SphereCollider(h.x, h.y, h.z, 0.17f)
                body.colliders += c
                t.finger = c
                t.px = h.x; t.py = h.y; t.pz = h.z
                Audio.play(Snd.SQUISH, 0.5f, 1.1f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.5f)
                stat(Stat.POKES)
            }
            Tool.STRETCH -> body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])?.let { h ->
                t.kind = Kind.GRAB
                val g = body.grab(h.x, h.y, h.z, 0.42f)
                body.grabs += g
                t.grab = g
                Audio.play(Snd.SQUISH, 0.5f, 0.85f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.6f)
            }
            Tool.SQUISH -> if (!palm.active) planeY(0.3f)?.takeIf { hypot(it[0], it[2]) < 2.3f }?.let { p ->
                body.extent(ext)
                palm.x = clampR(p[0], p[2], 1.4f, 0); palm.z = clampR(p[0], p[2], 1.4f, 2)
                palm.y = ext[0] + 0.05f
                palm.active = true
                t.kind = Kind.PALM
            }
            Tool.ROLL -> if (!pin.active) planeY(0.3f)?.takeIf { hypot(it[0], it[2]) < 2.3f }?.let { p ->
                body.extent(ext)
                pin.ax = cos(yaw); pin.az = -sin(yaw)
                pin.x = p[0]; pin.z = p[2]
                pin.y = ext[0] + pin.r
                pin.active = true
                t.kind = Kind.PIN
                t.px = p[0]; t.pz = p[2]
            }
            Tool.BUBBLE -> body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])?.let { h ->
                val b = Bubble(rnd.nextFloat())
                b.tri = h.tri; b.u = h.u; b.v = h.v
                b.x = h.x; b.y = h.y; b.z = h.z
                bubbles += b
                t.bubble = b
                t.kind = Kind.BUBBLE
                Audio.play(Snd.BLOW, 0.5f, 1f, pan(x))
            }
            Tool.SLAP -> slap(t, x)
            Tool.RAKE -> body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])?.let { h ->
                t.kind = Kind.RAKE
                t.depth = h.t
                t.balls = List(4) { SphereCollider(h.x, h.y, h.z, 0.075f) }
                body.colliders += t.balls
                t.px = h.x; t.py = h.y; t.pz = h.z
                aimRake(t)
                Audio.play(Snd.SQUISH, 0.45f, 1.25f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.5f)
            }
            Tool.STAMP -> if (touches.values.none { it.kind == Kind.STAMP }) planeY(0.3f)?.takeIf { hypot(it[0], it[2]) < 2.3f }?.let { p ->
                body.extent(ext)
                t.kind = Kind.STAMP
                t.level = ext[0] + 0.05f
                t.px = clampR(p[0], p[2], 1.3f, 0); t.pz = clampR(p[0], p[2], 1.3f, 2)
                t.angle = atan2(-cos(yaw), -sin(yaw))
                t.balls = buildList {
                    add(SphereCollider(0f, 0f, 0f, 0.11f))
                    for (k in 0 until 5) for (d in 1..3) add(SphereCollider(0f, 0f, 0f, 0.1f - d * 0.016f))
                }
                body.colliders += t.balls
                placeStamp(t)
            }
            Tool.SHAKER -> {
                body.extent(ext)
                t.level = ext[0] + 1.3f
                planeY(t.level)?.let { p ->
                    t.kind = Kind.SHAKER
                    t.px = clampR(p[0], p[2], 2f, 0); t.py = t.level; t.pz = clampR(p[0], p[2], 2f, 2)
                }
            }
            Tool.MAGNET -> {
                body.extent(ext)
                t.level = ext[0] + 0.5f
                planeY(t.level)?.let { p ->
                    t.kind = Kind.MAGNET
                    t.px = clampR(p[0], p[2], 1.8f, 0); t.py = t.level; t.pz = clampR(p[0], p[2], 1.8f, 2)
                    Audio.play(Snd.CLICK, 0.6f, 0.6f, pan(x))
                    Haptics.buzz(Haptics.Kind.TICK, 0.6f)
                }
            }
            Tool.WAND -> wand(x)
            Tool.STIR -> planeY(0.1f)?.takeIf { hypot(it[0], it[2]) < 2.1f }?.let { p ->
                t.kind = Kind.STIR
                val c = SphereCollider(clampR(p[0], p[2], 1.7f, 0), 0.12f, clampR(p[0], p[2], 1.7f, 2), 0.2f)
                body.colliders += c
                t.finger = c
                t.angle = atan2(p[2], p[0])
                t.px = c.x; t.py = c.y; t.pz = c.z
                Audio.play(Snd.SQUISH, 0.4f, 0.8f, pan(x))
            }
        }
    }

    private fun move(t: Touch, x: Float, y: Float) {
        val dx = x - t.x; val dy = y - t.y
        t.x = x; t.y = y
        t.travel += hypot(dx, dy)
        // Every tool that follows the finger works from the ray under it now, not the one from the touch-down.
        ray(x, y)
        when (t.kind) {
            Kind.ORBIT -> {
                val d = -dx / width * 3.4f
                yaw += d
                t.flick = d
                pitch = (pitch + dy / height * 2.2f).coerceIn(0.2f, 1.3f)
            }
            Kind.FINGER -> aimFinger(t)
            Kind.GRAB -> {
                val g = t.grab ?: return
                ray(x, y)
                val den = rd[0] * fwd[0] + rd[1] * fwd[1] + rd[2] * fwd[2]
                if (abs(den) < 1e-4f) return
                val s = ((g.ax - ro[0]) * fwd[0] + (g.ay - ro[1]) * fwd[1] + (g.az - ro[2]) * fwd[2]) / den
                g.tx = ro[0] + rd[0] * s; g.ty = max(0.03f, ro[1] + rd[1] * s); g.tz = ro[2] + rd[2] * s
                val pull = sqrt(sq(g.tx - g.ax) + sq(g.ty - g.ay) + sq(g.tz - g.az))
                t.pull = pull
                g.taper = 1f / (1f + pull * 0.9f)
                if (pull > body.mat.maxStretch) { snap(t); return }
                if (pull > t.tickAt + 0.12f) {
                    t.tickAt = pull
                    Haptics.buzz(Haptics.Kind.TICK, 0.25f + pull * 0.2f)
                    Audio.play(Snd.STRETCH, 0.35f + min(0.4f, pull * 0.2f), 1.15f - pull * 0.12f, pan(x), 260)
                } else if (pull < t.tickAt - 0.2f) t.tickAt = pull
            }
            Kind.RAKE -> aimRake(t)
            Kind.STAMP -> Unit
            Kind.SHAKER, Kind.MAGNET -> planeY(t.level)?.let { p ->
                val r = if (t.kind == Kind.SHAKER) 2f else 1.8f
                t.px = clampR(p[0], p[2], r, 0); t.pz = clampR(p[0], p[2], r, 2)
            }
            Kind.PALM -> planeY(0.3f)?.let { p -> palm.x = clampR(p[0], p[2], 1.4f, 0); palm.z = clampR(p[0], p[2], 1.4f, 2) }
            Kind.PIN -> planeY(pin.y)?.let { p ->
                val nx = clampR(p[0], p[2], 1.9f, 0); val nz = clampR(p[0], p[2], 1.9f, 2)
                val moved = hypot(nx - pin.x, nz - pin.z)
                pinSpin += ((nx - pin.x) * -pin.az + (nz - pin.z) * pin.ax) / pin.r
                pin.x = nx; pin.z = nz
                t.rolled += moved
                if (t.rolled > 1.5f) { t.rolled = 0f; stat(Stat.ROLLS) }
            }
            Kind.STIR -> planeY(0.1f)?.let { p ->
                val c = t.finger ?: return
                c.x = clampR(p[0], p[2], 1.7f, 0); c.z = clampR(p[0], p[2], 1.7f, 2)
                val a = atan2(c.z, c.x)
                var da = a - t.angle
                if (da > PI) da -= (2 * PI).toFloat()
                if (da < -PI) da += (2 * PI).toFloat()
                t.angle = a
                if (abs(da) > 0.9f || hypot(c.x, c.z) < 0.25f) return
                t.turn += abs(da)
                mix = min(0.8f, mix + abs(da) * 0.012f)
                if (t.turn > t.tickTurn + PI.toFloat() / 2) {
                    t.tickTurn = t.turn
                    Haptics.buzz(Haptics.Kind.TICK, 0.35f)
                    Audio.play(Snd.SWISH, 0.6f, 0.9f + rnd.nextFloat() * 0.2f, pan(x), 200)
                }
                if (mode == SceneMode.LAB && form < 1f) {
                    stirAcc += abs(da)
                    form = min(1f, stirAcc / STIR_TOTAL)
                    applyMaterial()
                    val f = form
                    main.post { listener.onStir(f) }
                }
            }
            else -> Unit
        }
    }

    private fun up(t: Touch) {
        when (t.kind) {
            Kind.ORBIT -> yawVel = (t.flick * 60f).coerceIn(-5f, 5f)
            Kind.FINGER -> {
                t.finger?.let { body.colliders.remove(it) }
                Audio.play(Snd.RELEASE, 0.55f, 1f, pan(t.x))
                Haptics.buzz(Haptics.Kind.TICK, 0.3f)
            }
            Kind.GRAB -> {
                t.grab?.let { body.grabs.remove(it) }
                if (t.pull > 0.35f) {
                    stat(Stat.STRETCHES)
                    Audio.play(Snd.RELEASE, 0.6f, 0.9f, pan(t.x))
                    Audio.play(Snd.SLAP, 0.3f + min(0.4f, t.pull * 0.2f), 1.1f, pan(t.x))
                    Haptics.buzz(Haptics.Kind.THUD, 0.4f)
                }
            }
            Kind.PALM -> {
                palm.active = false
                Audio.play(Snd.RELEASE, 0.6f, 0.8f, pan(t.x))
            }
            Kind.PIN -> {
                pin.active = false
                if (t.rolled > 0.5f) stat(Stat.ROLLS)
            }
            Kind.BUBBLE -> t.bubble?.let { b ->
                b.growing = false
                b.r = max(b.r, 0.08f)
                b.vy = 0.2f
            }
            Kind.STIR -> t.finger?.let { body.colliders.remove(it) }
            Kind.RAKE -> {
                body.colliders.removeAll(t.balls.toSet())
                Audio.play(Snd.RELEASE, 0.5f, 1.15f, pan(t.x))
                if (t.rolled > 0.6f) stat(Stat.RAKES)
            }
            Kind.STAMP -> {
                body.colliders.removeAll(t.balls.toSet())
                body.extent(ext)
                if (t.counted) {
                    stat(Stat.STAMPS)
                    Audio.play(Snd.RELEASE, 0.7f, 0.85f, pan(t.x))
                    Audio.play(Snd.PLIP, 0.5f, 1.4f, pan(t.x))
                    Haptics.buzz(Haptics.Kind.POP, 0.5f)
                    // The print it leaves, which fades after a few seconds.
                    val d = hypot(t.px - body.cx, t.pz - body.cz) / max(ext[1], 0.1f)
                    val print = spawn(t.px, ext[0] * sqrt(max(0f, 1f - d * d)) + 0.03f, t.pz, 0f, 0.6f)
                    print.size = 0.62f; print.shape = 1f; print.angle = 0f
                    print.vx = 0f; print.vy = 0f; print.vz = 0f; print.gravity = 0f
                    print.lands = true; print.landed = true
                    print.max = 4f; print.life = print.max
                    val c = rgb(colorOf(recipe.colors.first()).argb)
                    print.r = 0.55f + c[0] * 0.45f; print.g = 0.55f + c[1] * 0.45f; print.b = 0.55f + c[2] * 0.45f
                    repeat(12) {
                        val p = spawn(t.px, ext[0] + 0.1f, t.pz, 1.4f, 0.08f)
                        p.shape = 1f
                        p.r = 1f; p.g = 0.85f + rnd.nextFloat() * 0.15f; p.b = 0.4f + rnd.nextFloat() * 0.5f
                    }
                }
            }
            else -> Unit
        }
        t.kind = Kind.DONE
    }

    private fun aimFinger(t: Touch) {
        val c = t.finger ?: return
        ray(t.x, t.y)
        val s = t.depth + t.press
        c.x = ro[0] + rd[0] * s; c.y = max(0.05f, ro[1] + rd[1] * s); c.z = ro[2] + rd[2] * s
    }

    /** Rake: four tines in a row across the screen, following the finger like Poke does. */
    private fun aimRake(t: Touch) {
        ray(t.x, t.y)
        val s = t.depth + t.press
        val cx = ro[0] + rd[0] * s; val cy = max(0.06f, ro[1] + rd[1] * s); val cz = ro[2] + rd[2] * s
        t.rolled += sqrt(sq(cx - t.px) + sq(cz - t.pz))
        t.px = cx; t.py = cy; t.pz = cz
        val ax = cos(yaw); val az = -sin(yaw)
        t.balls.forEachIndexed { i, b ->
            val o = (i - 1.5f) * 0.17f
            b.x = cx + ax * o; b.y = cy; b.z = cz + az * o
        }
    }

    /** Star stamp: a star of balls, all with their bottoms on the stamp's face at [Touch.level]. */
    private fun placeStamp(t: Touch) {
        val c = t.balls.first()
        c.x = t.px; c.y = t.level + c.r; c.z = t.pz
        var i = 1
        for (k in 0 until 5) {
            val a = t.angle + k * 2f * PI.toFloat() / 5f
            for (d in 1..3) {
                val b = t.balls[i++]
                b.x = t.px + cos(a) * d * 0.13f; b.y = t.level + b.r; b.z = t.pz + sin(a) * d * 0.13f
            }
        }
    }

    private fun slap(t: Touch, x: Float) {
        t.kind = Kind.DONE
        val h = body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])
        if (h == null) {
            Audio.play(Snd.TAP, 0.5f, 0.8f, pan(x))
            return
        }
        body.impulseAt(h.x, h.y, h.z, 0.85f, rd[0] * 2f, -4.5f, rd[2] * 2f)
        body.impulseAt(body.cx, body.cy, body.cz, 2.5f, 0f, 0.6f, 0f)
        heat = min(1f, heat + 0.04f)
        Audio.play(Snd.SLAP, 1f, 0.85f + rnd.nextFloat() * 0.3f, pan(x), 60)
        Audio.play(Snd.SQUISH, 0.5f, 0.8f, pan(x), 60)
        Haptics.buzz(Haptics.Kind.THUD, 1f)
        stat(Stat.SLAPS)
        val c = rgb(colorOf(recipe.colors.first()).argb)
        repeat(10) {
            val p = spawn(h.x, h.y + 0.05f, h.z, 2f, 0.06f)
            p.shape = 0f
            p.r = c[0]; p.g = c[1]; p.b = c[2]
        }
    }

    /** Bubble wand: a little cloud of bubbles from where you tap, some of them golden. */
    private fun wand(x: Float) {
        body.extent(ext)
        val h = body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])
        val p = h?.let { floatArrayOf(it.x, it.y, it.z) } ?: planeY(ext[0] * 0.6f) ?: return
        val n = 6 + rnd.nextInt(5)
        repeat(n) {
            val b = Bubble(rnd.nextFloat())
            b.growing = false
            b.x = p[0] + (rnd.nextFloat() - 0.5f) * 0.6f
            b.y = max(0.15f, p[1] + 0.1f + rnd.nextFloat() * 0.35f)
            b.z = p[2] + (rnd.nextFloat() - 0.5f) * 0.6f
            b.r = 0.07f + rnd.nextFloat() * 0.12f
            b.vy = 0.3f + rnd.nextFloat() * 0.3f
            b.gold = rnd.nextFloat() < 0.12f
            bubbles += b
        }
        while (bubbles.size > 40) bubbles.removeAt(0)
        Audio.play(Snd.BLOW, 0.6f, 1.3f, pan(x))
        Audio.play(Snd.BLOP, 0.4f, 1.5f, pan(x))
        Haptics.buzz(Haptics.Kind.TICK, 0.5f)
        stat(Stat.WANDS)
    }

    private fun updateToys(dt: Float) {
        val out = ArrayList<Prop>()
        for (t in touches.values) when (t.kind) {
            Kind.RAKE -> {
                t.press = min(0.34f, t.press + dt * 1.1f)
                aimRake(t)
                for (b in t.balls) out += Prop(b.x, b.y, b.z, b.r, STEEL)
            }
            Kind.STAMP -> {
                body.extent(ext)
                t.level = max(0.12f, t.level - dt * 1.4f)
                placeStamp(t)
                if (!t.counted && t.level < ext[0] - 0.12f && t.balls.sumOf { it.contacts } > 30) t.counted = true
                for (b in t.balls) out += Prop(b.x, b.y, b.z, b.r, STAMP_PINK)
            }
            Kind.SHAKER -> {
                t.clock += dt
                // Faster shaking, more sprinkles.
                val rate = 22f + min(60f, t.travel * 0.15f)
                t.travel = 0f
                t.press += rate * dt
                while (t.press >= 1f) {
                    t.press -= 1f
                    val p = spawn(t.px + (rnd.nextFloat() - 0.5f) * 0.18f, t.py - 0.1f, t.pz + (rnd.nextFloat() - 0.5f) * 0.18f, 0f, 0.075f)
                    p.vx = (rnd.nextFloat() - 0.5f) * 0.6f; p.vy = -0.6f; p.vz = (rnd.nextFloat() - 0.5f) * 0.6f
                    p.gravity = 4.5f
                    p.shape = if (rnd.nextFloat() < 0.15f) 1f else 3f
                    p.max = 5f; p.life = p.max
                    p.lands = true
                    val c = rgb(SPRINKLES[rnd.nextInt(SPRINKLES.size)])
                    p.r = c[0]; p.g = c[1]; p.b = c[2]
                }
                if (t.clock - t.tickTurn > 0.22f) {
                    t.tickTurn = t.clock
                    Audio.play(Snd.SWISH, 0.25f, 1.6f + rnd.nextFloat() * 0.3f, pan(t.x), 150)
                    Haptics.buzz(Haptics.Kind.TICK, 0.2f)
                }
                if (t.clock.toInt() > t.ticks) { t.ticks = t.clock.toInt(); stat(Stat.SHAKES) }
                out += Prop(t.px, t.py + 0.1f, t.pz, 0.14f, SHAKER_WHITE)
                out += Prop(t.px, t.py + 0.26f, t.pz, 0.09f, STEEL)
            }
            Kind.MAGNET -> {
                t.clock += dt
                val m = body.mat
                val pull = 24f * (if (recipe.base == dev.m4s4t0.voidslime.model.Base.MAGNETIC) 2f else 1f + m.metal * 0.5f)
                body.attract(t.px, t.py, t.pz, 1.3f, pull * dt)
                heat = min(1f, heat + dt * 0.1f)
                if (rnd.nextFloat() < dt * 25f) {
                    val p = spawn(t.px + (rnd.nextFloat() - 0.5f) * 0.4f, t.py - 0.2f, t.pz + (rnd.nextFloat() - 0.5f) * 0.4f, 0.4f, 0.05f)
                    p.vx = (t.px - p.x) * 2f; p.vy = 0.8f; p.vz = (t.pz - p.z) * 2f
                    p.gravity = 0f
                    p.r = 0.6f; p.g = 0.85f; p.b = 1f
                }
                if (t.clock - t.tickTurn > 0.35f) {
                    t.tickTurn = t.clock
                    Haptics.buzz(Haptics.Kind.TICK, 0.3f)
                    Audio.play(Snd.STRETCH, 0.2f, 0.6f, pan(t.x), 300)
                }
                if (t.clock.toInt() > t.ticks) { t.ticks = t.clock.toInt(); stat(Stat.MAGNET) }
                out += Prop(t.px, t.py + 0.12f, t.pz, 0.16f, MAGNET_RED)
                out += Prop(t.px, t.py - 0.02f, t.pz, 0.09f, STEEL)
            }
            else -> Unit
        }
        props = out
    }

    private fun snap(t: Touch) {
        val g = t.grab ?: return
        body.grabs.remove(g)
        t.grab = null
        t.kind = Kind.DONE
        Audio.play(Snd.SNAP, 0.9f, 1f, pan(t.x))
        Haptics.buzz(Haptics.Kind.SNAP, 1f)
        stat(Stat.SNAPS)
        stat(Stat.STRETCHES)
        val c = rgb(colorOf(recipe.colors.first()).argb)
        burst(g.tx, g.ty, g.tz, 22, 2.2f, 0.07f) { c }
    }

    // ---- bubbles ----

    private fun hitBubble(): Bubble? {
        var best: Bubble? = null
        var bt = Float.MAX_VALUE
        for (b in bubbles) {
            if (b.growing) continue
            val ox = b.x - ro[0]; val oy = b.y - ro[1]; val oz = b.z - ro[2]
            val s = ox * rd[0] + oy * rd[1] + oz * rd[2]
            val d2 = ox * ox + oy * oy + oz * oz - s * s
            val rr = max(b.r * 1.25f, 0.22f)
            if (s > 0f && d2 < rr * rr && s < bt) { bt = s; best = b }
        }
        return best
    }

    private fun pop(b: Bubble) {
        bubbles.remove(b)
        val big = b.r > 0.4f
        Audio.play(if (big) Snd.BIG_POP else Snd.POP, 0.8f, (1.5f - b.r * 1.2f).coerceIn(0.7f, 1.6f), 0f, 15)
        Haptics.buzz(Haptics.Kind.POP, if (big) 1f else 0.6f)
        stat(Stat.POPS)
        if (b.gold && mode != SceneMode.RUSH) {
            stat(Stat.GOLD_POPS)
            Audio.play(Snd.COIN, 0.5f, 1.2f)
        }
        val n = if (big) 26 else 14
        repeat(n) {
            val a = it * 2f * PI.toFloat() / n
            val p = spawn(b.x + cos(a) * b.r, b.y + (rnd.nextFloat() - 0.5f) * b.r, b.z + sin(a) * b.r, 0.6f, 0.05f)
            p.vx = cos(a) * (1f + b.r * 2f); p.vz = sin(a) * (1f + b.r * 2f)
            p.gravity = 0.6f
            val c = if (b.gold) floatArrayOf(1f, 0.85f, 0.4f) else hue(b.seed + it / n.toFloat())
            p.r = c[0]; p.g = c[1]; p.b = c[2]
        }
        if (mode == SceneMode.RUSH) {
            val pts = if (b.gold) 5 else 1
            main.post { listener.onRushPop(pts) }
        }
    }

    private fun updateBubbles(dt: Float) {
        val it = bubbles.iterator()
        val toPop = ArrayList<Bubble>()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            if (b.growing) {
                b.r += dt * 0.3f * (1.15f - b.r / MAX_BUBBLE)
                body.surface(b.tri, b.u, b.v, tmp)
                b.x = tmp[0] + tmp[3] * b.r * 0.85f; b.y = tmp[1] + tmp[4] * b.r * 0.85f; b.z = tmp[2] + tmp[5] * b.r * 0.85f
                if (b.age - b.lastBlow > 0.45f) { b.lastBlow = b.age; Audio.play(Snd.BLOW, 0.35f, 1.1f - b.r * 0.5f, 0f, 400) }
                if (b.r >= MAX_BUBBLE) toPop += b
            } else {
                val rise = if (mode == SceneMode.RUSH) b.vy else 0.35f / (b.r * 3f + 0.4f)
                b.vy += (rise - b.vy) * min(1f, dt * 2f)
                b.y += b.vy * dt
                b.x += sin(b.age * 1.7f + b.seed * 40f) * 0.12f * dt
                b.z += cos(b.age * 1.3f + b.seed * 20f) * 0.12f * dt
                if (b.y > 5.5f || b.age > 14f) { b.fade -= dt * 2f; if (b.fade <= 0f) it.remove() }
            }
        }
        toPop.forEach { pop(it) }
        if (mode == SceneMode.RUSH) {
            rushClock += dt
            nextSpawn -= dt
            if (nextSpawn <= 0f && bubbles.size < 14) {
                nextSpawn = max(0.28f, 0.8f - rushClock * 0.009f) * (0.6f + rnd.nextFloat() * 0.8f)
                val b = Bubble(rnd.nextFloat())
                b.growing = false
                body.extent(ext)
                val a = rnd.nextFloat() * 2f * PI.toFloat()
                val rr = rnd.nextFloat() * ext[1] * 0.8f
                b.x = body.cx + cos(a) * rr; b.z = body.cz + sin(a) * rr; b.y = ext[0] * 0.7f
                b.r = 0.12f + rnd.nextFloat() * 0.18f
                b.vy = 0.55f + rnd.nextFloat() * 0.5f + rushClock * 0.004f
                b.gold = rnd.nextFloat() < 0.08f
                bubbles += b
                Audio.play(Snd.BLOP, 0.25f, 1.3f, 0f, 60)
            }
        }
    }

    // ---- particles ----

    private fun spawn(x: Float, y: Float, z: Float, speed: Float, size: Float): Particle {
        val p = Particle()
        p.x = x; p.y = y; p.z = z
        val a = rnd.nextFloat() * 2f * PI.toFloat()
        val e = rnd.nextFloat() * 0.8f + 0.2f
        p.vx = cos(a) * speed * (1f - e * 0.5f); p.vy = e * speed; p.vz = sin(a) * speed * (1f - e * 0.5f)
        p.max = 0.6f + rnd.nextFloat() * 0.6f; p.life = p.max
        p.size = size * (0.7f + rnd.nextFloat() * 0.6f)
        p.angle = rnd.nextFloat() * 6.28f
        if (particles.size >= MAX_PARTICLES) particles.removeAt(0)
        particles += p
        return p
    }

    private inline fun burst(x: Float, y: Float, z: Float, n: Int, speed: Float, size: Float, color: () -> FloatArray) {
        repeat(n) {
            val p = spawn(x, y, z, speed * (0.5f + rnd.nextFloat() * 0.5f), size)
            val c = color()
            p.r = c[0]; p.g = c[1]; p.b = c[2]
        }
    }

    private fun updateParticles(dt: Float) {
        body.extent(ext)
        val it = particles.iterator()
        val drag = exp(-1.6f * dt)
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) { it.remove(); continue }
            p.vy -= p.gravity * dt
            p.vx *= drag; p.vy *= drag; p.vz *= drag
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt
            if (p.lands && !p.landed) {
                // Lands on the dome of the slime (roughly its shape) and stays there a while.
                val d = hypot(p.x - body.cx, p.z - body.cz) / max(ext[1], 0.1f)
                val top = if (d < 1f) ext[0] * sqrt(1f - d * d) else 0f
                if (p.y <= top) {
                    p.y = top; p.vx = 0f; p.vy = 0f; p.vz = 0f; p.gravity = 0f
                    p.landed = true
                    Audio.play(Snd.PLIP, 0.12f, 1.5f + rnd.nextFloat() * 0.6f, 0f, 70)
                }
            }
            if (p.y < 0.01f) { p.y = 0.01f; p.vy = abs(p.vy) * 0.3f }
            if (!p.landed) p.angle += dt * 3f
        }
    }

    // ---- add-ins ----

    private fun buildItems(animate: Boolean) {
        val had = items.groupingBy { it.addIn }.eachCount()
        items.clear()
        val triCount = body.tris.size / 3
        for ((a, lvl) in recipe.addIns.toSortedMap()) {
            val count = ADD_IN_LEVELS[lvl.coerceIn(0, ADD_IN_LEVELS.lastIndex)].second
            val r = Random(recipe.id.hashCode() * 31 + a.ordinal)
            val before = had[a] ?: 0
            repeat(count) { k ->
                var tri = r.nextInt(triCount)
                // Mostly on the top: the underside is pressed into the dish.
                repeat(4) { if (body.pattern[body.tris[tri * 3] * 3 + 1] < -0.1f) tri = r.nextInt(triCount) }
                var u = r.nextFloat(); var v = r.nextFloat()
                if (u + v > 1f) { u = 1f - u; v = 1f - v }
                val tints = a.tints.ifEmpty { AddIn.BRIGHT }
                val c = rgb(tints[r.nextInt(tints.size)])
                val item = Item(a, tri, u, v, 0.1f + r.nextFloat() * 0.75f, c[0], c[1], c[2], a.size * (0.8f + r.nextFloat() * 0.4f), r.nextFloat() * 6.28f, (r.nextFloat() - 0.5f) * 1.5f)
                if (animate && k >= before) { item.drop = 0f; item.delay = (k - before) * 0.03f }
                items += item
            }
        }
    }

    private fun updateItems(dt: Float) {
        val inside = transparent
        val cx = body.cx; val cy = body.cy; val cz = body.cz
        var landed = 0
        for (it in items) {
            body.surface(it.tri, it.u, it.v, tmp)
            var x: Float; var y: Float; var z: Float
            if (inside) {
                x = tmp[0] + (cx - tmp[0]) * it.depth; y = tmp[1] + (cy - tmp[1]) * it.depth; z = tmp[2] + (cz - tmp[2]) * it.depth
                it.lit = 0.9f
            } else {
                val o = it.size * 0.25f
                x = tmp[0] + tmp[3] * o; y = tmp[1] + tmp[4] * o; z = tmp[2] + tmp[5] * o
                it.lit = 0.55f + 0.5f * max(0f, tmp[3] * LIGHT[0] + tmp[4] * LIGHT[1] + tmp[5] * LIGHT[2])
            }
            if (it.drop < 1f) {
                if (it.delay > 0f) it.delay -= dt else it.drop = min(1f, it.drop + dt * 1.7f)
                if (it.drop >= 1f) landed++
                // Falls in from above the dish, speeding up.
                y = 3.2f + (y - 3.2f) * it.drop * it.drop
            }
            it.x = x; it.y = y; it.z = z
        }
        if (landed > 0) {
            Audio.play(Snd.PLIP, 0.25f, 1.2f + rnd.nextFloat() * 0.4f, 0f, 45)
            body.impulseAt(body.cx, body.cy + 0.4f, body.cz, 1.2f, 0f, -0.15f * landed, 0f)
        }
    }

    // ---- feel: clicks, squishes, crunches ----

    private fun feel(t: Touch, c: Collider, x: Float, y: Float, z: Float, dt: Float) {
        val speed = sqrt(sq(x - t.px) + sq(y - t.py) + sq(z - t.pz)) / max(dt, 1e-3f)
        t.px = x; t.py = y; t.pz = z
        val con = c.contacts
        val pan = pan(t.x)
        if (t.lastContacts == 0 && con > 3) {
            Audio.play(Snd.SQUISH, 0.35f + min(0.4f, con * 0.01f), 1f, pan, 90)
            Haptics.buzz(Haptics.Kind.TICK, 0.4f)
        }
        if (con - t.lastContacts > 18) Audio.play(Snd.SQUISH, 0.5f, 0.85f, pan, 140)
        t.clickAcc += abs(con - t.lastContacts) * 0.06f + speed * min(con, 60) * 0.012f * dt
        t.lastContacts = con
        if (t.clickAcc > 1f) {
            t.clickAcc = min(t.clickAcc - 1f, 2f)
            val crunchy = body.mat.crunchy
            Audio.play(if (crunchy && rnd.nextFloat() < 0.5f) Snd.CRUNCH else Snd.CLICK, 0.25f + rnd.nextFloat() * 0.45f, 0.75f + rnd.nextFloat() * 0.7f, pan, 35)
            Haptics.buzz(Haptics.Kind.TICK, 0.25f)
            if (crunchy && rnd.nextFloat() < 0.3f) stat(Stat.CRUNCHES)
        }
        if (c === palm && con > 40 && !t.counted) { t.counted = true; stat(Stat.SQUISHES) }
        if (con == 0) return
        for (it in items) {
            if (!it.addIn.crunchy || time - it.lastCrunch < 0.6f) continue
            if (near(c, it.x, it.y, it.z)) {
                it.lastCrunch = time
                Audio.play(Snd.CRUNCH, 0.55f + rnd.nextFloat() * 0.3f, 0.85f + rnd.nextFloat() * 0.4f, pan, 50)
                Haptics.buzz(Haptics.Kind.CRUNCH, 0.6f)
                stat(Stat.CRUNCHES)
                break
            }
        }
    }

    private fun near(c: Collider, x: Float, y: Float, z: Float): Boolean = when (c) {
        is SphereCollider -> sq(x - c.x) + sq(y - c.y) + sq(z - c.z) < sq(c.r + 0.08f)
        is PlateCollider -> sq(x - c.x) + sq(z - c.z) < c.r * c.r && y > c.y - 0.15f
        is RollerCollider -> {
            val dx = x - c.x; val dz = z - c.z
            val s = dx * c.ax + dz * c.az
            sq(dx - c.ax * s) + sq(y - c.y) + sq(dz - c.az * s) < sq(c.r + 0.1f)
        }
    }

    // ---- the frame ----

    fun update(dt: Float) {
        time += dt
        room += ((if (lightsOn) 1f else 0.1f) - room) * min(1f, dt * 3f)
        val idle = touches.values.none { it.kind == Kind.ORBIT }
        if (idle) {
            yaw += yawVel * dt
            yawVel *= exp(-2.5f * dt)
            if (mode == SceneMode.HOME && touches.isEmpty()) yaw += 0.14f * dt
        }
        for (t in touches.values) when (t.kind) {
            Kind.FINGER -> { t.press = min(0.3f, t.press + dt * 0.9f); aimFinger(t) }
            Kind.PALM -> palm.y = max(0.13f, palm.y - dt * 1.5f)
            Kind.PIN -> pin.y = max(pin.r + 0.07f, pin.y - dt * 2f)
            else -> Unit
        }
        updateToys(dt)
        val working = touches.values.any { it.kind in WORKING }
        heat = if (working) min(1f, heat + dt * 0.16f) else max(0f, heat - dt * 0.04f)
        body.step(dt)
        if (mode != SceneMode.LAB) mix = min(0.75f, mix + body.agitation * dt * 0.1f)
        for (t in touches.values) {
            when (t.kind) {
                Kind.FINGER, Kind.STIR -> t.finger?.let { feel(t, it, it.x, it.y, it.z, dt) }
                Kind.PALM -> feel(t, palm, palm.x, palm.y, palm.z, dt)
                Kind.PIN -> feel(t, pin, pin.x, pin.y, pin.z, dt)
                Kind.RAKE, Kind.STAMP -> t.balls.getOrNull(if (t.kind == Kind.RAKE) 1 else 0)?.let { feel(t, it, it.x, it.y, it.z, dt) }
                else -> Unit
            }
        }
        updateItems(dt)
        updateBubbles(dt)
        updateParticles(dt)
        camera()
    }

    private fun camera() {
        val aspect = width / height.toFloat()
        val t = tan(Math.toRadians(22.5)).toFloat()
        val dist = (max(2.5f / (t * aspect), 1.9f / t) * zoom).coerceIn(4f, 18f)
        val ty = 0.3f
        eye[0] = dist * cos(pitch) * sin(yaw); eye[1] = ty + dist * sin(pitch); eye[2] = dist * cos(pitch) * cos(yaw)
        val l = sqrt(sq(eye[0]) + sq(eye[1] - ty) + sq(eye[2]))
        fwd[0] = -eye[0] / l; fwd[1] = -(eye[1] - ty) / l; fwd[2] = -eye[2] / l
        Matrix.perspectiveM(proj, 0, 45f, aspect, 0.5f, 80f)
        Matrix.setLookAtM(view, 0, eye[0], eye[1], eye[2], 0f, ty, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)
        Matrix.invertM(inv, 0, vp, 0)
    }

    // ---- helpers ----

    private fun ray(x: Float, y: Float) {
        val nx = 2f * x / width - 1f; val ny = 1f - 2f * y / height
        val a = FloatArray(4); val b = FloatArray(4)
        Matrix.multiplyMV(a, 0, inv, 0, floatArrayOf(nx, ny, -1f, 1f), 0)
        Matrix.multiplyMV(b, 0, inv, 0, floatArrayOf(nx, ny, 1f, 1f), 0)
        for (k in 0 until 3) { ro[k] = a[k] / a[3]; rd[k] = b[k] / b[3] - ro[k] }
        val l = sqrt(sq(rd[0]) + sq(rd[1]) + sq(rd[2]))
        for (k in 0 until 3) rd[k] /= l
    }

    /** Where the last ray crosses the level plane y = [h]. */
    private fun planeY(h: Float): FloatArray? {
        if (rd[1] > -1e-4f) return null
        val s = (h - ro[1]) / rd[1]
        return floatArrayOf(ro[0] + rd[0] * s, h, ro[2] + rd[2] * s)
    }

    private fun clampR(x: Float, z: Float, r: Float, axis: Int): Float {
        val d = hypot(x, z)
        val k = if (d > r) r / d else 1f
        return (if (axis == 0) x else z) * k
    }

    private fun pan(x: Float) = (x / width * 2f - 1f) * 0.6f
    private fun stat(s: String) { main.post { listener.onStat(s) } }

    companion object {
        const val DOWN = 0
        const val MOVE = 1
        const val UP = 2
        const val CANCEL = 3
        const val MAX_BUBBLE = 0.72f
        const val MAX_PARTICLES = 400
        val STIR_TOTAL = (6 * 2 * PI).toFloat()
        private val WORKING = setOf(Kind.FINGER, Kind.GRAB, Kind.PALM, Kind.PIN, Kind.STIR, Kind.RAKE, Kind.STAMP, Kind.MAGNET)
        private val STEEL = floatArrayOf(0.82f, 0.85f, 0.9f)
        private val STAMP_PINK = floatArrayOf(1f, 0.62f, 0.86f)
        private val SHAKER_WHITE = floatArrayOf(0.95f, 0.95f, 1f)
        private val MAGNET_RED = floatArrayOf(0.92f, 0.16f, 0.22f)
        private val SPRINKLES = listOf(0xFFFF4D8D, 0xFFFFE066, 0xFF22E3FF, 0xFF6EF2B4, 0xFFA970FF, 0xFFFF8A3D, 0xFFFFFFFF)
        val LIGHT = floatArrayOf(-0.45f, 0.85f, 0.5f).let { v -> val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]); floatArrayOf(v[0] / l, v[1] / l, v[2] / l) }

        private fun sq(x: Float) = x * x

        fun rgb(argb: Long) = floatArrayOf(((argb shr 16) and 0xFF) / 255f, ((argb shr 8) and 0xFF) / 255f, (argb and 0xFF) / 255f)

        fun hue(h: Float): FloatArray {
            val f = (h - kotlin.math.floor(h)) * 6f
            return floatArrayOf(
                (abs(((f + 0f) % 6f) - 3f) - 1f).coerceIn(0f, 1f),
                (abs(((f + 4f) % 6f) - 3f) - 1f).coerceIn(0f, 1f),
                (abs(((f + 2f) % 6f) - 3f) - 1f).coerceIn(0f, 1f),
            )
        }
    }
}
