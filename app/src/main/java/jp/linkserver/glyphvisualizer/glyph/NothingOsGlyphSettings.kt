package jp.linkserver.glyphvisualizer.glyph

import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import jp.linkserver.glyphvisualizer.PREF_KEY_SYNC_WITH_NOTHING_OS_GLYPH_SETTING
import jp.linkserver.glyphvisualizer.SettingsPreferences

internal data class NothingOsGlyphSettingState(
    val syncEnabled: Boolean,
    val systemEnabled: Boolean?,
    val outputAllowed: Boolean
)

internal object NothingOsGlyphSettingPolicy {
    fun parseSystemValue(rawValue: String?): Boolean? {
        val normalized = rawValue?.trim()?.lowercase().orEmpty()
        return when (normalized) {
            "1", "true", "on" -> true
            "0", "false", "off" -> false
            else -> normalized.toIntOrNull()?.let { it != 0 }
        }
    }

    fun outputAllowed(syncEnabled: Boolean, systemEnabled: Boolean?): Boolean {
        return !syncEnabled || systemEnabled != false
    }
}

internal object NothingOsGlyphSettings {
    const val SYSTEM_SETTING_KEY = "led_effect_enable"

    fun readSystemEnabled(context: Context): Boolean? = runCatching {
        NothingOsGlyphSettingPolicy.parseSystemValue(
            Settings.Global.getString(context.contentResolver, SYSTEM_SETTING_KEY)
        )
    }.getOrNull()

    fun currentState(context: Context): NothingOsGlyphSettingState {
        val syncEnabled = SettingsPreferences.load(context).syncWithNothingOsGlyphSettingEnabled
        val systemEnabled = if (syncEnabled) readSystemEnabled(context) else null
        return NothingOsGlyphSettingState(
            syncEnabled = syncEnabled,
            systemEnabled = systemEnabled,
            outputAllowed = NothingOsGlyphSettingPolicy.outputAllowed(
                syncEnabled = syncEnabled,
                systemEnabled = systemEnabled
            )
        )
    }
}

/**
 * Best-effort observer for Nothing OS's undocumented Glyph master switch.
 * A missing or unreadable value deliberately fails open to preserve existing behavior.
 */
internal class NothingOsGlyphSettingsMonitor(
    context: Context,
    private val onStateChanged: (NothingOsGlyphSettingState) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private var started = false
    private var systemObserverRegistered = false
    private var lastState: NothingOsGlyphSettingState? = null

    private val refreshRunnable = Runnable { refreshNow() }
    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_KEY_SYNC_WITH_NOTHING_OS_GLYPH_SETTING) refreshOnMainThread()
        }
    private val systemSettingObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            refreshOnMainThread()
        }
    }

    fun start() {
        if (started) return
        started = true
        SettingsPreferences.registerPreferenceChangeListener(appContext, preferenceListener)
        refreshOnMainThread()
    }

    fun stop() {
        if (!started) return
        started = false
        mainHandler.removeCallbacks(refreshRunnable)
        unregisterSystemObserver()
        SettingsPreferences.unregisterPreferenceChangeListener(appContext, preferenceListener)
        lastState = null
    }

    private fun refreshOnMainThread() {
        if (!started) return
        if (Looper.myLooper() == Looper.getMainLooper()) {
            refreshNow()
        } else {
            mainHandler.removeCallbacks(refreshRunnable)
            mainHandler.post(refreshRunnable)
        }
    }

    private fun refreshNow() {
        if (!started) return
        val syncEnabled = SettingsPreferences.load(appContext).syncWithNothingOsGlyphSettingEnabled
        updateSystemObserverRegistration(syncEnabled)
        val systemEnabled = if (syncEnabled) {
            NothingOsGlyphSettings.readSystemEnabled(appContext)
        } else {
            null
        }
        val state = NothingOsGlyphSettingState(
            syncEnabled = syncEnabled,
            systemEnabled = systemEnabled,
            outputAllowed = NothingOsGlyphSettingPolicy.outputAllowed(
                syncEnabled = syncEnabled,
                systemEnabled = systemEnabled
            )
        )
        if (state == lastState) return
        lastState = state
        onStateChanged(state)
    }

    private fun updateSystemObserverRegistration(syncEnabled: Boolean) {
        if (syncEnabled == systemObserverRegistered) return
        if (syncEnabled) {
            val registered = runCatching {
                appContext.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor(NothingOsGlyphSettings.SYSTEM_SETTING_KEY),
                    false,
                    systemSettingObserver
                )
            }.isSuccess
            systemObserverRegistered = registered
        } else {
            unregisterSystemObserver()
        }
    }

    private fun unregisterSystemObserver() {
        if (!systemObserverRegistered) return
        runCatching {
            appContext.contentResolver.unregisterContentObserver(systemSettingObserver)
        }
        systemObserverRegistered = false
    }
}
