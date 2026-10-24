package hr.bertlab.fluteassistant.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The few icons that are not emoji, hand-built so the app does not depend on the (large)
 * extended Material icon set: the tuner's ‹ › buttons (the standard 24dp Material paths) and
 * the pitch window's round-ended strokes (the canvas's own, in the same 24 box). Tinting is
 * applied by [androidx.compose.material3.Icon].
 */
object AppIcons {
    val ChevronLeft: ImageVector by lazy { filled("chevron_left", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z") }
    val ChevronRight: ImageVector by lazy { filled("chevron_right", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z") }
    val ChevronUp: ImageVector by lazy { stroked("chevron_up", "M6,15l6,-6 6,6", 3.5f) }
    val ChevronDown: ImageVector by lazy { stroked("chevron_down", "M6,9l6,6 6,-6", 3.5f) }
    val ArrowUp: ImageVector by lazy { stroked("arrow_up", "M12,19V6M6,11l6,-6 6,6", 3.5f) }
    val ArrowDown: ImageVector by lazy { stroked("arrow_down", "M12,5V18M6,13l6,6 6,-6", 3.5f) }
    val Check: ImageVector by lazy { stroked("check", "M5,12.5l4.5,4.5L19,7.5", 4f) }

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()

    private fun filled(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(pathData = nodes(pathData), fill = SolidColor(Color.Black))
            .build()

    private fun stroked(name: String, pathData: String, width: Float): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = nodes(pathData),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            .build()
}
