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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.BeatStrength
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.ui.common.AppIcons
import kotlinx.coroutines.delay

private const val FLASH_DURATION_MS = 180

/**
 * Holding − or + starts stepping after this delay, then once per [HOLD_REPEAT_MS]. Inside a
 * scrolling column Compose reports the press about 100 ms late (the tap timeout, so a scroll
 * can still claim the gesture), so the first step lands roughly half a second after touch.
 */
private const val HOLD_DELAY_MS = 400L
private const val HOLD_REPEAT_MS = 80L

/** The time signatures a young student meets; the others stay in the engine but are not offered. */
private val OfferedTimeSignatures = listOf(TimeSignature.TWO_FOUR, TimeSignature.THREE_FOUR, TimeSignature.FOUR_FOUR)

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
        onAdjustBpm = viewModel::adjustBpm,
        onTimeSignatureChange = viewModel::setTimeSignature,
        modifier = modifier,
    )
}

/**
 * The whole metronome, top to bottom: beat dots, the tempo ring, − / + buttons, the level
 * pills, the play button and the beats-in-a-bar choice. Nothing else: a child should never
 * have to read.
 */
@Composable
fun MetronomeContent(
    state: MetronomeUiState,
    onTogglePlayback: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onAdjustBpm: (Int) -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
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
            flash = flashValue,
            flashColor = flashColor,
            modifier = Modifier.size(250.dp),
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            HoldToRepeatButton(AppIcons.Remove, stringResource(R.string.decrease_tempo)) { onAdjustBpm(-1) }
            HoldToRepeatButton(AppIcons.Add, stringResource(R.string.increase_tempo)) { onAdjustBpm(+1) }
        }

        Spacer(Modifier.height(16.dp))
        SkillLevelChoice(bpm = state.config.bpm, onSelect = { onBpmChange(it.bpm) })

        Spacer(Modifier.height(20.dp))
        PlayButton(isPlaying = state.isPlaying, onClick = onTogglePlayback)

        Spacer(Modifier.height(24.dp))
        TimeSignatureChoice(selected = state.config.timeSignature, onSelect = onTimeSignatureChange)

        Spacer(Modifier.height(24.dp))
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

/** A big round button: a tap is one [onStep]; holding it keeps stepping. */
@Composable
private fun HoldToRepeatButton(
    icon: ImageVector,
    description: String,
    onStep: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)
    // True once the hold has started stepping, so the click on release does not add a step.
    var repeating by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (!pressed) {
            repeating = false
            return@LaunchedEffect
        }
        delay(HOLD_DELAY_MS)
        repeating = true
        while (true) {
            currentOnStep()
            delay(HOLD_REPEAT_MS)
        }
    }

    FilledTonalIconButton(
        onClick = { if (!repeating) onStep() },
        interactionSource = interactionSource,
        modifier = Modifier
            .size(72.dp)
            .semantics { contentDescription = description },
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(36.dp))
    }
}

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
 * sees where they are on the ladder; nudge in between with − / + and none is filled.
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

/** Three round toggles labelled the way the time signature is written in sheet music. */
@Composable
private fun TimeSignatureChoice(selected: TimeSignature, onSelect: (TimeSignature) -> Unit) {
    val groupLabel = stringResource(R.string.time_signature)
    Row(
        modifier = Modifier
            .selectableGroup()
            .semantics { contentDescription = groupLabel },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        OfferedTimeSignatures.forEach { signature ->
            val isSelected = signature == selected
            Surface(
                selected = isSelected,
                onClick = { onSelect(signature) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(68.dp)
                    .semantics { role = Role.RadioButton },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(signature.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
