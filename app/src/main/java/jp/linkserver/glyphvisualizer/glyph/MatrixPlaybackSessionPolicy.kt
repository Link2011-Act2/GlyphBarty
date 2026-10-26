package jp.linkserver.glyphvisualizer.glyph

import android.media.session.PlaybackState
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
        // Suppress pixels while confirming playback without closing an already healthy session.
        RESUME_CONFIRM(true, false, true),
    }

    // A proposal owns a copy of all normal-Matrix state. Discarding a stale snapshot's
    // proposal must not consume confirmation, retention or transition history.
    internal data class State(
        var retained: Boolean = false,
        var confirmingResume: Boolean = false,
        var resumeCandidateAtMs: Long? = null,
        var pausedSince: Long? = null,
        var missingSince: Long? = null,
        var transitionSince: Long? = null,
        var lastTrackKey: String? = null,
        var trackChangedAt: Long? = null,
        var lastNow: Long? = null,
        var nextEvaluationAtMs: Long? = null,
    )
    internal class Evaluation(val decision: Decision, internal val state: State)
    private var state = State()
    val nextEvaluationAtMs: Long? get() = state.nextEvaluationAtMs

    fun evaluate(snapshot: PlaybackSnapshot, nowMs: Long, audioActive: Boolean, mediaPlaybackOnly: Boolean): Decision =
        commit(prepare(snapshot, nowMs, audioActive, mediaPlaybackOnly))

    fun prepare(snapshot: PlaybackSnapshot, nowMs: Long, audioActive: Boolean, mediaPlaybackOnly: Boolean): Evaluation {
        val candidate = if (!snapshot.sessionAccessAvailable || state.lastNow?.let { nowMs < it } == true) {
            State()
        } else state.copy()
        val decision = if (!snapshot.sessionAccessAvailable) {
            if (mediaPlaybackOnly) Decision.NO_MEDIA else Decision.AUDIO_FALLBACK
        } else decide(candidate, snapshot, nowMs, audioActive, mediaPlaybackOnly)
        return Evaluation(decision, candidate)
    }

    fun commit(evaluation: Evaluation): Decision {
        state = evaluation.state
        return evaluation.decision
    }

    private fun decide(state: State, snapshot: PlaybackSnapshot, nowMs: Long,
        audioActive: Boolean, mediaPlaybackOnly: Boolean): Decision = with(state) {
        lastNow = nowMs
        nextEvaluationAtMs = null
        if (!mediaPlaybackOnly) confirmingResume = false
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
            val confirmed = !mediaPlaybackOnly || retained || run {
                val since = resumeCandidateAtMs ?: nowMs.also { resumeCandidateAtMs = it }
                nowMs - since >= RESUME_CONFIRM_MS
            }
            if (mediaPlaybackOnly && !retained && !confirmed) {
                confirmingResume = true
                nextEvaluationAtMs = requireNotNull(resumeCandidateAtMs) + RESUME_CONFIRM_MS
                return Decision.RESUME_CONFIRM
            }
            confirmingResume = false
            resumeCandidateAtMs = null
            retained = true
            return Decision.PLAYING
        }
        if (status == PlaybackStatus.STOPPED) {
            retained = false
            confirmingResume = false
            transitionSince = null
            resumeCandidateAtMs = null
            return if (!mediaPlaybackOnly && audioActive) Decision.AUDIO_FALLBACK else Decision.STOPPED
        }
        val transitionStart = transitionSince ?: nowMs.also { transitionSince = it }
        // A brief interruption must not verify playback or discard the held SDK session.
        if (confirmingResume) resumeCandidateAtMs = null
        val transition = if (confirmingResume) Decision.RESUME_CONFIRM else Decision.TRANSITION
        when (status) {
            PlaybackStatus.BUFFERING -> {
                if (nowMs - transitionStart < BUFFERING_MS) {
                    if (!confirmingResume) retained = true
                    nextEvaluationAtMs = transitionStart + BUFFERING_MS
                    return transition
                }
            }
            PlaybackStatus.PAUSED -> if (retained || confirmingResume) {
                val pauseStart = pausedSince ?: nowMs.also { pausedSince = it }
                val recentChange = trackChangedAt?.let { nowMs - it < TRACK_TRANSITION_MS } == true
                val pauseLimit = if (recentChange) TRACK_TRANSITION_MS else PAUSE_CONFIRM_MS
                val deadline = minOf(pauseStart + pauseLimit, transitionStart + BUFFERING_MS)
                if (nowMs < deadline) {
                    nextEvaluationAtMs = deadline
                    return transition
                }
            }
            PlaybackStatus.NONE -> if (retained || confirmingResume) {
                val missingStart = missingSince ?: nowMs.also { missingSince = it }
                if (nowMs - missingStart < MISSING_SESSION_MS && nowMs - transitionStart < BUFFERING_MS) {
                    nextEvaluationAtMs = minOf(missingStart + MISSING_SESSION_MS, transitionStart + BUFFERING_MS)
                    return transition
                }
            }
            else -> Unit
        }
        retained = false
        confirmingResume = false
        resumeCandidateAtMs = null
        if (!mediaPlaybackOnly && (audioActive || status == PlaybackStatus.NONE)) return Decision.AUDIO_FALLBACK
        return if (status == PlaybackStatus.PAUSED) Decision.PAUSED else Decision.NO_MEDIA
    }

    fun reset() {
        state = State()
    }
}
