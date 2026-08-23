package jp.linkserver.glyphvisualizer

import android.content.Context
import org.json.JSONObject
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.update.isIntDevBuild

object SettingsPreferences {
    private const val PREFS_NAME = "glyph_visualizer_settings"
    private const val EXPORT_FORMAT = "glyph_barty_parameters"
    private const val EXPORT_VERSION = 2
    private const val KEY_INITIAL_SETUP_COMPLETED = "initial_setup_completed"
    private const val KEY_PHONE4B_EMULATION_ENABLED = "phone4b_emulation_enabled"
    private const val KEY_DEBUG_DEVICE_PROFILE_OVERRIDE = "debug_device_profile_override"

    fun defaultParameters(): CaptureUiState = CaptureUiState()

    fun parameterStateOf(state: CaptureUiState): CaptureUiState {
        val defaults = defaultParameters()
        return normalizeDormantFlags(defaults.copy(
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
            binaryMode = state.binaryMode,
            baseIndicatorEnabled = state.baseIndicatorEnabled,
            recordingLightIncluded = state.recordingLightIncluded,
            phone4bEmulationEnabled = state.phone4bEmulationEnabled,
            debugDeviceProfileOverride = state.debugDeviceProfileOverride,
            levelAutoScale = state.levelAutoScale,
            spectrumAutoScale = state.spectrumAutoScale,
            allBrightnessAutoScale = state.allBrightnessAutoScale,
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
            experimentalVisualizerStabilizationEnabled = state.experimentalVisualizerStabilizationEnabled,
            experimentalVisualizerSignalWatchdogEnabled = state.experimentalVisualizerSignalWatchdogEnabled,
            experimentalSpectrumDecayEnabled = state.experimentalSpectrumDecayEnabled,
            experimentalPerformanceOptimizationsEnabled = state.experimentalPerformanceOptimizationsEnabled,
            matrixSmoothMotionEnabled = state.matrixSmoothMotionEnabled,
            oscilloscopeAutoTimeAxisEnabled = state.oscilloscopeAutoTimeAxisEnabled,
            showPhone1GlyphDebugControlsEverywhere = state.showPhone1GlyphDebugControlsEverywhere,
            autoEnablePhone1GlyphDebugOnStart = state.autoEnablePhone1GlyphDebugOnStart,
            nothingStyleEnabled = state.nothingStyleEnabled,
            experimentalMainUiEnabled = state.experimentalMainUiEnabled,
            turnOffWhenBackDown = state.turnOffWhenBackDown
        ))
    }

