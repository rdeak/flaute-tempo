package hr.bertlab.fluteassistant.ui.notes

import kotlin.math.roundToInt

/** Immutable snapshot of everything the note reading game renders: one round of [ROUND_LENGTH] notes. */
data class NotesUiState(
    /** The notes the child is practising, MIDI numbers from StaffNotes.ALL; never empty. */
    val practiceNotes: Set<Int>,
    /** The note being asked. */
    val currentMidi: Int,
    /** Notes tapped by mistake for the current question; drawn red until it is answered. */
    val wrongTaps: Set<Int> = emptySet(),
    /** True from the right tap until the next note appears: the place is green and the note sounds. */
    val solved: Boolean = false,
    /** One entry per note answered this round: true when found on the first tap. */
    val results: List<Boolean> = emptyList(),
    /** Notes found on the first tap in a row, right now. */
    val streak: Int = 0,
    val bestStreak: Int = 0,
    /** Every tap this round, right or wrong, for the accuracy. */
    val taps: Int = 0,
    /** True once [ROUND_LENGTH] notes are answered and the summary is up. */
    val roundOver: Boolean = false,
    /** Whether a found note is sounded; a toggle beside the note picker, remembered. */
    val soundOn: Boolean = true,
    /** How long the round took, first tap to the tenth note found; null until then. */
    val roundMillis: Long? = null,
) {
    val answered: Int get() = results.size

    /** Notes found on the first tap. */
    val score: Int get() = results.count { it }

    /** Right taps out of all taps, in whole percent (every answered note took exactly one right tap); null before the first tap. */
    val accuracyPercent: Int? get() = if (taps == 0) null else (results.size * 100.0 / taps).roundToInt()

    companion object {
        const val ROUND_LENGTH = 10
    }
}

/** A round's time as the child reads it on a stopwatch: "1:23", whole seconds, minutes unbounded. */
fun formatRoundTime(millis: Long): String {
    val seconds = millis / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
