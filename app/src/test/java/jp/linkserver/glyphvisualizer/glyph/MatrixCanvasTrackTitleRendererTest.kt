package jp.linkserver.glyphvisualizer.glyph

import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.icu.text.BreakIterator
import android.os.LocaleList
import java.util.Locale
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
class MatrixCanvasTrackTitleRendererTest {
    private fun renderer() = MatrixCanvasTrackTitleRenderer(RuntimeEnvironment.getApplication())

    @Test fun supportedPhysicalAndLogicalProfilesSelectCanvasAtTheirLogicalSize() {
        val context = RuntimeEnvironment.getApplication()
        val p3 = GlyphDeviceProfile.PHONE3_MATRIX
        val pro = GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        for ((physical, logical, size) in listOf(Triple(p3, p3, 25), Triple(pro, pro, 13), Triple(p3, pro, 13))) {
            val selected = MatrixTrackTitleRendererFactory.create(context, physical, logical, size)
            assertTrue(selected is MatrixCanvasTrackTitleRenderer)
            assertEquals(size, (selected as MatrixCanvasTrackTitleRenderer).matrixLength)
            assertEquals(if (size == 13) 21 else 40, selected.pixelsPerSecond)
            assertTrue(selected.preservesTextBrightness)
            assertEquals(-1, selected.leadingMargin)
            assertEquals(12, selected.trailingMargin)
        }
        for ((physical, logical, size) in listOf(
            Triple(pro, p3, 25), Triple(pro, pro, 25),
            Triple(p3, p3, 13), Triple(GlyphDeviceProfile.PHONE2, p3, 25)
        )) {
            val selected = MatrixTrackTitleRendererFactory.create(context, physical, logical, size)
            assertTrue(selected is MatrixSdkTrackTitleRenderer)
            assertEquals(12, selected.pixelsPerSecond)
        }
    }

    @Test fun canvasCreatesLargeTextAndClipsExactOnePixelStepsWithoutRedrawingBitmap() {
        val renderer = renderer()
        val width = renderer.prepare("Glyph Barty 123!", null, 25)
        val bitmap = requireNotNull(renderer.cachedBitmap)
        assertEquals(25, bitmap.height)
        assertEquals(40, renderer.pixelsPerSecond)
        assertTrue(width > 25)
        assertTrue(renderer.render(25).all { it == 0 })
        val visible = renderer.render(0).copyOf()
        assertTrue(visible.any { it == 127 })
        assertTrue(visible.all { it in 0..255 })
        assertTrue(visible.any { it == 42 || it == 85 })
        assertTrue((0 until 25).count { y -> (0 until 25).any { x -> visible[y * 25 + x] != 0 } } > 10)
        val shifted = renderer.render(-1)
        for (y in 0 until 25) for (x in 0 until 24) {
            assertEquals(visible[y * 25 + x + 1], shifted[y * 25 + x])
        }
        assertSame(shifted, renderer.render(-1))
        assertSame(shifted, renderer.render(-2)) // Frame storage is reused across positions too.
        assertSame(bitmap, renderer.cachedBitmap)
        assertTrue(renderer.render(-width).all { it == 0 })
        assertTrue(renderer.render(Int.MIN_VALUE).all { it == 0 })
        assertTrue(renderer.render(Int.MAX_VALUE).all { it == 0 })
        renderer.clear()
    }

    @Test fun sameDisplayCachesBitmapAndReplacementAndClearRecycleIt() {
        val renderer = renderer()
        val width = renderer.prepare("Title", "Artist", 25)
        val first = requireNotNull(renderer.cachedBitmap)
        renderer.render(0)
        assertEquals(width, renderer.prepare("Title", "Artist", 25))
        assertSame(first, renderer.cachedBitmap)
        renderer.prepare("Different title", null, 25)
        assertTrue(first.isRecycled)
        val second = requireNotNull(renderer.cachedBitmap)
        assertNotSame(first, second)
        renderer.clear()
        renderer.clear()
        assertTrue(second.isRecycled)
        assertNull(renderer.cachedBitmap)
        assertTrue(renderer.render(0).all { it == 0 })
    }

