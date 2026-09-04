package com.manglamfertilizer.app.ui.accounts

import android.app.DatePickerDialog
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.accounting.AccountingPeriodMode
import com.manglamfertilizer.app.data.accounting.DailyAccountingLifecycleManager
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.DailyAccountsColumnConfig
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.util.AppConstants
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.common.ManglamFloatingActionButton
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private fun ProductUnit.toDisplayLabel(): String = when (this) {
  ProductUnit.BAG -> "Bags"
  ProductUnit.KG -> "Kg"
  ProductUnit.LITER -> "L"
  ProductUnit.PACKET -> "Pkt"
  ProductUnit.BOTTLE -> "Btl"
  ProductUnit.PIECE -> "Pcs"
  ProductUnit.GRAM -> "g"
}

@Composable
fun DailyAccountsScreen(
  invoices: List<Invoice>,
  columns: List<DailyAccountsColumnConfig> = DailyAccountsColumnConfig.DEFAULT_COLUMNS,
  customers: List<Customer> = emptyList(),
  products: List<Product> = emptyList(),
  currentUser: User? = null,
  onSaveColumns: (List<DailyAccountsColumnConfig>) -> Unit = {},
  onAddCustomField: (String, String) -> Unit = { _, _ -> },
  onRenameField: (String, String) -> Unit = { _, _ -> },
  onDeleteField: (String) -> Unit = {},
  onResetDefaults: () -> Unit = {},
  onAddDailyAccountEntry: ((
    customerId: String?,
    customerName: String,
    customerPhone: String,
    customerVillage: String,
    items: List<InvoiceItem>,
    totalAmount: Double,
    amountPaid: Double,
    paymentMode: PaymentMode,
    timestamp: Long,
    dueDate: Long?
  ) -> Unit)? = null,
  onDeleteInvoice: ((String) -> Unit)? = null,
  onBack: () -> Unit,
  onNavigateToBilling: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val strings = LocalStrings.current

  BackHandler(onBack = onBack)

  var searchQuery by remember { mutableStateOf("") }
  var selectedInvoiceForDetail by remember { mutableStateOf<Invoice?>(null) }
  var showManageColumnsDialog by remember { mutableStateOf(false) }
  var showAddEntryDialog by remember { mutableStateOf(false) }
  var deletingInvoiceId by remember { mutableStateOf<Invoice?>(null) }

  // Period / Date Selection State
  var selectedPeriodMode by remember { mutableStateOf(AccountingPeriodMode.TODAY_ACTIVE) }
  var customStartDateMillis by remember { mutableStateOf<Long?>(null) }
  var customEndDateMillis by remember { mutableStateOf<Long?>(null) }
  var showDatePickerDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }

  val numberFormatter = remember { DecimalFormat("#,##,###.##") }

  // Active sorted visible columns
  val visibleColumns = remember(columns) {
    val active = columns.filter { it.isVisible }.sortedBy { it.order }
    if (active.isEmpty()) DailyAccountsColumnConfig.DEFAULT_COLUMNS.filter { it.isVisible } else active
  }

  // Authoritative Accounting Summary derived from DailyAccountingLifecycleManager
  val accountingSummary = remember(
    invoices,
    selectedPeriodMode,
    customStartDateMillis,
    customEndDateMillis,
    searchQuery
  ) {
    DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = invoices,
      mode = selectedPeriodMode,
      customStartMillis = customStartDateMillis,
      customEndMillis = customEndDateMillis,
      searchQuery = searchQuery,
      timeZone = TimeZone.getDefault()
    )
  }

  val displayedInvoices = accountingSummary.invoices
  val totalSales = accountingSummary.totalSales
  val totalCash = accountingSummary.totalCash
  val totalOnline = accountingSummary.totalOnline
  val totalDue = accountingSummary.totalDue

  // Action to share daily accounts report via Intent
  val onShareDailySummary = {
    val summaryBuilder = StringBuilder()
    summaryBuilder.append("📊 *${AppConstants.OFFICIAL_SHOP_NAME}*\n")
    summaryBuilder.append("📑 *DAILY ACCOUNTS LEDGER - ${accountingSummary.periodLabel}*\n")
    summaryBuilder.append("─────────────────────\n")
    summaryBuilder.append("💰 *Total Sales:* ₹${numberFormatter.format(totalSales)}\n")
    summaryBuilder.append("💵 *Cash Collected:* ₹${numberFormatter.format(totalCash)}\n")
    summaryBuilder.append("📱 *UPI / Online:* ₹${numberFormatter.format(totalOnline)}\n")
    summaryBuilder.append("⏳ *Total Due (Udhar):* ₹${numberFormatter.format(totalDue)}\n")
    summaryBuilder.append("🧾 *Total Bills:* ${displayedInvoices.size}\n")
    summaryBuilder.append("─────────────────────\n\n")

    if (displayedInvoices.isNotEmpty()) {
      summaryBuilder.append("📋 *ACCOUNTING SHEET ENTRIES:*\n\n")
      displayedInvoices.forEachIndexed { index, inv ->
        val timeStr = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault()).format(Date(inv.timestamp))
        val cashAmt = if (inv.paymentMode == PaymentMode.CASH) inv.amountPaid else 0.0
        val onlineAmt = if (inv.paymentMode != PaymentMode.CASH && inv.paymentMode != PaymentMode.CREDIT) inv.amountPaid else 0.0
        val dueAmt = inv.remainingDue.coerceAtLeast(0.0)

        val productsStr = inv.items.joinToString(", ") { "${it.productName} (${it.quantity})" }
        summaryBuilder.append("${index + 1}. *${inv.customerName}* (${inv.invoiceNumber}) - $timeStr\n")
        summaryBuilder.append("   Products: $productsStr\n")
        summaryBuilder.append("   Total: ₹${inv.grandTotal.toInt()} | Cash: ₹${cashAmt.toInt()} | Online: ₹${onlineAmt.toInt()} | Due: ₹${dueAmt.toInt()}\n\n")
      }
    } else {
      summaryBuilder.append("No accounting records found for this period.\n")
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Daily Accounts Ledger - ${AppConstants.OFFICIAL_SHOP_NAME}")
      putExtra(Intent.EXTRA_TEXT, summaryBuilder.toString())
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share Daily Accounts"))
  }

  val horizontalScrollState = rememberScrollState()
  val listState = rememberLazyListState()

  Scaffold(
    contentWindowInsets = WindowInsets.navigationBars,
    floatingActionButton = {
      if (onAddDailyAccountEntry != null) {
        ManglamFloatingActionButton(
          onClick = { showAddEntryDialog = true },
          contentDescription = "Add Entry",
          testTag = "daily_accounts_fab_add_entry",
          modifier = Modifier.padding(bottom = 12.dp, end = 8.dp)
        )
      }
    },
    containerColor = DarkBg,
    modifier = modifier
      .fillMaxSize()
      .testTag("daily_accounts_screen")
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(bottom = paddingValues.calculateBottomPadding())
        .imePadding()
    ) {
      // 1. Header (Daily Accounts / Shop Name / Actions)
      DailyAccountsTopBar(
        title = strings.dailyAccountsTitle,
        subtitle = AppConstants.OFFICIAL_SHOP_NAME,
        onBack = onBack,
        onAddEntry = { showAddEntryDialog = true },
        onManageColumns = { showManageColumnsDialog = true },
        onExport = { showExportDialog = true },
        onShare = onShareDailySummary
      )

      // 2. Standardized Search Bar directly below header
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        com.manglamfertilizer.app.ui.common.ManglamSearchBar(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = "Search farmer, product, bill no...",
          testTag = "daily_accounts_search_input",
          modifier = Modifier.fillMaxWidth()
        )
      }

      // 3. Compact Period Filter Pills (Today Active, Yesterday, This Month, Custom Date/Range, All)
      CompactPeriodFilterBar(
        currentMode = selectedPeriodMode,
        periodLabel = accountingSummary.periodLabel,
        onSelectMode = { mode ->
          selectedPeriodMode = mode
          if (mode != AccountingPeriodMode.CUSTOM_DATE && mode != AccountingPeriodMode.CUSTOM_RANGE) {
            customStartDateMillis = null
            customEndDateMillis = null
          }
        },
        onOpenDateSelector = {
          showDatePickerDialog = true
        }
      )

      // 4. Excel-Sheet Accounting Table Container
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
        ) {
          // Table Header
          AccountingTableHeader(columns = visibleColumns)

          HorizontalDivider(color = Emerald400.copy(alpha = 0.4f), thickness = 1.dp)

          // Table Body
          if (displayedInvoices.isEmpty()) {
            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Receipt,
                  contentDescription = null,
                  tint = TextMutedDark,
                  modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = if (searchQuery.isNotBlank()) "No records matching \"$searchQuery\"" else "No accounting records found",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (selectedPeriodMode == AccountingPeriodMode.TODAY_ACTIVE)
                    "Today's active ledger rolls over after 12:00 PM noon. Use '+ Add Entry' or switch period."
                  else
                    "Try changing the date filter or search query.",
                  fontSize = 11.sp,
                  color = TextMutedDark,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(horizontal = 16.dp)
                )
              }
            }
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier
                .weight(1f)
                .testTag("daily_accounts_table_list"),
              contentPadding = PaddingValues(bottom = 72.dp)
            ) {
              itemsIndexed(
                items = displayedInvoices,
                key = { _, inv -> inv.id }
              ) { index, invoice ->
                AccountingTableRow(
                  sNo = index + 1,
                  invoice = invoice,
                  columns = visibleColumns,
                  isEven = index % 2 == 0,
                  onClick = { selectedInvoiceForDetail = invoice }
                )
                HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.6.dp)
              }
            }
          }

          HorizontalDivider(color = Emerald400.copy(alpha = 0.6f), thickness = 1.2.dp)

          // Total Summary Row at table bottom
          AccountingTableTotalRow(
            totalBillsCount = displayedInvoices.size,
            totalSales = totalSales,
            totalCash = totalCash,
            totalOnline = totalOnline,
            totalDue = totalDue,
            columns = visibleColumns
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
    }
  }

  // Manage Daily Accounts Columns Dialog
  if (showManageColumnsDialog) {
    ManageDailyAccountsColumnsDialog(
      columns = columns,
      onSaveColumns = onSaveColumns,
      onAddCustomField = onAddCustomField,
      onRenameField = onRenameField,
      onDeleteField = onDeleteField,
      onResetDefaults = onResetDefaults,
      onDismiss = { showManageColumnsDialog = false }
    )
  }

  // Add Daily Account Entry Dialog
  if (showAddEntryDialog) {
    AddDailyAccountEntryDialog(
      customers = customers,
      products = products,
      onSaveEntry = { custId, custName, custPhone, custVillage, items, total, paid, mode, timestamp, dueDate ->
        onAddDailyAccountEntry?.invoke(
          custId,
          custName,
          custPhone,
          custVillage,
          items,
          total,
          paid,
          mode,
          timestamp,
          dueDate
        )
      },
      onDismiss = { showAddEntryDialog = false }
    )
  }

  // Invoice Detail Dialog
  selectedInvoiceForDetail?.let { inv ->
    DailyInvoiceDetailDialog(
      invoice = inv,
      onDismiss = { selectedInvoiceForDetail = null },
      onDelete = {
        deletingInvoiceId = inv
        selectedInvoiceForDetail = null
      }
    )
  }

  // Delete Invoice Confirmation Dialog
  deletingInvoiceId?.let { inv ->
    AlertDialog(
      onDismissRequest = { deletingInvoiceId = null },
      title = {
        Text("Delete Entry / Invoice #${inv.invoiceNumber}?", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
      },
      text = {
        Text(
          "Are you sure you want to delete this accounting entry for ${inv.customerName}? This will permanently remove the record from Daily Accounts.",
          color = TextSecondaryDark,
          fontSize = 12.5.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteInvoice?.invoke(inv.id)
            deletingInvoiceId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White)
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingInvoiceId = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // Date / Range Selection Dialog
  if (showDatePickerDialog) {
    AccountingDatePickerDialog(
      onDismiss = { showDatePickerDialog = false },
      onDateSelected = { dateMillis ->
        customStartDateMillis = dateMillis
        customEndDateMillis = null
        selectedPeriodMode = AccountingPeriodMode.CUSTOM_DATE
        showDatePickerDialog = false
      },
      onRangeSelected = { startMillis, endMillis ->
        customStartDateMillis = startMillis
        customEndDateMillis = endMillis
        selectedPeriodMode = AccountingPeriodMode.CUSTOM_RANGE
        showDatePickerDialog = false
      },
      onMonthSelected = { monthReferenceMillis ->
        customStartDateMillis = monthReferenceMillis
        customEndDateMillis = null
        selectedPeriodMode = AccountingPeriodMode.THIS_MONTH
        showDatePickerDialog = false
      }
    )
  }

  // Custom Data Export Dialog
  if (showExportDialog) {
    CustomExportDialog(
      invoices = invoices,
      initialMode = selectedPeriodMode,
      initialStartMillis = customStartDateMillis,
      initialEndMillis = customEndDateMillis,
      onDismiss = { showExportDialog = false }
    )
  }
}

/**
 * Top header for Daily Accounts Screen with prominent action buttons.
 */
@Composable
private fun DailyAccountsTopBar(
  title: String,
  subtitle: String,
  onBack: () -> Unit,
  onAddEntry: () -> Unit,
  onManageColumns: () -> Unit,
  onExport: () -> Unit,
  onShare: () -> Unit
) {
  Surface(
    color = DarkSurface,
    tonalElevation = 3.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier
            .size(38.dp)
            .testTag("daily_accounts_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimaryDark
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AccountBalanceWallet,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Daily Accounts",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              ),
              color = TextPrimaryDark,
              maxLines = 1
            )
          }
          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.5.sp
            ),
            color = TextSecondaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
      ) {
        // + Add Entry button
        Surface(
          shape = CircleShape,
          color = Emerald500.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
          modifier = Modifier
            .clip(CircleShape)
            .clickable { onAddEntry() }
            .testTag("daily_accounts_add_entry_header_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Add Entry",
              tint = Emerald400,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Entry",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Emerald400
            )
          }
        }

        // Manage Columns button
        Surface(
          shape = CircleShape,
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .clip(CircleShape)
            .clickable { onManageColumns() }
            .testTag("daily_accounts_columns_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.ViewColumn,
              contentDescription = "Columns",
              tint = Emerald400,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "Columns",
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextPrimaryDark
            )
          }
        }

        // Export button
        Surface(
          shape = CircleShape,
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .clip(CircleShape)
            .clickable { onExport() }
            .testTag("daily_accounts_export_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = "Export",
              tint = Emerald400,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        // Share button
        Surface(
          shape = CircleShape,
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .clip(CircleShape)
            .clickable { onShare() }
            .testTag("daily_accounts_share_button")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share",
              tint = Emerald400,
              modifier = Modifier.size(14.dp)
            )
          }
        }
      }
    }
    }
  }
}

