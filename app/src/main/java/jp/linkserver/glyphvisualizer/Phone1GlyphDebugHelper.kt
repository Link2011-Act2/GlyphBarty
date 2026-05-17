package jp.linkserver.glyphvisualizer

import android.content.Context
import android.content.pm.PackageManager
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import java.util.Locale
import rikka.shizuku.Shizuku
import rikka.sui.Sui

object Phone1GlyphDebugHelper {
    private const val TAG = "Phone1GlyphDebug"
    private val PHONE1_GLYPH_DEBUG_COMMAND = arrayOf(
        "sh",
        "-c",
        "settings put global nt_glyph_interface_debug_enable 1"
    )

    fun supports(profile: GlyphDeviceProfile): Boolean {
        return profile == GlyphDeviceProfile.PHONE1
    }

    private fun initializeBackend(context: Context) {
        runCatching {
            Sui.init(context.packageName)
            AppLogger.i(TAG, "Sui.init completed for package=${context.packageName}")
        }.onFailure { error ->
            AppLogger.w(TAG, "Sui.init failed", error)
        }
    }

    fun isSuiAvailable(context: Context): Boolean {
        initializeBackend(context)
        return runCatching { Sui.isSui() }
            .onFailure { error -> AppLogger.w(TAG, "Sui.isSui failed", error) }
            .getOrDefault(false)
            .also { AppLogger.i(TAG, "Sui availability=$it") }
    }

    fun isShizukuAvailable(context: Context): Boolean {
        initializeBackend(context)
        return runCatching { Shizuku.pingBinder() }
            .onFailure { error -> AppLogger.w(TAG, "Shizuku.pingBinder failed", error) }
            .getOrDefault(false)
            .also { AppLogger.i(TAG, "Shizuku availability=$it") }
    }

    fun isBackendAvailable(context: Context): Boolean {
        val shizukuAvailable = isShizukuAvailable(context)
        val suiAvailable = isSuiAvailable(context)
        val available = shizukuAvailable || suiAvailable
        AppLogger.i(
            TAG,
            "Backend availability=$available suiAvailable=$suiAvailable shizukuAvailable=$shizukuAvailable"
        )
        return available
    }

    fun hasPermission(context: Context): Boolean {
        val shizukuAvailable = isShizukuAvailable(context)
        if (!shizukuAvailable) {
            AppLogger.i(TAG, "Permission availability=false because Shizuku binder is unavailable")
            return false
        }

        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.onFailure { error ->
            AppLogger.w(TAG, "Shizuku.checkSelfPermission failed", error)
        }.getOrDefault(false)

        AppLogger.i(
            TAG,
            "Permission availability=$granted suiAvailable=${isSuiAvailable(context)} shizukuAvailable=$shizukuAvailable"
        )
        return granted
    }

    fun requestPermission(context: Context, requestCode: Int) {
        AppLogger.i(TAG, "requestPermission called requestCode=$requestCode")
        if (isShizukuAvailable(context)) {
            AppLogger.i(TAG, "Requesting Shizuku permission requestCode=$requestCode")
            Shizuku.requestPermission(requestCode)
        } else {
            AppLogger.i(TAG, "Skipping permission request because Shizuku is unavailable")
        }
    }

    fun enableGlyphDebug(context: Context): Result<Unit> {
        return runCatching {
            initializeBackend(context)
            val backendAvailable = isBackendAvailable(context)
            val permissionGranted = hasPermission(context)
            AppLogger.i(
                TAG,
                "enableGlyphDebug started backendAvailable=$backendAvailable permissionGranted=$permissionGranted"
            )
            check(backendAvailable) { "Shizuku or Sui is unavailable" }
            check(permissionGranted) { "Shizuku permission is unavailable" }

            AppLogger.i(
                TAG,
                "Executing remote shell command through Shizuku newProcess: ${PHONE1_GLYPH_DEBUG_COMMAND.joinToString(" ")}"
            )
            val process = createRemoteProcess(PHONE1_GLYPH_DEBUG_COMMAND)
            val stdout = process.inputStream.bufferedReader().use { it.readText().trim() }
            val stderr = process.errorStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            AppLogger.i(
                TAG,
                "Remote shell finished exitCode=$exitCode stdout=${stdout.ifBlank { "<empty>" }} stderr=${stderr.ifBlank { "<empty>" }}"
            )
            check(exitCode == 0) {
                listOf(stderr, stdout)
                    .firstOrNull { it.isNotBlank() }
                    ?: String.format(Locale.US, "exitCode=%d", exitCode)
            }
        }.onFailure { error ->
            AppLogger.w(TAG, "enableGlyphDebug failed", error)
        }
    }

    private fun createRemoteProcess(command: Array<String>): Process {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, command, null, null) as? Process
        check(process != null) { "Shizuku.newProcess returned null" }
        return process
    }
}
