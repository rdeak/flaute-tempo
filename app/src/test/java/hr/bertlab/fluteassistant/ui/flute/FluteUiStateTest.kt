package hr.bertlab.fluteassistant.ui.flute

import hr.bertlab.fluteassistant.domain.flute.TuneNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FluteUiStateTest {

    @Test
    fun `lit note and index follow playback first, then the held key`() {
        val tune = listOf(TuneNote(60, 300), TuneNote(67, 300))
        val playing = FluteUiState(tune = tune, heldMidi = 72, heldIndex = 1, playingIndex = 0)
        assertEquals(60, playing.litMidi)
        assertEquals(0, playing.litIndex)
        val held = FluteUiState(tune = tune, heldMidi = 72, heldIndex = 1)
        assertEquals(72, held.litMidi)
        assertEquals(1, held.litIndex)
        assertNull(FluteUiState().litMidi)
        assertNull(FluteUiState().litIndex)
    }

    @Test
    fun `empty, playing and cap flags`() {
        assertTrue(FluteUiState().isEmpty)
        assertFalse(FluteUiState().isPlaying)
        assertFalse(FluteUiState().atCap)
        assertTrue(FluteUiState(tune = listOf(TuneNote(60, 300)), playingIndex = 0).isPlaying)
        assertTrue(FluteUiState(tune = List(200) { TuneNote(60, 300) }).atCap)
    }
}
