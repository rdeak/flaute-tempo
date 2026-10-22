package hr.bertlab.fluteassistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CoralLight,
    onPrimary = OnCoralLight,
    primaryContainer = CoralContainerLight,
    onPrimaryContainer = OnCoralContainerLight,
    secondary = TealLight,
    onSecondary = OnTealLight,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = OnTealContainerLight,
    tertiary = SunLight,
    onTertiary = OnSunLight,
    tertiaryContainer = SunContainerLight,
    onTertiaryContainer = OnSunContainerLight,
    background = CreamSurface,
    onBackground = OnCreamSurface,
    surface = CreamSurface,
    onSurface = OnCreamSurface,
    surfaceVariant = PeachSurfaceVariant,
    onSurfaceVariant = OnPeachSurfaceVariant,
    outline = OutlineLight,
    outlineVariant = PeachSurfaceVariant,
    error = StopRedLight,
    onError = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF4E6),
    surfaceContainer = Color(0xFFFFEFDC),
    surfaceContainerHigh = Color(0xFFFFE9D2),
    surfaceContainerHighest = Color(0xFFFFE3C8),
    surfaceDim = Color(0xFFF3E4D3),
    surfaceBright = CreamSurface,
    inverseSurface = OnCreamSurface,
    inverseOnSurface = Color(0xFFFFF4E6),
    inversePrimary = CoralDark,
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralDark,
    onPrimary = OnCoralDark,
    primaryContainer = CoralContainerDark,
    onPrimaryContainer = OnCoralContainerDark,
    secondary = TealDark,
    onSecondary = OnTealDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = OnTealContainerDark,
    tertiary = SunDark,
    onTertiary = OnSunDark,
    tertiaryContainer = SunContainerDark,
    onTertiaryContainer = OnSunContainerDark,
    background = NavySurface,
    onBackground = OnNavySurface,
    surface = NavySurface,
    onSurface = OnNavySurface,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = OnNavySurfaceVariant,
    outline = OutlineDark,
    outlineVariant = NavySurfaceVariant,
    error = StopRedDark,
    onError = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFF161424),
    surfaceContainerLow = Color(0xFF26233A),
    surfaceContainer = Color(0xFF2C2942),
    surfaceContainerHigh = Color(0xFF363250),
    surfaceContainerHighest = Color(0xFF403B5C),
    surfaceDim = NavySurface,
    surfaceBright = Color(0xFF3C3852),
    inverseSurface = OnNavySurface,
    inverseOnSurface = OnCreamSurface,
    inversePrimary = CoralLight,
)

/**
 * Material 3 theme with a fixed playful palette (see [Color.kt]), extra-round shapes and
 * big, bold type. Dark mode follows the system setting.
 */
@Composable
fun FluteAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
