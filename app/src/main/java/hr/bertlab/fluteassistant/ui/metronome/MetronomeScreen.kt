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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.BeatStrength
import hr.bertlab.fluteassistant.domain.model.SoundProfile
import hr.bertlab.fluteassistant.domain.model.Subdivision
import hr.bertlab.fluteassistant.domain.model.TimeSignature
import hr.bertlab.fluteassistant.ui.common.AppIcons
import kotlin.math.roundToInt

private const val FLASH_DURATION_MS = 180

/** Stateful entry point: wires the ViewModel to the stateless [MetronomeContent]. */
@Composable
fun MetronomeScreen(
    state: MetronomeUiState,
    viewModel: MetronomeViewModel,
    modifier: Modifier = Modifier,
) {
    var showPresets by rememberSaveable { mutableStateOf(false) }

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
        onTapTempo = viewModel::tapTempo,
        onTimeSignatureChange = viewModel::setTimeSignature,
        onSubdivisionChange = viewModel::setSubdivision,
        onSoundChange = viewModel::setSound,
        onVolumeChange = viewModel::setVolume,
        onOpenPresets = { showPresets = true },
        modifier = modifier,
    )

    if (showPresets) {
        PresetsSheet(
            presets = state.presets,
            current = state.config,
            onSave = viewModel::savePreset,
            onLoad = { preset ->
                viewModel.loadPreset(preset)
                showPresets = false
            },
            onDelete = viewModel::deletePreset,
            onDismiss = { showPresets = false },
        )
    }
}

@Composable
fun MetronomeContent(
    state: MetronomeUiState,
    onTogglePlayback: () -> Unit,
    onBpmChange: (Int) -> Unit,
    onAdjustBpm: (Int) -> Unit,
    onTapTempo: () -> Unit,
    onTimeSignatureChange: (TimeSignature) -> Unit,
    onSubdivisionChange: (Subdivision) -> Unit,
    onSoundChange: (SoundProfile) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onOpenPresets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One flash animation shared by the LEDs and the dial: snaps to 1 on each beat, fades to 0.
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
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tab_metronome),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenPresets) {
                Icon(AppIcons.Bookmark, contentDescription = stringResource(R.string.presets))
            }
        }

        Spacer(Modifier.height(8.dp))
        BeatIndicator(
            timeSignature = state.config.timeSignature,
            currentBeat = state.currentBeat,
            flash = flashValue,
            flashColor = flashColor,
        )

        Spacer(Modifier.height(16.dp))
        TempoDial(
            bpm = state.config.bpm,
            onBpmChange = onBpmChange,
            flash = flashValue,
            flashColor = flashColor,
            modifier = Modifier.size(280.dp),
        )

        // Coarse / fine tempo buttons
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TempoStepButton(label = "−5", description = stringResource(R.string.decrease_tempo)) { onAdjustBpm(-5) }
            TempoStepButton(icon = AppIcons.Remove, description = stringResource(R.string.decrease_tempo)) { onAdjustBpm(-1) }
            TempoStepButton(icon = AppIcons.Add, description = stringResource(R.string.increase_tempo)) { onAdjustBpm(+1) }
            TempoStepButton(label = "+5", description = stringResource(R.string.increase_tempo)) { onAdjustBpm(+5) }
        }

        // Transport
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = onTapTempo,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.tap), style = MaterialTheme.typography.titleLarge)
                    if (state.tapCount > 0) {
                        Text(
                            text = pluralStringResource(R.plurals.tap_hint, state.tapCount, state.tapCount),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Button(
                onClick = onTogglePlayback,
                shape = RoundedCornerShape(24.dp),
                colors = if (state.isPlaying) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp),
            ) {
                Icon(
                    imageVector = if (state.isPlaying) AppIcons.Stop else AppIcons.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(if (state.isPlaying) R.string.stop else R.string.start),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }

        // Time signature
        SectionLabel(stringResource(R.string.time_signature))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TimeSignature.entries.forEachIndexed { index, signature ->
                SegmentedButton(
                    selected = signature == state.config.timeSignature,
                    onClick = { onTimeSignatureChange(signature) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TimeSignature.entries.size),
                    icon = {},
                ) {
                    Text(signature.label)
                }
            }
        }

        // Subdivision
        SectionLabel(stringResource(R.string.subdivision))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Subdivision.entries.forEach { subdivision ->
                FilterChip(
                    selected = subdivision == state.config.subdivision,
                    onClick = { onSubdivisionChange(subdivision) },
                    label = { Text(subdivision.label) },
                )
            }
        }

        // Sound
        SectionLabel(stringResource(R.string.sound))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SoundProfile.entries.forEachIndexed { index, sound ->
                SegmentedButton(
                    selected = sound == state.config.sound,
                    onClick = { onSoundChange(sound) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SoundProfile.entries.size),
                ) {
                    Text(sound.label)
                }
            }
        }

        // Volume
        val volumeLabel = stringResource(R.string.volume)
        SectionLabel(volumeLabel)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(AppIcons.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = state.volume,
                onValueChange = onVolumeChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .semantics { contentDescription = volumeLabel },
            )
            Text(
                text = "${(state.volume * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(44.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun TempoStepButton(
    description: String,
    label: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .semantics { contentDescription = description },
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null)
        } else {
            Text(label.orEmpty(), style = MaterialTheme.typography.titleMedium)
        }
    }
}
