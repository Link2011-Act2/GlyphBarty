package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.*
import org.junit.Test

class MatrixTrackTitleAnimationTest {
    @Test fun nativeTextCoverageSurvivesPreviewSdkConversionAndPhysicalMask() {
        val levels = listOf(0, 1, 32, 64, 128, 192, 254, 255)
        val text = IntArray(625)
        for ((x, level) in levels.withIndex()) text[12 * 25 + x] = level
        text[0] = 128 // No physical LED at this corner.
        val background = IntArray(625)
        val preview = IntArray(625)
        val sdk = IntArray(625)
        val heights = GlyphMatrixPreviewGeometry.columnHeights(GlyphDeviceProfile.PHONE3_MATRIX, 25)
        MatrixTrackTitleCompositor.compose(background, text, 0.2f, 25, heights, preview, true)
        MatrixFrameBrightness.copyToSdk(preview, sdk, 25, heights)
        for ((x, level) in levels.withIndex()) {
            assertEquals(level, preview[12 * 25 + x])
            assertEquals((level * 4095 + 127) / 255, sdk[12 * 25 + x])
        }
        assertEquals(0, preview[0])
        assertEquals(0, sdk[0])
        background.fill(200)
        MatrixTrackTitleCompositor.compose(background, text, 0.2f, 25, heights, preview, true)
        MatrixFrameBrightness.copyToSdk(preview, sdk, 25, heights)
        for ((x, level) in levels.withIndex()) {
            assertEquals(maxOf(40, level), preview[12 * 25 + x])
            assertEquals((maxOf(40, level) * 4095 + 127) / 255, sdk[12 * 25 + x])
        }
        assertEquals(642, sdk[12 * 25 + 12]) // The 20% background shares the text's scale.
        assertTrue(background.all { it == 200 })
    }

    @Test fun phoneThreeUsesFortyPixelsPerSecondWithDimAndRestoreUnchanged() {
        val animation = MatrixTrackTitleAnimation()
        animation.start(1000, 25, 112, 40) // 100px text plus 12px trailing space.
        animation.advance(1100)
        assertEquals(0.6f, animation.backgroundBrightness, 0.001f)
        animation.advance(1200)
        assertEquals(25, animation.textX)
        animation.advance(1224)
        assertEquals(25, animation.textX)
        animation.advance(1225)
        assertEquals(24, animation.textX)
        animation.advance(2200)
        assertEquals(-15, animation.textX)
        assertEquals(3425L, animation.scrollDurationMs)
        animation.advance(4624)
        assertEquals(-111, animation.textX)
        animation.advance(4625)
        assertNull(animation.textX)
        assertEquals(0.2f, animation.backgroundBrightness, 0.001f)
        animation.advance(4775)
        assertEquals(0.6f, animation.backgroundBrightness, 0.001f)
        animation.advance(4925)
        assertFalse(animation.active)
        assertEquals(1f, animation.backgroundBrightness, 0.001f)
        animation.start(10_000, 25, Int.MAX_VALUE, 40)
        assertEquals(20_000L, animation.scrollDurationMs)
    }

    @Test fun dimScrollRestoreFollowElapsedTimeOnBothSizes() {
        for (length in listOf(13, 25)) {
            val animation = MatrixTrackTitleAnimation()
            animation.start(1000, length, 60)
            assertEquals(1f, animation.backgroundBrightness, 0.001f)
            assertNull(animation.textX)
            animation.advance(1100)
            assertEquals(0.6f, animation.backgroundBrightness, 0.001f)
            animation.advance(1200)
            assertEquals(0.2f, animation.backgroundBrightness, 0.001f)
            assertEquals(length, animation.textX)
            animation.advance(2200)
            assertEquals(length - 12, animation.textX)
            val end = 1200 + animation.scrollDurationMs
            animation.advance(end - 1)
            assertEquals(-59, animation.textX)
            animation.advance(end)
            assertNull(animation.textX)
            assertEquals(0.2f, animation.backgroundBrightness, 0.001f)
            animation.advance(end + 150)
            assertEquals(0.6f, animation.backgroundBrightness, 0.001f)
            animation.advance(end + 300)
            assertFalse(animation.active)
            assertEquals(1f, animation.backgroundBrightness, 0.001f)
        }
    }

    @Test fun frameDropAndLongTitleAreBoundedAndShortTitleTravelsAcrossScreen() {
        val animation = MatrixTrackTitleAnimation()
        animation.start(0, 25, Int.MAX_VALUE)
        assertEquals(20_000L, animation.scrollDurationMs)
        animation.advance(21_000)
        assertFalse(animation.active)
        animation.start(22_000, 25, 1)
        assertEquals(2167L, animation.scrollDurationMs)
        animation.advance(22_000 + 200 + animation.scrollDurationMs)
        assertNull(animation.textX)
    }

    @Test fun replacementStartsNewTravelWithoutBrightnessJumpAndCancelRestores() {
        val animation = MatrixTrackTitleAnimation()
        animation.start(0, 13, 60)
        animation.advance(1000)
        animation.start(1000, 13, 10)
        assertNull(animation.textX)
        assertEquals(0.2f, animation.backgroundBrightness, 0.001f)
        animation.advance(1200)
        assertEquals(13, animation.textX)
        animation.cancel()
        assertFalse(animation.active)
        assertEquals(1f, animation.backgroundBrightness, 0.001f)
        assertNull(animation.textX)
    }

    @Test fun compositorDimsMovingBackgroundPrioritizesTextAndMasksPhysicalShape() {
        for ((profile, length) in listOf(GlyphDeviceProfile.PHONE3_MATRIX to 25, GlyphDeviceProfile.PHONE4A_PRO_MATRIX to 13)) {
            val background = IntArray(length * length) { 200 }
            val original = background.copyOf()
            val text = IntArray(background.size)
            val center = length / 2 * length + length / 2
            text[center] = 1
            text[0] = 255
            val output = IntArray(background.size)
            val heights = GlyphMatrixPreviewGeometry.columnHeights(profile, length)
            MatrixTrackTitleCompositor.compose(background, text, 0.2f, length, heights, output)
            assertEquals(255, output[center])
            assertEquals(40, output[center + 1])
            assertEquals(0, output[0])
            assertArrayEquals(original, background) // No cumulative dimming or damage to diff rendering.
            val sdkFrame = IntArray(output.size)
            MatrixFrameBrightness.copyToSdk(output, sdkFrame, length, heights)
            assertEquals(4095, sdkFrame[center])
            assertEquals(642, sdkFrame[center + 1])
            assertEquals(255, output[center]) // Preview retains its original scale.
            background[center + 1] = 100
            MatrixTrackTitleCompositor.compose(background, text, 0.2f, length, heights, output)
            assertEquals(20, output[center + 1])
        }
    }
}
