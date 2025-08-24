package com.mrunicorn.sb.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM Item ORDER BY pinned DESC, createdAt DESC")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM Item WHERE (text LIKE '%' || :query || '%' OR cleanedText LIKE '%' || :query || '%') ORDER BY pinned DESC, createdAt DESC")
    fun search(query: String): Flow<List<Item>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Item)

    @Query("SELECT * FROM Item WHERE id = :id")
    suspend fun getItemById(id: String): Item?

    @Query("UPDATE Item SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean)

    @Query("DELETE FROM Item WHERE id = :id")
    suspend fun delete(id: String)
}
