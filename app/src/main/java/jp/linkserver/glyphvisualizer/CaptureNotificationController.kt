package jp.linkserver.glyphvisualizer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

internal class CaptureNotificationController(private val service: Service) {
    companion object {
        const val CHANNEL_ID = "glyph_visualizer"
        const val NOTIFICATION_ID = 42
        const val ALERT_CHANNEL_ID = "glyph_visualizer_alerts"
        const val ALERT_NOTIFICATION_ID = 43
        private const val TAG = "GlyphVisualizerSvc"
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = service.getSystemService(NotificationManager::class.java)
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            service.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            service.getString(R.string.notification_alert_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = service.getString(R.string.notification_alert_channel_description)
        }
        manager.createNotificationChannels(listOf(serviceChannel, alertChannel))
    }

    fun startForeground(label: String, mediaProjection: Boolean = false) {
        val notification = buildNotification(label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (mediaProjection) {
                ServiceInfoCompat.mediaProjectionType()
            } else {
                ServiceInfoCompat.mediaPlaybackType()
            }
            service.startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            service.startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun stopForegroundSafely() {
        try {
            service.stopForeground(Service.STOP_FOREGROUND_REMOVE)
        } catch (error: Throwable) {
            AppLogger.w(TAG, "stopForeground failed (already stopped or invalid state)", error)
        }
    }

    fun cancelSpatialAudioWarning() {
        service.getSystemService(NotificationManager::class.java)?.cancel(ALERT_NOTIFICATION_ID)
    }

    fun showSpatialAudioWarning(message: String): Boolean {
        if (service.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            AppLogger.w(TAG, "Spatial Audio warning notification skipped: notification permission missing")
            return false
        }
        val manager = service.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.areNotificationsEnabled()) {
            AppLogger.w(TAG, "Spatial Audio warning notification skipped: notifications disabled")
            return false
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(ALERT_CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE
        ) {
            AppLogger.w(TAG, "Spatial Audio warning notification skipped: alert channel disabled")
            return false
        }

        val openAppIntent = Intent(service, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            service,
            ALERT_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(service, ALERT_CHANNEL_ID)
            .setContentTitle(service.getString(R.string.spatial_audio_warning_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .build()
        manager.notify(ALERT_NOTIFICATION_ID, notification)
        return true
    }

    private fun buildNotification(label: String): Notification {
        return NotificationCompat.Builder(service, CHANNEL_ID)
            .setContentTitle(service.getString(R.string.notification_title))
            .setContentText(label)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }
}

private object ServiceInfoCompat {
    fun mediaProjectionType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfoTypes.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }
    }

    fun mediaPlaybackType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfoTypes.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
    }
}

private object ServiceInfoTypes {
    const val FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION = 32
    const val FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = 2
}
