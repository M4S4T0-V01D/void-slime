package dev.m4s4t0.voidslime.gl

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLES20.GL_BACK
import android.opengl.GLES20.GL_BLEND
import android.opengl.GLES20.GL_COLOR_BUFFER_BIT
import android.opengl.GLES20.GL_CULL_FACE
import android.opengl.GLES20.GL_DEPTH_BUFFER_BIT
import android.opengl.GLES20.GL_DEPTH_TEST
import android.opengl.GLES20.GL_FLOAT
import android.opengl.GLES20.GL_FRONT
import android.opengl.GLES20.GL_LEQUAL
import android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA
import android.opengl.GLES20.GL_POINTS
import android.opengl.GLES20.GL_SRC_ALPHA
import android.opengl.GLES20.GL_TRIANGLES
import android.opengl.GLES20.GL_TRIANGLE_STRIP
import android.opengl.GLES20.GL_UNSIGNED_SHORT
import android.opengl.GLES20.glBlendFunc
import android.opengl.GLES20.glClear
import android.opengl.GLES20.glClearColor
import android.opengl.GLES20.glCullFace
import android.opengl.GLES20.glDepthFunc
import android.opengl.GLES20.glDepthMask
import android.opengl.GLES20.glDisable
import android.opengl.GLES20.glDisableVertexAttribArray
import android.opengl.GLES20.glDrawArrays
import android.opengl.GLES20.glDrawElements
import android.opengl.GLES20.glEnable
import android.opengl.GLES20.glEnableVertexAttribArray
import android.opengl.GLES20.glVertexAttribPointer
import android.opengl.GLES20.glViewport
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Plate
import dev.m4s4t0.voidslime.model.Shapes
import dev.m4s4t0.voidslime.model.World
import dev.m4s4t0.voidslime.ui.Art
import dev.m4s4t0.voidslime.model.Tray
import dev.m4s4t0.voidslime.model.Vessel
import dev.m4s4t0.voidslime.sim.Icosphere
import dev.m4s4t0.voidslime.model.colorOf
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.abs
import kotlin.math.sqrt

/** Draws a [Scene]: backdrop, dish, shadow, the slime with its add-ins, tools, bubbles and sparks. */
class SlimeRenderer(val scene: Scene, private val res: Resources) : GLSurfaceView.Renderer {
    @Volatile var tray: Tray = Tray.VOID
    @Volatile var world: World = World.VOID
    @Volatile var plate: Plate = Plate.ROUND
    private var builtPlate: Plate? = null
    @Volatile var vessel: Vessel = Vessel.DISH
    private var builtVessel: Vessel? = null
    private var vesselMesh: Mesh? = null
    private var texWorld: World? = null
    private var tex = 0
    private var texW = 1
    private var texH = 1

    private lateinit var pSlime: Program
    private lateinit var pSolid: Program
    private lateinit var pSprite: Program
    private lateinit var pBubble: Program
    private lateinit var pShadow: Program
    private lateinit var pBack: Program
    private lateinit var trayMesh: Mesh
    private lateinit var pinMesh: Mesh
    private lateinit var bubbleMesh: Mesh
    private lateinit var rodMesh: Mesh
    private lateinit var cupMesh: Mesh

