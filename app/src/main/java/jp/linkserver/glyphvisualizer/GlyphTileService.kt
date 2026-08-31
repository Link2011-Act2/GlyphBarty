package jp.linkserver.glyphvisualizer

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import jp.linkserver.glyphvisualizer.audio.AudioRouteDiagnostics
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
        val shouldStop = isConfirmedTileCaptureActive(CaptureUiStore.runtimeState) ||
            GlyphVisualizerService.isRunning(this)
        updateTileState(capturing = false)

        if (shouldStop) {
            GlyphVisualizerService.stop(this)
        } else {
            val s = SettingsPreferences.load(this)
            val effectiveProfile = GlyphDeviceCatalog.effectiveUiProfile(
                actualProfile = GlyphDeviceCatalog.currentProfile(),
                phone4bEmulationEnabled = s.phone4bEmulationEnabled,
                debugDeviceProfileOverride = s.debugDeviceProfileOverride
            )
            Phone1GlyphDebugHelper.autoEnableOnStartIfPossible(
                context = this,
                profile = effectiveProfile,
                autoEnableOnStart = s.autoEnablePhone1GlyphDebugOnStart
            )
            val resolved = s.withResolvedLatency(AudioRouteDiagnostics.isBluetoothOutputLikelyConnected(this))
            CaptureCommandGateway.startVisualizer(
                context = this,
                config = resolved.toCaptureConfig(),
                source = VisualizerStartSource.QUICK_SETTINGS
            )
        }

        // サービスの状態変化を待ってタイルを再更新させる
        refresh(this)
    }

    private fun updateTile() {
        val capturing = isConfirmedTileCaptureActive(CaptureUiStore.runtimeState)
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

internal fun isConfirmedTileCaptureActive(runtimeState: CaptureRuntimeState): Boolean {
    return runtimeState.isCapturing
}
