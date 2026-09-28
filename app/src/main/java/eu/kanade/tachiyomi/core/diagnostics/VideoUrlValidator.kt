package eu.kanade.tachiyomi.core.diagnostics

import java.net.URI

/** Rejects values that must never reach MPV as a resolved video URL. */
object VideoUrlValidator {
    private val allowedSchemes = setOf("http", "https", "content", "file", "magnet")

    fun validate(value: String?): Result {
        if (value.isNullOrBlank()) return Result(false, "blank")
        val normalized = value.trim()
        if (normalized.equals("null", ignoreCase = true)) return Result(false, "literal-null")
        if (normalized.equals("undefined", ignoreCase = true)) return Result(false, "literal-undefined")

        return try {
            val uri = URI(normalized)
            val scheme = uri.scheme?.lowercase()
            if (scheme == null || scheme !in allowedSchemes) {
                Result(false, "unsupported-scheme")
            } else if (scheme in setOf("http", "https") && uri.host.isNullOrBlank()) {
                Result(false, "missing-host")
            } else {
                Result(true, "ok")
            }
        } catch (_: Throwable) {
            Result(false, "malformed")
        }
    }

    data class Result(val valid: Boolean, val reason: String)
}
