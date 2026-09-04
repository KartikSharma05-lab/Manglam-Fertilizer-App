package com.manglamfertilizer.app.data.model

data class NotificationItem(
  val id: String,
  val title: String,
  val message: String,
  val timestamp: Long = System.currentTimeMillis(),
  val type: String = "INFO",
  val isRead: Boolean = false
)
