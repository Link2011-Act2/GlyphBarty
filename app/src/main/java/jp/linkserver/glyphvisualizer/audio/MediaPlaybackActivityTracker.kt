package jp.linkserver.glyphvisualizer.audio

internal class MediaPlaybackActivityTracker(
    private val resumeConfirmMs: Long,
    private val openReelGraceMs: Long,
    private val openReelPausedHoldMs: Long,
) {
    internal enum class Event {
        OPEN_REEL_GRACE_STARTED,
        OPEN_REEL_GRACE_KEEPING_ALIVE,
        OPEN_REEL_GRACE_RECOVERED,
        OPEN_REEL_GRACE_EXPIRED,
        OPEN_REEL_PAUSED_HOLD_STARTED,
        OPEN_REEL_PAUSED_HOLD_CLEARED,
        OPEN_REEL_PAUSED_HOLD_EXPIRED,
        PLAYBACK_RESUMED_CONFIRMED,
    }

    internal data class Result(
        val allowed: Boolean,
        val events: List<Event> = emptyList(),
    )

    var lastMediaPlaybackActive: Boolean = false
        private set

    var mediaPlaybackResumeCandidateAtMs: Long = 0L
        private set

    var openReelMediaSessionMissingSinceMs: Long = 0L
        private set

    var openReelPausedSinceMs: Long = 0L
        private set

    private var openReelGraceKeepAliveLogged: Boolean = false
    private var openReelPausedHoldExpired: Boolean = false

    fun update(
        nowMs: Long,
        rawMediaPlaybackActive: Boolean,
        allowPaused: Boolean,
        openReelNonPlayingSessionActive: Boolean,
        openReelMotionPaused: Boolean = false,
    ): Result {
        val events = mutableListOf<Event>()

        if (!rawMediaPlaybackActive) {
            clearOpenReelPausedHold(events)
            if (allowPaused && lastMediaPlaybackActive) {
                if (openReelMediaSessionMissingSinceMs == 0L) {
                    openReelMediaSessionMissingSinceMs = nowMs
                    openReelGraceKeepAliveLogged = false
                    events += Event.OPEN_REEL_GRACE_STARTED
                }

                if (nowMs - openReelMediaSessionMissingSinceMs < openReelGraceMs) {
                    if (!openReelGraceKeepAliveLogged &&
                        nowMs > openReelMediaSessionMissingSinceMs
                    ) {
                        openReelGraceKeepAliveLogged = true
                        events += Event.OPEN_REEL_GRACE_KEEPING_ALIVE
                    }
                    return Result(allowed = true, events = events)
                }

                clearOpenReelGrace()
                lastMediaPlaybackActive = false
                mediaPlaybackResumeCandidateAtMs = 0L
                events += Event.OPEN_REEL_GRACE_EXPIRED
                return Result(allowed = false, events = events)
            }

            clearOpenReelGrace()
            lastMediaPlaybackActive = false
            mediaPlaybackResumeCandidateAtMs = 0L
            return Result(allowed = false)
        }

        if (allowPaused &&
            openReelMediaSessionMissingSinceMs != 0L &&
            lastMediaPlaybackActive
        ) {
            events += Event.OPEN_REEL_GRACE_RECOVERED
        }
        clearOpenReelGrace()

        if (allowPaused && openReelMotionPaused) {
            if (openReelPausedHoldExpired) {
                return Result(allowed = false, events = events)
            }
            if (openReelPausedSinceMs == 0L) {
                openReelPausedSinceMs = nowMs
                events += Event.OPEN_REEL_PAUSED_HOLD_STARTED
            }
            if (nowMs - openReelPausedSinceMs >= openReelPausedHoldMs) {
                openReelPausedSinceMs = 0L
                openReelPausedHoldExpired = true
                lastMediaPlaybackActive = false
                mediaPlaybackResumeCandidateAtMs = 0L
                events += Event.OPEN_REEL_PAUSED_HOLD_EXPIRED
                return Result(allowed = false, events = events)
            }
            lastMediaPlaybackActive = true
            mediaPlaybackResumeCandidateAtMs = 0L
            return Result(allowed = true, events = events)
        }

        clearOpenReelPausedHold(events)
        if (allowPaused && openReelNonPlayingSessionActive) {
            lastMediaPlaybackActive = true
            mediaPlaybackResumeCandidateAtMs = 0L
        } else if (!lastMediaPlaybackActive) {
            if (mediaPlaybackResumeCandidateAtMs == 0L) {
                mediaPlaybackResumeCandidateAtMs = nowMs
            }
            if (nowMs - mediaPlaybackResumeCandidateAtMs >= resumeConfirmMs) {
                lastMediaPlaybackActive = true
                mediaPlaybackResumeCandidateAtMs = 0L
                events += Event.PLAYBACK_RESUMED_CONFIRMED
            }
        } else {
            mediaPlaybackResumeCandidateAtMs = 0L
        }

        return Result(allowed = lastMediaPlaybackActive, events = events)
    }

    fun reset() {
        lastMediaPlaybackActive = false
        mediaPlaybackResumeCandidateAtMs = 0L
        clearOpenReelGrace()
        openReelPausedSinceMs = 0L
        openReelPausedHoldExpired = false
    }

    private fun clearOpenReelGrace() {
        openReelMediaSessionMissingSinceMs = 0L
        openReelGraceKeepAliveLogged = false
    }

    private fun clearOpenReelPausedHold(events: MutableList<Event>) {
        if (openReelPausedSinceMs != 0L || openReelPausedHoldExpired) {
            events += Event.OPEN_REEL_PAUSED_HOLD_CLEARED
        }
        openReelPausedSinceMs = 0L
        openReelPausedHoldExpired = false
    }
}
