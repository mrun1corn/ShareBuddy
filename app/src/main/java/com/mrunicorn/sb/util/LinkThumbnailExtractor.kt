package com.mrunicorn.sb.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object LinkThumbnailExtractor {

    private val OG_IMAGE_REGEX = "<meta property=\"og:image\" content=\"(.*?)\">".toRegex()

    suspend fun extractThumbnailUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { bufferedReader ->
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        val matchResult = OG_IMAGE_REGEX.find(line!!)
                        if (matchResult != null) {
                            return@withContext matchResult.groupValues[1]
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
