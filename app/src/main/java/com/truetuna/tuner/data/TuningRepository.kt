package com.truetuna.tuner.data

import com.truetuna.tuner.model.GuitarString
import com.truetuna.tuner.model.GuitarTuning
import com.truetuna.tuner.model.semitoneToFrequency

/**
 * Repository of all guitar tunings with accurate frequencies.
 * Frequencies are computed from equal temperament formula relative to A4=440 Hz.
 *
 * Semitone mapping (0 = C0):
 *   E2 = 28, A2 = 33, D3 = 38, G3 = 43, B3 = 47, E4 = 52
 */
object TuningRepository {

    // Helper: build a GuitarString from note name + octave
    private fun str(num: Int, noteName: String, octave: Int, a4: Float = 440f): GuitarString {
        val semitone = noteOctaveToSemitone(noteName, octave)
        return GuitarString(num, noteName, octave, semitone, semitoneToFrequency(semitone, a4))
    }

    private fun noteOctaveToSemitone(noteName: String, octave: Int): Int {
        val noteIndex = mapOf(
            "C" to 0, "C#" to 1, "Db" to 1,
            "D" to 2, "D#" to 3, "Eb" to 3,
            "E" to 4, "Fb" to 4,
            "F" to 5, "E#" to 5, "F#" to 6, "Gb" to 6,
            "G" to 7, "G#" to 8, "Ab" to 8,
            "A" to 9, "A#" to 10, "Bb" to 10,
            "B" to 11, "Cb" to 11
        )
        val idx = noteIndex[noteName] ?: 0
        return idx + octave * 12
    }

    val allTunings: List<GuitarTuning> = buildList {

        // ── Standard & Drop ──────────────────────────────────────────────────
        add(GuitarTuning(
            name = "Standard",
            categoryKey = "standard_drop",
            strings = listOf(
                str(1, "E", 2), str(2, "A", 2), str(3, "D", 3),
                str(4, "G", 3), str(5, "B", 3), str(6, "E", 4)
            )
        ))
        add(GuitarTuning(
            name = "Drop D",
            categoryKey = "standard_drop",
            strings = listOf(
                str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
                str(4, "G", 3), str(5, "B", 3), str(6, "E", 4)
            )
        ))
        add(GuitarTuning(
            name = "Double Drop D",
            categoryKey = "standard_drop",
            strings = listOf(
                str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
                str(4, "G", 3), str(5, "B", 3), str(6, "D", 4)
            )
        ))
        add(GuitarTuning(
            name = "Drop C",
            categoryKey = "standard_drop",
            strings = listOf(
                str(1, "C", 2), str(2, "G", 2), str(3, "C", 3),
                str(4, "F", 3), str(5, "A", 3), str(6, "D", 4)
            )
        ))
        add(GuitarTuning(
            name = "Eb / Half-Step Down",
            categoryKey = "standard_drop",
            strings = listOf(
                str(1, "Eb", 2), str(2, "Ab", 2), str(3, "Db", 3),
                str(4, "Gb", 3), str(5, "Bb", 3), str(6, "Eb", 4)
            )
        ))
        add(GuitarTuning(
            name = "D Standard",
            categoryKey = "standard_drop",
            strings = listOf(
                str(1, "D", 2), str(2, "G", 2), str(3, "C", 3),
                str(4, "F", 3), str(5, "A", 3), str(6, "D", 4)
            )
        ))

        // ── Open Tunings ─────────────────────────────────────────────────────
        add(GuitarTuning(
            name = "Open D",
            categoryKey = "open",
            strings = listOf(
                str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
                str(4, "F#", 3), str(5, "A", 3), str(6, "D", 4)
            )
        ))
        add(GuitarTuning(
            name = "Open G",
            categoryKey = "open",
            strings = listOf(
                str(1, "D", 2), str(2, "G", 2), str(3, "D", 3),
                str(4, "G", 3), str(5, "B", 3), str(6, "D", 4)
            )
        ))
        add(GuitarTuning(
            name = "Open E",
            categoryKey = "open",
            strings = listOf(
                str(1, "E", 2), str(2, "B", 2), str(3, "E", 3),
                str(4, "G#", 3), str(5, "B", 3), str(6, "E", 4)
            )
        ))
        add(GuitarTuning(
            name = "Open A",
            categoryKey = "open",
            strings = listOf(
                str(1, "E", 2), str(2, "A", 2), str(3, "E", 3),
                str(4, "A", 3), str(5, "C#", 4), str(6, "E", 4)
            )
        ))
        add(GuitarTuning(
            name = "Open C",
            categoryKey = "open",
            strings = listOf(
                str(1, "C", 2), str(2, "G", 2), str(3, "C", 3),
                str(4, "G", 3), str(5, "C", 4), str(6, "E", 4)
            )
        ))
        add(GuitarTuning(
            name = "Open F",
            categoryKey = "open",
            strings = listOf(
                str(1, "C", 2), str(2, "F", 2), str(3, "C", 3),
                str(4, "F", 3), str(5, "A", 3), str(6, "C", 4)
            )
        ))

        // ── Alternative & Acoustic ────────────────────────────────────────────
        add(GuitarTuning(
            name = "DADGAD",
            categoryKey = "alternative",
            strings = listOf(
                str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
                str(4, "G", 3), str(5, "A", 3), str(6, "D", 4)
            )
        ))
        add(GuitarTuning(
            name = "Nick Drake CGCGCE",
            categoryKey = "alternative",
            strings = listOf(
                str(1, "C", 2), str(2, "G", 2), str(3, "C", 3),
                str(4, "G", 3), str(5, "C", 4), str(6, "E", 4)
            )
        ))
        add(GuitarTuning(
            name = "All Fourths",
            categoryKey = "alternative",
            strings = listOf(
                str(1, "E", 2), str(2, "A", 2), str(3, "D", 3),
                str(4, "G", 3), str(5, "C", 4), str(6, "F", 4)
            )
        ))
    }

    fun getByCategory(key: String): List<GuitarTuning> =
        allTunings.filter { it.categoryKey == key }

    val categories = listOf("standard_drop", "open", "alternative")
}