    @Test fun japaneseSymbolsArtistAndSingleLineTextUseCanvasAndTrailingSpace() {
        val renderer = renderer()
        val titleWidth = renderer.prepare("日本語の曲名", null, 25)
        assertTrue(titleWidth > 12)
        assertTrue(renderer.render(0).any { it == 127 })
        assertEquals(titleWidth, renderer.prepare("日本語の曲名", "", 25))
        assertTrue(renderer.prepare("日本語の曲名", "アーティスト", 25) > titleWidth)
        val width = renderer.prepare("A\nB\tC \u2028D !?", null, 25)
        val bitmap = requireNotNull(renderer.cachedBitmap)
        assertEquals(width, renderer.prepare("A B C  D !?", null, 25))
        assertSame(bitmap, renderer.cachedBitmap)
        for (x in width - MatrixCanvasTrackTitleRenderer.TRAILING_SPACE_PX until width) {
            for (y in 0 until 25) assertEquals(Color.BLACK, bitmap.getPixel(x, y))
        }
        assertTrue(renderer.render(-(width - 12)).all { it == 0 })
        renderer.clear()
    }

    @Test fun longUnicodeTextIsBoundedAndEmptyTextClears() {
        val renderer = renderer()
        val width = renderer.prepare("Long 日本語 👋 ".repeat(1000), "Artist", 25)
        assertTrue(width <= MatrixTrackTitleAnimation.maxTextWidth(25, 40))
        assertTrue(renderer.render(0).any { it == 127 })
        val bitmap = requireNotNull(renderer.cachedBitmap)
        assertEquals(0, renderer.prepare(" \n\t", null, 25))
        assertNull(renderer.cachedBitmap)
        assertTrue(bitmap.isRecycled)
    }

    @Test fun toyGrayscaleNormalizationAndFourLevelsMatchAllBoundaries() {
        assertEquals(255, MatrixCanvasTrackTitleRenderer.toGray(Color.WHITE))
        assertEquals(76, MatrixCanvasTrackTitleRenderer.toGray(Color.RED))
        assertEquals(149, MatrixCanvasTrackTitleRenderer.toGray(Color.GREEN))
        assertEquals(29, MatrixCanvasTrackTitleRenderer.toGray(Color.BLUE))
        assertEquals(127, MatrixCanvasTrackTitleRenderer.quantizePixel(Color.WHITE))
        assertEquals(0, MatrixCanvasTrackTitleRenderer.quantizePixel(Color.BLACK))
        assertEquals(0, MatrixCanvasTrackTitleRenderer.quantizePixel(Color.TRANSPARENT))
        for (gray in 0..255) {
            val expected = when (gray) { in 0..109 -> 0; in 110..167 -> 85; in 168..226 -> 170; else -> 255 }
            assertEquals("gray=$gray", expected, MatrixCanvasTrackTitleRenderer.quantizeGray(gray))
            val toyLevel = expected * 8
            val preview = (toyLevel * 255 + 2047) / 4095
            assertEquals(preview, MatrixCanvasTrackTitleRenderer.quantizePixel(Color.rgb(gray, gray, gray)))
            assertTrue(kotlin.math.abs(MatrixFrameBrightness.toSdk(preview) - toyLevel) <= 6)
        }
        assertEquals(listOf(0, 674, 1365, 2039), listOf(0, 42, 85, 127).map(MatrixFrameBrightness::toSdk))
    }

    @Test fun unicodeIsPreservedAndLongTitlesEndAtGraphemeBoundaries() {
        val renderer = renderer()
        val original = "漢字ひらがなカタカナ 123!? か\u3099 A\u0301 👩‍🎤 𠮷"
        renderer.prepare(original, null, 25)
        assertEquals(original, renderer.preparedText)
        assertTrue(renderer.preservesTextBrightness)
        val breaks = BreakIterator.getCharacterInstance(Locale.ROOT)
        for (cluster in listOf("か\u3099", "A\u0301", "👩‍🎤", "𠮷", "✈️")) {
            val longTitle = cluster.repeat(1500)
            renderer.prepare(longTitle, null, 25)
            val prefix = requireNotNull(renderer.preparedText).removeSuffix("…")
            assertTrue(longTitle.startsWith(prefix))
            breaks.setText(longTitle)
            assertTrue("Must not split $cluster", breaks.isBoundary(prefix.length))
        }
        renderer.clear()
    }

