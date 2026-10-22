package hr.bertlab.flutetempo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import hr.bertlab.flutetempo.ui.MainScreen
import hr.bertlab.flutetempo.ui.theme.FluteTempoTheme

/** Single-activity host. All navigation and state live in Compose and the ViewModels. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FluteTempoTheme {
                MainScreen()
            }
        }
    }
}
