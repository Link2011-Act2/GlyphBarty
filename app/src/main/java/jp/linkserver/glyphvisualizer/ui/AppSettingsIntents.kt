package jp.linkserver.glyphvisualizer.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

fun openAppLanguageSettings(context: Context): Boolean {
    val packageName = context.packageName
    val intents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
        }
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        add(Intent(Settings.ACTION_LOCALE_SETTINGS))
    }

    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return true
            }
        } catch (_: Exception) {
        }
    }
    return false
}

fun openNotificationAccessSettings(context: Context): Boolean {
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    if (context !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        if (intent.resolveActivity(context.packageManager) == null) {
            false
        } else {
            context.startActivity(intent)
            true
        }
    } catch (_: Throwable) {
        false
    }
}
