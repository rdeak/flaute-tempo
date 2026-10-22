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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
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
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.BeatStrength
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.ui.common.AppIcons

private const val FLASH_DURATION_MS = 180

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
        onVolumeChange = viewModel::setVolume,
        modifier = modifier,
    )
}

/**
 * The whole metronome, top to bottom: beat dots, the tempo ring (tap the number for a
 * wheel), the level pills, the beats-in-a-bar stepper, the play button and the volume
 * slider. Nothing else: a child should never have to read.
 */
@Composable
fun MetronomeContent(
    state: MetronomeUiState,
    onTogglePlayback: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One flash animation shared by the dots and the ring: snaps to 1 on each beat, fades to 0.
    // It is handed down as a lambda so only the draw phase re-reads it (no per-frame recomposition).
    val flash = remember { Animatable(0f) }
    val flashValue: () -> Float = remember(flash) { { flash.value } }
    LaunchedEffect(state.beatPulse) {
        if (state.beatPulse > 0L && state.isPlaying) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(FLASH_DURATION_MS, easing = LinearOutSlowInEasing))
        }
    }
    val flashColor = when (state.currentStrength) {
        BeatStrength.STRONG -> MaterialTheme.colorScheme.tertiary
        BeatStrength.MEDIUM -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    // The tempo wheel is open while this is true. A touch that starts anywhere outside the
    // wheel closes it, observed here on the initial pass so the touched control still works.
    var editingTempo by remember { mutableStateOf(false) }
    var wheelCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        BeatIndicator(
            timeSignature = state.config.timeSignature,
            currentBeat = state.currentBeat,
            flash = flashValue,
            flashColor = flashColor,
        )

        Spacer(Modifier.height(20.dp))
        TempoDial(
            bpm = state.config.bpm,
            onBpmChange = onBpmChange,
            editing = editingTempo,
            onEditingChange = { editingTempo = it },
            onWheelPositioned = { wheelCoordinates = it },
            flash = flashValue,
            flashColor = flashColor,
            modifier = Modifier.size(250.dp),
        )

        Spacer(Modifier.height(16.dp))
        SkillLevelChoice(bpm = state.config.bpm, onSelect = { onBpmChange(it.bpm) })

        Spacer(Modifier.height(16.dp))
        TimeSignatureStepper(selected = state.config.timeSignature, onSelect = onTimeSignatureChange)

        Spacer(Modifier.height(20.dp))
        PlayButton(isPlaying = state.isPlaying, onClick = onTogglePlayback)

        Spacer(Modifier.height(20.dp))
        VolumeRow(volume = state.volume, onVolumeChange = onVolumeChange)

        Spacer(Modifier.height(24.dp))
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

@Composable
private fun PlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = if (isPlaying) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
    ) {
        Icon(
            imageVector = if (isPlaying) AppIcons.Stop else AppIcons.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(if (isPlaying) R.string.stop else R.string.play),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/**
 * One tall pill per [SkillLevel], an emoji over a word and no number: a tap jumps straight to
 * that practice tempo. The pill whose tempo is exactly the current one is filled, so a child
 * sees where they are on the ladder; nudge in between with the ring or wheel and none is filled.
 */
@Composable
private fun SkillLevelChoice(bpm: Int, onSelect: (SkillLevel) -> Unit) {
    val groupLabel = stringResource(R.string.skill_level)
    val current = skillLevelFor(bpm)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SkillLevel.entries.forEach { level ->
            val isSelected = level == current
            Surface(
                selected = isSelected,
                onClick = { onSelect(level) },
                shape = MaterialTheme.shapes.large,
                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(level.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(level.labelRes),
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
 * One pill: an arrow, the current signature written the way it is in sheet music, an arrow.
 * The arrows walk [TimeSignature]'s display order and wrap at the ends, so every signature
 * is a few taps away and the beat dots above show the result at once.
 */
@Composable
private fun TimeSignatureStepper(selected: TimeSignature, onSelect: (TimeSignature) -> Unit) {
    val groupLabel = stringResource(R.string.time_signature)
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics { contentDescription = groupLabel },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(AppIcons.ChevronLeft, stringResource(R.string.previous_time_signature)) {
                onSelect(selected.previous())
            }
            Text(
                text = selected.label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            StepButton(AppIcons.ChevronRight, stringResource(R.string.next_time_signature)) {
                onSelect(selected.next())
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(64.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(36.dp))
    }
}

/** A quiet speaker, a slider and a loud speaker. No number: the icons say what it does. */
@Composable
private fun VolumeRow(volume: Float, onVolumeChange: (Float) -> Unit) {
    val label = stringResource(R.string.volume)
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.VolumeMute, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = label },
        )
        Icon(AppIcons.VolumeUp, contentDescription = null, tint = iconTint, modifier = Modifier.size(30.dp))
    }
}
