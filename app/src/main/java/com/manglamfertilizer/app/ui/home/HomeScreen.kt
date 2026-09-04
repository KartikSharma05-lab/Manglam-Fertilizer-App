package com.manglamfertilizer.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.manglamfertilizer.app.ui.common.ManglamSearchBar
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.manglamfertilizer.app.data.model.CloudSyncState
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.DailyHighlight
import com.manglamfertilizer.app.data.model.DashboardMetrics
import com.manglamfertilizer.app.data.model.GreetingInfo
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  user: User?,
  cloudSyncState: CloudSyncState = CloudSyncState.CONNECTED_SYNCED,
  syncStatusText: String = "Synced",
  greetingInfo: GreetingInfo,
  highlight: DailyHighlight,
  metrics: DashboardMetrics,
  stockAlerts: List<StockAlert>,
  recentInvoices: List<Invoice>,
  products: List<Product>,
  customers: List<Customer>,
  isRefreshing: Boolean = false,
  onRefresh: () -> Unit = {},
  onNavigateToScreen: (String) -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenBarcodeScanner: () -> Unit,
  onCloudClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var debouncedSearchQuery by remember { mutableStateOf("") }
  val scrollState = rememberScrollState()

  LaunchedEffect(searchQuery) {
    delay(150)
    debouncedSearchQuery = searchQuery.trim()
  }

  // BackHandler to clear search query if typed
  BackHandler(enabled = searchQuery.isNotBlank()) {
    searchQuery = ""
  }

  // Filtered Results for Inline Search
  val matchingProducts = remember(debouncedSearchQuery, products) {
    if (debouncedSearchQuery.isBlank()) emptyList()
    else products.filter {
      it.name.contains(debouncedSearchQuery, ignoreCase = true) ||
          it.company.contains(debouncedSearchQuery, ignoreCase = true) ||
          it.category.contains(debouncedSearchQuery, ignoreCase = true)
    }.take(4)
  }

  val matchingCustomers = remember(debouncedSearchQuery, customers) {
    if (debouncedSearchQuery.isBlank()) emptyList()
    else customers.filter {
      it.name.contains(debouncedSearchQuery, ignoreCase = true) ||
          it.phoneNumber.contains(debouncedSearchQuery) ||
          it.village.contains(debouncedSearchQuery, ignoreCase = true)
    }.take(4)
  }

  val matchingInvoices = remember(debouncedSearchQuery, recentInvoices) {
    if (debouncedSearchQuery.isBlank()) emptyList()
    else recentInvoices.filter {
      it.invoiceNumber.contains(debouncedSearchQuery, ignoreCase = true) ||
          it.customerName.contains(debouncedSearchQuery, ignoreCase = true)
    }.take(4)
  }

  PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .testTag("home_pull_to_refresh")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .imePadding()
    ) {
      // 1. Top Header
      HomeHeader(
        cloudSyncState = cloudSyncState,
        syncStatusText = syncStatusText,
        unreadAlertsCount = stockAlerts.size,
        onNotificationsClick = onOpenNotifications,
        onCloudClick = onCloudClick
      )

      Spacer(modifier = Modifier.height(6.dp))

      // 2. Dynamic Greeting + Date + Live Time Pill + Today's Highlight
      TodayHighlightCard(
        user = user,
        greetingInfo = greetingInfo,
        highlight = highlight
      )

      Spacer(modifier = Modifier.height(8.dp))

      // 3. Inline Direct Search Bar (NO popup overlay)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp)
      ) {
        ManglamSearchBar(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = "Search products, customers, bills...",
          trailingContent = {
            IconButton(
              onClick = onOpenBarcodeScanner,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan Barcode",
                tint = Emerald400,
                modifier = Modifier.size(18.dp)
              )
            }
          },
          testTag = "home_inline_search_input",
          modifier = Modifier.fillMaxWidth()
        )

        // Inline Search Results
        AnimatedVisibility(
          visible = searchQuery.isNotBlank(),
          enter = fadeIn() + expandVertically(),
          exit = fadeOut() + shrinkVertically()
        ) {
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 6.dp)
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (matchingProducts.isEmpty() && matchingCustomers.isEmpty() && matchingInvoices.isEmpty()) {
                Text(
                  text = "No matching records found for \"$searchQuery\"",
                  color = TextMutedDark,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(8.dp)
                )
              }

              // Matching Products
              if (matchingProducts.isNotEmpty()) {
                Text(
                  text = "PRODUCTS (${matchingProducts.size})",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Emerald400,
                  letterSpacing = 0.5.sp
                )
                matchingProducts.forEach { prod ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        searchQuery = ""
                        onNavigateToScreen("inventory")
                      }
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(prod.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                          Text("Stock: ${prod.stockQuantity} ${prod.unit} • ₹${prod.sellingPrice}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                      }
                      Text("View", fontSize = 11.sp, color = Emerald400, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }

              // Matching Customers
              if (matchingCustomers.isNotEmpty()) {
                Text(
                  text = "FARMERS / CUSTOMERS (${matchingCustomers.size})",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Emerald400,
                  letterSpacing = 0.5.sp
                )
                matchingCustomers.forEach { cust ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        searchQuery = ""
                        onNavigateToScreen("customers")
                      }
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(cust.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                          Text(cust.village + (if (cust.phoneNumber.isNotBlank()) " • ${cust.phoneNumber}" else ""), style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                      }
                      if (cust.totalDue > 0) {
                        Text("Due ₹${cust.totalDue.toInt()}", fontSize = 11.sp, color = GoldAmber, fontWeight = FontWeight.Bold)
                      }
                    }
                  }
                }
              }

              // Matching Invoices
              if (matchingInvoices.isNotEmpty()) {
                Text(
                  text = "INVOICES (${matchingInvoices.size})",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = Emerald400,
                  letterSpacing = 0.5.sp
                )
                matchingInvoices.forEach { inv ->
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .clickable {
                        searchQuery = ""
                        onNavigateToScreen("billing")
                      }
                  ) {
                    Row(
                      modifier = Modifier.padding(8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(inv.invoiceNumber, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                          Text(inv.customerName + " • ₹${inv.grandTotal.toInt()}", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                        }
                      }
                      Text("View", fontSize = 11.sp, color = Emerald400, fontWeight = FontWeight.Bold)
                    }
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // 4. Quick Actions Grid (8 quick actions)
      QuickActionsGrid(
        onActionClick = { actionId ->
          when (actionId) {
            "barcode" -> onOpenBarcodeScanner()
            "alerts" -> onOpenNotifications()
            else -> onNavigateToScreen(actionId)
          }
        }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 5. Smart Business Dashboard Cards (4 metrics: Today's Sales, Customer Dues, Low Stock, Near Expiry)
      SmartDashboardSection(metrics = metrics)

      Spacer(modifier = Modifier.height(10.dp))

      // 6. Recent Invoices Section (Today's Invoices only, display/history only)
      RecentInvoicesSection(
        invoices = recentInvoices,
        onViewAllInvoices = { onNavigateToScreen("billing") }
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }
}
