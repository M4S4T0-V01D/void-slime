package dev.m4s4t0.voidslime.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** A cute looping tune per world, and a quick one for Bubble rush. Written by [Music] from a few choices. */
enum class Song(
    val bpm: Int,
    /** MIDI note of the key's root. */
    val root: Int,
    /** Scale degrees (of the major scale) of the four chords. */
    val chords: IntArray,
    val lead: Inst,
    val lead2: Inst?,
    val comp: Inst,
    val compStyle: Comp,
    val drums: Drums,
    val pentatonic: Boolean,
    val swing: Float,
    val seed: Int,
) {
    VOID(78, 65, intArrayOf(0, 5, 3, 4), Inst.BELL, Inst.MUSIC_BOX, Inst.PAD, Comp.PAD, Drums.SOFT, false, 0f, 11),
    MEADOW(104, 67, intArrayOf(0, 3, 4, 0), Inst.FLUTE, Inst.PLUCK, Inst.PLUCK, Comp.ARP, Drums.POP, false, 0.12f, 23),
    CANDY(118, 60, intArrayOf(0, 5, 3, 4), Inst.CHIP, Inst.MUSIC_BOX, Inst.MARIMBA, Comp.BOUNCE, Drums.POP, false, 0f, 37),
    OCEAN(90, 62, intArrayOf(0, 3, 5, 4), Inst.MARIMBA, Inst.BELL, Inst.PAD, Comp.PAD, Drums.SHAKER, false, 0.1f, 41),
    SAKURA(84, 69, intArrayOf(0, 5, 3, 4), Inst.PLUCK, Inst.FLUTE, Inst.PAD, Comp.PAD, Drums.SOFT, true, 0f, 53),
    SNOW(92, 64, intArrayOf(0, 3, 0, 4), Inst.MUSIC_BOX, Inst.BELL, Inst.PLUCK, Comp.ARP, Drums.SLEIGH, false, 0f, 67),
    BEACH(100, 65, intArrayOf(0, 4, 5, 3), Inst.MARIMBA, Inst.FLUTE, Inst.PLUCK, Comp.BOUNCE, Drums.SHAKER, true, 0.18f, 71),
    SPACE(80, 57, intArrayOf(0, 5, 3, 6), Inst.BELL, Inst.CHIP, Inst.PAD, Comp.ARP, Drums.SOFT, false, 0f, 83),
    NEON(120, 62, intArrayOf(5, 3, 0, 4), Inst.CHIP, Inst.BELL, Inst.CHIP, Comp.ARP, Drums.DANCE, false, 0f, 97),
    RUSH(144, 64, intArrayOf(0, 4, 5, 3), Inst.CHIP, Inst.MARIMBA, Inst.CHIP, Comp.BOUNCE, Drums.DANCE, true, 0f, 101),
}

enum class Inst { MUSIC_BOX, BELL, MARIMBA, PLUCK, FLUTE, CHIP, PAD, BASS }
enum class Comp { PAD, ARP, BOUNCE }
enum class Drums { SOFT, SHAKER, POP, SLEIGH, DANCE }

/**
 * Writes a [Song] into a stereo loop: a melody built from a little motif (A A' B A''), chords, a bass line and light
 * drums, all synthesized. Sixteen bars, rendered at half the mixer's rate (it plays them back interpolated).
 */
