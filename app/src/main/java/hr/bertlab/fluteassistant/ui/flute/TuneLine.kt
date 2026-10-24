package hr.bertlab.fluteassistant.ui.flute

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.flute.TuneNote
import hr.bertlab.fluteassistant.ui.common.OutlineWidth
import hr.bertlab.fluteassistant.ui.common.dashedBorder
import hr.bertlab.fluteassistant.ui.common.noteColor
import hr.bertlab.fluteassistant.ui.common.noteText
import hr.bertlab.fluteassistant.ui.theme.FluteTheme
import hr.bertlab.fluteassistant.ui.theme.InkOnFill
import hr.bertlab.fluteassistant.ui.theme.Sun

/** The line's height: one row of note circles with room for the ring. */
val TuneLineHeight = 48.dp

/**
 * The child's tune as a line of small circles, one per note in its letter's colour, the one
 * at [litIndex] (sounding now, or just pressed) ringed in yellow. The line scrolls sideways:
 * it follows the newest note and the playing one, and the child can drag it back to look.
 * While [tune] is empty it is a dashed pill with one hint instead.
 */
@Composable
fun TuneLine(
    tune: List<TuneNote>,
    litIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = FluteTheme.colors
    if (tune.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(TuneLineHeight)
                .dashedBorder(OutlineWidth / 2, colors.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.flute_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedText,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 22.sp),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        return
    }
    val description = pluralStringResource(R.plurals.flute_tune_line, tune.size, tune.size)
    val listState = rememberLazyListState()
    LaunchedEffect(tune.size, litIndex) {
        listState.animateScrollToItem(litIndex ?: tune.lastIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .height(TuneLineHeight)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(tune) { index, note -> TuneNoteCircle(midi = note.midi, lit = index == litIndex) }
    }
}

@Composable
private fun TuneNoteCircle(midi: Int, lit: Boolean) {
    val colors = FluteTheme.colors
    Box(
        modifier = Modifier
            .size(38.dp)
            .drawBehind {
                if (lit) drawCircle(Sun, radius = size.minDimension / 2 + 4.dp.toPx(), style = Stroke(4.dp.toPx()))
            }
            .clip(CircleShape)
            .background(noteColor(midi))
            .border(OutlineWidth, colors.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(noteText(midi), style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = InkOnFill, maxLines = 1)
    }
}
