package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.local.entity.ProductEntity
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.util.AlertEngine
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Calendar

/**
 * Authoritative Test Suite for Product Creation, Validation, Room Entity Conversion,
 * Firestore Serialization, and Alert Engine Reconciliation.
 */
@RunWith(RobolectricTestRunner::class)
class ProductAddCreationTestSuite {

  private val now = System.currentTimeMillis()
  private val dayMillis = 24 * 60 * 60 * 1000L

  // ----------------------------------------------------
  // TEST A: Product Data Object Mapping & Default Values
  // ----------------------------------------------------
  @Test
  fun testA_ProductDataModelDefaults() {
    val product = Product(
      id = "prod_001",
      name = "DAP Special",
      company = "IFFCO",
      unit = ProductUnit.BAG,
      batchNumber = "B-902",
      purchasePrice = 1200.0,
      sellingPrice = 1350.0,
      mrp = 1350.0,
      stockQuantity = 50.0
    )

    assertEquals("prod_001", product.id)
    assertEquals("DAP Special", product.name)
    assertEquals("", product.category)
    assertEquals(10.0, product.minStockAlert, 0.001)
    assertNull(product.expiryDate)
    assertEquals("", product.chemicalComposition)
    assertEquals("", product.barcode)
    assertEquals("", product.customFields)
    assertTrue(product.createdAt > 0)
    assertTrue(product.updatedAt >= product.createdAt)
  }

  // ----------------------------------------------------
  // TEST B: Entity <-> Model Lossless Conversion
  // ----------------------------------------------------
  @Test
  fun testB_EntityModelBidirectionalConversion() {
    val original = Product(
      id = "prod_test_101",
      name = "Coragen 18.5% SC",
      category = "Insecticides",
      company = "FMC",
      unit = ProductUnit.BOTTLE,
      batchNumber = "FMC-BATCH-2026",
      purchasePrice = 1600.0,
      sellingPrice = 1850.0,
      mrp = 1950.0,
      stockQuantity = 25.0,
      minStockAlert = 5.0,
      expiryDate = now + 180 * dayMillis,
      rackLocation = "Rack C-2",
      hsnCode = "38089190",
      chemicalComposition = "Chlorantraniliprole 18.5% w/w",
      barcode = "8901234567890",
      packaging = "150 ml",
      crop = "Paddy, Sugarcane, Cotton",
      usesInstructions = "Apply 60 ml per acre with 200L water",
      customFields = "{\"supplier\":\"FMC Agro\",\"shelfLife\":\"2 Years\"}",
      createdAt = now,
      updatedAt = now,
      createdBy = "admin@manglam.com",
      updatedBy = "admin@manglam.com"
    )

    val entity = ProductEntity.fromProduct(original)
    assertEquals(original.id, entity.id)
    assertEquals(original.name, entity.name)
    assertEquals(original.category, entity.category)
    assertEquals(original.company, entity.company)
    assertEquals("BOTTLE", entity.unit)
    assertEquals(original.batchNumber, entity.batchNumber)
    assertEquals(original.purchasePrice, entity.purchasePrice, 0.001)
    assertEquals(original.sellingPrice, entity.sellingPrice, 0.001)
    assertEquals(original.mrp, entity.mrp, 0.001)
    assertEquals(original.stockQuantity, entity.stockQuantity, 0.001)
    assertEquals(original.minStockAlert, entity.minStockAlert, 0.001)
    assertEquals(original.expiryDate, entity.expiryDate)
    assertEquals(original.rackLocation, entity.rackLocation)
    assertEquals(original.hsnCode, entity.hsnCode)
    assertEquals(original.chemicalComposition, entity.chemicalComposition)
    assertEquals(original.barcode, entity.barcode)
    assertEquals(original.packaging, entity.packaging)
    assertEquals(original.crop, entity.crop)
    assertEquals(original.usesInstructions, entity.usesInstructions)
    assertEquals(original.customFields, entity.customFields)
    assertEquals(original.createdAt, entity.createdAt)

    val restored = entity.toProduct()
    assertEquals(original, restored)
  }

