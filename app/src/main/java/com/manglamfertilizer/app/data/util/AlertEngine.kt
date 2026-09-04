package com.manglamfertilizer.app.data.util

import androidx.compose.ui.graphics.Color
import com.manglamfertilizer.app.data.model.AlertType
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Deterministic Product Status system with strict priority ordering:
 * 1. EXPIRED (Red + ⭐)
 * 2. EXPIRING_1_MONTH (Red)
 * 3. EXPIRING_3_MONTHS (Yellow)
 * 4. EXPIRING_6_MONTHS (Yellow)
 * 5. LOW_STOCK (Blue)
 * 6. HEALTHY (Green)
 */
enum class ProductStatus(val priority: Int, val label: String) {
  EXPIRED(1, "Expired"),
  EXPIRING_1_MONTH(2, "Expiring within 1 Month"),
  EXPIRING_3_MONTHS(3, "Expiring within 3 Months"),
  EXPIRING_6_MONTHS(4, "Expiring within 6 Months"),
  LOW_STOCK(5, "Low Stock"),
  HEALTHY(6, "Healthy")
}

data class ProductStatusInfo(
  val status: ProductStatus,
  val nameColor: Color,
  val showStar: Boolean,
  val badgeLabel: String,
  val statusMessage: String
)

/**
 * Authoritative evaluation engine for stock and expiry alerts across MANGALAM FERTILIZER.
 * Evaluates individual minimum stock thresholds and strict calendar expiry windows
 * (≤ 1 Month = HIGH, ≤ 3 Months = MEDIUM, ≤ 6 Months = NORMAL, Past = EXPIRED).
 */
object AlertEngine {

  data class ExpiryEvaluation(
    val isExpired: Boolean,
    val isNearExpiry: Boolean,
    val priority: ExpiryPriority?,
    val remainingTimeText: String,
    val daysRemaining: Long
  )

  data class StockEvaluation(
    val isLowStock: Boolean,
    val isOutOfStock: Boolean,
    val message: String
  )

  /**
   * Calculates Start of Today in Milliseconds (00:00:00.000) for exact calendar comparisons.
   */
  fun getStartOfToday(currentTimeMs: Long = System.currentTimeMillis()): Long {
    return Calendar.getInstance().apply {
      timeInMillis = currentTimeMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }

  /**
   * Evaluates product expiry against the 3 required thresholds:
   * 1. Already Expired: expiryDate < startOfToday
   * 2. ≤ 1 Month: HIGH PRIORITY EXPIRY ALERT
   * 3. ≤ 3 Months: MEDIUM PRIORITY EXPIRY ALERT
   * 4. ≤ 6 Months: EXPIRY ALERT
   * > 6 Months: Valid / In-date (no expiry alert)
   */
  fun evaluateExpiry(expiryDate: Long?, currentTimeMs: Long = System.currentTimeMillis()): ExpiryEvaluation {
    if (expiryDate == null || expiryDate <= 0) {
      return ExpiryEvaluation(
        isExpired = false,
        isNearExpiry = false,
        priority = null,
        remainingTimeText = "Not specified",
        daysRemaining = Long.MAX_VALUE
      )
    }

    val startOfToday = getStartOfToday(currentTimeMs)
    val diffMillis = expiryDate - startOfToday
    val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

    val cal1Month = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 1)
    }.timeInMillis

