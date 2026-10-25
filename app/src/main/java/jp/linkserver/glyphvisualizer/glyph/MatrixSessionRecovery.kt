package jp.linkserver.glyphvisualizer.glyph

/** Bounded, elapsed-time retry budget. Registration alone does not prove healthy output. */
internal class MatrixSessionRecovery {
    companion object {
        const val MAX_ATTEMPTS = 5
        const val HEALTHY_OUTPUT_MS = 10_000L
    }

    var pending = false
        private set
    var attempts = 0
        private set
    var nextAttemptAtMs = 0L
        private set
    private var healthySinceMs: Long? = null
    val exhausted: Boolean get() = attempts >= MAX_ATTEMPTS

    fun request(nowMs: Long, immediate: Boolean = false) {
        if (pending) return
        pending = true
        healthySinceMs = null
        if (immediate) {
            if (attempts == 0) nextAttemptAtMs = nowMs
        } else {
            nextAttemptAtMs = maxOf(nextAttemptAtMs, nowMs + (500L shl attempts.coerceAtMost(4)))
        }
    }

    fun takeAttempt(nowMs: Long): Boolean {
        if (!pending || exhausted || nowMs < nextAttemptAtMs) return false
        attempts++
        nextAttemptAtMs = nowMs + (500L shl (attempts - 1))
        return true
    }

    fun registered() { pending = false }
    fun frameFailed() { healthySinceMs = null }

    /** Returns true only when a recovery episode has reached stable output. */
    fun frameSucceeded(nowMs: Long): Boolean {
        if (pending || attempts == 0) return false
        val since = healthySinceMs ?: nowMs.also { healthySinceMs = it }
        if (nowMs - since < HEALTHY_OUTPUT_MS) return false
        cancel()
        return true
    }

    fun cancel() {
        pending = false
        attempts = 0
        nextAttemptAtMs = 0L
        healthySinceMs = null
    }
}
