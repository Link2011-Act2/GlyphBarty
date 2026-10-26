package jp.linkserver.glyphvisualizer.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualizerSignalWatchdogTest {
    private fun VisualizerSignalWatchdog.read(
        at: Long,
        waveform: Boolean = true,
        measurement: Boolean = true,
        signal: Boolean = false,
        music: Boolean = true,
        enabled: Boolean = true
    ) = observe(at, waveform, measurement, signal, music, enabled)

    @Test fun successfulSilenceIsAmbiguousEvenAfterHoursOfActivePlayback() {
        val watchdog = VisualizerSignalWatchdog(0)
        assertEquals(VisualizerCaptureHealth.WAITING, watchdog.read(1_799))
        assertEquals(VisualizerCaptureHealth.NO_SIGNAL, watchdog.read(1_800))
        assertEquals(VisualizerCaptureHealth.NO_SIGNAL, watchdog.read(3_600_000))
        assertEquals(VisualizerCaptureHealth.SIGNAL, watchdog.read(3_600_016, signal = true))
    }

    @Test fun switchingSilentVideosNeverBecomesAnApiError() {
        val watchdog = VisualizerSignalWatchdog(0)
        repeat(10_000) { index ->
            val music = index % 3 != 0
            val at = 2_000L + index * 16L
            assertEquals(
                if (music) VisualizerCaptureHealth.NO_SIGNAL else VisualizerCaptureHealth.WAITING,
                watchdog.read(at, music = music)
            )
        }
    }

    @Test fun signalReturningAfterSilenceResetsRunningStallThreshold() {
        val watchdog = VisualizerSignalWatchdog(0)
        assertEquals(VisualizerCaptureHealth.NO_SIGNAL, watchdog.read(2_000))
        assertEquals(VisualizerCaptureHealth.SIGNAL, watchdog.read(3_000, signal = true))
        assertEquals(VisualizerCaptureHealth.WAITING, watchdog.read(7_999))
        assertEquals(VisualizerCaptureHealth.NO_SIGNAL, watchdog.read(8_000))
    }

    @Test fun eitherApiFailingContinuouslyIsAnErrorEvenWhenTheOtherHasSignal() {
        for (waveform in listOf(false, true)) {
            val watchdog = VisualizerSignalWatchdog(0)
            assertEquals(VisualizerCaptureHealth.API_PENDING, watchdog.read(0, waveform, !waveform, signal = true))
            assertEquals(VisualizerCaptureHealth.API_PENDING, watchdog.read(1_999, waveform, !waveform, signal = true))
            assertEquals(VisualizerCaptureHealth.API_ERROR, watchdog.read(2_000, waveform, !waveform, signal = true))
            assertEquals(VisualizerCaptureHealth.SIGNAL, watchdog.read(2_016, signal = true))
        }
    }

    @Test fun transientErrorsResetIndependentlyAndAreNeverClassifiedAsSilence() {
        val watchdog = VisualizerSignalWatchdog(0)
        assertEquals(VisualizerCaptureHealth.API_PENDING, watchdog.read(5_000, waveform = false))
        assertEquals(VisualizerCaptureHealth.API_PENDING, watchdog.read(6_999, waveform = false))
        assertEquals(VisualizerCaptureHealth.NO_SIGNAL, watchdog.read(7_000))
        assertEquals(VisualizerCaptureHealth.API_PENDING, watchdog.read(7_016, measurement = false))
        assertEquals(VisualizerCaptureHealth.API_PENDING, watchdog.read(9_000, measurement = false))
        assertEquals(VisualizerCaptureHealth.API_ERROR, watchdog.read(9_016, measurement = false))
        assertEquals(VisualizerCaptureHealth.NO_SIGNAL, watchdog.read(9_032))
    }

    @Test fun disablingOptionalSignalWatchdogKeepsApiErrorRecovery() {
        val watchdog = VisualizerSignalWatchdog(0)
        assertEquals(VisualizerCaptureHealth.WAITING, watchdog.read(5_000, enabled = false))
        watchdog.read(5_016, waveform = false, enabled = false, music = false)
        assertEquals(VisualizerCaptureHealth.API_ERROR,
            watchdog.read(7_016, waveform = false, enabled = false, music = false))
    }
}
