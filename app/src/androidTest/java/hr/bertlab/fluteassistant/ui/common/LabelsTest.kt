package hr.bertlab.fluteassistant.ui.common

import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.SoundProfile
import hr.bertlab.fluteassistant.domain.model.Subdivision
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class LabelsTest {

    private val base = InstrumentationRegistry.getInstrumentation().targetContext

    private fun resources(locale: Locale): Resources {
        val configuration = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)) }
        return base.createConfigurationContext(configuration).resources
    }

    @Test
    fun summary_is_localised() {
        val config = MetronomeConfig(bpm = 120, subdivision = Subdivision.NONE, sound = SoundProfile.WOODBLOCK)
        assertEquals("120 BPM · 4/4 · Quarter · Woodblock", config.summary(resources(Locale.ENGLISH)))
        assertEquals("120 BPM · 4/4 · Četvrtinke · Drveni blok", config.summary(resources(Locale("hr"))))
    }

    @Test
    fun note_names_follow_the_locale() {
        // Croatian musicians use the German names: H for B and B for B flat.
        assertEquals("B4", noteLabel(resources(Locale.ENGLISH), 71))
        assertEquals("H4", noteLabel(resources(Locale("hr")), 71))
        assertEquals("A♯4", noteLabel(resources(Locale.ENGLISH), 70))
        assertEquals("B4", noteLabel(resources(Locale("hr")), 70))
        assertEquals("C4", noteLabel(resources(Locale("hr")), 60))
    }

    @Test
    fun other_system_languages_fall_back_to_english() {
        assertEquals("Triplets", resources(Locale.GERMAN).getString(Subdivision.TRIPLET.labelRes()))
        assertEquals("Bell", resources(Locale.FRENCH).getString(SoundProfile.BELL.labelRes()))
    }
}
