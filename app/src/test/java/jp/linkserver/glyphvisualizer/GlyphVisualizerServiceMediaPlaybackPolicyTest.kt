package jp.linkserver.glyphvisualizer

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
}
