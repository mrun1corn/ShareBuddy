package com.mrunicorn.sb.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.UUID
import android.net.Uri

@Entity
@TypeConverters(Converters::class)
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: ItemType,
    val text: String? = null,
    val cleanedText: String? = null,
    val imageUris: List<Uri> = emptyList(),
    val thumbnailUrl: String? = null,
    val sourcePackage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val reminderAt: Long? = null,
    val label: String? = null
)

enum class ItemType { TEXT, LINK, IMAGE }

object Converters {
    @TypeConverter fun fromStringList(list: List<String>?): String = list?.joinToString("|:|") ?: ""
    @TypeConverter fun toStringList(csv: String?): List<String> = csv?.takeIf { it.isNotBlank() }?.split("|:|") ?: emptyList()

    @TypeConverter fun fromUriList(list: List<Uri>?): String = list?.joinToString("|:|") { it.toString() } ?: ""
    @TypeConverter fun toUriList(csv: String?): List<Uri> = csv?.takeIf { it.isNotBlank() }?.split("|:|")?.map { Uri.parse(it) } ?: emptyList()
}
