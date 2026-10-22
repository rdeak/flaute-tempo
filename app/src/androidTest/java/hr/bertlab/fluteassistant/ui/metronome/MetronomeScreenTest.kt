package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.ui.common.LocaleProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private var chosenBpm: Int? = null
    private var chosenSignature: TimeSignature? = null
    private var chosenVolume: Float? = null

    private fun show(initial: MetronomeUiState, locale: Locale = Locale.ENGLISH) {
        state = mutableStateOf(initial)
        compose.setContent {
            LocaleProvider(locale) {
                MetronomeContent(
                    state = state.value,
                    onTogglePlayback = { toggles++ },
                    onBpmChange = { bpm ->
                        chosenBpm = bpm
                        state.value = state.value.copy(config = state.value.config.withBpm(bpm))
                    },
                    onTimeSignatureChange = { chosenSignature = it },
                    onVolumeChange = { chosenVolume = it },
                )
            }
        }
    }

    @Test
    fun start_button_toggles_playback_and_reflects_the_playing_state() {
        show(MetronomeUiState())
        compose.onNodeWithText("Let's play!").performClick()
        assertEquals(1, toggles)

        compose.runOnIdle { state.value = state.value.copy(isPlaying = true) }
        compose.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun tapping_the_number_opens_a_wheel_with_the_neighbouring_tempos() {
        show(MetronomeUiState(config = MetronomeConfig(bpm = 120)))
        compose.onNodeWithText("119").assertDoesNotExist()
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithText("119").assertIsDisplayed()
        compose.onNodeWithText("121").assertIsDisplayed()
    }

    @Test
    fun scrolling_the_wheel_changes_the_tempo_one_beat_per_notch() {
        show(MetronomeUiState(config = MetronomeConfig(bpm = 120)))
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithContentDescription("Tempo wheel, scroll to change the speed")
            .performScrollToIndex(123 - MetronomeConfig.MIN_BPM)
        compose.waitForIdle()
        assertEquals(123, chosenBpm)
        compose.onNodeWithText("122").assertIsDisplayed()
        compose.onNodeWithText("124").assertIsDisplayed()
    }

    @Test
    fun tapping_the_centre_value_closes_the_wheel() {
        show(MetronomeUiState(config = MetronomeConfig(bpm = 120)))
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithText("119").assertIsDisplayed()
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithText("119").assertDoesNotExist()
        compose.onNodeWithText("120").assertIsDisplayed()
    }

    @Test
    fun tapping_anywhere_else_closes_the_wheel() {
        show(MetronomeUiState(config = MetronomeConfig(bpm = 120)))
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithText("119").assertIsDisplayed()
        compose.onNodeWithText("Let's play!").performClick()
        compose.onNodeWithText("119").assertDoesNotExist()
        assertEquals(1, toggles)
    }

    @Test
    fun the_open_wheel_follows_a_level_pill() {
        show(MetronomeUiState(config = MetronomeConfig(bpm = 120)))
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithText("Star").performClick()
        assertEquals(100, chosenBpm)
        compose.onNodeWithText("119").assertDoesNotExist()
        compose.onNodeWithText("100").assertIsDisplayed()
    }

    @Test
    fun the_beats_stepper_shows_the_current_signature_and_steps_both_ways() {
        show(MetronomeUiState(config = MetronomeConfig(timeSignature = TimeSignature.FOUR_FOUR)))
        compose.onNodeWithText("4/4").assertIsDisplayed()
        compose.onNodeWithText("3/4").assertDoesNotExist()

        compose.onNodeWithContentDescription("Next beats in a bar").performClick()
        assertEquals(TimeSignature.FIVE_FOUR, chosenSignature)
        compose.onNodeWithContentDescription("Previous beats in a bar").performClick()
        assertEquals(TimeSignature.THREE_FOUR, chosenSignature)
    }

    @Test
    fun the_beats_stepper_sits_above_the_play_button() {
        show(MetronomeUiState())
        val stepper = compose.onNodeWithContentDescription("Beats in a bar").getBoundsInRoot()
        val play = compose.onNodeWithText("Let's play!").getBoundsInRoot()
        assertTrue("stepper bottom ${stepper.bottom} should be above play top ${play.top}", stepper.bottom < play.top)
    }

    @Test
    fun the_beats_stepper_wraps_from_the_last_signature_to_the_first() {
        show(MetronomeUiState(config = MetronomeConfig(timeSignature = TimeSignature.entries.last())))
        compose.onNodeWithContentDescription("Next beats in a bar").performClick()
        assertEquals(TimeSignature.TWO_FOUR, chosenSignature)
    }

    @Test
    fun skill_level_chip_sets_its_tempo_in_one_tap() {
        show(MetronomeUiState())
        compose.onNodeWithText("Star").performClick()
        assertEquals(100, chosenBpm)
        compose.onNodeWithText("Beginner").performClick()
        assertEquals(60, chosenBpm)
    }

    @Test
    fun the_level_matching_the_tempo_is_highlighted_and_an_in_between_tempo_highlights_none() {
        show(MetronomeUiState(config = MetronomeConfig(bpm = 120)))
        compose.onNodeWithText("Master").assertIsSelected()
        compose.onNodeWithText("Star").assertIsNotSelected()

        compose.runOnIdle { state.value = state.value.copy(config = MetronomeConfig(bpm = 121)) }
        listOf("Beginner", "Learner", "Star", "Master").forEach { compose.onNodeWithText(it).assertIsNotSelected() }
    }

    @Test
    fun croatian_system_language_shows_croatian_labels() {
        show(MetronomeUiState(), Locale("hr"))
        compose.onNodeWithText("Idemo!").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dobe u taktu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tvoja razina").assertIsDisplayed()
        compose.onNodeWithContentDescription("Glasnoća").assertExists()
        compose.onNodeWithText("Početnik").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Sljedeće dobe u taktu").assertCountEquals(1)
        compose.onNodeWithText("120").performClick()
        compose.onNodeWithContentDescription("Kotačić tempa, zavrti ga za promjenu brzine").assertExists()
    }
}
