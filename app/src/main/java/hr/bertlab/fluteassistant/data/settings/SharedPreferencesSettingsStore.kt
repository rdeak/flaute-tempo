package hr.bertlab.fluteassistant.data.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.SoundProfile
import hr.bertlab.fluteassistant.domain.model.Subdivision
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.domain.repository.SettingsStore
import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import javax.inject.Inject
import javax.inject.Singleton

/** [SettingsStore] on top of SharedPreferences; no extra dependency needed for a few values. */
@Singleton
class SharedPreferencesSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) : SettingsStore {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun loadMetronomeConfig(): MetronomeConfig = MetronomeConfig(
        timeSignature = enumOrDefault(prefs.getString(KEY_TIME_SIGNATURE, null), TimeSignature.FOUR_FOUR),
        subdivision = enumOrDefault(prefs.getString(KEY_SUBDIVISION, null), Subdivision.NONE),
        sound = enumOrDefault(prefs.getString(KEY_SOUND, null), SoundProfile.WOODBLOCK),
    ).withBpm(prefs.getInt(KEY_BPM, MetronomeConfig.DEFAULT_BPM))

    override fun saveMetronomeConfig(config: MetronomeConfig) = prefs.edit {
        putInt(KEY_BPM, config.bpm)
        putString(KEY_TIME_SIGNATURE, config.timeSignature.name)
        putString(KEY_SUBDIVISION, config.subdivision.name)
        putString(KEY_SOUND, config.sound.name)
    }

    override fun loadVolume(): Float = prefs.getFloat(KEY_VOLUME, DEFAULT_VOLUME).coerceIn(0f, 1f)

    override fun saveVolume(volume: Float) = prefs.edit { putFloat(KEY_VOLUME, volume) }

    override fun loadReferenceA4(): Double =
        prefs.getFloat(KEY_A4, NoteMath.DEFAULT_A4.toFloat()).toDouble()

    override fun saveReferenceA4(hz: Double) = prefs.edit { putFloat(KEY_A4, hz.toFloat()) }

    override fun loadTargetNote(): Int = prefs.getInt(KEY_TARGET_NOTE, FluteRange.DEFAULT_TARGET)

    override fun saveTargetNote(midi: Int) = prefs.edit { putInt(KEY_TARGET_NOTE, midi) }

    override fun loadTunerMode(): TunerMode =
        enumOrDefault(prefs.getString(KEY_TUNER_MODE, null), TunerMode.IDENTIFY)

    override fun saveTunerMode(mode: TunerMode) = prefs.edit { putString(KEY_TUNER_MODE, mode.name) }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        const val FILE = "settings"
        const val KEY_BPM = "bpm"
        const val KEY_TIME_SIGNATURE = "time_signature"
        const val KEY_SUBDIVISION = "subdivision"
        const val KEY_SOUND = "sound"
        const val KEY_VOLUME = "volume"
        const val KEY_A4 = "reference_a4"
        const val KEY_TARGET_NOTE = "target_note"
        const val KEY_TUNER_MODE = "tuner_mode"
        const val DEFAULT_VOLUME = 0.8f
    }
}
