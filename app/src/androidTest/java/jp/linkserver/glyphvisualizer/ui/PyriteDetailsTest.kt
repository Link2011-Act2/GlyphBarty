package jp.linkserver.glyphvisualizer.ui

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import jp.linkserver.glyphvisualizer.*
import jp.linkserver.glyphvisualizer.R
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.ui.theme.GlyphBartyTheme
import jp.linkserver.glyphvisualizer.ui.theme.NTypeFontFamily
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Offline Details fixture: control callbacks are counted; no device/audio commands are executed. */
class PyriteDetailsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    private fun family(label: String): FontFamily? {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.style.fontFamily
    }
    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(1200)
        compose.waitForIdle()
        val phase = InstrumentationRegistry.getArguments().getString("detailsPhase", "after")
        val root = File(compose.activity.getExternalFilesDir(null), "pyrite-details/$phase").apply { mkdirs() }
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(root, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun verify(nothing: Boolean, dark: Boolean) {
        var tab by mutableStateOf(ExperimentalDetailsTab.LIVE)
        var state by mutableStateOf(CaptureUiState())
        var changes=0; var starts=0; var resets=0; var imports=0; var exports=0; var reverses=0
        AppLogger.init(compose.activity)
        compose.setContent {
            val system=LocalConfiguration.current
            val config=remember(system,dark) { Configuration(system).apply {
                uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    (if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
            } }
            CompositionLocalProvider(LocalConfiguration provides config) {
                GlyphBartyTheme(nothingStyle=nothing,darkTheme=dark) {
                    key(tab) {
                        DetailsFixture(nothing,tab,state,
                            onSensitivity={ state=state.copy(sensitivity=it);changes++;SettingsPreferences.save(compose.activity,state) },
                            onReverse={ state=state.copy(reverseDirection=it);reverses++ },
                            onStart={starts++},onReset={resets++},onImport={imports++},onExport={exports++})
                    }
                }
            }
        }
        val prefix="${if(nothing) "nothing" else "material"}-${if(dark) "dark" else "light"}"
        capture("$prefix-live")
        if (nothing) assertEquals(NTypeFontFamily, family("Test device"))
        compose.onNodeWithText(text(R.string.button_no_capture)).performClick()
        compose.runOnIdle { assertEquals(1,starts); tab=ExperimentalDetailsTab.TUNE }
        capture("$prefix-tune")
        if (nothing) assertEquals(FontFamily.SansSerif, family(text(R.string.param_sensitivity_title)))
        compose.onNodeWithText(text(R.string.param_sensitivity_title)).performScrollTo()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).onFirst()
            .performSemanticsAction(SemanticsActions.SetProgress) { assertTrue(it(2f)) }
        compose.runOnIdle { assertEquals(1,changes); assertEquals(2f,state.sensitivity,.001f)
            assertEquals(2f,SettingsPreferences.load(compose.activity).sensitivity,.001f) }
        compose.onNode(hasText(text(R.string.advanced_meter_title)) and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.param_auto_scale_window_title)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(text(R.string.settings_reset_button)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.dialog_cancel)).performClick()
        compose.runOnIdle { assertEquals(0,resets) }
        compose.onNodeWithText(text(R.string.settings_reset_button)).performScrollTo().performClick()
        compose.onNode(hasText(text(R.string.settings_reset_button)) and hasAnyAncestor(isDialog())).performClick()
        compose.runOnIdle { assertEquals(1,resets) }
        compose.onNodeWithText(text(R.string.settings_import_button)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.settings_export_button)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1,imports);assertEquals(1,exports);tab=ExperimentalDetailsTab.SYSTEM }
        capture("$prefix-system")
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        compose.runOnIdle { assertEquals(1,reverses);assertTrue(state.reverseDirection) }
    }
    @Test fun nothingLight()=verify(true,false)
    @Test fun nothingDark()=verify(true,true)
    @Test fun materialLight()=verify(false,false)
    @Test fun materialDark()=verify(false,true)
}

