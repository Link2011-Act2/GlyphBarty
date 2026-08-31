package jp.linkserver.glyphvisualizer

import android.content.SharedPreferences
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphVisualTuningKey
import jp.linkserver.glyphvisualizer.update.isIntDevBuild
import org.json.JSONObject

data class PersistedSettings(
    val sensitivity: Float,
    val noiseGate: Float,
    val dynamics: Float,
    val outputGamma: Float,
    val toneFocus: Float,
    val smoothing: Float,
    val smoothingBalance: Float,
    val reverseDirection: Boolean,
    val peakHoldEnabled: Boolean,
    val glyphMode: String,
    val fillOtherGlyphLights: Boolean,
    val phone1ClassicCSplitEnabled: Boolean,
    val binaryMode: Boolean,
    val baseIndicatorEnabled: Boolean,
    val recordingLightIncluded: Boolean,
    val phone4bEmulationEnabled: Boolean,
    val debugDeviceProfileOverride: GlyphDeviceProfile?,
    val levelAutoScale: Boolean,
    val spectrumAutoScale: Boolean,
    val allBrightnessAutoScale: Boolean,
    val experimentalAdaptiveAutoScaleEnabled: Boolean,
    val visualDynamicsOverrides: Map<GlyphVisualTuningKey, Float>,
    val autoScaleWindowSeconds: Float,
    val autoScaleOffset: Float,
    val latencyMs: Float,
    val defaultOutputLatencyMs: Float,
    val bluetoothLatencyMs: Float,
    val latencyAutoSwitchEnabled: Boolean,
    val mediaProjectionEnabled: Boolean,
    val glyphMeterPreviewEnabled: Boolean,
    val meterVisibleEnabled: Boolean,
    val lightweightMeterEnabled: Boolean,
    val spectrumMeterEnabled: Boolean,
    val nativeMeterViewEnabled: Boolean,
    val mainScreenUiIsolationEnabled: Boolean,
    val automaticUpdateCheckEnabled: Boolean,
    val mediaPlaybackOnlyEnabled: Boolean,
    val experimentalVisualizerStabilizationEnabled: Boolean,
    val experimentalVisualizerSignalWatchdogEnabled: Boolean,
    val experimentalSpectrumDecayEnabled: Boolean,
    val experimentalPerformanceOptimizationsEnabled: Boolean,
    val matrixSmoothMotionEnabled: Boolean,
    val oscilloscopeAutoTimeAxisEnabled: Boolean,
    val showPhone1GlyphDebugControlsEverywhere: Boolean,
    val autoEnablePhone1GlyphDebugOnStart: Boolean,
    val nothingStyleEnabled: Boolean,
    val experimentalMainUiEnabled: Boolean,
    val detailedHomeEnabled: Boolean,
    val turnOffWhenBackDown: Boolean
)

data class EffectiveSettings(
    val persisted: PersistedSettings,
    val state: CaptureUiState
)

internal object PersistedSettingsSchema {
    fun defaults(): PersistedSettings = fromState(CaptureUiState())

    fun fromState(state: CaptureUiState): PersistedSettings = PersistedSettings(
        sensitivity = state.sensitivity,
        noiseGate = state.noiseGate,
        dynamics = state.dynamics,
        outputGamma = state.outputGamma,
        toneFocus = state.toneFocus,
        smoothing = state.smoothing,
        smoothingBalance = state.smoothingBalance,
        reverseDirection = state.reverseDirection,
        peakHoldEnabled = state.peakHoldEnabled,
        glyphMode = state.glyphMode,
        fillOtherGlyphLights = state.fillOtherGlyphLights,
        phone1ClassicCSplitEnabled = state.phone1ClassicCSplitEnabled,
        binaryMode = state.binaryMode,
        baseIndicatorEnabled = state.baseIndicatorEnabled,
        recordingLightIncluded = state.recordingLightIncluded,
        phone4bEmulationEnabled = state.phone4bEmulationEnabled,
        debugDeviceProfileOverride = state.debugDeviceProfileOverride,
        levelAutoScale = state.levelAutoScale,
        spectrumAutoScale = state.spectrumAutoScale,
        allBrightnessAutoScale = state.allBrightnessAutoScale,
        experimentalAdaptiveAutoScaleEnabled = state.experimentalAdaptiveAutoScaleEnabled,
        visualDynamicsOverrides = state.visualDynamicsOverrides,
        autoScaleWindowSeconds = state.autoScaleWindowSeconds,
        autoScaleOffset = state.autoScaleOffset,
        latencyMs = state.latencyMs,
        defaultOutputLatencyMs = state.defaultOutputLatencyMs,
        bluetoothLatencyMs = state.bluetoothLatencyMs,
        latencyAutoSwitchEnabled = state.latencyAutoSwitchEnabled,
        mediaProjectionEnabled = state.mediaProjectionEnabled,
        glyphMeterPreviewEnabled = state.glyphMeterPreviewEnabled,
        meterVisibleEnabled = state.meterVisibleEnabled,
        lightweightMeterEnabled = state.lightweightMeterEnabled,
        spectrumMeterEnabled = state.spectrumMeterEnabled,
        nativeMeterViewEnabled = state.nativeMeterViewEnabled,
        mainScreenUiIsolationEnabled = state.mainScreenUiIsolationEnabled,
        automaticUpdateCheckEnabled = state.automaticUpdateCheckEnabled,
        mediaPlaybackOnlyEnabled = state.mediaPlaybackOnlyEnabled,
        experimentalVisualizerStabilizationEnabled =
            state.experimentalVisualizerStabilizationEnabled,
        experimentalVisualizerSignalWatchdogEnabled =
            state.experimentalVisualizerSignalWatchdogEnabled,
        experimentalSpectrumDecayEnabled = state.experimentalSpectrumDecayEnabled,
        experimentalPerformanceOptimizationsEnabled =
            state.experimentalPerformanceOptimizationsEnabled,
        matrixSmoothMotionEnabled = state.matrixSmoothMotionEnabled,
        oscilloscopeAutoTimeAxisEnabled = state.oscilloscopeAutoTimeAxisEnabled,
        showPhone1GlyphDebugControlsEverywhere = state.showPhone1GlyphDebugControlsEverywhere,
        autoEnablePhone1GlyphDebugOnStart = state.autoEnablePhone1GlyphDebugOnStart,
        nothingStyleEnabled = state.nothingStyleEnabled,
        experimentalMainUiEnabled = state.experimentalMainUiEnabled,
        detailedHomeEnabled = state.detailedHomeEnabled,
        turnOffWhenBackDown = state.turnOffWhenBackDown
    )

