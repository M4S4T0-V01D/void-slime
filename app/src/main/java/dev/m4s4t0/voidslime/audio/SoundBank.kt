package dev.m4s4t0.voidslime.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Every sound in the game. Several have a few variants, picked at random, so repeats never sound mechanical. */
enum class Snd(val variants: Int = 1) {
    BLOP, SQUISH(5), CLICK(5), CRUNCH(4), POP(3), BIG_POP, STRETCH(3), SNAP, SLAP, RELEASE(3), BLOW, PLIP, POUR, SWISH(3),
    TAP, COIN, LEVEL, STICKER, BUY, DONE, WHOOSH, BELL, SQUEAK(3), BONK, HISS,
}

/**
 * Synthesizes the sounds (mono, 48 kHz) with a handful of tiny DSP pieces: noise through moving resonant filters for
 * the wet squelches, decaying resonances for clicks and crunches, pitch sweeps for pops and drips, FM bells for the
 * interface (the same bell as the other VOID apps' sounds).
 */
object SoundBank {
    const val SR = 48000

    fun build(): Map<Snd, Array<FloatArray>> {
        val rnd = Random(11)
        return Snd.entries.associateWith { s -> Array(s.variants) { v -> normalize(make(s, v, Random(rnd.nextInt() + v * 7919)), peak(s)) } }
    }

    private fun peak(s: Snd) = when (s) {
        Snd.TAP -> 0.5f
        Snd.BELL -> 0.6f
        Snd.POUR, Snd.BLOW, Snd.SWISH, Snd.HISS -> 0.6f
        else -> 0.85f
    }

    private fun make(s: Snd, v: Int, r: Random): FloatArray = when (s) {
        Snd.BLOP -> blop(r)
        Snd.SQUISH -> squish(r)
        Snd.CLICK -> click(r)
        Snd.CRUNCH -> crunch(r)
        Snd.POP -> pop(r, 1f + v * 0.12f)
        Snd.BIG_POP -> bigPop(r)
        Snd.STRETCH -> stretch(r)
        Snd.SNAP -> snap(r)
        Snd.SLAP -> slap(r)
        Snd.RELEASE -> release(r)
        Snd.BLOW -> blow(r)
        Snd.PLIP -> plip(r)
        Snd.POUR -> pour(r)
        Snd.SWISH -> swish(r)
        Snd.TAP -> tap()
        Snd.COIN -> chord(listOf(1567.98 to 0.0, 2093.0 to 0.06), 0.5, 0.25)
        Snd.LEVEL -> chord(listOf(523.25 to 0.0, 659.25 to 0.07, 783.99 to 0.14, 1046.5 to 0.21, 1318.5 to 0.28), 1.2, 0.4)
        Snd.STICKER -> sparkle(r)
        Snd.BUY -> chord(listOf(1318.5 to 0.0, 1975.5 to 0.05, 2637.0 to 0.1), 0.6, 0.25)
        Snd.DONE -> fanfare()
        Snd.WHOOSH -> whoosh(r)
        Snd.BELL -> bellNote(1046.5, 1.4, 0.7, 0.6)
        Snd.SQUEAK -> squeak(r, 1f + v * 0.15f)
        Snd.BONK -> bonk(r)
        Snd.HISS -> hiss(r)
    }

    // ---- building blocks ----

    private fun n(sec: Double) = (sec * SR).toInt()

    private fun normalize(x: FloatArray, peak: Float): FloatArray {
        var m = 1e-6f
        for (s in x) m = max(m, abs(s))
        val k = peak / m
        // Soft clip, then a short fade so nothing ends in a click.
        for (i in x.indices) x[i] = kotlin.math.tanh(x[i] * k * 1.1f) / kotlin.math.tanh(1.1f)
        val f = min(x.size, n(0.006))
        for (i in 0 until f) x[x.size - 1 - i] *= i / f.toFloat()
        return x
    }