/**
 * Compact Period Filter Bar for switching between Today, Yesterday, Month, and Date Range.
 */
@Composable
private fun CompactPeriodFilterBar(
  currentMode: AccountingPeriodMode,
  periodLabel: String,
  onSelectMode: (AccountingPeriodMode) -> Unit,
  onOpenDateSelector: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 10.dp, vertical = 2.dp)
  ) {
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      item {
        FilterPill(
          label = "Today (Active)",
          isSelected = currentMode == AccountingPeriodMode.TODAY_ACTIVE,
          icon = Icons.Default.Today,
          onClick = { onSelectMode(AccountingPeriodMode.TODAY_ACTIVE) }
        )
      }
      item {
        FilterPill(
          label = "Yesterday",
          isSelected = currentMode == AccountingPeriodMode.YESTERDAY,
          icon = Icons.Default.History,
          onClick = { onSelectMode(AccountingPeriodMode.YESTERDAY) }
        )
      }
      item {
        FilterPill(
          label = "This Month",
          isSelected = currentMode == AccountingPeriodMode.THIS_MONTH,
          icon = Icons.Default.CalendarMonth,
          onClick = { onSelectMode(AccountingPeriodMode.THIS_MONTH) }
        )
      }
      item {
        FilterPill(
          label = if (currentMode == AccountingPeriodMode.CUSTOM_DATE || currentMode == AccountingPeriodMode.CUSTOM_RANGE) "Selected Date 📅" else "Select Date 📅",
          isSelected = currentMode == AccountingPeriodMode.CUSTOM_DATE || currentMode == AccountingPeriodMode.CUSTOM_RANGE,
          icon = Icons.Default.CalendarToday,
          onClick = onOpenDateSelector
        )
      }
      item {
        FilterPill(
          label = "All History",
          isSelected = currentMode == AccountingPeriodMode.ALL_RECORDS,
          icon = Icons.Default.FilterAlt,
          onClick = { onSelectMode(AccountingPeriodMode.ALL_RECORDS) }
        )
      }
    }

    // Period Info Badge
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp, bottom = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(Emerald400)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
          text = periodLabel,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = Emerald400,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (currentMode != AccountingPeriodMode.TODAY_ACTIVE) {
        Text(
          text = "Reset to Today",
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Bold,
          color = TextSecondaryDark,
          modifier = Modifier
            .clickable { onSelectMode(AccountingPeriodMode.TODAY_ACTIVE) }
            .padding(horizontal = 4.dp, vertical = 2.dp)
        )
      }
    }
  }
}

