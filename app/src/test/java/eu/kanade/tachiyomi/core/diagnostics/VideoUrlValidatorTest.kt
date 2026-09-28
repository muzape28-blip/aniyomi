package eu.kanade.tachiyomi.core.diagnostics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

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
        assertFalse(VideoUrlValidator.validate("https:///file.mp4").valid)
    }

    @Test
    fun acceptsLocalAndMagnetSchemes() {
        assertTrue(VideoUrlValidator.validate("content://media/external/video/1").valid)
        assertTrue(VideoUrlValidator.validate("magnet:?xt=urn:btih:test").valid)
    }
}
