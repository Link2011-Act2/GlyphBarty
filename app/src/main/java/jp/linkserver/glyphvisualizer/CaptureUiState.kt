package jp.linkserver.glyphvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.os.Handler
import android.os.Looper
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphVisualTuningKey
import jp.linkserver.glyphvisualizer.glyph.DEFAULT_ADAPTIVE_AUTO_SCALE_ENABLED

data class CaptureUiState(
    val statusText: String = "",
    val level: Float = 0f,
    val peak: Float = 0f,
    val sensitivity: Float = 1.75f,
    val noiseGate: Float = 0.08f,
    val dynamics: Float = 1.45f,
    val outputGamma: Float = 1.8f,
    val toneFocus: Float = -0.2f,
    val smoothing: Float = 0.30f,
    val smoothingBalance: Float = 0f,
    val reverseDirection: Boolean = false,
    val peakHoldEnabled: Boolean = true,
    val isCapturing: Boolean = false,
    val meterSegments: Int = 0,
    val spectrumBands: FloatArray = FloatArray(0),
    val activeMode: String = "IDLE",
    val glyphMode: String = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice(),
    val fillOtherGlyphLights: Boolean = false,
    val phone1ClassicCSplitEnabled: Boolean = false,
    val binaryMode: Boolean = false,
    val baseIndicatorEnabled: Boolean = false,
    val recordingLightIncluded: Boolean = false,
    val phone4bEmulationEnabled: Boolean = false,
    val debugDeviceProfileOverride: GlyphDeviceProfile? = null,
    val levelAutoScale: Boolean = true,
    val spectrumAutoScale: Boolean = true,
    val allBrightnessAutoScale: Boolean = true,
    val experimentalAdaptiveAutoScaleEnabled: Boolean = DEFAULT_ADAPTIVE_AUTO_SCALE_ENABLED,
    val visualDynamicsOverrides: Map<GlyphVisualTuningKey, Float> = emptyMap(),
    val autoScaleWindowSeconds: Float = 20f,
    val autoScaleOffset: Float = 0f,
    val latencyMs: Float = 0f,
    val defaultOutputLatencyMs: Float = 0f,
    val bluetoothLatencyMs: Float = 0f,
    val latencyAutoSwitchEnabled: Boolean = true,
    val isBluetoothOutputActive: Boolean = false,
    val mediaProjectionEnabled: Boolean = false,
    val glyphMeterPreviewEnabled: Boolean = true,
    val meterVisibleEnabled: Boolean = true,
    val lightweightMeterEnabled: Boolean = false,
    val spectrumMeterEnabled: Boolean = false,
    val nativeMeterViewEnabled: Boolean = true,
    val mainScreenUiIsolationEnabled: Boolean = true,
    val automaticUpdateCheckEnabled: Boolean = false,
    val mediaPlaybackOnlyEnabled: Boolean = false,
    val experimentalVisualizerStabilizationEnabled: Boolean = false,
    val experimentalVisualizerSignalWatchdogEnabled: Boolean = true,
    val experimentalSpectrumDecayEnabled: Boolean = false,
    val experimentalPerformanceOptimizationsEnabled: Boolean = true,
    val matrixSmoothMotionEnabled: Boolean = false,
    val oscilloscopeAutoTimeAxisEnabled: Boolean = false,
    val showPhone1GlyphDebugControlsEverywhere: Boolean = false,
    val autoEnablePhone1GlyphDebugOnStart: Boolean = true,
    val nothingStyleEnabled: Boolean = false,
    val experimentalMainUiEnabled: Boolean = true,
    val detailedHomeEnabled: Boolean = false,
    val turnOffWhenBackDown: Boolean = false,
    val logMessage: String? = null,
    val pendingSpatialAudioWarning: SpatialAudioWarning? = null
)

data class SpatialAudioWarning(
    val nothingOrCmfProductName: String? = null
)

enum class RecordingLightBehavior {
    NONE,
    INCLUDED_IN_METER,
    BASS_INDICATOR
}

val RecordingLightBehavior.baseIndicatorEnabled: Boolean
    get() = this == RecordingLightBehavior.BASS_INDICATOR

val RecordingLightBehavior.recordingLightIncluded: Boolean
    get() = this == RecordingLightBehavior.INCLUDED_IN_METER

fun resolveRecordingLightBehavior(
    baseIndicatorEnabled: Boolean,
    recordingLightIncluded: Boolean
): RecordingLightBehavior = when {
    baseIndicatorEnabled -> RecordingLightBehavior.BASS_INDICATOR
    recordingLightIncluded -> RecordingLightBehavior.INCLUDED_IN_METER
    else -> RecordingLightBehavior.NONE
}

fun GlyphDeviceProfile.supportsRecordingLightBehavior(): Boolean =
    this == GlyphDeviceProfile.PHONE4A || this == GlyphDeviceProfile.PHONE4B

fun CaptureUiState.withRecordingLightBehavior(
    behavior: RecordingLightBehavior
): CaptureUiState = copy(
    baseIndicatorEnabled = behavior.baseIndicatorEnabled,
    recordingLightIncluded = behavior.recordingLightIncluded
)

data class CaptureLiveFrame(
    val level: Float = 0f,
    val peak: Float = 0f,
    val meterSegments: Int = 0,
    val spectrumBands: FloatArray = FloatArray(0)
)

