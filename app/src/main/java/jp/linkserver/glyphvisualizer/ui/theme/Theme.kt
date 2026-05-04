package jp.linkserver.glyphvisualizer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NightAccent,
    onPrimary = Color(0xFF492100),
    primaryContainer = Color(0xFF6B3200),
    onPrimaryContainer = Color(0xFFFFDBC3),
    secondary = Color(0xFFE6BEA6),
    secondaryContainer = Color(0xFF584134),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFFFC95E),
    tertiaryContainer = Color(0xFF5E4300),
    onTertiaryContainer = Color(0xFFFFE08C),
    background = NightSurface,
    surface = NightSurface,
    surfaceContainer = Color(0xFF221C19),
    surfaceContainerHigh = Color(0xFF2A2421),
    surfaceContainerHighest = Color(0xFF352E2A),
    surfaceDim = Color(0xFF120E0D),
    onSurface = Color(0xFFF4DFD5),
    onSurfaceVariant = Color(0xFFD8C2B8)
)

private val LightColorScheme = lightColorScheme(
    primary = EmberSeed,
    onPrimary = Color(0xFFFFF7F2),
    primaryContainer = Color(0xFFFFDCC8),
    onPrimaryContainer = Color(0xFF391300),
    secondary = Slate,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFDCD1),
    onSecondaryContainer = Color(0xFF251916),
    tertiary = EmberAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEA6),
    onTertiaryContainer = Color(0xFF2E1800),
    background = Fog,
    surface = Fog,
    surfaceContainer = Color(0xFFFFF8F4),
    surfaceContainerHigh = Color(0xFFFCEEE5),
    surfaceContainerHighest = Color(0xFFF6E3D7),
    surfaceDim = Color(0xFFE6D2C3),
    onSurface = Ink,
    onSurfaceVariant = Slate
)

@Composable
fun GlyphBartyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
