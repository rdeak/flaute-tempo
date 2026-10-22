package hr.bertlab.flutetempo.domain.usecase

import hr.bertlab.flutetempo.domain.model.MetronomeConfig
import hr.bertlab.flutetempo.domain.model.Preset
import hr.bertlab.flutetempo.domain.repository.PresetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePresetsUseCase @Inject constructor(private val repository: PresetRepository) {
    operator fun invoke(): Flow<List<Preset>> = repository.observePresets()
}

class SavePresetUseCase @Inject constructor(private val repository: PresetRepository) {
    /** Trims the name and refuses blank names so the UI can rely on a simple contract. */
    suspend operator fun invoke(name: String, config: MetronomeConfig): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Preset name must not be blank" }
        return repository.save(trimmed, config)
    }
}

class DeletePresetUseCase @Inject constructor(private val repository: PresetRepository) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}
