package hr.bertlab.fluteassistant.ui.metronome

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.BigActionButton
import hr.bertlab.fluteassistant.ui.common.Stage
import hr.bertlab.fluteassistant.ui.common.fitOrScroll
import hr.bertlab.fluteassistant.ui.theme.ActionGo
import hr.bertlab.fluteassistant.ui.theme.ActionStop

private const val FLASH_DURATION_MS = 220

/**
 * The ring shrinks to fit a small phone; only below this height (split screen, a tiny phone)
 * does the tab scroll instead.
 */
private val MetronomeMinHeight = 420.dp

/** Stateful entry point: wires the ViewModel to the stateless [MetronomeContent]. */
@Composable
fun MetronomeScreen(
    state: MetronomeUiState,
    viewModel: MetronomeViewModel,
    modifier: Modifier = Modifier,
) {
    // Android 13+ only shows the background-playback notification with permission. Ask once,
    // on the first start, when the notification becomes relevant; playback works either way.
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    var notificationPermissionAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isPlaying) {
        if (state.isPlaying && !notificationPermissionAsked && needsNotificationPermission(context)) {
            notificationPermissionAsked = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    MetronomeContent(
        state = state,
        onTogglePlayback = viewModel::togglePlayback,
        onBpmChange = viewModel::setBpm,
        onTimeSignatureChange = viewModel::setTimeSignature,
        modifier = modifier,
    )
}

/**
 * The whole metronome: a stage holding the beat bubbles (with − / + for beats in a bar) and
 * the tempo ring (drag the animal, tap a band, or tap the number for a wheel), then the big
 * play button. Nothing else: a child should never have to read. Loudness is left to the
 * phone's volume buttons.
 */
@Composable
fun MetronomeContent(
    state: MetronomeUiState,
    onTogglePlayback: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One flash animation shared by the bubbles and the knob: snaps to 1 on each beat, fades to 0.
    // It is handed down as a lambda so only the layer phase re-reads it (no per-frame recomposition).
    val flash = remember { Animatable(0f) }
    val flashValue: () -> Float = remember(flash) { { flash.value } }
    LaunchedEffect(state.beatPulse) {
        if (state.beatPulse > 0L && state.isPlaying) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(FLASH_DURATION_MS, easing = LinearOutSlowInEasing))
        }
    }

    // The tempo wheel is open while this is true. A touch that starts anywhere outside the
    // wheel closes it, observed here on the initial pass so the touched control still works.
    var editingTempo by remember { mutableStateOf(false) }
    var wheelCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fitOrScroll(fits = maxHeight >= MetronomeMinHeight, minHeight = MetronomeMinHeight)
                .onGloballyPositioned { rootCoordinates = it }
                .pointerInput(editingTempo) {
                    if (!editingTempo) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val wheel = wheelCoordinates?.takeIf { it.isAttached }
                        val root = rootCoordinates?.takeIf { it.isAttached }
                        val insideWheel = wheel != null && root != null &&
                            root.localBoundingBoxOf(wheel).contains(down.position)
                        if (!insideWheel) editingTempo = false
                    }
                }
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Stage(
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                BeatIndicator(
                    timeSignature = state.config.timeSignature,
                    currentBeat = state.currentBeat,
                    animal = tempoBand(state.config.bpm).emoji,
                    flash = flashValue,
                    onTimeSignatureChange = onTimeSignatureChange,
                )
                TempoDial(
                    bpm = state.config.bpm,
                    onBpmChange = onBpmChange,
                    editing = editingTempo,
                    onEditingChange = { editingTempo = it },
                    onWheelPositioned = { wheelCoordinates = it },
                    flash = flashValue,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 304.dp)
                        .aspectRatio(1f),
                )
            }
            BigActionButton(
                icon = if (state.isPlaying) AppIcons.Stop else AppIcons.Play,
                text = stringResource(if (state.isPlaying) R.string.stop else R.string.play),
                color = if (state.isPlaying) ActionStop else ActionGo,
                onClick = onTogglePlayback,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
