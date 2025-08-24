package com.mrunicorn.sb.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import com.mrunicorn.sb.util.LinkCleaner
import kotlinx.coroutines.flow.Flow

class Repository(private val context: Context, private val dao: ItemDao) {

    fun inbox(query: String?): Flow<List<Item>> =
        if (query.isNullOrBlank()) dao.observeAll() else dao.search(query)

    suspend fun saveTextOrLink(raw: String, sourcePkg: String? = null) {
        val trimmed = raw.trim()
        val isLink = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
        val cleaned = if (isLink) LinkCleaner.clean(trimmed) else null
        val type = if (isLink) ItemType.LINK else ItemType.TEXT
        dao.upsert(Item(type = type, text = trimmed, cleanedText = cleaned, sourcePackage = sourcePkg))
    }

    suspend fun saveImages(uris: List<Uri>, sourcePkg: String? = null) {
        dao.upsert(Item(type = ItemType.IMAGE, imageUris = uris.map { it.toString() }, sourcePackage = sourcePkg))
    }

    suspend fun delete(id: String) = dao.delete(id)
    suspend fun pin(id: String, pinned: Boolean) = dao.setPinned(id, pinned)

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("ShareBuddy", text))
    }
}
