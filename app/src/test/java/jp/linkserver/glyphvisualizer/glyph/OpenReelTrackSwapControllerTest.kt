package jp.linkserver.glyphvisualizer.glyph

import jp.linkserver.glyphvisualizer.audio.MediaTrackChangeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class OpenReelTrackSwapControllerTest {
    private val centerX = 12f
    private val centerY = 12f

    @Test
    fun initialTrackAndSameTrack_doNotStartSwap() {
        val controller = OpenReelTrackSwapController()

        val missingIdentity = update(controller, nowMs = 900L, trackKey = null)
        val initial = update(controller, nowMs = 1_000L, trackKey = "track-a")
        val sameTrack = update(controller, nowMs = 1_100L, trackKey = "track-a")

        assertFalse(missingIdentity.active)
        assertFalse(initial.active)
        assertFalse(sameTrack.active)
        assertTrue(initial.drawTapeExit)
        assertTrue(sameTrack.drawTapeExit)
    }

    @Test
    fun nextSwap_movesIncomingLeftToCenterAndOutgoingCenterToRight() {
        val controller = OpenReelTrackSwapController()
        update(controller, nowMs = 1_000L, trackKey = "track-a")

        val start = update(
            controller,
            nowMs = 1_100L,
            trackKey = "track-b",
            direction = MediaTrackChangeDirection.NEXT
        )
        val middle = update(controller, nowMs = 1_360L, trackKey = "track-b")
        val end = update(controller, nowMs = 1_620L, trackKey = "track-b")
        val carouselRadius = 25f * 1.5f
        val fullAngle = 35f / 180f * PI.toFloat()
        val halfAngle = fullAngle * 0.5f

        assertEquals(centerX, start.outgoingCenter.x, 0.001f)
        assertEquals(centerY, start.outgoingCenter.y, 0.001f)
        assertEquals(centerX - sin(fullAngle) * carouselRadius, start.incomingCenter.x, 0.001f)
        assertEquals(
            centerY + carouselRadius * (1f - cos(fullAngle)),
            start.incomingCenter.y,
            0.001f
        )
        assertEquals(centerX + sin(halfAngle) * carouselRadius, middle.outgoingCenter.x, 0.001f)
        assertEquals(centerX - sin(halfAngle) * carouselRadius, middle.incomingCenter.x, 0.001f)
        assertEquals(
            centerY + carouselRadius * (1f - cos(halfAngle)),
            middle.outgoingCenter.y,
            0.001f
        )
        assertEquals(middle.outgoingCenter.y, middle.incomingCenter.y, 0.001f)
        assertFalse(end.active)
        assertTrue(end.completed)
        assertEquals(centerX + sin(fullAngle) * carouselRadius, end.outgoingCenter.x, 0.001f)
        assertEquals(
            centerY + carouselRadius * (1f - cos(fullAngle)),
            end.outgoingCenter.y,
            0.001f
        )
        assertEquals(centerX, end.incomingCenter.x, 0f)
        assertEquals(centerY, end.incomingCenter.y, 0f)
    }

    @Test
    fun previousSwap_reversesCarouselDirection() {
        val controller = OpenReelTrackSwapController()
        update(controller, nowMs = 1_000L, trackKey = "track-a")

        val start = update(
            controller,
            nowMs = 1_100L,
            trackKey = "track-b",
            direction = MediaTrackChangeDirection.PREVIOUS
        )
        val middle = update(controller, nowMs = 1_360L, trackKey = "track-b")
        val end = update(controller, nowMs = 1_620L, trackKey = "track-b")

        assertTrue(start.incomingCenter.x > centerX)
        assertTrue(middle.outgoingCenter.x < centerX)
        assertTrue(middle.incomingCenter.x > centerX)
        assertTrue(end.outgoingCenter.x < centerX)
        assertEquals(centerX, end.incomingCenter.x, 0f)
    }

    @Test
    fun unknownDirection_defaultsToNextAndHidesTapeUntilCompletion() {
        val controller = OpenReelTrackSwapController()
        update(controller, nowMs = 1_000L, trackKey = "track-a")

        val start = update(
            controller,
            nowMs = 1_100L,
            trackKey = "track-b",
            direction = MediaTrackChangeDirection.UNKNOWN
        )
        val middle = update(controller, nowMs = 1_360L, trackKey = "track-b")
        val end = update(controller, nowMs = 1_620L, trackKey = "track-b")

        assertEquals(MediaTrackChangeDirection.NEXT, start.direction)
        assertTrue(start.incomingCenter.x < centerX)
        assertTrue(middle.outgoingCenter.x > centerX)
        assertFalse(start.drawTapeExit)
        assertFalse(middle.drawTapeExit)
        assertTrue(end.drawTapeExit)
    }

    @Test
    fun activeSwap_neverRequestsTapeExitDrawing() {
        val controller = OpenReelTrackSwapController()
        update(controller, nowMs = 1_000L, trackKey = "track-a")

        for (elapsedMs in 0L until 520L step 13) {
            val frame = update(controller, nowMs = 1_100L + elapsedMs, trackKey = "track-b")
            assertTrue(frame.active)
            assertFalse(frame.drawTapeExit)
        }

        val completed = update(controller, nowMs = 1_620L, trackKey = "track-b")
        assertFalse(completed.active)
        assertTrue(completed.drawTapeExit)
    }

    @Test
    fun rapidTrackChanges_finishWithLatestIncomingTrack() {
        val controller = OpenReelTrackSwapController()
        update(controller, nowMs = 1_000L, trackKey = "track-a")
        update(controller, nowMs = 1_100L, trackKey = "track-b")
        val retargeted = update(controller, nowMs = 1_200L, trackKey = "track-c")
        val completed = update(controller, nowMs = 1_620L, trackKey = "track-c")
        val settled = update(controller, nowMs = 1_636L, trackKey = "track-c")

        assertTrue(retargeted.active)
        assertTrue(completed.completed)
        assertFalse(settled.active)
        assertFalse(settled.completed)
    }

    @Test
    fun trackChangeProgressJump_bypassesSeekAndResetsAtLatestProgress() {
        val swapController = OpenReelTrackSwapController()
        val motionController = OpenReelMotionController()
        var motion = motionController.update(1_000L, 16L, 0.9f, 180_000L, playbackPaused = false)
        update(swapController, nowMs = 1_000L, trackKey = "track-a")

        val startedSwap = swapController.update(
            nowMs = 1_100L,
            matrixLength = 25,
            matrixCenterX = centerX,
            matrixCenterY = centerY,
            trackKey = "track-b",
            directionHint = MediaTrackChangeDirection.NEXT,
            currentProgress = motion.progress,
            currentPhase = motion.phase,
            latestTrackProgress = 0.01f
        )

        assertTrue(startedSwap.active)
        assertEquals(OpenReelMotionStage.NORMAL, motion.stage)

        val completedSwap = swapController.update(
            nowMs = 1_620L,
            matrixLength = 25,
            matrixCenterX = centerX,
            matrixCenterY = centerY,
            trackKey = "track-b",
            directionHint = null,
            currentProgress = motion.progress,
            currentPhase = motion.phase,
            latestTrackProgress = 0.03f
        )
        assertTrue(completedSwap.completed)

        motionController.reset()
        motion = motionController.update(1_620L, 16L, 0.03f, 180_000L, playbackPaused = false)
        assertEquals(OpenReelMotionStage.NORMAL, motion.stage)
        assertEquals(0.03f, motion.progress, 0f)
    }

    @Test
    fun sameTrackProgressJump_remainsASeek() {
        val swapController = OpenReelTrackSwapController()
        val motionController = OpenReelMotionController()
        var motion = motionController.update(1_000L, 16L, 0.1f, 180_000L, playbackPaused = false)
        update(swapController, nowMs = 1_000L, trackKey = "track-a")

        val swap = update(swapController, nowMs = 1_100L, trackKey = "track-a")
        if (!swap.active) {
            motion = motionController.update(1_100L, 16L, 0.8f, 180_000L, playbackPaused = false)
        }

        assertFalse(swap.active)
        assertEquals(OpenReelMotionStage.BEFORE_SEEK_STOP, motion.stage)
    }

    private fun update(
        controller: OpenReelTrackSwapController,
        nowMs: Long,
        trackKey: String?,
        direction: MediaTrackChangeDirection? = null
    ): OpenReelTrackSwapFrame {
        return controller.update(
            nowMs = nowMs,
            matrixLength = 25,
            matrixCenterX = centerX,
            matrixCenterY = centerY,
            trackKey = trackKey,
            directionHint = direction,
            currentProgress = 0.8f,
            currentPhase = 1.25f,
            latestTrackProgress = 0.05f
        )
    }
}
