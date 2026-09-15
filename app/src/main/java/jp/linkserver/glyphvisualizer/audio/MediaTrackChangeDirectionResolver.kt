package jp.linkserver.glyphvisualizer.audio

internal data class MediaTrackDirectionResolution(
    val resolvedDirection: MediaTrackChangeDirection,
    val animationDirection: MediaTrackChangeDirection,
    val directionSource: MediaTrackDirectionSource,
    val trackChanged: Boolean,
    val oldTrackKey: String?,
    val newTrackKey: String?,
    val rawSkipHint: MediaTrackChangeDirection?,
    val queueDirection: MediaTrackChangeDirection?,
    val trackNumberDirection: MediaTrackChangeDirection?,
    val trustedHistoryDirection: MediaTrackChangeDirection,
    val trustedEdges: List<TrustedTrackEdge>
)

internal class MediaTrackChangeDirectionResolver(
    private val transportHintTtlMs: Long = 2_000L,
    private val trackHistory: TrackDirectionHistory = TrackDirectionHistory()
) {
    private data class TransportHint(
        val packageName: String,
        val direction: MediaTrackChangeDirection,
        val capturedAtMs: Long
    )

    private data class TrackObservation(
        val packageName: String,
        val trackKey: String,
        val queueIndex: Int?,
        val trackNumber: Long?
    )

    private var recentTransportHint: TransportHint? = null
    private var lastTrackObservation: TrackObservation? = null

    fun resolve(
        nowMs: Long,
        packageName: String,
        rawDirection: MediaTrackChangeDirection?,
        trackKey: String?,
        queueIndex: Int?,
        trackNumber: Long?
    ): MediaTrackDirectionResolution {
        if (rawDirection != null && rawDirection != MediaTrackChangeDirection.UNKNOWN) {
            recentTransportHint = TransportHint(packageName, rawDirection, nowMs)
        } else if (recentTransportHint?.let { nowMs - it.capturedAtMs > transportHintTtlMs } == true) {
            recentTransportHint = null
        }
        if (trackKey == null) {
            val semanticDirection = rawDirection ?: MediaTrackChangeDirection.UNKNOWN
            return emptyResolution(
                resolvedDirection = semanticDirection,
                rawSkipHint = rawDirection,
                directionSource = sourceForRawDirection(rawDirection)
                    ?: MediaTrackDirectionSource.UNKNOWN_FALLBACK
            )
        }

        val current = TrackObservation(packageName, trackKey, queueIndex, trackNumber)
        val previous = lastTrackObservation
        if (previous == null) {
            lastTrackObservation = current
            return fromHistory(
                historyResolution = trackHistory.resolve(
                    newTrackKey = trackKey,
                    explicitDirection = rawDirection,
                    explicitSource = sourceForRawDirection(rawDirection)
                ),
                rawSkipHint = rawDirection,
                queueDirection = null,
                trackNumberDirection = null
            )
        }
        if (previous.trackKey == trackKey) {
            lastTrackObservation = previous.copy(
                queueIndex = previous.queueIndex ?: queueIndex,
                trackNumber = previous.trackNumber ?: trackNumber
            )
            return fromHistory(
                historyResolution = trackHistory.resolve(
                    newTrackKey = trackKey,
                    explicitDirection = rawDirection,
                    explicitSource = sourceForRawDirection(rawDirection)
                ),
                rawSkipHint = rawDirection,
                queueDirection = null,
                trackNumberDirection = null
            )
        }

        val retainedSkipHint = recentTransportHint?.takeIf {
            it.packageName == packageName && nowMs - it.capturedAtMs <= transportHintTtlMs
        }?.direction
        val rawSkipHint = rawDirection?.takeUnless { it == MediaTrackChangeDirection.UNKNOWN }
            ?: retainedSkipHint
        val samePackage = previous.packageName == packageName
        val queueDirection = compareOrderedValues(
            samePackage = samePackage,
            previous = previous.queueIndex,
            current = queueIndex
        )
        val trackNumberDirection = compareOrderedValues(
            samePackage = samePackage,
            previous = previous.trackNumber,
            current = trackNumber
        )
        val explicitDirection = rawSkipHint ?: queueDirection ?: trackNumberDirection
        val explicitSource = when {
            rawSkipHint != null -> MediaTrackDirectionSource.RAW_SKIP_HINT
            queueDirection != null -> MediaTrackDirectionSource.QUEUE_INDEX
            trackNumberDirection != null -> MediaTrackDirectionSource.TRACK_NUMBER
            else -> null
        }
        val historyResolution = if (samePackage) {
            trackHistory.resolve(trackKey, explicitDirection, explicitSource)
        } else {
            trackHistory.reset(trackKey)
            TrackHistoryResolution(
                trackChanged = true,
                oldTrackKey = previous.trackKey,
                newTrackKey = trackKey,
                trustedHistoryDirection = MediaTrackChangeDirection.UNKNOWN,
                resolvedDirection = explicitDirection ?: MediaTrackChangeDirection.UNKNOWN,
                directionSource = explicitSource ?: MediaTrackDirectionSource.UNKNOWN_FALLBACK,
                trustedEdges = emptyList()
            )
        }
        lastTrackObservation = current
        recentTransportHint = null
        return fromHistory(
            historyResolution = historyResolution,
            rawSkipHint = rawSkipHint,
            queueDirection = queueDirection,
            trackNumberDirection = trackNumberDirection
        )
    }

    private fun fromHistory(
        historyResolution: TrackHistoryResolution,
        rawSkipHint: MediaTrackChangeDirection?,
        queueDirection: MediaTrackChangeDirection?,
        trackNumberDirection: MediaTrackChangeDirection?
    ): MediaTrackDirectionResolution {
        val resolvedDirection = historyResolution.resolvedDirection
        return MediaTrackDirectionResolution(
            resolvedDirection = resolvedDirection,
            animationDirection = resolvedDirection.forAnimation(),
            directionSource = historyResolution.directionSource,
            trackChanged = historyResolution.trackChanged,
            oldTrackKey = historyResolution.oldTrackKey,
            newTrackKey = historyResolution.newTrackKey,
            rawSkipHint = rawSkipHint,
            queueDirection = queueDirection,
            trackNumberDirection = trackNumberDirection,
            trustedHistoryDirection = historyResolution.trustedHistoryDirection,
            trustedEdges = historyResolution.trustedEdges
        )
    }

    private fun emptyResolution(
        resolvedDirection: MediaTrackChangeDirection,
        rawSkipHint: MediaTrackChangeDirection?,
        directionSource: MediaTrackDirectionSource
    ): MediaTrackDirectionResolution {
        return MediaTrackDirectionResolution(
            resolvedDirection = resolvedDirection,
            animationDirection = resolvedDirection.forAnimation(),
            directionSource = directionSource,
            trackChanged = false,
            oldTrackKey = lastTrackObservation?.trackKey,
            newTrackKey = null,
            rawSkipHint = rawSkipHint,
            queueDirection = null,
            trackNumberDirection = null,
            trustedHistoryDirection = MediaTrackChangeDirection.UNKNOWN,
            trustedEdges = emptyList()
        )
    }

    private fun sourceForRawDirection(
        direction: MediaTrackChangeDirection?
    ): MediaTrackDirectionSource? {
        return if (direction != null && direction != MediaTrackChangeDirection.UNKNOWN) {
            MediaTrackDirectionSource.RAW_SKIP_HINT
        } else {
            null
        }
    }

    private fun <T : Comparable<T>> compareOrderedValues(
        samePackage: Boolean,
        previous: T?,
        current: T?
    ): MediaTrackChangeDirection? {
        if (!samePackage || previous == null || current == null || previous == current) return null
        return if (current > previous) {
            MediaTrackChangeDirection.NEXT
        } else {
            MediaTrackChangeDirection.PREVIOUS
        }
    }
}

internal fun MediaTrackChangeDirection.forAnimation(): MediaTrackChangeDirection {
    return if (this == MediaTrackChangeDirection.UNKNOWN) {
        MediaTrackChangeDirection.NEXT
    } else {
        this
    }
}
