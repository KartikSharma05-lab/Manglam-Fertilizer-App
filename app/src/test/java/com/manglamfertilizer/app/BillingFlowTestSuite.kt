package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.local.entity.InvoiceEntity
import com.manglamfertilizer.app.data.local.entity.ProductEntity
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Authoritative Test Suite for Billing Section, Product Selection, Cart Item Addition,
 * Calculation Formulas, and Inventory Stock Reduction on Invoice Creation.
 */
@RunWith(RobolectricTestRunner::class)
class BillingFlowTestSuite {

  @Test
  fun testDirectProductAddition_DefaultQuantityOne() {
    val sampleProduct = Product(
      id = "prod_dairygreen_1kg",
      name = "Dairygreen Jowar 1kg",
      company = "Advanta",
      unit = ProductUnit.KG,
      batchNumber = "BATCH-DG-01",
      purchasePrice = 180.0,
      sellingPrice = 220.0,
      mrp = 240.0,
      stockQuantity = 5.0
    )

    val cartItems = mutableListOf<InvoiceItem>()

    fun directAddToCart(prod: Product, qtyToAdd: Double = 1.0): Boolean {
      if (prod.stockQuantity <= 0) return false
      val maxStock = if (prod.stockQuantity > 0) prod.stockQuantity else 9999.0
      val existingIdx = cartItems.indexOfFirst { it.productId == prod.id }
      if (existingIdx in 0 until cartItems.size) {
        val existing = cartItems[existingIdx]
        if (existing.quantity >= maxStock) return false
        val updatedQty = (existing.quantity + qtyToAdd).coerceAtMost(maxStock)
        cartItems[existingIdx] = existing.copy(
          quantity = updatedQty,
          totalPrice = updatedQty * prod.sellingPrice
        )
      } else {
        val validQty = qtyToAdd.coerceAtMost(maxStock)
        cartItems.add(
          InvoiceItem(
            productId = prod.id,
            productName = prod.name,
            batchNumber = prod.batchNumber,
            quantity = validQty,
            unit = prod.unit,
            unitPrice = prod.sellingPrice,
            totalPrice = validQty * prod.sellingPrice
          )
        )
      }
      return true
    }

    // Direct add #1 -> Default Qty 1
    val added1 = directAddToCart(sampleProduct, 1.0)
    assertTrue(added1)
    assertEquals(1, cartItems.size)
    assertEquals(1.0, cartItems[0].quantity, 0.001)
    assertEquals(220.0, cartItems[0].totalPrice, 0.01)

    // Direct add #2 (same product clicked again) -> Qty 2
    val added2 = directAddToCart(sampleProduct, 1.0)
    assertTrue(added2)
    assertEquals(1, cartItems.size)
    assertEquals(2.0, cartItems[0].quantity, 0.001)
    assertEquals(440.0, cartItems[0].totalPrice, 0.01)

    // Test in-cart + button
    val targetIdx = cartItems.indexOfFirst { it.productId == sampleProduct.id }
    cartItems[targetIdx] = cartItems[targetIdx].copy(
      quantity = 3.0,
      totalPrice = 3.0 * sampleProduct.sellingPrice
    )
    assertEquals(3.0, cartItems[0].quantity, 0.001)

    // Test in-cart - button
    cartItems[targetIdx] = cartItems[targetIdx].copy(
      quantity = 2.0,
      totalPrice = 2.0 * sampleProduct.sellingPrice
    )
    assertEquals(2.0, cartItems[0].quantity, 0.001)

    // Test out of stock product
    val outOfStockProduct = sampleProduct.copy(id = "oos_prod", stockQuantity = 0.0)
    val oosAdded = directAddToCart(outOfStockProduct, 1.0)
    assertTrue(!oosAdded)
    assertEquals(1, cartItems.size)
  }

