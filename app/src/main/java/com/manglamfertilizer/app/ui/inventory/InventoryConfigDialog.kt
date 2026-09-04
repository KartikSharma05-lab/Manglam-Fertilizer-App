package com.manglamfertilizer.app.ui.inventory

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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun InventoryConfigDialog(
  onOpenManageCategories: () -> Unit,
  onOpenManageColumns: () -> Unit,
  onOpenImportInventory: () -> Unit,
  onOpenExportInventory: () -> Unit,
  onDismiss: () -> Unit
) {
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
        .testTag("inventory_config_dialog")
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
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Inventory Configuration",
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

        // Options List
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          ConfigOptionTile(
            title = "Manage Categories",
            subtitle = "Add, rename, reorder, and remove product categories",
            icon = Icons.Default.Category,
            iconTint = Emerald400,
            onClick = {
              onDismiss()
              onOpenManageCategories()
            },
            testTag = "config_manage_categories"
          )

          ConfigOptionTile(
            title = "Manage Columns / Fields",
            subtitle = "Configure visible inventory table fields and attributes",
            icon = Icons.Default.ViewColumn,
            iconTint = Color(0xFF60A5FA),
            onClick = {
              onDismiss()
              onOpenManageColumns()
            },
            testTag = "config_manage_columns"
          )

          ConfigOptionTile(
            title = "Import Inventory",
            subtitle = "Bulk import product records via CSV spreadsheet file",
            icon = Icons.Default.FileUpload,
            iconTint = Color(0xFFA78BFA),
            onClick = {
              onDismiss()
              onOpenImportInventory()
            },
            testTag = "config_import_inventory"
          )

          ConfigOptionTile(
            title = "Export Inventory",
            subtitle = "Download current or filtered inventory as PDF / Excel / CSV",
            icon = Icons.Default.FileDownload,
            iconTint = Color(0xFF34D399),
            onClick = {
              onDismiss()
              onOpenExportInventory()
            },
            testTag = "config_export_inventory"
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color(0xFF0A0F0D)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Close", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun ConfigOptionTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = iconTint.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.3f)),
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold
          ),
          color = TextPrimaryDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = TextSecondaryDark
        )
      }
    }
  }
}
