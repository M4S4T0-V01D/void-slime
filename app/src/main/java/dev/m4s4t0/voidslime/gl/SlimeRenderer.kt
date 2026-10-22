package dev.m4s4t0.voidslime.gl

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
import android.opengl.Matrix
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Tray
import dev.m4s4t0.voidslime.model.colorOf
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

/** Draws a [Scene]: backdrop, dish, shadow, the slime with its add-ins, tools, bubbles and sparks. */
class SlimeRenderer(val scene: Scene) : GLSurfaceView.Renderer {
    @Volatile var tray: Tray = Tray.VOID

    private lateinit var pSlime: Program
    private lateinit var pSolid: Program
    private lateinit var pSprite: Program
    private lateinit var pBubble: Program
    private lateinit var pShadow: Program
    private lateinit var pBack: Program
    private lateinit var trayMesh: Mesh
    private lateinit var pinMesh: Mesh
    private lateinit var palmMesh: Mesh
    private lateinit var bubbleMesh: Mesh

    private val body = scene.body
    private val idx = shorts(ShortArray(body.tris.size) { body.tris[it].toShort() })
    private val posBuf = floats(body.n * 3)
    private val norBuf = floats(body.n * 3)
    private val patBuf = floats(body.pattern)
    private val quad = floats(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
    private val sprites = floats(MAX_SPRITES * SPRITE_FLOATS)
    private val model = FloatArray(16)
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
        trayMesh = Meshes.tray()
        pinMesh = Meshes.rollingPin()
        palmMesh = Meshes.palm()
        bubbleMesh = Meshes.sphere(2)
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
        drawShadow()

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
            drawSlime(false)
            glEnable(GL_BLEND)
            drawItems()
        }
        glDisable(GL_CULL_FACE)
        glEnable(GL_BLEND)
        drawTools()
        glDepthMask(false)
        drawBubbles()
        drawParticles()
        glDepthMask(true)
    }

    private fun colors() {
        val l = scene.look
        for (k in 0 until 3) {
            envTop[k] = min(1f, l.bg[k] + (l.accent[k] - l.bg[k]) * 0.3f + 0.35f)
            envBot[k] = l.bg[k] * 0.6f + l.accent2[k] * 0.08f
            backTop[k] = l.bg[k] + (l.accent[k] - l.bg[k]) * 0.16f
            backBot[k] = l.bg[k] * 0.75f + l.accent2[k] * 0.06f
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
        bind(p.a("aPos"), posBuf); bind(p.a("aNor"), norBuf); bind(p.a("aPat"), patBuf)
        idx.position(0)
        glDrawElements(GL_TRIANGLES, body.tris.size, GL_UNSIGNED_SHORT, idx)
        glDisableVertexAttribArray(p.a("aPos")); glDisableVertexAttribArray(p.a("aNor")); glDisableVertexAttribArray(p.a("aPat"))
    }

    private fun bind(a: Int, b: FloatBuffer) {
        if (a < 0) return
        b.position(0)
        glEnableVertexAttribArray(a)
        glVertexAttribPointer(a, 3, GL_FLOAT, false, 12, b)
    }

    private fun drawTools() {
        val palm = scene.palm
        if (palm.active) {
            glDepthMask(false)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, palm.x, palm.y, palm.z)
            Matrix.scaleM(model, 0, palm.r, 1f, palm.r)
            drawSolid(palmMesh, model, floatArrayOf(0.85f, 0.92f, 1f), 0.28f, 1f, ZERO)
            glDepthMask(true)
        }
        val pin = scene.pin
        if (pin.active) {
            glDisable(GL_BLEND)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, pin.x, pin.y, pin.z)
            Matrix.rotateM(model, 0, Math.toDegrees(kotlin.math.atan2(-pin.az, pin.ax).toDouble()).toFloat(), 0f, 1f, 0f)
            Matrix.rotateM(model, 0, -90f, 0f, 0f, 1f)
            Matrix.rotateM(model, 0, Math.toDegrees(scene.pinSpin.toDouble()).toFloat(), 0f, 1f, 0f)
            drawSolid(pinMesh, model, floatArrayOf(0.85f, 0.62f, 0.4f), 1f, 0.4f, ZERO)
            glEnable(GL_BLEND)
        }
        val props = scene.props
        if (props.isNotEmpty()) {
            glDisable(GL_BLEND)
            for (pr in props) {
                Matrix.setIdentityM(model, 0)
                Matrix.translateM(model, 0, pr.x, pr.y, pr.z)
                Matrix.scaleM(model, 0, pr.r, pr.r, pr.r)
                drawSolid(bubbleMesh, model, pr.rgb, 1f, 0.8f, ZERO)
            }
            glEnable(GL_BLEND)
        }
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
