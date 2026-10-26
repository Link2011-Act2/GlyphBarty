package jp.linkserver.glyphvisualizer.audio

/** Owned by the Matrix render thread. Missing metadata must not manufacture track changes. */
internal class MediaTrackTitleChangeDetector {
    companion object {
        // Covers the observed ~780ms replacement plus the MediaSession snapshot cache interval.
        const val BROWSER_TITLE_SETTLE_MS = 1_200L
        private val browserPackages = setOf(
            "com.brave.browser", "com.brave.browser_beta", "com.brave.browser_nightly",
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
            "org.chromium.chrome", "com.microsoft.emmx", "com.microsoft.emmx.beta",
            "com.microsoft.emmx.dev", "com.microsoft.emmx.canary",
            "com.vivaldi.browser", "com.vivaldi.browser.snapshot",
            "com.sec.android.app.sbrowser", "com.sec.android.app.sbrowser.beta",
            "com.opera.browser", "com.opera.browser.beta"
        )
    }

    private var previous: MediaSessionPlaybackGate.PlaybackSnapshot? = null
    private var pendingBrowserTitle: BrowserTitle? = null
    private var pendingSinceMs = 0L
    val awaitingBrowserTitle: Boolean get() = pendingBrowserTitle != null

    private data class BrowserTitle(val packageName: String?, val mediaId: String?, val title: String, val artist: String?)

    fun reset() {
        previous = null
        clearPending()
    }

    fun clearPending() { pendingBrowserTitle = null }

    fun observe(snapshot: MediaSessionPlaybackGate.PlaybackSnapshot, nowMs: Long = 0L): Boolean {
        if (!snapshot.activeForOpenReel) {
            reset()
            return false
        }
        val old = previous
        if (old == null) {
            previous = snapshot
            clearPending()
            return false // Already playing at startup, enabling, or reconnection.
        }
        if (snapshot.status != MediaSessionPlaybackGate.PlaybackStatus.PLAYING) {
            clearPending()
            return false
        }
        val browser = snapshot.packageName in browserPackages
        val oldId = old.trackKey?.takeIf { it.contains("\u001fmedia-id\u001f") }
        val newId = snapshot.trackKey?.takeIf { it.contains("\u001fmedia-id\u001f") }
        val changed = when {
            old.packageName != snapshot.packageName -> true
            oldId != null && newId != null -> oldId != newId ||
                (browser && old.title != null && snapshot.title != null && old.title != snapshot.title)
            old.title != null && snapshot.title != null -> old.title != snapshot.title
            old.trackKey != null && snapshot.trackKey != null -> old.trackKey != snapshot.trackKey
            else -> false
        }
        if (browser && changed) {
            val title = snapshot.title?.takeIf { it.isNotBlank() }
            if (title == null) {
                clearPending()
                return false
            }
            val candidate = BrowserTitle(snapshot.packageName, newId, title, snapshot.artist)
            if (pendingBrowserTitle != candidate || nowMs < pendingSinceMs) {
                pendingBrowserTitle = candidate
                pendingSinceMs = nowMs
                return false
            }
            if (nowMs - pendingSinceMs < BROWSER_TITLE_SETTLE_MS) return false
        }
        clearPending()
        // Keep the last complete identity across transient metadata loss; accept enrichment
        // without comparing duration/album/artwork updates as a new track.
        if (snapshot.title != null || changed) previous = snapshot
        return changed
    }
}
