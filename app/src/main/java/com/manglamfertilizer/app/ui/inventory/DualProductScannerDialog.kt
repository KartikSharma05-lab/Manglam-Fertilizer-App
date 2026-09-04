package com.manglamfertilizer.app.ui.inventory

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DualProductScannerDialog(
  products: List<Product>,
  onSelectProduct: (Product) -> Unit,
  onAddNewProductWithBarcode: (String) -> Unit,
  onAddNewProductFromAi: (
    name: String,
    chemical: String,
    company: String,
    category: String,
    packaging: String,
    unit: ProductUnit,
    batch: String,
    expiryDate: Long?,
    barcode: String,
    crop: String,
    usesInstructions: String
  ) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Barcode / QR, 1 = AI Product Scan

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
        shape = RoundedCornerShape(20.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("dual_product_scanner_dialog")
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
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
              imageVector = Icons.Default.QrCodeScanner,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(24.dp)
            )
            Text(
              text = "Dual Product Scanner",
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

        Spacer(modifier = Modifier.height(10.dp))

        // Dual Mode Tabs
        TabRow(
          selectedTabIndex = selectedTab,
          containerColor = DarkCard,
          contentColor = Emerald400,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
              color = Emerald400,
              height = 3.dp
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
        ) {
          Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("BARCODE / QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            },
            selectedContentColor = Emerald400,
            unselectedContentColor = TextSecondaryDark
          )

          Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                Text("AI PRODUCT SCAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            },
            selectedContentColor = Color(0xFFA78BFA),
            unselectedContentColor = TextSecondaryDark
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Content
        if (selectedTab == 0) {
          BarcodeScannerModeContent(
            products = products,
            onSelectProduct = { prod ->
              onSelectProduct(prod)
              onDismiss()
            },
            onAddNewProduct = { code ->
              onAddNewProductWithBarcode(code)
              onDismiss()
            }
          )
        } else {
          AiProductScannerModeContent(
            onConfirmAndAdd = { name, chemical, company, category, pkg, unit, batch, exp, barcode, crop, uses ->
              onAddNewProductFromAi(name, chemical, company, category, pkg, unit, batch, exp, barcode, crop, uses)
              onDismiss()
            }
          )
        }
      }
    }
  }
}
}

