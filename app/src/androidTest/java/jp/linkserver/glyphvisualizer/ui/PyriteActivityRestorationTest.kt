package jp.linkserver.glyphvisualizer.ui

import android.util.Log
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Real Activity/Window recreation; never starts the visualizer or media capture. */
class PyriteActivityRestorationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun aboutScrollAndInsetsSurviveTwoActivityRecreations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        SettingsPreferences.save(context, CaptureUiState(nothingStyleEnabled = true, automaticUpdateCheckEnabled = false))
        SettingsPreferences.markInitialSetupCompleted(context)
        SettingsPreferences.markNotificationPermissionPromptShown(context)
        val phase = InstrumentationRegistry.getArguments().getString("migrationPhase", "after")
        val log = File(context.getExternalFilesDir(null), "pyrite-migration/$phase/geometry.txt")
        log.parentFile?.mkdirs()
        log.writeText("")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.onNodeWithContentDescription(context.getString(R.string.cd_settings)).performClick()
            compose.onNodeWithText(context.getString(R.string.settings_home_screen_title)).performScrollTo().performClick()
            compose.onAllNodes(isSelectable()).onLast().performClick()
            assertEquals(true, SettingsPreferences.load(context).detailedHomeEnabled)
            compose.onNodeWithText(context.getString(R.string.settings_about_title)).performScrollTo().performClick()
            compose.onNodeWithText(context.getString(R.string.about_oss_open)).performScrollTo().assertIsDisplayed()
            fun geometry(label: String): String {
                compose.waitForIdle()
                val scroll = compose.onNode(hasScrollAction()).fetchSemanticsNode()
                val axis = scroll.config[SemanticsProperties.VerticalScrollAxisRange]
                var window = ""
                scenario.onActivity { activity ->
                    val content = activity.findViewById<View>(android.R.id.content)
                    val bars = ViewCompat.getRootWindowInsets(content)?.getInsets(WindowInsetsCompat.Type.systemBars())
                    val location = IntArray(2).also(content::getLocationOnScreen)
                    window = "content=${content.width}x${content.height}@${location.toList()},bars=$bars"
                }
                val result = "scroll=${axis.value()},max=${axis.maxValue()},viewport=${scroll.boundsInRoot},$window"
                log.appendText("$label $result\n")
                Log.i("PyriteGlyphGeometry", "$label $result")
                return result
            }
            val before = geometry("before")
            repeat(2) { index ->
                scenario.recreate()
                compose.onNodeWithText(context.getString(R.string.about_oss_open)).assertIsDisplayed()
                val after = geometry("recreate-$index")
                if (phase != "before") assertEquals(before, after)
                assertEquals(true, SettingsPreferences.load(context).detailedHomeEnabled)
            }
            if (phase != "before") {
                compose.onNodeWithContentDescription(context.getString(R.string.cd_back)).performClick()
                compose.onNodeWithText(context.getString(R.string.settings_about_title))
                    .assertIsDisplayed().performClick()
                compose.onNodeWithText(context.getString(R.string.about_oss_open)).assertIsDisplayed()
                assertEquals(before, geometry("return-from-settings"))
            }
        }
    }
}
