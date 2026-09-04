package com.manglamfertilizer.app.ui.inventory

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.repository.DuplicateHandlingPolicy
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipInputStream

enum class ImportStep {
  SELECT_FILE,
  PREVIEW_AND_MAP,
  VALIDATE_AND_POLICY,
  IMPORTING
}

enum class AppFieldKey(val displayName: String, val required: Boolean, val sampleValues: String) {
  NAME("Product Name", true, "Urea 46% N, DAP 18:46:0, Karate 5 EC"),
  COMPANY("Company / Brand", true, "IFFCO, Syngenta, Bayer, Coromandel"),
  CHEMICAL("Chemical Composition", false, "Thiamethoxam 12.6% + Lambdacyhalothrin 9.5%"),
  CATEGORY("Category", false, "Fertilizers, Insecticides, Seeds, Micronutrients"),
  PACKAGING("Packaging", false, "500 ml, 1 kg, 45 kg Bag"),
  UNIT("Unit", false, "BAG, BOTTLE, KG, LITER, PACKET, PIECE"),
  SELLING_PRICE("Price (Sale ₹)", true, "266.5, 520, 1350"),
  PURCHASE_PRICE("Buy Price (₹)", false, "240, 450, 1200"),
  MRP("MRP (₹)", false, "266.5, 550, 1350"),
  QUANTITY("Quantity", true, "150, 25, 5"),
  MIN_STOCK("Minimum Stock", false, "20, 10, 5"),
  EXPIRY_DATE("Expiry Date", false, "31/12/2026, 2026-12-31"),
  BARCODE("Barcode / SKU", false, "890123450001, 890123450002"),
  CROP("Crop", false, "Wheat, Rice, Cotton, Tomato"),
  USES_INSTRUCTIONS("Uses / Instructions", false, "2 ml/L water foliar spray"),
  BATCH_NUMBER("Batch Number", false, "B2026-01, SYN-991"),
  RACK_LOCATION("Rack Location", false, "A-12, Bay 3"),
  HSN_CODE("HSN Code", false, "3102, 3808")
}

data class RowValidationError(
  val rowIndex: Int,
  val fieldName: String,
  val error: String
)

data class ParsedProductRow(
  val originalIndex: Int,
  val product: Product?,
  val errors: List<RowValidationError>
)

