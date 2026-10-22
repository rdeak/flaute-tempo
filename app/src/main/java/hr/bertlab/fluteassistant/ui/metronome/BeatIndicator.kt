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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.domain.model.BeatStrength
import hr.bertlab.fluteassistant.domain.model.TimeSignature

/** Dot diameters and the gap between them; long bars get smaller dots so 12 still fit a 360 dp phone. */
private class DotSizes(val strong: Dp, val medium: Dp, val weak: Dp, val gap: Dp) {
    fun diameter(strength: BeatStrength): Dp = when (strength) {
        BeatStrength.STRONG -> strong
        BeatStrength.MEDIUM -> medium
        else -> weak
    }
}

private fun dotSizesFor(beatsPerBar: Int): DotSizes = when {
    beatsPerBar <= 6 -> DotSizes(strong = 36.dp, medium = 30.dp, weak = 24.dp, gap = 18.dp)
    beatsPerBar <= 9 -> DotSizes(strong = 28.dp, medium = 24.dp, weak = 20.dp, gap = 10.dp)
    else -> DotSizes(strong = 24.dp, medium = 20.dp, weak = 16.dp, gap = 8.dp)
}

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
    val sizes = dotSizesFor(timeSignature.beatsPerBar)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(sizes.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        timeSignature.accents.forEachIndexed { index, strength ->
            val diameter = sizes.diameter(strength)
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
