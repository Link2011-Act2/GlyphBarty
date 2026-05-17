package jp.linkserver.glyphvisualizer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val LOG_DIR = "logs"
    private const val LOG_FILE = "app.log"
    private const val MAX_SIZE_BYTES = 512 * 1024L  // 512 KB

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val lock = Any()
    private var logFile: File? = null

    fun init(context: Context) {
        val dir = File(context.applicationContext.filesDir, LOG_DIR)
        dir.mkdirs()
        logFile = File(dir, LOG_FILE)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        write("E", tag, message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        write("W", tag, message, throwable)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        write("I", tag, message, null)
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val file = logFile ?: return
        val timestamp = dateFormat.format(Date())
        val line = buildString {
            append("$timestamp $level/$tag: $message\n")
            if (throwable != null) {
                append("  ${throwable.javaClass.name}: ${throwable.message}\n")
                throwable.cause?.let { append("  Caused by: ${it.javaClass.name}: ${it.message}\n") }
            }
        }
        synchronized(lock) {
            try {
                if (file.exists() && file.length() > MAX_SIZE_BYTES) {
                    val content = file.readText()
                    val cutPoint = content.indexOf('\n', content.length / 2)
                    file.writeText(if (cutPoint >= 0) content.substring(cutPoint + 1) else content)
                }
                FileWriter(file, true).use { it.write(line) }
            } catch (_: Exception) {}
        }
    }

    fun share(context: Context) {
        val file = logFile
        if (file == null || !file.exists()) {
            android.widget.Toast.makeText(context, "ログファイルがありません", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "ログファイルの共有に失敗しました", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_debug_share_subject))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.about_debug_share_chooser))
        )
    }

    fun clear() {
        synchronized(lock) {
            try { logFile?.delete() } catch (_: Exception) {}
        }
    }

    fun exists(): Boolean = logFile?.exists() == true
}
