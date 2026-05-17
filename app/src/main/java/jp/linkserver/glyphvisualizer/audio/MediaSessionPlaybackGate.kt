package jp.linkserver.glyphvisualizer.audio

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings
import jp.linkserver.glyphvisualizer.MediaSessionNotificationListenerService

internal object MediaSessionPlaybackGate {
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
        if (!hasNotificationAccess(context)) return false
        val manager = context.getSystemService(MediaSessionManager::class.java) ?: return false
        val listener = ComponentName(context, MediaSessionNotificationListenerService::class.java)
        val sessions = runCatching { manager.getActiveSessions(listener) }.getOrElse { return false }
        return sessions.any(::isSessionPlaying)
    }

    private fun isSessionPlaying(controller: MediaController): Boolean {
        val state = controller.playbackState ?: return false
        return state.state == android.media.session.PlaybackState.STATE_PLAYING
    }
}
