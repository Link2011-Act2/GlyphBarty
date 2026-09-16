package jp.linkserver.glyphvisualizer

import android.media.session.PlaybackState
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlyphVisualizerServiceMediaPlaybackPolicyTest {
    @Test
    fun `open reel tracking remains enabled when media playback only is off`() {
        assertTrue(
            GlyphVisualizerService.shouldTrackMediaPlayback(
                allowPaused = true,
                mediaPlaybackOnlyEnabled = false,
            ),
        )
    }

    @Test
    fun `normal visualizer remains ungated when media playback only is off`() {
        assertFalse(
            GlyphVisualizerService.shouldTrackMediaPlayback(
                allowPaused = false,
                mediaPlaybackOnlyEnabled = false,
            ),
        )
    }

    @Test
    fun `normal visualizer uses existing gate when media playback only is on`() {
        assertTrue(
            GlyphVisualizerService.shouldTrackMediaPlayback(
                allowPaused = false,
                mediaPlaybackOnlyEnabled = true,
            ),
        )
    }

    @Test
    fun `paused timeout drops stale frames while actual mode remains open reel`() {
        assertTrue(
            GlyphVisualizerService.shouldDropOpenReelFrameForPausedTimeout(
                currentModeIsOpenReel = true,
                pausedTimeoutSuppressed = true,
            ),
        )
    }

    @Test
    fun `paused timeout suppression does not affect other modes or inactive latch`() {
        assertFalse(
            GlyphVisualizerService.shouldDropOpenReelFrameForPausedTimeout(
                currentModeIsOpenReel = false,
                pausedTimeoutSuppressed = true,
            ),
        )
        assertFalse(
            GlyphVisualizerService.shouldDropOpenReelFrameForPausedTimeout(
                currentModeIsOpenReel = true,
                pausedTimeoutSuppressed = false,
            ),
        )
    }

    @Test
    fun `paused timeout suppression clears only for resumed playback states`() {
        assertTrue(
            GlyphVisualizerService.shouldClearOpenReelPausedTimeoutSuppression(
                MediaSessionPlaybackGate.PlaybackStatus.PLAYING,
            ),
        )
        assertTrue(
            GlyphVisualizerService.shouldClearOpenReelPausedTimeoutSuppression(
                MediaSessionPlaybackGate.PlaybackStatus.BUFFERING,
            ),
        )
        assertTrue(
            GlyphVisualizerService.shouldClearOpenReelPausedTimeoutSuppression(
                MediaSessionPlaybackGate.playbackStateSemantics(
                    PlaybackState.STATE_CONNECTING,
                ).status,
            ),
        )
        assertFalse(
            GlyphVisualizerService.shouldClearOpenReelPausedTimeoutSuppression(
                MediaSessionPlaybackGate.PlaybackStatus.PAUSED,
            ),
        )
        assertFalse(
            GlyphVisualizerService.shouldClearOpenReelPausedTimeoutSuppression(
                MediaSessionPlaybackGate.PlaybackStatus.STOPPED,
            ),
        )
        assertFalse(
            GlyphVisualizerService.shouldClearOpenReelPausedTimeoutSuppression(
                MediaSessionPlaybackGate.PlaybackStatus.NONE,
            ),
        )
    }
}
