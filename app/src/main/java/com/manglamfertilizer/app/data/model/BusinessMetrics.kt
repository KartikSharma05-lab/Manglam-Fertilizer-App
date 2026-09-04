package com.manglamfertilizer.app.data.model

data class DashboardMetrics(
  val todaySales: Double = 0.0,
  val todayInvoicesCount: Int = 0,
  val totalCustomerDues: Double = 0.0,
  val lowStockCount: Int = 0,
  val nearExpiryCount: Int = 0,
  val expiredCount: Int = 0,
  val totalStockValue: Double = 0.0
)

data class StockAlert(
  val id: String = "",
  val productId: String,
  val productName: String,
  val currentStock: Double,
  val minStock: Double,
  val unit: ProductUnit,
  val alertType: AlertType,
  val expiryDate: Long? = null,
  val expiryPriority: ExpiryPriority? = null,
  val company: String = "",
  val chemicalComposition: String = "",
  val category: String = "",
  val batchNumber: String = "",
  val rackLocation: String = "",
  val remainingTimeText: String = "",
  val statusMessage: String = "",
  val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
  LOW_STOCK,
  OUT_OF_STOCK,
  NEAR_EXPIRY,
  EXPIRED
}

enum class ExpiryPriority {
  HIGH,    // <= 1 month (approx 30 days) - HIGH PRIORITY EXPIRY ALERT
  MEDIUM,  // <= 3 months (approx 90 days) - MEDIUM PRIORITY EXPIRY ALERT
  NORMAL   // <= 6 months (approx 180 days) - EXPIRY ALERT
}

