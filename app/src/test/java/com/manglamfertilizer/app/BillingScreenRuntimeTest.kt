package com.manglamfertilizer.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.ui.billing.BillingScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BillingScreenRuntimeTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleProducts = listOf(
    Product(
      id = "prod_urea_01",
      name = "Neem Coated Urea 45kg",
      company = "NFL",
      unit = ProductUnit.BAG,
      batchNumber = "BATCH-2026-A",
      purchasePrice = 242.0,
      sellingPrice = 266.50,
      mrp = 266.50,
      stockQuantity = 50.0
    ),
    Product(
      id = "prod_dap_02",
      name = "DAP 18:46:0 50kg",
      company = "IFFCO",
      unit = ProductUnit.BAG,
      batchNumber = "BATCH-DAP-99",
      purchasePrice = 1250.0,
      sellingPrice = 1350.0,
      mrp = 1350.0,
      stockQuantity = 20.0
    ),
    Product(
      id = "prod_oos_03",
      name = "Out of Stock Product",
      company = "Bayer",
      unit = ProductUnit.BOTTLE,
      batchNumber = "BATCH-OOS-00",
      purchasePrice = 500.0,
      sellingPrice = 600.0,
      mrp = 650.0,
      stockQuantity = 0.0
    )
  )

  private val sampleCustomers = listOf(
    Customer(
      id = "cust_001",
      name = "Ramesh Patel",
      phoneNumber = "9876543210",
      village = "Khed",
      address = "Near Panchayat",
      totalDue = 1500.0
    )
  )

  @Test
  fun testBillingFlow_SearchAndAddProductToCart_DirectNoPopup_NoCrash() {
    var createdInvoiceItems: List<InvoiceItem>? = null
    var createdCustomerName: String? = null

    composeTestRule.setContent {
      BillingScreen(
        invoices = emptyList(),
        products = sampleProducts,
        customers = sampleCustomers,
        onCreateInvoice = { _, name, _, _, _, _, items, _, _, _, _, _, onDone ->
          createdCustomerName = name
          createdInvoiceItems = items
          onDone(true, null)
        },
        onPrintInvoice = {}
      )
    }

    // 1. Enter Farmer Name in Section 1
    composeTestRule.onNodeWithTag("invoice_farmer_name")
      .performTextInput("Ramesh Patel")

    // 2. Scroll to Section 2 (Products)
    composeTestRule.onNodeWithTag("billing_create_invoice_list")
      .performScrollToIndex(1)

    // 3. Type search query
    composeTestRule.onNodeWithTag("invoice_product_search_input")
      .performTextInput("Urea")

    // 4. Verify search result appears
    composeTestRule.onNodeWithText("Neem Coated Urea 45kg").assertExists()

    // 5. Click on the product row directly (Direct Add, No intermediate popup)
    composeTestRule.onNodeWithText("Neem Coated Urea 45kg").performClick()

    // 6. Verify product is immediately added with default quantity 1.0
    composeTestRule.onNodeWithText("₹266.5/BAG • Max: 50.0 BAG").assertExists()

    // 7. Add a second product (DAP)
    composeTestRule.onNodeWithTag("invoice_product_search_input")
      .performTextInput("DAP")
    composeTestRule.onNodeWithText("DAP 18:46:0 50kg").assertExists()
    composeTestRule.onNodeWithText("DAP 18:46:0 50kg").performClick()

    // 8. Verify DAP is in the cart
    composeTestRule.onNodeWithText("₹1350.0/BAG • Max: 20.0 BAG").assertExists()

    // 9. Verify 2 items in cart header
    composeTestRule.onNodeWithText("2 item(s)").assertExists()

    // 10. Scroll to Section 3 / Submit button
    composeTestRule.onNodeWithTag("billing_create_invoice_list")
      .performScrollToIndex(3)

    // 11. Click "CREATE & SAVE INVOICE"
    composeTestRule.onNodeWithTag("save_and_generate_invoice_button")
      .performClick()

    // 12. Duplicate warning dialog is shown because "Ramesh Patel" exists in sampleCustomers
    composeTestRule.onNodeWithTag("duplicate_customer_warning_dialog").assertExists()
    composeTestRule.onNodeWithTag("use_existing_customer_button").performClick()

    // 13. Assert invoice submitted successfully with existing customer
    assertEquals("Ramesh Patel", createdCustomerName)
    assertNotNull(createdInvoiceItems)
    assertEquals(2, createdInvoiceItems?.size)
    assertEquals("prod_urea_01", createdInvoiceItems?.get(0)?.productId)
    assertEquals("prod_dap_02", createdInvoiceItems?.get(1)?.productId)
  }

  @Test
  fun testBillingFlow_NewUniqueCustomer_DirectSubmit_NoDuplicateDialog() {
    var createdInvoiceItems: List<InvoiceItem>? = null
    var createdCustomerName: String? = null

    composeTestRule.setContent {
      BillingScreen(
        invoices = emptyList(),
        products = sampleProducts,
        customers = sampleCustomers,
        onCreateInvoice = { _, name, _, _, _, _, items, _, _, _, _, _, onDone ->
          createdCustomerName = name
          createdInvoiceItems = items
          onDone(true, null)
        },
        onPrintInvoice = {}
      )
    }

    // 1. Enter Unique Farmer Name
    composeTestRule.onNodeWithTag("invoice_farmer_name")
      .performTextInput("Sunil Verma")

    // 2. Add product
    composeTestRule.onNodeWithTag("billing_create_invoice_list")
      .performScrollToIndex(1)
    composeTestRule.onNodeWithTag("invoice_product_search_input")
      .performTextInput("Urea")
    composeTestRule.onNodeWithText("Neem Coated Urea 45kg").performClick()

    // 3. Submit
    composeTestRule.onNodeWithTag("billing_create_invoice_list")
      .performScrollToIndex(3)
    composeTestRule.onNodeWithTag("save_and_generate_invoice_button")
      .performClick()

    // 4. Assert direct submission without duplicate dialog
    composeTestRule.onNodeWithTag("duplicate_customer_warning_dialog").assertDoesNotExist()
    assertEquals("Sunil Verma", createdCustomerName)
    assertEquals(1, createdInvoiceItems?.size)
  }
}
