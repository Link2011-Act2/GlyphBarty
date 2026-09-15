package jp.linkserver.glyphvisualizer.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaTrackChangeDirectionResolverTest {
    @Test
    fun appleMusicUnknownSequence_neverLearnsFalsePrevious() {
        val resolver = MediaTrackChangeDirectionResolver()
        val initial = observe(resolver, nowMs = 1_000L, trackKey = "6")
        val sixToFive = observe(resolver, nowMs = 1_100L, trackKey = "5")
        val fiveToFour = observe(resolver, nowMs = 1_200L, trackKey = "4")
        val fourToFive = observe(resolver, nowMs = 1_300L, trackKey = "5")

        assertFalse(initial.trackChanged)
        listOf(sixToFive, fiveToFour, fourToFive).forEach { result ->
            assertEquals(MediaTrackChangeDirection.UNKNOWN, result.resolvedDirection)
            assertEquals(MediaTrackChangeDirection.NEXT, result.animationDirection)
            assertEquals(MediaTrackDirectionSource.UNKNOWN_FALLBACK, result.directionSource)
            assertTrue(result.trustedEdges.isEmpty())
        }
    }

    @Test
    fun transportHint_isRetainedAndHasPriorityOverQueueTrackAndHistory() {
        val resolver = MediaTrackChangeDirectionResolver()
        observe(resolver, nowMs = 1_000L, trackKey = "a", queueIndex = 1, trackNumber = 2)
        observe(
            resolver,
            nowMs = 1_100L,
            trackKey = "a",
            rawDirection = MediaTrackChangeDirection.NEXT,
            queueIndex = 1,
            trackNumber = 2
        )

        val result = observe(
            resolver,
            nowMs = 1_500L,
            trackKey = "b",
            queueIndex = 0,
            trackNumber = 1
        )

        assertEquals(MediaTrackChangeDirection.NEXT, result.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.RAW_SKIP_HINT, result.directionSource)
        assertEquals(MediaTrackChangeDirection.PREVIOUS, result.queueDirection)
        assertEquals(MediaTrackChangeDirection.PREVIOUS, result.trackNumberDirection)
    }

    @Test
    fun queueThenTrackNumber_keepPriorityOverTrustedHistory() {
        val queueResolver = resolverWithTrustedNextEdge()
        val queueResult = observe(
            queueResolver,
            nowMs = 1_200L,
            trackKey = "a",
            queueIndex = 4,
            trackNumber = 2
        )
        assertEquals(MediaTrackChangeDirection.PREVIOUS, queueResult.trustedHistoryDirection)
        assertEquals(MediaTrackChangeDirection.NEXT, queueResult.queueDirection)
        assertEquals(MediaTrackChangeDirection.NEXT, queueResult.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.QUEUE_INDEX, queueResult.directionSource)

        val trackResolver = resolverWithTrustedNextEdge()
        val trackResult = observe(
            trackResolver,
            nowMs = 1_200L,
            trackKey = "a",
            trackNumber = 4
        )
        assertEquals(MediaTrackChangeDirection.PREVIOUS, trackResult.trustedHistoryDirection)
        assertEquals(MediaTrackChangeDirection.NEXT, trackResult.trackNumberDirection)
        assertEquals(MediaTrackChangeDirection.NEXT, trackResult.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.TRACK_NUMBER, trackResult.directionSource)
    }

    @Test
    fun trustedHistory_infersReverseAndForwardAfterExplicitNext() {
        val resolver = MediaTrackChangeDirectionResolver()
        observe(resolver, nowMs = 1_000L, trackKey = "a")
        observe(
            resolver,
            nowMs = 1_100L,
            trackKey = "b",
            rawDirection = MediaTrackChangeDirection.NEXT
        )

        val back = observe(resolver, nowMs = 1_200L, trackKey = "a")
        val forward = observe(resolver, nowMs = 1_300L, trackKey = "b")

        assertEquals(MediaTrackChangeDirection.PREVIOUS, back.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.TRUSTED_HISTORY, back.directionSource)
        assertEquals(MediaTrackChangeDirection.NEXT, forward.resolvedDirection)
        assertEquals(MediaTrackDirectionSource.TRUSTED_HISTORY, forward.directionSource)
    }

    @Test
    fun sameTrackDoesNotChangeHistory() {
        val resolver = MediaTrackChangeDirectionResolver()
        val initial = observe(resolver, nowMs = 1_000L, trackKey = "a")
        val same = observe(resolver, nowMs = 1_100L, trackKey = "a")

        assertFalse(initial.trackChanged)
        assertFalse(same.trackChanged)
        assertTrue(same.trustedEdges.isEmpty())
    }

    private fun resolverWithTrustedNextEdge(): MediaTrackChangeDirectionResolver {
        return MediaTrackChangeDirectionResolver().also { resolver ->
            observe(resolver, nowMs = 1_000L, trackKey = "a", queueIndex = 1, trackNumber = 1)
            observe(
                resolver,
                nowMs = 1_100L,
                trackKey = "b",
                rawDirection = MediaTrackChangeDirection.NEXT,
                queueIndex = 3,
                trackNumber = 3
            )
        }
    }

    private fun observe(
        resolver: MediaTrackChangeDirectionResolver,
        nowMs: Long,
        trackKey: String,
        rawDirection: MediaTrackChangeDirection? = null,
        queueIndex: Int? = null,
        trackNumber: Long? = null
    ): MediaTrackDirectionResolution {
        return resolver.resolve(
            nowMs = nowMs,
            packageName = "player",
            rawDirection = rawDirection,
            trackKey = trackKey,
            queueIndex = queueIndex,
            trackNumber = trackNumber
        )
    }
}
