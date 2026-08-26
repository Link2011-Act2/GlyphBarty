package jp.linkserver.glyphvisualizer

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import jp.linkserver.glyphvisualizer.glyph.GlyphDeviceProfile
import java.util.Locale
import rikka.shizuku.Shizuku
import rikka.sui.Sui

object Phone1GlyphDebugHelper {
    private const val TAG = "Phone1GlyphDebug"
    private const val AUTO_ENABLE_PERMISSION_REQUEST_CODE = 1402
    private val PHONE1_GLYPH_DEBUG_COMMAND = arrayOf(
        "sh",
        "-c",
        "settings put global nt_glyph_interface_debug_enable 1"
    )

    private val autoEnablePermissionLock = Any()
    private var pendingAutoEnableContext: Context? = null
    private val autoEnablePermissionListener: Shizuku.OnRequestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != AUTO_ENABLE_PERMISSION_REQUEST_CODE) {
                return@OnRequestPermissionResultListener
            }

            val context = synchronized(autoEnablePermissionLock) {
                pendingAutoEnableContext.also {
                    pendingAutoEnableContext = null
                }
            }
            runCatching {
                Shizuku.removeRequestPermissionResultListener(autoEnablePermissionListener)
            }.onFailure { error ->
                AppLogger.w(TAG, "Failed to remove automatic permission listener", error)
            }

            AppLogger.i(
                TAG,
                "Automatic permission result received grantResult=$grantResult pending=${context != null}"
            )
            if (grantResult == PackageManager.PERMISSION_GRANTED && context != null) {
                val settings = SettingsPreferences.load(context)
                val effectiveProfile = GlyphDeviceCatalog.effectiveUiProfile(
                    actualProfile = GlyphDeviceCatalog.currentProfile(),
                    phone4bEmulationEnabled = settings.phone4bEmulationEnabled,
                    debugDeviceProfileOverride = settings.debugDeviceProfileOverride
                )
                if (shouldAutoEnableOnStart(
                        profile = effectiveProfile,
                        autoEnableOnStart = settings.autoEnablePhone1GlyphDebugOnStart
                    )
                ) {
                    enableAutomatically(context)
                } else {
                    AppLogger.i(
                        TAG,
                        "Skipping automatic enable after permission result because eligibility changed"
                    )
                }
            }
        }

    fun supports(profile: GlyphDeviceProfile): Boolean {
        return profile == GlyphDeviceProfile.PHONE1
    }

    data class BackendStatus(
        val suiAvailable: Boolean,
        val apiAvailable: Boolean,
        val permissionGranted: Boolean
    )

    internal enum class AutoEnableBackendAction {
        ENABLE,
        REQUEST_PERMISSION,
        SKIP
    }

    internal fun shouldAutoEnableOnStart(
        profile: GlyphDeviceProfile,
        autoEnableOnStart: Boolean
    ): Boolean {
        return autoEnableOnStart && supports(profile)
    }

    internal fun resolveAutoEnableBackendAction(
        backendStatus: BackendStatus
    ): AutoEnableBackendAction = when {
        backendStatus.permissionGranted -> AutoEnableBackendAction.ENABLE
        backendStatus.apiAvailable -> AutoEnableBackendAction.REQUEST_PERMISSION
        else -> AutoEnableBackendAction.SKIP
    }

    fun autoEnableOnStartIfPossible(
        context: Context,
        profile: GlyphDeviceProfile,
        autoEnableOnStart: Boolean
    ) {
        if (!shouldAutoEnableOnStart(profile, autoEnableOnStart)) {
            return
        }

        val applicationContext = context.applicationContext
        AppLogger.i(TAG, "Attempting automatic Phone (1) glyph debug enable")
        val backendStatus = backendStatus()
        when (resolveAutoEnableBackendAction(backendStatus)) {
            AutoEnableBackendAction.ENABLE -> enableAutomatically(applicationContext)
            AutoEnableBackendAction.REQUEST_PERMISSION -> requestAutomaticPermission(applicationContext)
            AutoEnableBackendAction.SKIP -> AppLogger.i(
                TAG,
                "Skipping automatic enable because the Shizuku API binder is unavailable; suiDetected=${backendStatus.suiAvailable}"
            )
        }
    }

    private fun requestAutomaticPermission(context: Context) {
        val shouldRequest = synchronized(autoEnablePermissionLock) {
            if (pendingAutoEnableContext != null) {
                false
            } else {
                pendingAutoEnableContext = context
                true
            }
        }
        if (!shouldRequest) {
            AppLogger.i(TAG, "Automatic permission request is already pending")
            return
        }

        runCatching {
            Shizuku.addRequestPermissionResultListener(autoEnablePermissionListener)
            check(requestPermission(AUTO_ENABLE_PERMISSION_REQUEST_CODE)) {
                "Shizuku API became unavailable before requesting permission"
            }
        }.onFailure { error ->
            synchronized(autoEnablePermissionLock) {
                pendingAutoEnableContext = null
            }
            runCatching {
                Shizuku.removeRequestPermissionResultListener(autoEnablePermissionListener)
            }
            AppLogger.w(TAG, "Automatic Shizuku permission request failed", error)
        }
    }

    private fun enableAutomatically(context: Context) {
        val result = enableGlyphDebug()
        if (result.isSuccess) {
            AppLogger.i(TAG, "Automatic Phone (1) glyph debug enable completed successfully")
            Toast.makeText(
                context,
                context.getString(R.string.phone1_glyph_debug_success),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            AppLogger.w(
                TAG,
                "Automatic Phone (1) glyph debug enable failed",
                result.exceptionOrNull()
            )
        }
    }

    fun backendStatus(): BackendStatus {
        // ShizukuProvider initializes Sui once when this app process is created.
        val suiAvailable = runCatching { Sui.isSui() }
            .onFailure { error -> AppLogger.w(TAG, "Sui.isSui failed", error) }
            .getOrDefault(false)
        val apiAvailable = runCatching { Shizuku.pingBinder() }
            .onFailure { error -> AppLogger.w(TAG, "Shizuku.pingBinder failed", error) }
            .getOrDefault(false)
        val permissionGranted = apiAvailable && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
            .onFailure { error -> AppLogger.w(TAG, "Shizuku.checkSelfPermission failed", error) }
            .getOrDefault(false)

        AppLogger.i(
            TAG,
            "Backend status suiAvailable=$suiAvailable apiAvailable=$apiAvailable permissionGranted=$permissionGranted"
        )
        return BackendStatus(
            suiAvailable = suiAvailable,
            apiAvailable = apiAvailable,
            permissionGranted = permissionGranted
        )
    }

    fun requestPermission(requestCode: Int): Boolean {
        AppLogger.i(TAG, "requestPermission called requestCode=$requestCode")
        val apiAvailable = runCatching { Shizuku.pingBinder() }
            .onFailure { error -> AppLogger.w(TAG, "Shizuku.pingBinder failed before permission request", error) }
            .getOrDefault(false)
        if (!apiAvailable) {
            AppLogger.i(TAG, "Skipping permission request because Shizuku is unavailable")
            return false
        }
        AppLogger.i(TAG, "Requesting Shizuku API permission requestCode=$requestCode")
        Shizuku.requestPermission(requestCode)
        return true
    }

    fun enableGlyphDebug(): Result<Unit> {
        return runCatching {
            val backendStatus = backendStatus()
            AppLogger.i(
                TAG,
                "enableGlyphDebug started apiAvailable=${backendStatus.apiAvailable} permissionGranted=${backendStatus.permissionGranted}"
            )
            check(backendStatus.apiAvailable) { "Shizuku or Sui is unavailable" }
            check(backendStatus.permissionGranted) { "Shizuku API permission is unavailable" }

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
