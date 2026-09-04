package com.manglamfertilizer.app.ui.inventory

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.model.InventoryColumnConfig
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun ManageColumnsDialog(
  columns: List<InventoryColumnConfig>,
  onSaveColumns: (List<InventoryColumnConfig>) -> Unit,
  onAddCustomField: (String) -> Unit,
  onRenameCustomField: (String, String) -> Unit,
  onDeleteCustomField: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var workingColumns by remember(columns) {
    mutableStateOf(if (columns.isEmpty()) InventoryColumnConfig.DEFAULT_COLUMNS else columns)
  }
  var showAddCustomFieldDialog by remember { mutableStateOf(false) }
  var editingField by remember { mutableStateOf<InventoryColumnConfig?>(null) }
  var deletingField by remember { mutableStateOf<InventoryColumnConfig?>(null) }
  var showResetDefaultsConfirm by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .imePadding()
        .padding(6.dp)
        .testTag("manage_columns_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
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
              imageVector = Icons.Default.ViewColumn,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(22.dp)
            )
            Text(
              text = "Manage Columns & Fields",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              ),
              color = TextPrimaryDark
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
          }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Reorder, show/hide, rename, or remove table columns. All stored product data remains safe.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Create Custom Field button
        Button(
          onClick = { showAddCustomFieldDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = Emerald400),
          border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .testTag("add_custom_field_btn")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Create Custom Field", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Column List
        LazyColumn(
          modifier = Modifier
            .weight(1f, fill = false)
            .height(320.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          itemsIndexed(workingColumns, key = { _, col -> col.id }) { index, col ->
            ColumnConfigItemTile(
              column = col,
              isFirst = index == 0,
              isLast = index == workingColumns.size - 1,
              onToggleVisibility = {
                val updated = workingColumns.toMutableList()
                updated[index] = col.copy(isVisible = !col.isVisible)
                workingColumns = updated
              },
              onMoveUp = {
                if (index > 0) {
                  val updated = workingColumns.toMutableList()
                  val temp = updated[index]
                  updated[index] = updated[index - 1]
                  updated[index - 1] = temp
                  workingColumns = updated
                }
              },
              onMoveDown = {
                if (index < workingColumns.size - 1) {
                  val updated = workingColumns.toMutableList()
                  val temp = updated[index]
                  updated[index] = updated[index + 1]
                  updated[index + 1] = temp
                  workingColumns = updated
                }
              },
              onRename = { editingField = col },
              onDelete = { deletingField = col }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Actions (Reset Defaults / Save)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = { showResetDefaultsConfirm = true },
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Defaults", color = TextSecondaryDark, fontSize = 11.5.sp)
          }

          Button(
            onClick = {
              onSaveColumns(workingColumns)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1.5f)
              .height(42.dp)
              .testTag("save_columns_btn")
          ) {
            Text("Save Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }

  // Dialog: Add Custom Field
  if (showAddCustomFieldDialog) {
    var newFieldName by remember { mutableStateOf("") }
    var selectedDataType by remember { mutableStateOf("Text") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }
    val dataTypes = listOf("Text", "Number", "Date", "Select Options")

    AlertDialog(
      onDismissRequest = { showAddCustomFieldDialog = false },
      modifier = Modifier.imePadding(),
      title = { Text("Create Custom Field", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Enter the name of the new product attribute (e.g. 'Manufacturer Code', 'Warehouse Section', 'Dealer Code', 'Special Notes'):",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
          )

          OutlinedTextField(
            value = newFieldName,
            onValueChange = { newFieldName = it; fieldError = null },
            label = { Text("Field Name") },
            placeholder = { Text("e.g. Warehouse Section") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            modifier = Modifier.fillMaxWidth().testTag("custom_field_name_input")
          )

          // Data Type Picker
          Column {
            Text("DATA TYPE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold), color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkBg,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { dropdownExpanded = true }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(text = selectedDataType, color = TextPrimaryDark, fontSize = 12.5.sp)
                  Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark)
                }
              }

              DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(DarkCard)
              ) {
                dataTypes.forEach { type ->
                  DropdownMenuItem(
                    text = { Text(type, color = TextPrimaryDark, fontSize = 12.sp) },
                    onClick = {
                      selectedDataType = type
                      dropdownExpanded = false
                    }
                  )
                }
              }
            }
          }

          if (fieldError != null) {
            Text(fieldError ?: "", color = SoftRed, style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newFieldName.isBlank()) {
              fieldError = "Field name cannot be empty"
              return@Button
            }
            val title = newFieldName.trim()
            val id = "custom_" + title.lowercase().replace("\\s+".toRegex(), "_") + "_" + System.currentTimeMillis() % 10000
            val newCol = InventoryColumnConfig(
              id = id,
              title = title,
              isVisible = true,
              isCustom = true,
              isLocked = false,
              order = workingColumns.size,
              dataType = selectedDataType
            )
            workingColumns = workingColumns + newCol
            onAddCustomField(title)
            showAddCustomFieldDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
        ) {
          Text("Add Field", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCustomFieldDialog = false }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // Dialog: Rename Field
  editingField?.let { field ->
    var renameValue by remember(field.id) { mutableStateOf(field.title) }

    AlertDialog(
      onDismissRequest = { editingField = null },
      modifier = Modifier.imePadding(),
      title = { Text("Rename Column", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column {
          Text(
            text = "Enter new display title for '${field.title}':",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          OutlinedTextField(
            value = renameValue,
            onValueChange = { renameValue = it },
            label = { Text("Column Title") },
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
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (renameValue.isNotBlank()) {
              val targetId = field.id
              val updated = workingColumns.map {
                if (it.id == targetId) it.copy(title = renameValue.trim()) else it
              }
              workingColumns = updated
              if (field.isCustom) {
                onRenameCustomField(targetId, renameValue.trim())
              }
              editingField = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
        ) {
          Text("Rename", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingField = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // Dialog: Delete / Remove Column from Active Display
  deletingField?.let { targetField ->
    AlertDialog(
      onDismissRequest = { deletingField = null },
      title = { Text("Remove from Active Table?", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "Remove '${targetField.title}' from the active inventory display configuration?",
            color = TextPrimaryDark,
            style = MaterialTheme.typography.bodyMedium
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = Emerald900.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "🛡️ Safe Removal: Historical and stored data in the database will NOT be destroyed. You can restore this column anytime by tapping 'Reset Defaults'.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
              color = Emerald400,
              modifier = Modifier.padding(8.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            workingColumns = workingColumns.filterNot { it.id == targetField.id }
            if (targetField.isCustom) {
              onDeleteCustomField(targetField.id)
            }
            deletingField = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = TextPrimaryDark)
        ) {
          Text("Remove", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingField = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // Dialog: Confirm Reset Defaults (Display Configuration Only)
  if (showResetDefaultsConfirm) {
    AlertDialog(
      onDismissRequest = { showResetDefaultsConfirm = false },
      title = { Text("Reset Display Configuration?", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Text(
          text = "This will restore the standard column order and visibility. It will NOT delete or alter any product, customer, or invoice data.",
          color = TextSecondaryDark,
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = {
            workingColumns = InventoryColumnConfig.DEFAULT_COLUMNS
            showResetDefaultsConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
        ) {
          Text("Reset", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDefaultsConfirm = false }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(14.dp)
    )
  }
}

@Composable
private fun ColumnConfigItemTile(
  column: InventoryColumnConfig,
  isFirst: Boolean,
  isLast: Boolean,
  onToggleVisibility: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  val bg = if (column.isVisible) DarkCard else DarkBg
  val borderColor = if (column.isVisible) DarkBorder else DarkBorder.copy(alpha = 0.5f)

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bg,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Visibility Checkbox & Column Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.weight(1f)
      ) {
        Checkbox(
          checked = column.isVisible,
          onCheckedChange = { onToggleVisibility() },
          colors = CheckboxDefaults.colors(
            checkedColor = Emerald500,
            uncheckedColor = TextMutedDark,
            checkmarkColor = DarkBg
          ),
          modifier = Modifier.size(26.dp)
        )

        Column {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = column.title,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (column.isVisible) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp
              ),
              color = if (column.isVisible) TextPrimaryDark else TextMutedDark
            )
            if (column.isCustom) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFA78BFA).copy(alpha = 0.2f)
              ) {
                Text(
                  text = "CUSTOM",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                  color = Color(0xFFA78BFA),
                  modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
              }
            }
          }
        }
      }

      // Reorder & Action buttons (Rename, Delete, Move Up, Move Down)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(onClick = onRename, modifier = Modifier.size(26.dp)) {
          Icon(Icons.Default.Edit, contentDescription = "Rename", tint = Emerald400, modifier = Modifier.size(15.dp))
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftRed, modifier = Modifier.size(15.dp))
        }

        IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(26.dp)) {
          Icon(
            Icons.Default.ArrowUpward,
            contentDescription = "Move Up",
            tint = if (!isFirst) TextSecondaryDark else TextMutedDark.copy(alpha = 0.25f),
            modifier = Modifier.size(15.dp)
          )
        }

        IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(26.dp)) {
          Icon(
            Icons.Default.ArrowDownward,
            contentDescription = "Move Down",
            tint = if (!isLast) TextSecondaryDark else TextMutedDark.copy(alpha = 0.25f),
            modifier = Modifier.size(15.dp)
          )
        }
      }
    }
  }
}
