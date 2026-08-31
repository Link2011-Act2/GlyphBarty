package jp.linkserver.glyphvisualizer.update

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ApkDownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long?
)

/** UI-independent update checks and APK transfer operations. */
object AppUpdateRepository {
    suspend fun checkAndRecord(
        context: Context,
        repositoryUrl: String,
        showLatestForTesting: Boolean
    ): Result<AppUpdateInfo?> {
        return checkGitHubReleaseUpdate(repositoryUrl, showLatestForTesting)
            .onSuccess { markUpdateCheckFinished(context) }
    }

    suspend fun checkAutomatically(
        context: Context,
        repositoryUrl: String,
        enabled: Boolean,
        showLatestForTesting: Boolean
    ): Result<AppUpdateInfo?>? {
        if (!enabled || !shouldCheckForUpdates(context)) return null
        return checkAndRecord(context, repositoryUrl, showLatestForTesting)
    }

    fun downloadApk(
        context: Context,
        downloadUrl: String,
        assetName: String,
        onProgress: (ApkDownloadProgress) -> Unit
    ): File {
        val safeName = assetName
            .substringAfterLast('/')
            .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
            .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
        val updatesDir = File(context.cacheDir, "updates").apply {
            mkdirs()
        }
        val destination = File(updatesDir, safeName)
        val partial = File(updatesDir, "$safeName.part")
        updatesDir.listFiles()
            ?.filter {
                it.isFile &&
                    (it.extension.equals("apk", ignoreCase = true) || it.name.endsWith(".part"))
            }
            ?.forEach { it.delete() }

        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "GlyphBarty-Updater")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                error("HTTP $status")
            }
            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
            connection.inputStream.use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloadedBytes = 0L
                    var lastProgressUpdateBytes = 0L
                    onProgress(ApkDownloadProgress(downloadedBytes, totalBytes))
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (
                            downloadedBytes == totalBytes ||
                            downloadedBytes - lastProgressUpdateBytes >= PROGRESS_UPDATE_STEP_BYTES
                        ) {
                            onProgress(ApkDownloadProgress(downloadedBytes, totalBytes))
                            lastProgressUpdateBytes = downloadedBytes
                        }
                    }
                }
            }
            if (destination.exists()) destination.delete()
            check(partial.renameTo(destination)) {
                "Could not finalize APK"
            }
            return destination
        } finally {
            connection.disconnect()
            if (partial.exists()) partial.delete()
        }
    }

    private const val PROGRESS_UPDATE_STEP_BYTES = 256L * 1024L
}
