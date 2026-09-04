package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.AlertType
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.util.AlertEngine
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEngineTest {

  @Test
  fun testCase1_expiryWithin15Days_evaluatesToHighPriority() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    // 15 days from today
    cal.add(Calendar.DAY_OF_MONTH, 15)
    val expiry15Days = cal.timeInMillis

    val eval = AlertEngine.evaluateExpiry(expiry15Days, now)

    assertFalse("Should not be expired", eval.isExpired)
    assertTrue("Should be near expiry", eval.isNearExpiry)
    assertEquals("Should be High Priority (<= 1 Month)", ExpiryPriority.HIGH, eval.priority)
  }

  @Test
  fun testCase2_expiryWithin2Months_evaluatesToMediumPriority() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    // 2 months from today (e.g. 60 days)
    cal.add(Calendar.MONTH, 2)
    val expiry2Months = cal.timeInMillis

    val eval = AlertEngine.evaluateExpiry(expiry2Months, now)

    assertFalse("Should not be expired", eval.isExpired)
    assertTrue("Should be near expiry", eval.isNearExpiry)
    assertEquals("Should be Medium Priority (<= 3 Months)", ExpiryPriority.MEDIUM, eval.priority)
  }

  @Test
  fun testCase3_expiryWithin5Months_evaluatesToNormalPriority() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    // 5 months from today
    cal.add(Calendar.MONTH, 5)
    val expiry5Months = cal.timeInMillis

    val eval = AlertEngine.evaluateExpiry(expiry5Months, now)

    assertFalse("Should not be expired", eval.isExpired)
    assertTrue("Should be near expiry", eval.isNearExpiry)
    assertEquals("Should be Normal Expiry Alert (<= 6 Months)", ExpiryPriority.NORMAL, eval.priority)
  }

  @Test
  fun testCase4_expiryPastDate_evaluatesToExpired() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    // 10 days in the past
    cal.add(Calendar.DAY_OF_MONTH, -10)
    val expiredPast = cal.timeInMillis

    val eval = AlertEngine.evaluateExpiry(expiredPast, now)

    assertTrue("Should be expired", eval.isExpired)
    assertFalse("Should not be flagged as merely near-expiry", eval.isNearExpiry)
    assertNull("Expired has no priority enum", eval.priority)
  }

  @Test
  fun testCase5_lowStockIndividualThreshold_evaluatesToLowStock() {
    val evalLow = AlertEngine.evaluateStock(quantity = 5.0, minStock = 10.0, unitName = "KG")
    assertTrue("Should be low stock", evalLow.isLowStock)
    assertFalse("Should not be out of stock", evalLow.isOutOfStock)

    val evalZero = AlertEngine.evaluateStock(quantity = 0.0, minStock = 10.0, unitName = "KG")
    assertTrue("Should be low stock", evalZero.isLowStock)
    assertTrue("Should be out of stock", evalZero.isOutOfStock)
  }

  @Test
  fun testCase6_sufficientStock_evaluatesToInStock() {
    val eval = AlertEngine.evaluateStock(quantity = 20.0, minStock = 10.0, unitName = "BAG")
    assertFalse("Should not be low stock", eval.isLowStock)
    assertFalse("Should not be out of stock", eval.isOutOfStock)
  }

  @Test
  fun testAlertGeneration_fullInventoryPipeline() {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val now = cal.timeInMillis

    val calExp1 = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_MONTH, 15) }
    val calExp2 = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, 2) }
    val calExp3 = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, 5) }
    val calPast = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_MONTH, -5) }
    val calFuture = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.MONTH, 10) }

    val products = listOf(
      Product(
        id = "p1",
        name = "Urea 46% N",
        company = "IFFCO",
        chemicalComposition = "46% Nitrogen",
        stockQuantity = 4.0,
        minStockAlert = 10.0,
        purchasePrice = 250.0,
        sellingPrice = 266.0,
        mrp = 280.0,
        expiryDate = calExp1.timeInMillis,
        unit = ProductUnit.BAG,
        batchNumber = "B-001",
        rackLocation = "Rack A-1"
      ),
      Product(
        id = "p2",
        name = "DAP 18:46:0",
        company = "KRIBHCO",
        chemicalComposition = "18% N, 46% P2O5",
        stockQuantity = 25.0,
        minStockAlert = 10.0,
        purchasePrice = 1300.0,
        sellingPrice = 1350.0,
        mrp = 1400.0,
        expiryDate = calExp2.timeInMillis,
        unit = ProductUnit.BAG,
        batchNumber = "B-002",
        rackLocation = "Rack A-2"
      ),
      Product(
        id = "p3",
        name = "Zinc Sulphate 33%",
        company = "Coromandel",
        chemicalComposition = "Zn 33%, S 15%",
        stockQuantity = 50.0,
        minStockAlert = 5.0,
        purchasePrice = 60.0,
        sellingPrice = 75.0,
        mrp = 80.0,
        expiryDate = calExp3.timeInMillis,
        unit = ProductUnit.KG,
        batchNumber = "B-003",
        rackLocation = "Rack B-1"
      ),
      Product(
        id = "p4",
        name = "Chlorpyrifos 50% EC",
        company = "Bayer",
        chemicalComposition = "Chlorpyrifos 50% EC",
        stockQuantity = 8.0,
        minStockAlert = 2.0,
        purchasePrice = 450.0,
        sellingPrice = 520.0,
        mrp = 550.0,
        expiryDate = calPast.timeInMillis,
        unit = ProductUnit.BOTTLE,
        batchNumber = "B-004",
        rackLocation = "Rack C-1"
      ),
      Product(
        id = "p5",
        name = "NPK 19:19:19",
        company = "Tata Rallis",
        chemicalComposition = "19% N, 19% P, 19% K",
        stockQuantity = 100.0,
        minStockAlert = 10.0,
        purchasePrice = 110.0,
        sellingPrice = 130.0,
        mrp = 140.0,
        expiryDate = calFuture.timeInMillis, // 10 months away (Valid, no expiry alert)
        unit = ProductUnit.BAG,
        batchNumber = "B-005",
        rackLocation = "Rack D-1"
      )
    )

    val alerts = AlertEngine.generateAlerts(products, now)

    // Expected alerts:
    // - p1: Low Stock alert AND Near Expiry (High Priority)
    // - p2: Near Expiry (Medium Priority)
    // - p3: Near Expiry (Normal Priority)
    // - p4: Expired alert
    // - p5: No alerts (Stock 100 > 10, Expiry 10 months > 6 months)

    assertEquals(5, alerts.size)

    val expiredAlert = alerts.firstOrNull { it.alertType == AlertType.EXPIRED }
    assertEquals("p4", expiredAlert?.productId)

    val highPriorityAlert = alerts.firstOrNull { it.expiryPriority == ExpiryPriority.HIGH }
    assertEquals("p1", highPriorityAlert?.productId)

    val mediumPriorityAlert = alerts.firstOrNull { it.expiryPriority == ExpiryPriority.MEDIUM }
    assertEquals("p2", mediumPriorityAlert?.productId)

    val normalPriorityAlert = alerts.firstOrNull { it.expiryPriority == ExpiryPriority.NORMAL }
    assertEquals("p3", normalPriorityAlert?.productId)

    val lowStockAlert = alerts.firstOrNull { it.alertType == AlertType.LOW_STOCK }
    assertEquals("p1", lowStockAlert?.productId)
  }
}
