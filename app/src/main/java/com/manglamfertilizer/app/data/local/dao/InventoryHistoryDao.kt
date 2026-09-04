package com.manglamfertilizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manglamfertilizer.app.data.local.entity.InventoryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryHistoryDao {
  @Query("SELECT * FROM inventory_history ORDER BY timestamp DESC")
  fun getAllHistory(): Flow<List<InventoryHistoryEntity>>

  @Query("SELECT * FROM inventory_history ORDER BY timestamp DESC LIMIT :limit")
  fun getRecentHistory(limit: Int): Flow<List<InventoryHistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(item: InventoryHistoryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistories(items: List<InventoryHistoryEntity>)

  @Query("DELETE FROM inventory_history WHERE id = :id")
  suspend fun deleteHistoryById(id: String)

  @Query("DELETE FROM inventory_history")
  suspend fun clearHistory()
}
