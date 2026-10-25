package jp.linkserver.glyphvisualizer.glyph

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Phone3TrackTitleRendererDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun playingSilenceKeepsLargeMultitoneTitleBeyondTheFormerStartupGrace() {
        val renderer = Phone3TrackTitleRenderer(context)
        val animation = MatrixTrackTitleAnimation()
        val policy = MatrixPlaybackSessionPolicy()
        val snapshot = jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackSnapshot(
            jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus.PLAYING, null)
        val background = IntArray(625)
        val composite = IntArray(625)
        val heights = GlyphMatrixPreviewGeometry.columnHeights(GlyphDeviceProfile.PHONE3_MATRIX, 25)
        try {
            val width = renderer.prepare("日本語の長い曲名 Artist ABC 123", null, 25)
            animation.start(1000, 25, width, renderer.pixelsPerSecond)
            for (elapsed in listOf(100L, 300L, 500L, 1000L, 2500L)) {
                assertTrue(policy.evaluate(snapshot, 1000 + elapsed, false, false).retainSession)
                animation.advance(1000 + elapsed)
                assertTrue(animation.active)
                assertEquals(if (elapsed < 200) 0.55f else 0.1f, animation.backgroundBrightness, 0.001f)
                MatrixTrackTitleCompositor.compose(background, animation.textX?.let { renderer.render(it) },
                    animation.backgroundBrightness, 25, heights, composite, true)
                if (elapsed >= 500) {
                    assertTrue(composite.any { it > 0 })
                    assertTrue(composite.any { it in 1..254 })
                }
            }
        } finally { renderer.clear() }
    }

    @Test fun bundledNTypeAndAndroidJapaneseFallbackRenderLargeSingleLineText() {
        val font = Phone3TrackTitleRenderer.loadTypeface(context)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = 24f }
        assertTrue(paint.hasGlyph("A"))
        assertTrue("Android's font fallback must provide Japanese glyphs", paint.hasGlyph("日"))
        val renderer = Phone3TrackTitleRenderer(context)
        try {
            renderer.prepare("日本語の曲名 - アーティスト", null, 25)
            val frame = renderer.render(0).copyOf()
            assertTrue(frame.any { it == 255 })
            assertTrue(frame.any { it in 1..254 })
            assertTrue((0 until 25).count { y -> (0 until 25).any { x -> frame[y * 25 + x] > 0 } } > 10)
            savePreview(renderer, "japanese")
            renderer.prepare("Glyph Barty - Cafe 123!", null, 25)
            assertFalse(frame.contentEquals(renderer.render(0)))
            savePreview(renderer, "latin")
        } finally { renderer.clear() }
    }

    @Test fun japaneseMixedUnicodeAndAntialiasingSurviveCompositionAndDeviceScale() {
        val renderer = Phone3TrackTitleRenderer(context)
        val heights = GlyphMatrixPreviewGeometry.columnHeights(GlyphDeviceProfile.PHONE3_MATRIX, 25)
        val background = IntArray(625) { 100 }
        val preview = IntArray(625)
        val sdk = IntArray(625)
        try {
            for (title in listOf("日本語", "ひらがな", "カタカナ", "日ABC123!?", "か\u3099 𠮷 👩‍🎤")) {
                val width = renderer.prepare(title, null, 25)
                assertEquals(title, renderer.preparedText)
                var intermediate = false
                for (position in 0 downTo -width step 10) {
                    MatrixTrackTitleCompositor.compose(background, renderer.render(position),
                        MatrixTrackTitleAnimation.BACKGROUND_LEVEL, 25, heights, preview, true)
                    MatrixFrameBrightness.copyToSdk(preview, sdk, 25, heights)
                    if (preview.any { it in 21..254 } && sdk.any { it in 21..4094 }) intermediate = true
                }
                assertTrue("Missing intermediate text brightness: $title", intermediate)
            }
            renderer.prepare("日", null, 25)
            val first = renderer.render(0).copyOf()
            renderer.prepare("本", null, 25)
            assertFalse(first.contentEquals(renderer.render(0)))
        } finally { renderer.clear() }
    }

    @Test fun bitmapAndFrameAreReusedAndEndOfTravelIsBlank() {
        val renderer = Phone3TrackTitleRenderer(context)
        try {
            val width = renderer.prepare("Title", "Artist", 25)
            val bitmap = requireNotNull(renderer.cachedBitmap)
            assertEquals(width, renderer.prepare("Title", "Artist", 25))
            assertSame(bitmap, renderer.cachedBitmap)
            val frame = renderer.render(25)
            assertTrue(frame.all { it == 0 })
            for (x in 24 downTo -width) {
                assertSame(frame, renderer.render(x))
                assertSame(bitmap, renderer.cachedBitmap)
            }
            assertTrue(frame.all { it == 0 })
            renderer.prepare("Next song", null, 25)
            assertTrue(bitmap.isRecycled)
            val replacement = requireNotNull(renderer.cachedBitmap)
            renderer.clear()
            assertTrue(replacement.isRecycled)
        } finally { renderer.clear() }
    }

    private fun savePreview(renderer: Phone3TrackTitleRenderer, name: String) {
        val folder = File(context.getExternalFilesDir(null), "phone3-title-preview").apply { mkdirs() }
        File(folder, "$name-strip.png").outputStream().use {
            requireNotNull(renderer.cachedBitmap).compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val sheet = Bitmap.createBitmap(100, 25, Bitmap.Config.ARGB_8888)
        val background = IntArray(625) { 160 }
        val composed = IntArray(625)
        val heights = GlyphMatrixPreviewGeometry.columnHeights(GlyphDeviceProfile.PHONE3_MATRIX, 25)
        for ((column, x) in listOf(0, -10, -25, -40).withIndex()) {
            MatrixTrackTitleCompositor.compose(background, renderer.render(x), MatrixTrackTitleAnimation.BACKGROUND_LEVEL, 25, heights, composed, true)
            for (y in 0 until 25) for (px in 0 until 25) {
                val level = composed[y * 25 + px]
                sheet.setPixel(column * 25 + px, y, Color.rgb(level, level, level))
            }
        }
        val scaled = Bitmap.createScaledBitmap(sheet, 800, 200, false)
        try {
            File(folder, "$name-matrix.png").outputStream().use {
                scaled.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        } finally { scaled.recycle(); sheet.recycle() }
    }
}
