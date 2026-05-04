package jp.linkserver.glyphvisualizer

import android.content.Context

object SettingsPreferences {
    private const val PREFS_NAME = "glyph_visualizer_settings"

    fun load(context: Context): CaptureUiState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val defaults = CaptureUiState()
        return defaults.copy(
            sensitivity = prefs.getFloat("sensitivity", defaults.sensitivity),
            noiseGate = prefs.getFloat("noise_gate", defaults.noiseGate),
            dynamics = prefs.getFloat("dynamics", defaults.dynamics),
            toneFocus = prefs.getFloat("tone_focus", defaults.toneFocus),
            smoothing = prefs.getFloat("smoothing", defaults.smoothing),
            smoothingBalance = prefs.getFloat("smoothing_balance", defaults.smoothingBalance),
            reverseDirection = prefs.getBoolean("reverse_direction", defaults.reverseDirection),
            peakHoldEnabled = prefs.getBoolean("peak_hold_enabled", defaults.peakHoldEnabled),
            glyphMode = prefs.getString("glyph_mode", defaults.glyphMode) ?: defaults.glyphMode,
            binaryMode = prefs.getBoolean("binary_mode", defaults.binaryMode),
            spectrumAutoScale = prefs.getBoolean("spectrum_auto_scale", defaults.spectrumAutoScale),
            allBrightnessAutoScale = prefs.getBoolean("all_brightness_auto_scale", defaults.allBrightnessAutoScale),
            turnOffWhenBackDown = prefs.getBoolean("turn_off_when_back_down", defaults.turnOffWhenBackDown),
        )
    }

    fun save(context: Context, state: CaptureUiState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat("sensitivity", state.sensitivity)
            .putFloat("noise_gate", state.noiseGate)
            .putFloat("dynamics", state.dynamics)
            .putFloat("tone_focus", state.toneFocus)
            .putFloat("smoothing", state.smoothing)
            .putFloat("smoothing_balance", state.smoothingBalance)
            .putBoolean("reverse_direction", state.reverseDirection)
            .putBoolean("peak_hold_enabled", state.peakHoldEnabled)
            .putString("glyph_mode", state.glyphMode)
            .putBoolean("binary_mode", state.binaryMode)
            .putBoolean("spectrum_auto_scale", state.spectrumAutoScale)
            .putBoolean("all_brightness_auto_scale", state.allBrightnessAutoScale)
            .putBoolean("turn_off_when_back_down", state.turnOffWhenBackDown)
            .apply()
    }
}
