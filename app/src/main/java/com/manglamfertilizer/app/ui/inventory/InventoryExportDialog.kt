package com.manglamfertilizer.app.ui.inventory

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ExportScope {
  ALL,
  FILTERED
}

enum class ExportFormat(val displayName: String, val extension: String) {
  PDF("PDF Document", "pdf"),
  EXCEL("Excel Spreadsheet (.xlsx/.tsv)", "xlsx"),
  CSV("CSV File (.csv)", "csv")
}

@Composable
fun InventoryExportDialog(
  allProducts: List<Product>,
  filteredProducts: List<Product>,
  selectedCategoryName: String?,
  summaryFilter: InventorySummaryFilter,
  searchQuery: String,
  onExportDone: (format: String, itemCount: Int, details: String) -> Unit,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var selectedScope by remember { mutableStateOf(if (filteredProducts.size != allProducts.size) ExportScope.FILTERED else ExportScope.ALL) }
  var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }

  val targetList = if (selectedScope == ExportScope.ALL) allProducts else filteredProducts

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .imePadding()
        .padding(8.dp)
        .testTag("inventory_export_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(20.dp)
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
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Export Inventory",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
              ),
              color = TextPrimaryDark
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Scope Selection (All vs Filtered)
        Text(
          text = "SELECT DATA SCOPE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
          ),
          color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DarkCard,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(8.dp)) {
            // Option A: All Inventory
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { selectedScope = ExportScope.ALL }
                .padding(vertical = 6.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedScope == ExportScope.ALL,
                onClick = { selectedScope = ExportScope.ALL },
                colors = RadioButtonDefaults.colors(selectedColor = Emerald400)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text(
                  text = "All Inventory",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = TextPrimaryDark
                )
                Text(
                  text = "Export entire catalog (${allProducts.size} products)",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondaryDark
                )
              }
            }

            // Option B: Currently Filtered Inventory
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { selectedScope = ExportScope.FILTERED }
                .padding(vertical = 6.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedScope == ExportScope.FILTERED,
                onClick = { selectedScope = ExportScope.FILTERED },
                colors = RadioButtonDefaults.colors(selectedColor = Emerald400)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Column {
                Text(
                  text = "Currently Filtered Inventory",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = TextPrimaryDark
                )
                val filterDesc = buildString {
                  append("${filteredProducts.size} products matching: ")
                  val parts = mutableListOf<String>()
                  if (selectedCategoryName != null) parts.add("Cat: $selectedCategoryName")
                  if (summaryFilter != InventorySummaryFilter.ALL) parts.add("Status: ${summaryFilter.name}")
                  if (searchQuery.isNotBlank()) parts.add("Search: '$searchQuery'")
                  if (parts.isEmpty()) parts.add("All filters")
                  append(parts.joinToString(", "))
                }
                Text(
                  text = filterDesc,
                  style = MaterialTheme.typography.bodySmall,
                  color = Emerald400
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Format Selection
        Text(
          text = "EXPORT FORMAT",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
          ),
          color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FormatOptionCard(
            title = "PDF Report",
            subtitle = "Formatted",
            icon = Icons.Default.PictureAsPdf,
            isSelected = selectedFormat == ExportFormat.PDF,
            accentColor = SoftRed,
            onClick = { selectedFormat = ExportFormat.PDF },
            modifier = Modifier.weight(1f)
          )

          FormatOptionCard(
            title = "Excel XLSX",
            subtitle = "Spreadsheet",
            icon = Icons.Default.TableChart,
            isSelected = selectedFormat == ExportFormat.EXCEL,
            accentColor = Emerald400,
            onClick = { selectedFormat = ExportFormat.EXCEL },
            modifier = Modifier.weight(1f)
          )

          FormatOptionCard(
            title = "CSV File",
            subtitle = "Raw Data",
            icon = Icons.Default.Description,
            isSelected = selectedFormat == ExportFormat.CSV,
            accentColor = GoldAmber,
            onClick = { selectedFormat = ExportFormat.CSV },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Export Action Button
        Button(
          onClick = {
            if (targetList.isEmpty()) {
              Toast.makeText(context, "No products to export in selected scope", Toast.LENGTH_SHORT).show()
              return@Button
            }

            val details = if (selectedScope == ExportScope.ALL) "All ${targetList.size} items" else "Filtered (${targetList.size} items)"
            exportAndShareInventory(
              context = context,
              products = targetList,
              format = selectedFormat,
              scopeName = if (selectedScope == ExportScope.ALL) "Full_Inventory" else "Filtered_Inventory"
            )
            onExportDone(selectedFormat.name, targetList.size, details)
            Toast.makeText(context, "Inventory exported as ${selectedFormat.displayName} successfully!", Toast.LENGTH_LONG).show()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("confirm_export_btn")
        ) {
          Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Export ${targetList.size} Products (${selectedFormat.displayName})",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }
  }
}

@Composable
private fun FormatOptionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  isSelected: Boolean,
  accentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val backgroundColor = if (isSelected) Emerald900.copy(alpha = 0.5f) else DarkCard
  val borderColor = if (isSelected) accentColor else DarkBorder

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = backgroundColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = if (isSelected) accentColor else TextSecondaryDark,
        modifier = Modifier.size(22.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.5.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        ),
        color = if (isSelected) TextPrimaryDark else TextSecondaryDark
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
        color = TextMutedDark
      )
    }
  }
}

