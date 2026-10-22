package hr.bertlab.fluteassistant.ui.tuner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.tuner.FluteFingerings
import hr.bertlab.fluteassistant.domain.tuner.TunerMode
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.labelRes
import hr.bertlab.fluteassistant.ui.common.noteLabel
import hr.bertlab.fluteassistant.ui.theme.InTuneGreen
import hr.bertlab.fluteassistant.ui.theme.InTuneGreenText
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRed
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRedText
import hr.bertlab.fluteassistant.ui.theme.SlightlyOffAmber
import hr.bertlab.fluteassistant.ui.theme.SlightlyOffAmberText
import kotlin.math.abs

private const val IN_TUNE_CENTS = 5.0
private const val SLIGHTLY_OFF_CENTS = 15.0

/**
 * Stateful entry point: handles the RECORD_AUDIO permission and starts / stops the microphone
 * with the screen's lifecycle, then renders the stateless [TunerContent].
 */
@Composable
fun TunerScreen(
    state: TunerUiState,
    viewModel: TunerViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    // Check the permission whenever the screen appears; ask the system only once automatically
    // (the card below offers a manual retry).
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
        if (!granted && !state.permissionRequested) {
            viewModel.onPermissionRequested()
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Only hold the microphone while this screen is started (visible). Re-checking the
    // permission here picks up a grant made in system Settings while the app was stopped.
    LifecycleStartEffect(state.permissionGranted) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted != state.permissionGranted) viewModel.onPermissionResult(granted)
        if (granted) viewModel.startListening()
        onStopOrDispose { viewModel.stopListening() }
    }

    TunerContent(
        state = state,
        onTargetNoteChange = viewModel::setTargetNote,
        onModeChange = viewModel::setMode,
        onPlayNote = viewModel::playTargetNote,
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
        modifier = modifier,
    )
}

/**
 * The whole tuner: either the name of the note being played ([TunerMode.IDENTIFY]) or the
 * wheel with the note the child chose to play ([TunerMode.TARGET], with a speaker button to
 * hear it), that note's fingering, the gauge measuring against it, one friendly status line,
 * and the two mode pills at the bottom. No numbers beyond the octave in the note name: a
 * child only needs to know whether to go up or down.
 */
@Composable
fun TunerContent(
    state: TunerUiState,
    onTargetNoteChange: (Int) -> Unit,
    onModeChange: (TunerMode) -> Unit,
    onPlayNote: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(24.dp))

        if (!state.permissionGranted) {
            PermissionCard(onRequestPermission = onRequestPermission, onOpenSettings = onOpenSettings)
        } else {
            val cents = state.gaugeCents
            val status = when {
                cents == null -> TuningStatus.NONE
                abs(cents) <= IN_TUNE_CENTS -> TuningStatus.IN_TUNE
                cents < 0 -> TuningStatus.LOW
                else -> TuningStatus.HIGH
            }
            // The gauge keeps the bright zone colours; text uses darker ones on the light
            // surface so "Perfect!" stays readable.
            val dark = isSystemInDarkTheme()
            val inTune = if (dark) InTuneGreen else InTuneGreenText
            val slightlyOff = if (dark) SlightlyOffAmber else SlightlyOffAmberText
            val farOff = if (dark) OutOfTuneRed else OutOfTuneRedText
            val statusColor = when (status) {
                TuningStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                TuningStatus.IN_TUNE -> inTune
                TuningStatus.LOW, TuningStatus.HIGH ->
                    if (abs(cents ?: 0.0) <= SLIGHTLY_OFF_CENTS) slightlyOff else farOff
            }

            // Same height in both modes so the gauge does not jump when switching.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(NoteWheelHeight),
                contentAlignment = Alignment.Center,
            ) {
                when (state.mode) {
                    TunerMode.TARGET -> NoteWheel(
                        targetMidi = state.targetMidi,
                        onTargetChange = onTargetNoteChange,
                        inTune = status == TuningStatus.IN_TUNE,
                        onPlay = onPlayNote,
                        isPlaying = state.isPlayingReference,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TunerMode.IDENTIFY -> Text(
                        text = state.reading?.let { noteLabel(it.midi) } ?: "…",
                        style = MaterialTheme.typography.displayLarge,
                        color = when (status) {
                            TuningStatus.IN_TUNE -> inTune
                            TuningStatus.NONE -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }

            // Which keys to press: for the chosen note, or for whatever is being played.
            val fingeredMidi = when (state.mode) {
                TunerMode.TARGET -> state.targetMidi
                TunerMode.IDENTIFY -> state.reading?.midi
            }
            Spacer(Modifier.height(8.dp))
            FingeringDiagram(
                keys = fingeredMidi?.let(FluteFingerings::keysFor),
                noteLabel = fingeredMidi?.let { noteLabel(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            )

            Spacer(Modifier.height(8.dp))
            TuningGauge(
                cents = cents,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.9f),
            )

            Spacer(Modifier.height(20.dp))
            StatusLine(
                status = status,
                color = statusColor,
                targetNote = noteLabel(state.targetMidi).takeIf { state.mode == TunerMode.TARGET },
                playing = state.isPlayingReference,
            )

            state.errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(32.dp))
            TunerModeChoice(mode = state.mode, onSelect = onModeChange)
        }
        Spacer(Modifier.height(24.dp))
    }
}

private enum class TuningStatus { NONE, IN_TUNE, LOW, HIGH }

private val TunerMode.emoji: String
    get() = when (this) {
        TunerMode.IDENTIFY -> "🔍"
        TunerMode.TARGET -> "🎯"
    }

/**
 * Two pills at the bottom of the tuner, an emoji and a few words each: "Which note?" names
 * whatever is played, "Tune to a note" measures against the note in the wheel. The chosen
 * one is filled.
 */
@Composable
private fun TunerModeChoice(mode: TunerMode, onSelect: (TunerMode) -> Unit) {
    val groupLabel = stringResource(R.string.tuner_mode)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TunerMode.entries.forEach { entry ->
            val isSelected = entry == mode
            Surface(
                selected = isSelected,
                onClick = { onSelect(entry) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "${entry.emoji} ${stringResource(entry.labelRes())}",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * One line of feedback; while nothing is heard it names [targetNote] when there is one, or
 * asks for any note, and while the reference note is [playing] it says to listen. It bounces
 * in when the note comes into tune.
 */
@Composable
private fun StatusLine(status: TuningStatus, color: Color, targetNote: String?, playing: Boolean) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(status == TuningStatus.IN_TUNE) {
        if (status == TuningStatus.IN_TUNE) {
            scale.snapTo(1.3f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        }
    }
    Text(
        text = when (status) {
            TuningStatus.NONE -> when {
                playing -> stringResource(R.string.listen_status)
                targetNote != null -> stringResource(R.string.play_note_prompt, targetNote)
                else -> stringResource(R.string.play_a_note)
            }
            TuningStatus.IN_TUNE -> stringResource(R.string.in_tune)
            TuningStatus.LOW -> stringResource(R.string.too_low)
            TuningStatus.HIGH -> stringResource(R.string.too_high)
        },
        style = MaterialTheme.typography.titleLarge,
        color = color,
        modifier = Modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
    )
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit, onOpenSettings: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                AppIcons.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.mic_permission_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.mic_permission_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRequestPermission,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Text(stringResource(R.string.allow_microphone), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
        }
    }
}
