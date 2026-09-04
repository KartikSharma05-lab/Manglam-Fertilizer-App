package com.manglamfertilizer.app.data.model

/**
 * Authoritative Credit and Due record model.
 * Cloud Firestore Collection: credit_records/{creditId}
 */
data class CreditRecord(
  val id: String,
  val customerId: String,
  val customerName: String = "",
  val invoiceId: String,
  val invoiceNumber: String = "",
  val amount: Double,
  val dueDate: Long? = null,
  val paidAmount: Double = 0.0,
  val remainingAmount: Double = 0.0,
  val status: String = "PENDING", // PENDING, PARTIAL, PAID
  val createdAt: Long = System.currentTimeMillis(),
  val createdBy: String = "Admin",
  val createdByEmail: String = "",
  val updatedAt: Long = System.currentTimeMillis()
)