    fun load(context: Context): CaptureUiState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = defaultParameters()
        val legacyNormalLatencyMs = loadLegacyLatencyForRoute(
            prefs = prefs,
            bluetooth = false,
            defaultValue = defaults.defaultOutputLatencyMs
        )
        val legacyBluetoothLatencyMs = loadLegacyLatencyForRoute(
            prefs = prefs,
            bluetooth = true,
            defaultValue = defaults.bluetoothLatencyMs
        )
        return normalizeDormantFlags(defaults.copy(
            sensitivity = prefs.getFloatCompat("sensitivity", defaults.sensitivity),
            noiseGate = prefs.getFloatCompat("noise_gate", defaults.noiseGate),
            dynamics = prefs.getFloatCompat("dynamics", defaults.dynamics),
            outputGamma = prefs.getFloatCompat("output_gamma", defaults.outputGamma),
            toneFocus = prefs.getFloatCompat("tone_focus", defaults.toneFocus),
            smoothing = prefs.getFloatCompat("smoothing", defaults.smoothing),
            smoothingBalance = prefs.getFloatCompat("smoothing_balance", defaults.smoothingBalance),
            reverseDirection = prefs.getBoolean("reverse_direction", defaults.reverseDirection),
            peakHoldEnabled = prefs.getBoolean("peak_hold_enabled", defaults.peakHoldEnabled),
            glyphMode = prefs.getString("glyph_mode", defaults.glyphMode) ?: defaults.glyphMode,
            fillOtherGlyphLights = prefs.getBoolean("fill_other_glyph_lights", defaults.fillOtherGlyphLights),
            binaryMode = prefs.getBoolean("binary_mode", defaults.binaryMode),
            baseIndicatorEnabled = prefs.getBoolean("base_indicator_enabled", defaults.baseIndicatorEnabled),
            recordingLightIncluded = prefs.getBoolean(
                "recording_light_included",
                defaults.recordingLightIncluded
            ),
            phone4bEmulationEnabled = prefs.getBoolean(
                KEY_PHONE4B_EMULATION_ENABLED,
                defaults.phone4bEmulationEnabled
            ),
            debugDeviceProfileOverride = prefs.getString(
                KEY_DEBUG_DEVICE_PROFILE_OVERRIDE,
                null
            )?.let { savedName ->
                GlyphDeviceProfile.entries.firstOrNull { it.name == savedName }
            },
            levelAutoScale = prefs.getBoolean("level_auto_scale", defaults.levelAutoScale),
            spectrumAutoScale = prefs.getBoolean("spectrum_auto_scale", defaults.spectrumAutoScale),
            allBrightnessAutoScale = prefs.getBoolean("all_brightness_auto_scale", defaults.allBrightnessAutoScale),
            autoScaleWindowSeconds = prefs.getFloatCompat("auto_scale_window_seconds", defaults.autoScaleWindowSeconds),
            autoScaleOffset = prefs.getFloatCompat("auto_scale_offset", defaults.autoScaleOffset),
            latencyMs = prefs.getFloatCompat("latency_ms", defaults.latencyMs),
            defaultOutputLatencyMs = prefs.getFloatCompat("default_output_latency_ms", legacyNormalLatencyMs),
            bluetoothLatencyMs = prefs.getFloatCompat("bluetooth_latency_ms", legacyBluetoothLatencyMs),
            latencyAutoSwitchEnabled = prefs.getBoolean("latency_auto_switch_enabled", defaults.latencyAutoSwitchEnabled),
            mediaProjectionEnabled = prefs.getBoolean("media_projection_enabled", defaults.mediaProjectionEnabled),
            glyphMeterPreviewEnabled = prefs.getBoolean("glyph_meter_preview_enabled", defaults.glyphMeterPreviewEnabled),
            meterVisibleEnabled = prefs.getBoolean("meter_visible_enabled", defaults.meterVisibleEnabled),
            lightweightMeterEnabled = prefs.getBoolean("lightweight_meter_enabled", defaults.lightweightMeterEnabled),
            spectrumMeterEnabled = prefs.getBoolean("spectrum_meter_enabled", defaults.spectrumMeterEnabled),
            nativeMeterViewEnabled = prefs.getBoolean("native_meter_view_enabled", defaults.nativeMeterViewEnabled),
            mainScreenUiIsolationEnabled = prefs.getBoolean("main_screen_ui_isolation_enabled", defaults.mainScreenUiIsolationEnabled),
            automaticUpdateCheckEnabled = prefs.getBoolean("automatic_update_check_enabled", defaults.automaticUpdateCheckEnabled),
            mediaPlaybackOnlyEnabled = prefs.getBoolean("media_playback_only_enabled", defaults.mediaPlaybackOnlyEnabled),
            experimentalVisualizerStabilizationEnabled = prefs.getBoolean("experimental_visualizer_stabilization_enabled", defaults.experimentalVisualizerStabilizationEnabled),
            experimentalVisualizerSignalWatchdogEnabled = prefs.getBoolean("experimental_visualizer_signal_watchdog_enabled", defaults.experimentalVisualizerSignalWatchdogEnabled),
            experimentalSpectrumDecayEnabled = prefs.getBoolean("experimental_spectrum_decay_enabled", defaults.experimentalSpectrumDecayEnabled),
            experimentalPerformanceOptimizationsEnabled = prefs.getBoolean("experimental_performance_optimizations_enabled", defaults.experimentalPerformanceOptimizationsEnabled),
            matrixSmoothMotionEnabled = prefs.getBoolean("matrix_smooth_motion_enabled", defaults.matrixSmoothMotionEnabled),
            oscilloscopeAutoTimeAxisEnabled = prefs.getBoolean("oscilloscope_auto_time_axis_enabled", defaults.oscilloscopeAutoTimeAxisEnabled),
            showPhone1GlyphDebugControlsEverywhere = prefs.getBoolean("show_phone1_glyph_debug_controls_everywhere", defaults.showPhone1GlyphDebugControlsEverywhere),
            autoEnablePhone1GlyphDebugOnStart = prefs.getBoolean("auto_enable_phone1_glyph_debug_on_start", defaults.autoEnablePhone1GlyphDebugOnStart),
            nothingStyleEnabled = prefs.getBoolean("nothing_style_enabled", defaults.nothingStyleEnabled),
            experimentalMainUiEnabled = prefs.getBoolean(
                "experimental_main_ui_enabled",
                defaults.experimentalMainUiEnabled
            ),
            turnOffWhenBackDown = prefs.getBoolean("turn_off_when_back_down", defaults.turnOffWhenBackDown),
        ))
    }

    fun save(context: Context, state: CaptureUiState) {
        val parameters = parameterStateOf(state)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat("sensitivity", parameters.sensitivity)
            .putFloat("noise_gate", parameters.noiseGate)
            .putFloat("dynamics", parameters.dynamics)
            .putFloat("output_gamma", parameters.outputGamma)
            .putFloat("tone_focus", parameters.toneFocus)
            .putFloat("smoothing", parameters.smoothing)
            .putFloat("smoothing_balance", parameters.smoothingBalance)
            .putBoolean("reverse_direction", parameters.reverseDirection)
            .putBoolean("peak_hold_enabled", parameters.peakHoldEnabled)
            .putString("glyph_mode", parameters.glyphMode)
            .putBoolean("fill_other_glyph_lights", parameters.fillOtherGlyphLights)
            .putBoolean("binary_mode", parameters.binaryMode)
            .putBoolean("base_indicator_enabled", parameters.baseIndicatorEnabled)
            .putBoolean("recording_light_included", parameters.recordingLightIncluded)
            .putBoolean(KEY_PHONE4B_EMULATION_ENABLED, parameters.phone4bEmulationEnabled)
            .putString(
                KEY_DEBUG_DEVICE_PROFILE_OVERRIDE,
                parameters.debugDeviceProfileOverride?.name
            )
            .putBoolean("level_auto_scale", parameters.levelAutoScale)
            .putBoolean("spectrum_auto_scale", parameters.spectrumAutoScale)
            .putBoolean("all_brightness_auto_scale", parameters.allBrightnessAutoScale)
            .putFloat("auto_scale_window_seconds", parameters.autoScaleWindowSeconds)
            .putFloat("auto_scale_offset", parameters.autoScaleOffset)
            .putFloat("latency_ms", parameters.latencyMs)
            .putFloat("default_output_latency_ms", parameters.defaultOutputLatencyMs)
            .putFloat("bluetooth_latency_ms", parameters.bluetoothLatencyMs)
            .putBoolean("latency_auto_switch_enabled", parameters.latencyAutoSwitchEnabled)
            .putBoolean("media_projection_enabled", parameters.mediaProjectionEnabled)
            .putBoolean("glyph_meter_preview_enabled", parameters.glyphMeterPreviewEnabled)
            .putBoolean("meter_visible_enabled", parameters.meterVisibleEnabled)
            .putBoolean("lightweight_meter_enabled", parameters.lightweightMeterEnabled)
            .putBoolean("spectrum_meter_enabled", parameters.spectrumMeterEnabled)
            .putBoolean("native_meter_view_enabled", parameters.nativeMeterViewEnabled)
            .putBoolean("main_screen_ui_isolation_enabled", parameters.mainScreenUiIsolationEnabled)
            .putBoolean("automatic_update_check_enabled", parameters.automaticUpdateCheckEnabled)
            .putBoolean("media_playback_only_enabled", parameters.mediaPlaybackOnlyEnabled)
            .putBoolean("experimental_visualizer_stabilization_enabled", parameters.experimentalVisualizerStabilizationEnabled)
            .putBoolean("experimental_visualizer_signal_watchdog_enabled", parameters.experimentalVisualizerSignalWatchdogEnabled)
            .putBoolean("experimental_spectrum_decay_enabled", parameters.experimentalSpectrumDecayEnabled)
            .putBoolean("experimental_performance_optimizations_enabled", parameters.experimentalPerformanceOptimizationsEnabled)
            .putBoolean("matrix_smooth_motion_enabled", parameters.matrixSmoothMotionEnabled)
            .putBoolean("oscilloscope_auto_time_axis_enabled", parameters.oscilloscopeAutoTimeAxisEnabled)
            .putBoolean("show_phone1_glyph_debug_controls_everywhere", parameters.showPhone1GlyphDebugControlsEverywhere)
            .putBoolean("auto_enable_phone1_glyph_debug_on_start", parameters.autoEnablePhone1GlyphDebugOnStart)
            .putBoolean("nothing_style_enabled", parameters.nothingStyleEnabled)
            .putBoolean("experimental_main_ui_enabled", parameters.experimentalMainUiEnabled)
            .putBoolean("turn_off_when_back_down", parameters.turnOffWhenBackDown)
            .apply()
    }

    fun loadPhone4bEmulationEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(
            KEY_PHONE4B_EMULATION_ENABLED,
            defaultParameters().phone4bEmulationEnabled
        )
    }

    fun hasCompletedInitialSetup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_INITIAL_SETUP_COMPLETED)) {
            return prefs.getBoolean(KEY_INITIAL_SETUP_COMPLETED, false)
        }
        return prefs.all.isNotEmpty()
    }

    fun markInitialSetupCompleted(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_INITIAL_SETUP_COMPLETED, true)
            .apply()
    }

    fun exportJson(state: CaptureUiState): String {
        val parameters = parameterStateOf(state)
        val json = JSONObject().apply {
            put("format", EXPORT_FORMAT)
            put("version", EXPORT_VERSION)
            put(
                "parameters",
                JSONObject().apply {
                    put("sensitivity", parameters.sensitivity.toDouble())
                    put("noiseGate", parameters.noiseGate.toDouble())
                    put("dynamics", parameters.dynamics.toDouble())
                    put("outputGamma", parameters.outputGamma.toDouble())
                    put("toneFocus", parameters.toneFocus.toDouble())
                    put("smoothing", parameters.smoothing.toDouble())
                    put("smoothingBalance", parameters.smoothingBalance.toDouble())
                    put("glyphMode", parameters.glyphMode)
                    put("autoScaleWindowSeconds", parameters.autoScaleWindowSeconds.toDouble())
                    put("autoScaleOffset", parameters.autoScaleOffset.toDouble())
                    put("oscilloscopeAutoTimeAxisEnabled", parameters.oscilloscopeAutoTimeAxisEnabled)
                }
            )
        }
        return json.toString(2)
    }

    fun importJson(jsonText: String): CaptureUiState {
        val root = JSONObject(jsonText)
        val parameters = if (root.has("parameters")) {
            root.getJSONObject("parameters")
        } else {
            root
        }
        val defaults = defaultParameters()
        return normalizeDormantFlags(defaults.copy(
            sensitivity = parameters.optDouble("sensitivity", defaults.sensitivity.toDouble()).toFloat(),
            noiseGate = parameters.optDouble("noiseGate", defaults.noiseGate.toDouble()).toFloat(),
            dynamics = parameters.optDouble("dynamics", defaults.dynamics.toDouble()).toFloat(),
            outputGamma = parameters.optDouble("outputGamma", defaults.outputGamma.toDouble()).toFloat(),
            toneFocus = parameters.optDouble("toneFocus", defaults.toneFocus.toDouble()).toFloat(),
            smoothing = parameters.optDouble("smoothing", defaults.smoothing.toDouble()).toFloat(),
            smoothingBalance = parameters.optDouble("smoothingBalance", defaults.smoothingBalance.toDouble()).toFloat(),
            glyphMode = parameters.optString("glyphMode", defaults.glyphMode),
            autoScaleWindowSeconds = parameters.optDouble("autoScaleWindowSeconds", defaults.autoScaleWindowSeconds.toDouble()).toFloat(),
            autoScaleOffset = parameters.optDouble("autoScaleOffset", defaults.autoScaleOffset.toDouble()).toFloat(),
            oscilloscopeAutoTimeAxisEnabled = parameters.optBoolean(
                "oscilloscopeAutoTimeAxisEnabled",
                defaults.oscilloscopeAutoTimeAxisEnabled
            )
        ))
    }

    private fun normalizeDormantFlags(state: CaptureUiState): CaptureUiState {
        val normalizedRecordingLight = state.withRecordingLightBehavior(
            resolveRecordingLightBehavior(
                baseIndicatorEnabled = state.baseIndicatorEnabled,
                recordingLightIncluded = state.recordingLightIncluded
            )
        )
        // Hidden for now because some devices already enforce a similar OS-level behavior.
        return normalizedRecordingLight.copy(
            turnOffWhenBackDown = false,
            mainScreenUiIsolationEnabled = true,
            debugDeviceProfileOverride = if (isIntDevBuild()) {
                state.debugDeviceProfileOverride
            } else {
                null
            },
            showPhone1GlyphDebugControlsEverywhere = if (isIntDevBuild()) {
                state.showPhone1GlyphDebugControlsEverywhere
            } else {
                false
            }
        )
    }

    private fun loadLegacyLatencyForRoute(
        prefs: android.content.SharedPreferences,
        bluetooth: Boolean,
        defaultValue: Float
    ): Float {
        val presetIdKey = if (bluetooth) {
            "bluetooth_latency_preset_id"
        } else {
            "default_output_latency_preset_id"
        }
        val presetStorageKey = when (prefs.getString(presetIdKey, null)) {
            "latency_preset_1" -> "latency_preset_1_ms"
            "latency_preset_2" -> "latency_preset_2_ms"
            "latency_preset_3" -> "latency_preset_3_ms"
            else -> null
        }
        return if (presetStorageKey == null) {
            defaultValue
        } else {
            prefs.getFloatCompat(presetStorageKey, defaultValue)
        }
    }

    private fun android.content.SharedPreferences.getFloatCompat(key: String, defaultValue: Float): Float {
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
}
