package jp.linkserver.glyphvisualizer.glyph

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale

class MatrixCanvasTrackTitleRendererDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun proDescendersCapsDigitsJapaneseAndAccentsFitAndroidCanvasBeforeLedMask() {
        val renderer = MatrixCanvasTrackTitleRenderer(context, 13)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12.5f
            isLinearText = true
            typeface = MatrixCanvasTrackTitleRenderer.loadTypeface(context)
            textLocales = LocaleList(Locale.JAPANESE, Locale.ENGLISH)
        }
        val padding = 32
        val inkExtents = mutableListOf<String>()
        var allInkFits = true
        try {
            for (text in listOf("gjpqy", "Agjpqy 123", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "0123456789",
                "漢字ひらがなカタカナ", "日本 ABC 123", "ÁÉÍÓÚ ÄÖÜ", "Çñ", "A\u0301 e\u0301")) {
                val metrics = paint.fontMetrics
                val preferred = minOf((13 + kotlin.math.abs(metrics.ascent)) / 2f, 13 - metrics.descent) - 1f
                val bounds = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
                val firstBaseline = -bounds.top.toFloat()
                val lastBaseline = 13 - bounds.bottom.toFloat()
                val baseline = if (firstBaseline <= lastBaseline) preferred.coerceIn(firstBaseline, lastBaseline)
                    else (firstBaseline + lastBaseline) / 2f
                val width = renderer.prepare(text, null, 13)
                assertEquals(kotlin.math.ceil(paint.measureText(text)).toInt() + 12, width)
                val bitmap = requireNotNull(renderer.cachedBitmap)
                assertEquals(13, bitmap.height)
                val padded = Bitmap.createBitmap(width, 13 + padding * 2, Bitmap.Config.ARGB_8888)
                try {
                    Canvas(padded).apply {
                        drawColor(Color.BLACK)
                        drawText(text, 0f, baseline + padding, paint)
                    }
                    val expected = IntArray(width * 13)
                    val actual = IntArray(expected.size)
                    padded.getPixels(expected, 0, width, 0, padding, width, 13)
                    bitmap.getPixels(actual, 0, width, 0, 0, width, 13)
                    assertArrayEquals("Vertical crop differs: $text", expected, actual)
                    val inkRows = (0 until padded.height).filter { y ->
                        (0 until width).any { x -> padded.getPixel(x, y) != Color.BLACK }
                    }
                    assertTrue(inkRows.isNotEmpty())
                    inkExtents += "$text bounds=$bounds baseline=$baseline inkRows=${inkRows.first() - padding}..${inkRows.last() - padding}"
                    allInkFits = allInkFits && inkRows.first() >= padding && inkRows.last() < padding + 13
                } finally { padded.recycle() }
            }
            android.util.Log.i("ProTitleBoundsTest", inkExtents.joinToString("\n"))
            assertTrue("Canvas clipping:\n${inkExtents.joinToString("\n")}", allInkFits)
        } finally { renderer.clear() }
    }

    @Test fun bothLogicalSizesUseFourTextLevelsAndProEmulationMapsThemWithoutRescaling() {
        for (length in listOf(25, 13)) {
            val profile = if (length == 25) GlyphDeviceProfile.PHONE3_MATRIX else GlyphDeviceProfile.PHONE4A_PRO_MATRIX
            val renderer = MatrixCanvasTrackTitleRenderer(context, length)
            val heights = GlyphMatrixPreviewGeometry.columnHeights(profile, length)
            assertEquals(if (length == 25) 489 else 137, heights.sum())
            try {
                for (title in listOf("Glyph Barty 123!?", "漢字ひらがなカタカナ", "か\u3099 𠮷 👩‍🎤 ABC")) {
                    val width = renderer.prepare(title, null, length)
                    assertEquals(title, renderer.preparedText)
                    assertEquals(length, requireNotNull(renderer.cachedBitmap).height)
                    var levels = emptySet<Int>()
                    val logical = IntArray(length * length)
                    for (x in length - 1 downTo -width step 3) {
                        MatrixTrackTitleCompositor.compose(IntArray(logical.size), renderer.render(x), 0.07f,
                            length, heights, logical, true)
                        levels = levels + logical.toSet()
                        if (length == 13) {
                            val physical = IntArray(625)
                            GlyphMatrixProfileEmulator.copyPhone4aProFrameIntoCenteredRegion(logical, 25, physical)
                            val sdk = IntArray(625)
                            MatrixFrameBrightness.copyToSdk(physical, sdk, 25,
                                GlyphMatrixPreviewGeometry.columnHeights(GlyphDeviceProfile.PHONE3_MATRIX, 25))
                            for (py in 0 until 25) for (px in 0 until 25) {
                                val expected = if (py in 6..18 && px in 6..18) logical[(py - 6) * 13 + px - 6] else 0
                                assertEquals(expected, physical[py * 25 + px])
                                assertEquals(MatrixFrameBrightness.toSdk(expected), sdk[py * 25 + px])
                            }
                        }
                    }
                    assertEquals(setOf(0, 42, 85, 127), levels)
                }
            } finally { renderer.clear() }
        }
    }

    @Test fun playingSilenceKeepsLargeMultitoneTitleBeyondTheFormerStartupGrace() {
        val renderer = MatrixCanvasTrackTitleRenderer(context)
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
                assertEquals(if (elapsed < 200) 0.535f else 0.07f, animation.backgroundBrightness, 0.001f)
                MatrixTrackTitleCompositor.compose(background, animation.textX?.let { renderer.render(it) },
                    animation.backgroundBrightness, 25, heights, composite, true)
                if (elapsed >= 500) {
                    assertTrue(composite.any { it > 0 })
                    assertTrue(composite.any { it == 42 || it == 85 })
                }
            }
        } finally { renderer.clear() }
    }

    @Test fun bundledNTypeAndAndroidJapaneseFallbackRenderLargeSingleLineQuantizedText() {
        val font = MatrixCanvasTrackTitleRenderer.loadTypeface(context)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = font; textSize = 24f }
        assertTrue(paint.hasGlyph("A"))
        assertTrue("Android's font fallback must provide Japanese glyphs", paint.hasGlyph("日"))
        val renderer = MatrixCanvasTrackTitleRenderer(context)
        try {
            renderer.prepare("日本語の曲名 - アーティスト", null, 25)
            val frame = renderer.render(0).copyOf()
            assertTrue(frame.any { it == 127 })
            assertTrue(frame.any { it == 42 || it == 85 })
            assertTrue((0 until 25).count { y -> (0 until 25).any { x -> frame[y * 25 + x] > 0 } } > 10)
            savePreview(renderer, "japanese")
            renderer.prepare("Glyph Barty - Cafe 123!", null, 25)
            assertFalse(frame.contentEquals(renderer.render(0)))
            savePreview(renderer, "latin")
        } finally { renderer.clear() }
    }

    @Test fun japaneseMixedUnicodeAndAntialiasingSurviveCompositionAndDeviceScale() {
        val renderer = MatrixCanvasTrackTitleRenderer(context)
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
        val renderer = MatrixCanvasTrackTitleRenderer(context)
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

    private fun savePreview(renderer: MatrixCanvasTrackTitleRenderer, name: String) {
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
