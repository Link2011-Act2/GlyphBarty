package jp.linkserver.glyphvisualizer

import android.content.Context
import org.json.JSONObject

object SettingsPreferences {
    private const val PREFS_NAME = "glyph_visualizer_settings"
    private const val EXPORT_FORMAT = "glyph_barty_parameters"
    private const val EXPORT_VERSION = 2

    fun defaultParameters(): CaptureUiState = CaptureUiState()

    fun parameterStateOf(state: CaptureUiState): CaptureUiState {
        val defaults = defaultParameters()
        return defaults.copy(
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
            binaryMode = state.binaryMode,
            levelAutoScale = state.levelAutoScale,
            spectrumAutoScale = state.spectrumAutoScale,
            allBrightnessAutoScale = state.allBrightnessAutoScale,
            autoScaleWindowSeconds = state.autoScaleWindowSeconds,
            latencyMs = state.latencyMs,
            defaultOutputLatencyMs = state.defaultOutputLatencyMs,
            bluetoothLatencyMs = state.bluetoothLatencyMs,
            latencyAutoSwitchEnabled = state.latencyAutoSwitchEnabled,
            mediaProjectionEnabled = state.mediaProjectionEnabled,
            glyphMeterPreviewEnabled = state.glyphMeterPreviewEnabled,
            nothingStyleEnabled = state.nothingStyleEnabled,
            turnOffWhenBackDown = state.turnOffWhenBackDown
        )
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
        return defaults.copy(
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
            binaryMode = prefs.getBoolean("binary_mode", defaults.binaryMode),
            levelAutoScale = prefs.getBoolean("level_auto_scale", defaults.levelAutoScale),
            spectrumAutoScale = prefs.getBoolean("spectrum_auto_scale", defaults.spectrumAutoScale),
            allBrightnessAutoScale = prefs.getBoolean("all_brightness_auto_scale", defaults.allBrightnessAutoScale),
            autoScaleWindowSeconds = prefs.getFloatCompat("auto_scale_window_seconds", defaults.autoScaleWindowSeconds),
            latencyMs = prefs.getFloatCompat("latency_ms", defaults.latencyMs),
            defaultOutputLatencyMs = prefs.getFloatCompat("default_output_latency_ms", legacyNormalLatencyMs),
            bluetoothLatencyMs = prefs.getFloatCompat("bluetooth_latency_ms", legacyBluetoothLatencyMs),
            latencyAutoSwitchEnabled = prefs.getBoolean("latency_auto_switch_enabled", defaults.latencyAutoSwitchEnabled),
            mediaProjectionEnabled = prefs.getBoolean("media_projection_enabled", defaults.mediaProjectionEnabled),
            glyphMeterPreviewEnabled = prefs.getBoolean("glyph_meter_preview_enabled", defaults.glyphMeterPreviewEnabled),
            nothingStyleEnabled = prefs.getBoolean("nothing_style_enabled", defaults.nothingStyleEnabled),
            turnOffWhenBackDown = prefs.getBoolean("turn_off_when_back_down", defaults.turnOffWhenBackDown),
        )
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
            .putBoolean("binary_mode", parameters.binaryMode)
            .putBoolean("level_auto_scale", parameters.levelAutoScale)
            .putBoolean("spectrum_auto_scale", parameters.spectrumAutoScale)
            .putBoolean("all_brightness_auto_scale", parameters.allBrightnessAutoScale)
            .putFloat("auto_scale_window_seconds", parameters.autoScaleWindowSeconds)
            .putFloat("latency_ms", parameters.latencyMs)
            .putFloat("default_output_latency_ms", parameters.defaultOutputLatencyMs)
            .putFloat("bluetooth_latency_ms", parameters.bluetoothLatencyMs)
            .putBoolean("latency_auto_switch_enabled", parameters.latencyAutoSwitchEnabled)
            .putBoolean("media_projection_enabled", parameters.mediaProjectionEnabled)
            .putBoolean("glyph_meter_preview_enabled", parameters.glyphMeterPreviewEnabled)
            .putBoolean("nothing_style_enabled", parameters.nothingStyleEnabled)
            .putBoolean("turn_off_when_back_down", parameters.turnOffWhenBackDown)
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
                    put("reverseDirection", parameters.reverseDirection)
                    put("peakHoldEnabled", parameters.peakHoldEnabled)
                    put("glyphMode", parameters.glyphMode)
                    put("binaryMode", parameters.binaryMode)
                    put("levelAutoScale", parameters.levelAutoScale)
                    put("spectrumAutoScale", parameters.spectrumAutoScale)
                    put("allBrightnessAutoScale", parameters.allBrightnessAutoScale)
                    put("autoScaleWindowSeconds", parameters.autoScaleWindowSeconds.toDouble())
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
        return defaults.copy(
            sensitivity = parameters.optDouble("sensitivity", defaults.sensitivity.toDouble()).toFloat(),
            noiseGate = parameters.optDouble("noiseGate", defaults.noiseGate.toDouble()).toFloat(),
            dynamics = parameters.optDouble("dynamics", defaults.dynamics.toDouble()).toFloat(),
            outputGamma = parameters.optDouble("outputGamma", defaults.outputGamma.toDouble()).toFloat(),
            toneFocus = parameters.optDouble("toneFocus", defaults.toneFocus.toDouble()).toFloat(),
            smoothing = parameters.optDouble("smoothing", defaults.smoothing.toDouble()).toFloat(),
            smoothingBalance = parameters.optDouble("smoothingBalance", defaults.smoothingBalance.toDouble()).toFloat(),
            reverseDirection = parameters.optBoolean("reverseDirection", defaults.reverseDirection),
            peakHoldEnabled = parameters.optBoolean("peakHoldEnabled", defaults.peakHoldEnabled),
            glyphMode = parameters.optString("glyphMode", defaults.glyphMode),
            binaryMode = parameters.optBoolean("binaryMode", defaults.binaryMode),
            levelAutoScale = parameters.optBoolean("levelAutoScale", defaults.levelAutoScale),
            spectrumAutoScale = parameters.optBoolean("spectrumAutoScale", defaults.spectrumAutoScale),
            allBrightnessAutoScale = parameters.optBoolean("allBrightnessAutoScale", defaults.allBrightnessAutoScale),
            autoScaleWindowSeconds = parameters.optDouble("autoScaleWindowSeconds", defaults.autoScaleWindowSeconds.toDouble()).toFloat()
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
