package hr.bertlab.fluteassistant.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.notes.StaffNotes
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.noteLabel

/** Pills per row: fifteen notes make three rows of five. */
private const val NOTES_PER_ROW = 5

/**
 * The notes the game may ask, one pill each from c1 to c3 in three rows of five, the chosen
 * ones filled teal like the level pills on the metronome; the row above it asks "Which
 * notes?". A tap calls [onToggle]; the ViewModel keeps the last note from being switched
 * off. Under the grid, past a divider so it cannot pass for a sixteenth pill, one switch row
 * says what the sound setting does ("Play the note you find", [soundOn],
 * [onToggleSound]): a setting, so it lives here with the other one rather than on the game
 * screen. Last, one big OK button ([onDone]) closes the picker: choices apply as they are
 * tapped, so OK only brings the game back.
 */
@Composable
fun PracticeNotePicker(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    soundOn: Boolean,
    onToggleSound: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupLabel = stringResource(R.string.practice_notes)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = groupLabel },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = NOTES_PER_ROW,
        ) {
            StaffNotes.ALL.forEach { midi ->
                val isSelected = midi in selected
                Surface(
                    selected = isSelected,
                    onClick = { onToggle(midi) },
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .semantics { role = Role.Checkbox },
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(noteLabel(midi), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        SoundSettingRow(soundOn = soundOn, onToggleSound = onToggleSound)
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onDone,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(OkButtonHeight),
        ) {
            Icon(AppIcons.Check, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.choose_notes_done), style = MaterialTheme.typography.titleLarge)
        }
    }
}

/** The OK button's height: a sure thumb target, a step under the summary's "Play again". */
private val OkButtonHeight = 64.dp

/**
 * One settings row, the shape every phone uses for an on/off choice: the label at the start
 * (a speaker emoji that mutes while off, then what the switch does) and a switch at the end.
 * The whole row toggles and is the one accessible node, so the switch itself is passive.
 */
@Composable
private fun SoundSettingRow(soundOn: Boolean, onToggleSound: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .toggleable(value = soundOn, role = Role.Switch, onValueChange = { onToggleSound() })
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (if (soundOn) "🔊 " else "🔇 ") + stringResource(R.string.note_sound),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Switch(checked = soundOn, onCheckedChange = null)
    }
}
