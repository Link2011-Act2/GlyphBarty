package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphVisualTuningKey

data class CaptureParameters(
    val config: CaptureConfig,
    val phone4bEmulationEnabled: Boolean,
    val debugDeviceProfileOverride: GlyphDeviceProfile?,
    val legacyAutoScaleEnabled: Boolean,
    val visualDynamicsOverrides: Map<GlyphVisualTuningKey, Float>,
    val defaultOutputLatencyMs: Float,
    val bluetoothLatencyMs: Float,
    val latencyAutoSwitchEnabled: Boolean
) {
    internal fun applyTo(state: CaptureUiState): CaptureUiState = config.applyToUiState(state).copy(
        phone4bEmulationEnabled = phone4bEmulationEnabled,
        debugDeviceProfileOverride = debugDeviceProfileOverride,
        legacyAutoScaleEnabled = legacyAutoScaleEnabled,
        visualDynamicsOverrides = visualDynamicsOverrides,
        defaultOutputLatencyMs = defaultOutputLatencyMs,
        bluetoothLatencyMs = bluetoothLatencyMs,
        latencyAutoSwitchEnabled = latencyAutoSwitchEnabled
    )

    companion object {
        internal fun from(state: CaptureUiState): CaptureParameters = CaptureParameters(
            config = state.toCaptureConfig(),
            phone4bEmulationEnabled = state.phone4bEmulationEnabled,
            debugDeviceProfileOverride = state.debugDeviceProfileOverride,
            legacyAutoScaleEnabled = state.legacyAutoScaleEnabled,
            visualDynamicsOverrides = state.visualDynamicsOverrides,
            defaultOutputLatencyMs = state.defaultOutputLatencyMs,
            bluetoothLatencyMs = state.bluetoothLatencyMs,
            latencyAutoSwitchEnabled = state.latencyAutoSwitchEnabled
        )
    }
}

data class UiPreferences(
    val mediaProjectionEnabled: Boolean,
    val glyphMeterPreviewEnabled: Boolean,
    val meterVisibleEnabled: Boolean,
    val lightweightMeterEnabled: Boolean,
    val spectrumMeterEnabled: Boolean,
    val nativeMeterViewEnabled: Boolean,
    val mainScreenUiIsolationEnabled: Boolean,
    val automaticUpdateCheckEnabled: Boolean,
    val batteryGlyphEnabled: Boolean,
    val syncWithNothingOsGlyphSettingEnabled: Boolean,
    val showPhone1GlyphDebugControlsEverywhere: Boolean,
    val autoEnablePhone1GlyphDebugOnStart: Boolean,
    val nothingStyleEnabled: Boolean,
    val experimentalMainUiEnabled: Boolean,
    val detailedHomeEnabled: Boolean
) {
    internal fun applyTo(state: CaptureUiState): CaptureUiState = state.copy(
        mediaProjectionEnabled = mediaProjectionEnabled,
        glyphMeterPreviewEnabled = glyphMeterPreviewEnabled,
        meterVisibleEnabled = meterVisibleEnabled,
        lightweightMeterEnabled = lightweightMeterEnabled,
        spectrumMeterEnabled = spectrumMeterEnabled,
        nativeMeterViewEnabled = nativeMeterViewEnabled,
        mainScreenUiIsolationEnabled = mainScreenUiIsolationEnabled,
        automaticUpdateCheckEnabled = automaticUpdateCheckEnabled,
        batteryGlyphEnabled = batteryGlyphEnabled,
        syncWithNothingOsGlyphSettingEnabled = syncWithNothingOsGlyphSettingEnabled,
        showPhone1GlyphDebugControlsEverywhere = showPhone1GlyphDebugControlsEverywhere,
        autoEnablePhone1GlyphDebugOnStart = autoEnablePhone1GlyphDebugOnStart,
        nothingStyleEnabled = nothingStyleEnabled,
        experimentalMainUiEnabled = experimentalMainUiEnabled,
        detailedHomeEnabled = detailedHomeEnabled
    )

    companion object {
        internal fun from(state: CaptureUiState): UiPreferences = UiPreferences(
            mediaProjectionEnabled = state.mediaProjectionEnabled,
            glyphMeterPreviewEnabled = state.glyphMeterPreviewEnabled,
            meterVisibleEnabled = state.meterVisibleEnabled,
            lightweightMeterEnabled = state.lightweightMeterEnabled,
            spectrumMeterEnabled = state.spectrumMeterEnabled,
            nativeMeterViewEnabled = state.nativeMeterViewEnabled,
            mainScreenUiIsolationEnabled = state.mainScreenUiIsolationEnabled,
            automaticUpdateCheckEnabled = state.automaticUpdateCheckEnabled,
            batteryGlyphEnabled = state.batteryGlyphEnabled,
            syncWithNothingOsGlyphSettingEnabled = state.syncWithNothingOsGlyphSettingEnabled,
            showPhone1GlyphDebugControlsEverywhere = state.showPhone1GlyphDebugControlsEverywhere,
            autoEnablePhone1GlyphDebugOnStart = state.autoEnablePhone1GlyphDebugOnStart,
            nothingStyleEnabled = state.nothingStyleEnabled,
            experimentalMainUiEnabled = state.experimentalMainUiEnabled,
            detailedHomeEnabled = state.detailedHomeEnabled
        )
    }
}

