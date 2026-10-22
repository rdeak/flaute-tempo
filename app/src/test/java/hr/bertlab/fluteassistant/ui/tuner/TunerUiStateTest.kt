package hr.bertlab.fluteassistant.ui.tuner

import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
