package jp.linkserver.glyphvisualizer.ui

import android.view.View
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.CaptureUiState
import jp.linkserver.glyphvisualizer.MainActivity
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.SettingsPreferences
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Actual app navigation/recreation on an unsupported emulator; no Glyph/audio capture is started. */
class PyriteExtrasActivityTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun nothingExtrasSurvivesRecreation() = verify(true)
    @Test fun m3eExtrasSurvivesRecreation() = verify(false)

    private fun verify(nothing: Boolean) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SettingsPreferences.save(context, CaptureUiState(
            nothingStyleEnabled = nothing, batteryGlyphEnabled = true,
            automaticUpdateCheckEnabled = false, experimentalMainUiEnabled = true))
        SettingsPreferences.markInitialSetupCompleted(context)
        SettingsPreferences.markNotificationPermissionPromptShown(context)
        fun text(id: Int) = context.getString(id)
        val log = File(context.getExternalFilesDir(null),
            "pyrite-extras/after/geometry-${if (nothing) "nothing" else "m3e"}.txt")
        log.parentFile?.mkdirs()
        log.writeText("")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithContentDescription(text(R.string.cd_menu)).performClick()
            compose.onNodeWithText(text(R.string.menu_extras)).performClick()
            compose.onNodeWithText(text(R.string.extras_battery_glyph_unsupported)).assertIsDisplayed()
            val toggle = compose.onNode(isToggleable())
            toggle.assertIsNotEnabled().assertIsOff()
            toggle.performTouchInput { click(center) }
            assertTrue(SettingsPreferences.load(context).batteryGlyphEnabled)
            fun geometry(label: String): String {
                compose.waitForIdle()
                val row = compose.onNode(isToggleable()).fetchSemanticsNode()
                val scroll = compose.onNode(hasScrollAction()).fetchSemanticsNode()
                val axis = scroll.config[SemanticsProperties.VerticalScrollAxisRange]
                var window = ""
                scenario.onActivity { activity ->
                    val content = activity.findViewById<View>(android.R.id.content)
                    val insets = ViewCompat.getRootWindowInsets(content)?.getInsets(WindowInsetsCompat.Type.systemBars())
                    window = "${content.width}x${content.height},insets=$insets"
                }
                return "row=${row.boundsInRoot},viewport=${scroll.boundsInRoot},scroll=${axis.value()},$window"
                    .also { log.appendText("$label $it\n") }
            }
            val before = geometry("before")
            compose.onNodeWithContentDescription(text(R.string.cd_settings)).performClick()
            compose.onNodeWithText(text(R.string.settings_screen_title)).assertIsDisplayed()
            compose.onNodeWithContentDescription(text(R.string.cd_back)).performClick()
            compose.onNodeWithText(text(R.string.extras_battery_glyph_unsupported)).assertIsDisplayed()
            assertEquals(before, geometry("return-from-settings"))
            repeat(2) {
                scenario.recreate()
                compose.onNodeWithText(text(R.string.extras_battery_glyph_unsupported)).assertIsDisplayed()
                compose.onNode(isToggleable()).assertIsNotEnabled().assertIsOff()
                assertEquals(before, geometry("recreate-$it"))
                assertTrue(SettingsPreferences.load(context).batteryGlyphEnabled)
            }
            compose.onNodeWithContentDescription(text(R.string.cd_menu)).performClick()
            compose.onNode(hasText(text(R.string.menu_extras)) and hasClickAction()).assertIsDisplayed()
        }
    }
}
