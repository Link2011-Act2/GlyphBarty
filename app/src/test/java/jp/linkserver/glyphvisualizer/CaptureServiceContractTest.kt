package jp.linkserver.glyphvisualizer

import android.app.Application
import android.content.Intent
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
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
class CaptureServiceContractTest {
    private lateinit var application: Application

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
    }

    @Test
    fun captureUiState_mapsEveryServiceSettingToCaptureConfig() {
        val expected = sampleConfig()
        val state = CaptureUiState(
            sensitivity = expected.sensitivity,
            noiseGate = expected.noiseGate,
            dynamics = expected.dynamics,
            outputGamma = expected.outputGamma,
            toneFocus = expected.toneFocus,
            smoothing = expected.smoothing,
            smoothingBalance = expected.smoothingBalance,
            reverseDirection = expected.reverseDirection,
            peakHoldEnabled = expected.peakHoldEnabled,
            glyphMode = expected.glyphMode,
            fillOtherGlyphLights = expected.fillOtherGlyphLights,
            phone1ClassicCSplitEnabled = expected.phone1ClassicCSplitEnabled,
            binaryMode = expected.binaryMode,
            baseIndicatorEnabled = expected.baseIndicatorEnabled,
            recordingLightIncluded = expected.recordingLightIncluded,
            levelAutoScale = expected.levelAutoScale,
            spectrumAutoScale = expected.spectrumAutoScale,
            allBrightnessAutoScale = expected.allBrightnessAutoScale,
            autoScaleWindowSeconds = expected.autoScaleWindowSeconds,
            autoScaleOffset = expected.autoScaleOffset,
            latencyMs = expected.latencyMs,
            mediaPlaybackOnlyEnabled = expected.mediaPlaybackOnlyEnabled,
            experimentalVisualizerStabilizationEnabled =
                expected.experimentalVisualizerStabilizationEnabled,
            experimentalVisualizerSignalWatchdogEnabled =
                expected.experimentalVisualizerSignalWatchdogEnabled,
            experimentalSpectrumDecayEnabled = expected.experimentalSpectrumDecayEnabled,
            experimentalPerformanceOptimizationsEnabled =
                expected.experimentalPerformanceOptimizationsEnabled,
            matrixSmoothMotionEnabled = expected.matrixSmoothMotionEnabled,
            oscilloscopeAutoTimeAxisEnabled = expected.oscilloscopeAutoTimeAxisEnabled,
            turnOffWhenBackDown = expected.turnOffWhenBackDown
        )

        assertEquals(expected, state.toCaptureConfig())
    }

    @Test
    fun startVisualizerCodec_roundTripsTypedConfigAndSource() {
        val expected = CaptureCommand.StartVisualizer(
            config = sampleConfig(),
            source = VisualizerStartSource.QUICK_SETTINGS
        )

        val intent = CaptureIntentCommandCodec.encode(application, expected)
        val decoded = CaptureIntentCommandCodec.decode(intent, fallbackConfig())
            as CaptureCommand.StartVisualizer

        assertEquals(expected.config, decoded.config)
        assertEquals(expected.source, decoded.source)
    }

    @Test
    fun mediaProjectionAndStopCodec_roundTripCommandSpecificData() {
        val projectionData = Intent("projection-result")
        val expected = CaptureCommand.StartMediaProjection(
            resultCode = 73,
            data = projectionData,
            config = sampleConfig()
        )

        val mediaIntent = CaptureIntentCommandCodec.encode(application, expected)
        val decodedMedia = CaptureIntentCommandCodec.decode(mediaIntent, fallbackConfig())
            as CaptureCommand.StartMediaProjection
        val stopIntent = CaptureIntentCommandCodec.encode(application, CaptureCommand.Stop)

        assertEquals(expected.resultCode, decodedMedia.resultCode)
        assertEquals(projectionData.action, decodedMedia.data?.action)
        assertEquals(expected.config, decodedMedia.config)
        assertEquals(
            CaptureCommand.Stop,
            CaptureIntentCommandCodec.decode(stopIntent, fallbackConfig())
        )
    }

    @Test
    fun updateCodec_preservesLegacyOptionalExtraSemanticsAndDecodeFallbacks() {
        val fallback = fallbackConfig()
        val command = CaptureCommand.UpdateConfig(
            config = sampleConfig(),
            encodedOutputGamma = Float.NaN,
            encodedRecordingLightIncluded = null
        )

        val intent = CaptureIntentCommandCodec.encode(application, command)
        val decoded = CaptureIntentCommandCodec.decode(intent, fallback)
            as CaptureCommand.UpdateConfig

        assertTrue(intent.hasExtra(CaptureIntentCommandCodec.EXTRA_OUTPUT_GAMMA))
        assertTrue(
            intent.getFloatExtra(CaptureIntentCommandCodec.EXTRA_OUTPUT_GAMMA, 0f).isNaN()
        )
        assertFalse(intent.hasExtra(CaptureIntentCommandCodec.EXTRA_RECORDING_LIGHT_INCLUDED))
        assertTrue(decoded.encodedOutputGamma.isNaN())
        assertEquals(null, decoded.encodedRecordingLightIncluded)
        assertEquals(
            sampleConfig().copy(
                outputGamma = fallback.outputGamma,
                recordingLightIncluded = fallback.recordingLightIncluded
            ),
            decoded.config
        )
    }

    @Test
    fun typedUpdateCodec_roundTripsTheCompleteConfig() {
        val expected = sampleConfig()
        val intent = CaptureIntentCommandCodec.encode(
            application,
            CaptureCommand.UpdateConfig(expected)
        )

        val decoded = CaptureIntentCommandCodec.decode(intent, fallbackConfig())
            as CaptureCommand.UpdateConfig

        assertTrue(intent.hasExtra(CaptureIntentCommandCodec.EXTRA_RECORDING_LIGHT_INCLUDED))
        assertEquals(expected, decoded.config)
    }

    @Test
    fun decodeWithMissingExtras_keepsCurrentConfigAndExistingDefaults() {
        val fallback = fallbackConfig()
        val intent = Intent(application, GlyphVisualizerService::class.java).apply {
            action = CaptureIntentCommandCodec.ACTION_START_VISUALIZER
            putExtra(CaptureIntentCommandCodec.EXTRA_START_SOURCE, "UNKNOWN_SOURCE")
        }

        val decoded = CaptureIntentCommandCodec.decode(intent, fallback)
            as CaptureCommand.StartVisualizer

        assertEquals(fallback, decoded.config)
        assertEquals(VisualizerStartSource.APP, decoded.source)
    }

    private fun sampleConfig(): CaptureConfig = CaptureConfig(
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
        baseIndicatorEnabled = true,
        recordingLightIncluded = false,
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
        oscilloscopeAutoTimeAxisEnabled = true,
        turnOffWhenBackDown = true
    )

    private fun fallbackConfig(): CaptureConfig = CaptureUiState(
        outputGamma = 2.6f,
        recordingLightIncluded = true
    ).toCaptureConfig()
}
