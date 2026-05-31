package jp.linkserver.glyphvisualizer.update

import android.content.Context
import jp.linkserver.glyphvisualizer.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val UPDATE_PREFS = "github_release_updates"
private const val KEY_LAST_CHECK_MS = "last_check_ms"
private const val KEY_SHOW_LATEST_FOR_TESTING = "show_latest_release_for_testing"
private const val KEY_IGNORE_CHECK_INTERVAL_FOR_TESTING = "ignore_check_interval_for_testing"
private const val KEY_DISMISSED_UPDATE_TAG = "dismissed_update_tag"
private const val CHECK_INTERVAL_MS = 8L * 60L * 60L * 1000L

data class AppUpdateInfo(
    val tagName: String,
    val channel: String,
    val releaseNotes: String,
    val releaseUrl: String,
    val apkAssetName: String?,
    val apkDownloadUrl: String?
)

suspend fun checkGitHubReleaseUpdate(
    repositoryUrl: String,
    showLatestForTesting: Boolean = false
): Result<AppUpdateInfo?> {
    return runCatching {
        val repository = parseGitHubRepository(repositoryUrl)
            ?: error("Unsupported GitHub repository URL")
        val latestRelease = fetchLatestRelease(repository.owner, repository.name)
        val updateInfo = latestRelease.toUpdateInfo()
        if (!showLatestForTesting && !isNewerRelease(updateInfo.tagName, BuildConfig.VERSION_NAME)) {
            return@runCatching null
        }
        updateInfo
    }
}

fun shouldCheckForUpdates(context: Context): Boolean {
    if (isUpdateCheckIntervalIgnoredForTesting(context)) {
        return true
    }
    val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
    val lastCheckMs = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
    return System.currentTimeMillis() - lastCheckMs >= CHECK_INTERVAL_MS
}

fun markUpdateCheckFinished(context: Context) {
    context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
        .apply()
}

fun isShowLatestReleaseForTestingEnabled(context: Context): Boolean {
    if (!isIntDevBuild()) {
        return false
    }
    return context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_SHOW_LATEST_FOR_TESTING, false)
}

fun setShowLatestReleaseForTestingEnabled(context: Context, enabled: Boolean) {
    if (!isIntDevBuild()) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_LATEST_FOR_TESTING, false)
            .apply()
        return
    }
    context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_SHOW_LATEST_FOR_TESTING, enabled)
        .apply()
}

fun isUpdateCheckIntervalIgnoredForTesting(context: Context): Boolean {
    if (!isIntDevBuild()) {
        return false
    }
    return context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_IGNORE_CHECK_INTERVAL_FOR_TESTING, false)
}

fun setUpdateCheckIntervalIgnoredForTesting(context: Context, enabled: Boolean) {
    if (!isIntDevBuild()) {
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IGNORE_CHECK_INTERVAL_FOR_TESTING, false)
            .apply()
        return
    }
    context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_IGNORE_CHECK_INTERVAL_FOR_TESTING, enabled)
        .apply()
}

fun isUpdateNotificationDismissed(context: Context, tagName: String): Boolean {
    return context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_DISMISSED_UPDATE_TAG, null)
        .equals(tagName, ignoreCase = true)
}

fun dismissUpdateNotificationUntilNextVersion(context: Context, tagName: String) {
    context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_DISMISSED_UPDATE_TAG, tagName)
        .apply()
}

fun isIntDevBuild(): Boolean {
    return detectReleaseChannel(BuildConfig.VERSION_NAME).equals("IntDev", ignoreCase = true)
}

private data class GitHubRepository(val owner: String, val name: String)

private data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String,
    val htmlUrl: String,
    val assets: List<GitHubReleaseAsset>
)

private data class GitHubReleaseAsset(
    val name: String,
    val browserDownloadUrl: String
)

private fun parseGitHubRepository(repositoryUrl: String): GitHubRepository? {
    val marker = "github.com/"
    val index = repositoryUrl.indexOf(marker, ignoreCase = true)
    if (index < 0) return null
    val path = repositoryUrl.substring(index + marker.length)
        .trim('/')
        .substringBefore("?")
        .substringBefore("#")
    val parts = path.split('/')
    if (parts.size < 2) return null
    return GitHubRepository(
        owner = parts[0].trim(),
        name = parts[1].removeSuffix(".git").trim()
    )
}

