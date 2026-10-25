package jp.linkserver.glyphvisualizer.glyph

import android.media.session.PlaybackState
import jp.linkserver.glyphvisualizer.audio.MediaPlaybackActivityTracker
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackSnapshot
import jp.linkserver.glyphvisualizer.audio.MediaSessionPlaybackGate.PlaybackStatus

/** Normal Matrix only. Open Reel continues to use its existing service watchdog and timers. */
internal class MatrixPlaybackSessionPolicy {
    companion object {
        const val PAUSE_CONFIRM_MS = 400L
        const val TRACK_TRANSITION_MS = 1_500L
        const val MISSING_SESSION_MS = 750L
        const val BUFFERING_MS = 3_000L
        const val RESUME_CONFIRM_MS = 1_000L
    }

    enum class Decision(val retainSession: Boolean, val allowOutput: Boolean, val cancelTitle: Boolean) {
        PLAYING(true, true, false),
        TRANSITION(true, true, false),
        AUDIO_FALLBACK(false, true, true),
        PAUSED(false, false, true),
        STOPPED(false, false, true),
        NO_MEDIA(false, false, true),
        RESUME_CONFIRM(false, false, true),
    }

    // Reuse the generic PLAYING confirmation without entering any Open Reel branch.
    private val activityTracker = MediaPlaybackActivityTracker(RESUME_CONFIRM_MS, 0L, 0L)
    private var retained = false
    private var pausedSince: Long? = null
    private var missingSince: Long? = null
    private var transitionSince: Long? = null
    private var lastTrackKey: String? = null
    private var trackChangedAt: Long? = null
    private var lastNow: Long? = null

    fun evaluate(snapshot: PlaybackSnapshot, nowMs: Long, audioActive: Boolean, mediaPlaybackOnly: Boolean): Decision {
        if (lastNow?.let { nowMs < it } == true) reset()
        lastNow = nowMs
        if (!snapshot.sessionAccessAvailable) {
            reset()
            return if (mediaPlaybackOnly) Decision.NO_MEDIA else Decision.AUDIO_FALLBACK
        }
        val key = snapshot.trackKey
        val skip = snapshot.rawState == PlaybackState.STATE_SKIPPING_TO_NEXT ||
            snapshot.rawState == PlaybackState.STATE_SKIPPING_TO_PREVIOUS ||
            snapshot.rawState == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM
        if (skip || (key != null && lastTrackKey != null && key != lastTrackKey)) trackChangedAt = nowMs
        if (key != null) lastTrackKey = key
        val status = if (snapshot.rawState == PlaybackState.STATE_NONE) PlaybackStatus.NONE else snapshot.status
        if (status != PlaybackStatus.PAUSED) pausedSince = null
        if (status != PlaybackStatus.NONE) missingSince = null
        if (status == PlaybackStatus.PLAYING) {
            transitionSince = null
            val confirmed = !mediaPlaybackOnly || retained ||
                activityTracker.update(nowMs, true, false, false).allowed
            if (mediaPlaybackOnly && !retained && !confirmed) return Decision.RESUME_CONFIRM
            retained = true
            return Decision.PLAYING
        }
        if (status == PlaybackStatus.STOPPED) {
            retained = false
            transitionSince = null
            activityTracker.reset()
            return if (!mediaPlaybackOnly && audioActive) Decision.AUDIO_FALLBACK else Decision.STOPPED
        }
        val transitionStart = transitionSince ?: nowMs.also { transitionSince = it }
        when (status) {
            PlaybackStatus.BUFFERING -> {
                if (nowMs - transitionStart < BUFFERING_MS) {
                    retained = true
                    return Decision.TRANSITION
                }
            }
            PlaybackStatus.PAUSED -> if (retained) {
                val pauseStart = pausedSince ?: nowMs.also { pausedSince = it }
                val recentChange = trackChangedAt?.let { nowMs - it < TRACK_TRANSITION_MS } == true
                val pauseLimit = if (recentChange) TRACK_TRANSITION_MS else PAUSE_CONFIRM_MS
                val deadline = minOf(pauseStart + pauseLimit, transitionStart + BUFFERING_MS)
                if (nowMs < deadline) return Decision.TRANSITION
            }
            PlaybackStatus.NONE -> if (retained) {
                val missingStart = missingSince ?: nowMs.also { missingSince = it }
                if (nowMs - missingStart < MISSING_SESSION_MS && nowMs - transitionStart < BUFFERING_MS) {
                    return Decision.TRANSITION
                }
            }
            else -> Unit
        }
        retained = false
        activityTracker.reset()
        if (!mediaPlaybackOnly && (audioActive || status == PlaybackStatus.NONE)) return Decision.AUDIO_FALLBACK
        return if (status == PlaybackStatus.PAUSED) Decision.PAUSED else Decision.NO_MEDIA
    }

    fun reset() {
        retained = false
        pausedSince = null
        missingSince = null
        transitionSince = null
        lastTrackKey = null
        trackChangedAt = null
        lastNow = null
        activityTracker.reset()
    }
}