    fun toRawState(settings: PersistedSettings): CaptureUiState = CaptureUiState(
        sensitivity = settings.sensitivity,
        noiseGate = settings.noiseGate,
        dynamics = settings.dynamics,
        outputGamma = settings.outputGamma,
        toneFocus = settings.toneFocus,
        smoothing = settings.smoothing,
        smoothingBalance = settings.smoothingBalance,
        reverseDirection = settings.reverseDirection,
        peakHoldEnabled = settings.peakHoldEnabled,
        glyphMode = settings.glyphMode,
        fillOtherGlyphLights = settings.fillOtherGlyphLights,
        phone1ClassicCSplitEnabled = settings.phone1ClassicCSplitEnabled,
        binaryMode = settings.binaryMode,
        baseIndicatorEnabled = settings.baseIndicatorEnabled,
        recordingLightIncluded = settings.recordingLightIncluded,
        phone4bEmulationEnabled = settings.phone4bEmulationEnabled,
        debugDeviceProfileOverride = settings.debugDeviceProfileOverride,
        levelAutoScale = settings.levelAutoScale,
        spectrumAutoScale = settings.spectrumAutoScale,
        allBrightnessAutoScale = settings.allBrightnessAutoScale,
        experimentalAdaptiveAutoScaleEnabled = settings.experimentalAdaptiveAutoScaleEnabled,
        visualDynamicsOverrides = settings.visualDynamicsOverrides,
        autoScaleWindowSeconds = settings.autoScaleWindowSeconds,
        autoScaleOffset = settings.autoScaleOffset,
        latencyMs = settings.latencyMs,
        defaultOutputLatencyMs = settings.defaultOutputLatencyMs,
        bluetoothLatencyMs = settings.bluetoothLatencyMs,
        latencyAutoSwitchEnabled = settings.latencyAutoSwitchEnabled,
        mediaProjectionEnabled = settings.mediaProjectionEnabled,
        glyphMeterPreviewEnabled = settings.glyphMeterPreviewEnabled,
        meterVisibleEnabled = settings.meterVisibleEnabled,
        lightweightMeterEnabled = settings.lightweightMeterEnabled,
        spectrumMeterEnabled = settings.spectrumMeterEnabled,
        nativeMeterViewEnabled = settings.nativeMeterViewEnabled,
        mainScreenUiIsolationEnabled = settings.mainScreenUiIsolationEnabled,
        automaticUpdateCheckEnabled = settings.automaticUpdateCheckEnabled,
        mediaPlaybackOnlyEnabled = settings.mediaPlaybackOnlyEnabled,
        experimentalVisualizerStabilizationEnabled =
            settings.experimentalVisualizerStabilizationEnabled,
        experimentalVisualizerSignalWatchdogEnabled =
            settings.experimentalVisualizerSignalWatchdogEnabled,
        experimentalSpectrumDecayEnabled = settings.experimentalSpectrumDecayEnabled,
        experimentalPerformanceOptimizationsEnabled =
            settings.experimentalPerformanceOptimizationsEnabled,
        matrixSmoothMotionEnabled = settings.matrixSmoothMotionEnabled,
        oscilloscopeAutoTimeAxisEnabled = settings.oscilloscopeAutoTimeAxisEnabled,
        showPhone1GlyphDebugControlsEverywhere = settings.showPhone1GlyphDebugControlsEverywhere,
        autoEnablePhone1GlyphDebugOnStart = settings.autoEnablePhone1GlyphDebugOnStart,
        nothingStyleEnabled = settings.nothingStyleEnabled,
        experimentalMainUiEnabled = settings.experimentalMainUiEnabled,
        detailedHomeEnabled = settings.detailedHomeEnabled,
        turnOffWhenBackDown = settings.turnOffWhenBackDown
    )

