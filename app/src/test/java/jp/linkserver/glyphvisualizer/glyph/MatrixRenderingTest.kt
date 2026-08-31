package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixRenderingTest {
    @Test
    fun barRenderer_preservesBottomAnchoredFullAndPartialRows() {
        val frame = IntArray(25)

        MatrixBarPatternRenderer.render(
            frame = frame,
            matrixLength = 5,
            centerX = 2,
            barWidth = 1,
            fullRows = 2,
            edgeRowBrightness = 128,
            reverseDirection = false,
            colorOn = 255
        )

        assertArrayEquals(
            intArrayOf(
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 128, 0, 0,
                0, 0, 255, 0, 0,
                0, 0, 255, 0, 0
            ),
            frame
        )
    }

    @Test
    fun fieldRenderer_preservesReversedRowOrder() {
        val frame = IntArray(9)

        MatrixFieldPatternRenderer.render(
            frame = frame,
            matrixLength = 3,
            fullRows = 1,
            edgeRowBrightness = 64,
            reverseDirection = true,
            colorOn = 255
        )

        assertArrayEquals(
            intArrayOf(
                255, 255, 255,
                64, 64, 64,
                0, 0, 0
            ),
            frame
        )
    }

    @Test
    fun allBrightnessRenderer_fillsEveryPixel() {
        val frame = IntArray(6)

        MatrixAllBrightnessPatternRenderer.render(frame, 73)

        assertArrayEquals(IntArray(6) { 73 }, frame)
    }

    @Test
    fun frameOptimizer_keepsExistingDiffModeContract() {
        assertTrue(MatrixFrameOptimizer.supportsDiffRendering(GlyphPatternRenderMode.MATRIX_BAR))
        assertTrue(MatrixFrameOptimizer.supportsDiffRendering(GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM))
        assertTrue(MatrixFrameOptimizer.supportsDiffRendering(GlyphPatternRenderMode.ALL_BRIGHTNESS))
        assertFalse(MatrixFrameOptimizer.supportsDiffRendering(GlyphPatternRenderMode.MATRIX_RAIN))
        assertTrue(MatrixFrameOptimizer.shouldReuseFrame(renderSignature = 42L, lastRenderSignature = 42L))
        assertFalse(MatrixFrameOptimizer.shouldReuseFrame(renderSignature = null, lastRenderSignature = 42L))
    }
}
