package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.domain.model.Preset
import hr.bertlab.fluteassistant.ui.common.AppIcons
import kotlinx.coroutines.launch

/**
 * Bottom sheet to save the current configuration as a named preset, and load / delete presets.
 * Deleting asks for confirmation (the trash icon sits next to the tap-to-load row);
 * loading animates the sheet closed before [onLoad] fires.
 */
@Composable
fun PresetsSheet(
    presets: List<Preset>,
    current: MetronomeConfig,
    onSave: (String) -> Unit,
    onLoad: (Preset) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var pendingDelete by rememberSaveable { mutableStateOf<Preset?>(null) }
    val save = {
        if (name.isNotBlank()) {
            onSave(name)
            name = ""
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .imePadding(),
        ) {
            Text(stringResource(R.string.presets), style = MaterialTheme.typography.titleLarge)
            Text(
                text = stringResource(R.string.current_settings, current.summary()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.preset_name)) },
                    placeholder = { Text(stringResource(R.string.preset_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.weight(1f),
                )
                FilledTonalButton(onClick = save, enabled = name.isNotBlank()) {
                    Icon(AppIcons.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.save))
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            if (presets.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_presets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                ) {
                    items(presets, key = { it.id }) { preset ->
                        ListItem(
                            headlineContent = { Text(preset.name) },
                            supportingContent = { Text(preset.config.summary()) },
                            trailingContent = {
                                IconButton(onClick = { pendingDelete = preset }) {
                                    Icon(
                                        AppIcons.Delete,
                                        contentDescription = stringResource(R.string.delete_preset, preset.name),
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                scope.launch { sheetState.hide() }.invokeOnCompletion { onLoad(preset) }
                            },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_preset_title)) },
            text = { Text(stringResource(R.string.delete_preset_body, preset.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(preset.id)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
