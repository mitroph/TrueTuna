package com.truetuna.tuner.model

import kotlin.math.abs
import kotlin.math.ln

/**
 * Picks which string of the instrument is being played in auto-detect mode.
 *
 * Choosing the nearest string independently on every analysis frame (~23 ms) makes the target
 * flicker: the attack of a pluck is sharp and harmonic-rich, and a string that is far out of tune
 * sits near the midpoint between two targets. This tracker adds two guards:
 *
 *  - a new string must win [confirmFrames] frames in a row before it is selected;
 *  - while a string is selected, another one must be closer by at least [switchMarginCents]
 *    before it can take over (hysteresis), so a detuned string is not "stolen" by its neighbour.
 *
 * Silence resets the selection, so every new pluck is judged on its own.
 * Not thread-safe: call from a single thread.
 */
class StringTracker(
    private val confirmFrames: Int = 3,
    private val switchMarginCents: Float = 35f
) {
    var currentIndex: Int = -1
        private set

    private var candidate = -1
    private var candidateFrames = 0

    /**
     * Feed one detected [frequency] (0 = silence) against the target frequencies of the strings.
     * @return the selected string index, or -1 while nothing is confirmed yet.
     */
    fun update(frequency: Float, targets: List<Float>): Int {
        if (frequency <= 0f || targets.isEmpty()) {
            reset()
            return -1
        }
        if (currentIndex !in targets.indices) currentIndex = -1

        val nearest = targets.indices.minByOrNull { abs(cents(frequency, targets[it])) } ?: return -1
        if (nearest == currentIndex) {
            candidate = -1
            candidateFrames = 0
            return currentIndex
        }

        if (currentIndex >= 0) {
            val toCurrent = abs(cents(frequency, targets[currentIndex]))
            val toNearest = abs(cents(frequency, targets[nearest]))
            if (toCurrent - toNearest < switchMarginCents) {
                candidate = -1
                candidateFrames = 0
                return currentIndex
            }
        }

        if (nearest == candidate) candidateFrames++ else {
            candidate = nearest
            candidateFrames = 1
        }
        if (candidateFrames >= confirmFrames) {
            currentIndex = nearest
            candidate = -1
            candidateFrames = 0
        }
        return currentIndex
    }

    fun reset() {
        currentIndex = -1
        candidate = -1
        candidateFrames = 0
    }

    private fun cents(freq: Float, target: Float): Float =
        (1200.0 * ln(freq.toDouble() / target.toDouble()) / ln(2.0)).toFloat()
}
