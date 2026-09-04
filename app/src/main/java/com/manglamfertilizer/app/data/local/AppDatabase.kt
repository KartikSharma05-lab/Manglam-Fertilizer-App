package com.manglamfertilizer.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.manglamfertilizer.app.data.local.dao.CategoryDao
import com.manglamfertilizer.app.data.local.dao.CreditRecordDao
import com.manglamfertilizer.app.data.local.dao.CustomerDao
import com.manglamfertilizer.app.data.local.dao.InventoryHistoryDao
import com.manglamfertilizer.app.data.local.dao.InvoiceDao
import com.manglamfertilizer.app.data.local.dao.ProductDao
import com.manglamfertilizer.app.data.local.entity.CategoryEntity
import com.manglamfertilizer.app.data.local.entity.CreditRecordEntity
import com.manglamfertilizer.app.data.local.entity.CustomerEntity
import com.manglamfertilizer.app.data.local.entity.InventoryHistoryEntity
import com.manglamfertilizer.app.data.local.entity.InvoiceEntity
import com.manglamfertilizer.app.data.local.entity.ProductEntity

@Database(
  entities = [
    ProductEntity::class,
    CustomerEntity::class,
    InvoiceEntity::class,
    CategoryEntity::class,
    InventoryHistoryEntity::class,
    CreditRecordEntity::class
  ],
  version = 8,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun productDao(): ProductDao
  abstract fun customerDao(): CustomerDao
  abstract fun invoiceDao(): InvoiceDao
  abstract fun categoryDao(): CategoryDao
  abstract fun inventoryHistoryDao(): InventoryHistoryDao
  abstract fun creditRecordDao(): CreditRecordDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "manglam_fertilizer_db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
