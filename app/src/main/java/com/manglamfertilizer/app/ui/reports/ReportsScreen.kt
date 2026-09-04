package com.manglamfertilizer.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import com.manglamfertilizer.app.ui.common.ManglamCard
import com.manglamfertilizer.app.ui.common.ManglamDesignTokens
import com.manglamfertilizer.app.ui.common.ManglamSearchBar
import com.manglamfertilizer.app.ui.common.ManglamSectionHeader
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.ui.accounts.CustomExportDialog
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ReportsScreen(
  invoices: List<Invoice>,
  products: List<Product>,
  customers: List<Customer>,
  onExportReport: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val reportsListState = rememberLazyListState()
  var showExportDialog by remember { mutableStateOf(false) }

  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }

  val totalRevenue = remember(invoices) { invoices.sumOf { it.grandTotal } }
  val totalInvoices = invoices.size
  val avgBill = remember(totalRevenue, totalInvoices) {
    if (totalInvoices > 0) totalRevenue / totalInvoices else 0.0
  }
  val totalStockVal = remember(products) {
    products.sumOf { it.stockQuantity * it.purchasePrice }
  }
  val totalDues = remember(customers) {
    customers.sumOf { it.totalDue }
  }

  var searchQuery by remember { mutableStateOf("") }

  val filteredInvoices = remember(searchQuery, invoices) {
    if (searchQuery.isBlank()) emptyList()
    else invoices.filter {
      it.customerName.contains(searchQuery, ignoreCase = true) ||
      it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
      it.items.any { item -> item.productName.contains(searchQuery, ignoreCase = true) }
    }
  }

  val filteredProducts = remember(searchQuery, products) {
    if (searchQuery.isBlank()) emptyList()
    else products.filter {
      it.name.contains(searchQuery, ignoreCase = true) ||
      it.company.contains(searchQuery, ignoreCase = true) ||
      it.barcode.contains(searchQuery, ignoreCase = true)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .imePadding()
  ) {
    // Header
    Surface(
      color = DarkSurface,
      modifier = Modifier.fillMaxWidth(),
      tonalElevation = 4.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Business Reports & Analytics",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = TextPrimaryDark
            )
            Text(
              text = "Real-time revenue, stock and credit performance",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryDark
            )
          }

          Button(
            onClick = { showExportDialog = true },
            shape = ManglamDesignTokens.ActionButtonRadius,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            modifier = Modifier
              .height(34.dp)
              .testTag("reports_export_button")
          ) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Standardized Global Search Bar
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      ManglamSearchBar(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = "Search reports, products, or invoices...",
        testTag = "reports_search_input",
        modifier = Modifier.fillMaxWidth()
      )
    }

    LazyColumn(
      state = reportsListState,
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 4.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // Search Results Section if searching
      if (searchQuery.isNotBlank()) {
        item {
          ManglamCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
              ManglamSectionHeader(
                title = "SEARCH RESULTS (${filteredInvoices.size} Invoices, ${filteredProducts.size} Products)",
                icon = Icons.Default.Assessment
              )
              Spacer(modifier = Modifier.height(10.dp))

              if (filteredInvoices.isEmpty() && filteredProducts.isEmpty()) {
                Text(
                  text = "No matching records found for \"$searchQuery\"",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMutedDark,
                  modifier = Modifier.padding(vertical = 8.dp)
                )
              } else {
                if (filteredInvoices.isNotEmpty()) {
                  Text(
                    text = "Matching Invoices:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Emerald400
                  )
                  filteredInvoices.take(5).forEach { inv ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = "#${inv.invoiceNumber} - ${inv.customerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimaryDark
                      )
                      Text(
                        text = currencyFormat.format(inv.grandTotal),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Emerald400
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                }

                if (filteredProducts.isNotEmpty()) {
                  Text(
                    text = "Matching Products:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = InfoSky
                  )
                  filteredProducts.take(5).forEach { prod ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = "${prod.name} (${prod.company})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimaryDark
                      )
                      Text(
                        text = "Stock: ${prod.stockQuantity}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = if (prod.stockQuantity <= prod.minStockAlert) SoftRed else TextSecondaryDark
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // 1. Overall Performance Card
      item {
        ManglamCard(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            ManglamSectionHeader(
              title = "ALL-TIME SALES SUMMARY",
              icon = Icons.Default.PieChart
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              StatBlock(
                title = "Total Revenue",
                value = currencyFormat.format(totalRevenue),
                color = Emerald400,
                modifier = Modifier.weight(1f)
              )
              StatBlock(
                title = "Invoices Generated",
                value = "$totalInvoices",
                color = InfoSky,
                modifier = Modifier.weight(1f)
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              StatBlock(
                title = "Average Bill Value",
                value = currencyFormat.format(avgBill),
                color = TextPrimaryDark,
                modifier = Modifier.weight(1f)
              )
              StatBlock(
                title = "Customer Dues",
                value = currencyFormat.format(totalDues),
                color = GoldAmber,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      // 2. Inventory Health & Valuation
      item {
        ManglamCard(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(16.dp)) {
            ManglamSectionHeader(
              title = "INVENTORY VALUATION & HEALTH",
              icon = Icons.Default.Assessment
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Total Stock Purchase Value", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
              Text(currencyFormat.format(totalStockVal), fontWeight = FontWeight.Bold, color = Emerald400, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Total Unique Products", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
              Text("${products.size} Items", fontWeight = FontWeight.Bold, color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
            }

            val lowStockCount = products.count { it.stockQuantity <= it.minStockAlert }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Low Stock Items", color = TextSecondaryDark, style = MaterialTheme.typography.bodyMedium)
              Text("$lowStockCount Items", fontWeight = FontWeight.Bold, color = if (lowStockCount > 0) SoftRed else Emerald400, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }

  // Custom Data Export Dialog in Reports Screen
  if (showExportDialog) {
    CustomExportDialog(
      invoices = invoices,
      initialMode = com.manglamfertilizer.app.data.accounting.AccountingPeriodMode.THIS_MONTH,
      onDismiss = { showExportDialog = false }
    )
  }
}

@Composable
private fun StatBlock(
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkSurfaceElevated,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
    modifier = modifier
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(text = title, style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
  }
}
