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

    @Query("UPDATE item SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("SELECT * FROM item WHERE text LIKE '%' || :query || '%' OR cleanedText LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun search(query: String): Flow<List<Item>>

    @Query("UPDATE item SET reminderAt = :reminderAt WHERE id = :id")
    suspend fun setReminder(id: String, reminderAt: Long?)
}
