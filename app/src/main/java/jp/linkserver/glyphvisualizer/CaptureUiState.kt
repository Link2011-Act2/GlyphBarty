package jp.linkserver.glyphvisualizer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CaptureUiState(
    val statusText: String = "Preparing Glyph session...",
    val level: Float = 0f,
    val peak: Float = 0f,
    val sensitivity: Float = 1.75f,
    val noiseGate: Float = 0.08f,
    val dynamics: Float = 1.45f,
    val toneFocus: Float = -0.2f,
    val smoothing: Float = 0.55f,
    val smoothingBalance: Float = 0f,
    val reverseDirection: Boolean = true,
    val peakHoldEnabled: Boolean = true,
    val isCapturing: Boolean = false,
    val meterSegments: Int = 0,
    val spectrumBands: FloatArray = FloatArray(0),
    val activeMode: String = "IDLE",
    val glyphMode: String = "C1_LINEAR",
    val binaryMode: Boolean = false,
    val spectrumAutoScale: Boolean = false,
    val allBrightnessAutoScale: Boolean = false,
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
