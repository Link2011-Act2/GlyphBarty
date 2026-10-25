package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Elapsed-time animation; no scheduler or second Matrix session. */
internal class MatrixTrackTitleAnimation {
    companion object {
        const val DIM_MS = 200L
        const val RESTORE_MS = 300L
        const val PIXELS_PER_SECOND = 12
        const val MAX_SCROLL_MS = 20_000L
        const val BACKGROUND_LEVEL = 0.2f
        fun maxTextWidth(matrixLength: Int, pixelsPerSecond: Int = PIXELS_PER_SECOND): Int =
            (MAX_SCROLL_MS * pixelsPerSecond / 1000).toInt() - matrixLength
    }

    private var startedAtMs: Long? = null
    private var length = 0
    private var width = 0
    private var scrollPixelsPerSecond = PIXELS_PER_SECOND
    private var initialBrightness = 1f
    var backgroundBrightness = 1f
        private set
    var textX: Int? = null
        private set
    val active: Boolean get() = startedAtMs != null
    val scrollDurationMs: Long get() =
        ((length + width) * 1000L + scrollPixelsPerSecond - 1) / scrollPixelsPerSecond

    fun start(nowMs: Long, matrixLength: Int, textWidth: Int, pixelsPerSecond: Int = PIXELS_PER_SECOND) {
        require(pixelsPerSecond > 0)
        initialBrightness = backgroundBrightness
        startedAtMs = nowMs
        length = matrixLength
        scrollPixelsPerSecond = pixelsPerSecond
        width = textWidth.coerceIn(1, maxTextWidth(matrixLength, pixelsPerSecond))
        advance(nowMs)
    }

    fun cancel() {
        startedAtMs = null
        backgroundBrightness = 1f
        textX = null
    }

    fun advance(nowMs: Long) {
        val start = startedAtMs ?: return
        val elapsed = (nowMs - start).coerceAtLeast(0)
        textX = null
        when {
            elapsed < DIM_MS -> backgroundBrightness =
                initialBrightness + (BACKGROUND_LEVEL - initialBrightness) * ease(elapsed, DIM_MS)
            elapsed < DIM_MS + scrollDurationMs -> {
                backgroundBrightness = BACKGROUND_LEVEL
                textX = length - floor((elapsed - DIM_MS) * scrollPixelsPerSecond / 1000.0).toInt()
            }
            elapsed < DIM_MS + scrollDurationMs + RESTORE_MS -> backgroundBrightness =
                BACKGROUND_LEVEL + (1f - BACKGROUND_LEVEL) *
                    ease(elapsed - DIM_MS - scrollDurationMs, RESTORE_MS)
            else -> cancel()
        }
    }

    private fun ease(elapsed: Long, duration: Long): Float {
        val t = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}

internal object MatrixTrackTitleCompositor {
    fun compose(
        background: IntArray,
        text: IntArray?,
        brightness: Float,
        length: Int,
        columnHeights: IntArray,
        destination: IntArray,
        preserveTextBrightness: Boolean = false,
    ) {
        for (y in 0 until length) for (x in 0 until length) {
            val index = y * length + x
            val top = (length - columnHeights[x]) / 2
            val dimmed = (background[index] * brightness).roundToInt().coerceIn(0, 255)
            val textLevel = text?.get(index)?.coerceIn(0, 255) ?: 0
            val physicalLed = y >= top && y < top + columnHeights[x]
            destination[index] = when {
                !physicalLed -> 0
                preserveTextBrightness -> max(dimmed, textLevel)
                textLevel > 0 -> 255 // Preserve the legacy SDK renderer on Phone (4a) Pro.
                else -> dimmed
            }
        }
    }
}