data class CaptureRuntimeState(
    val statusText: String,
    val isCapturing: Boolean,
    val activeMode: String,
    val isBluetoothOutputActive: Boolean,
    val logMessage: String?,
    val pendingSpatialAudioWarning: SpatialAudioWarning?
) {
    internal fun applyTo(state: CaptureUiState): CaptureUiState = state.copy(
        statusText = statusText,
        isCapturing = isCapturing,
        activeMode = activeMode,
        isBluetoothOutputActive = isBluetoothOutputActive,
        logMessage = logMessage,
        pendingSpatialAudioWarning = pendingSpatialAudioWarning
    )

    companion object {
        internal fun from(state: CaptureUiState): CaptureRuntimeState = CaptureRuntimeState(
            statusText = state.statusText,
            isCapturing = state.isCapturing,
            activeMode = state.activeMode,
            isBluetoothOutputActive = state.isBluetoothOutputActive,
            logMessage = state.logMessage,
            pendingSpatialAudioWarning = state.pendingSpatialAudioWarning
        )
    }
}

data class CaptureLiveFrame(
    val level: Float = 0f,
    val peak: Float = 0f,
    val meterSegments: Int = 0,
    val spectrumBands: FloatArray = FloatArray(0)
) {
    internal fun applyTo(state: CaptureUiState): CaptureUiState = state.copy(
        level = level,
        peak = peak,
        meterSegments = meterSegments,
        spectrumBands = spectrumBands
    )

    companion object {
        internal fun from(state: CaptureUiState): CaptureLiveFrame = CaptureLiveFrame(
            level = state.level,
            peak = state.peak,
            meterSegments = state.meterSegments,
            spectrumBands = state.spectrumBands
        )
    }
}

typealias CaptureFrame = CaptureLiveFrame

internal data class CaptureStateSlices(
    val parameters: CaptureParameters,
    val uiPreferences: UiPreferences,
    val runtime: CaptureRuntimeState,
    val compatibilityFrame: CaptureFrame
) {
    fun toFacadeState(): CaptureUiState {
        var state = CaptureUiState()
        state = parameters.applyTo(state)
        state = uiPreferences.applyTo(state)
        state = runtime.applyTo(state)
        return compatibilityFrame.applyTo(state)
    }

    companion object {
        fun from(state: CaptureUiState): CaptureStateSlices = CaptureStateSlices(
            parameters = CaptureParameters.from(state),
            uiPreferences = UiPreferences.from(state),
            runtime = CaptureRuntimeState.from(state),
            compatibilityFrame = CaptureFrame.from(state)
        )
    }
}

internal class CaptureStateRepository(initialState: CaptureUiState = CaptureUiState()) {
    private val lock = Any()
    private var slices = CaptureStateSlices.from(initialState)
    private var facadeState = initialState

    fun state(): CaptureUiState = synchronized(lock) { facadeState }

    fun slices(): CaptureStateSlices = synchronized(lock) { slices }

    fun update(transform: (CaptureUiState) -> CaptureUiState): CaptureUiState = synchronized(lock) {
        val next = transform(facadeState)
        slices = CaptureStateSlices.from(next)
        facadeState = next
        next
    }

    fun updateRuntime(
        transform: (CaptureRuntimeState) -> CaptureRuntimeState
    ): CaptureUiState = synchronized(lock) {
        slices = slices.copy(runtime = transform(slices.runtime))
        facadeState = slices.runtime.applyTo(facadeState)
        facadeState
    }
}

internal class CaptureFrameRepository(initialFrame: CaptureFrame = CaptureFrame()) {
    @Volatile
    private var current = initialFrame

    fun latest(): CaptureFrame = current

    fun publish(frame: CaptureFrame): CaptureFrame {
        current = frame
        return frame
    }
}
