package jp.linkserver.glyphvisualizer.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.update.AppUpdateInfo
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Stable offline Settings/About fixture, shared by pre/post migration screenshots. */
class PyriteMigrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)

    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val phase = InstrumentationRegistry.getArguments().getString("migrationPhase", "after")
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "pyrite-migration/$phase").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun automaticToggle(): SemanticsNodeInteraction {
        val title = compose.onNodeWithText(text(R.string.settings_automatic_update_check_title)).fetchSemanticsNode().boundsInRoot
        val toggles = compose.onAllNodes(isToggleable())
        val nodes = toggles.fetchSemanticsNodes()
        val index = nodes.indices.minBy { kotlin.math.abs(nodes[it].boundsInRoot.center.y - title.center.y) }
        return toggles[index]
    }

    private fun verify(dark: Boolean, nothing: Boolean) {
        val prefix = "${if (nothing) "nothing" else "material"}-${if (dark) "dark" else "light"}"
        AppLogger.init(InstrumentationRegistry.getInstrumentation().targetContext)
        var changes = 0
        compose.setContent {
            val system = LocalConfiguration.current
            val config = remember(system, dark) { Configuration(system).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            } }
            CompositionLocalProvider(LocalConfiguration provides config) {
            var about by rememberSaveable { mutableStateOf(false) }
            var automatic by rememberSaveable { mutableStateOf(true) }
            GlyphBartyTheme(darkTheme = dark, nothingStyle = nothing) {
                if (about) AboutScreen(onBack = { about = false }, nothingStyleEnabled = nothing)
                else FixtureSettings(nothing, automatic, { automatic = it; changes++ }, { about = true })
            }
            }
        }
        compose.onNodeWithText(text(R.string.settings_screen_title)).assertIsDisplayed()
        capture("$prefix-settings-top")
        compose.onNodeWithText(text(R.string.settings_automatic_update_check_title)).performScrollTo()
        capture("$prefix-settings-updates")
        // The original UI exposes the Switch separately; the migrated row merges it.
        val switch = automaticToggle()
        switch.performClick()
        compose.runOnIdle { assertEquals(1, changes) }
        compose.onNodeWithText(text(R.string.settings_about_title)).performScrollTo().performClick()
        capture("$prefix-about-top")
        compose.onNodeWithText(text(R.string.about_overview_channel_title)).performScrollTo().performClick()
        compose.onNodeWithText(text(android.R.string.ok)).assertIsDisplayed().performClick()
        compose.onNodeWithText(text(R.string.about_overview_version_title)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.about_version_details_title)).assertIsDisplayed()
        compose.onNodeWithText(text(android.R.string.ok)).performClick()
        compose.onNodeWithText(text(R.string.about_oss_open)).performScrollTo()
        capture("$prefix-about-actions")
        compose.onNodeWithContentDescription(text(R.string.cd_back)).performClick()
        compose.onNodeWithText(text(R.string.settings_automatic_update_check_title)).performScrollTo()
        automaticToggle().assertIsOff()
    }

    @Test fun nothingLight() = verify(false, true)
    @Test fun nothingDark() = verify(true, true)
    @Test fun materialLight() = verify(false, false)
    @Test fun materialDark() = verify(true, false)

    @Test fun updateScreensKeepTheirExistingDesign() {
        var dark by mutableStateOf(false)
        var nothing by mutableStateOf(true)
        val update = AppUpdateInfo("v2.2.0-Beta1", "Beta", "Offline release notes.\n更新内容の表示確認。",
            "https://example.com/release", null, null)
        compose.setContent {
            GlyphBartyTheme(darkTheme = dark, nothingStyle = nothing) {
                UpdateOverviewScreen(update, nothing, {})
            }
        }
        for (style in listOf(true, false)) for (night in listOf(false, true)) {
            compose.runOnIdle { nothing = style; dark = night }
            capture("${if (style) "nothing" else "material"}-${if (night) "dark" else "light"}-update")
        }
    }
}

@Composable
internal fun FixtureSettings(nothing: Boolean, automatic: Boolean, onAutomatic: (Boolean) -> Unit, onAbout: () -> Unit) {
    SettingsScreen(
        onBack = {},
        onAbout = onAbout,
        onExperimentalFeatures = {},
        mediaProjectionEnabled = false,
        onMediaProjectionEnabledChanged = {},
        glyphMeterPreviewEnabled = true,
        onGlyphMeterPreviewEnabledChanged = {},
        meterVisibleEnabled = true,
        onMeterVisibleEnabledChanged = {},
        lightweightMeterEnabled = false,
        onLightweightMeterEnabledChanged = {},
        spectrumMeterEnabled = false,
        onSpectrumMeterEnabledChanged = {},
        onMeterStyleChanged = { _, _, _, _ -> },
        nativeMeterViewEnabled = true,
        onNativeMeterViewEnabledChanged = {},
        automaticUpdateCheckEnabled = automatic,
        onAutomaticUpdateCheckEnabledChanged = onAutomatic,
        syncWithNothingOsGlyphSettingEnabled = false,
        onSyncWithNothingOsGlyphSettingEnabledChanged = {},
        mediaPlaybackOnlyEnabled = false,
        onMediaPlaybackOnlyEnabledChanged = {},
        experimentalVisualizerStabilizationEnabled = false,
        onExperimentalVisualizerStabilizationEnabledChanged = {},
        experimentalVisualizerSignalWatchdogEnabled = true,
        onExperimentalVisualizerSignalWatchdogEnabledChanged = {},
        showAutoEnablePhone1GlyphDebugOnStart = false,
        autoEnablePhone1GlyphDebugOnStart = true,
        onAutoEnablePhone1GlyphDebugOnStartChanged = {},
        experimentalMainUiEnabled = true,
        onExperimentalMainUiEnabledChanged = {},
        detailedHomeEnabled = false,
        onDetailedHomeEnabledChanged = {},
        nothingStyleEnabled = nothing,
        onNothingStyleEnabledChanged = {},
    )
}
