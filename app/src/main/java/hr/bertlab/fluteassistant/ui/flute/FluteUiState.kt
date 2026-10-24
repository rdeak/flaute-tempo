package hr.bertlab.fluteassistant.ui.flute

import hr.bertlab.fluteassistant.domain.flute.NoteLength
import hr.bertlab.fluteassistant.domain.flute.Tune
import hr.bertlab.fluteassistant.domain.flute.TuneNote

/** Immutable snapshot of everything the flute tab renders. */
data class FluteUiState(
    /** The child's tune, oldest first; saved on every change. */
    val tune: List<TuneNote> = emptyList(),
    /** The key under a finger right now, sounding; null when none. */
    val heldMidi: Int? = null,
    /** Where the held key's note sits in [tune]; null at the cap, where a press sounds but is not added. */
    val heldIndex: Int? = null,
    /** What the name and the flute show: the held, the last played, or the note playing now; null before the first tap. */
    val shownMidi: Int? = null,
    /** The note of [tune] sounding during playback; null when not playing. */
    val playingIndex: Int? = null,
    /** How long the held key's note has grown so far ([NoteLength.ofBeats]); null when no key is held. */
    val heldLength: NoteLength? = null,
) {
    val isPlaying: Boolean get() = playingIndex != null
    val isEmpty: Boolean get() = tune.isEmpty()
    val atCap: Boolean get() = tune.size >= Tune.MAX_NOTES

    /** The hold dots filled under the note: one per whole beat of [heldLength]; null when no key is held. */
    val holdBeats: Int? get() = heldLength?.wholeBeats

    /** The key drawn pressed: the playing note's, else the held key. */
    val litMidi: Int? get() = playingIndex?.let { tune.getOrNull(it)?.midi } ?: heldMidi

    /** The pill of the tune line drawn coral: the playing note, else the held key's. */
    val litIndex: Int? get() = playingIndex ?: heldIndex
}
