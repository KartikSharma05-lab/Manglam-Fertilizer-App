package com.manglamfertilizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manglamfertilizer.app.data.model.InventoryHistoryItem

@Entity(tableName = "inventory_history")
data class InventoryHistoryEntity(
  @PrimaryKey val id: String,
  val timestamp: Long,
  val userName: String,
  val userEmail: String,
  val userId: String = "",
  val userRole: String = "STAFF",
  val productId: String = "",
  val actionType: String,
  val productName: String,
  val details: String,
  val previousValue: String?,
  val newValue: String?
) {
  fun toHistoryItem(): InventoryHistoryItem {
    return InventoryHistoryItem(
      id = id,
      timestamp = timestamp,
      userName = userName,
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      productId = productId,
      actionType = actionType,
      productName = productName,
      details = details,
      previousValue = previousValue,
      newValue = newValue
    )
  }

  companion object {
    fun fromHistoryItem(item: InventoryHistoryItem): InventoryHistoryEntity {
      return InventoryHistoryEntity(
        id = item.id,
        timestamp = item.timestamp,
        userName = item.userName,
        userEmail = item.userEmail,
        userId = item.userId,
        userRole = item.userRole,
        productId = item.productId,
        actionType = item.actionType,
        productName = item.productName,
        details = item.details,
        previousValue = item.previousValue,
        newValue = item.newValue
      )
    }
  }
}

