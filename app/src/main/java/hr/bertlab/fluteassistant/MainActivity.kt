package hr.bertlab.fluteassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import hr.bertlab.fluteassistant.ui.MainScreen
import hr.bertlab.fluteassistant.ui.theme.FluteAssistantTheme

/** Single-activity host. All navigation and state live in Compose and the ViewModels. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
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
