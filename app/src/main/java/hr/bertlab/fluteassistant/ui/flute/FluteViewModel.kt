package hr.bertlab.fluteassistant.ui.flute

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.bertlab.fluteassistant.audio.flute.FluteTonePlayer
import hr.bertlab.fluteassistant.core.MonotonicClock
import hr.bertlab.fluteassistant.core.log.LogTags
import hr.bertlab.fluteassistant.core.log.Logger
import hr.bertlab.fluteassistant.domain.flute.NoteLength
import hr.bertlab.fluteassistant.domain.flute.Tune
import hr.bertlab.fluteassistant.domain.flute.TuneNote
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import hr.bertlab.fluteassistant.domain.repository.SettingsStore
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.ui.metronome.TempoBand
import hr.bertlab.fluteassistant.ui.tuner.TunerViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The flute tab: a key pressed sounds through [FluteTonePlayer] until it is released and is
 * appended to the tune; its [NoteLength] grows while the key is held, in beats of the
 * metronome's tempo (an eighth at first, then a quarter at ¾ of a beat and so on, see
 * [Tune.growAtMs]), and the release records the length the hold makes ([Tune.lengthOf]), the
 * same one the screen was showing. One Play button plays the tune back at the metronome's
 * tempo; erase drops the last note. The tempo is the metronome tab's, read from
 * [SettingsStore] at each press and at Play. The tune is saved through [SettingsStore] on every
 * change and restored, sanitised, on creation. Activity-scoped, so the tune survives tab
 * switches and rotation; [enterTab] and [leaveTab] follow the tab's visibility so the player is
 * open only while the flute is on screen.
 *
 * Logs under [LogTags.FLUTE]: `flute.note` per note ended (midi, heldMs, length, added: false
 * at the cap), `flute.play` (notes, bpm), `flute.play_done`, `flute.stop` (reason: pill, key,
 * erase or leave), `flute.erase` (left).
 */
