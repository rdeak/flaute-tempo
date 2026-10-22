package hr.bertlab.fluteassistant.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Hand-built icons so the app does not depend on the (large) extended Material icon set.
 * Paths for the Material symbols are the standard 24dp definitions; the metronome and tuning
 * fork are simple strokes drawn for this app. Tinting is applied by [androidx.compose.material3.Icon].
 */
object AppIcons {
    val Add: ImageVector by lazy { filled("add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val Remove: ImageVector by lazy { filled("remove", "M19,13H5v-2h14v2z") }
    val PlayArrow: ImageVector by lazy { filled("play_arrow", "M8,5v14l11,-7z") }
    val Stop: ImageVector by lazy { filled("stop", "M6,6h12v12H6z") }
    val Delete: ImageVector by lazy {
        filled("delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z")
    }
    val Save: ImageVector by lazy {
        filled(
            "save",
            "M17,3L5,3c-1.11,0 -2,0.9 -2,2v14c0,1.1 0.89,2 2,2h14c1.1,0 2,-0.9 2,-2L21,7l-4,-4zM12,19c-1.66,0 -3,-1.34 " +
                "-3,-3s1.34,-3 3,-3 3,1.34 3,3 -1.34,3 -3,3zM15,9L5,9L5,5h10v4z",
        )
    }
    val Bookmark: ImageVector by lazy {
        filled("bookmark", "M17,3H7c-1.1,0 -1.99,0.9 -1.99,2L5,21l7,-3 7,3V5c0,-1.1 -0.9,-2 -2,-2z")
    }
    val Mic: ImageVector by lazy {
        filled(
            "mic",
            "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 " +
                "-2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
        )
    }
    val VolumeUp: ImageVector by lazy {
        filled(
            "volume_up",
            "M3,9v6h4l5,5V4L7,9H3zm13.5,3c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06" +
                "c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z",
        )
    }

    /** Pyramid-shaped metronome body with a pendulum. */
    val Metronome: ImageVector by lazy {
        stroked("metronome", "M8.5,3 L15.5,3 L19.5,21 L4.5,21 Z", "M12,20 L16.5,7.5", "M6.5,16 L17.5,16")
    }

    /** U-shaped tuning fork with a stem and foot. */
    val TuningFork: ImageVector by lazy {
        stroked("tuning_fork", "M8,2 V8 a4,4 0 0 0 8,0 V2", "M12,12 V22", "M9,22 H15")
    }

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()

    private fun filled(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(pathData = nodes(pathData), fill = SolidColor(Color.Black))
            .build()

    private fun stroked(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        for (path in paths) {
            builder.addPath(
                pathData = nodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
