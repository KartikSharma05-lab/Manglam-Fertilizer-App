package com.manglamfertilizer.app

import androidx.compose.ui.graphics.Color
import com.manglamfertilizer.app.data.model.AlertType
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.data.util.AlertEngine
import com.manglamfertilizer.app.ui.alerts.resolveAlertCardDisplay
import com.manglamfertilizer.app.ui.theme.GoldAmberLight
import com.manglamfertilizer.app.ui.theme.SoftRed
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertPriorityAndCardTest {

  @Test
  fun testCase1_healthyProduct_producesNoAlerts() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    cal.add(Calendar.MONTH, 10) // 10 months away
    val healthyProd = Product(
      id = "healthy_1",
      name = "Healthy Fertilizer 50kg",
      company = "IFFCO",
      stockQuantity = 50.0,
      minStockAlert = 10.0,
      purchasePrice = 200.0,
      sellingPrice = 250.0,
      mrp = 270.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.BAG,
      batchNumber = "B-HEALTHY"
    )

    val alerts = AlertEngine.generateAlerts(listOf(healthyProd), now)
    assertTrue("Healthy product should not generate any alerts", alerts.isEmpty())
  }

  @Test
  fun testCase2_lowStock_productNameWhite_reasonLowStock() {
    val prod = Product(
      id = "low_stock_1",
      name = "Blender",
      company = "Generic",
      stockQuantity = 2.0,
      minStockAlert = 5.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = null,
      unit = ProductUnit.PIECE,
      batchNumber = "B-01"
    )

    val alert = StockAlert(
      id = "alert_1",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.LOW_STOCK,
      statusMessage = "Low Stock"
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(alert))
    assertEquals("Low-stock product name must be WHITE", Color.White, display.nameColor)
    assertFalse("Low-stock product must NOT have a star", display.showStar)
    assertEquals("Reason must be Low Stock", "Low Stock", display.reasonText)
  }

  @Test
  fun testCase3_expiryWithin6Months_productNameYellow_reasonNearExpiry6Months() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis
    cal.add(Calendar.MONTH, 5)

    val prod = Product(
      id = "exp_6m",
      name = "KAROKARI 100gm",
      company = "Rallis",
      stockQuantity = 20.0,
      minStockAlert = 5.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.PACKET,
      batchNumber = "B-02"
    )

    val alert = StockAlert(
      id = "alert_2",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.NEAR_EXPIRY,
      expiryPriority = ExpiryPriority.NORMAL,
      expiryDate = cal.timeInMillis
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(alert))
    assertEquals("6-month expiry product name must be YELLOW", GoldAmberLight, display.nameColor)
    assertFalse("6-month expiry product must NOT have a star", display.showStar)
    assertEquals("Reason must be Near Expiry — ≤ 6 months", "Near Expiry — ≤ 6 months", display.reasonText)
  }

  @Test
  fun testCase4_expiryWithin3Months_productNameYellow_reasonNearExpiry3Months() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    cal.add(Calendar.MONTH, 2)

    val prod = Product(
      id = "exp_3m",
      name = "LARVEE 250ml",
      company = "Bayer",
      stockQuantity = 20.0,
      minStockAlert = 5.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.BOTTLE,
      batchNumber = "B-03"
    )

    val alert = StockAlert(
      id = "alert_3",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.NEAR_EXPIRY,
      expiryPriority = ExpiryPriority.MEDIUM,
      expiryDate = cal.timeInMillis
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(alert))
    assertEquals("3-month expiry product name must be YELLOW", GoldAmberLight, display.nameColor)
    assertFalse("3-month expiry product must NOT have a star", display.showStar)
    assertEquals("Reason must be Near Expiry — ≤ 3 months", "Near Expiry — ≤ 3 months", display.reasonText)
  }

  @Test
  fun testCase5_expiryWithin1Month_productNameRed_reasonExpiringWithin1Month() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    cal.add(Calendar.DAY_OF_MONTH, 15)

    val prod = Product(
      id = "exp_1m",
      name = "Tag Vanish 20gm",
      company = "Coromandel",
      stockQuantity = 20.0,
      minStockAlert = 5.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.PACKET,
      batchNumber = "B-04"
    )

    val alert = StockAlert(
      id = "alert_4",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.NEAR_EXPIRY,
      expiryPriority = ExpiryPriority.HIGH,
      expiryDate = cal.timeInMillis
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(alert))
    assertEquals("1-month expiry product name must be RED", SoftRed, display.nameColor)
    assertFalse("1-month expiry product must NOT have a star", display.showStar)
    assertEquals("Reason must be Expiring within 1 month", "Expiring within 1 month", display.reasonText)
  }

  @Test
  fun testCase6_alreadyExpired_productNameRed_showStarTrue_reasonExpired() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    cal.add(Calendar.DAY_OF_MONTH, -10) // 10 days ago

    val prod = Product(
      id = "exp_past",
      name = "Expired Product",
      company = "Advanta",
      stockQuantity = 20.0,
      minStockAlert = 5.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.KG,
      batchNumber = "B-05"
    )

    val alert = StockAlert(
      id = "alert_5",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.EXPIRED,
      expiryDate = cal.timeInMillis
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(alert))
    assertEquals("Expired product name must be RED", SoftRed, display.nameColor)
    assertTrue("Expired product MUST have a star icon", display.showStar)
    assertEquals("Reason must be Expired", "Expired", display.reasonText)
  }

  @Test
  fun testCase7_lowStockAndExpiryWithin1Month_priorityTakesRed_reasonCombined() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    cal.add(Calendar.DAY_OF_MONTH, 12)

    val prod = Product(
      id = "combined_1m_low",
      name = "Multi Alert Product",
      company = "Syngenta",
      stockQuantity = 2.0, // Low stock (min is 10)
      minStockAlert = 10.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.LITER,
      batchNumber = "B-06"
    )

    val lowStockAlert = StockAlert(
      id = "alert_low",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.LOW_STOCK
    )

    val highExpAlert = StockAlert(
      id = "alert_exp",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.NEAR_EXPIRY,
      expiryPriority = ExpiryPriority.HIGH,
      expiryDate = cal.timeInMillis
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(lowStockAlert, highExpAlert))
    assertEquals("Expiry within 1 month priority takes precedence -> RED", SoftRed, display.nameColor)
    assertFalse(display.showStar)
    assertEquals("Reason must combine expiry and low stock", "Expiring within 1 month • Low Stock", display.reasonText)
  }

  @Test
  fun testCase8_lowStockAndExpired_priorityTakesRedWithStar_reasonCombined() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    cal.add(Calendar.DAY_OF_MONTH, -5)

    val prod = Product(
      id = "combined_exp_low",
      name = "Expired and Low Stock Prod",
      company = "Tata",
      stockQuantity = 1.0,
      minStockAlert = 10.0,
      purchasePrice = 100.0,
      sellingPrice = 150.0,
      mrp = 160.0,
      expiryDate = cal.timeInMillis,
      unit = ProductUnit.BAG,
      batchNumber = "B-07"
    )

    val lowStockAlert = StockAlert(
      id = "alert_low",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.LOW_STOCK
    )

    val expAlert = StockAlert(
      id = "alert_exp",
      productId = prod.id,
      productName = prod.name,
      currentStock = prod.stockQuantity,
      minStock = prod.minStockAlert,
      unit = prod.unit,
      alertType = AlertType.EXPIRED,
      expiryDate = cal.timeInMillis
    )

    val display = resolveAlertCardDisplay(prod.id, prod.name, prod, listOf(lowStockAlert, expAlert))
    assertEquals("Expired takes highest priority -> RED", SoftRed, display.nameColor)
    assertTrue("Expired must show star ⭐", display.showStar)
    assertEquals("Reason must be Expired • Low Stock", "Expired • Low Stock", display.reasonText)
  }
}