    fun resolve(settings: PersistedSettings): EffectiveSettings {
        val rawState = toRawState(settings)
        val recordingLightNormalized = rawState.withRecordingLightBehavior(
            resolveRecordingLightBehavior(
                baseIndicatorEnabled = rawState.baseIndicatorEnabled,
                recordingLightIncluded = rawState.recordingLightIncluded
            )
        )
        val effectiveState = recordingLightNormalized.copy(
            turnOffWhenBackDown = false,
            mainScreenUiIsolationEnabled = true,
            debugDeviceProfileOverride = if (isIntDevBuild()) {
                rawState.debugDeviceProfileOverride
            } else {
                null
            },
            showPhone1GlyphDebugControlsEverywhere = if (isIntDevBuild()) {
                rawState.showPhone1GlyphDebugControlsEverywhere
            } else {
                false
            }
        )
        return EffectiveSettings(persisted = settings, state = effectiveState)
    }

    fun mergeForSave(
        previous: PersistedSettings,
        state: CaptureUiState
    ): PersistedSettings {
        val incoming = fromState(state)
        val previousEffective = resolve(previous).state
        val recordingBehaviorChanged =
            state.baseIndicatorEnabled != previousEffective.baseIndicatorEnabled ||
                state.recordingLightIncluded != previousEffective.recordingLightIncluded

        return incoming.copy(
            baseIndicatorEnabled = if (recordingBehaviorChanged) {
                incoming.baseIndicatorEnabled
            } else {
                previous.baseIndicatorEnabled
            },
            recordingLightIncluded = if (recordingBehaviorChanged) {
                incoming.recordingLightIncluded
            } else {
                previous.recordingLightIncluded
            },
            turnOffWhenBackDown = preserveRawWhenEffectiveUnchanged(
                incoming = incoming.turnOffWhenBackDown,
                previousRaw = previous.turnOffWhenBackDown,
                previousEffective = previousEffective.turnOffWhenBackDown
            ),
            mainScreenUiIsolationEnabled = preserveRawWhenEffectiveUnchanged(
                incoming = incoming.mainScreenUiIsolationEnabled,
                previousRaw = previous.mainScreenUiIsolationEnabled,
                previousEffective = previousEffective.mainScreenUiIsolationEnabled
            ),
            debugDeviceProfileOverride = preserveRawWhenEffectiveUnchanged(
                incoming = incoming.debugDeviceProfileOverride,
                previousRaw = previous.debugDeviceProfileOverride,
                previousEffective = previousEffective.debugDeviceProfileOverride
            ),
            showPhone1GlyphDebugControlsEverywhere = preserveRawWhenEffectiveUnchanged(
                incoming = incoming.showPhone1GlyphDebugControlsEverywhere,
                previousRaw = previous.showPhone1GlyphDebugControlsEverywhere,
                previousEffective = previousEffective.showPhone1GlyphDebugControlsEverywhere
            )
        )
    }

    private fun <T> preserveRawWhenEffectiveUnchanged(
        incoming: T,
        previousRaw: T,
        previousEffective: T
    ): T = if (incoming == previousEffective) previousRaw else incoming
}

