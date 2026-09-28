package eu.kanade.tachiyomi.util

import eu.kanade.tachiyomi.BuildConfig

/** Build-variant gates for the reversible streaming UAT flavor. */
object StreamingOnly {
    val enabled: Boolean
        get() = BuildConfig.STREAMING_ONLY

    /** Features intentionally outside the first streaming UAT gate. */
    val nonPlaybackFeaturesEnabled: Boolean
        get() = !enabled
}
