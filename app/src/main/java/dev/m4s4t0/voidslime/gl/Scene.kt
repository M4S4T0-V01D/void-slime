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
import dev.m4s4t0.voidslime.model.Mood
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.STARTER_JARS
import dev.m4s4t0.voidslime.model.Shapes
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.Vessel
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidslime.sim.Blister
import dev.m4s4t0.voidslime.sim.Collider
import dev.m4s4t0.voidslime.sim.PlateCollider
import dev.m4s4t0.voidslime.sim.RollerCollider
import dev.m4s4t0.voidslime.sim.SoftBody
import dev.m4s4t0.voidslime.sim.SphereCollider
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
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
    /** Bits of slime that broke off and are making their way back. */
    internal val droplets = ArrayList<Droplet>()
    internal val hand = Hand()
    /** Builds up with each slap or bonk and fades: slap it fast enough and bits fly off. */
    private var slapEnergy = 0f
    private var flashT = 0f
    private val flashP = FloatArray(3)
    private val pat = FloatArray(3)
    private val hp = FloatArray(3)
    // The camera's right and its towards-you direction, level with the floor.
    private var rgtX = 1f; private var rgtZ = 0f
    private var camX = 0f; private var camZ = 1f
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
    val cameraYaw get() = yaw
    val cameraPitch get() = pitch

    /**
     * The part of the screen the dish should sit in (left, top, right, bottom as fractions), so it's centred in the
     * space the menus leave free. Eases over when it changes.
     */
    var focus = floatArrayOf(0f, 0f, 1f, 1f)
    private val focusNow = floatArrayOf(0f, 0f, 1f, 1f)

    /** Skin points the face is drawn on: eyes, cheeks, mouth (see [Shapes.faceSpot]). */
    val faceVerts = FACE.map { (dx, dy) ->
        val f = Shapes.faceSpot(dx, dy)
        (0 until body.n).maxBy { body.pattern[it * 3] * f[0] + body.pattern[it * 3 + 1] * f[1] + body.pattern[it * 3 + 2] * f[2] }
    }
    /** The face it's pulling: its own mood, or for a moment a reaction to what just happened. */
    val mood: Mood get() = if (reactT > 0f) reaction else recipe.mood
    private var reaction = Mood.HAPPY
    private var reactT = 0f

    /** Pulls face [m] for [secs]; a bigger reaction (later in [REACTIONS]) isn't cut short by a smaller one. */
    private fun react(m: Mood, secs: Float) {
        if (reactT > 0f && REACTIONS.indexOf(m) < REACTIONS.indexOf(reaction)) return
        reaction = m
        reactT = max(reactT, secs)
    }

    /** How closed the eyes are (0 open, 1 shut): blinks now and then, squints happily when squished. */
    var blink = 0f; private set
    private var nextBlink = 3f

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
            droplets.clear()
            body.amount = 1f
            body.reset()
            items.clear()
            buildItems(false)
            return
        }
        if (old.base != r.base || old.mold != r.mold) applyMaterial()
        if (old.mold != r.mold) {
            Audio.play(Snd.SQUISH, 0.7f, 0.8f)
            body.impulse(0f, 1.6f, 0f)
        }
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

    private fun applyMaterial() {
        body.mold = recipe.mold
        body.setMaterial(recipe.base.mat, form)
    }

    fun ballUp() {
        callDroplets()
        body.ballUp()
        Audio.play(Snd.SLAP, 0.7f)
        Haptics.buzz(Haptics.Kind.THUD, 0.7f)
    }

    fun resetShape() {
        droplets.clear()
        body.amount = 1f
        body.reset()
    }

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

    private val playing get() = mode == SceneMode.PLAY || mode == SceneMode.LAB

    private fun down(id: Int, x: Float, y: Float) {
        ray(x, y)
        val t = Touch(id, x, y, time)
        touches[id] = t
        hitBubble()?.let { pop(it); t.kind = Kind.DONE; return }
        val tl = if (mode == SceneMode.HOME || mode == SceneMode.RUSH) Tool.POKE else tool
        if (playing && (tl == Tool.SLAP || tl == Tool.HAMMER)) { swingAt(t, x, tl); return }
        if (playing && (tl == Tool.POKE || tl == Tool.BUBBLE)) {
            hitDroplet()?.let { d -> prod(d, x); t.kind = Kind.DONE; return }
            blisterUnderRay()?.let { b -> popBlister(b, x); flash(hp[0], hp[1], hp[2]); t.kind = Kind.DONE; return }
        }
        when (tl) {
            Tool.NONE, Tool.SLAP, Tool.HAMMER -> Unit
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
                // Room for a few at once: the oldest goes pop to make room for another.
                if (body.blisters.size >= MAX_BLISTERS) popBlister(body.blisters.first(), x)
                body.patternAt(h.tri, h.u, h.v, pat)
                body.surface(h.tri, h.u, h.v, tmp)
                t.blister = body.addBlister(pat[0], pat[1], pat[2], tmp[3], tmp[4], tmp[5])
                t.kind = Kind.BLISTER
                t.nx = tmp[3]; t.ny = tmp[4]; t.nz = tmp[5]
                Audio.play(Snd.BLOW, 0.5f, 1f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.4f)
            }
            Tool.RAKE -> body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])?.let { h ->
                // The tines sink in where you touch and stay at that depth as you drag, combing grooves that hold.
                t.kind = Kind.RAKE
                t.level = max(RAKE_FLOOR, h.y - 0.1f)
                t.px = h.x; t.pz = h.z
                t.balls = List(10) { SphereCollider(h.x, h.y, h.z, 0.065f).apply { sculpt = true } }
                body.colliders += t.balls
                placeRake(t)
                Audio.play(Snd.SQUISH, 0.45f, 1.25f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.5f)
            }
            Tool.STAMP -> if (touches.values.none { it.kind == Kind.STAMP }) planeY(0.3f)?.takeIf { hypot(it[0], it[2]) < 2.3f }?.let { p ->
                body.extent(ext)
                t.kind = Kind.STAMP
                t.level = ext[0] + 0.05f
                t.px = clampR(p[0], p[2], 1.3f, 0); t.pz = clampR(p[0], p[2], 1.3f, 2)
                t.angle = starAngle()
                t.balls = buildList {
                    add(SphereCollider(0f, 0f, 0f, 0.11f))
                    for (k in 0 until 5) for (d in 1..3) add(SphereCollider(0f, 0f, 0f, 0.1f - d * 0.016f))
                }
                t.balls.forEach { it.sculpt = true }
                body.colliders += t.balls
                placeStamp(t)
            }
            Tool.CUTTER -> if (touches.values.none { it.kind == Kind.CUTTER }) planeY(0.3f)?.takeIf { hypot(it[0], it[2]) < 2.3f }?.let { p ->
                body.extent(ext)
                t.kind = Kind.CUTTER
                t.level = ext[0] + 0.05f
                t.px = clampR(p[0], p[2], 1.2f, 0); t.pz = clampR(p[0], p[2], 1.2f, 2)
                t.angle = yaw
                t.balls = List(HEART.size / 2) { SphereCollider(0f, 0f, 0f, 0.055f).apply { sculpt = true } }
                body.colliders += t.balls
                placeCutter(t)
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
            Tool.DRYER -> {
                body.extent(ext)
                t.level = ext[0] + 0.95f
                planeY(t.level)?.let { p ->
                    t.kind = Kind.DRYER
                    t.px = clampR(p[0], p[2], 1.9f, 0); t.py = t.level; t.pz = clampR(p[0], p[2], 1.9f, 2)
                    Audio.play(Snd.HISS, 0.35f, 1.5f, pan(x))
                    Haptics.buzz(Haptics.Kind.TICK, 0.4f)
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
            Tool.WHISK -> planeY(0.3f)?.takeIf { hypot(it[0], it[2]) < 2.3f }?.let { p ->
                t.kind = Kind.WHISK
                t.px = clampR(p[0], p[2], 1.3f, 0); t.pz = clampR(p[0], p[2], 1.3f, 2)
                t.balls = List(9) { SphereCollider(0f, 0f, 0f, 0.07f) }
                body.colliders += t.balls
                placeWhisk(t)
                Audio.play(Snd.SWISH, 0.5f, 1.2f, pan(x))
            }
            Tool.SQUEEZE -> if (touches.values.none { it.kind == Kind.SQUEEZE }) {
                body.extent(ext)
                val h = body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])
                t.kind = Kind.SQUEEZE
                t.px = h?.x ?: body.cx; t.py = h?.y ?: ext[0]; t.pz = h?.z ?: body.cz
                // The fingertips are the colliders; they switch on once the hand has arrived.
                t.balls = List(5) { SphereCollider(t.px, t.py + 1f, t.pz, 0.1f).apply { active = false } }
                body.colliders += t.balls
                Audio.play(Snd.SQUISH, 0.5f, 0.75f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.5f)
            }
            Tool.SCOOP -> {
                body.extent(ext)
                t.kind = Kind.SCOOP
                t.level = ext[0] + 0.6f
                val d = if (playing) hitDroplet() else null
                val h = if (d == null) body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2]) else null
                when {
                    d != null -> {
                        d.held = true; t.drop = d
                        t.px = d.x; t.py = d.y; t.pz = d.z
                        Audio.play(Snd.BLOP, 0.6f, 1.1f, pan(x))
                        stat(Stat.SCOOPS)
                    }
                    h != null -> {
                        t.px = h.x; t.py = h.y; t.pz = h.z
                        body.impulseAt(h.x, h.y, h.z, 0.6f, 0f, 1.8f, 0f)
                        spawnDroplet(h.x, h.y, h.z, 0f, 0f, 0f, 0.04f + rnd.nextFloat() * 0.015f)?.let { nd ->
                            nd.held = true; t.drop = nd
                            stat(Stat.SCOOPS)
                        }
                        Audio.play(Snd.SQUISH, 0.6f, 0.9f, pan(x))
                        Audio.play(Snd.RELEASE, 0.5f, 0.9f, pan(x), 80)
                        Haptics.buzz(Haptics.Kind.TICK, 0.6f)
                    }
                    else -> planeY(t.level)?.let { p -> t.px = clampR(p[0], p[2], 2f, 0); t.py = t.level; t.pz = clampR(p[0], p[2], 2f, 2) }
                }
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
            Kind.RAKE -> planeY(t.level)?.let { p ->
                val nx = clampR(p[0], p[2], 2f, 0); val nz = clampR(p[0], p[2], 2f, 2)
                t.rolled += hypot(nx - t.px, nz - t.pz)
                t.px = nx; t.pz = nz
                placeRake(t)
            }
            Kind.SHAKER, Kind.MAGNET, Kind.DRYER, Kind.SCOOP -> planeY(t.level)?.let { p ->
                val r = when (t.kind) { Kind.MAGNET -> 1.8f; Kind.DRYER -> 1.9f; else -> 2f }
                t.px = clampR(p[0], p[2], r, 0); t.pz = clampR(p[0], p[2], r, 2)
            }
            Kind.SQUEEZE -> planeY(t.py)?.let { p -> t.px = clampR(p[0], p[2], 1.6f, 0); t.pz = clampR(p[0], p[2], 1.6f, 2) }
            Kind.WHISK -> planeY(0.3f)?.let { p -> t.px = clampR(p[0], p[2], 1.3f, 0); t.pz = clampR(p[0], p[2], 1.3f, 2) }
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
            // A blister stays blown up when you let go: tap it to pop it. A quick tap leaves nothing.
            Kind.BLISTER -> t.blister?.let { b -> if (b.size < 0.06f) body.popBlister(b) }
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
                    repeat(12) {
                        val p = spawn(t.px, ext[0] + 0.1f, t.pz, 1.4f, 0.08f)
                        p.shape = 1f
                        p.r = 1f; p.g = 0.85f + rnd.nextFloat() * 0.15f; p.b = 0.4f + rnd.nextFloat() * 0.5f
                    }
                }
            }
            Kind.CUTTER -> {
                body.colliders.removeAll(t.balls.toSet())
                body.extent(ext)
                if (t.counted) {
                    stat(Stat.CUTS)
                    Audio.play(Snd.RELEASE, 0.7f, 1f, pan(t.x))
                    Audio.play(Snd.PLIP, 0.6f, 1.2f, pan(t.x))
                    Haptics.buzz(Haptics.Kind.POP, 0.6f)
                    // The cut-out heart pops up out of the slime and hops away.
                    spawnDroplet(t.px, ext[0] * 0.8f, t.pz, -camX * 0.6f, 2.4f, -camZ * 0.6f, 0.035f)
                    repeat(10) {
                        val p = spawn(t.px, ext[0] + 0.15f, t.pz, 1.3f, 0.09f)
                        p.shape = 2f
                        p.r = 1f; p.g = 0.4f + rnd.nextFloat() * 0.3f; p.b = 0.6f + rnd.nextFloat() * 0.3f
                    }
                }
            }
            Kind.WHISK -> {
                body.colliders.removeAll(t.balls.toSet())
                Audio.play(Snd.RELEASE, 0.4f, 1.3f, pan(t.x))
            }
            Kind.SQUEEZE -> {
                body.colliders.removeAll(t.balls.toSet())
                Audio.play(Snd.RELEASE, 0.6f, 0.75f, pan(t.x))
                if (t.press > 0.4f) stat(Stat.SQUEEZES)
            }
            Kind.SCOOP -> t.drop?.let { d ->
                // Tipped out: it plops down, sits a while, then crawls home like any other droplet.
                d.held = false
                d.vy = 0.4f; d.vx = 0f; d.vz = 0f
                d.wait = d.age + 2f + rnd.nextFloat() * 1.5f
                Audio.play(Snd.BLOP, 0.5f, 0.9f, pan(t.x))
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

    /** Rake: five tines in a row across the screen, two balls deep each, at the depth they first sank to. */
    private fun placeRake(t: Touch) {
        val ax = cos(yaw); val az = -sin(yaw)
        for (i in 0 until 5) {
            val o = (i - 2) * RAKE_GAP
            val lo = t.balls[i * 2]; val hi = t.balls[i * 2 + 1]
            lo.x = t.px + ax * o; lo.y = t.level + lo.r * 0.5f; lo.z = t.pz + az * o
            hi.x = lo.x; hi.y = lo.y + 0.12f; hi.z = lo.z
        }
    }

    /** The star's points face the camera. */
    private fun starAngle() = atan2(-cos(yaw), -sin(yaw))

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

    /** Heart cutter: a ring of balls in a heart, lobes away from the camera, pressing down from [Touch.level]. */
    private fun placeCutter(t: Touch) {
        val rx = cos(t.angle); val rz = -sin(t.angle)
        val fx = -sin(t.angle); val fz = -cos(t.angle)
        for ((i, b) in t.balls.withIndex()) {
            val hx = HEART[i * 2]; val hy = HEART[i * 2 + 1]
            b.x = t.px + rx * hx + fx * hy; b.y = t.level + b.r; b.z = t.pz + rz * hx + fz * hy
        }
    }

    /** Whisk: three wire loops spinning round, the colliders along the bottom of each. */
    private fun placeWhisk(t: Touch) {
        var i = 0
        for (k in 0 until 3) {
            val a = t.angle + k * PI.toFloat() / 3f
            val dx = cos(a); val dz = sin(a)
            for (j in -1..1) {
                val psi = j * PI.toFloat() / 3f
                val b = t.balls[i++]
                b.x = t.px + dx * 0.17f * sin(psi); b.y = WHISK_Y + 0.27f - 0.27f * cos(psi); b.z = t.pz + dz * 0.17f * sin(psi)
            }
        }
    }

    // ---- slap and hammer ----

    /** A slap or a hammer blow on its way down: it lands a moment after the tap, where you tapped. */
    private class Swing(val tool: Tool, val panX: Float) {
        val from = FloatArray(3)
        val at = FloatArray(3)
        var t = 0f
        var hit = false
        var after = 0f
        var dx = 0f; var dz = 0f
        var drop: Droplet? = null
        var blister: Blister? = null
        var onBody = false
        val dur get() = if (tool == Tool.HAMMER) 0.13f else 0.09f
    }

    private var swing: Swing? = null

    private fun swingAt(t: Touch, x: Float, tl: Tool) {
        t.kind = Kind.DONE
        // Tapping faster than the hand can swing: land the last one now.
        swing?.let { if (!it.hit) { it.hit = true; impact(it) } }
        val s = Swing(tl, x)
        s.dx = rd[0]; s.dz = rd[2]
        val d = hitDroplet()
        if (d != null) {
            s.drop = d
            s.at[0] = d.x; s.at[1] = d.y + d.r * 0.5f; s.at[2] = d.z
        } else {
            val h = body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2])
            if (h != null) {
                s.onBody = true
                s.at[0] = h.x; s.at[1] = h.y; s.at[2] = h.z
                body.patternAt(h.tri, h.u, h.v, pat)
                s.blister = body.blisterAt(pat[0], pat[1], pat[2])
            } else {
                val p = planeY(0.02f) ?: return
                if (hypot(p[0], p[2]) > 2.6f) return
                p.copyInto(s.at)
            }
        }
        if (hand.visible) hand.pos.copyInto(s.from) else { s.from[0] = s.at[0]; s.from[1] = s.at[1] + 1f; s.from[2] = s.at[2] }
        if (tl == Tool.HAMMER) s.from[1] += 0.3f
        swing = s
        Audio.play(Snd.SWISH, 0.3f, if (tl == Tool.HAMMER) 1.1f else 1.5f, pan(x), 40)
    }

    private fun impact(s: Swing) {
        val x = s.panX
        val (ax, ay, az) = s.at
        val hammer = s.tool == Tool.HAMMER
        val d = s.drop
        val b = s.blister
        when {
            d != null && d in droplets -> {
                if (d.vol >= 0.024f) splitDroplet(d) else { d.squash = 1f; d.squashVel = 0f }
                Audio.play(if (hammer) Snd.SQUEAK else Snd.SLAP, 0.7f, 1.2f, pan(x), 40)
                Audio.play(Snd.PLIP, 0.5f, 0.9f, pan(x), 40)
                Haptics.buzz(Haptics.Kind.THUD, 0.5f)
                if (hammer) stat(Stat.BONKS) else stat(Stat.SLAPS)
            }
            b != null && b in body.blisters -> {
                popBlister(b, x)
                if (hammer) { Audio.play(Snd.SQUEAK, 0.7f, 1f, pan(x), 0); stat(Stat.BONKS) } else stat(Stat.SLAPS)
            }
            s.onBody -> if (hammer) bonk(ax, ay, az, x) else slapBody(s, ax, ay, az, x)
            else -> {
                Audio.play(if (hammer) Snd.SQUEAK else Snd.TAP, 0.5f, 0.8f, pan(x))
                Haptics.buzz(Haptics.Kind.TICK, 0.4f)
            }
        }
    }

    private fun slapBody(s: Swing, x: Float, y: Float, z: Float, px: Float) {
        body.impulseAt(x, y, z, 0.85f, s.dx * 2f, -4.5f, s.dz * 2f)
        body.impulseAt(body.cx, body.cy, body.cz, 2.5f, 0f, 0.6f, 0f)
        heat = min(1f, heat + 0.04f)
        Audio.play(Snd.SLAP, 1f, 0.85f + rnd.nextFloat() * 0.3f, pan(px), 60)
        Audio.play(Snd.SQUISH, 0.5f, 0.8f, pan(px), 60)
        Haptics.buzz(Haptics.Kind.THUD, 1f)
        stat(Stat.SLAPS)
        val c = rgb(colorOf(recipe.colors.first()).argb)
        repeat(10) {
            val p = spawn(x, y + 0.05f, z, 2f, 0.06f)
            p.shape = 0f
            p.r = c[0]; p.g = c[1]; p.b = c[2]
        }
        // Slap it again and again and bits fly off.
        slapEnergy += 1f
        if (slapEnergy > 2.2f && rnd.nextFloat() < 0.45f + (slapEnergy - 2.2f) * 0.2f) {
            splash(x, y, z, 1 + (if (slapEnergy > 4f) rnd.nextInt(2) else 0), 1f)
        }
    }

    /** The squeaky hammer: a dent, a wobble, little stars, and now and then a blob knocked off. */
    private fun bonk(x: Float, y: Float, z: Float, px: Float) {
        body.burstAt(x, y, z, 0.6f, 1.6f)
        body.impulseAt(x, y, z, 0.75f, 0f, -5.5f, 0f)
        body.impulse(0f, 0.5f, 0f)
        heat = min(1f, heat + 0.05f)
        Audio.play(Snd.SQUEAK, 0.9f, 0.9f + rnd.nextFloat() * 0.25f, pan(px), 40)
        Audio.play(Snd.BONK, 0.7f, 0.9f + rnd.nextFloat() * 0.2f, pan(px), 40)
        Audio.play(Snd.SQUISH, 0.4f, 0.7f, pan(px), 60)
        Haptics.buzz(Haptics.Kind.THUD, 1f)
        stat(Stat.BONKS)
        repeat(7) {
            val p = spawn(x, y + 0.25f, z, 1.6f, 0.11f)
            p.shape = 1f; p.gravity = 0.8f
            p.r = 1f; p.g = 0.88f; p.b = 0.3f
        }
        slapEnergy += 1.2f
        if (rnd.nextFloat() < 0.3f + max(0f, slapEnergy - 2f) * 0.2f) splash(x, y, z, 1 + rnd.nextInt(2), 1.15f)
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
        flash(p[0], p[1] + 0.3f, p[2])
        Audio.play(Snd.BLOW, 0.6f, 1.3f, pan(x))
        Audio.play(Snd.BLOP, 0.4f, 1.5f, pan(x))
        Haptics.buzz(Haptics.Kind.TICK, 0.5f)
        stat(Stat.WANDS)
    }

    /** The hand darts to (x, y, z) for a moment (a poke at a droplet, a wave of the wand). */
    private fun flash(x: Float, y: Float, z: Float) {
        flashT = 0.35f
        flashP[0] = x; flashP[1] = y; flashP[2] = z
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
        // A hard snap flings a blob off the end.
        if (t.pull > body.mat.maxStretch * 0.95f && rnd.nextFloat() < 0.5f) {
            spawnDroplet(g.tx, g.ty, g.tz, (g.tx - g.ax) * 1.5f, 1.5f, (g.tz - g.az) * 1.5f, 0.025f)
        }
    }

    // ---- air blisters ----

    /** The blister under the last ray, if any (its top goes in [hp]). */
    private fun blisterUnderRay(): Blister? {
        if (body.blisters.isEmpty()) return null
        val h = body.raycast(ro[0], ro[1], ro[2], rd[0], rd[1], rd[2]) ?: return null
        body.patternAt(h.tri, h.u, h.v, pat)
        val b = body.blisterAt(pat[0], pat[1], pat[2]) ?: return null
        hp[0] = h.x; hp[1] = h.y; hp[2] = h.z
        return b
    }

    /** Pop! The air rushes out of [b] in a jet, the skin slaps back in, a few soap bubbles and maybe a blob fly off. */
    private fun popBlister(b: Blister, x: Float) {
        react(Mood.SHOCKED, 0.9f)
        body.blisterTop(b, hp)
        val big = b.size / Blister.MAX
        body.popBlister(b)
        val nx = b.nx; val ny = b.ny; val nz = b.nz
        body.impulseAt(hp[0], hp[1], hp[2], 0.75f, -nx * 2.5f * big, -ny * 2.5f * big, -nz * 2.5f * big)
        body.impulse(0f, 0.3f * big, 0f)
        Audio.play(Snd.BIG_POP, 0.6f + 0.4f * big, 1.2f - big * 0.35f, pan(x), 0)
        Audio.play(Snd.HISS, 0.3f + 0.5f * big, 1.1f - big * 0.2f, pan(x), 0)
        Haptics.buzz(Haptics.Kind.POP, 0.6f + 0.4f * big)
        stat(Stat.BLISTERS)
        repeat((10 + 26 * big).toInt()) {
            val p = spawn(hp[0], hp[1], hp[2], 0f, 0.1f)
            val sp = 2f + rnd.nextFloat() * 2.5f * (0.5f + big)
            p.vx = (nx + (rnd.nextFloat() - 0.5f) * 0.7f) * sp
            p.vy = (ny + (rnd.nextFloat() - 0.5f) * 0.7f) * sp + 0.4f
            p.vz = (nz + (rnd.nextFloat() - 0.5f) * 0.7f) * sp
            p.gravity = -0.4f
            p.shape = 0f
            p.max = 0.5f + rnd.nextFloat() * 0.4f; p.life = p.max
            p.r = 0.9f; p.g = 0.95f; p.b = 1f
        }
        repeat(1 + (big * 3).toInt()) {
            val bb = Bubble(rnd.nextFloat())
            bb.growing = false
            bb.x = hp[0] + nx * 0.15f + (rnd.nextFloat() - 0.5f) * 0.3f
            bb.y = hp[1] + ny * 0.15f + rnd.nextFloat() * 0.2f
            bb.z = hp[2] + nz * 0.15f + (rnd.nextFloat() - 0.5f) * 0.3f
            bb.r = 0.06f + rnd.nextFloat() * 0.1f * (0.5f + big)
            bb.vy = 0.4f
            bubbles += bb
        }
        if (playing && big > 0.5f) splash(hp[0], hp[1], hp[2], 1 + rnd.nextInt(2), 0.9f)
    }

    private fun updateBlisters(dt: Float) {
        val blowing = touches.values.mapNotNull { it.blister.takeIf { _ -> it.kind == Kind.BLISTER } }
        var burst: Blister? = null
        for (b in body.blisters) {
            if (b in blowing) {
                body.blow(b, dt * 0.4f * (1f - 0.4f * b.size / Blister.MAX))
                if (b.size >= Blister.MAX - 1e-3f) burst = b
            } else if (b.size > 0f) {
                // Air slowly seeps back out of one left alone.
                body.blow(b, -dt * 0.012f)
            }
        }
        body.blisters.firstOrNull { it.size <= 0.02f && it !in blowing && it !== burst }?.let { body.popBlister(it) }
        burst?.let { b ->
            val t = touches.values.firstOrNull { it.blister === b }
            popBlister(b, t?.x ?: (width / 2f))
            t?.kind = Kind.DONE
        }
    }

    // ---- droplets ----

    /** The droplet under the last ray, if any. */
    private fun hitDroplet(): Droplet? {
        var best: Droplet? = null
        var bt = Float.MAX_VALUE
        for (d in droplets) {
            if (d.held) continue
            val ox = d.x - ro[0]; val oy = d.y - ro[1]; val oz = d.z - ro[2]
            val s = ox * rd[0] + oy * rd[1] + oz * rd[2]
            val d2 = ox * ox + oy * oy + oz * oz - s * s
            val rr = d.r * 1.3f + 0.1f
            if (s > 0f && d2 < rr * rr && s < bt) { bt = s; best = d }
        }
        return best
    }

    /** A poke at a droplet: it squishes flat and springs back, and scoots a little. */
    private fun prod(d: Droplet, x: Float) {
        d.squash = 1f; d.squashVel = 0f
        d.vx += -camX * 0.5f + (rnd.nextFloat() - 0.5f) * 0.4f
        d.vz += -camZ * 0.5f + (rnd.nextFloat() - 0.5f) * 0.4f
        d.vy += 0.6f
        d.wait = max(d.wait, d.age + 1.2f)
        flash(d.x, d.y + d.r * 0.6f, d.z)
        Audio.play(Snd.SQUISH, 0.5f, 1.4f, pan(x), 40)
        Audio.play(Snd.PLIP, 0.35f, 1.3f, pan(x), 40)
        Haptics.buzz(Haptics.Kind.TICK, 0.5f)
        stat(Stat.POKES)
    }

    /**
     * Breaks a blob of [frac] of the slime off at (x, y, z), flying at (vx, vy, vz). Null when there's too little
     * left here or too many droplets about.
     */
    private fun spawnDroplet(x: Float, y: Float, z: Float, vx: Float, vy: Float, vz: Float, frac: Float): Droplet? {
        if (!playing || droplets.size >= MAX_DROPLETS) return null
        val f = min(frac, body.amount - SoftBody.MIN_AMOUNT)
        if (f < 0.012f) return null
        val full = body.fullVolume
        body.amount -= f
        val d = Droplet(f, rnd.nextFloat())
        d.r = cbrt(3f * f * full / (4f * PI.toFloat()))
        d.x = x; d.y = max(y, d.r); d.z = z
        d.vx = vx; d.vy = vy; d.vz = vz
        d.wait = 2.5f + rnd.nextFloat() * 2.5f
        droplets += d
        stat(Stat.SPLATS)
        return d
    }

    /** [n] blobs fly off outwards from (x, y, z). */
    private fun splash(x: Float, y: Float, z: Float, n: Int, power: Float) {
        react(Mood.SHOCKED, 1.4f)
        var dx = x - body.cx; var dz = z - body.cz
        val l = hypot(dx, dz)
        if (l < 0.1f) { val a = rnd.nextFloat() * 6.28f; dx = cos(a); dz = sin(a) } else { dx /= l; dz /= l }
        var made = 0
        repeat(n) {
            val a = (rnd.nextFloat() - 0.5f) * 1.6f
            val ex = dx * cos(a) - dz * sin(a); val ez = dx * sin(a) + dz * cos(a)
            val sp = (1.4f + rnd.nextFloat() * 1.2f) * power
            spawnDroplet(x + ex * 0.15f, y + 0.12f, z + ez * 0.15f, ex * sp, (2.2f + rnd.nextFloat() * 1.2f) * power, ez * sp, 0.03f + rnd.nextFloat() * 0.03f)?.let { made++ }
        }
        if (made > 0) {
            Audio.play(Snd.SQUISH, 0.7f, 0.7f, 0f, 30)
            Audio.play(Snd.PLIP, 0.5f, 0.8f, 0f, 30)
        }
    }

    /** Slapped or bonked: a droplet big enough breaks in two. */
    private fun splitDroplet(d: Droplet) {
        droplets.remove(d)
        val a = rnd.nextFloat() * 6.28f
        for (k in 0..1) {
            val sgn = if (k == 0) 1f else -1f
            val h = Droplet(d.vol / 2f, rnd.nextFloat())
            h.r = d.r / cbrt(2f)
            h.x = d.x + cos(a) * sgn * h.r; h.y = d.y; h.z = d.z + sin(a) * sgn * h.r
            h.vx = cos(a) * sgn * 1.6f; h.vy = 1.6f; h.vz = sin(a) * sgn * 1.6f
            h.wait = d.wait.coerceAtLeast(d.age + 2f) - d.age
            h.squash = 0.8f
            droplets += h
        }
    }

    /** It's back: the slime grows by the droplet and ripples where it joined. */
    private fun merge(d: Droplet) {
        body.amount += d.vol
        body.impulseAt(d.x, d.y, d.z, 0.8f, (body.cx - d.x) * 0.5f, -0.9f, (body.cz - d.z) * 0.5f)
        Audio.play(Snd.PLIP, 0.45f, 0.75f + rnd.nextFloat() * 0.4f, 0f, 50)
        Audio.play(Snd.SQUISH, 0.25f, 1.2f, 0f, 50)
        Haptics.buzz(Haptics.Kind.TICK, 0.25f)
        stat(Stat.MERGES)
    }

    /** The inside of the container's wall at (x, y, z). */
    private fun wallAt(x: Float, y: Float, z: Float): Float =
        if (body.vessel == Vessel.DISH) SoftBody.DISH_R * Shapes.plateR(body.plate, atan2(z, x)) else Shapes.vesselR(body.vessel, y)

    private fun updateDroplets(dt: Float) {
        if (droplets.isEmpty()) return
        body.extent(ext)
        val gone = ArrayList<Droplet>()
        for (d in droplets) {
            d.age += dt
            // Wobble: squash springs back to round.
            d.squashVel += (-d.squash * 110f - d.squashVel * 9f) * dt
            d.squash = (d.squash + d.squashVel * dt).coerceIn(-0.6f, 1f)
            if (d.held) continue
            val home = d.age > d.wait
            d.vy -= 7f * dt
            if (home && d.onFloor) {
                // Inching home: little pushes, stretching long as it goes and bunching up between.
                val dx = body.cx - d.x; val dz = body.cz - d.z
                val l = max(1e-3f, hypot(dx, dz))
                val pulse = max(0f, sin(d.age * 5.5f + d.seed * 6f))
                val hurry = min(3f, 1f + (d.age - d.wait) * 0.12f)
                val sp = 0.45f * pulse * pulse * hurry
                d.vx += (dx / l * sp - d.vx) * min(1f, dt * 10f)
                d.vz += (dz / l * sp - d.vz) * min(1f, dt * 10f)
                d.squash += ((0.25f - 0.6f * pulse) - d.squash) * min(1f, dt * 8f)
            } else if (d.onFloor) {
                val f = exp(-5f * dt)
                d.vx *= f; d.vz *= f
            }
            d.x += d.vx * dt; d.y += d.vy * dt; d.z += d.vz * dt
            val bottom = d.r * (1f - 0.45f * d.squash.coerceAtLeast(0f))
            if (d.y < bottom) {
                if (d.vy < -1.5f) {
                    d.squash = min(1f, -d.vy * 0.18f)
                    d.squashVel = 0f
                    Audio.play(Snd.PLIP, min(0.5f, -d.vy * 0.08f), 0.9f + rnd.nextFloat() * 0.5f, 0f, 60)
                }
                d.y = bottom
                d.vy = if (d.vy < -2.5f) -d.vy * 0.2f else 0f
            }
            d.onFloor = d.y <= bottom + 0.02f
            val w = wallAt(d.x, d.y, d.z) - d.r
            val hr = hypot(d.x, d.z)
            if (hr > w && hr > 1e-3f) {
                val nx = d.x / hr; val nz = d.z / hr
                d.x = nx * w; d.z = nz * w
                val vn = d.vx * nx + d.vz * nz
                if (vn > 0f) { d.vx -= vn * nx * 1.3f; d.vz -= vn * nz * 1.3f }
            }
            // Touching the slime again (falling back on, or crawled home): in it goes.
            val hd = hypot(d.x - body.cx, d.z - body.cz)
            if (d.age > 0.6f && d.vy <= 0.2f && hd < ext[1] * 0.85f + d.r * 0.3f && d.y < ext[0] + d.r * 0.4f) gone += d
        }
        for (d in gone) { droplets.remove(d); merge(d) }
        // Two droplets that bump together join up.
        for (i in droplets.indices) for (j in i + 1 until droplets.size) {
            val a = droplets[i]; val b = droplets[j]
            if (a.held || b.held || a.age < 0.5f || b.age < 0.5f) continue
            if (sq(a.x - b.x) + sq(a.y - b.y) + sq(a.z - b.z) < sq((a.r + b.r) * 0.75f)) {
                val w = a.vol / (a.vol + b.vol)
                a.x = a.x * w + b.x * (1 - w); a.z = a.z * w + b.z * (1 - w)
                a.r = cbrt(a.r * a.r * a.r + b.r * b.r * b.r)
                a.vol += b.vol
                a.squash = 0.6f
                b.vol = 0f
                Audio.play(Snd.PLIP, 0.35f, 1.1f, 0f, 50)
                droplets.removeAt(j)
                return
            }
        }
    }

    /** Sends every droplet straight home ("Ball up"). */
    private fun callDroplets() {
        for (d in droplets) { d.wait = min(d.wait, d.age); d.age = max(d.age, d.wait + 12f) }
    }

    // ---- tools in the hand ----

    private fun updateToys(dt: Float) {
        val out = ArrayList<Prop>()
        body.extent(ext)
        flashT -= dt
        slapEnergy = max(0f, slapEnergy - dt * 0.7f)
        var aimed = false
        for (t in touches.values) {
            val first = !aimed
            when (t.kind) {
                Kind.FINGER -> t.finger?.let { c ->
                    if (!first) return@let
                    aimed = true
                    var fx = c.x - eye[0]; var fy = c.y - eye[1] - 2.5f; var fz = c.z - eye[2]
                    val l = sqrt(fx * fx + fy * fy + fz * fz); fx /= l; fy /= l; fz /= l
                    val o = c.r - 0.07f
                    hand.aim(Pose.POINT, Grip.TIP, c.x + fx * o, c.y + fy * o, c.z + fz * o, fx, fy, fz, camX, 1f, camZ, 40f)
                }
                Kind.GRAB -> t.grab?.let { g ->
                    if (!first) return@let
                    aimed = true
                    var fx = g.tx - eye[0]; var fy = g.ty - eye[1] - 2f; var fz = g.tz - eye[2]
                    val l = sqrt(fx * fx + fy * fy + fz * fz); fx /= l; fy /= l; fz /= l
                    hand.aim(Pose.PINCH, Grip.PINCH, g.tx, g.ty, g.tz, fx, fy, fz, camX, 1f, camZ, 30f)
                }
                Kind.PALM -> if (first) {
                    aimed = true
                    hand.aim(Pose.OPEN, Grip.PALM, palm.x, palm.y + 0.1f, palm.z, -camX, 0f, -camZ, 0f, 1f, 0f, 25f)
                }
                Kind.PIN -> if (first) {
                    aimed = true
                    hand.aim(Pose.OPEN, Grip.PALM, pin.x, pin.y + pin.r + 0.1f, pin.z, -camX, 0f, -camZ, 0f, 1f, 0f, 25f)
                }
                Kind.STIR -> t.finger?.let { c -> if (drawTool(Tool.STIR, c.x, c.y, c.z, out) && first) { aimed = true; hold(25f) } }
                Kind.BLISTER -> t.blister?.let { b ->
                    if (b !in body.blisters) { t.kind = Kind.DONE; return@let }
                    body.blisterTop(b, hp)
                    t.clock += dt
                    if (t.clock - t.tickTurn > 0.45f) {
                        t.tickTurn = t.clock
                        Audio.play(Snd.BLOW, 0.4f, 1.15f - b.size * 0.4f, pan(t.x), 300)
                        Haptics.buzz(Haptics.Kind.TICK, 0.2f)
                    }
                    if (drawTool(Tool.BUBBLE, hp[0], hp[1], hp[2], out, nx = b.nx, ny = b.ny, nz = b.nz) && first) { aimed = true; hold(25f) }
                }
                Kind.RAKE -> {
                    t.level = max(RAKE_FLOOR, t.level - dt * 0.3f)
                    placeRake(t)
                    if (drawTool(Tool.RAKE, t.px, t.level, t.pz, out) && first) { aimed = true; hold(30f) }
                }
                Kind.STAMP -> {
                    t.level = max(0.12f, t.level - dt * 1.4f)
                    placeStamp(t)
                    if (!t.counted && t.level < ext[0] - 0.12f && t.balls.sumOf { it.contacts } > 30) t.counted = true
                    if (drawTool(Tool.STAMP, t.px, t.level, t.pz, out, spin = t.angle) && first) { aimed = true; hold(30f) }
                }
                Kind.CUTTER -> {
                    t.level = max(0.1f, t.level - dt * 1.3f)
                    placeCutter(t)
                    if (!t.counted && t.level < ext[0] - 0.15f && t.balls.sumOf { it.contacts } > 25) t.counted = true
                    if (drawTool(Tool.CUTTER, t.px, t.level, t.pz, out, spin = t.angle) && first) { aimed = true; hold(30f) }
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
                    val shake = sin(t.clock * 30f) * 0.04f
                    if (drawTool(Tool.SHAKER, t.px, t.py + shake, t.pz, out) && first) { aimed = true; hold(30f) }
                }
                Kind.MAGNET -> {
                    t.clock += dt
                    val m = body.mat
                    val pull = 24f * (if (recipe.base == Base.MAGNETIC) 2f else 1f + m.metal * 0.5f)
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
                    if (drawTool(Tool.MAGNET, t.px, t.py, t.pz, out) && first) { aimed = true; hold(30f) }
                }
                Kind.WHISK -> {
                    t.clock += dt
                    t.angle += dt * 13f
                    placeWhisk(t)
                    mix = min(0.8f, mix + dt * 0.05f)
                    heat = min(1f, heat + dt * 0.12f)
                    if (t.clock - t.tickTurn > 0.24f) {
                        t.tickTurn = t.clock
                        Audio.play(Snd.SWISH, 0.35f, 1.3f + rnd.nextFloat() * 0.3f, pan(t.x), 150)
                        Haptics.buzz(Haptics.Kind.TICK, 0.2f)
                    }
                    if (t.clock.toInt() > t.ticks) { t.ticks = t.clock.toInt(); stat(Stat.WHISKS) }
                    if (drawTool(Tool.WHISK, t.px, WHISK_Y, t.pz, out, spin = t.angle) && first) { aimed = true; hold(30f) }
                }
                Kind.DRYER -> {
                    t.clock += dt
                    dry(t, dt)
                    if (t.clock.toInt() > t.ticks) { t.ticks = t.clock.toInt(); stat(Stat.DRYS) }
                    val buzz = sin(t.clock * 60f) * 0.006f
                    if (drawTool(Tool.DRYER, t.px, t.py + buzz, t.pz, out) && first) { aimed = true; hold(30f) }
                }
                Kind.SQUEEZE -> {
                    t.clock += dt
                    t.press = min(1f, t.press + dt * 0.9f)
                    if (first) {
                        aimed = true
                        hand.aim(Pose.CLAW, Grip.CLAW, t.px, t.py - 0.05f - 0.22f * t.press, t.pz, -camX * 0.3f, -1f, -camZ * 0.3f, camX, 0.2f, camZ, 18f, t.press)
                    }
                    if (t.clock - t.tickTurn > 0.35f) {
                        t.tickTurn = t.clock
                        Audio.play(Snd.SQUISH, 0.3f + t.press * 0.3f, 0.7f + t.press * 0.3f, pan(t.x), 120)
                        Haptics.buzz(Haptics.Kind.TICK, 0.2f + t.press * 0.3f)
                    }
                    // Squeezed hard, it squirts out between the fingers.
                    if (t.press > 0.6f && t.clock - t.rolled > 0.6f) {
                        t.rolled = t.clock
                        if (rnd.nextFloat() < 0.7f) {
                            val a = rnd.nextFloat() * 6.28f
                            spawnDroplet(t.px + cos(a) * 0.2f, t.py + 0.2f, t.pz + sin(a) * 0.2f, cos(a) * 1.3f, 2.6f, sin(a) * 1.3f, 0.022f + rnd.nextFloat() * 0.015f)?.let {
                                Audio.play(Snd.PLIP, 0.5f, 0.8f, pan(t.x), 40)
                            }
                        }
                    }
                }
                Kind.SCOOP -> {
                    t.press = min(1f, t.press + dt * 4f)
                    val cy = t.py + (t.level - t.py) * SoftBody.smooth(t.press)
                    val d = t.drop
                    val size = max(1f, (d?.r ?: 0f) * 1.3f / 0.32f)
                    if (d != null) {
                        d.x = t.px; d.y = cy - 0.3f * size + d.r * 0.85f; d.z = t.pz
                        d.vx = 0f; d.vy = 0f; d.vz = 0f
                        d.squash = max(d.squash, 0.2f)
                    }
                    if (drawTool(Tool.SCOOP, t.px, cy, t.pz, out, size = size) && first) { aimed = true; hold(30f) }
                }
                else -> Unit
            }
        }
        val s = swing
        if (s != null) {
            s.t += dt
            if (!s.hit && s.t >= s.dur) { s.hit = true; impact(s) }
            if (s.hit) s.after += dt
            val u = min(1f, s.t / s.dur)
            val lift = if (s.hit) 0.3f * min(1f, s.after / 0.12f) else sin(PI.toFloat() * u) * 0.35f
            val e = u * u
            val x = s.from[0] + (s.at[0] - s.from[0]) * e
            val y = s.from[1] + (s.at[1] - s.from[1]) * e + lift
            val z = s.from[2] + (s.at[2] - s.from[2]) * e
            if (!aimed) {
                aimed = true
                if (s.tool == Tool.SLAP) hand.aim(Pose.OPEN, Grip.PALM, x, y + 0.09f, z, -camX, 0f, -camZ, 0f, 1f, 0f, 70f)
                else if (drawTool(Tool.HAMMER, x, y, z, out)) hold(70f)
            }
            if (s.after > 0.4f) swing = null
        }
        val show = playing && tool != Tool.NONE
        if (show && !aimed) idleHand(out)
        if (show) hand.update(dt, out) else hand.hide()
        // The squeezing hand's fingertips are what press the slime.
        for (t in touches.values) if (t.kind == Kind.SQUEEZE) {
            for ((i, b) in t.balls.withIndex()) {
                hand.tip(i, hp)
                b.x = hp[0]; b.y = max(0.06f, hp[1]); b.z = hp[2]
                b.active = t.clock > 0.15f
            }
        }
        props = out
    }

    /** The hand waits beside the slime, bobbing gently, holding the tool you picked. */
    private fun idleHand(out: MutableList<Prop>) {
        if (flashT > 0f) {
            hand.aim(Pose.POINT, Grip.TIP, flashP[0], flashP[1], flashP[2], -camX * 0.5f, -1f, -camZ * 0.5f, camX, 0.3f, camZ, 30f)
            if (tool == Tool.WAND) drawTool(Tool.WAND, flashP[0], flashP[1] + 0.3f, flashP[2], out)
            return
        }
        val bob = sin(time * 1.7f) * 0.05f
        val r = ext[1] + 0.6f
        val x = body.cx + rgtX * r + camX * 0.35f
        val z = body.cz + rgtZ * r + camZ * 0.35f
        val y = ext[0] * 0.5f + 0.45f + bob
        val tl = tool
        val spin = when (tl) { Tool.STAMP -> starAngle(); Tool.CUTTER -> yaw; Tool.WHISK -> time * 0.8f; else -> 0f }
        if (drawTool(tl, x, y, z, out, spin = spin)) { hold(7f); return }
        val pose = when (tl) {
            Tool.POKE -> Pose.POINT
            Tool.STRETCH -> Pose.PINCH
            Tool.SQUISH, Tool.SLAP, Tool.ROLL -> Pose.OPEN
            Tool.SQUEEZE -> Pose.CLAW
            else -> Pose.REST
        }
        if (pose == Pose.OPEN) hand.aim(pose, Grip.PALM, x, y + 0.2f, z, -camX, -0.15f, -camZ, 0f, 1f, 0f, 7f)
        else hand.aim(pose, Grip.PALM, x, y + 0.25f, z, -camX * 0.6f, -0.7f, -camZ * 0.6f, camX, 0.6f, camZ, 7f)
    }

    // Where drawTool says the hand holds the tool, and the handle's direction (towards the thumb).
    private val grip = FloatArray(6)

    private fun grip(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, f: Float) {
        grip[0] = ax + (bx - ax) * f; grip[1] = ay + (by - ay) * f; grip[2] = az + (bz - az) * f
        val l = max(1e-5f, sqrt(sq(bx - ax) + sq(by - ay) + sq(bz - az)))
        grip[3] = (bx - ax) / l; grip[4] = (by - ay) / l; grip[5] = (bz - az) / l
    }

    /** Closes the hand round the handle [drawTool] set: fingers wrapping away from the camera, thumb up the handle. */
    private fun hold(rate: Float) {
        val ax = grip[3]; val ay = grip[4]; val az = grip[5]
        var fx = -camX; var fy = 0f; var fz = -camZ
        val d = fx * ax + fy * ay + fz * az
        fx -= ax * d; fy -= ay * d; fz -= az * d
        var l = sqrt(fx * fx + fy * fy + fz * fz)
        if (l < 0.1f) { fx = rgtX; fy = 0f; fz = rgtZ; l = 1f }
        fx /= l; fy /= l; fz /= l
        val nx = fy * az - fz * ay; val ny = fz * ax - fx * az; val nz = fx * ay - fy * ax
        hand.aim(Pose.HOLD, Grip.FIST, grip[0], grip[1], grip[2], fx, fy, fz, nx, ny, nz, rate)
    }

    private fun rod(out: MutableList<Prop>, ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float, r: Float, c: FloatArray) {
        out += Prop(ax, ay, az, r, c, Prop.Shape.ROD, bx, by, bz)
    }

    /**
     * Draws [tl] working at (x, y, z) (its tip, face or head) into [out] and sets where it's held ([grip]). False for
     * the bare-hand tools.
     */
    private fun drawTool(tl: Tool, x: Float, y: Float, z: Float, out: MutableList<Prop>, spin: Float = 0f, nx: Float = 0f, ny: Float = 1f, nz: Float = 0f, size: Float = 1f): Boolean {
        val rx = rgtX; val rz = rgtZ; val cx = camX; val cz = camZ
        when (tl) {
            Tool.STIR -> {
                val ex = x + cx * 0.35f + rx * 0.2f; val ey = y + 1.25f; val ez = z + cz * 0.35f + rz * 0.2f
                rod(out, x, y, z, ex, ey, ez, 0.055f, WOOD)
                out += Prop(x, y, z, 0.055f, WOOD)
                grip(x, y, z, ex, ey, ez, 0.8f)
            }
            Tool.BUBBLE -> {
                var dx = nx + cx * 0.25f; var dy = ny + 0.5f; var dz = nz + cz * 0.25f
                val l = max(1e-4f, sqrt(dx * dx + dy * dy + dz * dz)); dx /= l; dy /= l; dz /= l
                val ex = x + dx * 0.85f; val ey = y + dy * 0.85f; val ez = z + dz * 0.85f
                rod(out, x, y, z, ex, ey, ez, 0.04f, STRAW)
                rod(out, x + dx * 0.25f, y + dy * 0.25f, z + dz * 0.25f, x + dx * 0.33f, y + dy * 0.33f, z + dz * 0.33f, 0.043f, SHAKER_WHITE)
                rod(out, x + dx * 0.45f, y + dy * 0.45f, z + dz * 0.45f, x + dx * 0.53f, y + dy * 0.53f, z + dz * 0.53f, 0.043f, SHAKER_WHITE)
                grip(x, y, z, ex, ey, ez, 0.78f)
            }
            Tool.RAKE -> {
                val top = y + 0.42f
                for (i in 0 until 5) {
                    val o = (i - 2) * RAKE_GAP
                    rod(out, x + rx * o, y - 0.03f, z + rz * o, x + rx * o, top, z + rz * o, 0.035f, STEEL)
                    out += Prop(x + rx * o, y - 0.03f, z + rz * o, 0.035f, STEEL)
                }
                rod(out, x - rx * 0.3f, top, z - rz * 0.3f, x + rx * 0.3f, top, z + rz * 0.3f, 0.05f, RAKE_GREEN)
                out += Prop(x - rx * 0.3f, top, z - rz * 0.3f, 0.05f, RAKE_GREEN)
                out += Prop(x + rx * 0.3f, top, z + rz * 0.3f, 0.05f, RAKE_GREEN)
                val hx = x + cx * 0.5f; val hy = top + 0.85f; val hz = z + cz * 0.5f
                rod(out, x, top, z, hx, hy, hz, 0.05f, WOOD)
                grip(x, top, z, hx, hy, hz, 0.8f)
            }
            Tool.STAMP -> {
                out += Prop(x, y + 0.11f, z, 0.11f, STAMP_PINK)
                for (k in 0 until 5) {
                    val a = spin + k * 2f * PI.toFloat() / 5f
                    for (d in 1..3) {
                        val r = 0.1f - d * 0.016f
                        out += Prop(x + cos(a) * d * 0.13f, y + r, z + sin(a) * d * 0.13f, r, STAMP_PINK)
                    }
                }
                rod(out, x, y + 0.2f, z, x, y + 0.3f, z, 0.46f, STAMP_DARK)
                rod(out, x, y + 0.3f, z, x, y + 0.8f, z, 0.06f, WOOD)
                out += Prop(x, y + 0.85f, z, 0.12f, STAMP_PINK)
                grip(x, y + 0.3f, z, x, y + 0.8f, z, 0.6f)
            }
            Tool.CUTTER -> {
                val hx0 = cos(spin); val hz0 = -sin(spin); val fx = -sin(spin); val fz = -cos(spin)
                for (i in 0 until HEART.size / 2) {
                    val hx = HEART[i * 2]; val hy = HEART[i * 2 + 1]
                    out += Prop(x + hx0 * hx + fx * hy, y + 0.055f, z + hz0 * hx + fz * hy, 0.055f, CUTTER_PINK)
                }
                val hub = y + 0.3f
                for (i in intArrayOf(5, HEART.size / 2 - 6)) {
                    val hx = HEART[i * 2]; val hy = HEART[i * 2 + 1]
                    rod(out, x + hx0 * hx + fx * hy, y + 0.08f, z + hz0 * hx + fz * hy, x, hub, z, 0.025f, STEEL)
                }
                rod(out, x, hub, z, x, y + 0.75f, z, 0.05f, CUTTER_PINK)
                out += Prop(x, y + 0.78f, z, 0.09f, CUTTER_PINK)
                grip(x, hub, z, x, y + 0.75f, z, 0.6f)
            }
            Tool.SHAKER -> {
                rod(out, x, y - 0.08f, z, x, y + 0.24f, z, 0.13f, SHAKER_WHITE)
                out += Prop(x, y + 0.25f, z, 0.11f, STEEL)
                grip(x, y - 0.05f, z, x, y + 0.25f, z, 0.45f)
            }
            Tool.MAGNET -> {
                val lx = x - rx * 0.13f; val lz = z - rz * 0.13f
                val qx = x + rx * 0.13f; val qz = z + rz * 0.13f
                rod(out, lx, y, lz, lx, y + 0.28f, lz, 0.07f, MAGNET_RED)
                rod(out, qx, y, qz, qx, y + 0.28f, qz, 0.07f, MAGNET_RED)
                rod(out, lx, y + 0.28f, lz, qx, y + 0.28f, qz, 0.075f, MAGNET_RED)
                out += Prop(lx, y + 0.28f, lz, 0.075f, MAGNET_RED)
                out += Prop(qx, y + 0.28f, qz, 0.075f, MAGNET_RED)
                rod(out, lx, y - 0.04f, lz, lx, y + 0.03f, lz, 0.072f, STEEL)
                rod(out, qx, y - 0.04f, qz, qx, y + 0.03f, qz, 0.072f, STEEL)
                rod(out, x, y + 0.28f, z, x, y + 0.65f, z, 0.045f, STEEL)
                grip(x, y + 0.28f, z, x, y + 0.65f, z, 0.65f)
            }
            Tool.HAMMER -> {
                val hy = y + 0.13f
                rod(out, x - rx * 0.24f, hy, z - rz * 0.24f, x + rx * 0.24f, hy, z + rz * 0.24f, 0.13f, HAMMER_RED)
                for (sgn in intArrayOf(-1, 1)) {
                    rod(out, x + rx * 0.11f * sgn, hy, z + rz * 0.11f * sgn, x + rx * 0.17f * sgn, hy, z + rz * 0.17f * sgn, 0.137f, HAMMER_YELLOW)
                }
                val ex = x + cx * 0.32f; val ey = hy + 0.78f; val ez = z + cz * 0.32f
                rod(out, x, hy, z, ex, ey, ez, 0.05f, HAMMER_YELLOW)
                grip(x, hy, z, ex, ey, ez, 0.85f)
            }
            Tool.SCOOP -> {
                val r = 0.32f * size
                out += Prop(x, y, z, r, STEEL, Prop.Shape.CUP)
                val ax = x + cx * r; val az = z + cz * r
                val ex = x + cx * (r + 0.5f); val ey = y + 0.4f; val ez = z + cz * (r + 0.5f)
                rod(out, ax, y, az, ex, ey, ez, 0.045f, SCOOP_BLUE)
                out += Prop(ex, ey, ez, 0.05f, SCOOP_BLUE)
                grip(ax, y, az, ex, ey, ez, 0.75f)
            }
            Tool.WHISK -> {
                val hub = y + 0.54f
                for (k in 0 until 3) {
                    val a = spin + k * PI.toFloat() / 3f
                    val dx = cos(a); val dz = sin(a)
                    var px = x; var py = hub; var pz = z
                    for (j in 1..10) {
                        val psi = PI.toFloat() + j * 2f * PI.toFloat() / 10f
                        val qx = x + dx * 0.17f * sin(psi); val qy = y + 0.27f - 0.27f * cos(psi); val qz = z + dz * 0.17f * sin(psi)
                        rod(out, px, py, pz, qx, qy, qz, 0.014f, STEEL)
                        px = qx; py = qy; pz = qz
                    }
                }
                rod(out, x, hub - 0.02f, z, x, hub + 0.62f, z, 0.055f, WHISK_MINT)
                out += Prop(x, hub + 0.62f, z, 0.055f, WHISK_MINT)
                grip(x, hub, z, x, hub + 0.62f, z, 0.6f)
            }
            Tool.DRYER -> {
                // Nozzle at (x, y, z), pointing down and away at the slime; the handle hangs back towards you.
                var dx = -cx * 0.5f; var dy = -1f; var dz = -cz * 0.5f
                val l = sqrt(dx * dx + dy * dy + dz * dz); dx /= l; dy /= l; dz /= l
                val bx = x - dx * 0.55f; val by = y - dy * 0.55f; val bz = z - dz * 0.55f
                rod(out, bx, by, bz, x, y, z, 0.15f, DRYER_MINT)
                out += Prop(bx, by, bz, 0.15f, DRYER_MINT)
                rod(out, x - dx * 0.06f, y - dy * 0.06f, z - dz * 0.06f, x + dx * 0.04f, y + dy * 0.04f, z + dz * 0.04f, 0.11f, DARK)
                var hx = cx; var hy = -1f; var hz = cz
                val hd = hx * dx + hy * dy + hz * dz
                hx -= dx * hd; hy -= dy * hd; hz -= dz * hd
                val hl = max(1e-4f, sqrt(hx * hx + hy * hy + hz * hz)); hx /= hl; hy /= hl; hz /= hl
                val sx = bx + dx * 0.18f; val sy = by + dy * 0.18f; val sz = bz + dz * 0.18f
                val ex = sx + hx * 0.45f; val ey = sy + hy * 0.45f; val ez = sz + hz * 0.45f
                rod(out, sx, sy, sz, ex, ey, ez, 0.07f, DRYER_MINT)
                out += Prop(ex, ey, ez, 0.07f, DRYER_MINT)
                grip(ex, ey, ez, sx, sy, sz, 0.4f)
            }
            Tool.WAND -> {
                for (k in 0 until 12) {
                    val a = k * 2f * PI.toFloat() / 12f
                    out += Prop(x + rx * cos(a) * 0.16f, y + sin(a) * 0.16f, z + rz * cos(a) * 0.16f, 0.032f, STRAW)
                }
                val ex = x + cx * 0.15f; val ey = y - 0.85f; val ez = z + cz * 0.15f
                rod(out, x, y - 0.16f, z, ex, ey, ez, 0.03f, STRAW)
                grip(ex, ey, ez, x, y - 0.16f, z, 0.25f)
            }
            else -> return false
        }
        return true
    }

    /** The hair dryer: a gusty wind that ripples the skin, warms it, and blows bubbles and droplets about. */
    private fun dry(t: Touch, dt: Float) {
        var dx = -camX * 0.5f; var dy = -1f; var dz = -camZ * 0.5f
        val l = sqrt(dx * dx + dy * dy + dz * dz); dx /= l; dy /= l; dz /= l
        val reach = max(0.2f, (t.py - ext[0] * 0.7f) / -dy)
        val hx = t.px + dx * reach; val hz = t.pz + dz * reach
        val hy = ext[0] * 0.7f
        val gust = 0.6f + 0.4f * sin(t.clock * 37f) * sin(t.clock * 13f)
        val k = 7f * dt * gust
        body.impulseAt(hx, hy, hz, 0.95f, dx * k, dy * k * 0.6f, dz * k)
        heat = min(1f, heat + dt * 0.22f)
        for (d in droplets) if (!d.held && sq(d.x - hx) + sq(d.z - hz) < 1.2f) {
            d.vx += (d.x - hx + dx) * 2.2f * dt; d.vz += (d.z - hz + dz) * 2.2f * dt
        }
        for (b in bubbles) if (!b.growing && sq(b.x - hx) + sq(b.z - hz) < 2f) {
            b.x += (b.x - hx + dx) * 0.9f * dt; b.z += (b.z - hz + dz) * 0.9f * dt
        }
        if (rnd.nextFloat() < dt * 40f) {
            val p = spawn(t.px, t.py, t.pz, 0f, 0.05f)
            val sp = 2.5f + rnd.nextFloat()
            p.vx = (dx + (rnd.nextFloat() - 0.5f) * 0.35f) * sp
            p.vy = (dy + (rnd.nextFloat() - 0.5f) * 0.35f) * sp
            p.vz = (dz + (rnd.nextFloat() - 0.5f) * 0.35f) * sp
            p.gravity = 0f; p.shape = 0f
            p.max = 0.35f; p.life = p.max
            p.r = 0.85f; p.g = 0.92f; p.b = 1f
        }
        if (t.clock - t.tickTurn > 0.5f) {
            t.tickTurn = t.clock
            Audio.play(Snd.HISS, 0.22f, 1.6f + rnd.nextFloat() * 0.2f, pan(t.x), 300)
            Haptics.buzz(Haptics.Kind.TICK, 0.15f)
        }
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
        updateBlisters(dt)
        updateDroplets(dt)
        val working = touches.values.any { it.kind in WORKING }
        heat = if (working) min(1f, heat + dt * 0.16f) else max(0f, heat - dt * 0.04f)
        body.step(dt)
        if (mode != SceneMode.LAB) mix = min(0.75f, mix + body.agitation * dt * 0.1f)
        for (t in touches.values) {
            when (t.kind) {
                Kind.FINGER, Kind.STIR -> t.finger?.let { feel(t, it, it.x, it.y, it.z, dt) }
                Kind.PALM -> feel(t, palm, palm.x, palm.y, palm.z, dt)
                Kind.PIN -> feel(t, pin, pin.x, pin.y, pin.z, dt)
                Kind.RAKE, Kind.STAMP, Kind.CUTTER, Kind.WHISK, Kind.SQUEEZE ->
                    t.balls.getOrNull(if (t.kind == Kind.RAKE) 1 else 0)?.let { feel(t, it, it.x, it.y, it.z, dt) }
                else -> Unit
            }
        }
        updateItems(dt)
        updateBubbles(dt)
        updateParticles(dt)
        reactT -= dt
        if (touches.values.any { it.kind == Kind.FINGER || it.kind == Kind.PALM || it.kind == Kind.SQUEEZE }) react(Mood.GIGGLY, 0.4f)
        if (slapEnergy > 3.2f) react(Mood.DIZZY, 2.2f)
        nextBlink -= dt
        if (nextBlink < 0f) nextBlink = 2.5f + rnd.nextFloat() * 3.5f
        val squint = if (working) min(1f, body.agitation * 1.5f) else 0f
        val shut = if (nextBlink < 0.14f) 1f else squint
        blink += (shut - blink) * min(1f, dt * 30f)
        for (k in 0 until 4) focusNow[k] += (focus[k] - focusNow[k]) * min(1f, dt * 6f)
        camera()
    }

    private fun camera() {
        val aspect = width / height.toFloat()
        val t = tan(Math.toRadians(22.5)).toFloat()
        // Fit the dish in the focus area, then shift the lens so it's centred there.
        val fw = (focusNow[2] - focusNow[0]).coerceIn(0.2f, 1f)
        val fh = (focusNow[3] - focusNow[1]).coerceIn(0.2f, 1f)
        val dish = Shapes.vesselWide(body.vessel, body.plate)
        val dist = (max(3.0f * dish / (t * aspect * fw), 2.5f * dish / (t * fh)) * zoom).coerceIn(4f, 24f)
        val ty = Shapes.vesselLook(body.vessel)
        rgtX = cos(yaw); rgtZ = -sin(yaw); camX = sin(yaw); camZ = cos(yaw)
        eye[0] = dist * cos(pitch) * sin(yaw); eye[1] = ty + dist * sin(pitch); eye[2] = dist * cos(pitch) * cos(yaw)
        val l = sqrt(sq(eye[0]) + sq(eye[1] - ty) + sq(eye[2]))
        fwd[0] = -eye[0] / l; fwd[1] = -(eye[1] - ty) / l; fwd[2] = -eye[2] / l
        Matrix.perspectiveM(proj, 0, 45f, aspect, 0.5f, 80f)
        proj[8] = -(focusNow[0] + focusNow[2] - 1f)
        proj[9] = -(1f - focusNow[1] - focusNow[3])
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
        private val WORKING = setOf(
            Kind.FINGER, Kind.GRAB, Kind.PALM, Kind.PIN, Kind.STIR, Kind.RAKE, Kind.STAMP, Kind.MAGNET, Kind.SQUEEZE,
            Kind.CUTTER, Kind.WHISK, Kind.DRYER,
        )
        const val MAX_BLISTERS = 3
        const val MAX_DROPLETS = 16
        private const val RAKE_GAP = 0.13f
        private const val RAKE_FLOOR = 0.08f
        private const val WHISK_Y = 0.06f
        /** The cookie cutter's outline: 22 points round a heart, lobes forward (x across, y away), in world units. */
        private val HEART: FloatArray = run {
            val n = 22
            val out = FloatArray(n * 2)
            for (i in 0 until n) {
                val t = i * 2f * PI.toFloat() / n
                val hx = 16f * sin(t) * sin(t) * sin(t)
                val hy = 13f * cos(t) - 5f * cos(2 * t) - 2f * cos(3 * t) - cos(4 * t)
                out[i * 2] = hx / 16f * 0.36f
                out[i * 2 + 1] = (hy + 2f) / 16f * 0.36f
            }
            out
        }
        private val WOOD = floatArrayOf(0.86f, 0.64f, 0.42f)
        private val STRAW = floatArrayOf(1f, 0.55f, 0.78f)
        private val RAKE_GREEN = floatArrayOf(0.36f, 0.85f, 0.55f)
        private val STAMP_DARK = floatArrayOf(0.78f, 0.36f, 0.62f)
        private val HAMMER_RED = floatArrayOf(1f, 0.3f, 0.36f)
        private val HAMMER_YELLOW = floatArrayOf(1f, 0.86f, 0.3f)
        private val SCOOP_BLUE = floatArrayOf(0.35f, 0.7f, 1f)
        private val CUTTER_PINK = floatArrayOf(1f, 0.45f, 0.65f)
        private val WHISK_MINT = floatArrayOf(0.5f, 0.92f, 0.8f)
        private val DRYER_MINT = floatArrayOf(0.62f, 0.9f, 0.95f)
        private val DARK = floatArrayOf(0.2f, 0.2f, 0.26f)
        private val STEEL = floatArrayOf(0.82f, 0.85f, 0.9f)
        private val STAMP_PINK = floatArrayOf(1f, 0.62f, 0.86f)
        private val SHAKER_WHITE = floatArrayOf(0.95f, 0.95f, 1f)
        private val MAGNET_RED = floatArrayOf(0.92f, 0.16f, 0.22f)
        private val SPRINKLES = listOf(0xFFFF4D8D, 0xFFFFE066, 0xFF22E3FF, 0xFF6EF2B4, 0xFFA970FF, 0xFFFF8A3D, 0xFFFFFFFF)
        val LIGHT = floatArrayOf(-0.45f, 0.85f, 0.5f).let { v -> val l = sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]); floatArrayOf(v[0] / l, v[1] / l, v[2] / l) }

        private fun sq(x: Float) = x * x

        /** Face spots on the front of the unit sphere: two eyes, two cheeks, the mouth. */
        /** Reactions, smallest first. */
        private val REACTIONS = listOf(Mood.GIGGLY, Mood.SHOCKED, Mood.DIZZY)
        val FACE = listOf(-0.27f to 0.5f, 0.27f to 0.5f, -0.5f to 0.3f, 0.5f to 0.3f, 0f to 0.36f)

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
