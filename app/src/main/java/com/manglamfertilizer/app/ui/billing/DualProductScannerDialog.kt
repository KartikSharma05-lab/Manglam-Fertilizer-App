package com.manglamfertilizer.app.ui.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.Product
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

@Composable
fun DualProductScannerDialog(
  products: List<Product>,
  onDismiss: () -> Unit,
  onProductSelected: (Product) -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Barcode / QR", "AI Product Scan")

  // Barcode State
  var barcodeQuery by remember { mutableStateOf("") }
  var barcodeError by remember { mutableStateOf<String?>(null) }
  var scannedProduct by remember { mutableStateOf<Product?>(null) }

  // AI Scanner State
  var aiQueryText by remember { mutableStateOf("") }
  var aiDetectedInfo by remember { mutableStateOf<Product?>(null) }
  var aiSuggestions by remember { mutableStateOf<List<Product>>(emptyList()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .padding(16.dp)
      .fillMaxWidth()
      .imePadding(),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = Emerald900,
            modifier = Modifier.size(32.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = if (selectedTab == 0) Icons.Default.QrCodeScanner else Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Dual Product Scanner",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimaryDark
            )
            Text(
              text = if (selectedTab == 0) "Instant Barcode / QR Identification" else "AI Packaging & Chemical Recognition",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondaryDark
            )
          }
        }

        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMutedDark, modifier = Modifier.size(18.dp))
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(390.dp)
      ) {
        // Tab Selector
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DarkSurface,
          contentColor = Emerald400,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = Emerald400
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = selectedTab == index,
              onClick = { selectedTab = index },
              text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (index == 0) Icons.Default.QrCodeScanner else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (selectedTab == index) Emerald400 else TextSecondaryDark,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == index) Emerald400 else TextSecondaryDark
                  )
                }
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
          // ==========================================
          // TAB 0: BARCODE / QR SCANNER
          // ==========================================
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Scan or type product barcode / batch number:",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = barcodeQuery,
              onValueChange = {
                barcodeQuery = it
                barcodeError = null
                scannedProduct = null
              },
              placeholder = { Text("e.g. 8901234567890 or Batch #", fontSize = 12.5.sp) },
              leadingIcon = {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
              },
              trailingIcon = {
                if (barcodeQuery.isNotBlank()) {
                  IconButton(onClick = { barcodeQuery = ""; barcodeError = null; scannedProduct = null }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMutedDark, modifier = Modifier.size(16.dp))
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(10.dp),
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
                .testTag("barcode_scanner_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = {
                val q = barcodeQuery.trim()
                if (q.isBlank()) {
                  barcodeError = "Please enter or scan a barcode"
                  return@Button
                }
                val matched = products.find { p ->
                  p.barcode.equals(q, ignoreCase = true) ||
                      p.batchNumber.equals(q, ignoreCase = true) ||
                      p.hsnCode.equals(q, ignoreCase = true) ||
                      p.name.contains(q, ignoreCase = true)
                }
                if (matched != null) {
                  scannedProduct = matched
                  barcodeError = null
                } else {
                  scannedProduct = null
                  barcodeError = "Product not found in inventory."
                }
              },
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Search Real Inventory", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            barcodeError?.let { err ->
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
                  Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = err, color = SoftRed, fontSize = 12.sp)
                }
              }
            }

            scannedProduct?.let { prod ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, Emerald400),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(prod.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 14.sp)
                      Text("${prod.company} • Stock: ${prod.stockQuantity} ${prod.unit}", color = TextSecondaryDark, fontSize = 11.5.sp)
                      if (prod.chemicalComposition.isNotBlank()) {
                        Text("Comp: ${prod.chemicalComposition}", color = Emerald400, fontSize = 11.sp)
                      }
                    }
                    Text("₹${prod.sellingPrice}", fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 16.sp)
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Button(
                    onClick = {
                      onProductSelected(prod)
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add to Invoice Cart", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        } else {
          // ==========================================
          // TAB 1: AI PRODUCT SCAN
          // ==========================================
          Column(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = "Describe product, chemical formulation, or crop use:",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = aiQueryText,
              onValueChange = {
                aiQueryText = it
                if (it.isNotBlank()) {
                  val q = it.trim().lowercase()
                  aiSuggestions = products.filter { p ->
                    p.name.lowercase().contains(q) ||
                        p.company.lowercase().contains(q) ||
                        p.chemicalComposition.lowercase().contains(q) ||
                        p.category.lowercase().contains(q) ||
                        p.crop.lowercase().contains(q) ||
                        q.split(" ").any { word -> word.length > 2 && (p.name.lowercase().contains(word) || p.chemicalComposition.lowercase().contains(word)) }
                  }
                } else {
                  aiSuggestions = emptyList()
                }
              },
              placeholder = { Text("e.g. Urea 45kg, DAP 18:46:0, Zinc Sulfate, Glyphosate...", fontSize = 12.sp) },
              leadingIcon = {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
              },
              singleLine = true,
              shape = RoundedCornerShape(10.dp),
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

            Spacer(modifier = Modifier.height(10.dp))

            if (aiSuggestions.isNotEmpty()) {
              Text(
                text = "CONFIRM DETECTED PRODUCT (${aiSuggestions.size} matches):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Emerald400,
                letterSpacing = 0.8.sp
              )
              Spacer(modifier = Modifier.height(6.dp))

              LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(aiSuggestions) { prod ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable {
                        onProductSelected(prod)
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
                        Text(prod.name, fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 13.sp)
                        Text("${prod.company} • ${prod.unit} • Stock: ${prod.stockQuantity}", fontSize = 11.sp, color = TextSecondaryDark)
                        if (prod.chemicalComposition.isNotBlank()) {
                          Text(prod.chemicalComposition, fontSize = 10.5.sp, color = Emerald400)
                        }
                      }
                      Column(horizontalAlignment = Alignment.End) {
                        Text("₹${prod.sellingPrice}", fontWeight = FontWeight.Bold, color = Emerald400, fontSize = 14.sp)
                        Text("Tap to add", fontSize = 9.5.sp, color = GoldAmber)
                      }
                    }
                  }
                }
              }
            } else if (aiQueryText.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
              ) {
                Text(
                  text = "No matching inventory product found. Please verify spelling or add product in Inventory.",
                  color = SoftRed,
                  fontSize = 12.sp,
                  modifier = Modifier.padding(12.dp)
                )
              }
            } else {
              Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(32.dp))
                  Spacer(modifier = Modifier.height(6.dp))
                  Text("Type crop formulation or chemical above", color = TextMutedDark, fontSize = 12.sp)
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close", color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}
