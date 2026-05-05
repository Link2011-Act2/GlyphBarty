package jp.linkserver.glyphvisualizer

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import java.lang.ref.WeakReference

class GlyphTileService : TileService() {
    companion object {
        private var activeInstance = WeakReference<GlyphTileService>(null)

        fun refresh(context: android.content.Context) {
            val instance = activeInstance.get()
            if (instance != null) {
                instance.updateTile()
                return
            }

            requestListeningState(context, ComponentName(context, GlyphTileService::class.java))
        }
    }

    override fun onStartListening() {
        super.onStartListening()
        activeInstance = WeakReference(this)
        updateTile()
    }

    override fun onStopListening() {
        activeInstance = WeakReference(null)
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val willEnable = !GlyphVisualizerService.isRunning(this)
        updateTileState(capturing = willEnable)

        if (!willEnable) {
            GlyphVisualizerService.stop(this)
        } else {
            val s = SettingsPreferences.load(this)
            GlyphVisualizerService.startVisualizer(
                context = this,
                sensitivity = s.sensitivity,
                noiseGate = s.noiseGate,
                dynamics = s.dynamics,
                toneFocus = s.toneFocus,
                smoothing = s.smoothing,
                smoothingBalance = s.smoothingBalance,
                reverseDirection = s.reverseDirection,
                peakHoldEnabled = s.peakHoldEnabled,
                glyphMode = s.glyphMode,
                binaryMode = s.binaryMode,
                levelAutoScale = s.levelAutoScale,
                spectrumAutoScale = s.spectrumAutoScale,
                allBrightnessAutoScale = s.allBrightnessAutoScale,
                turnOffWhenBackDown = s.turnOffWhenBackDown
            )
        }

        // サービスの状態変化を待ってタイルを再更新させる
        refresh(this)
    }

    private fun updateTile() {
        val capturing = GlyphVisualizerService.isRunning(this)
        updateTileState(capturing)
    }

    private fun updateTileState(capturing: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (capturing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.icon = Icon.createWithResource(this, R.mipmap.icon_qs)
        tile.updateTile()
    }
}
