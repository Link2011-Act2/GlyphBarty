package jp.linkserver.glyphvisualizer.battery

import jp.linkserver.glyphvisualizer.GlyphBatteryIndicatorSpec
import kotlin.math.ceil

internal object BatteryGlyphLogic {
    const val FULL_BAR_ANIMATION_MS = 250L
    const val HOLD_DURATION_MS = 3_000L
    const val FRAME_INTERVAL_MS = 20L
    const val MAX_BRIGHTNESS = 4_095

    fun litChannelCount(batteryPercent: Int, channelCount: Int): Int {
        if (batteryPercent <= 0 || channelCount <= 0) return 0
        val clampedPercent = batteryPercent.coerceAtMost(100)
        return ceil(clampedPercent * channelCount / 100.0).toInt().coerceAtMost(channelCount)
    }

    fun animationDurationMs(batteryPercent: Int): Long {
        return (FULL_BAR_ANIMATION_MS * batteryPercent.coerceIn(0, 100) / 100L)
    }

    fun frameForLitCount(
        spec: GlyphBatteryIndicatorSpec,
        litChannelCount: Int
    ): IntArray {
        val frame = IntArray(spec.frameChannelCount)
        spec.orderedChannels
            .take(litChannelCount.coerceIn(0, spec.orderedChannels.size))
            .forEach { channel -> frame[channel] = MAX_BRIGHTNESS }
        return frame
    }
}

internal class BatteryShakeDetector(
    private val faceDownZThreshold: Float = -8.0f,
    private val shakeAccelerationThreshold: Float = 12.0f,
    private val requiredPeaks: Int = 2,
    private val peakWindowMs: Long = 500L,
    private val minimumPeakSpacingMs: Long = 60L,
    private val cooldownMs: Long = 1_500L
) {
    private var firstPeakAtMs = Long.MIN_VALUE
    private var lastPeakAtMs = Long.MIN_VALUE
    private var peakCount = 0
    private var lastTriggerAtMs = Long.MIN_VALUE

    fun onSample(
        charging: Boolean,
        gravityZ: Float,
        linearAccelerationMagnitude: Float,
        timestampMs: Long
    ): Boolean {
        if (!charging || gravityZ > faceDownZThreshold) {
            resetPeaks()
            return false
        }
        if (lastTriggerAtMs != Long.MIN_VALUE && timestampMs - lastTriggerAtMs < cooldownMs) {
            return false
        }
        if (linearAccelerationMagnitude < shakeAccelerationThreshold) return false
        if (lastPeakAtMs != Long.MIN_VALUE && timestampMs - lastPeakAtMs < minimumPeakSpacingMs) {
            return false
        }
        if (firstPeakAtMs == Long.MIN_VALUE || timestampMs - firstPeakAtMs > peakWindowMs) {
            firstPeakAtMs = timestampMs
            peakCount = 1
        } else {
            peakCount += 1
        }
        lastPeakAtMs = timestampMs
        if (peakCount < requiredPeaks) return false

        lastTriggerAtMs = timestampMs
        resetPeaks()
        return true
    }

    fun reset() {
        resetPeaks()
        lastTriggerAtMs = Long.MIN_VALUE
    }

    private fun resetPeaks() {
        firstPeakAtMs = Long.MIN_VALUE
        lastPeakAtMs = Long.MIN_VALUE
        peakCount = 0
    }
}
