package jp.linkserver.glyphvisualizer.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import jp.linkserver.glyphvisualizer.SettingsPreferences

class BatteryGlyphBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return
        BatteryGlyphService.syncEnabledState(
            context = context,
            enabled = SettingsPreferences.load(context).batteryGlyphEnabled
        )
    }
}
