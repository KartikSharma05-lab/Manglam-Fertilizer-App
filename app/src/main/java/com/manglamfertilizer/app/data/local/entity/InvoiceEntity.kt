package com.manglamfertilizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.ProductUnit
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "invoices")
data class InvoiceEntity(
  @PrimaryKey val id: String,
  val invoiceNumber: String,
  val customerId: String?,
  val customerName: String,
  val customerPhone: String,
  val customerAadhaar: String = "",
  val customerAddress: String = "",
  val customerVillage: String = "",
  val itemsJson: String,
  val subTotal: Double,
  val gstRate: Double = 0.0,
  val gstAmount: Double = 0.0,
  val discount: Double,
  val grandTotal: Double,
  val amountPaid: Double,
  val remainingDue: Double,
  val dueDate: Long? = null,
  val paymentMode: String,
  val timestamp: Long,
  val createdBy: String
) {
  fun toInvoice(): Invoice {
    val itemsList = mutableListOf<InvoiceItem>()
    try {
      val jsonArray = JSONArray(itemsJson)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        val unt = try {
          ProductUnit.valueOf(obj.optString("unit", "BAG"))
        } catch (e: Exception) {
          ProductUnit.BAG
        }
        itemsList.add(
          InvoiceItem(
            productId = obj.optString("productId"),
            productName = obj.optString("productName"),
            batchNumber = obj.optString("batchNumber"),
            quantity = obj.optDouble("quantity", 0.0),
            unit = unt,
            unitPrice = obj.optDouble("unitPrice", 0.0),
            totalPrice = obj.optDouble("totalPrice", 0.0)
          )
        )
      }
    } catch (e: Exception) {
      // Ignored
    }

    val mode = try {
      PaymentMode.valueOf(paymentMode)
    } catch (e: Exception) {
      PaymentMode.CASH
    }

    return Invoice(
      id = id,
      invoiceNumber = invoiceNumber,
      customerId = customerId,
      customerName = customerName,
      customerPhone = customerPhone,
      customerAadhaar = customerAadhaar,
      customerAddress = customerAddress,
      customerVillage = customerVillage,
      items = itemsList,
      subTotal = subTotal,
      gstRate = gstRate,
      gstAmount = gstAmount,
      discount = discount,
      grandTotal = grandTotal,
      amountPaid = amountPaid,
      remainingDue = remainingDue,
      dueDate = dueDate,
      paymentMode = mode,
      timestamp = timestamp,
      createdBy = createdBy
    )
  }

  companion object {
    fun fromInvoice(inv: Invoice): InvoiceEntity {
      val jsonArray = JSONArray()
      inv.items.forEach { item ->
        val obj = JSONObject()
        obj.put("productId", item.productId)
        obj.put("productName", item.productName)
        obj.put("batchNumber", item.batchNumber)
        obj.put("quantity", item.quantity)
        obj.put("unit", item.unit.name)
        obj.put("unitPrice", item.unitPrice)
        obj.put("totalPrice", item.totalPrice)
        jsonArray.put(obj)
      }

      return InvoiceEntity(
        id = inv.id,
        invoiceNumber = inv.invoiceNumber,
        customerId = inv.customerId,
        customerName = inv.customerName,
        customerPhone = inv.customerPhone,
        customerAadhaar = inv.customerAadhaar,
        customerAddress = inv.customerAddress,
        customerVillage = inv.customerVillage,
        itemsJson = jsonArray.toString(),
        subTotal = inv.subTotal,
        gstRate = inv.gstRate,
        gstAmount = inv.gstAmount,
        discount = inv.discount,
        grandTotal = inv.grandTotal,
        amountPaid = inv.amountPaid,
        remainingDue = inv.remainingDue,
        dueDate = inv.dueDate,
        paymentMode = inv.paymentMode.name,
        timestamp = inv.timestamp,
        createdBy = inv.createdBy
      )
    }
  }
}