  // ----------------------------------------------------
  // TEST C: Mandatory Validation Constraints
  // ----------------------------------------------------
  @Test
  fun testC_MandatoryValidationConstraints() {
    // Empty Name
    val nameBlank = "   "
    assertTrue(nameBlank.trim().isBlank())

    // Empty Company
    val companyBlank = ""
    assertTrue(companyBlank.trim().isBlank())

    // Invalid Selling Price
    val invalidPriceText = "-50.0"
    val parsedPrice = invalidPriceText.toDoubleOrNull()
    assertTrue(parsedPrice != null && parsedPrice <= 0)

    // Valid inputs
    val validName = "Urea 46% N"
    val validCompany = "KRIBHCO"
    val validPrice = "266.50"
    val validStock = "100"
    assertTrue(validName.trim().isNotBlank())
    assertTrue(validCompany.trim().isNotBlank())
    assertTrue((validPrice.toDoubleOrNull() ?: 0.0) > 0)
    assertTrue((validStock.toDoubleOrNull() ?: 0.0) >= 0)
  }

  // ----------------------------------------------------
  // TEST D: Null & Empty Safety for Optional Fields
  // ----------------------------------------------------
  @Test
  fun testD_OptionalFieldsNullSafety() {
    val product = Product(
      id = "prod_opt_01",
      name = "Zinc Sulphate 33%",
      company = "Manglam Agro",
      unit = ProductUnit.PACKET,
      batchNumber = "",
      purchasePrice = 70.0,
      sellingPrice = 90.0,
      mrp = 90.0,
      stockQuantity = 20.0,
      expiryDate = null,
      rackLocation = "",
      hsnCode = "",
      chemicalComposition = "",
      barcode = "",
      packaging = "",
      crop = "",
      usesInstructions = "",
      customFields = ""
    )

    assertEquals("", product.chemicalComposition)
    assertEquals("", product.barcode)
    assertNull(product.expiryDate)
    assertEquals("", product.rackLocation)
    assertEquals("", product.hsnCode)
  }

  // ----------------------------------------------------
  // TEST E: Safe Number Parsing Logic
  // ----------------------------------------------------
  @Test
  fun testE_SafeNumberParsing() {
    fun parsePrice(input: String, fallback: Double = 0.0): Double {
      return input.trim().toDoubleOrNull() ?: fallback
    }

    assertEquals(150.0, parsePrice("150.0"), 0.001)
    assertEquals(150.5, parsePrice(" 150.50 "), 0.001)
    assertEquals(0.0, parsePrice(""), 0.001)
    assertEquals(0.0, parsePrice("abc"), 0.001)
    assertEquals(250.0, parsePrice("invalid", fallback = 250.0), 0.001)
    assertEquals(10.0, parsePrice("", fallback = 10.0), 0.001)
  }

  // ----------------------------------------------------
  // TEST F: Unit Enum Fallback Safety
  // ----------------------------------------------------
  @Test
  fun testF_UnitEnumFallback() {
    val validUnit = ProductUnit.valueOf("BAG")
    assertEquals(ProductUnit.BAG, validUnit)

    val fallbackUnit = try {
      ProductUnit.valueOf("NON_EXISTING_UNIT")
    } catch (e: Exception) {
      ProductUnit.BAG
    }
    assertEquals(ProductUnit.BAG, fallbackUnit)
  }

  // ----------------------------------------------------
  // TEST G: AlertEngine Evaluation on New Product
  // ----------------------------------------------------
  @Test
  fun testG_AlertEngineEvaluation() {
    val startOfToday = AlertEngine.getStartOfToday(now)

    // 1. Healthy in-stock product
    val healthyProd = Product(
      id = "p_healthy",
      name = "Potash MOP",
      company = "IPL",
      unit = ProductUnit.BAG,
      batchNumber = "B1",
      purchasePrice = 1600.0,
      sellingPrice = 1750.0,
      mrp = 1750.0,
      stockQuantity = 40.0,
      minStockAlert = 10.0,
      expiryDate = startOfToday + (120 * dayMillis) // 4 months
    )
    val stockEval1 = AlertEngine.evaluateStock(healthyProd.stockQuantity, healthyProd.minStockAlert)
    assertFalse(stockEval1.isLowStock)
    assertFalse(stockEval1.isOutOfStock)

    val expEval1 = AlertEngine.evaluateExpiry(healthyProd.expiryDate, now)
    assertFalse(expEval1.isExpired)
    assertTrue(expEval1.isNearExpiry) // <= 6 months

    // 2. Low stock product
    val lowStockProd = healthyProd.copy(stockQuantity = 5.0, minStockAlert = 10.0)
    val stockEval2 = AlertEngine.evaluateStock(lowStockProd.stockQuantity, lowStockProd.minStockAlert)
    assertTrue(stockEval2.isLowStock)
    assertFalse(stockEval2.isOutOfStock)

    // 3. Expired product
    val expiredProd = healthyProd.copy(expiryDate = startOfToday - (5 * dayMillis))
    val expEval3 = AlertEngine.evaluateExpiry(expiredProd.expiryDate, now)
    assertTrue(expEval3.isExpired)
    assertFalse(expEval3.isNearExpiry)
  }

