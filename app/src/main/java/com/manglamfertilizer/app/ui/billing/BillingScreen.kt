package com.manglamfertilizer.app.ui.billing

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import java.util.UUID
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Visibility
import com.manglamfertilizer.app.ui.common.ManglamSearchBar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.CustomerDuplicateHelper
import com.manglamfertilizer.app.data.util.DuplicateCustomerMatch
import com.manglamfertilizer.app.ui.customers.DuplicateCustomerWarningDialog
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import com.manglamfertilizer.app.util.InvoicePdfGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillingScreen(
  invoices: List<Invoice>,
  products: List<Product>,
  customers: List<Customer>,
  currentUser: User? = null,
  invoiceNumberConfig: InvoiceNumberConfig = InvoiceNumberConfig(),
  onCreateInvoice: (
    customerId: String?,
    name: String,
    phone: String,
    aadhaar: String,
    address: String,
    village: String,
    items: List<InvoiceItem>,
    gstRate: Double,
    discount: Double,
    amountPaid: Double,
    dueDate: Long?,
    paymentMode: PaymentMode,
    onComplete: (Boolean, String?) -> Unit
  ) -> Unit,
  onUpdateCustomer: ((Customer, onDone: (Boolean, String?) -> Unit) -> Unit)? = null,
  onPrintInvoice: (Invoice) -> Unit,
  onDeleteInvoice: ((String, (Boolean, String?) -> Unit) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) }
  val tabs = listOf("Create Invoice", "Invoice History (${invoices.size})")

  // If user is on Invoice History tab, Back button navigates back to Create Invoice tab
  BackHandler(enabled = selectedTab == 1) {
    selectedTab = 0
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .imePadding()
  ) {
    // Top App Header - Compact & Clean with proper status bar insets
    Surface(
      color = DarkSurface,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
          .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "MANGALAM BILLING",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = TextPrimaryDark
            )
            Text(
              text = "GST Invoicing, Dues & Real-time Stock Sync",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark
            )
          }
        }
      }
    }

    // Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = DarkSurface,
      contentColor = Emerald400,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = Emerald400
        )
      }
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = {
            Text(
              text = title,
              fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == index) Emerald400 else TextSecondaryDark
            )
          },
          modifier = Modifier.testTag("billing_tab_$index")
        )
      }
    }

    if (selectedTab == 0) {
      CreateInvoiceTab(
        products = products,
        customers = customers,
        invoices = invoices,
        invoiceNumberConfig = invoiceNumberConfig,
        onSubmitInvoice = onCreateInvoice,
        onUpdateCustomer = onUpdateCustomer,
        onPrintInvoice = onPrintInvoice
      )
    } else {
      InvoiceHistoryTab(
        invoices = invoices,
        currentUser = currentUser,
        onPrintInvoice = onPrintInvoice,
        onDeleteInvoice = onDeleteInvoice
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreateInvoiceTab(
  products: List<Product>,
  customers: List<Customer>,
  invoices: List<Invoice>,
  invoiceNumberConfig: InvoiceNumberConfig = InvoiceNumberConfig(),
  onSubmitInvoice: (
    customerId: String?,
    name: String,
    phone: String,
    aadhaar: String,
    address: String,
    village: String,
    items: List<InvoiceItem>,
    gstRate: Double,
    discount: Double,
    amountPaid: Double,
    dueDate: Long?,
    paymentMode: PaymentMode,
    onDone: (Boolean, String?) -> Unit
  ) -> Unit,
  onUpdateCustomer: ((Customer, onDone: (Boolean, String?) -> Unit) -> Unit)? = null,
  onPrintInvoice: (Invoice) -> Unit
) {
  val context = LocalContext.current
  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }

  // Section 1: Customer / Farmer Details State
  var selectedCustomerId by remember { mutableStateOf<String?>(null) }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var customerAadhaar by remember { mutableStateOf("") }
  var customerAddress by remember { mutableStateOf("") }
  var customerVillage by remember { mutableStateOf("") }
  var showCustomerSearchDialog by remember { mutableStateOf(false) }

  // Section 2: Invoice Items & Cart State
  val cartItems = remember { mutableStateListOf<InvoiceItem>() }
  var productSearchQuery by remember { mutableStateOf("") }
  var showProductSearchDropdown by remember { mutableStateOf(false) }
  var showDualScannerDialog by remember { mutableStateOf(false) }

  fun addProductToCart(prod: Product, quantityToAdd: Double = 1.0) {
    try {
      val validName = prod.name.ifBlank { "Unnamed Product" }
      val validStock = if (prod.stockQuantity.isNaN() || prod.stockQuantity < 0) 0.0 else prod.stockQuantity
      if (validStock <= 0) {
        Toast.makeText(context, "Product is out of stock ($validName)", Toast.LENGTH_SHORT).show()
        return
      }
      val validSellingPrice = if (prod.sellingPrice.isNaN() || prod.sellingPrice < 0) 0.0 else prod.sellingPrice
      val validBatch = prod.batchNumber
      val validUnit = prod.unit
      val maxStock = if (validStock > 0) validStock else 9999.0
      val existingIdx = cartItems.indexOfFirst { it.productId == prod.id }
      if (existingIdx in 0 until cartItems.size) {
        val existing = cartItems[existingIdx]
        if (existing.quantity >= maxStock) {
          Toast.makeText(context, "Cannot add more. Max stock available: ${validStock.toInt()}", Toast.LENGTH_SHORT).show()
          return
        }
        val safeAddQty = if (quantityToAdd.isNaN() || quantityToAdd <= 0) 1.0 else quantityToAdd
        val updatedQty = (existing.quantity + safeAddQty).coerceAtMost(maxStock)
        cartItems[existingIdx] = existing.copy(
          quantity = updatedQty,
          totalPrice = updatedQty * validSellingPrice
        )
      } else {
        val safeAddQty = if (quantityToAdd.isNaN() || quantityToAdd <= 0) 1.0 else quantityToAdd
        val validQty = safeAddQty.coerceAtMost(maxStock)
        cartItems.add(
          InvoiceItem(
            productId = prod.id.ifBlank { UUID.randomUUID().toString() },
            productName = validName,
            batchNumber = validBatch,
            quantity = validQty,
            unit = validUnit,
            unitPrice = validSellingPrice,
            totalPrice = validQty * validSellingPrice
          )
        )
      }
    } catch (e: Exception) {
      Log.e("BillingScreen", "Error adding product to cart: ${e.message}", e)
    }
  }

  // Section 3: Tax, Discount & Payment State
  var selectedGstRate by remember { mutableDoubleStateOf(0.0) }
  val gstRateOptions = listOf(0.0, 5.0, 12.0, 18.0)
  var discountText by remember { mutableStateOf("0") }
  var paymentMode by remember { mutableStateOf(PaymentMode.CASH) }
  
  // Received and Due Amount States
  var receivedAmountInput by remember { mutableStateOf("") }
  var dueAmountInput by remember { mutableStateOf("") }
  var isDueManuallySet by remember { mutableStateOf(false) }

  var selectedDueDateMillis by remember {
    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 15) }
    mutableStateOf<Long?>(cal.timeInMillis)
  }

  // Submission & Validation State
  var isSubmitting by remember { mutableStateOf(false) }
  var formError by remember { mutableStateOf<String?>(null) }
  var createdInvoiceForSuccessDialog by remember { mutableStateOf<Invoice?>(null) }
  var pendingDuplicateMatchForBilling by remember { mutableStateOf<DuplicateCustomerMatch?>(null) }

  // Priority BackHandler for open dialogs / scanner / search sheets
  val isAnyBillingSubViewOpen = pendingDuplicateMatchForBilling != null ||
      createdInvoiceForSuccessDialog != null ||
      showDualScannerDialog ||
      showCustomerSearchDialog ||
      showProductSearchDropdown ||
      productSearchQuery.isNotBlank()

  BackHandler(enabled = isAnyBillingSubViewOpen) {
    when {
      pendingDuplicateMatchForBilling != null -> pendingDuplicateMatchForBilling = null
      createdInvoiceForSuccessDialog != null -> createdInvoiceForSuccessDialog = null
      showDualScannerDialog -> showDualScannerDialog = false
      showCustomerSearchDialog -> showCustomerSearchDialog = false
      showProductSearchDropdown -> showProductSearchDropdown = false
      productSearchQuery.isNotBlank() -> productSearchQuery = ""
    }
  }

  // Direct Financial Calculations (Computed cleanly during recomposition without snapshot observation errors)
  val subTotal = cartItems.sumOf { it.totalPrice }
  val parsedDiscount = (discountText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  val discountAmount = parsedDiscount.coerceIn(0.0, subTotal)
  val taxableAmount = (subTotal - discountAmount).coerceAtLeast(0.0)
  val gstAmount = (taxableAmount * selectedGstRate) / 100.0
  val grandTotal = (taxableAmount + gstAmount).coerceAtLeast(0.0)

  // Calculated Received & Due values
  val computedReceivedAmount = if (isDueManuallySet) {
    val userDue = dueAmountInput.toDoubleOrNull() ?: 0.0
    (grandTotal - userDue).coerceAtLeast(0.0)
  } else if (receivedAmountInput.isNotBlank()) {
    (receivedAmountInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  } else {
    if (paymentMode == PaymentMode.CREDIT) 0.0 else grandTotal
  }

  val computedDueAmount = if (isDueManuallySet) {
    (dueAmountInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
  } else {
    (grandTotal - computedReceivedAmount).coerceAtLeast(0.0)
  }

  // Quick Customer Auto-matching
  val matchedCustomers = remember(customerName, customers) {
    if (customerName.isBlank() || selectedCustomerId != null) emptyList()
    else customers.filter { it.name.contains(customerName, ignoreCase = true) }.take(4)
  }

  // Real Inventory Search Filtering
  val matchingInventoryProducts = remember(productSearchQuery, products) {
    if (productSearchQuery.isBlank()) emptyList()
    else {
      val query = productSearchQuery.trim()
      products.filter { prod ->
        prod.name.contains(query, ignoreCase = true) ||
            prod.company.contains(query, ignoreCase = true) ||
            prod.category.contains(query, ignoreCase = true) ||
            prod.batchNumber.contains(query, ignoreCase = true) ||
            prod.hsnCode.contains(query, ignoreCase = true) ||
            prod.chemicalComposition.contains(query, ignoreCase = true)
      }.take(8)
    }
  }

  val coroutineScope = rememberCoroutineScope()
  val farmerNameBringIntoView = remember { BringIntoViewRequester() }
  val farmerPhoneBringIntoView = remember { BringIntoViewRequester() }
  val farmerAadhaarBringIntoView = remember { BringIntoViewRequester() }
  val farmerAddressBringIntoView = remember { BringIntoViewRequester() }
  val productSearchBringIntoView = remember { BringIntoViewRequester() }
  val discountBringIntoView = remember { BringIntoViewRequester() }
  val receivedAmountBringIntoView = remember { BringIntoViewRequester() }
  val dueAmountBringIntoView = remember { BringIntoViewRequester() }

  val lazyListState = rememberLazyListState()

  LazyColumn(
    state = lazyListState,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .testTag("billing_create_invoice_list"),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // =========================================================================
    // SECTION 1: CUSTOMER / FARMER DETAILS
    // =========================================================================
    item {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // CUSTOMER DETAILS CARD
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = DarkCard),
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Emerald900,
                modifier = Modifier.size(24.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "1. CUSTOMER / FARMER DETAILS",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = Emerald400
              )
            }

            // Customer Picker Button
            OutlinedButton(
              onClick = { showCustomerSearchDialog = true },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.height(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.PersonSearch,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Select Customer", fontSize = 11.sp, color = Emerald400)
            }
          }

          // Linked Customer Badge
          if (selectedCustomerId != null) {
            val selectedCustomer = customers.find { it.id == selectedCustomerId }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceElevated,
              border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = Emerald400,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "Linked: $customerName",
                      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                      color = TextPrimaryDark
                    )
                  }
                  if (selectedCustomer != null && selectedCustomer.totalDue > 0) {
                    Text(
                      text = "⚠️ Previous Outstanding Due: ${currencyFormat.format(selectedCustomer.totalDue)}",
                      style = MaterialTheme.typography.labelSmall,
                      color = GoldAmber
                    )
                  }
                }

                IconButton(
                  onClick = {
                    selectedCustomerId = null
                    customerName = ""
                    customerPhone = ""
                    customerAadhaar = ""
                    customerAddress = ""
                    customerVillage = ""
                  },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = TextMutedDark,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Customer Name Field
          OutlinedTextField(
            value = customerName,
            onValueChange = {
              customerName = it
              if (selectedCustomerId != null && it != customers.find { c -> c.id == selectedCustomerId }?.name) {
                selectedCustomerId = null
              }
            },
            label = { Text("Customer / Farmer Name *", fontSize = 12.sp) },
            placeholder = { Text("Enter farmer name or select above", fontSize = 12.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              cursorColor = Emerald400,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated,
              focusedLabelColor = Emerald400,
              unfocusedLabelColor = TextSecondaryDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .bringIntoViewRequester(farmerNameBringIntoView)
              .onFocusChanged {
                if (it.isFocused) {
                  coroutineScope.launch { farmerNameBringIntoView.bringIntoView() }
                }
              }
              .testTag("invoice_farmer_name")
          )

          // Autocomplete suggestion chips
          if (matchedCustomers.isNotEmpty() && selectedCustomerId == null) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              items(matchedCustomers, key = { it.id }) { cust ->
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = DarkSurfaceElevated,
                  border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                  modifier = Modifier.clickable {
                    selectedCustomerId = cust.id
                    customerName = cust.name
                    customerPhone = cust.phoneNumber
                    customerAadhaar = cust.aadhaarNumber
                    customerAddress = cust.address
                    customerVillage = cust.village
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = Emerald400,
                      modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = cust.name + (if (cust.village.isNotBlank()) " (${cust.village})" else ""),
                      fontSize = 10.5.sp,
                      color = TextPrimaryDark
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Row: Mobile Number & Aadhaar Number
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = customerPhone,
              onValueChange = { if (it.length <= 10) customerPhone = it },
              label = { Text("Mobile (Optional)", fontSize = 11.sp) },
              placeholder = { Text("10 digits", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                cursorColor = Emerald400,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedLabelColor = Emerald400,
                unfocusedLabelColor = TextSecondaryDark
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(farmerPhoneBringIntoView)
                .onFocusChanged {
                  if (it.isFocused) {
                    coroutineScope.launch { farmerPhoneBringIntoView.bringIntoView() }
                  }
                }
            )

            OutlinedTextField(
              value = customerAadhaar,
              onValueChange = { if (it.length <= 12) customerAadhaar = it },
              label = { Text("Aadhaar (Optional)", fontSize = 11.sp) },
              placeholder = { Text("12 digits", fontSize = 11.sp) },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                cursorColor = Emerald400,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated,
                focusedLabelColor = Emerald400,
                unfocusedLabelColor = TextSecondaryDark
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .bringIntoViewRequester(farmerAadhaarBringIntoView)
                .onFocusChanged {
                  if (it.isFocused) {
                    coroutineScope.launch { farmerAadhaarBringIntoView.bringIntoView() }
                  }
                }
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Address Field (Full Width - Reflowed after removing Village/Area)
          OutlinedTextField(
            value = customerAddress,
            onValueChange = { customerAddress = it },
            label = { Text("Address (Optional)", fontSize = 11.sp) },
            placeholder = { Text("Street / Village / Landmark", fontSize = 11.sp) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              cursorColor = Emerald400,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated,
              focusedLabelColor = Emerald400,
              unfocusedLabelColor = TextSecondaryDark
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .fillMaxWidth()
              .bringIntoViewRequester(farmerAddressBringIntoView)
              .onFocusChanged {
                if (it.isFocused) {
                  coroutineScope.launch { farmerAddressBringIntoView.bringIntoView() }
                }
              }
              .testTag("invoice_farmer_address")
          )
        }
      }
    }
  }

    // =========================================================================
    // SECTION 2: ADD PRODUCTS
    // =========================================================================
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          // Section 2 Header
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = CircleShape,
                color = Emerald900,
                modifier = Modifier.size(24.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(14.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "2. ADD PRODUCTS",
                style = MaterialTheme.typography.labelMedium.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = Emerald400
              )
            }

            Text(
              text = "${cartItems.size} item(s)",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Unified Search & Embedded Scanner Bar
          ManglamSearchBar(
            value = productSearchQuery,
            onValueChange = {
              productSearchQuery = it
              showProductSearchDropdown = it.isNotBlank()
            },
            placeholder = "Search product, barcode or scan...",
            trailingContent = {
              IconButton(
                onClick = { showDualScannerDialog = true },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("dual_scanner_button")
              ) {
                Icon(
                  imageVector = Icons.Default.QrCodeScanner,
                  contentDescription = "Scan QR or Barcode",
                  tint = Emerald400,
                  modifier = Modifier.size(20.dp)
                )
              }
            },
            testTag = "invoice_product_search_input",
            modifier = Modifier
              .fillMaxWidth()
              .bringIntoViewRequester(productSearchBringIntoView)
              .onFocusChanged {
                if (it.isFocused) {
                  coroutineScope.launch { productSearchBringIntoView.bringIntoView() }
                }
              }
          )

          // Live Product Search Dropdown Results
          AnimatedVisibility(
            visible = showProductSearchDropdown && productSearchQuery.isNotBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
            ) {
              Text(
                text = "INVENTORY MATCHES",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Emerald400,
                letterSpacing = 0.5.sp
              )
              Spacer(modifier = Modifier.height(4.dp))

              if (matchingInventoryProducts.isEmpty()) {
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = DarkSurfaceElevated,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = "No matching products found in inventory.",
                    color = SoftRed,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(10.dp)
                  )
                }
              } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  matchingInventoryProducts.forEach { prod ->
                    Surface(
                      shape = RoundedCornerShape(8.dp),
                      color = DarkSurfaceElevated,
                      border = BorderStroke(1.dp, DarkBorder),
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                          addProductToCart(prod, 1.0)
                          productSearchQuery = ""
                          showProductSearchDropdown = false
                        }
                    ) {
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = prod.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimaryDark,
                            fontSize = 12.5.sp
                          )
                          Text(
                            text = "${prod.company} • Stock: ${prod.stockQuantity} ${prod.unit}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (prod.stockQuantity > 0) TextSecondaryDark else SoftRed
                          )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = "₹${prod.sellingPrice}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Emerald400
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Surface(
                            shape = CircleShape,
                            color = Emerald500,
                            modifier = Modifier.size(22.dp)
                          ) {
                            Box(contentAlignment = Alignment.Center) {
                              Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = DarkBg,
                                modifier = Modifier.size(12.dp)
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          if (cartItems.isEmpty()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = DarkSurfaceElevated,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Icon(
                  imageVector = Icons.Default.ShoppingCart,
                  contentDescription = null,
                  tint = TextMutedDark,
                  modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Cart is empty",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = TextSecondaryDark
                )
                Text(
                  text = "Search product above or scan barcode to add items",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMutedDark
                )
              }
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              cartItems.forEachIndexed { index, item ->
                val realProduct = products.find { it.id == item.productId }
                val maxStock = realProduct?.stockQuantity ?: 9999.0

                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = DarkSurfaceElevated,
                  border = BorderStroke(1.dp, DarkBorder),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = item.productName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = "₹${item.unitPrice}/${item.unit} • Max: ${maxStock} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                      )
                    }

                    // Quantity increment/decrement buttons
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                      Surface(
                        shape = CircleShape,
                        color = DarkCard,
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                          .size(26.dp)
                          .clip(CircleShape)
                          .clickable {
                            val targetIdx = cartItems.indexOfFirst { it.productId == item.productId }
                            if (targetIdx in 0 until cartItems.size) {
                              if (item.quantity > 1) {
                                val newQty = item.quantity - 1
                                cartItems[targetIdx] = item.copy(
                                  quantity = newQty,
                                  totalPrice = newQty * item.unitPrice
                                )
                              } else {
                                cartItems.removeAt(targetIdx)
                              }
                            }
                          }
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = TextPrimaryDark,
                            modifier = Modifier.size(12.dp)
                          )
                        }
                      }

                      Text(
                        text = "${item.quantity.toInt()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimaryDark,
                        modifier = Modifier.padding(horizontal = 4.dp)
                      )

                      Surface(
                        shape = CircleShape,
                        color = if (item.quantity < maxStock) Emerald900 else DarkCard,
                        border = BorderStroke(1.dp, if (item.quantity < maxStock) Emerald400 else DarkBorder),
                        modifier = Modifier
                          .size(26.dp)
                          .clip(CircleShape)
                          .clickable(enabled = item.quantity < maxStock) {
                            val targetIdx = cartItems.indexOfFirst { it.productId == item.productId }
                            if (targetIdx in 0 until cartItems.size) {
                              val newQty = item.quantity + 1
                              cartItems[targetIdx] = item.copy(
                                quantity = newQty,
                                totalPrice = newQty * item.unitPrice
                              )
                            }
                          }
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = if (item.quantity < maxStock) Emerald400 else TextMutedDark,
                            modifier = Modifier.size(12.dp)
                          )
                        }
                      }

                      Spacer(modifier = Modifier.width(6.dp))

                      Text(
                        text = currencyFormat.format(item.totalPrice),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Emerald400
                      )

                      IconButton(
                        onClick = {
                          val targetIdx = cartItems.indexOfFirst { it.productId == item.productId }
                          if (targetIdx in 0 until cartItems.size) {
                            cartItems.removeAt(targetIdx)
                          }
                        },
                        modifier = Modifier.size(26.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Remove",
                          tint = SoftRed,
                          modifier = Modifier.size(15.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // =========================================================================
    // SECTION 3: TAX, DISCOUNT & PAYMENT (Consolidated With Dues)
    // =========================================================================
    item {
      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = Emerald900,
              modifier = Modifier.size(24.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Payments,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "3. TAX, DISCOUNT & PAYMENT",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = Emerald400
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Subtotal Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Subtotal (${cartItems.sumOf { it.quantity }.toInt()} items)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondaryDark
            )
            Text(
              text = currencyFormat.format(subTotal),
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = TextPrimaryDark
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // GST Rate Selectable Chips
          Text(
            text = "GST Rate",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            gstRateOptions.forEach { rate ->
              val isSelected = selectedGstRate == rate
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Emerald900 else DarkSurfaceElevated,
                border = BorderStroke(1.dp, if (isSelected) Emerald400 else DarkBorder),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { selectedGstRate = rate }
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 5.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = "${rate.toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Emerald400 else TextSecondaryDark
                  )
                }
              }
            }
          }

          if (selectedGstRate > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "GST (${selectedGstRate.toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
              )
              Text(
                text = "+ ${currencyFormat.format(gstAmount)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimaryDark
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Discount Row
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Discount (₹)",
              style = MaterialTheme.typography.bodyMedium,
              color = TextSecondaryDark
            )
            OutlinedTextField(
              value = discountText,
              onValueChange = { input ->
                discountText = input.filter { ch -> ch.isDigit() || ch == '.' }
              },
              leadingIcon = {
                Text(
                  text = "₹",
                  fontSize = 13.sp,
                  color = if (discountText.isNotBlank() && discountText != "0") Emerald400 else TextMutedDark,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                )
              },
              textStyle = TextStyle(
                color = TextPrimaryDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
              ),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = if (discountText.isNotBlank() && discountText != "0") Emerald400.copy(alpha = 0.5f) else DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                cursorColor = Emerald400,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .width(135.dp)
                .bringIntoViewRequester(discountBringIntoView)
                .onFocusChanged {
                  if (it.isFocused) {
                    coroutineScope.launch { discountBringIntoView.bringIntoView() }
                  }
                }
                .testTag("invoice_discount_input")
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = DarkBorder, thickness = 1.dp)
          Spacer(modifier = Modifier.height(10.dp))

          // Total Amount / Grand Total
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Total Amount",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimaryDark
            )
            Text(
              text = currencyFormat.format(grandTotal),
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Emerald400
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Payment Method Selector
          Text(
            text = "Payment Mode",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            val modes = listOf(
              PaymentMode.CASH to "Cash",
              PaymentMode.UPI to "UPI",
              PaymentMode.CARD to "Card",
              PaymentMode.CHEQUE to "Cheque",
              PaymentMode.OTHER to "Other"
            )
            modes.forEach { (mode, label) ->
              val isSelected = paymentMode == mode
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Emerald900 else DarkSurfaceElevated,
                border = BorderStroke(1.dp, if (isSelected) Emerald400 else DarkBorder),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    paymentMode = mode
                    if (receivedAmountInput.isBlank() && !isDueManuallySet) {
                      receivedAmountInput = grandTotal.toString()
                    }
                  }
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 6.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Emerald400 else TextSecondaryDark
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Received Amount & Due Amount Fields (Clean side-by-side)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Received Amount Field
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Received Amount (₹)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimaryDark
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = if (isDueManuallySet) {
                  if (computedReceivedAmount % 1.0 == 0.0) computedReceivedAmount.toInt().toString() else computedReceivedAmount.toString()
                } else receivedAmountInput,
                onValueChange = {
                  isDueManuallySet = false
                  receivedAmountInput = it.filter { ch -> ch.isDigit() || ch == '.' }
                },
                placeholder = {
                  val formattedGrand = if (grandTotal % 1.0 == 0.0) grandTotal.toInt().toString() else grandTotal.toString()
                  Text(formattedGrand, fontSize = 12.sp, color = TextMutedDark)
                },
                leadingIcon = {
                  Text(
                    text = "₹",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Emerald400,
                    modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                  )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = TextStyle(
                  color = TextPrimaryDark,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  cursorColor = Emerald400,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .bringIntoViewRequester(receivedAmountBringIntoView)
                  .onFocusChanged {
                    if (it.isFocused) {
                      coroutineScope.launch { receivedAmountBringIntoView.bringIntoView() }
                    }
                  }
                  .testTag("received_amount_input")
              )
            }

            // Due Amount Field (Editable with validation)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Due Amount (₹)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (computedDueAmount > 0) GoldAmber else TextSecondaryDark
              )
              Spacer(modifier = Modifier.height(4.dp))
              OutlinedTextField(
                value = if (isDueManuallySet) {
                  dueAmountInput
                } else if (computedDueAmount > 0) {
                  if (computedDueAmount % 1.0 == 0.0) computedDueAmount.toInt().toString() else computedDueAmount.toString()
                } else {
                  ""
                },
                onValueChange = {
                  isDueManuallySet = true
                  val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                  dueAmountInput = filtered
                  val enteredDue = filtered.toDoubleOrNull() ?: 0.0
                  val newReceived = (grandTotal - enteredDue).coerceAtLeast(0.0)
                  receivedAmountInput = if (newReceived % 1.0 == 0.0) newReceived.toInt().toString() else newReceived.toString()
                },
                placeholder = { Text("0", fontSize = 12.sp, color = TextMutedDark) },
                leadingIcon = {
                  Text(
                    text = "₹",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (computedDueAmount > 0) GoldAmber else TextMutedDark,
                    modifier = Modifier.padding(start = 10.dp, end = 2.dp)
                  )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                textStyle = TextStyle(
                  color = if (computedDueAmount > 0) GoldAmber else TextPrimaryDark,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GoldAmber,
                  unfocusedBorderColor = if (computedDueAmount > 0) GoldAmber.copy(alpha = 0.5f) else DarkBorder,
                  focusedTextColor = if (computedDueAmount > 0) GoldAmber else TextPrimaryDark,
                  unfocusedTextColor = if (computedDueAmount > 0) GoldAmber else TextPrimaryDark,
                  cursorColor = GoldAmber,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .bringIntoViewRequester(dueAmountBringIntoView)
                  .onFocusChanged {
                    if (it.isFocused) {
                      coroutineScope.launch { dueAmountBringIntoView.bringIntoView() }
                    }
                  }
                  .testTag("due_amount_input")
              )
            }
          }

          if (computedDueAmount == 0.0 && grandTotal > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceElevated,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Fully Paid — No pending credit dues for this invoice.",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextSecondaryDark
                )
              }
            }
          }
        }
      }
    }

    // =========================================================================
    // SECTION 4: OUTSTANDING DUE (Only when Due Amount > 0)
    // =========================================================================
    if (computedDueAmount > 0) {
      item {
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = DarkCard),
          border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            // Header Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                  shape = CircleShape,
                  color = Color(0xFF382900),
                  modifier = Modifier.size(24.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Default.CalendarMonth,
                      contentDescription = null,
                      tint = GoldAmber,
                      modifier = Modifier.size(14.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "4. OUTSTANDING DUE",
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  ),
                  color = GoldAmber
                )
              }

              Text(
                text = currencyFormat.format(computedDueAmount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GoldAmber
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Will be added to customer's credit ledger automatically",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Due Date Picker Row (Full Width button)
            val formattedDueDate = remember(selectedDueDateMillis) {
              selectedDueDateMillis?.let {
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
              } ?: "Select date"
            }

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceElevated,
              border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.6f)),
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  val cal = Calendar.getInstance().apply {
                    selectedDueDateMillis?.let { timeInMillis = it }
                  }
                  DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                      val pickedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 12, 0, 0)
                      }
                      selectedDueDateMillis = pickedCal.timeInMillis
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                  ).show()
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = GoldAmber,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Due Date: $formattedDueDate",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldAmber
                  )
                }
                Text(
                  text = "Change Date",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextSecondaryDark
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick preset duration buttons: +7 Days, +15 Days, +30 Days
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val presets = listOf(7 to "+7 Days", 15 to "+15 Days", 30 to "+30 Days")
              presets.forEach { (days, label) ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = DarkSurfaceElevated,
                  border = BorderStroke(1.dp, DarkBorder),
                  modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, days) }
                      selectedDueDateMillis = cal.timeInMillis
                    }
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                  ) {
                    Text(
                      text = label,
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = TextPrimaryDark,
                      maxLines = 1,
                      softWrap = false
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // =========================================================================
    // SAVE INVOICE BUTTON & ERROR
    // =========================================================================
    item {
      formError?.let { err ->
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = SoftRed.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = SoftRed,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = err, color = SoftRed, style = MaterialTheme.typography.bodySmall)
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      Button(
        onClick = {
          if (cartItems.isEmpty()) {
            formError = "Please add at least one product to the invoice"
            return@Button
          }
          if (customerName.isBlank()) {
            formError = "Please enter Customer / Farmer Name"
            return@Button
          }

          // Stock validation
          for (item in cartItems) {
            val prod = products.find { it.id == item.productId }
            if (prod != null && item.quantity > prod.stockQuantity) {
              formError = "Quantity for ${item.productName} exceeds available stock (${prod.stockQuantity} ${prod.unit})"
              return@Button
            }
          }

          if (selectedCustomerId == null && customerName.isNotBlank() && !customerName.equals("Walk-in Farmer", ignoreCase = true)) {
            val duplicates = CustomerDuplicateHelper.findDuplicates(
              existingCustomers = customers,
              name = customerName,
              phone = customerPhone,
              village = customerVillage,
              address = customerAddress
            )
            if (duplicates.isNotEmpty()) {
              pendingDuplicateMatchForBilling = duplicates.first()
              return@Button
            }
          }

          formError = null
          isSubmitting = true

          val finalPaid = computedReceivedAmount
          val finalDue = computedDueAmount
          val finalDueDate = if (finalDue > 0) selectedDueDateMillis else null

          onSubmitInvoice(
            selectedCustomerId,
            customerName.trim(),
            customerPhone.trim(),
            customerAadhaar.trim(),
            customerAddress.trim(),
            customerVillage.trim(),
            cartItems.toList(),
            selectedGstRate,
            discountAmount,
            finalPaid,
            finalDueDate,
            paymentMode
          ) { success, msg ->
            isSubmitting = false
            if (success) {
              // Create temporary invoice object to show in PDF dialog immediately
              val generatedInv = Invoice(
                id = "inv_${System.currentTimeMillis()}",
                invoiceNumber = "INV-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}",
                customerId = selectedCustomerId,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerAadhaar = customerAadhaar.trim(),
                customerAddress = customerAddress.trim(),
                customerVillage = customerVillage.trim(),
                items = cartItems.toList(),
                subTotal = subTotal,
                gstRate = selectedGstRate,
                gstAmount = gstAmount,
                discount = discountAmount,
                grandTotal = grandTotal,
                amountPaid = finalPaid,
                remainingDue = finalDue,
                dueDate = finalDueDate,
                paymentMode = paymentMode,
                timestamp = System.currentTimeMillis()
              )
              createdInvoiceForSuccessDialog = generatedInv

              // Reset form
              selectedCustomerId = null
              customerName = ""
              customerPhone = ""
              customerAadhaar = ""
              customerAddress = ""
              customerVillage = ""
              cartItems.clear()
              discountText = "0"
              receivedAmountInput = ""
              dueAmountInput = ""
              isDueManuallySet = false
            } else {
              formError = msg ?: "Failed to save invoice"
            }
          }
        },
        enabled = !isSubmitting && cartItems.isNotEmpty(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Emerald500,
          contentColor = DarkBg,
          disabledContainerColor = DarkSurfaceElevated,
          disabledContentColor = TextMutedDark
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_and_generate_invoice_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Save & Generate Invoice (${currencyFormat.format(grandTotal)})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))
    }
  }

  // =========================================================================
  // MODALS & DIALOGS
  // =========================================================================

  // 1. Dual Product Scanner Dialog
  if (showDualScannerDialog) {
    DualProductScannerDialog(
      products = products,
      onDismiss = { showDualScannerDialog = false },
      onProductSelected = { prod ->
        addProductToCart(prod, 1.0)
        showDualScannerDialog = false
      }
    )
  }

  // 2. Customer Search & Selection Dialog
  if (showCustomerSearchDialog) {
    var custQuery by remember { mutableStateOf("") }
    val filteredCusts = remember(custQuery, customers) {
      if (custQuery.isBlank()) customers
      else customers.filter {
        it.name.contains(custQuery, ignoreCase = true) ||
            it.phoneNumber.contains(custQuery) ||
            it.village.contains(custQuery, ignoreCase = true)
      }
    }

    AlertDialog(
      onDismissRequest = { showCustomerSearchDialog = false },
      title = {
        Text("Select Customer / Farmer", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
      },
      text = {
        Column(modifier = Modifier.height(350.dp)) {
          ManglamSearchBar(
            value = custQuery,
            onValueChange = { custQuery = it },
            placeholder = "Search by name, phone, or village...",
            testTag = "billing_customer_search_input",
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (filteredCusts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text("No registered customers found.", color = TextMutedDark, fontSize = 12.sp)
            }
          } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              items(filteredCusts, key = { it.id }) { cust ->
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = DarkSurfaceElevated,
                  border = BorderStroke(1.dp, DarkBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      selectedCustomerId = cust.id
                      customerName = cust.name
                      customerPhone = cust.phoneNumber
                      customerAadhaar = cust.aadhaarNumber
                      customerAddress = cust.address
                      customerVillage = cust.village
                      showCustomerSearchDialog = false
                    }
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(cust.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                      Text(
                        text = (if (cust.phoneNumber.isNotBlank()) "${cust.phoneNumber} • " else "") + cust.village,
                        fontSize = 11.sp,
                        color = TextSecondaryDark
                      )
                    }

                    if (cust.totalDue > 0) {
                      Column(horizontalAlignment = Alignment.End) {
                        Text("Due Balance", fontSize = 10.sp, color = GoldAmber)
                        Text(currencyFormat.format(cust.totalDue), fontWeight = FontWeight.Bold, color = GoldAmber, fontSize = 12.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showCustomerSearchDialog = false }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // 4. Success / Invoice Generated Action Dialog
  createdInvoiceForSuccessDialog?.let { generatedInvoice ->
    InvoiceGeneratedSuccessDialog(
      invoice = generatedInvoice,
      currencyFormat = currencyFormat,
      onDismiss = { createdInvoiceForSuccessDialog = null },
      onPrint = { onPrintInvoice(generatedInvoice) }
    )
  }

  // 5. Duplicate Customer Warning Dialog for Invoicing
  pendingDuplicateMatchForBilling?.let { match ->
    DuplicateCustomerWarningDialog(
      enteredName = customerName,
      enteredPhone = customerPhone,
      enteredVillage = customerVillage,
      enteredAddress = customerAddress,
      match = match,
      onUseExisting = { existing ->
        pendingDuplicateMatchForBilling = null
        selectedCustomerId = existing.id
        customerName = existing.name
        customerPhone = existing.phoneNumber
        customerVillage = existing.village
        customerAddress = existing.address

        formError = null
        isSubmitting = true
        val finalPaid = computedReceivedAmount
        val finalDue = computedDueAmount
        val finalDueDate = if (finalDue > 0) selectedDueDateMillis else null

        onSubmitInvoice(
          existing.id,
          existing.name.trim(),
          existing.phoneNumber.trim(),
          customerAadhaar.trim(),
          existing.address.trim(),
          existing.village.trim(),
          cartItems.toList(),
          selectedGstRate,
          discountAmount,
          finalPaid,
          finalDueDate,
          paymentMode
        ) { success, msg ->
          isSubmitting = false
          if (success) {
            val generatedInv = Invoice(
              id = "inv_${System.currentTimeMillis()}",
              invoiceNumber = "INV-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}",
              customerId = existing.id,
              customerName = existing.name.trim(),
              customerPhone = existing.phoneNumber.trim(),
              customerAadhaar = customerAadhaar.trim(),
              customerAddress = existing.address.trim(),
              customerVillage = existing.village.trim(),
              items = cartItems.toList(),
              subTotal = subTotal,
              gstRate = selectedGstRate,
              gstAmount = gstAmount,
              discount = discountAmount,
              grandTotal = grandTotal,
              amountPaid = finalPaid,
              remainingDue = finalDue,
              dueDate = finalDueDate,
              paymentMode = paymentMode,
              timestamp = System.currentTimeMillis()
            )
            createdInvoiceForSuccessDialog = generatedInv
            selectedCustomerId = null
            customerName = ""
            customerPhone = ""
            customerAadhaar = ""
            customerAddress = ""
            customerVillage = ""
            cartItems.clear()
            discountText = "0"
            receivedAmountInput = ""
            dueAmountInput = ""
            isDueManuallySet = false
          } else {
            formError = msg ?: "Failed to save invoice"
          }
        }
      },
      onUpdateAndUse = { updated ->
        pendingDuplicateMatchForBilling = null
        selectedCustomerId = updated.id
        customerName = updated.name
        customerPhone = updated.phoneNumber
        customerVillage = updated.village
        customerAddress = updated.address
        onUpdateCustomer?.invoke(updated) { _, _ -> }

        formError = null
        isSubmitting = true
        val finalPaid = computedReceivedAmount
        val finalDue = computedDueAmount
        val finalDueDate = if (finalDue > 0) selectedDueDateMillis else null

        onSubmitInvoice(
          updated.id,
          updated.name.trim(),
          updated.phoneNumber.trim(),
          customerAadhaar.trim(),
          updated.address.trim(),
          updated.village.trim(),
          cartItems.toList(),
          selectedGstRate,
          discountAmount,
          finalPaid,
          finalDueDate,
          paymentMode
        ) { success, msg ->
          isSubmitting = false
          if (success) {
            val generatedInv = Invoice(
              id = "inv_${System.currentTimeMillis()}",
              invoiceNumber = "INV-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}",
              customerId = updated.id,
              customerName = updated.name.trim(),
              customerPhone = updated.phoneNumber.trim(),
              customerAadhaar = customerAadhaar.trim(),
              customerAddress = updated.address.trim(),
              customerVillage = updated.village.trim(),
              items = cartItems.toList(),
              subTotal = subTotal,
              gstRate = selectedGstRate,
              gstAmount = gstAmount,
              discount = discountAmount,
              grandTotal = grandTotal,
              amountPaid = finalPaid,
              remainingDue = finalDue,
              dueDate = finalDueDate,
              paymentMode = paymentMode,
              timestamp = System.currentTimeMillis()
            )
            createdInvoiceForSuccessDialog = generatedInv
            selectedCustomerId = null
            customerName = ""
            customerPhone = ""
            customerAadhaar = ""
            customerAddress = ""
            customerVillage = ""
            cartItems.clear()
            discountText = "0"
            receivedAmountInput = ""
            dueAmountInput = ""
            isDueManuallySet = false
          } else {
            formError = msg ?: "Failed to save invoice"
          }
        }
      },
      onAddAsNew = {
        pendingDuplicateMatchForBilling = null
        formError = null
        isSubmitting = true
        val finalPaid = computedReceivedAmount
        val finalDue = computedDueAmount
        val finalDueDate = if (finalDue > 0) selectedDueDateMillis else null

        onSubmitInvoice(
          null,
          customerName.trim(),
          customerPhone.trim(),
          customerAadhaar.trim(),
          customerAddress.trim(),
          customerVillage.trim(),
          cartItems.toList(),
          selectedGstRate,
          discountAmount,
          finalPaid,
          finalDueDate,
          paymentMode
        ) { success, msg ->
          isSubmitting = false
          if (success) {
            val generatedInv = Invoice(
              id = "inv_${System.currentTimeMillis()}",
              invoiceNumber = "INV-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}",
              customerId = null,
              customerName = customerName.trim(),
              customerPhone = customerPhone.trim(),
              customerAadhaar = customerAadhaar.trim(),
              customerAddress = customerAddress.trim(),
              customerVillage = customerVillage.trim(),
              items = cartItems.toList(),
              subTotal = subTotal,
              gstRate = selectedGstRate,
              gstAmount = gstAmount,
              discount = discountAmount,
              grandTotal = grandTotal,
              amountPaid = finalPaid,
              remainingDue = finalDue,
              dueDate = finalDueDate,
              paymentMode = paymentMode,
              timestamp = System.currentTimeMillis()
            )
            createdInvoiceForSuccessDialog = generatedInv
            selectedCustomerId = null
            customerName = ""
            customerPhone = ""
            customerAadhaar = ""
            customerAddress = ""
            customerVillage = ""
            cartItems.clear()
            discountText = "0"
            receivedAmountInput = ""
            dueAmountInput = ""
            isDueManuallySet = false
          } else {
            formError = msg ?: "Failed to save invoice"
          }
        }
      },
      onDismiss = {
        pendingDuplicateMatchForBilling = null
      }
    )
  }
}

@Composable
private fun InvoiceGeneratedSuccessDialog(
  invoice: Invoice,
  currencyFormat: NumberFormat,
  onDismiss: () -> Unit,
  onPrint: () -> Unit
) {
  val context = LocalContext.current

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = CircleShape,
          color = Emerald900,
          modifier = Modifier.size(32.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(20.dp))
          }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text("Invoice Generated!", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 16.sp)
          Text(invoice.invoiceNumber, fontSize = 11.5.sp, color = Emerald400)
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = "Invoice for ${invoice.customerName} saved successfully.",
          color = TextSecondaryDark,
          fontSize = 12.sp
        )
        Text(
          text = "Grand Total: ${currencyFormat.format(invoice.grandTotal)} • Paid: ${currencyFormat.format(invoice.amountPaid)}" +
              (if (invoice.remainingDue > 0) " • Due: ${currencyFormat.format(invoice.remainingDue)}" else ""),
          fontWeight = FontWeight.Bold,
          color = TextPrimaryDark,
          fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // PDF Actions
        Button(
          onClick = {
            val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
            InvoicePdfGenerator.viewInvoicePdf(context, pdfFile)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("View Invoice PDF", fontWeight = FontWeight.Bold)
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
              InvoicePdfGenerator.shareInvoicePdf(context, pdfFile, invoice)
            },
            border = BorderStroke(1.dp, Emerald400),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Emerald400, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Share PDF", color = Emerald400, fontSize = 11.5.sp)
          }

          OutlinedButton(
            onClick = {
              val pdfFile = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
              InvoicePdfGenerator.saveInvoicePdfToDownloads(context, pdfFile, invoice)
            },
            border = BorderStroke(1.dp, Emerald400),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = Emerald400, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Save PDF", color = Emerald400, fontSize = 11.5.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimaryDark)
      ) {
        Text("Done")
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          onPrint()
          onDismiss()
        }
      ) {
        Icon(Icons.Default.Print, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Print", color = Emerald400)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(14.dp)
  )
}

@Composable
private fun InvoiceHistoryTab(
  invoices: List<Invoice>,
  currentUser: User? = null,
  onPrintInvoice: (Invoice) -> Unit,
  onDeleteInvoice: ((String, (Boolean, String?) -> Unit) -> Unit)? = null
) {
  val context = LocalContext.current
  val isAdmin = AdminAuthUtils.isAdmin(currentUser)
  var searchQuery by remember { mutableStateOf("") }
  var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }
  var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }
  val historyListState = rememberLazyListState()
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

  // BackHandler to dismiss invoice detail or search query
  BackHandler(enabled = selectedInvoice != null || searchQuery.isNotBlank()) {
    if (selectedInvoice != null) {
      selectedInvoice = null
    } else if (searchQuery.isNotBlank()) {
      searchQuery = ""
    }
  }

  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }

  val filtered = remember(searchQuery, invoices) {
    if (searchQuery.isBlank()) invoices
    else invoices.filter {
      it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
          it.customerName.contains(searchQuery, ignoreCase = true) ||
          it.customerPhone.contains(searchQuery)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(12.dp)
  ) {
    ManglamSearchBar(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = "Search by invoice #, farmer, or phone...",
      testTag = "invoice_history_search_input",
      modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(10.dp))

    if (filtered.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Receipt, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(44.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text("No invoices found", style = MaterialTheme.typography.titleMedium, color = TextPrimaryDark)
          Text("Invoices created in the billing tab will appear here", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
        }
      }
    } else {
      LazyColumn(
        state = historyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filtered, key = { it.id }) { inv ->
          val dateStr = dateFormat.format(Date(inv.timestamp))
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedInvoice = inv }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = inv.customerName,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimaryDark
                )
                Text(
                  text = "${inv.invoiceNumber} • $dateStr",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMutedDark
                )
                if (inv.remainingDue > 0) {
                  Text(
                    text = "Due: ${currencyFormat.format(inv.remainingDue)}" +
                        (if (inv.dueDate != null) " (Due: ${SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(inv.dueDate))})" else ""),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = GoldAmber
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = currencyFormat.format(inv.grandTotal),
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Emerald400
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = inv.paymentMode.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inv.remainingDue > 0) GoldAmber else TextSecondaryDark
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  IconButton(
                    onClick = { onPrintInvoice(inv) },
                    modifier = Modifier.size(28.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Print,
                      contentDescription = "Print",
                      tint = Emerald400,
                      modifier = Modifier.size(15.dp)
                    )
                  }
                  if (isAdmin && onDeleteInvoice != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                      onClick = { invoiceToDelete = inv },
                      modifier = Modifier.size(28.dp).testTag("delete_invoice_${inv.id}")
                    ) {
                      Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Invoice",
                        tint = SoftRed,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Invoice Detail Dialog
  selectedInvoice?.let { inv ->
    InvoiceDetailDialog(
      invoice = inv,
      currencyFormat = currencyFormat,
      isAdmin = isAdmin,
      onDismiss = { selectedInvoice = null },
      onPrint = { onPrintInvoice(inv) },
      onDelete = {
        invoiceToDelete = inv
      }
    )
  }

  // Admin Delete Confirmation Dialog
  invoiceToDelete?.let { targetInv ->
    AlertDialog(
      onDismissRequest = { invoiceToDelete = null },
      icon = {
        Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(32.dp))
      },
      title = {
        Text("Delete Invoice?", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "This will permanently remove invoice ${targetInv.invoiceNumber} for ${targetInv.customerName}.",
            color = TextPrimaryDark,
            fontSize = 13.sp
          )
          Text(
            "• Stock deduction will be reversed (${targetInv.items.sumOf { it.quantity }.toInt()} items returned to inventory).\n" +
            "• Customer due balance will be updated accordingly.\n" +
            "• Credit ledger entries will be removed.",
            color = TextSecondaryDark,
            fontSize = 11.5.sp
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val toDeleteId = targetInv.id
            onDeleteInvoice?.invoke(toDeleteId) { success, errorMsg ->
              if (success) {
                Toast.makeText(context, "Invoice deleted & stock restored successfully", Toast.LENGTH_SHORT).show()
                invoiceToDelete = null
                if (selectedInvoice?.id == toDeleteId) {
                  selectedInvoice = null
                }
              } else {
                Toast.makeText(context, errorMsg ?: "Failed to delete invoice", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White),
          modifier = Modifier.testTag("confirm_delete_invoice_btn")
        ) {
          Text("Delete Invoice", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { invoiceToDelete = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(14.dp)
    )
  }
}

@Composable
private fun InvoiceDetailDialog(
  invoice: Invoice,
  currencyFormat: NumberFormat,
  isAdmin: Boolean = false,
  onDismiss: () -> Unit,
  onPrint: () -> Unit,
  onDelete: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "TAX INVOICE RECEIPT",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
            color = Emerald400
          )
          Text(
            text = invoice.invoiceNumber,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimaryDark
          )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMutedDark, modifier = Modifier.size(16.dp))
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("Customer: ${invoice.customerName}", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
        if (invoice.customerPhone.isNotBlank()) Text("Mobile: ${invoice.customerPhone}", color = TextSecondaryDark, fontSize = 11.5.sp)
        if (invoice.customerVillage.isNotBlank()) Text("Village: ${invoice.customerVillage}", color = TextSecondaryDark, fontSize = 11.5.sp)
        Text("Date: $dateStr", color = TextMutedDark, fontSize = 10.5.sp)

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Text("ITEMS (${invoice.items.size})", fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 10.5.sp, letterSpacing = 0.5.sp)
        Spacer(modifier = Modifier.height(4.dp))

        invoice.items.forEach { item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("${item.productName} (${item.quantity.toInt()} ${item.unit})", color = TextPrimaryDark, fontSize = 11.5.sp)
            Text(currencyFormat.format(item.totalPrice), color = TextPrimaryDark, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Subtotal:", color = TextSecondaryDark, fontSize = 11.5.sp)
          Text(currencyFormat.format(invoice.subTotal), color = TextPrimaryDark, fontSize = 11.5.sp)
        }
        if (invoice.gstRate > 0) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GST (${invoice.gstRate.toInt()}%):", color = TextSecondaryDark, fontSize = 11.5.sp)
            Text("+ ${currencyFormat.format(invoice.gstAmount)}", color = TextPrimaryDark, fontSize = 11.5.sp)
          }
        }
        if (invoice.discount > 0) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Discount:", color = TextSecondaryDark, fontSize = 11.5.sp)
            Text("- ${currencyFormat.format(invoice.discount)}", color = TextPrimaryDark, fontSize = 11.5.sp)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Grand Total:", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
          Text(currencyFormat.format(invoice.grandTotal), fontWeight = FontWeight.Bold, color = Emerald400)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Amount Paid (${invoice.paymentMode.name}):", color = TextSecondaryDark, fontSize = 11.5.sp)
          Text(currencyFormat.format(invoice.amountPaid), color = TextPrimaryDark, fontSize = 11.5.sp)
        }

        if (invoice.remainingDue > 0) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pending Due (Udhar):", fontWeight = FontWeight.Bold, color = GoldAmber)
            Text(currencyFormat.format(invoice.remainingDue), fontWeight = FontWeight.Bold, color = GoldAmber)
          }
          if (invoice.dueDate != null) {
            val dueFormatted = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(invoice.dueDate))
            Text("Expected Payment Date: $dueFormatted", color = GoldAmber, fontSize = 10.5.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // PDF View / Share Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          OutlinedButton(
            onClick = {
              val file = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
              InvoicePdfGenerator.viewInvoicePdf(context, file)
            },
            border = BorderStroke(1.dp, Emerald400),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Visibility, contentDescription = null, tint = Emerald400, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("View PDF", color = Emerald400, fontSize = 11.sp)
          }

          OutlinedButton(
            onClick = {
              val file = InvoicePdfGenerator.generateInvoicePdf(context, invoice)
              InvoicePdfGenerator.shareInvoicePdf(context, file, invoice)
            },
            border = BorderStroke(1.dp, Emerald400),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Emerald400, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Share PDF", color = Emerald400, fontSize = 11.sp)
          }
        }

        if (isAdmin && onDelete != null) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = { onDelete() },
            border = BorderStroke(1.dp, SoftRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("dialog_delete_invoice_btn")
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = SoftRed, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete Invoice (Admin Only)", color = SoftRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onPrint(); onDismiss() },
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
      ) {
        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Print")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(14.dp)
  )
}
