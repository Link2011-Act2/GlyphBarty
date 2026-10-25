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
import android.text.TextUtils
import android.view.View
import jp.linkserver.glyphvisualizer.R
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.util.Locale

/** Independent Canvas implementation, owned by the same thread as the Matrix controller. */
internal class MatrixCanvasTrackTitleRenderer(context: Context, val matrixLength: Int = 25) : MatrixTrackTitleRenderer {
    companion object {
        const val FONT_SIZE_PX = 24f
        const val PRO_FONT_SIZE_PX = 12.5f
        const val SCROLL_PIXELS_PER_SECOND = 40
        const val PRO_SCROLL_PIXELS_PER_SECOND = 21
        const val TRAILING_SPACE_PX = 12
        private const val MAX_INPUT_CODE_POINTS = 512

        internal fun toGray(argb: Int): Int =
            (0.299 * Color.red(argb) + 0.587 * Color.green(argb) + 0.114 * Color.blue(argb)).toInt()

        internal fun quantizeGray(gray: Int): Int {
            val normalized = ((gray.coerceIn(0, 255) - 80) * 255 / 175).coerceIn(0, 255)
            return (Math.rint(normalized / 85.0) * 85).toInt()
        }

        internal fun quantizePixel(argb: Int): Int {
            // Toy's text layer is 0/680/1360/2040, not the SDK's full 4095 range.
            // Approximate it in the existing 8-bit pipeline: 0/42/85/127. Only the
            // shared send boundary converts back to 12-bit (0/674/1365/2039).
            val toyLevel = quantizeGray(toGray(argb)) * 8
            return (toyLevel * 255 + 2047) / MatrixFrameBrightness.SDK_MAX_BRIGHTNESS
        }

        internal fun loadTypeface(context: Context): Typeface = runCatching {
            val font = Font.Builder(context.resources, R.font.ntype82_regular).build()
            Typeface.CustomFallbackBuilder(FontFamily.Builder(font).build())
                .setSystemFallback("sans-serif")
                .build()
        }.getOrElse { Typeface.create("NType82-Regular", Typeface.NORMAL) }
    }

    override val pixelsPerSecond = if (matrixLength == 13) PRO_SCROLL_PIXELS_PER_SECOND else SCROLL_PIXELS_PER_SECOND
    override val preservesTextBrightness = true
    override val leadingMargin = -1
    override val trailingMargin = TRAILING_SPACE_PX
    override val rightToLeft = TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL
    init { require(matrixLength == 25 || matrixLength == 13) }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        isLinearText = true
        // Logical profile also covers Pro emulation; Phone (3)'s 25px path stays 24px.
        textSize = if (matrixLength == 13) PRO_FONT_SIZE_PX else FONT_SIZE_PX
        typeface = loadTypeface(context)
        textLocales = LocaleList(Locale.JAPANESE, Locale.ENGLISH)
    }
    private val characterBreaks = BreakIterator.getCharacterInstance(Locale.ROOT)
    private val textBounds = Rect()
    private var cachedText: String? = null
    internal val preparedText: String? get() = cachedText
    internal var cachedBitmap: Bitmap? = null
        private set
    private var stripPixels = IntArray(0)
    private val frame = IntArray(matrixLength * matrixLength)
    private var cachedX: Int? = null

    override fun prepare(title: String, artist: String?, matrixLength: Int): Int {
        require(matrixLength == this.matrixLength)
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
        val maxInkWidth = MatrixTrackTitleAnimation.maxTextWidth(matrixLength, pixelsPerSecond, leadingMargin) - TRAILING_SPACE_PX
        if (measureWidth(text) > maxInkWidth) {
            val ellipsis = "…"
            val end = boundaryAtOrBefore(text,
                paint.breakText(text, true, maxInkWidth - paint.measureText(ellipsis), null))
            text = text.substring(0, end).trimEnd() + ellipsis
            while (measureWidth(text) > maxInkWidth && text.length > 1) {
                val source = text.removeSuffix(ellipsis)
                text = source.substring(0, boundaryAtOrBefore(source, source.length - 1)) + ellipsis
            }
        }
        if (cachedText == text) return requireNotNull(cachedBitmap).width

        clear()
        val inkWidth = measureWidth(text)
        val bitmap = Bitmap.createBitmap(inkWidth + TRAILING_SPACE_PX, matrixLength, Bitmap.Config.ARGB_8888)
        try {
            val metrics = paint.fontMetrics
            // Recompute placement from the selected font size for the logical viewport.
            var baseline = min((matrixLength + abs(metrics.ascent)) / 2f, matrixLength - metrics.descent)
            if (matrixLength == 13) {
                // Pro's NType descenders extend beyond the metrics-based viewport.
                // Prefer one pixel upward, but keep accents/fallback glyphs inside
                // the Canvas whenever their ink bounds fit the fixed 13px height.
                paint.getTextBounds(text, 0, text.length, textBounds)
                val firstBaseline = -textBounds.top.toFloat()
                val lastBaseline = matrixLength - textBounds.bottom.toFloat()
                baseline = if (firstBaseline <= lastBaseline) {
                    (baseline - 1f).coerceIn(firstBaseline, lastBaseline)
                } else {
                    // Oversized combinations cannot fit without changing font size.
                    // Center their bounds rather than losing all overhang on one edge.
                    (firstBaseline + lastBaseline) / 2f
                }
            }
            Canvas(bitmap).apply {
                drawColor(Color.BLACK)
                drawText(text, 0f, baseline, paint)
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

    private fun measureWidth(text: String): Int = ceil(paint.measureText(text)).toInt().coerceAtLeast(1)

    override fun render(x: Int): IntArray {
        if (cachedX == x) return frame
        frame.fill(0)
        val stripWidth = cachedBitmap?.width ?: return frame
        val destinationLeft = max(0L, x.toLong()).coerceAtMost(matrixLength.toLong()).toInt()
        val sourceLeft = max(0L, -x.toLong()).coerceAtMost(stripWidth.toLong()).toInt()
        val visibleWidth = min(matrixLength - destinationLeft, stripWidth - sourceLeft)
        if (visibleWidth > 0) {
            for (y in 0 until matrixLength) {
                stripPixels.copyInto(frame, y * matrixLength + destinationLeft,
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
