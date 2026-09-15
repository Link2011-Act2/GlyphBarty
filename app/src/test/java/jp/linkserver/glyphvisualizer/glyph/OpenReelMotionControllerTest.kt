package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.PI

class OpenReelMotionControllerTest {
    @Test
    fun reelSpeedCurve_matchesReferenceRpmTable() {
        val playbackRpm = floatArrayOf(
            22.0f, 23.1f, 24.3f, 25.7f, 27.4f, 29.5f,
            32.1f, 35.6f, 40.5f, 48.3f, 63.7f
        )
        val seekRpm = floatArrayOf(
            450f, 471f, 496f, 525f, 559f, 601f,
            655f, 726f, 827f, 987f, 1_300f
        )

        for (index in 0..10) {
            val progress = index / 10f
            assertEquals(playbackRpm[index], OpenReelMotionController.playbackRpm(progress), 0.1f)
            assertEquals(seekRpm[index], OpenReelMotionController.seekRpm(progress), 1.1f)
        }
    }

    @Test
    fun playbackAngleBetween_matchesRotationsFromNormalPlayback() {
        val angle = OpenReelMotionController.playbackAngleBetween(
            startProgress = 0.25f,
            endProgress = 0.75f,
            durationMs = 180_000L
        )

        assertEquals(-45.11f, angle / (2f * PI.toFloat()), 0.02f)
    }

    @Test
    fun playbackAngleBetween_reversesForBackwardSeek() {
        val forward = OpenReelMotionController.playbackAngleBetween(0.25f, 0.75f, 180_000L)
        val backward = OpenReelMotionController.playbackAngleBetween(0.75f, 0.25f, 180_000L)

        assertEquals(-forward, backward, 0.001f)
    }

    @Test
    fun seekAcceleration_startsMuchSlowerThanLinearAndReachesCruiseSmoothly() {
        val earlyElapsed = OpenReelMotionController.SEEK_ACCELERATION_MS / 10L
        val early = OpenReelMotionController.seekSpeedFactor(earlyElapsed, 1_000L)
        val late = OpenReelMotionController.seekSpeedFactor(
            OpenReelMotionController.SEEK_ACCELERATION_MS * 9L / 10L,
            1_000L,
        )
        val cruise = OpenReelMotionController.seekSpeedFactor(
            OpenReelMotionController.SEEK_ACCELERATION_MS,
            1_000L,
        )

        assertEquals(0.001f, early, 0.0001f)
        assertTrue(early < 0.1f)
        assertTrue(late > 0.7f)
        assertEquals(1f, cruise, 0f)
    }

    @Test
    fun seekDeceleration_isShorterThanAccelerationAndDoesNotOvershoot() {
        assertTrue(
            OpenReelMotionController.SEEK_DECELERATION_MS <
                OpenReelMotionController.SEEK_ACCELERATION_MS,
        )
        val duration = 1_000L
        val decelerationStart = duration - OpenReelMotionController.SEEK_DECELERATION_MS
        val start = OpenReelMotionController.seekSpeedFactor(decelerationStart, duration)
        val middle = OpenReelMotionController.seekSpeedFactor(
            decelerationStart + OpenReelMotionController.SEEK_DECELERATION_MS / 2L,
            duration,
        )
        val end = OpenReelMotionController.seekSpeedFactor(duration, duration)

        assertEquals(1f, start, 0f)
        assertEquals(0.75f, middle, 0.001f)
        assertEquals(0f, end, 0f)
        assertTrue(start >= middle && middle >= end)
    }

    @Test
    fun shortForwardSeek_keepsAllTurnsAndExactFinalPhase() {
        assertSeekPlanAndFinalPhase(
            startProgress = 0.10f,
            endProgress = 0.13f,
            expectCompression = false,
        )
    }

    @Test
    fun shortReverseSeek_keepsAllTurnsAndExactFinalPhase() {
        assertSeekPlanAndFinalPhase(
            startProgress = 0.13f,
            endProgress = 0.10f,
            expectCompression = false,
        )
    }

    @Test
    fun longForwardSeek_skipsOnlyFullTurnsAndKeepsExactFinalPhase() {
        assertSeekPlanAndFinalPhase(
            startProgress = 0.10f,
            endProgress = 0.90f,
            expectCompression = true,
        )
    }

