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
                MatrixRecoveryTestSettings.isAvailable(intDevBuild = true, actualProfile = profile, diagnosticsEnabled = true))
            assertFalse(MatrixRecoveryTestSettings.isAvailable(intDevBuild = false, actualProfile = profile, diagnosticsEnabled = true))
        }
        assertFalse(MatrixRecoveryTestSettings.isAvailable(intDevBuild = true, actualProfile = null, diagnosticsEnabled = true))
    }

    @Test fun phone4aProUiEmulationOnlyExtendsVisibilityAndNeverSdkEligibility() {
        for (actual in GlyphDeviceProfile.entries + listOf(null)) {
            for (ui in GlyphDeviceProfile.entries + listOf(null)) {
                for (intDev in listOf(false, true)) {
                    assertEquals(intDev && (actual == GlyphDeviceProfile.PHONE4A_PRO_MATRIX ||
                        ui == GlyphDeviceProfile.PHONE4A_PRO_MATRIX),
                        MatrixRecoveryTestSettings.isVisible(ui, intDev, actual, diagnosticsEnabled = true))
                    assertEquals(intDev && actual == GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
                        MatrixRecoveryTestSettings.isAvailable(intDev, actual, diagnosticsEnabled = true))
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

    @Test fun disabledDiagnosticsHideTheSelectorEvenOnAnEmulatedPhone4aProUi() {
        for (actual in GlyphDeviceProfile.entries + listOf(null)) {
            for (ui in GlyphDeviceProfile.entries + listOf(null)) {
                for (intDev in listOf(false, true)) {
                    assertFalse(MatrixRecoveryTestSettings.isVisible(ui, intDev, actual, diagnosticsEnabled = false))
                    assertFalse(MatrixRecoveryTestSettings.isAvailable(intDev, actual, diagnosticsEnabled = false))
                    assertEquals(ENABLE_MATRIX_RECOVERY_DIAGNOSTICS && intDev &&
                        (actual == GlyphDeviceProfile.PHONE4A_PRO_MATRIX || ui == GlyphDeviceProfile.PHONE4A_PRO_MATRIX),
                        MatrixRecoveryTestSettings.isVisible(ui, intDev, actual))
                }
            }
        }
    }

    @Test fun disabledDiagnosticsIgnoreEverySavedMethodOnBothBuildChannels() {
        for (selected in MatrixRecoveryTestMode.entries) {
            MatrixRecoveryTestSettings.save(context, selected)
            for (intDev in listOf(false, true)) {
                for (profile in GlyphDeviceProfile.entries + listOf(null)) {
                    for (normal in listOf(false, true)) {
                        assertEquals(if (normal && profile == GlyphDeviceProfile.PHONE4A_PRO_MATRIX)
                            MatrixRecoveryTestMode.CLOSE_ONLY else MatrixRecoveryTestMode.STANDARD,
                            MatrixRecoveryTestSettings.effectiveMode(MatrixRecoveryTestSettings.load(context),
                                profile, normal, diagnosticsEnabled = false, intDevBuild = intDev))
                    }
                }
            }
        }
    }

    @Test fun enabledDiagnosticsRestoreAllMethodsWithoutChangingOtherHardware() {
        for (selected in MatrixRecoveryTestMode.entries) {
            for (profile in GlyphDeviceProfile.entries + listOf(null)) {
                for (intDev in listOf(false, true)) {
                    val expected = when {
                        profile != GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> MatrixRecoveryTestMode.STANDARD
                        intDev -> selected
                        else -> MatrixRecoveryTestMode.CLOSE_ONLY
                    }
                    assertEquals(expected, MatrixRecoveryTestSettings.effectiveMode(selected, profile,
                        normalMatrix = true, diagnosticsEnabled = true, intDevBuild = intDev))
                    assertEquals(MatrixRecoveryTestMode.STANDARD, MatrixRecoveryTestSettings.effectiveMode(
                        selected, profile, normalMatrix = false, diagnosticsEnabled = true, intDevBuild = intDev))
                }
            }
        }
    }
}
