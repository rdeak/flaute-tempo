package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.flute.TuneNote
import hr.bertlab.fluteassistant.ui.common.noteLabel

/** The line's height: one row of pills with air around it. */
val TuneLineHeight = 48.dp

/**
 * The child's tune as a line of small pills, one per note in the colour of its octave row, the
 * one at [litIndex] (sounding now, or just pressed) coral. The line scrolls sideways: it
 * follows the newest note and the playing one, and the child can drag it back to look.
 * While [tune] is empty it shows one faint hint instead.
 */
@Composable
fun TuneLine(
    tune: List<TuneNote>,
    litIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TuneLineHeight)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (tune.isEmpty()) {
            Text(
                text = stringResource(R.string.flute_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val description = pluralStringResource(R.plurals.flute_tune_line, tune.size, tune.size)
            val listState = rememberLazyListState()
            LaunchedEffect(tune.size, litIndex) {
                listState.animateScrollToItem(litIndex ?: tune.lastIndex)
            }
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = description },
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                itemsIndexed(tune) { index, note -> TunePill(midi = note.midi, lit = index == litIndex) }
            }
        }
    }
}

@Composable
private fun TunePill(midi: Int, lit: Boolean) {
    val (container, content) = if (lit) {
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
    } else {
        octaveColors(midi)
    }
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 34.dp)
            .height(34.dp)
            .background(container, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(noteLabel(midi), style = MaterialTheme.typography.labelLarge, color = content)
    }
}
