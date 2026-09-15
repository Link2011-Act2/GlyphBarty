package jp.linkserver.glyphvisualizer.glyph

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixAudioSilencePolicyTest {
    @Test
    fun `open reel playing at 1000ms of silence keeps the matrix session`() {
        val decision = openReelDecision(elapsedMs = 1_000L)

        assertTrue(decision.isSilent)
        assertFalse(decision.blackoutDue)
        assertFalse(decision.releaseDue)
        assertTrue(decision.renderWhileSilent)
    }

    @Test
    fun `open reel playing waits until 2000ms before blackout`() {
        val beforeBlackout = openReelDecision(elapsedMs = 1_999L)
        val atBlackout = openReelDecision(elapsedMs = 2_000L)

        assertFalse(beforeBlackout.blackoutDue)
        assertTrue(beforeBlackout.renderWhileSilent)
        assertTrue(atBlackout.blackoutDue)
        assertFalse(atBlackout.releaseDue)
        assertFalse(atBlackout.renderWhileSilent)
    }

    @Test
    fun `open reel playing waits until 3000ms before release`() {
        val beforeRelease = openReelDecision(elapsedMs = 2_999L)
        val atRelease = openReelDecision(elapsedMs = 3_000L)

        assertTrue(beforeRelease.blackoutDue)
        assertFalse(beforeRelease.releaseDue)
        assertTrue(atRelease.releaseDue)
    }

    @Test
    fun `open reel activity recovery clears silence actions and restarts elapsed time`() {
        val silent = openReelDecision(elapsedMs = 1_500L)
        val recovered = MatrixAudioSilencePolicy.evaluate(
            renderMode = GlyphPatternRenderMode.MATRIX_OPEN_REEL,
            activity = 0.5f,
            silenceElapsedMs = 1_500L,
        )
        val nextSilenceStart = openReelDecision(elapsedMs = 0L)

        assertTrue(silent.isSilent)
        assertFalse(silent.blackoutDue)
        assertFalse(recovered.isSilent)
        assertFalse(recovered.blackoutDue)
        assertFalse(recovered.releaseDue)
        assertFalse(recovered.renderWhileSilent)
        assertTrue(nextSilenceStart.isSilent)
        assertFalse(nextSilenceStart.blackoutDue)
        assertFalse(nextSilenceStart.releaseDue)
        assertTrue(nextSilenceStart.renderWhileSilent)
    }

    @Test
    fun `open reel paused suppresses silence beyond three seconds`() {
        val decision = MatrixAudioSilencePolicy.evaluate(
            renderMode = GlyphPatternRenderMode.MATRIX_OPEN_REEL,
            activity = 0f,
            silenceElapsedMs = 3_500L,
            holdOpenReelFrameForPause = true,
        )

        assertFalse(decision.isSilent)
        assertFalse(decision.blackoutDue)
        assertFalse(decision.releaseDue)
        assertFalse(decision.renderWhileSilent)
    }

    @Test
    fun `normal matrix mode retains 120ms blackout and 450ms release`() {
        val beforeBlackout = normalDecision(elapsedMs = 119L)
        val atBlackout = normalDecision(elapsedMs = 120L)
        val beforeRelease = normalDecision(elapsedMs = 449L)
        val atRelease = normalDecision(elapsedMs = 450L)

        assertTrue(beforeBlackout.isSilent)
        assertFalse(beforeBlackout.blackoutDue)
        assertTrue(atBlackout.blackoutDue)
        assertFalse(atBlackout.renderWhileSilent)
        assertFalse(beforeRelease.releaseDue)
        assertTrue(atRelease.releaseDue)
    }

    @Test
    fun `rain spectrogram and ripple retain silence drain behavior`() {
        listOf(
            GlyphPatternRenderMode.MATRIX_RAIN,
            GlyphPatternRenderMode.MATRIX_SPECTROGRAM,
            GlyphPatternRenderMode.MATRIX_RIPPLE,
        ).forEach { renderMode ->
            val decision = MatrixAudioSilencePolicy.evaluate(
                renderMode = renderMode,
                activity = 0f,
                silenceElapsedMs = 1_000L,
            )

            assertTrue(decision.isSilent)
            assertTrue(decision.drainsBeforeRelease)
            assertFalse(decision.blackoutDue)
            assertFalse(decision.releaseDue)
            assertFalse(decision.renderWhileSilent)
        }
    }

    private fun normalDecision(elapsedMs: Long): MatrixAudioSilenceDecision {
        return MatrixAudioSilencePolicy.evaluate(
            renderMode = GlyphPatternRenderMode.MATRIX_BAR,
            activity = 0f,
            silenceElapsedMs = elapsedMs,
        )
    }

    private fun openReelDecision(elapsedMs: Long): MatrixAudioSilenceDecision {
        return MatrixAudioSilencePolicy.evaluate(
            renderMode = GlyphPatternRenderMode.MATRIX_OPEN_REEL,
            activity = 0f,
            silenceElapsedMs = elapsedMs,
        )
    }
}
