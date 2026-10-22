package hr.bertlab.fluteassistant.ui.notes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.ui.common.AppIcons

/**
 * What the round came to, in place of the staff, under the score shown big in the row
 * above: its caption, a headline that grows with the score, the best streak, the accuracy,
 * how long the round took, and one big button to go again.
 */
@Composable
fun RoundSummary(state: NotesUiState, onPlayAgain: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.round_score_caption),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(headlineFor(state.score)),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.round_best_streak, state.bestStreak), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.round_accuracy, state.accuracyPercent ?: 0), style = MaterialTheme.typography.titleMedium)
        state.roundMillis?.let { millis ->
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.round_time, formatRoundTime(millis)), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onPlayAgain,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
        ) {
            Icon(AppIcons.PlayArrow, contentDescription = null, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.play_again), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@StringRes
private fun headlineFor(score: Int): Int = when {
    score >= 9 -> R.string.round_amazing
    score >= 7 -> R.string.round_great
    score >= 4 -> R.string.round_good
    else -> R.string.round_keep_going
}
