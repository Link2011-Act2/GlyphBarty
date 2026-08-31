package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CaptureStateModelTest {
    private lateinit var previousState: CaptureUiState
    private var previousUiVisible = false

    @Before
    fun setUp() {
        previousState = CaptureUiStore.state
        previousUiVisible = CaptureUiStore.isUiVisible()
    }

    @After
    fun tearDown() {
        CaptureUiStore.update { previousState }
        CaptureUiStore.syncLiveFrameFromState(previousState)
        CaptureUiStore.setUiVisible(previousUiVisible)
    }

    @Test
    fun stateSlices_roundTripParametersPreferencesRuntimeAndCompatibilityFrame() {
        val expected = CaptureUiState(
            statusText = "capturing",
            level = 0.41f,
            peak = 0.83f,
            meterSegments = 7,
            spectrumBands = floatArrayOf(0.1f, 0.5f, 0.9f),
            sensitivity = 2.2f,
            glyphMode = GlyphPatternRegistry.P2_C1_CENTER,
            isCapturing = true,
            activeMode = "VISUALIZER",
            phone4bEmulationEnabled = true,
            debugDeviceProfileOverride = GlyphDeviceProfile.PHONE2,
            defaultOutputLatencyMs = 17f,
            bluetoothLatencyMs = 93f,
            latencyAutoSwitchEnabled = false,
            isBluetoothOutputActive = true,
            mediaProjectionEnabled = true,
            glyphMeterPreviewEnabled = false,
            meterVisibleEnabled = false,
            lightweightMeterEnabled = true,
            spectrumMeterEnabled = true,
            nativeMeterViewEnabled = false,
            mainScreenUiIsolationEnabled = false,
            automaticUpdateCheckEnabled = true,
            showPhone1GlyphDebugControlsEverywhere = true,
            autoEnablePhone1GlyphDebugOnStart = false,
            nothingStyleEnabled = true,
            experimentalMainUiEnabled = false,
            detailedHomeEnabled = true,
            logMessage = "log",
            pendingSpatialAudioWarning = SpatialAudioWarning("Phone")
        )

        val actual = CaptureStateSlices.from(expected).toFacadeState()

        assertEquals(expected, actual)
    }

    @Test
    fun runtimeUpdate_doesNotReplaceParametersPreferencesOrFrameSlices() {
        val repository = CaptureStateRepository(
            CaptureUiState(
                sensitivity = 2.4f,
                nothingStyleEnabled = true,
                level = 0.7f,
                peak = 0.9f
            )
        )
        val before = repository.slices()

        repository.updateRuntime { it.copy(statusText = "running", isCapturing = true) }
        val after = repository.slices()

        assertEquals(before.parameters, after.parameters)
        assertEquals(before.uiPreferences, after.uiPreferences)
        assertEquals(before.compatibilityFrame, after.compatibilityFrame)
        assertEquals("running", after.runtime.statusText)
        assertTrue(after.runtime.isCapturing)
    }

    @Test
    fun highFrequencyFramePublishing_doesNotMutateSettingsOrRuntimeState() {
        CaptureUiStore.update {
            it.copy(
                sensitivity = 2.35f,
                isCapturing = true,
                activeMode = "VISUALIZER",
                mainScreenUiIsolationEnabled = true
            )
        }
        val stateBefore = CaptureUiStore.state
        val parametersBefore = CaptureUiStore.captureParameters
        val runtimeBefore = CaptureUiStore.runtimeState

        CaptureUiStore.publishLiveFrame(
            level = 0.44f,
            peak = 0.88f,
            meterSegments = 9,
            spectrumBands = floatArrayOf(0.2f, 0.6f)
        )

        assertEquals(stateBefore, CaptureUiStore.state)
        assertEquals(parametersBefore, CaptureUiStore.captureParameters)
        assertEquals(runtimeBefore, CaptureUiStore.runtimeState)
        assertEquals(0.44f, CaptureUiStore.liveFrame.level, 0.0001f)
        assertEquals(0.88f, CaptureUiStore.liveFrame.peak, 0.0001f)
    }

    @Test
    fun uiVisibilityRecreationBoundary_keepsServiceAndPreferenceState() {
        CaptureUiStore.update {
            it.copy(
                sensitivity = 2.05f,
                isCapturing = true,
                activeMode = "VISUALIZER",
                nothingStyleEnabled = true,
                level = 0.8f
            )
        }

        CaptureUiStore.setUiVisible(false)
        CaptureUiStore.publishDirectLiveFrame(0.2f, 0.5f, 3, floatArrayOf(0.3f))
        CaptureUiStore.setUiVisible(true)

        assertEquals(2.05f, CaptureUiStore.state.sensitivity, 0.0001f)
        assertTrue(CaptureUiStore.state.isCapturing)
        assertEquals("VISUALIZER", CaptureUiStore.state.activeMode)
        assertTrue(CaptureUiStore.state.nothingStyleEnabled)
        assertFalse(CaptureUiStore.state.level == CaptureUiStore.liveFrame.level)
    }
}
