package hr.bertlab.flutetempo.domain.repository

import hr.bertlab.flutetempo.domain.model.MetronomeConfig
import hr.bertlab.flutetempo.domain.model.Preset
import kotlinx.coroutines.flow.Flow

/** Persistence boundary for presets. Implemented by the data layer (Room). */
interface PresetRepository {
    /** Emits the full preset list whenever it changes, newest first. */
    fun observePresets(): Flow<List<Preset>>

    /** Persists [config] under [name] and returns the new preset id. */
    suspend fun save(name: String, config: MetronomeConfig): Long

    suspend fun delete(id: Long)
}