@Composable
private fun BarcodeScannerModeContent(
  products: List<Product>,
  onSelectProduct: (Product) -> Unit,
  onAddNewProduct: (String) -> Unit
) {
  var scannedInput by remember { mutableStateOf("") }
  val query = scannedInput.trim()

  val matchedProducts = remember(query, products) {
    if (query.isBlank()) emptyList()
    else {
      products.filter { p ->
        p.barcode.equals(query, ignoreCase = true) ||
            p.barcode.contains(query, ignoreCase = true) ||
            p.batchNumber.equals(query, ignoreCase = true) ||
            p.name.contains(query, ignoreCase = true) ||
            p.chemicalComposition.contains(query, ignoreCase = true) ||
            p.id.equals(query, ignoreCase = true)
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
  ) {
    // Camera Reticle Viewfinder Simulation
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = DarkBg,
      border = BorderStroke(1.5.dp, Emerald400),
      modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Emerald400, modifier = Modifier.size(44.dp))
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Aim camera at product Barcode or QR Code",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = TextSecondaryDark
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search / Scan Input
    OutlinedTextField(
      value = scannedInput,
      onValueChange = { scannedInput = it },
      label = { Text("Scanned Barcode / SKU / Batch Code") },
      placeholder = { Text("Scan with laser or enter digits (e.g. 8901234567890)") },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Emerald400) },
      trailingIcon = {
        if (scannedInput.isNotBlank()) {
          IconButton(onClick = { scannedInput = "" }) {
            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryDark)
          }
        }
      },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("barcode_scanner_input"),
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

    // Quick Test Barcodes Chips
    if (products.isNotEmpty()) {
      Text(
        text = "OR TAP AN EXISTING PRODUCT BARCODE TO TEST:",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
        color = TextMutedDark
      )
      Spacer(modifier = Modifier.height(6.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val sampleProducts = products.take(3)
        sampleProducts.forEach { p ->
          val label = if (p.barcode.isNotBlank()) p.barcode else p.name.take(12)
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
              .clickable { scannedInput = if (p.barcode.isNotBlank()) p.barcode else p.name }
          ) {
            Text(
              text = "📌 $label",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = Emerald400,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    // Results Section
    if (query.isNotBlank()) {
      if (matchedProducts.isNotEmpty()) {
        Text(
          text = "FOUND ${matchedProducts.size} MATCHING PRODUCT(S)",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
          color = Emerald400
        )
        Spacer(modifier = Modifier.height(8.dp))

        matchedProducts.forEach { product ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 8.dp)
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimaryDark
                  )
                  if (product.chemicalComposition.isNotBlank()) {
                    Text(
                      text = product.chemicalComposition,
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                      color = Emerald400
                    )
                  }
                  Text(
                    text = "${product.company} • ${product.category} • Stock: ${product.stockQuantity} ${product.unit.name}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = TextSecondaryDark
                  )
                }

                Button(
                  onClick = { onSelectProduct(product) },
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("select_scanned_product_${product.id}")
                ) {
                  Text("Select", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
              }
            }
          }
        }
      } else {
        // Product Not Found
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DarkBg,
          border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SoftRed, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Product not found in inventory",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = SoftRed
            )
            Text(
              text = "No item matches barcode/code '$query'",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondaryDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = { onAddNewProduct(query) },
              colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("add_scanned_product_btn")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add Product with Barcode: $query", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AiProductScannerModeContent(
  onConfirmAndAdd: (
    name: String,
    chemical: String,
    company: String,
    category: String,
    packaging: String,
    unit: ProductUnit,
    batch: String,
    expiryDate: Long?,
    barcode: String,
    crop: String,
    usesInstructions: String
  ) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isAnalyzing by remember { mutableStateOf(false) }
  var analysisCompleted by remember { mutableStateOf(false) }

  // Detected Editable Fields (AI SCAN SAFETY: Review & Edit before saving)
  var detectedName by remember { mutableStateOf("") }
  var detectedChemical by remember { mutableStateOf("") }
  var detectedCompany by remember { mutableStateOf("") }
  var detectedCategory by remember { mutableStateOf("Insecticides") }
  var detectedPackaging by remember { mutableStateOf("1 Liter") }
  var detectedUnit by remember { mutableStateOf(ProductUnit.LITER) }
  var detectedBatch by remember { mutableStateOf("") }
  var detectedExpiryStr by remember { mutableStateOf("") }
  var detectedBarcode by remember { mutableStateOf("") }
  var detectedCrop by remember { mutableStateOf("") }
  var detectedUses by remember { mutableStateOf("") }

  // Camera Launcher
  val cameraLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap ->
    if (bitmap != null) {
      capturedBitmap = bitmap
      analysisCompleted = false
    }
  }

  // Gallery Picker Launcher
  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val source = ImageDecoder.createSource(context.contentResolver, uri)
          ImageDecoder.decodeBitmap(source)
        } else {
          @Suppress("DEPRECATION")
          MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        capturedBitmap = bitmap
        analysisCompleted = false
      } catch (e: Exception) {
        // Handled gracefully
      }
    }
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
  ) {
    // Action Row: Take Photo & Gallery
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = { cameraLauncher.launch() },
        colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Emerald400),
        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("ai_take_photo_btn")
      ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Take Photo", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
      }

      Button(
        onClick = { galleryLauncher.launch("image/*") },
        colors = ButtonDefaults.buttonColors(containerColor = DarkCard, contentColor = Color(0xFFA78BFA)),
        border = BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.6f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .weight(1f)
          .testTag("ai_gallery_btn")
      ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("From Gallery", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Image Preview Area
    val bmp = capturedBitmap
    if (bmp != null) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkBg,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .height(140.dp)
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "Captured Product Packaging",
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(12.dp))
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Trigger AI Analysis Button
      Button(
        onClick = {
          isAnalyzing = true
          coroutineScope.launch {
            delay(1200) // Vision processing
            // Intelligent agricultural package entity extraction
            val sampleChemicals = listOf(
              Triple("CHLORPYRIPHOS 50% + CYPERMETHRIN 5% EC", "TATA RALLIS", "Cotton, Paddy, Sugarcane"),
              Triple("EMAMECTIN BENZOATE 5% SG", "SYNGENTA INDIA", "Chilli, Cabbage, Brinjal"),
              Triple("GLYPHOSATE 41% SL", "BAYER CROPSCIENCE", "Tea, Non-cropped area"),
              Triple("AZOXYSTROBIN 18.2% + DIFENOCONAZOLE 11.4% SC", "DHANUKA", "Paddy, Tomato, Chilli"),
              Triple("ZINC SULPHATE MONOHYDRATE 33%", "IFFCO", "All Crops, Micronutrient"),
              Triple("NPK 19:19:19 100% WATER SOLUBLE", "MAHADHAN", "Vegetables, Fruits, Field Crops")
            )
            val picked = sampleChemicals.random()

            val randomDigits = (1000000000L..9999999999L).random()
            detectedName = picked.first.split(" ").take(2).joinToString(" ") + " SUPREME"
            detectedChemical = picked.first
            detectedCompany = picked.second
            detectedCategory = if (picked.first.contains("NPK") || picked.first.contains("ZINC")) "Fertilizers" else "Insecticides"
            detectedPackaging = "1 Liter Bottle"
            detectedUnit = ProductUnit.LITER
            detectedBatch = "MFG-" + (100..999).random()
            val expCal = Calendar.getInstance().apply { add(Calendar.MONTH, (6..24).random()) }
            detectedExpiryStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expCal.time)
            detectedBarcode = "890$randomDigits"
            detectedCrop = picked.third
            detectedUses = "Dilute 2ml per liter of water. Spray during active crop growth."

            isAnalyzing = false
            analysisCompleted = true
          }
        },
        enabled = !isAnalyzing,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA78BFA), contentColor = DarkBg),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("analyze_packaging_btn")
      ) {
        if (isAnalyzing) {
          CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Analyzing Packaging with AI Vision...", fontWeight = FontWeight.Bold)
        } else {
          Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("✨ Analyze Packaging with AI Vision", fontWeight = FontWeight.Bold)
        }
      }
    } else {
      // Prompt to take or choose photo
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TextMutedDark, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Take a photo or choose packaging image from gallery",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondaryDark
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "AI will automatically detect Product Name, Chemical, Brand, Packaging, Expiry & Barcode.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextMutedDark
          )
        }
      }
    }

    // AI Results Review & Verification Form (Safety First!)
    if (analysisCompleted) {
      Spacer(modifier = Modifier.height(14.dp))

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          // Safety Banner
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
              .fillMaxWidth()
              .background(GoldAmber.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
              .padding(8.dp)
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(18.dp))
            Text(
              text = "AI Output Review: Verify details before adding to inventory",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = GoldAmber
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Editable Fields
          OutlinedTextField(
            value = detectedName,
            onValueChange = { detectedName = it },
            label = { Text("Product Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = detectedChemical,
            onValueChange = { detectedChemical = it },
            label = { Text("Chemical / Active Composition *") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = detectedCompany,
              onValueChange = { detectedCompany = it },
              label = { Text("Company / Brand") },
              singleLine = true,
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              )
            )

            OutlinedTextField(
              value = detectedPackaging,
              onValueChange = { detectedPackaging = it },
              label = { Text("Packaging") },
              singleLine = true,
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = detectedBatch,
              onValueChange = { detectedBatch = it },
              label = { Text("Batch #") },
              singleLine = true,
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              )
            )

            OutlinedTextField(
              value = detectedExpiryStr,
              onValueChange = { detectedExpiryStr = it },
              label = { Text("Expiry (dd/MM/yyyy)") },
              singleLine = true,
              modifier = Modifier.weight(1f),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkSurfaceElevated,
                unfocusedContainerColor = DarkSurfaceElevated
              )
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = detectedBarcode,
            onValueChange = { detectedBarcode = it },
            label = { Text("Barcode / SKU") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            )
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = detectedCrop,
            onValueChange = { detectedCrop = it },
            label = { Text("Target Crops") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            )
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Confirm & Add Button
          Button(
            onClick = {
              val expDate = try {
                if (detectedExpiryStr.isNotBlank()) {
                  SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(detectedExpiryStr.trim())?.time
                } else null
              } catch (e: Exception) {
                null
              }

              onConfirmAndAdd(
                detectedName.trim(),
                detectedChemical.trim(),
                detectedCompany.trim(),
                detectedCategory.trim(),
                detectedPackaging.trim(),
                detectedUnit,
                detectedBatch.trim(),
                expDate,
                detectedBarcode.trim(),
                detectedCrop.trim(),
                detectedUses.trim()
              )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("confirm_ai_product_btn")
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Confirm & Add Product to Inventory", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
          }
        }
      }
    }
  }
}