@Composable
private fun FilterPill(
  label: String,
  isSelected: Boolean,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit
) {
  val bgColor = if (isSelected) Emerald400 else DarkCard
  val contentColor = if (isSelected) Color(0xFF02231B) else TextSecondaryDark
  val borderColor = if (isSelected) Emerald400 else DarkBorder

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable { onClick() }
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = label,
        fontSize = 11.5.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = contentColor
      )
    }
  }
}

/**
 * Table Header with horizontal text and column alignments.
 */
@Composable
private fun AccountingTableHeader(columns: List<DailyAccountsColumnConfig>) {
  Row(
    modifier = Modifier
      .background(DarkSurfaceElevated)
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    columns.forEachIndexed { index, col ->
      val textAlign = when (col.dataType) {
        "Currency" -> TextAlign.End
        "Number" -> TextAlign.Center
        "Date" -> TextAlign.Center
        else -> if (col.id == "sNo") TextAlign.Center else TextAlign.Start
      }
      HeaderCell(text = col.title, width = col.defaultWidthDp.dp, textAlign = textAlign)
      if (index < columns.size - 1) {
        VerticalDivider(color = DarkBorder, thickness = 1.dp, modifier = Modifier.height(18.dp))
      }
    }
  }
}

@Composable
private fun HeaderCell(
  text: String,
  width: Dp,
  textAlign: TextAlign
) {
  Box(
    modifier = Modifier
      .width(width)
      .padding(horizontal = 6.dp),
    contentAlignment = when (textAlign) {
      TextAlign.Center -> Alignment.Center
      TextAlign.End -> Alignment.CenterEnd
      else -> Alignment.CenterStart
    }
  ) {
    Text(
      text = text,
      fontSize = 11.5.sp,
      fontWeight = FontWeight.Bold,
      color = Emerald400,
      textAlign = textAlign,
      maxLines = 1
    )
  }
}

