package com.truetuna.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import kotlin.concurrent.thread

/**
 * Plays one reference note in the timbre of the instrument being tuned (see [InstrumentVoice]).
 *
 * Usage:
 *   tonePlayer.play(frequency = 82.41f, timbre = Timbre.ACOUSTIC_GUITAR) { /* note ended */ }
 *   tonePlayer.stop()
 *
 * Each note runs on its own audio thread. [stop] (or a new [play]) fades the current note out
 * instead of cutting it, which would click. Thread-safe: play/stop can be called from any thread.
 */
class TonePlayer {

    private class Session {
        @Volatile var stopRequested = false
    }

    @Volatile private var session: Session? = null

    /**
     * Start a note at [frequency] Hz. [onFinished] is invoked on the audio thread once the note
     * has finished playing or was stopped.
     */
    fun play(frequency: Float, timbre: Timbre, onFinished: () -> Unit = {}) {
        stop()
        if (frequency <= 0f) {
            onFinished()
            return
        }
        val s = Session()
        session = s
        thread(name = "TonePlayer", isDaemon = true) {
            try {
                run(s, frequency, timbre)
            } finally {
                onFinished()
            }
        }
    }

    /** Fade out and stop the current note, if any. */
    fun stop() {
        session?.stopRequested = true
        session = null
    }

    fun release() = stop()

    private fun run(s: Session, frequency: Float, timbre: Timbre) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
            .takeIf { it > 0 } ?: 48000
        val note = InstrumentVoice.render(timbre, frequency, sampleRate)
        if (s.stopRequested) return

        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        )
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, CHUNK * 4 * 2))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }

        try {
            track.play()
            val fadeLen = (FADE_SEC * sampleRate).toInt()
            var pos = 0
            var fadeStart = -1
            while (pos < note.size) {
                if (s.stopRequested && fadeStart < 0) fadeStart = pos
                val end = minOf(pos + CHUNK, note.size)
                if (fadeStart >= 0) {
                    // Apply the fade-out in place and stop once it has reached silence.
                    for (i in pos until end) {
                        val g = 1f - (i - fadeStart).toFloat() / fadeLen
                        note[i] *= g.coerceAtLeast(0f)
                    }
                }
                var offset = pos
                while (offset < end) {
                    val w = track.write(note, offset, end - offset, AudioTrack.WRITE_BLOCKING)
                    if (w < 0) return
                    offset += w
                }
                pos = end
                if (fadeStart >= 0 && pos - fadeStart >= fadeLen) break
            }
            // Let the queued audio drain before releasing the track.
            val total = pos.toLong()
            val deadline = System.currentTimeMillis() + 1000
            while (System.currentTimeMillis() < deadline &&
                (track.playbackHeadPosition.toLong() and 0xFFFFFFFFL) < total
            ) {
                if (s.stopRequested && fadeStart < 0) break
                Thread.sleep(10)
            }
        } catch (_: Exception) {
            // Audio device lost etc.: just stop.
        } finally {
            try {
                track.pause()
                track.flush()
            } catch (_: Exception) {
            }
            track.release()
        }
    }

    private companion object {
        const val CHUNK = 512
        const val FADE_SEC = 0.04
    }
}