internal object PersistedSettingsPreferenceCodec {
    fun read(preferences: SharedPreferences): PersistedSettings {
        val defaults = PersistedSettingsSchema.defaults()
        val legacyNormalLatencyMs = loadLegacyLatencyForRoute(
            preferences = preferences,
            bluetooth = false,
            defaultValue = defaults.defaultOutputLatencyMs
        )
        val legacyBluetoothLatencyMs = loadLegacyLatencyForRoute(
            preferences = preferences,
            bluetooth = true,
            defaultValue = defaults.bluetoothLatencyMs
        )
        return PersistedSettings(
            sensitivity = preferences.getFloatCompat(Keys.SENSITIVITY, defaults.sensitivity),
            noiseGate = preferences.getFloatCompat(Keys.NOISE_GATE, defaults.noiseGate),
            dynamics = preferences.getFloatCompat(Keys.DYNAMICS, defaults.dynamics),
            outputGamma = preferences.getFloatCompat(Keys.OUTPUT_GAMMA, defaults.outputGamma),
            toneFocus = preferences.getFloatCompat(Keys.TONE_FOCUS, defaults.toneFocus),
            smoothing = preferences.getFloatCompat(Keys.SMOOTHING, defaults.smoothing),
            smoothingBalance = preferences.getFloatCompat(
                Keys.SMOOTHING_BALANCE,
                defaults.smoothingBalance
            ),
            reverseDirection = preferences.getBoolean(
                Keys.REVERSE_DIRECTION,
                defaults.reverseDirection
            ),
            peakHoldEnabled = preferences.getBoolean(
                Keys.PEAK_HOLD_ENABLED,
                defaults.peakHoldEnabled
            ),
            glyphMode = preferences.getString(Keys.GLYPH_MODE, defaults.glyphMode)
                ?: defaults.glyphMode,
            fillOtherGlyphLights = preferences.getBoolean(
                Keys.FILL_OTHER_GLYPH_LIGHTS,
                defaults.fillOtherGlyphLights
            ),
            phone1ClassicCSplitEnabled = preferences.getBoolean(
                Keys.PHONE1_CLASSIC_C_SPLIT_ENABLED,
                defaults.phone1ClassicCSplitEnabled
            ),
            binaryMode = preferences.getBoolean(Keys.BINARY_MODE, defaults.binaryMode),
            baseIndicatorEnabled = preferences.getBoolean(
                Keys.BASE_INDICATOR_ENABLED,
                defaults.baseIndicatorEnabled
            ),
            recordingLightIncluded = preferences.getBoolean(
                Keys.RECORDING_LIGHT_INCLUDED,
                defaults.recordingLightIncluded
            ),
            phone4bEmulationEnabled = preferences.getBoolean(
                Keys.PHONE4B_EMULATION_ENABLED,
                defaults.phone4bEmulationEnabled
            ),
            debugDeviceProfileOverride = preferences.getString(
                Keys.DEBUG_DEVICE_PROFILE_OVERRIDE,
                null
            )?.let { savedName ->
                GlyphDeviceProfile.entries.firstOrNull { it.name == savedName }
            },
            levelAutoScale = preferences.getBoolean(
                Keys.LEVEL_AUTO_SCALE,
                defaults.levelAutoScale
            ),
            spectrumAutoScale = preferences.getBoolean(
                Keys.SPECTRUM_AUTO_SCALE,
                defaults.spectrumAutoScale
            ),
            allBrightnessAutoScale = preferences.getBoolean(
                Keys.ALL_BRIGHTNESS_AUTO_SCALE,
                defaults.allBrightnessAutoScale
            ),
            experimentalAdaptiveAutoScaleEnabled = preferences.getBoolean(
                Keys.EXPERIMENTAL_ADAPTIVE_AUTO_SCALE_ENABLED,
                defaults.experimentalAdaptiveAutoScaleEnabled
            ),
            visualDynamicsOverrides = decodeVisualDynamicsOverrides(
                preferences.getString(Keys.VISUAL_DYNAMICS_OVERRIDES, null)
            ),
            autoScaleWindowSeconds = preferences.getFloatCompat(
                Keys.AUTO_SCALE_WINDOW_SECONDS,
                defaults.autoScaleWindowSeconds
            ),
            autoScaleOffset = preferences.getFloatCompat(
                Keys.AUTO_SCALE_OFFSET,
                defaults.autoScaleOffset
            ),
            latencyMs = preferences.getFloatCompat(Keys.LATENCY_MS, defaults.latencyMs),
            defaultOutputLatencyMs = preferences.getFloatCompat(
                Keys.DEFAULT_OUTPUT_LATENCY_MS,
                legacyNormalLatencyMs
            ),
            bluetoothLatencyMs = preferences.getFloatCompat(
                Keys.BLUETOOTH_LATENCY_MS,
                legacyBluetoothLatencyMs
            ),
            latencyAutoSwitchEnabled = preferences.getBoolean(
                Keys.LATENCY_AUTO_SWITCH_ENABLED,
                defaults.latencyAutoSwitchEnabled
            ),
            mediaProjectionEnabled = preferences.getBoolean(
                Keys.MEDIA_PROJECTION_ENABLED,
                defaults.mediaProjectionEnabled
            ),
            glyphMeterPreviewEnabled = preferences.getBoolean(
                Keys.GLYPH_METER_PREVIEW_ENABLED,
                defaults.glyphMeterPreviewEnabled
            ),
            meterVisibleEnabled = preferences.getBoolean(
                Keys.METER_VISIBLE_ENABLED,
                defaults.meterVisibleEnabled
            ),
            lightweightMeterEnabled = preferences.getBoolean(
                Keys.LIGHTWEIGHT_METER_ENABLED,
                defaults.lightweightMeterEnabled
            ),
            spectrumMeterEnabled = preferences.getBoolean(
                Keys.SPECTRUM_METER_ENABLED,
                defaults.spectrumMeterEnabled
            ),
            nativeMeterViewEnabled = preferences.getBoolean(
                Keys.NATIVE_METER_VIEW_ENABLED,
                defaults.nativeMeterViewEnabled
            ),
            mainScreenUiIsolationEnabled = preferences.getBoolean(
                Keys.MAIN_SCREEN_UI_ISOLATION_ENABLED,
                defaults.mainScreenUiIsolationEnabled
            ),
            automaticUpdateCheckEnabled = preferences.getBoolean(
                Keys.AUTOMATIC_UPDATE_CHECK_ENABLED,
                defaults.automaticUpdateCheckEnabled
            ),
            mediaPlaybackOnlyEnabled = preferences.getBoolean(
                Keys.MEDIA_PLAYBACK_ONLY_ENABLED,
                defaults.mediaPlaybackOnlyEnabled
            ),
            experimentalVisualizerStabilizationEnabled = preferences.getBoolean(
                Keys.EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
                defaults.experimentalVisualizerStabilizationEnabled
            ),
            experimentalVisualizerSignalWatchdogEnabled = preferences.getBoolean(
                Keys.EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
                defaults.experimentalVisualizerSignalWatchdogEnabled
            ),
            experimentalSpectrumDecayEnabled = preferences.getBoolean(
                Keys.EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
                defaults.experimentalSpectrumDecayEnabled
            ),
            experimentalPerformanceOptimizationsEnabled = preferences.getBoolean(
                Keys.EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
                defaults.experimentalPerformanceOptimizationsEnabled
            ),
            matrixSmoothMotionEnabled = preferences.getBoolean(
                Keys.MATRIX_SMOOTH_MOTION_ENABLED,
                defaults.matrixSmoothMotionEnabled
            ),
            oscilloscopeAutoTimeAxisEnabled = preferences.getBoolean(
                Keys.OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
                defaults.oscilloscopeAutoTimeAxisEnabled
            ),
            showPhone1GlyphDebugControlsEverywhere = preferences.getBoolean(
                Keys.SHOW_PHONE1_GLYPH_DEBUG_CONTROLS_EVERYWHERE,
                defaults.showPhone1GlyphDebugControlsEverywhere
            ),
            autoEnablePhone1GlyphDebugOnStart = preferences.getBoolean(
                Keys.AUTO_ENABLE_PHONE1_GLYPH_DEBUG_ON_START,
                defaults.autoEnablePhone1GlyphDebugOnStart
            ),
            nothingStyleEnabled = preferences.getBoolean(
                Keys.NOTHING_STYLE_ENABLED,
                defaults.nothingStyleEnabled
            ),
            experimentalMainUiEnabled = preferences.getBoolean(
                Keys.EXPERIMENTAL_MAIN_UI_ENABLED,
                defaults.experimentalMainUiEnabled
            ),
            detailedHomeEnabled = preferences.getBoolean(
                Keys.DETAILED_HOME_ENABLED,
                defaults.detailedHomeEnabled
            ),
            turnOffWhenBackDown = preferences.getBoolean(
                Keys.TURN_OFF_WHEN_BACK_DOWN,
                defaults.turnOffWhenBackDown
            )
        )
    }

