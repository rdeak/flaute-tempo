package hr.bertlab.fluteassistant.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.bertlab.fluteassistant.playback.BackgroundPlayback
import hr.bertlab.fluteassistant.playback.ForegroundServiceBackgroundPlayback

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {

    @Binds
    abstract fun bindBackgroundPlayback(impl: ForegroundServiceBackgroundPlayback): BackgroundPlayback
}
