package hr.bertlab.fluteassistant.ui.tuner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.bertlab.fluteassistant.audio.tuner.PitchFrame
import hr.bertlab.fluteassistant.audio.tuner.ReferenceTonePlayer
import hr.bertlab.fluteassistant.audio.tuner.TunerEngine
import hr.bertlab.fluteassistant.domain.repository.SettingsStore
import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log2

/**
 * Smooths raw [PitchFrame]s into a stable [TunerUiState]:
 * - shows a reading only once a few consecutive frames agree (a held note, not room noise),
 * - keeps a short median window so a single mis-detected frame does not flick the needle,
 * - ignores low-confidence frames,
 * - holds the last reading for a few silent frames so the display does not blink between notes.
 */
@HiltViewModel
class TunerViewModel @Inject constructor(
    private val engine: TunerEngine,
    private val settings: SettingsStore,
    private val tonePlayer: ReferenceTonePlayer,
) : ViewModel() {

    // The simplified UI cannot show or change the A4 reference, so a stale stored value must
    // not silently relabel every note: start from the standard 440 Hz (the stored value is
    // left for a future advanced mode, which would call setReferenceA4 explicitly). The
    // target note and the mode, by contrast, are on screen, so the stored ones are honoured.
    private val _state = MutableStateFlow(
        TunerUiState(targetMidi = FluteRange.clamp(settings.loadTargetNote()), mode = settings.loadTunerMode()),
    )
    val state: StateFlow<TunerUiState> = _state.asStateFlow()

    private var listenJob: Job? = null
    private var playJob: Job? = null
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
        stopTone()
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

    /** Chooses the note the gauge measures against; clamped to the flute's range and remembered. */
    fun setTargetNote(midi: Int) {
        val clamped = FluteRange.clamp(midi)
        settings.saveTargetNote(clamped)
        _state.update { it.copy(targetMidi = clamped) }
    }

    /**
     * Sounds the chosen note for [REFERENCE_TONE_MS] at [REFERENCE_TONE_VOLUME]. Readings pause meanwhile
     * (the microphone would otherwise measure the speaker) and start afresh when it ends.
     */
    fun playTargetNote() {
        playJob?.cancel()
        val current = _state.value
        tonePlayer.play(
            NoteMath.midiToFrequency(current.targetMidi, current.referenceA4),
            REFERENCE_TONE_MS,
            REFERENCE_TONE_VOLUME,
        )
        recentFrequencies.clear()
        _state.update { it.copy(isPlayingReference = true, reading = null) }
        playJob = viewModelScope.launch {
            delay(REFERENCE_TONE_MS.toLong())
            tonePlayer.stop() // the data has played out; release the track rather than leave it registered as playing
            recentFrequencies.clear()
            silentFrames = 0
            _state.update { it.copy(isPlayingReference = false) }
        }
    }

    private fun stopTone() {
        playJob?.cancel()
        playJob = null
        tonePlayer.stop()
        _state.update { it.copy(isPlayingReference = false) }
    }

    /** Switches between naming the heard note and measuring against the chosen one; remembered. */
    fun setMode(mode: TunerMode) {
        settings.saveTunerMode(mode)
        _state.update { it.copy(mode = mode) }
    }

    private fun onFrame(frame: PitchFrame) {
        if (_state.value.isPlayingReference) {
            _state.update { it.copy(level = frame.level) }
            return
        }
        val frequency = frame.frequency
        if (frequency != null && frame.probability >= MIN_PROBABILITY) {
            silentFrames = 0
            recentFrequencies.addLast(frequency)
            while (recentFrequencies.size > SMOOTHING_WINDOW) recentFrequencies.removeFirst()
            if (isSteady()) {
                val median = recentFrequencies.sorted()[recentFrequencies.size / 2]
                _state.update { it.copy(reading = NoteMath.frequencyToNote(median, it.referenceA4), level = frame.level) }
            } else {
                _state.update { it.copy(level = frame.level) }
            }
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

    /**
     * True once the last [LOCK_FRAMES] frames agree within [LOCK_CENTS]: a held note, as
     * opposed to a syllable, a bump or a voice gliding past, whose pitch moves or stops within
     * that time. Until then the previous reading stays rather than jumping to the stray sound.
     */
    private fun isSteady(): Boolean {
        if (recentFrequencies.size < LOCK_FRAMES) return false
        val last = recentFrequencies.takeLast(LOCK_FRAMES)
        return 1200.0 * log2(last.max() / last.min()) <= LOCK_CENTS
    }

    override fun onCleared() {
        stopTone()
        listenJob?.cancel()
    }

    companion object {
        const val SMOOTHING_WINDOW = 5
        /** A reading needs this many consecutive frames (~130 ms) that agree within [LOCK_CENTS]. */
        const val LOCK_FRAMES = 3
        const val LOCK_CENTS = 30.0
        const val MIN_PROBABILITY = 0.85
        /** ~0.9 s at 43 ms per frame. */
        const val HOLD_FRAMES = 20
        const val MIN_A4 = 415.0
        const val MAX_A4 = 466.0
        /** How long the reference note sounds. */
        const val REFERENCE_TONE_MS = 2000
        /** How loud, in 0..1. Fixed: the app has no volume control, the phone's buttons do that. */
        const val REFERENCE_TONE_VOLUME = 0.8f
    }
}