    fun write(editor: SharedPreferences.Editor, settings: PersistedSettings) {
        editor
            .putFloat(Keys.SENSITIVITY, settings.sensitivity)
            .putFloat(Keys.NOISE_GATE, settings.noiseGate)
            .putFloat(Keys.DYNAMICS, settings.dynamics)
            .putFloat(Keys.OUTPUT_GAMMA, settings.outputGamma)
            .putFloat(Keys.TONE_FOCUS, settings.toneFocus)
            .putFloat(Keys.SMOOTHING, settings.smoothing)
            .putFloat(Keys.SMOOTHING_BALANCE, settings.smoothingBalance)
            .putBoolean(Keys.REVERSE_DIRECTION, settings.reverseDirection)
            .putBoolean(Keys.PEAK_HOLD_ENABLED, settings.peakHoldEnabled)
            .putString(Keys.GLYPH_MODE, settings.glyphMode)
            .putBoolean(Keys.FILL_OTHER_GLYPH_LIGHTS, settings.fillOtherGlyphLights)
            .putBoolean(
                Keys.PHONE1_CLASSIC_C_SPLIT_ENABLED,
                settings.phone1ClassicCSplitEnabled
            )
            .putBoolean(Keys.BINARY_MODE, settings.binaryMode)
            .putBoolean(Keys.BASE_INDICATOR_ENABLED, settings.baseIndicatorEnabled)
            .putBoolean(Keys.RECORDING_LIGHT_INCLUDED, settings.recordingLightIncluded)
            .putBoolean(Keys.PHONE4B_EMULATION_ENABLED, settings.phone4bEmulationEnabled)
            .putString(
                Keys.DEBUG_DEVICE_PROFILE_OVERRIDE,
                settings.debugDeviceProfileOverride?.name
            )
            .putBoolean(Keys.LEVEL_AUTO_SCALE, settings.levelAutoScale)
            .putBoolean(Keys.SPECTRUM_AUTO_SCALE, settings.spectrumAutoScale)
            .putBoolean(Keys.ALL_BRIGHTNESS_AUTO_SCALE, settings.allBrightnessAutoScale)
            .putBoolean(
                Keys.EXPERIMENTAL_ADAPTIVE_AUTO_SCALE_ENABLED,
                settings.experimentalAdaptiveAutoScaleEnabled
            )
            .putString(
                Keys.VISUAL_DYNAMICS_OVERRIDES,
                encodeVisualDynamicsOverrides(settings.visualDynamicsOverrides)
            )
            .putFloat(Keys.AUTO_SCALE_WINDOW_SECONDS, settings.autoScaleWindowSeconds)
            .putFloat(Keys.AUTO_SCALE_OFFSET, settings.autoScaleOffset)
            .putFloat(Keys.LATENCY_MS, settings.latencyMs)
            .putFloat(Keys.DEFAULT_OUTPUT_LATENCY_MS, settings.defaultOutputLatencyMs)
            .putFloat(Keys.BLUETOOTH_LATENCY_MS, settings.bluetoothLatencyMs)
            .putBoolean(Keys.LATENCY_AUTO_SWITCH_ENABLED, settings.latencyAutoSwitchEnabled)
            .putBoolean(Keys.MEDIA_PROJECTION_ENABLED, settings.mediaProjectionEnabled)
            .putBoolean(Keys.GLYPH_METER_PREVIEW_ENABLED, settings.glyphMeterPreviewEnabled)
            .putBoolean(Keys.METER_VISIBLE_ENABLED, settings.meterVisibleEnabled)
            .putBoolean(Keys.LIGHTWEIGHT_METER_ENABLED, settings.lightweightMeterEnabled)
            .putBoolean(Keys.SPECTRUM_METER_ENABLED, settings.spectrumMeterEnabled)
            .putBoolean(Keys.NATIVE_METER_VIEW_ENABLED, settings.nativeMeterViewEnabled)
            .putBoolean(
                Keys.MAIN_SCREEN_UI_ISOLATION_ENABLED,
                settings.mainScreenUiIsolationEnabled
            )
            .putBoolean(
                Keys.AUTOMATIC_UPDATE_CHECK_ENABLED,
                settings.automaticUpdateCheckEnabled
            )
            .putBoolean(Keys.MEDIA_PLAYBACK_ONLY_ENABLED, settings.mediaPlaybackOnlyEnabled)
            .putBoolean(
                Keys.EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED,
                settings.experimentalVisualizerStabilizationEnabled
            )
            .putBoolean(
                Keys.EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED,
                settings.experimentalVisualizerSignalWatchdogEnabled
            )
            .putBoolean(
                Keys.EXPERIMENTAL_SPECTRUM_DECAY_ENABLED,
                settings.experimentalSpectrumDecayEnabled
            )
            .putBoolean(
                Keys.EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED,
                settings.experimentalPerformanceOptimizationsEnabled
            )
            .putBoolean(Keys.MATRIX_SMOOTH_MOTION_ENABLED, settings.matrixSmoothMotionEnabled)
            .putBoolean(
                Keys.OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED,
                settings.oscilloscopeAutoTimeAxisEnabled
            )
            .putBoolean(
                Keys.SHOW_PHONE1_GLYPH_DEBUG_CONTROLS_EVERYWHERE,
                settings.showPhone1GlyphDebugControlsEverywhere
            )
            .putBoolean(
                Keys.AUTO_ENABLE_PHONE1_GLYPH_DEBUG_ON_START,
                settings.autoEnablePhone1GlyphDebugOnStart
            )
            .putBoolean(Keys.NOTHING_STYLE_ENABLED, settings.nothingStyleEnabled)
            .putBoolean(Keys.EXPERIMENTAL_MAIN_UI_ENABLED, settings.experimentalMainUiEnabled)
            .putBoolean(Keys.DETAILED_HOME_ENABLED, settings.detailedHomeEnabled)
            .putBoolean(Keys.TURN_OFF_WHEN_BACK_DOWN, settings.turnOffWhenBackDown)
            .apply()
    }

