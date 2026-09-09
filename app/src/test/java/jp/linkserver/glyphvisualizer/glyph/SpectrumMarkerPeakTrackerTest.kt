package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Test

class SpectrumMarkerPeakTrackerTest {
    private val tracker = SpectrumMarkerPeakTracker()

    @Test
    fun nearbyPeakMovesImmediately() {
        assertEquals(5, tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L))

        assertEquals(
            6,
            tracker.selectPeakIndex(bands(5 to 0.95f, 6 to 1f), rawPeak = 0.2f, nowMs = 1_033L)
        )
    }

    @Test
    fun briefDistantPeakDoesNotSwitch() {
        tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L)

        assertEquals(
            5,
            tracker.selectPeakIndex(bands(5 to 0.8f, 20 to 1f), rawPeak = 0.2f, nowMs = 1_033L)
        )
        assertEquals(
            5,
            tracker.selectPeakIndex(bands(5 to 1f, 20 to 0.7f), rawPeak = 0.2f, nowMs = 1_066L)
        )
    }

    @Test
    fun distantPeakMustBeClearlyStronger() {
        tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L)
        val slightlyStrongerDistantPeak = bands(5 to 0.9f, 20 to 1f)

        assertEquals(
            5,
            tracker.selectPeakIndex(
                slightlyStrongerDistantPeak,
                rawPeak = 0.2f,
                nowMs = 1_033L
            )
        )
        assertEquals(
            5,
            tracker.selectPeakIndex(
                slightlyStrongerDistantPeak,
                rawPeak = 0.2f,
                nowMs = 1_500L
            )
        )
    }

    @Test
    fun eligibleDistantPeakWaitsForFullHold() {
        tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L)
        val distantPeak = bands(5 to 0.8f, 20 to 1f)

        assertEquals(5, tracker.selectPeakIndex(distantPeak, rawPeak = 0.2f, nowMs = 1_033L))
        assertEquals(5, tracker.selectPeakIndex(distantPeak, rawPeak = 0.2f, nowMs = 1_098L))
        assertEquals(20, tracker.selectPeakIndex(distantPeak, rawPeak = 0.2f, nowMs = 1_099L))
    }

    @Test
    fun veryStrongDistantPeakSwitchesImmediately() {
        tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L)

        assertEquals(
            20,
            tracker.selectPeakIndex(bands(5 to 0.6f, 20 to 1f), rawPeak = 0.2f, nowMs = 1_033L)
        )
    }

    @Test
    fun lowRawPeakSuppressesImmediateDistantSwitch() {
        tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L)
        val distantNoisePeak = bands(5 to 0.1f, 20 to 1f)

        assertEquals(5, tracker.selectPeakIndex(distantNoisePeak, rawPeak = 0.01f, nowMs = 1_033L))
        assertEquals(5, tracker.selectPeakIndex(distantNoisePeak, rawPeak = 0.01f, nowMs = 1_500L))
    }

    @Test
    fun resetClearsTrackedAndPendingPeaks() {
        tracker.selectPeakIndex(bands(5 to 1f), rawPeak = 0.2f, nowMs = 1_000L)
        tracker.selectPeakIndex(bands(5 to 0.8f, 20 to 1f), rawPeak = 0.2f, nowMs = 1_033L)

        tracker.reset()

        assertEquals(20, tracker.selectPeakIndex(bands(20 to 1f), rawPeak = 0.2f, nowMs = 1_050L))
    }

    private fun bands(vararg peaks: Pair<Int, Float>): FloatArray {
        return FloatArray(25).also { values ->
            peaks.forEach { (index, value) -> values[index] = value }
        }
    }
}
