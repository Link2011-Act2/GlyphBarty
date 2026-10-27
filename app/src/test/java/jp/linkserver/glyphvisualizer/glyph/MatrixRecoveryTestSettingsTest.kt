package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import jp.linkserver.glyphvisualizer.SettingsPreferences
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [36])
class MatrixRecoveryTestSettingsTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test fun absentOrInvalidPreferenceAlwaysDefaultsToStandard() {
        val prefs = context.getSharedPreferences("matrix_recovery_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        assertEquals(MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestSettings.load(context))
        prefs.edit().putString("mode", "unknown-future-method").commit()
        assertEquals(MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestSettings.load(context))
        prefs.edit().putInt("mode", 2).commit()
        assertEquals(MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestSettings.load(context))
    }

    @Test fun everyMethodIsPersistedInPreferencesRatherThanUiOrControllerMemory() {
        for (mode in MatrixRecoveryTestMode.entries) {
            MatrixRecoveryTestSettings.save(context, mode)
            assertEquals(mode.code, context.getSharedPreferences("matrix_recovery_test", Context.MODE_PRIVATE).getString("mode", null))
            assertEquals(mode, MatrixRecoveryTestSettings.load(context.applicationContext))
        }
    }

    @Test fun onlyTheActualIntDevPhone4aProIsEligible() {
        for (profile in GlyphDeviceProfile.entries) {
            assertEquals(profile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
                MatrixRecoveryTestSettings.isAvailable(intDevBuild = true, actualProfile = profile))
            assertFalse(MatrixRecoveryTestSettings.isAvailable(intDevBuild = false, actualProfile = profile))
        }
        assertFalse(MatrixRecoveryTestSettings.isAvailable(intDevBuild = true, actualProfile = null))
    }

    @Test fun phone4aProUiEmulationOnlyExtendsVisibilityAndNeverSdkEligibility() {
        for (actual in GlyphDeviceProfile.entries + listOf(null)) {
            for (ui in GlyphDeviceProfile.entries + listOf(null)) {
                for (intDev in listOf(false, true)) {
                    assertEquals(intDev && (actual == GlyphDeviceProfile.PHONE4A_PRO_MATRIX ||
                        ui == GlyphDeviceProfile.PHONE4A_PRO_MATRIX),
                        MatrixRecoveryTestSettings.isVisible(ui, intDev, actual))
                    assertEquals(intDev && actual == GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
                        MatrixRecoveryTestSettings.isAvailable(intDev, actual))
                }
            }
        }
    }

    @Test fun diagnosticSelectionNeverChangesCaptureSettingsOrTriggersTheirListeners() {
        val before = SettingsPreferences.load(context)
        val capturePreferences = context.getSharedPreferences("glyph_visualizer_settings", Context.MODE_PRIVATE)
        val beforeValues = capturePreferences.all.toMap()
        var changes = 0
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> changes++ }
        SettingsPreferences.registerPreferenceChangeListener(context, listener)
        try {
            MatrixRecoveryTestMode.entries.forEach { MatrixRecoveryTestSettings.save(context, it) }
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            assertEquals(0, changes)
            assertEquals(beforeValues, capturePreferences.all)
            SettingsPreferences.save(context, before.copy(sensitivity = 2f))
            assertEquals(MatrixRecoveryTestMode.CLOSE_ONLY, MatrixRecoveryTestSettings.load(context))
        } finally {
            SettingsPreferences.unregisterPreferenceChangeListener(context, listener)
        }
    }
}
