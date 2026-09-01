package jp.linkserver.glyphvisualizer.glyph

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

data class GlyphAnalysisFrame(
    val timestampMs: Long,
    val level: Float,
    val peak: Float,
    val lowEnergy: Float,
    val highEnergy: Float,
    val leftLevel: Float,
    val rightLevel: Float,
    val spectrumBands: FloatArray,
    val spectrumRawPeak: Float,
    val phone4aBaseBandLevel: Float,
    val waveformSamples: FloatArray,
    val leftWaveformSamples: FloatArray,
    val rightWaveformSamples: FloatArray
)

/** Fans out the existing production analysis result only while Inspector is listening. */
object GlyphAnalysisFrameStore {
    private const val FRAME_INTERVAL_MS = 33L

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<(GlyphAnalysisFrame?) -> Unit>()
    private var latestFrame: GlyphAnalysisFrame? = null
    private var lastPublishedAtMs = 0L

    fun register(listener: (GlyphAnalysisFrame?) -> Unit) {
        val current = synchronized(listeners) {
            listeners.add(listener)
            if (listeners.size == 1) {
                latestFrame = null
                lastPublishedAtMs = 0L
            }
            latestFrame
        }
        dispatch(listener, current)
    }

    fun unregister(listener: (GlyphAnalysisFrame?) -> Unit) {
        synchronized(listeners) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                latestFrame = null
                lastPublishedAtMs = 0L
            }
        }
    }

    fun clear() {
        val targets = synchronized(listeners) {
            latestFrame = null
            lastPublishedAtMs = 0L
            listeners.toList()
        }
        targets.forEach { listener -> dispatch(listener, null) }
    }

    fun publish(
        level: Float,
        peak: Float,
        lowEnergy: Float,
        highEnergy: Float,
        leftLevel: Float,
        rightLevel: Float,
        spectrumBands: FloatArray,
        spectrumRawPeak: Float,
        phone4aBaseBandLevel: Float,
        waveformSamples: FloatArray,
        leftWaveformSamples: FloatArray,
        rightWaveformSamples: FloatArray
    ) {
        val now = SystemClock.elapsedRealtime()
        val dispatchTarget = synchronized(listeners) {
            if (listeners.isEmpty()) return
            if (now - lastPublishedAtMs < FRAME_INTERVAL_MS) return

            val frame = GlyphAnalysisFrame(
                timestampMs = now,
                level = level.coerceIn(0f, 1f),
                peak = peak.coerceIn(0f, 1f),
                lowEnergy = lowEnergy.coerceIn(0f, 1f),
                highEnergy = highEnergy.coerceIn(0f, 1f),
                leftLevel = leftLevel.coerceIn(0f, 1f),
                rightLevel = rightLevel.coerceIn(0f, 1f),
                spectrumBands = spectrumBands.copyOf(),
                spectrumRawPeak = spectrumRawPeak.coerceIn(0f, 1f),
                phone4aBaseBandLevel = phone4aBaseBandLevel.coerceIn(0f, 1f),
                waveformSamples = waveformSamples.copyOf(),
                leftWaveformSamples = leftWaveformSamples.copyOf(),
                rightWaveformSamples = rightWaveformSamples.copyOf()
            )
            latestFrame = frame
            lastPublishedAtMs = now
            frame to listeners.toList()
        }
        val (frame, targets) = dispatchTarget
        targets.forEach { listener -> dispatch(listener, frame) }
    }

    private fun dispatch(
        listener: (GlyphAnalysisFrame?) -> Unit,
        frame: GlyphAnalysisFrame?
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
