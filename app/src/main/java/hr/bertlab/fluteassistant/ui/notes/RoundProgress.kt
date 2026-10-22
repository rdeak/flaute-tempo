package hr.bertlab.fluteassistant.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.ui.theme.InTuneGreen
import hr.bertlab.fluteassistant.ui.theme.SlightlyOffAmber

/** Shows a flame once the child has this many first-tap answers in a row. */
private const val STREAK_SHOWN_FROM = 2

/**
 * One dot per note of the round: green when found on the first tap, amber when it took
 * more, the one being asked ringed in coral, the rest faint. The score and the accuracy
 * at a glance, without a number. When [streak] reaches [STREAK_SHOWN_FROM], a flame with
 * the count sits at the right end.
 */
@Composable
fun RoundProgress(results: List<Boolean>, streak: Int, modifier: Modifier = Modifier) {
    val description = pluralStringResource(
        R.plurals.round_progress,
        NotesUiState.ROUND_LENGTH,
        results.size,
        NotesUiState.ROUND_LENGTH,
    )
    val streakDescription = stringResource(R.string.streak_description, streak)
    val faint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            repeat(NotesUiState.ROUND_LENGTH) { index ->
                val fill = when {
                    index < results.size -> if (results[index]) InTuneGreen else SlightlyOffAmber
                    else -> faint
                }
                Box(
                    Modifier
                        .size(14.dp)
                        .background(if (index == results.size) Color.Transparent else fill, CircleShape)
                        .then(
                            if (index == results.size) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier,
                        ),
                )
            }
        }
        if (streak >= STREAK_SHOWN_FROM) {
            Text(
                text = stringResource(R.string.streak_badge, streak),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .semantics { contentDescription = streakDescription },
            )
        }
    }
}
