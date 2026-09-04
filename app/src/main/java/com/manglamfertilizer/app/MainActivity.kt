package com.manglamfertilizer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.manglamfertilizer.app.ui.MainScreen
import com.manglamfertilizer.app.ui.MainViewModel
import com.manglamfertilizer.app.ui.localization.EnglishStrings
import com.manglamfertilizer.app.ui.localization.HindiStrings
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.ManglamFertilizerTheme
import com.manglamfertilizer.app.util.UpdateNotificationHelper
import com.manglamfertilizer.app.worker.UpdateWorkScheduler

class MainActivity : FragmentActivity() {
  private val viewModel: MainViewModel by viewModels()

  private val notificationPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    // Notification permission granted or denied.
    // Denial never breaks the in-app update flow or blocks the app.
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // 1. Initialize Notification Channel
    UpdateNotificationHelper.createNotificationChannel(this)

    // 2. Schedule battery-safe periodic background update checks
    UpdateWorkScheduler.schedulePeriodicUpdateCheck(this)

    // 3. Request POST_NOTIFICATIONS on Android 13+ (API 33+)
    checkAndRequestNotificationPermission()

    // 4. Handle Notification click intent if opened via status bar notification
    handleUpdateIntent(intent)

    setContent {
      val themeMode by viewModel.appThemeMode.collectAsState()
      val appLanguage by viewModel.appLanguage.collectAsState()
      val systemIsDark = isSystemInDarkTheme()

      val isDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemIsDark
      }

      val strings = if (appLanguage == "hi") HindiStrings else EnglishStrings

      CompositionLocalProvider(LocalStrings provides strings) {
        ManglamFertilizerTheme(darkTheme = isDarkTheme) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
          ) {
            MainScreen(viewModel = viewModel)
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleUpdateIntent(intent)
  }

  private fun handleUpdateIntent(intent: Intent?) {
    if (intent != null && intent.getBooleanExtra(UpdateNotificationHelper.EXTRA_OPEN_UPDATE, false)) {
      // Force an immediate update check and show dialog or forced screen
      viewModel.checkForUpdates(isManual = true)
    }
  }

  private fun checkAndRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val permission = Manifest.permission.POST_NOTIFICATIONS
      if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(permission)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.checkForUpdates(isManual = false)
  }
}
