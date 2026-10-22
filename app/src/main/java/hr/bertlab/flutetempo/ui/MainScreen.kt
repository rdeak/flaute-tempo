package hr.bertlab.flutetempo.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.bertlab.flutetempo.R
import hr.bertlab.flutetempo.ui.common.AppIcons
import hr.bertlab.flutetempo.ui.common.KeepScreenOn
import hr.bertlab.flutetempo.ui.metronome.MetronomeMessage
import hr.bertlab.flutetempo.ui.metronome.MetronomeScreen
import hr.bertlab.flutetempo.ui.metronome.MetronomeViewModel
import hr.bertlab.flutetempo.ui.tuner.TunerScreen
import hr.bertlab.flutetempo.ui.tuner.TunerViewModel

/** The two top-level destinations shown in the bottom navigation bar. */
enum class AppTab(@StringRes val labelRes: Int) {
    METRONOME(R.string.tab_metronome),
    TUNER(R.string.tab_tuner);

    val icon: ImageVector
        get() = when (this) {
            METRONOME -> AppIcons.Metronome
            TUNER -> AppIcons.TuningFork
        }
}

/**
 * Root of the UI: a [Scaffold] with a bottom [NavigationBar] that switches between the
 * metronome and the tuner. Both ViewModels are scoped to the Activity, so settings and
 * playback survive switching tabs and rotation.
 */
@Composable
fun MainScreen(
    metronomeViewModel: MetronomeViewModel = hiltViewModel(),
    tunerViewModel: TunerViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.METRONOME) }
    val metronomeState by metronomeViewModel.state.collectAsStateWithLifecycle()
    val tunerState by tunerViewModel.state.collectAsStateWithLifecycle()

    KeepScreenOn(enabled = metronomeState.isPlaying || tunerState.isListening)

    // One-shot messages (audio unavailable, persistence failures) as a snackbar.
    val snackbarHostState = remember { SnackbarHostState() }
    val message = metronomeState.userMessage
    val messageText = message?.let {
        stringResource(
            when (it) {
                MetronomeMessage.AUDIO_UNAVAILABLE -> R.string.msg_audio_unavailable
                MetronomeMessage.SAVE_FAILED -> R.string.msg_save_failed
                MetronomeMessage.DELETE_FAILED -> R.string.msg_delete_failed
            },
        )
    }
    LaunchedEffect(message) {
        if (message != null && messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            metronomeViewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selectedTab) {
                AppTab.METRONOME -> MetronomeScreen(state = metronomeState, viewModel = metronomeViewModel)
                AppTab.TUNER -> TunerScreen(state = tunerState, viewModel = tunerViewModel)
            }
        }
    }
}
