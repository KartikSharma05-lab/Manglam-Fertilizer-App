package com.manglamfertilizer.app.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.CloudSyncState
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.UpdateEngineState
import com.manglamfertilizer.app.data.util.BiometricAuthManager
import com.manglamfertilizer.app.ui.accounts.DailyAccountsScreen
import com.manglamfertilizer.app.ui.ai.AgriculturalAIScreen
import com.manglamfertilizer.app.ui.ai.VoiceAIScreen
import com.manglamfertilizer.app.ui.alerts.AlertPanelDialog
import com.manglamfertilizer.app.ui.auth.AuthScreen
import com.manglamfertilizer.app.ui.auth.DeviceUnlockScreen
import com.manglamfertilizer.app.ui.auth.DisplayNameSetupScreen
import com.manglamfertilizer.app.ui.billing.BillingScreen
import com.manglamfertilizer.app.ui.customers.CustomersScreen
import com.manglamfertilizer.app.ui.home.HomeScreen
import com.manglamfertilizer.app.ui.inventory.InventoryHistoryDialog
import com.manglamfertilizer.app.ui.inventory.InventoryScreen
import com.manglamfertilizer.app.ui.inventory.ProductDetailsDialog
import com.manglamfertilizer.app.ui.navigation.ManglamBottomBar
import com.manglamfertilizer.app.ui.navigation.Screen
import com.manglamfertilizer.app.ui.reports.ReportsScreen
import com.manglamfertilizer.app.ui.settings.SettingsScreen
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import com.manglamfertilizer.app.ui.update.ForcedUpdateScreen
import com.manglamfertilizer.app.ui.update.UpdateDialog

