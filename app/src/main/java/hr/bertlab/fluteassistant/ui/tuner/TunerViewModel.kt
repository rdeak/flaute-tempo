package hr.bertlab.fluteassistant.ui.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.bertlab.fluteassistant.audio.tuner.PitchFrame
import hr.bertlab.fluteassistant.audio.tuner.TunerEngine
import hr.bertlab.fluteassistant.domain.repository.SettingsStore
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Smooths raw [PitchFrame]s into a stable [TunerUiState]:
 * - keeps a short median window so a single mis-detected frame does not flick the needle,
 * - ignores low-confidence frames,
 * - holds the last reading for a few silent frames so the display does not blink between notes.
 */
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val engine: TunerEngine,
    private val settings: SettingsStore,
) : ViewModel() {

    // The simplified UI cannot show or change the A4 reference, so a stale stored value must
    // not silently relabel every note: start from the standard 440 Hz (the stored value is
    // left for a future advanced mode, which would call setReferenceA4 explicitly).
    private val _state = MutableStateFlow(TunerUiState())
    val state: StateFlow<TunerUiState> = _state.asStateFlow()

    private var listenJob: Job? = null
    private val recentFrequencies = ArrayDeque<Double>()
    private var silentFrames = 0

    fun onPermissionRequested() {
        _state.update { it.copy(permissionRequested = true) }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionGranted = granted) }
        if (!granted) stopListening()
    }

    /** Opens the microphone. Safe to call repeatedly; no-op without permission. */
    fun startListening() {
        if (!_state.value.permissionGranted || listenJob?.isActive == true) return
        listenJob = viewModelScope.launch {
            _state.update { it.copy(isListening = true, errorMessage = null) }
            try {
                engine.pitchFrames().collect(::onFrame)
                _state.update { it.copy(isListening = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(isListening = false, reading = null, errorMessage = e.message ?: "Microphone unavailable")
                }
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        listenJob = null
        recentFrequencies.clear()
        silentFrames = 0
        _state.update { it.copy(isListening = false, reading = null, level = 0f) }
    }

    fun setReferenceA4(hz: Double) {
        val clamped = hz.coerceIn(MIN_A4, MAX_A4)
        settings.saveReferenceA4(clamped)
        _state.update { s ->
            s.copy(
                referenceA4 = clamped,
                reading = s.reading?.let { NoteMath.frequencyToNote(it.frequency, clamped) },
            )
        }
    }

    private fun onFrame(frame: PitchFrame) {
        val frequency = frame.frequency
        if (frequency != null && frame.probability >= MIN_PROBABILITY) {
            silentFrames = 0
            recentFrequencies.addLast(frequency)
            while (recentFrequencies.size > SMOOTHING_WINDOW) recentFrequencies.removeFirst()
            val median = recentFrequencies.sorted()[recentFrequencies.size / 2]
            _state.update { it.copy(reading = NoteMath.frequencyToNote(median, it.referenceA4), level = frame.level) }
        } else {
            silentFrames++
            if (silentFrames >= HOLD_FRAMES) {
                recentFrequencies.clear()
                _state.update { it.copy(reading = null, level = frame.level) }
            } else {
                _state.update { it.copy(level = frame.level) }
            }
        }
    }

    override fun onCleared() {
        listenJob?.cancel()
    }

    companion object {
        const val SMOOTHING_WINDOW = 5
        const val MIN_PROBABILITY = 0.85
        /** ~0.9 s at 43 ms per frame. */
        const val HOLD_FRAMES = 20
        const val MIN_A4 = 415.0
        const val MAX_A4 = 466.0
    }
}
