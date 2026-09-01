package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NothingOsGlyphSettingPolicyTest {
    @Test
    fun parsesKnownNothingOsSettingValues() {
        assertEquals(true, NothingOsGlyphSettingPolicy.parseSystemValue("1"))
        assertEquals(false, NothingOsGlyphSettingPolicy.parseSystemValue("0"))
        assertEquals(true, NothingOsGlyphSettingPolicy.parseSystemValue("on"))
        assertEquals(false, NothingOsGlyphSettingPolicy.parseSystemValue("false"))
    }

    @Test
    fun unknownOrMissingSystemValueIsUnavailable() {
        assertNull(NothingOsGlyphSettingPolicy.parseSystemValue(null))
        assertNull(NothingOsGlyphSettingPolicy.parseSystemValue("unknown"))
    }

    @Test
    fun disabledSyncAlwaysAllowsOutput() {
        assertTrue(
            NothingOsGlyphSettingPolicy.outputAllowed(
                syncEnabled = false,
                systemEnabled = false
            )
        )
    }

    @Test
    fun enabledSyncBlocksOnlyAnExplicitSystemOffValue() {
        assertFalse(
            NothingOsGlyphSettingPolicy.outputAllowed(
                syncEnabled = true,
                systemEnabled = false
            )
        )
        assertTrue(
            NothingOsGlyphSettingPolicy.outputAllowed(
                syncEnabled = true,
                systemEnabled = true
            )
        )
        assertTrue(
            NothingOsGlyphSettingPolicy.outputAllowed(
                syncEnabled = true,
                systemEnabled = null
            )
        )
    }
}
