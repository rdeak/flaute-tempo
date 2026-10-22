package hr.bertlab.fluteassistant.ui.common

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.SoundProfile
import hr.bertlab.fluteassistant.domain.model.Subdivision

/*
 * Display names live in string resources so the app can be localised (English by default,
 * Croatian in values-hr); the domain enums stay language-free.
 */

@StringRes
fun Subdivision.labelRes(): Int = when (this) {
    Subdivision.NONE -> R.string.subdivision_quarter
    Subdivision.EIGHTH -> R.string.subdivision_eighth
    Subdivision.SIXTEENTH -> R.string.subdivision_sixteenth
    Subdivision.TRIPLET -> R.string.subdivision_triplet
    Subdivision.SHUFFLE -> R.string.subdivision_shuffle
}

@StringRes
fun SoundProfile.labelRes(): Int = when (this) {
    SoundProfile.WOODBLOCK -> R.string.sound_woodblock
    SoundProfile.BEEP -> R.string.sound_beep
    SoundProfile.BELL -> R.string.sound_bell
}

/** One line for preset lists and the playback notification, e.g. "120 BPM · 4/4 · Quarter · Woodblock". */
fun MetronomeConfig.summary(resources: Resources): String = resources.getString(
    R.string.config_summary,
    bpm,
    timeSignature.label,
    resources.getString(subdivision.labelRes()),
    resources.getString(sound.labelRes()),
)

@Composable
@ReadOnlyComposable
fun MetronomeConfig.summary(): String = summary(LocalResources.current)

/**
 * Note name with octave for a MIDI number in the user's language: "A♯4" in English, "B4" in
 * Croatian, where the German names (H for B, B for B flat) are what musicians learn.
 */
fun noteLabel(resources: Resources, midi: Int): String {
    val pitchClass = Math.floorMod(midi, 12)
    val octave = Math.floorDiv(midi, 12) - 1
    return resources.getStringArray(R.array.note_names)[pitchClass] + octave
}

@Composable
@ReadOnlyComposable
fun noteLabel(midi: Int): String = noteLabel(LocalResources.current, midi)
