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
    fun seek_usesFastWindRpmCurve() {
        val controller = OpenReelMotionController(transitionStopMs = 0L)
        controller.update(1_000L, 16L, 0.5f, 180_000L, playbackPaused = false)

        val frame = controller.update(1_016L, 16L, 0.75f, 180_000L, playbackPaused = false)
        val effectiveRpm = abs(frame.phase) / 0.016f * 60f / (2f * PI.toFloat())

        assertEquals(OpenReelMotionStage.SEEKING, frame.stage)
        assertEquals(OpenReelMotionController.seekRpm(0.5f), effectiveRpm, 1f)
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

        assertEquals(OpenReelMotionStage.NORMAL, frame.stage)
        assertEquals(0.27f, frame.progress, 0f)
    }
}
