package jp.linkserver.glyphvisualizer.glyph

/** The single 8-bit -> SDK 12-bit boundary. Internal and preview frames stay 8-bit. */
internal object MatrixFrameBrightness {
    const val SDK_MAX_BRIGHTNESS = 4095

    fun toSdk(value: Int): Int {
        require(value in 0..255) { "Expected an 8-bit Matrix brightness" }
        return (value * SDK_MAX_BRIGHTNESS + 127) / 255
    }

    fun copyToSdk(
        frame8Bit: IntArray,
        destination: IntArray,
        matrixLength: Int,
        physicalColumnHeights: IntArray,
    ) {
        require(frame8Bit !== destination) { "SDK conversion must not modify the internal frame" }
        require(frame8Bit.size == matrixLength * matrixLength && destination.size == frame8Bit.size)
        require(physicalColumnHeights.size == matrixLength)
        for (y in 0 until matrixLength) for (x in 0 until matrixLength) {
            val index = y * matrixLength + x
            val top = (matrixLength - physicalColumnHeights[x]) / 2
            destination[index] = if (y >= top && y < top + physicalColumnHeights[x]) {
                toSdk(frame8Bit[index])
            } else 0
        }
    }
}
