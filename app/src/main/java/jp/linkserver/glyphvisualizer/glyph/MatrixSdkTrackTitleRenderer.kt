package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils

/** Text pixels come exclusively from the official SDK, including its unsupported glyph behavior. */
internal class MatrixSdkTrackTitleRenderer(private val context: Context) : MatrixTrackTitleRenderer {
    private var text = ""
    private var y = 0
    private var cachedX: Int? = null
    private var cachedFrame: IntArray? = null
    private var logicalLength = 0
    private var sdkLength = 0

    override fun clear() {
        text = ""
        cachedX = null
        cachedFrame = null
    }

    override fun prepare(title: String, artist: String?, matrixLength: Int): Int {
        clear()
        logicalLength = matrixLength
        sdkLength = Common.getDeviceMatrixLength()
        require(sdkLength >= logicalLength)
        // Bound both SDK work and total travel. Trim by code point, preserving surrogate pairs.
        val display = if (artist.isNullOrBlank()) title else "$title - $artist"
        var candidate = display.replace('\n', ' ').replace('\r', ' ').trim()
        val count = candidate.codePointCount(0, candidate.length)
        if (count > 128) candidate = candidate.substring(0, candidate.offsetByCodePoints(0, 128)) + "..."
        val maxWidth = MatrixTrackTitleAnimation.maxTextWidth(matrixLength)
        var letters = GlyphMatrixUtils.getLetterConfigs(candidate, context, null)
        var width = GlyphMatrixUtils.getLetterMaxLength(letters, false)
        while (width > maxWidth && candidate.isNotEmpty()) {
            val source = candidate.removeSuffix("...")
            if (source.isEmpty()) break
            candidate = source.substring(0, source.offsetByCodePoints(source.length, -1)) + "..."
            letters = GlyphMatrixUtils.getLetterConfigs(candidate, context, null)
            width = GlyphMatrixUtils.getLetterMaxLength(letters, false)
        }
        if (width <= 0) return 0
        text = candidate
        val height = letters.maxOfOrNull { it.height } ?: 0
        y = ((matrixLength - height) / 2).coerceAtLeast(0)
        return width
    }

    override fun render(x: Int): IntArray {
        if (cachedX == x) return requireNotNull(cachedFrame)
        val obj = GlyphMatrixObject.Builder()
            .setText(text)
            .setPosition(x, y)
            .setBrightness(255)
            .build()
        val frame = GlyphMatrixFrame.Builder().addTop(obj).build(context).render()
        require(frame.size == sdkLength * sdkLength)
        // SDK 2.0 LetterMatrix emits 0..4095 even when setBrightness(255) is used.
        // The existing Visualizer pipeline and preview use 0..255.
        for (index in frame.indices) {
            frame[index] = (frame[index].coerceIn(0, 4095) * 255 + 2047) / 4095
        }
        // The existing Phone (4a) Pro emulation uses a logical 13x13 frame on Phone (3).
        val result = if (sdkLength == logicalLength) frame else IntArray(logicalLength * logicalLength) {
            frame[(it / logicalLength) * sdkLength + it % logicalLength]
        }
        cachedX = x
        cachedFrame = result
        return result
    }
}
