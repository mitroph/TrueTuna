package com.truetuna.tuner.audio

import com.truetuna.tuner.model.Instrument
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Timbre used for the reference tone ("play this string"). */
enum class Timbre { ACOUSTIC_GUITAR, BASS_GUITAR, UKULELE, VIOLIN, CELLO }

val Instrument.referenceTimbre: Timbre
    get() = when (this) {
        Instrument.GUITAR_6 -> Timbre.ACOUSTIC_GUITAR
        Instrument.BASS_4, Instrument.BASS_5 -> Timbre.BASS_GUITAR
        Instrument.UKULELE, Instrument.UKULELE_TENOR, Instrument.UKULELE_BARITONE -> Timbre.UKULELE
        Instrument.VIOLIN -> Timbre.VIOLIN
        Instrument.CELLO -> Timbre.CELLO
    }

/**
 * Offline synthesis of a single reference note that sounds like the selected instrument.
 * Pure Kotlin (no Android types) so pitch accuracy can be unit-tested.
 *
 * - Plucked instruments use extended Karplus-Strong: a frequency-dependent loss filter
 *   (separate decay times for low and high partials), a pick-position comb on the excitation,
 *   an all-pass fractional delay so the pitch is exact (plain Karplus-Strong rounds the loop to
 *   whole samples and can be 10+ cents sharp on high strings), and a few body resonances.
 * - Bowed instruments use a Helmholtz-motion (sawtooth-like) spectrum shaped by the body's
 *   resonances, a bowed attack, slight bow-pressure fluctuation and bow noise. No vibrato:
 *   a reference pitch must stay put, and open strings are not played with vibrato anyway.
 *
 * The output is peak-normalised to [PEAK] and ends in silence.
 */
object InstrumentVoice {

    private const val PEAK = 0.8f

    fun render(timbre: Timbre, frequency: Float, sampleRate: Int): FloatArray = when (timbre) {
        Timbre.ACOUSTIC_GUITAR -> renderPluck(frequency, sampleRate, GUITAR)
        Timbre.BASS_GUITAR -> renderPluck(frequency, sampleRate, BASS)
        Timbre.UKULELE -> renderPluck(frequency, sampleRate, UKULELE)
        Timbre.VIOLIN -> renderBowed(frequency, sampleRate, VIOLIN)
        Timbre.CELLO -> renderBowed(frequency, sampleRate, CELLO)
    }

    // ── Plucked strings ──────────────────────────────────────────────────────

    private class Resonance(val hz: Double, val gainDb: Double, val q: Double)

    private class PluckParams(
        val durationSec: Double,
        /** Decay time (−60 dB) of the fundamental region. */
        val t60LowSec: Double,
        /** Decay time (−60 dB) of the highest partials. */
        val t60HighSec: Double,
        /** Pick position as a fraction of the string length (controls the comb notches). */
        val pickPosition: Double,
        /** Cut-off of the excitation low-pass: lower = softer finger / felt, higher = pick. */
        val excitationCutoffHz: Double,
        val body: List<Resonance>,
        /** Final output low-pass (pickup / speaker colour). */
        val outputCutoffHz: Double
    )

    private val GUITAR = PluckParams(
        durationSec = 3.5, t60LowSec = 4.0, t60HighSec = 0.35,
        pickPosition = 0.14, excitationCutoffHz = 5000.0,
        body = listOf(Resonance(105.0, 6.0, 2.5), Resonance(210.0, 4.0, 2.0), Resonance(2800.0, 2.0, 1.0)),
        outputCutoffHz = 9000.0
    )
    private val BASS = PluckParams(
        durationSec = 4.0, t60LowSec = 6.0, t60HighSec = 0.25,
        pickPosition = 0.22, excitationCutoffHz = 1800.0,
        // Mid "growl" keeps low notes audible on phone speakers that cannot play the fundamental.
        body = listOf(Resonance(700.0, 5.0, 1.0), Resonance(1500.0, 2.0, 1.2)),
        outputCutoffHz = 4000.0
    )
    private val UKULELE = PluckParams(
        durationSec = 2.2, t60LowSec = 1.8, t60HighSec = 0.2,
        pickPosition = 0.25, excitationCutoffHz = 2500.0,
        body = listOf(Resonance(260.0, 5.0, 2.0), Resonance(520.0, 3.0, 2.0)),
        outputCutoffHz = 7000.0
    )

