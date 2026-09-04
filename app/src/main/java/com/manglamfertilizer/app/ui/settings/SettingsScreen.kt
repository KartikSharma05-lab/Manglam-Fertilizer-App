package com.manglamfertilizer.app.ui.settings

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.model.ReleaseHistoryItem
import com.manglamfertilizer.app.data.model.ReleaseType
import com.manglamfertilizer.app.data.model.UpdateEngineState
import com.manglamfertilizer.app.data.model.UpdateManifest
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.repository.GitHubStatusInfo
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.InvoiceNumberManager
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import com.manglamfertilizer.app.ui.update.AdminPublishUpdateDialog
import com.manglamfertilizer.app.ui.update.AdminReleaseConfigurationDialog
import com.manglamfertilizer.app.ui.update.AdminTestingSimulatorSheet
import com.manglamfertilizer.app.ui.update.ReleaseHistoryDialog

@Composable
fun SettingsScreen(
  user: User?,
  language: String,
  themeMode: String,
  googleDisplayName: String?,
  isDeviceUnlockEnabled: Boolean,
  onToggleDeviceUnlock: (Boolean) -> Unit,
  onUpdateDisplayName: (String) -> Unit,
  onLanguageChange: (String) -> Unit,
  onThemeChange: (String) -> Unit,
  onPrinterConfigClick: () -> Unit,
  onBackupClick: () -> Unit,
  onLogout: () -> Unit,
  updateInfo: AppUpdateInfo = AppUpdateInfo(),
  releaseHistory: List<ReleaseHistoryItem> = emptyList(),
  githubStatus: GitHubStatusInfo = GitHubStatusInfo(),
  onCheckForUpdates: () -> Unit = {},
  onShowUpdateDialog: () -> Unit = {},
  onInstallUpdate: (Context) -> Unit = {},
  onPublishRelease: (UpdateManifest) -> Unit = {},
  onSimulateRelease: (String, Long, ReleaseType, Int, Int, String) -> Unit = { _, _, _, _, _, _ -> },
  onSkipUpdate: () -> Unit = {},
  manifestUrl: String = "",
  onSetCustomManifestUrl: (String) -> Unit = {},
  onResetManifestUrl: () -> Unit = {},
  onOpenAuditLogs: () -> Unit = {},
  invoiceNumberConfig: InvoiceNumberConfig = InvoiceNumberConfig(),
  invoices: List<Invoice> = emptyList(),
  onSaveInvoiceNumberConfig: ((InvoiceNumberConfig, (Boolean, String?) -> Unit) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val strings = LocalStrings.current
  val scrollState = rememberScrollState()

  // Strict Admin Verification based ONLY on the two allowed emails
  val isAdminUser = AdminAuthUtils.isAdmin(user)

  var showDisplayNameDialog by remember { mutableStateOf(false) }
  var showSignOutConfirmDialog by remember { mutableStateOf(false) }
  var showAdminPublishDialog by remember { mutableStateOf(false) }
  var showReleaseHistoryDialog by remember { mutableStateOf(false) }
  var showSimulatorDialog by remember { mutableStateOf(false) }
  var showInvoiceConfigDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .imePadding()
  ) {
    // Top Bar Header
    Surface(
      color = DarkSurface,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = strings.settingsTitle,
            style = MaterialTheme.typography.titleLarge.copy(
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextPrimaryDark
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag("settings_screen")
    ) {
      // User Profile Card (Compact with Integrated Logout & Edit Actions)
      UserProfileCard(
        user = user,
        isAdmin = isAdminUser,
        googleDisplayName = googleDisplayName,
        onEditNameClick = { showDisplayNameDialog = true },
        onLogoutClick = { showSignOutConfirmDialog = true }
      )

      Spacer(modifier = Modifier.height(10.dp))

      // ==========================================
      // 1. APP UPDATES (NORMAL USER / STAFF VIEW)
      // ==========================================
      NormalUserAppUpdatesSection(
        updateInfo = updateInfo,
        onCheckForUpdates = onCheckForUpdates,
        onUpdateNow = onShowUpdateDialog,
        onLaterOrSkip = onSkipUpdate,
        onInstallUpdate = onInstallUpdate
      )

      // ==========================================
      // 2. ADMIN VIEW (UPDATE MANAGEMENT CONTROLS)
      // STRICTLY GATED: Visible ONLY to the two Admin emails
      // ==========================================
      if (isAdminUser) {
        Spacer(modifier = Modifier.height(10.dp))

        AdminUpdateManagementSection(
          user = user,
          updateInfo = updateInfo,
          githubStatus = githubStatus,
          manifestUrl = manifestUrl,
          onCheckGitHub = onCheckForUpdates,
          onCreateRelease = { showAdminPublishDialog = true },
          onRefresh = onCheckForUpdates,
          onViewHistory = { showReleaseHistoryDialog = true },
          onOpenSimulator = { showSimulatorDialog = true }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Hardware & Printer Section
      SettingsSectionHeader(title = strings.hardwareBilling)
      SettingsItemCard(
        icon = Icons.Default.Print,
        iconTint = Emerald400,
        title = strings.bluetoothThermalPrinter,
        subtitle = strings.printerConfigDesc,
        onClick = onPrinterConfigClick,
        testTag = "settings_printer_button"
      )

      Spacer(modifier = Modifier.height(12.dp))

      // ==========================================
      // INVOICE NUMBER CONFIGURATION SECTION
      // ==========================================
      SettingsSectionHeader(title = "Invoice Number")
      InvoiceNumberSection(
        config = invoiceNumberConfig,
        isAdmin = isAdminUser,
        onConfigureClick = { showInvoiceConfigDialog = true }
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Preferences & Localization Section
      SettingsSectionHeader(title = strings.prefLocalization)

      // Language Selector
      LanguageSelectionCard(
        currentLanguage = language,
        onLanguageSelected = onLanguageChange
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Theme Selector
      ThemeSelectionCard(
        currentTheme = themeMode,
        onThemeSelected = onThemeChange
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Data Backup & Security Section
      SettingsSectionHeader(title = strings.cloudSecurity)

      // Biometric / Device Lock Switch
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = DarkSurfaceElevated,
              modifier = Modifier.size(38.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = strings.biometricUnlock,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimaryDark
              )
              Text(
                text = if (isDeviceUnlockEnabled) strings.biometricUnlockDescEnabled else strings.biometricUnlockDescDisabled,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondaryDark
              )
            }
          }
          Switch(
            checked = isDeviceUnlockEnabled,
            onCheckedChange = onToggleDeviceUnlock,
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = Emerald500,
              uncheckedThumbColor = TextSecondaryDark,
              uncheckedTrackColor = DarkBorder
            ),
            modifier = Modifier.testTag("settings_biometric_switch")
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Cloud Sync Status Card
      SettingsItemCard(
        icon = Icons.Default.CloudSync,
        iconTint = InfoSky,
        title = strings.backupCloud,
        subtitle = strings.backupCloudDesc,
        onClick = onBackupClick,
        testTag = "settings_cloud_sync_button"
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Firebase Activity Audit System Card
      SettingsItemCard(
        icon = Icons.Default.Security,
        iconTint = Emerald400,
        title = "Firebase Activity Audit Logs",
        subtitle = "Immutable records of logins, inventory CRUD, billing, settings & updates",
        onClick = onOpenAuditLogs,
        testTag = "settings_audit_logs_button"
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Footer Info
      Text(
        text = "${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} Dealer POS & ERP • Build ${updateInfo.installedVersionCode}",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
        color = TextMutedDark,
        modifier = Modifier.align(Alignment.CenterHorizontally)
      )
      Spacer(modifier = Modifier.height(12.dp))
    }
  }

  // --- Dialogs ---

  if (showDisplayNameDialog) {
    EditDisplayNameDialog(
      initialName = user?.name ?: "",
      googleName = googleDisplayName,
      onSave = { newName ->
        onUpdateDisplayName(newName)
        showDisplayNameDialog = false
      },
      onDismiss = { showDisplayNameDialog = false }
    )
  }

  if (showSignOutConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showSignOutConfirmDialog = false },
      title = {
        Text(text = strings.signOutConfirmTitle, color = TextPrimaryDark, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(text = strings.signOutConfirmMsg, color = TextSecondaryDark, fontSize = 13.sp)
      },
      confirmButton = {
        Button(
          onClick = {
            showSignOutConfirmDialog = false
            onLogout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoftRed, contentColor = Color.White)
        ) {
          Text(text = strings.signOut)
        }
      },
      dismissButton = {
        TextButton(onClick = { showSignOutConfirmDialog = false }) {
          Text(text = strings.cancel, color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard
    )
  }

  if (showAdminPublishDialog && isAdminUser) {
    AdminPublishUpdateDialog(
      adminEmail = user?.email ?: "admin.manglamferilizer@gmail.com",
      currentVersionName = updateInfo.installedVersionName,
      currentVersionCode = updateInfo.installedVersionCode,
      onPublish = { manifest ->
        showAdminPublishDialog = false
        onPublishRelease(manifest)
      },
      onDismiss = { showAdminPublishDialog = false }
    )
  }

  if (showReleaseHistoryDialog && isAdminUser) {
    ReleaseHistoryDialog(
      history = releaseHistory,
      onDismiss = { showReleaseHistoryDialog = false }
    )
  }

  if (showSimulatorDialog && isAdminUser) {
    AdminTestingSimulatorSheet(
      onSimulate = { verName, verCode, relType, daysAgo, graceDays, notes ->
        showSimulatorDialog = false
        onSimulateRelease(verName, verCode, relType, daysAgo, graceDays, notes)
      },
      onDismiss = { showSimulatorDialog = false }
    )
  }

  if (showInvoiceConfigDialog && isAdminUser) {
    AdminInvoiceNumberConfigDialog(
      currentConfig = invoiceNumberConfig,
      existingInvoices = invoices,
      onDismiss = { showInvoiceConfigDialog = false },
      onSave = { updatedConfig ->
        onSaveInvoiceNumberConfig?.invoke(updatedConfig) { success, errorMsg ->
          if (success) {
            showInvoiceConfigDialog = false
            android.widget.Toast.makeText(context, "Invoice numbering configuration saved", android.widget.Toast.LENGTH_SHORT).show()
          } else {
            android.widget.Toast.makeText(context, "Failed: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
          }
        }
      }
    )
  }
}

/**
 * Normal User / Staff In-App Updates Card.
 */
@Composable
private fun NormalUserAppUpdatesSection(
  updateInfo: AppUpdateInfo,
  onCheckForUpdates: () -> Unit,
  onUpdateNow: () -> Unit,
  onLaterOrSkip: () -> Unit,
  onInstallUpdate: (Context) -> Unit
) {
  val context = LocalContext.current
  val hasUpdate = updateInfo.hasUpdate
  val manifest = updateInfo.manifest

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, if (hasUpdate && updateInfo.isForced) SoftRed else DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = when {
              updateInfo.isForced -> Color(0xFF3B0C0C)
              hasUpdate -> Color(0xFF0C2B42)
              else -> Emerald900
            },
            modifier = Modifier.size(38.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = when {
                  updateInfo.isForced -> Icons.Default.Warning
                  hasUpdate -> Icons.Default.SystemUpdate
                  else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = when {
                  updateInfo.isForced -> SoftRed
                  hasUpdate -> InfoSky
                  else -> Emerald400
                },
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column {
            Text(
              text = "App Updates",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              ),
              color = TextPrimaryDark
            )
            Text(
              text = "Current Version: ${updateInfo.installedVersionName}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = TextSecondaryDark
            )
          }
        }

        // Check for Updates action
        if (!hasUpdate && !updateInfo.isChecking) {
          OutlinedButton(
            onClick = onCheckForUpdates,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald400),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            modifier = Modifier.testTag("settings_check_update_button")
          ) {
            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Check for Updates", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
          }
        } else if (updateInfo.isChecking) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Emerald400)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // State Message & Actions
      if (!hasUpdate) {
        // No Update State
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkBg,
          border = BorderStroke(0.5.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "You are using the latest version.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = Emerald400
            )
          }
        }
      } else {
        // Update Available State
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, if (updateInfo.isForced) SoftRed else DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = "New version available: ${manifest?.versionName ?: "1.0.1"}",
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                  ),
                  color = if (updateInfo.isForced) SoftRed else Emerald400
                )
                if (updateInfo.releaseType == ReleaseType.RECOMMENDED && !updateInfo.isForced) {
                  Text(
                    text = "Grace period: ${updateInfo.remainingGraceDays} days remaining",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = InfoSky
                  )
                }
              }

              Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (updateInfo.releaseType) {
                  ReleaseType.FORCED, ReleaseType.CRITICAL -> SoftRed.copy(alpha = 0.2f)
                  ReleaseType.RECOMMENDED -> InfoSky.copy(alpha = 0.2f)
                  ReleaseType.SILENT -> GoldAmber.copy(alpha = 0.2f)
                  else -> Emerald400.copy(alpha = 0.2f)
                }
              ) {
                Text(
                  text = updateInfo.releaseType.badgeText,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = when (updateInfo.releaseType) {
                    ReleaseType.FORCED, ReleaseType.CRITICAL -> SoftRed
                    ReleaseType.RECOMMENDED -> InfoSky
                    ReleaseType.SILENT -> GoldAmber
                    else -> Emerald400
                  },
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            if (!manifest?.releaseNotes.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = manifest?.releaseNotes ?: "",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = TextSecondaryDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: [ Update Now ] and [ Later ] / [ Skip ]
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (!updateInfo.isForced) {
                OutlinedButton(
                  onClick = onLaterOrSkip,
                  shape = RoundedCornerShape(8.dp),
                  border = BorderStroke(1.dp, DarkBorder),
                  colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("staff_update_later_button")
                ) {
                  Text(text = if (updateInfo.isOptional) "Skip" else "Later", fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
              }

              if (updateInfo.isReadyToInstall) {
                Button(
                  onClick = { onInstallUpdate(context) },
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("staff_install_update_button")
                ) {
                  Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = "Install Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              } else {
                Button(
                  onClick = onUpdateNow,
                  shape = RoundedCornerShape(8.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (updateInfo.isForced) SoftRed else Emerald500,
                    contentColor = Color.White
                  ),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("staff_update_now_button")
                ) {
                  Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = "Update Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Admin-Only Update Management & Publishing Section.
 * Strictly forbidden and invisible for Staff users.
 */
@Composable
private fun AdminUpdateManagementSection(
  user: User?,
  updateInfo: AppUpdateInfo,
  githubStatus: GitHubStatusInfo,
  manifestUrl: String,
  onCheckGitHub: () -> Unit,
  onCreateRelease: () -> Unit,
  onRefresh: () -> Unit,
  onViewHistory: () -> Unit,
  onOpenSimulator: () -> Unit
) {
  val manifest = updateInfo.manifest

  val formattedLastChecked = if (updateInfo.lastCheckedTimestamp > 0L) {
    try {
      java.text.SimpleDateFormat("MMM dd, yyyy, hh:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(updateInfo.lastCheckedTimestamp))
    } catch (e: Exception) {
      "Recently"
    }
  } else {
    "Never"
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .testTag("admin_update_management_section")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .padding(12.dp)
    ) {
      // Admin Header (Compact)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = Emerald900,
          modifier = Modifier.size(28.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "In-App Updates (Admin Panel)",
            style = MaterialTheme.typography.titleSmall.copy(
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextPrimaryDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = "Authorized Administrator • ${user?.email ?: "admin"}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
            color = Emerald400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      if (updateInfo.isSimulation) {
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = GoldAmber.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, GoldAmber),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = null,
              tint = GoldAmber,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "SIMULATION ACTIVE — Testing scenario in progress. Live GitHub releases unaffected.",
              fontSize = 9.5.sp,
              color = GoldAmber,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Compact 2-Column Version Information Grid
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Row 1: Current Version & Latest Available
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "CURRENT VERSION",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = TextMutedDark
              )
              Text(
                text = "v${updateInfo.installedVersionName} (${updateInfo.installedVersionCode})",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                color = TextPrimaryDark
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "LATEST AVAILABLE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = TextMutedDark
              )
              Text(
                text = "v${manifest?.versionName ?: updateInfo.installedVersionName} (${manifest?.versionCode ?: updateInfo.installedVersionCode})",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Bold),
                color = Emerald400
              )
            }
          }

          // Row 2: Update Status & Release Type
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "UPDATE STATUS",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = TextMutedDark
              )
              Text(
                text = updateInfo.displayStatus,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = when {
                  updateInfo.isForced -> SoftRed
                  updateInfo.hasUpdate -> InfoSky
                  else -> Emerald400
                }
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "RELEASE TYPE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = TextMutedDark
              )
              Text(
                text = updateInfo.releaseType.displayName,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = when (updateInfo.releaseType) {
                  ReleaseType.FORCED, ReleaseType.CRITICAL -> SoftRed
                  ReleaseType.RECOMMENDED -> InfoSky
                  ReleaseType.SILENT -> GoldAmber
                  else -> Emerald400
                }
              )
            }
          }

          // Row 3: Last Check & Publish Source
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "LAST UPDATE CHECK",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = TextMutedDark
              )
              Text(
                text = formattedLastChecked,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = TextSecondaryDark
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "PUBLISH / STAGE SOURCE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
                color = TextMutedDark
              )
              Text(
                text = manifest?.publishedBy?.take(22) ?: "GitHub Actions / Admin",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = TextSecondaryDark
              )
            }
          }

          if (!manifest?.releaseNotes.isNullOrBlank()) {
            Text(
              text = "RELEASE NOTES",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, fontWeight = FontWeight.Bold),
              color = TextMutedDark
            )
            Text(
              text = manifest?.releaseNotes ?: "",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
              color = TextSecondaryDark,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // GitHub Release Status Indicator
      val statusColor = when {
        githubStatus.isGreen -> Emerald400
        githubStatus.isYellow -> GoldAmber
        else -> SoftRed
      }
      val statusIcon = when {
        githubStatus.isGreen -> Icons.Default.CloudDone
        githubStatus.isYellow -> Icons.Default.Info
        else -> Icons.Default.Warning
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = DarkBg,
        border = BorderStroke(0.5.dp, statusColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = statusIcon,
              contentDescription = null,
              tint = statusColor,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = "GitHub Release Status: ${githubStatus.message}",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
              color = statusColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = "manglam-app",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
            color = TextMutedDark
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Admin Action Buttons Row 1: [ Check for Updates ] [ Release Configuration ]
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedButton(
          onClick = onCheckGitHub,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = InfoSky),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .testTag("admin_check_github_button")
        ) {
          Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Check for Updates", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }

        Button(
          onClick = onCreateRelease,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
          modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .testTag("admin_create_release_button")
        ) {
          Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Release Config", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Admin Action Buttons Row 2: [ Release History ] [ Testing Simulator ] [ Refresh ]
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedButton(
          onClick = onViewHistory,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .testTag("admin_view_history_button")
        ) {
          Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(text = "History", fontSize = 10.sp, maxLines = 1)
        }

        OutlinedButton(
          onClick = onOpenSimulator,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAmber),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
          modifier = Modifier
            .weight(1.2f)
            .height(30.dp)
            .testTag("admin_open_simulator_button")
        ) {
          Icon(imageVector = Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(text = "Policy Simulator", fontSize = 10.sp, maxLines = 1)
        }

        OutlinedButton(
          onClick = onRefresh,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, DarkBorder),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald400),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
          modifier = Modifier
            .weight(0.9f)
            .height(30.dp)
            .testTag("admin_refresh_button")
        ) {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
          Spacer(modifier = Modifier.width(3.dp))
          Text(text = "Refresh", fontSize = 10.sp, maxLines = 1)
        }
      }
    }
  }
}

@Composable
private fun UserProfileCard(
  user: User?,
  isAdmin: Boolean,
  googleDisplayName: String?,
  onEditNameClick: () -> Unit,
  onLogoutClick: () -> Unit
) {
  val strings = LocalStrings.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, if (isAdmin) Emerald400.copy(alpha = 0.5f) else DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .testTag("settings_user_profile_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // User Avatar
      Surface(
        shape = CircleShape,
        color = if (isAdmin) Emerald900 else DarkSurfaceElevated,
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
            contentDescription = null,
            tint = if (isAdmin) Emerald400 else InfoSky,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // User Information: Name, Email, Branch
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = user?.name ?: "Mangalam Dealer",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.5.sp
          ),
          color = TextPrimaryDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = user?.email ?: "kartik.bharadwaj0105@gmail.com",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = TextSecondaryDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
          text = "Branch: ${user?.branchName ?: "Main Branch"}",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
          color = TextMutedDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Action Area: Edit display name & Integrated Logout Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        IconButton(
          onClick = onEditNameClick,
          modifier = Modifier
            .size(32.dp)
            .testTag("settings_edit_name_button")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = strings.changeGreetingName,
            tint = Emerald400,
            modifier = Modifier.size(16.dp)
          )
        }

        OutlinedButton(
          onClick = onLogoutClick,
          shape = RoundedCornerShape(8.dp),
          border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SoftRed,
            containerColor = SoftRed.copy(alpha = 0.08f)
          ),
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
          modifier = Modifier
            .height(30.dp)
            .testTag("settings_logout_button")
        ) {
          Icon(
            imageVector = Icons.Default.Logout,
            contentDescription = null,
            tint = SoftRed,
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(3.dp))
          Text(
            text = strings.signOut,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SoftRed
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title.uppercase(),
    style = MaterialTheme.typography.labelSmall.copy(
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    ),
    color = TextMutedDark,
    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
  )
}

@Composable
private fun SettingsItemCard(
  icon: ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  testTag: String
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = DarkSurfaceElevated,
        modifier = Modifier.size(38.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = TextPrimaryDark
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = TextSecondaryDark
        )
      }
    }
  }
}

@Composable
private fun LanguageSelectionCard(
  currentLanguage: String,
  onLanguageSelected: (String) -> Unit
) {
  val strings = LocalStrings.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Language,
          contentDescription = null,
          tint = Emerald400,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = strings.languageTitle,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = TextPrimaryDark
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        LanguageOptionButton(
          title = "English",
          isSelected = currentLanguage == "en",
          onClick = { onLanguageSelected("en") },
          modifier = Modifier.weight(1f)
        )

        LanguageOptionButton(
          title = "हिन्दी (Hindi)",
          isSelected = currentLanguage == "hi",
          onClick = { onLanguageSelected("hi") },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun LanguageOptionButton(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) Emerald900 else DarkSurfaceElevated,
    border = BorderStroke(
      if (isSelected) 1.5.dp else 1.dp,
      if (isSelected) Emerald400 else DarkBorder
    ),
    modifier = modifier.clickable { onClick() }
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.5.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        ),
        color = if (isSelected) Emerald400 else TextSecondaryDark
      )

      if (isSelected) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = null,
          tint = Emerald400,
          modifier = Modifier.size(16.dp)
        )
      }
    }
  }
}

