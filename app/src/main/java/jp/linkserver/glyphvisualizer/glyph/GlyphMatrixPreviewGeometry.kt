package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.roundToInt

internal object GlyphMatrixPreviewGeometry {
    fun columnHeights(profile: GlyphDeviceProfile, length: Int): IntArray {
        if (length <= 0) return IntArray(0)
        val reference = when (profile) {
            GlyphDeviceProfile.PHONE3_MATRIX -> intArrayOf(
                7, 11, 15, 17, 19, 21, 21, 23, 23, 25, 25, 25, 25,
                25, 25, 25, 23, 23, 21, 21, 19, 17, 15, 11, 7
            )
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX ->
                GlyphMatrixProfileEmulator.phone4aProColumnHeights()
            else -> IntArray(length) { length }
        }
        if (length == reference.size) return reference

        return IntArray(length) { index ->
            val source = if (length <= 1) {
                reference.lastIndex / 2f
            } else {
                index * (reference.lastIndex.toFloat() / (length - 1f))
            }
            val sourceIndex = source.roundToInt().coerceIn(0, reference.lastIndex)
            val scaled = (reference[sourceIndex] * (length / reference.size.toFloat()))
                .roundToInt()
                .coerceAtLeast(1)
            if (scaled % 2 == 0) (scaled - 1).coerceAtLeast(1) else scaled
        }
    }
}
