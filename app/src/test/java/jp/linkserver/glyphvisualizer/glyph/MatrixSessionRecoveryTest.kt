package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.*
import org.junit.Test

class MatrixSessionRecoveryTest {
    @Test fun registrationRetriesAreBackedOffAndBoundedEvenWithRepeatedRequests() {
        val recovery = MatrixSessionRecovery()
        recovery.request(0, immediate = true)
        for (now in listOf(0L, 500L, 1500L, 3500L, 7500L)) {
            assertFalse(recovery.takeAttempt(now - 1))
            recovery.request(now, immediate = true)
            assertTrue(recovery.takeAttempt(now))
            assertFalse(recovery.takeAttempt(now))
        }
        assertTrue(recovery.exhausted)
        recovery.request(60_000, immediate = true)
        assertFalse(recovery.takeAttempt(60_000))
    }

    @Test fun successfulRegistrationDoesNotResetAnUnhealthyOutputLoop() {
        val recovery = MatrixSessionRecovery()
        recovery.request(1000, immediate = true)
        assertTrue(recovery.takeAttempt(1000))
        recovery.registered()
        assertFalse(recovery.frameSucceeded(1100))
        recovery.frameFailed()
        recovery.request(1200)
        assertEquals(2200L, recovery.nextAttemptAtMs)
        assertFalse(recovery.takeAttempt(2199))
        assertTrue(recovery.takeAttempt(2200))
        assertEquals(2, recovery.attempts)
    }

    @Test fun onlyTenSecondsOfHealthyFramesRearmsTheBudgetAndStopCancelsIt() {
        val recovery = MatrixSessionRecovery()
        recovery.request(1000)
        assertFalse(recovery.takeAttempt(1499))
        assertTrue(recovery.takeAttempt(1500))
        recovery.registered()
        assertFalse(recovery.frameSucceeded(1600))
        assertFalse(recovery.frameSucceeded(11599))
        assertTrue(recovery.frameSucceeded(11600))
        assertEquals(0, recovery.attempts)
        recovery.request(12_000)
        recovery.cancel()
        assertFalse(recovery.pending)
        assertFalse(recovery.takeAttempt(60_000))
    }
}
