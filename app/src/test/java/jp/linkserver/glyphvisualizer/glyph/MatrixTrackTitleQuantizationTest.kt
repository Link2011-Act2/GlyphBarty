package jp.linkserver.glyphvisualizer.glyph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import kotlin.math.roundToInt
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
class MatrixTrackTitleQuantizationTest {
    private val samples = listOf("Glyph Barty", "Ilij! 123", "gjpqy ÁÉÍÓÚ", "日本語の曲名", "漢字ひらがなカタカナ")
    private data class Distribution(val raw: IntArray, val levels: IntArray, val lost: Int, val brightness: Long) {
        val ink: Int get() = raw.sum() - raw[0]
        val visible: Int get() = levels.drop(1).sum()
    }

    private fun distribution(pixels: IntArray, gain: Float): Distribution {
        val raw = IntArray(256)
        val levels = IntArray(4)
        var lost = 0
        var brightness = 0L
        for (pixel in pixels) {
            val gray = MatrixCanvasTrackTitleRenderer.toGray(pixel)
            raw[gray]++
            val corrected = (gray * gain).roundToInt().coerceIn(0, 255)
            val quantized = MatrixCanvasTrackTitleRenderer.quantizeGray(corrected)
            levels[quantized / 85]++
            if (gray > 0 && quantized == 0) lost++
            brightness += quantized
        }
        return Distribution(raw, levels, lost, brightness)
    }

