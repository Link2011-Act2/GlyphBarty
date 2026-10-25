package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.*
import org.junit.Test

class MatrixFrameBrightnessTest {
    @Test fun endpointsIntermediateLevelsAndRelativeOrderCoverFullSdkRange() {
        assertEquals(0, MatrixFrameBrightness.toSdk(0))
        assertEquals(4095, MatrixFrameBrightness.toSdk(255))
        assertEquals(16, MatrixFrameBrightness.toSdk(1))
        assertEquals(1028, MatrixFrameBrightness.toSdk(64))
        assertEquals(2056, MatrixFrameBrightness.toSdk(128))
        for (level in 0..255) {
            val encoded = MatrixFrameBrightness.toSdk(level)
            assertTrue(encoded in 0..4095)
            assertTrue(kotlin.math.abs(encoded - level * 4095.0 / 255) <= 0.5)
            if (level > 0) assertTrue(encoded > MatrixFrameBrightness.toSdk(level - 1))
        }
    }

    @Test fun alreadyEncodedInputAndInPlaceEncodingAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { MatrixFrameBrightness.toSdk(4095) }
        assertThrows(IllegalArgumentException::class.java) { MatrixFrameBrightness.toSdk(-1) }
        val frame = IntArray(625)
        assertThrows(IllegalArgumentException::class.java) {
            MatrixFrameBrightness.copyToSdk(frame, frame, 25, IntArray(25) { 25 })
        }
    }

    @Test fun emulatedThirteenPixelFrameMapsFirstAndEncodesOnceWithoutLightingOutsideRegion() {
        val logical = IntArray(169) { 128 }
        val physical = IntArray(625)
        GlyphMatrixProfileEmulator.copyPhone4aProFrameIntoCenteredRegion(logical, 25, physical)
        val original = physical.copyOf()
        val sdk = IntArray(625)
        MatrixFrameBrightness.copyToSdk(physical, sdk, 25,
            GlyphMatrixPreviewGeometry.columnHeights(GlyphDeviceProfile.PHONE3_MATRIX, 25))
        assertTrue(sdk.any { it == 2056 })
        assertTrue(sdk.all { it == 0 || it == 2056 })
        assertArrayEquals(original, physical)
        assertEquals(0, sdk[0])
        assertEquals(2056, sdk[12 * 25 + 12])
    }
}
