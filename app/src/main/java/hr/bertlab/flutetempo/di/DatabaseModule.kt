package hr.bertlab.flutetempo.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hr.bertlab.flutetempo.data.local.FluteTempoDatabase
import hr.bertlab.flutetempo.data.local.PresetDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FluteTempoDatabase =
        Room.databaseBuilder(context, FluteTempoDatabase::class.java, FluteTempoDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun providePresetDao(database: FluteTempoDatabase): PresetDao = database.presetDao()
}