    @Test fun compareProfilePixelDistributionsAndProGainCandidates() {
        val folder = File("build/reports/pro-title-quantization").apply { mkdirs() }
        val summary = StringBuilder("text,size,gain,rawInk,rawAA,rawWhite,lost,visible,q0,q85,q170,q255,brightness\n")
        val histogram = StringBuilder("text,size,gray,count\n")
        val context = RuntimeEnvironment.getApplication()
        for ((sample, text) in samples.withIndex()) {
            val rows = mutableListOf<Pair<String, Bitmap>>()
            try {
                for (size in listOf(25, 13)) {
                    val renderer = MatrixCanvasTrackTitleRenderer(context, size)
                    try {
                        renderer.prepare(text, null, size)
                        val bitmap = requireNotNull(renderer.cachedBitmap)
                        val raw = IntArray(bitmap.width * size)
                        bitmap.getPixels(raw, 0, bitmap.width, 0, 0, bitmap.width, size)
                        rows += "$size raw" to bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        val gains = if (size == 13) listOf(1f, 1.2f, 1.25f, 1.3f) else listOf(1f)
                        for (gain in gains) {
                            val data = distribution(raw, gain)
                            assertEquals(raw.size, data.raw.sum())
                            assertEquals(raw.size, data.levels.sum())
                            assertEquals(data.ink - data.visible, data.lost)
                            assertTrue(data.visible > 0)
                            val textCsv = "\"${text.replace("\"", "\"\"")}\""
                            summary.appendLine(listOf(textCsv, size, gain, data.ink,
                                data.raw.slice(1..254).sum(), data.raw[255], data.lost, data.visible,
                                *data.levels.toTypedArray(), data.brightness).joinToString(","))
                            if (gain == 1f) for (gray in 0..255) {
                                histogram.appendLine("$textCsv,$size,$gray,${data.raw[gray]}")
                            }
                            val quantized = IntArray(raw.size) { index ->
                                val gray = MatrixCanvasTrackTitleRenderer.toGray(raw[index])
                                val level = MatrixCanvasTrackTitleRenderer.quantizeGray((gray * gain).roundToInt().coerceAtMost(255))
                                // Normalize both profiles' same text maximum to white for inspection.
                                Color.rgb(level, level, level)
                            }
                            rows += "$size gain $gain" to Bitmap.createBitmap(quantized, bitmap.width, size, Bitmap.Config.ARGB_8888)
                        }
                    } finally { renderer.clear() }
                }
                saveComparison(File(folder, "sample-$sample.png"), rows)
            } finally { rows.forEach { it.second.recycle() } }
        }
        File(folder, "distributions.csv").writeText(summary.toString())
        File(folder, "raw-histograms.csv").writeText(histogram.toString())
        println(summary)
    }

    @Test fun proCorrectionPreservesFourLevelsPeakAndBlackWhilePhone3IsExactlyLegacy() {
        val levels = mutableSetOf<Int>()
        for (gray in 0..255) {
            val pixel = Color.rgb(gray, gray, gray)
            val measured = MatrixCanvasTrackTitleRenderer.toGray(pixel)
            val legacy = when (measured) { in 0..109 -> 0; in 110..167 -> 42; in 168..226 -> 85; else -> 127 }
            val corrected = when (measured) { in 0..87 -> 0; in 88..133 -> 42; in 134..181 -> 85; else -> 127 }
            assertEquals("Phone3 gray=$gray", legacy, MatrixCanvasTrackTitleRenderer.quantizePixel(pixel, 25))
            assertEquals("Pro gray=$gray", corrected, MatrixCanvasTrackTitleRenderer.quantizePixel(pixel, 13))
            assertEquals(legacy, MatrixCanvasTrackTitleRenderer.quantizePixel(pixel))
            levels += corrected
        }
        assertEquals(setOf(0, 42, 85, 127), levels)
        assertEquals(0, MatrixCanvasTrackTitleRenderer.quantizePixel(Color.BLACK, 13))
        assertEquals(127, MatrixCanvasTrackTitleRenderer.quantizePixel(Color.WHITE, 13))
    }

    @Test fun chosenGainRecoversThinStrokesWithBoundedGrowthAndNoInkOutsideRawCoverage() {
        var oldBrightness = 0L
        var newBrightness = 0L
        var oldVisible = 0
        var newVisible = 0
        val renderer = MatrixCanvasTrackTitleRenderer(RuntimeEnvironment.getApplication(), 13)
        try {
            for (text in samples) {
                renderer.prepare(text, null, 13)
                val bitmap = requireNotNull(renderer.cachedBitmap)
                val raw = IntArray(bitmap.width * 13)
                bitmap.getPixels(raw, 0, bitmap.width, 0, 0, bitmap.width, 13)
                val before = distribution(raw, 1f)
                val after = distribution(raw, MatrixCanvasTrackTitleRenderer.PRO_TEXT_GRAY_GAIN)
                assertTrue("No brightness recovery: $text", after.brightness > before.brightness)
                assertTrue("No visible stroke recovery: $text", after.lost < before.lost)
                assertTrue("Excessive support growth: $text", after.visible <= before.visible * 1.2)
                for (left in 0 until bitmap.width step 13) {
                    val frame = renderer.render(-left)
                    for (y in 0 until 13) for (x in 0 until minOf(13, bitmap.width - left)) {
                        val pixel = raw[y * bitmap.width + left + x]
                        val actual = frame[y * 13 + x]
                        assertEquals(MatrixCanvasTrackTitleRenderer.quantizePixel(pixel, 13), actual)
                        if (MatrixCanvasTrackTitleRenderer.toGray(pixel) <= 87) assertEquals("No halo: $text", 0, actual)
                    }
                }
                oldBrightness += before.brightness
                newBrightness += after.brightness
                oldVisible += before.visible
                newVisible += after.visible
            }
        } finally { renderer.clear() }
        assertTrue(newBrightness > oldBrightness * 1.3)
        assertTrue(newBrightness < oldBrightness * 1.5)
        assertTrue(newVisible <= oldVisible * 1.15)
    }

    @Test fun nativeEmulatedAndInspectorProUseIdenticalCorrectedLogicalFrames() {
        val context = RuntimeEnvironment.getApplication()
        val pro = GlyphDeviceProfile.PHONE4A_PRO_MATRIX
        val native = MatrixTrackTitleRendererFactory.create(context, pro, pro, 13)
        val emulated = MatrixTrackTitleRendererFactory.create(context, GlyphDeviceProfile.PHONE3_MATRIX, pro, 13)
        val animation = MatrixTrackTitleAnimation()
        val background = GlyphPreviewFrame.Matrix(pro, pro, GlyphPatternRegistry.P3_MATRIX_ALL_BRIGHTNESS,
            0, 13, IntArray(169))
        val heights = GlyphMatrixPreviewGeometry.columnHeights(pro, 13)
        val expected = IntArray(169)
        GlyphInspectorTrackTitlePreview(context, pro).use { inspector ->
            try {
                val width = native.prepare("Ilij! 日本語 gjpqy", null, 13)
                assertEquals(width, emulated.prepare("Ilij! 日本語 gjpqy", null, 13))
                animation.start(0, 13, width, native.pixelsPerSecond, native.leadingMargin, native.trailingMargin, native.rightToLeft)
                inspector.updateManual(true, 1, "Ilij! 日本語 gjpqy", null, 0)
                for (now in listOf(200L, 600L, 1000L, 1800L)) {
                    animation.advance(now)
                    val x = requireNotNull(animation.textX)
                    assertArrayEquals(native.render(x), emulated.render(x))
                    MatrixTrackTitleCompositor.compose(background.pixels, native.render(x),
                        animation.backgroundBrightness, 13, heights, expected, true)
                    assertArrayEquals(expected, inspector.compose(background, now).pixels)
                }
            } finally { native.clear(); emulated.clear() }
        }
    }

    private fun saveComparison(file: File, rows: List<Pair<String, Bitmap>>) {
        val scale = 6
        val labelWidth = 180
        val board = Bitmap.createBitmap(labelWidth + rows.maxOf { it.second.width } * scale,
            rows.sumOf { it.second.height * scale + 20 }, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(board)
            canvas.drawColor(Color.BLACK)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 18f }
            var y = 0
            for ((name, bitmap) in rows) {
                canvas.drawText(name, 4f, y + 22f, label)
                val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width * scale, bitmap.height * scale, false)
                try { canvas.drawBitmap(scaled, labelWidth.toFloat(), y.toFloat(), null) }
                finally { scaled.recycle() }
                y += bitmap.height * scale + 20
            }
            file.outputStream().use { board.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally { board.recycle() }
    }
}