    @Test
    fun longReverseSeek_skipsOnlyFullTurnsAndKeepsExactFinalPhase() {
        assertSeekPlanAndFinalPhase(
            startProgress = 0.90f,
            endProgress = 0.10f,
            expectCompression = true,
        )
    }

    @Test
    fun anglePlan_canReduce37Point4TurnsTo7Point4WithoutChangingPhysicalAngle() {
        val fullTurn = 2f * PI.toFloat()
        val physicalAngle = 37.4f * fullTurn

        val plan = OpenReelMotionController.seekAnglePlan(
            physicalAngle = physicalAngle,
            predictedUncompressedMotionMs = 5_000L,
        )

        assertEquals(30, plan.skippedFullTurns)
        assertEquals(7.4f, plan.displayedAngle / fullTurn, 0.0001f)
        assertEquals(0.4f, fractionalTurn(plan.displayedAngle / fullTurn), 0.0001f)
        assertEquals(physicalAngle, plan.physicalAngle, 0f)
    }

    @Test
    fun anglePlan_doesNotCompressSeekAtOrBelowEightTurns() {
        val fullTurn = 2f * PI.toFloat()
        listOf(3.2f, 7.6f).forEach { turns ->
            val plan = OpenReelMotionController.seekAnglePlan(
                physicalAngle = turns * fullTurn,
                predictedUncompressedMotionMs = 5_000L,
            )

            assertEquals(0, plan.skippedFullTurns)
            assertEquals(turns, plan.displayedAngle / fullTurn, 0.0001f)
        }
    }

    @Test
    fun longSeekMotionDuration_staysWithinTargetMaximum() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        controller.update(1_000L, 16L, 0f, 180_000L, playbackPaused = false)

        val seeking = controller.update(1_016L, 16L, 1f, 180_000L, playbackPaused = false)

