package com.manglamfertilizer.app.ui.accounts

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.model.DailyAccountsColumnConfig
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

/**
 * Redesigned, spacious, professional Daily Accounts Columns Customization Dialog.
 *
 * Highlights:
 * - Full-width responsive container formatted properly for small and large screens.
 * - Horizontal text layout exclusively — no vertical text rotation.
 * - Removed cluttered "MANDATORY" badges from UI.
 * - Spacious horizontal cards for each column with clear Move Up/Down, Visibility, Rename, Width Stepper (+/-), and Delete controls.
 * - Prominent "+ Create Custom Column" button.
 * - Safe layout reset and persistence.
 */
@Composable
fun ManageDailyAccountsColumnsDialog(
  columns: List<DailyAccountsColumnConfig>,
  onSaveColumns: (List<DailyAccountsColumnConfig>) -> Unit,
  onAddCustomField: (String, String) -> Unit,
  onRenameField: (String, String) -> Unit,
  onDeleteField: (String) -> Unit,
  onResetDefaults: () -> Unit,
  onDismiss: () -> Unit
) {
  var workingColumns by remember(columns) {
    mutableStateOf(
      if (columns.isEmpty()) DailyAccountsColumnConfig.DEFAULT_COLUMNS else columns
    )
  }
  var showAddCustomFieldDialog by remember { mutableStateOf(false) }
  var editingField by remember { mutableStateOf<DailyAccountsColumnConfig?>(null) }
  var deletingField by remember { mutableStateOf<DailyAccountsColumnConfig?>(null) }
  var showResetDefaultsConfirm by remember { mutableStateOf(false) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(18.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .imePadding()
        .padding(horizontal = 4.dp, vertical = 12.dp)
        .testTag("manage_daily_accounts_columns_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        // 1. Dialog Header with Title and Close Action
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Emerald400.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(20.dp)
              )
            }

            Column {
              Text(
                text = "Customize Columns",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryDark
              )
              Text(
                text = "Manage visibility, order & widths",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = TextSecondaryDark
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(32.dp)
              .testTag("close_columns_dialog_btn")
          ) {
            Icon(
              Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondaryDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Action Bar: "+ Create Custom Column" & Column Count Info
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val visibleCount = workingColumns.count { it.isVisible }
          Text(
            text = "$visibleCount of ${workingColumns.size} columns visible",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondaryDark
          )

          Button(
            onClick = { showAddCustomFieldDialog = true },
            colors = ButtonDefaults.buttonColors(
              containerColor = Emerald500.copy(alpha = 0.18f),
              contentColor = Emerald400
            ),
            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier
              .height(34.dp)
              .testTag("add_daily_accounts_custom_field_btn")
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+ New Column", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // 3. Scrollable Column Cards List
        LazyColumn(
          modifier = Modifier
            .weight(1f, fill = false)
            .height(380.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(workingColumns, key = { _, col -> col.id }) { index, col ->
            ColumnConfigCard(
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
              onUpdateWidth = { newWidth ->
                val updated = workingColumns.toMutableList()
                updated[index] = col.copy(defaultWidthDp = newWidth)
                workingColumns = updated
              },
              onRename = { editingField = col },
              onDelete = {
                if (!col.isMandatory) {
                  deletingField = col
                }
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DarkBorder, thickness = 0.8.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // 4. Bottom Controls: Reset Defaults and Save
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = { showResetDefaultsConfirm = true },
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1f)
              .height(42.dp)
              .testTag("reset_defaults_columns_btn")
          ) {
            Icon(
              Icons.Default.Refresh,
              contentDescription = null,
              tint = TextSecondaryDark,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset", color = TextSecondaryDark, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }

          Button(
            onClick = {
              val reorderedWithIndex = workingColumns.mapIndexed { i, c -> c.copy(order = i) }
              onSaveColumns(reorderedWithIndex)
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = Emerald500,
              contentColor = DarkBg
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(1.6f)
              .height(42.dp)
              .testTag("save_daily_accounts_columns_btn")
          ) {
            Text("Save & Apply", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
          }
        }
      }
    }
  }

  // --- Modal: Add Custom Column ---
  if (showAddCustomFieldDialog) {
    var newFieldName by remember { mutableStateOf("") }
    var selectedDataType by remember { mutableStateOf("Text") }
    var initialWidthDp by remember { mutableIntStateOf(100) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }
    val dataTypes = listOf("Text", "Number", "Currency", "Date")

    AlertDialog(
      onDismissRequest = { showAddCustomFieldDialog = false },
      modifier = Modifier.imePadding(),
      title = {
        Text(
          "Create Custom Column",
          color = TextPrimaryDark,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Add a new accounting column (e.g. 'Vehicle No.', 'Agent / Ref', 'Bag Type', 'Remarks'):",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
          )

          OutlinedTextField(
            value = newFieldName,
            onValueChange = { newFieldName = it; fieldError = null },
            label = { Text("Column Title") },
            placeholder = { Text("e.g. Vehicle No.") },
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
              .testTag("custom_column_title_input")
          )

          // Data Type Selector
          Column {
            Text(
              "DATA TYPE",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
              ),
              color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { dropdownExpanded = true }
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(selectedDataType, color = TextPrimaryDark, fontSize = 13.sp)
                  Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = TextSecondaryDark
                  )
                }
              }

              DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.background(DarkCard)
              ) {
                dataTypes.forEach { type ->
                  DropdownMenuItem(
                    text = { Text(type, color = TextPrimaryDark) },
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
            Text(fieldError!!, color = SoftRed, style = MaterialTheme.typography.labelSmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = newFieldName.trim()
            if (clean.isBlank()) {
              fieldError = "Column title cannot be empty"
              return@Button
            }
            onAddCustomField(clean, selectedDataType)
            showAddCustomFieldDialog = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Emerald500,
            contentColor = DarkBg
          ),
          modifier = Modifier.testTag("confirm_add_custom_column_btn")
        ) {
          Text("Add Column", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddCustomFieldDialog = false }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // --- Modal: Rename Column ---
  editingField?.let { col ->
    var renameValue by remember { mutableStateOf(col.title) }
    var renameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { editingField = null },
      modifier = Modifier.imePadding(),
      title = {
        Text(
          "Rename Column",
          color = TextPrimaryDark,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Enter new name for '${col.title}':", color = TextSecondaryDark, fontSize = 12.sp)
          OutlinedTextField(
            value = renameValue,
            onValueChange = { renameValue = it; renameError = null },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            modifier = Modifier.fillMaxWidth().testTag("rename_column_input")
          )
          if (renameError != null) {
            Text(renameError!!, color = SoftRed, style = MaterialTheme.typography.labelSmall)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val clean = renameValue.trim()
            if (clean.isBlank()) {
              renameError = "Column label cannot be empty"
              return@Button
            }
            onRenameField(col.id, clean)
            // Also update local workingColumns title immediately
            workingColumns = workingColumns.map { if (it.id == col.id) it.copy(title = clean) else it }
            editingField = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Emerald500,
            contentColor = DarkBg
          ),
          modifier = Modifier.testTag("confirm_rename_column_btn")
        ) {
          Text("Rename", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingField = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // --- Modal: Delete Custom Column Confirmation ---
  deletingField?.let { col ->
    AlertDialog(
      onDismissRequest = { deletingField = null },
      title = {
        Text(
          "Delete Custom Column",
          color = TextPrimaryDark,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      },
      text = {
        Text(
          text = "Are you sure you want to delete '${col.title}'? This will remove the column from the Daily Accounts table layout.",
          color = TextSecondaryDark,
          fontSize = 12.5.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteField(col.id)
            workingColumns = workingColumns.filterNot { it.id == col.id }
            deletingField = null
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = SoftRed,
            contentColor = Color.White
          ),
          modifier = Modifier.testTag("confirm_delete_column_btn")
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingField = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(14.dp)
    )
  }

  // --- Modal: Reset Defaults Confirmation ---
  if (showResetDefaultsConfirm) {
    AlertDialog(
      onDismissRequest = { showResetDefaultsConfirm = false },
      title = {
        Text(
          "Reset Column Layout?",
          color = TextPrimaryDark,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      },
      text = {
        Text(
          "This will restore standard Daily Accounts columns (S.No., Farmer Name, Product, Qty, Total, Cash, Online, Due) to their default order and widths.",
          color = TextSecondaryDark,
          fontSize = 12.5.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onResetDefaults()
            workingColumns = DailyAccountsColumnConfig.DEFAULT_COLUMNS
            showResetDefaultsConfirm = false
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Emerald500,
            contentColor = DarkBg
          ),
          modifier = Modifier.testTag("confirm_reset_defaults_btn")
        ) {
          Text("Reset Defaults", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDefaultsConfirm = false }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(14.dp)
    )
  }
}

/**
 * Clean, Horizontal Configuration Card for a single Daily Accounts Column.
 * Ensures ample horizontal space, horizontal typography, and responsive controls.
 */
@Composable
private fun ColumnConfigCard(
  column: DailyAccountsColumnConfig,
  isFirst: Boolean,
  isLast: Boolean,
  onToggleVisibility: () -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onUpdateWidth: (Int) -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit
) {
  val cardBg = if (column.isVisible) DarkCard else DarkSurfaceElevated.copy(alpha = 0.45f)
  val borderStroke = if (column.isVisible) {
    BorderStroke(1.dp, Emerald400.copy(alpha = 0.25f))
  } else {
    BorderStroke(1.dp, DarkBorder.copy(alpha = 0.6f))
  }

  Card(
    shape = RoundedCornerShape(10.dp),
    colors = CardDefaults.cardColors(containerColor = cardBg),
    border = borderStroke,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("daily_acc_col_${column.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
      // Row 1: Checkbox, Column Name (strictly horizontal), Type Badge, and Reorder/Rename/Delete Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left: Visibility Checkbox + Title + Custom/Type Badges
        Row(
          verticalAlignment = Alignment.CenterVertically,
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
            modifier = Modifier
              .size(32.dp)
              .testTag("col_checkbox_${column.id}")
          )

          Spacer(modifier = Modifier.width(6.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = column.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (column.isVisible) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 13.5.sp
                ),
                color = if (column.isVisible) TextPrimaryDark else TextMutedDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              if (column.isCustom) {
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Emerald400.copy(alpha = 0.15f),
                  border = BorderStroke(0.6.dp, Emerald400.copy(alpha = 0.4f))
                ) {
                  Text(
                    text = "CUSTOM",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald400,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                  )
                }
              }
            }

            Text(
              text = "${column.dataType} format",
              fontSize = 10.sp,
              color = TextMutedDark
            )
          }
        }

        // Right Actions: Move Up, Move Down, Rename, Delete
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          // Move Up
          IconButton(
            onClick = onMoveUp,
            enabled = !isFirst,
            modifier = Modifier
              .size(30.dp)
              .testTag("move_up_${column.id}")
          ) {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = "Move Up",
              tint = if (!isFirst) Emerald400 else TextMutedDark.copy(alpha = 0.3f),
              modifier = Modifier.size(16.dp)
            )
          }

          // Move Down
          IconButton(
            onClick = onMoveDown,
            enabled = !isLast,
            modifier = Modifier
              .size(30.dp)
              .testTag("move_down_${column.id}")
          ) {
            Icon(
              imageVector = Icons.Default.ArrowDownward,
              contentDescription = "Move Down",
              tint = if (!isLast) Emerald400 else TextMutedDark.copy(alpha = 0.3f),
              modifier = Modifier.size(16.dp)
            )
          }

          // Rename
          IconButton(
            onClick = onRename,
            modifier = Modifier
              .size(30.dp)
              .testTag("rename_${column.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Rename",
              tint = TextSecondaryDark,
              modifier = Modifier.size(15.dp)
            )
          }

          // Delete (Enabled for custom/non-mandatory fields)
          if (!column.isMandatory) {
            IconButton(
              onClick = onDelete,
              modifier = Modifier
                .size(30.dp)
                .testTag("delete_${column.id}")
            ) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = SoftRed.copy(alpha = 0.85f),
                modifier = Modifier.size(15.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Row 2: Column Width Adjustment Controls (Decrease / Increase Steppers + Width Label)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurfaceElevated.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "Column Width:",
            fontSize = 11.sp,
            color = TextSecondaryDark,
            fontWeight = FontWeight.Medium
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = DarkCard,
            border = BorderStroke(0.6.dp, DarkBorder)
          ) {
            Text(
              text = "${column.defaultWidthDp} dp",
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold,
              color = Emerald400,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // Stepper Controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // - Width Stepper (min 45 dp)
          Surface(
            shape = CircleShape,
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .clickable {
                val newW = (column.defaultWidthDp - 10).coerceAtLeast(45)
                onUpdateWidth(newW)
              }
              .testTag("width_dec_${column.id}")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease Width",
                tint = TextSecondaryDark,
                modifier = Modifier.size(12.dp)
              )
            }
          }

          // + Width Stepper (max 300 dp)
          Surface(
            shape = CircleShape,
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .clickable {
                val newW = (column.defaultWidthDp + 10).coerceAtMost(300)
                onUpdateWidth(newW)
              }
              .testTag("width_inc_${column.id}")
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase Width",
                tint = Emerald400,
                modifier = Modifier.size(12.dp)
              )
            }
          }
        }
      }
    }
  }
}
