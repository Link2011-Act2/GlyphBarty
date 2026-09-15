package jp.linkserver.glyphvisualizer.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackActivityTrackerTest {
    @Test
    fun `open reel keeps playback active when session recovers after 500ms`() {
        val tracker = tracker()
        activateOpenReel(tracker, nowMs = 1_000L)

        val missing = tracker.update(1_100L, false, true, false)
        val stillMissing = tracker.update(1_350L, false, true, false)
        val recovered = tracker.update(1_600L, true, true, false)

        assertTrue(missing.allowed)
        assertTrue(stillMissing.allowed)
        assertTrue(recovered.allowed)
        assertTrue(tracker.lastMediaPlaybackActive)
        assertEquals(0L, tracker.mediaPlaybackResumeCandidateAtMs)
        assertEquals(0L, tracker.openReelMediaSessionMissingSinceMs)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_GRACE_STARTED),
            missing.events,
        )
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_GRACE_KEEPING_ALIVE),
            stillMissing.events,
        )
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_GRACE_RECOVERED),
            recovered.events,
        )
    }

    @Test
    fun `open reel suppresses playback when missing reaches 750ms`() {
        val tracker = tracker()
        activateOpenReel(tracker, nowMs = 1_000L)
        tracker.update(1_100L, false, true, false)

        val expired = tracker.update(1_850L, false, true, false)

        assertFalse(expired.allowed)
        assertFalse(tracker.lastMediaPlaybackActive)
        assertEquals(0L, tracker.openReelMediaSessionMissingSinceMs)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_GRACE_EXPIRED),
            expired.events,
        )
    }

    @Test
    fun `recovery within grace does not start resume confirmation`() {
        val tracker = tracker()
        activateOpenReel(tracker, nowMs = 1_000L)
        tracker.update(1_100L, false, true, false)

        val recovered = tracker.update(1_600L, true, true, false)

        assertTrue(recovered.allowed)
        assertEquals(0L, tracker.mediaPlaybackResumeCandidateAtMs)
        assertFalse(
            recovered.events.contains(
                MediaPlaybackActivityTracker.Event.PLAYBACK_RESUMED_CONFIRMED,
            ),
        )
    }

    @Test
    fun `recovery after grace uses existing one second resume confirmation`() {
        val tracker = tracker()
        activateOpenReel(tracker, nowMs = 1_000L)
        tracker.update(1_100L, false, true, false)
        tracker.update(1_850L, false, true, false)

        val candidate = tracker.update(1_900L, true, true, false)
        val notYetConfirmed = tracker.update(2_899L, true, true, false)
        val confirmed = tracker.update(2_900L, true, true, false)

        assertFalse(candidate.allowed)
        assertFalse(notYetConfirmed.allowed)
        assertTrue(confirmed.allowed)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.PLAYBACK_RESUMED_CONFIRMED),
            confirmed.events,
        )
    }

    @Test
    fun `open reel paused and buffering-equivalent snapshots remain allowed`() {
        val tracker = tracker()

        val paused = tracker.update(1_000L, true, true, true)
        val bufferingEquivalent = tracker.update(1_250L, true, true, true)

        assertTrue(paused.allowed)
        assertTrue(bufferingEquivalent.allowed)
        assertTrue(tracker.lastMediaPlaybackActive)
    }

    @Test
    fun `normal visualizer suppresses immediately without grace`() {
        val tracker = tracker()
        activateOpenReel(tracker, nowMs = 1_000L)

        val missing = tracker.update(1_100L, false, false, false)

        assertFalse(missing.allowed)
        assertFalse(tracker.lastMediaPlaybackActive)
        assertEquals(0L, tracker.openReelMediaSessionMissingSinceMs)
        assertTrue(missing.events.isEmpty())
    }

    @Test
    fun `reset clears open reel grace and playback tracking state`() {
        val tracker = tracker()
        activateOpenReel(tracker, nowMs = 1_000L)
        tracker.update(1_100L, false, true, false)
        assertEquals(1_100L, tracker.openReelMediaSessionMissingSinceMs)

        tracker.reset()

        assertFalse(tracker.lastMediaPlaybackActive)
        assertEquals(0L, tracker.mediaPlaybackResumeCandidateAtMs)
        assertEquals(0L, tracker.openReelMediaSessionMissingSinceMs)
    }

    @Test
    fun `paused remains active at ten seconds and 29999ms`() {
        val tracker = tracker()
        val started = tracker.update(1_000L, true, true, true, true)
        val atTenSeconds = tracker.update(11_000L, true, true, true, true)
        val beforeExpiry = tracker.update(30_999L, true, true, true, true)

        assertTrue(started.allowed)
        assertTrue(atTenSeconds.allowed)
        assertTrue(beforeExpiry.allowed)
        assertEquals(1_000L, tracker.openReelPausedSinceMs)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_PAUSED_HOLD_STARTED),
            started.events,
        )
    }

    @Test
    fun `paused expires at exactly thirty seconds and stays inactive`() {
        val tracker = tracker()
        tracker.update(1_000L, true, true, true, true)

        val expired = tracker.update(31_000L, true, true, true, true)
        val stillPaused = tracker.update(40_000L, true, true, true, true)

        assertFalse(expired.allowed)
        assertFalse(stillPaused.allowed)
        assertFalse(tracker.lastMediaPlaybackActive)
        assertEquals(0L, tracker.openReelPausedSinceMs)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_PAUSED_HOLD_EXPIRED),
            expired.events,
        )
        assertTrue(stillPaused.events.isEmpty())
    }

    @Test
    fun `playing recovery before paused timeout clears hold without suspension`() {
        val tracker = tracker()
        tracker.update(1_000L, true, true, true, true)
        tracker.update(16_000L, true, true, true, true)

        val recovered = tracker.update(16_100L, true, true, false, false)

        assertTrue(recovered.allowed)
        assertEquals(0L, tracker.openReelPausedSinceMs)
        assertEquals(0L, tracker.mediaPlaybackResumeCandidateAtMs)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_PAUSED_HOLD_CLEARED),
            recovered.events,
        )
    }

    @Test
    fun `buffering or connecting does not start paused timeout`() {
        val tracker = tracker()

        val buffering = tracker.update(1_000L, true, true, true, false)
        val connectingAfterThirtySeconds = tracker.update(31_000L, true, true, true, false)

        assertTrue(buffering.allowed)
        assertTrue(connectingAfterThirtySeconds.allowed)
        assertEquals(0L, tracker.openReelPausedSinceMs)
    }

    @Test
    fun `buffering clears an existing paused timeout`() {
        val tracker = tracker()
        tracker.update(1_000L, true, true, true, true)
        tracker.update(16_000L, true, true, true, true)

        val buffering = tracker.update(16_100L, true, true, true, false)
        val stillBuffering = tracker.update(46_100L, true, true, true, false)

        assertTrue(buffering.allowed)
        assertTrue(stillBuffering.allowed)
        assertEquals(0L, tracker.openReelPausedSinceMs)
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.OPEN_REEL_PAUSED_HOLD_CLEARED),
            buffering.events,
        )
    }

    @Test
    fun `playing after paused timeout uses resume confirmation and clears old hold`() {
        val tracker = tracker()
        tracker.update(1_000L, true, true, true, true)
        tracker.update(31_000L, true, true, true, true)

        val candidate = tracker.update(31_100L, true, true, false, false)
        val confirmed = tracker.update(32_100L, true, true, false, false)

        assertFalse(candidate.allowed)
        assertTrue(confirmed.allowed)
        assertEquals(0L, tracker.openReelPausedSinceMs)
        assertTrue(
            candidate.events.contains(
                MediaPlaybackActivityTracker.Event.OPEN_REEL_PAUSED_HOLD_CLEARED,
            ),
        )
        assertEquals(
            listOf(MediaPlaybackActivityTracker.Event.PLAYBACK_RESUMED_CONFIRMED),
            confirmed.events,
        )
    }

    @Test
    fun `reset clears paused hold state`() {
        val tracker = tracker()
        tracker.update(1_000L, true, true, true, true)
        assertEquals(1_000L, tracker.openReelPausedSinceMs)

        tracker.reset()

        assertEquals(0L, tracker.openReelPausedSinceMs)
        assertFalse(tracker.lastMediaPlaybackActive)
    }

    @Test
    fun `cleared paused hold does not carry its old deadline into a later pause`() {
        val tracker = tracker()
        tracker.update(1_000L, true, true, true, true)
        tracker.clearOpenReelPausedHoldState()

        val secondPause = tracker.update(20_000L, true, true, true, true)
        val beforeSecondDeadline = tracker.update(49_999L, true, true, true, true)

        assertTrue(secondPause.allowed)
        assertTrue(beforeSecondDeadline.allowed)
        assertEquals(20_000L, tracker.openReelPausedSinceMs)
    }

    private fun tracker() = MediaPlaybackActivityTracker(
        resumeConfirmMs = 1_000L,
        openReelGraceMs = 750L,
        openReelPausedHoldMs = 30_000L,
    )

    private fun activateOpenReel(
        tracker: MediaPlaybackActivityTracker,
        nowMs: Long,
    ) {
        val active = tracker.update(
            nowMs = nowMs,
            rawMediaPlaybackActive = true,
            allowPaused = true,
            openReelNonPlayingSessionActive = true,
        )
        assertTrue(active.allowed)
    }
}
