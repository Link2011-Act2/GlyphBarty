package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.icu.text.BreakIterator
import android.os.LocaleList
import jp.linkserver.glyphvisualizer.R
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

/** Independent Canvas implementation, owned by the same thread as the Matrix controller. */
internal class Phone3TrackTitleRenderer(context: Context) : MatrixTrackTitleRenderer {
    companion object {
        const val MATRIX_LENGTH = 25
        const val FONT_SIZE_PX = 24f
        const val SCROLL_PIXELS_PER_SECOND = 40
        const val TRAILING_SPACE_PX = 12
        private const val MAX_INPUT_CODE_POINTS = 512

        internal fun quantizePixel(argb: Int): Int {
            val gray = (Color.red(argb) * 77 + Color.green(argb) * 150 + Color.blue(argb) * 29 + 128) shr 8
            val brightness = gray * Color.alpha(argb) / 255
            return brightness // Keep all 8-bit coverage levels, including antialiased edges.
        }

        internal fun loadTypeface(context: Context): Typeface = runCatching {
            val font = Font.Builder(context.resources, R.font.ntype82_regular).build()
            Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
                .setSystemFallback("sans-serif")
                .build()
        }.getOrElse { Typeface.create("NType82-Regular", Typeface.NORMAL) }
    }

    override val pixelsPerSecond = SCROLL_PIXELS_PER_SECOND
    override val preservesTextBrightness = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = FONT_SIZE_PX // Matrix pixels, independent of screen density and font scale.
        typeface = loadTypeface(context)
        textLocales = LocaleList(Locale.JAPANESE, Locale.ENGLISH)
    }
    private val characterBreaks = BreakIterator.getCharacterInstance(Locale.ROOT)
    private val bounds = Rect()
    private var cachedText: String? = null
    internal val preparedText: String? get() = cachedText
    internal var cachedBitmap: Bitmap? = null
        private set
    private var stripPixels = IntArray(0)
    private val frame = IntArray(MATRIX_LENGTH * MATRIX_LENGTH)
    private var cachedX: Int? = null

    override fun prepare(title: String, artist: String?, matrixLength: Int): Int {
        require(matrixLength == MATRIX_LENGTH)
        val display = if (artist.isNullOrBlank()) title else "$title - $artist"
        var text = display.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
            .replace('\u2028', ' ').replace('\u2029', ' ').trim()
        val count = text.codePointCount(0, text.length)
        if (count > MAX_INPUT_CODE_POINTS) {
            text = text.substring(0, boundaryAtOrBefore(text, text.offsetByCodePoints(0, MAX_INPUT_CODE_POINTS))) + "…"
        }
        if (text.isEmpty()) {
            clear()
            return 0
        }
        val maxInkWidth = MatrixTrackTitleAnimation.maxTextWidth(MATRIX_LENGTH, pixelsPerSecond) - TRAILING_SPACE_PX
        if (measureInkWidth(text) > maxInkWidth) {
            val ellipsis = "…"
            val end = boundaryAtOrBefore(text,
                paint.breakText(text, true, maxInkWidth - paint.measureText(ellipsis), null))
            text = text.substring(0, end).trimEnd() + ellipsis
            // Include glyph overhangs as well as measureText's advance width.
            while (measureInkWidth(text) > maxInkWidth && text.length > 1) {
                val source = text.removeSuffix(ellipsis)
                text = source.substring(0, boundaryAtOrBefore(source, source.length - 1)) + ellipsis
            }
        }
        if (cachedText == text) return requireNotNull(cachedBitmap).width

        clear()
        val inkWidth = measureInkWidth(text)
        val bitmap = Bitmap.createBitmap(inkWidth + TRAILING_SPACE_PX, MATRIX_LENGTH, Bitmap.Config.ARGB_8888)
        try {
            val metrics = paint.fontMetrics
            val preferredBaseline = (MATRIX_LENGTH - metrics.ascent - metrics.descent) / 2f
            // NType metrics alone need not include the Japanese fallback glyphs' ink.
            val minBaseline = -bounds.top.toFloat()
            val maxBaseline = MATRIX_LENGTH - bounds.bottom.toFloat()
            val baseline = if (minBaseline <= maxBaseline) {
                preferredBaseline.coerceIn(minBaseline, maxBaseline)
            } else (MATRIX_LENGTH - bounds.top - bounds.bottom) / 2f
            Canvas(bitmap).apply {
                drawColor(Color.BLACK)
                drawText(text, -min(0, bounds.left).toFloat(), baseline, paint)
            }
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            for (index in pixels.indices) pixels[index] = quantizePixel(pixels[index])
            stripPixels = pixels
            cachedBitmap = bitmap
            cachedText = text
            return bitmap.width
        } catch (error: Exception) {
            bitmap.recycle()
            throw error
        }
    }

    private fun boundaryAtOrBefore(text: String, index: Int): Int {
        characterBreaks.setText(text)
        return if (characterBreaks.isBoundary(index)) index
        else characterBreaks.preceding(index).coerceAtLeast(0)
    }

    private fun measureInkWidth(text: String): Int {
        paint.getTextBounds(text, 0, text.length, bounds)
        return (ceil(max(paint.measureText(text), bounds.right.toFloat())).toInt() - min(0, bounds.left))
            .coerceAtLeast(1)
    }

    override fun render(x: Int): IntArray {
        if (cachedX == x) return frame
        frame.fill(0)
        val stripWidth = cachedBitmap?.width ?: return frame
        val destinationLeft = max(0L, x.toLong()).coerceAtMost(MATRIX_LENGTH.toLong()).toInt()
        val sourceLeft = max(0L, -x.toLong()).coerceAtMost(stripWidth.toLong()).toInt()
        val visibleWidth = min(MATRIX_LENGTH - destinationLeft, stripWidth - sourceLeft)
        if (visibleWidth > 0) {
            for (y in 0 until MATRIX_LENGTH) {
                stripPixels.copyInto(frame, y * MATRIX_LENGTH + destinationLeft,
                    y * stripWidth + sourceLeft, y * stripWidth + sourceLeft + visibleWidth)
            }
        }
        cachedX = x
        return frame
    }

    override fun clear() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedText = null
        cachedX = null
        stripPixels = IntArray(0)
        frame.fill(0)
    }
}
