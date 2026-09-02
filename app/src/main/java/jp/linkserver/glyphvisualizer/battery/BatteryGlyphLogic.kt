package jp.linkserver.glyphvisualizer.battery

import jp.linkserver.glyphvisualizer.GlyphBatteryIndicatorSpec
import kotlin.math.abs
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

internal class BatteryFaceDownDetector(
    private val movingAverageWeight: Float = 0.5f,
    private val faceDownEnterZThreshold: Float = -9.5f,
    private val faceDownExitZThreshold: Float = -8.5f,
    private val movementThreshold: Float = 0.2f,
    private val requiredDurationMs: Long = 1_000L
) {
    private var currentXyAcceleration = 0f
    private var currentZAcceleration = 0f
    private var previousXyAcceleration = 0f
    private var lastMovementAtMs = Long.MIN_VALUE
    private var zAccelerationIsFaceDown = false
    private var zAccelerationFaceDownSinceMs = Long.MIN_VALUE

    var isFaceDown: Boolean = false
        private set

    fun onSample(
        xAcceleration: Float,
        yAcceleration: Float,
        zAcceleration: Float,
        timestampMs: Long
    ): Boolean {
        currentXyAcceleration = updateMovingAverage(
            currentXyAcceleration,
            xAcceleration * xAcceleration + yAcceleration * yAcceleration
        )
        currentZAcceleration = updateMovingAverage(currentZAcceleration, zAcceleration)

        if (abs(currentXyAcceleration - previousXyAcceleration) > movementThreshold) {
            previousXyAcceleration = currentXyAcceleration
            lastMovementAtMs = timestampMs
        }
        val moving = lastMovementAtMs != Long.MIN_VALUE &&
            timestampMs - lastMovementAtMs < requiredDurationMs

        val zThreshold = if (isFaceDown) {
            faceDownExitZThreshold
        } else {
            faceDownEnterZThreshold
        }
        val isCurrentlyFaceDown = currentZAcceleration < zThreshold
        val isFaceDownForRequiredDuration = isCurrentlyFaceDown &&
            zAccelerationIsFaceDown &&
            timestampMs - zAccelerationFaceDownSinceMs >= requiredDurationMs

        if (isCurrentlyFaceDown && !zAccelerationIsFaceDown) {
            zAccelerationIsFaceDown = true
            zAccelerationFaceDownSinceMs = timestampMs
        } else if (!isCurrentlyFaceDown) {
            zAccelerationIsFaceDown = false
            zAccelerationFaceDownSinceMs = Long.MIN_VALUE
        }

        if (!moving && isFaceDownForRequiredDuration && !isFaceDown) {
            isFaceDown = true
        } else if (!isFaceDownForRequiredDuration && isFaceDown) {
            isFaceDown = false
        }
        return isFaceDown
    }

    fun reset() {
        currentXyAcceleration = 0f
        currentZAcceleration = 0f
        previousXyAcceleration = 0f
        lastMovementAtMs = Long.MIN_VALUE
        zAccelerationIsFaceDown = false
        zAccelerationFaceDownSinceMs = Long.MIN_VALUE
        isFaceDown = false
    }

    private fun updateMovingAverage(currentAverage: Float, newValue: Float): Float {
        return newValue + movingAverageWeight * (currentAverage - newValue)
    }
}

internal class BatteryShakeDetector(
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
        faceDown: Boolean,
        linearAccelerationMagnitude: Float,
        timestampMs: Long
    ): Boolean {
        if (!charging || !faceDown) {
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