object CaptureUiStore {
    private const val TAG = "CaptureUiStore"
    private const val DEBUG_UI_VISIBILITY_LOGS = false
    private const val DIRECT_FRAME_DISPATCH_LOG_INTERVAL_MS = 5_000L
    var state by mutableStateOf(CaptureUiState())
        private set
    var liveFrame by mutableStateOf(CaptureLiveFrame())
        private set
    @Volatile
    private var uiVisible: Boolean = false
    @Volatile
    private var meterVisibleForPublishing: Boolean = state.meterVisibleEnabled
    private val mainHandler = Handler(Looper.getMainLooper())
    private val directMeterFrameListeners = mutableSetOf<(CaptureLiveFrame) -> Unit>()
    private var directMeterFrameDispatchCount = 0
    private var lastDirectMeterFrameDispatchLogAtMs = 0L

    fun update(transform: (CaptureUiState) -> CaptureUiState) {
        val nextState = transform(state)
        meterVisibleForPublishing = nextState.meterVisibleEnabled
        state = nextState
    }

    fun setUiVisible(visible: Boolean) {
        if (DEBUG_UI_VISIBILITY_LOGS && uiVisible != visible) {
            AppLogger.i(TAG, "UI visibility changed: visible=$visible")
        }
        uiVisible = visible
    }

    fun shouldPublishLiveUiFrames(): Boolean {
        return uiVisible && meterVisibleForPublishing
    }

    fun isUiVisible(): Boolean = uiVisible

    fun publishLiveFrame(
        level: Float,
        peak: Float,
        meterSegments: Int,
        spectrumBands: FloatArray
    ) {
        val nextFrame = CaptureLiveFrame(
            level = level,
            peak = peak,
            meterSegments = meterSegments,
            spectrumBands = spectrumBands
        )
        liveFrame = nextFrame
        publishDirectMeterFrame(nextFrame)
        if (!state.mainScreenUiIsolationEnabled) {
            state = state.copy(
                level = level,
                peak = peak,
                meterSegments = meterSegments,
                spectrumBands = spectrumBands
            )
        }
    }

    fun publishDirectLiveFrame(
        level: Float,
        peak: Float,
        meterSegments: Int,
        spectrumBands: FloatArray
    ) {
        val nextFrame = CaptureLiveFrame(
            level = level,
            peak = peak,
            meterSegments = meterSegments,
            spectrumBands = spectrumBands
        )
        liveFrame = nextFrame
        publishDirectMeterFrame(nextFrame)
    }

    fun syncLiveFrameFromState(source: CaptureUiState = state) {
        val nextFrame = CaptureLiveFrame(
            level = source.level,
            peak = source.peak,
            meterSegments = source.meterSegments,
            spectrumBands = source.spectrumBands
        )
        liveFrame = nextFrame
        publishDirectMeterFrame(nextFrame)
    }

    fun applyLiveFrameToState() {
        val frame = liveFrame
        state = state.copy(
            level = frame.level,
            peak = frame.peak,
            meterSegments = frame.meterSegments,
            spectrumBands = frame.spectrumBands
        )
    }

    fun resetLevels(statusText: String = state.statusText, activeMode: String = "IDLE") {
        state = state.copy(
            statusText = statusText,
            level = 0f,
            peak = 0f,
            isCapturing = false,
            meterSegments = 0,
            activeMode = activeMode
        )
        liveFrame = CaptureLiveFrame()
        publishDirectMeterFrame(liveFrame)
    }

    fun registerDirectMeterFrameListener(listener: (CaptureLiveFrame) -> Unit) {
        val listenerCount: Int
        synchronized(directMeterFrameListeners) {
            directMeterFrameListeners.add(listener)
            listenerCount = directMeterFrameListeners.size
        }
        if (DEBUG_UI_VISIBILITY_LOGS) {
            AppLogger.i(TAG, "Direct meter listener registered: count=$listenerCount uiVisible=$uiVisible")
        }
        if (shouldPublishLiveUiFrames()) {
            listener(liveFrame)
        }
    }

    fun unregisterDirectMeterFrameListener(listener: (CaptureLiveFrame) -> Unit) {
        val listenerCount: Int
        synchronized(directMeterFrameListeners) {
            directMeterFrameListeners.remove(listener)
            listenerCount = directMeterFrameListeners.size
        }
        if (DEBUG_UI_VISIBILITY_LOGS) {
            AppLogger.i(TAG, "Direct meter listener unregistered: count=$listenerCount uiVisible=$uiVisible")
        }
    }

    private fun publishDirectMeterFrame(frame: CaptureLiveFrame) {
        if (!shouldPublishLiveUiFrames()) return
        val listeners = synchronized(directMeterFrameListeners) {
            directMeterFrameListeners.toList()
        }
        if (listeners.isEmpty()) return
        directMeterFrameDispatchCount += 1
        val now = android.os.SystemClock.uptimeMillis()
        if (
            DEBUG_UI_VISIBILITY_LOGS &&
                (
                    directMeterFrameDispatchCount == 1 ||
                        now - lastDirectMeterFrameDispatchLogAtMs >= DIRECT_FRAME_DISPATCH_LOG_INTERVAL_MS
                    )
        ) {
            lastDirectMeterFrameDispatchLogAtMs = now
            AppLogger.i(
                TAG,
                "Direct meter frame dispatched: count=$directMeterFrameDispatchCount listeners=${listeners.size} uiVisible=$uiVisible"
            )
        }
        val dispatch = {
            listeners.forEach { it(frame) }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            dispatch()
        } else {
            mainHandler.post(dispatch)
        }
    }
}