    /** A resonant band-pass (RBJ biquad) whose centre can glide; fed sample by sample. */
    private class Band(var q: Float) {
        private var b0 = 0f; private var b2 = 0f; private var a1 = 0f; private var a2 = 0f
        private var x1 = 0f; private var x2 = 0f; private var y1 = 0f; private var y2 = 0f
        fun tune(freq: Float) {
            val w = 2.0 * PI * freq.coerceIn(20f, SR / 2.2f) / SR
            val alpha = sin(w) / (2.0 * q)
            val a0 = 1.0 + alpha
            b0 = (alpha / a0).toFloat(); b2 = (-alpha / a0).toFloat()
            a1 = (-2.0 * cos(w) / a0).toFloat(); a2 = ((1.0 - alpha) / a0).toFloat()
        }
        fun run(x: Float): Float {
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            return y
        }
    }

    private class OnePole(freq: Float) {
        private val a = exp(-2.0 * PI * freq / SR).toFloat()
        private var y = 0f
        fun low(x: Float): Float { y = x + a * (y - x); return y }
    }

    private fun env(i: Int, attack: Double, tau: Double): Float {
        val t = i.toDouble() / SR
        val a = min(1.0, t / max(attack, 1e-4))
        return (a * exp(-max(0.0, t - attack) / tau)).toFloat()
    }

    /** A tiny wet "tk": a resonance struck once, ringing for a few milliseconds. */
    private fun ping(out: FloatArray, at: Int, freq: Double, tau: Double, gain: Float) {
        val len = min(out.size - at, n(tau * 6))
        for (i in 0 until len) {
            val t = i.toDouble() / SR
            out[at + i] += (sin(2 * PI * freq * t) * exp(-t / tau)).toFloat() * gain
        }
    }

    private fun bellNote(freq: Double, sec: Double, decay: Double, bright: Double): FloatArray {
        val out = FloatArray(n(sec))
        for (i in out.indices) {
            val t = i.toDouble() / SR
            val mod = bright * 2.2 * exp(-t / (decay * 0.35)) * sin(2 * PI * freq * 3.5 * t)
            var y = sin(2 * PI * freq * t + mod)
            y += 0.35 * sin(2 * PI * freq * 2.76 * t) * exp(-t / (decay * 0.4))
            y += 0.18 * sin(2 * PI * freq * 5.4 * t) * exp(-t / (decay * 0.2))
            out[i] = (y * env(i, 0.004, decay)).toFloat()
        }
        return out
    }

    private fun mixIn(out: FloatArray, x: FloatArray, at: Int, gain: Float) {
        for (i in x.indices) if (at + i < out.size) out[at + i] += x[i] * gain
    }

    // ---- the sounds ----

    private fun blop(r: Random): FloatArray {
        val out = FloatArray(n(0.16))
        var ph = 0.0
        val f0 = 230.0 + r.nextDouble() * 40
        val band = Band(3f).apply { tune(500f) }
        for (i in out.indices) {
            val t = i.toDouble() / SR
            val f = 90.0 + (f0 - 90.0) * exp(-t / 0.025)
            ph += 2 * PI * f / SR
            out[i] = (sin(ph) * env(i, 0.002, 0.04)).toFloat() + band.run(r.nextFloat() * 2 - 1) * env(i, 0.001, 0.012) * 0.5f
        }
        return out
    }

