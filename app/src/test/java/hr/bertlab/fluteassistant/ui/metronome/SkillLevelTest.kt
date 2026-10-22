package hr.bertlab.fluteassistant.ui.metronome

import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillLevelTest {

    @Test
    fun `levels climb from beginner to master inside the supported tempo range`() {
        val tempos = SkillLevel.entries.map { it.bpm }
        assertEquals(tempos.sorted(), tempos)
        assertEquals(tempos.distinct(), tempos)
        assertTrue(tempos.all { it in MetronomeConfig.MIN_BPM..MetronomeConfig.MAX_BPM })
        assertEquals(60, SkillLevel.BEGINNER.bpm)
        assertEquals(120, SkillLevel.MASTER.bpm)
    }

    @Test
    fun `a level is current only when the tempo matches it exactly`() {
        assertEquals(SkillLevel.BEGINNER, skillLevelFor(60))
        assertEquals(SkillLevel.MASTER, skillLevelFor(120))
        assertNull(skillLevelFor(61))
        assertNull(skillLevelFor(MetronomeConfig.MIN_BPM))
    }
}
