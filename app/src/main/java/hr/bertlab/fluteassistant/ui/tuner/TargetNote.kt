package hr.bertlab.fluteassistant.ui.tuner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.tuner.FluteRange
import hr.bertlab.fluteassistant.ui.common.AppIcons
import hr.bertlab.fluteassistant.ui.common.ChunkyButton
import hr.bertlab.fluteassistant.ui.common.Emoji
import hr.bertlab.fluteassistant.ui.common.NoteBubble
import hr.bertlab.fluteassistant.ui.common.noteLabel

/**
 * The note the child is tuning to, in its letter's colour between a ‹ and a › button that
 * move it a half step down or up the flute's range ([FluteRange]); the buttons fade at the
 * ends. Under it a "Hear" pill calls [onPlay] so the child can hear the note first; it is
 * disabled while [isPlaying].
 */
@Composable
fun TargetNote(
    targetMidi: Int,
    onTargetChange: (Int) -> Unit,
    onPlay: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val noteName = noteLabel(targetMidi)
    val description = stringResource(R.string.target_note)
    // The canvas drew a 120 dp bubble between 52 dp buttons; a narrow or short space shrinks
    // the bubble (and on a narrow one the buttons) rather than squeeze the buttons out.
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val button = if (maxWidth >= 240.dp) 52.dp else 44.dp
        val gap = if (maxHeight >= 220.dp) 22.dp else 12.dp
        val bubble = minOf(120.dp, maxWidth - button * 2 - 16.dp, maxHeight - HearHeight - gap - 10.dp)
            .coerceAtLeast(64.dp)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton(
                    icon = AppIcons.ChevronLeft,
                    description = stringResource(R.string.lower_note),
                    enabled = targetMidi > FluteRange.LOWEST_MIDI,
                    size = button,
                    onClick = { onTargetChange(targetMidi - 1) },
                )
                NoteBubble(
                    midi = targetMidi,
                    size = bubble,
                    fontSize = (bubble.value / 2).sp,
                    modifier = Modifier.semantics {
                        contentDescription = description
                        stateDescription = noteName
                    },
                )
                StepButton(
                    icon = AppIcons.ChevronRight,
                    description = stringResource(R.string.higher_note),
                    enabled = targetMidi < FluteRange.HIGHEST_MIDI,
                    size = button,
                    onClick = { onTargetChange(targetMidi + 1) },
                )
            }
            ChunkyButton(
                onClick = onPlay,
                enabled = !isPlaying,
                modifier = Modifier.height(HearHeight),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 22.dp),
                ) {
                    Emoji(SPEAKER_EMOJI, fontSize = 24)
                    Text(stringResource(R.string.hear_note), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp))
                }
            }
        }
    }
}

private val HearHeight = 56.dp

@Composable
private fun StepButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    size: Dp,
    onClick: () -> Unit,
) {
    ChunkyButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(size)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(30.dp))
    }
}

private const val SPEAKER_EMOJI = "🔊" // speaker, loud
