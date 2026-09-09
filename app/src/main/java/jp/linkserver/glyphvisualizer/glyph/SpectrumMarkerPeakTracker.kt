package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.abs

internal class SpectrumMarkerPeakTracker(
    private val nearbyBandDistance: Int = DEFAULT_NEARBY_BAND_DISTANCE,
    private val switchStrengthRatio: Float = SPECTRUM_MARKER_SWITCH_RATIO,
    private val immediateSwitchStrengthRatio: Float = SPECTRUM_MARKER_IMMEDIATE_SWITCH_RATIO,
    private val switchHoldMs: Long = SPECTRUM_MARKER_SWITCH_HOLD_MS,
    private val minimumRawPeakForDistantSwitch: Float = DEFAULT_MINIMUM_RAW_PEAK_FOR_DISTANT_SWITCH
) {
    private var trackedPeakIndex = NO_PEAK
    private var pendingPeakIndex = NO_PEAK
    private var pendingPeakSinceMs = NO_PENDING_TIME

    fun selectPeakIndex(bands: FloatArray, rawPeak: Float, nowMs: Long): Int {
        if (bands.isEmpty()) {
            reset()
            return NO_PEAK
        }

        val strongestIndex = strongestIndexInRange(bands, 0, bands.lastIndex)
        if (trackedPeakIndex !in bands.indices) {
            if (rawPeak < minimumRawPeakForDistantSwitch) return NO_PEAK
            trackedPeakIndex = strongestIndex
            clearPendingPeak()
            return trackedPeakIndex
        }

        val localFirst = (trackedPeakIndex - nearbyBandDistance).coerceAtLeast(0)
        val localLast = (trackedPeakIndex + nearbyBandDistance).coerceAtMost(bands.lastIndex)
        val localPeakIndex = strongestIndexInRange(
            bands = bands,
            first = localFirst,
            last = localLast,
            preferredIndex = trackedPeakIndex
        )

        if (strongestIndex in localFirst..localLast) {
            trackedPeakIndex = strongestIndex
            clearPendingPeak()
            return trackedPeakIndex
        }

        // Follow gradual movement inside the current region while a distant peak is evaluated.
        trackedPeakIndex = localPeakIndex
        val currentStrength = bands[localPeakIndex].coerceAtLeast(0f)
        val candidateStrength = bands[strongestIndex].coerceAtLeast(0f)
        if (rawPeak < minimumRawPeakForDistantSwitch) {
            clearPendingPeak()
            return trackedPeakIndex
        }

        if (candidateStrength >= currentStrength * immediateSwitchStrengthRatio) {
            trackedPeakIndex = strongestIndex
            clearPendingPeak()
            return trackedPeakIndex
        }

        if (candidateStrength < currentStrength * switchStrengthRatio) {
            clearPendingPeak()
            return trackedPeakIndex
        }

        if (
            pendingPeakIndex == NO_PEAK ||
                abs(strongestIndex - pendingPeakIndex) > nearbyBandDistance
        ) {
            pendingPeakIndex = strongestIndex
            pendingPeakSinceMs = nowMs
            return trackedPeakIndex
        }

        pendingPeakIndex = strongestIndex
        if (nowMs - pendingPeakSinceMs < switchHoldMs) return trackedPeakIndex

        trackedPeakIndex = strongestIndex
        clearPendingPeak()
        return trackedPeakIndex
    }

    fun reset() {
        trackedPeakIndex = NO_PEAK
        clearPendingPeak()
    }

    private fun clearPendingPeak() {
        pendingPeakIndex = NO_PEAK
        pendingPeakSinceMs = NO_PENDING_TIME
    }

    private fun strongestIndexInRange(
        bands: FloatArray,
        first: Int,
        last: Int,
        preferredIndex: Int = first
    ): Int {
        var strongestIndex = preferredIndex
        var strongestValue = bands[preferredIndex]
        for (index in first..last) {
            if (bands[index] > strongestValue) {
                strongestIndex = index
                strongestValue = bands[index]
            }
        }
        return strongestIndex
    }

    private companion object {
        const val NO_PEAK = -1
        const val NO_PENDING_TIME = -1L
        const val DEFAULT_NEARBY_BAND_DISTANCE = 2
        const val SPECTRUM_MARKER_SWITCH_RATIO = 1.15f
        const val SPECTRUM_MARKER_IMMEDIATE_SWITCH_RATIO = 1.45f
        const val SPECTRUM_MARKER_SWITCH_HOLD_MS = 66L
        const val DEFAULT_MINIMUM_RAW_PEAK_FOR_DISTANT_SWITCH = 0.02f
    }
}
