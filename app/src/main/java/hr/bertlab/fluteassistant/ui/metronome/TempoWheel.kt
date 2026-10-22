package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import kotlinx.coroutines.launch
import kotlin.math.abs

private val WheelItemHeight = 48.dp
private const val WheelVisibleItems = 3

/**
 * A vertical drum of tempos, three visible: the value in the middle is the chosen one, the
 * neighbours above and below are small and faded. Scrolling snaps to whole beats and every
 * notch ticks; the chosen value is reported as it changes, so the ring and a running
 * metronome follow live. Tapping a neighbour scrolls to it; tapping the middle calls
 * [onClose], the caller's cue to show the plain number again.
 */
@Composable
fun TempoWheel(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    minBpm: Int = MetronomeConfig.MIN_BPM,
    maxBpm: Int = MetronomeConfig.MAX_BPM,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = bpm - minBpm)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val description = stringResource(R.string.tempo_wheel)
    val centredBpm by remember(listState, minBpm) {
        derivedStateOf { listState.centredIndex()?.plus(minBpm) }
    }

    // Wheel -> tempo: as soon as another value sits in the middle, it is the tempo.
    LaunchedEffect(centredBpm) {
        val value = centredBpm ?: return@LaunchedEffect
        if (value != bpm) {
            onBpmChange(value)
            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }
    // Tempo -> wheel: a change from elsewhere (assistive technology on the ring) moves the drum.
    LaunchedEffect(bpm) {
        if (!listState.isScrollInProgress && centredBpm != bpm) listState.scrollToItem(bpm - minBpm)
    }

    // The line must be taller than the glyphs or Text clips the bottom of the digits.
    val centreStyle = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, lineHeight = 76.sp)
    val neighbourStyle = MaterialTheme.typography.titleLarge
    val centreColor = MaterialTheme.colorScheme.onSurface
    val neighbourColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        // One item's worth of padding at each end lets the first and last tempo reach the middle.
        contentPadding = PaddingValues(vertical = WheelItemHeight * ((WheelVisibleItems - 1) / 2)),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(WheelItemHeight * WheelVisibleItems)
            .semantics { contentDescription = description },
    ) {
        items(count = maxBpm - minBpm + 1, key = { it }) { index ->
            val value = minBpm + index
            val isCentre = value == centredBpm
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .clickable(interactionSource = null, indication = null) {
                        if (isCentre) onClose() else scope.launch { listState.animateScrollToItem(index) }
                    },
            ) {
                Text(
                    text = value.toString(),
                    style = if (isCentre) centreStyle else neighbourStyle,
                    color = if (isCentre) centreColor else neighbourColor,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                )
            }
        }
    }
}

/** Index of the item whose middle is nearest the middle of the viewport, or null before layout. */
private fun LazyListState.centredIndex(): Int? {
    val info = layoutInfo
    val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - centre) }?.index
}
