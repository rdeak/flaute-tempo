package hr.bertlab.fluteassistant.ui.tuner

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import hr.bertlab.fluteassistant.ui.common.DISABLED_ALPHA
import hr.bertlab.fluteassistant.ui.common.Emoji
import hr.bertlab.fluteassistant.ui.common.OutlineWidth
import hr.bertlab.fluteassistant.ui.common.chunky
import hr.bertlab.fluteassistant.ui.theme.FluteTheme
import hr.bertlab.fluteassistant.ui.theme.Sun
import kotlin.math.abs

/** Cents at either end of the meter; anything further is pinned there. */
private const val RANGE_CENTS = 50f

/** Share of the travel from the middle to an end that the green "in tune" band takes. */
private const val IN_TUNE_SHARE = 0.37f

private val MeterWidth = 60.dp
private val MaxFaceSize = 84.dp

/**
 * A tall, numberless meter with a face that rides up when the note is too high and down when
 * it is too low: green in the middle (within [TunerUiState.IN_TUNE_CENTS], the same
 * threshold as the status line, so the face's centre is in the green exactly when the status
 * says "Perfect!"), orange with an arrow towards either end. The green band takes more of
 * the travel than its share of cents, so a child sees the target; beyond it the travel is
 * even up to [RANGE_CENTS]. The face is grey and blank in silence ([cents] null), hops with a grin in
 * tune and looks surprised when off.
 */
@Composable
fun TuningGauge(
    cents: Double?,
    modifier: Modifier = Modifier,
) {
    val colors = FluteTheme.colors
    val inTuneCents = TunerUiState.IN_TUNE_CENTS.toFloat()
    val inTune = cents != null && abs(cents) <= inTuneCents
    val position by animateFloatAsState(
        targetValue = travel((cents ?: 0.0).toFloat(), inTuneCents),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "face",
    )
    // Only runs while in tune, so a silent tuner draws no frames.
    val hop = if (inTune) {
        rememberInfiniteTransition(label = "hop").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(250), RepeatMode.Reverse),
            label = "hop",
        )
    } else {
        null
    }

    BoxWithConstraints(modifier = modifier.width(MaxFaceSize), contentAlignment = Alignment.TopCenter) {
        // A short meter (a small phone) gets a smaller face, so it still has room to travel.
        val faceSize = minOf(MaxFaceSize, maxHeight * 0.28f)
        // The face's centre is inside the green exactly while in tune.
        val greenHeight = (maxHeight - faceSize) * IN_TUNE_SHARE
        val meterShape = RoundedCornerShape(MeterWidth / 2)
        Column(
            modifier = Modifier
                .width(MeterWidth)
                .fillMaxHeight()
                .clip(meterShape)
                .background(colors.gaugeOff)
                .border(OutlineWidth, colors.outline, meterShape),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
                Emoji(UP_EMOJI, fontSize = 20)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(greenHeight)
                    .background(colors.gaugeInTune)
                    .drawBehind {
                        val line = OutlineWidth.toPx()
                        drawRect(colors.outline, size = Size(size.width, line))
                        drawRect(colors.outline, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                    },
            )
            Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.BottomCenter) {
                Emoji(DOWN_EMOJI, fontSize = 20)
            }
        }

        // -1 at the bottom (too low) to 1 at the top (too high).
        val travelRoom = (maxHeight - faceSize) / 2
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(0, (travelRoom - travelRoom * position).roundToPx()) }
                .size(faceSize)
                .alpha(if (cents == null) DISABLED_ALPHA else 1f)
                .grayscale(cents == null)
                .chunky(CircleShape, Sun, colors.outline, depth = 4.dp),
        ) {
            Emoji(
                emoji = when {
                    cents == null -> SILENT_FACE
                    inTune -> HAPPY_FACE
                    else -> SURPRISED_FACE
                },
                fontSize = (faceSize.value * 0.55f).toInt(),
                modifier = Modifier.graphicsLayer { translationY = -6.dp.toPx() * (hop?.value ?: 0f) },
            )
        }
    }
}

/**
 * Where the face sits for [cents], -1 (bottom) to 1 (top): the in-tune cents fill the green
 * band's share of the travel, the rest of the range the remainder.
 */
private fun travel(cents: Float, inTuneCents: Float): Float {
    val magnitude = abs(cents).coerceAtMost(RANGE_CENTS)
    val share = if (magnitude <= inTuneCents) {
        magnitude / inTuneCents * IN_TUNE_SHARE
    } else {
        IN_TUNE_SHARE + (magnitude - inTuneCents) / (RANGE_CENTS - inTuneCents) * (1f - IN_TUNE_SHARE)
    }
    return if (cents < 0) -share else share
}

/** Draws the content without colour while [enabled]: the silent face is greyed out. */
private fun Modifier.grayscale(enabled: Boolean): Modifier = if (!enabled) this else drawWithContent {
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset(0f, 0f), Size(size.width, size.height + 8.dp.toPx())), paint)
        drawContent()
        canvas.restore()
    }
}

private const val UP_EMOJI = "⬆️" // up arrow
private const val DOWN_EMOJI = "⬇️" // down arrow
private const val SILENT_FACE = "😶" // face without mouth
private const val HAPPY_FACE = "😄" // grinning face with smiling eyes
private const val SURPRISED_FACE = "😮" // face with open mouth