@Composable
private fun ThemeSelectionCard(
  currentTheme: String,
  onThemeSelected: (String) -> Unit
) {
  val strings = LocalStrings.current
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Palette,
          contentDescription = null,
          tint = Emerald400,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = strings.themeMode,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = TextPrimaryDark
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ThemeOptionButton(
          title = "Dark",
          isSelected = currentTheme == "dark",
          onClick = { onThemeSelected("dark") },
          modifier = Modifier.weight(1f)
        )

        ThemeOptionButton(
          title = "Light",
          isSelected = currentTheme == "light",
          onClick = { onThemeSelected("light") },
          modifier = Modifier.weight(1f)
        )

        ThemeOptionButton(
          title = "System",
          isSelected = currentTheme == "system",
          onClick = { onThemeSelected("system") },
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun ThemeOptionButton(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) Emerald900 else DarkSurfaceElevated,
    border = BorderStroke(
      if (isSelected) 1.5.dp else 1.dp,
      if (isSelected) Emerald400 else DarkBorder
    ),
    modifier = modifier.clickable { onClick() }
  ) {
    Box(
      modifier = Modifier.padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 12.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        ),
        color = if (isSelected) Emerald400 else TextSecondaryDark
      )
    }
  }
}

@Composable
private fun EditDisplayNameDialog(
  initialName: String,
  googleName: String?,
  onSave: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val strings = LocalStrings.current
  var selectedOption by remember { mutableStateOf(if (googleName != null && initialName == googleName) "google" else "custom") }
  var customNameInput by remember { mutableStateOf(if (selectedOption == "custom") initialName else "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(text = strings.whatShouldWeCallYou, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    },
    text = {
      Column {
        Text(text = strings.chooseGreetingNameDesc, color = TextSecondaryDark, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (!googleName.isNullOrBlank()) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedOption = "google" }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = selectedOption == "google",
              onClick = { selectedOption = "google" },
              colors = RadioButtonDefaults.colors(selectedColor = Emerald400, unselectedColor = DarkBorder)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
              Text(text = strings.useGoogleName, color = TextPrimaryDark, fontSize = 13.sp)
              Text(text = googleName, color = Emerald400, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { selectedOption = "custom" }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = selectedOption == "custom",
            onClick = { selectedOption = "custom" },
            colors = RadioButtonDefaults.colors(selectedColor = Emerald400, unselectedColor = DarkBorder)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = strings.customName, color = TextPrimaryDark, fontSize = 13.sp)
        }

        if (selectedOption == "custom") {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = customNameInput,
            onValueChange = { customNameInput = it },
            placeholder = { Text(strings.enterYourName) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val finalName = if (selectedOption == "google" && !googleName.isNullOrBlank()) {
            googleName
          } else {
            customNameInput.trim().ifBlank { initialName }
          }
          onSave(finalName)
        },
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White)
      ) {
        Text(text = strings.save)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = strings.cancel, color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard
  )
}

/**
 * Compact Invoice Number Section matching App Updates visual language.
 */
@Composable
private fun InvoiceNumberSection(
  config: InvoiceNumberConfig,
  isAdmin: Boolean,
  onConfigureClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("invoice_number_settings_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: 🧾 Invoice Number / Automatic sequential numbering
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = CircleShape,
          color = Emerald900,
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Receipt,
              contentDescription = "Invoice Number",
              tint = Emerald400,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = "Invoice Number",
            style = MaterialTheme.typography.titleMedium.copy(
              fontSize = 14.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextPrimaryDark
          )
          Text(
            text = "Automatic sequential numbering",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
            color = TextSecondaryDark
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Compact details rows
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = DarkSurfaceElevated,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Starting Number",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = TextSecondaryDark
            )
            Text(
              text = config.startingNumber.toString(),
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
              ),
              color = TextPrimaryDark
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Next Invoice",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
              color = TextSecondaryDark
            )
            Text(
              text = config.formattedNextNumber(),
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
              ),
              color = Emerald400
            )
          }
        }
      }

      if (isAdmin) {
        Spacer(modifier = Modifier.height(10.dp))
        Button(
          onClick = onConfigureClick,
          shape = RoundedCornerShape(8.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.Black),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_configure_invoice_number_button")
        ) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Configure",
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp
          )
        }
      }
    }
  }
}

