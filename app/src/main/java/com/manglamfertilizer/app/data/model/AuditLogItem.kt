package com.manglamfertilizer.app.data.model

data class AuditLogItem(
  val logId: String = "",
  val userId: String = "",
  val userEmail: String = "",
  val userRole: String = "STAFF", // "ADMIN" or "STAFF"
  val action: String = "",
  val entityType: String = "",
  val entityId: String = "",
  val description: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val deviceInstallationId: String = "",
  val metadata: Map<String, String> = emptyMap()
)
