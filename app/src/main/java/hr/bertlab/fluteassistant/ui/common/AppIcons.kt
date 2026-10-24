package hr.bertlab.fluteassistant.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * The few icons that are not emoji, hand-built so the app does not depend on the (large)
 * extended Material icon set: the tuner's ‹ › buttons. Paths are the standard 24dp Material
 * definitions. Tinting is applied by [androidx.compose.material3.Icon].
 */
object AppIcons {
    val ChevronLeft: ImageVector by lazy { filled("chevron_left", "M15.41,7.41L14,6l-6,6 6,6 1.41,-1.41L10.83,12z") }
    val ChevronRight: ImageVector by lazy { filled("chevron_right", "M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z") }

    private fun nodes(pathData: String) = PathParser().parsePathString(pathData).toNodes()

    private fun filled(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(pathData = nodes(pathData), fill = SolidColor(Color.Black))
            .build()
}
