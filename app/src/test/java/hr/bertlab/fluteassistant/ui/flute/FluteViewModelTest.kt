package hr.bertlab.fluteassistant.ui.flute

import hr.bertlab.fluteassistant.domain.flute.NoteLength.DOTTED_HALF
import hr.bertlab.fluteassistant.domain.flute.NoteLength.EIGHTH
import hr.bertlab.fluteassistant.domain.flute.NoteLength.HALF
import hr.bertlab.fluteassistant.domain.flute.NoteLength.QUARTER
import hr.bertlab.fluteassistant.domain.flute.NoteLength.WHOLE
import hr.bertlab.fluteassistant.domain.flute.TuneNote
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.fakes.FakeClock
import hr.bertlab.fluteassistant.fakes.FakeFluteTonePlayer
import hr.bertlab.fluteassistant.fakes.FakeLogger
import hr.bertlab.fluteassistant.fakes.FakeSettingsStore
import hr.bertlab.fluteassistant.ui.tuner.TunerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FluteViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val settings = FakeSettingsStore()
    private val tonePlayer = FakeFluteTonePlayer()
    private val clock = FakeClock(now = 10_000L)
    private val logger = FakeLogger()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FluteViewModel(settings, tonePlayer, clock, logger)

    private fun freq(midi: Int) = NoteMath.midiToFrequency(midi)

    private fun on(midi: Int) = FakeFluteTonePlayer.NoteOn(freq(midi), TunerViewModel.REFERENCE_TONE_VOLUME)

    private val FluteViewModel.current: FluteUiState get() = state.value

    /** Presses [midi], holds it for [heldMs] of fake time and lets go. */
    private fun FluteViewModel.tap(midi: Int, heldMs: Long) {
        onKeyPressed(midi)
        clock.now += heldMs
        onKeyReleased(midi)
    }

    /** A two-note tune at the default 120 bpm (c1 a quarter, g1 a half note) with playback just started. */
    private fun playingViewModel(): FluteViewModel = viewModel().apply {
        tap(60, 500)
        tap(67, 1000)
        togglePlayback()
    }

    @Test
    fun `starts empty and silent`() {
        val vm = viewModel()
        assertEquals(FluteUiState(), vm.current)
        assertTrue(tonePlayer.notesOn.isEmpty())
    }

    @Test
    fun `a stored tune is restored and sanitized`() {
        settings.tune = listOf(TuneNote(60, HALF), TuneNote(61, QUARTER), TuneNote(84, WHOLE))
        val vm = viewModel()
        assertEquals(listOf(TuneNote(60, HALF), TuneNote(84, WHOLE)), vm.current.tune)
        assertNull(vm.current.shownMidi)
    }

    @Test
    fun `entering the tab opens the player and leaving closes it`() {
        val vm = viewModel()
        vm.enterTab()
        assertEquals(1, tonePlayer.openCount)
        vm.leaveTab()
        assertEquals(1, tonePlayer.closeCount)
    }

    @Test
    fun `a press sounds the note at once, shows it and adds it as an eighth`() {
        val vm = viewModel()
        vm.onKeyPressed(67)
        assertEquals(on(67), tonePlayer.sounding)
        assertEquals(67, vm.current.heldMidi)
        assertEquals(0, vm.current.heldIndex)
        assertEquals(67, vm.current.shownMidi)
        assertEquals(listOf(TuneNote(67, EIGHTH)), vm.current.tune)
        assertEquals(EIGHTH, vm.current.heldLength)
        assertEquals(0, vm.current.holdBeats)
        assertEquals(vm.current.tune, settings.tune)
    }

    @Test
    fun `a release stops the sound and records the length the hold made`() {
        val vm = viewModel()
        vm.tap(67, 450) // 0.9 of a beat at 120 bpm
        assertEquals(1, tonePlayer.offCount)
        assertNull(tonePlayer.sounding)
        assertNull(vm.current.heldMidi)
        assertNull(vm.current.heldIndex)
        assertNull(vm.current.heldLength)
        assertEquals(67, vm.current.shownMidi)
        assertEquals(listOf(TuneNote(67, QUARTER)), vm.current.tune)
        assertEquals(vm.current.tune, settings.tune)
        val event = logger.events("flute.note").single()
        assertEquals(67, event["midi"])
        assertEquals(450L, event["heldMs"])
        assertEquals(QUARTER, event["length"])
        assertEquals(true, event["added"])
    }

    @Test
    fun `a quick tap is an eighth and a long hold stays a whole note`() {
        val vm = viewModel()
        vm.tap(67, 50)
        vm.tap(69, 7000)
        assertEquals(listOf(TuneNote(67, EIGHTH), TuneNote(69, WHOLE)), vm.current.tune)
    }

    @Test
    fun `a held note grows beat by beat and keeps its length when let go`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKeyPressed(67)
        advanceTimeBy(374)
        assertEquals(EIGHTH, vm.current.tune[0].length)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(QUARTER, vm.current.tune[0].length)
        assertEquals(1, vm.current.holdBeats)
        advanceTimeBy(375)
        runCurrent()
        assertEquals(HALF, vm.current.heldLength)
        assertEquals(2, vm.current.holdBeats)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(DOTTED_HALF, vm.current.tune[0].length)
        advanceTimeBy(500)
        runCurrent()
        assertEquals(WHOLE, vm.current.tune[0].length)
        assertEquals(4, vm.current.holdBeats)
        clock.now += 1800
        vm.onKeyReleased(67)
        assertEquals(listOf(TuneNote(67, WHOLE)), vm.current.tune)
        assertNull(vm.current.holdBeats)
        advanceTimeBy(5000)
        assertEquals(listOf(TuneNote(67, WHOLE)), vm.current.tune)
    }

    @Test
    fun `beats are counted at the metronome's tempo, kept in the ring's range`() {
        settings.config = MetronomeConfig(bpm = 60)
        val vm = viewModel()
        vm.tap(67, 450) // under 3/4 of a one-second beat
        assertEquals(EIGHTH, vm.current.tune[0].length)
        settings.config = MetronomeConfig(bpm = 30) // an older build's tempo, below the ring: counted at 40
        vm.tap(69, 1200) // 0.8 of a 1.5 s beat
        assertEquals(QUARTER, vm.current.tune[1].length)
    }

    @Test
    fun `releasing a key that is not sounding is ignored`() {
        val vm = viewModel()
        vm.onKeyPressed(67)
        vm.onKeyReleased(69)
        assertEquals(0, tonePlayer.offCount)
        assertEquals(67, vm.current.heldMidi)
    }

    @Test
    fun `the newest press wins`() {
        val vm = viewModel()
        vm.onKeyPressed(67)
        clock.now += 400
        vm.onKeyPressed(69)
        assertEquals(on(69), tonePlayer.sounding)
        assertEquals(listOf(TuneNote(67, QUARTER), TuneNote(69, EIGHTH)), vm.current.tune)
        assertEquals(1, vm.current.heldIndex)
        clock.now += 500
        vm.onKeyReleased(67)
        assertEquals(0, tonePlayer.offCount)
        assertEquals(69, vm.current.heldMidi)
        vm.onKeyReleased(69)
        assertEquals(1, tonePlayer.offCount)
        assertEquals(QUARTER, vm.current.tune[1].length)
    }

    @Test
    fun `a press outside the keys is ignored`() {
        val vm = viewModel()
        vm.onKeyPressed(61)
        assertTrue(tonePlayer.notesOn.isEmpty())
        assertTrue(vm.current.tune.isEmpty())
    }

    @Test
    fun `at the cap a press still sounds but is not added`() {
        settings.tune = List(200) { TuneNote(60, QUARTER) }
        val vm = viewModel()
        vm.onKeyPressed(67)
        assertEquals(on(67), tonePlayer.sounding)
        assertEquals(67, vm.current.shownMidi)
        assertNull(vm.current.heldIndex)
        assertNull(vm.current.litIndex)
        assertEquals(200, vm.current.tune.size)
        clock.now += 400
        vm.onKeyReleased(67)
        assertEquals(1, tonePlayer.offCount)
        assertEquals(200, vm.current.tune.size)
        assertEquals(TuneNote(60, QUARTER), vm.current.tune.last())
        assertEquals(false, logger.events("flute.note").single()["added"])
    }

    @Test
    fun `erase removes the last note and is ignored while a key is held or the tune is empty`() {
        val vm = viewModel()
        vm.eraseLast()
        assertTrue(logger.events("flute.erase").isEmpty())
        vm.tap(67, 300)
        vm.tap(69, 300)
        vm.eraseLast()
        assertEquals(listOf(TuneNote(67, EIGHTH)), vm.current.tune)
        assertEquals(vm.current.tune, settings.tune)
        assertEquals(1, logger.events("flute.erase").size)
        vm.onKeyPressed(72)
        vm.eraseLast()
        assertEquals(2, vm.current.tune.size)
    }

    @Test
    fun `play sounds each note for its length at the tempo with a gap, lighting it as it goes`() = runTest(dispatcher) {
        val vm = playingViewModel()
        assertTrue(vm.current.isPlaying)
        assertEquals(0, vm.current.playingIndex)
        assertEquals(60, vm.current.shownMidi)
        assertEquals(on(60), tonePlayer.sounding)
        assertEquals(60, vm.current.litMidi)
        advanceTimeBy(421) // a beat is 500 ms, the last 80 of them silent
        assertEquals(3, tonePlayer.offCount)
        assertEquals(0, vm.current.playingIndex)
        advanceTimeBy(80)
        assertEquals(1, vm.current.playingIndex)
        assertEquals(on(67), tonePlayer.sounding)
        assertEquals(67, vm.current.shownMidi)
        advanceTimeBy(920) // two beats
        assertEquals(4, tonePlayer.offCount)
        advanceTimeBy(80)
        assertNull(vm.current.playingIndex)
        assertFalse(vm.current.isPlaying)
        assertEquals(67, vm.current.shownMidi)
        val play = logger.events("flute.play").single()
        assertEquals(2, play["notes"])
        assertEquals(120, play["bpm"])
        assertEquals(1, logger.events("flute.play_done").size)
    }

    @Test
    fun `playback follows the metronome's tempo when it changes`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.tap(60, 500)
        settings.config = MetronomeConfig(bpm = 60)
        vm.togglePlayback()
        advanceTimeBy(919)
        assertEquals(on(60), tonePlayer.sounding)
        advanceTimeBy(2)
        assertNull(tonePlayer.sounding)
        assertEquals(QUARTER, vm.current.tune[0].length)
    }

    @Test
    fun `stop ends playback at once`() = runTest(dispatcher) {
        val vm = playingViewModel()
        advanceTimeBy(100)
        vm.togglePlayback()
        assertFalse(vm.current.isPlaying)
        assertNull(tonePlayer.sounding)
        assertEquals(1, logger.events("flute.stop").size)
        val notesOn = tonePlayer.notesOn.size
        advanceTimeBy(5000)
        assertEquals(notesOn, tonePlayer.notesOn.size)
    }

    @Test
    fun `a press during playback stops it and plays the key`() = runTest(dispatcher) {
        val vm = playingViewModel()
        advanceTimeBy(100)
        vm.onKeyPressed(72)
        assertFalse(vm.current.isPlaying)
        assertEquals(on(72), tonePlayer.sounding)
        assertEquals(3, vm.current.tune.size)
        assertEquals(2, vm.current.heldIndex)
    }

    @Test
    fun `erase during playback stops it first`() = runTest(dispatcher) {
        val vm = playingViewModel()
        advanceTimeBy(100)
        vm.eraseLast()
        assertFalse(vm.current.isPlaying)
        assertEquals(1, vm.current.tune.size)
    }

    @Test
    fun `play with a key held releases it first`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKeyPressed(60)
        clock.now += 500
        vm.togglePlayback()
        assertEquals(QUARTER, vm.current.tune[0].length)
        assertNull(vm.current.heldMidi)
        assertEquals(0, vm.current.playingIndex)
    }

    @Test
    fun `play does nothing while the tune is empty`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.togglePlayback()
        assertFalse(vm.current.isPlaying)
        assertTrue(tonePlayer.notesOn.isEmpty())
    }

    @Test
    fun `leaving the tab silences the flute but keeps the tune`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKeyPressed(60)
        clock.now += 350
        vm.leaveTab()
        assertEquals(1, tonePlayer.offCount)
        assertNull(vm.current.heldMidi)
        assertEquals(listOf(TuneNote(60, EIGHTH)), vm.current.tune)
        assertEquals(1, tonePlayer.closeCount)
        assertEquals(vm.current.tune, settings.tune)
        vm.enterTab()
        vm.togglePlayback()
        advanceTimeBy(100)
        vm.leaveTab()
        assertFalse(vm.current.isPlaying)
        assertNull(tonePlayer.sounding)
        assertEquals(2, tonePlayer.closeCount)
        assertEquals(1, vm.current.tune.size)
    }

    @Test
    fun `stop says why`() = runTest(dispatcher) {
        val vm = playingViewModel()
        advanceTimeBy(100)
        vm.togglePlayback()
        assertEquals("pill", logger.events("flute.stop").single()["reason"])
        vm.togglePlayback()
        advanceTimeBy(100)
        vm.onKeyPressed(72)
        assertEquals("key", logger.events("flute.stop")[1]["reason"])
        vm.onKeyReleased(72)
        vm.togglePlayback()
        advanceTimeBy(100)
        vm.eraseLast()
        assertEquals("erase", logger.events("flute.stop")[2]["reason"])
    }

    @Test
    fun `leaving the tab mid-playback logs why it stopped`() = runTest(dispatcher) {
        val vm = playingViewModel()
        advanceTimeBy(100)
        vm.leaveTab()
        assertEquals("leave", logger.events("flute.stop").single()["reason"])
        assertTrue(logger.events("flute.play_done").isEmpty())
    }

    @Test
    fun `a release after leaving the tab is ignored`() {
        val vm = viewModel()
        vm.onKeyPressed(60)
        clock.now += 350
        vm.leaveTab()
        vm.onKeyReleased(60)
        assertEquals(1, tonePlayer.offCount)
        assertEquals(listOf(TuneNote(60, EIGHTH)), vm.current.tune)
        assertEquals(1, logger.events("flute.note").size)
    }

    @Test
    fun `a real press on an activated key is not released by the activation`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKeyActivated(67)
        advanceTimeBy(100)
        vm.onKeyPressed(67) // a finger lands on the same key before the activation's beat is up
        advanceTimeBy(501)
        assertEquals(67, vm.current.heldMidi)
        assertEquals(0, tonePlayer.offCount)
        assertEquals(2, vm.current.tune.size)
    }

    @Test
    fun `activating a key sounds it for one beat and adds a quarter note`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onKeyActivated(67)
        assertEquals(on(67), tonePlayer.sounding)
        advanceTimeBy(501)
        assertEquals(1, tonePlayer.offCount)
        assertEquals(listOf(TuneNote(67, QUARTER)), vm.current.tune)
    }
}