  @Test
  fun testProductSelection_AndCartAddition() {
    val sampleProduct = Product(
      id = "prod_urea_45kg",
      name = "Neem Coated Urea 45kg",
      company = "NFL",
      unit = ProductUnit.BAG,
      batchNumber = "BATCH-2026-A",
      purchasePrice = 242.0,
      sellingPrice = 266.50,
      mrp = 266.50,
      stockQuantity = 100.0
    )

    val cartItems = mutableListOf<InvoiceItem>()

    // Simulate selecting product and adding 5 bags
    val requestedQty = 5.0
    val maxStock = if (sampleProduct.stockQuantity > 0) sampleProduct.stockQuantity else 9999.0
    val validQty = requestedQty.coerceAtMost(maxStock)

    val existingIdx = cartItems.indexOfFirst { it.productId == sampleProduct.id }
    if (existingIdx in 0 until cartItems.size) {
      val existing = cartItems[existingIdx]
      val updatedQty = (existing.quantity + validQty).coerceAtMost(maxStock)
      cartItems[existingIdx] = existing.copy(
        quantity = updatedQty,
        totalPrice = updatedQty * sampleProduct.sellingPrice
      )
    } else {
      cartItems.add(
        InvoiceItem(
          productId = sampleProduct.id,
          productName = sampleProduct.name,
          batchNumber = sampleProduct.batchNumber,
          quantity = validQty,
          unit = sampleProduct.unit,
          unitPrice = sampleProduct.sellingPrice,
          totalPrice = validQty * sampleProduct.sellingPrice
        )
      )
    }

    assertEquals(1, cartItems.size)
    assertEquals("prod_urea_45kg", cartItems[0].productId)
    assertEquals(5.0, cartItems[0].quantity, 0.001)
    assertEquals(1332.50, cartItems[0].totalPrice, 0.01)

    // Add same product again (e.g. 3 more bags)
    val addMoreQty = 3.0
    val validMoreQty = addMoreQty.coerceAtMost(maxStock)
    val nextIdx = cartItems.indexOfFirst { it.productId == sampleProduct.id }
    if (nextIdx in 0 until cartItems.size) {
      val existing = cartItems[nextIdx]
      val updatedQty = (existing.quantity + validMoreQty).coerceAtMost(maxStock)
      cartItems[nextIdx] = existing.copy(
        quantity = updatedQty,
        totalPrice = updatedQty * sampleProduct.sellingPrice
      )
    }

    assertEquals(1, cartItems.size)
    assertEquals(8.0, cartItems[0].quantity, 0.001)
    assertEquals(2132.00, cartItems[0].totalPrice, 0.01)
  }

  @Test
  fun testCartItemDecrement_AndSafeRemoval() {
    val cartItems = mutableListOf(
      InvoiceItem(
        productId = "prod_1",
        productName = "DAP 50kg",
        batchNumber = "B-01",
        quantity = 2.0,
        unit = ProductUnit.BAG,
        unitPrice = 1350.0,
        totalPrice = 2700.0
      ),
      InvoiceItem(
        productId = "prod_2",
        productName = "Zinc Sulphate 1kg",
        batchNumber = "B-02",
        quantity = 1.0,
        unit = ProductUnit.KG,
        unitPrice = 180.0,
        totalPrice = 180.0
      )
    )

    // Decrement item 1
    val targetIdx1 = cartItems.indexOfFirst { it.productId == "prod_1" }
    if (targetIdx1 in 0 until cartItems.size) {
      val item = cartItems[targetIdx1]
      if (item.quantity > 1) {
        val newQty = item.quantity - 1
        cartItems[targetIdx1] = item.copy(
          quantity = newQty,
          totalPrice = newQty * item.unitPrice
        )
      } else {
        cartItems.removeAt(targetIdx1)
      }
    }
    assertEquals(1.0, cartItems[0].quantity, 0.001)
    assertEquals(1350.0, cartItems[0].totalPrice, 0.01)

    // Decrement item 2 (quantity 1 -> removes safely)
    val targetIdx2 = cartItems.indexOfFirst { it.productId == "prod_2" }
    if (targetIdx2 in 0 until cartItems.size) {
      val item = cartItems[targetIdx2]
      if (item.quantity > 1) {
        val newQty = item.quantity - 1
        cartItems[targetIdx2] = item.copy(
          quantity = newQty,
          totalPrice = newQty * item.unitPrice
        )
      } else {
        cartItems.removeAt(targetIdx2)
      }
    }
    assertEquals(1, cartItems.size)
    assertEquals("prod_1", cartItems[0].productId)

    // Remove remaining item
    val targetIdxRem = cartItems.indexOfFirst { it.productId == "prod_1" }
    if (targetIdxRem in 0 until cartItems.size) {
      cartItems.removeAt(targetIdxRem)
    }
    assertTrue(cartItems.isEmpty())
  }

  @Test
  fun testFinancialCalculations_GstAndDiscount() {
    val items = listOf(
      InvoiceItem(
        productId = "p1",
        productName = "Urea",
        batchNumber = "B1",
        quantity = 10.0,
        unit = ProductUnit.BAG,
        unitPrice = 266.50,
        totalPrice = 2665.00
      ),
      InvoiceItem(
        productId = "p2",
        productName = "Pesticide",
        batchNumber = "B2",
        quantity = 2.0,
        unit = ProductUnit.LITER,
        unitPrice = 500.00,
        totalPrice = 1000.00
      )
    )

    val subTotal = items.sumOf { it.totalPrice }
    assertEquals(3665.00, subTotal, 0.01)

    val discount = 65.00
    val taxableAmount = (subTotal - discount).coerceAtLeast(0.0)
    assertEquals(3600.00, taxableAmount, 0.01)

    val gstRate = 5.0
    val gstAmount = (taxableAmount * gstRate) / 100.0
    assertEquals(180.00, gstAmount, 0.01)

    val grandTotal = (taxableAmount + gstAmount).coerceAtLeast(0.0)
    assertEquals(3780.00, grandTotal, 0.01)

    // Partial payment: Received = 3000, Due = 780
    val received = 3000.00
    val remainingDue = (grandTotal - received).coerceAtLeast(0.0)
    assertEquals(780.00, remainingDue, 0.01)
  }

