package jp.linkserver.glyphvisualizer.audio

enum class VisualizerCaptureHealth {
    WAITING,
    API_PENDING,
    SIGNAL,
    NO_SIGNAL,
    API_ERROR
}

/** Successful silence is ambiguous, whereas persistent API failures are capture errors. */
internal class VisualizerSignalWatchdog(private val activeSinceMs: Long) {
    private var lastSignalAtMs = activeSinceMs
    private var signalSeen = false
    private var waveformErrorSinceMs: Long? = null
    private var measurementErrorSinceMs: Long? = null

    fun observe(
        nowMs: Long,
        waveformSucceeded: Boolean,
        measurementSucceeded: Boolean,
        hasSignal: Boolean,
        musicActive: Boolean,
        signalWatchdogEnabled: Boolean
    ): VisualizerCaptureHealth {
        waveformErrorSinceMs = if (waveformSucceeded) null else waveformErrorSinceMs ?: nowMs
        measurementErrorSinceMs = if (measurementSucceeded) null else measurementErrorSinceMs ?: nowMs
        if (hasSignal && waveformSucceeded && measurementSucceeded) {
            signalSeen = true
            lastSignalAtMs = nowMs
        }
        if (waveformErrorSinceMs?.let { nowMs - it >= 2_000L } == true ||
            measurementErrorSinceMs?.let { nowMs - it >= 2_000L } == true
        ) {
            return VisualizerCaptureHealth.API_ERROR
        }
        // Never classify unsuccessful reads (or a stale buffer) as successful silence.
        if (!waveformSucceeded || !measurementSucceeded) return VisualizerCaptureHealth.API_PENDING
        if (hasSignal) return VisualizerCaptureHealth.SIGNAL
        val silenceThresholdMs = if (signalSeen) 5_000L else 1_800L
        return if (signalWatchdogEnabled && musicActive && nowMs - lastSignalAtMs >= silenceThresholdMs) {
            VisualizerCaptureHealth.NO_SIGNAL
        } else {
            VisualizerCaptureHealth.WAITING
        }
    }
}
