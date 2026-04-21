package com.mrunicorn.sb.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Item)

    @Query("SELECT * FROM item ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun getItemById(id: String): Item?

    @Query("DELETE FROM item WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM item WHERE id IN (:ids)")
    suspend fun deleteBulk(ids: List<String>)

    @Query("UPDATE item SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("UPDATE item SET pinned = :pinned WHERE id IN (:ids)")
    suspend fun setPinnedBulk(ids: List<String>, pinned: Boolean)

    @Query("""
        SELECT * FROM item 
        WHERE (:filterType IS NULL OR type = :filterType)
        AND (:query IS NULL OR text LIKE '%' || :query || '%' OR cleanedText LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%')
        ORDER BY 
            CASE WHEN :sortBy = 'Date' THEN createdAt END DESC,
            CASE WHEN :sortBy = 'Name' THEN text END ASC,
            CASE WHEN :sortBy = 'Label' THEN label END ASC
    """)
    fun observeFiltered(query: String?, filterType: ItemType?, sortBy: String): Flow<List<Item>>

    @Query("UPDATE item SET reminderAt = :reminderAt, deleteAfterReminder = :deleteAfter WHERE id = :id")
    suspend fun setReminder(id: String, reminderAt: Long?, deleteAfter: Boolean)

    // One-shot read for background tasks (not a Flow)
    @Query("SELECT * FROM item WHERE reminderAt IS NOT NULL AND reminderAt > :now")
    suspend fun getPendingReminders(now: Long): List<Item>
}
