package jp.linkserver.glyphvisualizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = NightAccent,
    onPrimary = Color(0xFF4D2100),
    primaryContainer = Color(0xFF723507),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFE2C1FF),
    onSecondary = Color(0xFF3E2456),
    secondaryContainer = Color(0xFF563B6E),
    onSecondaryContainer = Color(0xFFF5DEFF),
    tertiary = Color(0xFFFFCF88),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF6B3D00),
    onTertiaryContainer = Color(0xFFFFE1B4),
    background = CocoaSurface,
    surface = CocoaSurface,
    surfaceContainer = Color(0xFF1E1613),
    surfaceContainerHigh = CocoaSurfaceHigh,
    surfaceContainerHighest = CocoaSurfaceHighest,
    surfaceDim = Color(0xFF130D0B),
    onSurface = Color(0xFFF8E7DE),
    onSurfaceVariant = Color(0xFFD9C4BC),
    outline = Color(0xFF8E7B74),
    outlineVariant = Color(0xFF4A3D38)
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = Color(0xFFFFF8F6),
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3D0F05),
    secondary = PlumAccent,
    onSecondary = Color(0xFFFFF7FD),
    secondaryContainer = Color(0xFFF2DAFF),
    onSecondaryContainer = Color(0xFF2D173F),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF442300),
    tertiaryContainer = Color(0xFFFFE1B7),
    onTertiaryContainer = Color(0xFF2E1700),
    background = MistSurface,
    surface = MistSurface,
    surfaceContainer = MistContainer,
    surfaceContainerHigh = Color(0xFFFCECE5),
    surfaceContainerHighest = Color(0xFFF7E0D8),
    surfaceDim = Color(0xFFE7D3CB),
    onSurface = Ink,
    onSurfaceVariant = Slate,
    outline = Color(0xFFA68E84),
    outlineVariant = Color(0xFFE7D2CA)
)

private val NothingDarkColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    primaryContainer = Color(0xFFE7E7E7),
    onPrimaryContainer = NothingBlack,
    secondary = Color(0xFFD8D8D8),
    onSecondary = NothingBlack,
    secondaryContainer = Color(0xFF1B1B1B),
    onSecondaryContainer = NothingWhite,
    tertiary = NothingRed,
    onTertiary = NothingWhite,
    tertiaryContainer = Color(0xFF2B1113),
    onTertiaryContainer = Color(0xFFFFDAD9),
    background = NothingBlack,
    surface = NothingBlack,
    surfaceContainer = Color(0xFF050505),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF181818),
    surfaceDim = Color(0xFF030303),
    onSurface = NothingWhite,
    onSurfaceVariant = Color(0xFFD0D0D0),
    outline = Color(0xFF707070),
    outlineVariant = Color(0xFF3A3A3A)
)

private val NothingLightColorScheme = lightColorScheme(
    primary = NothingBlack,
    onPrimary = NothingWhite,
    primaryContainer = NothingBlack,
    onPrimaryContainer = NothingWhite,
    secondary = NothingBlack,
    onSecondary = NothingWhite,
    secondaryContainer = Color(0xFFE2E2E2),
    onSecondaryContainer = NothingBlack,
    tertiary = NothingRed,
    onTertiary = NothingWhite,
    tertiaryContainer = Color(0xFFFFDAD9),
    onTertiaryContainer = Color(0xFF410005),
    background = NothingLightSurface,
    surface = NothingLightSurface,
    surfaceContainer = NothingWhite,
    surfaceContainerHigh = NothingWhite,
    surfaceContainerHighest = NothingWhite,
    surfaceDim = Color(0xFFD6D6D6),
    onSurface = NothingBlack,
    onSurfaceVariant = Color(0xFF565656),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFE7E7E7)
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

@Composable
fun GlyphBartyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    nothingStyle: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        nothingStyle && darkTheme -> NothingDarkColorScheme
        nothingStyle -> NothingLightColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = if (nothingStyle) NothingTypography else Typography,
        shapes = if (nothingStyle) Shapes() else ExpressiveShapes,
        content = content
    )
}
