package com.manglamfertilizer.app.data.model

data class InventoryHistoryItem(
  val id: String,
  val timestamp: Long = System.currentTimeMillis(),
  val userName: String,
  val userEmail: String,
  val userId: String = "",
  val userRole: String = "STAFF",
  val productId: String = "",
  val actionType: String,
  val productName: String,
  val details: String,
  val previousValue: String? = null,
  val newValue: String? = null
)
