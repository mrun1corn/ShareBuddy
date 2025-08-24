package com.mrunicorn.sb.util

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object LinkThumbnailExtractor {
    private const val TIMEOUT = 5000

    suspend fun extractThumbnailUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = Jsoup.connect(url)
                .timeout(TIMEOUT)
                .followRedirects(true)
                .ignoreContentType(true)
                .execute()
            if (response.statusCode() != 200) return@withContext null
            return@withContext parseThumbnailFromDocument(response.parse())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @VisibleForTesting
    internal fun parseThumbnailFromHtml(html: String, baseUri: String = ""): String? {
        val document = Jsoup.parse(html, baseUri)
        return parseThumbnailFromDocument(document)
    }

    private fun parseThumbnailFromDocument(document: Document): String? {
        val element = document.selectFirst("meta[property=og:image], meta[name=twitter:image]")
            ?: return null
        val content = element.attr("content")
        if (content.isNullOrBlank()) return null
        val abs = element.absUrl("content")
        return if (abs.isNotBlank()) abs else content
    }
}
