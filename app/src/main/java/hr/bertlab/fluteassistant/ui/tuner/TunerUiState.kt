package hr.bertlab.fluteassistant.ui.tuner

import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.NoteReading

/** Immutable snapshot of everything the tuner screen renders. */
data class TunerUiState(
    val permissionGranted: Boolean = false,
    /** True once the system permission dialog has been shown, so it is not re-shown on every visit. */
    val permissionRequested: Boolean = false,
    val isListening: Boolean = false,
    /** Smoothed reading of the note being played, or null during silence. */
    val reading: NoteReading? = null,
    /** Microphone input level in [0, 1]. */
    val level: Float = 0f,
    /** Reference pitch for A4 in Hz (440 standard; many orchestras use 442). */
    val referenceA4: Double = NoteMath.DEFAULT_A4,
    val errorMessage: String? = null,
)
