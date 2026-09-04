package com.manglamfertilizer.app.data.model

data class Invoice(
  val id: String,
  val invoiceNumber: String,
  val customerId: String?,
  val customerName: String,
  val customerPhone: String = "",
  val customerAadhaar: String = "",
  val customerAddress: String = "",
  val customerVillage: String = "",
  val items: List<InvoiceItem>,
  val subTotal: Double,
  val gstRate: Double = 0.0,
  val gstAmount: Double = 0.0,
  val discount: Double = 0.0,
  val grandTotal: Double,
  val amountPaid: Double,
  val remainingDue: Double = 0.0,
  val dueDate: Long? = null,
  val paymentMode: PaymentMode = PaymentMode.CASH,
  val timestamp: Long = System.currentTimeMillis(),
  val createdBy: String = "Admin"
)

data class InvoiceItem(
  val productId: String,
  val productName: String,
  val batchNumber: String = "",
  val quantity: Double,
  val unit: ProductUnit,
  val unitPrice: Double,
  val totalPrice: Double
)

enum class PaymentMode {
  CASH,
  UPI,
  CARD,
  CHEQUE,
  CREDIT,
  SPLIT,
  OTHER
}
