package hr.bertlab.fluteassistant.ui.tuner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
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

    private fun show(state: TunerUiState, locale: Locale = Locale.ENGLISH) {
        compose.setContent {
            LocaleProvider(locale) {
                TunerContent(
                    state = state,
                    onRequestPermission = { permissionRequests++ },
                    onOpenSettings = {},
                )
            }
        }
    }

    private fun listening(frequency: Double) =
        TunerUiState(permissionGranted = true, isListening = true, reading = NoteMath.frequencyToNote(frequency))

    @Test
    fun asks_for_the_microphone_until_permission_is_granted() {
        show(TunerUiState(permissionGranted = false))
        compose.onNodeWithText("Yes, allow").performClick()
        assertEquals(1, permissionRequests)
    }

    @Test
    fun shows_the_note_and_too_high() {
        show(listening(443.0)) // A4 played 12 cents sharp
        compose.onNodeWithText("A").assertIsDisplayed()
        compose.onNodeWithText("⬆️ Too high").assertIsDisplayed()
        compose.onNodeWithText("+12 ¢").assertDoesNotExist()
        compose.onNodeWithText("A4").assertDoesNotExist()
    }

    @Test
    fun far_off_note_shows_too_low() {
        show(listening(430.0)) // A4, about 40 cents flat
        compose.onNodeWithText("A").assertIsDisplayed()
        compose.onNodeWithText("⬇️ Too low").assertIsDisplayed()
    }

    @Test
    fun in_tune_note_celebrates() {
        show(listening(440.0))
        compose.onNodeWithText("🎉 Perfect!").assertIsDisplayed()
    }

    @Test
    fun silence_asks_for_a_note() {
        show(TunerUiState(permissionGranted = true, isListening = true, reading = null))
        compose.onNodeWithText("👂 Play a note").assertIsDisplayed()
    }

    @Test
    fun croatian_system_language_uses_croatian_note_names() {
        show(listening(493.88), Locale("hr")) // B4, called H in Croatia
        compose.onNodeWithText("H").assertIsDisplayed()
        compose.onNodeWithText("🎉 Savršeno!").assertIsDisplayed()
    }
}
