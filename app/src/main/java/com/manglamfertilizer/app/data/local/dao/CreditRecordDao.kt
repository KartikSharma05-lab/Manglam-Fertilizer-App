package com.manglamfertilizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manglamfertilizer.app.data.local.entity.CreditRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditRecordDao {
  @Query("SELECT * FROM credit_records ORDER BY createdAt DESC")
  fun getAllCreditRecords(): Flow<List<CreditRecordEntity>>

  @Query("SELECT * FROM credit_records WHERE customerId = :customerId ORDER BY createdAt DESC")
  fun getCreditRecordsByCustomer(customerId: String): Flow<List<CreditRecordEntity>>

  @Query("SELECT * FROM credit_records WHERE id = :id LIMIT 1")
  suspend fun getCreditRecordById(id: String): CreditRecordEntity?

  @Query("SELECT * FROM credit_records WHERE invoiceId = :invoiceId LIMIT 1")
  suspend fun getCreditRecordByInvoiceId(invoiceId: String): CreditRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCreditRecord(record: CreditRecordEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCreditRecords(records: List<CreditRecordEntity>)

  @Update
  suspend fun updateCreditRecord(record: CreditRecordEntity)

  @Query("DELETE FROM credit_records WHERE id = :id")
  suspend fun deleteCreditRecordById(id: String)

  @Query("DELETE FROM credit_records")
  suspend fun clearAll()
}
