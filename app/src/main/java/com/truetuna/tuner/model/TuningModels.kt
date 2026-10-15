package com.truetuna.tuner.model

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// NOTE NAMES & ENHARMONICS
// ─────────────────────────────────────────────────────────────────────────────

val NOTE_NAMES_12TET = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

/**
 * Returns note name and octave for a semitone offset from C0.
 */
fun semitoneToNoteOctave(semitone: Int): Pair<String, Int> {
    val noteName = NOTE_NAMES_12TET[Math.floorMod(semitone, 12)]
    val octave = Math.floorDiv(semitone, 12)
    return Pair(noteName, octave)
}

/**
 * Convert frequency to semitone index relative to C0 (MIDI note 0 = C-1, but here C0 = 0).
 * A4 = 440 Hz. A4 is 57 semitones above C0 (A = 9, octave 4 → 9 + 4*12 = 57).
 */
fun frequencyToSemitone(freq: Float, a4Hz: Float = 440f): Int {
    if (freq <= 0f) return 0
    val semitonesFromA4 = 12.0 * log2(freq.toDouble() / a4Hz.toDouble())
    // A4 is semitone 57 from C0
    return (semitonesFromA4 + 57).roundToInt()
}

fun semitoneToFrequency(semitone: Int, a4Hz: Float = 440f): Float {
    // semitone relative to C0; A4 = 57 from C0
    val offset = semitone - 57
    return (a4Hz * 2.0.pow(offset / 12.0)).toFloat()
}

fun frequencyToCentsFromA4(freq: Float, a4Hz: Float = 440f): Double {
    if (freq <= 0f) return 0.0
    return 1200.0 * log2(freq.toDouble() / a4Hz.toDouble())
}

// ─────────────────────────────────────────────────────────────────────────────
// TUNING MODE
// ─────────────────────────────────────────────────────────────────────────────

enum class TuningMode { CHROMATIC, GUITAR }
enum class TETMode { TET_12, TET_24 }
enum class Language { RU, UA, EN }

/** Which physical instrument is selected in Guitar guided mode. */
enum class Instrument {
    GUITAR_6,   // 6-string guitar  (existing presets)
    BASS_4,     // 4-string bass
    BASS_5,     // 5-string bass
    UKULELE,    // Soprano / Concert ukulele
    UKULELE_TENOR, // Tenor ukulele
    UKULELE_BARITONE, // Baritone ukulele
    VIOLIN,     // Violin (4 strings)
    CELLO;      // Cello  (4 strings)

    /** Number of strings on this instrument. */
    val stringCount: Int get() = when (this) {
        GUITAR_6 -> 6
        BASS_4, UKULELE, UKULELE_TENOR, UKULELE_BARITONE, VIOLIN, CELLO -> 4
        BASS_5 -> 5
    }
}

/** Tuner visualization display mode. */
enum class DisplayMode { NEEDLE, STROBE }

// ─────────────────────────────────────────────────────────────────────────────
// PITCH RESULT — what the ViewModel exposes to the UI
// ─────────────────────────────────────────────────────────────────────────────

data class PitchResult(
    /** Detected frequency in Hz, or 0 if silence */
    val frequency: Float = 0f,
    /** Main note name to display, e.g. "E" (12-TET) or the nearest 24-TET step base note */
    val noteName: String = "",
    /** Octave number (e.g., 2) */
    val octave: Int = 0,
    /**
     * Deviation in cents from the displayed note.
     * 12-TET: [-50, +50]. 24-TET: [-25, +25] (relative to the nearest quarter-tone step).
     */
    val centsDeviation: Float = 0f,
    /** Quarter-tone badge: "+¼" when the displayed step is a quarter-tone above [noteName], else "" */
    val quarterToneBadge: String = "",
    /** Full 24-TET label, e.g. "C+¼" or "C#" (empty in 12-TET) */
    val quarterToneName: String = "",
    /** Deviation from the nearest 12-TET semitone, range [-50, +50] */
    val semitoneCents: Float = 0f,
    /** True if within ±2.5 cents (in-tune) */
    val isInTune: Boolean = false,
    /** True if close but not in-tune: ±2.5..±15 cents */
    val isClose: Boolean = false,
    /** Index of the guitar string this result refers to, -1 if none */
    val lockedStringIndex: Int = -1
)

/** A4 sits 57 semitones above C0. */
private const val A4_SEMITONES_FROM_C0 = 57

