package com.mrunicorn.sb.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkThumbnailExtractorTest {
    @Test
    fun extractsFromDoubleQuotes() {
        val html = """
            <html><head>
            <meta property="og:image" content="http://example.com/a.jpg">
            </head><body></body></html>
        """.trimIndent()
        val url = LinkThumbnailExtractor.parseThumbnailFromHtml(html)
        assertEquals("http://example.com/a.jpg", url)
    }

    @Test
    fun extractsFromSingleQuotes() {
        val html = """
            <html><head>
            <meta property='og:image' content='http://example.com/b.png'>
            </head><body></body></html>
        """.trimIndent()
        val url = LinkThumbnailExtractor.parseThumbnailFromHtml(html)
        assertEquals("http://example.com/b.png", url)
    }

    @Test
    fun resolvesRelativeUrl() {
        val html = """
            <html><head>
            <meta content="/img/c.webp" property="og:image" />
            </head><body></body></html>
        """.trimIndent()
        val url = LinkThumbnailExtractor.parseThumbnailFromHtml(html, "http://example.com")
        assertEquals("http://example.com/img/c.webp", url)
    }

    @Test
    fun readsTwitterImage() {
        val html = """
            <html><head>
            <meta name="twitter:image" content="http://example.com/d.jpg" />
            </head><body></body></html>
        """.trimIndent()
        val url = LinkThumbnailExtractor.parseThumbnailFromHtml(html)
        assertEquals("http://example.com/d.jpg", url)
    }
}
