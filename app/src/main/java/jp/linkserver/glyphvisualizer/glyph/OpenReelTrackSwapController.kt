package jp.linkserver.glyphvisualizer.glyph

import jp.linkserver.glyphvisualizer.audio.MediaTrackChangeDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class OpenReelPoint(
    val x: Float,
    val y: Float
)

internal data class OpenReelTrackSwapFrame(
    val active: Boolean,
    val completed: Boolean,
    val direction: MediaTrackChangeDirection,
    val animationProgress: Float,
    val outgoingCenter: OpenReelPoint,
    val incomingCenter: OpenReelPoint,
    val outgoingProgress: Float,
    val outgoingPhase: Float,
    val incomingProgress: Float,
    val incomingPhase: Float,
    val drawTapeExit: Boolean
)

/** Carousel motion for track replacement. Reel self-rotation remains owned by OpenReelMotionController. */
internal class OpenReelTrackSwapController(
    private val animationDurationMs: Long = 520L,
    private val carouselRadiusScale: Float = 1.5f,
    private val replacementAngleDegrees: Float = 35f
) {
    private var currentTrackKey: String? = null
    private var incomingTrackKey: String? = null
    private var active = false
    private var startedAtMs = 0L
    private var direction = MediaTrackChangeDirection.NEXT
    private var outgoingProgress = 0f
    private var outgoingPhase = 0f
    private var incomingProgress = 0f
    private var incomingPhase = 0f

    fun update(
        nowMs: Long,
        matrixLength: Int,
        matrixCenterX: Float,
        matrixCenterY: Float,
        trackKey: String?,
        directionHint: MediaTrackChangeDirection?,
        currentProgress: Float,
        currentPhase: Float,
        latestTrackProgress: Float
    ): OpenReelTrackSwapFrame {
        if (currentTrackKey == null && !active) {
            if (trackKey != null) currentTrackKey = trackKey
            return idleFrame(matrixCenterX, matrixCenterY, currentProgress, currentPhase)
        }

        if (!active && trackKey != null && trackKey != currentTrackKey) {
            active = true
            startedAtMs = nowMs
            incomingTrackKey = trackKey
            direction = if (directionHint == MediaTrackChangeDirection.PREVIOUS) {
                MediaTrackChangeDirection.PREVIOUS
            } else {
                // UNKNOWN is semantic; only the animation falls back to NEXT.
                MediaTrackChangeDirection.NEXT
            }
            outgoingProgress = currentProgress
            outgoingPhase = currentPhase
            incomingProgress = latestTrackProgress
            incomingPhase = 0f
        } else if (active && trackKey != null && trackKey != incomingTrackKey) {
            // Multiple rapid skips use the latest track as the reel currently being installed.
            incomingTrackKey = trackKey
            incomingProgress = latestTrackProgress
        } else if (active) {
            incomingProgress = latestTrackProgress
        }

        if (!active) {
            return idleFrame(matrixCenterX, matrixCenterY, currentProgress, currentPhase)
        }

        val linearProgress = ((nowMs - startedAtMs).coerceAtLeast(0L) /
            animationDurationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        val easedProgress = easeInOutCubic(linearProgress)
        val sign = if (direction == MediaTrackChangeDirection.NEXT) 1f else -1f
        val replacementAngle = (replacementAngleDegrees / 180f) * PI.toFloat()
        val outgoingAngle = sign * replacementAngle * easedProgress
        val incomingAngle = -sign * replacementAngle * (1f - easedProgress)
        val carouselRadius = matrixLength * carouselRadiusScale
        val outgoingCenter = pointOnCarousel(
            matrixCenterX,
            matrixCenterY,
            carouselRadius,
            outgoingAngle
        )
        val incomingCenter = pointOnCarousel(
            matrixCenterX,
            matrixCenterY,
            carouselRadius,
            incomingAngle
        )

        if (linearProgress >= 1f) {
            currentTrackKey = incomingTrackKey
            incomingTrackKey = null
            active = false
            startedAtMs = 0L
            return OpenReelTrackSwapFrame(
                active = false,
                completed = true,
                direction = direction,
                animationProgress = 1f,
                outgoingCenter = outgoingCenter,
                incomingCenter = OpenReelPoint(matrixCenterX, matrixCenterY),
                outgoingProgress = outgoingProgress,
                outgoingPhase = outgoingPhase,
                incomingProgress = incomingProgress,
                incomingPhase = incomingPhase,
                drawTapeExit = true
            )
        }

        return OpenReelTrackSwapFrame(
            active = true,
            completed = false,
            direction = direction,
            animationProgress = easedProgress,
            outgoingCenter = outgoingCenter,
            incomingCenter = incomingCenter,
            outgoingProgress = outgoingProgress,
            outgoingPhase = outgoingPhase,
            incomingProgress = incomingProgress,
            incomingPhase = incomingPhase,
            drawTapeExit = false
        )
    }

    fun reset() {
        currentTrackKey = null
        incomingTrackKey = null
        active = false
        startedAtMs = 0L
        direction = MediaTrackChangeDirection.NEXT
        outgoingProgress = 0f
        outgoingPhase = 0f
        incomingProgress = 0f
        incomingPhase = 0f
    }

    private fun idleFrame(
        centerX: Float,
        centerY: Float,
        progress: Float,
        phase: Float
    ): OpenReelTrackSwapFrame {
        val center = OpenReelPoint(centerX, centerY)
        return OpenReelTrackSwapFrame(
            active = false,
            completed = false,
            direction = MediaTrackChangeDirection.NEXT,
            animationProgress = 1f,
            outgoingCenter = center,
            incomingCenter = center,
            outgoingProgress = progress,
            outgoingPhase = phase,
            incomingProgress = progress,
            incomingPhase = phase,
            drawTapeExit = true
        )
    }

    private fun pointOnCarousel(
        matrixCenterX: Float,
        matrixCenterY: Float,
        radius: Float,
        angle: Float
    ): OpenReelPoint {
        return OpenReelPoint(
            x = matrixCenterX + sin(angle) * radius,
            y = matrixCenterY + radius * (1f - cos(angle))
        )
    }

    private fun easeInOutCubic(value: Float): Float {
        return if (value < 0.5f) {
            4f * value * value * value
        } else {
            val inverse = -2f * value + 2f
            1f - (inverse * inverse * inverse) / 2f
        }
    }
}
