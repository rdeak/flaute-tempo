package hr.bertlab.fluteassistant.ui.tuner

import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.NoteReading
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.pow

/** The note a flute sounds [cents] above (positive) or below A4 at 440 Hz. */
private fun offA4(cents: Double) = NoteMath.frequencyToNote(440.0 * 2.0.pow(cents / 1200.0))

/** The note a flute sounds [cents] above (positive) or below the MIDI note [midi] at A4 = 440 Hz. */
private fun off(midi: Int, cents: Double) =
    NoteMath.frequencyToNote(NoteMath.midiToFrequency(midi) * 2.0.pow(cents / 1200.0))

class TunerUiStateTest {

    @Test
    fun `cents off target follow the chosen note`() {
        val a4 = NoteMath.frequencyToNote(440.0)
        assertEquals(0.0, TunerUiState(reading = a4, targetMidi = 69).centsOffTarget!!, 0.01)
        assertEquals(200.0, TunerUiState(reading = a4, targetMidi = 67).centsOffTarget!!, 0.01)
        assertNull(TunerUiState(reading = null, targetMidi = 69).centsOffTarget)
    }

    @Test
    fun `gauge cents follow the mode`() {
        val sharpA4 = NoteMath.frequencyToNote(443.0) // about 12 cents above A4
        val identify = TunerUiState(reading = sharpA4, targetMidi = 67, mode = TunerMode.IDENTIFY)
        val target = TunerUiState(reading = sharpA4, targetMidi = 67, mode = TunerMode.TARGET)
        assertEquals(11.8, identify.gaugeCents!!, 0.1)
        assertEquals(211.8, target.gaugeCents!!, 0.1)
        assertNull(identify.copy(reading = null).gaugeCents)
    }

    @Test
    fun `status allows a child-sized wobble`() {
        fun at(cents: Double) = TunerUiState(reading = NoteMath.frequencyToNote(440.0 * 2.0.pow(cents / 1200.0)))
        assertEquals(TuningStatus.IN_TUNE, at(8.0).status)
        assertEquals(TuningStatus.IN_TUNE, at(-8.0).status)
        assertEquals(TuningStatus.HIGH, at(12.0).status)
        assertEquals(TuningStatus.LOW, at(-12.0).status)
        assertEquals(TuningStatus.NONE, TunerUiState().status)
    }

    @Test
    fun `status follows the chosen note in target mode`() {
        val a4 = NoteMath.frequencyToNote(440.0)
        assertEquals(TuningStatus.IN_TUNE, TunerUiState(reading = a4, targetMidi = 67, mode = TunerMode.IDENTIFY).status)
        assertEquals(TuningStatus.HIGH, TunerUiState(reading = a4, targetMidi = 67, mode = TunerMode.TARGET).status)
    }
}

class TunerUiStateMismatchTest {
    private val a4 = NoteMath.frequencyToNote(440.0)

    @Test
    fun `silence and identify mode report no mismatch`() {
        assertEquals(TargetMismatch.NONE, TunerUiState(reading = null, targetMidi = 69, mode = TunerMode.TARGET).mismatch)
        assertEquals(TargetMismatch.NONE, TunerUiState(reading = a4, targetMidi = 60, mode = TunerMode.IDENTIFY).mismatch)
    }

    @Test
    fun `the chosen note played sharp or flat is still the same note`() {
        val sharp = NoteMath.frequencyToNote(452.0) // about 47 cents above A4
        assertEquals(TargetMismatch.NONE, TunerUiState(reading = sharp, targetMidi = 69, mode = TunerMode.TARGET).mismatch)
    }

    @Test
    fun `the octave above or below the chosen note is told apart from any other note`() {
        assertEquals(TargetMismatch.OCTAVE_UP, TunerUiState(reading = a4, targetMidi = 57, mode = TunerMode.TARGET).mismatch)
        assertEquals(TargetMismatch.OCTAVE_DOWN, TunerUiState(reading = a4, targetMidi = 81, mode = TunerMode.TARGET).mismatch)
        assertEquals(TargetMismatch.OTHER_NOTE, TunerUiState(reading = a4, targetMidi = 71, mode = TunerMode.TARGET).mismatch)
        assertEquals(TargetMismatch.OTHER_NOTE, TunerUiState(reading = a4, targetMidi = 70, mode = TunerMode.TARGET).mismatch)
    }

