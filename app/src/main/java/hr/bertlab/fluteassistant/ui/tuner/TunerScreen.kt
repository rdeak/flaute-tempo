package hr.bertlab.fluteassistant.ui.tuner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.tuner.NoteMath
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.theme.InTuneGreen
import hr.bertlab.fluteassistant.ui.theme.OutOfTuneRed
import hr.bertlab.fluteassistant.ui.theme.SlightlyOffAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val IN_TUNE_CENTS = 5.0

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
        onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onOpenSettings = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                ),
            )
        },
        onReferenceChange = viewModel::setReferenceA4,
        modifier = modifier,
    )
}

@Composable
fun TunerContent(
    state: TunerUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onReferenceChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header with reference pitch control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tab_tuner),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            ReferencePitchControl(referenceA4 = state.referenceA4, onChange = onReferenceChange)
        }

        Spacer(Modifier.height(16.dp))

        if (!state.permissionGranted) {
            PermissionCard(onRequestPermission = onRequestPermission, onOpenSettings = onOpenSettings)
        } else {
            val reading = state.reading
            val cents = reading?.cents
            val status = when {
                cents == null -> TuningStatus.NONE
                abs(cents) <= IN_TUNE_CENTS -> TuningStatus.IN_TUNE
                cents < 0 -> TuningStatus.LOW
                else -> TuningStatus.HIGH
            }
            val statusColor = when (status) {
                TuningStatus.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                TuningStatus.IN_TUNE -> InTuneGreen
                TuningStatus.LOW, TuningStatus.HIGH ->
                    if (abs(cents ?: 0.0) <= 15) SlightlyOffAmber else OutOfTuneRed
            }

            // Note name
            Text(
                text = reading?.label ?: "—",
                style = MaterialTheme.typography.displayLarge,
                color = if (status == TuningStatus.IN_TUNE) InTuneGreen else MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = reading?.let { String.format(Locale.US, "%.1f Hz", it.frequency) }
                        ?: stringResource(R.string.play_a_note),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (cents != null) {
                    Text(
                        text = String.format(Locale.US, "%+.0f ¢", cents),
                        style = MaterialTheme.typography.bodyLarge,
                        color = statusColor,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            TuningGauge(
                cents = cents,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.9f),
            )

            // Neighbouring notes as orientation
            if (reading != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(NoteMath.labelOf(reading.midi - 1), style = MaterialTheme.typography.labelLarge)
                    Text(NoteMath.labelOf(reading.midi + 1), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = when (status) {
                    TuningStatus.NONE -> stringResource(if (state.isListening) R.string.listening else R.string.play_a_note)
                    TuningStatus.IN_TUNE -> stringResource(R.string.in_tune)
                    TuningStatus.LOW -> stringResource(R.string.too_low)
                    TuningStatus.HIGH -> stringResource(R.string.too_high)
                },
                style = MaterialTheme.typography.titleMedium,
                color = statusColor,
            )

            // Input level
            Spacer(Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { state.level },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            )

            state.errorMessage?.let { message ->
                Spacer(Modifier.height(16.dp))
                Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private enum class TuningStatus { NONE, IN_TUNE, LOW, HIGH }

@Composable
private fun ReferencePitchControl(referenceA4: Double, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(referenceA4 - 1) }) {
            Icon(AppIcons.Remove, contentDescription = stringResource(R.string.lower_reference))
        }
        Text(
            text = stringResource(R.string.reference_pitch, referenceA4.roundToInt()),
            style = MaterialTheme.typography.labelLarge,
        )
        IconButton(onClick = { onChange(referenceA4 + 1) }) {
            Icon(AppIcons.Add, contentDescription = stringResource(R.string.raise_reference))
        }
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit, onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                AppIcons.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.mic_permission_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.mic_permission_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequestPermission) { Text(stringResource(R.string.allow_microphone)) }
                TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.open_settings)) }
            }
        }
    }
}
