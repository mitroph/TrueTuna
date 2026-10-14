package com.truetuna.tuner.audio

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * YIN pitch detector in pure Kotlin.
 * Based on "YIN, a fundamental frequency estimator for speech and music"
 * (de Cheveigné & Kawahara, 2002), with parabolic interpolation.
 *
 * Tracks ~28 Hz (below B0) up to ~1500 Hz (above E6).
 *
 * Usage: pass a window of at least [minWindowSize] samples to [detect]. The integration
 * window is fixed to (buffer.size - maxPeriod) so every lag is evaluated over the same
 * number of samples, which keeps low notes (E1, B0) accurate.
 */
class PitchDetector(private val sampleRate: Int) {

    companion object {
        /** YIN absolute threshold — lower = stricter. */
        private const val YIN_THRESHOLD = 0.15f
        /** Fallback acceptance level for the global minimum if the threshold is never crossed. */
        private const val YIN_FALLBACK_LIMIT = 0.30f
        const val MIN_FREQUENCY = 28.0
        const val MAX_FREQUENCY = 1500.0

        /** EMA smoothing factor applied to the median-filtered frequency. */
        private const val EMA_ALPHA = 0.30f
        /** If the new value is further than this from the smoothed one, snap instead of smoothing. */
        private const val SNAP_CENTS = 60.0
        /** Number of consecutive failed frames tolerated before reporting silence. */
        private const val MAX_MISSES = 4

        const val DEFAULT_NOISE_GATE_DBFS = -55.0
    }

    val minPeriod: Int = (sampleRate / MAX_FREQUENCY).toInt().coerceAtLeast(2)
    val maxPeriod: Int = (sampleRate / MIN_FREQUENCY).toInt()

    /** Smallest buffer that gives a usable integration window (≥ 1.5× the longest period). */
    val minWindowSize: Int = maxPeriod + maxPeriod / 2 + 1

    @Volatile
    private var noiseGateDbfs: Double = DEFAULT_NOISE_GATE_DBFS

    private var smoothedFrequency = 0f
    private var misses = 0
    private val medianBuffer = ArrayDeque<Float>()
    private val medianBufferSize = 3

    // CMND buffer is reused between frames to avoid allocations on the audio thread.
    private val yin = FloatArray(maxPeriod + 2)

    fun setNoiseGate(dbfs: Double) {
        noiseGateDbfs = dbfs
    }

    /**
     * Detect pitch from the most recent [buffer] of float PCM samples (range -1..1).
     * Returns the smoothed frequency in Hz, or 0f if the signal is too quiet / no pitch found.
     */
    fun detect(buffer: FloatArray): Float {
        require(buffer.size >= minWindowSize) { "buffer too small: ${buffer.size} < $minWindowSize" }

        // Noise gate on the newest ~2048 samples (most relevant for the current note).
        val dbfs = rmsDbfs(buffer, maxOf(0, buffer.size - 2048), buffer.size)
        if (dbfs < noiseGateDbfs) {
            reset()
            return 0f
        }

        val raw = yinDetect(buffer)
        if (raw == null) {
            // Short dropouts: hold the last value for a few frames to keep the UI steady.
            misses++
            if (misses > MAX_MISSES) {
                reset()
                return 0f
            }
            return smoothedFrequency
        }
        misses = 0

        if (smoothedFrequency > 0f && centsBetween(raw, smoothedFrequency) > SNAP_CENTS) {
            // New note (or octave jump): drop history so the display reacts immediately.
            medianBuffer.clear()
            smoothedFrequency = 0f
        }

        if (medianBuffer.size >= medianBufferSize) medianBuffer.removeFirst()
        medianBuffer.addLast(raw)
        val med = median(medianBuffer)

        smoothedFrequency = if (smoothedFrequency == 0f) med
        else EMA_ALPHA * med + (1f - EMA_ALPHA) * smoothedFrequency

        return smoothedFrequency
    }

    private fun rmsDbfs(buf: FloatArray, from: Int, to: Int): Double {
        var sum = 0.0
        for (i in from until to) {
            val v = buf[i].toDouble()
            sum += v * v
        }
        val rms = sqrt(sum / (to - from).coerceAtLeast(1))
        return if (rms > 1e-9) 20.0 * log10(rms) else -120.0
    }

    /**
     * Core YIN: difference function + cumulative mean normalised difference,
     * absolute threshold, parabolic interpolation. Computation stops as soon as the first
     * valid dip has been found, so high notes are cheap.
     */
    private fun yinDetect(buffer: FloatArray): Float? {
        val window = buffer.size - maxPeriod // fixed integration window
        if (window < maxPeriod / 2) return null

        yin[0] = 1f
        var runningSum = 0.0
        var foundTau = -1
        var tau = 1
        var bestTau = -1
        var bestVal = Float.MAX_VALUE
        var lastComputed = 0

        while (tau <= maxPeriod) {
            var delta = 0.0
            for (i in 0 until window) {
                val d = (buffer[i] - buffer[i + tau]).toDouble()
                delta += d * d
            }
            runningSum += delta
            val v = if (runningSum > 0.0) (delta * tau / runningSum).toFloat() else 1f
            yin[tau] = v
            lastComputed = tau

            if (tau >= minPeriod) {
                if (v < bestVal) {
                    bestVal = v
                    bestTau = tau
                }
                if (foundTau < 0) {
                    if (v < YIN_THRESHOLD) foundTau = tau
                } else {
                    // Descend to the local minimum, then stop.
                    if (v >= yin[tau - 1]) {
                        foundTau = tau - 1
                        break
                    }
                    foundTau = tau
                }
            }
            tau++
        }

        var chosen = foundTau
        if (chosen < 0) {
            // Threshold never crossed: accept the global minimum only if it is convincing.
            if (bestTau < 0 || bestVal > YIN_FALLBACK_LIMIT) return null
            chosen = bestTau
        }
        val better = parabolic(chosen, lastComputed)
        val freq = (sampleRate / better).toFloat()
        if (freq < MIN_FREQUENCY || freq > MAX_FREQUENCY) return null
        return freq
    }

    private fun parabolic(tau: Int, lastComputed: Int): Double {
        if (tau <= 1 || tau >= lastComputed) return tau.toDouble()
        val s0 = yin[tau - 1].toDouble()
        val s1 = yin[tau].toDouble()
        val s2 = yin[tau + 1].toDouble()
        val denom = 2.0 * (s0 - 2.0 * s1 + s2)
        if (abs(denom) < 1e-12) return tau.toDouble()
        val shift = (s0 - s2) / denom
        return tau + shift.coerceIn(-1.0, 1.0)
    }

    private fun median(values: Collection<Float>): Float {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2f else sorted[mid]
    }

    private fun centsBetween(a: Float, b: Float): Double =
        abs(1200.0 * ln(a.toDouble() / b.toDouble()) / ln(2.0))

    fun reset() {
        smoothedFrequency = 0f
        misses = 0
        medianBuffer.clear()
    }
}
