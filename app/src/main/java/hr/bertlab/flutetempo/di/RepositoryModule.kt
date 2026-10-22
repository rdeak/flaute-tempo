package hr.bertlab.flutetempo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.bertlab.flutetempo.data.repository.PresetRepositoryImpl
import hr.bertlab.flutetempo.data.settings.SharedPreferencesSettingsStore
import hr.bertlab.flutetempo.domain.repository.PresetRepository
import hr.bertlab.flutetempo.domain.repository.SettingsStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPresetRepository(impl: PresetRepositoryImpl): PresetRepository

    @Binds
    @Singleton
    abstract fun bindSettingsStore(impl: SharedPreferencesSettingsStore): SettingsStore
}
