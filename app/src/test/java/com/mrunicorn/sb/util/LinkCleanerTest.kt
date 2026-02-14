package com.mrunicorn.sb.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkCleanerTest {

    @Test
    fun `clean removes tracking parameters`() {
        val raw = "https://example.com/page?utm_source=news&utm_medium=email&id=123"
        val expected = "https://example.com/page?id=123"
        assertEquals(expected, LinkCleaner.clean(raw))
    }

    @Test
    fun `clean handles mixed case parameters`() {
        val raw = "https://example.com/path?ID=456&UTM_CAMPAIGN=summer"
        val expected = "https://example.com/path?ID=456"
        assertEquals(expected, LinkCleaner.clean(raw))
    }

    @Test
    fun `suggestLabel returns correct labels for known domains`() {
        assertEquals("Video", LinkCleaner.suggestLabel("https://www.youtube.com/watch?v=123"))
        assertEquals("Dev", LinkCleaner.suggestLabel("https://github.com/mrun1corn/ShareBuddy"))
        assertEquals("Social", LinkCleaner.suggestLabel("https://reddit.com/r/android"))
        assertEquals("Shop", LinkCleaner.suggestLabel("https://amazon.com/dp/B000000000"))
    }

    @Test
    fun `suggestLabel returns null for unknown domains`() {
        assertEquals(null, LinkCleaner.suggestLabel("https://google.com"))
    }
}