@Composable
fun InventoryImportDialog(
  onDismiss: () -> Unit,
  onImportConfirmed: (List<Product>, DuplicateHandlingPolicy, String, Int, (Boolean, Int, Int, String?) -> Unit) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var currentStep by remember { mutableStateOf(ImportStep.SELECT_FILE) }
  var fileType by remember { mutableStateOf("CSV") }
  var fileName by remember { mutableStateOf("") }
  var isParsing by remember { mutableStateOf(false) }
  var parseErrorMessage by remember { mutableStateOf<String?>(null) }

  // Parsed Raw Data
  var rawHeaders by remember { mutableStateOf<List<String>>(emptyList()) }
  var rawRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }

  // Field Mapping: AppFieldKey -> Selected Column Header Name (or "" for unmapped)
  val fieldMappings = remember { mutableStateMapOf<AppFieldKey, String>() }

  // Validation results
  var validatedRows by remember { mutableStateOf<List<ParsedProductRow>>(emptyList()) }
  var validCount by remember { mutableStateOf(0) }
  var errorCount by remember { mutableStateOf(0) }
  var duplicatePolicy by remember { mutableStateOf(DuplicateHandlingPolicy.UPDATE_EXISTING) }

  // Import Execution State
  var isImporting by remember { mutableStateOf(false) }
  var importResultSuccess by remember { mutableStateOf<Boolean?>(null) }
  var importResultMessage by remember { mutableStateOf("") }
  var importedCountResult by remember { mutableStateOf(0) }
  var skippedCountResult by remember { mutableStateOf(0) }

  // Manual Paste State
  var showPasteInput by remember { mutableStateOf(false) }
  var pastedText by remember { mutableStateOf("") }

  // File Picker Launcher
  val fileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      coroutineScope.launch {
        isParsing = true
        parseErrorMessage = null
        try {
          val cr = context.contentResolver
          val type = cr.getType(uri) ?: ""
          val isXlsx = uri.toString().endsWith(".xlsx", ignoreCase = true) ||
              type.contains("spreadsheetml", ignoreCase = true) ||
              type.contains("openxmlformats", ignoreCase = true)

          fileType = if (isXlsx) "XLSX" else "CSV"
          fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "spreadsheet_import"

          val parsed = withContext(Dispatchers.IO) {
            cr.openInputStream(uri)?.use { stream ->
              if (isXlsx) {
                parseXlsxStream(stream)
              } else {
                parseCsvStream(stream)
              }
            } ?: Pair(emptyList(), emptyList())
          }

          if (parsed.first.isEmpty() || parsed.second.isEmpty()) {
            parseErrorMessage = "No data rows found in the selected file."
            isParsing = false
          } else {
            rawHeaders = parsed.first
            rawRows = parsed.second
            autoMapFields(rawHeaders, fieldMappings)
            currentStep = ImportStep.PREVIEW_AND_MAP
            isParsing = false
          }
        } catch (e: Exception) {
          parseErrorMessage = "Failed to parse file: ${e.localizedMessage ?: "Unknown error"}"
          isParsing = false
        }
      }
    }
  }

  Dialog(
    onDismissRequest = { if (!isImporting) onDismiss() },
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
          .fillMaxHeight(0.92f)
          .testTag("inventory_import_dialog")
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight()
          .padding(20.dp)
      ) {
        // Dialog Header
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
              color = Color(0xFFA78BFA).copy(alpha = 0.15f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.FileUpload,
                  contentDescription = null,
                  tint = Color(0xFFA78BFA),
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Column {
              Text(
                text = "Excel / CSV Inventory Import",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryDark
              )
              Text(
                text = when (currentStep) {
                  ImportStep.SELECT_FILE -> "Step 1/3: Choose file or paste spreadsheet data"
                  ImportStep.PREVIEW_AND_MAP -> "Step 2/3: Detect headers & configure column mapping"
                  ImportStep.VALIDATE_AND_POLICY -> "Step 3/3: Review validation & duplicate policy"
                  ImportStep.IMPORTING -> "Importing product catalog..."
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondaryDark
              )
            }
          }

          if (!isImporting) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Step Progress Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          StepProgressIndicator(title = "1. Select", active = currentStep == ImportStep.SELECT_FILE, completed = currentStep > ImportStep.SELECT_FILE, modifier = Modifier.weight(1f))
          StepProgressIndicator(title = "2. Map Columns", active = currentStep == ImportStep.PREVIEW_AND_MAP, completed = currentStep > ImportStep.PREVIEW_AND_MAP, modifier = Modifier.weight(1f))
          StepProgressIndicator(title = "3. Validate & Import", active = currentStep == ImportStep.VALIDATE_AND_POLICY || currentStep == ImportStep.IMPORTING, completed = importResultSuccess == true, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STEP 1: SELECT FILE OR PASTE
        if (currentStep == ImportStep.SELECT_FILE) {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Text(
              text = "Import agricultural products seamlessly from Microsoft Excel (.xlsx) or CSV (.csv) spreadsheets.",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
              color = TextSecondaryDark
            )

            // File selection action cards
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Select Excel XLSX
              ImportOptionButton(
                title = "Select Excel (.xlsx)",
                subtitle = "Read Excel workbook",
                badge = ".xlsx",
                icon = Icons.Default.TableChart,
                badgeColor = Emerald400,
                modifier = Modifier.weight(1f),
                onClick = {
                  fileLauncher.launch("*/*")
                }
              )

              // Select CSV
              ImportOptionButton(
                title = "Select CSV (.csv)",
                subtitle = "Comma-separated sheet",
                badge = ".csv",
                icon = Icons.Default.UploadFile,
                badgeColor = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f),
                onClick = {
                  fileLauncher.launch("*/*")
                }
              )
            }

            // Quick Demo & Paste Options
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Load Sample Agricultural Template
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    val sample = getSampleFertilizerCsv()
                    fileType = "CSV (Sample)"
                    fileName = "mangalam_inventory_template.csv"
                    val parsed = parseCsvString(sample)
                    rawHeaders = parsed.first
                    rawRows = parsed.second
                    autoMapFields(rawHeaders, fieldMappings)
                    currentStep = ImportStep.PREVIEW_AND_MAP
                  }
                  .testTag("load_sample_template_btn")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(20.dp))
                  Column {
                    Text("Load Sample Sheet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryDark)
                    Text("Pre-filled fertilizer & pesticide data", fontSize = 11.sp, color = TextSecondaryDark)
                  }
                }
              }

              // Paste CSV / Tab-separated text
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(12.dp))
                  .clickable { showPasteInput = !showPasteInput }
                  .testTag("paste_csv_text_btn")
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(20.dp))
                  Column {
                    Text("Paste Table / CSV", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryDark)
                    Text("Paste copied sheet text", fontSize = 11.sp, color = TextSecondaryDark)
                  }
                }
              }
            }

            if (showPasteInput) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                OutlinedTextField(
                  value = pastedText,
                  onValueChange = { pastedText = it },
                  placeholder = { Text("Paste CSV or Tab-Separated spreadsheet text here...\nFirst row should contain column headings.") },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("paste_spreadsheet_input"),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald400,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated
                  )
                )

                Button(
                  onClick = {
                    if (pastedText.isNotBlank()) {
                      fileType = "CSV (Pasted)"
                      fileName = "pasted_spreadsheet.csv"
                      val parsed = parseCsvString(pastedText)
                      if (parsed.first.isNotEmpty() && parsed.second.isNotEmpty()) {
                        rawHeaders = parsed.first
                        rawRows = parsed.second
                        autoMapFields(rawHeaders, fieldMappings)
                        currentStep = ImportStep.PREVIEW_AND_MAP
                      } else {
                        parseErrorMessage = "Unable to parse headers from pasted text"
                      }
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
                  modifier = Modifier.align(Alignment.End)
                ) {
                  Text("Process Pasted Data", fontWeight = FontWeight.Bold)
                }
              }
            }

            if (isParsing) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                CircularProgressIndicator(color = Emerald400, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Reading and parsing workbook...", color = TextSecondaryDark)
              }
            }

            if (parseErrorMessage != null) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = SoftRed.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SoftRed)
                  Text(parseErrorMessage ?: "", color = SoftRed, fontSize = 13.sp)
                }
              }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Supported Headers Guidance
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = DarkSurfaceElevated,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "Supported Columns / Headings:",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = TextPrimaryDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Product Name, Company, Chemical Composition, Packaging, Category, Price, Quantity, Minimum Stock, Expiry Date, Barcode, Crop, Uses / Instructions, Batch, Rack, HSN.",
                  fontSize = 11.sp,
                  color = TextMutedDark,
                  lineHeight = 16.sp
                )
              }
            }
          }
        }

        // STEP 2: PREVIEW & MAP COLUMNS
        else if (currentStep == ImportStep.PREVIEW_AND_MAP) {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Stats & File Banner
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = DarkCard,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = fileName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimaryDark
                  )
                  Text(
                    text = "Detected ${rawRows.size} data rows across ${rawHeaders.size} columns ($fileType)",
                    fontSize = 11.sp,
                    color = Emerald400
                  )
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = Emerald400.copy(alpha = 0.15f),
                  border = BorderStroke(0.5.dp, Emerald400.copy(alpha = 0.4f))
                ) {
                  Text(
                    text = "${rawRows.size} Rows",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Emerald400,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
              }
            }

            Text(
              text = "Map the columns in your file to the corresponding ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} application fields:",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = TextSecondaryDark
            )

            // Column Mapping List
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              items(AppFieldKey.values()) { fieldKey ->
                ColumnMappingRow(
                  fieldKey = fieldKey,
                  headers = rawHeaders,
                  selectedHeader = fieldMappings[fieldKey] ?: "",
                  onSelectHeader = { selected ->
                    if (selected == "(None)") {
                      fieldMappings.remove(fieldKey)
                    } else {
                      fieldMappings[fieldKey] = selected
                    }
                  }
                )
              }
            }

            // Bottom Navigation for Step 2
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { currentStep = ImportStep.SELECT_FILE },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
              ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Back")
              }

              Button(
                onClick = {
                  // Perform validation
                  val validationResult = validateParsedRows(rawHeaders, rawRows, fieldMappings)
                  validatedRows = validationResult
                  validCount = validationResult.count { it.product != null && it.errors.isEmpty() }
                  errorCount = validationResult.size - validCount
                  currentStep = ImportStep.VALIDATE_AND_POLICY
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
                modifier = Modifier.testTag("proceed_to_validation_btn")
              ) {
                Text("Validate & Review (${rawRows.size} Rows)", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // STEP 3: VALIDATE & CHOOSE DUPLICATE POLICY
        else if (currentStep == ImportStep.VALIDATE_AND_POLICY) {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            // Validation Summary Cards (Total, Valid, Errors)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ValidationMetricCard(
                title = "Total Rows",
                count = rawRows.size,
                color = TextPrimaryDark,
                modifier = Modifier.weight(1f)
              )
              ValidationMetricCard(
                title = "Valid Rows",
                count = validCount,
                color = Emerald400,
                modifier = Modifier.weight(1f)
              )
              ValidationMetricCard(
                title = "Rows with Errors",
                count = errorCount,
                color = if (errorCount > 0) SoftRed else TextMutedDark,
                modifier = Modifier.weight(1f)
              )
            }

            // Duplicate Handling Policy Selector
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = DarkCard,
              border = BorderStroke(1.dp, DarkBorder),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "Duplicate Handling Policy",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = TextPrimaryDark
                )
                Text(
                  text = "Matching criteria: Barcode / SKU OR Product Name + Company",
                  fontSize = 11.sp,
                  color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                PolicyRadioOption(
                  title = "Update Existing Products",
                  description = "Update current inventory stock, prices, chemical formula, and details for matching products",
                  selected = duplicatePolicy == DuplicateHandlingPolicy.UPDATE_EXISTING,
                  onClick = { duplicatePolicy = DuplicateHandlingPolicy.UPDATE_EXISTING }
                )

                PolicyRadioOption(
                  title = "Skip Existing Products",
                  description = "Only insert new products; do not modify existing inventory records",
                  selected = duplicatePolicy == DuplicateHandlingPolicy.SKIP_EXISTING,
                  onClick = { duplicatePolicy = DuplicateHandlingPolicy.SKIP_EXISTING }
                )

                PolicyRadioOption(
                  title = "Create as New (Keep Both)",
                  description = "Create fresh product records with new IDs regardless of existing entries",
                  selected = duplicatePolicy == DuplicateHandlingPolicy.KEEP_BOTH,
                  onClick = { duplicatePolicy = DuplicateHandlingPolicy.KEEP_BOTH }
                )
              }
            }

            // Error List (if any errors found)
            if (errorCount > 0) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Validation Errors ($errorCount rows will be skipped):",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp,
                  color = SoftRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                  modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  val errorRows = validatedRows.filter { it.errors.isNotEmpty() }
                  items(errorRows) { row ->
                    Surface(
                      shape = RoundedCornerShape(6.dp),
                      color = DarkCard,
                      border = BorderStroke(0.5.dp, SoftRed.copy(alpha = 0.4f)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                          text = "Row ${row.originalIndex + 1}:",
                          fontWeight = FontWeight.Bold,
                          fontSize = 12.sp,
                          color = SoftRed
                        )
                        row.errors.forEach { err ->
                          Text(
                            text = "• ${err.fieldName}: ${err.error}",
                            fontSize = 11.sp,
                            color = TextSecondaryDark
                          )
                        }
                      }
                    }
                  }
                }
              }
            } else {
              // All valid banner
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = Emerald400.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400)
                  Text(
                    text = "All $validCount products validated successfully! Ready to import into database.",
                    color = Emerald400,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                  )
                }
              }
              Spacer(modifier = Modifier.weight(1f))
            }

            // Step 3 Navigation / Confirm Button
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              OutlinedButton(
                onClick = { currentStep = ImportStep.PREVIEW_AND_MAP },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
              ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Map Columns")
              }

              Button(
                onClick = {
                  val productsToImport = validatedRows.mapNotNull { it.product }
                  if (productsToImport.isEmpty()) {
                    return@Button
                  }
                  isImporting = true
                  currentStep = ImportStep.IMPORTING

                  onImportConfirmed(
                    productsToImport,
                    duplicatePolicy,
                    fileType,
                    rawRows.size
                  ) { success, imported, skipped, errorMsg ->
                    isImporting = false
                    importResultSuccess = success
                    importedCountResult = imported
                    skippedCountResult = skipped
                    importResultMessage = errorMsg ?: if (success) {
                      "Successfully imported $imported products ($skipped skipped) into inventory!"
                    } else {
                      "Import encountered an error"
                    }
                  }
                },
                enabled = validCount > 0 && !isImporting,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
                modifier = Modifier.testTag("confirm_import_final_btn")
              ) {
                Text("Confirm Import ($validCount Products)", fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        // STEP 4: IMPORTING / RESULT
        else if (currentStep == ImportStep.IMPORTING) {
          Column(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            if (isImporting) {
              CircularProgressIndicator(color = Emerald400, modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
              Spacer(modifier = Modifier.height(16.dp))
              Text("Importing products into Room database & Firestore...", fontWeight = FontWeight.Bold, color = TextPrimaryDark, fontSize = 15.sp)
              Text("Syncing stock counts, categories, and inventory history", color = TextSecondaryDark, fontSize = 12.sp)
            } else if (importResultSuccess == true) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald400, modifier = Modifier.size(56.dp))
              Spacer(modifier = Modifier.height(12.dp))
              Text("Import Successful!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimaryDark)
              Spacer(modifier = Modifier.height(4.dp))
              Text(importResultMessage, color = TextSecondaryDark, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(20.dp))
              Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
                modifier = Modifier.testTag("import_done_close_btn")
              ) {
                Text("View Inventory", fontWeight = FontWeight.Bold)
              }
            } else {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = SoftRed, modifier = Modifier.size(56.dp))
              Spacer(modifier = Modifier.height(12.dp))
              Text("Import Failed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SoftRed)
              Spacer(modifier = Modifier.height(4.dp))
              Text(importResultMessage, color = TextSecondaryDark, fontSize = 13.sp)
              Spacer(modifier = Modifier.height(20.dp))
              Button(
                onClick = { currentStep = ImportStep.VALIDATE_AND_POLICY },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = TextPrimaryDark)
              ) {
                Text("Try Again")
              }
            }
          }
        }
      }
    }
  }
}
}

