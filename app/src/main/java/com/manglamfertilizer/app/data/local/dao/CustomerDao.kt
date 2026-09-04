package com.manglamfertilizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manglamfertilizer.app.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
  @Query("SELECT * FROM customers ORDER BY name ASC")
  fun getAllCustomers(): Flow<List<CustomerEntity>>

  @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
  suspend fun getCustomerById(id: String): CustomerEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomer(customer: CustomerEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCustomers(customers: List<CustomerEntity>)

  @Update
  suspend fun updateCustomer(customer: CustomerEntity)

  @Query("DELETE FROM customers WHERE id = :id")
  suspend fun deleteCustomerById(id: String)

  @Query("DELETE FROM customers")
  suspend fun clearAll()
}
