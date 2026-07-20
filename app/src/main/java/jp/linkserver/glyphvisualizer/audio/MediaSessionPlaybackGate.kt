package jp.linkserver.glyphvisualizer.audio

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.SystemClock
import android.provider.Settings
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.MediaSessionNotificationListenerService

internal object MediaSessionPlaybackGate {
    private const val TAG = "MediaSessionPlaybackGate"
    private const val DEBUG_MEDIA_SESSION_LOGS = true
    private const val PERIODIC_LOG_INTERVAL_MS = 5_000L
    private const val SNAPSHOT_CACHE_MS = 250L
    private var lastLogAtMs = 0L
    private var lastLogSummary = ""
    private var cachedSnapshotAtMs = 0L
    private var cachedSnapshot = PlaybackSnapshot(PlaybackStatus.NONE, null)

    enum class PlaybackStatus {
        NONE,
        PLAYING,
        PAUSED,
        STOPPED
    }

    data class PlaybackSnapshot(
        val status: PlaybackStatus,
        val progress: Float?,
        val packageName: String? = null
    )

    fun hasNotificationAccess(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        val targetComponent = ComponentName(context, MediaSessionNotificationListenerService::class.java)
        return enabledListeners
            .split(':')
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it == targetComponent }
    }

    fun isMediaSessionPlaybackActive(context: Context): Boolean {
        if (!hasNotificationAccess(context)) {
            logStatus("notificationAccess=false result=false")
            return false
        }
        val manager = context.getSystemService(MediaSessionManager::class.java)
        if (manager == null) {
            logStatus("notificationAccess=true manager=null result=false")
            return false
        }
        val listener = ComponentName(context, MediaSessionNotificationListenerService::class.java)
        val sessions = runCatching { manager.getActiveSessions(listener) }.getOrElse { error ->
            logStatus("notificationAccess=true getActiveSessions=${error.javaClass.simpleName} result=false")
            return false
        }
        val result = sessions.any(::isSessionPlaying)
        val sessionSummary = sessions.joinToString(separator = ", ", prefix = "[", postfix = "]") { controller ->
            val state = controller.playbackState
            "${controller.packageName}:${stateName(state?.state)}"
        }
        logStatus(
            "notificationAccess=true sessions=${sessions.size} details=$sessionSummary result=$result"
        )
        return result
    }

    fun currentPlaybackProgress(context: Context): Float? {
        return currentPlaybackSnapshot(context).progress
    }

    fun currentPlaybackSnapshot(context: Context): PlaybackSnapshot {
        val now = SystemClock.elapsedRealtime()
        if (now - cachedSnapshotAtMs < SNAPSHOT_CACHE_MS) {
            return cachedSnapshot
        }
        cachedSnapshotAtMs = now
        cachedSnapshot = readPlaybackSnapshot(context, now)
        return cachedSnapshot
    }

    private fun isSessionPlaying(controller: MediaController): Boolean {
        val state = controller.playbackState ?: return false
        return state.state == PlaybackState.STATE_PLAYING
    }

    private fun readPlaybackSnapshot(context: Context, now: Long): PlaybackSnapshot {
        if (!hasNotificationAccess(context)) return PlaybackSnapshot(PlaybackStatus.NONE, null)
        val manager = context.getSystemService(MediaSessionManager::class.java)
            ?: return PlaybackSnapshot(PlaybackStatus.NONE, null)
        val listener = ComponentName(context, MediaSessionNotificationListenerService::class.java)
        val sessions = runCatching { manager.getActiveSessions(listener) }.getOrNull()
            ?: return PlaybackSnapshot(PlaybackStatus.NONE, null)
        val controller = sessions.firstOrNull(::isSessionPlaying)
            ?: sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
            ?: sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_STOPPED }
            ?: return PlaybackSnapshot(PlaybackStatus.NONE, null)
        val state = controller.playbackState
            ?: return PlaybackSnapshot(PlaybackStatus.NONE, null, controller.packageName)
        val status = when (state.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> PlaybackStatus.PLAYING
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING -> PlaybackStatus.PAUSED
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE,
            PlaybackState.STATE_ERROR -> PlaybackStatus.STOPPED
            else -> PlaybackStatus.NONE
        }
        val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
        if (duration == null || duration <= 0L) {
            return PlaybackSnapshot(status, null, controller.packageName)
        }

        val projectedPosition = if (status == PlaybackStatus.PLAYING && state.lastPositionUpdateTime > 0L) {
            val elapsed = (now - state.lastPositionUpdateTime).coerceAtLeast(0L)
            state.position + (elapsed * state.playbackSpeed).toLong()
        } else {
            state.position
        }
        return PlaybackSnapshot(
            status = status,
            progress = (projectedPosition / duration.toFloat()).coerceIn(0f, 1f),
            packageName = controller.packageName
        )
    }

    private fun logStatus(summary: String) {
        if (!DEBUG_MEDIA_SESSION_LOGS) return
        val now = SystemClock.uptimeMillis()
        if (summary == lastLogSummary && now - lastLogAtMs < PERIODIC_LOG_INTERVAL_MS) return
        lastLogSummary = summary
        lastLogAtMs = now
        AppLogger.i(TAG, summary)
    }

    private fun stateName(state: Int?): String {
        return when (state) {
            null -> "NONE"
            PlaybackState.STATE_NONE -> "NONE"
            PlaybackState.STATE_STOPPED -> "STOPPED"
            PlaybackState.STATE_PAUSED -> "PAUSED"
            PlaybackState.STATE_PLAYING -> "PLAYING"
            PlaybackState.STATE_FAST_FORWARDING -> "FAST_FORWARDING"
            PlaybackState.STATE_REWINDING -> "REWINDING"
            PlaybackState.STATE_BUFFERING -> "BUFFERING"
            PlaybackState.STATE_ERROR -> "ERROR"
            PlaybackState.STATE_CONNECTING -> "CONNECTING"
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> "SKIPPING_TO_PREVIOUS"
            PlaybackState.STATE_SKIPPING_TO_NEXT -> "SKIPPING_TO_NEXT"
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> "SKIPPING_TO_QUEUE_ITEM"
            else -> "UNKNOWN($state)"
        }
    }
}
