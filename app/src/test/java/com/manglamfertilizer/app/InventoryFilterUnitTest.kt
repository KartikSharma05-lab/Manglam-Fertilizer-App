package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.ui.inventory.ProductStatusFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class InventoryFilterUnitTest {

  private val now = System.currentTimeMillis()
  private val dayMillis = 24 * 60 * 60 * 1000L
  private val thirtyDaysMillis = 30 * dayMillis

  private val testProducts = listOf(
    Product(
      id = "p1",
      name = "Urea 46% N",
      chemicalComposition = "Nitrogen 46% Prilled",
      category = "Fertilizers",
      company = "IFFCO",
      unit = ProductUnit.BAG,
      batchNumber = "B2026-01",
      purchasePrice = 240.0,
      sellingPrice = 266.5,
      mrp = 266.5,
      stockQuantity = 150.0,
      minStockAlert = 20.0,
      expiryDate = now + 90 * dayMillis, // Healthy
      barcode = "890123450001",
      packaging = "45 kg Bag",
      crop = "Wheat, Rice"
    ),
    Product(
      id = "p2",
      name = "DAP 18:46:0",
      chemicalComposition = "Di-Ammonium Phosphate",
      category = "Fertilizers",
      company = "Coromandel",
      unit = ProductUnit.BAG,
      batchNumber = "B2026-02",
      purchasePrice = 1200.0,
      sellingPrice = 1350.0,
      mrp = 1350.0,
      stockQuantity = 5.0, // LOW STOCK (<= 10)
      minStockAlert = 10.0,
      expiryDate = now + 60 * dayMillis,
      barcode = "890123450002",
      packaging = "50 kg Bag",
      crop = "Wheat, Mustard, Potato"
    ),
    Product(
      id = "p3",
      name = "Karate 5 EC",
      chemicalComposition = "Lambda-cyhalothrin 5% EC",
      category = "Insecticides",
      company = "Syngenta",
      unit = ProductUnit.BOTTLE,
      batchNumber = "SYN-991",
      purchasePrice = 450.0,
      sellingPrice = 520.0,
      mrp = 550.0,
      stockQuantity = 25.0,
      minStockAlert = 5.0,
      expiryDate = now + 15 * dayMillis, // NEAR EXPIRY (within 30 days)
      barcode = "890123450003",
      packaging = "1000 ml",
      crop = "Cotton, Chilli, Tomato"
    ),
    Product(
      id = "p4",
      name = "Coragen 18.5% SC",
      chemicalComposition = "Chlorantraniliprole 18.5% w/w",
      category = "Insecticides",
      company = "FMC",
      unit = ProductUnit.BOTTLE,
      batchNumber = "FMC-002",
      purchasePrice = 1600.0,
      sellingPrice = 1850.0,
      mrp = 1950.0,
      stockQuantity = 2.0, // LOW STOCK & EXPIRED
      minStockAlert = 5.0,
      expiryDate = now - 10 * dayMillis, // EXPIRED (< now)
      barcode = "890123450004",
      packaging = "150 ml",
      crop = "Sugarcane, Paddy"
    )
  )

  @Test
  fun testTotalCountCalculation() {
    assertEquals(4, testProducts.size)
  }

  @Test
  fun testLowStockFilter() {
    val lowStock = testProducts.filter { it.stockQuantity <= it.minStockAlert }
    assertEquals(2, lowStock.size)
    assertTrue(lowStock.any { it.name.contains("DAP") })
    assertTrue(lowStock.any { it.name.contains("Coragen") })
  }

  @Test
  fun testNearExpiryFilter() {
    val startOfToday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val nearExpiry = testProducts.filter { p ->
      p.expiryDate != null && p.expiryDate >= startOfToday && p.expiryDate <= (startOfToday + thirtyDaysMillis)
    }
    assertEquals(1, nearExpiry.size)
    assertEquals("Karate 5 EC", nearExpiry.first().name)
  }

  @Test
  fun testExpiredFilter() {
    val startOfToday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val expired = testProducts.filter { p ->
      p.expiryDate != null && p.expiryDate < startOfToday
    }
    assertEquals(1, expired.size)
    assertEquals("Coragen 18.5% SC", expired.first().name)
  }

  @Test
  fun testMultiFieldSearch_chemicalComposition() {
    val query = "Lambda-cyhalothrin"
    val results = testProducts.filter {
      it.name.contains(query, ignoreCase = true) ||
          it.chemicalComposition.contains(query, ignoreCase = true) ||
          it.company.contains(query, ignoreCase = true)
    }
    assertEquals(1, results.size)
    assertEquals("Karate 5 EC", results.first().name)
  }

  @Test
  fun testMultiFieldSearch_crop() {
    val query = "Mustard"
    val results = testProducts.filter {
      it.crop.contains(query, ignoreCase = true) ||
          it.name.contains(query, ignoreCase = true)
    }
    assertEquals(1, results.size)
    assertEquals("DAP 18:46:0", results.first().name)
  }

  @Test
  fun testMultiFieldSearch_barcode() {
    val query = "890123450004"
    val results = testProducts.filter {
      it.barcode.contains(query, ignoreCase = true) ||
          it.name.contains(query, ignoreCase = true)
    }
    assertEquals(1, results.size)
    assertEquals("Coragen 18.5% SC", results.first().name)
  }

  @Test
  fun testCombinedCategoryAndStatusFilter() {
    val startOfToday = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val filtered = testProducts.filter { p ->
      val matchesCat = p.category.equals("Fertilizers", ignoreCase = true)
      val matchesStatus = p.stockQuantity <= p.minStockAlert
      matchesCat && matchesStatus
    }
    assertEquals(1, filtered.size)
    assertEquals("DAP 18:46:0", filtered.first().name)
  }
}