/**
 * Excel-Style Table Row representing a transaction / daily entry.
 */
@Composable
private fun AccountingTableRow(
  sNo: Int,
  invoice: Invoice,
  columns: List<DailyAccountsColumnConfig>,
  isEven: Boolean,
  onClick: () -> Unit
) {
  val numberFormatter = remember { DecimalFormat("#,##,###") }
  val rowBg = if (isEven) DarkCard else DarkSurface

  val cashAmount = remember(invoice) {
    if (invoice.paymentMode == PaymentMode.CASH) invoice.amountPaid else 0.0
  }
  val onlineAmount = remember(invoice) {
    if (invoice.paymentMode != PaymentMode.CASH && invoice.paymentMode != PaymentMode.CREDIT) invoice.amountPaid else 0.0
  }
  val dueAmount = remember(invoice) {
    invoice.remainingDue.coerceAtLeast(0.0)
  }

  Row(
    modifier = Modifier
      .background(rowBg)
      .clickable { onClick() }
      .padding(vertical = 7.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    columns.forEachIndexed { index, col ->
      when (col.id) {
        "sNo" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = sNo.toString(),
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextSecondaryDark,
              textAlign = TextAlign.Center
            )
          }
        }
        "farmerName" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Column {
              Text(
                text = invoice.customerName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (invoice.customerVillage.isNotBlank() || invoice.invoiceNumber.isNotBlank()) {
                Text(
                  text = if (invoice.customerVillage.isNotBlank()) "${invoice.customerVillage} • #${invoice.invoiceNumber}" else "#${invoice.invoiceNumber}",
                  fontSize = 9.5.sp,
                  color = TextMutedDark,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
        "product" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            if (invoice.items.isEmpty()) {
              Text(
                text = "-",
                fontSize = 11.5.sp,
                color = TextMutedDark
              )
            } else {
              Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                invoice.items.forEach { item ->
                  Text(
                    text = item.productName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimaryDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }
            }
          }
        }
        "qty" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            if (invoice.items.isEmpty()) {
              Text(
                text = "-",
                fontSize = 11.5.sp,
                color = TextMutedDark
              )
            } else {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
              ) {
                invoice.items.forEach { item ->
                  val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                  Text(
                    text = "$qtyStr ${item.unit.toDisplayLabel()}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondaryDark,
                    maxLines = 1
                  )
                }
              }
            }
          }
        }
        "total" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(invoice.grandTotal)}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimaryDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "cash" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(cashAmount)}",
              fontSize = 11.5.sp,
              fontWeight = if (cashAmount > 0) FontWeight.Bold else FontWeight.Normal,
              color = if (cashAmount > 0) Emerald400 else TextMutedDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "online" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(onlineAmount)}",
              fontSize = 11.5.sp,
              fontWeight = if (onlineAmount > 0) FontWeight.Bold else FontWeight.Normal,
              color = if (onlineAmount > 0) InfoSky else TextMutedDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "due" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(dueAmount)}",
              fontSize = 11.5.sp,
              fontWeight = if (dueAmount > 0) FontWeight.Bold else FontWeight.Normal,
              color = if (dueAmount > 0) GoldAmber else TextMutedDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "phone" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text(
              text = invoice.customerPhone.ifBlank { "-" },
              fontSize = 11.sp,
              color = TextSecondaryDark,
              maxLines = 1
            )
          }
        }
        "village" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text(
              text = invoice.customerVillage.ifBlank { "-" },
              fontSize = 11.sp,
              color = TextSecondaryDark,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
        "invoiceNumber" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text(
              text = invoice.invoiceNumber.ifBlank { "-" },
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium,
              color = Emerald400,
              maxLines = 1
            )
          }
        }
        "paymentMode" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = invoice.paymentMode.name,
              fontSize = 10.5.sp,
              fontWeight = FontWeight.SemiBold,
              color = when (invoice.paymentMode) {
                PaymentMode.CASH -> Emerald400
                PaymentMode.UPI, PaymentMode.CARD, PaymentMode.SPLIT, PaymentMode.CHEQUE -> InfoSky
                PaymentMode.CREDIT -> GoldAmber
                else -> TextSecondaryDark
              },
              maxLines = 1
            )
          }
        }
        "date" -> {
          val dateStr = remember(invoice.timestamp) {
            SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))
          }
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = dateStr,
              fontSize = 10.5.sp,
              color = TextSecondaryDark,
              maxLines = 1
            )
          }
        }
        "discount" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = if (invoice.discount > 0) "₹${numberFormatter.format(invoice.discount)}" else "-",
              fontSize = 11.sp,
              color = if (invoice.discount > 0) Emerald400 else TextMutedDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "gst" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = if (invoice.gstRate > 0) "${invoice.gstRate.toInt()}%" else "0%",
              fontSize = 11.sp,
              color = TextSecondaryDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        else -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = when (col.dataType) {
              "Currency", "Number" -> Alignment.CenterEnd
              else -> Alignment.CenterStart
            }
          ) {
            Text(
              text = "-",
              fontSize = 11.sp,
              color = TextMutedDark,
              maxLines = 1
            )
          }
        }
      }

      if (index < columns.size - 1) {
        VerticalDivider(color = DarkBorder.copy(alpha = 0.3f), thickness = 0.8.dp, modifier = Modifier.height(20.dp))
      }
    }
  }
}

