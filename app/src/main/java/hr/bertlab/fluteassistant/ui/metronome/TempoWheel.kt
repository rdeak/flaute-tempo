package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import hr.bertlab.fluteassistant.ui.common.ValueWheel

/**
 * A [ValueWheel] of tempos, one beat per notch: the chosen tempo big in the middle, the
 * neighbours small and faded, reported as it changes so the ring and a running metronome
 * follow live. Tapping the middle calls [onClose], the caller's cue to show the plain
 * number again.
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
    ValueWheel(
        itemCount = maxBpm - minBpm + 1,
        selectedIndex = bpm - minBpm,
        onSelectedIndexChange = { onBpmChange(minBpm + it) },
        label = { (minBpm + it).toString() },
        description = stringResource(R.string.tempo_wheel),
        onCentreClick = onClose,
        modifier = modifier,
    )
}
