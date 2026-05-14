package jp.linkserver.glyphvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CaptureUiState(
    val statusText: String = "",
    val level: Float = 0f,
    val peak: Float = 0f,
    val sensitivity: Float = 1.75f,
    val noiseGate: Float = 0.08f,
    val dynamics: Float = 1.45f,
    val outputGamma: Float = 1.8f,
    val toneFocus: Float = -0.2f,
    val smoothing: Float = 0.45f,
    val smoothingBalance: Float = 0f,
    val reverseDirection: Boolean = false,
    val peakHoldEnabled: Boolean = true,
    val isCapturing: Boolean = false,
    val meterSegments: Int = 0,
    val spectrumBands: FloatArray = FloatArray(0),
    val activeMode: String = "IDLE",
    val glyphMode: String = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice(),
    val binaryMode: Boolean = false,
    val baseIndicatorEnabled: Boolean = false,
    val levelAutoScale: Boolean = false,
    val spectrumAutoScale: Boolean = false,
    val allBrightnessAutoScale: Boolean = false,
    val autoScaleWindowSeconds: Float = 30f,
    val autoScaleOffset: Float = 0f,
    val latencyMs: Float = 0f,
    val defaultOutputLatencyMs: Float = 0f,
    val bluetoothLatencyMs: Float = 0f,
    val latencyAutoSwitchEnabled: Boolean = true,
    val isBluetoothOutputActive: Boolean = false,
    val mediaProjectionEnabled: Boolean = false,
    val glyphMeterPreviewEnabled: Boolean = false,
    val automaticUpdateCheckEnabled: Boolean = false,
    val nothingStyleEnabled: Boolean = false,
    val turnOffWhenBackDown: Boolean = false,
    val logMessage: String? = null
)

object CaptureUiStore {
    var state by mutableStateOf(CaptureUiState())
        private set

    fun update(transform: (CaptureUiState) -> CaptureUiState) {
        state = transform(state)
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
    }
}
