package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphPatternRegistry
import org.junit.Test

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun spectrumMarker_isLimitedToGlyphBarProfiles() {
        val mode = GlyphPatternRegistry.P4A_SPECTRUM_MARKER

        assertTrue(GlyphPatternRegistry.isSupported(GlyphDeviceProfile.PHONE4A, mode))
        assertTrue(GlyphPatternRegistry.isSupported(GlyphDeviceProfile.PHONE4B, mode))
        assertFalse(GlyphPatternRegistry.isSupported(GlyphDeviceProfile.PHONE3A, mode))
        assertFalse(GlyphPatternRegistry.isSupported(GlyphDeviceProfile.PHONE3_MATRIX, mode))
    }

    @Test
    fun spectrumMarker_usesBarSegmentCounts() {
        val mode = GlyphPatternRegistry.P4A_SPECTRUM_MARKER

        assertEquals(6, GlyphPatternRegistry.uiMeterSegmentCount(GlyphDeviceProfile.PHONE4A, mode))
        assertEquals(7, GlyphPatternRegistry.uiMeterSegmentCount(GlyphDeviceProfile.PHONE4A, mode, true))
        assertEquals(4, GlyphPatternRegistry.uiMeterSegmentCount(GlyphDeviceProfile.PHONE4B, mode))
        assertEquals(5, GlyphPatternRegistry.uiMeterSegmentCount(GlyphDeviceProfile.PHONE4B, mode, true))
    }

    @Test
    fun recordingLightBehavior_resolvesLegacyEnabledFlagsToBassIndicator() {
        assertEquals(
            RecordingLightBehavior.BASS_INDICATOR,
            resolveRecordingLightBehavior(
                baseIndicatorEnabled = true,
                recordingLightIncluded = true
            )
        )
    }

    @Test
    fun recordingLightBehavior_updatesFlagsExclusively() {
        val meter = RecordingLightBehavior.INCLUDED_IN_METER
        assertFalse(meter.baseIndicatorEnabled)
        assertTrue(meter.recordingLightIncluded)

        val bass = RecordingLightBehavior.BASS_INDICATOR
        assertTrue(bass.baseIndicatorEnabled)
        assertFalse(bass.recordingLightIncluded)

        val none = RecordingLightBehavior.NONE
        assertFalse(none.baseIndicatorEnabled)
        assertFalse(none.recordingLightIncluded)
    }
}
