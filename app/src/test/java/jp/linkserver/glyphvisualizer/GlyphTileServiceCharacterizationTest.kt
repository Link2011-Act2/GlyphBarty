package jp.linkserver.glyphvisualizer

import android.app.Application
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlyphTileServiceCharacterizationTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        drainStartedServices()
    }

    @After
    fun tearDown() {
        application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        drainStartedServices()
    }

    @Test
    fun clickingInactiveTile_startsVisualizerWithSavedSettingsAndQuickSettingsSource() {
        SettingsPreferences.save(
            application,
            CaptureUiState(
                sensitivity = 2.31f,
                noiseGate = 0.16f,
                dynamics = 1.66f,
                outputGamma = 2.05f,
                toneFocus = 0.28f,
                smoothing = 0.48f,
                smoothingBalance = -0.18f,
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
                autoScaleWindowSeconds = 39f,
                autoScaleOffset = -0.09f,
                latencyMs = 64f,
                latencyAutoSwitchEnabled = false,
                mediaPlaybackOnlyEnabled = true,
                experimentalVisualizerStabilizationEnabled = true,
                experimentalVisualizerSignalWatchdogEnabled = false,
                experimentalSpectrumDecayEnabled = true,
                experimentalPerformanceOptimizationsEnabled = false,
                matrixSmoothMotionEnabled = true,
                oscilloscopeAutoTimeAxisEnabled = true,
                autoEnablePhone1GlyphDebugOnStart = false
            )
        )
        val controller = Robolectric.buildService(GlyphTileService::class.java).create()
        val service = controller.get()

        service.onClick()

        val intent = requireNotNull(shadowOf(application).nextStartedService)
        assertEquals(ACTION_START_VISUALIZER, intent.action)
        assertEquals(GlyphVisualizerService::class.java.name, intent.component?.className)
        assertEquals("QUICK_SETTINGS", intent.getStringExtra("extra_start_source"))
        assertFloatEquals(2.31f, intent.getFloatExtra("extra_sensitivity", -1f))
        assertFloatEquals(0.16f, intent.getFloatExtra("extra_noise_gate", -1f))
        assertFloatEquals(1.66f, intent.getFloatExtra("extra_dynamics", -1f))
        assertFloatEquals(2.05f, intent.getFloatExtra("extra_output_gamma", -1f))
        assertFloatEquals(0.28f, intent.getFloatExtra("extra_tone_focus", -1f))
        assertFloatEquals(0.48f, intent.getFloatExtra("extra_smoothing", -1f))
        assertFloatEquals(-0.18f, intent.getFloatExtra("extra_smoothing_balance", 1f))
        assertTrue(intent.getBooleanExtra("extra_reverse_direction", false))
        assertFalse(intent.getBooleanExtra("extra_peak_hold_enabled", true))
        assertEquals(GlyphPatternRegistry.P2_C1_CENTER, intent.getStringExtra("extra_glyph_mode"))
        assertTrue(intent.getBooleanExtra("extra_fill_other_glyph_lights", false))
        assertTrue(intent.getBooleanExtra("extra_phone1_classic_c_split_enabled", false))
        assertTrue(intent.getBooleanExtra("extra_binary_mode", false))
        assertTrue(intent.getBooleanExtra("extra_base_indicator_enabled", false))
        assertFalse(intent.getBooleanExtra("extra_recording_light_included", true))
        assertFalse(intent.getBooleanExtra("extra_level_auto_scale", true))
        assertFalse(intent.getBooleanExtra("extra_spectrum_auto_scale", true))
        assertFalse(intent.getBooleanExtra("extra_all_brightness_auto_scale", true))
        assertFloatEquals(39f, intent.getFloatExtra("extra_auto_scale_window_seconds", -1f))
        assertFloatEquals(-0.09f, intent.getFloatExtra("extra_auto_scale_offset", 1f))
        assertFloatEquals(64f, intent.getFloatExtra("extra_latency_ms", -1f))
        assertTrue(intent.getBooleanExtra("extra_media_playback_only_enabled", false))
        assertTrue(
            intent.getBooleanExtra("extra_experimental_visualizer_stabilization_enabled", false)
        )
        assertFalse(
            intent.getBooleanExtra("extra_experimental_visualizer_signal_watchdog_enabled", true)
        )
        assertTrue(intent.getBooleanExtra("extra_experimental_spectrum_decay_enabled", false))
        assertFalse(
            intent.getBooleanExtra("extra_experimental_performance_optimizations_enabled", true)
        )
        assertTrue(intent.getBooleanExtra("extra_matrix_smooth_motion_enabled", false))
        assertTrue(intent.getBooleanExtra("extra_oscilloscope_auto_time_axis_enabled", false))

    }

    private fun drainStartedServices() {
        val shadowApplication = shadowOf(application)
        while (shadowApplication.nextStartedService != null) {
            // Drain intents left by another test in the same Robolectric application.
        }
    }

    private fun assertFloatEquals(expected: Float, actual: Float) {
        assertEquals(expected, actual, 0.0001f)
    }

    companion object {
        private const val PREFS_NAME = "glyph_visualizer_settings"
        private const val ACTION_START_VISUALIZER =
            "jp.linkserver.glyphvisualizer.action.START_VISUALIZER"
    }
}
