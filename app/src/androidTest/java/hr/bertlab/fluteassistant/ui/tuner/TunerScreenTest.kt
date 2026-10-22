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
                    onReferenceChange = {},
                )
            }
        }
    }

    @Test
    fun asks_for_the_microphone_until_permission_is_granted() {
        show(TunerUiState(permissionGranted = false))
        compose.onNodeWithText("Allow microphone").performClick()
        assertEquals(1, permissionRequests)
    }

    @Test
    fun shows_the_note_its_deviation_and_the_neighbouring_notes() {
        val reading = NoteMath.frequencyToNote(443.0) // A4 played 12 cents sharp
        show(TunerUiState(permissionGranted = true, isListening = true, reading = reading))
        compose.onNodeWithText("A4").assertIsDisplayed()
        compose.onNodeWithText("+12 ¢").assertIsDisplayed()
        compose.onNodeWithText("G♯4").assertIsDisplayed()
        compose.onNodeWithText("A♯4").assertIsDisplayed()
    }

    @Test
    fun croatian_system_language_uses_croatian_note_names() {
        val reading = NoteMath.frequencyToNote(493.88) // B4, called H4 in Croatia
        show(TunerUiState(permissionGranted = true, isListening = true, reading = reading), Locale("hr"))
        compose.onNodeWithText("H4").assertIsDisplayed()
        compose.onNodeWithText("B4").assertIsDisplayed() // the neighbour below (B flat) is "B"
        compose.onNodeWithText("C5").assertIsDisplayed()
        compose.onNodeWithText("Ugođeno").assertIsDisplayed()
    }
}