/**
 * Sticky Total Row at the bottom of the Daily Accounts Table.
 */
@Composable
private fun AccountingTableTotalRow(
  totalBillsCount: Int,
  totalSales: Double,
  totalCash: Double,
  totalOnline: Double,
  totalDue: Double,
  columns: List<DailyAccountsColumnConfig>
) {
  val numberFormatter = remember { DecimalFormat("#,##,###") }

  Row(
    modifier = Modifier
      .background(Color(0xFF04271E))
      .padding(vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    columns.forEachIndexed { index, col ->
      when (col.id) {
        "sNo" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "#",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Emerald400
            )
          }
        }
        "farmerName" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "TOTAL",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Emerald400,
                letterSpacing = 0.6.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "($totalBillsCount)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondaryDark
              )
            }
          }
        }
        "total" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(totalSales)}",
              fontSize = 12.5.sp,
              fontWeight = FontWeight.ExtraBold,
              color = TextPrimaryDark,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "cash" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(totalCash)}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Emerald400,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "online" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(totalOnline)}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = InfoSky,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        "due" -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterEnd
          ) {
            Text(
              text = "₹${numberFormatter.format(totalDue)}",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = if (totalDue > 0) GoldAmber else Emerald400,
              textAlign = TextAlign.End,
              maxLines = 1
            )
          }
        }
        else -> {
          Box(
            modifier = Modifier
              .width(col.defaultWidthDp.dp)
              .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
          ) {}
        }
      }

      if (index < columns.size - 1) {
        VerticalDivider(color = Emerald400.copy(alpha = 0.4f), thickness = 1.dp, modifier = Modifier.height(20.dp))
      }
    }
  }
}

