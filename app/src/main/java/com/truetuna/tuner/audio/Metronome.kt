package com.truetuna.tuner.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import kotlin.concurrent.thread

/**
 * Metronome driven by the audio clock.
 *
 * A dedicated audio-priority thread streams [MetronomeRenderer] output (clicks *and* the silence
 * between them) into an AudioTrack with blocking writes, so timing is sample-accurate. The old
 * implementation slept between beats and dropped isolated clicks into an otherwise starved
 * stream: the track underran between beats and the moment each click actually started depended
 * on how the mixer recovered from the underrun, which made slower tempos (longer gaps) erratic.
 *
 * [onBeat] is invoked (on the audio thread) when the beat reaches the speaker, i.e. once the
 * playback head passes it, so the visual pulse matches what is heard.
 */
class Metronome(private val onBeat: (beatIndex: Int) -> Unit) {

    private class Session(
        @Volatile var bpm: Int,
        @Volatile var beatsPerBar: Int,
        @Volatile var soundEnabled: Boolean
    ) {
        @Volatile var running = true
    }

    @Volatile private var session: Session? = null

    fun start(bpm: Int, beatsPerBar: Int, soundEnabled: Boolean) {
        stop()
        val s = Session(bpm, beatsPerBar, soundEnabled)
        session = s
        thread(name = "Metronome", isDaemon = true) { run(s) }
    }

    /** Apply new settings to the running metronome without restarting it. */
    fun update(bpm: Int, beatsPerBar: Int, soundEnabled: Boolean) {
        session?.let {
            it.bpm = bpm
            it.beatsPerBar = beatsPerBar
            it.soundEnabled = soundEnabled
        }
    }

    fun stop() {
        session?.running = false
        session = null
    }

    fun release() = stop()

    private fun run(s: Session) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val sampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC)
            .takeIf { it > 0 } ?: 48000
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_FLOAT
        )
        val track = try {
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
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuf, BLOCK_FRAMES * 4 * 4))
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }

        val renderer = MetronomeRenderer(sampleRate)
        val block = FloatArray(BLOCK_FRAMES)
        // Beats rendered but not yet heard: (absolute frame, beat index).
        val pending = ArrayDeque<Pair<Long, Int>>()
        try {
            track.play()
            while (s.running) {
                renderer.render(block, s.bpm, s.beatsPerBar, s.soundEnabled) { frame, beat ->
                    pending.addLast(frame to beat)
                }
                var offset = 0
                while (offset < block.size && s.running) {
                    val written = track.write(block, offset, block.size - offset, AudioTrack.WRITE_BLOCKING)
                    if (written < 0) return
                    offset += written
                }
                // playbackHeadPosition is an unsigned 32-bit frame counter.
                val played = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                while (pending.isNotEmpty() && pending.first().first <= played) {
                    val beat = pending.removeFirst().second
                    if (s.running) onBeat(beat)
                }
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
        /** ~5 ms at 48 kHz: granularity of tempo changes and of beat notifications. */
        const val BLOCK_FRAMES = 256
    }
}