    private fun loadLegacyLatencyForRoute(
        preferences: SharedPreferences,
        bluetooth: Boolean,
        defaultValue: Float
    ): Float {
        val presetIdKey = if (bluetooth) {
            Keys.BLUETOOTH_LATENCY_PRESET_ID
        } else {
            Keys.DEFAULT_OUTPUT_LATENCY_PRESET_ID
        }
        val presetStorageKey = when (preferences.getString(presetIdKey, null)) {
            "latency_preset_1" -> Keys.LATENCY_PRESET_1_MS
            "latency_preset_2" -> Keys.LATENCY_PRESET_2_MS
            "latency_preset_3" -> Keys.LATENCY_PRESET_3_MS
            else -> null
        }
        return if (presetStorageKey == null) {
            defaultValue
        } else {
            preferences.getFloatCompat(presetStorageKey, defaultValue)
        }
    }

    private fun SharedPreferences.getFloatCompat(key: String, defaultValue: Float): Float {
        val raw = all[key] ?: return defaultValue
        return when (raw) {
            is Float -> raw
            is Double -> raw.toFloat()
            is Int -> raw.toFloat()
            is Long -> raw.toFloat()
            is String -> raw.toFloatOrNull() ?: defaultValue
            is Number -> raw.toFloat()
            else -> defaultValue
        }
    }

