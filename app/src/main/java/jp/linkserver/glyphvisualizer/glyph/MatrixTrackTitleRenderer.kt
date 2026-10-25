package jp.linkserver.glyphvisualizer.glyph

import android.content.Context

/** Renderers return a reusable logical frame in the existing preview's 0..255 scale. */
internal interface MatrixTrackTitleRenderer {
    val pixelsPerSecond: Int get() = MatrixTrackTitleAnimation.PIXELS_PER_SECOND
    val preservesTextBrightness: Boolean get() = false
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
    ): MatrixTrackTitleRenderer = if (
        physicalProfile == GlyphDeviceProfile.PHONE3_MATRIX &&
        renderingProfile == GlyphDeviceProfile.PHONE3_MATRIX &&
        matrixLength == Phone3TrackTitleRenderer.MATRIX_LENGTH
    ) {
        Phone3TrackTitleRenderer(context)
    } else {
        // Keep the SDK renderer for Phone (4a) Pro and its existing 13x13 emulation.
        MatrixSdkTrackTitleRenderer(context)
    }
}