@Composable
private fun StepProgressIndicator(
  title: String,
  active: Boolean,
  completed: Boolean,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = RoundedCornerShape(4.dp),
      color = when {
        completed -> Emerald400
        active -> Emerald400.copy(alpha = 0.8f)
        else -> DarkBorder
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(4.dp)
    ) {}
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.5.sp,
        fontWeight = if (active || completed) FontWeight.Bold else FontWeight.Normal
      ),
      color = if (active || completed) TextPrimaryDark else TextMutedDark
    )
  }
}

@Composable
private fun ImportOptionButton(
  title: String,
  subtitle: String,
  badge: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  badgeColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = badgeColor.copy(alpha = 0.15f),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
          }
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = badgeColor.copy(alpha = 0.15f),
          border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.4f))
        ) {
          Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextPrimaryDark)
      Text(subtitle, fontSize = 11.sp, color = TextSecondaryDark)
    }
  }
}

@Composable
private fun ColumnMappingRow(
  fieldKey: AppFieldKey,
  headers: List<String>,
  selectedHeader: String,
  onSelectHeader: (String) -> Unit
) {
  var expanded by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DarkCard,
    border = BorderStroke(0.5.dp, if (selectedHeader.isNotBlank()) Emerald400.copy(alpha = 0.4f) else DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1.1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = fieldKey.displayName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = TextPrimaryDark
          )
          if (fieldKey.required) {
            Text(" *", color = SoftRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          if (fieldKey == AppFieldKey.CHEMICAL) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Science, contentDescription = null, tint = Emerald400, modifier = Modifier.size(13.dp))
          }
        }
        Text(
          text = "e.g. ${fieldKey.sampleValues}",
          fontSize = 10.sp,
          color = TextMutedDark,
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Header Dropdown Selector
      Box(modifier = Modifier.weight(1f)) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, if (selectedHeader.isNotBlank()) Emerald400 else DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { expanded = true }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (selectedHeader.isNotBlank()) selectedHeader else "(Select Column)",
              fontSize = 12.sp,
              color = if (selectedHeader.isNotBlank()) Emerald400 else TextMutedDark,
              fontWeight = if (selectedHeader.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(18.dp))
          }
        }

        DropdownMenu(
          expanded = expanded,
          onDismissRequest = { expanded = false },
          modifier = Modifier.background(DarkSurfaceElevated)
        ) {
          DropdownMenuItem(
            text = { Text("(None / Skip)", color = TextSecondaryDark) },
            onClick = {
              onSelectHeader("(None)")
              expanded = false
            }
          )
          headers.forEach { header ->
            DropdownMenuItem(
              text = { Text(header, color = TextPrimaryDark) },
              onClick = {
                onSelectHeader(header)
                expanded = false
              }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ValidationMetricCard(
  title: String,
  count: Int,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count.toString(),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = color
      )
      Text(
        text = title,
        fontSize = 11.sp,
        color = TextSecondaryDark
      )
    }
  }
}

