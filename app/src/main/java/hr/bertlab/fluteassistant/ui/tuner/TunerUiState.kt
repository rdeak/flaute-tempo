package hr.bertlab.fluteassistant.ui.tuner

import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.NoteReading
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import kotlin.math.abs

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
    /** True while the reference tone for [targetMidi] sounds; readings pause so the speaker is not measured. */
    val isPlayingReference: Boolean = false,
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

    /** Whether [gaugeCents] counts as in tune, and if not which way to go; [TuningStatus.NONE] in silence. */
    val status: TuningStatus
        get() {
            val cents = gaugeCents ?: return TuningStatus.NONE
            return when {
                abs(cents) <= IN_TUNE_CENTS -> TuningStatus.IN_TUNE
                cents < 0 -> TuningStatus.LOW
                else -> TuningStatus.HIGH
            }
        }

    companion object {
        /**
         * The green zone, shared by the status line, the gauge and the note wheel. A child's
         * held note wobbles about this much from breath alone, so a professional's 5 cents
         * would rarely settle on "Perfect!".
         */
        const val IN_TUNE_CENTS = 10.0
        /** Amber up to here, red beyond. */
        const val SLIGHTLY_OFF_CENTS = 20.0
    }
}

/** What the tuner tells the child: nothing heard, in tune, or which way the note must go. */
enum class TuningStatus { NONE, IN_TUNE, LOW, HIGH }
