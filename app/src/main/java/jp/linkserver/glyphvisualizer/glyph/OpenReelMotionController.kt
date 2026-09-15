package jp.linkserver.glyphvisualizer.glyph

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
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
    val stage: OpenReelMotionStage,
    val seekSpeedFactor: Float = 0f,
    val seekMotionDurationMs: Long = 0L,
    val physicalSeekTurns: Float = 0f,
    val displayedSeekTurns: Float = 0f,
    val skippedFullTurns: Int = 0,
)

internal data class OpenReelSeekAnglePlan(
    val physicalAngle: Float,
    val displayedAngle: Float,
    val skippedFullTurns: Int,
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
        internal const val SEEK_ACCELERATION_MS = 280L
        internal const val SEEK_DECELERATION_MS = 180L
        internal const val MAX_SEEK_MOTION_MS = 1_100L
        private const val MAX_UNCOMPRESSED_DISPLAY_TURNS = 8f
        private val RADIANS_PER_SECOND_PER_RPM = (2.0 * PI / 60.0).toFloat()
        private val FULL_TURN_RADIANS = (2.0 * PI).toFloat()

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

        internal fun seekAnglePlan(
            physicalAngle: Float,
            predictedUncompressedMotionMs: Long,
        ): OpenReelSeekAnglePlan {
            val physicalTurns = abs(physicalAngle) / FULL_TURN_RADIANS
            val shouldCompress = physicalTurns > MAX_UNCOMPRESSED_DISPLAY_TURNS &&
                predictedUncompressedMotionMs > MAX_SEEK_MOTION_MS
            val skippedFullTurns = if (shouldCompress) {
                ceil(physicalTurns - MAX_UNCOMPRESSED_DISPLAY_TURNS)
                    .toInt()
                    .coerceAtMost(floor(physicalTurns).toInt())
            } else {
                0
            }
            val signedSkippedAngle = when {
                physicalAngle > 0f -> skippedFullTurns * FULL_TURN_RADIANS
                physicalAngle < 0f -> -skippedFullTurns * FULL_TURN_RADIANS
                else -> 0f
            }
            return OpenReelSeekAnglePlan(
                physicalAngle = physicalAngle,
                displayedAngle = physicalAngle - signedSkippedAngle,
                skippedFullTurns = skippedFullTurns,
            )
        }

        internal fun seekSpeedFactor(elapsedMs: Long, motionDurationMs: Long): Float {
            val duration = motionDurationMs.coerceAtLeast(
                SEEK_ACCELERATION_MS + SEEK_DECELERATION_MS
            )
            val elapsed = elapsedMs.coerceIn(0L, duration)
            return when {
                elapsed <= SEEK_ACCELERATION_MS -> {
                    val t = elapsed / SEEK_ACCELERATION_MS.toFloat()
                    t * t * t
                }

                elapsed < duration - SEEK_DECELERATION_MS -> 1f
                else -> {
                    val t = (elapsed - (duration - SEEK_DECELERATION_MS)) /
                        SEEK_DECELERATION_MS.toFloat()
                    (1f - t * t).coerceIn(0f, 1f)
                }
            }
        }

        internal fun seekTravelFraction(elapsedMs: Long, motionDurationMs: Long): Float {
            val duration = motionDurationMs.coerceAtLeast(
                SEEK_ACCELERATION_MS + SEEK_DECELERATION_MS
            )
            val elapsed = elapsedMs.coerceIn(0L, duration)
            val accelerationWeightMs = SEEK_ACCELERATION_MS / 4f
            val cruiseDurationMs =
                (duration - SEEK_ACCELERATION_MS - SEEK_DECELERATION_MS).coerceAtLeast(0L)
            val decelerationWeightMs = SEEK_DECELERATION_MS * (2f / 3f)
            val totalWeightMs = accelerationWeightMs + cruiseDurationMs + decelerationWeightMs
            val travelledWeightMs = when {
                elapsed <= SEEK_ACCELERATION_MS -> {
                    val t = elapsed / SEEK_ACCELERATION_MS.toFloat()
                    accelerationWeightMs * t * t * t * t
                }

                elapsed < duration - SEEK_DECELERATION_MS -> {
                    accelerationWeightMs + (elapsed - SEEK_ACCELERATION_MS)
                }

                else -> {
                    val decelerationElapsed = elapsed - (duration - SEEK_DECELERATION_MS)
                    val t = decelerationElapsed / SEEK_DECELERATION_MS.toFloat()
                    accelerationWeightMs + cruiseDurationMs +
                        SEEK_DECELERATION_MS * (t - t * t * t / 3f)
                }
            }
            return (travelledWeightMs / totalWeightMs).coerceIn(0f, 1f)
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
    private var seekStartProgress = Float.NaN
    private var seekStartPhase = 0f
    private var seekPhysicalAngle = 0f
    private var seekDisplayedAngle = 0f
    private var seekMotionStartedMs = 0L
    private var seekMotionDurationMs = 0L
    private var seekSkippedFullTurns = 0

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
                        advanceSeek(nowMs)
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
                    startSeekMotion(nowMs)
                    advanceSeek(nowMs)
                }
            }

            OpenReelMotionStage.SEEKING -> {
                // Retarget only for another deliberate jump; ordinary playback drift remains small.
                if (abs(target - seekTargetProgress) > SEEK_THRESHOLD) {
                    seekTargetProgress = target
                    durationMs?.takeIf { it > 0L }?.let { seekDurationMs = it }
                    startSeekMotion(nowMs)
                }
                advanceSeek(nowMs)
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
        seekStartProgress = Float.NaN
        seekStartPhase = 0f
        seekPhysicalAngle = 0f
        seekDisplayedAngle = 0f
        seekMotionStartedMs = 0L
        seekMotionDurationMs = 0L
        seekSkippedFullTurns = 0
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
        if (stage == OpenReelMotionStage.SEEKING) {
            startSeekMotion(nowMs)
        }
    }

    private fun startSeekMotion(nowMs: Long) {
        seekStartProgress = displayedProgress
        seekStartPhase = phase
        seekPhysicalAngle = playbackAngleBetween(
            seekStartProgress,
            seekTargetProgress,
            seekDurationMs,
        )
        val progressDelta = abs(seekTargetProgress - seekStartProgress)
        val windToPlaybackSpeedRatio = SEEK_RPM_AT_START / PLAYBACK_RPM_AT_START
        val predictedUncompressedMotionMs = (
            progressDelta * seekDurationMs / windToPlaybackSpeedRatio
            ).toLong().coerceAtLeast(1L)
        val anglePlan = seekAnglePlan(
            physicalAngle = seekPhysicalAngle,
            predictedUncompressedMotionMs = predictedUncompressedMotionMs,
        )
        seekDisplayedAngle = anglePlan.displayedAngle
        seekSkippedFullTurns = anglePlan.skippedFullTurns
        val compressionRatio = if (abs(seekPhysicalAngle) > 0f) {
            abs(seekDisplayedAngle / seekPhysicalAngle)
        } else {
            1f
        }
        val accelerationAndDecelerationPenaltyMs =
            SEEK_ACCELERATION_MS * 3f / 4f + SEEK_DECELERATION_MS / 3f
        seekMotionDurationMs = (
            predictedUncompressedMotionMs * compressionRatio +
                accelerationAndDecelerationPenaltyMs
            ).toLong().coerceIn(
            SEEK_ACCELERATION_MS + SEEK_DECELERATION_MS,
            MAX_SEEK_MOTION_MS,
        )
        seekMotionStartedMs = nowMs
        stageStartedMs = nowMs
        stage = OpenReelMotionStage.SEEKING
    }

    private fun advanceSeek(nowMs: Long) {
        val elapsedMs = (nowMs - seekMotionStartedMs).coerceAtLeast(0L)
        val travelFraction = seekTravelFraction(elapsedMs, seekMotionDurationMs)
        displayedProgress = seekStartProgress +
            (seekTargetProgress - seekStartProgress) * travelFraction
        phase = seekStartPhase + seekDisplayedAngle * travelFraction
        if (elapsedMs >= seekMotionDurationMs) {
            displayedProgress = seekTargetProgress
            phase = seekStartPhase + seekPhysicalAngle
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
            stage = stage,
            seekSpeedFactor = if (stage == OpenReelMotionStage.SEEKING) {
                seekSpeedFactor(
                    elapsedMs = (lastUpdateMs - seekMotionStartedMs).coerceAtLeast(0L),
                    motionDurationMs = seekMotionDurationMs,
                )
            } else {
                0f
            },
            seekMotionDurationMs = seekMotionDurationMs,
            physicalSeekTurns = seekPhysicalAngle / FULL_TURN_RADIANS,
            displayedSeekTurns = seekDisplayedAngle / FULL_TURN_RADIANS,
            skippedFullTurns = seekSkippedFullTurns,
        )
    }
}
