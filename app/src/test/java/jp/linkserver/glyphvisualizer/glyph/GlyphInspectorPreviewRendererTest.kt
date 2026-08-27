package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphInspectorPreviewRendererTest {
    @Test
    fun zeroLevel_producesSilentTestSignals() {
        assertTrue(GlyphInspectorPreviewRenderer.spectrum(1_000L, 0f).all { it == 0f })
        assertTrue(GlyphInspectorPreviewRenderer.waveform(1_000L, 0f, 0f).all { it == 0f })
    }

    @Test
    fun activeTestSignals_stayInExpectedRanges() {
        val spectrum = GlyphInspectorPreviewRenderer.spectrum(1_000L, 0.5f)
        val waveform = GlyphInspectorPreviewRenderer.waveform(1_000L, 0.5f, 0f)

        assertEquals(32, spectrum.size)
        assertEquals(256, waveform.size)
        assertTrue(spectrum.all { it in 0f..1f })
        assertTrue(waveform.all { it in -1f..1f })
        assertTrue(spectrum.any { it > 0f })
        assertTrue(waveform.any { it != 0f })
    }

    @Test
    fun matrixGeometry_matchesKnownPhysicalShapes() {
        val phone3 = GlyphMatrixPreviewGeometry.columnHeights(
            GlyphDeviceProfile.PHONE3_MATRIX,
            25
        )
        val phone4aPro = GlyphMatrixPreviewGeometry.columnHeights(
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX,
            13
        )

        assertEquals(25, phone3.size)
        assertEquals(7, phone3.first())
        assertEquals(25, phone3[12])
        assertEquals(13, phone4aPro.size)
        assertEquals(5, phone4aPro.first())
        assertEquals(13, phone4aPro[6])
    }
}
