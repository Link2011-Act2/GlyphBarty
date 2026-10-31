package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import android.content.SharedPreferences
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.update.isIntDevBuild

// Rebuild with true to restore the IntDev A-D selector, including UI emulation.
internal const val ENABLE_MATRIX_RECOVERY_DIAGNOSTICS = false

internal enum class MatrixRecoveryTestMode(val code: String) {
    STANDARD("A"),
    KEEP_SESSION("B"),
    TURN_OFF_ONLY("C"),
    CLOSE_ONLY("D");
}

/** Temporary diagnosis; deliberately separate from capture settings and their restart path. */
internal object MatrixRecoveryTestSettings {
    private const val PREFS_NAME = "matrix_recovery_test"
    private const val KEY_MODE = "mode"

    fun isAvailable(
        intDevBuild: Boolean = isIntDevBuild(),
        actualProfile: GlyphDeviceProfile? = GlyphDeviceCatalog.currentOrNull()?.profile,
        diagnosticsEnabled: Boolean = ENABLE_MATRIX_RECOVERY_DIAGNOSTICS,
    ): Boolean = diagnosticsEnabled && intDevBuild && actualProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX

    // UI emulation exposes the selector only; SDK eligibility continues to use isAvailable().
    fun isVisible(
        uiProfile: GlyphDeviceProfile?,
        intDevBuild: Boolean = isIntDevBuild(),
        actualProfile: GlyphDeviceProfile? = GlyphDeviceCatalog.currentOrNull()?.profile,
        diagnosticsEnabled: Boolean = ENABLE_MATRIX_RECOVERY_DIAGNOSTICS,
    ): Boolean = isAvailable(intDevBuild, actualProfile, diagnosticsEnabled) ||
        (diagnosticsEnabled && intDevBuild && uiProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX)

    fun effectiveMode(
        selected: MatrixRecoveryTestMode,
        actualProfile: GlyphDeviceProfile?,
        normalMatrix: Boolean,
        diagnosticsEnabled: Boolean = ENABLE_MATRIX_RECOVERY_DIAGNOSTICS,
        intDevBuild: Boolean = isIntDevBuild(),
    ): MatrixRecoveryTestMode = when {
        !normalMatrix || actualProfile != GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> MatrixRecoveryTestMode.STANDARD
        isAvailable(intDevBuild, actualProfile, diagnosticsEnabled) -> selected
        else -> MatrixRecoveryTestMode.CLOSE_ONLY
    }

    fun load(context: Context): MatrixRecoveryTestMode {
        val code = runCatching { preferences(context).getString(KEY_MODE, "A") }.getOrNull()
        return MatrixRecoveryTestMode.entries.firstOrNull { it.code == code } ?: MatrixRecoveryTestMode.STANDARD
    }

    fun save(context: Context, mode: MatrixRecoveryTestMode) {
        preferences(context).edit().putString(KEY_MODE, mode.code).apply()
    }

    fun register(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
