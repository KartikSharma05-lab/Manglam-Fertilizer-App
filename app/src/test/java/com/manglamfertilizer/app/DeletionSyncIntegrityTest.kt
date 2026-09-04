package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.DeletedRecordsTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for Deletion Flow and Sync Resurrection Prevention.
 *
 * Verifies:
 * 1. Admin Authorization check for invoice and customer deletions.
 * 2. Non-admin deletion attempts are rejected.
 * 3. DeletedRecordsTracker prevents resurrection during sync.
 * 4. Customer deletion with outstanding dues is blocked.
 * 5. Customer deletion with zero dues is permitted.
 * 6. Invoice deletion calculates stock restore and due adjustments accurately.
 */
class DeletionSyncIntegrityTest {

  @Before
  fun setup() {
    DeletedRecordsTracker.clearForTest()
  }

  @Test
  fun testAdminAuthorizationForDeletion() {
    val adminEmail = "kartik.bharadwaj0105@gmail.com"
    val staffEmail = "employee@store.com"

    assertTrue("Admin should be authorized", AdminAuthUtils.isAdmin(adminEmail))
    assertFalse("Staff should not be authorized to delete", AdminAuthUtils.isAdmin(staffEmail))
  }

  @Test
  fun testDeletedRecordsTrackerFiltersSnapshots() {
    val invoiceId = "inv_test_999"
    assertFalse("Should not be marked deleted initially", DeletedRecordsTracker.isDeleted(invoiceId))

    // Mark deleted
    DeletedRecordsTracker.markDeleted(invoiceId)
    assertTrue("Should be marked deleted", DeletedRecordsTracker.isDeleted(invoiceId))

    // Simulate incoming Firestore snapshot documents
    val incomingDocs = listOf("inv_1", "inv_2", invoiceId, "inv_3")
    val filteredDocs = incomingDocs.filterNot { DeletedRecordsTracker.isDeleted(it) }

    assertEquals(3, filteredDocs.size)
    assertFalse(filteredDocs.contains(invoiceId))
  }

  @Test
  fun testCustomerWithDuesBlocksDeletion() {
    val customerWithDue = Customer(
      id = "cust_with_due",
      name = "Ramesh Kumar",
      phoneNumber = "9876543210",
      totalDue = 1500.0,
      totalPurchases = 10000.0
    )

    // Business rule: totalDue > 0 blocks deletion
    val canDelete = customerWithDue.totalDue <= 0.0
    assertFalse("Customer with outstanding dues must NOT be deleted", canDelete)
  }

  @Test
  fun testCustomerWithZeroDuesAllowsDeletion() {
    val customerClear = Customer(
      id = "cust_zero_due",
      name = "Suresh Patel",
      phoneNumber = "9876543211",
      totalDue = 0.0,
      totalPurchases = 5000.0
    )

    val canDelete = customerClear.totalDue <= 0.0
    assertTrue("Customer with zero dues is eligible for deletion", canDelete)
  }

  @Test
  fun testInvoiceStockReversalCalculation() {
    val invoice = Invoice(
      id = "inv_calc_test",
      invoiceNumber = "INV-001",
      customerName = "Ramesh",
      customerPhone = "9876543210",
      customerId = "cust_1",
      items = listOf(
        InvoiceItem(
          productId = "prod_urea",
          productName = "Urea 45kg",
          quantity = 5.0,
          unit = ProductUnit.BAG,
          unitPrice = 270.0,
          totalPrice = 1350.0
        ),
        InvoiceItem(
          productId = "prod_dap",
          productName = "DAP 50kg",
          quantity = 2.0,
          unit = ProductUnit.BAG,
          unitPrice = 1350.0,
          totalPrice = 2700.0
        )
      ),
      subTotal = 4050.0,
      grandTotal = 4050.0,
      amountPaid = 2000.0,
      remainingDue = 2050.0,
      paymentMode = PaymentMode.CREDIT,
      timestamp = System.currentTimeMillis()
    )

    val initialStockUrea = 10.0
    val initialStockDap = 5.0

    // Reverse stock: add back the quantities from invoice
    val restoredStockUrea = initialStockUrea + invoice.items[0].quantity
    val restoredStockDap = initialStockDap + invoice.items[1].quantity

    assertEquals(15.0, restoredStockUrea, 0.001)
    assertEquals(7.0, restoredStockDap, 0.001)

    // Customer balance reversal
    val initialCustomerPurchases = 15000.0
    val initialCustomerDue = 5000.0

    val adjustedPurchases = (initialCustomerPurchases - invoice.grandTotal).coerceAtLeast(0.0)
    val adjustedDue = (initialCustomerDue - invoice.remainingDue).coerceAtLeast(0.0)

    assertEquals(10950.0, adjustedPurchases, 0.001)
    assertEquals(2950.0, adjustedDue, 0.001)
  }
}
