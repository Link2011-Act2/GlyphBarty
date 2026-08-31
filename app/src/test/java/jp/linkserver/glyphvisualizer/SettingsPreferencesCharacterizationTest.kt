package jp.linkserver.glyphvisualizer

import android.app.Application
import android.content.Context
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import jp.linkserver.glyphvisualizer.glyph.GlyphVisualTuningKey
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsPreferencesCharacterizationTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        preferences().edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences().edit().clear().commit()
    }

    @Test
    fun saveThenLoad_roundTripsImportantCaptureUiSettingsWithoutChangingPreferenceKeys() {
        val visualKey = GlyphVisualTuningKey(
            GlyphDeviceProfile.PHONE2,
            GlyphPatternRegistry.P2_C1_LINEAR
        )
        val source = CaptureUiState(
            sensitivity = 2.25f,
            noiseGate = 0.14f,
            dynamics = 1.72f,
            outputGamma = 2.1f,
            toneFocus = 0.35f,
            smoothing = 0.44f,
            smoothingBalance = -0.3f,
            reverseDirection = true,
            peakHoldEnabled = false,
            glyphMode = GlyphPatternRegistry.P2_C1_CENTER,
            fillOtherGlyphLights = true,
            phone1ClassicCSplitEnabled = true,
            binaryMode = true,
            baseIndicatorEnabled = false,
            recordingLightIncluded = true,
            phone4bEmulationEnabled = true,
            debugDeviceProfileOverride = GlyphDeviceProfile.PHONE3A,
            levelAutoScale = false,
            spectrumAutoScale = false,
            allBrightnessAutoScale = false,
            experimentalAdaptiveAutoScaleEnabled = false,
            visualDynamicsOverrides = mapOf(visualKey to 0.65f),
            autoScaleWindowSeconds = 42f,
            autoScaleOffset = -0.12f,
            latencyMs = 17f,
            defaultOutputLatencyMs = 31f,
            bluetoothLatencyMs = 96f,
            latencyAutoSwitchEnabled = false,
            mediaProjectionEnabled = true,
            glyphMeterPreviewEnabled = false,
            meterVisibleEnabled = false,
            lightweightMeterEnabled = true,
            spectrumMeterEnabled = true,
            nativeMeterViewEnabled = false,
            mainScreenUiIsolationEnabled = false,
            automaticUpdateCheckEnabled = true,
            mediaPlaybackOnlyEnabled = true,
            experimentalVisualizerStabilizationEnabled = true,
            experimentalVisualizerSignalWatchdogEnabled = false,
            experimentalSpectrumDecayEnabled = true,
            experimentalPerformanceOptimizationsEnabled = false,
            matrixSmoothMotionEnabled = true,
            oscilloscopeAutoTimeAxisEnabled = true,
            showPhone1GlyphDebugControlsEverywhere = true,
            autoEnablePhone1GlyphDebugOnStart = false,
            nothingStyleEnabled = true,
            experimentalMainUiEnabled = false,
            detailedHomeEnabled = true,
            turnOffWhenBackDown = true,
            statusText = "runtime only",
            level = 0.9f,
            isCapturing = true,
            activeMode = "VISUALIZER"
        )

        SettingsPreferences.save(application, source)
        val loaded = SettingsPreferences.load(application)

        assertPersistentSettingsEqual(SettingsPreferences.parameterStateOf(source), loaded)
        assertTrue(preferences().all.keys.containsAll(EXISTING_PREFERENCE_KEYS))
        assertTrue(preferences().all["sensitivity"] is Float)
        assertTrue(preferences().all["reverse_direction"] is Boolean)
        assertTrue(preferences().all["glyph_mode"] is String)
    }

    @Test
    fun exportThenImport_roundTripsTheCurrentPortableParameterContract() {
        val visualKey = GlyphVisualTuningKey(
            GlyphDeviceProfile.PHONE2,
            GlyphPatternRegistry.P2_C1_LINEAR_PEAK
        )
        val source = CaptureUiState(
            sensitivity = 2.4f,
            noiseGate = 0.18f,
            dynamics = 1.8f,
            outputGamma = 2.2f,
            toneFocus = 0.45f,
            smoothing = 0.51f,
            smoothingBalance = 0.22f,
            glyphMode = GlyphPatternRegistry.P2_C1_SPECTRUM,
            autoScaleWindowSeconds = 37f,
            autoScaleOffset = -0.08f,
            visualDynamicsOverrides = mapOf(visualKey to 0.72f),
            oscilloscopeAutoTimeAxisEnabled = true
        )

        val exported = SettingsPreferences.exportJson(source)
        val root = JSONObject(exported)
        val parameters = root.getJSONObject("parameters")
        val imported = SettingsPreferences.importJson(exported)

        assertEquals("glyph_barty_parameters", root.getString("format"))
        assertEquals(2, root.getInt("version"))
        assertEquals(PORTABLE_PARAMETER_KEYS, parameters.keys().asSequence().toSet())
        assertFloatEquals(source.sensitivity, imported.sensitivity)
        assertFloatEquals(source.noiseGate, imported.noiseGate)
        assertFloatEquals(source.dynamics, imported.dynamics)
        assertFloatEquals(source.outputGamma, imported.outputGamma)
        assertFloatEquals(source.toneFocus, imported.toneFocus)
        assertFloatEquals(source.smoothing, imported.smoothing)
        assertFloatEquals(source.smoothingBalance, imported.smoothingBalance)
        assertEquals(source.glyphMode, imported.glyphMode)
        assertFloatEquals(source.autoScaleWindowSeconds, imported.autoScaleWindowSeconds)
        assertFloatEquals(source.autoScaleOffset, imported.autoScaleOffset)
        assertEquals(source.visualDynamicsOverrides, imported.visualDynamicsOverrides)
        assertEquals(
            source.oscilloscopeAutoTimeAxisEnabled,
            imported.oscilloscopeAutoTimeAxisEnabled
        )
    }

    @Test
    fun load_readsLegacyLatencyPresetsAndNumericStorageFormats() {
        preferences().edit()
            .putString("default_output_latency_preset_id", "latency_preset_2")
            .putString("bluetooth_latency_preset_id", "latency_preset_3")
            .putString("latency_preset_2_ms", "37.5")
            .putLong("latency_preset_3_ms", 88L)
            .putString("latency_ms", "12.25")
            .commit()

        val loaded = SettingsPreferences.load(application)

        assertFloatEquals(37.5f, loaded.defaultOutputLatencyMs)
        assertFloatEquals(88f, loaded.bluetoothLatencyMs)
        assertFloatEquals(12.25f, loaded.latencyMs)
    }

    @Test
    fun save_preservesUnknownDormantValuesWhileKeepingCurrentDormantFlagNormalization() {
        preferences().edit()
            .putString("future_dormant_setting", "keep-me")
            .putBoolean("turn_off_when_back_down", true)
            .putBoolean("main_screen_ui_isolation_enabled", false)
            .putBoolean("phone4b_emulation_enabled", true)
            .putBoolean("experimental_visualizer_stabilization_enabled", true)
            .commit()

        val loaded = SettingsPreferences.load(application)
        SettingsPreferences.save(application, loaded)

        assertFalse(loaded.turnOffWhenBackDown)
        assertTrue(loaded.mainScreenUiIsolationEnabled)
        assertTrue(loaded.phone4bEmulationEnabled)
        assertTrue(loaded.experimentalVisualizerStabilizationEnabled)
        assertEquals("keep-me", preferences().getString("future_dormant_setting", null))
        assertFalse(preferences().getBoolean("turn_off_when_back_down", true))
        assertTrue(preferences().getBoolean("main_screen_ui_isolation_enabled", false))
    }

    @Test
    fun setupAndNotificationMarkers_keepTheirExistingKeysAndFallbackBehavior() {
        assertFalse(SettingsPreferences.hasCompletedInitialSetup(application))
        assertFalse(SettingsPreferences.hasShownNotificationPermissionPrompt(application))

        preferences().edit().putFloat("sensitivity", 2f).commit()
        assertTrue(SettingsPreferences.hasCompletedInitialSetup(application))

        SettingsPreferences.markInitialSetupCompleted(application)
        SettingsPreferences.markNotificationPermissionPromptShown(application)

        assertTrue(preferences().getBoolean("initial_setup_completed", false))
        assertTrue(preferences().getBoolean("notification_permission_prompt_shown", false))
        assertTrue(SettingsPreferences.hasCompletedInitialSetup(application))
        assertTrue(SettingsPreferences.hasShownNotificationPermissionPrompt(application))
    }

    private fun preferences() = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun assertPersistentSettingsEqual(expected: CaptureUiState, actual: CaptureUiState) {
        assertFloatEquals(expected.sensitivity, actual.sensitivity)
        assertFloatEquals(expected.noiseGate, actual.noiseGate)
        assertFloatEquals(expected.dynamics, actual.dynamics)
        assertFloatEquals(expected.outputGamma, actual.outputGamma)
        assertFloatEquals(expected.toneFocus, actual.toneFocus)
        assertFloatEquals(expected.smoothing, actual.smoothing)
        assertFloatEquals(expected.smoothingBalance, actual.smoothingBalance)
        assertEquals(expected.reverseDirection, actual.reverseDirection)
        assertEquals(expected.peakHoldEnabled, actual.peakHoldEnabled)
        assertEquals(expected.glyphMode, actual.glyphMode)
        assertEquals(expected.fillOtherGlyphLights, actual.fillOtherGlyphLights)
        assertEquals(expected.phone1ClassicCSplitEnabled, actual.phone1ClassicCSplitEnabled)
        assertEquals(expected.binaryMode, actual.binaryMode)
        assertEquals(expected.baseIndicatorEnabled, actual.baseIndicatorEnabled)
        assertEquals(expected.recordingLightIncluded, actual.recordingLightIncluded)
        assertEquals(expected.phone4bEmulationEnabled, actual.phone4bEmulationEnabled)
        assertEquals(expected.debugDeviceProfileOverride, actual.debugDeviceProfileOverride)
        assertEquals(expected.levelAutoScale, actual.levelAutoScale)
        assertEquals(expected.spectrumAutoScale, actual.spectrumAutoScale)
        assertEquals(expected.allBrightnessAutoScale, actual.allBrightnessAutoScale)
        assertEquals(
            expected.experimentalAdaptiveAutoScaleEnabled,
            actual.experimentalAdaptiveAutoScaleEnabled
        )
        assertEquals(expected.visualDynamicsOverrides, actual.visualDynamicsOverrides)
        assertFloatEquals(expected.autoScaleWindowSeconds, actual.autoScaleWindowSeconds)
        assertFloatEquals(expected.autoScaleOffset, actual.autoScaleOffset)
        assertFloatEquals(expected.latencyMs, actual.latencyMs)
        assertFloatEquals(expected.defaultOutputLatencyMs, actual.defaultOutputLatencyMs)
        assertFloatEquals(expected.bluetoothLatencyMs, actual.bluetoothLatencyMs)
        assertEquals(expected.latencyAutoSwitchEnabled, actual.latencyAutoSwitchEnabled)
        assertEquals(expected.mediaProjectionEnabled, actual.mediaProjectionEnabled)
        assertEquals(expected.glyphMeterPreviewEnabled, actual.glyphMeterPreviewEnabled)
        assertEquals(expected.meterVisibleEnabled, actual.meterVisibleEnabled)
        assertEquals(expected.lightweightMeterEnabled, actual.lightweightMeterEnabled)
        assertEquals(expected.spectrumMeterEnabled, actual.spectrumMeterEnabled)
        assertEquals(expected.nativeMeterViewEnabled, actual.nativeMeterViewEnabled)
        assertEquals(expected.mainScreenUiIsolationEnabled, actual.mainScreenUiIsolationEnabled)
        assertEquals(expected.automaticUpdateCheckEnabled, actual.automaticUpdateCheckEnabled)
        assertEquals(expected.mediaPlaybackOnlyEnabled, actual.mediaPlaybackOnlyEnabled)
        assertEquals(
            expected.experimentalVisualizerStabilizationEnabled,
            actual.experimentalVisualizerStabilizationEnabled
        )
        assertEquals(
            expected.experimentalVisualizerSignalWatchdogEnabled,
            actual.experimentalVisualizerSignalWatchdogEnabled
        )
        assertEquals(expected.experimentalSpectrumDecayEnabled, actual.experimentalSpectrumDecayEnabled)
        assertEquals(
            expected.experimentalPerformanceOptimizationsEnabled,
            actual.experimentalPerformanceOptimizationsEnabled
        )
        assertEquals(expected.matrixSmoothMotionEnabled, actual.matrixSmoothMotionEnabled)
        assertEquals(expected.oscilloscopeAutoTimeAxisEnabled, actual.oscilloscopeAutoTimeAxisEnabled)
        assertEquals(
            expected.showPhone1GlyphDebugControlsEverywhere,
            actual.showPhone1GlyphDebugControlsEverywhere
        )
        assertEquals(expected.autoEnablePhone1GlyphDebugOnStart, actual.autoEnablePhone1GlyphDebugOnStart)
        assertEquals(expected.nothingStyleEnabled, actual.nothingStyleEnabled)
        assertEquals(expected.experimentalMainUiEnabled, actual.experimentalMainUiEnabled)
        assertEquals(expected.detailedHomeEnabled, actual.detailedHomeEnabled)
        assertEquals(expected.turnOffWhenBackDown, actual.turnOffWhenBackDown)
    }

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }

    companion object {
        private const val PREFS_NAME = "glyph_visualizer_settings"

        private val PORTABLE_PARAMETER_KEYS = setOf(
            "sensitivity",
            "noiseGate",
            "dynamics",
            "outputGamma",
            "toneFocus",
            "smoothing",
            "smoothingBalance",
            "glyphMode",
            "autoScaleWindowSeconds",
            "autoScaleOffset",
            "visualDynamicsOverrides",
            "oscilloscopeAutoTimeAxisEnabled"
        )

        private val EXISTING_PREFERENCE_KEYS = setOf(
            "sensitivity",
            "noise_gate",
            "dynamics",
            "output_gamma",
            "tone_focus",
            "smoothing",
            "smoothing_balance",
            "reverse_direction",
            "peak_hold_enabled",
            "glyph_mode",
            "fill_other_glyph_lights",
            "phone1_classic_c_split_enabled",
            "binary_mode",
            "base_indicator_enabled",
            "recording_light_included",
            "phone4b_emulation_enabled",
            "debug_device_profile_override",
            "level_auto_scale",
            "spectrum_auto_scale",
            "all_brightness_auto_scale",
            "experimental_adaptive_auto_scale_enabled",
            "glyph_visual_dynamics_overrides_v1",
            "auto_scale_window_seconds",
            "auto_scale_offset",
            "latency_ms",
            "default_output_latency_ms",
            "bluetooth_latency_ms",
            "latency_auto_switch_enabled",
            "media_projection_enabled",
            "glyph_meter_preview_enabled",
            "meter_visible_enabled",
            "lightweight_meter_enabled",
            "spectrum_meter_enabled",
            "native_meter_view_enabled",
            "main_screen_ui_isolation_enabled",
            "automatic_update_check_enabled",
            "media_playback_only_enabled",
            "experimental_visualizer_stabilization_enabled",
            "experimental_visualizer_signal_watchdog_enabled",
            "experimental_spectrum_decay_enabled",
            "experimental_performance_optimizations_enabled",
            "matrix_smooth_motion_enabled",
            "oscilloscope_auto_time_axis_enabled",
            "show_phone1_glyph_debug_controls_everywhere",
            "auto_enable_phone1_glyph_debug_on_start",
            "nothing_style_enabled",
            "experimental_main_ui_enabled",
            "detailed_home_enabled",
            "turn_off_when_back_down"
        )
    }
}
