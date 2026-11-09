package jp.linkserver.glyphvisualizer.update

import android.content.Context
import jp.linkserver.glyphvisualizer.BuildConfig
import jp.linkserver.glyphvisualizer.SettingsPreferences
import jp.linkserver.goldy.*
import java.util.Locale

private const val PREFS="github_release_updates"
/** Display/build capability only: existing Matrix/debug callers keep their build-time gate. */
fun isIntDevBuild(): Boolean = detectReleaseChannel(BuildConfig.VERSION_NAME)=="IntDev"
fun detectReleaseChannel(value: String): String {
    val lower=value.lowercase(Locale.US)
    return when { "intdev" in lower || "internal" in lower -> "IntDev"; "beta" in lower -> "Beta"; "stable" in lower -> "Stable"; "release" in lower -> "Release"; else -> "Unknown" }
}
private fun installedIntDev(context: Context): Boolean = VersionPolicies.Common.channel(installedVersionName(context))==ReleaseChannel.IntDev
fun isShowLatestReleaseForTestingEnabled(context: Context): Boolean = installedIntDev(context) && context.getSharedPreferences(PREFS,0).getBoolean("show_latest_release_for_testing",false)
fun setShowLatestReleaseForTestingEnabled(context: Context,enabled: Boolean) { context.getSharedPreferences(PREFS,0).edit().putBoolean("show_latest_release_for_testing",enabled && installedIntDev(context)).apply() }
fun isUpdateCheckIntervalIgnoredForTesting(context: Context): Boolean = installedIntDev(context) && context.getSharedPreferences(PREFS,0).getBoolean("ignore_check_interval_for_testing",false)
fun setUpdateCheckIntervalIgnoredForTesting(context: Context,enabled: Boolean) { context.getSharedPreferences(PREFS,0).edit().putBoolean("ignore_check_interval_for_testing",enabled && installedIntDev(context)).apply() }
internal fun updateOptions(context: Context): RuntimeOptions {
    val enabled=SettingsPreferences.load(context).automaticUpdateCheckEnabled
    return RuntimeOptions(automaticChecksEnabled=enabled,notificationsEnabled=enabled,developer=DeveloperOptions(
        ignoreVersionComparison=isShowLatestReleaseForTestingEnabled(context),ignoreCheckInterval=isUpdateCheckIntervalIgnoredForTesting(context)))
}
