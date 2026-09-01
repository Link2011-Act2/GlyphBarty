package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile

/** Stable, typed input boundary for the root Compose tree. */
internal data class VisualizerUiState(
    val capture: CaptureUiState,
    val initialSetupPending: Boolean,
    val heroTitle: String,
    val deviceProfile: GlyphDeviceProfile,
    val actualDeviceProfile: GlyphDeviceProfile,
    val batteryGlyphSupported: Boolean,
    val visualDynamics: Float,
    val visualDynamicsOverridden: Boolean,
    val showPhone1GlyphDebugPermissionDialog: Boolean
) {
    val isPhone3Device: Boolean
        get() = deviceProfile == GlyphDeviceProfile.PHONE3_MATRIX
    val isPhone4aProDevice: Boolean
        get() = deviceProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX
    val isPhone2aDevice: Boolean
        get() = deviceProfile == GlyphDeviceProfile.PHONE2A
    val isPhone3aDevice: Boolean
        get() = deviceProfile == GlyphDeviceProfile.PHONE3A
    val isPhone4aDevice: Boolean
        get() = deviceProfile == GlyphDeviceProfile.PHONE4A
    val isPhone1Device: Boolean
        get() = Phone1GlyphDebugHelper.supports(deviceProfile)
}

internal data class VisualizerUiActions(
    val onInitialSetupCompleted: () -> Unit,
    val visualTuning: VisualTuningActions,
    val settings: VisualizerSettingsActions,
    val capture: VisualizerCaptureActions,
    val dialogs: VisualizerDialogActions
)

internal data class VisualTuningActions(
    val onVisualDynamicsChanged: (Float) -> Unit,
    val onVisualDynamicsChangeFinished: () -> Unit,
    val onVisualDynamicsReset: () -> Unit
)

internal data class VisualizerSettingsActions(
    val onSensitivityChanged: (Float) -> Unit,
    val onNoiseGateChanged: (Float) -> Unit,
    val onDynamicsChanged: (Float) -> Unit,
    val onOutputGammaChanged: (Float) -> Unit,
    val onSmoothingChanged: (Float) -> Unit,
    val onSmoothingBalanceChanged: (Float) -> Unit,
    val onToneFocusChanged: (Float) -> Unit,
    val onAutoScaleWindowSecondsChanged: (Float) -> Unit,
    val onAutoScaleWindowSecondsChangeFinished: () -> Unit,
    val onAutoScaleOffsetChanged: (Float) -> Unit,
    val onAutoScaleOffsetChangeFinished: () -> Unit,
    val onLatencyMsChanged: (Float) -> Unit,
    val onLatencyMsChangeFinished: () -> Unit,
    val onLatencyAutoSwitchChanged: (Boolean) -> Unit,
    val onGlyphMeterPreviewEnabledChanged: (Boolean) -> Unit,
    val onMeterVisibleEnabledChanged: (Boolean) -> Unit,
    val onLightweightMeterEnabledChanged: (Boolean) -> Unit,
    val onSpectrumMeterEnabledChanged: (Boolean) -> Unit,
    val onNativeMeterViewEnabledChanged: (Boolean) -> Unit,
    val onAutomaticUpdateCheckEnabledChanged: (Boolean) -> Unit,
    val onBatteryGlyphEnabledChanged: (Boolean) -> Unit,
    val onSyncWithNothingOsGlyphSettingEnabledChanged: (Boolean) -> Unit,
    val onMediaPlaybackOnlyEnabledChanged: (Boolean) -> Unit,
    val onExperimentalVisualizerStabilizationEnabledChanged: (Boolean) -> Unit,
    val onExperimentalVisualizerSignalWatchdogEnabledChanged: (Boolean) -> Unit,
    val onMatrixSmoothMotionEnabledChanged: (Boolean) -> Unit,
    val onOscilloscopeAutoTimeAxisEnabledChanged: (Boolean) -> Unit,
    val onAutoEnablePhone1GlyphDebugOnStartChanged: (Boolean) -> Unit,
    val onRecordingLightBehaviorChanged: (RecordingLightBehavior) -> Unit,
    val onPhone4bEmulationEnabledChanged: (Boolean) -> Unit,
    val onDebugDeviceProfileOverrideChanged: (GlyphDeviceProfile?) -> Unit,
    val onReverseDirectionChanged: (Boolean) -> Unit,
    val onGlyphModeChanged: (String) -> Unit,
    val onFillOtherGlyphLightsChanged: (Boolean) -> Unit,
    val onPhone1ClassicCSplitEnabledChanged: (Boolean) -> Unit,
    val onBinaryModeChanged: (Boolean) -> Unit,
    val onLevelAutoScaleChanged: (Boolean) -> Unit,
    val onSpectrumAutoScaleChanged: (Boolean) -> Unit,
    val onAllBrightnessAutoScaleChanged: (Boolean) -> Unit,
    val onExperimentalAdaptiveAutoScaleEnabledChanged: (Boolean) -> Unit,
    val onMediaProjectionEnabledChanged: (Boolean) -> Unit,
    val onNothingStyleEnabledChanged: (Boolean) -> Unit,
    val onExperimentalMainUiEnabledChanged: (Boolean) -> Unit,
    val onDetailedHomeEnabledChanged: (Boolean) -> Unit,
    val onTurnOffWhenBackDownChanged: (Boolean) -> Unit,
    val onResetParametersClick: () -> Unit,
    val onExportParametersClick: () -> Unit,
    val onImportParametersClick: () -> Unit
)

internal data class VisualizerCaptureActions(
    val onStartVisualizerClick: () -> Unit,
    val onStartProjectionClick: () -> Unit,
    val onEnablePhone1GlyphDebugClick: () -> Unit,
    val onStopClick: () -> Unit
)

internal data class VisualizerDialogActions(
    val onDismissLog: () -> Unit,
    val onDismissPhone1GlyphDebugPermissionDialog: () -> Unit
)