/**
 * Admin Dialog for configuring starting invoice number, next sequence number, and prefix/suffix.
 */
@Composable
private fun AdminInvoiceNumberConfigDialog(
  currentConfig: InvoiceNumberConfig,
  existingInvoices: List<Invoice>,
  onDismiss: () -> Unit,
  onSave: (InvoiceNumberConfig) -> Unit
) {
  var startingNumberInput by remember { mutableStateOf(currentConfig.startingNumber.toString()) }
  var nextNumberInput by remember { mutableStateOf(currentConfig.nextInvoiceNumber.toString()) }
  var prefixInput by remember { mutableStateOf(currentConfig.prefix) }
  var suffixInput by remember { mutableStateOf(currentConfig.suffix) }
  var showConfirmationDialog by remember { mutableStateOf(false) }
  var pendingConfigToSave by remember { mutableStateOf<InvoiceNumberConfig?>(null) }

  val highestExisting = remember(existingInvoices) {
    InvoiceNumberManager.findHighestIssuedInvoiceNumber(existingInvoices)
  }

  val parsedStarting = startingNumberInput.toLongOrNull()
  val parsedNext = nextNumberInput.toLongOrNull()

  val validationError = remember(parsedStarting, parsedNext) {
    when {
      parsedStarting == null || parsedStarting <= 0L -> "Starting number must be a valid positive integer."
      parsedNext == null || parsedNext <= 0L -> "Next invoice number must be a valid positive integer."
      parsedNext < parsedStarting -> "Next invoice number cannot be lower than the starting number."
      else -> null
    }
  }

  val conflictWarning = remember(parsedNext, parsedStarting, highestExisting) {
    if (highestExisting != null && highestExisting > 0L) {
      val candidateNext = parsedNext ?: 0L
      val candidateStarting = parsedStarting ?: 0L
      if (candidateNext <= highestExisting || candidateStarting <= highestExisting) {
        "Warning: Setting a starting or next number ($candidateNext) that is less than or equal to the highest existing invoice ($highestExisting) may cause duplicate invoice numbers. Existing invoices will not be modified."
      } else null
    } else null
  }

  if (showConfirmationDialog && pendingConfigToSave != null) {
    val pending = pendingConfigToSave!!
    AlertDialog(
      onDismissRequest = { showConfirmationDialog = false },
      title = {
        Text(
          text = "Change Invoice Sequence?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimaryDark
        )
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceElevated,
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Current next number:",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondaryDark
                )
                Text(
                  text = currentConfig.formattedNextNumber(),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = TextPrimaryDark
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "New next number:",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextSecondaryDark
                )
                Text(
                  text = pending.formattedNextNumber(),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = Emerald400
                )
              }
            }
          }

          Text(
            text = "Existing invoices will not be changed.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = TextSecondaryDark
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showConfirmationDialog = false
            onSave(pending)
          },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.Black),
          modifier = Modifier.testTag("admin_confirm_invoice_sequence_button")
        ) {
          Text("Confirm", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showConfirmationDialog = false }) {
          Text("Cancel", color = TextSecondaryDark)
        }
      },
      containerColor = DarkCard
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Receipt,
          contentDescription = null,
          tint = Emerald400,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Invoice Number Settings",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          ),
          color = TextPrimaryDark
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Current Number & Next Invoice Number Summary
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = DarkSurfaceElevated,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Current Number",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
              )
              Text(
                text = if (currentConfig.lastIssuedNumber != null) currentConfig.formatNumber(currentConfig.lastIssuedNumber) else currentConfig.startingNumber.toString(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimaryDark
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "Next Invoice Number",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
              )
              Text(
                text = currentConfig.formattedNextNumber(),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Emerald400
              )
            }
          }
        }

        // Starting / Next Number Input
        OutlinedTextField(
          value = startingNumberInput,
          onValueChange = { newValue ->
            startingNumberInput = newValue.filter { it.isDigit() }
            if (nextNumberInput.isBlank() || nextNumberInput == currentConfig.startingNumber.toString()) {
              nextNumberInput = startingNumberInput
            }
          },
          label = { Text("Starting / Next Number", fontSize = 12.sp) },
          placeholder = { Text("e.g. 2026001", fontSize = 12.sp) },
          singleLine = true,
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_starting_invoice_input")
        )

        // Clear explanation
        Text(
          text = "Invoice numbers are generated sequentially.\nExisting invoice numbers will never be changed.",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
          color = TextSecondaryDark
        )

        // Conflict / Duplicate Warning Banner if applicable
        if (conflictWarning != null) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = GoldAmber.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, GoldAmber),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(10.dp),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = GoldAmber,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = conflictWarning,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = GoldAmber
              )
            }
          }
        }

        // Validation Error Message
        if (validationError != null) {
          Text(
            text = validationError,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = SoftRed
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (parsedStarting != null && parsedNext != null && validationError == null) {
            val updated = currentConfig.copy(
              startingNumber = parsedStarting,
              nextInvoiceNumber = parsedNext,
              prefix = prefixInput.trim(),
              suffix = suffixInput.trim(),
              updatedAt = System.currentTimeMillis()
            )
            if (updated.nextInvoiceNumber != currentConfig.nextInvoiceNumber || updated.startingNumber != currentConfig.startingNumber) {
              pendingConfigToSave = updated
              showConfirmationDialog = true
            } else {
              onSave(updated)
            }
          }
        },
        enabled = validationError == null,
        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.Black),
        modifier = Modifier.testTag("admin_save_invoice_config_button")
      ) {
        Text("Save Configuration", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard
  )
}