@Composable
private fun PolicyRadioOption(
  title: String,
  description: String,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = selected,
      onClick = onClick,
      colors = RadioButtonDefaults.colors(
        selectedColor = Emerald400,
        unselectedColor = TextSecondaryDark
      ),
      modifier = Modifier.size(24.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryDark)
      Text(description, fontSize = 11.sp, color = TextSecondaryDark)
    }
  }
}

// -------------------------------------------------------------
// SPREADSHEET PARSING & VALIDATION ENGINE
// -------------------------------------------------------------

private fun autoMapFields(headers: List<String>, fieldMappings: MutableMap<AppFieldKey, String>) {
  for (header in headers) {
    val h = header.trim().lowercase()
    when {
      // Product Name
      h.contains("product name") || h == "product" || h == "item name" || h == "item" || h == "name" -> {
        if (!fieldMappings.containsKey(AppFieldKey.NAME)) fieldMappings[AppFieldKey.NAME] = header
      }
      // Company / Brand
      h.contains("company") || h.contains("brand") || h.contains("manufacturer") || h.contains("mfg") -> {
        if (!fieldMappings.containsKey(AppFieldKey.COMPANY)) fieldMappings[AppFieldKey.COMPANY] = header
      }
      // Chemical Composition / Active Ingredient
      h.contains("chemical") || h.contains("composition") || h.contains("technical") || h.contains("active") || h.contains("formula") || h.contains("ingredient") -> {
        if (!fieldMappings.containsKey(AppFieldKey.CHEMICAL)) fieldMappings[AppFieldKey.CHEMICAL] = header
      }
      // Category
      h.contains("category") || h.contains("type") || h.contains("group") -> {
        if (!fieldMappings.containsKey(AppFieldKey.CATEGORY)) fieldMappings[AppFieldKey.CATEGORY] = header
      }
      // Packaging
      h.contains("packaging") || h.contains("pack size") || h.contains("packing") || h.contains("pack") -> {
        if (!fieldMappings.containsKey(AppFieldKey.PACKAGING)) fieldMappings[AppFieldKey.PACKAGING] = header
      }
      // Unit
      h == "unit" || h == "uom" -> {
        if (!fieldMappings.containsKey(AppFieldKey.UNIT)) fieldMappings[AppFieldKey.UNIT] = header
      }
      // Sale Price / Price
      h.contains("selling price") || h.contains("sale price") || h.contains("price") || h.contains("rate") -> {
        if (!fieldMappings.containsKey(AppFieldKey.SELLING_PRICE)) fieldMappings[AppFieldKey.SELLING_PRICE] = header
      }
      // Purchase Price / Buy Price
      h.contains("purchase price") || h.contains("buy price") || h.contains("cost price") || h.contains("cost") -> {
        if (!fieldMappings.containsKey(AppFieldKey.PURCHASE_PRICE)) fieldMappings[AppFieldKey.PURCHASE_PRICE] = header
      }
      // MRP
      h.contains("mrp") -> {
        if (!fieldMappings.containsKey(AppFieldKey.MRP)) fieldMappings[AppFieldKey.MRP] = header
      }
      // Quantity / Stock
      h.contains("quantity") || h.contains("qty") || h.contains("stock") || h.contains("current stock") -> {
        if (!fieldMappings.containsKey(AppFieldKey.QUANTITY)) fieldMappings[AppFieldKey.QUANTITY] = header
      }
      // Minimum Stock
      h.contains("min stock") || h.contains("minimum stock") || h.contains("alert stock") || h.contains("min alert") || h.contains("min qty") -> {
        if (!fieldMappings.containsKey(AppFieldKey.MIN_STOCK)) fieldMappings[AppFieldKey.MIN_STOCK] = header
      }
      // Expiry Date
      h.contains("expiry") || h.contains("exp date") || h.contains("exp") || h.contains("validity") -> {
        if (!fieldMappings.containsKey(AppFieldKey.EXPIRY_DATE)) fieldMappings[AppFieldKey.EXPIRY_DATE] = header
      }
      // Barcode / SKU
      h.contains("barcode") || h.contains("sku") || h.contains("upc") || h.contains("ean") || h.contains("code") -> {
        if (!fieldMappings.containsKey(AppFieldKey.BARCODE)) fieldMappings[AppFieldKey.BARCODE] = header
      }
      // Crop
      h.contains("crop") || h.contains("crops") || h.contains("target crop") -> {
        if (!fieldMappings.containsKey(AppFieldKey.CROP)) fieldMappings[AppFieldKey.CROP] = header
      }
      // Uses / Instructions
      h.contains("uses") || h.contains("instruction") || h.contains("dosage") || h.contains("application") -> {
        if (!fieldMappings.containsKey(AppFieldKey.USES_INSTRUCTIONS)) fieldMappings[AppFieldKey.USES_INSTRUCTIONS] = header
      }
      // Batch Number
      h.contains("batch") || h.contains("lot") -> {
        if (!fieldMappings.containsKey(AppFieldKey.BATCH_NUMBER)) fieldMappings[AppFieldKey.BATCH_NUMBER] = header
      }
      // Rack Location
      h.contains("rack") || h.contains("bay") || h.contains("location") || h.contains("shelf") -> {
        if (!fieldMappings.containsKey(AppFieldKey.RACK_LOCATION)) fieldMappings[AppFieldKey.RACK_LOCATION] = header
      }
      // HSN Code
      h.contains("hsn") -> {
        if (!fieldMappings.containsKey(AppFieldKey.HSN_CODE)) fieldMappings[AppFieldKey.HSN_CODE] = header
      }
    }
  }
}

