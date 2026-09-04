package com.manglamfertilizer.app.ui.customers

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.CustomerDuplicateHelper
import com.manglamfertilizer.app.data.util.DuplicateCustomerMatch
import com.manglamfertilizer.app.ui.common.ManglamFloatingActionButton
import com.manglamfertilizer.app.ui.common.ManglamSearchBar
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(
  customers: List<Customer>,
  invoices: List<Invoice> = emptyList(),
  currentUser: User? = null,
  onAddCustomer: (name: String, phone: String, village: String, address: String, onDone: (Boolean, String?) -> Unit) -> Unit,
  onUpdateCustomer: ((Customer, onDone: (Boolean, String?) -> Unit) -> Unit)? = null,
  onRecordPayment: (customerId: String, amount: Double, onDone: (Boolean, String?) -> Unit) -> Unit,
  onDeleteCustomer: (String, (Boolean, String?) -> Unit) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val isAdmin = AdminAuthUtils.isAdmin(currentUser)
  var selectedTab by remember { mutableStateOf(0) }
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var pendingDuplicateMatchForAdd by remember { mutableStateOf<Pair<DuplicateCustomerMatch, List<String>>?>(null) }
  var paymentCustomer by remember { mutableStateOf<Customer?>(null) }
  var deletingCustomer by remember { mutableStateOf<Customer?>(null) }
  var selectedCustomerForDetails by remember { mutableStateOf<Customer?>(null) }
  var blockedDueWarningCustomer by remember { mutableStateOf<Customer?>(null) }
  val customersListState = rememberLazyListState()

  // BackHandler to handle dialog dismissals and sub-state navigation
  val isAnyCustomerSubViewOpen = pendingDuplicateMatchForAdd != null ||
      deletingCustomer != null ||
      paymentCustomer != null ||
      selectedCustomerForDetails != null ||
      blockedDueWarningCustomer != null ||
      showAddDialog ||
      searchQuery.isNotBlank() ||
      selectedTab == 1

  BackHandler(enabled = isAnyCustomerSubViewOpen) {
    when {
      pendingDuplicateMatchForAdd != null -> pendingDuplicateMatchForAdd = null
      blockedDueWarningCustomer != null -> blockedDueWarningCustomer = null
      deletingCustomer != null -> deletingCustomer = null
      selectedCustomerForDetails != null -> selectedCustomerForDetails = null
      paymentCustomer != null -> paymentCustomer = null
      showAddDialog -> showAddDialog = false
      searchQuery.isNotBlank() -> searchQuery = ""
      selectedTab == 1 -> selectedTab = 0
    }
  }

  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }

  val totalDues = remember(customers) {
    customers.sumOf { it.totalDue }
  }

  val dueCustomersCount = remember(customers) {
    customers.count { it.totalDue > 0 }
  }

  val filteredCustomers = remember(searchQuery, selectedTab, customers) {
    customers.filter { cust ->
      val matchesTab = if (selectedTab == 1) cust.totalDue > 0 else true
      val matchesQuery = searchQuery.isBlank() ||
          cust.name.contains(searchQuery, ignoreCase = true) ||
          cust.phoneNumber.contains(searchQuery) ||
          cust.village.contains(searchQuery, ignoreCase = true) ||
          cust.address.contains(searchQuery, ignoreCase = true)
      matchesTab && matchesQuery
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg),
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    floatingActionButton = {
      ManglamFloatingActionButton(
        onClick = { showAddDialog = true },
        contentDescription = "Add Customer",
        testTag = "add_customer_fab",
        modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
      )
    },
    containerColor = DarkBg
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .imePadding()
    ) {
      // Top Header
      Surface(
        color = DarkSurface,
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
          Text(
            text = "Farmers & Accounts",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimaryDark
          )
          Text(
            text = "Total Outstanding Dues: ${currencyFormat.format(totalDues)}",
            style = MaterialTheme.typography.bodySmall,
            color = GoldAmber
          )
        }
      }

      // Tabs: All Farmers vs DUE (Strict requirement: section strictly named "DUE")
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = DarkSurface,
        contentColor = Emerald400,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = if (selectedTab == 1) GoldAmber else Emerald400
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Text(
              "All Farmers (${customers.size})",
              fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 0) Emerald400 else TextSecondaryDark
            )
          },
          modifier = Modifier.testTag("tab_all_farmers")
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Text(
              "DUE ($dueCustomersCount)",
              fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 1) GoldAmber else TextSecondaryDark
            )
          },
          modifier = Modifier.testTag("tab_due")
        )
      }

      Column(modifier = Modifier.padding(16.dp)) {
        // Search Input (Standardized ManglamSearchBar)
        com.manglamfertilizer.app.ui.common.ManglamSearchBar(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = if (selectedTab == 1) "Search due accounts..." else "Search farmer by name, mobile, address...",
          leadingIconTint = if (selectedTab == 1) GoldAmber else Emerald400,
          testTag = "customers_search_input",
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredCustomers.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = if (selectedTab == 1) Icons.Default.MonetizationOn else Icons.Default.People,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = if (selectedTab == 1) "No pending dues found" else "No farmers registered",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark
              )
              Text(
                text = if (selectedTab == 1) "All farmer accounts are settled with 0 due balance" else "Add farmers using the + button below",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
              )
            }
          }
        } else {
          LazyColumn(
            state = customersListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(filteredCustomers, key = { it.id }) { cust ->
              if (selectedTab == 0) {
                // Section 7 & 8: "All Farmers" list item showing ONLY 3 fields: Name, Address, Mobile
                AllFarmersRowItem(
                  customer = cust,
                  isAdmin = isAdmin,
                  onClick = { selectedCustomerForDetails = cust },
                  onDeleteClick = {
                    if (cust.totalDue > 0) {
                      blockedDueWarningCustomer = cust
                    } else {
                      deletingCustomer = cust
                    }
                  }
                )
              } else {
                // Section 9 & 10: "DUE" section item showing: Name, Due Amount, Due Date, Pay Due button
                val customerInvoices = invoices.filter { it.customerId == cust.id && it.remainingDue > 0 }
                val latestDueDate = customerInvoices.mapNotNull { it.dueDate }.maxOrNull()
                DueSectionRowItem(
                  customer = cust,
                  dueDateMillis = latestDueDate,
                  pendingInvoicesCount = customerInvoices.size,
                  currencyFormat = currencyFormat,
                  onRecordPayment = { paymentCustomer = cust },
                  onClick = { selectedCustomerForDetails = cust }
                )
              }
            }
          }
        }
      }
    }
  }

  // Customer Details / Profile Dialog
  selectedCustomerForDetails?.let { cust ->
    val custInvoices = invoices.filter { it.customerId == cust.id }
    CustomerProfileDialog(
      customer = cust,
      invoices = custInvoices,
      currencyFormat = currencyFormat,
      isAdmin = isAdmin,
      onDismiss = { selectedCustomerForDetails = null },
      onPayDue = {
        selectedCustomerForDetails = null
        paymentCustomer = cust
      },
      onDelete = {
        selectedCustomerForDetails = null
        if (cust.totalDue > 0) {
          blockedDueWarningCustomer = cust
        } else {
          deletingCustomer = cust
        }
      }
    )
  }

  // Add Customer Dialog
  if (showAddDialog) {
    AddCustomerDialog(
      onDismiss = { showAddDialog = false },
      onSubmit = { name, phone, village, address, onDone ->
        val duplicates = CustomerDuplicateHelper.findDuplicates(
          existingCustomers = customers,
          name = name,
          phone = phone,
          village = village,
          address = address
        )
        if (duplicates.isNotEmpty()) {
          onDone(true, null)
          showAddDialog = false
          pendingDuplicateMatchForAdd = Pair(duplicates.first(), listOf(name, phone, village, address))
        } else {
          onAddCustomer(name, phone, village, address) { success, msg ->
            onDone(success, msg)
            if (success) showAddDialog = false
          }
        }
      }
    )
  }

  // Duplicate Warning Dialog for Adding Farmer
  pendingDuplicateMatchForAdd?.let { (match, enteredFields) ->
    val (eName, ePhone, eVillage, eAddress) = enteredFields
    DuplicateCustomerWarningDialog(
      enteredName = eName,
      enteredPhone = ePhone,
      enteredVillage = eVillage,
      enteredAddress = eAddress,
      match = match,
      onUseExisting = { existing ->
        pendingDuplicateMatchForAdd = null
        selectedCustomerForDetails = existing
      },
      onUpdateAndUse = { updated ->
        pendingDuplicateMatchForAdd = null
        onUpdateCustomer?.invoke(updated) { success, _ ->
          if (success) {
            selectedCustomerForDetails = updated
          }
        } ?: run {
          selectedCustomerForDetails = updated
        }
      },
      onAddAsNew = {
        pendingDuplicateMatchForAdd = null
        onAddCustomer(eName, ePhone, eVillage, eAddress) { success, msg ->
          if (!success) {
            Toast.makeText(context, msg ?: "Failed to add farmer", Toast.LENGTH_LONG).show()
          }
        }
      },
      onDismiss = {
        pendingDuplicateMatchForAdd = null
      }
    )
  }

  // Record Due Payment Dialog
  paymentCustomer?.let { cust ->
    RecordPaymentDialog(
      customer = cust,
      currencyFormat = currencyFormat,
      onDismiss = { paymentCustomer = null },
      onSubmit = { amount, onDone ->
        onRecordPayment(cust.id, amount) { success, msg ->
          onDone(success, msg)
          if (success) {
            paymentCustomer = null
          }
        }
      }
    )
  }

  // Blocked Delete Warning Dialog (when customer has active dues)
  blockedDueWarningCustomer?.let { cust ->
    AlertDialog(
      onDismissRequest = { blockedDueWarningCustomer = null },
      icon = {
        Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(36.dp))
      },
      title = {
        Text("Cannot Delete Farmer", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
      },
      text = {
        Text(
          "Farmer '${cust.name}' has an outstanding pending due of ${currencyFormat.format(cust.totalDue)}. You cannot delete a customer record with active dues. Please settle or clear the dues first.",
          color = TextSecondaryDark,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            blockedDueWarningCustomer = null
            paymentCustomer = cust
          },
          colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = DarkBg)
        ) {
          Text("Settle Dues First", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { blockedDueWarningCustomer = null }) {
          Text("Close", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(16.dp)
    )
  }

  // Admin Delete Customer Confirmation Dialog
  deletingCustomer?.let { cust ->
    AlertDialog(
      onDismissRequest = { deletingCustomer = null },
      icon = {
        Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(32.dp))
      },
      title = { Text("Delete Farmer Record?", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
      text = {
        Text(
          "Are you sure you want to remove farmer '${cust.name}'? This action cannot be undone.",
          color = TextSecondaryDark
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCustomer(cust.id) { success, msg ->
              deletingCustomer = null
              if (!success) {
                Toast.makeText(context, msg ?: "Failed to delete customer", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White),
          modifier = Modifier.testTag("confirm_delete_farmer_btn")
        ) {
          Text("Delete Farmer", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingCustomer = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(16.dp)
    )
  }
}

/**
 * Section 7 & 8: "All Farmers" list item showing strictly 3 information fields:
 * 1. Name
 * 2. Address (village / address)
 * 3. Mobile Number
 * Plus Admin-authorized Delete button.
 */
@Composable
private fun AllFarmersRowItem(
  customer: Customer,
  isAdmin: Boolean,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit
) {
  val displayAddress = when {
    customer.village.isNotBlank() && customer.address.isNotBlank() -> "${customer.village}, ${customer.address}"
    customer.village.isNotBlank() -> customer.village
    customer.address.isNotBlank() -> customer.address
    else -> "No address recorded"
  }

  val displayPhone = if (customer.phoneNumber.isNotBlank()) customer.phoneNumber else "No mobile recorded"

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("farmer_item_${customer.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        Surface(
          shape = CircleShape,
          color = Emerald900,
          modifier = Modifier.size(40.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.People,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          // Field 1: Farmer Name
          Text(
            text = customer.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimaryDark
          )

          Spacer(modifier = Modifier.height(2.dp))

          // Field 2: Address
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = TextMutedDark,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = displayAddress,
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark,
              maxLines = 1
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          // Field 3: Mobile Number
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Call,
              contentDescription = null,
              tint = TextMutedDark,
              modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = displayPhone,
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark
            )
          }
        }
      }

      if (isAdmin) {
        IconButton(
          onClick = onDeleteClick,
          modifier = Modifier.size(32.dp).testTag("delete_farmer_btn_${customer.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete Farmer",
            tint = SoftRed,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

/**
 * Section 9 & 10: "DUE" section item showing:
 * 1. Farmer Name
 * 2. Due Amount
 * 3. Due Date
 * 4. Quick "Pay Due" button
 */
@Composable
private fun DueSectionRowItem(
  customer: Customer,
  dueDateMillis: Long?,
  pendingInvoicesCount: Int,
  currencyFormat: NumberFormat,
  onRecordPayment: () -> Unit,
  onClick: () -> Unit
) {
  val dueDateFormatted = if (dueDateMillis != null && dueDateMillis > 0L) {
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dueDateMillis))
  } else {
    val lastTx = customer.lastTransactionDate
    if (lastTx != null && lastTx > 0L) {
      SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(lastTx))
    } else {
      "Immediate / Settle on visit"
    }
  }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, GoldAmber),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("due_item_${customer.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(modifier = Modifier.weight(1f)) {
        // Farmer Name
        Text(
          text = customer.name,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
          color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Due Amount
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Due Amount: ",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondaryDark
          )
          Text(
            text = currencyFormat.format(customer.totalDue),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = GoldAmber
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Due Date
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = TextMutedDark,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Due Date: $dueDateFormatted",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark
          )
        }

        if (pendingInvoicesCount > 1) {
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "Across $pendingInvoicesCount pending invoices",
            style = MaterialTheme.typography.labelSmall,
            color = Emerald400
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Quick "Pay Due" Button
      Button(
        onClick = onRecordPayment,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = DarkBg),
        modifier = Modifier.testTag("pay_due_btn_${customer.id}")
      ) {
        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Pay Due", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Complete profile & ledger dialog when tapping a farmer.
 */
@Composable
private fun CustomerProfileDialog(
  customer: Customer,
  invoices: List<Invoice>,
  currencyFormat: NumberFormat,
  isAdmin: Boolean,
  onDismiss: () -> Unit,
  onPayDue: () -> Unit,
  onDelete: () -> Unit
) {
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
            text = "FARMER PROFILE",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
            color = Emerald400
          )
          Text(
            text = customer.name,
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
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (customer.phoneNumber.isNotBlank()) {
          Row {
            Text("Mobile: ", color = TextSecondaryDark, fontSize = 12.sp)
            Text(customer.phoneNumber, color = TextPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
          }
        }
        if (customer.village.isNotBlank()) {
          Row {
            Text("Village: ", color = TextSecondaryDark, fontSize = 12.sp)
            Text(customer.village, color = TextPrimaryDark, fontSize = 12.sp)
          }
        }
        if (customer.address.isNotBlank()) {
          Row {
            Text("Address: ", color = TextSecondaryDark, fontSize = 12.sp)
            Text(customer.address, color = TextPrimaryDark, fontSize = 12.sp)
          }
        }

        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Total Purchases:", color = TextSecondaryDark, fontSize = 12.sp)
          Text(currencyFormat.format(customer.totalPurchases), color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Outstanding Due (Udhar):", color = if (customer.totalDue > 0) GoldAmber else TextSecondaryDark, fontSize = 12.sp)
          Text(currencyFormat.format(customer.totalDue), color = if (customer.totalDue > 0) GoldAmber else Emerald400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        if (customer.totalDue > 0) {
          Spacer(modifier = Modifier.height(4.dp))
          Button(
            onClick = onPayDue,
            colors = ButtonDefaults.buttonColors(containerColor = GoldAmber, contentColor = DarkBg),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pay / Settle Due Balance", fontWeight = FontWeight.Bold)
          }
        }

        if (invoices.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text("Recent Invoices (${invoices.size})", fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 11.sp)
          invoices.take(3).forEach { inv ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(inv.invoiceNumber, color = TextSecondaryDark, fontSize = 11.sp)
              Text(currencyFormat.format(inv.grandTotal), color = TextPrimaryDark, fontSize = 11.sp)
            }
          }
        }

        if (isAdmin) {
          Spacer(modifier = Modifier.height(6.dp))
          OutlinedButton(
            onClick = onDelete,
            border = BorderStroke(1.dp, SoftRed),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, tint = SoftRed, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Delete Farmer Record", color = SoftRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
      ) {
        Text("Close")
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun AddCustomerDialog(
  onDismiss: () -> Unit,
  onSubmit: (name: String, phone: String, village: String, address: String, onDone: (Boolean, String?) -> Unit) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var village by remember { mutableStateOf("") }
  var address by remember { mutableStateOf("") }
  var isSaving by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    modifier = Modifier.imePadding(),
    title = { Text("Add Farmer / Customer", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Farmer Name *") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone Number") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = village,
          onValueChange = { village = it },
          label = { Text("Village / Gram") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = address,
          onValueChange = { address = it },
          label = { Text("Detailed Address") },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          modifier = Modifier.fillMaxWidth()
        )
        errorMsg?.let { err ->
          Text(text = err, color = SoftRed, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isBlank()) {
            errorMsg = "Please enter customer name"
            return@Button
          }
          isSaving = true
          onSubmit(name, phone, village, address) { success, msg ->
            isSaving = false
            if (!success) errorMsg = msg
          }
        },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
      ) {
        if (isSaving) CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        else Text("Save Farmer")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) {
        Text("Cancel", color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}

@Composable
private fun RecordPaymentDialog(
  customer: Customer,
  currencyFormat: NumberFormat,
  onDismiss: () -> Unit,
  onSubmit: (Double, (Boolean, String?) -> Unit) -> Unit
) {
  var paymentText by remember { mutableStateOf(customer.totalDue.toString()) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
    title = { Text("Record Due Payment", fontWeight = FontWeight.Bold, color = TextPrimaryDark) },
    text = {
      Column {
        Text("Farmer: ${customer.name}", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
        Text("Total Due: ${currencyFormat.format(customer.totalDue)}", color = GoldAmber, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = paymentText,
          onValueChange = { paymentText = it },
          label = { Text("Payment Received (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          modifier = Modifier.fillMaxWidth()
        )
        errorMsg?.let { err ->
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = err, color = SoftRed, style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = paymentText.toDoubleOrNull() ?: 0.0
          if (amt <= 0) {
            errorMsg = "Please enter valid payment amount"
            return@Button
          }
          isSaving = true
          onSubmit(amt) { success, msg ->
            isSaving = false
            if (!success) errorMsg = msg
          }
        },
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
      ) {
        if (isSaving) CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        else Text("Record Payment")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSaving) {
        Text("Cancel", color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}
