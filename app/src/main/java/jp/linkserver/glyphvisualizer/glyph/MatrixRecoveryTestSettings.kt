package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import android.content.SharedPreferences
import jp.linkserver.glyphvisualizer.GlyphDeviceCatalog
import jp.linkserver.glyphvisualizer.update.isIntDevBuild

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
    ): Boolean = intDevBuild && actualProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX

    // UI emulation exposes the selector only; SDK eligibility continues to use isAvailable().
    fun isVisible(
        uiProfile: GlyphDeviceProfile?,
        intDevBuild: Boolean = isIntDevBuild(),
        actualProfile: GlyphDeviceProfile? = GlyphDeviceCatalog.currentOrNull()?.profile,
    ): Boolean = isAvailable(intDevBuild, actualProfile) ||
        (intDevBuild && uiProfile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX)

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
