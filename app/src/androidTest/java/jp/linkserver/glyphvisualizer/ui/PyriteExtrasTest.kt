package jp.linkserver.glyphvisualizer.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Offline screen: hardware support and callbacks are owned by the host. */
class PyriteExtrasTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun nothingLight() = verify(nothing = true, dark = false)
    @Test fun nothingDark() = verify(nothing = true, dark = true)
    @Test fun m3eLight() = verify(nothing = false, dark = false)
    @Test fun m3eDark() = verify(nothing = false, dark = true)

    private fun verify(nothing: Boolean, dark: Boolean) {
        val args = InstrumentationRegistry.getArguments()
        val before = args.getString("extrasPhase", "after") == "before"
        val prefix = "${if (nothing) "nothing" else "m3e"}-${if (dark) "dark" else "light"}"
        val directory = File(compose.activity.getExternalFilesDir(null),
            "pyrite-extras/${if (before) "before" else "after"}").apply { mkdirs() }
        var supported by mutableStateOf(true)
        var changes = 0
        var menus = 0
        var settings = 0
        val restore = StateRestorationTester(compose)
        restore.setContent {
            var checked by rememberSaveable { mutableStateOf(false) }
            val system = LocalConfiguration.current
            val config = remember(system) { Configuration(system).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            } }
            CompositionLocalProvider(LocalConfiguration provides config) {
                GlyphBartyTheme(darkTheme = dark, nothingStyle = nothing) {
                    ExtrasScreen(nothing, checked, supported,
                        { checked = it; changes++ }, { menus++ }, { settings++ })
                }
            }
        }
        fun text(id: Int) = compose.activity.getString(id)
        fun toggle() = compose.onNode(isToggleable())
        fun capture(state: String) {
            compose.mainClock.advanceTimeBy(1_200)
            compose.waitForIdle()
            // Android's RenderThread ripple uses wall time, independently of the Compose clock.
            val settledAt = SystemClock.uptimeMillis() + 600
            compose.waitUntil(timeoutMillis = 2_000) { SystemClock.uptimeMillis() >= settledAt }
            compose.waitForIdle()
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(directory, "$prefix-$state.png").outputStream().use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
        fun tapSwitch() {
            // Before: Switch itself. After: right-hand switch inside the single merged row.
            toggle().performTouchInput {
                click(if (before) center else Offset(width - 46 * density, centerY))
            }
        }
        toggle().assertIsEnabled().assertIsOff()
        compose.onAllNodes(isToggleable()).assertCountEquals(1)
        capture("supported-off")
        val titleLayout = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text(R.string.menu_extras), useUnmergedTree = true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(titleLayout) }
        if (nothing) assertEquals(NTypeFontFamily, titleLayout.single().layoutInput.style.fontFamily)
        val bodyLayout = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text(R.string.extras_battery_glyph_title), useUnmergedTree = true)
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(bodyLayout) }
        val bodyFamily = bodyLayout.single().layoutInput.style.fontFamily
        if (nothing) assertEquals(FontFamily.SansSerif, bodyFamily)
        else assertTrue(bodyFamily == null || bodyFamily == FontFamily.SansSerif)

        tapSwitch()
        toggle().assertIsOn()
        compose.runOnIdle { assertEquals(1, changes) }
        capture("supported-on")
        if (!before) {
            // The title is part of the one semantic Switch, and tapping its text toggles once.
            compose.onNodeWithText(text(R.string.extras_battery_glyph_title)).assertIsToggleable()
            toggle().performTouchInput { click(Offset(30 * density, centerY)) }
            toggle().assertIsOff()
            compose.runOnIdle { assertEquals(2, changes) }
            toggle().performClick()
            toggle().assertIsOn()
            compose.runOnIdle { assertEquals(3, changes) }
        }
        val retainedChanges = changes
        restore.emulateSavedInstanceStateRestore()
        toggle().assertIsOn()
        compose.runOnIdle { assertEquals(retainedChanges, changes) }
        compose.onNodeWithContentDescription(text(R.string.cd_menu)).performClick()
        compose.onNodeWithContentDescription(text(R.string.cd_settings)).performClick()
        compose.runOnIdle { assertEquals(1, menus); assertEquals(1, settings) }

        compose.runOnIdle { supported = false }
        toggle().assertIsNotEnabled().assertIsOff()
        compose.onNodeWithText(text(R.string.extras_battery_glyph_unsupported)).assertIsDisplayed()
        tapSwitch()
        if (!before) toggle().performTouchInput { click(Offset(30 * density, centerY)) }
        compose.runOnIdle { assertEquals(retainedChanges, changes) }
        capture("unsupported-stored-on")
        compose.runOnIdle { supported = true }
        toggle().assertIsOn() // Becoming unsupported must not overwrite the saved preference.
        tapSwitch()
        toggle().assertIsOff()
        compose.runOnIdle { supported = false }
        toggle().assertIsNotEnabled().assertIsOff()
        capture("unsupported-stored-off")
    }
}
