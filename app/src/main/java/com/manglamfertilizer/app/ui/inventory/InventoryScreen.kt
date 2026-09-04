package com.manglamfertilizer.app.ui.inventory

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import com.manglamfertilizer.app.ui.common.ManglamFloatingActionButton
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.AuditCleanupRun
import com.manglamfertilizer.app.data.model.AuditLogItem
import com.manglamfertilizer.app.data.model.CategoryItem
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.InventoryColumnConfig
import com.manglamfertilizer.app.data.model.InventoryHistoryItem
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.repository.DuplicateHandlingPolicy
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
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
fun InventoryScreen(
  products: List<Product>,
  categories: List<CategoryItem>,
  inventoryHistory: List<InventoryHistoryItem>,
  auditLogs: List<AuditLogItem> = emptyList(),
  auditCleanupRuns: List<AuditCleanupRun> = emptyList(),
  isAuditLoading: Boolean = false,
  isRetentionSimulating: Boolean = false,
  latestRetentionSimulation: AuditCleanupRun? = null,
  onRefreshAudit: () -> Unit = {},
  onRunRetentionSimulation: (String) -> Unit = {},
  currentUser: User?,
  inventoryColumns: List<InventoryColumnConfig> = emptyList(),
  onSaveColumns: (List<InventoryColumnConfig>) -> Unit = {},
  onAddCustomField: (String) -> Unit = {},
  onRenameCustomField: (String, String) -> Unit = { _, _ -> },
  onDeleteCustomField: (String) -> Unit = {},
  onAddProduct: (
    name: String,
    category: String,
    company: String,
    unit: ProductUnit,
    batch: String,
    purchasePrice: Double,
    sellingPrice: Double,
    mrp: Double,
    stock: Double,
    minAlert: Double,
    expiryDate: Long?,
    rack: String,
    hsn: String,
    chemicalComposition: String,
    barcode: String,
    packaging: String,
    crop: String,
    usesInstructions: String,
    customFields: String,
    onResult: (Boolean, String?) -> Unit
  ) -> Unit,
  onUpdateProduct: (Product, Product?, (Boolean, String?) -> Unit) -> Unit,
  onDeleteProduct: (String, String, (Boolean, String?) -> Unit) -> Unit,
  onAddCategory: (String, (Boolean, String?) -> Unit) -> Unit,
  onUpdateCategory: (CategoryItem, String, (Boolean, String?) -> Unit) -> Unit,
  onDeleteCategory: (String, String, (Boolean, String?) -> Unit) -> Unit,
  onReorderCategories: (List<CategoryItem>, (Boolean, String?) -> Unit) -> Unit,
  onLogExport: (format: String, count: Int, details: String) -> Unit,
  onImportProductsBatch: (List<Product>, DuplicateHandlingPolicy, String, Int, (Boolean, Int, Int, String?) -> Unit) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var searchInput by remember { mutableStateOf("") }
  var debouncedSearchQuery by remember { mutableStateOf("") }
  var selectedCategoryName by remember { mutableStateOf<String?>(null) } // null = "All"
  var statusFilter by remember { mutableStateOf(ProductStatusFilter.ALL) }

  // Debounced search to prevent unnecessary filtering lag
  LaunchedEffect(searchInput) {
    delay(150)
    debouncedSearchQuery = searchInput.trim()
  }

  // Dialog States
  var showAddProductDialog by remember { mutableStateOf(false) }
  var scannedInitialBarcode by remember { mutableStateOf("") }
  var aiScannedInitialProduct by remember { mutableStateOf<Product?>(null) }
  var showImportDialog by remember { mutableStateOf(false) }
  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var deletingProduct by remember { mutableStateOf<Product?>(null) }
  var detailedProduct by remember { mutableStateOf<Product?>(null) }
  var showManageCategoriesDialog by remember { mutableStateOf(false) }
  var showManageColumnsDialog by remember { mutableStateOf(false) }
  var showHistoryDialog by remember { mutableStateOf(false) }
  var showExportDialog by remember { mutableStateOf(false) }
  var showConfigDialog by remember { mutableStateOf(false) }
  var showScannerDialog by remember { mutableStateOf(false) }
  val tableListState = rememberLazyListState()

  // BackHandler to handle dialog/modal closure first before navigating away
  val isAnyInventoryDialogOpen = showScannerDialog ||
      deletingProduct != null ||
      showManageCategoriesDialog ||
      showManageColumnsDialog ||
      showImportDialog ||
      showHistoryDialog ||
      showExportDialog ||
      showConfigDialog ||
      detailedProduct != null ||
      editingProduct != null ||
      showAddProductDialog ||
      searchInput.isNotBlank()

  BackHandler(enabled = isAnyInventoryDialogOpen) {
    when {
      showScannerDialog -> showScannerDialog = false
      deletingProduct != null -> deletingProduct = null
      showManageCategoriesDialog -> showManageCategoriesDialog = false
      showManageColumnsDialog -> showManageColumnsDialog = false
      showImportDialog -> showImportDialog = false
      showHistoryDialog -> showHistoryDialog = false
      showExportDialog -> showExportDialog = false
      showConfigDialog -> showConfigDialog = false
      detailedProduct != null -> detailedProduct = null
      editingProduct != null -> editingProduct = null
      showAddProductDialog -> showAddProductDialog = false
      searchInput.isNotBlank() -> searchInput = ""
    }
  }

  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }

  // Authoritative date calculations for Expiry Status
  val startOfToday = remember {
    Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis
  }
  val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000

  // Real database dynamic counts using AlertEngine
  val lowStockCount = remember(products) {
    products.count { it.stockQuantity <= it.minStockAlert }
  }

  val nearExpiryCount = remember(products) {
    products.count { p ->
      val eval = AlertEngine.evaluateExpiry(p.expiryDate)
      eval.isNearExpiry
    }
  }

  val expiredCount = remember(products) {
    products.count { p ->
      val eval = AlertEngine.evaluateExpiry(p.expiryDate)
      eval.isExpired
    }
  }

  val inStockCount = remember(products) {
    products.count { p ->
      val eval = AlertEngine.evaluateExpiry(p.expiryDate)
      p.stockQuantity > p.minStockAlert && !eval.isExpired
    }
  }

  // Active Columns list from unified shared configuration model
  val activeColumns = remember(inventoryColumns) {
    InventoryTableColumn.fromConfigs(inventoryColumns)
  }

  // Multi-dimensional Filter Pipeline (Category + Status Filter + Debounced Search Query)
  val filteredProducts = remember(debouncedSearchQuery, selectedCategoryName, statusFilter, products) {
    products.filter { prod ->
      // 1. Category Filter
      val matchesCat = selectedCategoryName == null || prod.category.equals(selectedCategoryName, ignoreCase = true)

      // 2. Status Filter
      val expiryEval = AlertEngine.evaluateExpiry(prod.expiryDate)
      val isProdExpired = expiryEval.isExpired
      val isProdNearExpiry = expiryEval.isNearExpiry
      val isProdLowStock = prod.stockQuantity <= prod.minStockAlert

      val matchesStatus = when (statusFilter) {
        ProductStatusFilter.ALL -> true
        ProductStatusFilter.IN_STOCK -> !isProdLowStock && !isProdExpired
        ProductStatusFilter.LOW_STOCK -> isProdLowStock
        ProductStatusFilter.NEAR_EXPIRY, ProductStatusFilter.EXPIRED -> isProdNearExpiry || isProdExpired
      }

      // 3. Search Query (debounced across Name, Chemical, Company, Category, Barcode, Packaging, Crop, Rack, HSN, Batch, Custom)
      val q = debouncedSearchQuery
      val matchesQuery = if (q.isBlank()) {
        true
      } else {
        prod.name.contains(q, ignoreCase = true) ||
            prod.chemicalComposition.contains(q, ignoreCase = true) ||
            prod.company.contains(q, ignoreCase = true) ||
            prod.category.contains(q, ignoreCase = true) ||
            prod.barcode.contains(q, ignoreCase = true) ||
            prod.packaging.contains(q, ignoreCase = true) ||
            prod.crop.contains(q, ignoreCase = true) ||
            prod.batchNumber.contains(q, ignoreCase = true) ||
            prod.hsnCode.contains(q, ignoreCase = true) ||
            prod.rackLocation.contains(q, ignoreCase = true) ||
            prod.customFields.contains(q, ignoreCase = true)
      }

      matchesCat && matchesStatus && matchesQuery
    }
  }

  // Shared horizontal scroll state for Table Header & Rows
  val tableScrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .imePadding()
  ) {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      // 1. INVENTORY HEADER (Compact vertical positioning, no blank top gap)
      InventoryHeader(
        onOpenHistory = { showHistoryDialog = true },
        onOpenScanner = { showScannerDialog = true },
        onOpenExport = { showExportDialog = true },
        onOpenConfig = { showConfigDialog = true }
      )

      // 2. SEARCH BAR (Standardized ManglamSearchBar)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
      ) {
        com.manglamfertilizer.app.ui.common.ManglamSearchBar(
          value = searchInput,
          onValueChange = { searchInput = it },
          placeholder = "Search by name, chemical, comp...",
          testTag = "inventory_search_bar",
          modifier = Modifier.fillMaxWidth()
        )
      }

      // 3. CATEGORY TABS (Dynamic Categories from DB only, NO + Categories button)
      CategoryFilterBar(
        categories = categories,
        products = products,
        selectedCategory = selectedCategoryName,
        onSelectCategory = { selectedCategoryName = it }
      )

      // 4. SUMMARY CARDS (PRODUCTS, LOW STOCK, EXPIRY) - Compact horizontal layout
      InventorySummaryCards(
        totalProductsCount = products.size,
        inStockCount = inStockCount,
        lowStockCount = lowStockCount,
        nearExpiryCount = nearExpiryCount,
        expiredCount = expiredCount,
        selectedFilter = statusFilter,
        onSelectFilter = { statusFilter = it }
      )

      // 5. ROWS & COLS CONTROL BAR
      val totalVisibleCols = activeColumns.size + 1 // including Actions
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "Rows: ${filteredProducts.size} of ${products.size}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = TextSecondaryDark
          )
          if (filteredProducts.size != products.size) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Emerald900.copy(alpha = 0.5f)
            ) {
              Text(
                text = "FILTERED",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = Emerald400,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(6.dp),
          color = DarkCard,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { showManageColumnsDialog = true }
            .testTag("manage_columns_trigger_btn")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Text(
              text = "Cols: $totalVisibleCols",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              ),
              color = TextPrimaryDark
            )
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Manage Columns",
              tint = Emerald400,
              modifier = Modifier.size(13.dp)
            )
          }
        }
      }

      // 6. INVENTORY TABLE (2-Way Scrolling: Horizontal across Columns, Vertical across Product rows)
      if (filteredProducts.isEmpty()) {
        val emptyTitle = when {
          products.isEmpty() -> "No products added yet."
          statusFilter == ProductStatusFilter.LOW_STOCK -> "No low-stock products"
          statusFilter == ProductStatusFilter.NEAR_EXPIRY || statusFilter == ProductStatusFilter.EXPIRED -> "No products near expiry or expired"
          debouncedSearchQuery.isNotBlank() -> "No products matching '$debouncedSearchQuery'"
          selectedCategoryName != null -> "No products in '$selectedCategoryName'"
          else -> "No products found"
        }

        val emptySubtitle = when {
          products.isEmpty() -> "Tap the '+' button below to add your first product."
          statusFilter == ProductStatusFilter.LOW_STOCK -> "All active inventory items are stocked above their minimum alert levels."
          statusFilter == ProductStatusFilter.NEAR_EXPIRY || statusFilter == ProductStatusFilter.EXPIRED -> "All inventory items have valid future expiry dates."
          debouncedSearchQuery.isNotBlank() -> "Try searching by chemical formula, brand, company, or barcode."
          selectedCategoryName != null -> "This category currently has no products assigned."
          else -> "Try adjusting your search query or filters."
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = when (statusFilter) {
                ProductStatusFilter.LOW_STOCK -> Icons.Default.Warning
                else -> Icons.Default.Inventory2
              },
              contentDescription = null,
              tint = when (statusFilter) {
                ProductStatusFilter.LOW_STOCK -> SoftRed.copy(alpha = 0.7f)
                else -> TextMutedDark
              },
              modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = emptyTitle,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = TextSecondaryDark
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = emptySubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = TextMutedDark,
              modifier = Modifier.padding(horizontal = 24.dp),
              lineHeight = 16.sp
            )
          }
        }
      } else {
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          // Table Sticky Header Row (Horizontally synchronized with content rows)
          Surface(
            color = Color(0xFF0F291E), // Deep emerald table header as in reference video
            border = BorderStroke(0.5.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .horizontalScroll(tableScrollState)
                .padding(vertical = 8.dp, horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              activeColumns.forEach { col ->
                Box(
                  modifier = Modifier
                    .width(col.width)
                    .padding(horizontal = 6.dp),
                  contentAlignment = col.alignment
                ) {
                  Text(
                    text = col.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontSize = 11.5.sp,
                      fontWeight = FontWeight.Bold
                    ),
                    color = Emerald400
                  )
                }
              }

              // Actions Column Header
              Box(
                modifier = Modifier
                  .width(InventoryTableColumn.ACTIONS_COLUMN_WIDTH)
                  .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "Actions",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = Emerald400
                )
              }
            }
          }

          // Table Rows (LazyColumn with horizontalScroll synchronized per row)
          LazyColumn(
            state = tableListState,
            modifier = Modifier
              .fillMaxSize()
              .testTag("inventory_table_list"),
            contentPadding = PaddingValues(bottom = 76.dp)
          ) {
            items(filteredProducts.distinctBy { it.id }, key = { it.id }) { product ->
              val expiryEval = AlertEngine.evaluateExpiry(product.expiryDate)
              val isLowStock = product.stockQuantity <= product.minStockAlert
              val isExpired = expiryEval.isExpired
              val isNearExpiry = expiryEval.isNearExpiry

              Surface(
                color = DarkBg,
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { detailedProduct = product }
                  .testTag("inventory_row_${product.id}")
              ) {
                Column {
                  Row(
                    modifier = Modifier
                      .horizontalScroll(tableScrollState)
                      .padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    activeColumns.forEach { col ->
                      Box(
                        modifier = Modifier
                          .width(col.width)
                          .padding(horizontal = 6.dp),
                        contentAlignment = col.alignment
                      ) {
                        RenderTableCellContent(
                          columnId = col.id,
                          product = product,
                          isLowStock = isLowStock,
                          isExpired = isExpired,
                          isNearExpiry = isNearExpiry,
                          expiryEval = expiryEval,
                          currencyFormat = currencyFormat
                        )
                      }
                    }

                    // Actions Row: Info (Details), Edit, Delete
                    Row(
                      modifier = Modifier
                        .width(InventoryTableColumn.ACTIONS_COLUMN_WIDTH)
                        .padding(horizontal = 6.dp),
                      horizontalArrangement = Arrangement.Center,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      IconButton(
                        onClick = { detailedProduct = product },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Info,
                          contentDescription = "Details",
                          tint = Emerald400,
                          modifier = Modifier.size(17.dp)
                        )
                      }

                      IconButton(
                        onClick = { editingProduct = product },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Edit,
                          contentDescription = "Edit",
                          tint = Emerald400,
                          modifier = Modifier.size(16.dp)
                        )
                      }

                      IconButton(
                        onClick = { deletingProduct = product },
                        modifier = Modifier.size(28.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete",
                          tint = SoftRed,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }

                  HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
              }
            }
          }
        }
      }
    }

    // Floating Action Button anchored to bottom end with safe area positioning
    ManglamFloatingActionButton(
      onClick = { showAddProductDialog = true },
      contentDescription = "Add Product",
      testTag = "add_product_fab",
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(16.dp)
    )
  }

  // --- DIALOGS ---

  // 0. Product Details Dialog (Displays complete stored info, nothing silently hidden)
  detailedProduct?.let { currentDetailed ->
    ProductDetailsDialog(
      product = currentDetailed,
      customColumns = inventoryColumns,
      onEdit = {
        detailedProduct = null
        editingProduct = currentDetailed
      },
      onDelete = {
        detailedProduct = null
        deletingProduct = currentDetailed
      },
      onDismiss = { detailedProduct = null }
    )
  }

  // 1. Add Product Dialog
  if (showAddProductDialog) {
    AddEditProductDialog(
      product = null,
      categories = categories,
      initialBarcode = scannedInitialBarcode,
      initialProductData = aiScannedInitialProduct,
      customColumns = inventoryColumns,
      onDismiss = {
        showAddProductDialog = false
        scannedInitialBarcode = ""
        aiScannedInitialProduct = null
      },
      onOpenAddCategory = { showManageCategoriesDialog = true },
      onSubmit = { name, cat, comp, unit, batch, pPrice, sPrice, mrp, stock, minAlert, exp, rack, hsn, chem, barcode, pkg, crop, usesInst, custom, onDone ->
        onAddProduct(name, cat, comp, unit, batch, pPrice, sPrice, mrp, stock, minAlert, exp, rack, hsn, chem, barcode, pkg, crop, usesInst, custom) { success, msg ->
          if (success) {
            showAddProductDialog = false
            scannedInitialBarcode = ""
            aiScannedInitialProduct = null
            try {
              Toast.makeText(context.applicationContext, "Product saved to inventory", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
          }
          try {
            onDone(success, msg)
          } catch (e: Exception) {}
        }
      }
    )
  }

  // 2. Edit Product Dialog
  editingProduct?.let { currentProd ->
    AddEditProductDialog(
      product = currentProd,
      categories = categories,
      customColumns = inventoryColumns,
      onDismiss = { editingProduct = null },
      onOpenAddCategory = { showManageCategoriesDialog = true },
      onSubmit = { name, cat, comp, unit, batch, pPrice, sPrice, mrp, stock, minAlert, exp, rack, hsn, chem, barcode, pkg, crop, usesInst, custom, onDone ->
        val updated = currentProd.copy(
          name = name,
          category = cat,
          company = comp,
          unit = unit,
          batchNumber = batch,
          purchasePrice = pPrice,
          sellingPrice = sPrice,
          mrp = mrp,
          stockQuantity = stock,
          minStockAlert = minAlert,
          expiryDate = exp,
          rackLocation = rack,
          hsnCode = hsn,
          chemicalComposition = chem,
          barcode = barcode,
          packaging = pkg,
          crop = crop,
          usesInstructions = usesInst,
          customFields = custom
        )
        onUpdateProduct(updated, currentProd) { success, msg ->
          if (success) {
            editingProduct = null
            try {
              Toast.makeText(context.applicationContext, "Product updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {}
          }
          try {
            onDone(success, msg)
          } catch (e: Exception) {}
        }
      }
    )
  }

  // 3. Delete Product Confirmation
  deletingProduct?.let { prod ->
    AlertDialog(
      onDismissRequest = { deletingProduct = null },
      containerColor = DarkSurfaceElevated,
      icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed) },
      title = { Text("Delete Product?", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          text = "Are you sure you want to delete '${prod.name}' (${prod.company}) from inventory?",
          color = TextSecondaryDark
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteProduct(prod.id, prod.name) { success, _ ->
              if (success) {
                deletingProduct = null
                Toast.makeText(context, "Product deleted", Toast.LENGTH_SHORT).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { deletingProduct = null }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      }
    )
  }

  // 4. Manage Categories Dialog
  if (showManageCategoriesDialog) {
    ManageCategoriesDialog(
      categories = categories,
      products = products,
      currentUser = currentUser,
      onAddCategory = onAddCategory,
      onUpdateCategory = onUpdateCategory,
      onDeleteCategory = onDeleteCategory,
      onReorderCategories = onReorderCategories,
      onDismiss = { showManageCategoriesDialog = false }
    )
  }

  // 5. Manage Columns Dialog
  if (showManageColumnsDialog) {
    ManageColumnsDialog(
      columns = if (inventoryColumns.isEmpty()) InventoryColumnConfig.DEFAULT_COLUMNS else inventoryColumns,
      onSaveColumns = onSaveColumns,
      onAddCustomField = onAddCustomField,
      onRenameCustomField = onRenameCustomField,
      onDeleteCustomField = onDeleteCustomField,
      onDismiss = { showManageColumnsDialog = false }
    )
  }

  // 6. Inventory History Dialog
  if (showHistoryDialog) {
    InventoryHistoryDialog(
      history = inventoryHistory,
      auditLogs = auditLogs,
      auditCleanupRuns = auditCleanupRuns,
      isAuditLoading = isAuditLoading,
      isRetentionSimulating = isRetentionSimulating,
      latestRetentionSimulation = latestRetentionSimulation,
      onRefreshAudit = onRefreshAudit,
      onRunRetentionSimulation = onRunRetentionSimulation,
      onDismiss = { showHistoryDialog = false }
    )
  }

  // 7. Download / Export Dialog
  if (showExportDialog) {
    InventoryExportDialog(
      allProducts = products,
      filteredProducts = filteredProducts,
      selectedCategoryName = selectedCategoryName,
      summaryFilter = statusFilter,
      searchQuery = debouncedSearchQuery,
      onExportDone = onLogExport,
      onDismiss = { showExportDialog = false }
    )
  }

  // 8. Inventory Configuration Dialog
  if (showConfigDialog) {
    InventoryConfigDialog(
      onOpenManageCategories = { showManageCategoriesDialog = true },
      onOpenManageColumns = { showManageColumnsDialog = true },
      onOpenImportInventory = { showImportDialog = true },
      onOpenExportInventory = { showExportDialog = true },
      onDismiss = { showConfigDialog = false }
    )
  }

  // 9. Excel & CSV Inventory Import Dialog
  if (showImportDialog) {
    InventoryImportDialog(
      onDismiss = { showImportDialog = false },
      onImportConfirmed = { importProducts, policy, fileType, totalRows, onResult ->
        onImportProductsBatch(importProducts, policy, fileType, totalRows) { success, imported, skipped, errorMsg ->
          onResult(success, imported, skipped, errorMsg)
        }
      }
    )
  }

  // 10. Dual Product Scanner Dialog
  if (showScannerDialog) {
    DualProductScannerDialog(
      products = products,
      onSelectProduct = { prod ->
        searchInput = prod.name
      },
      onAddNewProductWithBarcode = { barcode ->
        scannedInitialBarcode = barcode
        aiScannedInitialProduct = null
        showAddProductDialog = true
      },
      onAddNewProductFromAi = { name, chem, comp, cat, pkg, unit, batch, exp, barcode, crop, uses ->
        aiScannedInitialProduct = Product(
          id = "",
          name = name,
          chemicalComposition = chem,
          company = comp,
          category = cat,
          packaging = pkg,
          unit = unit,
          batchNumber = batch,
          purchasePrice = 0.0,
          sellingPrice = 0.0,
          mrp = 0.0,
          stockQuantity = 0.0,
          expiryDate = exp,
          barcode = barcode,
          crop = crop,
          usesInstructions = uses
        )
        scannedInitialBarcode = barcode
        showAddProductDialog = true
      },
      onDismiss = { showScannerDialog = false }
    )
  }
}

@Composable
private fun RenderTableCellContent(
  columnId: String,
  product: Product,
  isLowStock: Boolean,
  isExpired: Boolean,
  isNearExpiry: Boolean,
  expiryEval: AlertEngine.ExpiryEvaluation,
  currencyFormat: NumberFormat
) {
  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

  when (columnId) {
    "name" -> {
      // Deterministic Status Priority for Product Name in Inventory table:
      // 1. Expired -> Red (SoftRed) + Star Icon ⭐
      // 2. Expiry <= 1 Month (High Priority) -> Red (SoftRed)
      // 3. Expiry <= 3 Months -> Yellow (GoldAmber)
      // 4. Expiry <= 6 Months -> Yellow (GoldAmber)
      // 5. Low Stock -> Blue (InfoSky)
      // 6. Healthy -> Green (Emerald400)
      val statusInfo = remember(product.id, product.stockQuantity, product.minStockAlert, product.expiryDate) {
        AlertEngine.evaluateProductStatus(product)
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (statusInfo.showStar) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Expired warning",
            tint = GoldAmber,
            modifier = Modifier.size(13.dp)
          )
        }
        Text(
          text = product.name,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 16.sp
          ),
          color = statusInfo.nameColor
        )
      }
    }

    "chemicalComposition" -> {
      // Chemical Composition is never clipped, cleanly wraps within adequate cell width
      Text(
        text = product.chemicalComposition.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Normal,
          lineHeight = 15.sp
        ),
        color = TextPrimaryDark
      )
    }

    "company" -> {
      Text(
        text = product.company.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
        color = TextSecondaryDark
      )
    }

    "packaging" -> {
      Text(
        text = product.packaging.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = TextSecondaryDark
      )
    }

    "category" -> {
      Text(
        text = product.category.ifBlank { "General" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = TextSecondaryDark
      )
    }

    "price" -> {
      Text(
        text = currencyFormat.format(product.sellingPrice),
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        ),
        color = TextPrimaryDark
      )
    }

    "purchasePrice" -> {
      Text(
        text = if (product.purchasePrice > 0) currencyFormat.format(product.purchasePrice) else "—",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = TextSecondaryDark
      )
    }

    "mrp" -> {
      Text(
        text = if (product.mrp > 0) currencyFormat.format(product.mrp) else "—",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = TextSecondaryDark
      )
    }

    "stockQuantity" -> {
      Text(
        text = "${product.stockQuantity.toInt()}",
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        ),
        color = if (isLowStock) InfoSky else Emerald400
      )
    }

    "minStockAlert" -> {
      Text(
        text = "${product.minStockAlert.toInt()}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = TextMutedDark
      )
    }

    "expiryDate" -> {
      val expText = if (product.expiryDate != null) dateFormat.format(Date(product.expiryDate)) else "—"
      val textColor = when {
        isExpired -> SoftRed
        isNearExpiry -> GoldAmber
        else -> TextSecondaryDark
      }
      Text(
        text = expText,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.5.sp,
          fontWeight = if (isExpired || isNearExpiry) FontWeight.Bold else FontWeight.Normal
        ),
        color = textColor
      )
    }

    "barcode" -> {
      Text(
        text = product.barcode.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium
        ),
        color = TextPrimaryDark
      )
    }

    "batchNumber" -> {
      Text(
        text = product.batchNumber.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextSecondaryDark
      )
    }

    "crop" -> {
      Text(
        text = product.crop.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
        color = TextSecondaryDark
      )
    }

    "usesInstructions" -> {
      Text(
        text = product.usesInstructions.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp),
        color = TextMutedDark
      )
    }

    "rackLocation" -> {
      Text(
        text = product.rackLocation.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextSecondaryDark
      )
    }

    "hsnCode" -> {
      Text(
        text = product.hsnCode.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextMutedDark
      )
    }

    else -> {
      // Dynamic Custom Field lookup from JSON
      val customVal = remember(product.customFields, columnId) {
        try {
          if (product.customFields.isNotBlank()) {
            val json = JSONObject(product.customFields)
            json.optString(columnId, "—")
          } else {
            "—"
          }
        } catch (e: Exception) {
          "—"
        }
      }
      Text(
        text = customVal.ifBlank { "—" },
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextSecondaryDark
      )
    }
  }
}
