package dev.m4s4t0.voidslime.gl

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig

/**
 * The 3D slime on screen. Touches and settings are handed to the GL thread (queueEvent), where the [Scene] lives,
 * so nothing is ever shared between threads.
 */
@SuppressLint("ViewConstructor")
class SlimeView(ctx: Context, listener: SceneListener) : GLSurfaceView(ctx) {
    private val scene = Scene(listener)
    val renderer = SlimeRenderer(scene, ctx.resources)

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser { egl, display ->
            fun pick(samples: Int): EGLConfig? {
                val ms = if (samples > 0) intArrayOf(EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, samples) else intArrayOf()
                val attrs = intArrayOf(
                    EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, 4, *ms, EGL10.EGL_NONE,
                )
                val out = arrayOfNulls<EGLConfig>(1)
                val num = IntArray(1)
                return if (egl.eglChooseConfig(display, attrs, out, 1, num) && num[0] > 0) out[0] else null
            }
            pick(4) ?: pick(0) ?: error("No OpenGL ES 2 config")
        }
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    /** Runs [block] on the GL thread, with the scene. */
    fun scene(block: Scene.() -> Unit) = queueEvent { scene.block() }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                send(Scene.DOWN, e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) send(Scene.MOVE, e.getPointerId(i), e.getX(i), e.getY(i))
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                send(Scene.UP, e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_CANCEL -> send(Scene.CANCEL, 0, 0f, 0f)
        }
        return true
    }

    private fun send(action: Int, id: Int, x: Float, y: Float) = queueEvent { scene.touch(action, id, x, y) }
}
