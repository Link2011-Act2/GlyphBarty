package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import jp.linkserver.glyphvisualizer.glyph.GlyphMatrixProfileEmulator
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

    @Test
    fun recordingLightBehavior_isAvailableOnlyOnBarProfiles() {
        assertTrue(GlyphDeviceProfile.PHONE4A.supportsRecordingLightBehavior())
        assertTrue(GlyphDeviceProfile.PHONE4B.supportsRecordingLightBehavior())

        GlyphDeviceProfile.entries
            .filterNot { it == GlyphDeviceProfile.PHONE4A || it == GlyphDeviceProfile.PHONE4B }
            .forEach { profile ->
                assertFalse(profile.supportsRecordingLightBehavior())
            }
    }

    @Test
    fun effectiveUiProfile_debugOverrideTakesPriority() {
        assertEquals(
            GlyphDeviceProfile.PHONE1,
            GlyphDeviceCatalog.effectiveUiProfile(
                actualProfile = GlyphDeviceProfile.PHONE4A,
                phone4bEmulationEnabled = true,
                debugDeviceProfileOverride = GlyphDeviceProfile.PHONE1
            )
        )
    }

    @Test
    fun effectiveUiProfile_withoutOverrideKeepsExistingEmulationBehavior() {
        assertEquals(
            GlyphDeviceProfile.PHONE4B,
            GlyphDeviceCatalog.effectiveUiProfile(
                actualProfile = GlyphDeviceProfile.PHONE4A,
                phone4bEmulationEnabled = true,
                debugDeviceProfileOverride = null
            )
        )
    }

    @Test
    fun effectiveOutputProfile_activatesSupportedProfileEmulations() {
        assertEquals(
            GlyphDeviceProfile.PHONE4B,
            GlyphDeviceCatalog.effectiveOutputProfile(
                actualProfile = GlyphDeviceProfile.PHONE4A,
                phone4bEmulationEnabled = false,
                debugDeviceProfileOverride = GlyphDeviceProfile.PHONE4B
            )
        )
        assertEquals(
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
            GlyphDeviceCatalog.effectiveOutputProfile(
                actualProfile = GlyphDeviceProfile.PHONE3_MATRIX,
                phone4bEmulationEnabled = false,
                debugDeviceProfileOverride = GlyphDeviceProfile.PHONE4A_PRO_MATRIX
            )
        )
    }

    @Test
    fun effectiveOutputProfile_ignoresUnsupportedHardwareMappings() {
        assertEquals(
            GlyphDeviceProfile.PHONE3_MATRIX,
            GlyphDeviceCatalog.effectiveOutputProfile(
                actualProfile = GlyphDeviceProfile.PHONE3_MATRIX,
                phone4bEmulationEnabled = false,
                debugDeviceProfileOverride = GlyphDeviceProfile.PHONE4B
            )
        )
    }

    @Test
    fun phone4aProFrame_isCenteredAndRoundedInsidePhone3Matrix() {
        val sourceLength = GlyphMatrixProfileEmulator.PHONE4A_PRO_MATRIX_LENGTH
        val physicalLength = 25
        val source = IntArray(sourceLength * sourceLength) { 255 }
        val destination = IntArray(physicalLength * physicalLength)

        GlyphMatrixProfileEmulator.copyPhone4aProFrameIntoCenteredRegion(
            source = source,
            physicalMatrixLength = physicalLength,
            destination = destination
        )

        assertEquals(137, destination.count { it == 255 })
        assertEquals(255, destination[12 * physicalLength + 12])
        assertEquals(0, destination[6 * physicalLength + 6])
        assertEquals(255, destination[10 * physicalLength + 6])
        assertEquals(0, destination[5 * physicalLength + 12])
        assertEquals(0, destination[19 * physicalLength + 12])
    }
}
