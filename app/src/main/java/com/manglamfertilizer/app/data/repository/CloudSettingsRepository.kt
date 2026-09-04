package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.manglamfertilizer.app.data.model.DailyAccountsColumnConfig
import com.manglamfertilizer.app.data.model.InventoryColumnConfig
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.util.FirestoreProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class AlertSettings(
  val lowStockThreshold: Double = 5.0,
  val expiryHighMonths: Int = 1,
  val expiryMediumMonths: Int = 3,
  val expiryNormalMonths: Int = 6,
  val notificationPreferences: Map<String, Boolean> = mapOf("sound" to true, "vibration" to true),
  val alertEnablement: Boolean = true,
  val updatedAt: Long = 0L,
  val updatedBy: String = ""
)

data class BusinessSettings(
  val businessName: String = "Manglam Fertilizer",
  val businessAddress: String = "",
  val businessPhone: String = "",
  val gstNumber: String = "",
  val currencySymbol: String = "₹",
  val invoiceTerms: String = "Goods once sold will not be taken back.",
  val updatedAt: Long = 0L,
  val updatedBy: String = ""
)

data class AiSettings(
  val aiModelPreference: String = "gemini-2.5-flash",
  val voiceSpeed: Float = 1.0f,
  val businessContext: String = "Manglam Fertilizer agricultural retail & inventory management",
  val usageMetadata: Map<String, Any> = emptyMap(),
  val updatedAt: Long = 0L,
  val updatedBy: String = ""
)

data class UserCloudPreferences(
  val displayName: String = "",
  val greetingName: String = "",
  val defaultView: String = "inventory",
  val updatedAt: Long = 0L
)

/**
 * Authoritative Cloud Settings Repository.
 * All application, business, inventory columns, daily accounts columns, alerts, AI, and user settings
 * are stored in and synchronized via Cloud Firestore.
 */
