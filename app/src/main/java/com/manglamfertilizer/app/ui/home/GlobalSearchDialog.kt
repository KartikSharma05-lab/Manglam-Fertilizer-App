package com.manglamfertilizer.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.manglamfertilizer.app.ui.common.ManglamSearchBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun GlobalSearchDialog(
  products: List<Product>,
  customers: List<Customer>,
  invoices: List<Invoice>,
  onDismiss: () -> Unit,
  onSelectProduct: (Product) -> Unit,
  onSelectCustomer: (Customer) -> Unit,
  onSelectInvoice: (Invoice) -> Unit
) {
  var query by remember { mutableStateOf("") }

  val filteredProducts = remember(query, products) {
    if (query.isBlank()) emptyList()
    else products.filter { it.name.contains(query, ignoreCase = true) || it.company.contains(query, ignoreCase = true) }
  }

  val filteredCustomers = remember(query, customers) {
    if (query.isBlank()) emptyList()
    else customers.filter { it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query) || it.village.contains(query, ignoreCase = true) }
  }

  val filteredInvoices = remember(query, invoices) {
    if (query.isBlank()) emptyList()
    else invoices.filter { it.invoiceNumber.contains(query, ignoreCase = true) || it.customerName.contains(query, ignoreCase = true) }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth()
      .imePadding(),
    title = null,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(480.dp)
      ) {
        ManglamSearchBar(
          value = query,
          onValueChange = { query = it },
          placeholder = "Search products, customers, bills...",
          testTag = "global_search_input",
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (query.isBlank()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Type name, brand, mobile or invoice # to search",
              style = MaterialTheme.typography.bodyMedium,
              color = TextMutedDark
            )
          }
        } else if (filteredProducts.isEmpty() && filteredCustomers.isEmpty() && filteredInvoices.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No matching records found for \"$query\"",
              style = MaterialTheme.typography.bodyMedium,
              color = TextMutedDark
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (filteredProducts.isNotEmpty()) {
              item {
                Text(
                  text = "PRODUCTS (${filteredProducts.size})",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Emerald400
                )
              }
              items(filteredProducts) { prod ->
                SearchItemRow(
                  title = prod.name,
                  subtitle = "${prod.company} • Stock: ${prod.stockQuantity} ${prod.unit}",
                  icon = Icons.Default.Inventory2,
                  iconTint = Emerald400,
                  containerTint = Emerald900,
                  onClick = { onSelectProduct(prod); onDismiss() }
                )
              }
            }

            if (filteredCustomers.isNotEmpty()) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "CUSTOMERS (${filteredCustomers.size})",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = InfoSky
                )
              }
              items(filteredCustomers) { cust ->
                SearchItemRow(
                  title = cust.name,
                  subtitle = "${cust.phoneNumber} • ${cust.village}",
                  icon = Icons.Default.People,
                  iconTint = InfoSky,
                  containerTint = androidx.compose.ui.graphics.Color(0xFF0C2B42),
                  onClick = { onSelectCustomer(cust); onDismiss() }
                )
              }
            }

            if (filteredInvoices.isNotEmpty()) {
              item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "INVOICES (${filteredInvoices.size})",
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = GoldAmber
                )
              }
              items(filteredInvoices) { inv ->
                SearchItemRow(
                  title = inv.invoiceNumber,
                  subtitle = "${inv.customerName} • ₹${inv.grandTotal}",
                  icon = Icons.Default.Receipt,
                  iconTint = GoldAmber,
                  containerTint = androidx.compose.ui.graphics.Color(0xFF3B2A06),
                  onClick = { onSelectInvoice(inv); onDismiss() }
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      IconButton(onClick = onDismiss) {
        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(20.dp)
  )
}

@Composable
private fun SearchItemRow(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: androidx.compose.ui.graphics.Color,
  containerTint: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = DarkSurfaceElevated,
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = containerTint,
        modifier = Modifier.size(32.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
      }
    }
  }
}
