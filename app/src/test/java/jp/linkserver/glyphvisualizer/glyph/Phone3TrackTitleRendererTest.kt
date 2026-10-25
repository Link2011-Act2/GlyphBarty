package jp.linkserver.glyphvisualizer.glyph

import android.graphics.Color
import android.graphics.Paint
import android.icu.text.BreakIterator
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
class Phone3TrackTitleRendererTest {
    private fun renderer() = Phone3TrackTitleRenderer(RuntimeEnvironment.getApplication())

    @Test fun onlyPhoneThreeTwentyFivePixelProfileSelectsCanvasRenderer() {
        val context = RuntimeEnvironment.getApplication()
        val p3 = GlyphDeviceProfile.PHONE3_MATRIX
        val pro = GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        assertTrue(MatrixTrackTitleRendererFactory.create(context, p3, p3, 25) is Phone3TrackTitleRenderer)
        for ((physical, logical, size) in listOf(
            Triple(pro, pro, 13), Triple(p3, pro, 13), Triple(pro, p3, 25),
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
        assertTrue(visible.any { it == 255 })
        assertTrue(visible.all { it in 0..255 })
        assertTrue(visible.any { it in 1..254 })
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
        assertTrue(renderer.render(0).any { it == 255 })
        assertEquals(titleWidth, renderer.prepare("日本語の曲名", "", 25))
        assertTrue(renderer.prepare("日本語の曲名", "アーティスト", 25) > titleWidth)
        val width = renderer.prepare("A\nB\tC \u2028D !?", null, 25)
        val bitmap = requireNotNull(renderer.cachedBitmap)
        assertEquals(width, renderer.prepare("A B C  D !?", null, 25))
        assertSame(bitmap, renderer.cachedBitmap)
        for (x in width - Phone3TrackTitleRenderer.TRAILING_SPACE_PX until width) {
            for (y in 0 until 25) assertEquals(Color.BLACK, bitmap.getPixel(x, y))
        }
        assertTrue(renderer.render(-(width - 12)).all { it == 0 })
        renderer.clear()
    }

    @Test fun longUnicodeTextIsBoundedAndEmptyTextClears() {
        val renderer = renderer()
        val width = renderer.prepare("Long 日本語 👋 ".repeat(1000), "Artist", 25)
        assertTrue(width <= MatrixTrackTitleAnimation.maxTextWidth(25, 40))
        assertTrue(renderer.render(0).any { it == 255 })
        val bitmap = requireNotNull(renderer.cachedBitmap)
        assertEquals(0, renderer.prepare(" \n\t", null, 25))
        assertNull(renderer.cachedBitmap)
        assertTrue(bitmap.isRecycled)
    }

    @Test fun grayscaleAlphaConversionPreservesAllCoverageLevels() {
        assertEquals(255, Phone3TrackTitleRenderer.quantizePixel(Color.WHITE))
        assertEquals(0, Phone3TrackTitleRenderer.quantizePixel(Color.BLACK))
        assertEquals(0, Phone3TrackTitleRenderer.quantizePixel(Color.TRANSPARENT))
        for (level in 0..255) {
            assertEquals(level, Phone3TrackTitleRenderer.quantizePixel(Color.rgb(level, level, level)))
            assertEquals(level, Phone3TrackTitleRenderer.quantizePixel(Color.argb(level, 255, 255, 255)))
        }
        assertEquals(149, Phone3TrackTitleRenderer.quantizePixel(Color.GREEN))
        assertEquals(29, Phone3TrackTitleRenderer.quantizePixel(Color.BLUE))
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
            typeface = Phone3TrackTitleRenderer.loadTypeface(RuntimeEnvironment.getApplication())
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
}
