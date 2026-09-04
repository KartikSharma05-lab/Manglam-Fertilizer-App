package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.model.InvoiceNumberValidationResult
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.InvoiceNumberManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceNumberingUnitTest {

  @Test
  fun `test default starting invoice number is 2026001`() {
    val config = InvoiceNumberConfig()
    assertEquals(2026001L, config.startingNumber)
    assertEquals(2026001L, config.nextInvoiceNumber)
    assertNull(config.lastIssuedNumber)
    assertEquals("2026001", config.formattedNextNumber())
  }

  @Test
  fun `test formatNumber with custom prefix and suffix`() {
    val config = InvoiceNumberConfig(
      startingNumber = 2026001L,
      nextInvoiceNumber = 2026001L,
      prefix = "INV-",
      suffix = "-MF"
    )
    assertEquals("INV-2026001-MF", config.formatNumber(2026001L))
    assertEquals("INV-2026002-MF", config.formatNumber(2026002L))
    assertEquals("INV-#-MF", config.formatPattern())
  }

  @Test
  fun `test extractNumericValue from various format styles`() {
    assertEquals(2026001L, InvoiceNumberManager.extractNumericValue("2026001"))
    assertEquals(2026001L, InvoiceNumberManager.extractNumericValue("INV-2026001"))
    assertEquals(2026005L, InvoiceNumberManager.extractNumericValue("INV-2026005-MF"))
    assertEquals(42L, InvoiceNumberManager.extractNumericValue("INV-00042"))
    assertNull(InvoiceNumberManager.extractNumericValue("NO_DIGITS"))
    assertNull(InvoiceNumberManager.extractNumericValue(""))
  }

  @Test
  fun `test findHighestIssuedInvoiceNumber from list of invoices`() {
    val dummyItems = listOf(InvoiceItem("p1", "Urea", "B1", 1.0, ProductUnit.BAG, 300.0, 300.0))
    val invoices = listOf(
      Invoice(id = "1", invoiceNumber = "2026001", customerId = "c1", customerName = "Ram", customerPhone = "9876543210", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0, paymentMode = PaymentMode.CASH),
      Invoice(id = "2", invoiceNumber = "INV-2026003", customerId = "c2", customerName = "Shyam", customerPhone = "9876543211", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0, paymentMode = PaymentMode.CASH),
      Invoice(id = "3", invoiceNumber = "2026002", customerId = "c3", customerName = "Gopal", customerPhone = "9876543212", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0, paymentMode = PaymentMode.CASH)
    )

    val highest = InvoiceNumberManager.findHighestIssuedInvoiceNumber(invoices)
    assertEquals(2026003L, highest)
  }

  @Test
  fun `test validateStartingNumber accepts valid number higher than existing`() {
    val dummyItems = listOf(InvoiceItem("p1", "Urea", "B1", 1.0, ProductUnit.BAG, 300.0, 300.0))
    val existing = listOf(
      Invoice(id = "1", invoiceNumber = "2026001", customerId = "c1", customerName = "Ram", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0)
    )
    val result = InvoiceNumberManager.validateStartingNumber("2026002", existing)
    assertTrue(result is InvoiceNumberValidationResult.Valid)
  }

  @Test
  fun `test validateStartingNumber rejects zero or empty`() {
    val resultZero = InvoiceNumberManager.validateStartingNumber("0", emptyList())
    assertTrue(resultZero is InvoiceNumberValidationResult.Error)

    val resultEmpty = InvoiceNumberManager.validateStartingNumber("", emptyList())
    assertTrue(resultEmpty is InvoiceNumberValidationResult.Error)
  }

  @Test
  fun `test validateStartingNumber rejects existing duplicate number`() {
    val dummyItems = listOf(InvoiceItem("p1", "Urea", "B1", 1.0, ProductUnit.BAG, 300.0, 300.0))
    val existing = listOf(
      Invoice(id = "1", invoiceNumber = "2026005", customerId = "c1", customerName = "Ram", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0)
    )
    val result = InvoiceNumberManager.validateStartingNumber("2026005", existing)
    assertTrue(result is InvoiceNumberValidationResult.Error)
  }

  @Test
  fun `test validateNextNumberOverride warning on large gap`() {
    val config = InvoiceNumberConfig(startingNumber = 2026001L, nextInvoiceNumber = 2026002L)
    val result = InvoiceNumberManager.validateNextNumberOverride("2028000", config, emptyList())
    assertTrue(result is InvoiceNumberValidationResult.Warning)
  }

  @Test
  fun `test sequence continuity when invoice is deleted does not rewind counter`() {
    // Starting state: 2026001, 2026002, 2026003 created
    val dummyItems = listOf(InvoiceItem("p1", "Urea", "B1", 1.0, ProductUnit.BAG, 300.0, 300.0))
    val inv1 = Invoice(id = "1", invoiceNumber = "2026001", customerId = "c1", customerName = "A", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0)
    val inv2 = Invoice(id = "2", invoiceNumber = "2026002", customerId = "c1", customerName = "A", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0)
    val inv3 = Invoice(id = "3", invoiceNumber = "2026003", customerId = "c1", customerName = "A", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0)

    val currentInvoices = mutableListOf(inv1, inv2, inv3)
    val config = InvoiceNumberConfig(startingNumber = 2026001L, nextInvoiceNumber = 2026004L, lastIssuedNumber = 2026003L)

    // Admin deletes invoice 2026002
    currentInvoices.remove(inv2)
    assertEquals(2, currentInvoices.size)

    // Next allocated invoice number must continue from 2026004, NOT reuse 2026002
    val nextAllocated = config.nextInvoiceNumber
    assertEquals(2026004L, nextAllocated)
    assertEquals("2026004", config.formattedNextNumber())
  }

  @Test
  fun `test sequence continuity after restart with highest existing invoice`() {
    val dummyItems = listOf(InvoiceItem("p1", "Urea", "B1", 1.0, ProductUnit.BAG, 300.0, 300.0))
    val storedInvoices = listOf(
      Invoice(id = "1", invoiceNumber = "2026001", customerId = "c1", customerName = "A", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0),
      Invoice(id = "2", invoiceNumber = "2026002", customerId = "c1", customerName = "A", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0),
      Invoice(id = "3", invoiceNumber = "2026003", customerId = "c1", customerName = "A", items = dummyItems, subTotal = 300.0, grandTotal = 300.0, amountPaid = 300.0)
    )

    val highestIssued = InvoiceNumberManager.findHighestIssuedInvoiceNumber(storedInvoices) ?: 0L
    assertEquals(2026003L, highestIssued)

    val nextToIssue = maxOf(highestIssued + 1, InvoiceNumberManager.DEFAULT_STARTING_NUMBER)
    assertEquals(2026004L, nextToIssue)
  }

  @Test
  fun `test admin authorization recognizes official admin email`() {
    assertTrue(AdminAuthUtils.isAdmin("admin.manglamferilizer@gmail.com"))
    assertTrue(AdminAuthUtils.isAdmin("ADMIN.MANGLAMFERILIZER@GMAIL.COM"))
    assertTrue(AdminAuthUtils.isAdmin("kartik.bharadwaj0105@gmail.com"))
    assertFalse(AdminAuthUtils.isAdmin("user@example.com"))
  }
}
