package jp.linkserver.glyphvisualizer.glyph

internal object MatrixFrameOptimizer {
    fun supportsDiffRendering(renderMode: GlyphPatternRenderMode): Boolean {
        return when (renderMode) {
            GlyphPatternRenderMode.MATRIX_BAR,
            GlyphPatternRenderMode.MATRIX_FIELD,
            GlyphPatternRenderMode.MATRIX_CIRCLE,
            GlyphPatternRenderMode.MATRIX_SPECTRUM,
            GlyphPatternRenderMode.MATRIX_SPECTRUM_CENTER,
            GlyphPatternRenderMode.MATRIX_SPECTRUM_BOTTOM,
            GlyphPatternRenderMode.ALL_BRIGHTNESS -> true
            else -> false
        }
    }

    fun shouldReuseFrame(renderSignature: Long?, lastRenderSignature: Long): Boolean {
        return renderSignature != null && renderSignature == lastRenderSignature
    }
}

internal object MatrixBarPatternRenderer {
    fun render(
        frame: IntArray,
        matrixLength: Int,
        centerX: Int,
        barWidth: Int,
        fullRows: Int,
        edgeRowBrightness: Int,
        reverseDirection: Boolean,
        colorOn: Int
    ) {
        val startX = (centerX - barWidth / 2).coerceAtLeast(0)
        val endXExclusive = (startX + barWidth).coerceAtMost(matrixLength)
        for (row in 0 until matrixLength) {
            val brightness = SignalScalingPipeline.brightnessForSlot(
                index = row,
                fullSlots = fullRows,
                slotCount = matrixLength,
                edgeBrightness = edgeRowBrightness,
                fullBrightness = colorOn
            )
            if (brightness <= 0) continue
            val y = if (reverseDirection) row else (matrixLength - 1 - row)
            val rowOffset = y * matrixLength
            for (x in startX until endXExclusive) {
                frame[rowOffset + x] = brightness
            }
        }
    }
}

internal object MatrixFieldPatternRenderer {
    fun render(
        frame: IntArray,
        matrixLength: Int,
        fullRows: Int,
        edgeRowBrightness: Int,
        reverseDirection: Boolean,
        colorOn: Int
    ) {
        for (row in 0 until matrixLength) {
            val brightness = SignalScalingPipeline.brightnessForSlot(
                index = row,
                fullSlots = fullRows,
                slotCount = matrixLength,
                edgeBrightness = edgeRowBrightness,
                fullBrightness = colorOn
            )
            val y = if (reverseDirection) row else (matrixLength - 1 - row)
            val rowOffset = y * matrixLength
            for (x in 0 until matrixLength) {
                frame[rowOffset + x] = brightness
            }
        }
    }
}

internal object MatrixAllBrightnessPatternRenderer {
    fun render(frame: IntArray, brightness: Int) {
        frame.fill(brightness)
    }
}
