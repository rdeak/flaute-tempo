package hr.bertlab.fluteassistant.di

import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.bertlab.fluteassistant.audio.metronome.AudioTrackMetronomePlayer
import hr.bertlab.fluteassistant.audio.metronome.MetronomePlayer
import hr.bertlab.fluteassistant.audio.tuner.AudioRecordTunerEngine
import hr.bertlab.fluteassistant.audio.tuner.TunerEngine
import hr.bertlab.fluteassistant.core.MonotonicClock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindMetronomePlayer(impl: AudioTrackMetronomePlayer): MetronomePlayer

    @Binds
    @Singleton
    abstract fun bindTunerEngine(impl: AudioRecordTunerEngine): TunerEngine

    companion object {
        @Provides
        fun provideMonotonicClock(): MonotonicClock = MonotonicClock { SystemClock.elapsedRealtime() }
    }
}