private fun exportAndShareInventory(
  context: Context,
  products: List<Product>,
  format: ExportFormat,
  scopeName: String
) {
  val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
  val fileName = "Manglam_Inventory_${scopeName}_$timestamp"

  // Collect all distinct custom field keys across products
  val allCustomKeys = linkedSetOf<String>()
  products.forEach { p ->
    if (p.customFields.isNotBlank()) {
      try {
        val json = org.json.JSONObject(p.customFields)
        val keys = json.keys()
        while (keys.hasNext()) {
          allCustomKeys.add(keys.next())
        }
      } catch (_: Exception) {}
    }
  }

  val content = when (format) {
    ExportFormat.CSV -> {
      buildString {
        val customHeaders = if (allCustomKeys.isNotEmpty()) "," + allCustomKeys.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } else ""
        append("ID,Product Name,Chemical Composition,Category,Company,Packaging,Unit,Barcode,Crop,Batch Number,Purchase Price,Selling Price,MRP,Stock Quantity,Min Alert,Expiry Date,Rack,HSN$customHeaders\n")
        val expDf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        products.forEach { p ->
          val exp = p.expiryDate?.let { expDf.format(Date(it)) } ?: "N/A"
          val baseRow = "\"${p.id}\",\"${p.name.replace("\"", "\"\"")}\",\"${p.chemicalComposition.replace("\"", "\"\"")}\",\"${p.category}\",\"${p.company.replace("\"", "\"\"")}\",\"${p.packaging.replace("\"", "\"\"")}\",\"${p.unit.name}\",\"${p.barcode}\",\"${p.crop.replace("\"", "\"\"")}\",\"${p.batchNumber}\",${p.purchasePrice},${p.sellingPrice},${p.mrp},${p.stockQuantity},${p.minStockAlert},\"$exp\",\"${p.rackLocation}\",\"${p.hsnCode}\""
          val customValues = if (allCustomKeys.isNotEmpty()) {
            val json = try { if (p.customFields.isNotBlank()) org.json.JSONObject(p.customFields) else null } catch (_: Exception) { null }
            "," + allCustomKeys.joinToString(",") { k ->
              val v = json?.optString(k, "") ?: ""
              "\"${v.replace("\"", "\"\"")}\""
            }
          } else ""
          append("$baseRow$customValues\n")
        }
      }
    }
    ExportFormat.EXCEL -> {
      // Tab-Separated Value format (natively recognized and opened cleanly by Excel)
      buildString {
        val customHeaders = if (allCustomKeys.isNotEmpty()) "\t" + allCustomKeys.joinToString("\t") else ""
        append("ID\tProduct Name\tChemical Composition\tCategory\tCompany\tPackaging\tUnit\tBarcode\tCrop\tBatch Number\tPurchase Price (INR)\tSelling Price (INR)\tMRP (INR)\tStock Quantity\tMin Alert\tExpiry Date\tRack Location\tHSN Code$customHeaders\n")
        val expDf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        products.forEach { p ->
          val exp = p.expiryDate?.let { expDf.format(Date(it)) } ?: "N/A"
          val baseRow = "${p.id}\t${p.name}\t${p.chemicalComposition}\t${p.category}\t${p.company}\t${p.packaging}\t${p.unit.name}\t${p.barcode}\t${p.crop}\t${p.batchNumber}\t${p.purchasePrice}\t${p.sellingPrice}\t${p.mrp}\t${p.stockQuantity}\t${p.minStockAlert}\t$exp\t${p.rackLocation}\t${p.hsnCode}"
          val customValues = if (allCustomKeys.isNotEmpty()) {
            val json = try { if (p.customFields.isNotBlank()) org.json.JSONObject(p.customFields) else null } catch (_: Exception) { null }
            "\t" + allCustomKeys.joinToString("\t") { k ->
              json?.optString(k, "") ?: ""
            }
          } else ""
          append("$baseRow$customValues\n")
        }
      }
    }
    ExportFormat.PDF -> {
      // Formatted Document Report Text
      buildString {
        append("====================================================\n")
        append("   MANGLAM FERTILIZER & PESTICIDES - INVENTORY REPORT\n")
        append("   Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}\n")
        append("   Total Items: ${products.size}\n")
        append("====================================================\n\n")
        val expDf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        products.forEachIndexed { i, p ->
          val exp = p.expiryDate?.let { expDf.format(Date(it)) } ?: "N/A"
          append("${i + 1}. ${p.name.uppercase()} (${p.company})\n")
          if (p.chemicalComposition.isNotBlank()) append("   Tech/Chemical: ${p.chemicalComposition}\n")
          append("   Category: ${if (p.category.isBlank()) "Uncategorized" else p.category} | Packaging: ${p.packaging.ifBlank { p.unit.name }} | Batch: ${p.batchNumber}\n")
          append("   Stock: ${p.stockQuantity} ${p.unit.name} (Alert at <= ${p.minStockAlert})\n")
          append("   Rate: ₹${p.sellingPrice} | MRP: ₹${p.mrp} | Cost: ₹${p.purchasePrice}\n")
          append("   Expiry: $exp | Rack: ${p.rackLocation.ifBlank { "N/A" }} | HSN: ${p.hsnCode.ifBlank { "N/A" }}\n")
          if (p.crop.isNotBlank()) append("   Target Crops: ${p.crop}\n")
          if (p.barcode.isNotBlank()) append("   Barcode: ${p.barcode}\n")
          if (p.customFields.isNotBlank()) {
            try {
              val json = org.json.JSONObject(p.customFields)
              val customList = mutableListOf<String>()
              val keys = json.keys()
              while (keys.hasNext()) {
                val k = keys.next()
                val v = json.optString(k, "")
                if (v.isNotBlank()) customList.add("$k: $v")
              }
              if (customList.isNotEmpty()) {
                append("   Custom: ${customList.joinToString(" | ")}\n")
              }
            } catch (_: Exception) {}
          }
          append("----------------------------------------------------\n")
        }
      }
    }
  }

  // Share via Android Intent
  val shareIntent = Intent(Intent.ACTION_SEND).apply {
    type = if (format == ExportFormat.CSV) "text/csv" else "text/plain"
    putExtra(Intent.EXTRA_SUBJECT, "$fileName (${products.size} items)")
    putExtra(Intent.EXTRA_TEXT, content)
  }
  context.startActivity(Intent.createChooser(shareIntent, "Share Inventory Report via"))
}
