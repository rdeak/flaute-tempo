package hr.bertlab.fluteassistant.ui

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.AppLanguage
import hr.bertlab.fluteassistant.ui.common.KeepScreenOn
import hr.bertlab.fluteassistant.ui.common.LanguageSwitch
import hr.bertlab.fluteassistant.ui.metronome.MetronomeMessage
import hr.bertlab.fluteassistant.ui.metronome.MetronomeScreen
import hr.bertlab.fluteassistant.ui.metronome.MetronomeViewModel
import hr.bertlab.fluteassistant.ui.notes.NotesScreen
import hr.bertlab.fluteassistant.ui.notes.NotesViewModel
import hr.bertlab.fluteassistant.ui.tuner.TunerScreen
import hr.bertlab.fluteassistant.ui.tuner.TunerViewModel
import java.util.Locale

/** The three top-level destinations shown in the bottom navigation bar. */
enum class AppTab(@StringRes val labelRes: Int) {
    METRONOME(R.string.tab_metronome),
    TUNER(R.string.tab_tuner),
    NOTES(R.string.tab_notes);

    val icon: ImageVector
        get() = when (this) {
            METRONOME -> AppIcons.Metronome
            TUNER -> AppIcons.TuningFork
            NOTES -> AppIcons.MusicNote
        }
}

/**
 * Root of the UI: a [Scaffold] with the language switch in the top corner and a bottom
 * [NavigationBar] that switches between the metronome, the tuner and the note reading game.
 * All three ViewModels are scoped to the Activity, so settings and playback survive switching
 * tabs, rotation and the re-creation that a language change triggers. The game's round is the
 * exception: leaving its tab resets the score, so the child always comes back to a fresh start.
 */
@Composable
fun MainScreen(
    metronomeViewModel: MetronomeViewModel = hiltViewModel(),
    tunerViewModel: TunerViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.METRONOME) }
    val metronomeState by metronomeViewModel.state.collectAsStateWithLifecycle()
    val tunerState by tunerViewModel.state.collectAsStateWithLifecycle()
    val notesState by notesViewModel.state.collectAsStateWithLifecycle()

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
        topBar = { LanguageBar() },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            if (selectedTab == AppTab.NOTES && tab != AppTab.NOTES) notesViewModel.resetRound()
                            selectedTab = tab
                        },
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
                AppTab.NOTES -> NotesScreen(state = notesState, viewModel = notesViewModel)
            }
        }
    }
}


/**
 * The top app bar: the app name and, as its action, the in-app language switch. The current
 * language is read from the activity's configuration (which is what the per-app locale
 * actually changed), so anything that is not Croatian shows as English, the fallback.
 * Choosing hands the tag to the platform's per-app language setting: the system on
 * Android 13+, AppCompat's stored backport below that. Either way the activity is re-created
 * in the new language.
 */
@Composable
private fun LanguageBar() {
    val configuration = LocalConfiguration.current
    val current = remember(configuration) {
        AppLanguage.fromLocale(ConfigurationCompat.getLocales(configuration)[0] ?: Locale.getDefault())
    }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge) },
        actions = {
            LanguageSwitch(
                current = current,
                onSelect = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(it.tag)) },
                modifier = Modifier.padding(end = 12.dp),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}
