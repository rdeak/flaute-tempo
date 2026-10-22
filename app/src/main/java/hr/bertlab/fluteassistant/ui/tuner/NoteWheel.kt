package hr.bertlab.fluteassistant.ui.tuner

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.ValueWheel
import hr.bertlab.fluteassistant.ui.common.WHEEL_VISIBLE_ITEMS
import hr.bertlab.fluteassistant.ui.common.noteLabel
import hr.bertlab.fluteassistant.ui.theme.InTuneGreen
import hr.bertlab.fluteassistant.ui.theme.InTuneGreenText

private val NoteRowHeight = 56.dp

/** Height of the whole wheel, so the big letter in the other mode can take the same space. */
val NoteWheelHeight: Dp = NoteRowHeight * WHEEL_VISIBLE_ITEMS

/**
 * The note the child is tuning: a [ValueWheel] over the flute's range, the chosen note huge
 * on a peach band and its neighbours faded above and below. Scrolling picks another note;
 * the band shows which row counts. The chosen note turns green while [inTune]. A speaker
 * button at the right end of the band calls [onPlay] so the child can hear the note; it is
 * disabled while [isPlaying].
 */
@Composable
fun NoteWheel(
    targetMidi: Int,
    onTargetChange: (Int) -> Unit,
    inTune: Boolean,
    onPlay: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current
    val inTuneColor = if (isSystemInDarkTheme()) InTuneGreen else InTuneGreenText
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(NoteRowHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large),
        )
        ValueWheel(
            itemCount = FluteRange.midiRange.count(),
            selectedIndex = targetMidi - FluteRange.LOWEST_MIDI,
            onSelectedIndexChange = { onTargetChange(FluteRange.LOWEST_MIDI + it) },
            label = { noteLabel(resources, FluteRange.LOWEST_MIDI + it) },
            description = stringResource(R.string.target_note),
            itemHeight = NoteRowHeight,
            centreStyle = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, lineHeight = 84.sp),
            centreColor = if (inTune) inTuneColor else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        FilledIconButton(
            onClick = onPlay,
            enabled = !isPlaying,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(44.dp),
        ) {
            Icon(AppIcons.VolumeUp, contentDescription = stringResource(R.string.hear_note))
        }
    }
}
