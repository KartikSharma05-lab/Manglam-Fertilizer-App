package com.manglamfertilizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.manglamfertilizer.app.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
  @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
  fun getAllInvoices(): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoices ORDER BY timestamp DESC")
  suspend fun getAllInvoicesList(): List<InvoiceEntity>

  @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
  suspend fun getInvoiceById(id: String): InvoiceEntity?

  @Query("SELECT * FROM invoices WHERE timestamp >= :startTimestamp AND timestamp < :endTimestamp ORDER BY timestamp DESC")
  fun getInvoicesBetween(startTimestamp: Long, endTimestamp: Long): Flow<List<InvoiceEntity>>

  @Query("SELECT * FROM invoices WHERE timestamp >= :startTimestamp ORDER BY timestamp DESC")
  fun getInvoicesSince(startTimestamp: Long): Flow<List<InvoiceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoice(invoice: InvoiceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertInvoices(invoices: List<InvoiceEntity>)

  @Query("DELETE FROM invoices WHERE id = :id")
  suspend fun deleteInvoiceById(id: String)

  @Query("DELETE FROM invoices")
  suspend fun clearAll()
}