private fun validateParsedRows(
  headers: List<String>,
  rows: List<List<String>>,
  mappings: Map<AppFieldKey, String>
): List<ParsedProductRow> {
  val headerIndexMap = headers.mapIndexed { index, name -> name to index }.toMap()
  val dateFormats = listOf(
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()),
    SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
  )

  return rows.mapIndexed { rowIndex, row ->
    val errors = mutableListOf<RowValidationError>()

    fun getVal(key: AppFieldKey): String {
      val colName = mappings[key] ?: return ""
      val idx = headerIndexMap[colName] ?: return ""
      return if (idx in row.indices) row[idx].trim() else ""
    }

    val name = getVal(AppFieldKey.NAME)
    if (name.isBlank()) {
      errors.add(RowValidationError(rowIndex, "Product Name", "Missing Product Name"))
    }

    val company = getVal(AppFieldKey.COMPANY)
    if (company.isBlank()) {
      errors.add(RowValidationError(rowIndex, "Company / Brand", "Missing Company"))
    }

    val priceStr = getVal(AppFieldKey.SELLING_PRICE).replace("₹", "").replace(",", "").trim()
    val sellingPrice = priceStr.toDoubleOrNull()
    if (sellingPrice == null || sellingPrice < 0) {
      errors.add(RowValidationError(rowIndex, "Price", if (priceStr.isBlank()) "Missing Price" else "Invalid numeric Price '$priceStr'"))
    }

    val qtyStr = getVal(AppFieldKey.QUANTITY).replace(",", "").trim()
    val quantity = qtyStr.toDoubleOrNull()
    if (quantity == null || quantity < 0) {
      errors.add(RowValidationError(rowIndex, "Quantity", if (qtyStr.isBlank()) "Missing Quantity" else "Invalid numeric Quantity '$qtyStr'"))
    }

    // Optional fields with safe fallbacks
    val pPriceStr = getVal(AppFieldKey.PURCHASE_PRICE).replace("₹", "").replace(",", "").trim()
    val purchasePrice = pPriceStr.toDoubleOrNull() ?: (sellingPrice ?: 0.0)

    val mrpStr = getVal(AppFieldKey.MRP).replace("₹", "").replace(",", "").trim()
    val mrp = mrpStr.toDoubleOrNull() ?: (sellingPrice ?: 0.0)

    val minStockStr = getVal(AppFieldKey.MIN_STOCK).replace(",", "").trim()
    val minStock = minStockStr.toDoubleOrNull() ?: 10.0

    val unitStr = getVal(AppFieldKey.UNIT).uppercase()
    val unit = try {
      ProductUnit.valueOf(unitStr)
    } catch (e: Exception) {
      when {
        unitStr.contains("BOTTLE") -> ProductUnit.BOTTLE
        unitStr.contains("LIT") -> ProductUnit.LITER
        unitStr.contains("PACK") -> ProductUnit.PACKET
        unitStr.contains("KG") -> ProductUnit.KG
        unitStr.contains("GM") || unitStr.contains("GRAM") -> ProductUnit.GRAM
        unitStr.contains("PC") || unitStr.contains("PIECE") -> ProductUnit.PIECE
        else -> ProductUnit.BAG
      }
    }

    // Expiry date parse
    val expStr = getVal(AppFieldKey.EXPIRY_DATE)
    var parsedExpiry: Long? = null
    if (expStr.isNotBlank()) {
      for (df in dateFormats) {
        try {
          val d = df.parse(expStr)
          if (d != null) {
            parsedExpiry = d.time
            break
          }
        } catch (e: Exception) {}
      }
      if (parsedExpiry == null) {
        // Try parsing timestamp
        parsedExpiry = expStr.toLongOrNull()
      }
    }

    val product = if (errors.isEmpty()) {
      Product(
        id = "prod_${UUID.randomUUID().toString().take(8)}",
        name = name,
        category = getVal(AppFieldKey.CATEGORY),
        company = company,
        unit = unit,
        batchNumber = getVal(AppFieldKey.BATCH_NUMBER),
        purchasePrice = purchasePrice,
        sellingPrice = sellingPrice ?: 0.0,
        mrp = mrp,
        stockQuantity = quantity ?: 0.0,
        minStockAlert = minStock,
        expiryDate = parsedExpiry,
        rackLocation = getVal(AppFieldKey.RACK_LOCATION),
        hsnCode = getVal(AppFieldKey.HSN_CODE),
        chemicalComposition = getVal(AppFieldKey.CHEMICAL),
        barcode = getVal(AppFieldKey.BARCODE),
        packaging = getVal(AppFieldKey.PACKAGING),
        crop = getVal(AppFieldKey.CROP),
        usesInstructions = getVal(AppFieldKey.USES_INSTRUCTIONS),
        customFields = "",
        createdAt = System.currentTimeMillis()
      )
    } else {
      null
    }

    ParsedProductRow(
      originalIndex = rowIndex,
      product = product,
      errors = errors
    )
  }
}

