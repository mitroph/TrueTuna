package com.truetuna.tuner.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * AudioEngine owns the AudioRecord lifecycle. It captures mono PCM-float audio, keeps a rolling
 * analysis window and feeds it to [PitchDetector]. The capture coroutine is the only owner of the
 * AudioRecord after start(), so it is also the one that stops and releases it.
 */
class AudioEngine(
    noiseGateDbfs: Double = PitchDetector.DEFAULT_NOISE_GATE_DBFS,
    lowPassEnabled: Boolean = true,
    private val onPitchDetected: (Float) -> Unit
) {
    constructor(onPitchDetected: (Float) -> Unit) : this(
        noiseGateDbfs = PitchDetector.DEFAULT_NOISE_GATE_DBFS,
        lowPassEnabled = true,
        onPitchDetected = onPitchDetected
    )
    companion object {
        private val SAMPLE_RATES = intArrayOf(44100, 48000)
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_FLOAT
        /** New samples per analysis step (~23 ms at 44.1 kHz). */
        private const val HOP_SAMPLES = 1024
        /** Rolling analysis window length (~93 ms at 44.1 kHz) — enough for B0/E1 periods. */
        private const val WINDOW_SAMPLES = 4096
    }

    private var captureJob: Job? = null
    private var scope: CoroutineScope? = null
    @Volatile private var isRunning = false
    @Volatile private var initialNoiseGate: Double = noiseGateDbfs
    @Volatile private var initialLowPass: Boolean = lowPassEnabled
    @Volatile private var detector: PitchDetector? = null

    fun setNoiseGate(dbfs: Double) {
        initialNoiseGate = dbfs
        detector?.setNoiseGate(dbfs)
    }

    fun setLowPassFilter(enabled: Boolean) {
        initialLowPass = enabled
        detector?.setLowPassFilter(enabled)
    }

    /**
     * Start audio capture. Must only be called when RECORD_AUDIO permission is granted.
     * @return true if recording actually started.
     */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRunning) return true

        var record: AudioRecord? = null
        var usedRate = 0
        for (rate in SAMPLE_RATES) {
            val minBuf = AudioRecord.getMinBufferSize(rate, CHANNEL_CONFIG, AUDIO_FORMAT)
            if (minBuf <= 0) continue
            val bufBytes = maxOf(minBuf, HOP_SAMPLES * 4 * 4)
            record = tryCreateAudioRecord(rate, bufBytes)
            if (record != null) {
                usedRate = rate
                break
            }
        }
        val rec: AudioRecord = record ?: return false

        val det = PitchDetector(usedRate).apply {
            setNoiseGate(initialNoiseGate)
            setLowPassFilter(initialLowPass)
        }
        detector = det
        val windowSize = maxOf(WINDOW_SAMPLES, det.minWindowSize)

        try {
            rec.startRecording()
        } catch (e: Exception) {
            rec.release()
            return false
        }
        if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            rec.release()
            return false
        }

        isRunning = true
        val newScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        scope = newScope
        captureJob = newScope.launch {
            val hop = FloatArray(HOP_SAMPLES)
            val window = FloatArray(windowSize)
            var filled = 0
            try {
                while (isActive && isRunning) {
                    val read = rec.read(hop, 0, hop.size, AudioRecord.READ_BLOCKING)
                    if (read < 0) break // error (e.g. device lost)
                    if (read == 0) continue

                    // Slide the window left by `read` samples and append the new ones.
                    System.arraycopy(window, read, window, 0, windowSize - read)
                    System.arraycopy(hop, 0, window, windowSize - read, read)
                    filled = minOf(filled + read, windowSize)
                    if (filled < windowSize) continue // wait until the window is full

                    onPitchDetected(det.detect(window))
                }
            } finally {
                try {
                    if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop()
                } catch (_: Exception) {
                }
                rec.release()
                det.reset()
                detector = null
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun tryCreateAudioRecord(sampleRate: Int, bufferBytes: Int): AudioRecord? {
        // UNPROCESSED avoids AGC / noise suppression (better for pitch); fall back to MIC.
        for (source in intArrayOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)) {
            try {
                val record = AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AUDIO_FORMAT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
                if (record.state == AudioRecord.STATE_INITIALIZED) return record
                record.release()
            } catch (_: Exception) {
                // try next source
            }
        }
        return null
    }

    /** Stop audio capture; the capture coroutine releases the AudioRecord. */
    fun stop() {
        isRunning = false
        captureJob?.cancel()
        captureJob = null
        scope = null
    }
}
