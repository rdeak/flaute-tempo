package hr.bertlab.fluteassistant.domain.repository

import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.tuner.TunerMode

/**
 * Small key-value persistence for "where the user left off": the last metronome settings,
 * the volume, the tuner's reference pitch and the note it tunes to. Synchronous by design
 * (tiny values, read once at ViewModel creation).
 */
interface SettingsStore {
    fun loadMetronomeConfig(): MetronomeConfig
    fun saveMetronomeConfig(config: MetronomeConfig)
    fun loadVolume(): Float
    fun saveVolume(volume: Float)
    fun loadReferenceA4(): Double
    fun saveReferenceA4(hz: Double)
    /** The note the tuner measures against, as a MIDI number (see FluteRange); not clamped here. */
    fun loadTargetNote(): Int
    fun saveTargetNote(midi: Int)
    fun loadTunerMode(): TunerMode
    fun saveTunerMode(mode: TunerMode)
}
