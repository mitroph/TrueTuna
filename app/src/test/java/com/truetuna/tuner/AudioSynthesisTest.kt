package com.truetuna.tuner

import com.truetuna.tuner.audio.InstrumentVoice
import com.truetuna.tuner.audio.MetronomeRenderer
import com.truetuna.tuner.audio.PitchDetector
import com.truetuna.tuner.audio.Timbre
import com.truetuna.tuner.model.StringTracker
import kotlin.math.abs
import kotlin.math.log2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSynthesisTest {

    private val sr = 48000

    /** Run the tuner's own detector over a steady part of the rendered note. */
    private fun measure(buf: FloatArray, startSec: Double): Float {
        val d = PitchDetector(sr)
        val window = maxOf(4096, d.minWindowSize)
        var pos = (startSec * sr).toInt()
        var out = 0f
        repeat(8) {
            out = d.detect(buf.copyOfRange(pos, pos + window))
            pos += 1024
        }
        return out
    }

    private fun assertPitch(timbre: Timbre, freqs: List<Float>, startSec: Double) {
        for (f in freqs) {
            val note = InstrumentVoice.render(timbre, f, sr)
            val hz = measure(note, startSec)
            val cents = 1200 * log2(hz / f)
            assertTrue("$timbre f=$f detected=$hz cents=$cents", abs(cents) < 1.0)
        }
    }

    @Test
    fun guitarReferenceIsInTune() =
        assertPitch(Timbre.ACOUSTIC_GUITAR, listOf(82.41f, 110f, 146.83f, 196f, 246.94f, 329.63f), 0.3)

    @Test
    fun bassReferenceIsInTune() =
        assertPitch(Timbre.BASS_GUITAR, listOf(30.87f, 41.20f, 55f, 73.42f, 98f), 0.4)

    @Test
    fun ukuleleReferenceIsInTune() =
        assertPitch(Timbre.UKULELE, listOf(392f, 261.63f, 329.63f, 440f), 0.2)

    @Test
    fun violinReferenceIsInTune() =
        assertPitch(Timbre.VIOLIN, listOf(196f, 293.66f, 440f, 659.26f), 0.5)

    @Test
    fun celloReferenceIsInTune() =
        assertPitch(Timbre.CELLO, listOf(65.41f, 98f, 146.83f, 220f), 0.5)

    @Test
    fun referenceNotesAreBoundedAndEndSilent() {
        for (t in Timbre.values()) {
            val note = InstrumentVoice.render(t, 110f, sr)
            assertTrue(t.name, note.all { it.isFinite() && abs(it) <= 1f })
            assertEquals(t.name, 0f, note.last(), 1e-3f)
        }
    }

    @Test
    fun metronomeBeatsAreSampleAccurateAt100Bpm() {
        val r = MetronomeRenderer(sr)
        val beats = mutableListOf<Pair<Long, Int>>()
        val block = FloatArray(256)
        // ~10 minutes of audio: 1000 beats.
        while (beats.size < 1000) r.render(block, 100, 4, true) { f, b -> beats += f to b }
        val period = 60.0 * sr / 100 // 28800 frames
        beats.forEachIndexed { i, (frame, beat) ->
            assertEquals("beat $i", (i * period).toLong(), frame)
            assertEquals(i % 4, beat)
        }
    }

    @Test
    fun metronomeHandlesFractionalPeriodsWithoutDrift() {
        val r = MetronomeRenderer(44100)
        val beats = mutableListOf<Long>()
        val block = FloatArray(256)
        while (beats.size < 500) r.render(block, 97, 3, true) { f, _ -> beats += f }
        val period = 60.0 * 44100 / 97
        beats.forEachIndexed { i, frame -> assertTrue(abs(frame - i * period) < 1.0) }
    }

    @Test
    fun metronomeTempoChangeAppliesFromLastBeat() {
        val r = MetronomeRenderer(sr)
        val beats = mutableListOf<Long>()
        val block = FloatArray(256)
        // Slow tempo, then speed up right after the first beat.
        r.render(block, 30, 4, true) { f, _ -> beats += f }
        while (beats.size < 3) r.render(block, 120, 4, true) { f, _ -> beats += f }
        assertEquals(listOf(0L, 24000L, 48000L), beats)
    }

    @Test
    fun metronomeClickIsAudibleOnlyWhenSoundEnabled() {
        val on = FloatArray(4800)
        MetronomeRenderer(sr).render(on, 100, 4, true) { _, _ -> }
        assertTrue(on.any { abs(it) > 0.1f })
        val off = FloatArray(4800)
        var beats = 0
        MetronomeRenderer(sr).render(off, 100, 4, false) { _, _ -> beats++ }
        assertTrue(off.all { it == 0f })
        assertEquals(1, beats)
    }

    // ── String tracker ───────────────────────────────────────────────────────

    private val standard = listOf(82.41f, 110f, 146.83f, 196f, 246.94f, 329.63f)

    @Test
    fun trackerNeedsConsecutiveFramesBeforeSelecting() {
        val t = StringTracker(confirmFrames = 3)
        assertEquals(-1, t.update(110f, standard))
        assertEquals(-1, t.update(110f, standard))
        assertEquals(1, t.update(110f, standard))
    }

    @Test
    fun trackerIgnoresOctaveBlipsAndAttackTransients() {
        val t = StringTracker(confirmFrames = 3)
        repeat(3) { t.update(82.41f, standard) }
        // A couple of frames at the octave (E3 ≈ D string) must not steal the low E.
        assertEquals(0, t.update(164.8f, standard))
        assertEquals(0, t.update(164.8f, standard))
        assertEquals(0, t.update(82.5f, standard))
    }

    @Test
    fun trackerKeepsDetunedStringWithinHysteresis() {
        val t = StringTracker(confirmFrames = 3, switchMarginCents = 35f)
        repeat(3) { t.update(82.41f, standard) }
        // Low E tuned 260 cents sharp: A2 is now nearer (240c vs 260c) but not by the margin.
        val f = 82.41f * Math.pow(2.0, 2.6 / 12).toFloat()
        repeat(10) { assertEquals(0, t.update(f, standard)) }
    }

    @Test
    fun trackerSwitchesOnAClearlyDifferentString() {
        val t = StringTracker(confirmFrames = 3)
        repeat(3) { t.update(82.41f, standard) }
        t.update(196f, standard)
        t.update(196f, standard)
        assertEquals(3, t.update(196f, standard))
    }

    @Test
    fun trackerResetsOnSilence() {
        val t = StringTracker(confirmFrames = 2)
        repeat(2) { t.update(82.41f, standard) }
        assertEquals(-1, t.update(0f, standard))
        assertEquals(-1, t.update(110f, standard))
        assertEquals(1, t.update(110f, standard))
    }
}
