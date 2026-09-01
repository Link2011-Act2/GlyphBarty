package jp.linkserver.glyphvisualizer.glyph

/**
 * Serializes access to the process-wide Nothing Glyph SDK singleton.
 * The visualizer always has priority; a battery animation is preempted synchronously.
 */
internal object GlyphSdkSessionCoordinator {
    private sealed interface Owner {
        val token: Any

        data class Visualizer(override val token: Any) : Owner
        data class Battery(
            override val token: Any,
            val onPreempt: () -> Unit
        ) : Owner
    }

    private val lock = Any()
    private var owner: Owner? = null

    fun claimVisualizer(token: Any) {
        val preempt = synchronized(lock) {
            val current = owner
            if (current is Owner.Visualizer && current.token === token) return
            owner = Owner.Visualizer(token)
            (current as? Owner.Battery)?.onPreempt
        }
        preempt?.invoke()
    }

    fun releaseVisualizer(token: Any) {
        synchronized(lock) {
            val current = owner
            if (current is Owner.Visualizer && current.token === token) owner = null
        }
    }

    fun tryClaimBattery(token: Any, onPreempt: () -> Unit): Boolean {
        return synchronized(lock) {
            if (owner != null) return@synchronized false
            owner = Owner.Battery(token, onPreempt)
            true
        }
    }

    fun releaseBattery(token: Any) {
        synchronized(lock) {
            val current = owner
            if (current is Owner.Battery && current.token === token) owner = null
        }
    }
}
