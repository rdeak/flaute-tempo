package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.SoundProfile
import hr.bertlab.fluteassistant.domain.model.Subdivision
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.ui.common.LocaleProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class MetronomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: MutableState<MetronomeUiState>
    private var toggles = 0
    private val adjustments = mutableListOf<Int>()
    private var chosenSignature: TimeSignature? = null
    private var chosenSubdivision: Subdivision? = null
    private var chosenSound: SoundProfile? = null

    private fun show(initial: MetronomeUiState, locale: Locale = Locale.ENGLISH) {
        state = mutableStateOf(initial)
        compose.setContent {
            LocaleProvider(locale) {
                MetronomeContent(
                    state = state.value,
                    onTogglePlayback = { toggles++ },
                    onBpmChange = {},
                    onAdjustBpm = { adjustments += it },
                    onTapTempo = {},
                    onTimeSignatureChange = { chosenSignature = it },
                    onSubdivisionChange = { chosenSubdivision = it },
                    onSoundChange = { chosenSound = it },
                    onVolumeChange = {},
                    onOpenPresets = {},
                )
            }
        }
    }

    @Test
    fun start_button_toggles_playback_and_reflects_the_playing_state() {
        show(MetronomeUiState())
        compose.onNodeWithText("Start").performClick()
        assertEquals(1, toggles)

        compose.runOnIdle { state.value = state.value.copy(isPlaying = true) }
        compose.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun step_buttons_adjust_the_tempo() {
        show(MetronomeUiState())
        compose.onNodeWithText("+5").performClick()
        compose.onNodeWithText("−5").performClick()
        assertEquals(listOf(5, -5), adjustments)
    }

    @Test
    fun meter_subdivision_and_sound_choices_reach_the_callbacks() {
        show(MetronomeUiState())
        compose.onNodeWithText("3/4").performClick()
        compose.onNodeWithText("Triplets").performClick()
        compose.onNodeWithText("Bell").performClick()
        assertEquals(TimeSignature.THREE_FOUR, chosenSignature)
        assertEquals(Subdivision.TRIPLET, chosenSubdivision)
        assertEquals(SoundProfile.BELL, chosenSound)
    }

    @Test
    fun croatian_system_language_shows_croatian_labels() {
        show(MetronomeUiState(config = MetronomeConfig(subdivision = Subdivision.TRIPLET)), Locale("hr"))
        compose.onNodeWithText("Pokreni").assertIsDisplayed()
        compose.onNodeWithText("Mjera").assertIsDisplayed()
        compose.onNodeWithText("Triole").assertIsDisplayed()
        compose.onNodeWithText("Drveni blok").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Povećaj tempo").assertCountEquals(2) // +1 and +5
    }
}
