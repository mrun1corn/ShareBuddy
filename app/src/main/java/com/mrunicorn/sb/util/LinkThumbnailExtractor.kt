package com.mrunicorn.sb.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object LinkThumbnailExtractor {
    private val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Mobile Safari/537.36"

    suspend fun extractThumbnailUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).userAgent(USER_AGENT).get()
            val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content")
            if (ogImage != null && (ogImage.startsWith("http://") || ogImage.startsWith("https://"))) return@withContext ogImage

            val image = doc.selectFirst("link[rel=image_src]")?.attr("href")
            if (image != null && (image.startsWith("http://") || image.startsWith("https://"))) return@withContext image

            val twitterImage = doc.selectFirst("meta[name=twitter:image]")?.attr("content")
            if (twitterImage != null && (twitterImage.startsWith("http://") || twitterImage.startsWith("https://"))) return@withContext twitterImage

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
