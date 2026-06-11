package jp.linkserver.glyphvisualizer.audio

import android.content.ComponentName
import android.content.Context
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
    private var lastLogAtMs = 0L
    private var lastLogSummary = ""

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

    private fun isSessionPlaying(controller: MediaController): Boolean {
        val state = controller.playbackState ?: return false
        return state.state == PlaybackState.STATE_PLAYING
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