    @Test fun explicitFontFallbackCanShapeDistinctJapaneseGlyphs() {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = MatrixCanvasTrackTitleRenderer.loadTypeface(RuntimeEnvironment.getApplication())
            textSize = 24f
        }
        assertTrue(paint.hasGlyph("日"))
        assertTrue(paint.hasGlyph("あ"))
        assertTrue(paint.hasGlyph("カ"))
        val renderer = renderer()
        renderer.prepare("日", null, 25)
        val first = renderer.render(0).copyOf()
        renderer.prepare("本", null, 25)
        assertFalse(first.contentEquals(renderer.render(0)))
        renderer.clear()
    }

    @Test fun bothSizesMatchCanvasWithUnchangedPhone3AndBoundsAdjustedProBaseline() {
        val context = RuntimeEnvironment.getApplication()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            isLinearText = true
            typeface = MatrixCanvasTrackTitleRenderer.loadTypeface(context)
            textLocales = LocaleList(Locale.JAPANESE, Locale.ENGLISH)
        }
        for (length in listOf(25, 13)) {
            paint.textSize = if (length == 13) 12.5f else 24f
            val renderer = MatrixCanvasTrackTitleRenderer(context, length)
            try {
                for (text in listOf("ABC 123!?", "漢字ひらがなカタカナ", "日ABC か\u3099 👩‍🎤 𠮷")) {
                    val width = renderer.prepare(text, null, length)
                    assertEquals(kotlin.math.ceil(paint.measureText(text)).toInt() + 12, width)
                    val cached = requireNotNull(renderer.cachedBitmap)
                    assertEquals(length, cached.height)
                    assertEquals(Bitmap.Config.ARGB_8888, cached.config)
                    val viewport = Bitmap.createBitmap(length, length, Bitmap.Config.ARGB_8888)
                    try {
                        val canvas = Canvas(viewport)
                        val metrics = paint.fontMetrics
                        val baseline = if (length == 13) expectedProBaseline(paint, text) else
                            minOf((length + kotlin.math.abs(metrics.ascent)) / 2f, length - metrics.descent)
                        for (x in listOf(length - 1, 0, -13, -width)) {
                            canvas.drawColor(Color.BLACK)
                            canvas.drawText(text, x.toFloat(), baseline, paint)
                            val expected = IntArray(length * length)
                            viewport.getPixels(expected, 0, length, 0, 0, length, length)
                            for (i in expected.indices) expected[i] = MatrixCanvasTrackTitleRenderer.quantizePixel(expected[i])
                            assertArrayEquals("length=$length x=$x text=$text", expected, renderer.render(x))
                            assertSame(cached, renderer.cachedBitmap)
                        }
                    } finally { viewport.recycle() }
                }
            } finally { renderer.clear() }
        }
    }

    @Test fun proDescendersCapsDigitsJapaneseAndAccentsFitCanvasBeforePhysicalMask() {
        val context = RuntimeEnvironment.getApplication()
        val renderer = MatrixCanvasTrackTitleRenderer(context, 13)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12.5f
            isLinearText = true
            typeface = MatrixCanvasTrackTitleRenderer.loadTypeface(context)
            textLocales = LocaleList(Locale.JAPANESE, Locale.ENGLISH)
        }
        val padding = 32
        try {
            for (text in listOf("gjpqy", "Agjpqy 123", "ABCDEFGHIJKLMNOPQRSTUVWXYZ", "0123456789",
                "漢字ひらがなカタカナ", "日本 ABC 123", "ÁÉÍÓÚ ÄÖÜ", "Çñ", "A\u0301 e\u0301")) {
                val baseline = expectedProBaseline(paint, text)
                val width = renderer.prepare(text, null, 13)
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
                    println("Pro text=$text baseline=$baseline inkRows=${inkRows.first() - padding}..${inkRows.last() - padding}")
                    assertTrue("Canvas clips top: $text", inkRows.first() >= padding)
                    assertTrue("Canvas clips bottom: $text", inkRows.last() < padding + 13)
                    if (text == "gjpqy") {
                        val metrics = paint.fontMetrics
                        assertEquals(minOf((13 + kotlin.math.abs(metrics.ascent)) / 2f, 13 - metrics.descent) - 1f,
                            baseline, 0f)
                        assertEquals("Descender must survive in the last Canvas row", padding + 12, inkRows.last())
                    }
                } finally { padded.recycle() }
            }
        } finally { renderer.clear() }
    }

    private fun expectedProBaseline(paint: Paint, text: String): Float {
        val metrics = paint.fontMetrics
        val preferred = minOf((13 + kotlin.math.abs(metrics.ascent)) / 2f, 13 - metrics.descent) - 1f
        val bounds = Rect().also { paint.getTextBounds(text, 0, text.length, it) }
        val top = -bounds.top.toFloat()
        val bottom = 13 - bounds.bottom.toFloat()
        return if (top <= bottom) preferred.coerceIn(top, bottom) else (top + bottom) / 2f
    }

    @Test fun proTextAndEmulatedProTextShareLogicalPixelsAndPhysicalMasks() {
        val context = RuntimeEnvironment.getApplication()
        val p3 = GlyphDeviceProfile.PHONE3_MATRIX
        val pro = GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        val native = MatrixTrackTitleRendererFactory.create(context, pro, pro, 13)
        val emulated = MatrixTrackTitleRendererFactory.create(context, p3, pro, 13)
        val proHeights = GlyphMatrixPreviewGeometry.columnHeights(pro, 13)
        val p3Heights = GlyphMatrixPreviewGeometry.columnHeights(p3, 25)
        assertEquals(137, proHeights.sum())
        assertEquals(489, p3Heights.sum())
        try {
            val width = native.prepare("gjpqy 日本語 ABC 123", null, 13)
            assertEquals(width, emulated.prepare("gjpqy 日本語 ABC 123", null, 13))
            var maskedInk = 0
            var retainedInk = 0
            for (x in 12 downTo -width step 7) {
                val logical = native.render(x)
                assertEquals(169, logical.size)
                assertArrayEquals(logical, emulated.render(x))
                val masked = IntArray(169)
                MatrixTrackTitleCompositor.compose(IntArray(169), logical, 0.07f, 13, proHeights, masked, true)
                for (row in 0 until 13) for (column in 0 until 13) {
                    val index = row * 13 + column
                    val top = (13 - proHeights[column]) / 2
                    val hasLed = row in top until top + proHeights[column]
                    assertEquals(if (hasLed) logical[index] else 0, masked[index])
                    if (logical[index] > 0) { if (hasLed) retainedInk++ else maskedInk++ }
                }
                val physical = IntArray(625)
                GlyphMatrixProfileEmulator.copyPhone4aProFrameIntoCenteredRegion(masked, 25, physical)
                val sdk = IntArray(625)
                MatrixFrameBrightness.copyToSdk(physical, sdk, 25, p3Heights)
                for (py in 0 until 25) for (px in 0 until 25) {
                    val value = if (py in 6..18 && px in 6..18) masked[(py - 6) * 13 + px - 6] else 0
                    assertEquals(value, physical[py * 25 + px])
                    assertEquals(MatrixFrameBrightness.toSdk(value), sdk[py * 25 + px])
                }
            }
            assertTrue("Physical corner LEDs must be absent even when Canvas has ink", maskedInk > 0)
            assertTrue("Existing LEDs must retain Canvas ink", retainedInk > 0)
        } finally { native.clear(); emulated.clear() }
    }

    @Test fun proLongUnicodeCacheAndLocaleDirectionRemainBounded() {
        val context = RuntimeEnvironment.getApplication()
        val previous = Locale.getDefault()
        try {
            for ((locale, rtl) in listOf(Locale.JAPANESE to false, Locale.forLanguageTag("ar") to true)) {
                Locale.setDefault(locale)
                val renderer = MatrixCanvasTrackTitleRenderer(context, 13)
                try {
                    assertEquals(rtl, renderer.rightToLeft)
                    val original = "か\u3099👩‍🎤𠮷✈️".repeat(1000)
                    val width = renderer.prepare(original, null, 13)
                    val prefix = requireNotNull(renderer.preparedText).removeSuffix("…")
                    val breaks = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(original) }
                    assertTrue(breaks.isBoundary(prefix.length))
                    assertEquals(21, renderer.pixelsPerSecond)
                    assertTrue(width <= MatrixTrackTitleAnimation.maxTextWidth(13, 21, -1))
                    val bitmap = requireNotNull(renderer.cachedBitmap)
                    assertEquals(width, renderer.prepare(original, null, 13))
                    assertSame(bitmap, renderer.cachedBitmap)
                    assertThrows(IllegalArgumentException::class.java) { renderer.prepare("A", null, 25) }
                    renderer.clear()
                    assertTrue(bitmap.isRecycled)
                } finally { renderer.clear() }
            }
        } finally { Locale.setDefault(previous) }
    }

    @Test fun profileSpeedControlsPixelTravelLongTextLimitAndUnchangedFades() {
        val context = RuntimeEnvironment.getApplication()
        for ((length, speed, widthLimit) in listOf(Triple(25, 40, 776), Triple(13, 21, 408))) {
            val renderer = MatrixCanvasTrackTitleRenderer(context, length)
            try {
                val width = renderer.prepare("長い曲名 ABC か\u3099 👩‍🎤 ".repeat(200), null, length)
                assertEquals(speed, renderer.pixelsPerSecond)
                assertEquals(widthLimit, MatrixTrackTitleAnimation.maxTextWidth(length, speed, renderer.leadingMargin))
                assertTrue(width in 300..widthLimit) // Includes the unchanged 12px trailing margin.
                val animation = MatrixTrackTitleAnimation()
                animation.start(0, length, width, renderer.pixelsPerSecond, renderer.leadingMargin, renderer.trailingMargin)
                assertTrue(animation.scrollDurationMs <= 20_000L)
                animation.advance(100)
                assertEquals(0.535f, animation.backgroundBrightness, 0.001f)
                val firstPixelMs = (1000L + speed - 1) / speed
                animation.advance(200 + firstPixelMs - 1)
                assertEquals(length - 1, animation.textX)
                animation.advance(200 + firstPixelMs)
                assertEquals(length - 2, animation.textX)
                animation.advance(1200)
                assertEquals(length - 1 - speed, animation.textX)
                assertEquals(0.07f, animation.backgroundBrightness, 0f)
                val end = 200 + animation.scrollDurationMs
                animation.advance(end)
                assertNull(animation.textX)
                assertTrue(renderer.render(-width).all { it == 0 })
                animation.advance(end + 150)
                assertEquals(0.535f, animation.backgroundBrightness, 0.001f)
                animation.advance(end + 300)
                assertFalse(animation.active)
                assertEquals(1f, animation.backgroundBrightness, 0f)
                for (rtl in listOf(false, true)) {
                    animation.start(30_000, length, Int.MAX_VALUE, speed, -1, 12, rtl)
                    assertEquals(20_000L, animation.scrollDurationMs)
                    animation.advance(50_199)
                    assertEquals(0.07f, animation.backgroundBrightness, 0f)
                    animation.advance(50_200)
                    assertNull(animation.textX)
                    animation.advance(50_500)
                    assertFalse(animation.active)
                }
            } finally { renderer.clear() }
        }
        assertEquals(12, MatrixTrackTitleAnimation.PIXELS_PER_SECOND) // SDK/global default is unchanged.
    }
}
