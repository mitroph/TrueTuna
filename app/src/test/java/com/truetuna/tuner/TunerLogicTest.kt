package com.truetuna.tuner

import com.truetuna.tuner.audio.PitchDetector
import com.truetuna.tuner.data.TuningRepository
import com.truetuna.tuner.model.TETMode
import com.truetuna.tuner.model.computePitchResult
import com.truetuna.tuner.model.withCapo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TunerLogicTest {

    private fun synth(freq: Double, sampleRate: Int, size: Int): FloatArray =
        FloatArray(size) { i ->
            val t = i.toDouble() / sampleRate
            (0.5 * sin(2 * PI * freq * t) + 0.25 * sin(4 * PI * freq * t) + 0.12 * sin(6 * PI * freq * t)).toFloat()
        }

    private fun detectedHz(freq: Double): Float {
        val sr = 44100
        val d = PitchDetector(sr)
        val buf = synth(freq, sr, 4096)
        var out = 0f
        repeat(3) { out = d.detect(buf) }
        return out
    }

    @Test
    fun yinTracksGuitarRange() {
        for (f in listOf(41.20, 82.41, 110.0, 196.0, 329.63, 659.26, 1318.5)) {
            val hz = detectedHz(f)
            val cents = 1200 * log2(hz / f)
            assertTrue("f=$f detected=$hz cents=$cents", abs(cents) < 3.0)
        }
    }

    @Test
    fun silenceIsGated() {
        val d = PitchDetector(44100)
        assertEquals(0f, d.detect(FloatArray(4096)), 0f)
    }

    @Test
    fun noteMath12Tet() {
        val e2 = computePitchResult(82.41f)
        assertEquals("E", e2.noteName)
        assertEquals(2, e2.octave)
        assertTrue(abs(e2.centsDeviation) < 0.5f)

        val a4 = computePitchResult(440f)
        assertEquals("A", a4.noteName)
        assertEquals(4, a4.octave)
        assertTrue(a4.isInTune)

        val a4Shifted = computePitchResult(432f, a4Hz = 432f)
        assertEquals("A", a4Shifted.noteName)
        assertTrue(a4Shifted.isInTune)
    }

    @Test
    fun noteMath24Tet() {
        val quarterAboveA = (440.0 * Math.pow(2.0, 50.0 / 1200.0)).toFloat()
        val r = computePitchResult(quarterAboveA, tetMode = TETMode.TET_24)
        assertEquals("A", r.noteName)
        assertEquals(4, r.octave)
        assertEquals("+¼", r.quarterToneBadge)
        assertTrue(abs(r.centsDeviation) < 0.5f)

        val plain = computePitchResult(440f, tetMode = TETMode.TET_24)
        assertEquals("", plain.quarterToneBadge)
    }

    @Test
    fun repositoryHasAllTunings() {
        assertEquals(15, TuningRepository.guitarPresets.size)
        TuningRepository.guitarPresets.forEach { assertEquals(6, it.strings.size) }
        assertTrue(TuningRepository.allTunings.size >= 15)
        val std = TuningRepository.guitarPresets.first { it.name == "Standard" }
        assertEquals(82.41f, std.strings[0].frequencyHz, 0.01f)
        assertEquals(329.63f, std.strings[5].frequencyHz, 0.01f)
        assertEquals(82.41f, std.strings[0].frequencyFor(440f), 0.01f)
        // A4 = 415 lowers every target by the same ratio
        assertEquals(82.41f * 415f / 440f, std.strings[0].frequencyFor(415f), 0.05f)
    }

    @Test
    fun multiInstrumentAndCapoLogic() {
        val bass = TuningRepository.forInstrument(com.truetuna.tuner.model.Instrument.BASS_4)
        assertTrue(bass.isNotEmpty())
        assertEquals(4, bass.first().strings.size)

        val uke = TuningRepository.forInstrument(com.truetuna.tuner.model.Instrument.UKULELE)
        assertTrue(uke.isNotEmpty())
        assertEquals(4, uke.first().strings.size)

        val std = TuningRepository.guitarPresets.first { it.name == "Standard" }
        val capo2 = std.withCapo(2)
        assertEquals(std.strings.size, capo2.strings.size)
        // Standard string 0 is E2 (semitone 28). Capo 2 -> F#2 (semitone 30)
        assertEquals("F#", capo2.strings[0].noteName)
        assertEquals(2, capo2.strings[0].octave)
    }
}
