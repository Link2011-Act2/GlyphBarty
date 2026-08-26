package jp.linkserver.glyphvisualizer.glyph

internal object GlyphMatrixProfileEmulator {
    const val PHONE4A_PRO_MATRIX_LENGTH = 13

    private val phone4aProColumnHeights = intArrayOf(
        5, 9, 11, 11, 13, 13, 13, 13, 13, 11, 11, 9, 5
    )

    fun phone4aProColumnHeights(): IntArray = phone4aProColumnHeights.copyOf()

    fun copyPhone4aProFrameIntoCenteredRegion(
        source: IntArray,
        physicalMatrixLength: Int,
        destination: IntArray
    ) {
        require(source.size == PHONE4A_PRO_MATRIX_LENGTH * PHONE4A_PRO_MATRIX_LENGTH)
        require(physicalMatrixLength >= PHONE4A_PRO_MATRIX_LENGTH)
        require(destination.size == physicalMatrixLength * physicalMatrixLength)

        destination.fill(0)
        val offset = (physicalMatrixLength - PHONE4A_PRO_MATRIX_LENGTH) / 2
        phone4aProColumnHeights.forEachIndexed { x, columnHeight ->
            val top = (PHONE4A_PRO_MATRIX_LENGTH - columnHeight) / 2
            for (y in top until top + columnHeight) {
                val sourceIndex = y * PHONE4A_PRO_MATRIX_LENGTH + x
                val destinationIndex = (offset + y) * physicalMatrixLength + offset + x
                destination[destinationIndex] = source[sourceIndex]
            }
        }
    }
}
