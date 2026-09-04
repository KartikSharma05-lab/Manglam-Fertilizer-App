package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.manglamfertilizer.app.data.model.AuditLogItem
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuditRepository private constructor(context: Context) {
  private val tag = "AuditRepository"
  private val scope = CoroutineScope(Dispatchers.IO)

  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("manglam_audit_prefs", Context.MODE_PRIVATE)

  private val firestore: FirebaseFirestore? get() = com.manglamfertilizer.app.data.util.FirestoreProvider.get()

  private val firebaseAuth: FirebaseAuth? get() = com.manglamfertilizer.app.data.util.FirestoreProvider.auth

  private val _auditLogs = MutableStateFlow<List<AuditLogItem>>(emptyList())
  val auditLogs: StateFlow<List<AuditLogItem>> = _auditLogs.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private var logsListener: ListenerRegistration? = null

  val deviceInstallationId: String by lazy {
    getOrCreateInstallationId()
  }

  init {
    startListening()
  }

  private fun getOrCreateInstallationId(): String {
    val existing = prefs.getString("device_installation_id", null)
    if (!existing.isNullOrBlank()) return existing
    val newId = "dev_" + UUID.randomUUID().toString().replace("-", "").take(16)
    prefs.edit().putString("device_installation_id", newId).apply()
    return newId
  }

  /**
   * Logs an authoritative audit event to Firestore collection `auditLogs`.
   * Strictly uses FieldValue.serverTimestamp() for the authoritative event timestamp.
   * Append-only; cannot be altered or rewritten by standard operations.
   */
  fun logEvent(
    action: String,
    entityType: String,
    entityId: String,
    description: String,
    userEmail: String? = null,
    userId: String? = null,
    userRole: String? = null,
    metadata: Map<String, Any?> = emptyMap()
  ) {
    scope.launch {
      try {
        val db = firestore ?: return@launch
        val authUser = firebaseAuth?.currentUser
        if (authUser == null) {
          Log.d(tag, "Skipping remote audit log: User not authenticated in Firebase Auth yet")
          return@launch
        }
        val effectiveEmail = (userEmail?.trim()?.takeIf { it.isNotBlank() && it.contains("@") }
          ?: authUser.email?.trim() ?: "").lowercase(java.util.Locale.ROOT)
        val effectiveUserId = userId?.trim()?.takeIf { it.isNotBlank() }
          ?: authUser.uid
        val effectiveRole = userRole?.trim()?.takeIf { it.isNotBlank() }
          ?: if (AdminAuthUtils.isAdmin(effectiveEmail)) "ADMIN" else "STAFF"

        val logId = "log_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        // Clean metadata strings (filter out nulls, convert values to strings, omit sensitive credentials)
        val cleanMetadata = mutableMapOf<String, String>()
        metadata.forEach { (k, v) ->
          if (v != null && !k.contains("password", ignoreCase = true) && !k.contains("token", ignoreCase = true) && !k.contains("secret", ignoreCase = true)) {
            cleanMetadata[k] = v.toString()
          }
        }

        val docData = hashMapOf(
          "logId" to logId,
          "userId" to effectiveUserId,
          "userEmail" to effectiveEmail,
          "userRole" to effectiveRole,
          "action" to action.trim(),
          "entityType" to entityType.trim(),
          "entityId" to entityId.trim(),
          "description" to description.trim(),
          "timestamp" to FieldValue.serverTimestamp(),
          "clientTimestamp" to System.currentTimeMillis(),
          "deviceInstallationId" to deviceInstallationId,
          "metadata" to cleanMetadata
        )

        // Write to canonical single collection: audit_logs
        db.collection("audit_logs").document(logId).set(docData).await()
        Log.d(tag, "Audit event logged: $action on $entityType:$entityId by $effectiveEmail ($effectiveRole)")
      } catch (e: Exception) {
        Log.e(tag, "Failed to write audit log to Firestore: ${e.message}", e)
      }
    }
  }

  // ----------------------------------------------------
  // TYPED AUDIT ACTION LOGGING HELPERS
  // ----------------------------------------------------

  fun logUserLogin(
    email: String,
    role: String,
    userId: String,
    loginMethod: String = "Email/Password"
  ) {
    logEvent(
      action = "LOGIN",
      entityType = "Auth",
      entityId = userId,
      description = "User signed in successfully ($email as $role)",
      userEmail = email,
      userId = userId,
      userRole = role,
      metadata = mapOf(
        "loginMethod" to loginMethod,
        "deviceInstallationId" to deviceInstallationId
      )
    )
  }

  fun logUserLogout(
    email: String,
    role: String,
    userId: String
  ) {
    logEvent(
      action = "LOGOUT",
      entityType = "Auth",
      entityId = userId,
      description = "User logged out ($email)",
      userEmail = email,
      userId = userId,
      userRole = role,
      metadata = mapOf(
        "deviceInstallationId" to deviceInstallationId
      )
    )
  }

  fun logProductCreated(
    product: Product,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "PRODUCT_CREATED",
      entityType = "Product",
      entityId = product.id,
      description = "Created product '${product.name}' (${product.company}) with ${product.stockQuantity.toInt()} ${product.unit}",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "productName" to product.name,
        "company" to product.company,
        "category" to product.category,
        "chemicalComposition" to product.chemicalComposition,
        "sellingPrice" to product.sellingPrice,
        "purchasePrice" to product.purchasePrice,
        "mrp" to product.mrp,
        "stockQuantity" to product.stockQuantity,
        "unit" to product.unit,
        "minStockAlert" to product.minStockAlert,
        "barcode" to product.barcode
      )
    )
  }

  fun logProductUpdated(
    productId: String,
    productName: String,
    changesSummary: String,
    changedFields: Map<String, Any?>,
    userEmail: String,
    userRole: String,
    userId: String,
    previousSnapshot: Map<String, Any?>? = null
  ) {
    val meta = mutableMapOf<String, Any?>()
    meta["productName"] = productName
    meta["changesSummary"] = changesSummary
    changedFields.forEach { (k, v) -> meta["field_$k"] = v }
    previousSnapshot?.forEach { (k, v) -> meta["prev_$k"] = v }

    logEvent(
      action = "PRODUCT_UPDATED",
      entityType = "Product",
      entityId = productId,
      description = "Updated product '$productName': $changesSummary",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = meta
    )
  }

  fun logProductDeleted(
    productId: String,
    productName: String,
    userEmail: String,
    userRole: String,
    userId: String,
    deletedSnapshot: Product? = null
  ) {
    val meta = mutableMapOf<String, Any?>()
    meta["productName"] = productName
    if (deletedSnapshot != null) {
      meta["company"] = deletedSnapshot.company
      meta["category"] = deletedSnapshot.category
      meta["sellingPrice"] = deletedSnapshot.sellingPrice
      meta["lastStock"] = deletedSnapshot.stockQuantity
      meta["chemicalComposition"] = deletedSnapshot.chemicalComposition
    }

    logEvent(
      action = "PRODUCT_DELETED",
      entityType = "Product",
      entityId = productId,
      description = "Deleted product '$productName' (ID: $productId)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = meta
    )
  }

  fun logCategoryCreated(
    categoryId: String,
    categoryName: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CATEGORY_CREATED",
      entityType = "Category",
      entityId = categoryId,
      description = "Created product category '$categoryName'",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("categoryName" to categoryName)
    )
  }

  fun logCategoryUpdated(
    categoryId: String,
    oldName: String,
    newName: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CATEGORY_UPDATED",
      entityType = "Category",
      entityId = categoryId,
      description = "Renamed category from '$oldName' to '$newName'",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("oldName" to oldName, "newName" to newName)
    )
  }

  fun logCategoryDeleted(
    categoryId: String,
    categoryName: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CATEGORY_DELETED",
      entityType = "Category",
      entityId = categoryId,
      description = "Deleted category '$categoryName'",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("categoryName" to categoryName)
    )
  }

  fun logInvoiceCreated(
    invoiceId: String,
    invoiceNumber: String,
    customerName: String,
    amount: Double,
    paymentMode: String,
    userEmail: String,
    userRole: String,
    userId: String,
    itemCount: Int
  ) {
    logEvent(
      action = "INVOICE_CREATED",
      entityType = "Invoice",
      entityId = invoiceId,
      description = "Created Invoice #$invoiceNumber for $customerName (₹$amount, $paymentMode, $itemCount items)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "customerName" to customerName,
        "amount" to amount,
        "paymentMode" to paymentMode,
        "itemCount" to itemCount
      )
    )
  }

  fun logInvoiceUpdated(
    invoiceId: String,
    invoiceNumber: String,
    customerName: String,
    amount: Double,
    status: String,
    changesSummary: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "INVOICE_UPDATED",
      entityType = "Invoice",
      entityId = invoiceId,
      description = "Updated Invoice #$invoiceNumber for $customerName: $changesSummary (Status: $status)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "customerName" to customerName,
        "amount" to amount,
        "status" to status,
        "changesSummary" to changesSummary
      )
    )
  }

  fun logInvoiceCancelled(
    invoiceId: String,
    invoiceNumber: String,
    customerName: String,
    amount: Double,
    reason: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "INVOICE_CANCELLED",
      entityType = "Invoice",
      entityId = invoiceId,
      description = "Cancelled Invoice #$invoiceNumber for $customerName (₹$amount). Reason: ${reason.ifBlank { "N/A" }}",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "customerName" to customerName,
        "amount" to amount,
        "reason" to reason
      )
    )
  }

  fun logInvoiceDeleted(
    invoiceId: String,
    invoiceNumber: String,
    customerName: String,
    amount: Double,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "INVOICE_DELETED",
      entityType = "Invoice",
      entityId = invoiceId,
      description = "Deleted Invoice #$invoiceNumber for $customerName (₹$amount)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "invoiceNumber" to invoiceNumber,
        "customerName" to customerName,
        "amount" to amount
      )
    )
  }

  fun logCustomerCreated(
    customerId: String,
    customerName: String,
    phone: String,
    village: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CUSTOMER_CREATED",
      entityType = "Customer",
      entityId = customerId,
      description = "Created customer '$customerName' (${phone.ifBlank { "No Phone" }}, ${village.ifBlank { "General" }})",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "customerName" to customerName,
        "phone" to phone,
        "village" to village
      )
    )
  }

  fun logCustomerUpdated(
    customerId: String,
    customerName: String,
    changesSummary: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CUSTOMER_UPDATED",
      entityType = "Customer",
      entityId = customerId,
      description = "Updated customer '$customerName': $changesSummary",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "customerName" to customerName,
        "changesSummary" to changesSummary
      )
    )
  }

  fun logCustomerDeleted(
    customerId: String,
    customerName: String,
    userEmail: String,
    userRole: String,
    userId: String,
    lastOutstanding: Double = 0.0
  ) {
    logEvent(
      action = "CUSTOMER_DELETED",
      entityType = "Customer",
      entityId = customerId,
      description = "Deleted customer '$customerName' (Outstanding balance: ₹$lastOutstanding)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "customerName" to customerName,
        "lastOutstanding" to lastOutstanding
      )
    )
  }

  fun logCreditRecordCreated(
    creditId: String,
    invoiceNumber: String,
    customerName: String,
    amount: Double,
    dueDate: Long?,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CREDIT_RECORD_CREATED",
      entityType = "Credit",
      entityId = creditId,
      description = "Created due record of ₹$amount for $customerName on Invoice #$invoiceNumber",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "creditId" to creditId,
        "invoiceNumber" to invoiceNumber,
        "customerName" to customerName,
        "amount" to amount,
        "dueDate" to (dueDate ?: 0L)
      )
    )
  }

  fun logCreditPaymentRecorded(
    creditId: String,
    customerName: String,
    paymentAmount: Double,
    previousDue: Double,
    newRemainingDue: Double,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CREDIT_PAYMENT_RECORDED",
      entityType = "Credit",
      entityId = creditId,
      description = "Recorded payment of ₹$paymentAmount for $customerName. Remaining due: ₹$newRemainingDue (was ₹$previousDue)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "creditId" to creditId,
        "customerName" to customerName,
        "paymentAmount" to paymentAmount,
        "previousDue" to previousDue,
        "newRemainingDue" to newRemainingDue
      )
    )
  }

  fun logProfileNameChanged(
    oldName: String,
    newName: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "PROFILE_NAME_CHANGED",
      entityType = "Settings",
      entityId = userId,
      description = "User changed display name from '$oldName' to '$newName'",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("oldName" to oldName, "newName" to newName)
    )
  }

  fun logThemeChanged(
    oldTheme: String,
    newTheme: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "THEME_CHANGED",
      entityType = "Settings",
      entityId = "app_theme",
      description = "Application theme changed to '$newTheme' (was '$oldTheme')",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("oldTheme" to oldTheme, "newTheme" to newTheme)
    )
  }

  fun logLanguageChanged(
    oldLang: String,
    newLang: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "LANGUAGE_CHANGED",
      entityType = "Settings",
      entityId = "app_language",
      description = "Application language changed to '$newLang' (was '$oldLang')",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("oldLanguage" to oldLang, "newLanguage" to newLang)
    )
  }

  fun logUpdateConfigurationChanged(
    oldUrl: String,
    newUrl: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "UPDATE_CONFIGURATION_CHANGED",
      entityType = "Settings",
      entityId = "update_manifest_url",
      description = "App update manifest URL changed from '$oldUrl' to '$newUrl'",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("oldUrl" to oldUrl, "newUrl" to newUrl)
    )
  }

  fun logUpdateChecked(
    manifestVersion: String,
    versionCode: Long,
    isManual: Boolean,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_CHECKED",
      entityType = "AppUpdate",
      entityId = "v$versionCode",
      description = "Checked for updates: remote version $manifestVersion (code $versionCode, trigger: ${if (isManual) "Manual" else "Background"})",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf(
        "manifestVersion" to manifestVersion,
        "versionCode" to versionCode,
        "isManual" to isManual
      )
    )
  }

  fun logUpdateStarted(
    version: String,
    versionCode: Long,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_STARTED",
      entityType = "AppUpdate",
      entityId = "v$versionCode",
      description = "Started update download for version $version (code $versionCode)",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf("version" to version, "versionCode" to versionCode)
    )
  }

  fun logUpdateDownloaded(
    version: String,
    fileSize: Long,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_DOWNLOADED",
      entityType = "AppUpdate",
      entityId = version,
      description = "Downloaded update APK for version $version (${fileSize / 1024} KB)",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf("version" to version, "fileSize" to fileSize)
    )
  }

  fun logUpdateVerificationFailed(
    version: String,
    reason: String,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_VERIFICATION_FAILED",
      entityType = "AppUpdate",
      entityId = version,
      description = "Update APK package verification failed for version $version: $reason",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf("version" to version, "reason" to reason)
    )
  }

  fun logUpdateInstalled(
    version: String,
    versionCode: Long,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_INSTALLED",
      entityType = "AppUpdate",
      entityId = "v$versionCode",
      description = "Initiated package installation for update version $version (code $versionCode)",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf("version" to version, "versionCode" to versionCode)
    )
  }

  fun logUpdateCompleted(
    version: String,
    versionCode: Long,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_COMPLETED",
      entityType = "AppUpdate",
      entityId = "v$versionCode",
      description = "Update to version $version (code $versionCode) verified and completed successfully",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf("version" to version, "versionCode" to versionCode)
    )
  }

  fun logProductImported(
    totalImported: Int,
    skippedCount: Int,
    fileName: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "PRODUCT_IMPORTED",
      entityType = "Product",
      entityId = "batch_import_${System.currentTimeMillis()}",
      description = "Imported $totalImported products from '$fileName' (Skipped: $skippedCount)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "totalImported" to totalImported,
        "skippedCount" to skippedCount,
        "fileName" to fileName
      )
    )
  }

  fun logCreditCreated(
    creditId: String,
    invoiceNumber: String,
    customerName: String,
    amount: Double,
    dueDate: Long?,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CREDIT_CREATED",
      entityType = "Credit",
      entityId = creditId,
      description = "Created due record of ₹$amount for $customerName on Invoice #$invoiceNumber",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "creditId" to creditId,
        "invoiceNumber" to invoiceNumber,
        "customerName" to customerName,
        "amount" to amount,
        "dueDate" to (dueDate ?: 0L)
      )
    )
  }

  fun logCreditPayment(
    creditId: String,
    customerName: String,
    paymentAmount: Double,
    previousDue: Double,
    newRemainingDue: Double,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "CREDIT_PAYMENT",
      entityType = "Credit",
      entityId = creditId,
      description = "Recorded payment of ₹$paymentAmount for $customerName. Remaining due: ₹$newRemainingDue (was ₹$previousDue)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf(
        "creditId" to creditId,
        "customerName" to customerName,
        "paymentAmount" to paymentAmount,
        "previousDue" to previousDue,
        "newRemainingDue" to newRemainingDue
      )
    )
  }

  fun logSettingsChanged(
    settingKey: String,
    oldValue: String,
    newValue: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "SETTINGS_CHANGED",
      entityType = "Settings",
      entityId = settingKey,
      description = "Changed setting '$settingKey' to '$newValue' (was '$oldValue')",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("settingKey" to settingKey, "oldValue" to oldValue, "newValue" to newValue)
    )
  }

  fun logInventoryColumnsChanged(
    visibleColumnCount: Int,
    columnsSummary: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "INVENTORY_COLUMNS_CHANGED",
      entityType = "Settings",
      entityId = "inventory_columns",
      description = "Updated custom inventory columns layout ($visibleColumnCount active columns)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("visibleColumnCount" to visibleColumnCount, "summary" to columnsSummary)
    )
  }

  fun logDailyAccountsColumnsChanged(
    visibleColumnCount: Int,
    columnsSummary: String,
    userEmail: String,
    userRole: String,
    userId: String
  ) {
    logEvent(
      action = "DAILY_ACCOUNTS_COLUMNS_CHANGED",
      entityType = "Settings",
      entityId = "daily_accounts_columns",
      description = "Updated custom daily accounts table columns ($visibleColumnCount active columns)",
      userEmail = userEmail,
      userId = userId,
      userRole = userRole,
      metadata = mapOf("visibleColumnCount" to visibleColumnCount, "summary" to columnsSummary)
    )
  }

  fun logUpdateFailed(
    version: String,
    errorCode: String,
    errorReason: String,
    userEmail: String,
    userRole: String
  ) {
    logEvent(
      action = "UPDATE_FAILED",
      entityType = "AppUpdate",
      entityId = version,
      description = "App update to $version failed ($errorCode): $errorReason",
      userEmail = userEmail,
      userRole = userRole,
      metadata = mapOf("version" to version, "errorCode" to errorCode, "reason" to errorReason)
    )
  }

  /**
   * Starts listening to recent audit logs in real-time from Firestore canonical collection `audit_logs`.
   * Only active for authenticated Administrators to comply with security policies.
   */
  fun startListening(limit: Long = 200) {
    val db = firestore ?: return
    val currentUser = firebaseAuth?.currentUser
    val email = currentUser?.email ?: ""
    if (currentUser == null || !AdminAuthUtils.isAdmin(email)) {
      Log.d(tag, "Skipping audit_logs listener: User is not an authenticated admin ($email)")
      _isLoading.value = false
      return
    }

    logsListener?.remove()

    try {
      _isLoading.value = true
      logsListener = db.collection("audit_logs")
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(limit)
        .addSnapshotListener { snapshot, error ->
          _isLoading.value = false
          if (error != null) {
            Log.w(tag, "Notice on audit_logs snapshot: ${error.message}")
            if (error.code != com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
              fallbackQuery(limit)
            }
            return@addSnapshotListener
          }

          if (snapshot != null) {
            val list = snapshot.documents.mapNotNull { doc -> parseDocToAuditLog(doc) }
            _auditLogs.value = list
          }
        }
    } catch (e: Exception) {
      _isLoading.value = false
      Log.w(tag, "Exception attaching audit_logs listener: ${e.message}")
    }
  }

  fun stopListening() {
    logsListener?.remove()
    logsListener = null
    _isLoading.value = false
  }

  fun refreshLogs(limit: Long = 200) {
    val currentUser = firebaseAuth?.currentUser
    val email = currentUser?.email ?: ""
    if (currentUser == null || !AdminAuthUtils.isAdmin(email)) {
      Log.d(tag, "Skipping refreshLogs: User is not an admin ($email)")
      _isLoading.value = false
      return
    }

    scope.launch {
      try {
        _isLoading.value = true
        val db = firestore ?: return@launch
        val querySnapshot = try {
          db.collection("audit_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        } catch (e: Exception) {
          db.collection("audit_logs")
            .limit(limit)
            .get()
            .await()
        }
        val list = querySnapshot.documents
          .mapNotNull { doc -> parseDocToAuditLog(doc) }
          .sortedByDescending { it.timestamp }
        _auditLogs.value = list
      } catch (e: Exception) {
        Log.w(tag, "Notice on refresh audit logs: ${e.message}")
      } finally {
        _isLoading.value = false
      }
    }
  }

  private fun fallbackQuery(limit: Long = 200) {
    val currentUser = firebaseAuth?.currentUser
    val email = currentUser?.email ?: ""
    if (currentUser == null || !AdminAuthUtils.isAdmin(email)) {
      return
    }

    scope.launch {
      try {
        val db = firestore ?: return@launch
        val querySnapshot = db.collection("audit_logs")
          .limit(limit)
          .get()
          .await()
        val list = querySnapshot.documents
          .mapNotNull { doc -> parseDocToAuditLog(doc) }
          .sortedByDescending { it.timestamp }
        _auditLogs.value = list
      } catch (e: Exception) {
        Log.w(tag, "Fallback audit query notice: ${e.message}")
      }
    }
  }

  private fun parseDocToAuditLog(doc: DocumentSnapshot): AuditLogItem? {
    return try {
      val logId = doc.getString("logId") ?: doc.id
      val userId = doc.getString("userId") ?: ""
      val userEmail = doc.getString("userEmail") ?: ""
      val userRole = doc.getString("userRole") ?: if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      val action = doc.getString("action") ?: "UNKNOWN_ACTION"
      val entityType = doc.getString("entityType") ?: "General"
      val entityId = doc.getString("entityId") ?: ""
      val description = doc.getString("description") ?: ""

      // Parse Firestore Server Timestamp with fallback to clientTimestamp or epoch
      val timestampVal = doc.get("timestamp")
      val timestamp: Long = when (timestampVal) {
        is Timestamp -> timestampVal.toDate().time
        is Date -> timestampVal.time
        is Long -> timestampVal
        is Number -> timestampVal.toLong()
        else -> doc.getLong("clientTimestamp") ?: System.currentTimeMillis()
      }

      val deviceInstallationId = doc.getString("deviceInstallationId") ?: ""

      val rawMetadata = doc.get("metadata") as? Map<*, *>
      val metadataMap = mutableMapOf<String, String>()
      rawMetadata?.forEach { (k, v) ->
        if (k != null && v != null) {
          metadataMap[k.toString()] = v.toString()
        }
      }

      AuditLogItem(
        logId = logId,
        userId = userId,
        userEmail = userEmail,
        userRole = userRole,
        action = action,
        entityType = entityType,
        entityId = entityId,
        description = description,
        timestamp = timestamp,
        deviceInstallationId = deviceInstallationId,
        metadata = metadataMap
      )
    } catch (e: Exception) {
      Log.e(tag, "Failed to parse audit log document ${doc.id}: ${e.message}", e)
      null
    }
  }

  companion object {
    @Volatile
    private var INSTANCE: AuditRepository? = null

    fun getInstance(context: Context): AuditRepository {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: AuditRepository(context.applicationContext).also { INSTANCE = it }
      }
    }
  }
}
