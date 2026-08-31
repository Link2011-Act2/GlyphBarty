package jp.linkserver.glyphvisualizer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
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
class GlyphVisualizerServiceIntentContractTest {
    private lateinit var context: RecordingContext
    private lateinit var previousUiState: CaptureUiState

    @Before
    fun setUp() {
        context = RecordingContext(RuntimeEnvironment.getApplication())
        previousUiState = CaptureUiStore.state
    }

    @After
    fun tearDown() {
        CaptureUiStore.update { previousUiState }
    }

    @Test
    fun startVisualizer_keepsActionComponentExtraNamesTypesAndPublicDefaults() {
        startVisualizer(context)

        val intent = requireNotNull(context.foregroundIntent)
        assertEquals(ACTION_START_VISUALIZER, intent.action)
        assertEquals(GlyphVisualizerService::class.java.name, intent.component?.className)
        assertEquals(START_VISUALIZER_EXTRA_KEYS, intent.extras?.keySet())
        assertConfigurationExtraTypes(intent, recordingLightExpected = true)
        assertConfigurationExtraValues(intent)
        assertFloatEquals(1.8f, intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, -1f))
        assertFalse(intent.getBooleanExtra(EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED, true))
        assertFalse(intent.getBooleanExtra(EXTRA_RECORDING_LIGHT_INCLUDED, true))
        assertEquals("APP", intent.getStringExtra(EXTRA_START_SOURCE))
    }

    @Test
    fun startMediaProjection_keepsActionResultExtrasAndPublicDefaults() {
        val projectionData = Intent("projection-result")

        startMediaProjection(context, projectionData)

        val intent = requireNotNull(context.foregroundIntent)
        assertEquals(ACTION_START_MEDIA_PROJECTION, intent.action)
        assertEquals(GlyphVisualizerService::class.java.name, intent.component?.className)
        assertEquals(START_MEDIA_PROJECTION_EXTRA_KEYS, intent.extras?.keySet())
        assertTrue(intent.extras?.get(EXTRA_RESULT_CODE) is Int)
        assertTrue(intent.extras?.get(EXTRA_RESULT_DATA) is Intent)
        assertEquals(73, intent.getIntExtra(EXTRA_RESULT_CODE, -1))
        assertEquals(
            projectionData.action,
            (intent.extras?.get(EXTRA_RESULT_DATA) as Intent).action
        )
        assertConfigurationExtraTypes(intent, recordingLightExpected = true)
        assertConfigurationExtraValues(intent)
        assertFloatEquals(1.8f, intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, -1f))
        assertFalse(intent.getBooleanExtra(EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED, true))
        assertFalse(intent.getBooleanExtra(EXTRA_RECORDING_LIGHT_INCLUDED, true))
    }

    @Test
    fun updateSensitivity_keepsActionExtraContractAndNullableDefaults() {
        CaptureUiStore.update { it.copy(isCapturing = true) }

        updateSensitivity(context)

        val intent = requireNotNull(context.startedIntent)
        assertEquals(ACTION_UPDATE_SENSITIVITY, intent.action)
        assertEquals(GlyphVisualizerService::class.java.name, intent.component?.className)
        assertEquals(UPDATE_EXTRA_KEYS_WITH_DEFAULTS, intent.extras?.keySet())
        assertConfigurationExtraTypes(intent, recordingLightExpected = false)
        assertConfigurationExtraValues(intent)
        assertTrue(intent.getFloatExtra(EXTRA_OUTPUT_GAMMA, 0f).isNaN())
        assertFalse(intent.getBooleanExtra(EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED, true))
        assertFalse(intent.hasExtra(EXTRA_RECORDING_LIGHT_INCLUDED))
    }

    @Test
    fun updateSensitivity_doesNotStartServiceWhenCaptureIsInactive() {
        CaptureUiStore.update { it.copy(isCapturing = false) }

        updateSensitivity(context)

        assertEquals(null, context.startedIntent)
    }

    @Test
    fun stop_keepsStopActionAndHasNoExtras() {
        GlyphVisualizerService.stop(context)

        val intent = requireNotNull(context.startedIntent)
        assertEquals(ACTION_STOP, intent.action)
        assertEquals(GlyphVisualizerService::class.java.name, intent.component?.className)
        assertTrue(intent.extras == null || intent.extras!!.isEmpty)
    }

    private fun startVisualizer(context: Context) {
        GlyphVisualizerService.startVisualizer(
            context = context,
            sensitivity = 2.25f,
            noiseGate = 0.14f,
            dynamics = 1.72f,
            toneFocus = 0.35f,
            smoothing = 0.44f,
            smoothingBalance = -0.3f,
            reverseDirection = true,
            peakHoldEnabled = false,
            glyphMode = GlyphPatternRegistry.P2_C1_CENTER,
            fillOtherGlyphLights = true,
            phone1ClassicCSplitEnabled = true,
            binaryMode = true,
            baseIndicatorEnabled = true,
            levelAutoScale = false,
            spectrumAutoScale = false,
            allBrightnessAutoScale = false,
            autoScaleWindowSeconds = 42f,
            autoScaleOffset = -0.12f,
            latencyMs = 67f,
            mediaPlaybackOnlyEnabled = true,
            experimentalVisualizerStabilizationEnabled = true,
            experimentalVisualizerSignalWatchdogEnabled = false,
            experimentalSpectrumDecayEnabled = true,
            experimentalPerformanceOptimizationsEnabled = false,
            matrixSmoothMotionEnabled = true,
            turnOffWhenBackDown = true
        )
    }

    private fun startMediaProjection(context: Context, data: Intent) {
        GlyphVisualizerService.startMediaProjection(
            context = context,
            resultCode = 73,
            data = data,
            sensitivity = 2.25f,
            noiseGate = 0.14f,
            dynamics = 1.72f,
            toneFocus = 0.35f,
            smoothing = 0.44f,
            smoothingBalance = -0.3f,
            reverseDirection = true,
            peakHoldEnabled = false,
            glyphMode = GlyphPatternRegistry.P2_C1_CENTER,
            fillOtherGlyphLights = true,
            phone1ClassicCSplitEnabled = true,
            binaryMode = true,
            baseIndicatorEnabled = true,
            levelAutoScale = false,
            spectrumAutoScale = false,
            allBrightnessAutoScale = false,
            autoScaleWindowSeconds = 42f,
            autoScaleOffset = -0.12f,
            latencyMs = 67f,
            mediaPlaybackOnlyEnabled = true,
            experimentalVisualizerStabilizationEnabled = true,
            experimentalVisualizerSignalWatchdogEnabled = false,
            experimentalSpectrumDecayEnabled = true,
            experimentalPerformanceOptimizationsEnabled = false,
            matrixSmoothMotionEnabled = true,
            turnOffWhenBackDown = true
        )
    }

    private fun updateSensitivity(context: Context) {
        GlyphVisualizerService.updateSensitivity(
            context = context,
            sensitivity = 2.25f,
            noiseGate = 0.14f,
            dynamics = 1.72f,
            toneFocus = 0.35f,
            smoothing = 0.44f,
            smoothingBalance = -0.3f,
            reverseDirection = true,
            peakHoldEnabled = false,
            glyphMode = GlyphPatternRegistry.P2_C1_CENTER,
            fillOtherGlyphLights = true,
            phone1ClassicCSplitEnabled = true,
            binaryMode = true,
            baseIndicatorEnabled = true,
            levelAutoScale = false,
            spectrumAutoScale = false,
            allBrightnessAutoScale = false,
            autoScaleWindowSeconds = 42f,
            autoScaleOffset = -0.12f,
            latencyMs = 67f,
            mediaPlaybackOnlyEnabled = true,
            experimentalVisualizerStabilizationEnabled = true,
            experimentalVisualizerSignalWatchdogEnabled = false,
            experimentalSpectrumDecayEnabled = true,
            experimentalPerformanceOptimizationsEnabled = false,
            matrixSmoothMotionEnabled = true,
            turnOffWhenBackDown = true
        )
    }

    private fun assertConfigurationExtraTypes(intent: Intent, recordingLightExpected: Boolean) {
        val extras = requireNotNull(intent.extras)
        FLOAT_EXTRA_KEYS.forEach { key -> assertTrue("$key must remain Float", extras.get(key) is Float) }
        BOOLEAN_EXTRA_KEYS.forEach { key -> assertTrue("$key must remain Boolean", extras.get(key) is Boolean) }
        assertTrue(extras.get(EXTRA_GLYPH_MODE) is String)
        if (recordingLightExpected) {
            assertTrue(extras.get(EXTRA_RECORDING_LIGHT_INCLUDED) is Boolean)
        }
    }

    private fun assertConfigurationExtraValues(intent: Intent) {
        assertFloatEquals(2.25f, intent.getFloatExtra(EXTRA_SENSITIVITY, -1f))
        assertFloatEquals(0.14f, intent.getFloatExtra(EXTRA_NOISE_GATE, -1f))
        assertFloatEquals(1.72f, intent.getFloatExtra(EXTRA_DYNAMICS, -1f))
        assertFloatEquals(0.35f, intent.getFloatExtra(EXTRA_TONE_FOCUS, -1f))
        assertFloatEquals(0.44f, intent.getFloatExtra(EXTRA_SMOOTHING, -1f))
        assertFloatEquals(-0.3f, intent.getFloatExtra(EXTRA_SMOOTHING_BALANCE, 1f))
        assertTrue(intent.getBooleanExtra(EXTRA_REVERSE_DIRECTION, false))
        assertFalse(intent.getBooleanExtra(EXTRA_PEAK_HOLD_ENABLED, true))
        assertEquals(GlyphPatternRegistry.P2_C1_CENTER, intent.getStringExtra(EXTRA_GLYPH_MODE))
        assertTrue(intent.getBooleanExtra(EXTRA_FILL_OTHER_GLYPH_LIGHTS, false))
        assertTrue(intent.getBooleanExtra(EXTRA_PHONE1_CLASSIC_C_SPLIT_ENABLED, false))
        assertTrue(intent.getBooleanExtra(EXTRA_BINARY_MODE, false))
        assertTrue(intent.getBooleanExtra(EXTRA_BASE_INDICATOR_ENABLED, false))
        assertFalse(intent.getBooleanExtra(EXTRA_LEVEL_AUTO_SCALE, true))
        assertFalse(intent.getBooleanExtra(EXTRA_SPECTRUM_AUTO_SCALE, true))
        assertFalse(intent.getBooleanExtra(EXTRA_ALL_BRIGHTNESS_AUTO_SCALE, true))
        assertFloatEquals(42f, intent.getFloatExtra(EXTRA_AUTO_SCALE_WINDOW_SECONDS, -1f))
        assertFloatEquals(-0.12f, intent.getFloatExtra(EXTRA_AUTO_SCALE_OFFSET, 1f))
        assertFloatEquals(67f, intent.getFloatExtra(EXTRA_LATENCY_MS, -1f))
        assertTrue(intent.getBooleanExtra(EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED, false))
        assertTrue(intent.getBooleanExtra(EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED, false))
        assertFalse(intent.getBooleanExtra(EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED, true))
        assertTrue(intent.getBooleanExtra(EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED, false))
        assertFalse(intent.getBooleanExtra(EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED, true))
        assertTrue(intent.getBooleanExtra(EXTRA_MATRIX_SMOOTH_MOTION_ENABLED, false))
        assertTrue(intent.getBooleanExtra(EXTRA_TURN_OFF_WHEN_BACK_DOWN, false))
    }

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }

    private class RecordingContext(base: Context) : ContextWrapper(base) {
        var foregroundIntent: Intent? = null
        var startedIntent: Intent? = null

        override fun startForegroundService(service: Intent): ComponentName? {
            foregroundIntent = Intent(service)
            return service.component
        }

        override fun startService(service: Intent): ComponentName? {
            startedIntent = Intent(service)
            return service.component
        }
    }

    companion object {
        private const val ACTION_START_VISUALIZER =
            "jp.linkserver.glyphvisualizer.action.START_VISUALIZER"
        private const val ACTION_START_MEDIA_PROJECTION =
            "jp.linkserver.glyphvisualizer.action.START_MEDIA_PROJECTION"
        private const val ACTION_UPDATE_SENSITIVITY =
            "jp.linkserver.glyphvisualizer.action.UPDATE_SENSITIVITY"
        private const val ACTION_STOP = "jp.linkserver.glyphvisualizer.action.STOP"
        private const val EXTRA_RESULT_CODE = "extra_result_code"
        private const val EXTRA_RESULT_DATA = "extra_result_data"
        private const val EXTRA_SENSITIVITY = "extra_sensitivity"
        private const val EXTRA_NOISE_GATE = "extra_noise_gate"
        private const val EXTRA_DYNAMICS = "extra_dynamics"
        private const val EXTRA_OUTPUT_GAMMA = "extra_output_gamma"
        private const val EXTRA_TONE_FOCUS = "extra_tone_focus"
        private const val EXTRA_SMOOTHING = "extra_smoothing"
        private const val EXTRA_SMOOTHING_BALANCE = "extra_smoothing_balance"
        private const val EXTRA_REVERSE_DIRECTION = "extra_reverse_direction"
        private const val EXTRA_PEAK_HOLD_ENABLED = "extra_peak_hold_enabled"
        private const val EXTRA_GLYPH_MODE = "extra_glyph_mode"
        private const val EXTRA_FILL_OTHER_GLYPH_LIGHTS = "extra_fill_other_glyph_lights"
        private const val EXTRA_PHONE1_CLASSIC_C_SPLIT_ENABLED =
            "extra_phone1_classic_c_split_enabled"
        private const val EXTRA_BINARY_MODE = "extra_binary_mode"
        private const val EXTRA_BASE_INDICATOR_ENABLED = "extra_base_indicator_enabled"
        private const val EXTRA_RECORDING_LIGHT_INCLUDED = "extra_recording_light_included"
        private const val EXTRA_LEVEL_AUTO_SCALE = "extra_level_auto_scale"
        private const val EXTRA_SPECTRUM_AUTO_SCALE = "extra_spectrum_auto_scale"
        private const val EXTRA_ALL_BRIGHTNESS_AUTO_SCALE = "extra_all_brightness_auto_scale"
        private const val EXTRA_AUTO_SCALE_WINDOW_SECONDS = "extra_auto_scale_window_seconds"
        private const val EXTRA_AUTO_SCALE_OFFSET = "extra_auto_scale_offset"
        private const val EXTRA_LATENCY_MS = "extra_latency_ms"
        private const val EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED = "extra_media_playback_only_enabled"
        private const val EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED =
            "extra_experimental_visualizer_stabilization_enabled"
        private const val EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED =
            "extra_experimental_visualizer_signal_watchdog_enabled"
        private const val EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED =
            "extra_experimental_spectrum_decay_enabled"
        private const val EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED =
            "extra_experimental_performance_optimizations_enabled"
        private const val EXTRA_MATRIX_SMOOTH_MOTION_ENABLED =
            "extra_matrix_smooth_motion_enabled"
        private const val EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED =
            "extra_oscilloscope_auto_time_axis_enabled"
        private const val EXTRA_TURN_OFF_WHEN_BACK_DOWN = "extra_turn_off_when_back_down"
        private const val EXTRA_START_SOURCE = "extra_start_source"

        private val FLOAT_EXTRA_KEYS = setOf(
            EXTRA_SENSITIVITY,
            EXTRA_NOISE_GATE,
            EXTRA_DYNAMICS,
            EXTRA_OUTPUT_GAMMA,
            EXTRA_TONE_FOCUS,
            EXTRA_SMOOTHING,
            EXTRA_SMOOTHING_BALANCE,
            EXTRA_AUTO_SCALE_WINDOW_SECONDS,
            EXTRA_AUTO_SCALE_OFFSET,
            EXTRA_LATENCY_MS
        )
        private val BOOLEAN_EXTRA_KEYS = setOf(
            EXTRA_REVERSE_DIRECTION,
            EXTRA_PEAK_HOLD_ENABLED,
            EXTRA_FILL_OTHER_GLYPH_LIGHTS,
            EXTRA_PHONE1_CLASSIC_C_SPLIT_ENABLED,
            EXTRA_BINARY_MODE,
            EXTRA_BASE_INDICATOR_ENABLED,
            EXTRA_LEVEL_AUTO_SCALE,
            EXTRA_SPECTRUM_AUTO_SCALE,
            EXTRA_ALL_BRIGHTNESS_AUTO_SCALE,
            EXTRA_MEDIA_PLAYBACK_ONLY_ENABLED,
            EXTRA_EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
            EXTRA_EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
            EXTRA_EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
            EXTRA_EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
            EXTRA_MATRIX_SMOOTH_MOTION_ENABLED,
            EXTRA_OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
            EXTRA_TURN_OFF_WHEN_BACK_DOWN
        )
        private val CONFIGURATION_EXTRA_KEYS =
            FLOAT_EXTRA_KEYS + BOOLEAN_EXTRA_KEYS + EXTRA_GLYPH_MODE + EXTRA_RECORDING_LIGHT_INCLUDED
        private val START_VISUALIZER_EXTRA_KEYS = CONFIGURATION_EXTRA_KEYS + EXTRA_START_SOURCE
        private val START_MEDIA_PROJECTION_EXTRA_KEYS =
            CONFIGURATION_EXTRA_KEYS + EXTRA_RESULT_CODE + EXTRA_RESULT_DATA
        private val UPDATE_EXTRA_KEYS_WITH_DEFAULTS =
            CONFIGURATION_EXTRA_KEYS - EXTRA_RECORDING_LIGHT_INCLUDED
    }
}
