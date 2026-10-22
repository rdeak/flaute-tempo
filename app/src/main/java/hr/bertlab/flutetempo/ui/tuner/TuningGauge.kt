package hr.bertlab.flutetempo.ui.tuner

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import hr.bertlab.flutetempo.ui.theme.InTuneGreen
import hr.bertlab.flutetempo.ui.theme.OutOfTuneRed
import hr.bertlab.flutetempo.ui.theme.SlightlyOffAmber
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val RANGE_CENTS = 50f

/**
 * Semicircular gauge from -50 to +50 cents with a spring-animated needle. Colour zones:
 * green within [inTuneCents], amber up to 15 cents, red beyond. A null [cents] (silence)
 * parks the needle at the centre with reduced opacity.
 */
@Composable
fun TuningGauge(
    cents: Double?,
    modifier: Modifier = Modifier,
    inTuneCents: Float = 5f,
) {
    val needle by animateFloatAsState(
        targetValue = (cents ?: 0.0).toFloat().coerceIn(-RANGE_CENTS, RANGE_CENTS),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "needle",
    )
    val needleAlpha by animateFloatAsState(targetValue = if (cents == null) 0.3f else 1f, label = "needleAlpha")

    val needleColor = MaterialTheme.colorScheme.onSurface
    val tickColor = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val strokeWidth = 18.dp.toPx()
        val radius = min(size.width / 2f, size.height) - strokeWidth
        val center = Offset(size.width / 2f, size.height - strokeWidth / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        fun angleFor(c: Float): Float = 180f + (c + RANGE_CENTS) / (2 * RANGE_CENTS) * 180f

        fun zone(from: Float, to: Float, color: Color) {
            drawArc(
                color = color, startAngle = angleFor(from), sweepAngle = angleFor(to) - angleFor(from),
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Butt),
            )
        }
        zone(-RANGE_CENTS, -15f, OutOfTuneRed.copy(alpha = 0.45f))
        zone(-15f, -inTuneCents, SlightlyOffAmber.copy(alpha = 0.6f))
        zone(-inTuneCents, inTuneCents, InTuneGreen)
        zone(inTuneCents, 15f, SlightlyOffAmber.copy(alpha = 0.6f))
        zone(15f, RANGE_CENTS, OutOfTuneRed.copy(alpha = 0.45f))

        // Ticks every 10 cents, labels every 25.
        val tickOuter = radius - strokeWidth / 2f - 4.dp.toPx()
        for (c in -50..50 step 10) {
            val angle = Math.toRadians(angleFor(c.toFloat()).toDouble())
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            val length = if (c % 25 == 0) 12.dp.toPx() else 6.dp.toPx()
            drawLine(
                color = tickColor,
                start = center + direction * tickOuter,
                end = center + direction * (tickOuter - length),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            if (c % 25 == 0) {
                val text = if (c > 0) "+$c" else c.toString()
                val layout = textMeasurer.measure(AnnotatedString(text), labelStyle)
                val position = center + direction * (tickOuter - length - 14.dp.toPx())
                drawText(
                    layout,
                    topLeft = Offset(position.x - layout.size.width / 2f, position.y - layout.size.height / 2f),
                )
            }
        }

        // Needle and hub.
        val needleAngle = Math.toRadians(angleFor(needle).toDouble())
        val tip = center + Offset(cos(needleAngle).toFloat(), sin(needleAngle).toFloat()) * (radius - strokeWidth)
        drawLine(
            color = needleColor.copy(alpha = needleAlpha),
            start = center,
            end = tip,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(needleColor.copy(alpha = needleAlpha), radius = 8.dp.toPx(), center = center)
    }
}
