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
    val ChevronLeft: ImageVector by lazy { filled("chevron_left", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z") }
    val ChevronRight: ImageVector by lazy { filled("chevron_right", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z") }
    /** Two small chevrons, up and down: the hint that the tempo number can be scrolled. */
    val UnfoldMore: ImageVector by lazy {
        filled(
            "unfold_more",
            "M12,5.83L15.17,9l1.41,-1.41L12,3 7.41,7.59 8.83,9 12,5.83zM12,18.17L8.83,15l-1.41,1.41L12,21l4.59,-4.59L15.17,15 12,18.17z",
        )
    }
    val PlayArrow: ImageVector by lazy { filled("play_arrow", "M8,5v14l11,-7z") }
    val Stop: ImageVector by lazy { filled("stop", "M6,6h12v12H6z") }
    /** A speaker with two sound waves: the tuner's button that sounds the chosen note. */
    val VolumeUp: ImageVector by lazy {
        filled(
            "volume_up",
            "M3,9v6h4l5,5V4L7,9H3zM16.5,12c0,-1.77 -1.02,-3.29 -2.5,-4.03v8.05c1.48,-0.73 2.5,-2.25 2.5,-4.02zM14,3.23v2.06" +
                "c2.89,0.86 5,3.54 5,6.71s-2.11,5.85 -5,6.71v2.06c4.01,-0.91 7,-4.49 7,-8.77s-2.99,-7.86 -7,-8.77z",
        )
    }
    val Mic: ImageVector by lazy {
        filled(
            "mic",
            "M12,14c1.66,0 2.99,-1.34 2.99,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 " +
                "-2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z",
        )
    }

    /** A list with two ticks: the game's button that opens the note picker. */
    val Checklist: ImageVector by lazy {
        filled(
            "checklist",
            "M22,7h-9v2h9V7zM22,15h-9v2h9V15zM5.54,11L2,7.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,11z" +
                "M5.54,19L2,15.46l1.41,-1.41l2.12,2.12l4.24,-4.24l1.41,1.41L5.54,19z",
        )
    }
    /** A tick: closes the note picker. */
    val Check: ImageVector by lazy { filled("check", "M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41,-1.41z") }

    /** An eighth note: the note reading tab. */
    val MusicNote: ImageVector by lazy {
        filled("music_note", "M12,3v10.55c-0.59,-0.34 -1.27,-0.55 -2,-0.55 -2.21,0 -4,1.79 -4,4s1.79,4 4,4 4,-1.79 4,-4V7h4V3h-6z")
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
