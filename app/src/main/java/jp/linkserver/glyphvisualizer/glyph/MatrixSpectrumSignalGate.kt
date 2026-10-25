package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.sqrt

/** Uses absolute, pre-AGC signal values, never the per-frame normalized spectrum shape. */
internal class MatrixSpectrumSignalGate {
    companion object {
        const val RELEASE_MS = 120L
        const val CLOSE_RMS = 0.00004f
        const val OPEN_RMS = 0.00006f
        const val CLOSE_PEAK = 0.003f
        const val OPEN_PEAK = 0.0045f
    }
    var quiet = true
        private set
    var holdGainIncrease = true
        private set
    private var quietSince: Long? = null
    private var lastNow: Long? = null

    fun update(rawPeak: Float, waveform: FloatArray?, nowMs: Long) {
        if (lastNow?.let { nowMs < it } == true) reset()
        lastNow = nowMs
        val rms = waveform?.takeIf { it.isNotEmpty() }?.let { samples ->
            var squares = 0.0
            for (sample in samples) squares += sample.toDouble() * sample
            sqrt(squares / samples.size).toFloat()
        }
        val signal = rawPeak > (if (quiet) OPEN_PEAK else CLOSE_PEAK) ||
            (rms != null && rms > (if (quiet) OPEN_RMS else CLOSE_RMS))
        holdGainIncrease = !signal
        if (signal) {
            quiet = false
            quietSince = null
        } else {
            val since = quietSince ?: nowMs.also { quietSince = it }
            if (nowMs - since >= RELEASE_MS) quiet = true
        }
    }

    fun reset() {
        quiet = true
        holdGainIncrease = true
        quietSince = null
        lastNow = null
    }
}
