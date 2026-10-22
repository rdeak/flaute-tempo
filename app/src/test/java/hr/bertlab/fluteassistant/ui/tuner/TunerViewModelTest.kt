package hr.bertlab.fluteassistant.ui.tuner

import hr.bertlab.fluteassistant.audio.tuner.PitchFrame
import hr.bertlab.fluteassistant.fakes.FakeSettingsStore
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.fakes.FakeTunerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val engine = FakeTunerEngine()
    private val settings = FakeSettingsStore()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun pitched(freq: Double, probability: Double = 0.97) = PitchFrame(freq, probability, level = 0.4f)
    private val silence = PitchFrame(null, 0.0, level = 0f)

    private fun listeningViewModel(): TunerViewModel = TunerViewModel(engine, settings).apply {
        onPermissionResult(true)
        startListening()
    }

    @Test
    fun `does not listen without permission`() = runTest(dispatcher) {
        val vm = TunerViewModel(engine, settings)
        vm.startListening()
        advanceUntilIdle()
        assertFalse(vm.state.value.isListening)
        assertEquals(0, engine.subscriptions)
    }

    @Test
    fun `stable frames produce a note reading`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        assertTrue(vm.state.value.isListening)

        repeat(TunerViewModel.SMOOTHING_WINDOW) { engine.frames.tryEmit(pitched(440.0)) }
        advanceUntilIdle()

        val reading = vm.state.value.reading
        assertNotNull(reading)
        assertEquals("A4", reading!!.label)
        assertEquals(0.0, reading.cents, 0.01)
        assertEquals(0.4f, vm.state.value.level)
    }

    @Test
    fun `median smoothing rejects a single outlier`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        listOf(440.0, 440.5, 880.0, 439.8, 440.2).forEach { engine.frames.tryEmit(pitched(it)) }
        advanceUntilIdle()
        assertEquals("A4", vm.state.value.reading!!.label)
    }

    @Test
    fun `low confidence frames are ignored`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        engine.frames.tryEmit(pitched(440.0))
        advanceUntilIdle()
        assertEquals("A4", vm.state.value.reading!!.label)

        engine.frames.tryEmit(pitched(1000.0, probability = 0.3))
        advanceUntilIdle()
        assertEquals("A4", vm.state.value.reading!!.label)
    }

    @Test
    fun `reading is held briefly through silence then cleared`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        engine.frames.tryEmit(pitched(440.0))
        advanceUntilIdle()

        repeat(TunerViewModel.HOLD_FRAMES - 1) { engine.frames.tryEmit(silence) }
        advanceUntilIdle()
        assertNotNull(vm.state.value.reading)

        engine.frames.tryEmit(silence)
        advanceUntilIdle()
        assertNull(vm.state.value.reading)
    }

    @Test
    fun `changing the reference pitch recalculates the reading`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        engine.frames.tryEmit(pitched(440.0))
        advanceUntilIdle()

        vm.setReferenceA4(442.0)
        assertEquals(442.0, vm.state.value.referenceA4, 0.0)
        assertEquals(-7.85, vm.state.value.reading!!.cents, 0.05)

        vm.setReferenceA4(9999.0)
        assertEquals(TunerViewModel.MAX_A4, vm.state.value.referenceA4, 0.0)
        assertEquals(TunerViewModel.MAX_A4, settings.referenceA4, 0.0)
    }

    @Test
    fun `stored reference pitch is reset to standard on load`() {
        // The simplified UI cannot show or change A4, so a stale 442 or 466 must not silently
        // relabel every note; only an explicit setReferenceA4 (future advanced mode) changes it.
        settings.referenceA4 = 442.0
        val vm = TunerViewModel(engine, settings)
        assertEquals(NoteMath.DEFAULT_A4, vm.state.value.referenceA4, 0.0)
    }

    @Test
    fun `stop listening clears the reading`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        engine.frames.tryEmit(pitched(440.0))
        advanceUntilIdle()

        vm.stopListening()
        advanceUntilIdle()
        assertFalse(vm.state.value.isListening)
        assertNull(vm.state.value.reading)
    }

    @Test
    fun `engine failures surface as an error message`() = runTest(dispatcher) {
        engine.flowOverride = flow { throw IllegalStateException("Microphone busy") }
        val vm = listeningViewModel()
        advanceUntilIdle()
        assertFalse(vm.state.value.isListening)
        assertEquals("Microphone busy", vm.state.value.errorMessage)
    }

    @Test
    fun `remembers that the permission prompt was shown`() = runTest(dispatcher) {
        val vm = TunerViewModel(engine, settings)
        assertFalse(vm.state.value.permissionRequested)
        vm.onPermissionRequested()
        assertTrue(vm.state.value.permissionRequested)
    }

    @Test
    fun `revoking permission stops listening`() = runTest(dispatcher) {
        val vm = listeningViewModel()
        advanceUntilIdle()
        vm.onPermissionResult(false)
        advanceUntilIdle()
        assertFalse(vm.state.value.isListening)
        assertFalse(vm.state.value.permissionGranted)
    }
}
