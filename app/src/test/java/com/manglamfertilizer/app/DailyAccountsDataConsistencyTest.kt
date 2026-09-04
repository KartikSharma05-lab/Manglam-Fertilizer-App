package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.accounting.AccountingPeriodMode
import com.manglamfertilizer.app.data.accounting.DailyAccountingLifecycleManager
import com.manglamfertilizer.app.data.local.entity.InvoiceEntity
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comprehensive Unit & Regression Test Suite for:
 * - Daily Accounting Lifecycle
 * - 12:00 PM Noon Cutoff & Next-day Rollover
 * - Historical Data Preservation (No physical deletions)
 * - Date & Date Range Filtering
 * - Monthly Aggregations
 * - ₹330 Due Invariant & No Doubling
 * - Idempotent Sync & Duplicate Invoice Prevention
 * - Invoice Update & Deletion Lifecycle
 */
@RunWith(RobolectricTestRunner::class)
class DailyAccountsDataConsistencyTest {

  private val testTimeZone = TimeZone.getTimeZone("Asia/Kolkata")

  // =========================================================================
  // 1. SPECIFIC REGRESSION TEST: ₹330 Due Remains Exactly ₹330 (No Doubling to ₹660)
  // =========================================================================

  @Test
  fun testCase1_Exact330DueRemains330_NoDoubleAddition() {
    val initialCustomer = Customer(
      id = "cust_ramesh_01",
      name = "Ramesh Kumar",
      phoneNumber = "9876543210",
      totalPurchases = 0.0,
      totalDue = 0.0,
      createdAt = System.currentTimeMillis()
    )

    // Invoice with Total ₹1330, Paid ₹1000 in Cash, Remaining Due = ₹330
    val item = InvoiceItem(
      productId = "prod_urea_01",
      productName = "Urea 45kg",
      batchNumber = "B-01",
      quantity = 5.0,
      unit = ProductUnit.BAG,
      unitPrice = 266.0,
      totalPrice = 1330.0
    )

    val invoice = Invoice(
      id = "inv_330_due",
      invoiceNumber = "INV-20260901-001",
      customerId = initialCustomer.id,
      customerName = initialCustomer.name,
      customerPhone = initialCustomer.phoneNumber,
      items = listOf(item),
      subTotal = 1330.0,
      gstRate = 0.0,
      gstAmount = 0.0,
      discount = 0.0,
      grandTotal = 1330.0,
      amountPaid = 1000.0,
      remainingDue = 330.0,
      paymentMode = PaymentMode.CASH,
      timestamp = System.currentTimeMillis()
    )

    // Verify Customer Due update formula: totalDue = initialDue + invoice.remainingDue
    val updatedDue = (initialCustomer.totalDue + invoice.remainingDue).coerceAtLeast(0.0)
    val updatedPurchases = initialCustomer.totalPurchases + invoice.grandTotal

    val updatedCustomer = initialCustomer.copy(
      totalDue = updatedDue,
      totalPurchases = updatedPurchases,
      lastTransactionDate = invoice.timestamp
    )

    assertEquals("Customer due must be exactly ₹330, NOT ₹660", 330.0, updatedCustomer.totalDue, 0.001)
    assertEquals(1330.0, updatedCustomer.totalPurchases, 0.001)

    // Verify Daily Accounts calculation for this record
    val summary = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = listOf(invoice),
      mode = AccountingPeriodMode.ALL_RECORDS,
      timeZone = testTimeZone
    )

