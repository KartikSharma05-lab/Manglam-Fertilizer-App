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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.manglamfertilizer.app.data.model.CategoryItem
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.model.UserRole
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
fun ManageCategoriesDialog(
  categories: List<CategoryItem>,
  products: List<Product>,
  currentUser: User?,
  onAddCategory: (String, (Boolean, String?) -> Unit) -> Unit,
  onUpdateCategory: (CategoryItem, String, (Boolean, String?) -> Unit) -> Unit,
  onDeleteCategory: (String, String, (Boolean, String?) -> Unit) -> Unit,
  onReorderCategories: (List<CategoryItem>, (Boolean, String?) -> Unit) -> Unit,
  onDismiss: () -> Unit
) {
  var newCategoryName by remember { mutableStateOf("") }
  var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
  var editCategoryName by remember { mutableStateOf("") }
  var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }
  var reassignmentTargetCategory by remember { mutableStateOf("Uncategorized") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }

  val isAdmin = currentUser?.role == UserRole.ADMIN || currentUser?.role == null

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
        .testTag("manage_categories_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
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
              imageVector = Icons.Default.Category,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(20.dp)
            )
            Text(
              text = "Manage Categories",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              ),
              color = TextPrimaryDark
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondaryDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Create and organize inventory categories. All categories sync live with database.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = TextSecondaryDark,
          modifier = Modifier.padding(bottom = 12.dp)
        )

        // Compact New Category Input Section
        if (isAdmin) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Text(
                text = "NEW CATEGORY NAME",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.5.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondaryDark
              )
              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = newCategoryName,
                  onValueChange = {
                    newCategoryName = it
                    errorMessage = null
                  },
                  placeholder = {
                    Text(
                      text = "Enter category name",
                      color = TextMutedDark,
                      fontSize = 12.5.sp
                    )
                  },
                  singleLine = true,
                  modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("new_category_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald400,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = DarkBg,
                    unfocusedContainerColor = DarkBg
                  )
                )

                Button(
                  onClick = {
                    if (newCategoryName.isNotBlank() && !isSubmitting) {
                      isSubmitting = true
                      onAddCategory(newCategoryName.trim()) { success, err ->
                        isSubmitting = false
                        if (success) {
                          newCategoryName = ""
                        } else {
                          errorMessage = err ?: "Failed to add category"
                        }
                      }
                    }
                  },
                  enabled = newCategoryName.isNotBlank() && !isSubmitting,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = DarkBg
                  ),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier
                    .height(46.dp)
                    .testTag("add_category_button")
                ) {
                  Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                  Spacer(modifier = Modifier.width(3.dp))
                  Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
              }
            }
          }
        } else {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
          ) {
            Text(
              text = "Admin permission required to add or modify categories.",
              color = SoftRed,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            color = SoftRed,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            modifier = Modifier.padding(top = 4.dp)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Existing Categories List
        Text(
          text = "EXISTING CATEGORIES (${categories.size})",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          ),
          color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (categories.isEmpty()) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkCard,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Text(
              text = "No custom categories created yet. Enter name above to add.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = TextMutedDark,
              modifier = Modifier.padding(14.dp)
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            itemsIndexed(categories, key = { _, it -> it.id }) { index, cat ->
              val productCount = products.count { it.category.equals(cat.name, ignoreCase = true) }

              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("category_row_${cat.id}")
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = cat.name,
                      style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                      ),
                      color = TextPrimaryDark
                    )
                    Text(
                      text = "$productCount product(s)",
                      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                      color = if (productCount > 0) Emerald400 else TextMutedDark
                    )
                  }

                  if (isAdmin) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                      // Move Up
                      IconButton(
                        onClick = {
                          if (index > 0) {
                            val mutable = categories.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index - 1, item)
                            onReorderCategories(mutable) { _, _ -> }
                          }
                        },
                        enabled = index > 0,
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.ArrowUpward,
                          contentDescription = "Move Up",
                          tint = if (index > 0) TextSecondaryDark else TextMutedDark.copy(alpha = 0.25f),
                          modifier = Modifier.size(15.dp)
                        )
                      }

                      // Move Down
                      IconButton(
                        onClick = {
                          if (index < categories.size - 1) {
                            val mutable = categories.toMutableList()
                            val item = mutable.removeAt(index)
                            mutable.add(index + 1, item)
                            onReorderCategories(mutable) { _, _ -> }
                          }
                        },
                        enabled = index < categories.size - 1,
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.ArrowDownward,
                          contentDescription = "Move Down",
                          tint = if (index < categories.size - 1) TextSecondaryDark else TextMutedDark.copy(alpha = 0.25f),
                          modifier = Modifier.size(15.dp)
                        )
                      }

                      // Edit / Rename
                      IconButton(
                        onClick = {
                          editingCategory = cat
                          editCategoryName = cat.name
                        },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Edit Category",
                          tint = Emerald400,
                          modifier = Modifier.size(15.dp)
                        )
                      }

                      // Delete
                      IconButton(
                        onClick = {
                          categoryToDelete = cat
                          reassignmentTargetCategory = categories.firstOrNull { it.id != cat.id }?.name ?: "Uncategorized"
                        },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete Category",
                          tint = SoftRed,
                          modifier = Modifier.size(15.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Done button
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(
            containerColor = Emerald500,
            contentColor = DarkBg
          ),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .testTag("manage_categories_done_btn")
        ) {
          Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }

  // Edit/Rename Dialog
  editingCategory?.let { targetCat ->
    AlertDialog(
      onDismissRequest = { editingCategory = null },
      containerColor = DarkSurfaceElevated,
      title = { Text("Rename Category", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column {
          Text(
            text = "Enter new name for '${targetCat.name}':",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark,
            modifier = Modifier.padding(bottom = 8.dp)
          )
          OutlinedTextField(
            value = editCategoryName,
            onValueChange = { editCategoryName = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkBg,
              unfocusedContainerColor = DarkBg
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (editCategoryName.isNotBlank()) {
              onUpdateCategory(targetCat.copy(name = editCategoryName.trim()), targetCat.name) { success, _ ->
                if (success) {
                  editingCategory = null
                }
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
        ) {
          Text("Save", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { editingCategory = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      }
    )
  }

  // Delete Category Confirmation Dialog (Safe Reassignment Support)
  categoryToDelete?.let { cat ->
    val productCount = products.count { it.category.equals(cat.name, ignoreCase = true) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val otherCategories = categories.filter { it.id != cat.id }.map { it.name } + listOf("Uncategorized")

    AlertDialog(
      onDismissRequest = { categoryToDelete = null },
      containerColor = DarkSurfaceElevated,
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(28.dp)) },
      title = { Text("Delete Category?", color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Are you sure you want to delete '${cat.name}'?",
            color = TextPrimaryDark,
            style = MaterialTheme.typography.bodyMedium
          )

          if (productCount > 0) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = SoftRed.copy(alpha = 0.12f),
              border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "⚠️ This category is currently assigned to $productCount product(s).",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                  color = SoftRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "To prevent orphaned products, choose a destination category to reassign these products to upon deletion:",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                  color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                      Text(
                        text = reassignmentTargetCategory,
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                      Icon(
                        imageVector = Icons.Default.ArrowDropDown,
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
                    otherCategories.forEach { target ->
                      DropdownMenuItem(
                        text = { Text(target, color = TextPrimaryDark, fontSize = 12.sp) },
                        onClick = {
                          reassignmentTargetCategory = target
                          dropdownExpanded = false
                        }
                      )
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteCategory(cat.id, cat.name) { success, _ ->
              if (success) {
                categoryToDelete = null
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White)
        ) {
          Text("Delete Category", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { categoryToDelete = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      }
    )
  }
}
