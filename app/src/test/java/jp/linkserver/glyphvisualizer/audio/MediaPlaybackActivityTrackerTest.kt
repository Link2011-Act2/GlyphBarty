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

    private fun tracker() = MediaPlaybackActivityTracker(
        resumeConfirmMs = 1_000L,
        openReelGraceMs = 750L,
    )

    private fun activateOpenReel(
        tracker: MediaPlaybackActivityTracker,
        nowMs: Long,
    ) {
        val active = tracker.update(
            nowMs = nowMs,
            rawMediaPlaybackActive = true,
            allowPaused = true,
            pausedPlayback = true,
        )
        assertTrue(active.allowed)
    }
}
