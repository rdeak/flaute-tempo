package hr.bertlab.fluteassistant.ui.notes

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import hr.bertlab.fluteassistant.ui.common.LocaleProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class NotesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: MutableState<NotesUiState>
    private val tapped = mutableListOf<Int>()
    private val toggled = mutableListOf<Int>()
    private var playAgainRequests = 0
    private var soundToggles = 0

    private fun show(initial: NotesUiState, locale: Locale = Locale.ENGLISH) {
        state = mutableStateOf(initial)
        compose.setContent {
            LocaleProvider(locale) {
                NotesContent(
                    state = state.value,
                    onNoteTapped = { tapped += it },
                    onPlayAgain = { playAgainRequests++ },
                    onTogglePracticeNote = { toggled += it },
                    onToggleSound = { soundToggles++ },
                )
            }
        }
    }

    /** A round in progress asking [midi], with the default notes chosen. */
    private fun asking(midi: Int = 60) = NotesUiState(practiceNotes = StaffNotes.DEFAULT_PRACTICE, currentMidi = midi)

        @Test
    fun shows_the_note_name_as_the_whole_question() {
        show(asking(60))
        compose.onNodeWithText("C4").assertIsDisplayed()
        compose.onNodeWithText("👆 Where is C4?").assertDoesNotExist() // the name says it already
        compose.onNodeWithText("🎉 That's it!").assertDoesNotExist()
        compose.onNodeWithContentDescription("Staff, tap where the note goes").assertExists()
        compose.onNodeWithContentDescription("Round progress, 0 of 10 notes").assertExists()
    }

    @Test
    fun the_choose_button_sits_beside_the_note_name() {
        show(asking(60))
        val name = compose.onNodeWithText("C4").getBoundsInRoot()
        val button = compose.onNodeWithContentDescription("Choose notes").getBoundsInRoot()
        assertTrue("button $button should be right of the name $name", button.left >= name.right)
        val nameMiddle = (name.top + name.bottom) / 2
        assertTrue("button $button should share the name's row $name", button.top <= nameMiddle && button.bottom >= nameMiddle)
        compose.onNodeWithText("🎵 Choose notes").assertDoesNotExist() // the old bottom pill is gone
    }

    @Test
    fun the_round_dots_sit_below_the_staff_and_its_feedback() {
        show(asking(60).copy(wrongTaps = setOf(62), taps = 1))
        val staff = compose.onNodeWithContentDescription("Staff, tap where the note goes").getBoundsInRoot()
        val feedback = compose.onNodeWithText("🙈 Not there, try again").getBoundsInRoot()
        val dots = compose.onNodeWithContentDescription("Round progress, 0 of 10 notes").getBoundsInRoot()
        assertTrue("feedback $feedback should be under the staff $staff", feedback.top >= staff.bottom)
        assertTrue("dots $dots should be under the feedback $feedback", dots.top >= feedback.bottom)
    }

    @Test
    fun tapping_a_staff_position_reports_its_note() {
        show(asking(60).copy(practiceNotes = StaffNotes.DEFAULT_PRACTICE + 84))
        compose.onNodeWithContentDescription("Staff position 1").performClick()
        compose.onNodeWithContentDescription("Staff position 15").performClick()
        assertEquals(listOf(60, 84), tapped)
    }

    @Test
    fun ledger_places_outside_the_staff_appear_only_for_chosen_notes() {
        show(asking(62).copy(practiceNotes = setOf(62, 64, 65))) // nothing below or above the staff
        compose.onNodeWithContentDescription("Staff position 1").assertDoesNotExist() // c1
        compose.onNodeWithContentDescription("Staff position 2").assertExists() // d1, the space under the staff
        compose.onNodeWithContentDescription("Staff position 12").assertExists() // g2, the space above it
        compose.onNodeWithContentDescription("Staff position 13").assertDoesNotExist() // a2

        compose.runOnIdle { state.value = state.value.copy(practiceNotes = setOf(62, 84)) } // c3 chosen
        compose.onNodeWithContentDescription("Staff position 13").assertExists()
        compose.onNodeWithContentDescription("Staff position 15").assertExists()
        compose.onNodeWithContentDescription("Staff position 1").assertDoesNotExist()
    }

    @Test
    fun the_staff_takes_the_height_left_on_the_screen() {
        show(asking(60))
        val staff = compose.onNodeWithContentDescription("Staff, tap where the note goes").getBoundsInRoot()
        val dots = compose.onNodeWithContentDescription("Round progress, 0 of 10 notes").getBoundsInRoot()
        assertTrue("staff $staff should be far taller than a fixed 390 dp", staff.height >= 450.dp)
        assertTrue("dots $dots should sit at the bottom, under the staff $staff", dots.top >= staff.bottom)
    }

    @Test
    fun places_stack_in_one_column_with_room_between_them() {
        show(asking(60))
        val c4 = compose.onNodeWithContentDescription("Staff position 1").getBoundsInRoot()
        val d4 = compose.onNodeWithContentDescription("Staff position 2").getBoundsInRoot()
        val g5 = compose.onNodeWithContentDescription("Staff position 12").getBoundsInRoot()
        // One column: every place spans the same width, the lower note below the higher one.
        assertEquals(c4.left, d4.left)
        assertEquals(c4.right, d4.right)
        assertTrue("d4 $d4 should sit above c4 $c4", d4.bottom <= c4.top)
        assertTrue("g5 $g5 should sit above d4 $d4", g5.bottom <= d4.top)
        // Each place is at least 30 dp tall and the full width, so a finger lands on one place.
        listOf(c4, d4, g5).forEach { assertTrue("target $it too small", it.height >= 30.dp && it.width >= 300.dp) }
    }

    @Test
    fun a_right_tap_says_so() {
        show(asking(60).copy(solved = true, results = listOf(true), taps = 1, streak = 1))
        compose.onNodeWithText("🎉 That's it!").assertIsDisplayed()
    }

    @Test
    fun a_wrong_tap_says_try_again() {
        show(asking(60).copy(wrongTaps = setOf(62), taps = 1))
        compose.onNodeWithText("🙈 Not there, try again").assertIsDisplayed()
        compose.onNodeWithText("C4").assertIsDisplayed() // the question stays
    }

    @Test
    fun the_summary_shows_the_numbers_and_plays_again() {
        show(
            asking(60).copy(
                roundOver = true,
                results = List(8) { true } + List(2) { false },
                taps = 13,
                bestStreak = 5,
                roundMillis = 83_000L,
            ),
        )
        compose.onNodeWithText("🌟 Great job!").assertIsDisplayed()
        compose.onNodeWithText("⏱️ Time: 1:23").assertIsDisplayed()
        compose.onNodeWithText("8 / 10").assertIsDisplayed()
        compose.onNodeWithText("🔥 Best streak: 5").assertIsDisplayed()
        compose.onNodeWithText("🎯 Accuracy: 77 %").assertIsDisplayed()
                compose.onNodeWithContentDescription("Staff, tap where the note goes").assertDoesNotExist()
        compose.onNodeWithText("Play again").performClick()
        assertEquals(1, playAgainRequests)
    }

    @Test
    fun choosing_notes_toggles_them() {
        show(asking(60))
        compose.onNodeWithContentDescription("Choose notes").performClick()
        compose.onNodeWithText("Which notes?").assertIsDisplayed()
        compose.onNodeWithText("C4").assertIsSelected()
        compose.onNodeWithText("C6").assertIsNotSelected()
        compose.onNodeWithText("C6").performClick()
        assertEquals(listOf(84), toggled)
        compose.onNodeWithContentDescription("Done").performClick()
        compose.onNodeWithContentDescription("Staff, tap where the note goes").assertExists()
    }

    @Test
    fun the_sound_toggle_lives_with_the_note_picker() {
        show(asking(60))
        compose.onNodeWithText("🔊 Hear the note").assertDoesNotExist() // not on the game screen itself
        compose.onNodeWithContentDescription("Choose notes").performClick()
        compose.onNodeWithText("🔊 Hear the note").assertIsOn()
        compose.onNodeWithText("🔊 Hear the note").performClick()
        assertEquals(1, soundToggles)

        compose.runOnIdle { state.value = state.value.copy(soundOn = false) }
        compose.onNodeWithText("🔇 Hear the note").assertIsOff()
    }

    @Test
    fun streak_badge_appears_from_two() {
        show(asking(60).copy(streak = 3, results = listOf(true, true, true), taps = 3))
        compose.onNodeWithText("🔥 3").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(streak = 1) }
        compose.onNodeWithText("🔥 1").assertDoesNotExist()
    }

    @Test
    fun croatian_names_and_copy() {
        show(asking(60), Locale("hr"))
                compose.onNodeWithText("c1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Odaberi note").assertExists()
        compose.onNodeWithContentDescription("Mjesto 1 u crtovlju").assertExists()
        compose.onNodeWithContentDescription("Odaberi note").performClick()
        compose.onNodeWithText("Koje note?").assertIsDisplayed()
        compose.onNodeWithText("🔊 Poslušaj ton").assertIsOn()
        compose.onNodeWithContentDescription("Gotovo").assertExists()
    }
}
