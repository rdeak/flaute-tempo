package hr.bertlab.fluteassistant.ui.tuner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import hr.bertlab.fluteassistant.ui.common.LocaleProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class TunerScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var permissionRequests = 0
    private var chosenNote: Int? = null
    private var chosenMode: TunerMode? = null
    private var playRequests = 0

    private fun show(state: TunerUiState, locale: Locale = Locale.ENGLISH) {
        compose.setContent {
            LocaleProvider(locale) {
                TunerContent(
                    state = state,
                    onTargetNoteChange = { chosenNote = it },
                    onModeChange = { chosenMode = it },
                    onPlayNote = { playRequests++ },
                    onRequestPermission = { permissionRequests++ },
                    onOpenSettings = {},
                )
            }
        }
    }

    /** Listening in [mode] with [targetMidi] chosen and [frequency] heard (null for silence). */
    private fun listening(
        frequency: Double?,
        targetMidi: Int = FluteRange.DEFAULT_TARGET,
        mode: TunerMode = TunerMode.TARGET,
    ) = TunerUiState(
        permissionGranted = true,
        isListening = true,
        reading = frequency?.let { NoteMath.frequencyToNote(it) },
        targetMidi = targetMidi,
        mode = mode,
    )

    private fun identifying(frequency: Double?) = listening(frequency, mode = TunerMode.IDENTIFY)

    @Test
    fun identify_mode_names_the_heard_note_without_the_wheel() {
        show(identifying(443.0)) // A4 played 12 cents sharp
        compose.onNodeWithText("A4").assertIsDisplayed()
        compose.onNodeWithText("G♯4").assertDoesNotExist()
        compose.onNodeWithText("⬆️ Too high").assertIsDisplayed()
    }

    @Test
    fun identify_mode_measures_from_the_nearest_note() {
        show(listening(440.0, targetMidi = 67, mode = TunerMode.IDENTIFY)) // the chosen G4 is ignored
        compose.onNodeWithText("🎉 Perfect!").assertIsDisplayed()
    }

    @Test
    fun identify_mode_silence_asks_for_any_note() {
        show(identifying(null))
        compose.onNodeWithText("…").assertIsDisplayed()
        compose.onNodeWithText("👂 Play a note").assertIsDisplayed()
    }

    @Test
    fun tapping_a_mode_pill_reports_the_mode() {
        show(identifying(null))
        compose.onNodeWithText("🎯 Tune to a note").performClick()
        assertEquals(TunerMode.TARGET, chosenMode)
    }

    @Test
    fun croatian_identify_mode_uses_the_school_names() {
        show(identifying(493.88), Locale("hr")) // B4 is h1 in Croatia
        compose.onNodeWithText("h1").assertIsDisplayed()
        compose.onNodeWithText("🔍 Koji je ton?").assertIsDisplayed()
    }

    @Test
    fun asks_for_the_microphone_until_permission_is_granted() {
        show(TunerUiState(permissionGranted = false))
        compose.onNodeWithText("Yes, allow").performClick()
        assertEquals(1, permissionRequests)
    }

    @Test
    fun shows_the_target_note_with_its_neighbours_and_too_high() {
        show(listening(443.0)) // A4 chosen and played 12 cents sharp
        compose.onNodeWithText("A4").assertIsDisplayed()
        compose.onNodeWithText("G♯4").assertIsDisplayed()
        compose.onNodeWithText("A♯4").assertIsDisplayed()
        compose.onNodeWithText("⬆️ Too high").assertIsDisplayed()
        compose.onNodeWithText("+12 ¢").assertDoesNotExist()
    }

    @Test
    fun measures_against_the_chosen_note() {
        show(listening(440.0, targetMidi = 67)) // a perfect A4, but G4 was chosen
        compose.onNodeWithText("G4").assertIsDisplayed()
        compose.onNodeWithText("⬆️ Too high, that's A4").assertIsDisplayed()
    }

    @Test
    fun in_tune_with_the_chosen_note_celebrates() {
        show(listening(440.0, targetMidi = 69))
        compose.onNodeWithText("🎉 Perfect!").assertIsDisplayed()
    }

    @Test
    fun a_lower_note_than_the_chosen_one_is_named_with_the_direction() {
        show(listening(329.63)) // E4 played, A4 chosen
        compose.onNodeWithText("⬇️ Too low, that's E4").assertIsDisplayed()
    }

    @Test
    fun a_higher_note_than_the_chosen_one_is_named_with_the_direction() {
        show(listening(493.88)) // B4 played, A4 chosen
        compose.onNodeWithText("⬆️ Too high, that's B4").assertIsDisplayed()
    }

    @Test
    fun the_octave_above_the_chosen_note_asks_for_gentler_air() {
        show(listening(880.0)) // A5 played, A4 chosen: overblown
        compose.onNodeWithText("⬆️ An octave too high: gentler air").assertIsDisplayed()
    }

    @Test
    fun the_octave_below_the_chosen_note_asks_for_faster_air() {
        show(listening(261.63, targetMidi = 72)) // C4 played, C5 chosen: underblown
        compose.onNodeWithText("⬇️ An octave too low: faster air").assertIsDisplayed()
    }

    @Test
    fun a_different_note_is_named_in_croatian_with_its_croatian_name() {
        show(listening(493.88), locale = Locale.forLanguageTag("hr")) // B4 is h1
        compose.onNodeWithText("⬆️ Previsoko, to je h1").assertIsDisplayed()
    }

    @Test
    fun silence_names_the_note_to_play() {
        show(listening(null))
        compose.onNodeWithText("👂 Play A4").assertIsDisplayed()
    }

    @Test
    fun scrolling_the_wheel_reports_the_new_note() {
        show(listening(null))
        compose.onNodeWithContentDescription("Note to tune, scroll to choose another")
            .performScrollToIndex(72 - FluteRange.LOWEST_MIDI)
        compose.waitForIdle()
        assertEquals(72, chosenNote)
    }

    @Test
    fun croatian_names_the_target_note_the_school_way() {
        show(listening(493.88, targetMidi = 71), Locale("hr")) // B4, called h1 in Croatia
        compose.onNodeWithText("h1").assertIsDisplayed()
        compose.onNodeWithText("🎉 Savršeno!").assertIsDisplayed()
    }

    @Test
    fun croatian_silence_names_the_note_to_play() {
        show(listening(null, targetMidi = 69), Locale("hr"))
        compose.onNodeWithText("👂 Odsviraj a1").assertIsDisplayed()
    }

    @Test
    fun target_mode_offers_to_hear_the_note_and_shows_its_fingering() {
        show(listening(null))
        compose.onNodeWithContentDescription("Hear the note").performClick()
        assertEquals(1, playRequests)
        compose.onNodeWithContentDescription("Fingering for A4").assertExists()
    }

    @Test
    fun identify_mode_shows_the_heard_notes_fingering_and_no_play_button() {
        show(identifying(392.0)) // G4
        compose.onNodeWithContentDescription("Hear the note").assertDoesNotExist()
        compose.onNodeWithContentDescription("Fingering for G4").assertExists()
    }

    @Test
    fun while_the_note_plays_the_status_says_listen() {
        show(listening(null).copy(isPlayingReference = true))
        compose.onNodeWithText("🔊 Listen").assertIsDisplayed()
    }
}
