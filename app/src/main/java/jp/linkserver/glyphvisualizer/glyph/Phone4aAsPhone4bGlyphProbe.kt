package jp.linkserver.glyphvisualizer.glyph

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import jp.linkserver.glyphvisualizer.AppLogger
import jp.linkserver.glyphvisualizer.R

class Phone4aAsPhone4bGlyphProbe(
    context: Context,
    private val emulatedOnPhone4a: Boolean,
    private val onStatusChanged: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val glyphManager = GlyphManager.getInstance(appContext)
    private val glyphSessionOwnerToken = Any()
    private var initialized = false
    private var sessionOpen = false
    private var ready = false

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            if (!initialized) return
            runCatching {
                val registered = glyphManager.register(
                    if (emulatedOnPhone4a) Glyph.DEVICE_25111 else Glyph.DEVICE_25131
                )
                if (!registered) {
                    ready = false
                    onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_register_failed))
                    return
                }
                glyphManager.openSession()
                sessionOpen = true
                ready = true
                onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_ready))
            }.onFailure { error ->
                ready = false
                onStatusChanged(
                    appContext.getString(
                        R.string.experimental_p4a_as_p4b_open_failed,
                        error.message ?: error.javaClass.simpleName
                    )
                )
                AppLogger.w(TAG, "Phone (4a) as Phone (4b) probe open failed", error)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            ready = false
            sessionOpen = false
            onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_disconnected))
        }
    }

    fun bind() {
        if (initialized) return
        val granted = GlyphSdkSessionCoordinator.claimVisualizer(
            token = glyphSessionOwnerToken,
            onSuspend = ::suspendForBatteryDisplay,
            onResume = ::resumeAfterBatteryDisplay
        )
        if (granted) bindGlyphManager()
    }

    private fun bindGlyphManager() {
        if (initialized) return
        initialized = true
        onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_connecting))
        try {
            glyphManager.init(callback)
        } catch (error: Throwable) {
            initialized = false
            GlyphSdkSessionCoordinator.releaseVisualizer(glyphSessionOwnerToken)
            throw error
        }
    }

    fun probe(logicalChannel: Int, brightness: Int = MAX_LIGHT) {
        if (!ensureReady()) return
        val physicalChannel = physicalChannelFor(logicalChannel)
        if (physicalChannel == null) {
            onStatusChanged(
                appContext.getString(R.string.experimental_p4a_as_p4b_invalid_channel, logicalChannel)
            )
            return
        }

        sendColors(IntArray(frameChannelCount(includeRecordingLight = logicalChannel == 4)).apply {
            this[physicalChannel] = brightness.coerceIn(0, MAX_LIGHT)
        }) {
            appContext.getString(
                R.string.experimental_p4a_as_p4b_sent,
                logicalChannel,
                physicalChannel
            )
        }
    }

    fun probeAll(includeRecordingLight: Boolean) {
        if (!ensureReady()) return
        sendColors(IntArray(frameChannelCount(includeRecordingLight)).apply {
            (0 until PHONE4B_OFFICIAL_CHANNEL_COUNT)
                .map(::physicalChannelFor)
                .filterNotNull()
                .forEach { this[it] = MAX_LIGHT }
            if (includeRecordingLight) {
                physicalChannelFor(4)?.let { this[it] = MAX_LIGHT }
            }
        }) {
            appContext.getString(R.string.experimental_p4a_as_p4b_sent_all)
        }
    }

    fun turnOff() {
        if (!ensureReady()) return
        runCatching {
            glyphManager.turnOff()
            onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_turned_off))
        }.onFailure { error ->
            onStatusChanged(
                appContext.getString(
                    R.string.experimental_p4a_as_p4b_turn_off_failed,
                    error.message ?: error.javaClass.simpleName
                )
            )
            AppLogger.w(TAG, "Phone (4a) as Phone (4b) probe turnOff failed", error)
        }
    }

    fun release() {
        releaseGlyphManager()
        GlyphSdkSessionCoordinator.releaseVisualizer(glyphSessionOwnerToken)
    }

    private fun suspendForBatteryDisplay() {
        releaseGlyphManager()
    }

    private fun resumeAfterBatteryDisplay() {
        bindGlyphManager()
    }

    private fun releaseGlyphManager() {
        if (!initialized && !sessionOpen) {
            ready = false
            return
        }
        runCatching { glyphManager.turnOff() }
        if (sessionOpen) {
            runCatching { glyphManager.closeSession() }
        }
        sessionOpen = false
        ready = false
        if (initialized) {
            runCatching { glyphManager.unInit() }
            initialized = false
        }
    }

    private fun ensureReady(): Boolean {
        if (ready && sessionOpen) return true
        onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_not_ready))
        return false
    }

    private fun physicalChannelFor(logicalChannel: Int): Int? {
        if (logicalChannel !in 0..PHONE4B_RECORDING_LIGHT_CHANNEL) return null
        return if (emulatedOnPhone4a) logicalChannel + PHONE4A_CHANNEL_OFFSET else logicalChannel
    }

    private fun frameChannelCount(includeRecordingLight: Boolean): Int {
        return if (emulatedOnPhone4a) {
            PHONE4A_CHANNEL_COUNT
        } else if (includeRecordingLight) {
            PHONE4B_RECORDING_LIGHT_CHANNEL + 1
        } else {
            PHONE4B_OFFICIAL_CHANNEL_COUNT
        }
    }

    private inline fun sendColors(colors: IntArray, successMessage: () -> String) {
        runCatching {
            glyphManager.setFrameColors(colors)
            onStatusChanged(successMessage())
        }.onFailure { error ->
            val message = when (error) {
                is GlyphException -> error.message ?: "GlyphException"
                else -> error.message ?: error.javaClass.simpleName
            }
            onStatusChanged(appContext.getString(R.string.experimental_p4a_as_p4b_send_failed, message))
            AppLogger.w(TAG, "Phone (4a) as Phone (4b) probe send failed", error)
        }
    }

    private companion object {
        private const val TAG = "P4aAsP4bProbe"
        private const val MAX_LIGHT = 4095
        private const val PHONE4A_CHANNEL_COUNT = 7
        private const val PHONE4B_OFFICIAL_CHANNEL_COUNT = 4
        private const val PHONE4B_RECORDING_LIGHT_CHANNEL = 4
        private const val PHONE4A_CHANNEL_OFFSET = 2
    }
}
