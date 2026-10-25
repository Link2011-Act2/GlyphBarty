package jp.linkserver.glyphvisualizer.glyph

import android.content.Context

/** Renderers return a reusable logical frame in the existing preview's 0..255 scale. */
internal interface MatrixTrackTitleRenderer {
    val pixelsPerSecond: Int get() = MatrixTrackTitleAnimation.PIXELS_PER_SECOND
    val preservesTextBrightness: Boolean get() = false
    val leadingMargin: Int get() = 0
    val trailingMargin: Int get() = 0
    val rightToLeft: Boolean get() = false
    fun prepare(title: String, artist: String?, matrixLength: Int): Int
    fun render(x: Int): IntArray
    fun clear()
}

internal object MatrixTrackTitleRendererFactory {
    fun create(
        context: Context,
        physicalProfile: GlyphDeviceProfile,
        renderingProfile: GlyphDeviceProfile,
        matrixLength: Int,
    ): MatrixTrackTitleRenderer {
        val canvasProfile = when (renderingProfile) {
            GlyphDeviceProfile.PHONE3_MATRIX -> physicalProfile == renderingProfile && matrixLength == 25
            GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> matrixLength == 13 &&
                physicalProfile in listOf(GlyphDeviceProfile.PHONE3_MATRIX, GlyphDeviceProfile.PHONE4A_PRO_MATRIX)
            else -> false
        }
        return if (canvasProfile) MatrixCanvasTrackTitleRenderer(context, matrixLength)
        else MatrixSdkTrackTitleRenderer(context) // Retain the official SDK fallback.
    }
}
