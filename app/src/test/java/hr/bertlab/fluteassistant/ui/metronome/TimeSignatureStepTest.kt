package hr.bertlab.fluteassistant.ui.metronome

import hr.bertlab.fluteassistant.domain.model.TimeSignature
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeSignatureStepTest {

    @Test
    fun `next and previous walk the display order`() {
        assertEquals(TimeSignature.THREE_FOUR, TimeSignature.TWO_FOUR.next())
        assertEquals(TimeSignature.FOUR_FOUR, TimeSignature.THREE_FOUR.next())
        assertEquals(TimeSignature.TWO_FOUR, TimeSignature.THREE_FOUR.previous())
    }

    @Test
    fun `stepping wraps around at both ends`() {
        assertEquals(TimeSignature.entries.first(), TimeSignature.entries.last().next())
        assertEquals(TimeSignature.entries.last(), TimeSignature.entries.first().previous())
    }

    @Test
    fun `a full lap of next visits every signature once`() {
        val visited = generateSequence(TimeSignature.TWO_FOUR) { it.next() }.take(TimeSignature.entries.size).toList()
        assertEquals(TimeSignature.entries.toList(), visited)
    }
}
