package com.manglamfertilizer.app.data.repository

import android.content.Context
import com.manglamfertilizer.app.data.model.DailyAccountsColumnConfig
import com.manglamfertilizer.app.data.model.InventoryColumnConfig
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Unified SettingsRepository that coordinates LocalSettingsRepository (theme, language, printer)
 * and CloudSettingsRepository (inventory columns, daily accounts columns, alerts, business, AI, user cloud preferences).
 */
class SettingsRepository(context: Context) {
  val local = LocalSettingsRepository.getInstance(context)
  val cloud = CloudSettingsRepository.getInstance(context)

  // Local-only settings flows
  val language: StateFlow<String> = local.language
  val themeMode: StateFlow<String> = local.themeMode
  val printerAddress: StateFlow<String> = local.printerAddress
  val printerPaperSize: StateFlow<String> = local.printerPaperSize

  // Cloud authoritative settings flows
  val inventoryColumns: StateFlow<List<InventoryColumnConfig>> = cloud.inventoryColumns
  val dailyAccountsColumns: StateFlow<List<DailyAccountsColumnConfig>> = cloud.dailyAccountsColumns
  val alertSettings: StateFlow<AlertSettings> = cloud.alertSettings
  val businessSettings: StateFlow<BusinessSettings> = cloud.businessSettings
  val aiSettings: StateFlow<AiSettings> = cloud.aiSettings
  val userPreferences: StateFlow<UserCloudPreferences> = cloud.userPreferences
  val invoiceNumberConfig: StateFlow<InvoiceNumberConfig> = cloud.invoiceNumberConfig

  fun startRealtimeSync(scope: CoroutineScope) {
    cloud.startRealtimeSync(scope)
  }

  fun stopRealtimeSync() {
    cloud.stopRealtimeSync()
  }

  suspend fun syncWithFirestore() {
    cloud.syncWithFirestore()
  }

  // Local settings mutations
  fun setLanguage(lang: String, userEmail: String = "", userName: String = "Admin") {
    local.setLanguage(lang, userEmail, userName)
  }

  fun setThemeMode(mode: String, userEmail: String = "", userName: String = "Admin") {
    local.setThemeMode(mode, userEmail, userName)
  }

  fun setPrinter(address: String, paperSize: String) {
    local.setPrinter(address, paperSize)
  }

  // Cloud settings mutations
  fun saveInventoryColumns(columns: List<InventoryColumnConfig>, userEmail: String = "") {
    CoroutineScope(Dispatchers.IO).launch {
      cloud.saveInventoryColumns(columns, userEmail)
    }
  }

  fun addCustomField(title: String, dataType: String = "Text", userEmail: String = "") {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    val id = "custom_" + cleanTitle.lowercase().replace("\\s+".toRegex(), "_") + "_" + System.currentTimeMillis() % 10000
    val current = inventoryColumns.value.toMutableList()
    current.add(
      InventoryColumnConfig(
        id = id,
        title = cleanTitle,
        isVisible = true,
        isCustom = true,
        isLocked = false,
        order = current.size,
        dataType = dataType
      )
    )
    saveInventoryColumns(current, userEmail)
  }

  fun renameField(id: String, newTitle: String, userEmail: String = "") {
    val cleanTitle = newTitle.trim()
    if (cleanTitle.isBlank()) return
    val current = inventoryColumns.value.map { col ->
      if (col.id == id) {
        col.copy(title = cleanTitle)
      } else {
        col
      }
    }
    saveInventoryColumns(current, userEmail)
  }

  fun renameCustomField(id: String, newTitle: String, userEmail: String = "") {
    renameField(id, newTitle, userEmail)
  }

  fun deleteField(id: String, userEmail: String = "") {
    val current = inventoryColumns.value.filterNot { it.id == id }
    saveInventoryColumns(current, userEmail)
  }

  fun deleteCustomField(id: String, userEmail: String = "") {
    deleteField(id, userEmail)
  }

  // Daily Accounts Columns Mutations
  fun saveDailyAccountsColumns(columns: List<DailyAccountsColumnConfig>, userEmail: String = "") {
    CoroutineScope(Dispatchers.IO).launch {
      cloud.saveDailyAccountsColumns(columns, userEmail)
    }
  }

  fun addCustomDailyAccountsField(title: String, dataType: String = "Text", userEmail: String = "") {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    val id = "custom_" + cleanTitle.lowercase().replace("\\s+".toRegex(), "_") + "_" + (System.currentTimeMillis() % 10000)
    val current = dailyAccountsColumns.value.toMutableList()
    val width = when (dataType) {
      "Number" -> 80
      "Currency" -> 90
      "Date" -> 110
      else -> 120
    }
    current.add(
      DailyAccountsColumnConfig(
        id = id,
        title = cleanTitle,
        isVisible = true,
        isCustom = true,
        isMandatory = false,
        order = current.size,
        dataType = dataType,
        defaultWidthDp = width
      )
    )
    saveDailyAccountsColumns(current, userEmail)
  }

  fun renameDailyAccountsField(id: String, newTitle: String, userEmail: String = "") {
    val cleanTitle = newTitle.trim()
    if (cleanTitle.isBlank()) return
    val current = dailyAccountsColumns.value.map { col ->
      if (col.id == id) {
        col.copy(title = cleanTitle)
      } else {
        col
      }
    }
    saveDailyAccountsColumns(current, userEmail)
  }

  fun deleteDailyAccountsField(id: String, userEmail: String = "") {
    // Prevent deletion of mandatory accounting columns
    if (DailyAccountsColumnConfig.MANDATORY_COLUMN_IDS.contains(id)) return
    val current = dailyAccountsColumns.value.filterNot { it.id == id }
    saveDailyAccountsColumns(current, userEmail)
  }

  fun resetDailyAccountsColumns(userEmail: String = "") {
    saveDailyAccountsColumns(DailyAccountsColumnConfig.DEFAULT_COLUMNS, userEmail)
  }

  fun saveAlertSettings(settings: AlertSettings, userEmail: String = "") {
    CoroutineScope(Dispatchers.IO).launch {
      cloud.saveAlertSettings(settings, userEmail)
    }
  }

  fun saveBusinessSettings(biz: BusinessSettings, userEmail: String = "") {
    CoroutineScope(Dispatchers.IO).launch {
      cloud.saveBusinessSettings(biz, userEmail)
    }
  }

  fun saveAiSettings(ai: AiSettings, userEmail: String = "") {
    CoroutineScope(Dispatchers.IO).launch {
      cloud.saveAiSettings(ai, userEmail)
    }
  }

  fun saveUserPreferences(userPrefs: UserCloudPreferences, userId: String) {
    CoroutineScope(Dispatchers.IO).launch {
      cloud.saveUserPreferences(userPrefs, userId)
    }
  }

  suspend fun saveInvoiceNumberConfig(config: InvoiceNumberConfig, userEmail: String = ""): Result<InvoiceNumberConfig> {
    return cloud.saveInvoiceNumberConfig(config, userEmail)
  }

  suspend fun setStartingInvoiceNumber(startingNumber: Long, userEmail: String = ""): Result<InvoiceNumberConfig> {
    return cloud.setStartingInvoiceNumber(startingNumber, userEmail)
  }

  suspend fun overrideNextInvoiceNumber(nextNumber: Long, userEmail: String = ""): Result<InvoiceNumberConfig> {
    return cloud.overrideNextInvoiceNumber(nextNumber, userEmail)
  }
}
