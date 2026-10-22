package hr.bertlab.fluteassistant.ui.metronome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.R
import hr.bertlab.fluteassistant.domain.model.MetronomeConfig
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val SWEEP_ANGLE = 270f

/**
 * A chunky tempo ring. Dragging anywhere on it rotates it; one degree of rotation is one
 * BPM, so the knob tracks the finger. The filled arc shows where the tempo sits in the
 * supported range, the number sits in the middle with a "how fast does it feel" line under
 * it, and the whole ring glows with [flash] (read in the draw phase only) on every beat.
 * Exposes slider semantics so assistive technology can adjust the tempo too.
 */
@Composable
fun TempoDial(
    bpm: Int,
    onBpmChange: (Int) -> Unit,
    flash: () -> Float,
    flashColor: Color,
    modifier: Modifier = Modifier,
    minBpm: Int = MetronomeConfig.MIN_BPM,
    maxBpm: Int = MetronomeConfig.MAX_BPM,
) {
    val haptics = LocalHapticFeedback.current
    val currentBpm by rememberUpdatedState(bpm)
    val currentOnBpmChange by rememberUpdatedState(onBpmChange)
    val degreesPerBpm = SWEEP_ANGLE / (maxBpm - minBpm)

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary
    val knobCenterColor = MaterialTheme.colorScheme.onPrimary
    val dialDescription = stringResource(R.string.tempo_dial)

    Box(
        modifier = modifier
            .progressSemantics(bpm.toFloat(), minBpm.toFloat()..maxBpm.toFloat(), steps = maxBpm - minBpm - 1)
            .semantics {
                contentDescription = dialDescription
                setProgress { value ->
                    onBpmChange(value.roundToInt().coerceIn(minBpm, maxBpm))
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(minBpm, maxBpm) {
                    var lastAngle = 0f
                    var accumulated = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            lastAngle = angleOf(offset, size)
                            accumulated = 0f
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val angle = angleOf(change.position, size)
                            var delta = angle - lastAngle
                            if (delta > 180f) delta -= 360f else if (delta < -180f) delta += 360f
                            lastAngle = angle
                            accumulated += delta
                            val steps = (accumulated / degreesPerBpm).toInt()
                            if (steps != 0) {
                                accumulated -= steps * degreesPerBpm
                                val next = (currentBpm + steps).coerceIn(minBpm, maxBpm)
                                if (next != currentBpm) {
                                    currentOnBpmChange(next)
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                }
                            }
                        },
                    )
                },
        ) {
            val strokeWidth = 22.dp.toPx()
            val radius = size.minDimension / 2f - strokeWidth
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            val fraction = (bpm - minBpm).toFloat() / (maxBpm - minBpm)

            // Beat glow behind the number.
            val glow = flash()
            if (glow > 0f) {
                drawCircle(flashColor.copy(alpha = 0.35f * glow), radius = radius - strokeWidth, center = center)
            }

            // Track and progress arcs.
            drawArc(
                color = trackColor, startAngle = START_ANGLE, sweepAngle = SWEEP_ANGLE, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = progressColor, startAngle = START_ANGLE, sweepAngle = SWEEP_ANGLE * fraction, useCenter = false,
                topLeft = topLeft, size = arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )

            // Knob at the end of the progress arc.
            val knobAngle = Math.toRadians((START_ANGLE + SWEEP_ANGLE * fraction).toDouble())
            val knobCenter = center + Offset(cos(knobAngle).toFloat(), sin(knobAngle).toFloat()) * radius
            drawCircle(progressColor, radius = strokeWidth * 0.95f, center = knobCenter)
            drawCircle(knobCenterColor, radius = strokeWidth * 0.35f, center = knobCenter)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = bpm.toString(), style = MaterialTheme.typography.displayLarge)
            val feel = tempoFeel(bpm)
            Text(
                text = "${feel.emoji} ${stringResource(feel.labelRes)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Angle in degrees of [point] around the centre of a [size]-sized box (0 = 3 o'clock). */
private fun angleOf(point: Offset, size: IntSize): Float {
    val dx = point.x - size.width / 2f
    val dy = point.y - size.height / 2f
    return Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
}