// -------------------------------------------------------------
// CSV & XLSX OPENXML PARSERS
// -------------------------------------------------------------

fun parseCsvStream(inputStream: InputStream): Pair<List<String>, List<List<String>>> {
  val reader = BufferedReader(InputStreamReader(inputStream))
  val lines = reader.readLines()
  return parseCsvLines(lines)
}

fun parseCsvString(content: String): Pair<List<String>, List<List<String>>> {
  val lines = content.lines().filter { it.isNotBlank() }
  return parseCsvLines(lines)
}

private fun parseCsvLines(lines: List<String>): Pair<List<String>, List<List<String>>> {
  val parsedRows = mutableListOf<List<String>>()

  for (line in lines) {
    if (line.isBlank()) continue
    val row = parseCsvLine(line)
    if (row.isNotEmpty() && row.any { it.isNotBlank() }) {
      parsedRows.add(row)
    }
  }

  if (parsedRows.isEmpty()) return Pair(emptyList(), emptyList())
  val headers = parsedRows.first().map { it.trim() }
  val dataRows = parsedRows.drop(1)
  return Pair(headers, dataRows)
}

private fun parseCsvLine(line: String): List<String> {
  val delimiter = if (line.contains('\t')) '\t' else if (line.contains(';') && !line.contains(',')) ';' else ','
  val tokens = mutableListOf<String>()
  val sb = StringBuilder()
  var inQuotes = false

  var i = 0
  while (i < line.length) {
    val c = line[i]
    if (c == '"') {
      if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
        sb.append('"')
        i++
      } else {
        inQuotes = !inQuotes
      }
    } else if (c == delimiter && !inQuotes) {
      tokens.add(sb.toString().trim())
      sb.setLength(0)
    } else {
      sb.append(c)
    }
    i++
  }
  tokens.add(sb.toString().trim())
  return tokens
}

