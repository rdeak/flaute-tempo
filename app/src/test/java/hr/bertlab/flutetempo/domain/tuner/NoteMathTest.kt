package hr.bertlab.flutetempo.domain.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMathTest {

    @Test
    fun `440 Hz is A4 with zero cents`() {
        val reading = NoteMath.frequencyToNote(440.0)
        assertEquals("A", reading.name)
        assertEquals(4, reading.octave)
        assertEquals(69, reading.midi)
        assertEquals(0.0, reading.cents, 0.01)
        assertEquals("A4", reading.label)
        assertEquals(440.0, reading.targetFrequency, 1e-9)
    }

    @Test
    fun `middle C and octave numbering`() {
        val c4 = NoteMath.frequencyToNote(261.6256)
        assertEquals("C", c4.name)
        assertEquals(4, c4.octave)
        assertEquals(60, c4.midi)
        assertEquals(0.0, c4.cents, 0.05)

        val c6 = NoteMath.frequencyToNote(1046.502)
        assertEquals("C6", c6.label)

        val b3 = NoteMath.frequencyToNote(246.94)
        assertEquals("B3", b3.label)
    }

    @Test
    fun `sharp names`() {
        assertEquals("A♯4", NoteMath.frequencyToNote(466.16).label)
        assertEquals("F♯5", NoteMath.frequencyToNote(739.99).label)
    }

    @Test
    fun `sharp and flat deviations in cents`() {
        val sharp = NoteMath.frequencyToNote(445.0)
        assertEquals("A4", sharp.label)
        assertEquals(19.56, sharp.cents, 0.05)

        val flat = NoteMath.frequencyToNote(432.0)
        assertEquals("A4", flat.label)
        assertEquals(-31.77, flat.cents, 0.05)
    }

    @Test
    fun `deviation is never more than half a semitone`() {
        var f = 55.0
        while (f < 4000.0) {
            val cents = NoteMath.frequencyToNote(f).cents
            assert(cents > -50.0001 && cents <= 50.0001) { "cents=$cents at $f Hz" }
            f *= 1.01
        }
    }

    @Test
    fun `reference pitch can be changed`() {
        val reading = NoteMath.frequencyToNote(442.0, a4 = 442.0)
        assertEquals("A4", reading.label)
        assertEquals(0.0, reading.cents, 0.01)
        assertEquals(442.0, reading.targetFrequency, 1e-9)
    }

    @Test
    fun `label of a midi number`() {
        assertEquals("A♯4", NoteMath.labelOf(70))
        assertEquals("B3", NoteMath.labelOf(59))
        assertEquals("C-1", NoteMath.labelOf(0))
    }

    @Test
    fun `midi to frequency`() {
        assertEquals(440.0, NoteMath.midiToFrequency(69), 1e-9)
        assertEquals(261.6256, NoteMath.midiToFrequency(60), 1e-3)
        assertEquals(442.0, NoteMath.midiToFrequency(69, a4 = 442.0), 1e-9)
    }
}
