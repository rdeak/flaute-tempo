package hr.bertlab.fluteassistant.audio.tuner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Captures microphone audio with [AudioRecord] and runs [YinPitchDetector] on a sliding window.
 *
 * - Prefers the UNPROCESSED source (no AGC / noise suppression) when the device supports it,
 *   because automatic gain and suppression distort the signal a tuner wants to analyse.
 * - Analysis window: 4096 samples, advanced by 2048 per frame (~43 ms at 48 kHz).
 * - Runs on [Dispatchers.IO] (the reads block) and conflates, so a slow collector always
 *   sees the newest frame; the microphone is released when the collector cancels.
 */
@Singleton
class AudioRecordTunerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : TunerEngine {

    override fun pitchFrames(): Flow<PitchFrame> = flow {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("Microphone permission not granted")
        }

        val sampleRate = pickSampleRate()
        val record = openRecorder(sampleRate)
        val detector = YinPitchDetector(sampleRate = sampleRate, bufferSize = WINDOW_SIZE)
        val window = FloatArray(WINDOW_SIZE)
        val chunk = FloatArray(HOP_SIZE)

        try {
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone is busy" }
            while (currentCoroutineContext().isActive) {
                val read = record.read(chunk, 0, HOP_SIZE, AudioRecord.READ_BLOCKING)
                if (read <= 0) throw IllegalStateException("Microphone read failed (code $read)")

                // Slide the analysis window left and append the new samples.
                System.arraycopy(window, read, window, 0, WINDOW_SIZE - read)
                System.arraycopy(chunk, 0, window, WINDOW_SIZE - read, read)

                val estimate = detector.detect(window)
                emit(
                    PitchFrame(
                        frequency = estimate?.frequency,
                        probability = estimate?.probability ?: 0.0,
                        level = levelOf(window),
                    ),
                )
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }
    }.conflate().flowOn(Dispatchers.IO) // blocking reads belong on IO; the UI only wants the newest frame

    private fun pickSampleRate(): Int =
        PREFERRED_SAMPLE_RATES.firstOrNull { rate ->
            AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT) > 0
        } ?: 44_100

    // Permission is verified at the top of pitchFrames() before this is reached.
    @SuppressLint("MissingPermission")
    private fun openRecorder(sampleRate: Int): AudioRecord {
        val minBytes = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT,
        )
        val record = AudioRecord.Builder()
            .setAudioSource(preferredSource())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(max(minBytes, WINDOW_SIZE * 4 * 2))
            .build()
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("Microphone could not be opened")
        }
        return record
    }

    private fun preferredSource(): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val unprocessed = audioManager
            ?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        return if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.MIC
    }

    private fun levelOf(window: FloatArray): Float {
        var energy = 0.0
        for (v in window) energy += v.toDouble() * v
        val rms = sqrt(energy / window.size)
        // Rough dB-style mapping: -60 dBFS -> 0, 0 dBFS -> 1.
        val db = 20.0 * kotlin.math.log10(rms + 1e-9)
        return min(1f, max(0f, ((db + 60.0) / 60.0).toFloat()))
    }

    private companion object {
        const val WINDOW_SIZE = 4096
        const val HOP_SIZE = 2048
        val PREFERRED_SAMPLE_RATES = intArrayOf(48_000, 44_100)
    }
}