object Music {
    const val RATE = SoundBank.SR / 2
    private const val BARS = 16
    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)

    private class Buf(val n: Int) {
        val l = FloatArray(n)
        val r = FloatArray(n)
        fun add(i: Int, v: Float, pan: Float) {
            val k = ((i % n) + n) % n
            l[k] += v * (1f - pan) * 0.5f
            r[k] += v * (1f + pan) * 0.5f
        }
    }

    /** The loop, interleaved stereo, 16-bit. */
    fun render(song: Song): ShortArray {
        val rnd = Random(song.seed)
        val beat = 60f / song.bpm
        val n = (BARS * 4 * beat * RATE).toInt()
        val b = Buf(n)
        fun at(beats: Float) = (beats * beat * RATE).toInt()
        fun freq(midi: Float) = 440f * 2f.pow((midi - 69f) / 12f)
        // A note on scale step [s] (7 steps an octave) in the song's key.
        fun step(s: Int) = song.root + MAJOR[((s % 7) + 7) % 7] + 12 * Math.floorDiv(s, 7)
        fun chordTones(bar: Int): IntArray { val d = song.chords[bar % 4]; return intArrayOf(d, d + 2, d + 4) }
        fun swung(slot: Int) = slot * 0.5f + if (slot % 2 == 1) song.swing else 0f

        // ---- melody ----
        val okSteps = if (song.pentatonic) setOf(0, 1, 2, 4, 5) else (0..6).toSet()
        fun allowed(s: Int) = ((s % 7) + 7) % 7 in okSteps
        data class N(val slot: Int, val len: Int, val step: Int)
        val rhythms = listOf(
            intArrayOf(1, 0, 1, 0, 1, 1, 1, 0), intArrayOf(1, 1, 1, 0, 1, 0, 1, 0), intArrayOf(1, 0, 0, 1, 1, 0, 1, 0),
            intArrayOf(1, 0, 1, 1, 1, 0, 0, 0), intArrayOf(1, 1, 0, 1, 1, 1, 1, 0), intArrayOf(1, 0, 1, 0, 1, 0, 1, 1),
        )
        fun motif(bars: Int, startBar: Int, from: Int): List<List<N>> {
            var cur = from
            return (0 until bars).map { k ->
                val rh = rhythms[rnd.nextInt(rhythms.size)]
                val onsets = rh.indices.filter { rh[it] == 1 }
                val tones = chordTones(startBar + k)
                onsets.mapIndexed { j, slot ->
                    val len = (onsets.getOrNull(j + 1) ?: 8) - slot
                    cur = if (slot % 4 == 0) {
                        // Strong beats land on the nearest chord tone (an octave up or down as needed).
                        tones.flatMap { listOf(it, it + 7) }.filter { it in 0..12 }.minBy { abs(it - cur) + rnd.nextFloat() }
                    } else {
                        var s = cur + listOf(-2, -1, -1, 1, 1, 2)[rnd.nextInt(6)]
                        if (s < 0) s += 2
                        if (s > 11) s -= 2
                        while (!allowed(s)) s += if (s > cur) 1 else -1
                        s
                    }
                    N(slot, len, cur)
                }
            }
        }
        val a = motif(2, 0, 4)
        val a3 = motif(1, 2, a[1].last().step)
        val bSec = motif(3, 0, 7)
        // A phrase is four bars; the last bar holds a chord tone (or the root, at the end of a section).
        fun cadence(bar: Int, home: Boolean): List<N> {
            val t = if (home) 7 else chordTones(bar)[rnd.nextInt(3)]
            return listOf(N(0, 2, t + 1), N(2, 2, t + (if (home) 2 else 0)), N(4, 4, t))
        }
        val phrases = listOf(
            a + a3 + listOf(cadence(3, false)),
            a + a3 + listOf(cadence(3, true)),
            bSec + listOf(cadence(3, false)),
            a + a3 + listOf(cadence(3, true)),
        )
        for ((p, bars) in phrases.withIndex()) for ((k, notes) in bars.withIndex()) {
            val bar = p * 4 + k
            for (nt in notes) {
                val t = bar * 4 + swung(nt.slot)
                val dur = nt.len * 0.5f * beat
                val inst = if (p == 2 && song.lead2 != null) song.lead2 else song.lead
                note(b, at(t), freq(step(nt.step).toFloat()), dur, inst, 0.34f, -0.15f)
                // A sparkly echo an octave up on the last phrase.
                if (p == 3 && song.lead2 != null && nt.slot % 4 == 0) note(b, at(t), freq(step(nt.step + 7).toFloat()), dur, song.lead2, 0.1f, 0.4f)
            }
        }

        // ---- chords and bass ----
        for (bar in 0 until BARS) {
            val tones = chordTones(bar).map { step(it) - 12f }
            val t0 = bar * 4f
            when (song.compStyle) {
                Comp.PAD -> for ((i, m) in tones.withIndex()) note(b, at(t0), freq(m), 4 * beat, song.comp, 0.09f, -0.5f + i * 0.5f)
                Comp.ARP -> for (s in 0 until 8) {
                    val m = tones[listOf(0, 1, 2, 1)[s % 4]] + if (s >= 4) 12 else 0
                    note(b, at(t0 + swung(s)), freq(m), 0.5f * beat, song.comp, 0.1f, if (s % 2 == 0) 0.45f else -0.45f)
                }
                Comp.BOUNCE -> for (s in listOf(1, 3, 5, 7)) for ((i, m) in tones.withIndex()) {
                    note(b, at(t0 + swung(s)), freq(m), 0.4f * beat, song.comp, 0.06f, -0.4f + i * 0.4f)
                }
            }
            val bass = step(song.chords[bar % 4]) - 24f
            note(b, at(t0), freq(bass), 1.5f * beat, Inst.BASS, 0.32f, 0f)
            note(b, at(t0 + 2f + if (song.drums == Drums.DANCE) 0.5f else 0f), freq(bass + 7f), 1.2f * beat, Inst.BASS, 0.22f, 0f)
            if (song.compStyle == Comp.BOUNCE) note(b, at(t0 + 3.5f), freq(bass + 12f), 0.4f * beat, Inst.BASS, 0.16f, 0f)
        }

        // ---- drums ----
        for (bar in 0 until BARS) {
            val t0 = bar * 4f
            when (song.drums) {
                Drums.SOFT -> { kick(b, at(t0), 0.3f); if (bar % 2 == 1) kick(b, at(t0 + 2.5f), 0.18f) }
                Drums.SHAKER -> {
                    kick(b, at(t0), 0.35f); kick(b, at(t0 + 2f), 0.25f)
                    for (s in 0 until 8) noise(b, at(t0 + swung(s)), 0.07f, if (s % 2 == 0) 0.05f else 0.08f, 0.82f, 0.3f, rnd)
                }
                Drums.POP -> {
                    kick(b, at(t0), 0.45f); kick(b, at(t0 + 2f), 0.35f)
                    noise(b, at(t0 + 1f), 0.12f, 0.2f, 0.4f, -0.1f, rnd); noise(b, at(t0 + 3f), 0.12f, 0.2f, 0.4f, -0.1f, rnd)
                    for (s in 0 until 8) noise(b, at(t0 + swung(s)), 0.03f, 0.05f, 0.92f, 0.35f, rnd)
                }
                Drums.SLEIGH -> {
                    kick(b, at(t0), 0.28f)
                    for (s in 0 until 8) noise(b, at(t0 + swung(s)), 0.09f, if (s % 2 == 0) 0.06f else 0.04f, 0.95f, 0.4f, rnd)
                    for (s in 0 until 4) note(b, at(t0 + s), 2600f + (s % 2) * 300f, 0.15f, Inst.BELL, 0.03f, 0.5f)
                }
                Drums.DANCE -> {
                    for (k in 0 until 4) kick(b, at(t0 + k), 0.45f)
                    noise(b, at(t0 + 1f), 0.15f, 0.18f, 0.35f, -0.1f, rnd); noise(b, at(t0 + 3f), 0.15f, 0.18f, 0.35f, -0.1f, rnd)
                    for (k in 0 until 4) noise(b, at(t0 + k + 0.5f), 0.12f, 0.06f, 0.94f, 0.3f, rnd)
                }
            }
        }

        // Normalise and pack.
        var peak = 1e-6f
        for (i in 0 until n) peak = max(peak, max(abs(b.l[i]), abs(b.r[i])))
        val g = 0.9f / peak * 32767f
        return ShortArray(n * 2) { i -> ((if (i % 2 == 0) b.l[i / 2] else b.r[i / 2]) * g).toInt().coerceIn(-32767, 32767).toShort() }
    }

    private fun note(b: Buf, start: Int, f: Float, dur: Float, inst: Inst, vel: Float, pan: Float) {
        val sr = RATE.toFloat()
        val w = 2f * PI.toFloat() * f / sr
        when (inst) {
            Inst.PLUCK -> {
                // Karplus-Strong string.
                val period = max(2, (sr / f).toInt())
                val line = FloatArray(period) { (Random.nextFloat() * 2f - 1f) }
                val len = (min(dur + 0.6f, 1.8f) * sr).toInt()
                var p = 0
                var prev = 0f
                for (i in 0 until len) {
                    val v = line[p]
                    val nv = (v + prev) * 0.5f * 0.996f
                    prev = v
                    line[p] = nv
                    p = (p + 1) % period
                    val env = min(1f, i / 40f) * if (i > dur * sr) exp(-(i - dur * sr) / (0.12f * sr)) else 1f
                    b.add(start + i, v * vel * env * 0.9f, pan)
                }
            }
            else -> {
                val (attack, release, tail) = when (inst) {
                    Inst.MUSIC_BOX -> Triple(0.003f, 0f, 1.2f)
                    Inst.BELL -> Triple(0.002f, 0f, 1.6f)
                    Inst.MARIMBA -> Triple(0.002f, 0f, 0.45f)
                    Inst.FLUTE -> Triple(0.06f, 0.12f, 0f)
                    Inst.CHIP -> Triple(0.004f, 0.05f, 0f)
                    Inst.PAD -> Triple(0.5f, 0.6f, 0f)
                    Inst.BASS -> Triple(0.008f, 0.08f, 0f)
                    Inst.PLUCK -> Triple(0f, 0f, 0f)
                }
                // Decaying instruments ring for [tail]; sustained ones hold for the note and fade over [release].
                val decaying = tail > 0f
                val len = ((if (decaying) tail * 1.6f else dur + release) * sr).toInt()
                val hold = dur * sr
                var ph = 0f
                for (i in 0 until len) {
                    val t = i / sr
                    val env = min(1f, t / attack) * if (decaying) exp(-t / (tail * 0.35f)) else if (i < hold) 1f else exp(-(i - hold) / (release * sr * 0.35f))
                    if (env < 1e-4f && i > 100) break
                    val v = when (inst) {
                        Inst.MUSIC_BOX -> sin(ph) * 0.8f + sin(ph * 2f) * 0.25f * exp(-t * 6f) + sin(ph * 5.04f) * 0.12f * exp(-t * 12f)
                        Inst.BELL -> sin(ph + 1.6f * sin(ph * 3.5f) * exp(-t * 3f)) * 0.8f
                        Inst.MARIMBA -> sin(ph) + sin(ph * 4f) * 0.35f * exp(-t * 30f)
                        Inst.FLUTE -> {
                            val vib = 1f + 0.004f * sin(t * 30f) * min(1f, t * 3f)
                            ph += w * (vib - 1f)
                            sin(ph) * 0.9f + sin(ph * 2f) * 0.12f + (Random.nextFloat() - 0.5f) * 0.03f
                        }
                        Inst.CHIP -> { val c = (ph / (2f * PI.toFloat())) % 1f; (if (c < 0.25f) 0.5f else -0.17f) * 0.8f }
                        Inst.PAD -> { val c = (ph / (2f * PI.toFloat())) % 1f; (4f * abs(c - 0.5f) - 1f) * 0.6f + sin(ph * 1.003f) * 0.4f }
                        Inst.BASS -> sin(ph) * 0.9f + sin(ph * 2f) * 0.15f
                        Inst.PLUCK -> 0f
                    }
                    b.add(start + i, v * env * vel, pan)
                    ph += w
                    if (ph > 2f * PI.toFloat() * 4096f) ph -= 2f * PI.toFloat() * 4096f
                }
            }
        }
    }

    private fun kick(b: Buf, start: Int, vel: Float) {
        val sr = RATE.toFloat()
        var ph = 0f
        for (i in 0 until (0.25f * sr).toInt()) {
            val t = i / sr
            ph += 2f * PI.toFloat() * (50f + 110f * exp(-t * 35f)) / sr
            b.add(start + i, sin(ph) * exp(-t * 14f) * vel, 0f)
        }
    }

    /** A noise hit: [tone] near 1 is a thin tick (hats, shakers), lower a fuller clap. */
    private fun noise(b: Buf, start: Int, vel: Float, len: Float, tone: Float, pan: Float, rnd: Random) {
        val sr = RATE.toFloat()
        var lp = 0f
        for (i in 0 until (len * sr).toInt()) {
            val t = i / sr
            val x = rnd.nextFloat() * 2f - 1f
            lp += (x - lp) * (1f - tone)
            val hp = x - lp
            b.add(start + i, (hp * tone + lp * (1f - tone) * 2f) * exp(-t / (len * 0.3f)) * vel, pan)
        }
    }
}