fun parseXlsxStream(inputStream: InputStream): Pair<List<String>, List<List<String>>> {
  val sharedStrings = mutableListOf<String>()
  val rawSheetRows = mutableListOf<List<String>>()

  val zip = ZipInputStream(inputStream)
  var entry = zip.nextEntry
  val xmlDataMap = mutableMapOf<String, ByteArray>()

  while (entry != null) {
    val name = entry.name
    if (name.equals("xl/sharedStrings.xml", ignoreCase = true) ||
        name.equals("xl/worksheets/sheet1.xml", ignoreCase = true)) {
      xmlDataMap[name] = zip.readBytes()
    }
    zip.closeEntry()
    entry = zip.nextEntry
  }

  // 1. Parse Shared Strings
  val sharedBytes = xmlDataMap["xl/sharedStrings.xml"]
  if (sharedBytes != null) {
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(sharedBytes.inputStream(), "UTF-8")

    var eventType = parser.eventType
    var currentText: StringBuilder? = null
    while (eventType != XmlPullParser.END_DOCUMENT) {
      val tag = parser.name
      when (eventType) {
        XmlPullParser.START_TAG -> {
          if (tag == "t") {
            currentText = StringBuilder()
          }
        }
        XmlPullParser.TEXT -> {
          currentText?.append(parser.text)
        }
        XmlPullParser.END_TAG -> {
          if (tag == "t") {
            sharedStrings.add(currentText?.toString() ?: "")
            currentText = null
          }
        }
      }
      eventType = parser.next()
    }
  }

  // 2. Parse Sheet1 Rows
  val sheetBytes = xmlDataMap["xl/worksheets/sheet1.xml"]
  if (sheetBytes != null) {
    val factory = XmlPullParserFactory.newInstance()
    val parser = factory.newPullParser()
    parser.setInput(sheetBytes.inputStream(), "UTF-8")

    var eventType = parser.eventType
    var currentRow = mutableListOf<String>()
    var cellType = ""
    var cellValue = StringBuilder()
    var isValTag = false

    while (eventType != XmlPullParser.END_DOCUMENT) {
      val tag = parser.name
      when (eventType) {
        XmlPullParser.START_TAG -> {
          if (tag == "row") {
            currentRow = mutableListOf()
          } else if (tag == "c") {
            cellType = parser.getAttributeValue(null, "t") ?: ""
            cellValue = StringBuilder()
          } else if (tag == "v" || tag == "t") {
            isValTag = true
          }
        }
        XmlPullParser.TEXT -> {
          if (isValTag) {
            cellValue.append(parser.text)
          }
        }
        XmlPullParser.END_TAG -> {
          if (tag == "v" || tag == "t") {
            isValTag = false
          } else if (tag == "c") {
            val raw = cellValue.toString().trim()
            val text = if (cellType == "s") {
              val idx = raw.toIntOrNull()
              if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
            } else {
              raw
            }
            currentRow.add(text)
          } else if (tag == "row") {
            if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
              rawSheetRows.add(currentRow)
            }
          }
        }
      }
      eventType = parser.next()
    }
  }

  if (rawSheetRows.isEmpty()) return Pair(emptyList(), emptyList())
  val headers = rawSheetRows.first()
  val rows = rawSheetRows.drop(1)
  return Pair(headers, rows)
}

