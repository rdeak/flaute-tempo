package hr.bertlab.fluteassistant.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import hr.bertlab.fluteassistant.ui.common.noteLabel

/** Pills per row: fifteen notes make three rows of five. */
private const val NOTES_PER_ROW = 5

/**
 * The notes the game may ask, one pill each from c1 to c3 in three rows of five, the chosen
 * ones filled teal like the level pills on the metronome; the row above it asks "Which
 * notes?". A tap calls [onToggle]; the ViewModel keeps the last note from being switched
 * off. Under the grid, one wide pill switches the sound of a found note ([soundOn],
 * [onToggleSound]): a setting, so it lives here with the other one rather than on the game
 * screen.
 */
@Composable
fun PracticeNotePicker(
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    soundOn: Boolean,
    onToggleSound: () -> Unit,
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
        Surface(
            shape = CircleShape,
            color = if (soundOn) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (soundOn) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .toggleable(value = soundOn, role = Role.Switch, onValueChange = { onToggleSound() }),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = (if (soundOn) "\uD83D\uDD0A " else "\uD83D\uDD07 ") + stringResource(R.string.note_sound),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