    private object Keys {
        const val SENSITIVITY = "sensitivity"
        const val NOISE_GATE = "noise_gate"
        const val DYNAMICS = "dynamics"
        const val OUTPUT_GAMMA = "output_gamma"
        const val TONE_FOCUS = "tone_focus"
        const val SMOOTHING = "smoothing"
        const val SMOOTHING_BALANCE = "smoothing_balance"
        const val REVERSE_DIRECTION = "reverse_direction"
        const val PEAK_HOLD_ENABLED = "peak_hold_enabled"
        const val GLYPH_MODE = "glyph_mode"
        const val FILL_OTHER_GLYPH_LIGHTS = "fill_other_glyph_lights"
        const val PHONE1_CLASSIC_C_SPLIT_ENABLED = "phone1_classic_c_split_enabled"
        const val BINARY_MODE = "binary_mode"
        const val BASE_INDICATOR_ENABLED = "base_indicator_enabled"
        const val RECORDING_LIGHT_INCLUDED = "recording_light_included"
        const val PHONE4B_EMULATION_ENABLED = "phone4b_emulation_enabled"
        const val DEBUG_DEVICE_PROFILE_OVERRIDE = "debug_device_profile_override"
        const val LEVEL_AUTO_SCALE = "level_auto_scale"
        const val SPECTRUM_AUTO_SCALE = "spectrum_auto_scale"
        const val ALL_BRIGHTNESS_AUTO_SCALE = "all_brightness_auto_scale"
        const val EXPERIMENTAL_ADAPTIVE_AUTO_SCALE_ENABLED =
            "experimental_adaptive_auto_scale_enabled"
        const val VISUAL_DYNAMICS_OVERRIDES = "glyph_visual_dynamics_overrides_v1"
        const val AUTO_SCALE_WINDOW_SECONDS = "auto_scale_window_seconds"
        const val AUTO_SCALE_OFFSET = "auto_scale_offset"
        const val LATENCY_MS = "latency_ms"
        const val DEFAULT_OUTPUT_LATENCY_MS = "default_output_latency_ms"
        const val BLUETOOTH_LATENCY_MS = "bluetooth_latency_ms"
        const val LATENCY_AUTO_SWITCH_ENABLED = "latency_auto_switch_enabled"
        const val MEDIA_PROJECTION_ENABLED = "media_projection_enabled"
        const val GLYPH_METER_PREVIEW_ENABLED = "glyph_meter_preview_enabled"
        const val METER_VISIBLE_ENABLED = "meter_visible_enabled"
        const val LIGHTWEIGHT_METER_ENABLED = "lightweight_meter_enabled"
        const val SPECTRUM_METER_ENABLED = "spectrum_meter_enabled"
        const val NATIVE_METER_VIEW_ENABLED = "native_meter_view_enabled"
        const val MAIN_SCREEN_UI_ISOLATION_ENABLED = "main_screen_ui_isolation_enabled"
        const val AUTOMATIC_UPDATE_CHECK_ENABLED = "automatic_update_check_enabled"
        const val MEDIA_PLAYBACK_ONLY_ENABLED = "media_playback_only_enabled"
        const val EXPERIMENTAL_VISUALIZER_STABILIZATION_ENABLED =
            "experimental_visualizer_stabilization_enabled"
        const val EXPERIMENTAL_VISUALIZER_SIGNAL_WATCHDOG_ENABLED =
            "experimental_visualizer_signal_watchdog_enabled"
        const val EXPERIMENTAL_SPECTRUM_DECAY_ENABLED =
            "experimental_spectrum_decay_enabled"
        const val EXPERIMENTAL_PERFORMANCE_OPTIMIZATIONS_ENABLED =
            "experimental_performance_optimizations_enabled"
        const val MATRIX_SMOOTH_MOTION_ENABLED = "matrix_smooth_motion_enabled"
        const val OSCILLOSCOPE_AUTO_TIME_AXIS_ENABLED =
            "oscilloscope_auto_time_axis_enabled"
        const val SHOW_PHONE1_GLYPH_DEBUG_CONTROLS_EVERYWHERE =
            "show_phone1_glyph_debug_controls_everywhere"
        const val AUTO_ENABLE_PHONE1_GLYPH_DEBUG_ON_START =
            "auto_enable_phone1_glyph_debug_on_start"
        const val NOTHING_STYLE_ENABLED = "nothing_style_enabled"
        const val EXPERIMENTAL_MAIN_UI_ENABLED = "experimental_main_ui_enabled"
        const val DETAILED_HOME_ENABLED = "detailed_home_enabled"
        const val TURN_OFF_WHEN_BACK_DOWN = "turn_off_when_back_down"
        const val DEFAULT_OUTPUT_LATENCY_PRESET_ID = "default_output_latency_preset_id"
        const val BLUETOOTH_LATENCY_PRESET_ID = "bluetooth_latency_preset_id"
        const val LATENCY_PRESET_1_MS = "latency_preset_1_ms"
        const val LATENCY_PRESET_2_MS = "latency_preset_2_ms"
        const val LATENCY_PRESET_3_MS = "latency_preset_3_ms"
    }
}