/**
 * Invoice / Entry Detail Dialog with complete transaction breakdown and actions.
 */
@Composable
private fun DailyInvoiceDetailDialog(
  invoice: Invoice,
  onDismiss: () -> Unit,
  onDelete: () -> Unit
) {
  val numberFormatter = remember { DecimalFormat("#,##,###.##") }
  val timeFormatter = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
  val formattedTime = remember(invoice.timestamp) { timeFormatter.format(Date(invoice.timestamp)) }

  val isManualEntry = invoice.invoiceNumber.startsWith("DA-") || invoice.invoiceNumber.startsWith("MAN-") || invoice.invoiceNumber.startsWith("ENTRY-")

  val cashAmount = remember(invoice) {
    if (invoice.paymentMode == PaymentMode.CASH) invoice.amountPaid else 0.0
  }
  val onlineAmount = remember(invoice) {
    if (invoice.paymentMode != PaymentMode.CASH && invoice.paymentMode != PaymentMode.CREDIT) invoice.amountPaid else 0.0
  }
  val dueAmount = remember(invoice) {
    invoice.remainingDue.coerceAtLeast(0.0)
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(6.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Entry #${invoice.invoiceNumber}",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimaryDark
                )
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = if (isManualEntry) Emerald400.copy(alpha = 0.15f) else InfoSky.copy(alpha = 0.15f),
                  border = BorderStroke(0.6.dp, if (isManualEntry) Emerald400.copy(alpha = 0.4f) else InfoSky.copy(alpha = 0.4f))
                ) {
                  Text(
                    text = if (isManualEntry) "MANUAL" else "BILLING",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isManualEntry) Emerald400 else InfoSky,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
              Text(
                text = formattedTime,
                fontSize = 10.5.sp,
                color = TextMutedDark
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextMutedDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Farmer Info Card
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkCard,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = invoice.customerName,
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimaryDark
            )
            if (invoice.customerPhone.isNotBlank() || invoice.customerVillage.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = listOf(invoice.customerPhone, invoice.customerVillage).filter { it.isNotBlank() }.joinToString(" • "),
                fontSize = 11.sp,
                color = TextSecondaryDark
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Items List
        Text(
          text = "Particulars / Items (${invoice.items.size})",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = Emerald400
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
        ) {
          itemsIndexed(invoice.items) { idx, item ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${idx + 1}. ${item.productName}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimaryDark,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
                Text(
                  text = "$qtyStr ${item.unit.toDisplayLabel()} @ ₹${item.unitPrice}",
                  fontSize = 10.5.sp,
                  color = TextMutedDark
                )
              }

              Text(
                text = "₹${numberFormatter.format(item.totalPrice)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
            }
            HorizontalDivider(color = DarkBorder.copy(alpha = 0.4f), thickness = 0.5.dp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // Payment Breakdown
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Total Amount", fontSize = 12.5.sp, color = TextSecondaryDark)
          Text(text = "₹${numberFormatter.format(invoice.grandTotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Cash Paid", fontSize = 12.sp, color = TextSecondaryDark)
          Text(text = "₹${numberFormatter.format(cashAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald400)
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Online / UPI Paid", fontSize = 12.sp, color = TextSecondaryDark)
          Text(text = "₹${numberFormatter.format(onlineAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InfoSky)
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = "Remaining Due (Udhar)", fontSize = 12.sp, color = TextSecondaryDark)
          Text(
            text = "₹${numberFormatter.format(dueAmount)}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (dueAmount > 0) GoldAmber else Emerald400
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Delete Action
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          OutlinedButton(
            onClick = onDelete,
            border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.6f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete Record", fontSize = 12.sp)
          }
        }
      }
    }
  }
}

/**
 * Compact Date Picker Dialog.
 */
@Composable
private fun AccountingDatePickerDialog(
  onDismiss: () -> Unit,
  onDateSelected: (Long) -> Unit,
  onRangeSelected: (Long, Long) -> Unit,
  onMonthSelected: (Long) -> Unit
) {
  val context = LocalContext.current
  val cal = Calendar.getInstance()

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(14.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Select Accounting Date",
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimaryDark
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextMutedDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Text(
          text = "Choose a specific day, month, or custom range to view historical daily records:",
          fontSize = 11.5.sp,
          color = TextSecondaryDark
        )

        // Option 1: Pick Specific Single Date
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkCard,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                  val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                  }
                  onDateSelected(selectedCal.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
              ).show()
            }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Pick Specific Date", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
              Text("View all bills on a particular calendar day", fontSize = 10.5.sp, color = TextMutedDark)
            }
          }
        }

        // Option 2: Quick Month Selection
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkCard,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onMonthSelected(System.currentTimeMillis()) }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = InfoSky, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Current Month (${SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
              Text("Monthly aggregate of Sales, Cash, Online & Due", fontSize = 10.5.sp, color = TextMutedDark)
            }
          }
        }

        // Option 3: Pick Date Range
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkCard,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              DatePickerDialog(
                context,
                { _, startYear, startMonth, startDay ->
                  val startCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, startYear)
                    set(Calendar.MONTH, startMonth)
                    set(Calendar.DAY_OF_MONTH, startDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                  }

                  DatePickerDialog(
                    context,
                    { _, endYear, endMonth, endDay ->
                      val endCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, endYear)
                        set(Calendar.MONTH, endMonth)
                        set(Calendar.DAY_OF_MONTH, endDay)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                      }
                      onRangeSelected(startCal.timeInMillis, endCal.timeInMillis)
                    },
                    startYear,
                    startMonth,
                    startDay
                  ).apply { setTitle("Select End Date") }.show()
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
              ).apply { setTitle("Select Start Date") }.show()
            }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.FilterAlt, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text("Pick Custom Date Range", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
              Text("Select from Date A to Date B (e.g. 01 Sep – 30 Sep)", fontSize = 10.5.sp, color = TextMutedDark)
            }
          }
        }
      }
    }
  }
}
