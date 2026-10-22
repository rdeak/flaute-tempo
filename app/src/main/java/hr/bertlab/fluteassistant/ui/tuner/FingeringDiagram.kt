package hr.bertlab.fluteassistant.ui.tuner

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.tuner.FluteKey
import kotlin.math.min

/**
 * A flute seen from above, head joint on the left: the thumb key, the three left-hand keys, the
 * G sharp lever, the three right-hand keys, the E flat key and the two foot-joint rollers.
 * Keys in [keys] are filled in coral, the rest are outlines; with no [keys] every key is drawn
 * faintly open. [noteLabel] names the note for accessibility.
 */
@Composable
fun FingeringDiagram(
    keys: Set<FluteKey>?,
    noteLabel: String?,
    modifier: Modifier = Modifier,
) {
    val pressed = MaterialTheme.colorScheme.primary
    val open = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (keys == null) 0.3f else 0.7f)
    val description = if (keys != null && noteLabel != null) stringResource(R.string.fingering_for, noteLabel) else null

    Canvas(
        modifier = modifier.then(
            if (description != null) Modifier.semantics { contentDescription = description } else Modifier,
        ),
    ) {
        val unit = size.width / SPAN
        val radius = min(unit * 0.38f, size.height * 0.32f)
        val small = radius * 0.62f
        val stroke = Stroke(width = 2.dp.toPx())
        val centreY = size.height / 2f
        val lowY = centreY + radius * 0.55f // the little-finger keys sit a bit lower, as on the flute

        fun x(slot: Float) = (MARGIN + slot) * unit

        fun circle(slot: Float, r: Float, y: Float, down: Boolean) {
            val centre = Offset(x(slot), y)
            if (down) drawCircle(pressed, r, centre) else drawCircle(open, r, centre, style = stroke)
        }

        fun paddle(slot: Float, width: Float, height: Float, y: Float, down: Boolean) {
            val topLeft = Offset(x(slot) - width / 2f, y - height / 2f)
            val corner = CornerRadius(width * 0.4f)
            if (down) drawRoundRect(pressed, topLeft, Size(width, height), corner)
            else drawRoundRect(open, topLeft, Size(width, height), corner, style = stroke)
        }

        fun down(key: FluteKey) = keys?.contains(key) == true

        paddle(THUMB_SLOT, radius * 1.1f, radius * 2.4f, centreY, down(FluteKey.THUMB))
        circle(L1_SLOT, radius, centreY, down(FluteKey.L1))
        circle(L1_SLOT + 1f, radius, centreY, down(FluteKey.L2))
        circle(L1_SLOT + 2f, radius, centreY, down(FluteKey.L3))
        circle(G_SHARP_SLOT, small, lowY, down(FluteKey.G_SHARP))
        circle(R1_SLOT, radius, centreY, down(FluteKey.R1))
        circle(R1_SLOT + 1f, radius, centreY, down(FluteKey.R2))
        circle(R1_SLOT + 2f, radius, centreY, down(FluteKey.R3))
        circle(E_FLAT_SLOT, small, lowY, down(FluteKey.E_FLAT))
        paddle(C_SHARP_SLOT, small * 1.3f, small * 2.2f, lowY, down(FluteKey.C_SHARP))
        paddle(LOW_C_SLOT, small * 1.3f, small * 2.2f, lowY, down(FluteKey.LOW_C))
    }
}

// Horizontal slots, in key widths; the right hand starts after a wider gap.
private const val MARGIN = 0.5f
private const val THUMB_SLOT = 0f
private const val L1_SLOT = 1f
private const val G_SHARP_SLOT = 4f
private const val R1_SLOT = 5.6f
private const val E_FLAT_SLOT = 8.6f
private const val C_SHARP_SLOT = 9.4f
private const val LOW_C_SLOT = 10.1f
private const val SPAN = LOW_C_SLOT + 2 * MARGIN