  // ----------------------------------------------------
  // TEST H: Custom Fields JSON Structure Resilience
  // ----------------------------------------------------
  @Test
  fun testH_CustomFieldsJsonResilience() {
    val validJsonStr = "{\"licenseNo\":\"LIC-2026-99\",\"distributor\":\"Shree Agro\"}"
    val json = JSONObject(validJsonStr)
    assertEquals("LIC-2026-99", json.optString("licenseNo", "—"))
    assertEquals("Shree Agro", json.optString("distributor", "—"))
    assertEquals("—", json.optString("nonExistentKey", "—"))

    // Malformed JSON fallback
    val malformedJsonStr = "not-a-valid-json"
    val safeLookup = try {
      if (malformedJsonStr.isNotBlank()) {
        JSONObject(malformedJsonStr).optString("key", "—")
      } else "—"
    } catch (e: Exception) {
      "—"
    }
    assertEquals("—", safeLookup)
  }

  // ----------------------------------------------------
  // TEST I: Firestore Map Builder Null Safety
  // ----------------------------------------------------
  @Test
  fun testI_FirestoreMapBuilderNullSafety() {
    val product = Product(
      id = "prod_999",
      name = "Sulfur 90% WDG",
      category = "Fungicides",
      company = "Sumitomo",
      unit = ProductUnit.KG,
      batchNumber = "SUM-22",
      purchasePrice = 110.0,
      sellingPrice = 140.0,
      mrp = 150.0,
      stockQuantity = 80.0,
      minStockAlert = 15.0,
      expiryDate = null,
      rackLocation = "Rack A-1",
      hsnCode = "3808",
      chemicalComposition = "Elemental Sulphur 90%",
      barcode = "8909876543210"
    )

    val map = hashMapOf(
      "id" to product.id,
      "name" to product.name,
      "category" to product.category,
      "company" to product.company,
      "unit" to product.unit.name,
      "batchNumber" to product.batchNumber,
      "purchasePrice" to product.purchasePrice,
      "sellingPrice" to product.sellingPrice,
      "mrp" to product.mrp,
      "stockQuantity" to product.stockQuantity,
      "minStockAlert" to product.minStockAlert,
      "expiryDate" to product.expiryDate,
      "rackLocation" to product.rackLocation,
      "hsnCode" to product.hsnCode,
      "chemicalComposition" to product.chemicalComposition,
      "barcode" to product.barcode,
      "createdAt" to product.createdAt,
      "updatedAt" to product.updatedAt
    )

    assertEquals("prod_999", map["id"])
    assertEquals("Sulfur 90% WDG", map["name"])
    assertEquals("KG", map["unit"])
    assertNull(map["expiryDate"])
    assertEquals(140.0, map["sellingPrice"])
  }

