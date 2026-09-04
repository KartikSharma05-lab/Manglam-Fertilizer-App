package com.manglamfertilizer.app.ui.inventory

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.manglamfertilizer.app.data.model.InventoryColumnConfig

/**
 * Shared Column Definition specification that guarantees strict synchronization
 * between the table headers and every product data row.
 */
data class InventoryTableColumn(
  val id: String,
  val title: String,
  val width: Dp,
  val alignment: Alignment,
  val isCustom: Boolean = false,
  val order: Int = 0
) {
  companion object {
    /**
     * Map of predetermined column widths to prevent row content from pushing
     * columns sideways or causing alignment shifts.
     */
    fun getDefaultWidth(columnId: String): Dp = when (columnId) {
      "name" -> 165.dp
      "chemicalComposition" -> 200.dp
      "company" -> 135.dp
      "category" -> 110.dp
      "packaging" -> 95.dp
      "price" -> 85.dp
      "purchasePrice" -> 85.dp
      "mrp" -> 85.dp
      "stockQuantity" -> 80.dp
      "minStockAlert" -> 80.dp
      "expiryDate" -> 105.dp
      "barcode" -> 120.dp
      "crop" -> 120.dp
      "usesInstructions" -> 160.dp
      "rackLocation" -> 100.dp
      "hsnCode" -> 90.dp
      "batchNumber" -> 105.dp
      else -> 110.dp
    }

    /**
     * Map of predetermined column text alignments.
     */
    fun getDefaultAlignment(columnId: String): Alignment = when (columnId) {
      "stockQuantity", "minStockAlert", "expiryDate" -> Alignment.Center
      "price", "purchasePrice", "mrp" -> Alignment.CenterEnd
      else -> Alignment.CenterStart
    }

    /**
     * Fixed column width for the actions column (Info, Edit, Delete).
     */
    val ACTIONS_COLUMN_WIDTH: Dp = 100.dp

    /**
     * Converts a list of persisted [InventoryColumnConfig] items into ordered, active [InventoryTableColumn] definitions.
     */
    fun fromConfigs(configs: List<InventoryColumnConfig>): List<InventoryTableColumn> {
      val base = if (configs.isEmpty()) InventoryColumnConfig.DEFAULT_COLUMNS else configs
      return base
        .filter { it.isVisible }
        .sortedBy { it.order }
        .map { config ->
          InventoryTableColumn(
            id = config.id,
            title = config.title,
            width = getDefaultWidth(config.id),
            alignment = getDefaultAlignment(config.id),
            isCustom = config.isCustom,
            order = config.order
          )
        }
    }
  }
}
