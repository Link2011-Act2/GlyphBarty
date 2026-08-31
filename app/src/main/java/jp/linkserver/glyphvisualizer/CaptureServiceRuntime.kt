package jp.linkserver.glyphvisualizer

internal fun defaultServiceCaptureConfig(): CaptureConfig = CaptureConfig(
    sensitivity = 1.75f,
    noiseGate = 0.08f,
    dynamics = 1.45f,
    outputGamma = 1.8f,
    toneFocus = -0.1f,
    smoothing = 0.55f,
    smoothingBalance = 0f,
    reverseDirection = false,
    peakHoldEnabled = true,
    glyphMode = GlyphDeviceCatalog.defaultGlyphModeForCurrentDevice(),
    fillOtherGlyphLights = false,
    phone1ClassicCSplitEnabled = true,
    binaryMode = false,
    baseIndicatorEnabled = false,
    recordingLightIncluded = false,
    levelAutoScale = true,
    spectrumAutoScale = true,
    allBrightnessAutoScale = true,
    autoScaleWindowSeconds = 30f,
    autoScaleOffset = 0f,
    latencyMs = 0f,
    mediaPlaybackOnlyEnabled = false,
    experimentalVisualizerStabilizationEnabled = false,
    experimentalVisualizerSignalWatchdogEnabled = false,
    experimentalSpectrumDecayEnabled = false,
    experimentalPerformanceOptimizationsEnabled = true,
    matrixSmoothMotionEnabled = false,
    oscilloscopeAutoTimeAxisEnabled = false,
    turnOffWhenBackDown = false
)

internal fun CaptureConfig.applyToServicePublishedUiState(state: CaptureUiState): CaptureUiState {
    return applyToUiState(state).copy(
        // These fields were intentionally not published by the legacy update command path.
        latencyMs = state.latencyMs,
        matrixSmoothMotionEnabled = state.matrixSmoothMotionEnabled
    )
}

internal fun CaptureConfig.applyToStartedUiState(
    state: CaptureUiState,
    statusText: String,
    activeMode: String
): CaptureUiState {
    return applyToUiState(state).copy(
        statusText = statusText,
        isCapturing = true,
        activeMode = activeMode
    )
}

internal fun CaptureConfig.applyToStoppedUiState(
    state: CaptureUiState,
    statusText: String
): CaptureUiState {
    return applyToUiState(state).copy(
        level = 0f,
        peak = 0f,
        meterSegments = 0,
        spectrumBands = FloatArray(0),
        isCapturing = false,
        activeMode = "IDLE",
        statusText = statusText,
        // Preserve the exact field omissions of the previous stop-state publication path.
        baseIndicatorEnabled = state.baseIndicatorEnabled,
        recordingLightIncluded = state.recordingLightIncluded
    )
}

internal data class CaptureSessionState(
    val requestId: Int = 0,
    val startSource: VisualizerStartSource = VisualizerStartSource.APP,
    val startActionAtMs: Long = 0L
)

internal class CaptureSessionCoordinator {
    private val lock = Any()
    @Volatile
    private var current = CaptureSessionState()

    fun snapshot(): CaptureSessionState = current

    fun beginVisualizer(source: VisualizerStartSource, actionAtMs: Long): CaptureSessionState =
        synchronized(lock) {
            current = current.copy(
                requestId = current.requestId + 1,
                startSource = source,
                startActionAtMs = actionAtMs
            )
            current
        }

    fun invalidate(): CaptureSessionState = synchronized(lock) {
        current = current.copy(requestId = current.requestId + 1)
        current
    }

    fun isCurrent(requestId: Int): Boolean = current.requestId == requestId
}

internal object CaptureRetryPolicy {
    fun maxAttempts(bluetoothOutputActive: Boolean, musicActive: Boolean): Int {
        return if (bluetoothOutputActive && musicActive) 6 else 4
    }

    fun retryDelayMs(
        attempt: Int,
        bluetoothOutputActive: Boolean,
        musicActive: Boolean
    ): Long {
        return if (bluetoothOutputActive && musicActive) 700L * attempt else 160L * attempt
    }

    fun routeRestartSuppressionMs(
        bluetoothOutputActive: Boolean,
        musicActive: Boolean
    ): Long {
        return if (bluetoothOutputActive && musicActive) 4_000L else 1_500L
    }
}

internal data class LatencyDrain<T>(
    val frames: List<T>,
    val nextDueAtMs: Long?
)

internal class LatencyFrameScheduler<T>(private val dueAtMs: (T) -> Long) {
    private val pending = ArrayDeque<T>()

    fun enqueue(frame: T) {
        pending.addLast(frame)
    }

    fun clear() {
        pending.clear()
    }

    fun drainAllDue(nowMs: Long, forceAll: Boolean = false): LatencyDrain<T> {
        val dueFrames = mutableListOf<T>()
        while (pending.isNotEmpty()) {
            val next = pending.first()
            if (!forceAll && dueAtMs(next) > nowMs) break
            dueFrames += pending.removeFirst()
        }
        return LatencyDrain(dueFrames, pending.firstOrNull()?.let(dueAtMs))
    }

    fun drainLatestDue(nowMs: Long, accept: (T) -> Boolean = { true }): LatencyDrain<T> {
        var latest: T? = null
        while (pending.isNotEmpty()) {
            val next = pending.first()
            if (!accept(next)) {
                pending.removeFirst()
                continue
            }
            if (dueAtMs(next) > nowMs) break
            latest = pending.removeFirst()
        }
        return LatencyDrain(listOfNotNull(latest), pending.firstOrNull()?.let(dueAtMs))
    }

    fun isEmpty(): Boolean = pending.isEmpty()

    fun nextDueAtMs(): Long? = pending.firstOrNull()?.let(dueAtMs)
}
