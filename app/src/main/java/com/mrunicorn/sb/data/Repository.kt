package com.mrunicorn.sb.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import com.mrunicorn.sb.util.LinkCleaner
import com.mrunicorn.sb.util.LinkThumbnailExtractor
import kotlinx.coroutines.flow.Flow

class Repository(private val context: Context, val dao: ItemDao) {

    fun inbox(query: String?): Flow<List<Item>> =
        if (query.isNullOrBlank()) dao.observeAll() else dao.search(query)

    suspend fun saveTextOrLink(raw: String, sourcePkg: String? = null, label: String? = null): Item {
        val trimmed = raw.trim()
        val isLink = trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)
        val cleaned = if (isLink) LinkCleaner.clean(trimmed) else null
        val type = if (isLink) ItemType.LINK else ItemType.TEXT
        val thumbnailUrl = if (isLink) LinkThumbnailExtractor.extractThumbnailUrl(trimmed) else null
        val item = Item(type = type, text = trimmed, cleanedText = cleaned, sourcePackage = sourcePkg, thumbnailUrl = thumbnailUrl, label = label)
        dao.upsert(item)
        return item
    }

    suspend fun saveImages(uris: List<Uri>, sourcePkg: String? = null, label: String? = null): Item {
        val imageUrisToSave = uris.map { uri ->
            try {
                // Attempt to take persistent permission
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Copy image to internal storage
                val fileName = "image_${System.currentTimeMillis()}.jpg"
                val file = File(context.filesDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // If successful, return the FileProvider URI
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (e: Exception) {
                e.printStackTrace()
                // If copying fails, return the original URI as a fallback
                uri
            }
        }
        val item = Item(type = ItemType.IMAGE, imageUris = imageUrisToSave, sourcePackage = sourcePkg, label = label)
        dao.upsert(item)
        return item
    }
    suspend fun delete(id: String) = dao.delete(id)
    suspend fun pin(id: String, pinned: Boolean) = dao.setPinned(id, pinned)

    suspend fun updateLabel(id: String, label: String?) {
        val item = dao.getItemById(id)
        if (item != null) {
            dao.upsert(item.copy(label = label))
        }
    }

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("ShareBuddy", text))
    }
}