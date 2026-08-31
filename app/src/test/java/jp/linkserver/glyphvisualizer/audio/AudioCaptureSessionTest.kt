package jp.linkserver.glyphvisualizer.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioCaptureSessionTest {
    @Test
    fun begin_invalidatesPreviousGenerationAndItsQueuedCallback() {
        val owner = AudioCaptureSessionOwner()
        val first = owner.begin()
        var callbackCount = 0
        val queuedOldCallback = {
            first.runIfCurrent { callbackCount += 1 }
        }

        val second = owner.begin()
        queuedOldCallback()

        assertNotEquals(first.id, second.id)
        assertFalse(first.isCurrent())
        assertFalse(first.shouldRun())
        assertTrue(second.isCurrent())
        assertTrue(second.shouldRun())
        assertTrue(callbackCount == 0)
    }

    @Test
    fun stopCurrent_preventsStoppedSessionCallbacks() {
        val owner = AudioCaptureSessionOwner()
        val session = owner.begin()
        var callbackCalled = false

        owner.stopCurrent()
        val delivered = session.runIfCurrent { callbackCalled = true }

        assertFalse(delivered)
        assertFalse(callbackCalled)
        assertFalse(session.isCurrent())
        assertFalse(session.shouldRun())
    }

    @Test
    fun stoppedWorkerCannotResumeWhenANewGenerationStarts() {
        val owner = AudioCaptureSessionOwner()
        val failedWorker = owner.begin()

        assertTrue(failedWorker.stopWorkerIfCurrent())
        assertFalse(failedWorker.shouldRun())
        assertTrue(failedWorker.isCurrent())
        assertFalse(failedWorker.runIfRunningCurrent { error("must not run") })
        assertTrue(failedWorker.runIfCurrent { })

        val replacement = owner.begin()

        assertFalse(failedWorker.shouldRun())
        assertFalse(failedWorker.isCurrent())
        assertTrue(replacement.shouldRun())
    }
}
