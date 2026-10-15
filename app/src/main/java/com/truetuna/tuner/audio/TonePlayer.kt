package com.truetuna.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Plays a reference tone (pure sine or Karplus-Strong plucked string) at a given frequency.
 *
 * Usage:
 *   tonePlayer.play(frequency = 82.41f)   // E2 — sine wave
 *   tonePlayer.play(frequency = 82.41f, mode = ToneMode.PLUCK)  // plucked
 *   tonePlayer.stop()
 *
 * Thread-safe: play/stop can be called from any thread.
 */
class TonePlayer {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHUNK = 1024      // samples per write burst
        private const val FADE_SAMPLES = 512 // fade-in length in samples
    }

    private var playJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @Volatile private var currentFrequency = 0f

    /**
     * Start (or retarget) the reference tone at [frequency] Hz.
     * Calling [play] while a tone is already running will smoothly cross-fade.
     */
    fun play(frequency: Float, mode: ToneMode = ToneMode.SINE) {
        if (frequency <= 0f) { stop(); return }
        if (currentFrequency == frequency && playJob?.isActive == true) return
        currentFrequency = frequency
        playJob?.cancel()
        playJob = scope.launch {
            val bufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
            ).coerceAtLeast(CHUNK * 4)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            try {
                track.play()
                val buffer = FloatArray(CHUNK)
                var phase = 0.0
                val phaseInc = 2.0 * PI * frequency / SAMPLE_RATE

                // Karplus-Strong plucked string: delay-line initialised with noise
                val ksBuffer = if (mode == ToneMode.PLUCK) initKarplusStrong(frequency) else null
                var ksIndex = 0
                var sampleIndex = 0L

                while (isActive && currentFrequency == frequency) {
                    if (mode == ToneMode.PLUCK && sampleIndex >= SAMPLE_RATE) {
                        break
                    }
                    for (i in 0 until CHUNK) {
                        val raw: Float = when (mode) {
                            ToneMode.SINE -> sin(phase).toFloat()
                            ToneMode.PLUCK -> {
                                val ks = ksBuffer!!
                                val idx = ksIndex % ks.size
                                val next = (ksIndex + 1) % ks.size
                                val v = ks[idx]
                                // Averaging filter with slight decay for natural decay envelope
                                ks[idx] = 0.996f * 0.5f * (v + ks[next])
                                ksIndex++
                                v
                            }
                        }
                        val amplitude = when {
                            sampleIndex < FADE_SAMPLES -> 0.5f * sampleIndex.toFloat() / FADE_SAMPLES
                            else -> 0.5f
                        }
                        buffer[i] = raw * amplitude
                        phase += phaseInc
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        sampleIndex++
                    }
                    track.write(buffer, 0, CHUNK, AudioTrack.WRITE_BLOCKING)
                }

                if (mode == ToneMode.PLUCK && currentFrequency == frequency) {
                    currentFrequency = 0f
                }

                // Short fade-out to avoid clicks
                for (i in 0 until CHUNK) {
                    buffer[i] = buffer[i] * (1f - i.toFloat() / CHUNK)
                }
                track.write(buffer, 0, CHUNK, AudioTrack.WRITE_BLOCKING)
            } finally {
                try { track.stop() } catch (_: Exception) {}
                track.release()
            }
        }
    }

    /** Stop the reference tone immediately. */
    fun stop() {
        currentFrequency = 0f
        playJob?.cancel()
        playJob = null
    }

    fun release() {
        stop()
    }

    private fun initKarplusStrong(frequency: Float): FloatArray {
        val period = (SAMPLE_RATE / frequency).toInt().coerceAtLeast(2)
        return FloatArray(period) { (Math.random().toFloat() * 2f - 1f) }
    }

    enum class ToneMode { SINE, PLUCK }
}

