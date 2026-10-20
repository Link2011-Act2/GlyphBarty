package jp.linkserver.glyphvisualizer.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import jp.linkserver.glyphvisualizer.ui.theme.NothingDotFontFamily
import jp.linkserver.pyrite.theme.PyriteStyle
import jp.linkserver.pyrite.theme.PyriteTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class PyriteFontsIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    private fun family(label: String): FontFamily? {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single().layoutInput.style.fontFamily
    }

    @Test fun appFontsStayInTheirOriginalSemanticRoles() {
        AppLogger.init(InstrumentationRegistry.getInstrumentation().targetContext)
        compose.setContent {
            var about by remember { mutableStateOf(false) }
            GlyphBartyTheme(nothingStyle = true, darkTheme = false) {
                if (about) AboutScreen({}, nothingStyleEnabled = true)
                else FixtureSettings(true, true, {}, { about = true })
            }
        }
        assertEquals(NTypeFontFamily, family(text(R.string.settings_screen_title)))
        assertEquals(FontFamily.SansSerif, family(text(R.string.settings_language_title)))
        assertEquals(FontFamily.SansSerif, family(text(R.string.settings_category_general)))
        compose.onNodeWithText(text(R.string.settings_about_title)).performScrollTo().performClick()
        assertEquals(NTypeFontFamily, family(text(R.string.about_screen_title)))
        assertEquals(NTypeFontFamily, family(text(R.string.app_name)))
        assertEquals(NTypeFontFamily, family("IntDev"))
        assertEquals(NothingDotFontFamily, family("v2.1.1"))
        assertEquals(FontFamily.SansSerif, family(text(R.string.about_overview_build_title)))
        compose.onNodeWithText(text(R.string.about_overview_version_title)).performScrollTo().performClick()
        assertEquals(FontFamily.SansSerif, family(text(R.string.about_version_details_title)))
    }

    @Test fun palettesMatchPublishedPyriteInBothStylesAndModes() {
        var dark by mutableStateOf(false)
        var nothing by mutableStateOf(true)
        var dynamic by mutableStateOf(false)
        var expected = ""
        var actual = ""
        compose.setContent {
            PyriteTheme(style = if (nothing) PyriteStyle.Nothing else PyriteStyle.Material3Expressive,
                darkTheme = dark, dynamicColor = dynamic) {
                val colors = MaterialTheme.colorScheme
                SideEffect { expected = colors.toString() }
            }
            GlyphBartyTheme(nothingStyle = nothing, darkTheme = dark, dynamicColor = dynamic) {
                val colors = MaterialTheme.colorScheme
                SideEffect { actual = colors.toString() }
            }
        }
        for (sourceStyle in listOf(true, false)) for (night in listOf(false, true)) for (wallpaper in listOf(false, true)) {
            compose.runOnIdle { nothing = sourceStyle; dark = night; dynamic = wallpaper }
            compose.runOnIdle { assertEquals("nothing=$nothing dark=$dark dynamic=$dynamic", expected, actual) }
        }
    }
}