        assertEquals(OpenReelMotionStage.SEEKING, seeking.stage)
        assertTrue(seeking.skippedFullTurns > 0)
        assertTrue(
            seeking.seekMotionDurationMs <= OpenReelMotionController.MAX_SEEK_MOTION_MS,
        )
    }

    @Test
    fun seekCompletion_returnsToProgressDependentNormalPlaybackSpeed() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        var nowMs = 1_000L
        controller.update(nowMs, 16L, 0.2f, 180_000L, playbackPaused = false)
        var frame = controller.update(nowMs + 16L, 16L, 0.8f, 180_000L, playbackPaused = false)
        nowMs += 16L
        while (frame.stage == OpenReelMotionStage.SEEKING) {
            nowMs += 16L
            frame = controller.update(nowMs, 16L, 0.8f, 180_000L, playbackPaused = false)
        }
        val completedPhase = frame.phase

        nowMs += 16L
        val normal = controller.update(nowMs, 16L, 0.8f, 180_000L, playbackPaused = false)
        val expectedDelta = -OpenReelMotionController.playbackRotationSpeed(0.8f) * 0.016f

        assertEquals(OpenReelMotionStage.NORMAL, normal.stage)
        assertEquals(expectedDelta, normal.phase - completedPhase, 0.0001f)
    }

    @Test
    fun seek_stopsBeforeAndAfterConsumingExactPlaybackAngle() {
        val controller = OpenReelMotionController(transitionStopMs = 120L)
        var nowMs = 1_000L
        var frame = controller.update(nowMs, 16L, 0.25f, 180_000L, playbackPaused = false)

        nowMs += 16L
        frame = controller.update(nowMs, 16L, 0.75f, 180_000L, playbackPaused = false)
        assertEquals(OpenReelMotionStage.BEFORE_SEEK_STOP, frame.stage)
        assertEquals(0f, frame.phase, 0f)

        nowMs += 100L
        frame = controller.update(nowMs, 16L, 0.75f, 180_000L, playbackPaused = false)
        assertEquals(OpenReelMotionStage.BEFORE_SEEK_STOP, frame.stage)
        assertEquals(0f, frame.phase, 0f)

        while (frame.stage != OpenReelMotionStage.AFTER_SEEK_STOP) {
            nowMs += 16L
            frame = controller.update(nowMs, 16L, 0.75f, 180_000L, playbackPaused = false)
        }

        val completedPhase = frame.phase
        assertEquals(-45.11f, completedPhase / (2f * PI.toFloat()), 0.02f)

        nowMs += 100L
        frame = controller.update(nowMs, 16L, 0.75f, 180_000L, playbackPaused = false)
        assertEquals(OpenReelMotionStage.AFTER_SEEK_STOP, frame.stage)
        assertEquals(completedPhase, frame.phase, 0f)

        nowMs += 20L
        frame = controller.update(nowMs, 16L, 0.75f, 180_000L, playbackPaused = false)
        assertEquals(OpenReelMotionStage.NORMAL, frame.stage)
        assertEquals(completedPhase, frame.phase, 0f)
        assertTrue(frame.progress == 0.75f)
    }

    @Test
    fun disabledTransitionStop_doesNotAddAStopStage() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        controller.update(1_000L, 33L, 0.25f, 10_000L, playbackPaused = false)

        val frame = controller.update(1_033L, 33L, 0.27f, 10_000L, playbackPaused = false)

        assertEquals(OpenReelMotionStage.SEEKING, frame.stage)
        assertEquals(0f, frame.seekSpeedFactor, 0f)
    }

    @Test
    fun pausedPlayback_keepsReelRotationStopped() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        val beforePause = controller.update(
            nowMs = 1_000L,
            frameIntervalMs = 16L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = false,
        )

        val paused = controller.update(
            nowMs = 2_000L,
            frameIntervalMs = 1_000L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = true,
        )

        assertEquals(OpenReelMotionStage.NORMAL, paused.stage)
        assertEquals(beforePause.phase, paused.phase, 0f)
    }

    @Test
    fun bufferingLikePlayback_keepsReelRotationMoving() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        val initial = controller.update(
            nowMs = 1_000L,
            frameIntervalMs = 16L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = false,
        )

        val buffering = controller.update(
            nowMs = 1_100L,
            frameIntervalMs = 100L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = false,
        )

        assertTrue(buffering.phase < initial.phase)
    }

    @Test
    fun pausedThenPlaying_resumesReelRotation() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        val initial = controller.update(
            nowMs = 1_000L,
            frameIntervalMs = 16L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = false,
        )
        val paused = controller.update(
            nowMs = 1_100L,
            frameIntervalMs = 100L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = true,
        )
        val resumed = controller.update(
            nowMs = 1_200L,
            frameIntervalMs = 100L,
            targetProgress = 0.25f,
            durationMs = 180_000L,
            playbackPaused = false,
        )

        assertEquals(initial.phase, paused.phase, 0f)
        assertTrue(resumed.phase < paused.phase)
    }

    private fun assertSeekPlanAndFinalPhase(
        startProgress: Float,
        endProgress: Float,
        expectCompression: Boolean,
    ) {
        val durationMs = 180_000L
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        var nowMs = 1_000L
        val initial = controller.update(
            nowMs = nowMs,
            frameIntervalMs = 16L,
            targetProgress = startProgress,
            durationMs = durationMs,
            playbackPaused = false,
        )
        nowMs += 16L
        var frame = controller.update(
            nowMs = nowMs,
            frameIntervalMs = 16L,
            targetProgress = endProgress,
            durationMs = durationMs,
            playbackPaused = false,
        )
        val physicalTurns = frame.physicalSeekTurns
        val displayedTurns = frame.displayedSeekTurns
        if (expectCompression) {
            assertTrue(frame.skippedFullTurns > 0)
            assertTrue(abs(displayedTurns) < abs(physicalTurns))
            assertEquals(
                fractionalTurn(abs(physicalTurns)),
                fractionalTurn(abs(displayedTurns)),
                0.0001f,
            )
        } else {
            assertEquals(0, frame.skippedFullTurns)
            assertEquals(physicalTurns, displayedTurns, 0.0001f)
        }

        while (frame.stage == OpenReelMotionStage.SEEKING) {
            nowMs += 16L
            frame = controller.update(
                nowMs = nowMs,
                frameIntervalMs = 16L,
                targetProgress = endProgress,
                durationMs = durationMs,
                playbackPaused = false,
            )
        }

        val expectedPhase = initial.phase + OpenReelMotionController.playbackAngleBetween(
            startProgress,
            endProgress,
            durationMs,
        )
        assertEquals(endProgress, frame.progress, 0f)
        assertEquals(expectedPhase, frame.phase, 0f)
    }

    private fun fractionalTurn(turns: Float): Float {
        return turns - kotlin.math.floor(turns)
    }
}