    private fun renderPluck(f0: Float, sr: Int, p: PluckParams): FloatArray {
        val total = (p.durationSec * sr).toInt()
        val out = FloatArray(total)
        val period = sr / f0.toDouble()

        // One-pole loss filter H(z) = g(1-a)/(1 - a z^-1): gain g at DC, g·r at Nyquist.
        val gDc = 10.0.pow(-3.0 * period / (p.t60LowSec * sr))
        val gNy = 10.0.pow(-3.0 * period / (p.t60HighSec * sr))
        val r = (gNy / gDc).coerceIn(1e-4, 1.0)
        val a = (1.0 - r) / (1.0 + r)
        val b0 = gDc * (1.0 - a)

        // Tune the loop: delay line N + loss-filter phase delay + all-pass fraction == period.
        val w = 2.0 * PI * f0 / sr
        val lossDelay = atan2(a * sin(w), 1.0 - a * cos(w)) / w
        val remaining = period - lossDelay
        var n = floor(remaining - 0.1).toInt().coerceAtLeast(2)
        var frac = remaining - n
        if (frac >= 1.1) { n++; frac-- }
        val apDelay = frac.coerceIn(0.1, 1.1)
        // Exact all-pass phase delay at w is used (not the low-frequency approximation).
        val c = allpassCoefficientFor(apDelay, w)

        // Excitation: one period of low-passed noise with a pick-position comb.
        val rng = Random(f0.toBits())
        val excLen = max(2, period.roundToInt())
        val exc = DoubleArray(excLen)
        val alphaExc = 1.0 - exp(-2.0 * PI * p.excitationCutoffHz / sr)
        var lp = 0.0
        for (i in 0 until excLen) {
            lp += alphaExc * (rng.nextDouble(-1.0, 1.0) - lp)
            exc[i] = lp
        }
        val pick = max(1, (p.pickPosition * period).roundToInt())
        val excitation = DoubleArray(excLen) { i -> exc[i] - (if (i >= pick) exc[i - pick] else 0.0) }

        val line = DoubleArray(n)
        var idx = 0
        var lossState = 0.0
        var apX1 = 0.0
        var apY1 = 0.0
        for (i in 0 until total) {
            val delayed = line[idx]
            lossState = b0 * delayed + a * lossState
            val ap = c * lossState + apX1 - c * apY1
            apX1 = lossState
            apY1 = ap
            val y = (if (i < excLen) excitation[i] else 0.0) + ap
            line[idx] = y
            idx = if (idx + 1 == n) 0 else idx + 1
            out[i] = y.toFloat()
        }

        dcBlock(out, sr)
        for (res in p.body) peakingEq(out, sr, res.hz, res.gainDb, res.q)
        onePoleLowPass(out, sr, p.outputCutoffHz)
        fadeOut(out, (0.25 * sr).toInt())
        normalize(out)
        return out
    }

    /** All-pass coefficient whose phase delay at [w] equals [delay] samples (solved by bisection). */
    private fun allpassCoefficientFor(delay: Double, w: Double): Double {
        fun phaseDelay(c: Double): Double {
            // H(z) = (c + z^-1)/(1 + c z^-1)
            val num = atan2(-sin(w), c + cos(w))
            val den = atan2(-c * sin(w), 1.0 + c * cos(w))
            return -(num - den) / w
        }
        var lo = -0.999
        var hi = 0.999
        // Phase delay decreases monotonically as c grows.
        repeat(60) {
            val mid = (lo + hi) / 2
            if (phaseDelay(mid) > delay) lo = mid else hi = mid
        }
        return (lo + hi) / 2
    }

    // ── Bowed strings ────────────────────────────────────────────────────────

    private class Formant(val hz: Double, val gain: Double, val widthOct: Double)

    private class BowParams(
        val durationSec: Double,
        val attackSec: Double,
        val releaseSec: Double,
        val baseGain: Double,
        val formants: List<Formant>,
        val rollOffHz: Double,
        val noiseLowHz: Double,
        val noiseHighHz: Double,
        val noiseLevel: Double
    )

    private val VIOLIN = BowParams(
        durationSec = 3.0, attackSec = 0.12, releaseSec = 0.3, baseGain = 0.25,
        formants = listOf(
            Formant(280.0, 1.0, 0.15),   // A0 air mode
            Formant(460.0, 0.8, 0.12),   // B1- corpus mode
            Formant(550.0, 0.9, 0.12),   // B1+ corpus mode
            Formant(2800.0, 0.9, 0.5)    // bridge hill
        ),
        rollOffHz = 6000.0, noiseLowHz = 2000.0, noiseHighHz = 6000.0, noiseLevel = 0.025
    )
    private val CELLO = BowParams(
        durationSec = 3.2, attackSec = 0.18, releaseSec = 0.35, baseGain = 0.25,
        formants = listOf(
            Formant(100.0, 1.0, 0.18),
            Formant(190.0, 0.8, 0.15),
            Formant(230.0, 0.9, 0.15),
            Formant(1300.0, 0.7, 0.5)
        ),
        rollOffHz = 3500.0, noiseLowHz = 900.0, noiseHighHz = 3000.0, noiseLevel = 0.02
    )

    private const val TABLE_SIZE = 4096