private fun getSampleFertilizerCsv(): String {
  return """Product Name,Company,Chemical Composition,Packaging,Category,Price,Quantity,Minimum Stock,Expiry Date,Barcode,Crop,Uses / Instructions
Urea 46% N,IFFCO,Nitrogen 46% Prilled,45 kg Bag,Fertilizers,266.5,150,20,31/12/2027,890123450001,"Wheat, Paddy, Maize, Sugarcane",Apply as basal dose and top dressing
DAP 18:46:0,Coromandel,Di-Ammonium Phosphate (N 18% P 46%),50 kg Bag,Fertilizers,1350,85,15,30/06/2027,890123450002,"Wheat, Mustard, Potato, Cotton",Apply at sowing time near roots
Karate 5 EC,Syngenta,Lambda-cyhalothrin 5% EC,1000 ml,Insecticides,520,40,10,31/08/2026,890123450003,"Cotton, Chilli, Tomato, Groundnut",Dilute 2 ml per liter water for foliar spray
Coragen 18.5% SC,FMC,Chlorantraniliprole 18.5% w/w,150 ml,Insecticides,1850,25,5,15/07/2026,890123450004,"Sugarcane, Paddy, Tomato, Cabbage",60 ml per acre for borer control
Ampligo 150 ZC,Syngenta,Chlorantraniliprole 9.3% + Lambdacyhalothrin 4.6%,200 ml,Insecticides,1150,30,8,20/09/2026,890123450005,"Paddy, Cotton, Tomato, Pulses",80-100 ml per acre for severe pest attack
Zinc Sulphate 33%,Multiplex,Zinc 33% (Monohydrate),5 kg,Micronutrients,420,60,10,31/12/2027,890123450006,"Paddy, Wheat, Maize, Citrus",Mix 5 kg with soil or compost per acre
MOP Potash 60%,IPL,Muriate of Potash (K2O 60%),50 kg Bag,Fertilizers,1650,50,10,30/11/2027,890123450007,"Sugarcane, Potato, Cotton, Banana",Apply at tuber / fruit formation stage
Confidor 200 SL,Bayer,Imidacloprid 17.8% SL,250 ml,Insecticides,680,35,8,31/10/2026,890123450008,"Chilli, Cotton, Mango, Citrus",0.5 ml per liter water for sucking pests"""
}