    private val body = scene.body
    private val idx = shorts(ShortArray(body.tris.size) { body.tris[it].toShort() })
    private val posBuf = floats(body.n * 3)
    private val norBuf = floats(body.n * 3)
    private val patBuf = floats(body.pattern)
    private val quad = floats(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    private val sprites = floats(MAX_SPRITES * SPRITE_FLOATS)
    private val model = FloatArray(16)
    private val air = FloatArray(12)
    // Droplets: a small sphere, reshaped on the CPU each frame and drawn with the slime's own shader.
    private val drop = Icosphere(2)
    private val dropIdx = shorts(ShortArray(drop.tris.size) { drop.tris[it].toShort() })
    private val dropPos = floats(drop.count * 3)
    private val dropNor = floats(drop.count * 3)
    private val dropPat = floats(drop.count * 3)
    private val dp = FloatArray(drop.count * 3)
    private val dn = FloatArray(drop.count * 3)
    private val dq = FloatArray(drop.count * 3)
    private val ext = FloatArray(2)
    private var last = 0L

    // Colours worked out per frame.
    private val envTop = FloatArray(3)
    private val envBot = FloatArray(3)
    private val backTop = FloatArray(3)
    private val backBot = FloatArray(3)
    private val c0 = FloatArray(3)
    private val c1 = FloatArray(3)
    private val c2 = FloatArray(3)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        pSlime = Program(Shaders.SLIME_VS, Shaders.SLIME_FS)
        pSolid = Program(Shaders.SOLID_VS, Shaders.SOLID_FS)
        pSprite = Program(Shaders.SPRITE_VS, Shaders.SPRITE_FS)
        pBubble = Program(Shaders.BUBBLE_VS, Shaders.BUBBLE_FS)
        pShadow = Program(Shaders.SHADOW_VS, Shaders.SHADOW_FS)
        pBack = Program(Shaders.BACK_VS, Shaders.BACK_FS)
        builtPlate = null
        texWorld = null
        tex = 0
        pinMesh = Meshes.rollingPin()
        bubbleMesh = Meshes.sphere(2)
        rodMesh = Meshes.rod()
        cupMesh = Meshes.cup()
        builtVessel = null
        glClearColor(0f, 0f, 0f, 1f)
        glDepthFunc(GL_LEQUAL)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        last = 0L
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)
        scene.resize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 1f / 60f else ((now - last) / 1e9f).coerceIn(1f / 240f, 1f / 20f)
        last = now
        val pl = plate
        if (pl != builtPlate) {
            builtPlate = pl
            trayMesh = if (pl == Plate.ROUND) Meshes.tray() else Meshes.tray({ Shapes.plateR(pl, it) }, 240)
            body.plate = pl
        }
        val ve = vessel
        if (ve != builtVessel) {
            builtVessel = ve
            vesselMesh = if (ve == Vessel.DISH) null else Meshes.vessel(ve)
            body.vessel = ve
        }
        loadWorld()
        scene.update(dt)
        colors()

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        drawBack()
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)
        glDisable(GL_CULL_FACE)
        val t = tray
        drawSolid(trayMesh, identity(), Scene.rgb(t.argb), 1f, t.gloss, if (t == Tray.NEON) scene.look.accent else ZERO)
        val vm = vesselMesh
        if (vm != null && ve == Vessel.BOWL) drawSolid(vm, identity(), Scene.rgb(t.argb).let { c -> FloatArray(3) { min(1f, c[it] * 0.8f + 0.2f) } }, 1f, t.gloss, ZERO)
        drawShadow()
        drawDroplets()

        posBuf.position(0); posBuf.put(body.pos); posBuf.position(0)
        norBuf.position(0); norBuf.put(body.normals); norBuf.position(0)
        glEnable(GL_CULL_FACE)
        if (scene.transparent) {
            glEnable(GL_BLEND)
            glDepthMask(false)
            glCullFace(GL_FRONT)
            drawSlime(true)
            glDepthMask(true)
            drawItems()
            glDepthMask(false)
            glCullFace(GL_BACK)
            drawSlime(false)
        } else {
            glCullFace(GL_BACK)
            // A blown-up blister's thin skin is see-through even on an opaque slime.
            if (body.blisters.isNotEmpty()) glEnable(GL_BLEND)
            drawSlime(false)
            glEnable(GL_BLEND)
            drawItems()
        }
        glDisable(GL_CULL_FACE)
        glEnable(GL_BLEND)
        drawFace()
        drawTools()
        glDepthMask(false)
        if (vm != null && ve != Vessel.BOWL) {
            // Glass last, over everything inside it.
            pSolid.use(); pSolid.f("uGlass", 1f)
            drawSolid(vm, identity(), floatArrayOf(0.8f, 0.92f, 1f), 1f, 1f, ZERO)
            pSolid.use(); pSolid.f("uGlass", 0f)
        }
        drawBubbles()
        drawParticles()
        glDepthMask(true)
    }

    /** The painted backdrop for [world], uploaded when it changes (Void has none: it's drawn by the shader). */
    private fun loadWorld() {
        val w = world
        if (w == texWorld) return
        texWorld = w
        if (tex != 0) { GLES20.glDeleteTextures(1, intArrayOf(tex), 0); tex = 0 }
        if (w == World.VOID) return
        val id = Art.ids[w.backdrop] ?: return
        val bmp = BitmapFactory.decodeResource(res, id, BitmapFactory.Options().apply { inScaled = false }) ?: return
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        tex = ids[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        texW = bmp.width; texH = bmp.height
        bmp.recycle()
    }

    private fun colors() {
        val l = scene.look
        val w = texWorld
        for (k in 0 until 3) {
            envTop[k] = min(1f, l.bg[k] + (l.accent[k] - l.bg[k]) * 0.3f + 0.35f)
            envBot[k] = l.bg[k] * 0.6f + l.accent2[k] * 0.08f
            backTop[k] = l.bg[k] + (l.accent[k] - l.bg[k]) * 0.16f
            backBot[k] = l.bg[k] * 0.75f + l.accent2[k] * 0.06f
        }
        // A painted world: the slime reflects its sky.
        if (w != null && w != World.VOID && tex != 0) {
            val top = Scene.rgb(w.skyTop); val bot = Scene.rgb(w.skyBottom)
            for (k in 0 until 3) {
                envTop[k] = min(1f, top[k] * 0.85f + 0.15f)
                envBot[k] = bot[k] * 0.7f
            }
        }
        val cs = scene.recipe.colors
        Scene.rgb(colorOf(cs[0]).argb).copyInto(c0)
        Scene.rgb(colorOf(cs.getOrElse(1) { cs[0] }).argb).copyInto(c1)
        Scene.rgb(colorOf(cs.getOrElse(2) { cs[0] }).argb).copyInto(c2)
    }

    private fun identity() = model.also { Matrix.setIdentityM(it, 0) }

    private fun env(p: Program) {
        p.v3("uEnvTop", envTop); p.v3("uEnvBot", envBot); p.f("uRoom", scene.room)
    }

    private fun drawBack() {
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        val p = pBack
        p.use()
        p.v3("uTop", backTop); p.v3("uBot", backBot); p.v3("uGlow", scene.look.accent)
        p.f("uRoom", scene.room); p.f("uTime", scene.time); p.v2("uRes", scene.width.toFloat(), scene.height.toFloat())
        val w = texWorld ?: World.VOID
        p.f("uFx", w.fx.ordinal.toFloat())
        if (tex != 0) {
            // Cover the screen, a little zoomed so it can drift as the camera turns.
            val a = scene.width / scene.height.toFloat()
            val ta = texW / texH.toFloat()
            val sx = if (a > ta) 1f else a / ta
            val sy = if (a > ta) ta / a else 1f
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
            GLES20.glUniform1i(p.u("uTex"), 0)
            p.f("uHasTex", 1f)
            p.v2("uScale", sx * 0.92f, sy * 0.92f)
            p.v2("uOff", sin(scene.cameraYaw - Shapes.FRONT_YAW) * 0.035f * sx, (scene.cameraPitch - 0.64f) * -0.03f * sy)
        } else {
            p.f("uHasTex", 0f)
            p.v2("uScale", 1f, 1f); p.v2("uOff", 0f, 0f)
        }
        drawQuad(p)
        glDepthMask(true)
    }

    private fun drawQuad(p: Program) {
        val a = p.a("aPos")
        quad.position(0)
        glEnableVertexAttribArray(a)
        glVertexAttribPointer(a, 2, GL_FLOAT, false, 8, quad)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glDisableVertexAttribArray(a)
    }

    private fun drawSolid(mesh: Mesh, m: FloatArray, rgb: FloatArray, alpha: Float, gloss: Float, emit: FloatArray) {
        val p = pSolid
        p.use()
        p.m4("uVP", scene.vp); p.m4("uModel", m)
        p.v3("uEye", scene.eye); p.v3("uLight", Scene.LIGHT)
        p.v4("uColor", rgb[0], rgb[1], rgb[2], alpha)
        p.f("uGloss", gloss); p.v3("uEmit", emit)
        env(p)
        mesh.draw(p)
    }

    private fun drawShadow() {
        body.extent(ext)
        glEnable(GL_BLEND)
        glDepthMask(false)
        val p = pShadow
        p.use()
        p.m4("uVP", scene.vp)
        p.v3("uCenter", body.cx, 0.004f, body.cz)
        p.f("uRadius", ext[1] * 1.35f + 0.1f)
        p.v4("uColor", 0f, 0f, 0f, 0.4f * (0.3f + 0.7f * scene.room))
        drawQuad(p)
        glDepthMask(true)
        glDisable(GL_BLEND)
    }

    private fun drawSlime(back: Boolean) {
        val p = pSlime
        drawSlimeUniforms(p, back)
        air.fill(0f)
        for ((i, b) in body.blisters.withIndex()) {
            if (i >= 3) break
            air[i * 4] = b.px; air[i * 4 + 1] = b.py; air[i * 4 + 2] = b.pz; air[i * 4 + 3] = b.size
        }
        GLES20.glUniform4fv(p.u("uAir"), 3, air, 0)
        bind(p.a("aPos"), posBuf); bind(p.a("aNor"), norBuf); bind(p.a("aPat"), patBuf)
        idx.position(0)
        glDrawElements(GL_TRIANGLES, body.tris.size, GL_UNSIGNED_SHORT, idx)
        glDisableVertexAttribArray(p.a("aPos")); glDisableVertexAttribArray(p.a("aNor")); glDisableVertexAttribArray(p.a("aPat"))
    }

    private fun drawSlimeUniforms(p: Program, back: Boolean) {
        val m = body.mat
        val r = scene.recipe
        p.use()
        p.m4("uVP", scene.vp); p.v3("uEye", scene.eye); p.v3("uLight", Scene.LIGHT)
        p.v3("uC0", c0); p.v3("uC1", c1); p.v3("uC2", c2)
        p.f("uCount", r.colors.size.toFloat()); p.f("uMix", scene.mix)
        p.f("uAlpha", m.alpha); p.f("uGloss", m.gloss); p.f("uFluff", m.fluff); p.f("uMetal", m.metal)
        p.f("uHolo", m.holo); p.f("uGlow", m.glow); p.f("uFrost", m.frost)
        p.f("uGalaxy", m.galaxy); p.f("uThermo", m.thermo); p.f("uHeat", scene.heat)
        val g = r.glitter
        when {
            g == Glitter.NONE -> p.v4("uGlitter", 0f, 0f, 0f, 0f)
            g.argb == 0L -> p.v4("uGlitter", -1f, 0f, 0f, body.form)
            else -> Scene.rgb(g.argb).let { c -> p.v4("uGlitter", c[0], c[1], c[2], body.form) }
        }
        p.f("uTime", scene.time); p.f("uBack", if (back) 1f else 0f)
        env(p)
    }

    private fun bind(a: Int, b: FloatBuffer) {
        if (a < 0) return
        b.position(0)
        glEnableVertexAttribArray(a)
        glVertexAttribPointer(a, 3, GL_FLOAT, false, 12, b)
    }

    private fun drawTools() {
        val pin = scene.pin
        glDisable(GL_BLEND)
        if (pin.active) {
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, pin.x, pin.y, pin.z)
            Matrix.rotateM(model, 0, Math.toDegrees(kotlin.math.atan2(-pin.az, pin.ax).toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(model, 0, -90f, 0f, 0f, 1f)
            Matrix.rotateM(model, 0, Math.toDegrees(scene.pinSpin.toDouble()).toFloat(), 0f, 1f, 0f)
            drawSolid(pinMesh, model, floatArrayOf(0.85f, 0.62f, 0.4f), 1f, 0.4f, ZERO)
        }
        // The hand and whatever it holds: balls, rods and the scoop's cup.
        for (pr in scene.props) {
            when (pr.shape) {
                Prop.Shape.BALL -> {
                    Matrix.setIdentityM(model, 0)
                    Matrix.translateM(model, 0, pr.x, pr.y, pr.z)
                    Matrix.scaleM(model, 0, pr.r, pr.r, pr.r)
                    drawSolid(bubbleMesh, model, pr.rgb, 1f, 0.6f, ZERO)
                }
                Prop.Shape.ROD -> if (rodMatrix(pr)) drawSolid(rodMesh, model, pr.rgb, 1f, 0.6f, ZERO)
                Prop.Shape.CUP -> {
                    Matrix.setIdentityM(model, 0)
                    Matrix.translateM(model, 0, pr.x, pr.y, pr.z)
                    Matrix.scaleM(model, 0, pr.r, pr.r, pr.r)
                    drawSolid(cupMesh, model, pr.rgb, 1f, 0.9f, ZERO)
                }
            }
        }
        glEnable(GL_BLEND)
    }

    /** Fits the unit rod between a prop's two ends. False if they're on top of each other. */
    private fun rodMatrix(pr: Prop): Boolean {
        val dx = pr.x2 - pr.x; val dy = pr.y2 - pr.y; val dz = pr.z2 - pr.z
        val l = sqrt(dx * dx + dy * dy + dz * dz)
        if (l < 1e-4f) return false
        val ax = dx / l; val ay = dy / l; val az = dz / l
        // Any direction square to the rod, then the third by a cross product.
        var ux: Float; var uy: Float; var uz: Float
        if (abs(ay) < 0.9f) { ux = az; uy = 0f; uz = -ax } else { ux = 0f; uy = -az; uz = ay }
        val ul = sqrt(ux * ux + uy * uy + uz * uz); ux /= ul; uy /= ul; uz /= ul
        val wx = ay * uz - az * uy; val wy = az * ux - ax * uz; val wz = ax * uy - ay * ux
        val r = pr.r
        val m = model
        m[0] = ux * r; m[1] = uy * r; m[2] = uz * r; m[3] = 0f
        m[4] = dx; m[5] = dy; m[6] = dz; m[7] = 0f
        m[8] = wx * r; m[9] = wy * r; m[10] = wz * r; m[11] = 0f
        m[12] = pr.x; m[13] = pr.y; m[14] = pr.z; m[15] = 1f
        return true
    }

    /** The bits that broke off: little squashy blobs of the same slime. */
    private fun drawDroplets() {
        val ds = scene.droplets
        if (ds.isEmpty()) return
        val p = pSlime
        val v = drop.verts
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        if (scene.transparent) { glEnable(GL_BLEND); glDepthMask(false) }
        drawSlimeUniforms(p, false)
        air.fill(0f)
        GLES20.glUniform4fv(p.u("uAir"), 3, air, 0)
        for (d in ds) {
            val sq = d.squash
            val wide = 1f + 0.3f * sq
            val tall = 1f - 0.45f * sq
            val r = d.r
            val by = d.y - r * tall
            for (i in 0 until drop.count) {
                val x = v[i * 3]; val y = v[i * 3 + 1]; val z = v[i * 3 + 2]
                dp[i * 3] = d.x + x * r * wide; dp[i * 3 + 1] = by + (y + 1f) * r * tall; dp[i * 3 + 2] = d.z + z * r * wide
                val nx = x / wide; val ny = y / tall; val nz = z / wide
                val nl = sqrt(nx * nx + ny * ny + nz * nz)
                dn[i * 3] = nx / nl; dn[i * 3 + 1] = ny / nl; dn[i * 3 + 2] = nz / nl
                // Each droplet shows a different bit of the swirl.
                dq[i * 3] = x * 0.45f + d.seed * 3f; dq[i * 3 + 1] = y * 0.45f; dq[i * 3 + 2] = z * 0.45f - d.seed * 2f
            }
            dropPos.position(0); dropPos.put(dp); dropNor.position(0); dropNor.put(dn); dropPat.position(0); dropPat.put(dq)
            bind(p.a("aPos"), dropPos); bind(p.a("aNor"), dropNor); bind(p.a("aPat"), dropPat)
            dropIdx.position(0)
            glDrawElements(GL_TRIANGLES, drop.tris.size, GL_UNSIGNED_SHORT, dropIdx)
        }
        glDisableVertexAttribArray(p.a("aPos")); glDisableVertexAttribArray(p.a("aNor")); glDisableVertexAttribArray(p.a("aPat"))
        glDisable(GL_CULL_FACE)
        glDisable(GL_BLEND)
        glDepthMask(true)
    }

    private fun drawBubbles() {
        if (scene.bubbles.isEmpty()) return
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        val p = pBubble
        p.use()
        p.m4("uVP", scene.vp); p.v3("uEye", scene.eye); p.v3("uLight", Scene.LIGHT)
        p.f("uTime", scene.time); p.f("uRoom", scene.room)
        for (b in scene.bubbles) {
            val wob = 1f + 0.04f * kotlin.math.sin(b.age * 9f + b.seed * 30f)
            p.v3("uCenter", b.x, b.y, b.z)
            p.v3("uScale3", b.r * wob, b.r / wob, b.r * wob)
            p.f("uSeed", b.seed); p.f("uFade", max(0f, b.fade)); p.f("uGold", if (b.gold) 1f else 0f)
            bubbleMesh.draw(p)
        }
        glDisable(GL_CULL_FACE)
    }

    private var spriteCount = 0

    private fun put(x: Float, y: Float, z: Float, r: Float, g: Float, b: Float, a: Float, size: Float, shape: Float, angle: Float, lit: Float) {
        if (spriteCount >= MAX_SPRITES) return
        val s = sprites
        s.put(x); s.put(y); s.put(z); s.put(r); s.put(g); s.put(b); s.put(a); s.put(size); s.put(shape); s.put(angle); s.put(lit)
        spriteCount++
    }

    private fun beginSprites() { sprites.position(0); spriteCount = 0 }

    private fun flushSprites() {
        if (spriteCount == 0) return
        val p = pSprite
        p.use()
        p.m4("uVP", scene.vp)
        p.f("uScale", scene.height / 2f * scene.proj[5])
        val stride = SPRITE_FLOATS * 4
        val ap = p.a("aPos"); val ac = p.a("aCol"); val am = p.a("aMeta")
        glEnableVertexAttribArray(ap); glEnableVertexAttribArray(ac); glEnableVertexAttribArray(am)
        sprites.position(0); glVertexAttribPointer(ap, 3, GL_FLOAT, false, stride, sprites)
        sprites.position(3); glVertexAttribPointer(ac, 4, GL_FLOAT, false, stride, sprites)
        sprites.position(7); glVertexAttribPointer(am, 4, GL_FLOAT, false, stride, sprites)
        glDrawArrays(GL_POINTS, 0, spriteCount)
        glDisableVertexAttribArray(ap); glDisableVertexAttribArray(ac); glDisableVertexAttribArray(am)
    }

    private fun drawItems() {
        if (scene.items.isEmpty()) return
        beginSprites()
        val room = 0.3f + 0.7f * scene.room
        val t = scene.time
        for (it in scene.items) {
            put(it.x, it.y, it.z, it.r, it.g, it.b, 1f, it.size, it.addIn.shape.ordinal.toFloat(), it.angle + it.spin * t * 0.1f, it.lit * room)
        }
        flushSprites()
    }

    /** Eyes, cheeks and a smile riding on the skin (a recipe with a face), fading as they turn away. */
    private fun drawFace() {
        if (!scene.recipe.face || body.form < 0.5f) return
        glDepthMask(false)
        beginSprites()
        val pos = body.pos; val nor = body.normals; val eye = scene.eye
        val room = 0.35f + 0.65f * scene.room
        val grow = ((body.form - 0.5f) * 2f).coerceIn(0f, 1f)
        for ((k, v) in scene.faceVerts.withIndex()) {
            val o = v * 3
            val x = pos[o] + nor[o] * 0.025f; val y = pos[o + 1] + nor[o + 1] * 0.025f; val z = pos[o + 2] + nor[o + 2] * 0.025f
            val ex = eye[0] - x; val ey = eye[1] - y; val ez = eye[2] - z
            val el = kotlin.math.sqrt(ex * ex + ey * ey + ez * ez)
            val facing = (nor[o] * ex + nor[o + 1] * ey + nor[o + 2] * ez) / el
            val a = ((facing - 0.05f) / 0.3f).coerceIn(0f, 1f) * grow
            if (a <= 0f) continue
            when (k) {
                0, 1 -> put(x, y, z, 1f, 1f, 1f, a, 0.17f, 12f, scene.blink, room)
                2, 3 -> put(x, y, z, 1f, 0.45f, 0.6f, a, 0.24f, 13f, 0f, room)
                else -> put(x, y, z, 1f, 1f, 1f, a, 0.15f, 14f, 0f, room)
            }
        }
        flushSprites()
        glDepthMask(true)
    }

    private fun drawParticles() {
        if (scene.particles.isEmpty()) return
        beginSprites()
        for (p in scene.particles) {
            val a = (if (p.lands) p.life else p.life / p.max).coerceIn(0f, 1f)
            put(p.x, p.y, p.z, p.r, p.g, p.b, a, p.size * (0.6f + 0.4f * a), p.shape, p.angle, 1f)
        }
        flushSprites()
    }

    companion object {
        private const val MAX_SPRITES = 1400
        private const val SPRITE_FLOATS = 11
        private val ZERO = floatArrayOf(0f, 0f, 0f)
    }
}
