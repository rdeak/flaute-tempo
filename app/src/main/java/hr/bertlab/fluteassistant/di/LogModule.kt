package hr.bertlab.fluteassistant.di

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hr.bertlab.fluteassistant.core.log.AndroidLogger
import hr.bertlab.fluteassistant.core.log.Logger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogModule {

    @Provides
    @Singleton
    fun provideLogger(@ApplicationContext context: Context): Logger =
        AndroidLogger(debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0)
}
