package hr.bertlab.fluteassistant.domain.model

/**
 * How loud / which sample a single click should use.
 *
 * STRONG      - first beat of the bar (accent sound, highest pitch)
 * MEDIUM      - secondary accent in compound meters (e.g. beat 4 of 6/8)
 * WEAK        - any other main beat
 * SUBDIVISION - an in-between tick (8th / 16th / triplet / shuffle)
 */
enum class BeatStrength { STRONG, MEDIUM, WEAK, SUBDIVISION }

private fun simpleMeter(beats: Int): List<BeatStrength> =
    List(beats) { index -> if (index == 0) BeatStrength.STRONG else BeatStrength.WEAK }

/**
 * Supported time signatures. [accents] has exactly [beatsPerBar] entries and describes
 * the strength of every main beat in the bar.
 */
enum class TimeSignature(
    val beatsPerBar: Int,
    val noteValue: Int,
    val accents: List<BeatStrength>,
) {
    TWO_FOUR(2, 4, simpleMeter(2)),
    THREE_FOUR(3, 4, simpleMeter(3)),
    FOUR_FOUR(4, 4, simpleMeter(4)),
    FIVE_FOUR(5, 4, simpleMeter(5)),
    SIX_EIGHT(
        6, 8,
        listOf(
            BeatStrength.STRONG, BeatStrength.WEAK, BeatStrength.WEAK,
            BeatStrength.MEDIUM, BeatStrength.WEAK, BeatStrength.WEAK,
        ),
    );

    val label: String get() = "$beatsPerBar/$noteValue"
}

/**
 * Rhythmic subdivision of one beat. [offsets] are the positions inside the beat
 * (0.0 = on the beat, 0.5 = halfway) at which a click is played. The first offset is
 * always 0.0 so the main beat itself is part of every pattern.
 */
enum class Subdivision(val label: String, val offsets: List<Double>) {
    NONE("Quarter", listOf(0.0)),
    EIGHTH("8ths", listOf(0.0, 0.5)),
    SIXTEENTH("16ths", listOf(0.0, 0.25, 0.5, 0.75)),
    TRIPLET("Triplets", listOf(0.0, 1.0 / 3.0, 2.0 / 3.0)),
    /** Swing feel: the off-beat 8th is delayed to the last triplet position. */
    SHUFFLE("Shuffle", listOf(0.0, 2.0 / 3.0)),
}

/** Click sound family. Each profile has an accent, beat and subdivision variant. */
enum class SoundProfile(val label: String) {
    WOODBLOCK("Woodblock"),
    BEEP("Digital Beep"),
    BELL("Bell"),
}

/** Everything the metronome engine needs to produce a click pattern. */
data class MetronomeConfig(
    val bpm: Int = DEFAULT_BPM,
    val timeSignature: TimeSignature = TimeSignature.FOUR_FOUR,
    val subdivision: Subdivision = Subdivision.NONE,
    val sound: SoundProfile = SoundProfile.WOODBLOCK,
) {
    /** Returns a copy with [bpm] clamped to the supported range. */
    fun withBpm(newBpm: Int): MetronomeConfig = copy(bpm = newBpm.coerceIn(MIN_BPM, MAX_BPM))

    /** One-line description for preset lists and the playback notification, e.g. "120 BPM · 4/4 · 8ths · Woodblock". */
    fun summary(): String = "$bpm BPM · ${timeSignature.label} · ${subdivision.label} · ${sound.label}"

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 300
        const val DEFAULT_BPM = 120
    }
}
