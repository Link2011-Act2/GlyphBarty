package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.abs
import kotlin.math.PI
import kotlin.math.sqrt

// Experimental mechanical pause at both sides of a seek animation. Set this to false to
// bypass the pauses without touching the tape-distance calculation or rendering code.
private const val EXPERIMENTAL_TRANSITION_STOP_ENABLED = true
private const val EXPERIMENTAL_TRANSITION_STOP_MS = 120L

internal enum class OpenReelMotionStage {
    NORMAL,
    BEFORE_SEEK_STOP,
    SEEKING,
    AFTER_SEEK_STOP
}

internal data class OpenReelMotionFrame(
    val progress: Float,
    val phase: Float,
    val stage: OpenReelMotionStage
)

/**
 * Keeps reel rotation continuous while making a seek consume the same angular distance that
 * normal playback would have consumed between the old and new positions.
 */
internal class OpenReelMotionController(
    private val transitionStopMs: Long = if (EXPERIMENTAL_TRANSITION_STOP_ENABLED) {
        EXPERIMENTAL_TRANSITION_STOP_MS
    } else {
        0L
    }
) {
    companion object {
        private const val SEEK_THRESHOLD = 0.012f
        private const val REEL_SPEED_CURVE_FACTOR = 0.88f
        private const val PLAYBACK_RPM_AT_START = 22.05f // 7½ ips
        private const val SEEK_RPM_AT_START = 450f
        private const val FALLBACK_DURATION_MS = 180_000L
        private val RADIANS_PER_SECOND_PER_RPM = (2.0 * PI / 60.0).toFloat()

        internal fun playbackRpm(progress: Float): Float {
            return rpmOnReelCurve(PLAYBACK_RPM_AT_START, progress)
        }

        internal fun seekRpm(progress: Float): Float {
            return rpmOnReelCurve(SEEK_RPM_AT_START, progress)
        }

        internal fun playbackRotationSpeed(progress: Float): Float {
            return playbackRpm(progress) * RADIANS_PER_SECOND_PER_RPM
        }

        internal fun playbackAngleBetween(
            startProgress: Float,
            endProgress: Float,
            durationMs: Long
        ): Float {
            val start = startProgress.coerceIn(0f, 1f)
            val end = endProgress.coerceIn(0f, 1f)
            val integratedRpm = (2f * PLAYBACK_RPM_AT_START / REEL_SPEED_CURVE_FACTOR) *
                (
                    sqrt(1f - REEL_SPEED_CURVE_FACTOR * start) -
                        sqrt(1f - REEL_SPEED_CURVE_FACTOR * end)
                    )
            return -(durationMs.coerceAtLeast(1L) / 1000f) *
                integratedRpm * RADIANS_PER_SECOND_PER_RPM
        }

        private fun rpmOnReelCurve(startRpm: Float, progress: Float): Float {
            return startRpm / sqrt(
                1f - REEL_SPEED_CURVE_FACTOR * progress.coerceIn(0f, 1f)
            )
        }
    }

    private var displayedProgress = Float.NaN
    private var phase = 0f
    private var lastUpdateMs = 0L
    private var stage = OpenReelMotionStage.NORMAL
    private var stageStartedMs = 0L
    private var seekTargetProgress = Float.NaN
    private var seekDurationMs = FALLBACK_DURATION_MS

    fun update(
        nowMs: Long,
        frameIntervalMs: Long,
        targetProgress: Float,
        durationMs: Long?,
        playbackPaused: Boolean
    ): OpenReelMotionFrame {
        val target = targetProgress.coerceIn(0f, 1f)
        val deltaMs = if (lastUpdateMs <= 0L) {
            frameIntervalMs
        } else {
            (nowMs - lastUpdateMs).coerceIn(1L, 120L)
        }
        lastUpdateMs = nowMs

        if (displayedProgress.isNaN()) {
            displayedProgress = target
            return currentFrame()
        }

        when (stage) {
            OpenReelMotionStage.NORMAL -> {
                if (abs(target - displayedProgress) > SEEK_THRESHOLD) {
                    beginSeek(target, durationMs, nowMs)
                    if (stage == OpenReelMotionStage.SEEKING) {
                        advanceSeek(deltaMs, nowMs)
                    }
                } else {
                    displayedProgress = target
                    if (!playbackPaused) {
                        phase -= playbackRotationSpeed(displayedProgress) * (deltaMs / 1000f)
                    }
                }
            }

            OpenReelMotionStage.BEFORE_SEEK_STOP -> {
                // A user may still be dragging the seek control during the short stop.
                seekTargetProgress = target
                durationMs?.takeIf { it > 0L }?.let { seekDurationMs = it }
                if (nowMs - stageStartedMs >= transitionStopMs) {
                    stage = OpenReelMotionStage.SEEKING
                    advanceSeek(deltaMs, nowMs)
                }
            }

            OpenReelMotionStage.SEEKING -> {
                // Retarget only for another deliberate jump; ordinary playback drift remains small.
                if (abs(target - seekTargetProgress) > SEEK_THRESHOLD) {
                    seekTargetProgress = target
                    durationMs?.takeIf { it > 0L }?.let { seekDurationMs = it }
                }
                advanceSeek(deltaMs, nowMs)
            }

            OpenReelMotionStage.AFTER_SEEK_STOP -> {
                if (nowMs - stageStartedMs >= transitionStopMs) {
                    if (abs(target - displayedProgress) > SEEK_THRESHOLD) {
                        beginSeek(target, durationMs, nowMs)
                    } else {
                        // Discard only the tiny playback drift accumulated during the experimental stop.
                        displayedProgress = target
                        stage = OpenReelMotionStage.NORMAL
                        stageStartedMs = 0L
                    }
                }
            }
        }

        return currentFrame()
    }

    fun reset() {
        displayedProgress = Float.NaN
        phase = 0f
        lastUpdateMs = 0L
        stage = OpenReelMotionStage.NORMAL
        stageStartedMs = 0L
        seekTargetProgress = Float.NaN
        seekDurationMs = FALLBACK_DURATION_MS
    }

    private fun beginSeek(targetProgress: Float, durationMs: Long?, nowMs: Long) {
        seekTargetProgress = targetProgress
        seekDurationMs = durationMs?.takeIf { it > 0L } ?: FALLBACK_DURATION_MS
        stageStartedMs = nowMs
        stage = if (transitionStopMs > 0L) {
            OpenReelMotionStage.BEFORE_SEEK_STOP
        } else {
            OpenReelMotionStage.SEEKING
        }
    }

    private fun advanceSeek(deltaMs: Long, nowMs: Long) {
        val progressDelta = seekTargetProgress - displayedProgress
        val durationSeconds = seekDurationMs.coerceAtLeast(1L) / 1000f
        val windToPlaybackSpeedRatio = SEEK_RPM_AT_START / PLAYBACK_RPM_AT_START
        val maxStep = (deltaMs / 1000f) * windToPlaybackSpeedRatio / durationSeconds
        val nextProgress = if (abs(progressDelta) <= maxStep) {
            seekTargetProgress
        } else {
            displayedProgress + progressDelta.coerceIn(-maxStep, maxStep)
        }
        phase += playbackAngleBetween(displayedProgress, nextProgress, seekDurationMs)
        displayedProgress = nextProgress
        if (displayedProgress == seekTargetProgress) {
            enterAfterSeek(nowMs)
        }
    }

    private fun enterAfterSeek(nowMs: Long) {
        stageStartedMs = nowMs
        stage = if (transitionStopMs > 0L) {
            OpenReelMotionStage.AFTER_SEEK_STOP
        } else {
            OpenReelMotionStage.NORMAL
        }
    }

    private fun currentFrame(): OpenReelMotionFrame {
        return OpenReelMotionFrame(
            progress = displayedProgress.coerceIn(0f, 1f),
            phase = phase,
            stage = stage
        )
    }
}
