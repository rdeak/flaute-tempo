package hr.bertlab.flutetempo.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.bertlab.flutetempo.playback.BackgroundPlayback
import hr.bertlab.flutetempo.playback.ForegroundServiceBackgroundPlayback

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {

    @Binds
    abstract fun bindBackgroundPlayback(impl: ForegroundServiceBackgroundPlayback): BackgroundPlayback
}
