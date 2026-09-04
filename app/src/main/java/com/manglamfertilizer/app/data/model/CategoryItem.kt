package com.manglamfertilizer.app.data.model

data class CategoryItem(
  val id: String,
  val name: String,
  val order: Int = 0,
  val createdAt: Long = System.currentTimeMillis()
)