    private fun renderBowed(f0: Float, sr: Int, p: BowParams): FloatArray {
        val total = (p.durationSec * sr).toInt()
        val out = FloatArray(total)
        val rng = Random(f0.toBits())

        // Single-cycle wavetable: Helmholtz motion gives ~1/k partials; the body shapes them.
        val maxHz = min(0.45 * sr, 12000.0)
        val partials = max(1, (maxHz / f0).toInt())
        val table = DoubleArray(TABLE_SIZE + 1)
        for (k in 1..partials) {
            val hz = k * f0.toDouble()
            val amp = bodyGain(hz, p) / k
            val phase = rng.nextDouble(0.0, 2 * PI)
            val step = 2.0 * PI * k / TABLE_SIZE
            for (i in 0 until TABLE_SIZE) table[i] += amp * sin(step * i + phase)
        }
        table[TABLE_SIZE] = table[0]

        val attack = (p.attackSec * sr).toInt().coerceAtLeast(1)
        val release = (p.releaseSec * sr).toInt().coerceAtLeast(1)
        val inc = f0.toDouble() * TABLE_SIZE / sr
        var pos = 0.0

        // Slow random bow-pressure fluctuation (~±4 %), smoothed with a one-pole filter.
        val pressureAlpha = 1.0 - exp(-2.0 * PI * 6.0 / sr)
        var pressure = 0.0
        var pressureTarget = 0.0
        val retarget = sr / 12

        // Band-limited bow noise.
        val nLowAlpha = 1.0 - exp(-2.0 * PI * p.noiseHighHz / sr)
        val nHighAlpha = 1.0 - exp(-2.0 * PI * p.noiseLowHz / sr)
        var nLp = 0.0
        var nHp = 0.0

        for (i in 0 until total) {
            val ip = pos.toInt()
            val fr = pos - ip
            val s = table[ip] + (table[ip + 1] - table[ip]) * fr
            pos += inc
            if (pos >= TABLE_SIZE) pos -= TABLE_SIZE

            if (i % retarget == 0) pressureTarget = rng.nextDouble(-0.04, 0.04)
            pressure += pressureAlpha * (pressureTarget - pressure)

            val white = rng.nextDouble(-1.0, 1.0)
            nLp += nLowAlpha * (white - nLp)
            nHp += nHighAlpha * (nLp - nHp)
            val noise = (nLp - nHp) * p.noiseLevel * 8.0

            val env = when {
                i < attack -> {
                    val x = i.toDouble() / attack
                    x * x * (3 - 2 * x)
                }
                i >= total - release -> (total - i).toDouble() / release
                else -> 1.0
            }
            // A bowed attack is noisier than the sustain.
            val noiseEnv = env * (if (i < attack) 2.0 else 1.0)
            out[i] = (s * env * (1.0 + pressure) + noise * noiseEnv).toFloat()
        }
        normalize(out)
        return out
    }

    private fun bodyGain(hz: Double, p: BowParams): Double {
        var g = p.baseGain
        for (f in p.formants) {
            val oct = ln(hz / f.hz) / ln(2.0)
            g += f.gain * exp(-0.5 * (oct / f.widthOct).pow(2))
        }
        return g / (1.0 + (hz / p.rollOffHz).pow(4))
    }

    // ── Shared helpers ───────────────────────────────────────────────────────

    private fun dcBlock(buf: FloatArray, sr: Int) {
        val r = 1.0 - 2.0 * PI * 20.0 / sr
        var x1 = 0.0
        var y1 = 0.0
        for (i in buf.indices) {
            val x = buf[i].toDouble()
            val y = x - x1 + r * y1
            x1 = x
            y1 = y
            buf[i] = y.toFloat()
        }
    }

    /** RBJ peaking EQ biquad, in place. */
    private fun peakingEq(buf: FloatArray, sr: Int, hz: Double, gainDb: Double, q: Double) {
        if (hz >= sr * 0.45) return
        val amp = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * hz / sr
        val alpha = sin(w0) / (2.0 * q)
        val a0 = 1.0 + alpha / amp
        val b0 = (1.0 + alpha * amp) / a0
        val b1 = (-2.0 * cos(w0)) / a0
        val b2 = (1.0 - alpha * amp) / a0
        val a1 = (-2.0 * cos(w0)) / a0
        val a2 = (1.0 - alpha / amp) / a0
        var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0
        for (i in buf.indices) {
            val x = buf[i].toDouble()
            val y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            buf[i] = y.toFloat()
        }
    }

    private fun onePoleLowPass(buf: FloatArray, sr: Int, cutoffHz: Double) {
        val alpha = 1.0 - exp(-2.0 * PI * cutoffHz / sr)
        var y = 0.0
        for (i in buf.indices) {
            y += alpha * (buf[i] - y)
            buf[i] = y.toFloat()
        }
    }

    private fun fadeOut(buf: FloatArray, length: Int) {
        val len = min(length, buf.size)
        val start = buf.size - len
        for (i in 0 until len) {
            val g = 0.5 * (1.0 + cos(PI * i / len))
            buf[start + i] = (buf[start + i] * g).toFloat()
        }
    }

    private fun normalize(buf: FloatArray) {
        var peak = 0f
        for (v in buf) peak = max(peak, abs(v))
        if (peak < 1e-9f) return
        val g = PEAK / peak
        for (i in buf.indices) buf[i] *= g
    }
}