fun computePitchResult(
    freq: Float,
    a4Hz: Float = 440f,
    tetMode: TETMode = TETMode.TET_12,
    lockedStringIndex: Int = -1
): PitchResult {
    if (freq <= 0f) return PitchResult()

    val centsFromA4 = frequencyToCentsFromA4(freq, a4Hz)
    val centsFromC0 = centsFromA4 + A4_SEMITONES_FROM_C0 * 100.0

    // 12-TET: nearest semitone
    val semitone = (centsFromC0 / 100.0).roundToInt()
    val semitoneDev = (centsFromC0 - semitone * 100.0).toFloat()

    val noteName: String
    val octave: Int
    val deviation: Float
    var badge = ""
    var qtName = ""

    if (tetMode == TETMode.TET_24) {
        // 24-TET: nearest quarter-tone step (50 cents per step)
        val step = (centsFromC0 / 50.0).roundToInt()
        val stepDev = (centsFromC0 - step * 50.0).toFloat()
        val baseSemitone = Math.floorDiv(step, 2)
        val isMid = Math.floorMod(step, 2) == 1
        val (n, o) = semitoneToNoteOctave(baseSemitone)
        noteName = n
        octave = o
        deviation = stepDev
        if (isMid) badge = "+¼"
        qtName = if (isMid) "$n+¼" else n
    } else {
        val (n, o) = semitoneToNoteOctave(semitone)
        noteName = n
        octave = o
        deviation = semitoneDev
    }

    val absDeviation = abs(deviation)
    val isInTune = absDeviation <= 2.5f
    val isClose = !isInTune && absDeviation <= 15f

    return PitchResult(
        frequency = freq,
        noteName = noteName,
        octave = octave,
        centsDeviation = deviation,
        quarterToneBadge = badge,
        quarterToneName = qtName,
        semitoneCents = semitoneDev,
        isInTune = isInTune,
        isClose = isClose,
        lockedStringIndex = lockedStringIndex
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// GUITAR STRING MODEL
// ─────────────────────────────────────────────────────────────────────────────

data class GuitarString(
    val stringNumber: Int,   // 1 = lowest (thickest) string, 6 = highest
    val noteName: String,
    val octave: Int,
    /** Semitone index relative to C0 — used to recompute the frequency for any A4 reference. */
    val semitone: Int,
    /** Frequency for A4 = 440 Hz */
    val frequencyHz: Float
) {
    /** Target frequency for the given A4 reference. */
    fun frequencyFor(a4Hz: Float): Float = semitoneToFrequency(semitone, a4Hz)
}

data class GuitarTuning(
    val name: String,
    val categoryKey: String,
    val strings: List<GuitarString>,
    val instrument: Instrument = Instrument.GUITAR_6,
    /** True when this tuning was created by the user and stored locally. */
    val isCustom: Boolean = false
)

/**
 * Apply capo offset to a tuning: raises each string by [capoFret] semitones.
 * The returned tuning strings have updated note names, octaves, and frequencies.
 */
fun GuitarTuning.withCapo(capoFret: Int, a4Hz: Float = 440f): GuitarTuning {
    if (capoFret == 0) return this
    val newStrings = strings.map { s ->
        val newSemitone = s.semitone + capoFret
        val (newNote, newOctave) = semitoneToNoteOctave(newSemitone)
        GuitarString(
            stringNumber = s.stringNumber,
            noteName = newNote,
            octave = newOctave,
            semitone = newSemitone,
            frequencyHz = semitoneToFrequency(newSemitone, a4Hz)
        )
    }
    return copy(strings = newStrings)
}

// ─────────────────────────────────────────────────────────────────────────────
// METRONOME STATE
// ─────────────────────────────────────────────────────────────────────────────

data class MetronomeState(
    val isRunning: Boolean = false,
    val bpm: Int = 100,                   // 40–240 BPM
    val beatsPerBar: Int = 4,             // time-signature numerator: 2, 3, 4
    val currentBeat: Int = 0,             // 0-based within bar
    val isSoundEnabled: Boolean = true,   // audio click
    val isVisualEnabled: Boolean = true   // visual flash
)

// ─────────────────────────────────────────────────────────────────────────────
// TUNING STATE
// ─────────────────────────────────────────────────────────────────────────────

enum class TuningStatus { IN_TUNE, CLOSE_LOW, CLOSE_HIGH, FAR_LOW, FAR_HIGH, SILENT }

fun getTuningStatus(centsDeviation: Float): TuningStatus {
    val absCents = abs(centsDeviation)
    return when {
        absCents <= 2.5f -> TuningStatus.IN_TUNE
        absCents <= 15f -> if (centsDeviation < 0) TuningStatus.CLOSE_LOW else TuningStatus.CLOSE_HIGH
        else -> if (centsDeviation < 0) TuningStatus.FAR_LOW else TuningStatus.FAR_HIGH
    }
}
