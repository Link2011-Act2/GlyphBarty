package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.sin

/** Test signal source for the Inspector's SDK-free production-renderer mode. */
internal object GlyphInspectorPreviewRenderer {
    fun spectrum(timestampMs: Long, level: Float): FloatArray {
        if (level <= 0f) return FloatArray(32)
        val phase = (timestampMs % 6_000L) / 6_000f * (Math.PI * 2.0).toFloat()
        return FloatArray(32) { index ->
            val ratio = index / 31f
            val bass = (1f - ratio) * 0.62f
            val movingPeak = ((sin(ratio * 10f - phase) + 1f) * 0.19f)
            (0.18f + bass + movingPeak).coerceIn(0f, 1f)
        }
    }

    fun waveform(
        timestampMs: Long,
        level: Float,
        phaseOffset: Float
    ): FloatArray {
        if (level <= 0f) return FloatArray(256)
        val phase = (timestampMs % 2_000L) / 2_000f * (Math.PI * 2.0).toFloat()
        return FloatArray(256) { index ->
            val x = index / 255f
            val fundamental = sin(x * 8f * Math.PI.toFloat() + phase + phaseOffset)
            val overtone = sin(x * 19f * Math.PI.toFloat() - phase * 0.7f) * 0.28f
            ((fundamental + overtone) * level).coerceIn(-1f, 1f)
        }
    }
}