@Composable
private fun DetailsFixture(nothing: Boolean, tab: ExperimentalDetailsTab, state: CaptureUiState,
    onSensitivity: (Float) -> Unit, onReverse: (Boolean) -> Unit,
    onStart: () -> Unit, onReset: () -> Unit, onImport: () -> Unit, onExport: () -> Unit) {
    ExperimentalDetailsScreenContent(
        heroTitle = "Test device\nOffline fixture",
        statusText = "Idle",
        logMessage = null,
        isCapturing = state.isCapturing,
        startPending = false,
        latencyMs = state.latencyMs,
        sensitivity = state.sensitivity,
        noiseGate = state.noiseGate,
        dynamics = state.dynamics,
        outputGamma = state.outputGamma,
        toneFocus = state.toneFocus,
        smoothing = state.smoothing,
        autoScaleWindowSeconds = state.autoScaleWindowSeconds,
        autoScaleOffset = state.autoScaleOffset,
        reverseDirection = state.reverseDirection,
        glyphMode = GlyphPatternRegistry.P3A_C_LINEAR,
        fillOtherGlyphLights = state.fillOtherGlyphLights,
        phone1ClassicCSplitEnabled = state.phone1ClassicCSplitEnabled,
        deviceProfile = GlyphDeviceProfile.PHONE3A,
        binaryMode = state.binaryMode,
        matrixSmoothMotionEnabled = state.matrixSmoothMotionEnabled,
        oscilloscopeAutoTimeAxisEnabled = state.oscilloscopeAutoTimeAxisEnabled,
        baseIndicatorEnabled = state.baseIndicatorEnabled,
        recordingLightIncluded = state.recordingLightIncluded,
        levelAutoScale = state.levelAutoScale,
        spectrumAutoScale = state.spectrumAutoScale,
        allBrightnessAutoScale = state.allBrightnessAutoScale,
        legacyAutoScaleEnabled = false,
        visualDynamics = .5f,
        visualDynamicsOverridden = false,
        onVisualDynamicsChanged = {},
        onVisualDynamicsChangeFinished = {},
        onVisualDynamicsReset = {},
        mediaProjectionEnabled = state.mediaProjectionEnabled,
        glyphMeterPreviewEnabled = state.glyphMeterPreviewEnabled,
        meterVisibleEnabled = false,
        lightweightMeterEnabled = state.lightweightMeterEnabled,
        spectrumMeterEnabled = state.spectrumMeterEnabled,
        nativeMeterViewEnabled = state.nativeMeterViewEnabled,
        nothingStyleEnabled = nothing,
        showPhone1GlyphDebugControls = false,
        isHome = false,
        initialTab = tab,
        onTabChanged = {},
        onResetParametersClick = onReset,
        onExportParametersClick = onExport,
        onImportParametersClick = onImport,
        onSensitivityChanged = onSensitivity,
        onNoiseGateChanged = {},
        onDynamicsChanged = {},
        onOutputGammaChanged = {},
        onSmoothingChanged = {},
        onToneFocusChanged = {},
        onAutoScaleWindowSecondsChanged = {},
        onAutoScaleWindowSecondsChangeFinished = {},
        onAutoScaleOffsetChanged = {},
        onAutoScaleOffsetChangeFinished = {},
        onReverseDirectionChanged = onReverse,
        onGlyphModeChanged = {},
        onFillOtherGlyphLightsChanged = {},
        onPhone1ClassicCSplitEnabledChanged = {},
        onBinaryModeChanged = {},
        onMatrixSmoothMotionEnabledChanged = {},
        onOscilloscopeAutoTimeAxisEnabledChanged = {},
        onLevelAutoScaleChanged = {},
        onSpectrumAutoScaleChanged = {},
        onAllBrightnessAutoScaleChanged = {},
        onLegacyAutoScaleEnabledChanged = {},
        onRecordingLightBehaviorChanged = {},
        onStartVisualizerClick = onStart,
        onStartProjectionClick = {},
        onEnablePhone1GlyphDebugClick = {},
        onStopClick = {},
        onOpenMenu = {},
        onOpenSettings = {},
        onOpenLatency = {},
        onBack = {},
    )
}
