package jp.linkserver.glyphvisualizer.glyph

internal data class MatrixAudioSilenceDecision(
    val isSilent: Boolean,
    val drainsBeforeRelease: Boolean,
    val blackoutDue: Boolean,
    val releaseDue: Boolean,
)

internal object MatrixAudioSilencePolicy {
    const val ACTIVITY_THRESHOLD = 0.003f
    const val BLACKOUT_MS = 120L
    const val RELEASE_MS = 450L
    const val OPEN_REEL_SILENCE_BLACKOUT_MS = 2_000L
    const val OPEN_REEL_SILENCE_RELEASE_MS = 3_000L

    fun evaluate(
        renderMode: GlyphPatternRenderMode,
        activity: Float,
        silenceElapsedMs: Long,
        holdOpenReelFrameForPause: Boolean = false,
    ): MatrixAudioSilenceDecision {
        val isOpenReel = renderMode == GlyphPatternRenderMode.MATRIX_OPEN_REEL
        val isSilent = activity < ACTIVITY_THRESHOLD &&
            !(isOpenReel && holdOpenReelFrameForPause)
        val drainsBeforeRelease = renderMode == GlyphPatternRenderMode.MATRIX_RAIN ||
            renderMode == GlyphPatternRenderMode.MATRIX_SPECTROGRAM ||
            renderMode == GlyphPatternRenderMode.MATRIX_RIPPLE
        val blackoutMs = if (isOpenReel) OPEN_REEL_SILENCE_BLACKOUT_MS else BLACKOUT_MS
        val releaseMs = if (isOpenReel) OPEN_REEL_SILENCE_RELEASE_MS else RELEASE_MS
        return MatrixAudioSilenceDecision(
            isSilent = isSilent,
            drainsBeforeRelease = drainsBeforeRelease,
            blackoutDue = isSilent &&
                !drainsBeforeRelease &&
                silenceElapsedMs >= blackoutMs,
            releaseDue = isSilent &&
                !drainsBeforeRelease &&
                silenceElapsedMs >= releaseMs,
        )
    }
}
