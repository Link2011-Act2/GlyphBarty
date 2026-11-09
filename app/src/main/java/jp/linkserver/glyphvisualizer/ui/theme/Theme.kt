package jp.linkserver.glyphvisualizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import jp.linkserver.pyrite.theme.PyriteTheme
import jp.linkserver.pyrite.theme.PyriteStyle
import jp.linkserver.pyrite.theme.PyriteFonts

@Composable
fun GlyphBartyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    nothingStyle: Boolean = false,
    content: @Composable () -> Unit
) {
    PyriteTheme(
        style = if (nothingStyle) PyriteStyle.Nothing else PyriteStyle.Material3Expressive,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        fonts = if (nothingStyle) PyriteFonts(title = NTypeFontFamily, accent = NothingDotFontFamily) else PyriteFonts()
    ) {
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = if (nothingStyle) NothingTypography else MaterialTheme.typography,
            shapes = if (nothingStyle) Shapes() else MaterialTheme.shapes,
            motionScheme = MaterialTheme.motionScheme,
            content = {
                CompositionLocalProvider(jp.linkserver.goldy.compose.LocalUpdateCardStyle provides
                    jp.linkserver.goldy.compose.UpdateCardStyle(
                        containerColor=if(darkTheme) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape=MaterialTheme.shapes.large)) { content() }
            }
        )
    }
}