    @Test
    fun `the chosen note played past the halfway point is still the chosen note`() {
        fun mismatchAt(cents: Double) = TunerUiState(reading = offA4(cents), targetMidi = 69, mode = TunerMode.TARGET).mismatch
        assertEquals(TargetMismatch.NONE, mismatchAt(-60.0)) // nearest note is G♯4, but it is a flat A4
        assertEquals(TargetMismatch.NONE, mismatchAt(69.0))
        assertEquals(TargetMismatch.OTHER_NOTE, mismatchAt(-71.0))
        assertEquals(TargetMismatch.OTHER_NOTE, mismatchAt(-100.0)) // G♯4 in tune: the fingers
        assertEquals(TuningStatus.LOW, TunerUiState(reading = offA4(-60.0), targetMidi = 69, mode = TunerMode.TARGET).status)
    }

    @Test
    fun `an octave played flat or sharp is still the octave`() {
        // c1 underblown to the octave below c2 and sixty cents flat: the nearest note is B3, but the fingers are right
        assertEquals(TargetMismatch.OCTAVE_DOWN, TunerUiState(reading = off(60, -60.0), targetMidi = 72, mode = TunerMode.TARGET).mismatch)
        assertEquals(TargetMismatch.OCTAVE_UP, TunerUiState(reading = off(84, 60.0), targetMidi = 72, mode = TunerMode.TARGET).mismatch)
        assertEquals(TargetMismatch.OTHER_NOTE, TunerUiState(reading = off(60, -71.0), targetMidi = 72, mode = TunerMode.TARGET).mismatch)
        assertEquals(TunerAdvice.FASTER_AIR, TunerUiState(reading = off(60, -60.0), targetMidi = 72, mode = TunerMode.TARGET).advice)
    }
}

class TunerUiStateAdviceTest {
    private fun target(reading: NoteReading?, targetMidi: Int = 69, hearing: Hearing = Hearing.CLEAR) =
        TunerUiState(reading = reading, targetMidi = targetMidi, mode = TunerMode.TARGET, hearing = hearing)

    @Test
    fun `silence gives no advice unless the sound is unclear`() {
        assertEquals(TunerAdvice.NONE, target(null).advice)
        assertEquals(TunerAdvice.COME_CLOSER, target(null, hearing = Hearing.QUIET_UNCLEAR).advice)
        assertEquals(TunerAdvice.CLEAR_NOTE, target(null, hearing = Hearing.LOUD_UNCLEAR).advice)
        assertEquals(TunerAdvice.COME_CLOSER, TunerUiState(mode = TunerMode.IDENTIFY, hearing = Hearing.QUIET_UNCLEAR).advice)
    }

    @Test
    fun `the reference tone silences the advice`() {
        assertEquals(TunerAdvice.NONE, target(null, hearing = Hearing.LOUD_UNCLEAR).copy(isPlayingReference = true).advice)
    }

    @Test
    fun `flat and sharp advise rolling, in both modes`() {
        assertEquals(TunerAdvice.ROLL_OUT, target(offA4(-30.0)).advice)
        assertEquals(TunerAdvice.ROLL_IN, target(offA4(30.0)).advice)
        assertEquals(TunerAdvice.ROLL_OUT, target(offA4(-60.0)).advice) // still the chosen note
        assertEquals(TunerAdvice.ROLL_OUT, TunerUiState(reading = offA4(-30.0), mode = TunerMode.IDENTIFY).advice)
        assertEquals(TunerAdvice.NONE, target(offA4(5.0)).advice) // in tune
    }

    @Test
    fun `a reading wins over unclear sound`() {
        assertEquals(TunerAdvice.ROLL_IN, target(offA4(30.0), hearing = Hearing.LOUD_UNCLEAR).advice)
    }

    @Test
    fun `octaves advise air and any other note the fingers, in target mode only`() {
        assertEquals(TunerAdvice.FASTER_AIR, target(offA4(0.0), targetMidi = 81).advice)
        assertEquals(TunerAdvice.GENTLER_AIR, target(offA4(0.0), targetMidi = 57).advice)
        assertEquals(TunerAdvice.CHECK_FINGERS, target(offA4(0.0), targetMidi = 67).advice)
        assertEquals(TunerAdvice.NONE, TunerUiState(reading = offA4(0.0), targetMidi = 67, mode = TunerMode.IDENTIFY).advice)
    }
}
