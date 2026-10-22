package hr.bertlab.fluteassistant

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import hr.bertlab.fluteassistant.ui.MainScreen
import hr.bertlab.fluteassistant.ui.theme.FluteAssistantTheme

/**
 * Single-activity host. All navigation and state live in Compose and the ViewModels.
 *
 * An [AppCompatActivity] only so that the in-app language switch
 * ([androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]) works below Android 13;
 * the activity is re-created when the language changes and the ViewModels survive that.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluteAssistantTheme {
                MainScreen()
            }
        }
    }
}
