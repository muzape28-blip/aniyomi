package eu.kanade.tachiyomi.core.diagnostics

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small, persistent diagnostic trail for failures that cannot be investigated
 * with logcat. This class intentionally performs a synchronous flush for every
 * event: the last flushed line is often the only evidence after a force close.
 *
 * Logging is best effort and must never become a new application failure.
 */
object Breadcrumb {
    private const val DIRECTORY = "logs/diagnostics"
    private const val ACTIVE_NAME = "breadcrumb.log"
    private const val ARCHIVE_NAME = "breadcrumb.1.log"
    private const val MAX_BYTES = 512 * 1024L
    private const val MAX_DETAIL_LENGTH = 512

    private val lock = Any()
    private val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var directory: File? = null

    fun init(context: Context) {
        try {
            val dir = File(context.applicationContext.filesDir, DIRECTORY)
            if (!dir.exists()) dir.mkdirs()
            directory = dir
        } catch (_: Throwable) {
            // Diagnostics must never prevent application startup.
        }
    }

    fun log(event: String, detail: String? = null) {
        try {
            val dir = directory ?: return
            val file = File(dir, ACTIVE_NAME)
            val line = buildString {
                append(timestamp.format(Date()))
                append(" | ")
                append(event.replace(Regex("[^A-Za-z0-9_.-]"), "_").take(80))
                if (!detail.isNullOrBlank()) {
                    append(" | ")
                    append(sanitize(detail))
                }
                append('\n')
            }

            synchronized(lock) {
                if (file.exists() && file.length() + line.toByteArray(Charsets.UTF_8).size > MAX_BYTES) {
                    rotate(dir, file)
                }
                FileWriter(file, true).use { writer ->
                    writer.write(line)
                    writer.flush()
                }
            }
        } catch (_: Throwable) {
            // Never throw from a diagnostic path.
        }
    }

    /** Returns archive + active content for copy/share/export. */
    fun dumpFull(): String = try {
        val dir = directory ?: return ""
        buildString {
            File(dir, ARCHIVE_NAME).takeIf { it.exists() }?.let { append(it.readText()) }
            File(dir, ACTIVE_NAME).takeIf { it.exists() }?.let { append(it.readText()) }
        }
    } catch (_: Throwable) {
        ""
    }

    /** Returns the newest lines without loading an unbounded UI buffer. */
    fun tail(maxLines: Int = 200): String = try {
        dumpFull().lineSequence().filter { it.isNotBlank() }.toList()
            .takeLast(maxLines.coerceAtLeast(1)).joinToString("\n")
    } catch (_: Throwable) {
        ""
    }

    fun clear(): Boolean = try {
        val dir = directory ?: return false
        synchronized(lock) {
            File(dir, ACTIVE_NAME).delete()
            File(dir, ARCHIVE_NAME).delete()
        }
        true
    } catch (_: Throwable) {
        false
    }

    private fun rotate(dir: File, active: File) {
        try {
            val archive = File(dir, ARCHIVE_NAME)
            if (archive.exists()) archive.delete()
            if (active.exists()) active.renameTo(archive)
        } catch (_: Throwable) {
            // The next write still has a chance to succeed.
        }
    }

    private fun sanitize(raw: String): String {
        var value = raw.replace(Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)) { match ->
            redactUrl(match.value)
        }
        value = value.replace(
            Regex("(?i)(authorization|cookie|token|access_token|refresh_token|api[_-]?key|password|secret)=([^,;\\s]+)"),
            "$1=[REDACTED]",
        )
        return value.replace(Regex("\\s+"), " ").trim().take(MAX_DETAIL_LENGTH)
    }

    private fun redactUrl(url: String): String = try {
        val marker = url.indexOf('?')
        if (marker < 0) url.take(MAX_DETAIL_LENGTH)
        else url.substring(0, marker).take(MAX_DETAIL_LENGTH) + "?[REDACTED]"
    } catch (_: Throwable) {
        "[URL_REDACTED]"
    }
}
