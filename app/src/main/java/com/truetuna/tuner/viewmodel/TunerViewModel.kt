package com.truetuna.tuner.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.truetuna.tuner.audio.AudioEngine
import com.truetuna.tuner.data.TuningRepository
import com.truetuna.tuner.model.GuitarTuning
import com.truetuna.tuner.model.Language
import com.truetuna.tuner.model.PitchResult
import com.truetuna.tuner.model.TETMode
import com.truetuna.tuner.model.TuningMode
import com.truetuna.tuner.model.computePitchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.ln

data class TunerUiState(
    val hasPermission: Boolean = false,
    val permissionDenied: Boolean = false,
    val tuningMode: TuningMode = TuningMode.CHROMATIC,
    val tetMode: TETMode = TETMode.TET_12,
    val language: Language = Language.RU,
    val a4Hz: Float = 440f,
    val pitchResult: PitchResult = PitchResult(),
    val selectedTuning: GuitarTuning = TuningRepository.allTunings.first(),
    val lockedStringIndex: Int = -1,  // -1 = auto detect
    val stringCentsDeviations: List<Float?> = List(6) { null }, // current deviation per string
    val allTunings: List<GuitarTuning> = TuningRepository.allTunings,
    val isListening: Boolean = false
)

class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("truetuna_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var audioEngine: AudioEngine? = null

    init {
        checkPermission()
    }

    private fun loadInitialState(): TunerUiState {
        val lang = runCatching { Language.valueOf(prefs.getString("lang", null) ?: "RU") }
            .getOrDefault(Language.RU)
        val tet = runCatching { TETMode.valueOf(prefs.getString("tet", null) ?: "TET_12") }
            .getOrDefault(TETMode.TET_12)
        val mode = runCatching { TuningMode.valueOf(prefs.getString("mode", null) ?: "CHROMATIC") }
            .getOrDefault(TuningMode.CHROMATIC)
        val a4 = prefs.getFloat("a4", 440f).coerceIn(415f, 466f)
        val tuning = TuningRepository.allTunings.firstOrNull { it.name == prefs.getString("tuning", null) }
            ?: TuningRepository.allTunings.first()
        return TunerUiState(
            language = lang,
            tetMode = tet,
            tuningMode = mode,
            a4Hz = a4,
            selectedTuning = tuning
        )
    }

    fun checkPermission() {
        val granted = ContextCompat.checkSelfPermission(
            getApplication(),
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        _uiState.update {
            it.copy(hasPermission = granted, permissionDenied = if (granted) false else it.permissionDenied)
        }
        if (granted && !_uiState.value.isListening) {
            startListening()
        }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasPermission = true, permissionDenied = false) }
        startListening()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(permissionDenied = true, hasPermission = false) }
    }

    fun startListening() {
        if (_uiState.value.isListening) return
        val engine = AudioEngine { rawFreq -> processFrequency(rawFreq) }
        val started = engine.start()
        audioEngine = if (started) engine else null
        _uiState.update { it.copy(isListening = started) }
    }

    fun stopListening() {
        audioEngine?.stop()
        audioEngine = null
        _uiState.update { it.copy(isListening = false, pitchResult = PitchResult()) }
    }

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

            // ── Guitar guided mode ───────────────────────────────────────────
            if (freq <= 0f) {
                _uiState.update { it.copy(pitchResult = PitchResult()) }
                return@launch
            }

            val strings = state.selectedTuning.strings
            val targetIndex = if (state.lockedStringIndex in strings.indices) {
                state.lockedStringIndex
            } else {
                // Auto-detect: closest string by pitch distance in cents
                strings.indices.minByOrNull { i ->
                    abs(centsBetween(freq, strings[i].frequencyFor(a4)))
                } ?: 0
            }

            val target = strings[targetIndex]
            val cents = centsBetween(freq, target.frequencyFor(a4))
            val absDev = abs(cents)
            val inTune = absDev <= 2.5f

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

            val deviations = state.stringCentsDeviations.toMutableList()
            deviations[targetIndex] = cents
            _uiState.update { it.copy(pitchResult = result, stringCentsDeviations = deviations) }
        }
    }

    private fun centsBetween(freq: Float, target: Float): Float =
        (1200.0 * ln(freq.toDouble() / target.toDouble()) / ln(2.0)).toFloat()

    fun setTuningMode(mode: TuningMode) {
        prefs.edit().putString("mode", mode.name).apply()
        _uiState.update {
            it.copy(
                tuningMode = mode,
                pitchResult = PitchResult(),
                lockedStringIndex = -1,
                stringCentsDeviations = List(6) { null }
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

    fun selectTuning(tuning: GuitarTuning) {
        prefs.edit().putString("tuning", tuning.name).apply()
        _uiState.update {
            it.copy(
                selectedTuning = tuning,
                lockedStringIndex = -1,
                stringCentsDeviations = List(6) { null },
                pitchResult = PitchResult()
            )
        }
    }

    fun lockString(index: Int) {
        val currentLock = _uiState.value.lockedStringIndex
        val newLock = if (currentLock == index) -1 else index  // tap again to unlock
        _uiState.update { it.copy(lockedStringIndex = newLock) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
