package com.mrunicorn.sb.util

import android.net.Uri

object LinkCleaner {
    private val stripParams = setOf(
        "utm_source","utm_medium","utm_campaign","utm_term","utm_content",
        "gclid","gbraid","wbraid","fbclid","mc_eid","mc_cid","igsh","vero_id",
        "spm","yclid","msclkid","otc","cmpid","s_cid"
    )

    fun clean(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val builder = uri.buildUpon().clearQuery()
            val kept = uri.queryParameterNames.filterNot { p ->
                stripParams.any { it.equals(p, ignoreCase = true) } || p.startsWith("utm_", true)
            }.sorted()
            for (p in kept) {
                for (v in uri.getQueryParameters(p)) {
                    builder.appendQueryParameter(p, v)
                }
            }
            val cleaned = builder.build().toString()
            if (cleaned.endsWith("/") && cleaned.count { it == '/' } > 2) cleaned.dropLast(1) else cleaned
        } catch (_: Exception) {
            url
        }
    }
}