    val cal3Months = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 3)
    }.timeInMillis

    val cal6Months = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 6)
    }.timeInMillis

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val expFormatted = dateFormat.format(Date(expiryDate))

    return when {
      expiryDate < startOfToday -> {
        val daysAgo = kotlin.math.abs(days)
        val agoText = if (daysAgo == 0L) "Today" else if (daysAgo == 1L) "1 day ago" else "$daysAgo days ago"
        ExpiryEvaluation(
          isExpired = true,
          isNearExpiry = false,
          priority = null,
          remainingTimeText = "Expired ($agoText • $expFormatted)",
          daysRemaining = days
        )
      }
      expiryDate <= cal1Month -> {
        val daysText = if (days <= 1) "in $days day" else "in $days days"
        ExpiryEvaluation(
          isExpired = false,
          isNearExpiry = true,
          priority = ExpiryPriority.HIGH,
          remainingTimeText = "Expires $daysText (≤ 1 Month • $expFormatted)",
          daysRemaining = days
        )
      }
      expiryDate <= cal3Months -> {
        val approxMonths = (days / 30.0).let { if (it < 1.0) 1 else it.toInt() }
        ExpiryEvaluation(
          isExpired = false,
          isNearExpiry = true,
          priority = ExpiryPriority.MEDIUM,
          remainingTimeText = "Expires in ~$approxMonths mos ($days days • $expFormatted)",
          daysRemaining = days
        )
      }
      expiryDate <= cal6Months -> {
        val approxMonths = (days / 30.0).let { if (it < 1.0) 1 else it.toInt() }
        ExpiryEvaluation(
          isExpired = false,
          isNearExpiry = true,
          priority = ExpiryPriority.NORMAL,
          remainingTimeText = "Expires in ~$approxMonths mos ($days days • $expFormatted)",
          daysRemaining = days
        )
      }
      else -> {
        ExpiryEvaluation(
          isExpired = false,
          isNearExpiry = false,
          priority = null,
          remainingTimeText = "Valid ($expFormatted)",
          daysRemaining = days
        )
      }
    }
  }

  /**
   * Evaluates product stock against its individual minimum stock alert.
   */
  fun evaluateStock(quantity: Double, minStock: Double, unitName: String = "Units"): StockEvaluation {
    return when {
      quantity <= 0 -> {
        StockEvaluation(
          isLowStock = true,
          isOutOfStock = true,
          message = "Out of Stock! (0 $unitName remaining)"
        )
      }
      quantity <= minStock -> {
        StockEvaluation(
          isLowStock = true,
          isOutOfStock = false,
          message = "Low Stock: $quantity $unitName (Min: $minStock $unitName)"
        )
      }
      else -> {
        StockEvaluation(
          isLowStock = false,
          isOutOfStock = false,
          message = "In Stock ($quantity $unitName)"
        )
      }
    }
  }

  /**
   * Generates all active stock & expiry alerts from live inventory.
   * Ensures:
   * 1. No duplicate alerts for the same condition.
   * 2. An expired product is NEVER marked as merely near expiry.
   * 3. Sorted by urgency.
   */
  fun generateAlerts(products: List<Product>, currentTimeMs: Long = System.currentTimeMillis()): List<StockAlert> {
    val alerts = mutableListOf<StockAlert>()

    products.forEach { prod ->
      // 1. Stock Alert evaluation (Individual product minimum stock)
      val stockEval = evaluateStock(prod.stockQuantity, prod.minStockAlert, prod.unit.name)
      if (stockEval.isOutOfStock) {
        alerts.add(
          StockAlert(
            id = "alert_out_of_stock_${prod.id}",
            productId = prod.id,
            productName = prod.name,
            currentStock = prod.stockQuantity,
            minStock = prod.minStockAlert,
            unit = prod.unit,
            alertType = AlertType.OUT_OF_STOCK,
            expiryDate = prod.expiryDate,
            expiryPriority = null,
            company = prod.company,
            chemicalComposition = prod.chemicalComposition,
            category = prod.category,
            batchNumber = prod.batchNumber,
            rackLocation = prod.rackLocation,
            remainingTimeText = "",
            statusMessage = stockEval.message,
            timestamp = currentTimeMs
          )
        )
      } else if (stockEval.isLowStock) {
        alerts.add(
          StockAlert(
            id = "alert_low_stock_${prod.id}",
            productId = prod.id,
            productName = prod.name,
            currentStock = prod.stockQuantity,
            minStock = prod.minStockAlert,
            unit = prod.unit,
            alertType = AlertType.LOW_STOCK,
            expiryDate = prod.expiryDate,
            expiryPriority = null,
            company = prod.company,
            chemicalComposition = prod.chemicalComposition,
            category = prod.category,
            batchNumber = prod.batchNumber,
            rackLocation = prod.rackLocation,
            remainingTimeText = "",
            statusMessage = stockEval.message,
            timestamp = currentTimeMs
          )
        )
      }

      // 2. Expiry Alert evaluation (≤6mo, ≤3mo, ≤1mo, Expired)
      val expiryEval = evaluateExpiry(prod.expiryDate, currentTimeMs)
      if (expiryEval.isExpired) {
        alerts.add(
          StockAlert(
            id = "alert_expired_${prod.id}",
            productId = prod.id,
            productName = prod.name,
            currentStock = prod.stockQuantity,
            minStock = prod.minStockAlert,
            unit = prod.unit,
            alertType = AlertType.EXPIRED,
            expiryDate = prod.expiryDate,
            expiryPriority = null,
            company = prod.company,
            chemicalComposition = prod.chemicalComposition,
            category = prod.category,
            batchNumber = prod.batchNumber,
            rackLocation = prod.rackLocation,
            remainingTimeText = expiryEval.remainingTimeText,
            statusMessage = "Batch has expired! Immediate removal required.",
            timestamp = currentTimeMs
          )
        )
      } else if (expiryEval.isNearExpiry) {
        val statusMsg = when (expiryEval.priority) {
          ExpiryPriority.HIGH -> "High Priority Expiry Alert (≤ 1 Month)"
          ExpiryPriority.MEDIUM -> "Medium Priority Expiry Alert (≤ 3 Months)"
          ExpiryPriority.NORMAL -> "Expiry Alert (≤ 6 Months)"
          null -> "Near Expiry Alert"
        }
        alerts.add(
          StockAlert(
            id = "alert_near_expiry_${prod.id}",
            productId = prod.id,
            productName = prod.name,
            currentStock = prod.stockQuantity,
            minStock = prod.minStockAlert,
            unit = prod.unit,
            alertType = AlertType.NEAR_EXPIRY,
            expiryDate = prod.expiryDate,
            expiryPriority = expiryEval.priority,
            company = prod.company,
            chemicalComposition = prod.chemicalComposition,
            category = prod.category,
            batchNumber = prod.batchNumber,
            rackLocation = prod.rackLocation,
            remainingTimeText = expiryEval.remainingTimeText,
            statusMessage = statusMsg,
            timestamp = currentTimeMs
          )
        )
      }
    }

    // Sort by severity: Expired / Out of stock first, High priority expiry, Low stock, Medium expiry, Normal expiry
    return alerts.sortedWith(
      compareBy<StockAlert> {
        when (it.alertType) {
          AlertType.EXPIRED -> 0
          AlertType.OUT_OF_STOCK -> 1
          AlertType.NEAR_EXPIRY -> when (it.expiryPriority) {
            ExpiryPriority.HIGH -> 2
            ExpiryPriority.MEDIUM -> 4
            ExpiryPriority.NORMAL -> 5
            null -> 5
          }
          AlertType.LOW_STOCK -> 3
        }
      }.thenBy { it.productName.lowercase() }
    )
  }

  /**
   * Deterministic status evaluator for a product adhering to the strict priority system:
   * 1. EXPIRED -> Red + ⭐
   * 2. EXPIRING ≤ 1 MONTH -> Red
   * 3. EXPIRING ≤ 3 MONTHS -> Yellow
   * 4. EXPIRING ≤ 6 MONTHS -> Yellow
   * 5. LOW STOCK -> Blue
   * 6. HEALTHY -> Green
   */
  fun evaluateProductStatus(product: Product, currentTimeMs: Long = System.currentTimeMillis()): ProductStatusInfo {
    val startOfToday = getStartOfToday(currentTimeMs)
    val expiryDate = product.expiryDate
    val isLowStock = product.stockQuantity <= product.minStockAlert

    val cal1Month = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 1)
    }.timeInMillis

    val cal3Months = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 3)
    }.timeInMillis

    val cal6Months = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 6)
    }.timeInMillis

    return when {
      // 1. EXPIRED
      expiryDate != null && expiryDate > 0 && expiryDate < startOfToday -> {
        ProductStatusInfo(
          status = ProductStatus.EXPIRED,
          nameColor = SoftRed,
          showStar = true,
          badgeLabel = "EXPIRED",
          statusMessage = "Batch expired! Immediate removal required."
        )
      }
      // 2. EXPIRING WITHIN 1 MONTH
      expiryDate != null && expiryDate > 0 && expiryDate <= cal1Month -> {
        ProductStatusInfo(
          status = ProductStatus.EXPIRING_1_MONTH,
          nameColor = SoftRed,
          showStar = false,
          badgeLabel = "EXPIRING (≤1 MO)",
          statusMessage = "High Priority Expiry Alert (≤ 1 Month)"
        )
      }
      // 3. EXPIRING WITHIN 3 MONTHS
      expiryDate != null && expiryDate > 0 && expiryDate <= cal3Months -> {
        ProductStatusInfo(
          status = ProductStatus.EXPIRING_3_MONTHS,
          nameColor = GoldAmber,
          showStar = false,
          badgeLabel = "EXPIRING (≤3 MOS)",
          statusMessage = "Medium Priority Expiry Alert (≤ 3 Months)"
        )
      }
      // 4. EXPIRING WITHIN 6 MONTHS
      expiryDate != null && expiryDate > 0 && expiryDate <= cal6Months -> {
        ProductStatusInfo(
          status = ProductStatus.EXPIRING_6_MONTHS,
          nameColor = GoldAmber,
          showStar = false,
          badgeLabel = "EXPIRING (≤6 MOS)",
          statusMessage = "Expiry Alert (≤ 6 Months)"
        )
      }
      // 5. LOW STOCK / OUT OF STOCK
      isLowStock -> {
        ProductStatusInfo(
          status = ProductStatus.LOW_STOCK,
          nameColor = InfoSky,
          showStar = false,
          badgeLabel = if (product.stockQuantity <= 0) "OUT OF STOCK" else "LOW STOCK",
          statusMessage = if (product.stockQuantity <= 0) "Out of Stock!" else "Low Stock: ${product.stockQuantity.toInt()} ${product.unit.name}"
        )
      }
      // 6. HEALTHY / NORMAL
      else -> {
        ProductStatusInfo(
          status = ProductStatus.HEALTHY,
          nameColor = Emerald400,
          showStar = false,
          badgeLabel = "IN STOCK",
          statusMessage = "In Stock (${product.stockQuantity.toInt()} ${product.unit.name})"
        )
      }
    }
  }
}