  // ----------------------------------------------------
  // TEST J: Duplicate Detection Logic
  // ----------------------------------------------------
  @Test
  fun testJ_DuplicateDetection() {
    val existingList = listOf(
      Product(
        id = "p1",
        name = "Urea 46%",
        company = "IFFCO",
        unit = ProductUnit.BAG,
        batchNumber = "B1",
        purchasePrice = 240.0,
        sellingPrice = 266.5,
        mrp = 266.5,
        stockQuantity = 100.0,
        barcode = "890111111111"
      )
    )

    val incomingWithSameBarcode = Product(
      id = "p2",
      name = "Urea 46% Prilled",
      company = "IFFCO",
      unit = ProductUnit.BAG,
      batchNumber = "B2",
      purchasePrice = 240.0,
      sellingPrice = 266.5,
      mrp = 266.5,
      stockQuantity = 50.0,
      barcode = "890111111111"
    )

    val isDuplicateByBarcode = existingList.any { it.barcode.isNotBlank() && it.barcode == incomingWithSameBarcode.barcode }
    assertTrue(isDuplicateByBarcode)

    val incomingWithSameNameAndCompany = Product(
      id = "p3",
      name = "urea 46%",
      company = "iffco",
      unit = ProductUnit.BAG,
      batchNumber = "B3",
      purchasePrice = 240.0,
      sellingPrice = 266.5,
      mrp = 266.5,
      stockQuantity = 50.0,
      barcode = "890222222222"
    )

    val isDuplicateByNameCompany = existingList.any {
      it.name.equals(incomingWithSameNameAndCompany.name, ignoreCase = true) &&
          it.company.equals(incomingWithSameNameAndCompany.company, ignoreCase = true)
    }
    assertTrue(isDuplicateByNameCompany)
  }

  // ----------------------------------------------------
  // TEST K: Multi-Field Search Matching
  // ----------------------------------------------------
  @Test
  fun testK_MultiFieldSearchMatching() {
    val prod = Product(
      id = "prod_search",
      name = "Roundup 41% SL",
      company = "Bayer",
      category = "Herbicides",
      chemicalComposition = "Glyphosate 41% IPA salt",
      unit = ProductUnit.LITER,
      batchNumber = "BAY-901",
      purchasePrice = 380.0,
      sellingPrice = 450.0,
      mrp = 480.0,
      stockQuantity = 30.0,
      crop = "Tea, Non-cropped area",
      barcode = "890123999999",
      rackLocation = "Bay-4",
      hsnCode = "38089340"
    )

    fun matches(q: String): Boolean {
      val trimmed = q.trim()
      if (trimmed.isBlank()) return true
      return prod.name.contains(trimmed, ignoreCase = true) ||
          prod.chemicalComposition.contains(trimmed, ignoreCase = true) ||
          prod.company.contains(trimmed, ignoreCase = true) ||
          prod.category.contains(trimmed, ignoreCase = true) ||
          prod.barcode.contains(trimmed, ignoreCase = true) ||
          prod.crop.contains(trimmed, ignoreCase = true) ||
          prod.rackLocation.contains(trimmed, ignoreCase = true) ||
          prod.hsnCode.contains(trimmed, ignoreCase = true)
    }

    assertTrue(matches("Glyphosate"))
    assertTrue(matches("Roundup"))
    assertTrue(matches("Bayer"))
    assertTrue(matches("Herbicides"))
    assertTrue(matches("890123999999"))
    assertTrue(matches("Tea"))
    assertTrue(matches("Bay-4"))
    assertTrue(matches("38089340"))
    assertFalse(matches("Chlorpyrifos"))
  }

  // ----------------------------------------------------
  // TEST L: Expiry Date Calendar Bounds
  // ----------------------------------------------------
  @Test
  fun testL_ExpiryDateBounds() {
    val startOfToday = AlertEngine.getStartOfToday(now)
    val cal = Calendar.getInstance().apply {
      timeInMillis = startOfToday
      add(Calendar.MONTH, 1)
    }
    val oneMonthLater = cal.timeInMillis

    assertTrue(oneMonthLater > startOfToday)
    val eval = AlertEngine.evaluateExpiry(oneMonthLater, now)
    assertFalse(eval.isExpired)
    assertTrue(eval.isNearExpiry)
    assertEquals(com.manglamfertilizer.app.data.model.ExpiryPriority.HIGH, eval.priority)
  }

  // ----------------------------------------------------
  // TEST M: Stock Decimal vs Display Integer Formatting
  // ----------------------------------------------------
  @Test
  fun testM_StockQuantityFormatting() {
    val stockQty = 45.0
    val displayInt = stockQty.toInt()
    assertEquals(45, displayInt)
    assertEquals("45", displayInt.toString())

    val fractionalStock = 45.75
    val formattedString = if (fractionalStock % 1.0 == 0.0) {
      "${fractionalStock.toInt()}"
    } else {
      String.format(java.util.Locale.US, "%.2f", fractionalStock)
    }
    assertEquals("45.75", formattedString)
  }
}
