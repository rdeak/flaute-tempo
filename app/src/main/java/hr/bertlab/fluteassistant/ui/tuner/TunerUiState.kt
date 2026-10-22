package hr.bertlab.fluteassistant.ui.tuner

import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.NoteReading
import hr.bertlab.fluteassistant.domain.tuner.TunerMode

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
    /** The note the child chose to tune, as a MIDI number inside [FluteRange]; used in [TunerMode.TARGET]. */
    val targetMidi: Int = FluteRange.DEFAULT_TARGET,
    val mode: TunerMode = TunerMode.IDENTIFY,
    val errorMessage: String? = null,
) {
    /** Distance in cents from [targetMidi] to the played note (positive = sharp), or null in silence. */
    val centsOffTarget: Double?
        get() = reading?.let { NoteMath.centsBetween(it.frequency, targetMidi, referenceA4) }

    /** What the gauge shows: cents from the nearest note when identifying, from the target when tuning to one. */
    val gaugeCents: Double?
        get() = when (mode) {
            TunerMode.IDENTIFY -> reading?.cents
            TunerMode.TARGET -> centsOffTarget
        }
}