    /** Wet squelch: noise through a resonance gliding upwards, with sticky crackles. */
    private fun squish(r: Random): FloatArray {
        val sec = 0.14 + r.nextDouble() * 0.08
        val out = FloatArray(n(sec))
        val band = Band(3.5f + r.nextFloat() * 2)
        val f0 = 320f + r.nextFloat() * 380
        val f1 = f0 * (1.6f + r.nextFloat())
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 32 == 0) band.tune(f0 + (f1 - f0) * x.pow(0.7f))
            out[i] = band.run(r.nextFloat() * 2 - 1) * env(i, 0.008, sec * 0.35) * 1.4f
        }
        repeat(6 + r.nextInt(10)) {
            ping(out, r.nextInt((out.size * 0.8).toInt()), 1300.0 + r.nextDouble() * 2600, 0.0012 + r.nextDouble() * 0.0015, 0.25f + r.nextFloat() * 0.35f)
        }
        return out
    }

    /** An air pocket bursting inside the slime: a sharp little "tk" with a soft thump. */
    private fun click(r: Random): FloatArray {
        val out = FloatArray(n(0.05))
        ping(out, 0, 1100.0 + r.nextDouble() * 2400, 0.0016 + r.nextDouble() * 0.0012, 1f)
        ping(out, n(0.001), 180.0 + r.nextDouble() * 120, 0.006, 0.35f)
        val hp = OnePole(2500f)
        for (i in 0 until n(0.0015)) { val w = r.nextFloat() * 2 - 1; out[i] += (w - hp.low(w)) * 0.6f }
        return out
    }

    /** Beads and crystals: a burst of tiny bright cracks. */
    private fun crunch(r: Random): FloatArray {
        val out = FloatArray(n(0.09))
        val lp = OnePole(3000f)
        repeat(10 + r.nextInt(14)) {
            val at = r.nextInt((out.size * 0.75).toInt())
            val len = n(0.0004 + r.nextDouble() * 0.0018)
            val g = 0.3f + r.nextFloat() * 0.7f
            for (i in 0 until len) {
                if (at + i >= out.size) break
                val w = r.nextFloat() * 2 - 1
                out[at + i] += (w - lp.low(w)) * g * (1f - i / len.toFloat())
            }
            if (r.nextFloat() < 0.4f) ping(out, at, 2500.0 + r.nextDouble() * 3000, 0.0008, g * 0.4f)
        }
        return out
    }

    private fun pop(r: Random, pitch: Float): FloatArray {
        val out = FloatArray(n(0.06))
        var ph = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / SR
            val f = (480.0 + 1300.0 * (1 - exp(-t / 0.006))) * pitch
            ph += 2 * PI * f / SR
            out[i] = (sin(ph) * env(i, 0.0005, 0.009)).toFloat()
        }
        val lp = OnePole(4000f)
        for (i in 0 until n(0.002)) { val w = r.nextFloat() * 2 - 1; out[i] += (w - lp.low(w)) * 0.8f }
        return out
    }

    private fun bigPop(r: Random): FloatArray {
        val out = FloatArray(n(0.16))
        var ph = 0.0
        var ph2 = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / SR
            ph += 2 * PI * (300.0 + 900.0 * (1 - exp(-t / 0.01))) / SR
            ph2 += 2 * PI * (150.0 * exp(-t / 0.05) + 60) / SR
            out[i] = (sin(ph) * env(i, 0.0005, 0.018) + 0.6 * sin(ph2) * env(i, 0.001, 0.04)).toFloat()
        }
        val band = Band(1.2f).apply { tune(1800f) }
        for (i in 0 until n(0.03)) out[i] += band.run(r.nextFloat() * 2 - 1) * env(i, 0.0005, 0.006)
        return out
    }

    /** Goo being pulled: a creaking, sliding resonance. */
    private fun stretch(r: Random): FloatArray {
        val sec = 0.38
        val out = FloatArray(n(sec))
        val band = Band(6f)
        val creak = 22f + r.nextFloat() * 18
        var ph = 0.0
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 32 == 0) band.tune(900f - 520f * x + 60f * sin(x * 19f))
            ph += 2 * PI * creak * (1 + 0.3 * sin(x * 7.0)) / SR
            val am = (0.55 + 0.45 * sin(ph)).toFloat().pow(3)
            val shape = min(1f, x / 0.12f) * min(1f, (1 - x) / 0.25f)
            out[i] = band.run(r.nextFloat() * 2 - 1) * am * shape * 1.6f
        }
        repeat(5) { ping(out, r.nextInt(out.size - 600), 1500.0 + r.nextDouble() * 1500, 0.001, 0.2f) }
        return out
    }

    private fun snap(r: Random): FloatArray {
        val out = FloatArray(n(0.3))
        val lp = OnePole(2600f)
        var ph = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / SR
            ph += 2 * PI * (55.0 + 70.0 * exp(-t / 0.03)) / SR
            out[i] = lp.low(r.nextFloat() * 2 - 1) * env(i, 0.0005, 0.012) * 1.2f + (sin(ph) * env(i, 0.002, 0.07)).toFloat() * 0.9f
        }
        mixIn(out, squish(r), n(0.02), 0.5f)
        return out
    }

    private fun slap(r: Random): FloatArray {
        val out = FloatArray(n(0.2))
        val band = Band(1.1f).apply { tune(900f) }
        var ph = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / SR
            ph += 2 * PI * (85.0 + 40 * exp(-t / 0.02)) / SR
            out[i] = band.run(r.nextFloat() * 2 - 1) * env(i, 0.0008, 0.025) * 1.5f + (sin(ph) * env(i, 0.002, 0.05)).toFloat() * 0.6f
        }
        repeat(8) { ping(out, r.nextInt(n(0.08)), 1500.0 + r.nextDouble() * 2000, 0.0012, 0.3f) }
        return out
    }

    /** Pulling a finger out: a suction "thwip" rising to a little click. */
    private fun release(r: Random): FloatArray {
        val sec = 0.07 + r.nextDouble() * 0.04
        val out = FloatArray(n(sec))
        val band = Band(5f)
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 16 == 0) band.tune(300f + 2400f * x * x)
            out[i] = band.run(r.nextFloat() * 2 - 1) * x.pow(1.5f) * 1.8f
        }
        ping(out, out.size - n(0.006), 2200.0 + r.nextDouble() * 1200, 0.001, 0.6f)
        return out
    }

    private fun blow(r: Random): FloatArray {
        val out = FloatArray(n(0.45))
        val lp = OnePole(1100f)
        var ph = 0.0
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            ph += 2 * PI * (190.0 + 40 * x + 5 * sin(i * 0.002)) / SR
            val shape = min(1f, x / 0.15f) * min(1f, (1 - x) / 0.3f)
            out[i] = (lp.low(r.nextFloat() * 2 - 1) * 0.9f + sin(ph).toFloat() * 0.18f) * shape
        }
        return out
    }

    /** A drop of dye landing: the classic rising "plip". */
    private fun plip(r: Random): FloatArray {
        val out = FloatArray(n(0.14))
        var ph = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / SR
            ph += 2 * PI * (650.0 + 1300.0 * (1 - exp(-t / 0.012))) / SR
            out[i] = (sin(ph) * env(i, 0.001, 0.03)).toFloat()
        }
        ping(out, 0, 3000.0, 0.0008, 0.3f)
        return out
    }

    private fun pour(r: Random): FloatArray {
        val out = FloatArray(n(1.3))
        val band = Band(1.6f)
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 64 == 0) band.tune(500f + 400f * sin(x * 23f) * sin(x * 7f) + 300f * x)
            val shape = min(1f, x / 0.08f) * min(1f, (1 - x) / 0.35f)
            out[i] = band.run(r.nextFloat() * 2 - 1) * shape * 1.3f
        }
        repeat(16) { k -> mixIn(out, blop(r), n(0.05 + k * 0.07 + r.nextDouble() * 0.04), 0.22f) }
        return out
    }

    private fun swish(r: Random): FloatArray {
        val out = FloatArray(n(0.28))
        val band = Band(2.2f)
        val f0 = 450f + r.nextFloat() * 300
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 32 == 0) band.tune(f0 + 700f * sin(x * PI.toFloat()))
            val shape = sin(x * PI.toFloat())
            out[i] = band.run(r.nextFloat() * 2 - 1) * shape * 1.4f
        }
        repeat(4) { ping(out, r.nextInt(out.size - 500), 1400.0 + r.nextDouble() * 1200, 0.0012, 0.15f) }
        return out
    }

    private fun tap(): FloatArray {
        val out = FloatArray(n(0.05))
        ping(out, 0, 1900.0, 0.008, 0.8f)
        ping(out, 0, 3800.0, 0.003, 0.25f)
        return out
    }

    private fun chord(notes: List<Pair<Double, Double>>, sec: Double, decay: Double): FloatArray {
        val out = FloatArray(n(sec))
        for ((k, nt) in notes.withIndex()) mixIn(out, bellNote(nt.first, sec - nt.second, decay, 0.7), n(nt.second), 0.5f + 0.1f * k / notes.size)
        return out
    }

    private fun sparkle(r: Random): FloatArray {
        val out = FloatArray(n(1.0))
        val scale = listOf(1318.5, 1568.0, 1760.0, 2093.0, 2349.3, 2637.0, 3136.0)
        for (k in 0 until 7) mixIn(out, bellNote(scale[(k * 3 + r.nextInt(2)) % scale.size], 0.6, 0.18, 0.5), n(k * 0.055), 0.4f)
        mixIn(out, bellNote(1046.5, 0.9, 0.4, 0.6), n(0.36), 0.5f)
        return out
    }

    private fun fanfare(): FloatArray {
        val out = FloatArray(n(1.6))
        for ((k, f) in listOf(523.25, 659.25, 783.99, 1046.5).withIndex()) mixIn(out, bellNote(f, 0.5, 0.15, 0.8), n(k * 0.08), 0.45f)
        for (f in listOf(1046.5, 1318.5, 1568.0, 2093.0)) mixIn(out, bellNote(f, 1.2, 0.45, 0.6), n(0.36), 0.32f)
        return out
    }

    /** A squeaky toy hammer: a rubbery wobbling tone that bends up then down. */
    private fun squeak(r: Random, pitch: Float): FloatArray {
        val out = FloatArray(n(0.22))
        var ph = 0.0
        val f0 = (900.0 + r.nextDouble() * 120) * pitch
        for (i in out.indices) {
            val x = i / out.size.toDouble()
            val f = f0 * (1 + 0.5 * sin(PI * x)) * (1 + 0.04 * sin(2 * PI * 38 * x))
            ph += 2 * PI * f / SR
            val a = min(1.0, x / 0.04) * (1 - x).pow(1.5)
            out[i] = (a * (sin(ph) + 0.35 * sin(2 * ph) + 0.15 * sin(3 * ph))).toFloat()
        }
        return out
    }

    /** A hollow bonk: a low knock with a woody resonance. */
    private fun bonk(r: Random): FloatArray {
        val out = FloatArray(n(0.18))
        var ph = 0.0
        for (i in out.indices) {
            val t = i.toDouble() / SR
            ph += 2 * PI * (180.0 + 120 * exp(-t / 0.01)) / SR
            out[i] = (sin(ph) * env(i, 0.0005, 0.045)).toFloat()
        }
        ping(out, 0, 620.0, 0.03, 0.5f)
        ping(out, 0, 1240.0, 0.012, 0.2f)
        return out
    }

    /** Air rushing out of a popped slime bubble: noise sweeping down and fading. */
    private fun hiss(r: Random): FloatArray {
        val out = FloatArray(n(0.7))
        val band = Band(1.4f)
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 32 == 0) band.tune(3200f - 2400f * x)
            out[i] = band.run(r.nextFloat() * 2 - 1) * min(1f, x / 0.02f) * (1 - x).pow(2f) * 1.4f
        }
        return out
    }

    private fun whoosh(r: Random): FloatArray {
        val out = FloatArray(n(0.4))
        val band = Band(1.4f)
        for (i in out.indices) {
            val x = i / out.size.toFloat()
            if (i % 32 == 0) band.tune(300f * (30f).pow(x))
            out[i] = band.run(r.nextFloat() * 2 - 1) * sin(x * PI.toFloat()) * 1.2f
        }
        return out
    }

    /** One cycle of a soft tone for the ambient pads (sine plus a whisper of octave). */
    fun padTable(size: Int = 4096) = FloatArray(size) { i ->
        val a = 2 * PI * i / size
        (sin(a) * 0.85 + sin(2 * a) * 0.12 + sin(3 * a) * 0.03).toFloat()
    }

    internal fun rms(x: FloatArray) = sqrt(x.fold(0.0) { s, v -> s + v * v } / x.size.coerceAtLeast(1))
}
