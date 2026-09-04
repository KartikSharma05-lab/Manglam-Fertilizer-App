package com.manglamfertilizer.app.ui.accounts

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
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

/**
 * Dialog for adding a new Daily Account Entry (`MANUAL` source).
 * Allows shop owners/operators to record quick daily accounting ledger transactions
 * without confusing or colliding with the formal Billing POS module.
 */
@Composable
fun AddDailyAccountEntryDialog(
  customers: List<Customer> = emptyList(),
  products: List<Product> = emptyList(),
  onSaveEntry: (
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
  ) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val numberFormatter = remember { DecimalFormat("#,##,###.##") }

  var farmerName by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var village by remember { mutableStateOf("") }
  var selectedCustomerId by remember { mutableStateOf<String?>(null) }
  var showCustomerSuggestions by remember { mutableStateOf(false) }

  var productDescription by remember { mutableStateOf("") }
  var quantityStr by remember { mutableStateOf("1") }
  var selectedUnit by remember { mutableStateOf(ProductUnit.BAG) }
  var unitDropdownExpanded by remember { mutableStateOf(false) }

  var totalAmountStr by remember { mutableStateOf("") }
  var cashAmountStr by remember { mutableStateOf("") }
  var onlineAmountStr by remember { mutableStateOf("") }
  var selectedPaymentMode by remember { mutableStateOf(PaymentMode.CASH) }

  var entryTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
  var dueDateMillis by remember { mutableStateOf<Long?>(null) }

  var validationError by remember { mutableStateOf<String?>(null) }

  // Autocomplete matching customers
  val filteredCustomers = remember(farmerName, customers) {
    if (farmerName.trim().length >= 2) {
      customers.filter {
        it.name.contains(farmerName, ignoreCase = true) ||
            it.phoneNumber.contains(farmerName) ||
            it.village.contains(farmerName, ignoreCase = true)
      }.take(5)
    } else emptyList()
  }

  // Auto-calculated Remaining Due
  val parsedTotal = totalAmountStr.toDoubleOrNull() ?: 0.0
  val parsedCash = cashAmountStr.toDoubleOrNull() ?: 0.0
  val parsedOnline = onlineAmountStr.toDoubleOrNull() ?: 0.0
  val calculatedPaid = (parsedCash + parsedOnline).coerceAtMost(parsedTotal)
  val calculatedDue = (parsedTotal - calculatedPaid).coerceAtLeast(0.0)

  // Auto-fill cash or online when total is entered in CASH or UPI modes
  fun updateModeAndAmounts(mode: PaymentMode) {
    selectedPaymentMode = mode
    when (mode) {
      PaymentMode.CASH -> {
        cashAmountStr = if (parsedTotal > 0) parsedTotal.toInt().toString() else ""
        onlineAmountStr = ""
      }
      PaymentMode.UPI, PaymentMode.CARD -> {
        onlineAmountStr = if (parsedTotal > 0) parsedTotal.toInt().toString() else ""
        cashAmountStr = ""
      }
      PaymentMode.CREDIT -> {
        cashAmountStr = ""
        onlineAmountStr = ""
      }
      PaymentMode.SPLIT, PaymentMode.OTHER -> {
        // keep current split values
      }
      else -> {}
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 10.dp)
        .testTag("add_daily_account_entry_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Emerald400.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(18.dp)
              )
            }
            Column {
              Text(
                text = "New Daily Ledger Entry",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 16.5.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryDark
              )
              Text(
                text = "Manual Daily Accounts Entry",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = Emerald400
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp).testTag("close_add_entry_dialog_btn")
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondaryDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)

        // 1. Farmer / Customer Info
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "FARMER / CUSTOMER DETAILS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextSecondaryDark
          )

          Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
              value = farmerName,
              onValueChange = {
                farmerName = it
                selectedCustomerId = null
                validationError = null
                showCustomerSuggestions = it.trim().length >= 2
              },
              label = { Text("Farmer Name *") },
              placeholder = { Text("e.g. Ramesh Kumar") },
              leadingIcon = {
                Icon(
                  Icons.Default.Person,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(18.dp)
                )
              },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("entry_farmer_name_input")
            )

            // Autocomplete suggestions popup
            if (showCustomerSuggestions && filteredCustomers.isNotEmpty()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 58.dp)
              ) {
                Column(modifier = Modifier.padding(4.dp)) {
                  filteredCustomers.forEach { cust ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                          farmerName = cust.name
                          phone = cust.phoneNumber
                          village = cust.village
                          selectedCustomerId = cust.id
                          showCustomerSuggestions = false
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column {
                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextPrimaryDark)
                        Text(
                          listOf(cust.village, cust.phoneNumber).filter { it.isNotBlank() }.joinToString(" • "),
                          fontSize = 10.5.sp,
                          color = TextMutedDark
                        )
                      }
                      if (cust.totalDue > 0) {
                        Text(
                          "Due: ₹${numberFormatter.format(cust.totalDue)}",
                          fontSize = 11.sp,
                          fontWeight = FontWeight.Bold,
                          color = GoldAmber
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text("Mobile No.") },
              placeholder = { Text("9876543210") },
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
              modifier = Modifier
                .weight(1f)
                .testTag("entry_phone_input")
            )

            OutlinedTextField(
              value = village,
              onValueChange = { village = it },
              label = { Text("Village / City") },
              placeholder = { Text("e.g. Rampur") },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("entry_village_input")
            )
          }
        }

        // 2. Product / Items Particulars
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "PARTICULARS / PRODUCT DESCRIPTION",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextSecondaryDark
          )

          OutlinedTextField(
            value = productDescription,
            onValueChange = { productDescription = it; validationError = null },
            label = { Text("Product / Item Details *") },
            placeholder = { Text("e.g. DAP 50kg, Urea, Zinc Sulphate") },
            leadingIcon = {
              Icon(
                Icons.Default.ShoppingCart,
                contentDescription = null,
                tint = InfoSky,
                modifier = Modifier.size(18.dp)
              )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("entry_product_desc_input")
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = quantityStr,
              onValueChange = { quantityStr = it },
              label = { Text("Quantity") },
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
              modifier = Modifier
                .weight(1f)
                .testTag("entry_quantity_input")
            )

            // Unit Selector
            Box(modifier = Modifier.weight(1f)) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .clickable { unitDropdownExpanded = true }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text("Unit", fontSize = 10.sp, color = TextMutedDark)
                    Text(selectedUnit.name, color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  }
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark)
                }
              }

              DropdownMenu(
                expanded = unitDropdownExpanded,
                onDismissRequest = { unitDropdownExpanded = false },
                modifier = Modifier.background(DarkCard)
              ) {
                ProductUnit.entries.forEach { unit ->
                  DropdownMenuItem(
                    text = { Text(unit.name, color = TextPrimaryDark) },
                    onClick = {
                      selectedUnit = unit
                      unitDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }
        }

        // 3. Financials & Payment Breakdown
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "PAYMENT & AMOUNTS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextSecondaryDark
          )

          // Total Amount
          OutlinedTextField(
            value = totalAmountStr,
            onValueChange = {
              totalAmountStr = it
              validationError = null
              val total = it.toDoubleOrNull() ?: 0.0
              if (selectedPaymentMode == PaymentMode.CASH) {
                cashAmountStr = if (total > 0) total.toInt().toString() else ""
              } else if (selectedPaymentMode == PaymentMode.UPI || selectedPaymentMode == PaymentMode.CARD) {
                onlineAmountStr = if (total > 0) total.toInt().toString() else ""
              }
            },
            label = { Text("Total Bill / Sale Amount (₹) *") },
            placeholder = { Text("e.g. 2400") },
            leadingIcon = {
              Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(18.dp)
              )
            },
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
            modifier = Modifier
              .fillMaxWidth()
              .testTag("entry_total_amount_input")
          )

          // Payment Mode Selector Pills
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            listOf(PaymentMode.CASH, PaymentMode.UPI, PaymentMode.CREDIT, PaymentMode.SPLIT).forEach { mode ->
              val isSelected = selectedPaymentMode == mode
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Emerald400 else DarkSurfaceElevated,
                border = BorderStroke(1.dp, if (isSelected) Emerald400 else DarkBorder),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { updateModeAndAmounts(mode) }
              ) {
                Box(
                  modifier = Modifier.padding(vertical = 7.dp),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = when (mode) {
                      PaymentMode.CASH -> "Cash"
                      PaymentMode.UPI -> "Online"
                      PaymentMode.CREDIT -> "Udhar"
                      PaymentMode.SPLIT -> "Split"
                      else -> mode.name
                    },
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) DarkBg else TextSecondaryDark
                  )
                }
              }
            }
          }

          // Breakdown: Cash Paid & Online Paid
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = cashAmountStr,
              onValueChange = { cashAmountStr = it },
              label = { Text("Cash Paid (₹)") },
              placeholder = { Text("0") },
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
              modifier = Modifier
                .weight(1f)
                .testTag("entry_cash_amount_input")
            )

            OutlinedTextField(
              value = onlineAmountStr,
              onValueChange = { onlineAmountStr = it },
              label = { Text("Online/UPI (₹)") },
              placeholder = { Text("0") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = InfoSky,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("entry_online_amount_input")
            )
          }

          // Live Due Calculation Card
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (calculatedDue > 0) GoldAmber.copy(alpha = 0.12f) else Emerald400.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, if (calculatedDue > 0) GoldAmber.copy(alpha = 0.4f) else Emerald400.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (calculatedDue > 0) "Remaining Due (Udhar):" else "Fully Paid / Settled:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (calculatedDue > 0) GoldAmber else Emerald400
              )
              Text(
                text = "₹${numberFormatter.format(calculatedDue)}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (calculatedDue > 0) GoldAmber else Emerald400
              )
            }
          }
        }

        // 4. Date & Time Selection
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              val cal = Calendar.getInstance().apply { timeInMillis = entryTimestamp }
              DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                  val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                  }
                  entryTimestamp = newCal.timeInMillis
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
              ).show()
            }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text("Entry Date", fontSize = 10.sp, color = TextMutedDark)
                Text(
                  text = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entryTimestamp)),
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = TextPrimaryDark
                )
              }
            }
            Text("Change", fontSize = 11.sp, color = Emerald400, fontWeight = FontWeight.Bold)
          }
        }

        if (validationError != null) {
          Text(
            text = validationError!!,
            color = SoftRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Actions: Cancel & Save Entry
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
          ) {
            Text("Cancel", color = TextSecondaryDark)
          }

          Button(
            onClick = {
              val cleanName = farmerName.trim()
              if (cleanName.isBlank()) {
                validationError = "Please enter farmer name"
                return@Button
              }
              val cleanDesc = productDescription.trim()
              if (cleanDesc.isBlank()) {
                validationError = "Please enter product description / particulars"
                return@Button
              }
              if (parsedTotal <= 0.0) {
                validationError = "Please enter a valid total amount"
                return@Button
              }

              val qty = quantityStr.toDoubleOrNull() ?: 1.0
              val item = InvoiceItem(
                productId = "manual_${System.currentTimeMillis()}",
                productName = cleanDesc,
                batchNumber = "",
                quantity = qty,
                unit = selectedUnit,
                unitPrice = parsedTotal / qty.coerceAtLeast(1.0),
                totalPrice = parsedTotal
              )

              onSaveEntry(
                selectedCustomerId,
                cleanName,
                phone.trim(),
                village.trim(),
                listOf(item),
                parsedTotal,
                calculatedPaid,
                selectedPaymentMode,
                entryTimestamp,
                dueDateMillis
              )
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = Emerald500,
              contentColor = DarkBg
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1.5f)
              .height(44.dp)
              .testTag("save_daily_account_entry_btn")
          ) {
            Text("Save Entry", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
          }
        }
      }
    }
  }
}
