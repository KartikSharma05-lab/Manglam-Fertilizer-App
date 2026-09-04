package com.manglamfertilizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manglamfertilizer.app.data.model.CreditRecord

@Entity(tableName = "credit_records")
data class CreditRecordEntity(
  @PrimaryKey val id: String,
  val customerId: String,
  val customerName: String,
  val invoiceId: String,
  val invoiceNumber: String,
  val amount: Double,
  val dueDate: Long?,
  val paidAmount: Double,
  val remainingAmount: Double,
  val status: String,
  val createdAt: Long,
  val createdBy: String,
  val createdByEmail: String,
  val updatedAt: Long
) {
  fun toCreditRecord(): CreditRecord = CreditRecord(
    id = id,
    customerId = customerId,
    customerName = customerName,
    invoiceId = invoiceId,
    invoiceNumber = invoiceNumber,
    amount = amount,
    dueDate = dueDate,
    paidAmount = paidAmount,
    remainingAmount = remainingAmount,
    status = status,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByEmail = createdByEmail,
    updatedAt = updatedAt
  )

  companion object {
    fun fromCreditRecord(item: CreditRecord): CreditRecordEntity = CreditRecordEntity(
      id = item.id,
      customerId = item.customerId,
      customerName = item.customerName,
      invoiceId = item.invoiceId,
      invoiceNumber = item.invoiceNumber,
      amount = item.amount,
      dueDate = item.dueDate,
      paidAmount = item.paidAmount,
      remainingAmount = item.remainingAmount,
      status = item.status,
      createdAt = item.createdAt,
      createdBy = item.createdBy,
      createdByEmail = item.createdByEmail,
      updatedAt = item.updatedAt
    )
  }
}