private fun fetchLatestRelease(owner: String, repo: String): GitHubRelease {
    val connection = (URL("https://api.github.com/repos/$owner/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 10_000
        readTimeout = 10_000
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "GlyphBarty/${BuildConfig.VERSION_NAME}")
    }
    try {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (status !in 200..299) {
            error("GitHub returned HTTP $status")
        }
        return parseReleaseResponse(response)
    } finally {
        connection.disconnect()
    }
}

private fun parseReleaseResponse(response: String): GitHubRelease {
    val json = JSONObject(response)
    val assetsJson = json.optJSONArray("assets")
    val assets = buildList {
        if (assetsJson != null) {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.optJSONObject(index) ?: continue
                val name = asset.optString("name").trim()
                val downloadUrl = asset.optString("browser_download_url").trim()
                if (name.isNotBlank() && downloadUrl.isNotBlank()) {
                    add(GitHubReleaseAsset(name, downloadUrl))
                }
            }
        }
    }
    return GitHubRelease(
        tagName = json.optString("tag_name").ifBlank { json.optString("name") },
        name = json.optString("name"),
        body = json.optString("body"),
        htmlUrl = json.optString("html_url"),
        assets = assets
    )
}

private fun GitHubRelease.toUpdateInfo(): AppUpdateInfo {
    val apkAsset = assets
        .filter { it.name.endsWith(".apk", ignoreCase = true) }
        .maxWithOrNull(compareBy<GitHubReleaseAsset> { it.name.contains("glyphbarty", ignoreCase = true) }
            .thenBy { it.name.contains("universal", ignoreCase = true) })
    val channelSource = listOf(tagName, name, apkAsset?.name.orEmpty()).firstOrNull { it.isNotBlank() }.orEmpty()
    return AppUpdateInfo(
        tagName = tagName.ifBlank { name.ifBlank { "unknown" } },
        channel = detectChannel(channelSource),
        releaseNotes = body.ifBlank { "" },
        releaseUrl = htmlUrl,
        apkAssetName = apkAsset?.name,
        apkDownloadUrl = apkAsset?.browserDownloadUrl
    )
}

private fun isNewerRelease(remoteTag: String, currentVersion: String): Boolean {
    val remote = parseVersionNumbers(remoteTag)
    val current = parseVersionNumbers(currentVersion)
    val maxSize = maxOf(remote.size, current.size)
    if (maxSize == 0) return false
    for (index in 0 until maxSize) {
        val remotePart = remote.getOrElse(index) { 0 }
        val currentPart = current.getOrElse(index) { 0 }
        if (remotePart != currentPart) return remotePart > currentPart
    }
    return channelPriority(detectChannel(remoteTag)) > channelPriority(detectChannel(currentVersion))
}

private fun parseVersionNumbers(value: String): List<Int> {
    val normalized = value
        .lowercase(Locale.US)
        .substringAfter("glyphbarty_", value.lowercase(Locale.US))
        .removePrefix("v")
        .substringBefore("-")
    val dotted = Regex("""\d+(?:\.\d+)+""").find(normalized)?.value
    if (dotted != null) {
        return dotted.split('.').mapNotNull { it.toIntOrNull() }
    }
    val compact = Regex("""\d{3,}""").find(normalized)?.value
    if (compact != null) {
        return compact.map { it.digitToInt() }
    }
    return Regex("""\d+""").findAll(normalized).mapNotNull { it.value.toIntOrNull() }.toList()
}

private fun detectChannel(value: String): String {
    val lower = value.lowercase(Locale.US)
    return when {
        "intdev" in lower || "internal" in lower -> "IntDev"
        "beta" in lower -> "Beta"
        "stable" in lower || "release" in lower -> "Release"
        else -> "Unknown"
    }
}

fun detectReleaseChannel(value: String): String = detectChannel(value)

private fun channelPriority(channel: String): Int {
    return when (channel.lowercase(Locale.US)) {
        "intdev" -> 0
        "beta" -> 1
        "release", "stable" -> 2
        else -> -1
    }
}
