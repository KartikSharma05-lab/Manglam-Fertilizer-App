package com.manglamfertilizer.app.data.model

data class Customer(
  val id: String,
  val name: String,
  val phoneNumber: String,
  val aadhaarNumber: String = "",
  val village: String = "",
  val address: String = "",
  val totalPurchases: Double = 0.0,
  val totalDue: Double = 0.0,
  val lastTransactionDate: Long? = null,
  val createdAt: Long = System.currentTimeMillis()
)
