package com.manglamfertilizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manglamfertilizer.app.data.model.CategoryItem

@Entity(tableName = "categories")
data class CategoryEntity(
  @PrimaryKey val id: String,
  val name: String,
  val displayOrder: Int = 0,
  val createdAt: Long = System.currentTimeMillis()
) {
  fun toCategoryItem(): CategoryItem {
    return CategoryItem(
      id = id,
      name = name,
      order = displayOrder,
      createdAt = createdAt
    )
  }

  companion object {
    fun fromCategoryItem(item: CategoryItem): CategoryEntity {
      return CategoryEntity(
        id = item.id,
        name = item.name,
        displayOrder = item.order,
        createdAt = item.createdAt
      )
    }
  }
}
