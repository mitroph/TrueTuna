package com.truetuna.tuner.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truetuna.tuner.audio.AudioEngine
import com.truetuna.tuner.audio.Metronome
import com.truetuna.tuner.audio.TonePlayer
import com.truetuna.tuner.data.TuningRepository
import com.truetuna.tuner.model.DisplayMode
import com.truetuna.tuner.model.GuitarTuning
import com.truetuna.tuner.model.Instrument
import com.truetuna.tuner.model.Language
import com.truetuna.tuner.model.MetronomeState
import com.truetuna.tuner.model.PitchResult
import com.truetuna.tuner.model.TETMode
import com.truetuna.tuner.model.TuningMode
import com.truetuna.tuner.model.computePitchResult
import com.truetuna.tuner.model.withCapo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln

// ─────────────────────────────────────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────────────────────────────────────

data class TunerUiState(
    val hasPermission: Boolean = false,
    val permissionDenied: Boolean = false,
    val tuningMode: TuningMode = TuningMode.CHROMATIC,
    val tetMode: TETMode = TETMode.TET_12,
    val language: Language = Language.RU,
    val a4Hz: Float = 440f,
    val pitchResult: PitchResult = PitchResult(),
    val selectedInstrument: Instrument = Instrument.GUITAR_6,
    val selectedTuning: GuitarTuning = TuningRepository.guitarPresets.first(),
    val lockedStringIndex: Int = -1,          // -1 = auto detect
    val stringCentsDeviations: List<Float?> = List(6) { null },
    val allTunings: List<GuitarTuning> = TuningRepository.allTunings,
    val isListening: Boolean = false,
    // ── A: DSP ────────────────────────────────────────────────────────────────
    val noiseGateDbfs: Float = -55f,          // dB noise gate threshold
    val lowPassFilterEnabled: Boolean = true,  // harmonic pre-filter
    val isReferenceTonePlaying: Boolean = false,
    // ── B: Musical ────────────────────────────────────────────────────────────
    val capoFret: Int = 0,                     // 0 = no capo
    val metronome: MetronomeState = MetronomeState(),
    // ── C: UI/UX ──────────────────────────────────────────────────────────────
    val displayMode: DisplayMode = DisplayMode.NEEDLE,
    val inTuneChimeEnabled: Boolean = true,
    val lastInTuneChimePlayed: Long = 0L      // epoch-ms guard against repeated chimes
)

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("truetuna_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    // Audio helpers
    private var audioEngine: AudioEngine? = null
    private val tonePlayer = TonePlayer()
    private val metronomeEngine = Metronome { beatIndex ->
        _uiState.update {
            it.copy(metronome = it.metronome.copy(currentBeat = beatIndex))
        }
    }

    init {
        TuningRepository.loadCustomTunings(prefs)
        checkPermission()
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun loadInitialState(): TunerUiState {
        val lang     = runCatching { Language.valueOf(prefs.getString("lang", null) ?: "RU") }.getOrDefault(Language.RU)
        val tet      = runCatching { TETMode.valueOf(prefs.getString("tet", null) ?: "TET_12") }.getOrDefault(TETMode.TET_12)
        val mode     = runCatching { TuningMode.valueOf(prefs.getString("mode", null) ?: "CHROMATIC") }.getOrDefault(TuningMode.CHROMATIC)
        val dispMode = runCatching { DisplayMode.valueOf(prefs.getString("dispMode", null) ?: "NEEDLE") }.getOrDefault(DisplayMode.NEEDLE)
        val instrument = runCatching { Instrument.valueOf(prefs.getString("instrument", null) ?: "GUITAR_6") }.getOrDefault(Instrument.GUITAR_6)
        val a4       = prefs.getFloat("a4", 440f).coerceIn(415f, 466f)
        val capo     = prefs.getInt("capo", 0).coerceIn(0, 11)
        val ngDbfs   = prefs.getFloat("noise_gate", -55f).coerceIn(-80f, -20f)
        val chime    = prefs.getBoolean("in_tune_chime", true)
        val lp       = prefs.getBoolean("low_pass", false)

        val tuning   = TuningRepository.forInstrument(instrument)
            .firstOrNull { it.name == prefs.getString("tuning_${instrument.name}", null) }
            ?: TuningRepository.forInstrument(instrument).first()

        return TunerUiState(
            language = lang,
            tetMode = tet,
            tuningMode = mode,
            displayMode = dispMode,
            a4Hz = a4,
            selectedInstrument = instrument,
            selectedTuning = tuning,
            capoFret = capo,
            noiseGateDbfs = ngDbfs,
            inTuneChimeEnabled = chime,
            lowPassFilterEnabled = lp,
            stringCentsDeviations = List(instrument.stringCount) { null }
        )
    }

    // ── Permission ────────────────────────────────────────────────────────────

    fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(), android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update {
            it.copy(hasPermission = granted, permissionDenied = if (granted) false else it.permissionDenied)
        }
        if (granted && !_uiState.value.isListening) startListening()
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true, permissionDenied = false) }
        startListening()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(permissionDenied = true, hasPermission = false) }
    }

    // ── Audio Engine ──────────────────────────────────────────────────────────

    fun startListening() {
        if (_uiState.value.isListening) return
        val state = _uiState.value
        val engine = AudioEngine(
            noiseGateDbfs = state.noiseGateDbfs.toDouble(),
            lowPassEnabled = state.lowPassFilterEnabled
        ) { rawFreq -> processFrequency(rawFreq) }
        val started = engine.start()
        audioEngine = if (started) engine else null
        _uiState.update { it.copy(isListening = started) }
    }

    fun stopListening() {
        audioEngine?.stop()
        audioEngine = null
        _uiState.update { it.copy(isListening = false, pitchResult = PitchResult()) }
    }

    // ── Core pitch processing ─────────────────────────────────────────────────

    private fun processFrequency(freq: Float) {
        viewModelScope.launch {
            if (!_uiState.value.isListening) return@launch
            val state = _uiState.value
            val a4 = state.a4Hz

            if (state.tuningMode == TuningMode.CHROMATIC) {
                val result = computePitchResult(freq, a4, state.tetMode, -1)
                _uiState.update { it.copy(pitchResult = result) }
                return@launch
            }

            // ── Guitar / instrument guided mode ──────────────────────────────
            if (freq <= 0f) {
                _uiState.update { it.copy(pitchResult = PitchResult()) }
                return@launch
            }

            // Apply capo: raise target string frequencies by capo fret
            val effectiveTuning = if (state.capoFret > 0)
                state.selectedTuning.withCapo(state.capoFret, a4)
            else state.selectedTuning

            val strings = effectiveTuning.strings
            val targetIndex = if (state.lockedStringIndex in strings.indices) {
                state.lockedStringIndex
            } else {
                strings.indices.minByOrNull { i ->
                    abs(centsBetween(freq, strings[i].frequencyFor(a4)))
                } ?: 0
            }

            val target = strings[targetIndex]
            val cents = centsBetween(freq, target.frequencyFor(a4))
            val absDev = abs(cents)
            val inTune = absDev <= 2.5f

            // In-tune chime: play once per in-tune event (guard 1 s cooldown)
            if (inTune && state.inTuneChimeEnabled) {
                val now = System.currentTimeMillis()
                if (now - state.lastInTuneChimePlayed > 1000L) {
                    tonePlayer.play(target.frequencyFor(a4) * 2f, TonePlayer.ToneMode.PLUCK)
                    _uiState.update { it.copy(lastInTuneChimePlayed = now) }
                }
            }

            val result = PitchResult(
                frequency = freq,
                noteName = target.noteName,
                octave = target.octave,
                centsDeviation = cents,
                semitoneCents = cents,
                isInTune = inTune,
                isClose = !inTune && absDev <= 15f,
                lockedStringIndex = targetIndex
            )

            val deviations = List(strings.size) { i ->
                if (i == targetIndex) cents else state.stringCentsDeviations.getOrNull(i)
            }
            _uiState.update { it.copy(pitchResult = result, stringCentsDeviations = deviations) }
        }
    }

    private fun centsBetween(freq: Float, target: Float): Float =
        (1200.0 * ln(freq.toDouble() / target.toDouble()) / ln(2.0)).toFloat()

    // ── Settings setters ──────────────────────────────────────────────────────

    fun setTuningMode(mode: TuningMode) {
        prefs.edit().putString("mode", mode.name).apply()
        _uiState.update {
            it.copy(
                tuningMode = mode,
                pitchResult = PitchResult(),
                lockedStringIndex = -1,
                stringCentsDeviations = List(it.selectedInstrument.stringCount) { null }
            )
        }
    }

    fun setTETMode(mode: TETMode) {
        prefs.edit().putString("tet", mode.name).apply()
        _uiState.update { it.copy(tetMode = mode) }
    }

    fun setLanguage(lang: Language) {
        prefs.edit().putString("lang", lang.name).apply()
        _uiState.update { it.copy(language = lang) }
    }

    fun setA4Hz(hz: Float) {
        val clamped = Math.round(hz.coerceIn(415f, 466f)).toFloat()
        prefs.edit().putFloat("a4", clamped).apply()
        _uiState.update { it.copy(a4Hz = clamped) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        prefs.edit().putString("dispMode", mode.name).apply()
        _uiState.update { it.copy(displayMode = mode) }
    }

    fun setNoiseGate(dbfs: Float) {
        val clamped = dbfs.coerceIn(-80f, -20f)
        prefs.edit().putFloat("noise_gate", clamped).apply()
        audioEngine?.setNoiseGate(clamped.toDouble())
        _uiState.update { it.copy(noiseGateDbfs = clamped) }
    }

    fun setLowPassFilter(enabled: Boolean) {
        prefs.edit().putBoolean("low_pass", enabled).apply()
        audioEngine?.setLowPassFilter(enabled)
        _uiState.update { it.copy(lowPassFilterEnabled = enabled) }
    }

    fun setInTuneChime(enabled: Boolean) {
        prefs.edit().putBoolean("in_tune_chime", enabled).apply()
        _uiState.update { it.copy(inTuneChimeEnabled = enabled) }
    }

    // ── Instrument & Tuning ───────────────────────────────────────────────────

    fun setInstrument(instrument: Instrument) {
        prefs.edit().putString("instrument", instrument.name).apply()
        val firstTuning = TuningRepository.forInstrument(instrument).firstOrNull()
            ?: TuningRepository.guitarPresets.first()
        _uiState.update {
            it.copy(
                selectedInstrument = instrument,
                selectedTuning = firstTuning,
                capoFret = 0,
                lockedStringIndex = -1,
                stringCentsDeviations = List(instrument.stringCount) { null },
                pitchResult = PitchResult()
            )
        }
    }

    fun selectTuning(tuning: GuitarTuning) {
        prefs.edit().putString("tuning_${tuning.instrument.name}", tuning.name).apply()
        _uiState.update {
            it.copy(
                selectedTuning = tuning,
                lockedStringIndex = -1,
                stringCentsDeviations = List(tuning.strings.size) { null },
                pitchResult = PitchResult()
            )
        }
    }

    fun lockString(index: Int) {
        val currentLock = _uiState.value.lockedStringIndex
        _uiState.update { it.copy(lockedStringIndex = if (currentLock == index) -1 else index) }
    }

    // ── Capo ──────────────────────────────────────────────────────────────────

    fun setCapo(fret: Int) {
        val clamped = fret.coerceIn(0, 11)
        prefs.edit().putInt("capo", clamped).apply()
        _uiState.update { it.copy(capoFret = clamped, pitchResult = PitchResult()) }
    }

    // ── Custom Tuning CRUD ────────────────────────────────────────────────────

    fun saveCustomTuning(tuning: GuitarTuning) {
        val saved = TuningRepository.saveCustomTuning(prefs, tuning)
        _uiState.update { it.copy(allTunings = TuningRepository.allTunings + TuningRepository.customTunings) }
        selectTuning(saved)
    }

    fun deleteCustomTuning(tuning: GuitarTuning) {
        TuningRepository.deleteCustomTuning(prefs, tuning.name, tuning.instrument)
        _uiState.update { it.copy(allTunings = TuningRepository.allTunings + TuningRepository.customTunings) }
    }

    // ── Reference Tone / Pitch Pipe ───────────────────────────────────────────

    fun playReferenceTone() {
        val state = _uiState.value
        if (state.isReferenceTonePlaying) {
            tonePlayer.stop()
            _uiState.update { it.copy(isReferenceTonePlaying = false) }
            return
        }
        val freq = if (state.lockedStringIndex >= 0) {
            val effectiveTuning = if (state.capoFret > 0)
                state.selectedTuning.withCapo(state.capoFret, state.a4Hz)
            else state.selectedTuning
            effectiveTuning.strings.getOrNull(state.lockedStringIndex)?.frequencyFor(state.a4Hz) ?: state.a4Hz
        } else state.a4Hz
        tonePlayer.play(freq, TonePlayer.ToneMode.SINE)
        _uiState.update { it.copy(isReferenceTonePlaying = true) }
    }

    fun stopReferenceTone() {
        tonePlayer.stop()
        _uiState.update { it.copy(isReferenceTonePlaying = false) }
    }

    // ── Metronome ─────────────────────────────────────────────────────────────

    fun toggleMetronome() {
        val metro = _uiState.value.metronome
        if (metro.isRunning) {
            metronomeEngine.stop()
            _uiState.update { it.copy(metronome = metro.copy(isRunning = false, currentBeat = 0)) }
        } else {
            metronomeEngine.start(metro.bpm, metro.beatsPerBar, metro.isSoundEnabled)
            _uiState.update { it.copy(metronome = metro.copy(isRunning = true)) }
        }
    }

    fun setMetronomeBpm(bpm: Int) {
        val clamped = bpm.coerceIn(20, 300)
        val metro = _uiState.value.metronome
        _uiState.update { it.copy(metronome = metro.copy(bpm = clamped)) }
        if (metro.isRunning) {
            metronomeEngine.stop()
            metronomeEngine.start(clamped, metro.beatsPerBar, metro.isSoundEnabled)
        }
    }

    fun setMetronomeBeatsPerBar(beats: Int) {
        val valid = beats.coerceIn(2, 8)
        val metro = _uiState.value.metronome
        _uiState.update { it.copy(metronome = metro.copy(beatsPerBar = valid)) }
        if (metro.isRunning) {
            metronomeEngine.stop()
            metronomeEngine.start(metro.bpm, valid, metro.isSoundEnabled)
        }
    }

    fun setMetronomeSound(enabled: Boolean) {
        val metro = _uiState.value.metronome
        _uiState.update { it.copy(metronome = metro.copy(isSoundEnabled = enabled)) }
        if (metro.isRunning) {
            metronomeEngine.stop()
            metronomeEngine.start(metro.bpm, metro.beatsPerBar, enabled)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        stopListening()
        tonePlayer.release()
        metronomeEngine.release()
    }
}