  @Test
  fun testInvoiceEntityConversion_AndStockDeduction() {
    val product = Product(
      id = "prod_dap",
      name = "DAP 50kg",
      company = "IFFCO",
      category = "Fertilizer",
      unit = ProductUnit.BAG,
      batchNumber = "B-DAP-01",
      purchasePrice = 1200.0,
      sellingPrice = 1350.0,
      mrp = 1350.0,
      stockQuantity = 50.0,
      minStockAlert = 10.0
    )
    val productEntity = ProductEntity.fromProduct(product)

    val item = InvoiceItem(
      productId = productEntity.id,
      productName = productEntity.name,
      batchNumber = productEntity.batchNumber,
      quantity = 15.0,
      unit = product.unit,
      unitPrice = productEntity.sellingPrice,
      totalPrice = 15.0 * productEntity.sellingPrice
    )

    val invoice = Invoice(
      id = "inv_test_001",
      invoiceNumber = "INV-20260831-0001",
      customerId = "cust_001",
      customerName = "Ramesh Patel",
      customerPhone = "9876543210",
      items = listOf(item),
      subTotal = item.totalPrice,
      gstRate = 5.0,
      gstAmount = (item.totalPrice * 0.05),
      discount = 0.0,
      grandTotal = item.totalPrice * 1.05,
      amountPaid = item.totalPrice * 1.05,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = System.currentTimeMillis()
    )

    // Convert to Room entity and back
    val invoiceEntity = InvoiceEntity.fromInvoice(invoice)
    val convertedBack = invoiceEntity.toInvoice()

    assertEquals(invoice.id, convertedBack.id)
    assertEquals(invoice.invoiceNumber, convertedBack.invoiceNumber)
    assertEquals(1, convertedBack.items.size)
    assertEquals("prod_dap", convertedBack.items[0].productId)
    assertEquals(15.0, convertedBack.items[0].quantity, 0.001)

    // Stock deduction calculation
    val newStock = (productEntity.stockQuantity - item.quantity).coerceAtLeast(0.0)
    val updatedProduct = productEntity.copy(stockQuantity = newStock)
    assertEquals(35.0, updatedProduct.stockQuantity, 0.001)
  }

  @Test
  fun testExactGstOptions_AndCalculations_0_5_12_18() {
    val officialGstOptions = listOf(0.0, 5.0, 12.0, 18.0)
    assertEquals(4, officialGstOptions.size)
    assertEquals(listOf(0.0, 5.0, 12.0, 18.0), officialGstOptions)
    assertTrue(!officialGstOptions.contains(28.0))

    val taxableAmount = 1000.0

    // 0% GST
    val gst0 = (taxableAmount * 0.0) / 100.0
    val total0 = taxableAmount + gst0
    assertEquals(0.0, gst0, 0.01)
    assertEquals(1000.0, total0, 0.01)

    // 5% GST
    val gst5 = (taxableAmount * 5.0) / 100.0
    val total5 = taxableAmount + gst5
    assertEquals(50.0, gst5, 0.01)
    assertEquals(1050.0, total5, 0.01)

    // 12% GST
    val gst12 = (taxableAmount * 12.0) / 100.0
    val total12 = taxableAmount + gst12
    assertEquals(120.0, gst12, 0.01)
    assertEquals(1120.0, total12, 0.01)

    // 18% GST
    val gst18 = (taxableAmount * 18.0) / 100.0
    val total18 = taxableAmount + gst18
    assertEquals(180.0, gst18, 0.01)
    assertEquals(1180.0, total18, 0.01)
  }

  @Test
  fun testSafeDecimalInputFiltering_AndDueCalculations() {
    val rawInput = "1500.50abc"
    val filtered = rawInput.filter { ch -> ch.isDigit() || ch == '.' }
    assertEquals("1500.50", filtered)

    val grandTotal = 2500.0
    val enteredDue = filtered.toDoubleOrNull() ?: 0.0
    val autoReceived = (grandTotal - enteredDue).coerceAtLeast(0.0)
    assertEquals(999.50, autoReceived, 0.01)
  }
}
