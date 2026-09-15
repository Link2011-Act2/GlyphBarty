package jp.linkserver.glyphvisualizer.audio

internal data class TrustedTrackEdge(
    val previousTrackKey: String,
    val nextTrackKey: String
)

internal data class TrackHistoryResolution(
    val trackChanged: Boolean,
    val oldTrackKey: String?,
    val newTrackKey: String,
    val trustedHistoryDirection: MediaTrackChangeDirection,
    val resolvedDirection: MediaTrackChangeDirection,
    val directionSource: MediaTrackDirectionSource,
    val trustedEdges: List<TrustedTrackEdge>
)

/** Keeps only adjacency edges confirmed by a trusted direction source. */
internal class TrackDirectionHistory(
    private val maxEdges: Int = 12
) {
    private val trustedEdges = linkedSetOf<TrustedTrackEdge>()
    private var currentTrackKey: String? = null

    fun resolve(
        newTrackKey: String,
        explicitDirection: MediaTrackChangeDirection?,
        explicitSource: MediaTrackDirectionSource?
    ): TrackHistoryResolution {
        val oldTrackKey = currentTrackKey
        if (oldTrackKey == null) {
            currentTrackKey = newTrackKey
            return resolution(
                trackChanged = false,
                oldTrackKey = null,
                newTrackKey = newTrackKey,
                trustedHistoryDirection = MediaTrackChangeDirection.UNKNOWN,
                resolvedDirection = MediaTrackChangeDirection.UNKNOWN,
                directionSource = MediaTrackDirectionSource.UNKNOWN_FALLBACK
            )
        }
        if (oldTrackKey == newTrackKey) {
            return resolution(
                trackChanged = false,
                oldTrackKey = oldTrackKey,
                newTrackKey = newTrackKey,
                trustedHistoryDirection = MediaTrackChangeDirection.UNKNOWN,
                resolvedDirection = explicitDirection ?: MediaTrackChangeDirection.UNKNOWN,
                directionSource = explicitSource ?: MediaTrackDirectionSource.UNKNOWN_FALLBACK
            )
        }

        val historyDirection = directionFromTrustedEdge(oldTrackKey, newTrackKey)
        val resolvedDirection = explicitDirection ?: historyDirection
        val directionSource = explicitSource ?: if (historyDirection != MediaTrackChangeDirection.UNKNOWN) {
            MediaTrackDirectionSource.TRUSTED_HISTORY
        } else {
            MediaTrackDirectionSource.UNKNOWN_FALLBACK
        }
        if (
            resolvedDirection != MediaTrackChangeDirection.UNKNOWN &&
            directionSource != MediaTrackDirectionSource.UNKNOWN_FALLBACK
        ) {
            recordTrustedTransition(oldTrackKey, newTrackKey, resolvedDirection)
        }
        currentTrackKey = newTrackKey
        return resolution(
            trackChanged = true,
            oldTrackKey = oldTrackKey,
            newTrackKey = newTrackKey,
            trustedHistoryDirection = historyDirection,
            resolvedDirection = resolvedDirection,
            directionSource = directionSource
        )
    }

    fun reset(initialTrackKey: String? = null) {
        trustedEdges.clear()
        currentTrackKey = initialTrackKey
    }

    private fun directionFromTrustedEdge(
        oldTrackKey: String,
        newTrackKey: String
    ): MediaTrackChangeDirection {
        return when {
            TrustedTrackEdge(oldTrackKey, newTrackKey) in trustedEdges ->
                MediaTrackChangeDirection.NEXT
            TrustedTrackEdge(newTrackKey, oldTrackKey) in trustedEdges ->
                MediaTrackChangeDirection.PREVIOUS
            else -> MediaTrackChangeDirection.UNKNOWN
        }
    }

    private fun recordTrustedTransition(
        oldTrackKey: String,
        newTrackKey: String,
        direction: MediaTrackChangeDirection
    ) {
        val edge = when (direction) {
            MediaTrackChangeDirection.NEXT -> TrustedTrackEdge(oldTrackKey, newTrackKey)
            MediaTrackChangeDirection.PREVIOUS -> TrustedTrackEdge(newTrackKey, oldTrackKey)
            MediaTrackChangeDirection.UNKNOWN -> return
        }
        trustedEdges.removeAll { existing ->
            existing.previousTrackKey == edge.previousTrackKey ||
                existing.nextTrackKey == edge.nextTrackKey ||
                (
                    existing.previousTrackKey == edge.nextTrackKey &&
                        existing.nextTrackKey == edge.previousTrackKey
                    )
        }
        trustedEdges += edge
        while (trustedEdges.size > maxEdges.coerceAtLeast(1)) {
            trustedEdges.remove(trustedEdges.first())
        }
    }

    private fun resolution(
        trackChanged: Boolean,
        oldTrackKey: String?,
        newTrackKey: String,
        trustedHistoryDirection: MediaTrackChangeDirection,
        resolvedDirection: MediaTrackChangeDirection,
        directionSource: MediaTrackDirectionSource
    ): TrackHistoryResolution {
        return TrackHistoryResolution(
            trackChanged = trackChanged,
            oldTrackKey = oldTrackKey,
            newTrackKey = newTrackKey,
            trustedHistoryDirection = trustedHistoryDirection,
            resolvedDirection = resolvedDirection,
            directionSource = directionSource,
            trustedEdges = trustedEdges.toList()
        )
    }
}
