package com.truetuna.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Coroutine-based metronome with synthesized click.
 *
 * - Beat 0 of each bar = accented click (higher frequency + louder).
 * - Non-accent beats = soft click.
 * - [onBeat] is invoked on every beat, passing the 0-based beat index within the bar.
 * - Uses a simple exponentially-decaying sine burst for the click sound (no audio files needed).
 */
class Metronome(private val onBeat: (beatIndex: Int) -> Unit) {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CLICK_DURATION_SAMPLES = 2048  // ~46 ms click
    }

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start(bpm: Int, beatsPerBar: Int, soundEnabled: Boolean) {
        stop()
        job = scope.launch {
            val intervalMs = 60_000L / bpm.coerceIn(20, 300)
            var beatIndex = 0

            // Pre-render the two click waveforms
            val accentClick = renderClick(freq = 1200f, amplitude = 0.9f)
            val normalClick = renderClick(freq = 800f, amplitude = 0.6f)

            val bufBytes = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
            ).coerceAtLeast(CLICK_DURATION_SAMPLES * 4)

            val track = if (soundEnabled) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
                    .also { it.play() }
            } else null

            try {
                var nextBeatTime = System.currentTimeMillis()
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val sleepMs = nextBeatTime - now
                    if (sleepMs > 0) delay(sleepMs)

                    // Fire callback for UI
                    val beat = beatIndex
                    onBeat(beat)

                    // Write click audio
                    if (track != null) {
                        val click = if (beat == 0) accentClick else normalClick
                        track.write(click, 0, click.size, AudioTrack.WRITE_NON_BLOCKING)
                    }

                    beatIndex = (beatIndex + 1) % beatsPerBar
                    nextBeatTime += intervalMs
                }
            } finally {
                try { track?.stop() } catch (_: Exception) {}
                track?.release()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun release() = stop()

    /**
     * Render a single click burst: sine wave at [freq] Hz with exponential decay envelope.
     */
    private fun renderClick(freq: Float, amplitude: Float): FloatArray {
        val buf = FloatArray(CLICK_DURATION_SAMPLES)
        val phaseInc = 2.0 * PI * freq / SAMPLE_RATE
        val decay = -8.0 / CLICK_DURATION_SAMPLES   // controls click sharpness
        for (i in buf.indices) {
            val envelope = amplitude * exp(decay * i).toFloat()
            buf[i] = (sin(phaseInc * i) * envelope).toFloat()
        }
        return buf
    }
}
