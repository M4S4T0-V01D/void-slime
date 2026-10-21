package dev.m4s4t0.voidslime.audio

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dev.m4s4t0.voidslime.model.Game

/** Little taps to go with the sounds: rich composed primitives where the motor has them, plain clicks otherwise. */
object Haptics {
    enum class Kind { TICK, CLICK, THUD, CRUNCH, POP, SNAP, SUCCESS }

    private var vib: Vibrator? = null
    private var rich = false
    private var lastMs = 0L

    fun init(ctx: Context) {
        vib = (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        rich = vib?.areAllPrimitivesSupported(
            VibrationEffect.Composition.PRIMITIVE_TICK,
            VibrationEffect.Composition.PRIMITIVE_CLICK,
            VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
            VibrationEffect.Composition.PRIMITIVE_THUD,
        ) == true
    }

    /** [strength] 0..1. Throttled so a fast squish is a purr of ticks, not a solid buzz. */
    fun buzz(kind: Kind, strength: Float = 1f) {
        val v = vib ?: return
        if (!Game.haptics || !v.hasVibrator()) return
        val now = System.nanoTime() / 1_000_000
        val gap = if (kind == Kind.TICK) 28 else 45
        if (now - lastMs < gap) return
        lastMs = now
        val s = strength.coerceIn(0.05f, 1f)
        val effect = if (rich) {
            val c = VibrationEffect.startComposition()
            when (kind) {
                Kind.TICK -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, s)
                Kind.CLICK -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, s)
                Kind.THUD -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, s)
                Kind.CRUNCH -> repeat(3) { c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, s * (1f - it * 0.25f), 12) }
                Kind.POP -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, s)
                Kind.SNAP -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, s).addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, s * 0.6f, 20)
                Kind.SUCCESS -> c.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.6f).addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1f, 90)
            }
            c.compose()
        } else {
            VibrationEffect.createPredefined(
                when (kind) {
                    Kind.TICK, Kind.CLICK, Kind.CRUNCH -> VibrationEffect.EFFECT_TICK
                    Kind.THUD, Kind.SNAP -> VibrationEffect.EFFECT_HEAVY_CLICK
                    Kind.POP -> VibrationEffect.EFFECT_CLICK
                    Kind.SUCCESS -> VibrationEffect.EFFECT_DOUBLE_CLICK
                },
            )
        }
        runCatching { v.vibrate(effect) }
    }
}
