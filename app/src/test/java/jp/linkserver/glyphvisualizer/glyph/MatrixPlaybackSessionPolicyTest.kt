package jp.linkserver.glyphvisualizer.glyph

import android.media.session.PlaybackState
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackSnapshot
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus.BUFFERING
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus.NONE
import jp.linkserver.glyphvisualizer.glyph.MatrixPlaybackSessionPolicy.Decision.*
import org.junit.Assert.*
import org.junit.Test

class MatrixPlaybackSessionPolicyTest {
    private fun snapshot(status: jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus,
        key: String = "a", rawState: Int? = null) = PlaybackSnapshot(status, null, trackKey = key, rawState = rawState)

    @Test fun playingRetainsSessionRegardlessOfAudioAndTitleDuration() {
        val policy = MatrixPlaybackSessionPolicy()
        for (now in listOf(1000L, 1500L, 3000L, 60_000L)) {
            assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), now, false, false))
        }
    }

    @Test fun ordinaryPauseIsConfirmedAtFourHundredMillisecondsAndShortPauseRecovers() {
        val policy = MatrixPlaybackSessionPolicy()
        policy.evaluate(snapshot(PLAYING_STATUS), 1000, true, false)
        assertEquals(TRANSITION, policy.evaluate(snapshot(PAUSED_STATUS), 2000, false, false))
        assertEquals(TRANSITION, policy.evaluate(snapshot(PAUSED_STATUS), 2399, false, false))
        assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 2399, false, false))
        policy.evaluate(snapshot(PAUSED_STATUS), 3000, false, false)
        assertEquals(PAUSED, policy.evaluate(snapshot(PAUSED_STATUS), 3400, false, false))
        assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 3500, false, false))
    }

    @Test fun trackChangeAndSkippingExtendPauseButCannotKeepItIndefinitely() {
        for (skip in listOf(false, true)) {
            val policy = MatrixPlaybackSessionPolicy()
            policy.evaluate(snapshot(PLAYING_STATUS), 1000, true, false)
            policy.evaluate(snapshot(PLAYING_STATUS, if (skip) "a" else "b",
                if (skip) PlaybackState.STATE_SKIPPING_TO_NEXT else null), 2000, false, false)
            policy.evaluate(snapshot(PAUSED_STATUS, if (skip) "a" else "b"), 2100, false, false)
            assertEquals(TRANSITION, policy.evaluate(snapshot(PAUSED_STATUS, "b"), 3000, false, false))
            assertEquals(PAUSED, policy.evaluate(snapshot(PAUSED_STATUS, "c"), 3600, false, false))
            assertEquals(PAUSED, policy.evaluate(snapshot(PAUSED_STATUS, "d"), 5000, false, false))
        }
    }

    @Test fun bufferingAndConnectingHoldTemporarilyAndPlayingRecovers() {
        val policy = MatrixPlaybackSessionPolicy()
        policy.evaluate(snapshot(PLAYING_STATUS), 1000, true, false)
        assertEquals(TRANSITION, policy.evaluate(snapshot(BUFFERING, rawState = PlaybackState.STATE_CONNECTING), 2000, false, false))
        assertEquals(TRANSITION, policy.evaluate(snapshot(BUFFERING), 4999, false, false))
        assertEquals(NO_MEDIA, policy.evaluate(snapshot(BUFFERING), 5000, false, false))
        assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 5100, false, false))
    }

    @Test fun noneMissingAndUnknownAreBoundedWhileExplicitStopIsImmediate() {
        for (none in listOf(snapshot(NONE), snapshot(STOPPED_STATUS, rawState = PlaybackState.STATE_NONE))) {
            val policy = MatrixPlaybackSessionPolicy()
            policy.evaluate(snapshot(PLAYING_STATUS), 1000, true, false)
            assertEquals(TRANSITION, policy.evaluate(none, 2000, false, false))
            assertEquals(TRANSITION, policy.evaluate(none, 2749, false, false))
            assertEquals(AUDIO_FALLBACK, policy.evaluate(none, 2750, false, false))
            assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 2800, false, false))
            assertEquals(STOPPED, policy.evaluate(snapshot(STOPPED_STATUS), 2801, false, false))
        }
    }

    @Test fun repeatedTransientStatesCannotExtendTheBufferingBudget() {
        val policy = MatrixPlaybackSessionPolicy()
        policy.evaluate(snapshot(PLAYING_STATUS), 1000, true, false)
        policy.evaluate(snapshot(BUFFERING), 2000, false, false)
        policy.evaluate(snapshot(NONE), 2500, false, false)
        policy.evaluate(snapshot(BUFFERING), 3000, false, false)
        policy.evaluate(snapshot(PAUSED_STATUS), 3500, false, false)
        assertFalse(policy.evaluate(snapshot(BUFFERING), 5000, false, false).retainSession)
    }

    @Test fun unavailableAccessFallsBackButMusicOnlyNeverAllowsUnverifiedAudio() {
        val unavailable = snapshot(NONE).copy(sessionAccessAvailable = false)
        assertEquals(AUDIO_FALLBACK, MatrixPlaybackSessionPolicy().evaluate(unavailable, 1000, true, false))
        assertEquals(NO_MEDIA, MatrixPlaybackSessionPolicy().evaluate(unavailable, 1000, true, true))
        for (status in listOf(PAUSED_STATUS, STOPPED_STATUS, NONE)) {
            assertEquals(AUDIO_FALLBACK, MatrixPlaybackSessionPolicy().evaluate(snapshot(status), 1000, true, false))
            assertFalse(MatrixPlaybackSessionPolicy().evaluate(snapshot(status), 1000, true, true).allowOutput)
        }
    }

    @Test fun musicOnlyUsesExistingPlayingConfirmationAfterARealPauseButNotABriefOne() {
        val policy = MatrixPlaybackSessionPolicy()
        assertEquals(RESUME_CONFIRM, policy.evaluate(snapshot(PLAYING_STATUS), 1000, true, true))
        assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 2000, true, true))
        assertEquals(TRANSITION, policy.evaluate(snapshot(PAUSED_STATUS), 2100, false, true))
        assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 2200, false, true))
        policy.evaluate(snapshot(PAUSED_STATUS), 3000, false, true)
        assertEquals(PAUSED, policy.evaluate(snapshot(PAUSED_STATUS), 3400, false, true))
        assertEquals(RESUME_CONFIRM, policy.evaluate(snapshot(PLAYING_STATUS), 3500, false, true))
        assertEquals(RESUME_CONFIRM, policy.evaluate(snapshot(PLAYING_STATUS), 4499, false, true))
        assertEquals(PLAYING, policy.evaluate(snapshot(PLAYING_STATUS), 4500, false, true))
    }

    companion object {
        private val PLAYING_STATUS = jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus.PLAYING
        private val PAUSED_STATUS = jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus.PAUSED
        private val STOPPED_STATUS = jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus.STOPPED
    }
}
