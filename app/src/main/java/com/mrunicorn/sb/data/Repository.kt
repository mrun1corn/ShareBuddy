package com.mrunicorn.sb.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
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

    suspend fun saveImages(
        uris: List<Uri>,
        sourcePkg: String? = null,
        label: String? = null
    ): Item {
        val resolver = context.contentResolver
        val mtm = MimeTypeMap.getSingleton()

        val imageUrisToSave = uris.map { src ->
            try {
                // Persist permission on the *incoming* shared content URI (if possible)
                if (src.scheme == "content") {
                    try {
                        resolver.takePersistableUriPermission(
                            src,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // Some share intents don't include persistable flags; safe to ignore.
                    }
                }

                // Work out the extension from MIME type
                val mime = resolver.getType(src)
                val ext = mime?.let { mtm.getExtensionFromMimeType(it) }

                if (ext != null) {
                    // (Optional, but recommended) keep images in a subfolder
                    val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
                    val fileName = "image_${System.currentTimeMillis()}.$ext"
                    val file = File(imagesDir, fileName)

                    resolver.openInputStream(src)?.use { input ->
                        file.outputStream().use { output -> input.copyTo(output) }
                    }

                    // Wrap with FileProvider and *grant* read permission to our own app/process
                    val fpUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    context.grantUriPermission(
                        context.packageName,
                        fpUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    fpUri
                } else {
                    // Unknown type; fall back to original content URI
                    src
                }
            } catch (e: Exception) {
                e.printStackTrace()
                src
            }
        }

        val item = Item(
            type = ItemType.IMAGE,
            imageUris = imageUrisToSave,
            sourcePackage = sourcePkg,
            label = label
        )
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

    suspend fun setReminder(id: String, reminderAt: Long?) = dao.setReminder(id, reminderAt)

    fun copyToClipboard(text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("ShareBuddy", text))
    }
    
    fun copyImageToClipboard(imageUri: android.net.Uri) {
    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
    val clip = android.content.ClipData.newUri(context.contentResolver, "ShareBuddy Image", imageUri)
    cm.setPrimaryClip(clip)
    }

    companion object {
        fun sortAndFilter(list: List<Item>, filter: ItemFilter, sort: ItemSort): List<Item> {
            val sorted = when (sort) {
                ItemSort.Date -> list.sortedByDescending { it.createdAt }
                ItemSort.Name -> list.sortedBy { it.text ?: "" }
                ItemSort.Label -> list.sortedBy { it.label ?: "" }
            }
            return when (filter) {
                ItemFilter.All -> sorted
                ItemFilter.Links -> sorted.filter { it.type == ItemType.LINK }
                ItemFilter.Text -> sorted.filter { it.type == ItemType.TEXT }
                ItemFilter.Images -> sorted.filter { it.type == ItemType.IMAGE }
            }
        }
    }
}

enum class ItemFilter { All, Links, Text, Images }
enum class ItemSort { Date, Name, Label }
