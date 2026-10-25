package jp.linkserver.glyphvisualizer.audio

/** Owned by the Matrix render thread. Missing metadata must not manufacture track changes. */
internal class MediaTrackTitleChangeDetector {
    private var previous: MediaSessionPlaybackGate.PlaybackSnapshot? = null

    fun reset() { previous = null }

    fun observe(snapshot: MediaSessionPlaybackGate.PlaybackSnapshot): Boolean {
        if (!snapshot.activeForOpenReel) {
            reset()
            return false
        }
        val old = previous
        if (old == null) {
            previous = snapshot
            return false // Already playing at startup, enabling, or reconnection.
        }
        if (snapshot.status != MediaSessionPlaybackGate.PlaybackStatus.PLAYING) return false
        val oldId = old.trackKey?.takeIf { it.contains("\u001fmedia-id\u001f") }
        val newId = snapshot.trackKey?.takeIf { it.contains("\u001fmedia-id\u001f") }
        val changed = when {
            old.packageName != snapshot.packageName -> true
            oldId != null && newId != null -> oldId != newId
            old.title != null && snapshot.title != null -> old.title != snapshot.title
            old.trackKey != null && snapshot.trackKey != null -> old.trackKey != snapshot.trackKey
            else -> false
        }
        // Keep the last complete identity across transient metadata loss; accept enrichment
        // without comparing duration/album/artwork updates as a new track.
        if (snapshot.title != null || changed) previous = snapshot
        return changed
    }
}
