package eu.kanade.tachiyomi.core.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VideoUrlValidatorTest {
    @Test
    fun acceptsHttpVideoUrl() {
        assertTrue(VideoUrlValidator.validate("https://video.example/file.m3u8").valid)
    }

    @Test
    fun rejectsPlaceholderValues() {
        assertFalse(VideoUrlValidator.validate("null").valid)
        assertEquals("literal-undefined", VideoUrlValidator.validate("undefined").reason)
    }

    @Test
    fun rejectsUrlWithoutHost() {
        assertEquals("missing-host", VideoUrlValidator.validate("https:///file.mp4").reason)
    }

    @Test
    fun acceptsLocalAndMagnetSchemes() {
        assertTrue(VideoUrlValidator.validate("content://media/external/video/1").valid)
        assertTrue(VideoUrlValidator.validate("magnet:?xt=urn:btih:test").valid)
    }
}
