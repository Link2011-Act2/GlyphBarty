package jp.linkserver.glyphvisualizer.audio

internal class MediaPlaybackActivityTracker(
    private val resumeConfirmMs: Long,
    private val openReelGraceMs: Long,
) {
    internal enum class Event {
        OPEN_REEL_GRACE_STARTED,
        OPEN_REEL_GRACE_KEEPING_ALIVE,
        OPEN_REEL_GRACE_RECOVERED,
        OPEN_REEL_GRACE_EXPIRED,
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

    private var openReelGraceKeepAliveLogged: Boolean = false

    fun update(
        nowMs: Long,
        rawMediaPlaybackActive: Boolean,
        allowPaused: Boolean,
        pausedPlayback: Boolean,
    ): Result {
        val events = mutableListOf<Event>()

        if (!rawMediaPlaybackActive) {
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

        if (allowPaused && pausedPlayback) {
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
    }

    private fun clearOpenReelGrace() {
        openReelMediaSessionMissingSinceMs = 0L
        openReelGraceKeepAliveLogged = false
    }
}
