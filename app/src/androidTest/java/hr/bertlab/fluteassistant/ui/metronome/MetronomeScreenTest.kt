package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
    private val adjustments = mutableListOf<Int>()
    private var chosenSignature: TimeSignature? = null

    private fun show(initial: MetronomeUiState, locale: Locale = Locale.ENGLISH) {
        state = mutableStateOf(initial)
        compose.setContent {
            LocaleProvider(locale) {
                MetronomeContent(
                    state = state.value,
                    onTogglePlayback = { toggles++ },
                    onBpmChange = { chosenBpm = it },
                    onAdjustBpm = { adjustments += it },
                    onTimeSignatureChange = { chosenSignature = it },
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
    fun step_buttons_adjust_the_tempo() {
        show(MetronomeUiState())
        compose.onNodeWithContentDescription("Increase tempo").performClick()
        compose.onNodeWithContentDescription("Decrease tempo").performClick()
        assertEquals(listOf(1, -1), adjustments)
    }

    @Test
    fun time_signature_choice_reaches_the_callback() {
        show(MetronomeUiState())
        compose.onNodeWithText("3/4").performClick()
        assertEquals(TimeSignature.THREE_FOUR, chosenSignature)
    }

    @Test
    fun hidden_time_signature_shows_no_selection() {
        show(MetronomeUiState(config = MetronomeConfig(timeSignature = TimeSignature.SIX_EIGHT)))
        compose.onNodeWithText("6/8").assertDoesNotExist()
        compose.onNodeWithText("4/4").assertIsDisplayed()
        listOf("2/4", "3/4", "4/4").forEach { compose.onNodeWithText(it).assertIsNotSelected() }

        compose.onNodeWithText("3/4").performClick()
        assertEquals(TimeSignature.THREE_FOUR, chosenSignature)
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
    fun holding_plus_keeps_stepping_and_the_release_adds_nothing() {
        show(MetronomeUiState())
        compose.mainClock.autoAdvance = false
        val plus = compose.onNodeWithContentDescription("Increase tempo")

        plus.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1_200)
        val stepsWhileHeld = adjustments.size
        assertTrue("expected repeated steps while held, got $stepsWhileHeld", stepsWhileHeld in 5..10)

        plus.performTouchInput { up() }
        compose.mainClock.advanceTimeBy(500)
        assertEquals(stepsWhileHeld, adjustments.size)
        assertTrue(adjustments.all { it == 1 })
    }

    @Test
    fun croatian_system_language_shows_croatian_labels() {
        show(MetronomeUiState(), Locale("hr"))
        compose.onNodeWithText("Idemo!").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dobe u taktu").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tvoja razina").assertIsDisplayed()
        compose.onNodeWithText("Početnik").assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Povećaj tempo").assertCountEquals(1)
    }
}