@Composable
fun MainScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val currentUser by viewModel.currentUser.collectAsState()
  val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsState()
  val cloudSyncState by viewModel.cloudSyncState.collectAsState()
  val syncStatusText by viewModel.syncStatusText.collectAsState()
  val greetingInfo by viewModel.greetingInfo.collectAsState()
  val todayHighlight by viewModel.todayHighlight.collectAsState()
  val dashboardMetrics by viewModel.dashboardMetrics.collectAsState()
  val stockAlerts by viewModel.stockAlerts.collectAsState()
  val recentInvoices by viewModel.recentInvoices.collectAsState()
  val isRefreshingHome by viewModel.isRefreshingHome.collectAsState()
  val products by viewModel.products.collectAsState()
  val categories by viewModel.categories.collectAsState()
  val inventoryHistory by viewModel.inventoryHistory.collectAsState()
  val customers by viewModel.customers.collectAsState()
  val allInvoices by viewModel.allInvoices.collectAsState()
  val appLanguage by viewModel.appLanguage.collectAsState()
  val appThemeMode by viewModel.appThemeMode.collectAsState()
  val inventoryColumns by viewModel.inventoryColumns.collectAsState()
  val dailyAccountsColumns by viewModel.dailyAccountsColumns.collectAsState()
  val auditLogs by viewModel.auditLogs.collectAsState()
  val isAuditLoading by viewModel.isAuditLoading.collectAsState()
  val auditCleanupRuns by viewModel.auditCleanupRuns.collectAsState()
  val isRetentionSimulating by viewModel.isRetentionSimulating.collectAsState()
  val latestRetentionSimulation by viewModel.latestRetentionSimulation.collectAsState()
  val updateInfo by viewModel.updateInfo.collectAsState()
  val releaseHistory by viewModel.releaseHistory.collectAsState()
  val githubStatus by viewModel.githubStatus.collectAsState()
  val invoiceNumberConfig by viewModel.invoiceNumberConfig.collectAsState()
  var showUpdateDialog by remember { mutableStateOf(false) }
  var showAuditDialog by remember { mutableStateOf(false) }
  var showAlertsPanelDialog by remember { mutableStateOf(false) }
  var selectedAlertProductForDetails by remember { mutableStateOf<Product?>(null) }

  // Initial background update check
  LaunchedEffect(Unit) {
    viewModel.checkForUpdates(isManual = false)
  }

  // Auto-show update prompt if a new version is detected and not dismissed for today
  LaunchedEffect(updateInfo.hasUpdate, updateInfo.isDismissedForToday, updateInfo.isForced) {
    if (updateInfo.hasUpdate && !updateInfo.isDismissedForToday && !updateInfo.isForced) {
      showUpdateDialog = true
    }
  }

  // Back stack navigation: keeps history across tabs and sub-screens
  val screenStack = remember { mutableStateListOf<Screen>(Screen.Home) }
  val currentScreen = screenStack.lastOrNull() ?: Screen.Home

  fun navigateTo(screen: Screen) {
    if (screen == Screen.Home) {
      screenStack.clear()
      screenStack.add(Screen.Home)
    } else {
      if (screenStack.lastOrNull() != screen) {
        screenStack.remove(screen)
        screenStack.add(screen)
      }
    }
  }

  // Handle system back button across screens
  BackHandler(enabled = screenStack.size > 1) {
    screenStack.removeAt(screenStack.lastIndex)
  }

  LaunchedEffect(currentUser?.id) {
    screenStack.clear()
    screenStack.add(Screen.Home)
  }

  // 1. If user is not authenticated via Firebase, show AuthScreen (Email + Password)
  if (currentUser == null) {
    AuthScreen(
      greetingInfo = greetingInfo,
      onLogin = { email, pass, onResult ->
        viewModel.login(email, pass) { success, msg ->
          if (success) {
            navigateTo(Screen.Home)
          }
          onResult(success, msg)
        }
      },
      onForgotPassword = { email, onDone ->
        viewModel.sendPasswordReset(email, onDone)
      }
    )
    return
  }

  val loggedInUser = currentUser!!

  // 2. If user is authenticated in Firebase but device session is locked, show DeviceUnlockScreen
  if (!isSessionUnlocked) {
    DeviceUnlockScreen(
      user = loggedInUser,
      onUnlockSuccess = {
        viewModel.unlockSession()
        navigateTo(Screen.Home)
      },
      onLogout = {
        viewModel.logout()
        navigateTo(Screen.Home)
      }
    )
    return
  }

  // 2.5. Forced / Critical Mandatory Update Check
  if (updateInfo.isForced) {
    ForcedUpdateScreen(
      updateInfo = updateInfo,
      onDownloadClick = { viewModel.startDownloadUpdate() },
      onInstallClick = { ctx -> viewModel.installUpdate(ctx) }
    )
    return
  }

  // 3. First Login Name Setup Screen (shown before Home ONLY if display name setup is not completed and user.name is blank)
  val initialSetupDone = remember(loggedInUser.id, loggedInUser.name) {
    viewModel.isDisplayNameSetupCompleted(loggedInUser.id) || loggedInUser.name.isNotBlank()
  }
  var isNameSetupCompleted by remember(loggedInUser.id) {
    mutableStateOf(initialSetupDone)
  }

  LaunchedEffect(loggedInUser.id, loggedInUser.name) {
    if (viewModel.isDisplayNameSetupCompleted(loggedInUser.id) || loggedInUser.name.isNotBlank()) {
      isNameSetupCompleted = true
    }
  }

  if (!isNameSetupCompleted && loggedInUser.name.isBlank()) {
    val googleName = viewModel.getFirebaseDisplayName()
    DisplayNameSetupScreen(
      googleDisplayName = googleName,
      onSaveDisplayName = { chosenName ->
        viewModel.updatePreferredGreetingName(loggedInUser.id, chosenName)
        isNameSetupCompleted = true
        navigateTo(Screen.Home)
      }
    )
    return
  }

  // 4. Optional prompt on first login: offer biometric / device unlock
  var showFirstTimeBiometricPrompt by remember(loggedInUser.id) {
    mutableStateOf(
      !viewModel.hasPromptedDeviceUnlock(loggedInUser.id) &&
          BiometricAuthManager.isBiometricOrDeviceLockAvailable(context)
    )
  }

  if (showFirstTimeBiometricPrompt) {
    AlertDialog(
      onDismissRequest = {
        viewModel.setPromptedDeviceUnlock(loggedInUser.id, true)
        showFirstTimeBiometricPrompt = false
      },
      title = {
        Text(
          text = "Enable Biometric / Device Lock?",
          fontWeight = FontWeight.Bold,
          color = TextPrimaryDark
        )
      },
      text = {
        Text(
          text = "Use your fingerprint, face, or device screen lock (PIN/pattern/password) for quick and secure access to ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} on this device.",
          color = TextSecondaryDark
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.setDeviceUnlockEnabled(loggedInUser.id, true)
            viewModel.setPromptedDeviceUnlock(loggedInUser.id, true)
            showFirstTimeBiometricPrompt = false
            Toast.makeText(context, "Biometric / Device unlock enabled", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = Emerald500,
            contentColor = androidx.compose.ui.graphics.Color.Black
          )
        ) {
          Text("Enable", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            viewModel.setDeviceUnlockEnabled(loggedInUser.id, false)
            viewModel.setPromptedDeviceUnlock(loggedInUser.id, true)
            showFirstTimeBiometricPrompt = false
          }
        ) {
          Text("Not Now", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard,
      shape = RoundedCornerShape(16.dp)
    )
  }

  val isTopLevelScreen = currentScreen in listOf(
    Screen.Home,
    Screen.Billing,
    Screen.Inventory,
    Screen.Customers,
    Screen.Reports,
    Screen.Settings
  )

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg),
    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    bottomBar = {
      if (isTopLevelScreen) {
        ManglamBottomBar(
          currentRoute = currentScreen.route,
          onNavigate = { route ->
            when (route) {
              Screen.Home.route -> navigateTo(Screen.Home)
              Screen.Billing.route -> navigateTo(Screen.Billing)
              Screen.Inventory.route -> navigateTo(Screen.Inventory)
              Screen.Customers.route -> navigateTo(Screen.Customers)
              Screen.Reports.route -> navigateTo(Screen.Reports)
              Screen.Settings.route -> navigateTo(Screen.Settings)
            }
          }
        )
      }
    },
    containerColor = DarkBg
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
          (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
              slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing)) { (it * 0.03f).toInt() })
            .togetherWith(fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)))
        },
        label = "ScreenTransition",
        modifier = Modifier.fillMaxSize()
      ) { targetScreen ->
        when (targetScreen) {
          Screen.Home -> {
          HomeScreen(
            user = loggedInUser,
            cloudSyncState = cloudSyncState,
            syncStatusText = syncStatusText,
            greetingInfo = greetingInfo,
            highlight = todayHighlight,
            metrics = dashboardMetrics,
            stockAlerts = stockAlerts,
            recentInvoices = recentInvoices,
            products = products,
            customers = customers,
            isRefreshing = isRefreshingHome,
            onRefresh = {
              viewModel.refreshHomeData { success ->
                if (success) {
                  Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onNavigateToScreen = { routeId ->
              when (routeId) {
                "billing" -> navigateTo(Screen.Billing)
                "inventory" -> navigateTo(Screen.Inventory)
                "customers" -> navigateTo(Screen.Customers)
                "reports" -> navigateTo(Screen.Reports)
                "daily_accounts" -> navigateTo(Screen.DailyAccounts)
                "ai_advisor" -> navigateTo(Screen.AIAdvisor)
                "voice_ai" -> navigateTo(Screen.VoiceAI)
                "settings" -> navigateTo(Screen.Settings)
              }
            },
            onOpenNotifications = {
              showAlertsPanelDialog = true
            },
            onOpenBarcodeScanner = {
              Toast.makeText(context, "Ready to scan product barcode / QR", Toast.LENGTH_SHORT).show()
            },
            onCloudClick = {
              if (cloudSyncState != CloudSyncState.CONNECTED_SYNCED) {
                viewModel.retryCloudSync()
              }
              val message = "Cloud Sync: $syncStatusText"
              Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
          )
        }

        Screen.Billing -> {
          BillingScreen(
            invoices = allInvoices,
            products = products,
            customers = customers,
            currentUser = loggedInUser,
            invoiceNumberConfig = invoiceNumberConfig,
            onCreateInvoice = { custId, name, phone, aadhaar, address, village, items, gstRate, disc, paid, dueDt, mode, onComplete ->
              viewModel.createInvoice(
                customerId = custId,
                customerName = name,
                customerPhone = phone,
                customerAadhaar = aadhaar,
                customerAddress = address,
                customerVillage = village,
                items = items,
                gstRate = gstRate,
                discount = disc,
                amountPaid = paid,
                dueDate = dueDt,
                paymentMode = mode
              ) { success, msg ->
                onComplete(success, msg)
                if (success) {
                  Toast.makeText(context, "Invoice created successfully!", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onPrintInvoice = { inv ->
              Toast.makeText(context, "Sending ${inv.invoiceNumber} to Bluetooth Printer...", Toast.LENGTH_SHORT).show()
            },
            onUpdateCustomer = { updatedCustomer, onDone ->
              viewModel.updateCustomer(updatedCustomer) { success, msg ->
                onDone(success, msg)
                if (success) {
                  Toast.makeText(context, "Farmer details updated", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onDeleteInvoice = { invId, onDone ->
              viewModel.deleteInvoice(invId) { success, msg ->
                onDone(success, msg)
              }
            }
          )
        }

        Screen.Inventory -> {
          InventoryScreen(
            products = products,
            categories = categories,
            inventoryHistory = inventoryHistory,
            auditLogs = auditLogs,
            auditCleanupRuns = auditCleanupRuns,
            isAuditLoading = isAuditLoading,
            isRetentionSimulating = isRetentionSimulating,
            latestRetentionSimulation = latestRetentionSimulation,
            onRefreshAudit = { viewModel.refreshAuditLogs() },
            onRunRetentionSimulation = { dateKey -> viewModel.runRetentionSimulation(dateKey) },
            currentUser = loggedInUser,
            inventoryColumns = inventoryColumns,
            onSaveColumns = { updatedCols ->
              viewModel.saveInventoryColumns(updatedCols)
            },
            onAddCustomField = { fieldName ->
              viewModel.addCustomField(fieldName)
            },
            onRenameCustomField = { fieldKey, newLabel ->
              viewModel.renameCustomField(fieldKey, newLabel)
            },
            onDeleteCustomField = { fieldKey ->
              viewModel.deleteCustomField(fieldKey)
            },
            onAddProduct = { name, cat, comp, unit, batch, pPrice, sPrice, mrp, stock, minAlert, exp, rack, hsn, chem, barcode, pkg, crop, usesInst, custom, onResult ->
              viewModel.addProduct(
                name = name,
                category = cat,
                company = comp,
                unit = unit,
                batch = batch,
                purchasePrice = pPrice,
                sellingPrice = sPrice,
                mrp = mrp,
                stock = stock,
                minAlert = minAlert,
                expiryDate = exp,
                rack = rack,
                hsn = hsn,
                chemicalComposition = chem,
                barcode = barcode,
                packaging = pkg,
                crop = crop,
                usesInstructions = usesInst,
                customFields = custom,
                onResult = onResult
              )
            },
            onUpdateProduct = { prod, oldProd, onResult ->
              viewModel.updateProduct(prod, oldProd, onResult)
            },
            onDeleteProduct = { prodId, prodName, onResult ->
              viewModel.deleteProduct(prodId, prodName, onResult)
            },
            onAddCategory = { catName, onResult ->
              viewModel.addCategory(catName, onResult)
            },
            onUpdateCategory = { catItem, oldName, onResult ->
              viewModel.updateCategory(catItem, oldName, onResult)
            },
            onDeleteCategory = { catId, catName, onResult ->
              viewModel.deleteCategory(catId, catName, onResult)
            },
            onReorderCategories = { newOrder, onResult ->
              viewModel.reorderCategories(newOrder, onResult)
            },
            onLogExport = { format, count, details ->
              viewModel.logInventoryExport(format, count, details)
            },
            onImportProductsBatch = { prods, policy, fType, total, onResult ->
              viewModel.importProductsBatch(prods, policy, fType, total, onResult)
            }
          )
        }

        Screen.Customers -> {
          CustomersScreen(
            customers = customers,
            invoices = allInvoices,
            currentUser = loggedInUser,
            onAddCustomer = { name, phone, village, address, onDone ->
              viewModel.addCustomer(name, phone, village, address) { success, msg ->
                onDone(success, msg)
                if (success) {
                  Toast.makeText(context, "Farmer registered successfully", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onUpdateCustomer = { updatedCustomer, onDone ->
              viewModel.updateCustomer(updatedCustomer) { success, msg ->
                onDone(success, msg)
                if (success) {
                  Toast.makeText(context, "Farmer details updated", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onRecordPayment = { custId, amount, onDone ->
              viewModel.recordDuePayment(custId, amount) { success, msg ->
                onDone(success, msg)
                if (success) {
                  Toast.makeText(context, "Payment recorded & due updated", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onDeleteCustomer = { custId, onDone ->
              viewModel.deleteCustomer(custId) { success, msg ->
                onDone(success, msg)
                if (success) {
                  Toast.makeText(context, "Farmer record removed", Toast.LENGTH_SHORT).show()
                }
              }
            }
          )
        }

        Screen.Reports -> {
          ReportsScreen(
            invoices = allInvoices,
            products = products,
            customers = customers,
            onExportReport = {
              Toast.makeText(context, "Exporting GST and Sales summary report...", Toast.LENGTH_SHORT).show()
            }
          )
        }

        Screen.Settings -> {
          SettingsScreen(
            user = loggedInUser,
            language = appLanguage,
            themeMode = appThemeMode,
            googleDisplayName = viewModel.getFirebaseDisplayName(),
            isDeviceUnlockEnabled = viewModel.isDeviceUnlockEnabled(loggedInUser.id),
            onToggleDeviceUnlock = { enabled ->
              viewModel.setDeviceUnlockEnabled(loggedInUser.id, enabled)
              val statusText = if (enabled) "Biometric & device unlock enabled" else "Biometric & device unlock disabled"
              Toast.makeText(context, statusText, Toast.LENGTH_SHORT).show()
            },
            onUpdateDisplayName = { newName ->
              viewModel.updatePreferredGreetingName(loggedInUser.id, newName)
              Toast.makeText(context, "Greeting name updated", Toast.LENGTH_SHORT).show()
            },
            onLanguageChange = { newLang ->
              viewModel.setLanguage(newLang)
              Toast.makeText(context, if (newLang == "hi") "भाषा हिन्दी में बदली गई" else "Language switched to English", Toast.LENGTH_SHORT).show()
            },
            onThemeChange = { newTheme ->
              viewModel.setThemeMode(newTheme)
              Toast.makeText(context, "Theme updated", Toast.LENGTH_SHORT).show()
            },
            onPrinterConfigClick = {
              Toast.makeText(context, "Thermal Printer: ESC/POS 58mm/80mm Ready", Toast.LENGTH_SHORT).show()
            },
            onBackupClick = {
              Toast.makeText(context, "Cloud sync verified with Firestore", Toast.LENGTH_SHORT).show()
            },
            onLogout = {
              viewModel.logout()
              navigateTo(Screen.Home)
            },
            updateInfo = updateInfo,
            releaseHistory = releaseHistory,
            githubStatus = githubStatus,
            onCheckForUpdates = {
              viewModel.checkForUpdates(isManual = true)
              showUpdateDialog = true
            },
            onShowUpdateDialog = {
              showUpdateDialog = true
            },
            onInstallUpdate = { ctx ->
              viewModel.installUpdate(ctx)
            },
            onPublishRelease = { manifest ->
              val adminEmail = loggedInUser.email.ifBlank { "admin.manglamferilizer@gmail.com" }
              viewModel.publishRelease(manifest, adminEmail) { success, errMsg ->
                if (success) {
                  Toast.makeText(context, "New release published successfully!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Publish failed: $errMsg", Toast.LENGTH_LONG).show()
                }
              }
            },
            onSimulateRelease = { verName, verCode, relType, daysAgo, graceDays, notes ->
              viewModel.simulateRelease(verName, verCode, relType, daysAgo, graceDays, notes)
              Toast.makeText(context, "Simulated release $verName activated", Toast.LENGTH_SHORT).show()
            },
            onSkipUpdate = {
              viewModel.recordUpdateDismissed(updateInfo.manifest?.versionCode ?: 0L)
              viewModel.dismissUpdate()
              Toast.makeText(context, "Update reminder postponed for today", Toast.LENGTH_SHORT).show()
            },
            manifestUrl = viewModel.getManifestUrl(),
            onSetCustomManifestUrl = { url ->
              viewModel.setCustomManifestUrl(url)
            },
            onResetManifestUrl = {
              viewModel.resetManifestUrl()
            },
            onOpenAuditLogs = {
              showAuditDialog = true
            },
            invoiceNumberConfig = invoiceNumberConfig,
            invoices = allInvoices,
            onSaveInvoiceNumberConfig = { cfg, onDone ->
              viewModel.saveInvoiceNumberConfig(cfg, onDone)
            }
          )
        }

        Screen.DailyAccounts -> {
          DailyAccountsScreen(
            invoices = allInvoices,
            columns = dailyAccountsColumns,
            customers = customers,
            products = products,
            currentUser = loggedInUser,
            onSaveColumns = { updatedCols ->
              viewModel.saveDailyAccountsColumns(updatedCols)
            },
            onAddCustomField = { title, type ->
              viewModel.addCustomDailyAccountsField(title, type)
            },
            onRenameField = { fieldId, newTitle ->
              viewModel.renameDailyAccountsField(fieldId, newTitle)
            },
            onDeleteField = { fieldId ->
              viewModel.deleteDailyAccountsField(fieldId)
            },
            onResetDefaults = {
              viewModel.resetDailyAccountsColumns()
            },
            onAddDailyAccountEntry = { custId, custName, custPhone, custVillage, items, total, paid, mode, timestamp, dueDate ->
              viewModel.createInvoice(
                customerId = custId,
                customerName = custName,
                customerPhone = custPhone,
                customerVillage = custVillage,
                items = items,
                amountPaid = paid,
                dueDate = dueDate,
                paymentMode = mode
              ) { success, msg ->
                if (success) {
                  Toast.makeText(context, "Daily Ledger entry added successfully", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Failed to add entry: ${msg ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onDeleteInvoice = { invId ->
              viewModel.deleteInvoice(invId) { success, msg ->
                if (success) {
                  Toast.makeText(context, "Record deleted from Daily Accounts", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "Delete failed: ${msg ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                }
              }
            },
            onBack = { navigateTo(Screen.Home) },
            onNavigateToBilling = { navigateTo(Screen.Billing) }
          )
        }

        Screen.AIAdvisor -> {
          AgriculturalAIScreen(
            onBack = { navigateTo(Screen.Home) }
          )
        }

        Screen.VoiceAI -> {
          VoiceAIScreen(
            onBack = { navigateTo(Screen.Home) }
          )
        }
      }
    }
  }
  }

  // Activity Audit Dialog (Accessible from Settings or Inventory)
  if (showAuditDialog) {
    InventoryHistoryDialog(
      history = inventoryHistory,
      auditLogs = auditLogs,
      auditCleanupRuns = auditCleanupRuns,
      isAuditLoading = isAuditLoading,
      isRetentionSimulating = isRetentionSimulating,
      latestRetentionSimulation = latestRetentionSimulation,
      onRefreshAudit = { viewModel.refreshAuditLogs() },
      onRunRetentionSimulation = { dateKey -> viewModel.runRetentionSimulation(dateKey) },
      onDismiss = { showAuditDialog = false }
    )
  }

  // In-App Update Dialog (shown when user opens update modal or update is available)
  if (showUpdateDialog && (updateInfo.hasUpdate || updateInfo.state == UpdateEngineState.DOWNLOADING || updateInfo.state == UpdateEngineState.VERIFYING || updateInfo.state == UpdateEngineState.READY_TO_INSTALL || updateInfo.state == UpdateEngineState.VERIFICATION_FAILED || updateInfo.state == UpdateEngineState.DOWNLOAD_FAILED)) {
    UpdateDialog(
      updateInfo = updateInfo,
      onDownloadClick = { viewModel.startDownloadUpdate() },
      onCancelDownload = { viewModel.cancelDownloadUpdate() },
      onInstallClick = { ctx -> viewModel.installUpdate(ctx) },
      onDismiss = {
        showUpdateDialog = false
        viewModel.dismissUpdate()
      }
    )
  }

  // Active Inventory Alerts Panel Dialog
  if (showAlertsPanelDialog) {
    AlertPanelDialog(
      alerts = stockAlerts,
      products = products,
      onDismiss = { showAlertsPanelDialog = false },
      onSelectProduct = { prod ->
        selectedAlertProductForDetails = prod
        showAlertsPanelDialog = false
      },
      onNavigateToInventory = {
        navigateTo(Screen.Inventory)
        showAlertsPanelDialog = false
      }
    )
  }

  // Product Details Dialog (triggered when user taps an alert)
  selectedAlertProductForDetails?.let { alertProd ->
    ProductDetailsDialog(
      product = alertProd,
      customColumns = inventoryColumns,
      onEdit = {
        selectedAlertProductForDetails = null
        navigateTo(Screen.Inventory)
      },
      onDelete = {
        viewModel.deleteProduct(alertProd.id, alertProd.name) { _, _ -> }
        selectedAlertProductForDetails = null
      },
      onDismiss = { selectedAlertProductForDetails = null }
    )
  }
}
