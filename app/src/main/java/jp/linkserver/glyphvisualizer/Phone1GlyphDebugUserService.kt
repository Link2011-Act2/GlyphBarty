package jp.linkserver.glyphvisualizer

import android.content.Context
import android.os.Binder
import java.util.Locale

private const val PHONE1_GLYPH_DEBUG_COMMAND = "settings put global nt_glyph_interface_debug_enable 1"

class Phone1GlyphDebugUserService(
    context: Context? = null
) : Binder() {
    private val unusedContext = context

    init {
        AppLogger.i(
            "Phone1GlyphDebug",
            "Phone1GlyphDebugUserService created binder=${javaClass.name}"
        )
        executeGlyphDebugCommand()
    }

    private fun executeGlyphDebugCommand() {
        val result = runCatching {
            AppLogger.i("Phone1GlyphDebug", "Executing command: $PHONE1_GLYPH_DEBUG_COMMAND")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", PHONE1_GLYPH_DEBUG_COMMAND))
            val stdout = process.inputStream.bufferedReader().use { it.readText().trim() }
            val stderr = process.errorStream.bufferedReader().use { it.readText().trim() }
            val exitCode = process.waitFor()
            AppLogger.i(
                "Phone1GlyphDebug",
                "Command finished exitCode=$exitCode stdout=${stdout.ifBlank { "<empty>" }} stderr=${stderr.ifBlank { "<empty>" }}"
            )
            check(exitCode == 0) {
                listOf(stderr, stdout)
                    .firstOrNull { it.isNotBlank() }
                    ?: String.format(Locale.US, "exitCode=%d", exitCode)
            }
        }
        result.onSuccess {
            AppLogger.i("Phone1GlyphDebug", "Phone (1) glyph debug mode enabled successfully")
        }
        result.onFailure { error ->
            AppLogger.w(
                "Phone1GlyphDebug",
                "Failed to enable Phone (1) glyph debug mode",
                error
            )
        }
    }
}
