package jp.linkserver.glyphvisualizer.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDirectionHistoryTest {
    @Test
    fun initialTrack_registersWithoutChangeOrTrustedEdge() {
        val result = TrackDirectionHistory().resolve("A", null, null)

        assertFalse(result.trackChanged)
        assertEquals(MediaTrackChangeDirection.UNKNOWN, result.resolvedDirection)
        assertTrue(result.trustedEdges.isEmpty())
    }

    @Test
    fun unknownTransitions_doNotCreateTrustedEdges() {
        val history = TrackDirectionHistory()
        history.resolve("A", null, null)

        val forwardUnknown = history.resolve("B", null, null)
        val returnUnknown = history.resolve("A", null, null)

        assertEquals(MediaTrackChangeDirection.UNKNOWN, forwardUnknown.resolvedDirection)
        assertEquals(MediaTrackChangeDirection.UNKNOWN, returnUnknown.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.UNKNOWN_FALLBACK, returnUnknown.directionSource)
        assertTrue(forwardUnknown.trustedEdges.isEmpty())
        assertTrue(returnUnknown.trustedEdges.isEmpty())
    }

    @Test
    fun trustedNextEdge_infersPreviousThenNext() {
        val history = TrackDirectionHistory()
        history.resolve("A", null, null)
        val learned = history.resolve(
            "B",
            MediaTrackChangeDirection.NEXT,
            MediaTrackDirectionSource.RAW_SKIP_HINT
        )

        val inferredPrevious = history.resolve("A", null, null)
        val inferredNext = history.resolve("B", null, null)

        assertEquals(listOf(TrustedTrackEdge("A", "B")), learned.trustedEdges)
        assertEquals(MediaTrackChangeDirection.PREVIOUS, inferredPrevious.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.TRUSTED_HISTORY, inferredPrevious.directionSource)
        assertEquals(MediaTrackChangeDirection.NEXT, inferredNext.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.TRUSTED_HISTORY, inferredNext.directionSource)
    }

    @Test
    fun explicitDirection_overridesConflictingTrustedEdge() {
        val history = TrackDirectionHistory()
        history.resolve("A", null, null)
        history.resolve(
            "B",
            MediaTrackChangeDirection.NEXT,
            MediaTrackDirectionSource.QUEUE_INDEX
        )

        val result = history.resolve(
            "A",
            MediaTrackChangeDirection.NEXT,
            MediaTrackDirectionSource.RAW_SKIP_HINT
        )

        assertEquals(MediaTrackChangeDirection.PREVIOUS, result.trustedHistoryDirection)
        assertEquals(MediaTrackChangeDirection.NEXT, result.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.RAW_SKIP_HINT, result.directionSource)
    }

    @Test
    fun sameTrack_doesNotChangeOrLearn() {
        val history = TrackDirectionHistory()
        history.resolve("A", null, null)

        val result = history.resolve("A", null, null)

        assertFalse(result.trackChanged)
        assertTrue(result.trustedEdges.isEmpty())
    }

    @Test
    fun trustedEdges_areBoundedDuringRapidChanges() {
        val history = TrackDirectionHistory(maxEdges = 3)
        history.resolve("A", null, null)
        listOf("B", "C", "D", "E").forEach { trackKey ->
            history.resolve(
                trackKey,
                MediaTrackChangeDirection.NEXT,
                MediaTrackDirectionSource.TRACK_NUMBER
            )
        }

        val result = history.resolve(
            "F",
            MediaTrackChangeDirection.NEXT,
            MediaTrackDirectionSource.TRACK_NUMBER
        )
        assertEquals(3, result.trustedEdges.size)
    }
}
