package hr.bertlab.fluteassistant.domain.flute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneTest {

    @Test
    fun `held lengths are clamped`() {
        assertEquals(300, Tune.clampHeld(10))
        assertEquals(450, Tune.clampHeld(450))
        assertEquals(2000, Tune.clampHeld(5000))
        assertEquals(300, Tune.clampHeld(-1))
    }

    @Test
    fun `encode and decode round-trip`() {
        val notes = listOf(TuneNote(60, 450), TuneNote(84, 2000))
        assertEquals("60:450,84:2000", Tune.encode(notes))
        assertEquals(notes, Tune.decode("60:450,84:2000"))
        assertEquals("", Tune.encode(emptyList()))
        assertTrue(Tune.decode("").isEmpty())
    }

    @Test
    fun `decode drops what it cannot read and ignores a pause field`() {
        assertEquals(
            listOf(TuneNote(60, 450), TuneNote(72, 300), TuneNote(74, 2000)),
            Tune.decode("60:450,x:1,61:400,67,72:300:150, ,74:5000"),
        )
    }

    @Test
    fun `sanitize keeps offered notes, clamps and caps`() {
        assertEquals(200, Tune.sanitize(List(250) { TuneNote(60, 300) }).size)
        assertEquals(listOf(TuneNote(62, 300)), Tune.sanitize(listOf(TuneNote(96, 400), TuneNote(62, 10))))
    }
}
