package com.manglamfertilizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.manglamfertilizer.app.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
  @Query("SELECT * FROM products ORDER BY name ASC")
  fun getAllProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
  suspend fun getProductById(id: String): ProductEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProducts(products: List<ProductEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProduct(product: ProductEntity)

  @Update
  suspend fun updateProduct(product: ProductEntity)

  @Query("DELETE FROM products WHERE id = :id")
  suspend fun deleteProductById(id: String)

  @Query("DELETE FROM products")
  suspend fun clearAll()
}
