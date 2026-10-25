package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import jp.linkserver.glyphvisualizer.AppLogger

/** Inspector-owned text layer. No SDK, frame-store writes, or animation scheduler. */
internal class GlyphInspectorTrackTitlePreview(
    private val context: Context,
    private val profile: GlyphDeviceProfile,
) : AutoCloseable {
    private val length = when (profile) {
        GlyphDeviceProfile.PHONE3_MATRIX -> 25
        GlyphDeviceProfile.PHONE4A_PRO_MATRIX -> 13
        else -> error("Track title preview requires a Matrix profile")
    }
    private val heights = GlyphMatrixPreviewGeometry.columnHeights(profile, length)
    private val animation = MatrixTrackTitleAnimation()
    private var renderer: MatrixTrackTitleRenderer? = null
    private val composite = IntArray(length * length)
    private var consumedRequest = 0
    private var prepared = false

    fun updateManual(enabled: Boolean, request: Int, title: String, artist: String?, nowMs: Long) {
        if (!enabled) {
            reset()
            consumedRequest = request
        } else if (request != consumedRequest) {
            consumedRequest = request
            start(title, artist, nowMs)
        }
    }

    private fun start(title: String, artist: String?, nowMs: Long) {
        if (title.isBlank()) {
            cancel()
            return
        }
        try {
            val text = renderer ?: MatrixTrackTitleRendererFactory.create(context, profile, profile, length)
                .also { renderer = it }
            val width = text.prepare(title, artist, length)
            prepared = width > 0
            if (width == 0) cancel()
            else animation.start(nowMs, length, width, text.pixelsPerSecond,
                text.leadingMargin, text.trailingMargin, text.rightToLeft)
        } catch (error: Exception) {
            AppLogger.w("GlyphInspector", "Track title preview preparation failed", error)
            cancel()
        }
    }

    fun compose(background: GlyphPreviewFrame.Matrix, nowMs: Long): GlyphPreviewFrame.Matrix {
        require(background.matrixSize == length && background.deviceProfile == profile)
        animation.advance(nowMs)
        if (!animation.active) {
            clearText()
            return background
        }
        return try {
            val text = animation.textX?.let { requireNotNull(renderer).render(it) }
            MatrixTrackTitleCompositor.compose(background.pixels, text, animation.backgroundBrightness,
                length, heights, composite, preserveTextBrightness = true)
            // Output frames own their pixels, including those retained by Freeze.
            background.copy(timestampMs = nowMs, pixels = composite.copyOf())
        } catch (error: Exception) {
            AppLogger.w("GlyphInspector", "Track title preview composition failed", error)
            cancel()
            background
        }
    }

    private fun cancel() {
        animation.cancel()
        clearText()
    }

    private fun clearText() {
        if (prepared) renderer?.clear()
        prepared = false
    }

    private fun reset() {
        cancel()
    }

    override fun close() {
        reset()
        renderer = null
        consumedRequest = 0
    }
}
