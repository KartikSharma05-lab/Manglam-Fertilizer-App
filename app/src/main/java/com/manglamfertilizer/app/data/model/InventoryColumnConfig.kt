package com.manglamfertilizer.app.data.model

data class InventoryColumnConfig(
  val id: String,
  val title: String,
  val isVisible: Boolean = true,
  val isCustom: Boolean = false,
  val isLocked: Boolean = false,
  val order: Int = 0,
  val dataType: String = "Text"
) {
  companion object {
    val DEFAULT_COLUMNS = listOf(
      InventoryColumnConfig(id = "name", title = "Product Name", isVisible = true, isCustom = false, isLocked = false, order = 0, dataType = "Text"),
      InventoryColumnConfig(id = "chemicalComposition", title = "Chemical Composition", isVisible = true, isCustom = false, isLocked = false, order = 1, dataType = "Text"),
      InventoryColumnConfig(id = "company", title = "Company / Brand", isVisible = true, isCustom = false, isLocked = false, order = 2, dataType = "Text"),
      InventoryColumnConfig(id = "category", title = "Category", isVisible = true, isCustom = false, isLocked = false, order = 3, dataType = "Text"),
      InventoryColumnConfig(id = "packaging", title = "Packaging", isVisible = true, isCustom = false, isLocked = false, order = 4, dataType = "Text"),
      InventoryColumnConfig(id = "price", title = "Price", isVisible = true, isCustom = false, isLocked = false, order = 5, dataType = "Number"),
      InventoryColumnConfig(id = "purchasePrice", title = "Buy Price", isVisible = true, isCustom = false, isLocked = false, order = 6, dataType = "Number"),
      InventoryColumnConfig(id = "mrp", title = "MRP", isVisible = true, isCustom = false, isLocked = false, order = 7, dataType = "Number"),
      InventoryColumnConfig(id = "stockQuantity", title = "Quantity", isVisible = true, isCustom = false, isLocked = false, order = 8, dataType = "Number"),
      InventoryColumnConfig(id = "minStockAlert", title = "Min Stock", isVisible = true, isCustom = false, isLocked = false, order = 9, dataType = "Number"),
      InventoryColumnConfig(id = "expiryDate", title = "Expiry", isVisible = true, isCustom = false, isLocked = false, order = 10, dataType = "Date"),
      InventoryColumnConfig(id = "barcode", title = "Barcode", isVisible = true, isCustom = false, isLocked = false, order = 11, dataType = "Text"),
      InventoryColumnConfig(id = "batchNumber", title = "Batch No.", isVisible = false, isCustom = false, isLocked = false, order = 12, dataType = "Text"),
      InventoryColumnConfig(id = "crop", title = "Target Crops", isVisible = false, isCustom = false, isLocked = false, order = 13, dataType = "Text"),
      InventoryColumnConfig(id = "usesInstructions", title = "Uses / Instructions", isVisible = false, isCustom = false, isLocked = false, order = 14, dataType = "Text"),
      InventoryColumnConfig(id = "rackLocation", title = "Rack Location", isVisible = false, isCustom = false, isLocked = false, order = 15, dataType = "Text"),
      InventoryColumnConfig(id = "hsnCode", title = "HSN Code", isVisible = false, isCustom = false, isLocked = false, order = 16, dataType = "Text")
    )
  }
}
