package com.truetuna.tuner.data

import android.content.SharedPreferences
import com.truetuna.tuner.model.GuitarString
import com.truetuna.tuner.model.GuitarTuning
import com.truetuna.tuner.model.Instrument
import com.truetuna.tuner.model.semitoneToFrequency

/**
 * Repository of all instrument tunings (guitar, bass, ukulele, violin, cello) plus
 * user-created custom tunings persisted in SharedPreferences as a simple line-based format.
 *
 * Frequencies use equal temperament relative to A4 = 440 Hz.
 * Semitone mapping: C0 = 0, A4 = 57.
 */
object TuningRepository {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private val noteIndex = mapOf(
        "C" to 0, "C#" to 1, "Db" to 1,
        "D" to 2, "D#" to 3, "Eb" to 3,
        "E" to 4, "Fb" to 4,
        "F" to 5, "E#" to 5, "F#" to 6, "Gb" to 6,
        "G" to 7, "G#" to 8, "Ab" to 8,
        "A" to 9, "A#" to 10, "Bb" to 10,
        "B" to 11, "Cb" to 11
    )

    private fun semitone(noteName: String, octave: Int): Int =
        (noteIndex[noteName] ?: 0) + octave * 12

    private fun str(num: Int, noteName: String, octave: Int): GuitarString {
        val semi = semitone(noteName, octave)
        return GuitarString(num, noteName, octave, semi, semitoneToFrequency(semi, 440f))
    }

    // ── Guitar 6-string ──────────────────────────────────────────────────────

