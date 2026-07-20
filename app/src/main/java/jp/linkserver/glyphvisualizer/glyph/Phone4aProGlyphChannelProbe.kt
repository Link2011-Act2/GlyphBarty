package jp.linkserver.glyphvisualizer.glyph

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import jp.linkserver.glyphvisualizer.AppLogger

class Phone4aProGlyphChannelProbe(
    context: Context,
    private val onStatusChanged: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val glyphManager = GlyphManager.getInstance(appContext)
    private var initialized = false
    private var sessionOpen = false
    private var ready = false

    private val callback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            runCatching {
                val registered = glyphManager.register(Glyph.DEVICE_25111p)
                if (!registered) {
                    ready = false
                    onStatusChanged("register(DEVICE_25111p) failed")
                    return
                }
                glyphManager.openSession()
                sessionOpen = true
                ready = true
                onStatusChanged("Ready: DEVICE_25111p session opened")
            }.onFailure { error ->
                ready = false
                onStatusChanged("Open failed: ${error.message ?: error.javaClass.simpleName}")
                AppLogger.w(TAG, "Phone (4a) Pro channel probe open failed", error)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            ready = false
            sessionOpen = false
            onStatusChanged("Glyph service disconnected")
        }
    }

    fun bind() {
        if (initialized) return
        initialized = true
        onStatusChanged("Connecting to GlyphManager...")
        glyphManager.init(callback)
    }

    fun probe(channel: Int, brightness: Int = MAX_LIGHT) {
        if (!ready || !sessionOpen) {
            onStatusChanged("Not ready yet")
            return
        }
        if (channel < 0) {
            onStatusChanged("Invalid channel: $channel")
            return
        }

        runCatching {
            val colors = IntArray(maxOf(7, channel + 1))
            colors[channel] = brightness.coerceIn(0, MAX_LIGHT)
            glyphManager.setFrameColors(colors)
            onStatusChanged("Sent channel=$channel size=${colors.size} brightness=${colors[channel]}")
        }.onFailure { error ->
            val message = when (error) {
                is GlyphException -> error.message ?: "GlyphException"
                else -> error.message ?: error.javaClass.simpleName
            }
            onStatusChanged("Channel $channel failed: $message")
            AppLogger.w(TAG, "Phone (4a) Pro channel probe failed. channel=$channel", error)
        }
    }

    fun turnOff() {
        runCatching {
            glyphManager.turnOff()
            onStatusChanged("Turned off")
        }.onFailure { error ->
            onStatusChanged("Turn off failed: ${error.message ?: error.javaClass.simpleName}")
            AppLogger.w(TAG, "Phone (4a) Pro channel probe turnOff failed", error)
        }
    }

    fun release() {
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

    private companion object {
        private const val TAG = "P4aProChannelProbe"
        private const val MAX_LIGHT = 4095
    }
}
