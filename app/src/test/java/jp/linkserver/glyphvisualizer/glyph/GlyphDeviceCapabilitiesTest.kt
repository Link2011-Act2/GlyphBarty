package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphDeviceCapabilitiesTest {
    @Test
    fun fillOtherGlyphLightsSupportsPhone3aSeries() {
        assertTrue(GlyphDeviceProfile.PHONE3A.supportsFillOtherGlyphLights())
    }

    @Test
    fun fillOtherGlyphLightsRemainsUnavailableForSingleBarAndMatrixProfiles() {
        assertFalse(GlyphDeviceProfile.PHONE4A.supportsFillOtherGlyphLights())
        assertFalse(GlyphDeviceProfile.PHONE4B.supportsFillOtherGlyphLights())
        assertFalse(GlyphDeviceProfile.PHONE3_MATRIX.supportsFillOtherGlyphLights())
        assertFalse(GlyphDeviceProfile.PHONE4A_PRO_MATRIX.supportsFillOtherGlyphLights())
    }

    @Test
    fun fillOtherGlyphLightsResolveClassicDynamicsPatternForEachSupportedProfile() {
        assertEquals(
            GlyphPatternRegistry.P2_CLASSIC,
            GlyphPatternRegistry.classicPatternIdFor(GlyphDeviceProfile.PHONE1)
        )
        assertEquals(
            GlyphPatternRegistry.P2_CLASSIC,
            GlyphPatternRegistry.classicPatternIdFor(GlyphDeviceProfile.PHONE2)
        )
        assertEquals(
            GlyphPatternRegistry.P2A_CLASSIC,
            GlyphPatternRegistry.classicPatternIdFor(GlyphDeviceProfile.PHONE2A)
        )
        assertEquals(
            GlyphPatternRegistry.P3A_CLASSIC,
            GlyphPatternRegistry.classicPatternIdFor(GlyphDeviceProfile.PHONE3A)
        )
        assertNull(GlyphPatternRegistry.classicPatternIdFor(GlyphDeviceProfile.PHONE4A))
    }
}
