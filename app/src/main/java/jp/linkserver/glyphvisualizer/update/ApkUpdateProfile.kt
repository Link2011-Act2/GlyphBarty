package jp.linkserver.glyphvisualizer.update

import android.content.Context
import jp.linkserver.goldy.*

/** APK distribution only. Capture, MediaProjection, Glyph/Matrix and tile services are independent. */
internal object ApkUpdateProfile {
    val repository = GitHubRepository("Link2011-Act2", "GlyphBarty")
    val policy = UpdatePolicies.glyphBarty()
    val selection = ApkSelectionPolicies.GlyphBarty
    const val ACTION_VIEW_TRANSFER = "jp.linkserver.glyphvisualizer.VIEW_APK_UPDATE"
    const val JOB_ID = 0x4741504b
    const val NOTIFICATION_ID = 0x4741504b
    const val WORK_NAME = "glyph-apk-update-transfer-v1"
    const val CHANNEL = "glyph-apk-update-transfer"
    const val JOB_NAMESPACE = "glyph-apk-updates"
    fun downloadConfig(context: Context) = ApkDownloadConfig(repository, selection, ApkVersionBindings.Published,
        userAgent = "GlyphBarty/${installedVersionName(context)}")
}

@Suppress("DEPRECATION")
internal fun installedVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName?.takeIf { it.isNotBlank() }
        ?: error("Installed package version is missing")