internal object PersistedSettingsJsonCodec {
    const val FORMAT = "glyph_barty_parameters"
    const val VERSION = 2

    fun export(settings: PersistedSettings): String {
        val json = JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION)
            put(
                "parameters",
                JSONObject().apply {
                    put("sensitivity", settings.sensitivity.toDouble())
                    put("noiseGate", settings.noiseGate.toDouble())
                    put("dynamics", settings.dynamics.toDouble())
                    put("outputGamma", settings.outputGamma.toDouble())
                    put("toneFocus", settings.toneFocus.toDouble())
                    put("smoothing", settings.smoothing.toDouble())
                    put("smoothingBalance", settings.smoothingBalance.toDouble())
                    put("glyphMode", settings.glyphMode)
                    put("autoScaleWindowSeconds", settings.autoScaleWindowSeconds.toDouble())
                    put("autoScaleOffset", settings.autoScaleOffset.toDouble())
                    put(
                        "visualDynamicsOverrides",
                        JSONObject(encodeVisualDynamicsOverrides(settings.visualDynamicsOverrides))
                    )
                    put(
                        "oscilloscopeAutoTimeAxisEnabled",
                        settings.oscilloscopeAutoTimeAxisEnabled
                    )
                }
            )
        }
        return json.toString(2)
    }

    fun import(jsonText: String, defaults: PersistedSettings): PersistedSettings {
        val root = JSONObject(jsonText)
        val parameters = if (root.has("parameters")) {
            root.getJSONObject("parameters")
        } else {
            root
        }
        return defaults.copy(
            sensitivity = parameters.optDouble(
                "sensitivity",
                defaults.sensitivity.toDouble()
            ).toFloat(),
            noiseGate = parameters.optDouble(
                "noiseGate",
                defaults.noiseGate.toDouble()
            ).toFloat(),
            dynamics = parameters.optDouble(
                "dynamics",
                defaults.dynamics.toDouble()
            ).toFloat(),
            outputGamma = parameters.optDouble(
                "outputGamma",
                defaults.outputGamma.toDouble()
            ).toFloat(),
            toneFocus = parameters.optDouble(
                "toneFocus",
                defaults.toneFocus.toDouble()
            ).toFloat(),
            smoothing = parameters.optDouble(
                "smoothing",
                defaults.smoothing.toDouble()
            ).toFloat(),
            smoothingBalance = parameters.optDouble(
                "smoothingBalance",
                defaults.smoothingBalance.toDouble()
            ).toFloat(),
            glyphMode = parameters.optString("glyphMode", defaults.glyphMode),
            autoScaleWindowSeconds = parameters.optDouble(
                "autoScaleWindowSeconds",
                defaults.autoScaleWindowSeconds.toDouble()
            ).toFloat(),
            autoScaleOffset = parameters.optDouble(
                "autoScaleOffset",
                defaults.autoScaleOffset.toDouble()
            ).toFloat(),
            visualDynamicsOverrides = parameters.optJSONObject("visualDynamicsOverrides")?.let {
                decodeVisualDynamicsOverrides(it.toString())
            } ?: defaults.visualDynamicsOverrides,
            oscilloscopeAutoTimeAxisEnabled = parameters.optBoolean(
                "oscilloscopeAutoTimeAxisEnabled",
                defaults.oscilloscopeAutoTimeAxisEnabled
            )
        )
    }
}

private fun encodeVisualDynamicsOverrides(
    overrides: Map<GlyphVisualTuningKey, Float>
): String = JSONObject().apply {
    overrides.forEach { (key, value) ->
        put("${key.profile.name}|${key.patternId}", value.coerceIn(0f, 1f).toDouble())
    }
}.toString()

private fun decodeVisualDynamicsOverrides(raw: String?): Map<GlyphVisualTuningKey, Float> {
    if (raw.isNullOrBlank()) return emptyMap()
    val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyMap()
    val result = mutableMapOf<GlyphVisualTuningKey, Float>()
    root.keys().forEach { encodedKey ->
        val parts = encodedKey.split('|', limit = 2)
        if (parts.size != 2) return@forEach
        val profile = GlyphDeviceProfile.entries.firstOrNull { it.name == parts[0] }
            ?: return@forEach
        val dynamics = root.optDouble(encodedKey, Double.NaN).toFloat()
        if (dynamics.isNaN()) return@forEach
        result[GlyphVisualTuningKey(profile, parts[1])] = dynamics.coerceIn(0f, 1f)
    }
    return result
}
