package com.manglamfertilizer.app.ui.inventory

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.HourglassDisabled
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.InventoryColumnConfig
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.util.AlertEngine
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
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject

@Composable
fun ProductDetailsDialog(
  product: Product,
  customColumns: List<InventoryColumnConfig> = emptyList(),
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onDismiss: () -> Unit
) {
  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }
  val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

  val expiryEval = remember(product.expiryDate) {
    AlertEngine.evaluateExpiry(product.expiryDate)
  }
  val isExpired = expiryEval.isExpired
  val isNearExpiry = expiryEval.isNearExpiry
  val isLowStock = product.stockQuantity <= product.minStockAlert

  // Parse custom fields JSON
  val customFieldsList = remember(product.customFields) {
    try {
      if (product.customFields.isNotBlank()) {
        val json = JSONObject(product.customFields)
        val list = mutableListOf<Pair<String, String>>()
        val keys = json.keys()
        while (keys.hasNext()) {
          val key = keys.next()
          val value = json.optString(key, "")
          if (value.isNotBlank()) {
            list.add(key to value)
          }
        }
        list
      } else {
        emptyList()
      }
    } catch (e: Exception) {
      emptyList()
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("product_details_dialog")
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Dialog Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Surface(
              shape = CircleShape,
              color = Emerald900,
              modifier = Modifier.size(34.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Inventory2,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Product Details",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryDark
              )
              Text(
                text = product.category.ifBlank { "Uncategorized" },
                style = MaterialTheme.typography.labelSmall,
                color = Emerald400
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondaryDark,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable Content with ALL Product Information
        Column(
          modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Product Name & Company
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 18.sp
                ),
                color = TextPrimaryDark
              )
              if (product.company.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Company: ${product.company}",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Emerald400
                )
              }
              if (product.packaging.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Packaging: ${product.packaging}",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondaryDark
                )
              }
            }
          }

          // Chemical Composition (Prominent & Complete, Never clipped)
          if (product.chemicalComposition.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DarkSurfaceElevated,
              border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "CHEMICAL COMPOSITION / FORMULATION",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  ),
                  color = Emerald400
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = product.chemicalComposition,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                  ),
                  color = TextPrimaryDark
                )
              }
            }
          }

          // Stock & Status Badges Card
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "INVENTORY & STOCK STATUS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondaryDark
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Current Stock", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = "${product.stockQuantity} ${product.unit.name}",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isLowStock) SoftRed else Emerald400
                    )
                  )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Min Alert Level", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = "${product.minStockAlert} ${product.unit.name}",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = TextPrimaryDark
                    )
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text("Stock Condition", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isLowStock) SoftRed.copy(alpha = 0.2f) else Emerald900,
                    border = BorderStroke(1.dp, if (isLowStock) SoftRed else Emerald400)
                  ) {
                    Text(
                      text = if (isLowStock) "LOW STOCK" else "IN STOCK",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (isLowStock) SoftRed else Emerald400,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }

          // Pricing Breakdown Card
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "PRICING DETAILS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondaryDark
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text("Selling Price", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = currencyFormat.format(product.sellingPrice),
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = Emerald400
                    )
                  )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text("Purchase Price", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = if (product.purchasePrice > 0) currencyFormat.format(product.purchasePrice) else "—",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = TextPrimaryDark
                    )
                  )
                }

                Column(horizontalAlignment = Alignment.End) {
                  Text("MRP", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = if (product.mrp > 0) currencyFormat.format(product.mrp) else "—",
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      color = TextSecondaryDark
                    )
                  )
                }
              }
            }
          }

          // Expiry & Batch Information
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "EXPIRY & BATCH DETAILS",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondaryDark
              )
              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text("Expiry Date", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  val expText = if (product.expiryDate != null) dateFormat.format(Date(product.expiryDate)) else "Not specified"
                  Text(
                    text = expText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                      fontWeight = FontWeight.Bold,
                      color = when {
                        isExpired -> SoftRed
                        isNearExpiry -> GoldAmber
                        else -> TextPrimaryDark
                      }
                    )
                  )
                }

                if (product.expiryDate != null) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                      isExpired -> SoftRed.copy(alpha = 0.2f)
                      isNearExpiry -> GoldAmber.copy(alpha = 0.2f)
                      else -> Emerald900
                    },
                    border = BorderStroke(
                      1.dp,
                      when {
                        isExpired -> SoftRed
                        isNearExpiry -> GoldAmber
                        else -> Emerald400
                      }
                    )
                  ) {
                    Text(
                      text = when {
                        isExpired -> "EXPIRED"
                        expiryEval.priority == ExpiryPriority.HIGH -> "HIGH PRIORITY (≤1 MO)"
                        expiryEval.priority == ExpiryPriority.MEDIUM -> "MEDIUM PRIORITY (≤3 MOS)"
                        expiryEval.priority == ExpiryPriority.NORMAL -> "EXPIRING (≤6 MOS)"
                        else -> "VALID"
                      },
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = when {
                        isExpired -> SoftRed
                        expiryEval.priority == ExpiryPriority.HIGH -> SoftRed
                        expiryEval.priority == ExpiryPriority.MEDIUM -> GoldAmber
                        expiryEval.priority == ExpiryPriority.NORMAL -> GoldAmber
                        else -> Emerald400
                      },
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }

              if (product.batchNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Batch Number", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(product.batchNumber, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                }
              }

              if (product.hsnCode.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("HSN Code", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(product.hsnCode, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                }
              }

              if (product.barcode.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Barcode / SKU", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(product.barcode, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                }
              }

              if (product.rackLocation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Rack / Shelf Location", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(product.rackLocation, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                }
              }
            }
          }

          // Target Crops & Application Instructions
          if (product.crop.isNotBlank() || product.usesInstructions.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DarkCard,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                if (product.crop.isNotBlank()) {
                  Text(
                    text = "TARGET CROPS",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    ),
                    color = Emerald400
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = product.crop,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryDark
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                }

                if (product.usesInstructions.isNotBlank()) {
                  Text(
                    text = "APPLICATION & USAGE INSTRUCTIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = FontWeight.Bold,
                      letterSpacing = 0.5.sp
                    ),
                    color = Emerald400
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = product.usesInstructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimaryDark
                  )
                }
              }
            }
          }

          // Custom Fields Section
          if (customFieldsList.isNotEmpty()) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DarkCard,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "CUSTOM FIELDS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                  ),
                  color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                customFieldsList.forEach { (key, value) ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(key, style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                    Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimaryDark)
                  }
                }
              }
            }
          }

          // Record Metadata & Audit Details (Created Date, Updated Date, Created By)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkBg,
            border = BorderStroke(1.dp, DarkBorder.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "RECORD AUDIT & METADATA",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondaryDark
              )
              Spacer(modifier = Modifier.height(8.dp))

              if (product.createdAt > 0) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Created Date", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = dateFormat.format(Date(product.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                  )
                }
              }

              if (product.updatedAt > 0) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Last Updated", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(
                    text = dateFormat.format(Date(product.updatedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                  )
                }
              }

              if (product.createdBy.isNotBlank()) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Text("Created By", style = MaterialTheme.typography.labelSmall, color = TextMutedDark)
                  Text(product.createdBy, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = DarkBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row (Edit, Delete, Close)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = {
              onDismiss()
              onDelete()
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
            border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              onDismiss()
              onEdit()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1.2f)
          ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Edit Product", fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
}
