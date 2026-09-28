package eu.kanade.tachiyomi

object BuildConfig {
    const val VERSION_NAME = inspector.BuildConfig.NAME
    val VERSION_CODE = inspector.BuildConfig.REVISION.trimStart('r').toInt()
}
