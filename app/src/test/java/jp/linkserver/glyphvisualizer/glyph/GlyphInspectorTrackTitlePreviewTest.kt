package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlyphInspectorTrackTitlePreviewTest {
    private val profiles = listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)
    private fun length(profile: GlyphDeviceProfile) = if (profile == profiles[0]) 25 else 13
    private fun frame(profile: GlyphDeviceProfile, level: Int = 200) = GlyphPreviewFrame.Matrix(
        profile, profile, GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS, 0, length(profile),
        IntArray(length(profile) * length(profile)) { level })
    private fun preview(profile: GlyphDeviceProfile) = GlyphInspectorTrackTitlePreview(RuntimeEnvironment.getApplication(), profile)
    private fun animation(preview: GlyphInspectorTrackTitlePreview) = GlyphInspectorTrackTitlePreview::class.java
        .getDeclaredField("animation").apply { isAccessible = true }.get(preview) as MatrixTrackTitleAnimation
    private fun renderer(preview: GlyphInspectorTrackTitlePreview) = GlyphInspectorTrackTitlePreview::class.java
        .getDeclaredField("renderer").apply { isAccessible = true }.get(preview) as MatrixCanvasTrackTitleRenderer

    @Test fun manualUsesCurrentCanvasProfilesFadesFourLevelsAndPhysicalMasks() {
        for (profile in profiles) preview(profile).use { preview ->
            val background = frame(profile)
            preview.updateManual(true, 1, "日本語の長い曲名 Glyph Barty", "歌手 Artist", 0)
            assertEquals(if (length(profile) == 25) 40 else 21, renderer(preview).pixelsPerSecond)
            val halfDim = preview.compose(background, 100)
            assertTrue(halfDim.pixels.any { it == 107 }) // 200 * 53.5%.
            val scrolling = preview.compose(background, 700)
            assertTrue(scrolling.pixels.all { it in listOf(0, 14, 42, 85, 127) })
            assertTrue(scrolling.pixels.any { it == 127 })
            assertTrue(scrolling.pixels.any { it == 42 || it == 85 })
            val heights = GlyphMatrixPreviewGeometry.columnHeights(profile, length(profile))
            assertEquals(if (length(profile) == 25) 489 else 137, scrolling.pixels.count { it > 0 })
            for (x in heights.indices) for (y in heights.indices) {
                val top = (length(profile) - heights[x]) / 2
                if (y !in top until top + heights[x]) assertEquals(0, scrolling.pixels[y * length(profile) + x])
            }
            val end = 200 + animation(preview).scrollDurationMs
            preview.compose(background, end)
            assertEquals(0.07f, animation(preview).backgroundBrightness, 0f)
            preview.compose(background, end + 150)
            assertEquals(0.535f, animation(preview).backgroundBrightness, 0.001f)
            assertSame(background, preview.compose(background, end + 300))
            assertTrue(background.pixels.all { it == 200 })
            assertNull(renderer(preview).cachedBitmap)
        }
    }

    @Test fun repeatedManualCommandsDoNotRestartButReplayAndReplacementDo() {
        preview(profiles[0]).use { preview ->
            val background = frame(profiles[0])
            preview.updateManual(true, 1, "A long title 日本語", null, 0)
            preview.compose(background, 800)
            val x = animation(preview).textX
            val bitmap = requireNotNull(renderer(preview).cachedBitmap)
            preview.updateManual(true, 1, "Edited without replay", null, 800)
            preview.compose(background, 800)
            assertEquals(x, animation(preview).textX)
            preview.updateManual(true, 2, "A long title 日本語", null, 800)
            assertNull(animation(preview).textX)
            assertSame(bitmap, renderer(preview).cachedBitmap)
            preview.compose(background, 1000)
            assertEquals(24, animation(preview).textX)
            preview.updateManual(true, 3, "New song", "New artist", 1000)
            assertTrue(bitmap.isRecycled)
            assertEquals("New song - New artist", renderer(preview).preparedText)
            preview.updateManual(false, 3, "", null, 1100)
            assertSame(background, preview.compose(background, 1100))
            preview.updateManual(true, 3, "New song", null, 1200)
            assertFalse(animation(preview).active)
        }
    }

    @Test fun frozenFramesAreImmutableAndResumeCatchesUpWithoutReplaying() {
        preview(profiles[0]).use { preview ->
            val background = frame(profiles[0])
            preview.updateManual(true, 1, "ABCDEFGHIJKLMNOPQRSTUVWXYZ 日本語", null, 0)
            val frozen = preview.compose(background, 800)
            val original = frozen.pixels.copyOf()
            val current = preview.compose(background, 1800)
            assertArrayEquals(original, frozen.pixels)
            assertFalse(current.pixels.contentEquals(frozen.pixels))
            assertSame(background, preview.compose(background, 21_000))
            preview.updateManual(true, 1, "ABCDEFGHIJKLMNOPQRSTUVWXYZ 日本語", null, 21_001)
            assertFalse(animation(preview).active)
        }
    }

    @Test fun closingOnePreviewRecyclesOnlyItsOwnResources() {
        val first = preview(profiles[0])
        preview(profiles[1]).use { second ->
            first.updateManual(true, 1, "First title", null, 0)
            second.updateManual(true, 1, "Second title", null, 0)
            val firstBitmap = requireNotNull(renderer(first).cachedBitmap)
            val secondBitmap = requireNotNull(renderer(second).cachedBitmap)
            first.close()
            assertTrue(firstBitmap.isRecycled)
            assertFalse(secondBitmap.isRecycled)
            assertTrue(second.compose(frame(profiles[1]), 700).pixels.any { it == 127 })
        }
    }

    @Test fun virtualPreviewDoesNotPublishOrClearProductionMirrorFrames() {
        val frames = mutableListOf<GlyphPreviewFrame.Matrix>()
        val listener: (GlyphPreviewFrame?) -> Unit = { if (it is GlyphPreviewFrame.Matrix) frames += it }
        GlyphPreviewFrameStore.register(listener)
        try {
            val production = frame(profiles[0], 85)
            GlyphPreviewFrameStore.publishMatrix(production.deviceProfile, production.physicalDeviceProfile,
                production.glyphMode, production.matrixSize, production.pixels, force = true)
            val productionPixels = frames.single().pixels.copyOf()
            preview(profiles[0]).use { preview ->
                preview.updateManual(true, 1, "Local 日本語 title", null, 0)
                for (now in listOf(100L, 700L, 1400L)) preview.compose(frame(profiles[0]), now)
            }
            assertEquals(1, frames.size)
            assertArrayEquals(productionPixels, frames.single().pixels)
        } finally { GlyphPreviewFrameStore.unregister(listener) }
    }
}
