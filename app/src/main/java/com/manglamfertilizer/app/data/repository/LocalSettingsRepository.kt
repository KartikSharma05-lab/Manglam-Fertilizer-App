package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local-only Settings Repository.
 * STRIKE RULE: ONLY Theme, Language, and Printer configuration are stored here.
 * NEVER uploaded to Cloud Firestore.
 */
class LocalSettingsRepository(context: Context) {
  private val prefs: SharedPreferences =
    context.applicationContext.getSharedPreferences("manglam_local_settings_prefs", Context.MODE_PRIVATE)

  private val auditRepository = AuditRepository.getInstance(context)

  private val _language = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
  val language: StateFlow<String> = _language.asStateFlow()

  private val _themeMode = MutableStateFlow(prefs.getString("app_theme_mode", "dark") ?: "dark")
  val themeMode: StateFlow<String> = _themeMode.asStateFlow()

  private val _printerAddress = MutableStateFlow(prefs.getString("printer_address", "") ?: "")
  val printerAddress: StateFlow<String> = _printerAddress.asStateFlow()

  private val _printerPaperSize = MutableStateFlow(prefs.getString("printer_paper_size", "58mm") ?: "58mm")
  val printerPaperSize: StateFlow<String> = _printerPaperSize.asStateFlow()

  fun setLanguage(lang: String, userEmail: String = "", userName: String = "Admin") {
    val oldLang = _language.value
    if (oldLang != lang) {
      prefs.edit().putString("app_language", lang).apply()
      _language.value = lang
      val role = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      auditRepository.logLanguageChanged(
        oldLang = oldLang,
        newLang = lang,
        userEmail = userEmail.ifBlank { userName },
        userRole = role,
        userId = ""
      )
    }
  }

  fun setThemeMode(mode: String, userEmail: String = "", userName: String = "Admin") {
    val oldTheme = _themeMode.value
    if (oldTheme != mode) {
      prefs.edit().putString("app_theme_mode", mode).apply()
      _themeMode.value = mode
      val role = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      auditRepository.logThemeChanged(
        oldTheme = oldTheme,
        newTheme = mode,
        userEmail = userEmail.ifBlank { userName },
        userRole = role,
        userId = ""
      )
    }
  }

  fun setPrinter(address: String, paperSize: String) {
    prefs.edit()
      .putString("printer_address", address)
      .putString("printer_paper_size", paperSize)
      .apply()
    _printerAddress.value = address
    _printerPaperSize.value = paperSize
  }

  companion object {
    @Volatile
    private var instance: LocalSettingsRepository? = null

    fun getInstance(context: Context): LocalSettingsRepository {
      return instance ?: synchronized(this) {
        instance ?: LocalSettingsRepository(context.applicationContext).also { instance = it }
      }
    }
  }
}
