package hr.bertlab.fluteassistant.domain.flute

import hr.bertlab.fluteassistant.domain.flute.NoteLength.DOTTED_HALF
import hr.bertlab.fluteassistant.domain.flute.NoteLength.EIGHTH
import hr.bertlab.fluteassistant.domain.flute.NoteLength.HALF
import hr.bertlab.fluteassistant.domain.flute.NoteLength.QUARTER
import hr.bertlab.fluteassistant.domain.flute.NoteLength.WHOLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TuneTest {

    @Test
    fun `a hold makes the nearest length, split halfway between two`() {
        assertEquals(EIGHTH, NoteLength.ofBeats(0.0))
        assertEquals(EIGHTH, NoteLength.ofBeats(0.74))
        assertEquals(QUARTER, NoteLength.ofBeats(0.75))
        assertEquals(QUARTER, NoteLength.ofBeats(1.49))
        assertEquals(HALF, NoteLength.ofBeats(1.5))
        assertEquals(DOTTED_HALF, NoteLength.ofBeats(2.5))
        assertEquals(WHOLE, NoteLength.ofBeats(3.5))
        assertEquals(WHOLE, NoteLength.ofBeats(12.0))
    }

    @Test
    fun `lengths follow the tempo`() {
        assertEquals(500.0, Tune.beatMs(120), 0.001)
        assertEquals(EIGHTH, Tune.lengthOf(374, bpm = 120))
        assertEquals(QUARTER, Tune.lengthOf(375, bpm = 120))
        assertEquals(QUARTER, Tune.lengthOf(749, bpm = 120))
        assertEquals(HALF, Tune.lengthOf(750, bpm = 120))
        assertEquals(EIGHTH, Tune.lengthOf(749, bpm = 60)) // the same hold is shorter at a slower tempo
        assertEquals(250L, Tune.durationMs(EIGHTH, bpm = 120))
        assertEquals(2000L, Tune.durationMs(WHOLE, bpm = 120))
        assertEquals(1500L, Tune.durationMs(QUARTER, bpm = 40))
    }

    @Test
    fun `a held note grows exactly when its release would make the longer length`() {
        for (bpm in listOf(40, 70, 97, 120, 133, 200)) {
            for ((step, at) in Tune.growAtMs(bpm).withIndex()) {
                assertEquals("bpm $bpm, step $step", NoteLength.entries[step + 1], Tune.lengthOf(at, bpm))
                assertEquals("bpm $bpm, step $step", NoteLength.entries[step], Tune.lengthOf(at - 1, bpm))
            }
        }
    }

    @Test
    fun `the gap is short and never more than a quarter of the note`() {
        assertEquals(80L, Tune.gapMs(500))
        assertEquals(37L, Tune.gapMs(150))
    }

    @Test
    fun `encode and decode round-trip in half beats`() {
        val notes = listOf(TuneNote(60, EIGHTH), TuneNote(67, QUARTER), TuneNote(69, DOTTED_HALF), TuneNote(84, WHOLE))
        assertEquals("60:1,67:2,69:6,84:8", Tune.encode(notes))
        assertEquals(notes, Tune.decode("60:1,67:2,69:6,84:8", legacyBpm = 120))
        assertEquals("", Tune.encode(emptyList()))
        assertTrue(Tune.decode("", legacyBpm = 120).isEmpty())
    }

    @Test
    fun `a tune saved as held times becomes lengths at the tempo given`() {
        assertEquals(
            listOf(TuneNote(60, EIGHTH), TuneNote(62, QUARTER), TuneNote(64, HALF), TuneNote(65, WHOLE)),
            Tune.decode("60:300,62:450,64:1000,65:2000", legacyBpm = 120),
        )
        assertEquals(listOf(TuneNote(60, QUARTER)), Tune.decode("60:450", legacyBpm = 100)) // 0.75 of a beat
    }

    @Test
    fun `decode drops what it cannot read, rounds odd lengths and ignores a pause field`() {
        assertEquals(
            listOf(TuneNote(60, QUARTER), TuneNote(72, HALF), TuneNote(74, HALF)),
            Tune.decode("60:2,x:1,61:2,67,72:4:150, ,74:3,76:0,77:-2", legacyBpm = 120),
        )
    }

    @Test
    fun `sanitize keeps offered notes and caps`() {
        assertEquals(200, Tune.sanitize(List(250) { TuneNote(60, QUARTER) }).size)
        assertEquals(listOf(TuneNote(62, HALF)), Tune.sanitize(listOf(TuneNote(96, QUARTER), TuneNote(62, HALF))))
    }
}