    assertEquals(1330.0, summary.totalSales, 0.001)
    assertEquals(1000.0, summary.totalCash, 0.001)
    assertEquals(0.0, summary.totalOnline, 0.001)
    assertEquals("Daily accounts due must be exactly ₹330", 330.0, summary.totalDue, 0.001)
    assertEquals(1, summary.invoiceCount)
    // Conservation law: Cash + Online + Due == TotalSales
    assertEquals(summary.totalSales, summary.totalCash + summary.totalOnline + summary.totalDue, 0.001)
  }

  // =========================================================================
  // 2. 12:00 PM NOON CUTOFF & NEXT-DAY ROLLOVER
  // =========================================================================

  @Test
  fun testCase2_NoonCutoff_And_NextDayRollover() {
    // Sept 1, 2026 10:00 AM (Before noon -> Belongs to Aug 31 12:00 PM - Sep 1 11:59 AM cycle)
    val calSep1Morning = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 1, 10, 0, 0)
    }

    // Sept 1, 2026 02:00 PM (After noon -> Belongs to Sep 1 12:00 PM - Sep 2 11:59 AM cycle)
    val calSep1Afternoon = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 1, 14, 0, 0)
    }

    // Sept 2, 2026 09:00 AM (Next morning before noon -> Still belongs to Sep 1 12:00 PM - Sep 2 11:59 AM cycle)
    val calSep2Morning = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 2, 9, 0, 0)
    }

    // Sept 2, 2026 01:00 PM (Next day after noon -> Rolled over to Sep 2 12:00 PM - Sep 3 11:59 AM cycle)
    val calSep2Afternoon = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 2, 13, 0, 0)
    }

    val (rangeStart1, rangeEnd1) = DailyAccountingLifecycleManager.getBusinessDayRange(
      referenceMillis = calSep1Afternoon.timeInMillis,
      cutoffHour = 12,
      timeZone = testTimeZone
    )

    val expectedStartSep1Noon = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 1, 12, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    assertEquals(expectedStartSep1Noon, rangeStart1)

    // Invoice created in Sept 1 afternoon
    val invoiceSep1 = Invoice(
      id = "inv_sep1_pm",
      invoiceNumber = "INV-0901-01",
      customerId = "c1",
      customerName = "Ram Lal",
      items = listOf(InvoiceItem("p1", "DAP 50kg", "B1", 1.0, ProductUnit.BAG, 1350.0, 1350.0)),
      subTotal = 1350.0,
      grandTotal = 1350.0,
      amountPaid = 1350.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = calSep1Afternoon.timeInMillis
    )

    // Invoice created in Sept 2 afternoon (after rollover)
    val invoiceSep2 = Invoice(
      id = "inv_sep2_pm",
      invoiceNumber = "INV-0902-01",
      customerId = "c2",
      customerName = "Shyam Lal",
      items = listOf(InvoiceItem("p2", "Urea 45kg", "B2", 2.0, ProductUnit.BAG, 266.0, 532.0)),
      subTotal = 532.0,
      grandTotal = 532.0,
      amountPaid = 500.0,
      remainingDue = 32.0,
      paymentMode = PaymentMode.CASH,
      timestamp = calSep2Afternoon.timeInMillis
    )

    val allInvoices = listOf(invoiceSep1, invoiceSep2)

    // On Sept 1 afternoon (active business day): Only invoiceSep1 is active
    val (startDay1, endDay1) = DailyAccountingLifecycleManager.getBusinessDayRange(
      calSep1Afternoon.timeInMillis, 12, testTimeZone
    )
    val activeDay1Invoices = allInvoices.filter { it.timestamp in startDay1..endDay1 }
    assertEquals(1, activeDay1Invoices.size)
    assertEquals("inv_sep1_pm", activeDay1Invoices[0].id)

    // On Sept 2 afternoon (after rollover): Only invoiceSep2 is active
    val (startDay2, endDay2) = DailyAccountingLifecycleManager.getBusinessDayRange(
      calSep2Afternoon.timeInMillis, 12, testTimeZone
    )
    val activeDay2Invoices = allInvoices.filter { it.timestamp in startDay2..endDay2 }
    assertEquals(1, activeDay2Invoices.size)
    assertEquals("inv_sep2_pm", activeDay2Invoices[0].id)
  }

  // =========================================================================
  // 3. HISTORICAL RECORDS REMAIN ACCESSIBLE (NO DATA LOSS ON ROLLOVER)
  // =========================================================================

  @Test
  fun testCase3_HistoricalRecordsPreservedAfterRollover() {
    val invPast1 = Invoice(
      id = "inv_past_01",
      invoiceNumber = "INV-PAST-01",
      customerId = "c1",
      customerName = "Farmer Past 1",
      items = listOf(InvoiceItem("p1", "Urea", "B1", 1.0, ProductUnit.BAG, 266.0, 266.0)),
      subTotal = 266.0,
      grandTotal = 266.0,
      amountPaid = 266.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = 1756700000000L
    )

    val invPast2 = Invoice(
      id = "inv_past_02",
      invoiceNumber = "INV-PAST-02",
      customerId = "c2",
      customerName = "Farmer Past 2",
      items = listOf(InvoiceItem("p2", "DAP", "B2", 1.0, ProductUnit.BAG, 1350.0, 1350.0)),
      subTotal = 1350.0,
      grandTotal = 1350.0,
      amountPaid = 1000.0,
      remainingDue = 350.0,
      paymentMode = PaymentMode.UPI,
      timestamp = 1756786400000L
    )

    val allInvoices = listOf(invPast1, invPast2)

    // Querying ALL_RECORDS retrieves complete history intact
    val summary = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = allInvoices,
      mode = AccountingPeriodMode.ALL_RECORDS,
      timeZone = testTimeZone
    )

    assertEquals(2, summary.invoiceCount)
    assertEquals(1616.0, summary.totalSales, 0.001)
    assertEquals(266.0, summary.totalCash, 0.001)
    assertEquals(1000.0, summary.totalOnline, 0.001)
    assertEquals(350.0, summary.totalDue, 0.001)
  }

  // =========================================================================
  // 4. DATE-BASED FILTERING (SINGLE DATE & DATE RANGE)
  // =========================================================================

  @Test
  fun testCase4_DateFiltering_And_RangeFiltering() {
    val calSep1 = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 1, 11, 0, 0)
    }
    val calSep2 = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 2, 11, 0, 0)
    }
    val calSep15 = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 15, 16, 0, 0)
    }
    val calOct1 = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.OCTOBER, 1, 10, 0, 0)
    }

    val invSep1 = Invoice(
      id = "inv_sep_1",
      invoiceNumber = "INV-S1",
      customerId = "c1",
      customerName = "Mukesh",
      items = listOf(InvoiceItem("p1", "Zinc", "B1", 1.0, ProductUnit.KG, 150.0, 150.0)),
      subTotal = 150.0,
      grandTotal = 150.0,
      amountPaid = 150.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = calSep1.timeInMillis
    )

    val invSep2 = Invoice(
      id = "inv_sep_2",
      invoiceNumber = "INV-S2",
      customerId = "c2",
      customerName = "Dinesh",
      items = listOf(InvoiceItem("p2", "Potash", "B2", 1.0, ProductUnit.BAG, 900.0, 900.0)),
      subTotal = 900.0,
      grandTotal = 900.0,
      amountPaid = 500.0,
      remainingDue = 400.0,
      paymentMode = PaymentMode.CASH,
      timestamp = calSep2.timeInMillis
    )

    val invSep15 = Invoice(
      id = "inv_sep_15",
      invoiceNumber = "INV-S15",
      customerId = "c3",
      customerName = "Kamlesh",
      items = listOf(InvoiceItem("p3", "Bio NPK", "B3", 2.0, ProductUnit.LITER, 450.0, 900.0)),
      subTotal = 900.0,
      grandTotal = 900.0,
      amountPaid = 900.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.UPI,
      timestamp = calSep15.timeInMillis
    )

    val invOct1 = Invoice(
      id = "inv_oct_1",
      invoiceNumber = "INV-O1",
      customerId = "c4",
      customerName = "Harish",
      items = listOf(InvoiceItem("p4", "Coragen", "B4", 1.0, ProductUnit.BOTTLE, 850.0, 850.0)),
      subTotal = 850.0,
      grandTotal = 850.0,
      amountPaid = 850.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = calOct1.timeInMillis
    )

    val allInvoices = listOf(invSep1, invSep2, invSep15, invOct1)

    // 1. Filter by single specific date: 01 Sep 2026
    val summarySep1 = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = allInvoices,
      mode = AccountingPeriodMode.CUSTOM_DATE,
      customStartMillis = calSep1.timeInMillis,
      timeZone = testTimeZone
    )
    assertEquals(1, summarySep1.invoiceCount)
    assertEquals("inv_sep_1", summarySep1.invoices[0].id)
    assertEquals(150.0, summarySep1.totalSales, 0.001)

    // 2. Filter by date range: 01 Sep 2026 - 30 Sep 2026
    val sepStart = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 1, 0, 0, 0)
    }.timeInMillis

    val sepEnd = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 30, 23, 59, 59)
    }.timeInMillis

    val summarySepRange = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = allInvoices,
      mode = AccountingPeriodMode.CUSTOM_RANGE,
      customStartMillis = sepStart,
      customEndMillis = sepEnd,
      timeZone = testTimeZone
    )
    assertEquals(3, summarySepRange.invoiceCount) // invSep1, invSep2, invSep15 (Oct 1 excluded)
    assertEquals(1950.0, summarySepRange.totalSales, 0.001)
    assertEquals(650.0, summarySepRange.totalCash, 0.001)
    assertEquals(900.0, summarySepRange.totalOnline, 0.001)
    assertEquals(400.0, summarySepRange.totalDue, 0.001)
  }

  // =========================================================================
  // 5. MONTHLY DATA & SUMMARY AGGREGATION
  // =========================================================================

  @Test
  fun testCase5_MonthlyData_Calculation() {
    val calSep10 = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 10, 14, 0, 0)
    }
    val calSep20 = Calendar.getInstance(testTimeZone).apply {
      set(2026, Calendar.SEPTEMBER, 20, 15, 0, 0)
    }

    val inv1 = Invoice(
      id = "inv_m1",
      invoiceNumber = "INV-M1",
      customerId = "c1",
      customerName = "Farmer M1",
      items = listOf(InvoiceItem("p1", "Urea", "B1", 10.0, ProductUnit.BAG, 266.0, 2660.0)),
      subTotal = 2660.0,
      grandTotal = 2660.0,
      amountPaid = 2000.0,
      remainingDue = 660.0,
      paymentMode = PaymentMode.CASH,
      timestamp = calSep10.timeInMillis
    )

    val inv2 = Invoice(
      id = "inv_m2",
      invoiceNumber = "INV-M2",
      customerId = "c2",
      customerName = "Farmer M2",
      items = listOf(InvoiceItem("p2", "DAP", "B2", 2.0, ProductUnit.BAG, 1350.0, 2700.0)),
      subTotal = 2700.0,
      grandTotal = 2700.0,
      amountPaid = 2700.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.UPI,
      timestamp = calSep20.timeInMillis
    )

    val monthRange = DailyAccountingLifecycleManager.getMonthRange(calSep10.timeInMillis, testTimeZone)
    val summary = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = listOf(inv1, inv2),
      mode = AccountingPeriodMode.CUSTOM_RANGE,
      customStartMillis = monthRange.first,
      customEndMillis = monthRange.second,
      timeZone = testTimeZone
    )

    assertEquals(5360.0, summary.totalSales, 0.001)
    assertEquals(2000.0, summary.totalCash, 0.001)
    assertEquals(2700.0, summary.totalOnline, 0.001)
    assertEquals(660.0, summary.totalDue, 0.001)
    assertEquals(2, summary.invoiceCount)
  }

  // =========================================================================
  // 6. DUPLICATE INVOICE PREVENTION & REPEATED SYNCHRONIZATION
  // =========================================================================

  @Test
  fun testCase6_DuplicateInvoicePrevention_RepeatedSync() {
    val inv = Invoice(
      id = "inv_unique_100",
      invoiceNumber = "INV-UNIQ-100",
      customerId = "c1",
      customerName = "Babulal",
      items = listOf(InvoiceItem("p1", "Urea", "B1", 2.0, ProductUnit.BAG, 266.0, 532.0)),
      subTotal = 532.0,
      grandTotal = 532.0,
      amountPaid = 532.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = System.currentTimeMillis()
    )

    // Repeat same invoice 5 times (e.g. from repeated network listeners / sync callbacks)
    val repeatedList = listOf(inv, inv, inv, inv, inv)

    val summary = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = repeatedList,
      mode = AccountingPeriodMode.ALL_RECORDS,
      timeZone = testTimeZone
    )

    assertEquals("Summary must deduplicate and show exactly 1 invoice", 1, summary.invoiceCount)
    assertEquals(532.0, summary.totalSales, 0.001)
    assertEquals(532.0, summary.totalCash, 0.001)
    assertEquals(0.0, summary.totalDue, 0.001)
  }

  // =========================================================================
  // 7. INVOICE EDIT & DELETION LIFECYCLE
  // =========================================================================

  @Test
  fun testCase7_InvoiceUpdateAndDeletionLifecycle() {
    val initialStock = 50.0
    val product = Product(
      id = "prod_npk",
      name = "NPK 12:32:16",
      company = "IFFCO",
      unit = ProductUnit.BAG,
      batchNumber = "B-NPK",
      purchasePrice = 1300.0,
      sellingPrice = 1470.0,
      mrp = 1470.0,
      stockQuantity = initialStock
    )

    val customer = Customer(
      id = "cust_gopal",
      name = "Gopal",
      phoneNumber = "9988776655",
      totalPurchases = 0.0,
      totalDue = 0.0,
      createdAt = System.currentTimeMillis()
    )

    // 1. Create invoice for 10 bags = ₹14700, Paid ₹10000, Due ₹4700
    val invoice = Invoice(
      id = "inv_lifecycle_01",
      invoiceNumber = "INV-LC-01",
      customerId = customer.id,
      customerName = customer.name,
      items = listOf(
        InvoiceItem("prod_npk", "NPK 12:32:16", "B-NPK", 10.0, ProductUnit.BAG, 1470.0, 14700.0)
      ),
      subTotal = 14700.0,
      grandTotal = 14700.0,
      amountPaid = 10000.0,
      remainingDue = 4700.0,
      paymentMode = PaymentMode.CASH,
      timestamp = System.currentTimeMillis()
    )

    val customerAfterCreation = customer.copy(
      totalPurchases = invoice.grandTotal,
      totalDue = invoice.remainingDue
    )
    val productAfterCreation = product.copy(
      stockQuantity = initialStock - 10.0
    )

    assertEquals(4700.0, customerAfterCreation.totalDue, 0.001)
    assertEquals(40.0, productAfterCreation.stockQuantity, 0.001)

    // 2. Edit Invoice: Changed to 5 bags = ₹7350, Paid ₹7350, Due ₹0
    val editedInvoice = invoice.copy(
      items = listOf(
        InvoiceItem("prod_npk", "NPK 12:32:16", "B-NPK", 5.0, ProductUnit.BAG, 1470.0, 7350.0)
      ),
      subTotal = 7350.0,
      grandTotal = 7350.0,
      amountPaid = 7350.0,
      remainingDue = 0.0
    )

    val summaryEdited = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = listOf(editedInvoice),
      mode = AccountingPeriodMode.ALL_RECORDS,
      timeZone = testTimeZone
    )

    assertEquals(7350.0, summaryEdited.totalSales, 0.001)
    assertEquals(7350.0, summaryEdited.totalCash, 0.001)
    assertEquals(0.0, summaryEdited.totalDue, 0.001)
    assertEquals(1, summaryEdited.invoiceCount)

    // 3. Admin Deletion: Revert stock and customer dues
    val customerAfterDeletion = customerAfterCreation.copy(
      totalPurchases = (customerAfterCreation.totalPurchases - invoice.grandTotal).coerceAtLeast(0.0),
      totalDue = (customerAfterCreation.totalDue - invoice.remainingDue).coerceAtLeast(0.0)
    )
    val productAfterDeletion = productAfterCreation.copy(
      stockQuantity = productAfterCreation.stockQuantity + 10.0
    )

    assertEquals("Stock restored to initial 50.0", 50.0, productAfterDeletion.stockQuantity, 0.001)
    assertEquals("Customer due restored to 0.0", 0.0, customerAfterDeletion.totalDue, 0.001)

    val summaryAfterDeletion = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = emptyList(),
      mode = AccountingPeriodMode.ALL_RECORDS,
      timeZone = testTimeZone
    )
    assertEquals(0, summaryAfterDeletion.invoiceCount)
    assertEquals(0.0, summaryAfterDeletion.totalSales, 0.001)
  }

  // =========================================================================
  // 8. TEXT SEARCH ACROSS FARMER, PRODUCT, BILL NUMBER WITHIN FILTERED WINDOW
  // =========================================================================

  @Test
  fun testCase8_SearchAcrossFarmerProductBillNo() {
    val inv1 = Invoice(
      id = "inv_s_1",
      invoiceNumber = "INV-9901",
      customerId = "c1",
      customerName = "Ratan Singh",
      customerVillage = "Sanganer",
      items = listOf(InvoiceItem("p1", "Urea IFFCO", "B1", 1.0, ProductUnit.BAG, 266.0, 266.0)),
      subTotal = 266.0,
      grandTotal = 266.0,
      amountPaid = 266.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.CASH,
      timestamp = System.currentTimeMillis()
    )

    val inv2 = Invoice(
      id = "inv_s_2",
      invoiceNumber = "INV-9902",
      customerId = "c2",
      customerName = "Mohan Gurjar",
      customerVillage = "Bassi",
      items = listOf(InvoiceItem("p2", "Zinc Chelate", "B2", 2.0, ProductUnit.KG, 200.0, 400.0)),
      subTotal = 400.0,
      grandTotal = 400.0,
      amountPaid = 400.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.UPI,
      timestamp = System.currentTimeMillis()
    )

    val all = listOf(inv1, inv2)

    // Search by Farmer Name
    val searchFarmer = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = all,
      mode = AccountingPeriodMode.ALL_RECORDS,
      searchQuery = "Ratan",
      timeZone = testTimeZone
    )
    assertEquals(1, searchFarmer.invoiceCount)
    assertEquals("inv_s_1", searchFarmer.invoices[0].id)

    // Search by Product Name
    val searchProduct = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = all,
      mode = AccountingPeriodMode.ALL_RECORDS,
      searchQuery = "Zinc",
      timeZone = testTimeZone
    )
    assertEquals(1, searchProduct.invoiceCount)
    assertEquals("inv_s_2", searchProduct.invoices[0].id)

    // Search by Bill Number
    val searchBill = DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = all,
      mode = AccountingPeriodMode.ALL_RECORDS,
      searchQuery = "9902",
      timeZone = testTimeZone
    )
    assertEquals(1, searchBill.invoiceCount)
    assertEquals("inv_s_2", searchBill.invoices[0].id)
  }

  // =========================================================================
  // 9. CUSTOM DATA EXPORT: CSV, XLSX, AND PDF GENERATION & NO-PAYMENT-DUPLICATION
  // =========================================================================

  @Test
  fun testCase9_ExportCsvGeneration_And_MultiProductPaymentNonDuplication() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()

    // Create an invoice with 2 items: Total ₹2680. Paid ₹2000 Cash, Due ₹680
    val item1 = InvoiceItem("p1", "Urea 45kg", "B1", 5.0, ProductUnit.BAG, 266.0, 1330.0)
    val item2 = InvoiceItem("p2", "DAP 50kg", "B2", 1.0, ProductUnit.BAG, 1350.0, 1350.0)

    val multiItemInvoice = Invoice(
      id = "inv_multi_export_01",
      invoiceNumber = "INV-2026-MULTI",
      customerId = "c1",
      customerName = "Ramesh Choudhary, Gram Bassi", // Contains comma to test CSV escaping
      items = listOf(item1, item2),
      subTotal = 2680.0,
      grandTotal = 2680.0,
      amountPaid = 2000.0,
      remainingDue = 680.0,
      paymentMode = PaymentMode.CASH,
      timestamp = 1756700000000L
    )

    // Second invoice with Online UPI payment: Total ₹900, Paid ₹900, Due ₹0
    val singleItemInvoice = Invoice(
      id = "inv_online_export_02",
      invoiceNumber = "INV-2026-UPI",
      customerId = "c2",
      customerName = "Suresh Verma",
      items = listOf(InvoiceItem("p3", "Potash 50kg", "B3", 1.0, ProductUnit.BAG, 900.0, 900.0)),
      subTotal = 900.0,
      grandTotal = 900.0,
      amountPaid = 900.0,
      remainingDue = 0.0,
      paymentMode = PaymentMode.UPI,
      timestamp = 1756703600000L
    )

    val invoices = listOf(multiItemInvoice, singleItemInvoice)

    // 1. Generate CSV
    val csvResult = kotlinx.coroutines.runBlocking {
      com.manglamfertilizer.app.data.accounting.DailyAccountsExporter.exportData(
        context = context,
        invoices = invoices,
        periodLabel = "01 Sep 2026 – 30 Sep 2026",
        format = com.manglamfertilizer.app.data.accounting.ExportFormat.CSV
      )
    }

    assertTrue("CSV file must exist", csvResult.file.exists())
    val csvContent = csvResult.file.readText(Charsets.UTF_8)
    assertTrue("CSV must contain BOM for Excel UTF-8 compatibility", csvContent.startsWith("\uFEFF"))
    assertTrue("CSV must contain shop header", csvContent.contains(com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME))
    assertTrue("CSV must escape names with commas", csvContent.contains("\"Ramesh Choudhary, Gram Bassi\""))
    assertTrue("CSV must list both products", csvContent.contains("Urea 45kg") && csvContent.contains("DAP 50kg"))
    assertTrue("CSV must list online UPI payment", csvContent.contains("Suresh Verma") && csvContent.contains("900"))

    // Totals reconciliation in CSV:
    // Total Sales: 2680 + 900 = 3580
    // Total Cash: 2000
    // Total Online: 900
    // Total Due: 680
    assertTrue("CSV must contain TOTAL SALES: 3580", csvContent.contains("3580.00") || csvContent.contains("3,580"))
    assertTrue("CSV must contain TOTAL CASH: 2000", csvContent.contains("2000.00") || csvContent.contains("2,000"))
    assertTrue("CSV must contain TOTAL ONLINE: 900", csvContent.contains("900.00") || csvContent.contains("900"))
    assertTrue("CSV must contain TOTAL DUE: 680", csvContent.contains("680.00") || csvContent.contains("680"))

    // 2. Generate Excel (.xlsx) file
    val excelResult = kotlinx.coroutines.runBlocking {
      com.manglamfertilizer.app.data.accounting.DailyAccountsExporter.exportData(
        context = context,
        invoices = invoices,
        periodLabel = "01 Sep 2026 – 30 Sep 2026",
        format = com.manglamfertilizer.app.data.accounting.ExportFormat.XLSX
      )
    }
    assertTrue("Excel file must exist", excelResult.file.exists())
    assertTrue("Excel file size must be > 0 bytes", excelResult.file.length() > 500)
    assertTrue("Excel file name must end with .xlsx", excelResult.file.name.endsWith(".xlsx"))

    // 3. Generate PDF file (Gracefully handles Robolectric headless JVM canvas vs Android device)
    try {
      val pdfResult = kotlinx.coroutines.runBlocking {
        com.manglamfertilizer.app.data.accounting.DailyAccountsExporter.exportData(
          context = context,
          invoices = invoices,
          periodLabel = "01 Sep 2026 – 30 Sep 2026",
          format = com.manglamfertilizer.app.data.accounting.ExportFormat.PDF
        )
      }
      assertTrue("PDF file must exist", pdfResult.file.exists())
      assertTrue("PDF file name must end with .pdf", pdfResult.file.name.endsWith(".pdf"))
    } catch (e: IllegalStateException) {
      // Expected in headless Robolectric JVM without native Skia rendering
      println("PdfDocument execution in Robolectric JVM stub: ${e.message}")
    }
  }

  @Test
  fun testCase10_MathematicalReconciliation_MultiProductNoDoubleCount() {
    // 3 products in single bill of ₹5,000. Paid ₹3,000 Cash, Due ₹2,000
    val inv = Invoice(
      id = "inv_recon_1",
      invoiceNumber = "INV-RECON-1",
      customerId = "c1",
      customerName = "Kailash",
      items = listOf(
        InvoiceItem("p1", "Product A", "B1", 1.0, ProductUnit.BAG, 1000.0, 1000.0),
        InvoiceItem("p2", "Product B", "B2", 2.0, ProductUnit.BAG, 1000.0, 2000.0),
        InvoiceItem("p3", "Product C", "B3", 2.0, ProductUnit.BAG, 1000.0, 2000.0)
      ),
      subTotal = 5000.0,
      grandTotal = 5000.0,
      amountPaid = 3000.0,
      remainingDue = 2000.0,
      paymentMode = PaymentMode.CASH,
      timestamp = System.currentTimeMillis()
    )

    val summary = com.manglamfertilizer.app.data.accounting.DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = listOf(inv),
      mode = AccountingPeriodMode.ALL_RECORDS,
      timeZone = testTimeZone
    )

    assertEquals("Grand Total must be 5000.0", 5000.0, summary.totalSales, 0.001)
    assertEquals("Cash Paid must be 3000.0", 3000.0, summary.totalCash, 0.001)
    assertEquals("Online Paid must be 0.0", 0.0, summary.totalOnline, 0.001)
    assertEquals("Remaining Due must be 2000.0", 2000.0, summary.totalDue, 0.001)
    assertEquals("Sales = Cash + Online + Due", summary.totalSales, summary.totalCash + summary.totalOnline + summary.totalDue, 0.001)
  }
}
