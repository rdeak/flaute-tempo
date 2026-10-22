package hr.bertlab.flutetempo.di

import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hr.bertlab.flutetempo.audio.metronome.AudioTrackMetronomePlayer
import hr.bertlab.flutetempo.audio.metronome.MetronomePlayer
import hr.bertlab.flutetempo.audio.tuner.AudioRecordTunerEngine
import hr.bertlab.flutetempo.audio.tuner.TunerEngine
import hr.bertlab.flutetempo.core.MonotonicClock
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
