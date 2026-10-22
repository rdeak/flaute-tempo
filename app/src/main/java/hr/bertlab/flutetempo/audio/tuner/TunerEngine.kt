package hr.bertlab.flutetempo.audio.tuner

import kotlinx.coroutines.flow.Flow

/**
 * One analysis window of microphone input.
 * @property frequency detected fundamental in Hz, or null when nothing pitched was found.
 * @property probability confidence in [0, 1] (0 when [frequency] is null).
 * @property level input level in [0, 1] for a simple meter.
 */
data class PitchFrame(val frequency: Double?, val probability: Double, val level: Float)

/** Microphone pitch-tracking engine. */
interface TunerEngine {
    /**
     * A cold flow: collecting it opens the microphone and emits a [PitchFrame] roughly every
     * 40 ms; cancelling the collector releases the microphone.
     * Fails with [SecurityException] when RECORD_AUDIO has not been granted.
     */
    fun pitchFrames(): Flow<PitchFrame>
}
