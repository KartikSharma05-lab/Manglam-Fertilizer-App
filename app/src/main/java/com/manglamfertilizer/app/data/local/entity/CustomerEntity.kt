package com.manglamfertilizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manglamfertilizer.app.data.model.Customer

@Entity(tableName = "customers")
data class CustomerEntity(
  @PrimaryKey val id: String,
  val name: String,
  val phoneNumber: String,
  val aadhaarNumber: String = "",
  val village: String = "",
  val address: String = "",
  val totalPurchases: Double,
  val totalDue: Double,
  val lastTransactionDate: Long?,
  val createdAt: Long
) {
  fun toCustomer(): Customer {
    return Customer(
      id = id,
      name = name,
      phoneNumber = phoneNumber,
      aadhaarNumber = aadhaarNumber,
      village = village,
      address = address,
      totalPurchases = totalPurchases,
      totalDue = totalDue,
      lastTransactionDate = lastTransactionDate,
      createdAt = createdAt
    )
  }

  companion object {
    fun fromCustomer(c: Customer): CustomerEntity {
      return CustomerEntity(
        id = c.id,
        name = c.name,
        phoneNumber = c.phoneNumber,
        aadhaarNumber = c.aadhaarNumber,
        village = c.village,
        address = c.address,
        totalPurchases = c.totalPurchases,
        totalDue = c.totalDue,
        lastTransactionDate = c.lastTransactionDate,
        createdAt = c.createdAt
      )
    }
  }
}
