package jp.linkserver.glyphvisualizer.audio

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaSessionPlaybackStateSemanticsTest {
    @Test
    fun `playing is active without pausing reel motion`() {
        assertSemantics(
            rawState = PlaybackState.STATE_PLAYING,
            expectedStatus = MediaSessionPlaybackGate.PlaybackStatus.PLAYING,
            expectedActive = true,
            expectedMotionPaused = false,
        )
    }

    @Test
    fun `buffering and connecting stay active without pausing reel motion`() {
        listOf(
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING,
        ).forEach { rawState ->
            assertSemantics(
                rawState = rawState,
                expectedStatus = MediaSessionPlaybackGate.PlaybackStatus.BUFFERING,
                expectedActive = true,
                expectedMotionPaused = false,
            )
        }
    }

    @Test
    fun `only real paused state pauses reel motion while remaining active`() {
        assertSemantics(
            rawState = PlaybackState.STATE_PAUSED,
            expectedStatus = MediaSessionPlaybackGate.PlaybackStatus.PAUSED,
            expectedActive = true,
            expectedMotionPaused = true,
        )
    }

    @Test
    fun `stopped none and error are inactive stopped states`() {
        listOf(
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE,
            PlaybackState.STATE_ERROR,
        ).forEach { rawState ->
            assertSemantics(
                rawState = rawState,
                expectedStatus = MediaSessionPlaybackGate.PlaybackStatus.STOPPED,
                expectedActive = false,
                expectedMotionPaused = false,
            )
        }
    }

    @Test
    fun `skipping fast forwarding and rewinding retain playing semantics`() {
        listOf(
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
        ).forEach { rawState ->
            assertSemantics(
                rawState = rawState,
                expectedStatus = MediaSessionPlaybackGate.PlaybackStatus.PLAYING,
                expectedActive = true,
                expectedMotionPaused = false,
            )
        }
    }

    private fun assertSemantics(
        rawState: Int,
        expectedStatus: MediaSessionPlaybackGate.PlaybackStatus,
        expectedActive: Boolean,
        expectedMotionPaused: Boolean,
    ) {
        val semantics = MediaSessionPlaybackGate.playbackStateSemantics(rawState)

        assertEquals(expectedStatus, semantics.status)
        if (expectedActive) {
            assertTrue(semantics.activeForOpenReel)
        } else {
            assertFalse(semantics.activeForOpenReel)
        }
        if (expectedMotionPaused) {
            assertTrue(semantics.motionPaused)
        } else {
            assertFalse(semantics.motionPaused)
        }
    }
}
