package com.manglamfertilizer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.manglamfertilizer.app.data.local.AppDatabase
import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.CategoryItem
import com.manglamfertilizer.app.data.model.CloudSyncState
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.DailyHighlight
import com.manglamfertilizer.app.data.model.DashboardMetrics
import com.manglamfertilizer.app.data.model.GreetingInfo
import com.manglamfertilizer.app.data.model.InventoryHistoryItem
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.model.AuditCleanupRun
import com.manglamfertilizer.app.data.model.AuditLogItem
import com.manglamfertilizer.app.data.model.AuditRetentionConstants
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.repository.AppUpdateRepository
import com.manglamfertilizer.app.data.repository.AuditRepository
import com.manglamfertilizer.app.data.repository.AuditRetentionRepository
import com.manglamfertilizer.app.data.repository.AuthRepository
import com.manglamfertilizer.app.data.repository.BillingRepository
import com.manglamfertilizer.app.data.repository.CalendarHighlightRepository
import com.manglamfertilizer.app.data.repository.CustomerRepository
import com.manglamfertilizer.app.data.repository.DashboardRepository
import com.manglamfertilizer.app.data.repository.DuplicateHandlingPolicy
import com.manglamfertilizer.app.data.repository.FirebaseHealthReport
import com.manglamfertilizer.app.data.repository.FirebaseHealthRepository
import com.manglamfertilizer.app.data.repository.InventoryRepository
import com.manglamfertilizer.app.data.repository.SettingsRepository
import com.manglamfertilizer.app.data.util.NetworkSyncObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val database = AppDatabase.getDatabase(application)
  private val networkSyncObserver = NetworkSyncObserver(application, viewModelScope)

  val auditRepository = AuditRepository.getInstance(application)
  val authRepository = AuthRepository(application, auditRepository)
  val inventoryRepository = InventoryRepository(
    database.productDao(),
    database.categoryDao(),
    database.inventoryHistoryDao(),
    auditRepository
  )
  val customerRepository = CustomerRepository(database.customerDao(), auditRepository)
  val billingRepository = BillingRepository(
    database.invoiceDao(),
    database.productDao(),
    database.customerDao(),
    database.creditRecordDao(),
    customerRepository,
    auditRepository
  )
  val dashboardRepository = DashboardRepository(inventoryRepository, billingRepository, customerRepository)
  val calendarHighlightRepository = CalendarHighlightRepository()
  val settingsRepository = SettingsRepository(application)
  val appUpdateRepository = AppUpdateRepository.getInstance(application)
  val auditRetentionRepository = AuditRetentionRepository.getInstance(application)
  val firebaseHealthRepository = FirebaseHealthRepository.getInstance(application)

  val currentUser: StateFlow<User?> = authRepository.currentUser
  val isSessionUnlocked: StateFlow<Boolean> = authRepository.isSessionUnlocked
  val cloudSyncState: StateFlow<CloudSyncState> = networkSyncObserver.syncState
  val syncStatusText: StateFlow<String> = networkSyncObserver.syncStatusText
  val firebaseHealthReport: StateFlow<FirebaseHealthReport?> = firebaseHealthRepository.healthReport
  val updateInfo: StateFlow<AppUpdateInfo> = appUpdateRepository.updateInfo
  val releaseHistory: StateFlow<List<com.manglamfertilizer.app.data.model.ReleaseHistoryItem>> = appUpdateRepository.releaseHistory
  val githubStatus: StateFlow<com.manglamfertilizer.app.data.repository.GitHubStatusInfo> = appUpdateRepository.githubStatus
  val creditRecords: StateFlow<List<com.manglamfertilizer.app.data.model.CreditRecord>> = billingRepository.creditRecords
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val auditLogs: StateFlow<List<AuditLogItem>> = auditRepository.auditLogs
  val isAuditLoading: StateFlow<Boolean> = auditRepository.isLoading

  val auditCleanupRuns: StateFlow<List<AuditCleanupRun>> = auditRetentionRepository.cleanupRuns
  val isRetentionSimulating: StateFlow<Boolean> = auditRetentionRepository.isSimulating
  val latestRetentionSimulation: StateFlow<AuditCleanupRun?> = auditRetentionRepository.latestSimulation
  val invoiceNumberConfig: StateFlow<InvoiceNumberConfig> = settingsRepository.invoiceNumberConfig

  init {
    com.manglamfertilizer.app.data.util.DeletedRecordsTracker.init(application)
    appUpdateRepository.checkPostInstallSuccess()
    inventoryRepository.attachNetworkObserver(networkSyncObserver, viewModelScope)
    billingRepository.attachNetworkObserver(networkSyncObserver)
    billingRepository.attachSettingsRepository(settingsRepository)
    customerRepository.attachNetworkObserver(networkSyncObserver)

    viewModelScope.launch {
      currentUser.collect { user ->
        if (user != null) {
          networkSyncObserver.triggerFirestoreReachabilityCheck(isImmediate = true)
          inventoryRepository.startRealtimeSync(viewModelScope)
          customerRepository.startRealtimeSync(viewModelScope)
          billingRepository.startRealtimeSync(viewModelScope)
          settingsRepository.startRealtimeSync(viewModelScope)

          // Initial batch sync from Firestore
          launch { inventoryRepository.syncWithFirestore() }
          launch { customerRepository.syncWithFirestore() }
          launch { billingRepository.syncWithFirestore() }
          launch { settingsRepository.syncWithFirestore() }

          if (user.role == com.manglamfertilizer.app.data.model.UserRole.ADMIN) {
            auditRepository.startListening(limit = 200)
            auditRetentionRepository.startListeningRuns()
          } else {
            auditRepository.stopListening()
            auditRetentionRepository.stopListeningRuns()
          }
        } else {
          networkSyncObserver.onUserSignedOut()
          inventoryRepository.stopRealtimeSync()
          customerRepository.stopRealtimeSync()
          billingRepository.stopRealtimeSync()
          settingsRepository.stopRealtimeSync()
          auditRepository.stopListening()
          auditRetentionRepository.stopListeningRuns()
        }
      }
    }
  }

  fun refreshAuditLogs(limit: Long = 200) {
    auditRepository.refreshLogs(limit)
  }

  private val _isRefreshingHome = MutableStateFlow(false)
  val isRefreshingHome: StateFlow<Boolean> = _isRefreshingHome.asStateFlow()

  private var homeRefreshJob: Job? = null

  /**
   * Safe Pull-to-Refresh implementation for Home Dashboard.
   * Single-flight execution prevents duplicate concurrent tasks.
   * Performs read-only cloud reconciliation without overwriting or generating duplicate writes.
   */
  fun refreshHomeData(onComplete: ((Boolean) -> Unit)? = null) {
    if (_isRefreshingHome.value || homeRefreshJob?.isActive == true) {
      return // Single-flight guard: avoid duplicate concurrent refresh runs
    }

    homeRefreshJob = viewModelScope.launch {
      _isRefreshingHome.value = true
      try {
        networkSyncObserver.triggerFirestoreReachabilityCheck(isImmediate = true)

        if (currentUser.value != null) {
          withTimeoutOrNull(6000L) {
            joinAll(
              launch { inventoryRepository.syncWithFirestore() },
              launch { customerRepository.syncWithFirestore() },
              launch { billingRepository.syncWithFirestore() },
              launch { settingsRepository.syncWithFirestore() }
            )
          }
        }
        onComplete?.invoke(true)
      } catch (e: Exception) {
        onComplete?.invoke(false)
      } finally {
        _isRefreshingHome.value = false
      }
    }
  }

  private var syncRetryJob: Job? = null

  fun retryCloudSync() {
    networkSyncObserver.retryConnection()
    if (currentUser.value != null) {
      if (syncRetryJob?.isActive == true) {
        return // Avoid concurrent duplicate repository sync jobs
      }
      syncRetryJob = viewModelScope.launch {
        inventoryRepository.startRealtimeSync(viewModelScope)
        customerRepository.startRealtimeSync(viewModelScope)
        billingRepository.startRealtimeSync(viewModelScope)
        settingsRepository.startRealtimeSync(viewModelScope)
        launch { inventoryRepository.syncWithFirestore() }
        launch { customerRepository.syncWithFirestore() }
        launch { billingRepository.syncWithFirestore() }
        launch { settingsRepository.syncWithFirestore() }
      }
    }
  }

  fun runFirebaseHealthCheck() {
    viewModelScope.launch {
      firebaseHealthRepository.runFullHealthCheck()
    }
  }

  fun runRetentionSimulation(simulatedDateKey: String, retentionDays: Int = 365) {
    viewModelScope.launch {
      val adminEmail = currentUser.value?.email ?: "kartik.bharadwaj0105@gmail.com"
      auditRetentionRepository.runSafetySimulation(
        simulatedDateKey = simulatedDateKey,
        retentionDays = retentionDays,
        adminEmail = adminEmail
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    inventoryRepository.stopRealtimeSync()
    customerRepository.stopRealtimeSync()
    billingRepository.stopRealtimeSync()
    settingsRepository.stopRealtimeSync()
    auditRepository.stopListening()
    auditRetentionRepository.stopListeningRuns()
  }

  private val _greetingInfo = MutableStateFlow(authRepository.getGreetingInfo())
  val greetingInfo: StateFlow<GreetingInfo> = _greetingInfo.asStateFlow()

  private val _todayHighlight = MutableStateFlow(calendarHighlightRepository.getTodayHighlight())
  val todayHighlight: StateFlow<DailyHighlight> = _todayHighlight.asStateFlow()

  val dashboardMetrics: StateFlow<DashboardMetrics> = dashboardRepository.dashboardMetrics
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

  val stockAlerts: StateFlow<List<StockAlert>> = dashboardRepository.stockAlerts
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentInvoices: StateFlow<List<Invoice>> = billingRepository.getTodayInvoices()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val products: StateFlow<List<Product>> = inventoryRepository.products
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val categories: StateFlow<List<CategoryItem>> = inventoryRepository.categories
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val inventoryHistory: StateFlow<List<InventoryHistoryItem>> = inventoryRepository.history
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val customers: StateFlow<List<Customer>> = customerRepository.customers
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val allInvoices: StateFlow<List<Invoice>> = billingRepository.invoices
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val appLanguage: StateFlow<String> = settingsRepository.language
  val appThemeMode: StateFlow<String> = settingsRepository.themeMode
  val inventoryColumns: StateFlow<List<com.manglamfertilizer.app.data.model.InventoryColumnConfig>> = settingsRepository.inventoryColumns
  val dailyAccountsColumns: StateFlow<List<com.manglamfertilizer.app.data.model.DailyAccountsColumnConfig>> = settingsRepository.dailyAccountsColumns

  fun saveInventoryColumns(columns: List<com.manglamfertilizer.app.data.model.InventoryColumnConfig>) {
    settingsRepository.saveInventoryColumns(columns, currentUser.value?.email ?: "")
  }

  fun addCustomField(title: String, dataType: String = "Text") {
    settingsRepository.addCustomField(title, dataType, currentUser.value?.email ?: "")
  }

  fun renameCustomField(id: String, newTitle: String) {
    settingsRepository.renameCustomField(id, newTitle, currentUser.value?.email ?: "")
  }

  fun deleteCustomField(id: String) {
    settingsRepository.deleteCustomField(id, currentUser.value?.email ?: "")
  }

  fun deleteField(id: String) {
    settingsRepository.deleteField(id, currentUser.value?.email ?: "")
  }

  fun saveDailyAccountsColumns(columns: List<com.manglamfertilizer.app.data.model.DailyAccountsColumnConfig>) {
    settingsRepository.saveDailyAccountsColumns(columns, currentUser.value?.email ?: "")
  }

  fun addCustomDailyAccountsField(title: String, dataType: String = "Text") {
    settingsRepository.addCustomDailyAccountsField(title, dataType, currentUser.value?.email ?: "")
  }

  fun renameDailyAccountsField(id: String, newTitle: String) {
    settingsRepository.renameDailyAccountsField(id, newTitle, currentUser.value?.email ?: "")
  }

  fun deleteDailyAccountsField(id: String) {
    settingsRepository.deleteDailyAccountsField(id, currentUser.value?.email ?: "")
  }

  fun resetDailyAccountsColumns() {
    settingsRepository.resetDailyAccountsColumns(currentUser.value?.email ?: "")
  }

  fun login(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val result = authRepository.login(email, pass)
      if (result.isSuccess) {
        _greetingInfo.value = authRepository.getGreetingInfo()
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Authentication failed")
      }
    }
  }

  fun sendPasswordReset(email: String, onComplete: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val result = authRepository.sendPasswordReset(email)
      if (result.isSuccess) {
        onComplete(true, result.getOrNull() ?: "Password reset email sent")
      } else {
        onComplete(false, result.exceptionOrNull()?.message ?: "Failed to send reset email")
      }
    }
  }

  fun logout() {
    viewModelScope.launch {
      authRepository.logout()
    }
  }

  fun setLanguage(lang: String) {
    settingsRepository.setLanguage(lang)
  }

  fun setThemeMode(mode: String) {
    settingsRepository.setThemeMode(mode)
  }

  fun isDisplayNameSetupCompleted(userId: String): Boolean {
    return authRepository.isDisplayNameSetupCompleted(userId)
  }

  fun setDisplayNameSetupCompleted(userId: String, completed: Boolean) {
    authRepository.setDisplayNameSetupCompleted(userId, completed)
  }

  fun getFirebaseDisplayName(): String? {
    return authRepository.getFirebaseDisplayName()
  }

  fun updatePreferredGreetingName(userId: String, name: String) {
    authRepository.updatePreferredGreetingName(userId, name)
  }

  fun unlockSession() {
    authRepository.unlockSession()
  }

  fun lockSession() {
    authRepository.lockSession()
  }

  fun isDeviceUnlockEnabled(userId: String): Boolean {
    return authRepository.isDeviceUnlockEnabled(userId)
  }

  fun setDeviceUnlockEnabled(userId: String, enabled: Boolean) {
    authRepository.setDeviceUnlockEnabled(userId, enabled)
  }

  fun hasPromptedDeviceUnlock(userId: String): Boolean {
    return authRepository.hasPromptedDeviceUnlock(userId)
  }

  fun setPromptedDeviceUnlock(userId: String, prompted: Boolean) {
    authRepository.setPromptedDeviceUnlock(userId, prompted)
  }

  fun addProduct(
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
    rack: String = "",
    hsn: String = "",
    chemicalComposition: String = "",
    barcode: String = "",
    packaging: String = "",
    crop: String = "",
    usesInstructions: String = "",
    customFields: String = "",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val user = currentUser.value
        val result = inventoryRepository.addProduct(
          name = name,
          category = category,
          company = company,
          unit = unit,
          batchNumber = batch,
          purchasePrice = purchasePrice,
          sellingPrice = sellingPrice,
          mrp = mrp,
          stockQuantity = stock,
          minStockAlert = minAlert,
          expiryDate = expiryDate,
          rackLocation = rack,
          hsnCode = hsn,
          chemicalComposition = chemicalComposition,
          barcode = barcode,
          packaging = packaging,
          crop = crop,
          usesInstructions = usesInstructions,
          customFields = customFields,
          userName = user?.name ?: "Admin",
          userEmail = user?.email ?: ""
        )
        if (result.isSuccess) {
          onResult(true, null)
        } else {
          onResult(false, result.exceptionOrNull()?.message ?: "Failed to add product")
        }
      } catch (e: Exception) {
        onResult(false, e.message ?: "Failed to add product")
      }
    }
  }

  fun importProductsBatch(
    products: List<Product>,
    policy: DuplicateHandlingPolicy,
    fileType: String,
    totalRows: Int,
    onResult: (Boolean, Int, Int, String?) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val user = currentUser.value
        val result = inventoryRepository.importProductsBatch(
          productsToImport = products,
          policy = policy,
          fileType = fileType,
          totalRows = totalRows,
          userName = user?.name ?: "Admin",
          userEmail = user?.email ?: ""
        )
        if (result.isSuccess) {
          val (imported, skipped) = result.getOrNull() ?: Pair(0, 0)
          onResult(true, imported, skipped, null)
        } else {
          onResult(false, 0, 0, result.exceptionOrNull()?.message ?: "Import failed")
        }
      } catch (e: Exception) {
        onResult(false, 0, 0, e.message ?: "Import failed")
      }
    }
  }

  fun updateProduct(
    product: Product,
    previousProduct: Product? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val user = currentUser.value
        val result = inventoryRepository.updateProduct(
          product = product,
          previousProduct = previousProduct,
          userName = user?.name ?: "Admin",
          userEmail = user?.email ?: ""
        )
        if (result.isSuccess) {
          onResult(true, null)
        } else {
          onResult(false, result.exceptionOrNull()?.message ?: "Failed to update product")
        }
      } catch (e: Exception) {
        onResult(false, e.message ?: "Failed to update product")
      }
    }
  }

  fun deleteProduct(
    productId: String,
    productName: String = "",
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      try {
        val user = currentUser.value
        val email = user?.email?.takeIf { it.isNotBlank() }
          ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
        val result = inventoryRepository.deleteProduct(
          productId = productId,
          productName = productName,
          userName = user?.name ?: "Admin",
          userEmail = email
        )
        if (result.isSuccess) {
          onResult(true, null)
        } else {
          onResult(false, result.exceptionOrNull()?.message ?: "Failed to delete product")
        }
      } catch (e: Exception) {
        onResult(false, e.message ?: "Failed to delete product")
      }
    }
  }

  fun addCategory(name: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      val result = inventoryRepository.addCategory(
        name = name,
        userName = user?.name ?: "Admin",
        userEmail = user?.email ?: ""
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to create category")
      }
    }
  }

  fun updateCategory(category: CategoryItem, oldName: String = "", onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      val result = inventoryRepository.updateCategory(
        category = category,
        oldName = oldName,
        userName = user?.name ?: "Admin",
        userEmail = user?.email ?: ""
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to update category")
      }
    }
  }

  fun deleteCategory(categoryId: String, categoryName: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      val email = user?.email?.takeIf { it.isNotBlank() }
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
      val result = inventoryRepository.deleteCategory(
        categoryId = categoryId,
        categoryName = categoryName,
        userName = user?.name ?: "Admin",
        userEmail = email
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to delete category")
      }
    }
  }

  fun reorderCategories(items: List<CategoryItem>, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val result = inventoryRepository.reorderCategories(items)
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to reorder categories")
      }
    }
  }

  fun logInventoryExport(format: String, itemCount: Int, details: String) {
    viewModelScope.launch {
      val user = currentUser.value
      inventoryRepository.logHistory(
        actionType = "Exported Inventory",
        productName = "$format Export",
        details = "Exported $itemCount products to $format ($details)",
        userName = user?.name ?: "Admin",
        userEmail = user?.email ?: "",
        newValue = "$itemCount items ($format)"
      )
    }
  }

  fun addCustomer(name: String, phone: String, village: String, address: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val result = customerRepository.addCustomer(name, phone, village, address)
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to add customer")
      }
    }
  }

  fun updateCustomer(customer: Customer, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      val result = customerRepository.updateCustomer(
        customer = customer,
        userEmail = user?.email ?: "",
        userName = user?.name ?: "Admin"
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to update customer")
      }
    }
  }

  fun recordDuePayment(
    customerId: String,
    amount: Double,
    creditId: String? = null,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val user = currentUser.value
      val result = billingRepository.recordDuePayment(
        customerId = customerId,
        creditId = creditId,
        paymentAmount = amount,
        userEmail = user?.email ?: "",
        userName = user?.name ?: "Admin"
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to record payment")
      }
    }
  }

  fun deleteCustomer(customerId: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      val email = user?.email?.takeIf { it.isNotBlank() }
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
      val result = customerRepository.deleteCustomer(
        customerId = customerId,
        userEmail = email,
        userName = user?.name ?: "Admin"
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to delete customer")
      }
    }
  }

  fun deleteInvoice(invoiceId: String, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      val email = user?.email?.takeIf { it.isNotBlank() }
        ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
      val result = billingRepository.deleteInvoice(
        invoiceId = invoiceId,
        deletedBy = user?.name ?: "Admin",
        deletedByEmail = email
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to delete invoice")
      }
    }
  }

  fun createInvoice(
    customerId: String?,
    customerName: String,
    customerPhone: String = "",
    customerAadhaar: String = "",
    customerAddress: String = "",
    customerVillage: String = "",
    items: List<InvoiceItem>,
    gstRate: Double = 0.0,
    discount: Double = 0.0,
    amountPaid: Double = 0.0,
    dueDate: Long? = null,
    paymentMode: PaymentMode = PaymentMode.CASH,
    onResult: (Boolean, String?) -> Unit
  ) {
    createInvoiceWithResult(
      customerId = customerId,
      customerName = customerName,
      customerPhone = customerPhone,
      customerAadhaar = customerAadhaar,
      customerAddress = customerAddress,
      customerVillage = customerVillage,
      items = items,
      gstRate = gstRate,
      discount = discount,
      amountPaid = amountPaid,
      dueDate = dueDate,
      paymentMode = paymentMode
    ) { success, msg, _ ->
      onResult(success, msg)
    }
  }

  fun createInvoiceWithResult(
    customerId: String?,
    customerName: String,
    customerPhone: String = "",
    customerAadhaar: String = "",
    customerAddress: String = "",
    customerVillage: String = "",
    items: List<InvoiceItem>,
    gstRate: Double = 0.0,
    discount: Double = 0.0,
    amountPaid: Double = 0.0,
    dueDate: Long? = null,
    paymentMode: PaymentMode = PaymentMode.CASH,
    onResult: (Boolean, String?, Invoice?) -> Unit
  ) {
    viewModelScope.launch {
      val user = currentUser.value
      val result = billingRepository.createInvoice(
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        customerAadhaar = customerAadhaar,
        customerAddress = customerAddress,
        customerVillage = customerVillage,
        items = items,
        gstRate = gstRate,
        discount = discount,
        amountPaid = amountPaid,
        dueDate = dueDate,
        paymentMode = paymentMode,
        createdBy = user?.name ?: "Admin"
      )
      if (result.isSuccess) {
        onResult(true, null, result.getOrNull())
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to create invoice", null)
      }
    }
  }

  // --- Admin-Controlled Invoice Number Configuration Actions ---

  fun setStartingInvoiceNumber(startingNumber: Long, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      if (user?.role != com.manglamfertilizer.app.data.model.UserRole.ADMIN) {
        onResult(false, "Unauthorized: Only Admin users can configure invoice numbers.")
        return@launch
      }
      val result = settingsRepository.setStartingInvoiceNumber(
        startingNumber = startingNumber,
        userEmail = user.email.ifBlank { "Admin" }
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to set starting invoice number")
      }
    }
  }

  fun overrideNextInvoiceNumber(nextNumber: Long, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      if (user?.role != com.manglamfertilizer.app.data.model.UserRole.ADMIN) {
        onResult(false, "Unauthorized: Only Admin users can change invoice numbering.")
        return@launch
      }
      val result = settingsRepository.overrideNextInvoiceNumber(
        nextNumber = nextNumber,
        userEmail = user.email.ifBlank { "Admin" }
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to update invoice number")
      }
    }
  }

  fun saveInvoiceNumberConfig(config: InvoiceNumberConfig, onResult: (Boolean, String?) -> Unit) {
    viewModelScope.launch {
      val user = currentUser.value
      if (user?.role != com.manglamfertilizer.app.data.model.UserRole.ADMIN) {
        onResult(false, "Unauthorized: Only Admin users can save invoice number configuration.")
        return@launch
      }
      val result = settingsRepository.saveInvoiceNumberConfig(
        config = config,
        userEmail = user.email.ifBlank { "Admin" }
      )
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to save configuration")
      }
    }
  }

  // --- In-App Update Engine Actions ---

  fun checkForUpdates(isManual: Boolean = false) {
    viewModelScope.launch {
      appUpdateRepository.checkPostInstallSuccess()
      appUpdateRepository.checkForUpdate(isManual = isManual)
    }
  }

  fun startDownloadUpdate() {
    appUpdateRepository.startDownload(viewModelScope)
  }

  fun cancelDownloadUpdate() {
    appUpdateRepository.cancelDownload()
  }

  fun installUpdate(context: android.content.Context): Result<Unit> {
    return appUpdateRepository.installUpdate(context)
  }

  fun dismissUpdate() {
    appUpdateRepository.dismissUpdate()
  }

  fun recordUpdateDismissed(versionCode: Long) {
    appUpdateRepository.recordDismissed(versionCode)
  }

  fun setCustomManifestUrl(url: String) {
    appUpdateRepository.setManifestUrl(url)
  }

  fun resetManifestUrl() {
    appUpdateRepository.resetManifestUrl()
  }

  fun getManifestUrl(): String {
    return appUpdateRepository.getManifestUrl()
  }

  fun publishRelease(
    manifest: com.manglamfertilizer.app.data.model.UpdateManifest,
    adminEmail: String,
    onResult: (Boolean, String?) -> Unit
  ) {
    viewModelScope.launch {
      val result = appUpdateRepository.publishRelease(manifest, adminEmail)
      if (result.isSuccess) {
        onResult(true, null)
      } else {
        onResult(false, result.exceptionOrNull()?.message ?: "Failed to publish release")
      }
    }
  }

  fun simulateRelease(
    versionName: String,
    versionCode: Long,
    releaseType: com.manglamfertilizer.app.data.model.ReleaseType,
    daysAgo: Int = 0,
    forceAfterDays: Int = 15,
    releaseNotes: String = "• Testing update distribution flow\n• UI performance enhancements\n• Bug fixes"
  ) {
    appUpdateRepository.simulateRelease(
      versionName = versionName,
      versionCode = versionCode,
      releaseType = releaseType,
      daysAgo = daysAgo,
      forceAfterDays = forceAfterDays,
      releaseNotes = releaseNotes
    )
  }
}