    private val guitarTunings: List<GuitarTuning> = buildList {
        add(GuitarTuning("Standard", "standard_drop", listOf(
            str(1, "E", 2), str(2, "A", 2), str(3, "D", 3),
            str(4, "G", 3), str(5, "B", 3), str(6, "E", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Drop D", "standard_drop", listOf(
            str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
            str(4, "G", 3), str(5, "B", 3), str(6, "E", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Double Drop D", "standard_drop", listOf(
            str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
            str(4, "G", 3), str(5, "B", 3), str(6, "D", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Drop C", "standard_drop", listOf(
            str(1, "C", 2), str(2, "G", 2), str(3, "C", 3),
            str(4, "F", 3), str(5, "A", 3), str(6, "D", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Eb / Half-Step Down", "standard_drop", listOf(
            str(1, "Eb", 2), str(2, "Ab", 2), str(3, "Db", 3),
            str(4, "Gb", 3), str(5, "Bb", 3), str(6, "Eb", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("D Standard", "standard_drop", listOf(
            str(1, "D", 2), str(2, "G", 2), str(3, "C", 3),
            str(4, "F", 3), str(5, "A", 3), str(6, "D", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Open D", "open", listOf(
            str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
            str(4, "F#", 3), str(5, "A", 3), str(6, "D", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Open G", "open", listOf(
            str(1, "D", 2), str(2, "G", 2), str(3, "D", 3),
            str(4, "G", 3), str(5, "B", 3), str(6, "D", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Open E", "open", listOf(
            str(1, "E", 2), str(2, "B", 2), str(3, "E", 3),
            str(4, "G#", 3), str(5, "B", 3), str(6, "E", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Open A", "open", listOf(
            str(1, "E", 2), str(2, "A", 2), str(3, "E", 3),
            str(4, "A", 3), str(5, "C#", 4), str(6, "E", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Open C", "open", listOf(
            str(1, "C", 2), str(2, "G", 2), str(3, "C", 3),
            str(4, "G", 3), str(5, "C", 4), str(6, "E", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Open F", "open", listOf(
            str(1, "C", 2), str(2, "F", 2), str(3, "C", 3),
            str(4, "F", 3), str(5, "A", 3), str(6, "C", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("DADGAD", "alternative", listOf(
            str(1, "D", 2), str(2, "A", 2), str(3, "D", 3),
            str(4, "G", 3), str(5, "A", 3), str(6, "D", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("Nick Drake CGCGCE", "alternative", listOf(
            str(1, "C", 2), str(2, "G", 2), str(3, "C", 3),
            str(4, "G", 3), str(5, "C", 4), str(6, "E", 4)
        ), Instrument.GUITAR_6))
        add(GuitarTuning("All Fourths", "alternative", listOf(
            str(1, "E", 2), str(2, "A", 2), str(3, "D", 3),
            str(4, "G", 3), str(5, "C", 4), str(6, "F", 4)
        ), Instrument.GUITAR_6))
    }

    // ── Bass 4-string ────────────────────────────────────────────────────────

    private val bass4Tunings: List<GuitarTuning> = listOf(
        GuitarTuning("Standard Bass", "bass", listOf(
            str(1, "E", 1), str(2, "A", 1), str(3, "D", 2), str(4, "G", 2)
        ), Instrument.BASS_4),
        GuitarTuning("Drop D Bass", "bass", listOf(
            str(1, "D", 1), str(2, "A", 1), str(3, "D", 2), str(4, "G", 2)
        ), Instrument.BASS_4),
        GuitarTuning("Eb Bass", "bass", listOf(
            str(1, "Eb", 1), str(2, "Ab", 1), str(3, "Db", 2), str(4, "Gb", 2)
        ), Instrument.BASS_4),
        GuitarTuning("D Standard Bass", "bass", listOf(
            str(1, "D", 1), str(2, "G", 1), str(3, "C", 2), str(4, "F", 2)
        ), Instrument.BASS_4),
        GuitarTuning("Drop C Bass", "bass", listOf(
            str(1, "C", 1), str(2, "G", 1), str(3, "C", 2), str(4, "F", 2)
        ), Instrument.BASS_4)
    )

    // ── Bass 5-string ────────────────────────────────────────────────────────

    private val bass5Tunings: List<GuitarTuning> = listOf(
        GuitarTuning("Standard 5-String", "bass", listOf(
            str(1, "B", 0), str(2, "E", 1), str(3, "A", 1), str(4, "D", 2), str(5, "G", 2)
        ), Instrument.BASS_5),
        GuitarTuning("High-C 5-String", "bass", listOf(
            str(1, "E", 1), str(2, "A", 1), str(3, "D", 2), str(4, "G", 2), str(5, "C", 3)
        ), Instrument.BASS_5)
    )

    // ── Ukulele ──────────────────────────────────────────────────────────────

    private val ukuleleTunings: List<GuitarTuning> = listOf(
        GuitarTuning("Soprano / Concert (GCEA)", "ukulele", listOf(
            str(1, "G", 4), str(2, "C", 4), str(3, "E", 4), str(4, "A", 4)
        ), Instrument.UKULELE),
        GuitarTuning("Soprano Low-G (gCEA)", "ukulele", listOf(
            str(1, "G", 3), str(2, "C", 4), str(3, "E", 4), str(4, "A", 4)
        ), Instrument.UKULELE),
        GuitarTuning("Tenor (re-entrant GCEA)", "ukulele", listOf(
            str(1, "G", 4), str(2, "C", 4), str(3, "E", 4), str(4, "A", 4)
        ), Instrument.UKULELE_TENOR),
        GuitarTuning("Tenor Low-G", "ukulele", listOf(
            str(1, "G", 3), str(2, "C", 4), str(3, "E", 4), str(4, "A", 4)
        ), Instrument.UKULELE_TENOR),
        GuitarTuning("Baritone (DGBE)", "ukulele", listOf(
            str(1, "D", 3), str(2, "G", 3), str(3, "B", 3), str(4, "E", 4)
        ), Instrument.UKULELE_BARITONE)
    )

    // ── Violin ───────────────────────────────────────────────────────────────

    private val violinTunings: List<GuitarTuning> = listOf(
        GuitarTuning("Violin Standard (GDAE)", "orchestral", listOf(
            str(1, "G", 3), str(2, "D", 4), str(3, "A", 4), str(4, "E", 5)
        ), Instrument.VIOLIN),
        GuitarTuning("Violin Scordatura (AEAE)", "orchestral", listOf(
            str(1, "A", 3), str(2, "E", 4), str(3, "A", 4), str(4, "E", 5)
        ), Instrument.VIOLIN)
    )

    // ── Cello ────────────────────────────────────────────────────────────────

    private val celloTunings: List<GuitarTuning> = listOf(
        GuitarTuning("Cello Standard (CGDA)", "orchestral", listOf(
            str(1, "C", 2), str(2, "G", 2), str(3, "D", 3), str(4, "A", 3)
        ), Instrument.CELLO)
    )

    // ── Combined preset catalogue ────────────────────────────────────────────

    /**
     * All built-in tunings across all instruments. 
     * The first 15 entries remain the original guitar tunings (backward-compatible with tests).
     */
    val allTunings: List<GuitarTuning> =
        guitarTunings + bass4Tunings + bass5Tunings + ukuleleTunings + violinTunings + celloTunings

    /** Guitar-only presets (15 total — backward-compatible). */
    val guitarPresets: List<GuitarTuning> get() = guitarTunings

    /** Filter built-in tunings by instrument. */
    fun forInstrument(instrument: Instrument): List<GuitarTuning> =
        allTunings.filter { it.instrument == instrument }

    fun getByCategory(key: String, instrument: Instrument = Instrument.GUITAR_6): List<GuitarTuning> =
        forInstrument(instrument).filter { it.categoryKey == key }

    // Category key lists per instrument
    val guitarCategories   = listOf("standard_drop", "open", "alternative")
    val bassCategories     = listOf("bass")
    val ukuleleCategories  = listOf("ukulele")
    val orchestralCategories = listOf("orchestral")

    fun categoriesForInstrument(instrument: Instrument): List<String> = when (instrument) {
        Instrument.GUITAR_6 -> guitarCategories
        Instrument.BASS_4, Instrument.BASS_5 -> bassCategories
        Instrument.UKULELE, Instrument.UKULELE_TENOR, Instrument.UKULELE_BARITONE -> ukuleleCategories
        Instrument.VIOLIN, Instrument.CELLO -> orchestralCategories
    }

    // Legacy accessor preserved for unit tests
    val categories: List<String> get() = guitarCategories

    // ── Custom Tuning Persistence ─────────────────────────────────────────────

    private const val PREFS_KEY_CUSTOM = "custom_tunings_v1"
    private val _customTunings = mutableListOf<GuitarTuning>()

    /** Live list of user-created tunings. */
    val customTunings: List<GuitarTuning> get() = _customTunings.toList()

    /**
     * Load custom tunings from SharedPreferences. Call once on app start.
     * Format per line: "name|INSTRUMENT_ENUM|NOTE:OCT,NOTE:OCT,..."
     */
    fun loadCustomTunings(prefs: SharedPreferences) {
        _customTunings.clear()
        val raw = prefs.getString(PREFS_KEY_CUSTOM, null) ?: return
        runCatching {
            raw.lines().filter { it.isNotBlank() }.forEach { line ->
                val parts = line.split("|")
                if (parts.size < 3) return@forEach
                val name = parts[0]
                val instrument = runCatching { Instrument.valueOf(parts[1]) }.getOrDefault(Instrument.GUITAR_6)
                val strings = parts[2].split(",").mapIndexedNotNull { idx, token ->
                    val np = token.split(":")
                    if (np.size < 2) null else {
                        val oct = np[1].toIntOrNull() ?: 0
                        val semi = semitone(np[0], oct)
                        GuitarString(idx + 1, np[0], oct, semi, semitoneToFrequency(semi, 440f))
                    }
                }
                if (strings.isNotEmpty()) {
                    _customTunings.add(GuitarTuning(name, "custom", strings, instrument, isCustom = true))
                }
            }
        }
    }

    /** Persist a new or updated custom tuning. Returns the saved instance. */
    fun saveCustomTuning(prefs: SharedPreferences, tuning: GuitarTuning): GuitarTuning {
        val saved = tuning.copy(categoryKey = "custom", isCustom = true)
        _customTunings.removeAll { it.name == saved.name && it.instrument == saved.instrument }
        _customTunings.add(saved)
        persistCustom(prefs)
        return saved
    }

    /** Remove a custom tuning by name and instrument. */
    fun deleteCustomTuning(prefs: SharedPreferences, name: String, instrument: Instrument) {
        _customTunings.removeAll { it.name == name && it.instrument == instrument }
        persistCustom(prefs)
    }

    private fun persistCustom(prefs: SharedPreferences) {
        val sb = StringBuilder()
        _customTunings.forEach { t ->
            val stringsToken = t.strings.joinToString(",") { "${it.noteName}:${it.octave}" }
            sb.appendLine("${t.name}|${t.instrument.name}|$stringsToken")
        }
        prefs.edit().putString(PREFS_KEY_CUSTOM, sb.toString()).apply()
    }
}

