package com.manglamfertilizer.app.ui.inventory

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.CategoryItem
import com.manglamfertilizer.app.data.model.InventoryColumnConfig
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONObject

@Composable
fun AddEditProductDialog(
  product: Product?,
  categories: List<CategoryItem>,
  initialBarcode: String = "",
  initialProductData: Product? = null,
  customColumns: List<InventoryColumnConfig> = emptyList(),
  onDismiss: () -> Unit,
  onOpenAddCategory: () -> Unit,
  onSubmit: (
    name: String,
    category: String,
    company: String,
    unit: ProductUnit,
    batch: String,
    pPrice: Double,
    sPrice: Double,
    mrp: Double,
    stock: Double,
    minAlert: Double,
    expDate: Long?,
    rack: String,
    hsn: String,
    chemicalComposition: String,
    barcode: String,
    packaging: String,
    crop: String,
    usesInstructions: String,
    customFields: String,
    onDone: (Boolean, String?) -> Unit
  ) -> Unit
) {
  val context = LocalContext.current
  val isEditing = product != null
  val sourceProduct = product ?: initialProductData

  var name by remember { mutableStateOf(sourceProduct?.name ?: "") }
  var chemicalComposition by remember { mutableStateOf(sourceProduct?.chemicalComposition ?: "") }
  var company by remember { mutableStateOf(sourceProduct?.company ?: "") }
  var selectedCategory by remember { mutableStateOf(sourceProduct?.category?.ifBlank { null } ?: (categories.firstOrNull()?.name ?: "")) }
  var packaging by remember { mutableStateOf(sourceProduct?.packaging ?: "") }
  val unit by remember { mutableStateOf(sourceProduct?.unit ?: ProductUnit.BAG) }
  val crop by remember { mutableStateOf(sourceProduct?.crop ?: "") }
  var usesInstructions by remember { mutableStateOf(sourceProduct?.usesInstructions ?: "") }
  var barcode by remember { mutableStateOf(sourceProduct?.barcode?.ifBlank { initialBarcode } ?: initialBarcode) }
  val batchNumber by remember { mutableStateOf(sourceProduct?.batchNumber ?: "") }
  val purchasePriceText by remember { mutableStateOf(sourceProduct?.purchasePrice?.let { if (it > 0) it.toString() else "" } ?: "") }
  var sellingPriceText by remember { mutableStateOf(sourceProduct?.sellingPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
  val mrpText by remember { mutableStateOf(sourceProduct?.mrp?.let { if (it > 0) it.toString() else "" } ?: "") }
  var stockText by remember { mutableStateOf(sourceProduct?.stockQuantity?.let { if (it > 0) it.toString() else "" } ?: "") }
  var minAlertText by remember { mutableStateOf(sourceProduct?.minStockAlert?.toString() ?: "10") }
  var expiryDate by remember { mutableStateOf<Long?>(sourceProduct?.expiryDate) }
  var rackLocation by remember { mutableStateOf(sourceProduct?.rackLocation ?: "") }
  val hsnCode by remember { mutableStateOf(sourceProduct?.hsnCode ?: "") }

  // Custom fields map
  val activeCustomFields = remember(customColumns) { customColumns.filter { it.isCustom } }
  val initialCustomMap = remember(sourceProduct?.customFields) {
    val map = mutableMapOf<String, String>()
    val raw = sourceProduct?.customFields ?: ""
    if (raw.isNotBlank()) {
      try {
        val obj = JSONObject(raw)
        obj.keys().forEach { key ->
          map[key] = obj.optString(key, "")
        }
      } catch (e: Exception) {
        // fallback
      }
    }
    map
  }
  var customValuesMap by remember { mutableStateOf(initialCustomMap.toMutableMap()) }

  var isSaving by remember { mutableStateOf(false) }
  var errorMsg by remember { mutableStateOf<String?>(null) }
  var showScanner by remember { mutableStateOf(false) }
  var categoryDropdownExpanded by remember { mutableStateOf(false) }

  val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

  val performSave: () -> Unit = {
    if (name.isBlank()) {
      errorMsg = "Product Name is required"
    } else {
      val sPrice = sellingPriceText.toDoubleOrNull()
      if (sPrice == null || sPrice < 0) {
        errorMsg = "Please enter a valid Price (₹)"
      } else {
        val stock = stockText.toDoubleOrNull()
        if (stock == null || stock < 0) {
          errorMsg = "Please enter a valid Quantity"
        } else {
          val minAlert = minAlertText.toDoubleOrNull()
          if (minAlert == null || minAlert < 0) {
            errorMsg = "Please enter a valid Minimum Stock"
          } else {
            val pPrice = purchasePriceText.toDoubleOrNull() ?: sPrice
            val mrp = mrpText.toDoubleOrNull() ?: sPrice

            val serializedCustomJson = try {
              val obj = JSONObject()
              customValuesMap.forEach { (k, v) ->
                if (v.isNotBlank()) obj.put(k, v.trim())
              }
              obj.toString()
            } catch (e: Exception) {
              ""
            }

            isSaving = true
            errorMsg = null
            onSubmit(
              name.trim(),
              selectedCategory.trim(),
              company.trim(),
              unit,
              batchNumber.trim(),
              pPrice,
              sPrice,
              mrp,
              stock,
              minAlert,
              expiryDate,
              rackLocation.trim(),
              hsnCode.trim(),
              chemicalComposition.trim(),
              barcode.trim(),
              packaging.trim(),
              crop.trim(),
              usesInstructions.trim(),
              serializedCustomJson
            ) { success, msg ->
              isSaving = false
              if (!success) {
                errorMsg = msg ?: "Unable to save product to cloud."
              }
            }
          }
        }
      }
    }
  }

  Dialog(
    onDismissRequest = { if (!isSaving) onDismiss() },
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
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 680.dp)
          .testTag("add_edit_product_dialog")
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp)
      ) {
        // Dialog Top Bar (Pinned at top)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Emerald400.copy(alpha = 0.15f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.Inventory,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Column {
              Text(
                text = if (isEditing) "Edit Product" else "Add New Product",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryDark
              )
              Text(
                text = if (isEditing) "Update inventory record & specifications" else "Create complete agricultural stock entry",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondaryDark
              )
            }
          }

          IconButton(
            onClick = { if (!isSaving) onDismiss() },
            modifier = Modifier.size(32.dp)
          ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Vertically Scrollable Form Body
        LazyColumn(
          modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Row 1: Product Name * | Company / Brand
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = name,
                onValueChange = { name = it; errorMsg = null },
                label = { Text("Product Name *", fontSize = 12.sp) },
                placeholder = { Text("e.g. Urea 46% N, DAP...", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_name_input")
              )

              OutlinedTextField(
                value = company,
                onValueChange = { company = it; errorMsg = null },
                label = { Text("Company / Brand", fontSize = 12.sp) },
                placeholder = { Text("e.g. IFFCO, Bayer...", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_company_input")
              )
            }
          }

          // Row 2: Chemical Composition | Product Category
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.Top
            ) {
              OutlinedTextField(
                value = chemicalComposition,
                onValueChange = { chemicalComposition = it },
                label = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = Emerald400, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Chemical Composition", fontSize = 12.sp)
                  }
                },
                placeholder = { Text("e.g. Thiamethoxam 12.6%...", fontSize = 12.sp) },
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_chemical_input")
              )

              // Product Category Dropdown
              Column(modifier = Modifier.weight(1f)) {
                Text("Product Category", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                  Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(52.dp)
                      .clip(RoundedCornerShape(8.dp))
                      .clickable { categoryDropdownExpanded = true }
                      .testTag("category_dropdown_trigger")
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Text(
                        text = if (selectedCategory.isNotBlank()) selectedCategory else "Select Category",
                        color = if (selectedCategory.isNotBlank()) TextPrimaryDark else TextMutedDark,
                        fontSize = 13.sp
                      )
                      Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark)
                    }
                  }

                  DropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                  ) {
                    DropdownMenuItem(
                      text = { Text("Uncategorized", color = TextSecondaryDark) },
                      onClick = {
                        selectedCategory = ""
                        categoryDropdownExpanded = false
                      }
                    )
                    categories.forEach { cat ->
                      DropdownMenuItem(
                        text = { Text(cat.name, color = TextPrimaryDark) },
                        onClick = {
                          selectedCategory = cat.name
                          categoryDropdownExpanded = false
                        }
                      )
                    }
                    DropdownMenuItem(
                      text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Icon(Icons.Default.Add, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                          Spacer(modifier = Modifier.width(6.dp))
                          Text("Manage Categories", color = Emerald400, fontWeight = FontWeight.Bold)
                        }
                      },
                      onClick = {
                        categoryDropdownExpanded = false
                        onOpenAddCategory()
                      }
                    )
                  }
                }
              }
            }
          }

          // Row 3: Packaging | Price (₹) *
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = packaging,
                onValueChange = { packaging = it },
                label = { Text("Packaging", fontSize = 12.sp) },
                placeholder = { Text("e.g. 500 ml, 50 kg Bag", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_packaging_input")
              )

              OutlinedTextField(
                value = sellingPriceText,
                onValueChange = { sellingPriceText = it; errorMsg = null },
                label = { Text("Price (₹) *", fontSize = 12.sp) },
                placeholder = { Text("e.g. 520", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_selling_price_input")
              )
            }
          }

          // Row 4: Quantity * | Minimum Stock *
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = stockText,
                onValueChange = { stockText = it; errorMsg = null },
                label = { Text("Quantity *", fontSize = 12.sp) },
                placeholder = { Text("e.g. 100", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_stock_input")
              )

              OutlinedTextField(
                value = minAlertText,
                onValueChange = { minAlertText = it; errorMsg = null },
                label = { Text("Minimum Stock *", fontSize = 12.sp) },
                placeholder = { Text("e.g. 10", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_min_stock_input")
              )
            }
          }

          // Row 5: Expiry Date | Barcode / SKU
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.Top
            ) {
              // Expiry Date Picker Box
              Column(modifier = Modifier.weight(1f)) {
                Text("Expiry Date", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = DarkSurfaceElevated,
                  border = BorderStroke(1.dp, DarkBorder),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      val cal = Calendar.getInstance()
                      expiryDate?.let { exp -> cal.timeInMillis = exp }
                      DatePickerDialog(
                        context,
                        { _, y, m, d ->
                          val picked = Calendar.getInstance().apply {
                            set(y, m, d, 23, 59, 59)
                          }.timeInMillis
                          expiryDate = picked
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                      ).show()
                    }
                    .testTag("expiry_date_picker_btn")
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = expiryDate?.let { dateFormat.format(Date(it)) } ?: "Select Date",
                      color = if (expiryDate != null) TextPrimaryDark else TextMutedDark,
                      fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (expiryDate != null) {
                        IconButton(
                          onClick = { expiryDate = null },
                          modifier = Modifier.size(20.dp)
                        ) {
                          Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMutedDark, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                      }
                      Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                    }
                  }
                }
              }

              // Barcode / SKU
              OutlinedTextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text("Barcode / SKU", fontSize = 12.sp) },
                placeholder = { Text("Manual or scan", fontSize = 12.sp) },
                singleLine = true,
                trailingIcon = {
                  IconButton(onClick = { showScanner = true }) {
                    Icon(
                      imageVector = Icons.Default.QrCodeScanner,
                      contentDescription = "Scan Barcode",
                      tint = Emerald400,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("product_barcode_input")
              )
            }
          }

          // Row 6: Uses / Instructions | Rack / Location
          item {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              OutlinedTextField(
                value = usesInstructions,
                onValueChange = { usesInstructions = it },
                label = { Text("Uses / Instructions", fontSize = 12.sp) },
                placeholder = { Text("e.g. 2 ml/L foliar spray...", fontSize = 12.sp) },
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(1.1f)
                  .testTag("product_instructions_input")
              )

              OutlinedTextField(
                value = rackLocation,
                onValueChange = { rackLocation = it },
                label = { Text("Rack / Location", fontSize = 12.sp) },
                placeholder = { Text("e.g. Bay A-12", fontSize = 12.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark,
                  focusedContainerColor = DarkSurfaceElevated,
                  unfocusedContainerColor = DarkSurfaceElevated,
                  focusedLabelColor = Emerald400,
                  unfocusedLabelColor = TextSecondaryDark
                ),
                modifier = Modifier
                  .weight(0.9f)
                  .testTag("product_rack_input")
              )
            }
          }

          // Custom Attributes Section (if any configured)
          if (activeCustomFields.isNotEmpty()) {
            item {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "CUSTOM ATTRIBUTES",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                ),
                color = TextSecondaryDark
              )
            }

            // Group custom fields in 2 columns
            val chunked = activeCustomFields.chunked(2)
            items(chunked.size) { chunkIdx ->
              val pair = chunked[chunkIdx]
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                val field1 = pair[0]
                val currentVal1 = customValuesMap[field1.id] ?: ""
                OutlinedTextField(
                  value = currentVal1,
                  onValueChange = { newVal ->
                    val updated = customValuesMap.toMutableMap()
                    updated[field1.id] = newVal
                    customValuesMap = updated
                  },
                  label = { Text(field1.title, fontSize = 12.sp) },
                  placeholder = { Text("Enter ${field1.title}", fontSize = 12.sp) },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFA78BFA),
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedLabelColor = Color(0xFFA78BFA),
                    unfocusedLabelColor = TextSecondaryDark
                  ),
                  modifier = Modifier.weight(1f)
                )

                if (pair.size > 1) {
                  val field2 = pair[1]
                  val currentVal2 = customValuesMap[field2.id] ?: ""
                  OutlinedTextField(
                    value = currentVal2,
                    onValueChange = { newVal ->
                      val updated = customValuesMap.toMutableMap()
                      updated[field2.id] = newVal
                      customValuesMap = updated
                    },
                    label = { Text(field2.title, fontSize = 12.sp) },
                    placeholder = { Text("Enter ${field2.title}", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                      focusedBorderColor = Color(0xFFA78BFA),
                      unfocusedBorderColor = DarkBorder,
                      focusedTextColor = TextPrimaryDark,
                      unfocusedTextColor = TextPrimaryDark,
                      focusedContainerColor = DarkSurfaceElevated,
                      unfocusedContainerColor = DarkSurfaceElevated,
                      focusedLabelColor = Color(0xFFA78BFA),
                      unfocusedLabelColor = TextSecondaryDark
                    ),
                    modifier = Modifier.weight(1f)
                  )
                } else {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          // Error Display with explicit Retry Action
          if (errorMsg != null) {
            item {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SoftRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = errorMsg ?: "",
                    color = SoftRed,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                    onClick = {
                      if (!isSaving) {
                        performSave()
                      }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                  ) {
                    Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pinned Actions Bottom Bar (Cancel & Save Product)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = onDismiss,
            enabled = !isSaving,
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", color = TextSecondaryDark)
          }

          Button(
            onClick = {
              performSave()
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .weight(2f)
              .testTag("save_product_submit_btn")
          ) {
            if (isSaving) {
              CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Text(
                text = if (isEditing) "Update Product" else "Save Product",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }
          }
        }
      }
    }
  }
}

  // Barcode Scanner / Generator Dialog
  if (showScanner) {
    BarcodeScanInputDialog(
      currentBarcode = barcode,
      onBarcodeConfirmed = { code ->
        barcode = code
        showScanner = false
      },
      onDismiss = { showScanner = false }
    )
  }
}

@Composable
private fun BarcodeScanInputDialog(
  currentBarcode: String,
  onBarcodeConfirmed: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var codeText by remember { mutableStateOf(currentBarcode) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DarkSurface,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("barcode_scan_input_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Emerald400, modifier = Modifier.size(24.dp))
            Text("Scan or Generate Barcode", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimaryDark)
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner viewfinder visual
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DarkBg,
          border = BorderStroke(1.5.dp, Emerald400),
          modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Emerald400, modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(4.dp))
              Text("Ready to read 1D / 2D Barcodes", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = codeText,
          onValueChange = { codeText = it },
          label = { Text("Barcode / SKU Number") },
          placeholder = { Text("e.g. 8901234567890") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkCard,
            unfocusedContainerColor = DarkCard
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Generate EAN button
        Button(
          onClick = {
            val randomDigits = (1000000000L..9999999999L).random()
            codeText = "890$randomDigits"
          },
          colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Emerald400),
          border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("⚡ Auto-Generate 13-Digit EAN Barcode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          TextButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", color = TextSecondaryDark)
          }

          Button(
            onClick = {
              if (codeText.isNotBlank()) {
                onBarcodeConfirmed(codeText.trim())
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1.5f)
          ) {
            Text("Apply Barcode", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
