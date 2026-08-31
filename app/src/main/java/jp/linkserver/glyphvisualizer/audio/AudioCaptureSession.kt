package jp.linkserver.glyphvisualizer.audio

/**
 * Owns capture generations independently from platform audio resources.
 *
 * A generation remains current after a worker failure long enough to deliver that failure once,
 * but it is no longer runnable. Starting or stopping a capture invalidates every callback from the
 * previous generation.
 */
internal class AudioCaptureSessionOwner {
    private val lock = Any()
    private var nextGenerationId = 0L
    private var current: AudioCaptureGeneration? = null

    fun begin(): AudioCaptureGeneration = synchronized(lock) {
        current?.invalidate()
        AudioCaptureGeneration(
            id = ++nextGenerationId,
            owner = this
        ).also { current = it }
    }

    fun stopCurrent(): AudioCaptureGeneration? = synchronized(lock) {
        current.also {
            current = null
            it?.invalidate()
        }
    }

    fun finish(generation: AudioCaptureGeneration) {
        synchronized(lock) {
            if (current === generation) {
                current = null
            }
            generation.invalidate()
        }
    }

    internal fun isCurrent(generation: AudioCaptureGeneration): Boolean =
        synchronized(lock) { current === generation }

    internal fun runIfCurrent(
        generation: AudioCaptureGeneration,
        block: () -> Unit
    ): Boolean = synchronized(lock) {
        if (current !== generation) {
            false
        } else {
            block()
            true
        }
    }

    internal fun runIfRunningCurrent(
        generation: AudioCaptureGeneration,
        block: () -> Unit
    ): Boolean = synchronized(lock) {
        if (current !== generation || !generation.isMarkedRunning()) {
            false
        } else {
            block()
            true
        }
    }

    internal fun stopWorkerIfCurrent(generation: AudioCaptureGeneration): Boolean =
        synchronized(lock) {
            if (current !== generation || !generation.isMarkedRunning()) {
                false
            } else {
                generation.markWorkerStopped()
                true
            }
        }
}

internal class AudioCaptureGeneration internal constructor(
    val id: Long,
    private val owner: AudioCaptureSessionOwner
) {
    @Volatile
    private var workerRunning = true

    fun isCurrent(): Boolean = owner.isCurrent(this)

    fun shouldRun(): Boolean = workerRunning && isCurrent()

    fun runIfCurrent(block: () -> Unit): Boolean {
        return owner.runIfCurrent(this, block)
    }

    fun runIfRunningCurrent(block: () -> Unit): Boolean {
        return owner.runIfRunningCurrent(this, block)
    }

    fun stopWorkerIfCurrent(): Boolean = owner.stopWorkerIfCurrent(this)

    internal fun isMarkedRunning(): Boolean = workerRunning

    internal fun markWorkerStopped() {
        workerRunning = false
    }

    internal fun invalidate() {
        workerRunning = false
    }
}
