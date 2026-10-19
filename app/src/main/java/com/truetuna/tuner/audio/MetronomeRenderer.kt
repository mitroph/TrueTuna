package com.truetuna.tuner.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sample-accurate metronome core. It renders a continuous audio stream (clicks + silence),
 * so beat timing comes from the audio clock instead of thread sleeps: beats land on exact
 * sample positions and never drift. Pure Kotlin so the timing can be unit-tested.
 *
 * Tempo, metre and sound can change between [render] calls; a tempo change re-times the
 * pending beat relative to the last one, so the new tempo is heard immediately.
 * Not thread-safe: call from the audio thread only.
 */
class MetronomeRenderer(private val sampleRate: Int) {

    private val accentClick = ClickSynth.render(sampleRate, accent = true)
    private val normalClick = ClickSynth.render(sampleRate, accent = false)

    /** Absolute index of the next frame to be rendered. */
    private var framePos = 0L
    /**
     * Beats are placed at anchor + k·period, computed from the anchor each time rather than
     * accumulated, so rounding never builds up. The anchor moves to the last beat when the
     * tempo changes. [beatsSinceAnchor] is -1 before the first beat.
     */
    private var anchor = 0.0
    private var anchorPeriod = 0.0
    private var beatsSinceAnchor = -1
    private var beatIndex = 0

    private var click: FloatArray? = null
    private var clickPos = 0

    /**
     * Render the next [out].size frames. [onBeat] receives the absolute frame and the 0-based
     * beat index (0 = accented downbeat) of each beat that starts inside this block.
     */
    fun render(
        out: FloatArray,
        bpm: Int,
        beatsPerBar: Int,
        soundEnabled: Boolean,
        onBeat: (frame: Long, beatIndex: Int) -> Unit
    ) {
        val period = 60.0 * sampleRate / bpm.coerceIn(MIN_BPM, MAX_BPM)
        val bar = beatsPerBar.coerceAtLeast(1)
        if (beatIndex >= bar) beatIndex = 0
        if (beatsSinceAnchor >= 0 && period != anchorPeriod) {
            anchor += beatsSinceAnchor * anchorPeriod
            beatsSinceAnchor = 0
        }
        anchorPeriod = period

        for (i in out.indices) {
            val frame = framePos + i
            val target = if (beatsSinceAnchor < 0) 0.0 else anchor + (beatsSinceAnchor + 1) * period
            if (frame + EPSILON >= target) {
                if (beatsSinceAnchor < 0 || frame - target > 1.0) {
                    // First beat, or the tempo was raised and the target is already behind:
                    // restart the grid from this frame.
                    anchor = frame.toDouble()
                    beatsSinceAnchor = 0
                } else {
                    beatsSinceAnchor++
                }
                onBeat(frame, beatIndex)
                if (soundEnabled) {
                    click = if (beatIndex == 0) accentClick else normalClick
                    clickPos = 0
                }
                beatIndex = (beatIndex + 1) % bar
            }

            val c = click
            if (c != null && soundEnabled) {
                out[i] = c[clickPos++]
                if (clickPos >= c.size) click = null
            } else {
                out[i] = 0f
                click = null
            }
        }
        framePos += out.size
    }

    companion object {
        const val MIN_BPM = 20
        const val MAX_BPM = 300
        private const val EPSILON = 1e-6
    }
}

/**
 * Synthesised woodblock click: a few inharmonic, quickly decaying modes plus a short noise
 * transient for the "knock". Warmer and less piercing than a pure sine beep; the accented
 * downbeat is pitched about a fifth higher.
 */
object ClickSynth {

    private val MODE_RATIOS = doubleArrayOf(1.0, 1.58, 2.79)
    private val MODE_GAINS = doubleArrayOf(1.0, 0.45, 0.18)
    private val MODE_DECAY_SEC = doubleArrayOf(0.030, 0.016, 0.008)

    fun render(sampleRate: Int, accent: Boolean): FloatArray {
        val length = (0.12 * sampleRate).toInt()
        val out = FloatArray(length)
        val f0 = if (accent) 1250.0 else 840.0
        val peak = if (accent) 0.9f else 0.6f
        val attack = max(1, (0.0006 * sampleRate).toInt())
        val release = max(1, (0.02 * sampleRate).toInt())
        val noiseLen = max(1, (0.003 * sampleRate).toInt())
        val rng = Random(if (accent) 1 else 2)
        var prevNoise = 0.0

        for (i in 0 until length) {
            val t = i.toDouble() / sampleRate
            var s = 0.0
            for (m in MODE_RATIOS.indices) {
                val hz = f0 * MODE_RATIOS[m]
                if (hz >= sampleRate * 0.45) continue
                s += MODE_GAINS[m] * exp(-t / MODE_DECAY_SEC[m]) * sin(2.0 * PI * hz * t)
            }
            if (i < noiseLen) {
                // First-difference (high-passed) noise burst with a linear decay.
                val n = rng.nextDouble(-1.0, 1.0)
                s += 0.35 * (n - prevNoise) * (1.0 - i.toDouble() / noiseLen)
                prevNoise = n
            }
            val env = when {
                i < attack -> i.toDouble() / attack
                i >= length - release -> (length - i).toDouble() / release
                else -> 1.0
            }
            out[i] = (s * env).toFloat()
        }

        var top = 0f
        for (v in out) top = max(top, abs(v))
        if (top > 0f) for (i in out.indices) out[i] *= peak / top
        return out
    }
}
