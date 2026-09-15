package jp.linkserver.glyphvisualizer.audio

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
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
    private var lastPlaybackStateLogSummary = ""
    private var cachedSnapshotAtMs = 0L
    private var cachedSnapshot = PlaybackSnapshot(PlaybackStatus.NONE, null)
    private val trackChangeDirectionResolver = MediaTrackChangeDirectionResolver()

    enum class PlaybackStatus {
        NONE,
        PLAYING,
        PAUSED,
        BUFFERING,
        STOPPED
    }

    data class PlaybackSnapshot(
        val status: PlaybackStatus,
        val progress: Float?,
        val packageName: String? = null,
        val durationMs: Long? = null,
        val trackKey: String? = null,
        val trackChangeDirection: MediaTrackChangeDirection? = null,
        val motionPaused: Boolean = false,
    ) {
        val activeForOpenReel: Boolean
            get() = status == PlaybackStatus.PLAYING ||
                status == PlaybackStatus.PAUSED ||
                status == PlaybackStatus.BUFFERING
    }

    internal data class PlaybackStateSemantics(
        val status: PlaybackStatus,
        val activeForOpenReel: Boolean,
        val motionPaused: Boolean,
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

    fun currentPlaybackSnapshot(
        context: Context,
        forceRefresh: Boolean = false,
    ): PlaybackSnapshot {
        val now = SystemClock.elapsedRealtime()
        if (!forceRefresh && now - cachedSnapshotAtMs < SNAPSHOT_CACHE_MS) {
            return cachedSnapshot
        }
        cachedSnapshotAtMs = now
        cachedSnapshot = readPlaybackSnapshot(context, now)
        return cachedSnapshot
    }

    private fun isSessionPlaying(controller: MediaController): Boolean {
        return playbackStatus(controller.playbackState?.state) == PlaybackStatus.PLAYING
    }

    private fun readPlaybackSnapshot(context: Context, now: Long): PlaybackSnapshot {
        if (!hasNotificationAccess(context)) return emptyPlaybackSnapshot()
        val manager = context.getSystemService(MediaSessionManager::class.java)
            ?: return emptyPlaybackSnapshot()
        val listener = ComponentName(context, MediaSessionNotificationListenerService::class.java)
        val sessions = runCatching { manager.getActiveSessions(listener) }.getOrNull()
            ?: return emptyPlaybackSnapshot()
        val controller = sessions.firstOrNull(::isSessionPlaying)
            ?: sessions.firstOrNull {
                playbackStateSemantics(it.playbackState?.state).activeForOpenReel
            }
            ?: sessions.firstOrNull {
                playbackStateSemantics(it.playbackState?.state).status == PlaybackStatus.STOPPED
            }
            ?: return emptyPlaybackSnapshot()
        val state = controller.playbackState
            ?: return emptyPlaybackSnapshot(controller.packageName)
        val semantics = playbackStateSemantics(state.state)
        val status = semantics.status
        val metadata = controller.metadata
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.takeIf { it > 0L }
        val trackKey = buildTrackKey(controller.packageName, metadata, duration)
        val rawDirection = when (state.state) {
            PlaybackState.STATE_SKIPPING_TO_NEXT -> MediaTrackChangeDirection.NEXT
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS -> MediaTrackChangeDirection.PREVIOUS
            else -> null
        }
        val directionResolution = trackChangeDirectionResolver.resolve(
            nowMs = now,
            packageName = controller.packageName,
            rawDirection = rawDirection,
            trackKey = trackKey,
            queueIndex = activeQueueIndex(controller, state.activeQueueItemId),
            trackNumber = metadata?.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER)?.takeIf { it > 0L }
        )
        if (directionResolution.trackChanged) {
            logTrackChangeDirection(directionResolution)
        }
        val projectedPosition = if (
            duration != null &&
            status == PlaybackStatus.PLAYING &&
            state.lastPositionUpdateTime > 0L
        ) {
            val elapsed = (now - state.lastPositionUpdateTime).coerceAtLeast(0L)
            state.position + (elapsed * state.playbackSpeed).toLong()
        } else {
            state.position
        }
        return PlaybackSnapshot(
            status = status,
            progress = duration?.let {
                (projectedPosition / it.toFloat()).coerceIn(0f, 1f)
            },
            packageName = controller.packageName,
            durationMs = duration,
            trackKey = trackKey,
            trackChangeDirection = directionResolution.resolvedDirection,
            motionPaused = semantics.motionPaused,
        ).also { snapshot ->
            logPlaybackState(state.state, snapshot)
        }
    }

    private fun logTrackChangeDirection(resolution: MediaTrackDirectionResolution) {
        if (!DEBUG_MEDIA_SESSION_LOGS) return
        val trustedEdgesSummary = resolution.trustedEdges.joinToString(prefix = "[", postfix = "]") {
            "${debugTrackKey(it.previousTrackKey)}->${debugTrackKey(it.nextTrackKey)}"
        }
        AppLogger.i(
            TAG,
            "trackChange oldTrackKey=${debugTrackKey(resolution.oldTrackKey)} " +
                "newTrackKey=${debugTrackKey(resolution.newTrackKey)} " +
                "rawSkipHint=${resolution.rawSkipHint} " +
                "queueDirection=${resolution.queueDirection} " +
                "trackNumberDirection=${resolution.trackNumberDirection} " +
                "trustedEdges=$trustedEdgesSummary " +
                "trustedHistoryDirection=${resolution.trustedHistoryDirection} " +
                "resolvedSemanticDirection=${resolution.resolvedDirection} " +
                "animationDirection=${resolution.animationDirection} " +
                "directionSource=${resolution.directionSource}"
        )
    }

    private fun debugTrackKey(trackKey: String?): String {
        return trackKey?.replace('\u001f', '|') ?: "null"
    }

    private fun activeQueueIndex(controller: MediaController, activeQueueItemId: Long): Int? {
        if (activeQueueItemId == MediaSession.QueueItem.UNKNOWN_ID.toLong()) return null
        return runCatching {
            controller.queue?.indexOfFirst { it.queueId == activeQueueItemId }
        }.getOrNull()?.takeIf { it >= 0 }
    }

    private fun buildTrackKey(
        packageName: String,
        metadata: MediaMetadata?,
        durationMs: Long?
    ): String? {
        metadata ?: return null
        metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            ?.takeIf { it.isNotBlank() }
            ?.let { return "$packageName\u001fmedia-id\u001f$it" }

        val title = firstNonBlank(
            metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
            metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ) ?: return null
        val artist = firstNonBlank(
            metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
            metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST),
            metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        )
        val album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?.takeIf { it.isNotBlank() }
        if (durationMs == null || (artist == null && album == null)) return null
        return listOf(
            packageName,
            "metadata",
            title,
            artist.orEmpty(),
            album.orEmpty(),
            durationMs.toString()
        ).joinToString("\u001f")
    }

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }
    }

    internal fun playbackStateSemantics(state: Int?): PlaybackStateSemantics {
        return when (state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> PlaybackStateSemantics(
                status = PlaybackStatus.PLAYING,
                activeForOpenReel = true,
                motionPaused = false,
            )

            PlaybackState.STATE_PAUSED -> PlaybackStateSemantics(
                status = PlaybackStatus.PAUSED,
                activeForOpenReel = true,
                motionPaused = true,
            )

            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_CONNECTING -> PlaybackStateSemantics(
                status = PlaybackStatus.BUFFERING,
                activeForOpenReel = true,
                motionPaused = false,
            )

            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_NONE,
            PlaybackState.STATE_ERROR -> PlaybackStateSemantics(
                status = PlaybackStatus.STOPPED,
                activeForOpenReel = false,
                motionPaused = false,
            )

            else -> PlaybackStateSemantics(
                status = PlaybackStatus.NONE,
                activeForOpenReel = false,
                motionPaused = false,
            )
        }
    }

    private fun playbackStatus(state: Int?): PlaybackStatus {
        return playbackStateSemantics(state).status
    }

    private fun emptyPlaybackSnapshot(packageName: String? = null): PlaybackSnapshot {
        return PlaybackSnapshot(
            status = PlaybackStatus.NONE,
            progress = null,
            packageName = packageName,
        ).also { snapshot ->
            logPlaybackState(null, snapshot)
        }
    }

    private fun logPlaybackState(rawState: Int?, snapshot: PlaybackSnapshot) {
        if (!DEBUG_MEDIA_SESSION_LOGS) return
        val summary = "rawPlaybackState=${stateName(rawState)}($rawState) " +
            "mappedPlaybackStatus=${snapshot.status} " +
            "motionPaused=${snapshot.motionPaused} " +
            "trackKey=${debugTrackKey(snapshot.trackKey)} " +
            "packageName=${snapshot.packageName}"
        if (summary == lastPlaybackStateLogSummary) return
        lastPlaybackStateLogSummary = summary
        AppLogger.i(TAG, summary)
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
