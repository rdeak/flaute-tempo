package hr.bertlab.fluteassistant.domain.flute

import hr.bertlab.fluteassistant.domain.notes.StaffNotes

/** One note of the child's tune: which key, and for how long it was held (clamped, see [Tune]). */
data class TuneNote(val midi: Int, val heldMs: Int)

/**
 * The tune the flute tab records: a line of [TuneNote]s, one per key press, played back in
 * order with each note's held length and a short [GAP_MS] of silence between notes so a
 * repeated note is heard twice. Pauses between presses are not recorded.
 *
 * Stored as one string, `"60:450,67:300"`. The decoder is lenient: entries it cannot read are
 * dropped, held lengths are clamped, and a third field per note (a pause before it, in ms) is
 * accepted and ignored so pauses can be stored later without a migration.
 */
object Tune {
    /** Far beyond what a child makes on purpose; a press at the cap sounds but is not added. */
    const val MAX_NOTES = 200
    const val MIN_NOTE_MS = 300
    const val MAX_NOTE_MS = 2000
    const val GAP_MS = 80

    private const val NOTE_SEPARATOR = ","
    private const val FIELD_SEPARATOR = ":"

    fun clampHeld(ms: Long): Int = ms.coerceIn(MIN_NOTE_MS.toLong(), MAX_NOTE_MS.toLong()).toInt()

    /** Keeps only the notes the keys offer ([StaffNotes.ALL]), clamps every length and caps the count. */
    fun sanitize(notes: List<TuneNote>): List<TuneNote> = notes
        .filter { it.midi in StaffNotes.ALL }
        .map { it.copy(heldMs = clampHeld(it.heldMs.toLong())) }
        .take(MAX_NOTES)

    fun encode(notes: List<TuneNote>): String =
        notes.joinToString(NOTE_SEPARATOR) { "${it.midi}$FIELD_SEPARATOR${it.heldMs}" }

    fun decode(text: String): List<TuneNote> = sanitize(
        text.split(NOTE_SEPARATOR).mapNotNull { entry ->
            val fields = entry.split(FIELD_SEPARATOR)
            if (fields.size < 2) return@mapNotNull null
            val midi = fields[0].trim().toIntOrNull() ?: return@mapNotNull null
            val heldMs = fields[1].trim().toIntOrNull() ?: return@mapNotNull null
            TuneNote(midi, heldMs)
        },
    )
}
