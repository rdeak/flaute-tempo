package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.bertlab.fluteassistant.domain.flute.NoteLength
import hr.bertlab.fluteassistant.domain.flute.TuneNote
import hr.bertlab.fluteassistant.ui.common.LocaleProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class FluteScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: MutableState<FluteUiState>
    private val pressed = mutableListOf<Int>()
    private val released = mutableListOf<Int>()
    private val activated = mutableListOf<Int>()
    private var playToggles = 0
    private var erases = 0

    private fun show(initial: FluteUiState, locale: Locale = Locale.ENGLISH) {
        state = mutableStateOf(initial)
        compose.setContent {
            LocaleProvider(locale) {
                FluteContent(
                    state = state.value,
                    onKeyPressed = { pressed += it },
                    onKeyReleased = { released += it },
                    onKeyActivated = { activated += it },
                    onTogglePlayback = { playToggles++ },
                    onEraseLast = { erases++ },
                )
            }
        }
    }

    @Test
    fun shows_the_keys_in_octave_rows_highest_on_top_with_a_hint_before_the_first_tap() {
        show(FluteUiState())
        compose.onNodeWithText("👆 Tap or hold a note").assertIsDisplayed()
        compose.onNodeWithContentDescription("Flute keys").assertExists()
        val c4 = compose.onNodeWithContentDescription("C4").getBoundsInRoot()
        val c5 = compose.onNodeWithContentDescription("C5").getBoundsInRoot()
        val c6 = compose.onNodeWithContentDescription("C6").getBoundsInRoot()
        val d4 = compose.onNodeWithContentDescription("D4").getBoundsInRoot()
        assertTrue("C6 $c6 should sit above C5 $c5", c6.bottom <= c5.top)
        assertTrue("C5 $c5 should sit above C4 $c4", c5.bottom <= c4.top)
        assertTrue("the octave rows should share a left edge: $c4 $c5 $c6", abs((c4.left - c5.left).value) <= 1f)
        assertTrue("the octave rows should share a left edge: $c4 $c5 $c6", abs((c5.left - c6.left).value) <= 1f)
        assertTrue("D4 $d4 should be right of C4 $c4", d4.left >= c4.right)
        assertTrue("a key should be at least 58 dp tall, was ${c4.height}", c4.height >= 58.dp)
    }

    @Test
    fun pressing_a_key_reports_press_and_release_separately() {
        show(FluteUiState())
        compose.onNodeWithContentDescription("G4").performTouchInput { down(center) }
        assertEquals(listOf(67), pressed)
        assertTrue(released.isEmpty())
        compose.onNodeWithContentDescription("G4").performTouchInput { up() }
        assertEquals(listOf(67), released)
    }

    @Test
    fun the_held_note_is_named_fingered_and_in_the_line() {
        show(
            FluteUiState(
                tune = listOf(TuneNote(67, NoteLength.QUARTER)),
                heldMidi = 67,
                heldIndex = 0,
                shownMidi = 67,
                heldLength = NoteLength.QUARTER,
            ),
        )
        compose.onAllNodesWithText("G4").assertCountEquals(2) // the name and the key
        compose.onNodeWithContentDescription("Fingering for G4").assertExists()
        compose.onNodeWithContentDescription("Your tune, 1 note").assertExists()
        compose.onNode(hasContentDescription("G4, 1 beat") and hasAnyAncestor(hasContentDescription("Your tune, 1 note"))).assertExists()
        compose.onNodeWithContentDescription("Held for 1 beat").assertExists()
        compose.onNodeWithText("👆 Tap or hold a note").assertDoesNotExist()
    }

    @Test
    fun play_and_erase_are_disabled_while_the_tune_is_empty() {
        show(FluteUiState())
        compose.onNodeWithText("Play").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Erase the last note").assertIsNotEnabled()
        compose.onNodeWithText("👆 Tap or hold a note").assertIsDisplayed()
        state.value = state.value.copy(tune = listOf(TuneNote(60, NoteLength.QUARTER)))
        compose.waitForIdle()
        compose.onNodeWithText("Play").assertIsEnabled()
        compose.onNodeWithContentDescription("Erase the last note").assertIsEnabled()
        compose.onNodeWithText("Play").performClick()
        assertEquals(1, playToggles)
        compose.onNodeWithContentDescription("Erase the last note").performClick()
        assertEquals(1, erases)
    }

    @Test
    fun the_erase_button_sinks_while_pressed() {
        show(FluteUiState(tune = listOf(TuneNote(60, NoteLength.QUARTER))))
        val erase = compose.onNodeWithContentDescription("Erase the last note")
        // A pixel on the button's top edge: its ink outline at rest, empty once it sinks.
        fun topPixel(): Color {
            val pixels = erase.captureToImage().toPixelMap()
            return pixels[pixels.width / 2, with(compose.density) { 1.dp.roundToPx() }]
        }
        val idle = topPixel()
        erase.performTouchInput { down(center) }
        assertNotEquals("the button should sink while held", idle, topPixel())
        erase.performTouchInput { up() }
        assertEquals(idle, topPixel())
    }

    @Test
    fun the_pill_reads_stop_while_playing() {
        show(FluteUiState(tune = listOf(TuneNote(60, NoteLength.QUARTER), TuneNote(67, NoteLength.QUARTER)), playingIndex = 1, shownMidi = 67))
        compose.onNodeWithText("Stop").assertIsDisplayed()
        compose.onNodeWithText("Play").assertDoesNotExist()
    }

    @Test
    fun the_line_scrolls_to_the_newest_note() {
        val tune = List(29) { TuneNote(if (it % 2 == 0) 60 else 62, NoteLength.QUARTER) } + TuneNote(84, NoteLength.QUARTER)
        show(FluteUiState(tune = tune, heldMidi = 84, heldIndex = 29, shownMidi = 84))
        compose.onNode(hasContentDescription("C6, 1 beat") and hasAnyAncestor(hasContentDescription("Your tune, 30 notes"))).assertIsDisplayed()
    }

    @Test
    fun the_play_label_fits_a_narrow_phone_in_croatian() {
        state = mutableStateOf(FluteUiState(tune = listOf(TuneNote(60, NoteLength.QUARTER))))
        compose.setContent {
            LocaleProvider(Locale("hr")) {
                Box(Modifier.width(360.dp)) {
                    FluteContent(
                        state = state.value,
                        onKeyPressed = {},
                        onKeyReleased = {},
                        onKeyActivated = {},
                        onTogglePlayback = {},
                        onEraseLast = {},
                    )
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        val node = compose.onNodeWithText("Sviraj", useUnmergedTree = true).fetchSemanticsNode()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        assertFalse("the Play label should shrink to fit, not be cut short", layouts.single().isLineEllipsized(0))
    }

    @Test
    fun croatian_copy() {
        show(FluteUiState(tune = listOf(TuneNote(60, NoteLength.QUARTER), TuneNote(62, NoteLength.QUARTER), TuneNote(64, NoteLength.QUARTER))), Locale("hr"))
        compose.onNodeWithText("Sviraj").assertExists()
        compose.onNodeWithContentDescription("Obriši zadnju notu").assertExists()
        compose.onNodeWithContentDescription("c1").assertExists()
        compose.onNodeWithContentDescription("Tvoja pjesmica, 3 note").assertExists()
        compose.onNodeWithContentDescription("d1, 1 doba").assertExists()
    }

    @Test
    fun each_note_in_the_line_says_how_long_it_is() {
        show(FluteUiState(tune = listOf(TuneNote(60, NoteLength.EIGHTH), TuneNote(62, NoteLength.HALF), TuneNote(64, NoteLength.WHOLE))))
        compose.onNodeWithContentDescription("C4, half a beat").assertExists()
        compose.onNodeWithContentDescription("D4, 2 beats").assertExists()
        compose.onNodeWithContentDescription("E4, 4 beats").assertExists()
    }

    @Test
    fun note_lengths_are_said_in_croatian() {
        show(FluteUiState(tune = listOf(TuneNote(60, NoteLength.EIGHTH), TuneNote(62, NoteLength.DOTTED_HALF))), Locale("hr"))
        compose.onNodeWithContentDescription("c1, pola dobe").assertExists()
        compose.onNodeWithContentDescription("d1, 3 dobe").assertExists()
    }

    @Test
    fun holding_a_key_does_not_move_the_keys() {
        show(FluteUiState(tune = listOf(TuneNote(60, NoteLength.QUARTER)), shownMidi = 60))
        val before = compose.onNodeWithContentDescription("C4").getBoundsInRoot()
        state.value = state.value.copy(heldMidi = 60, heldIndex = 0, heldLength = NoteLength.HALF)
        compose.onNodeWithContentDescription("Held for 2 beats").assertExists()
        assertEquals(before, compose.onNodeWithContentDescription("C4").getBoundsInRoot())
        state.value = FluteUiState()
        assertEquals(before, compose.onNodeWithContentDescription("C4").getBoundsInRoot())
    }
}
