package jp.linkserver.glyphvisualizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import jp.linkserver.pyrite.theme.PyriteTheme
import jp.linkserver.pyrite.theme.PyriteStyle
import jp.linkserver.pyrite.theme.PyriteFonts
import androidx.compose.ui.graphics.Color

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

@Composable
fun GlyphBartyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    nothingStyle: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    PyriteTheme(
        style = PyriteStyle.Nothing,
        darkTheme = darkTheme,
        fonts = if (nothingStyle) {
            PyriteFonts(title = NTypeFontFamily, accent = NothingDotFontFamily)
        } else PyriteFonts()
    ) {
        MaterialTheme(
            // Nothing inherits the published palette. The source Material option
            // intentionally retains its own colors, typography and component geometry.
            colorScheme = if (nothingStyle) MaterialTheme.colorScheme else colorScheme,
            typography = if (nothingStyle) NothingTypography else Typography,
            shapes = Shapes(),
            content = content
        )
    }
}