@HiltViewModel
class FluteViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val tonePlayer: FluteTonePlayer,
    private val clock: MonotonicClock,
    private val logger: Logger,
) : ViewModel() {

    private val _state = MutableStateFlow(FluteUiState(tune = Tune.sanitize(settings.loadTune())))
    val state: StateFlow<FluteUiState> = _state.asStateFlow()

    /** When the held key went down, so its release can record how long it was held. */
    private var pressedAt: Long? = null
    /** The tempo the held key's note is counted in, read at the press. */
    private var pressBpm: Int = MetronomeConfig.DEFAULT_BPM
    /** Grows the held key's note at each of [Tune.growAtMs] while the key stays down. */
    private var growJob: Job? = null
    private var playbackJob: Job? = null
    private var activationJob: Job? = null

    /** The tab came on screen: open the output now so the first press is not late. */
    fun enterTab() = tonePlayer.open()

    /** The tab left the screen: silence everything, keep the tune, give the output back. */
    fun leaveTab() {
        stopPlayback(reason = "leave")
        if (_state.value.heldMidi != null) {
            tonePlayer.noteOff()
            finishHeld(clock.nowMillis())
        }
        tonePlayer.close()
    }

    /** A finger went down on the key of [midi]: sound it at once and add it to the tune. */
    fun onKeyPressed(midi: Int) {
        if (midi !in StaffNotes.ALL) return
        stopPlayback(reason = "key")
        val now = clock.nowMillis()
        finishHeld(now) // the new note takes the voice over; no note off, so nothing clicks
        activationJob?.cancel()
        tonePlayer.noteOn(NoteMath.midiToFrequency(midi), TunerViewModel.REFERENCE_TONE_VOLUME)
        pressedAt = now
        pressBpm = bpm()
        val first = NoteLength.EIGHTH
        _state.update { s ->
            if (s.atCap) {
                s.copy(heldMidi = midi, heldIndex = null, shownMidi = midi, heldLength = first)
            } else {
                s.copy(
                    tune = s.tune + TuneNote(midi, first),
                    heldMidi = midi,
                    heldIndex = s.tune.size,
                    shownMidi = midi,
                    heldLength = first,
                )
            }
        }
        save()
        val bpm = pressBpm
        growJob = viewModelScope.launch {
            var waited = 0L
            for (at in Tune.growAtMs(bpm)) {
                delay(at - waited)
                waited = at
                grow(Tune.lengthOf(at, bpm))
            }
        }
    }

    /** The finger lifted off (or slid off) the key of [midi]; ignored unless that key is the one sounding. */
    fun onKeyReleased(midi: Int) {
        if (midi != _state.value.heldMidi) return
        release(midi, heldFor(clock.nowMillis()))
    }

    /** A screen reader activated the key of [midi]: sound it for one beat and add it, a quarter note. */
    fun onKeyActivated(midi: Int) {
        onKeyPressed(midi)
        if (_state.value.heldMidi != midi) return
        val beat = Tune.durationMs(NoteLength.QUARTER, pressBpm)
        activationJob = viewModelScope.launch {
            delay(beat)
            if (_state.value.heldMidi == midi) release(midi, beat)
        }
    }

    /** Plays the tune from the first note, or stops a playback in progress. Nothing to play: nothing happens. */
    fun togglePlayback() {
        if (_state.value.isPlaying) {
            stopPlayback(reason = "pill")
            return
        }
        if (_state.value.isEmpty) return
        if (_state.value.heldMidi != null) {
            tonePlayer.noteOff()
            finishHeld(clock.nowMillis())
        }
        activationJob?.cancel()
        val notes = _state.value.tune
        val bpm = bpm()
        logger.d(LogTags.FLUTE, "flute.play", "notes" to notes.size, "bpm" to bpm)
        // Undispatched, so the first note sounds and lights before this call returns.
        playbackJob = viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            for ((index, note) in notes.withIndex()) {
                val duration = Tune.durationMs(note.length, bpm)
                val gap = Tune.gapMs(duration)
                _state.update { it.copy(playingIndex = index, shownMidi = note.midi) }
                tonePlayer.noteOn(NoteMath.midiToFrequency(note.midi), TunerViewModel.REFERENCE_TONE_VOLUME)
                delay(duration - gap)
                tonePlayer.noteOff()
                delay(gap)
            }
            playbackJob = null
            _state.update { it.copy(playingIndex = null) }
            logger.d(LogTags.FLUTE, "flute.play_done")
        }
    }

    /** Drops the last note of the tune; ignored while a key is held or the tune is empty. Stops playback first. */
    fun eraseLast() {
        stopPlayback(reason = "erase")
        val current = _state.value
        if (current.heldMidi != null || current.isEmpty) return
        _state.update { it.copy(tune = it.tune.dropLast(1)) }
        save()
        logger.d(LogTags.FLUTE, "flute.erase", "left" to _state.value.tune.size)
    }

    /** Ends a playback in progress, saying what ended it; nothing playing: nothing happens. */
    private fun stopPlayback(reason: String) {
        if (!_state.value.isPlaying) return
        playbackJob?.cancel()
        playbackJob = null
        tonePlayer.noteOff()
        _state.update { it.copy(playingIndex = null) }
        logger.d(LogTags.FLUTE, "flute.stop", "reason" to reason)
    }

    /** Silences the held key and records how long it was held. */
    private fun release(midi: Int, heldMs: Long) {
        tonePlayer.noteOff()
        record(midi, heldMs)
    }

    /** Ends the held key's note in the tune, at [now], without touching the sound; nothing held: nothing happens. */
    private fun finishHeld(now: Long) {
        val midi = _state.value.heldMidi ?: return
        record(midi, heldFor(now))
    }

    private fun heldFor(now: Long): Long = (now - (pressedAt ?: now)).coerceAtLeast(0)

    /** The held key's note reached [length]: show it on the dots and in the line. */
    private fun grow(length: NoteLength) {
        _state.update { s -> s.copy(tune = s.withHeldLength(length), heldLength = length) }
    }

    private fun record(midi: Int, heldMs: Long) {
        growJob?.cancel()
        growJob = null
        val length = Tune.lengthOf(heldMs, pressBpm)
        val added = _state.value.heldIndex != null // false at the cap, where the press only sounded
        _state.update { s -> s.copy(tune = s.withHeldLength(length), heldMidi = null, heldIndex = null, heldLength = null) }
        pressedAt = null
        save()
        logger.d(LogTags.FLUTE, "flute.note", "midi" to midi, "heldMs" to heldMs, "length" to length, "added" to added)
    }

    /** The tune with the held key's note set to [length]; unchanged at the cap, where it was not added. */
    private fun FluteUiState.withHeldLength(length: NoteLength): List<TuneNote> =
        heldIndex?.let { index -> tune.toMutableList().also { it[index] = it[index].copy(length = length) } } ?: tune

    /** The metronome tab's tempo, in its ring's range (a stored one from an older build may lie outside). */
    private fun bpm(): Int = settings.loadMetronomeConfig().bpm.coerceIn(TempoBand.MIN_BPM, TempoBand.MAX_BPM)

    private fun save() = settings.saveTune(_state.value.tune)

    override fun onCleared() {
        playbackJob?.cancel()
        activationJob?.cancel()
        growJob?.cancel()
        tonePlayer.close()
    }
}
