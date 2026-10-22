package hr.bertlab.flutetempo.ui.metronome

import hr.bertlab.flutetempo.domain.model.BeatStrength
import hr.bertlab.flutetempo.domain.model.MetronomeConfig
import hr.bertlab.flutetempo.domain.model.Preset

/** One-shot messages the screen shows in a snackbar (mapped to string resources in the UI). */
enum class MetronomeMessage { AUDIO_UNAVAILABLE, SAVE_FAILED, DELETE_FAILED }

/** Immutable snapshot of everything the metronome screen renders. */
data class MetronomeUiState(
    val config: MetronomeConfig = MetronomeConfig(),
    val isPlaying: Boolean = false,
    /** Master volume in [0, 1]. Not part of presets. */
    val volume: Float = 0.8f,
    /** Index of the beat currently sounding, or -1 while stopped. */
    val currentBeat: Int = -1,
    val currentStrength: BeatStrength? = null,
    /** Increments on every main beat; the UI keys its flash animation on it. */
    val beatPulse: Long = 0L,
    val presets: List<Preset> = emptyList(),
    /** Taps in the current tap-tempo sequence (for a small hint in the UI). */
    val tapCount: Int = 0,
    val userMessage: MetronomeMessage? = null,
)

/** Traditional Italian tempo marking for a BPM value. */
fun tempoMarking(bpm: Int): String = when {
    bpm < 40 -> "Grave"
    bpm < 60 -> "Largo"
    bpm < 66 -> "Larghetto"
    bpm < 76 -> "Adagio"
    bpm < 108 -> "Andante"
    bpm < 120 -> "Moderato"
    bpm < 156 -> "Allegro"
    bpm < 176 -> "Vivace"
    bpm < 200 -> "Presto"
    else -> "Prestissimo"
}
