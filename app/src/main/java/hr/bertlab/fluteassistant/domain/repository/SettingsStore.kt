package hr.bertlab.fluteassistant.domain.repository

import hr.bertlab.fluteassistant.domain.model.MetronomeConfig

/**
 * Small key-value persistence for "where the user left off": the last metronome settings,
 * the volume and the tuner's reference pitch. Synchronous by design (tiny values, read once
 * at ViewModel creation).
 */
interface SettingsStore {
    fun loadMetronomeConfig(): MetronomeConfig
    fun saveMetronomeConfig(config: MetronomeConfig)
    fun loadVolume(): Float
    fun saveVolume(volume: Float)
    fun loadReferenceA4(): Double
    fun saveReferenceA4(hz: Double)
}