class CloudSettingsRepository(
  private val context: Context,
  private val auditRepository: AuditRepository = AuditRepository.getInstance(context)
) {
  private val tag = "CloudSettingsRepo"
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("manglam_cloud_settings_cache", Context.MODE_PRIVATE)

  private val firestore: FirebaseFirestore? get() = FirestoreProvider.get()
  private val firebaseAuth: FirebaseAuth? get() = FirestoreProvider.auth

  private val listeners = mutableListOf<ListenerRegistration>()

  // 1. Inventory Columns
  private val _inventoryColumns = MutableStateFlow(loadColumnsCache())
  val inventoryColumns: StateFlow<List<InventoryColumnConfig>> = _inventoryColumns.asStateFlow()

  // 1b. Daily Accounts Columns
  private val _dailyAccountsColumns = MutableStateFlow(loadDailyAccountsColumnsCache())
  val dailyAccountsColumns: StateFlow<List<DailyAccountsColumnConfig>> = _dailyAccountsColumns.asStateFlow()

  // 2. Alert Settings
  private val _alertSettings = MutableStateFlow(loadAlertSettingsCache())
  val alertSettings: StateFlow<AlertSettings> = _alertSettings.asStateFlow()

  // 3. Business Settings
  private val _businessSettings = MutableStateFlow(loadBusinessSettingsCache())
  val businessSettings: StateFlow<BusinessSettings> = _businessSettings.asStateFlow()

  // 4. AI Settings
  private val _aiSettings = MutableStateFlow(loadAiSettingsCache())
  val aiSettings: StateFlow<AiSettings> = _aiSettings.asStateFlow()

  // 5. User Preferences
  private val _userPreferences = MutableStateFlow(loadUserPrefsCache())
  val userPreferences: StateFlow<UserCloudPreferences> = _userPreferences.asStateFlow()

  // 6. Invoice Number Configuration (Admin-controlled sequential numbering)
  private val _invoiceNumberConfig = MutableStateFlow(loadInvoiceNumberConfigCache())
  val invoiceNumberConfig: StateFlow<InvoiceNumberConfig> = _invoiceNumberConfig.asStateFlow()

  // Cached column parsing
  private fun loadColumnsCache(): List<InventoryColumnConfig> {
    val jsonStr = prefs.getString("cached_inventory_columns", null)
    return if (jsonStr.isNullOrBlank()) {
      InventoryColumnConfig.DEFAULT_COLUMNS
    } else {
      parseColumnsFromJson(jsonStr)
    }
  }

  private fun parseColumnsFromJson(jsonStr: String): List<InventoryColumnConfig> {
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<InventoryColumnConfig>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          InventoryColumnConfig(
            id = obj.getString("id"),
            title = obj.getString("title"),
            isVisible = obj.optBoolean("isVisible", true),
            isCustom = obj.optBoolean("isCustom", false),
            isLocked = false,
            order = obj.optInt("order", i),
            dataType = obj.optString("dataType", "Text")
          )
        )
      }
      if (list.isEmpty()) InventoryColumnConfig.DEFAULT_COLUMNS else list
    } catch (e: Exception) {
      InventoryColumnConfig.DEFAULT_COLUMNS
    }
  }

  private fun loadDailyAccountsColumnsCache(): List<DailyAccountsColumnConfig> {
    val jsonStr = prefs.getString("cached_daily_accounts_columns", null)
    return if (jsonStr.isNullOrBlank()) {
      DailyAccountsColumnConfig.DEFAULT_COLUMNS
    } else {
      parseDailyAccountsColumnsFromJson(jsonStr)
    }
  }

  private fun parseDailyAccountsColumnsFromJson(jsonStr: String): List<DailyAccountsColumnConfig> {
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<DailyAccountsColumnConfig>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val id = obj.getString("id")
        list.add(
          DailyAccountsColumnConfig(
            id = id,
            title = obj.getString("title"),
            isVisible = obj.optBoolean("isVisible", true),
            isCustom = obj.optBoolean("isCustom", false),
            isMandatory = DailyAccountsColumnConfig.MANDATORY_COLUMN_IDS.contains(id) || obj.optBoolean("isMandatory", false),
            order = obj.optInt("order", i),
            dataType = obj.optString("dataType", "Text"),
            defaultWidthDp = obj.optInt("defaultWidthDp", 90)
          )
        )
      }
      if (list.isEmpty()) DailyAccountsColumnConfig.DEFAULT_COLUMNS else ensureMandatoryDailyAccountsColumns(list)
    } catch (e: Exception) {
      DailyAccountsColumnConfig.DEFAULT_COLUMNS
    }
  }

  private fun ensureMandatoryDailyAccountsColumns(current: List<DailyAccountsColumnConfig>): List<DailyAccountsColumnConfig> {
    val existingIds = current.map { it.id }.toSet()
    val missingMandatory = DailyAccountsColumnConfig.DEFAULT_COLUMNS.filter { it.isMandatory && !existingIds.contains(it.id) }
    return if (missingMandatory.isEmpty()) {
      current
    } else {
      current + missingMandatory
    }
  }

  private fun loadAlertSettingsCache(): AlertSettings {
    return AlertSettings(
      lowStockThreshold = prefs.getFloat("alert_low_stock", 5.0f).toDouble(),
      expiryHighMonths = prefs.getInt("alert_exp_high", 1),
      expiryMediumMonths = prefs.getInt("alert_exp_med", 3),
      expiryNormalMonths = prefs.getInt("alert_exp_norm", 6),
      alertEnablement = prefs.getBoolean("alert_enabled", true),
      updatedAt = prefs.getLong("alert_updated_at", 0L),
      updatedBy = prefs.getString("alert_updated_by", "") ?: ""
    )
  }

  private fun loadBusinessSettingsCache(): BusinessSettings {
    return BusinessSettings(
      businessName = prefs.getString("biz_name", "Manglam Fertilizer") ?: "Manglam Fertilizer",
      businessAddress = prefs.getString("biz_address", "") ?: "",
      businessPhone = prefs.getString("biz_phone", "") ?: "",
      gstNumber = prefs.getString("biz_gst", "") ?: "",
      currencySymbol = prefs.getString("biz_currency", "₹") ?: "₹",
      invoiceTerms = prefs.getString("biz_terms", "Goods once sold will not be taken back.") ?: "Goods once sold will not be taken back.",
      updatedAt = prefs.getLong("biz_updated_at", 0L),
      updatedBy = prefs.getString("biz_updated_by", "") ?: ""
    )
  }

  private fun loadAiSettingsCache(): AiSettings {
    return AiSettings(
      aiModelPreference = prefs.getString("ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash",
      voiceSpeed = prefs.getFloat("ai_voice_speed", 1.0f),
      businessContext = prefs.getString("ai_context", "Manglam Fertilizer agricultural retail & inventory management") ?: "Manglam Fertilizer agricultural retail & inventory management",
      updatedAt = prefs.getLong("ai_updated_at", 0L),
      updatedBy = prefs.getString("ai_updated_by", "") ?: ""
    )
  }

  private fun loadUserPrefsCache(): UserCloudPreferences {
    return UserCloudPreferences(
      displayName = prefs.getString("user_display_name", "") ?: "",
      greetingName = prefs.getString("user_greeting_name", "") ?: "",
      defaultView = prefs.getString("user_default_view", "inventory") ?: "inventory",
      updatedAt = prefs.getLong("user_prefs_updated_at", 0L)
    )
  }

  private fun loadInvoiceNumberConfigCache(): InvoiceNumberConfig {
    val starting = prefs.getLong("inv_num_starting", 2026001L)
    val next = prefs.getLong("inv_num_next", 2026001L)
    val last = if (prefs.contains("inv_num_last")) prefs.getLong("inv_num_last", -1L).takeIf { it >= 0 } else null
    val prefix = prefs.getString("inv_num_prefix", "") ?: ""
    val suffix = prefs.getString("inv_num_suffix", "") ?: ""
    val enabled = prefs.getBoolean("inv_num_enabled", true)
    val updatedAt = prefs.getLong("inv_num_updated_at", 0L)
    val updatedBy = prefs.getString("inv_num_updated_by", "") ?: ""

    return InvoiceNumberConfig(
      startingNumber = starting,
      nextInvoiceNumber = next,
      lastIssuedNumber = last,
      prefix = prefix,
      suffix = suffix,
      enabled = enabled,
      updatedAt = updatedAt,
      updatedBy = updatedBy
    )
  }

  /**
   * Starts realtime snapshot listeners for multi-device synchronization.
   */
  fun startRealtimeSync(scope: CoroutineScope) {
    val db = firestore ?: return
    val user = firebaseAuth?.currentUser ?: return

    stopRealtimeSync()

    try {
      val businessId = FirestoreProvider.BUSINESS_ID
      val settingsCol = db.collection("businesses").document(businessId).collection("settings")

      // 1. Inventory Columns Listener
      val colListener = settingsCol.document("inventory").addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(tag, "Notice on inventory settings snapshot: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val jsonStr = snapshot.getString("columnsJson")
          val remoteUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
          if (!jsonStr.isNullOrBlank()) {
            val cols = parseColumnsFromJson(jsonStr)
            if (cols.isNotEmpty()) {
              prefs.edit().putString("cached_inventory_columns", jsonStr).putLong("columns_updated_at", remoteUpdatedAt).apply()
              _inventoryColumns.value = cols
            }
          }
        }
      }
      listeners.add(colListener)

      // 1b. Daily Accounts Columns Listener
      val dailyAccListener = settingsCol.document("daily_accounts").addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(tag, "Notice on daily_accounts settings snapshot: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val jsonStr = snapshot.getString("columnsJson")
          val remoteUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
          if (!jsonStr.isNullOrBlank()) {
            val cols = parseDailyAccountsColumnsFromJson(jsonStr)
            if (cols.isNotEmpty()) {
              prefs.edit().putString("cached_daily_accounts_columns", jsonStr).putLong("daily_accounts_columns_updated_at", remoteUpdatedAt).apply()
              _dailyAccountsColumns.value = cols
            }
          }
        }
      }
      listeners.add(dailyAccListener)

      // 2. Alert Settings Listener
      val alertListener = settingsCol.document("alerts").addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(tag, "Notice on alerts settings snapshot: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val lowStock = snapshot.getDouble("lowStockThreshold") ?: 5.0
          val expHigh = snapshot.getLong("expiryHighMonths")?.toInt() ?: 1
          val expMed = snapshot.getLong("expiryMediumMonths")?.toInt() ?: 3
          val expNorm = snapshot.getLong("expiryNormalMonths")?.toInt() ?: 6
          val enabled = snapshot.getBoolean("alertEnablement") ?: true
          val updatedAt = snapshot.getLong("updatedAt") ?: 0L
          val updatedBy = snapshot.getString("updatedBy") ?: ""

          val settings = AlertSettings(
            lowStockThreshold = lowStock,
            expiryHighMonths = expHigh,
            expiryMediumMonths = expMed,
            expiryNormalMonths = expNorm,
            alertEnablement = enabled,
            updatedAt = updatedAt,
            updatedBy = updatedBy
          )
          prefs.edit()
            .putFloat("alert_low_stock", lowStock.toFloat())
            .putInt("alert_exp_high", expHigh)
            .putInt("alert_exp_med", expMed)
            .putInt("alert_exp_norm", expNorm)
            .putBoolean("alert_enabled", enabled)
            .putLong("alert_updated_at", updatedAt)
            .putString("alert_updated_by", updatedBy)
            .apply()
          _alertSettings.value = settings
        }
      }
      listeners.add(alertListener)

      // 3. Business Settings Listener
      val bizListener = settingsCol.document("business").addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(tag, "Notice on business settings snapshot: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val name = snapshot.getString("businessName") ?: "Manglam Fertilizer"
          val address = snapshot.getString("businessAddress") ?: ""
          val phone = snapshot.getString("businessPhone") ?: ""
          val gst = snapshot.getString("gstNumber") ?: ""
          val currency = snapshot.getString("currencySymbol") ?: "₹"
          val terms = snapshot.getString("invoiceTerms") ?: "Goods once sold will not be taken back."
          val updatedAt = snapshot.getLong("updatedAt") ?: 0L
          val updatedBy = snapshot.getString("updatedBy") ?: ""

          val biz = BusinessSettings(
            businessName = name,
            businessAddress = address,
            businessPhone = phone,
            gstNumber = gst,
            currencySymbol = currency,
            invoiceTerms = terms,
            updatedAt = updatedAt,
            updatedBy = updatedBy
          )
          prefs.edit()
            .putString("biz_name", name)
            .putString("biz_address", address)
            .putString("biz_phone", phone)
            .putString("biz_gst", gst)
            .putString("biz_currency", currency)
            .putString("biz_terms", terms)
            .putLong("biz_updated_at", updatedAt)
            .putString("biz_updated_by", updatedBy)
            .apply()
          _businessSettings.value = biz
        }
      }
      listeners.add(bizListener)

      // 4. AI Settings Listener
      val aiListener = settingsCol.document("ai").addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(tag, "Notice on AI settings snapshot: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val model = snapshot.getString("aiModelPreference") ?: "gemini-2.5-flash"
          val speed = snapshot.getDouble("voiceSpeed")?.toFloat() ?: 1.0f
          val ctx = snapshot.getString("businessContext") ?: "Manglam Fertilizer agricultural retail & inventory management"
          val updatedAt = snapshot.getLong("updatedAt") ?: 0L
          val updatedBy = snapshot.getString("updatedBy") ?: ""

          val ai = AiSettings(
            aiModelPreference = model,
            voiceSpeed = speed,
            businessContext = ctx,
            updatedAt = updatedAt,
            updatedBy = updatedBy
          )
          prefs.edit()
            .putString("ai_model", model)
            .putFloat("ai_voice_speed", speed)
            .putString("ai_context", ctx)
            .putLong("ai_updated_at", updatedAt)
            .putString("ai_updated_by", updatedBy)
            .apply()
          _aiSettings.value = ai
        }
      }
      listeners.add(aiListener)

      // 5. User-Specific Preferences Listener: users/{userId}/settings/display_name
      val userUid = user.uid
      if (userUid.isNotBlank()) {
        val userPrefsListener = db.collection("users").document(userUid)
          .collection("settings").document("preferences").addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(tag, "Notice on user preferences snapshot: ${error.message}")
              return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
              val dName = snapshot.getString("displayName") ?: ""
              val gName = snapshot.getString("greetingName") ?: ""
              val dView = snapshot.getString("defaultView") ?: "inventory"
              val uAt = snapshot.getLong("updatedAt") ?: 0L

              val up = UserCloudPreferences(
                displayName = dName,
                greetingName = gName,
                defaultView = dView,
                updatedAt = uAt
              )
              prefs.edit()
                .putString("user_display_name", dName)
                .putString("user_greeting_name", gName)
                .putString("user_default_view", dView)
                .putLong("user_prefs_updated_at", uAt)
                .apply()
              _userPreferences.value = up
            }
          }
        listeners.add(userPrefsListener)
      }

      // 6. Invoice Number Settings Listener: businesses/{businessId}/settings/invoiceNumber
      val invNumListener = settingsCol.document("invoiceNumber").addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(tag, "Notice on invoiceNumber settings snapshot: ${error.message}")
          return@addSnapshotListener
        }
        if (snapshot != null && snapshot.exists()) {
          val starting = snapshot.getLong("startingNumber") ?: 2026001L
          val next = snapshot.getLong("nextInvoiceNumber") ?: starting
          val last = snapshot.getLong("lastIssuedNumber")
          val prefix = snapshot.getString("prefix") ?: ""
          val suffix = snapshot.getString("suffix") ?: ""
          val enabled = snapshot.getBoolean("enabled") ?: true
          val updatedAt = snapshot.getLong("updatedAt") ?: 0L
          val updatedBy = snapshot.getString("updatedBy") ?: ""

          val cfg = InvoiceNumberConfig(
            startingNumber = starting,
            nextInvoiceNumber = next,
            lastIssuedNumber = last,
            prefix = prefix,
            suffix = suffix,
            enabled = enabled,
            updatedAt = updatedAt,
            updatedBy = updatedBy
          )
          prefs.edit()
            .putLong("inv_num_starting", starting)
            .putLong("inv_num_next", next)
            .apply {
              if (last != null) putLong("inv_num_last", last) else remove("inv_num_last")
            }
            .putString("inv_num_prefix", prefix)
            .putString("inv_num_suffix", suffix)
            .putBoolean("inv_num_enabled", enabled)
            .putLong("inv_num_updated_at", updatedAt)
            .putString("inv_num_updated_by", updatedBy)
            .apply()
          _invoiceNumberConfig.value = cfg
        }
      }
      listeners.add(invNumListener)
    } catch (e: Exception) {
      Log.w(tag, "Failed to initialize cloud settings listeners: ${e.message}")
    }
  }

  fun stopRealtimeSync() {
    listeners.forEach { it.remove() }
    listeners.clear()
  }

  /**
   * Pulls authoritative settings from Firestore once on startup or refresh.
   */
  suspend fun syncWithFirestore() = withContext(Dispatchers.IO) {
    val db = firestore ?: return@withContext
    val authUser = firebaseAuth?.currentUser ?: return@withContext
    val businessId = FirestoreProvider.BUSINESS_ID

    try {
      val settingsCol = db.collection("businesses").document(businessId).collection("settings")

      // 1. Inventory Columns
      val invDoc = settingsCol.document("inventory").get().await()
      if (invDoc.exists()) {
        val jsonStr = invDoc.getString("columnsJson")
        if (!jsonStr.isNullOrBlank()) {
          val cols = parseColumnsFromJson(jsonStr)
          if (cols.isNotEmpty()) {
            prefs.edit().putString("cached_inventory_columns", jsonStr).apply()
            _inventoryColumns.value = cols
          }
        }
      }

      // 1b. Daily Accounts Columns
      val dailyAccDoc = settingsCol.document("daily_accounts").get().await()
      if (dailyAccDoc.exists()) {
        val jsonStr = dailyAccDoc.getString("columnsJson")
        if (!jsonStr.isNullOrBlank()) {
          val cols = parseDailyAccountsColumnsFromJson(jsonStr)
          if (cols.isNotEmpty()) {
            prefs.edit().putString("cached_daily_accounts_columns", jsonStr).apply()
            _dailyAccountsColumns.value = cols
          }
        }
      }

      // 2. Alert Settings
      val alertDoc = settingsCol.document("alerts").get().await()
      if (alertDoc.exists()) {
        val lowStock = alertDoc.getDouble("lowStockThreshold") ?: 5.0
        val expHigh = alertDoc.getLong("expiryHighMonths")?.toInt() ?: 1
        val expMed = alertDoc.getLong("expiryMediumMonths")?.toInt() ?: 3
        val expNorm = alertDoc.getLong("expiryNormalMonths")?.toInt() ?: 6
        val enabled = alertDoc.getBoolean("alertEnablement") ?: true
        val updatedAt = alertDoc.getLong("updatedAt") ?: 0L
        val updatedBy = alertDoc.getString("updatedBy") ?: ""

        val settings = AlertSettings(
          lowStockThreshold = lowStock,
          expiryHighMonths = expHigh,
          expiryMediumMonths = expMed,
          expiryNormalMonths = expNorm,
          alertEnablement = enabled,
          updatedAt = updatedAt,
          updatedBy = updatedBy
        )
        _alertSettings.value = settings
      }

      // 3. Business Settings
      val bizDoc = settingsCol.document("business").get().await()
      if (bizDoc.exists()) {
        val name = bizDoc.getString("businessName") ?: "Manglam Fertilizer"
        val address = bizDoc.getString("businessAddress") ?: ""
        val phone = bizDoc.getString("businessPhone") ?: ""
        val gst = bizDoc.getString("gstNumber") ?: ""
        val currency = bizDoc.getString("currencySymbol") ?: "₹"
        val terms = bizDoc.getString("invoiceTerms") ?: "Goods once sold will not be taken back."
        val updatedAt = bizDoc.getLong("updatedAt") ?: 0L
        val updatedBy = bizDoc.getString("updatedBy") ?: ""

        _businessSettings.value = BusinessSettings(
          businessName = name,
          businessAddress = address,
          businessPhone = phone,
          gstNumber = gst,
          currencySymbol = currency,
          invoiceTerms = terms,
          updatedAt = updatedAt,
          updatedBy = updatedBy
        )
      }

      // 4. AI Settings
      val aiDoc = settingsCol.document("ai").get().await()
      if (aiDoc.exists()) {
        val model = aiDoc.getString("aiModelPreference") ?: "gemini-2.5-flash"
        val speed = aiDoc.getDouble("voiceSpeed")?.toFloat() ?: 1.0f
        val ctx = aiDoc.getString("businessContext") ?: "Manglam Fertilizer agricultural retail & inventory management"
        val updatedAt = aiDoc.getLong("updatedAt") ?: 0L
        val updatedBy = aiDoc.getString("updatedBy") ?: ""

        _aiSettings.value = AiSettings(
          aiModelPreference = model,
          voiceSpeed = speed,
          businessContext = ctx,
          updatedAt = updatedAt,
          updatedBy = updatedBy
        )
      }

      // 5. User Specific Preferences
      val userUid = authUser.uid
      if (userUid.isNotBlank()) {
        val userDoc = db.collection("users").document(userUid)
          .collection("settings").document("preferences").get().await()
        if (userDoc.exists()) {
          _userPreferences.value = UserCloudPreferences(
            displayName = userDoc.getString("displayName") ?: "",
            greetingName = userDoc.getString("greetingName") ?: "",
            defaultView = userDoc.getString("defaultView") ?: "inventory",
            updatedAt = userDoc.getLong("updatedAt") ?: 0L
          )
        }
      }

      // 6. Invoice Number Settings
      val invNumDoc = settingsCol.document("invoiceNumber").get().await()
      if (invNumDoc.exists()) {
        val starting = invNumDoc.getLong("startingNumber") ?: 2026001L
        val next = invNumDoc.getLong("nextInvoiceNumber") ?: starting
        val last = invNumDoc.getLong("lastIssuedNumber")
        val prefix = invNumDoc.getString("prefix") ?: ""
        val suffix = invNumDoc.getString("suffix") ?: ""
        val enabled = invNumDoc.getBoolean("enabled") ?: true
        val updatedAt = invNumDoc.getLong("updatedAt") ?: 0L
        val updatedBy = invNumDoc.getString("updatedBy") ?: ""

        _invoiceNumberConfig.value = InvoiceNumberConfig(
          startingNumber = starting,
          nextInvoiceNumber = next,
          lastIssuedNumber = last,
          prefix = prefix,
          suffix = suffix,
          enabled = enabled,
          updatedAt = updatedAt,
          updatedBy = updatedBy
        )
      }
    } catch (e: Exception) {
      Log.w(tag, "Notice during cloud settings initial sync: ${e.message}")
    }
  }

  // --- SAVE METHODS (Flow: UI -> Firestore -> Success -> In-Memory/StateFlow -> UI) ---

  suspend fun saveInventoryColumns(columns: List<InventoryColumnConfig>, userEmail: String = "") = withContext(Dispatchers.IO) {
    val array = JSONArray()
    columns.forEachIndexed { index, col ->
      val obj = JSONObject().apply {
        put("id", col.id)
        put("title", col.title)
        put("isVisible", col.isVisible)
        put("isCustom", col.isCustom)
        put("isLocked", false)
        put("order", index)
        put("dataType", col.dataType)
      }
      array.put(obj)
    }
    val jsonStr = array.toString()
    val now = System.currentTimeMillis()

    val db = firestore
    if (db != null) {
      val businessId = FirestoreProvider.BUSINESS_ID
      val map = hashMapOf(
        "columnsJson" to jsonStr,
        "updatedAt" to now,
        "serverTimestamp" to FieldValue.serverTimestamp(),
        "updatedBy" to userEmail
      )
      // Authoritative Cloud write
      db.collection("businesses").document(businessId)
        .collection("settings").document("inventory")
        .set(map, SetOptions.merge()).await()

      // Also mirror to legacy doc for backwards compatibility
      db.collection("businesses").document(businessId)
        .collection("settings").document("inventory_columns")
        .set(map, SetOptions.merge()).await()
    }

    // Update local cache & StateFlow after cloud confirmation
    prefs.edit().putString("cached_inventory_columns", jsonStr).putLong("columns_updated_at", now).apply()
    _inventoryColumns.value = columns

    auditRepository.logInventoryColumnsChanged(
      visibleColumnCount = columns.count { it.isVisible },
      columnsSummary = columns.filter { it.isVisible }.joinToString { it.title },
      userEmail = userEmail,
      userRole = if (com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF",
      userId = ""
    )
  }

  suspend fun saveDailyAccountsColumns(
    columns: List<DailyAccountsColumnConfig>,
    userEmail: String = ""
  ) = withContext(Dispatchers.IO) {
    val validatedColumns = ensureMandatoryDailyAccountsColumns(columns)
    val array = JSONArray()
    validatedColumns.forEachIndexed { index, col ->
      val obj = JSONObject().apply {
        put("id", col.id)
        put("title", col.title)
        put("isVisible", col.isVisible)
        put("isCustom", col.isCustom)
        put("isMandatory", col.isMandatory)
        put("order", index)
        put("dataType", col.dataType)
        put("defaultWidthDp", col.defaultWidthDp)
      }
      array.put(obj)
    }
    val jsonStr = array.toString()
    val now = System.currentTimeMillis()

    val db = firestore
    if (db != null) {
      val businessId = FirestoreProvider.BUSINESS_ID
      val map = hashMapOf(
        "columnsJson" to jsonStr,
        "updatedAt" to now,
        "serverTimestamp" to FieldValue.serverTimestamp(),
        "updatedBy" to userEmail
      )
      // Authoritative Cloud write to dedicated settings collection document
      db.collection("businesses").document(businessId)
        .collection("settings").document("daily_accounts")
        .set(map, SetOptions.merge()).await()

      // Also mirror for compatibility
      db.collection("businesses").document(businessId)
        .collection("settings").document("daily_accounts_columns")
        .set(map, SetOptions.merge()).await()
    }

    // Update local cache & StateFlow after cloud confirmation
    prefs.edit().putString("cached_daily_accounts_columns", jsonStr).putLong("daily_accounts_columns_updated_at", now).apply()
    _dailyAccountsColumns.value = validatedColumns

    auditRepository.logDailyAccountsColumnsChanged(
      visibleColumnCount = validatedColumns.count { it.isVisible },
      columnsSummary = validatedColumns.filter { it.isVisible }.joinToString { it.title },
      userEmail = userEmail,
      userRole = if (com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF",
      userId = ""
    )
  }

  suspend fun saveAlertSettings(settings: AlertSettings, userEmail: String = "") = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val oldThreshold = _alertSettings.value.lowStockThreshold
    val updated = settings.copy(updatedAt = now, updatedBy = userEmail)

    val db = firestore
    if (db != null) {
      val businessId = FirestoreProvider.BUSINESS_ID
      val map = hashMapOf(
        "lowStockThreshold" to updated.lowStockThreshold,
        "expiryHighMonths" to updated.expiryHighMonths,
        "expiryMediumMonths" to updated.expiryMediumMonths,
        "expiryNormalMonths" to updated.expiryNormalMonths,
        "alertEnablement" to updated.alertEnablement,
        "updatedAt" to now,
        "serverTimestamp" to FieldValue.serverTimestamp(),
        "updatedBy" to userEmail
      )
      db.collection("businesses").document(businessId)
        .collection("settings").document("alerts")
        .set(map, SetOptions.merge()).await()
    }

    prefs.edit()
      .putFloat("alert_low_stock", updated.lowStockThreshold.toFloat())
      .putInt("alert_exp_high", updated.expiryHighMonths)
      .putInt("alert_exp_med", updated.expiryMediumMonths)
      .putInt("alert_exp_norm", updated.expiryNormalMonths)
      .putBoolean("alert_enabled", updated.alertEnablement)
      .putLong("alert_updated_at", now)
      .putString("alert_updated_by", userEmail)
      .apply()
    _alertSettings.value = updated

    auditRepository.logSettingsChanged(
      settingKey = "alert_settings",
      oldValue = "lowStock=$oldThreshold, enabled=${_alertSettings.value.alertEnablement}",
      newValue = "lowStock=${updated.lowStockThreshold}, enabled=${updated.alertEnablement}",
      userEmail = userEmail,
      userRole = if (com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF",
      userId = ""
    )
  }

  suspend fun saveBusinessSettings(biz: BusinessSettings, userEmail: String = "") = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val oldName = _businessSettings.value.businessName
    val updated = biz.copy(updatedAt = now, updatedBy = userEmail)

    val db = firestore
    if (db != null) {
      val businessId = FirestoreProvider.BUSINESS_ID
      val map = hashMapOf(
        "businessName" to updated.businessName,
        "businessAddress" to updated.businessAddress,
        "businessPhone" to updated.businessPhone,
        "gstNumber" to updated.gstNumber,
        "currencySymbol" to updated.currencySymbol,
        "invoiceTerms" to updated.invoiceTerms,
        "updatedAt" to now,
        "serverTimestamp" to FieldValue.serverTimestamp(),
        "updatedBy" to userEmail
      )
      db.collection("businesses").document(businessId)
        .collection("settings").document("business")
        .set(map, SetOptions.merge()).await()
    }

    prefs.edit()
      .putString("biz_name", updated.businessName)
      .putString("biz_address", updated.businessAddress)
      .putString("biz_phone", updated.businessPhone)
      .putString("biz_gst", updated.gstNumber)
      .putString("biz_currency", updated.currencySymbol)
      .putString("biz_terms", updated.invoiceTerms)
      .putLong("biz_updated_at", now)
      .putString("biz_updated_by", userEmail)
      .apply()
    _businessSettings.value = updated

    auditRepository.logSettingsChanged(
      settingKey = "business_settings",
      oldValue = "name=$oldName",
      newValue = "name=${updated.businessName}, gst=${updated.gstNumber}",
      userEmail = userEmail,
      userRole = if (com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF",
      userId = ""
    )
  }

  suspend fun saveAiSettings(ai: AiSettings, userEmail: String = "") = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val oldModel = _aiSettings.value.aiModelPreference
    val updated = ai.copy(updatedAt = now, updatedBy = userEmail)

    val db = firestore
    if (db != null) {
      val businessId = FirestoreProvider.BUSINESS_ID
      val map = hashMapOf(
        "aiModelPreference" to updated.aiModelPreference,
        "voiceSpeed" to updated.voiceSpeed,
        "businessContext" to updated.businessContext,
        "updatedAt" to now,
        "serverTimestamp" to FieldValue.serverTimestamp(),
        "updatedBy" to userEmail
      )
      db.collection("businesses").document(businessId)
        .collection("settings").document("ai")
        .set(map, SetOptions.merge()).await()
    }

    prefs.edit()
      .putString("ai_model", updated.aiModelPreference)
      .putFloat("ai_voice_speed", updated.voiceSpeed)
      .putString("ai_context", updated.businessContext)
      .putLong("ai_updated_at", now)
      .putString("ai_updated_by", userEmail)
      .apply()
    _aiSettings.value = updated

    auditRepository.logSettingsChanged(
      settingKey = "ai_settings",
      oldValue = "model=$oldModel",
      newValue = "model=${updated.aiModelPreference}, voiceSpeed=${updated.voiceSpeed}",
      userEmail = userEmail,
      userRole = if (com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF",
      userId = ""
    )
  }

  suspend fun saveUserPreferences(userPrefs: UserCloudPreferences, userId: String) = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val updated = userPrefs.copy(updatedAt = now)

    val db = firestore
    if (db != null && userId.isNotBlank()) {
      val map = hashMapOf(
        "displayName" to updated.displayName,
        "greetingName" to updated.greetingName,
        "defaultView" to updated.defaultView,
        "updatedAt" to now,
        "serverTimestamp" to FieldValue.serverTimestamp()
      )
      db.collection("users").document(userId)
        .collection("settings").document("preferences")
        .set(map, SetOptions.merge()).await()
    }

    prefs.edit()
      .putString("user_display_name", updated.displayName)
      .putString("user_greeting_name", updated.greetingName)
      .putString("user_default_view", updated.defaultView)
      .putLong("user_prefs_updated_at", now)
      .apply()
    _userPreferences.value = updated
  }

  suspend fun saveInvoiceNumberConfig(config: InvoiceNumberConfig, userEmail: String = ""): Result<InvoiceNumberConfig> = withContext(Dispatchers.IO) {
    try {
      val now = System.currentTimeMillis()
      val oldConfig = _invoiceNumberConfig.value
      val updated = config.copy(updatedAt = now, updatedBy = userEmail)

      val db = firestore
      if (db != null) {
        val businessId = FirestoreProvider.BUSINESS_ID
        val map = hashMapOf(
          "startingNumber" to updated.startingNumber,
          "nextInvoiceNumber" to updated.nextInvoiceNumber,
          "prefix" to updated.prefix,
          "suffix" to updated.suffix,
          "enabled" to updated.enabled,
          "updatedAt" to now,
          "serverTimestamp" to FieldValue.serverTimestamp(),
          "updatedBy" to userEmail
        )
        if (updated.lastIssuedNumber != null) {
          map["lastIssuedNumber"] = updated.lastIssuedNumber
        }

        db.collection("businesses").document(businessId)
          .collection("settings").document("invoiceNumber")
          .set(map, SetOptions.merge()).await()
      }

      prefs.edit()
        .putLong("inv_num_starting", updated.startingNumber)
        .putLong("inv_num_next", updated.nextInvoiceNumber)
        .apply {
          if (updated.lastIssuedNumber != null) putLong("inv_num_last", updated.lastIssuedNumber) else remove("inv_num_last")
        }
        .putString("inv_num_prefix", updated.prefix)
        .putString("inv_num_suffix", updated.suffix)
        .putBoolean("inv_num_enabled", updated.enabled)
        .putLong("inv_num_updated_at", now)
        .putString("inv_num_updated_by", userEmail)
        .apply()

      _invoiceNumberConfig.value = updated

      auditRepository.logSettingsChanged(
        settingKey = "invoice_number_config",
        oldValue = "start=${oldConfig.startingNumber}, next=${oldConfig.nextInvoiceNumber}, last=${oldConfig.lastIssuedNumber}",
        newValue = "start=${updated.startingNumber}, next=${updated.nextInvoiceNumber}, last=${updated.lastIssuedNumber}",
        userEmail = userEmail,
        userRole = if (com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF",
        userId = ""
      )

      Result.success(updated)
    } catch (e: Exception) {
      Log.e(tag, "Failed to save invoice number config: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun setStartingInvoiceNumber(startingNumber: Long, userEmail: String = ""): Result<InvoiceNumberConfig> {
    val current = _invoiceNumberConfig.value
    val newConfig = current.copy(
      startingNumber = startingNumber,
      nextInvoiceNumber = startingNumber,
      enabled = true
    )
    return saveInvoiceNumberConfig(newConfig, userEmail)
  }

  suspend fun overrideNextInvoiceNumber(nextNumber: Long, userEmail: String = ""): Result<InvoiceNumberConfig> {
    val current = _invoiceNumberConfig.value
    val newConfig = current.copy(
      nextInvoiceNumber = nextNumber,
      enabled = true
    )
    return saveInvoiceNumberConfig(newConfig, userEmail)
  }

  companion object {
    @Volatile
    private var instance: CloudSettingsRepository? = null

    fun getInstance(context: Context): CloudSettingsRepository {
      return instance ?: synchronized(this) {
        instance ?: CloudSettingsRepository(context.applicationContext).also { instance = it }
      }
    }
  }
}
