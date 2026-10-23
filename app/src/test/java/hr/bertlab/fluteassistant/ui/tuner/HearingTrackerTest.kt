package hr.bertlab.fluteassistant.ui.tuner

import org.junit.Assert.assertEquals
import org.junit.Test

class HearingTrackerTest {

    private val tracker = HearingTracker()

    private fun sound(level: Float, n: Int, decision: FrameDecision = FrameDecision.SILENT) =
        repeat(n) { tracker.onFrame(level, decision) }

    @Test
    fun `quiet sound without a note is noticed after about a second`() {
        sound(0.3f, HearingTracker.UNCLEAR_FRAMES - 1)
        assertEquals(Hearing.CLEAR, tracker.hearing)
        sound(0.3f, 1)
        assertEquals(Hearing.QUIET_UNCLEAR, tracker.hearing)
        assertEquals(0.3, tracker.levelMean, 1e-6)
    }

    @Test
    fun `loud unclear sound is told apart from quiet`() {
        sound(0.6f, HearingTracker.UNCLEAR_FRAMES, FrameDecision.LOW_PROBABILITY)
        assertEquals(Hearing.LOUD_UNCLEAR, tracker.hearing)
    }

    @Test
    fun `a breath inside the run is tolerated, a longer gap ends it`() {
        sound(0.3f, 20)
        sound(0f, HearingTracker.UNCLEAR_GAP_FRAMES - 1)
        sound(0.3f, 5)
        assertEquals(Hearing.QUIET_UNCLEAR, tracker.hearing)
        sound(0f, HearingTracker.UNCLEAR_GAP_FRAMES)
        assertEquals(Hearing.CLEAR, tracker.hearing)
    }

    @Test
    fun `the level is judged on the last second, so moving closer is noticed`() {
        sound(0.3f, HearingTracker.UNCLEAR_FRAMES)
        sound(0.7f, HearingTracker.UNCLEAR_FRAMES)
        assertEquals(Hearing.LOUD_UNCLEAR, tracker.hearing)
        assertEquals(0.7, tracker.levelMean, 1e-6)
    }

    @Test
    fun `a shown note or a reset ends the run at once`() {
        sound(0.3f, HearingTracker.UNCLEAR_FRAMES)
        assertEquals(Hearing.CLEAR, tracker.onFrame(0.4f, FrameDecision.SHOWN))
        sound(0.3f, HearingTracker.UNCLEAR_FRAMES)
        tracker.reset()
        assertEquals(Hearing.CLEAR, tracker.hearing)
        assertEquals(0.0, tracker.levelMean, 0.0)
    }

    @Test
    fun `ignored frames and sound under the floor never start a run`() {
        sound(0.6f, HearingTracker.UNCLEAR_FRAMES, FrameDecision.IGNORED)
        sound(0.1f, 100)
        assertEquals(Hearing.CLEAR, tracker.hearing)
    }
}
