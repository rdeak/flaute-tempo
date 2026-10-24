package hr.bertlab.fluteassistant.domain.flute

import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * How long a note of the child's tune lasts, in beats of the metronome's tempo: the five
 * lengths a beginner reads (eighth, quarter, half, dotted half and whole note), kept as
 * [halfBeats] so every length is a whole number.
 */
enum class NoteLength(val halfBeats: Int) {
    EIGHTH(1),
    QUARTER(2),
    HALF(4),
    DOTTED_HALF(6),
    WHOLE(8),
    ;

    /** Whole beats, as the hold dots count them: none for an eighth. */
    val wholeBeats: Int get() = halfBeats / 2

    companion object {
        /**
         * The beats after the press at which a held key's note grows into the next length:
         * halfway between two lengths, so a hold always makes the nearest one.
         */
        val GROW_AT_BEATS: List<Double> = listOf(0.75, 1.5, 2.5, 3.5)

        /**
         * The length a key held for [beats] makes: the nearest of the five, so under ¾ of a beat
         * is an eighth, then a quarter up to 1½ beats, a half up to 2½, a dotted half up to 3½,
         * and anything longer a whole note.
         */
        fun ofBeats(beats: Double): NoteLength {
            val grown = GROW_AT_BEATS.count { beats >= it }
            return entries[grown]
        }

        /** The stored [halfBeats], or the nearest length to it. */
        fun ofHalfBeats(halfBeats: Int): NoteLength = ofBeats(halfBeats / 2.0)
    }
}

/** One note of the child's tune: which key, and how long it lasts ([NoteLength], in beats). */
data class TuneNote(val midi: Int, val length: NoteLength)

/**
 * The tune the flute tab records: a line of [TuneNote]s, one per key press, each as long as
 * the key was held, snapped to the metronome's tempo ([lengthOf]). It is played back at the
 * metronome's tempo, so the rhythm stays when the tempo changes, with a short gap
 * ([gapMs]) taken out of the end of each note so a repeated note is heard twice. Pauses
 * between presses are not recorded.
 *
 * Stored as one string, `"60:2,67:4"`: each note's key and its length in half beats. The
 * decoder is lenient: entries it cannot read are dropped, a third field per note (a pause
 * before it) is accepted and ignored so pauses can be stored later without a migration, and
 * a length above [NoteLength.WHOLE]'s is a held time in milliseconds from before lengths were
 * snapped (never under 300), turned into a length at the tempo given.
 */
object Tune {
    /** Far beyond what a child makes on purpose; a press at the cap sounds but is not added. */
    const val MAX_NOTES = 200

    /** The silence after each note, never more than a quarter of it. */
    const val GAP_MS = 80

    private const val NOTE_SEPARATOR = ","
    private const val FIELD_SEPARATOR = ":"

    /** How long one beat lasts at [bpm], in ms. */
    fun beatMs(bpm: Int): Double = 60_000.0 / bpm

    /** How long a note of [length] lasts at [bpm], gap included, in ms. */
    fun durationMs(length: NoteLength, bpm: Int): Long = (length.halfBeats * beatMs(bpm) / 2).roundToLong()

    /** The silence at the end of a note lasting [durationMs]. */
    fun gapMs(durationMs: Long): Long = minOf(GAP_MS.toLong(), durationMs / 4)

    /** The length a key held for [heldMs] at [bpm] makes. */
    fun lengthOf(heldMs: Long, bpm: Int): NoteLength = NoteLength.ofBeats(heldMs / beatMs(bpm))

    /**
     * The ms after the press at which a held key's note grows to its next length at [bpm],
     * rounded up, so the length shown while the key is held never runs ahead of [lengthOf].
     */
    fun growAtMs(bpm: Int): List<Long> = NoteLength.GROW_AT_BEATS.map { ceil(it * beatMs(bpm)).toLong() }

    /** Keeps only the notes the keys offer ([StaffNotes.ALL]) and caps the count. */
    fun sanitize(notes: List<TuneNote>): List<TuneNote> = notes
        .filter { it.midi in StaffNotes.ALL }
        .take(MAX_NOTES)

    fun encode(notes: List<TuneNote>): String =
        notes.joinToString(NOTE_SEPARATOR) { "${it.midi}$FIELD_SEPARATOR${it.length.halfBeats}" }

    /** Reads a stored tune; old held times in ms become lengths at [legacyBpm]. */
    fun decode(text: String, legacyBpm: Int): List<TuneNote> = sanitize(
        text.split(NOTE_SEPARATOR).mapNotNull { entry ->
            val fields = entry.split(FIELD_SEPARATOR)
            if (fields.size < 2) return@mapNotNull null
            val midi = fields[0].trim().toIntOrNull() ?: return@mapNotNull null
            val value = fields[1].trim().toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val length = if (value <= NoteLength.WHOLE.halfBeats) {
                NoteLength.ofHalfBeats(value)
            } else {
                lengthOf(value.toLong(), legacyBpm)
            }
            TuneNote(midi, length)
        },
    )
}
