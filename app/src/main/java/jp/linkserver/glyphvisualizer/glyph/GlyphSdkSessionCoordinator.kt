package jp.linkserver.glyphvisualizer.glyph

/**
 * Serializes access to the process-wide Nothing Glyph SDK singleton.
 * A battery animation has priority and temporarily suspends the visualizer session.
 */
internal object GlyphSdkSessionCoordinator {
    private sealed interface Owner {
        val token: Any

        data class Visualizer(
            override val token: Any,
            val onSuspend: () -> Unit,
            val onResume: () -> Unit
        ) : Owner

        data class Battery(override val token: Any) : Owner
    }

    private val lock = Any()
    private var owner: Owner? = null
    private var suspendedVisualizer: Owner.Visualizer? = null

    fun claimVisualizer(
        token: Any,
        onSuspend: () -> Unit,
        onResume: () -> Unit
    ): Boolean {
        var previousVisualizerSuspend: (() -> Unit)? = null
        val granted = synchronized(lock) {
            val visualizer = Owner.Visualizer(token, onSuspend, onResume)
            val current = owner
            when {
                current is Owner.Battery -> {
                    suspendedVisualizer = visualizer
                    false
                }

                current is Owner.Visualizer && current.token === token -> {
                    owner = visualizer
                    true
                }

                else -> {
                    previousVisualizerSuspend = (current as? Owner.Visualizer)?.onSuspend
                    owner = visualizer
                    suspendedVisualizer = null
                    true
                }
            }
        }
        previousVisualizerSuspend?.invoke()
        return granted
    }

    fun releaseVisualizer(token: Any) {
        synchronized(lock) {
            val current = owner
            if (current is Owner.Visualizer && current.token === token) owner = null
            if (suspendedVisualizer?.token === token) suspendedVisualizer = null
        }
    }

    fun tryClaimBattery(token: Any): Boolean {
        var suspendVisualizer: (() -> Unit)? = null
        val granted = synchronized(lock) {
            when (val current = owner) {
                is Owner.Battery -> false
                is Owner.Visualizer -> {
                    suspendedVisualizer = current
                    owner = Owner.Battery(token)
                    suspendVisualizer = current.onSuspend
                    true
                }

                null -> {
                    owner = Owner.Battery(token)
                    true
                }
            }
        }
        suspendVisualizer?.invoke()
        return granted
    }

    fun releaseBattery(token: Any) {
        val resumeVisualizer = synchronized(lock) {
            val current = owner
            if (current !is Owner.Battery || current.token !== token) {
                return@synchronized null
            }
            val visualizer = suspendedVisualizer
            suspendedVisualizer = null
            owner = visualizer
            visualizer?.onResume
        }
        resumeVisualizer?.invoke()
    }
}
