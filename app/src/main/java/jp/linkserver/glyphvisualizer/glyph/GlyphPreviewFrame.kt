package jp.linkserver.glyphvisualizer.glyph

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

sealed interface GlyphPreviewFrame {
    val deviceProfile: GlyphDeviceProfile
    val glyphMode: String
    val timestampMs: Long

    data class Lights(
        override val deviceProfile: GlyphDeviceProfile,
        val physicalDeviceProfile: GlyphDeviceProfile,
        override val glyphMode: String,
        override val timestampMs: Long,
        val brightness: IntArray
    ) : GlyphPreviewFrame

    data class Matrix(
        override val deviceProfile: GlyphDeviceProfile,
        val physicalDeviceProfile: GlyphDeviceProfile,
        override val glyphMode: String,
        override val timestampMs: Long,
        val matrixSize: Int,
        val pixels: IntArray
    ) : GlyphPreviewFrame
}

/**
 * Publishes final frames for the debug inspector without adding them to the main UI state.
 * Frames are copied and dispatched only while an inspector is subscribed.
 */
object GlyphPreviewFrameStore {
    private const val PREVIEW_FRAME_INTERVAL_MS = 33L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<(GlyphPreviewFrame?) -> Unit>()
    private var latestFrame: GlyphPreviewFrame? = null
    private var lastPublishedAtMs = 0L

    fun register(listener: (GlyphPreviewFrame?) -> Unit) {
        val currentFrame = synchronized(listeners) {
            listeners.add(listener)
            if (listeners.size == 1) {
                latestFrame = null
                lastPublishedAtMs = 0L
            }
            latestFrame
        }
        dispatch(listener, currentFrame)
    }

    fun unregister(listener: (GlyphPreviewFrame?) -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                latestFrame = null
                lastPublishedAtMs = 0L
            }
        }
    }

    fun publishLights(
        deviceProfile: GlyphDeviceProfile,
        physicalDeviceProfile: GlyphDeviceProfile,
        glyphMode: String,
        brightness: IntArray,
        force: Boolean = false
    ) {
        val now = SystemClock.elapsedRealtime()
        val dispatchTarget = synchronized(listeners) {
            if (listeners.isEmpty()) return
            if (!force && now - lastPublishedAtMs < PREVIEW_FRAME_INTERVAL_MS) return

            val frame = GlyphPreviewFrame.Lights(
                deviceProfile = deviceProfile,
                physicalDeviceProfile = physicalDeviceProfile,
                glyphMode = glyphMode,
                timestampMs = now,
                brightness = brightness.copyOf()
            )
            latestFrame = frame
            lastPublishedAtMs = now
            frame to listeners.toList()
        }

        val (frame, targetListeners) = dispatchTarget
        targetListeners.forEach { listener -> dispatch(listener, frame) }
    }

    fun publishMatrix(
        deviceProfile: GlyphDeviceProfile,
        physicalDeviceProfile: GlyphDeviceProfile,
        glyphMode: String,
        matrixSize: Int,
        pixels: IntArray,
        force: Boolean = false
    ) {
        if (matrixSize <= 0 || pixels.size != matrixSize * matrixSize) return

        val now = SystemClock.elapsedRealtime()
        val dispatchTarget = synchronized(listeners) {
            if (listeners.isEmpty()) return
            if (!force && now - lastPublishedAtMs < PREVIEW_FRAME_INTERVAL_MS) return

            val frame = GlyphPreviewFrame.Matrix(
                deviceProfile = deviceProfile,
                physicalDeviceProfile = physicalDeviceProfile,
                glyphMode = glyphMode,
                timestampMs = now,
                matrixSize = matrixSize,
                pixels = pixels.copyOf()
            )
            latestFrame = frame
            lastPublishedAtMs = now
            frame to listeners.toList()
        }

        val (frame, targetListeners) = dispatchTarget
        targetListeners.forEach { listener -> dispatch(listener, frame) }
    }

    private fun dispatch(
        listener: (GlyphPreviewFrame?) -> Unit,
        frame: GlyphPreviewFrame?
    ) {
        val notify = {
            val stillRegistered = synchronized(listeners) { listener in listeners }
            if (stillRegistered) listener(frame)
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notify()
        } else {
            mainHandler.post(notify)
        }
    }
}
