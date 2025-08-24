package com.mrunicorn.sb.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.UUID

@Entity
@TypeConverters(Converters::class)
data class Item(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: ItemType,
    val text: String? = null,
    val cleanedText: String? = null,
    val imageUris: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val sourcePackage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
    val reminderAt: Long? = null,
    val label: String? = null
)

enum class ItemType { TEXT, LINK, IMAGE }

object Converters {
    @TypeConverter fun fromList(list: List<String>?): String = list?.joinToString("|:|") ?: ""
    @TypeConverter fun toList(csv: String?): List<String> = csv?.takeIf { it.isNotBlank() }?.split("|:|") ?: emptyList()
}
