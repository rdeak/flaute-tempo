package hr.bertlab.fluteassistant.ui.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotesUiStateTest {

    private fun state(results: List<Boolean>, taps: Int) =
        NotesUiState(practiceNotes = setOf(60), currentMidi = 60, results = results, taps = taps)

    @Test
    fun `score counts first-tap answers`() {
        val s = state(results = listOf(true, false, true), taps = 5)
        assertEquals(3, s.answered)
        assertEquals(2, s.score)
    }

    @Test
    fun `round time reads as minutes and seconds`() {
        assertEquals("0:00", formatRoundTime(0L))
        assertEquals("0:07", formatRoundTime(7_400L))
        assertEquals("1:23", formatRoundTime(83_000L))
        assertEquals("12:05", formatRoundTime(725_900L))
    }

    @Test
    fun `accuracy is correct taps out of all taps`() {
        assertEquals(67, state(results = listOf(true, false), taps = 3).accuracyPercent)
        assertNull(state(results = emptyList(), taps = 0).accuracyPercent)
        assertEquals(100, state(results = List(10) { true }, taps = 10).accuracyPercent)
        assertEquals(10, NotesUiState.ROUND_LENGTH)
    }
}
