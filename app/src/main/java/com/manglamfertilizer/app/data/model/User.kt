package com.manglamfertilizer.app.data.model

data class User(
  val id: String, // Firebase UID
  val name: String,
  val email: String,
  val role: UserRole = UserRole.ADMIN,
  val active: Boolean = true,
  val phoneNumber: String = "",
  val branchName: String = "Main Branch",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val lastLogin: Long = System.currentTimeMillis()
)

enum class UserRole {
  ADMIN,
  STAFF
}

data class GreetingInfo(
  val greeting: String,
  val iconName: String
)
