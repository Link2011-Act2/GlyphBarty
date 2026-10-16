package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phone1GlyphDebugHelperTest {
    @Test
    fun autoEnableOnStart_isEnabledOnlyForPhone1WithSettingEnabled() {
        assertTrue(
            Phone1GlyphDebugHelper.shouldAutoEnableOnStart(
                profile = GlyphDeviceProfile.PHONE1,
                autoEnableOnStart = true,
                debugAssistEnabled = true
            )
        )
        assertFalse(
            Phone1GlyphDebugHelper.shouldAutoEnableOnStart(
                profile = GlyphDeviceProfile.PHONE1,
                autoEnableOnStart = false,
                debugAssistEnabled = true
            )
        )

        GlyphDeviceProfile.entries
            .filterNot { it == GlyphDeviceProfile.PHONE1 }
            .forEach { profile ->
                assertFalse(
                    Phone1GlyphDebugHelper.shouldAutoEnableOnStart(
                        profile = profile,
                        autoEnableOnStart = true,
                        debugAssistEnabled = true
                    )
                )
        }
    }

    @Test
    fun disabledDebugAssist_keepsPhone1SupportedButSkipsSavedAutoEnableSetting() {
        assertTrue(Phone1GlyphDebugHelper.supports(GlyphDeviceProfile.PHONE1))
        assertFalse(Phone1GlyphDebugHelper.isAvailable(GlyphDeviceProfile.PHONE1))
        assertFalse(
            Phone1GlyphDebugHelper.shouldAutoEnableOnStart(
                profile = GlyphDeviceProfile.PHONE1,
                autoEnableOnStart = true
            )
        )
        assertFalse(Phone1GlyphDebugHelper.requestPermission(1402))
    }

    @Test
    fun autoEnableBackendAction_usesSharedShizukuApiStateForShizukuAndSui() {
        assertEquals(
            Phone1GlyphDebugHelper.AutoEnableBackendAction.ENABLE,
            Phone1GlyphDebugHelper.resolveAutoEnableBackendAction(
                Phone1GlyphDebugHelper.BackendStatus(
                    suiAvailable = true,
                    apiAvailable = true,
                    permissionGranted = true
                )
            )
        )
        assertEquals(
            Phone1GlyphDebugHelper.AutoEnableBackendAction.REQUEST_PERMISSION,
            Phone1GlyphDebugHelper.resolveAutoEnableBackendAction(
                Phone1GlyphDebugHelper.BackendStatus(
                    suiAvailable = false,
                    apiAvailable = true,
                    permissionGranted = false
                )
            )
        )
        assertEquals(
            Phone1GlyphDebugHelper.AutoEnableBackendAction.SKIP,
            Phone1GlyphDebugHelper.resolveAutoEnableBackendAction(
                Phone1GlyphDebugHelper.BackendStatus(
                    suiAvailable = true,
                    apiAvailable = false,
                    permissionGranted = false
                )
            )
        )
    }
}
