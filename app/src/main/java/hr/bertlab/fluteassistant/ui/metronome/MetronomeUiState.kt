package hr.bertlab.fluteassistant.ui.metronome

import androidx.annotation.StringRes
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.BeatStrength
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.Preset

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
    /** Taps in the current tap-tempo sequence (not shown by the simplified UI; kept for an advanced mode). */
    val tapCount: Int = 0,
    val userMessage: MetronomeMessage? = null,
)

/**
 * How a tempo feels, in words a child understands: an emoji plus a localised word
 * ("🐢 Slow") instead of BPM figures or Italian markings.
 */
enum class TempoFeel(val emoji: String, @StringRes val labelRes: Int) {
    VERY_SLOW("\uD83D\uDC0C", R.string.feel_very_slow), // snail
    SLOW("\uD83D\uDC22", R.string.feel_slow), // turtle
    MEDIUM("\uD83D\uDEB6", R.string.feel_medium), // person walking
    FAST("\uD83C\uDFC3", R.string.feel_fast), // person running
    SUPER_FAST("\uD83D\uDE80", R.string.feel_super_fast), // rocket
}

fun tempoFeel(bpm: Int): TempoFeel = when {
    bpm < 60 -> TempoFeel.VERY_SLOW
    bpm < 90 -> TempoFeel.SLOW
    bpm < 120 -> TempoFeel.MEDIUM
    bpm < 160 -> TempoFeel.FAST
    else -> TempoFeel.SUPER_FAST
}
