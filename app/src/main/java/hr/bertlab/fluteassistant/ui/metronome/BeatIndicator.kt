package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.domain.model.BeatStrength
import hr.bertlab.fluteassistant.domain.model.TimeSignature

/**
 * A row of "LEDs", one per beat of the bar. The accented beats are drawn larger; the beat
 * that is currently sounding lights up in [flashColor] and pops with the [flash] value
 * (1 = just hit, 0 = faded). [flash] is a lambda read only in the draw/layer phase, so the
 * animation never recomposes this row.
 */
@Composable
fun BeatIndicator(
    timeSignature: TimeSignature,
    currentBeat: Int,
    flash: () -> Float,
    flashColor: Color,
    modifier: Modifier = Modifier,
) {
    val idleColor = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        timeSignature.accents.forEachIndexed { index, strength ->
            val diameter = when (strength) {
                BeatStrength.STRONG -> 28.dp
                BeatStrength.MEDIUM -> 24.dp
                else -> 18.dp
            }
            val active = index == currentBeat
            Box(
                modifier = Modifier
                    .size(diameter)
                    .graphicsLayer {
                        val pop = if (active) 1f + 0.3f * flash() else 1f
                        scaleX = pop
                        scaleY = pop
                    }
                    .drawBehind {
                        val color = if (active) flashColor.copy(alpha = 0.45f + 0.55f * flash()) else idleColor
                        drawCircle(color)
                    },
            )
        }
    }
}
