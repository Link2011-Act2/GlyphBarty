package jp.linkserver.glyphvisualizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CaptureServiceRuntimeTest {
    @Test
    fun toneFocusDefaultsToTenPercentBassAcrossUiAndServiceFallbacks() {
        assertEquals(-0.1f, SettingsPreferences.defaultParameters().toneFocus, 0.0001f)
        assertEquals(-0.1f, defaultServiceCaptureConfig().toneFocus, 0.0001f)
    }

    @Test
    fun sessionCoordinator_keepsSourceAndInvalidatesOldRequestGenerations() {
        val coordinator = CaptureSessionCoordinator()

        val appSession = coordinator.beginVisualizer(VisualizerStartSource.APP, actionAtMs = 100L)
        val tileSession = coordinator.beginVisualizer(VisualizerStartSource.QUICK_SETTINGS, actionAtMs = 250L)

        assertFalse(coordinator.isCurrent(appSession.requestId))
        assertTrue(coordinator.isCurrent(tileSession.requestId))
        assertEquals(VisualizerStartSource.QUICK_SETTINGS, tileSession.startSource)
        assertEquals(250L, tileSession.startActionAtMs)

        val invalidated = coordinator.invalidate()
        assertFalse(coordinator.isCurrent(tileSession.requestId))
        assertEquals(tileSession.startSource, invalidated.startSource)
        assertEquals(tileSession.startActionAtMs, invalidated.startActionAtMs)
    }

    @Test
    fun retryPolicy_preservesBluetoothAndDefaultTimingContract() {
        assertEquals(6, CaptureRetryPolicy.maxAttempts(true, true))
        assertEquals(4, CaptureRetryPolicy.maxAttempts(true, false))
        assertEquals(2_100L, CaptureRetryPolicy.retryDelayMs(3, true, true))
        assertEquals(480L, CaptureRetryPolicy.retryDelayMs(3, false, true))
        assertEquals(4_000L, CaptureRetryPolicy.routeRestartSuppressionMs(true, true))
        assertEquals(1_500L, CaptureRetryPolicy.routeRestartSuppressionMs(false, true))
    }

    @Test
    fun latencyScheduler_preservesOrderAndNextDueTime() {
        data class Frame(val id: Int, val dueAt: Long)
        val scheduler = LatencyFrameScheduler<Frame> { it.dueAt }
        scheduler.enqueue(Frame(1, 10L))
        scheduler.enqueue(Frame(2, 20L))
        scheduler.enqueue(Frame(3, 30L))

        val firstDrain = scheduler.drainAllDue(nowMs = 20L)
        val secondDrain = scheduler.drainAllDue(nowMs = 30L)

        assertEquals(listOf(1, 2), firstDrain.frames.map { it.id })
        assertEquals(30L, firstDrain.nextDueAtMs)
        assertEquals(listOf(3), secondDrain.frames.map { it.id })
        assertTrue(scheduler.isEmpty())
    }

    @Test
    fun matrixLatencyScheduler_collapsesDueFramesAndRejectsOldEpoch() {
        data class Frame(val epoch: Long, val id: Int, val dueAt: Long)
        val scheduler = LatencyFrameScheduler<Frame> { it.dueAt }
        scheduler.enqueue(Frame(epoch = 1L, id = 1, dueAt = 5L))
        scheduler.enqueue(Frame(epoch = 2L, id = 2, dueAt = 10L))
        scheduler.enqueue(Frame(epoch = 2L, id = 3, dueAt = 15L))
        scheduler.enqueue(Frame(epoch = 2L, id = 4, dueAt = 25L))

        val drain = scheduler.drainLatestDue(nowMs = 20L) { it.epoch == 2L }

        assertEquals(listOf(3), drain.frames.map { it.id })
        assertEquals(25L, drain.nextDueAtMs)
    }

    @Test
    fun servicePublishedConfig_keepsLegacyLatencyAndMatrixSmoothMotionBehavior() {
        val state = CaptureUiState(latencyMs = 12f, matrixSmoothMotionEnabled = true)
        val config = state.toCaptureConfig().copy(
            sensitivity = 2.7f,
            latencyMs = 88f,
            matrixSmoothMotionEnabled = false
        )

        val published = config.applyToServicePublishedUiState(state)

        assertEquals(2.7f, published.sensitivity, 0.0001f)
        assertEquals(12f, published.latencyMs, 0.0001f)
        assertTrue(published.matrixSmoothMotionEnabled)
    }

    @Test
    fun quickSettingsColdStart_publishesAllServiceConfigFieldsBeforeUiCanSync() {
        val state = CaptureUiState(
            smoothingBalance = -0.4f,
            baseIndicatorEnabled = true,
            recordingLightIncluded = true,
            latencyMs = 12f,
            level = 0.8f,
            peak = 0.9f,
            isCapturing = false
        )
        val config = state.toCaptureConfig().copy(
            sensitivity = 2.6f,
            smoothingBalance = 0.7f,
            baseIndicatorEnabled = false,
            recordingLightIncluded = false,
            latencyMs = 88f
        )

        val started = config.applyToStartedUiState(state, "running", "VISUALIZER")
        val stopped = config.applyToStoppedUiState(started, "ready")

        assertTrue(started.isCapturing)
        assertEquals("VISUALIZER", started.activeMode)
        assertEquals(0.7f, started.smoothingBalance, 0.0001f)
        assertFalse(started.baseIndicatorEnabled)
        assertFalse(started.recordingLightIncluded)
        assertEquals(88f, started.latencyMs, 0.0001f)
        assertFalse(stopped.isCapturing)
        assertEquals("IDLE", stopped.activeMode)
        assertEquals(0f, stopped.level, 0.0001f)
        assertEquals(0f, stopped.peak, 0.0001f)
        assertEquals(88f, stopped.latencyMs, 0.0001f)
        assertFalse(stopped.baseIndicatorEnabled)
        assertFalse(stopped.recordingLightIncluded)
    }

    @Test
    fun crashRetryGeneration_isRejectedAfterStopOrReplacementSession() {
        val coordinator = CaptureSessionCoordinator()
        val crashed = coordinator.beginVisualizer(VisualizerStartSource.APP, actionAtMs = 100L)
        assertTrue(coordinator.isCurrent(crashed.requestId))

        val delayedRetryRequestId = coordinator.invalidate().requestId
        assertTrue(coordinator.isCurrent(delayedRetryRequestId))

        coordinator.invalidate()
        assertFalse(coordinator.isCurrent(delayedRetryRequestId))

        val replacement = coordinator.beginVisualizer(
            VisualizerStartSource.QUICK_SETTINGS,
            actionAtMs = 200L
        )
        assertFalse(coordinator.isCurrent(delayedRetryRequestId))
        assertTrue(coordinator.isCurrent(replacement.requestId))
    }

    @Test
    fun notificationIdentifiersRemainCompatible() {
        assertEquals("glyph_visualizer", CaptureNotificationController.CHANNEL_ID)
        assertEquals(42, CaptureNotificationController.NOTIFICATION_ID)
        assertEquals("glyph_visualizer_alerts", CaptureNotificationController.ALERT_CHANNEL_ID)
        assertEquals(43, CaptureNotificationController.ALERT_NOTIFICATION_ID)
    }
}
