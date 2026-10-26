package jp.linkserver.glyphvisualizer

import jp.linkserver.glyphvisualizer.audio.VisualizerCaptureHealth
import jp.linkserver.glyphvisualizer.audio.VisualizerSignalWatchdog
import org.junit.Assert.*
import org.junit.Test

class VisualizerRecoveryPolicyTest {
    @Test fun silenceKeepsRecoveryOpportunitiesPastLegacyRetryLimitOnBothRoutes() {
        for (bluetooth in listOf(false, true)) {
            val policy = VisualizerRecoveryPolicy()
            val delays = (1..20).map { policy.retryDelayMs(true, bluetooth, true) }
            assertEquals(listOf(30_000L, 60_000L, 120_000L, 240_000L, 300_000L), delays.take(5))
            assertTrue(delays.drop(5).all { it == 300_000L })
            assertTrue(delays.size > CaptureRetryPolicy.maxAttempts(bluetooth, true))
        }
    }

    @Test fun apiErrorsUseExistingRouteDelaysThenKeepRetryingAfterCooldown() {
        for (bluetooth in listOf(false, true)) {
            val policy = VisualizerRecoveryPolicy()
            val maxAttempts = CaptureRetryPolicy.maxAttempts(bluetooth, true)
            for (attempt in 1..maxAttempts) {
                assertEquals(CaptureRetryPolicy.retryDelayMs(attempt, bluetooth, true),
                    policy.retryDelayMs(false, bluetooth, true))
            }
            repeat(100) { assertEquals(60_000L, policy.retryDelayMs(false, bluetooth, true)) }
        }
    }

    @Test fun briefSignalOrVideoSwitchDoesNotResetBackoffButSustainedSignalDoes() {
        val policy = VisualizerRecoveryPolicy()
        assertEquals(30_000L, policy.retryDelayMs(true, false, true))
        policy.observeHealth(VisualizerCaptureHealth.SIGNAL, 1_000)
        policy.observeHealth(VisualizerCaptureHealth.WAITING, 10_999)
        assertEquals(60_000L, policy.retryDelayMs(true, false, true))
        policy.observeHealth(VisualizerCaptureHealth.SIGNAL, 11_000)
        policy.observeHealth(VisualizerCaptureHealth.WAITING, 21_000)
        assertEquals(30_000L, policy.retryDelayMs(true, false, true))
        policy.reset()
        assertEquals(30_000L, policy.retryDelayMs(true, false, true))
    }

    @Test fun stuckZeroCaptureStillRequestsRecoveryAfterAnHourAndCanProduceSignalAgain() {
        val policy = VisualizerRecoveryPolicy()
        var now = 0L
        var attempts = 0
        while (now < 3_600_000L) {
            val watchdog = VisualizerSignalWatchdog(now)
            now += 1_800L
            assertEquals(VisualizerCaptureHealth.NO_SIGNAL,
                watchdog.observe(now, true, true, false, true, true))
            now += policy.retryDelayMs(true, true, true)
            attempts++
        }
        // The old service stopped at six retries; the new policy still has a bounded next retry.
        assertTrue(attempts > 6)
        assertTrue(attempts <= 16)
        val restored = VisualizerSignalWatchdog(now)
        assertEquals(VisualizerCaptureHealth.SIGNAL,
            restored.observe(now + 16, true, true, true, true, true))
    }
}
