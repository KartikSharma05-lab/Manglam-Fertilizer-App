package com.manglamfertilizer.app.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.ReleaseHistoryItem
import com.manglamfertilizer.app.data.model.ReleaseType
import com.manglamfertilizer.app.data.model.UpdateManifest
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
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

/**
 * Admin Dialog: Configure and Stage a New App Release.
 * Distinctly identifies this as local release configuration staging.
 * Real production releases are built and published by GitHub Actions CI/CD.
 */
@Composable
fun AdminReleaseConfigurationDialog(
  adminEmail: String,
  currentVersionName: String,
  currentVersionCode: Long,
  onSaveConfiguration: (UpdateManifest) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  var versionName by remember { mutableStateOf("1.0.1") }
  var versionCodeStr by remember { mutableStateOf((currentVersionCode + 1).toString()) }
  var selectedType by remember { mutableStateOf(ReleaseType.RECOMMENDED) }
  var releaseTitle by remember { mutableStateOf("Fertilizer ERP & Performance Improvements") }
  var releaseNotes by remember {
    mutableStateOf(
      "• Enhanced offline stock reconciliation\n• Speed improvements for Bluetooth 58mm/80mm billing\n• Bug fixes and database stability improvements"
    )
  }
  var releaseNotesHindi by remember {
    mutableStateOf("• स्टॉक प्रबंधन में सुधार\n• तेज बिलिंग व प्रिंटिंग\n• सामान्य सुधार")
  }
  var forceAfterDaysStr by remember { mutableStateOf("15") }
  var minSupportedVersionCodeStr by remember { mutableStateOf("1") }
  var apkUrl by remember {
    mutableStateOf("https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v1.0.1/ManglamFertilizer-v1.0.1.apk")
  }
  var sha256 by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val scrollState = rememberScrollState()

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
        color = DarkCard,
        border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
        modifier = modifier
          .fillMaxWidth()
          .fillMaxHeight(0.92f)
          .testTag("admin_release_configuration_dialog")
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = Emerald900,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Tune,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Column {
              Text(
                text = "Release Configuration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryDark
              )
              Text(
                text = "Authorized Admin • $adminEmail",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Emerald400
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondaryDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Informational CI/CD Notice Banner
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
              imageVector = Icons.Default.Info,
              contentDescription = null,
              tint = InfoSky,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Production releases are created by GitHub Actions. This form configures and saves release metadata locally for verification and workflow staging.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
              color = TextSecondaryDark
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Form Content
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(scrollState)
        ) {
          // Version Info Card
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "VERSION SPECIFICATION",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = TextMutedDark
              )
              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedTextField(
                  value = versionName,
                  onValueChange = { versionName = it },
                  label = { Text("Version Name") },
                  placeholder = { Text("e.g. 1.0.1") },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald400,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_input_version_name")
                )

                OutlinedTextField(
                  value = versionCodeStr,
                  onValueChange = { versionCodeStr = it },
                  label = { Text("Version Code") },
                  placeholder = { Text("e.g. 2") },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald400,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_input_version_code")
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Release Type Selector (5 Defined Types)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "RELEASE TYPE POLICY",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = TextMutedDark
              )
              Spacer(modifier = Modifier.height(8.dp))

              ReleaseTypeOptionCard(
                type = ReleaseType.OPTIONAL,
                isSelected = selectedType == ReleaseType.OPTIONAL,
                title = "OPTIONAL UPDATE",
                description = "Low priority. Minor UI or text tweaks. Users can freely skip.",
                accentColor = Emerald400,
                onClick = { selectedType = ReleaseType.OPTIONAL }
              )

              Spacer(modifier = Modifier.height(6.dp))

              ReleaseTypeOptionCard(
                type = ReleaseType.RECOMMENDED,
                isSelected = selectedType == ReleaseType.RECOMMENDED,
                title = "RECOMMENDED UPDATE",
                description = "Normal feature update. Daily reminders with a 15-day grace period before mandatory update.",
                accentColor = InfoSky,
                onClick = { selectedType = ReleaseType.RECOMMENDED }
              )

              Spacer(modifier = Modifier.height(6.dp))

              ReleaseTypeOptionCard(
                type = ReleaseType.SILENT,
                isSelected = selectedType == ReleaseType.SILENT,
                title = "SILENT / LOW-IMPACT UPDATE",
                description = "Discreet background notification. Non-intrusive flow, standard Android authorization.",
                accentColor = GoldAmber,
                onClick = { selectedType = ReleaseType.SILENT }
              )

              Spacer(modifier = Modifier.height(6.dp))

              ReleaseTypeOptionCard(
                type = ReleaseType.FORCED,
                isSelected = selectedType == ReleaseType.FORCED,
                title = "FORCED UPDATE",
                description = "Immediate mandatory update. Blocks application usage until APK is installed.",
                accentColor = SoftRed,
                onClick = { selectedType = ReleaseType.FORCED }
              )

              Spacer(modifier = Modifier.height(6.dp))

              ReleaseTypeOptionCard(
                type = ReleaseType.CRITICAL,
                isSelected = selectedType == ReleaseType.CRITICAL,
                title = "SECURITY-CRITICAL UPDATE",
                description = "Critical bug or security patch. Immediate forced update for all active installations.",
                accentColor = SoftRed,
                onClick = { selectedType = ReleaseType.CRITICAL }
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Grace Period & Minimum Supported Version Policy Card
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "FORCE PERIOD & MINIMUM VERSION COMPLIANCE",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = TextMutedDark
              )
              Spacer(modifier = Modifier.height(10.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                OutlinedTextField(
                  value = forceAfterDaysStr,
                  onValueChange = { forceAfterDaysStr = it },
                  label = { Text("Force After Days") },
                  supportingText = { Text("Default: 15 (>= 0)", fontSize = 10.5.sp, color = TextMutedDark) },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald400,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_input_force_days")
                )

                OutlinedTextField(
                  value = minSupportedVersionCodeStr,
                  onValueChange = { minSupportedVersionCodeStr = it },
                  label = { Text("Min Version Code") },
                  supportingText = { Text("Forced if below this", fontSize = 10.5.sp, color = TextMutedDark) },
                  singleLine = true,
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald400,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("admin_input_min_version_code")
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Release Notes & Changelog
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "RELEASE NOTES & TITLES",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = TextMutedDark
              )
              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = releaseTitle,
                onValueChange = { releaseTitle = it },
                label = { Text("Release Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = releaseNotes,
                onValueChange = { releaseNotes = it },
                label = { Text("What's New (English)") },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = releaseNotesHindi,
                onValueChange = { releaseNotesHindi = it },
                label = { Text("What's New (हिन्दी - Optional)") },
                minLines = 2,
                maxLines = 4,
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

          Spacer(modifier = Modifier.height(12.dp))

          // Distribution Package URL & Checksum
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "ARTIFACT DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                ),
                color = TextMutedDark
              )
              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = apkUrl,
                onValueChange = { apkUrl = it },
                label = { Text("APK Download URL") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Emerald400,
                  unfocusedBorderColor = DarkBorder,
                  focusedTextColor = TextPrimaryDark,
                  unfocusedTextColor = TextPrimaryDark
                ),
                modifier = Modifier.fillMaxWidth()
              )

              Spacer(modifier = Modifier.height(10.dp))

              OutlinedTextField(
                value = sha256,
                onValueChange = { sha256 = it },
                label = { Text("SHA-256 Checksum (Optional)") },
                placeholder = { Text("64-character SHA-256 hash") },
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

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = errorMessage ?: "",
              color = SoftRed,
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark)
          ) {
            Text(text = "Cancel", fontSize = 13.sp)
          }

          Spacer(modifier = Modifier.width(10.dp))

          Button(
            onClick = {
              val code = versionCodeStr.toLongOrNull() ?: 0L
              if (versionName.isBlank()) {
                errorMessage = "Please enter a valid version name"
                return@Button
              }
              if (code <= 0L) {
                errorMessage = "Version code must be a positive integer (> 0)"
                return@Button
              }
              val forceDays = forceAfterDaysStr.toIntOrNull()
              if (forceDays == null || forceDays < 0) {
                errorMessage = "Force after days must be an integer >= 0"
                return@Button
              }
              val minSupportedCode = minSupportedVersionCodeStr.toLongOrNull() ?: 1L
              if (minSupportedCode < 0L) {
                errorMessage = "Minimum supported version code must be >= 0"
                return@Button
              }

              val nowFormatted = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm 'IST'", java.util.Locale.getDefault()).format(java.util.Date())
              val manifest = UpdateManifest(
                packageName = "com.manglamfertilizer.app",
                versionName = versionName.trim(),
                versionCode = code,
                releaseType = selectedType.name,
                releaseTitle = releaseTitle.trim(),
                releaseNotes = releaseNotes.trim(),
                releaseNotesHindi = releaseNotesHindi.trim().ifBlank { null },
                apkUrl = apkUrl.trim(),
                sha256 = sha256.trim(),
                publishedAt = nowFormatted,
                publishedBy = adminEmail,
                forceAfterDays = forceDays,
                minimumSupportedVersion = "1.0.0",
                minimumSupportedVersionCode = minSupportedCode
              )

              onSaveConfiguration(manifest)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Emerald500,
              contentColor = Color.White
            ),
            modifier = Modifier.testTag("admin_save_release_configuration")
          ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Save Release Configuration", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}
}

/**
 * Backward compatibility alias for AdminReleaseConfigurationDialog
 */
@Composable
fun AdminPublishUpdateDialog(
  adminEmail: String,
  currentVersionName: String,
  currentVersionCode: Long,
  onPublish: (UpdateManifest) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  AdminReleaseConfigurationDialog(
    adminEmail = adminEmail,
    currentVersionName = currentVersionName,
    currentVersionCode = currentVersionCode,
    onSaveConfiguration = onPublish,
    onDismiss = onDismiss,
    modifier = modifier
  )
}

@Composable
private fun ReleaseTypeOptionCard(
  type: ReleaseType,
  isSelected: Boolean,
  title: String,
  description: String,
  accentColor: Color,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isSelected) accentColor.copy(alpha = 0.15f) else DarkBg,
    border = BorderStroke(
      if (isSelected) 1.5.dp else 1.dp,
      if (isSelected) accentColor else DarkBorder
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = if (isSelected) accentColor else DarkBorder,
        modifier = Modifier.size(18.dp)
      ) {
        if (isSelected) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 12.5.sp
          ),
          color = if (isSelected) accentColor else TextPrimaryDark
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = TextSecondaryDark
        )
      }
    }
  }
}

/**
 * Admin Dialog: View Release History.
 * Shows production release baseline and any verified release history records.
 */
@Composable
fun ReleaseHistoryDialog(
  history: List<ReleaseHistoryItem>,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
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
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
          .fillMaxWidth()
          .fillMaxHeight(0.88f)
          .testTag("admin_release_history_dialog")
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = Emerald900,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.History,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Column {
              Text(
                text = "Release History",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimaryDark
              )
              Text(
                text = "${history.size} Production / Staged Releases Recorded",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondaryDark
              )
            }
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = TextSecondaryDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (history.isEmpty()) {
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No release history recorded yet.",
              color = TextSecondaryDark,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            items(history) { item ->
              ReleaseHistoryRowCard(item = item)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
          onClick = onDismiss,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceElevated,
            contentColor = TextPrimaryDark
          ),
          modifier = Modifier.align(Alignment.End)
        ) {
          Text(text = "Close", fontSize = 13.sp)
        }
      }
    }
  }
}
}

@Composable
private fun ReleaseHistoryRowCard(item: ReleaseHistoryItem) {
  val type = item.type
  val badgeColor = when (type) {
    ReleaseType.FORCED, ReleaseType.CRITICAL -> SoftRed
    ReleaseType.RECOMMENDED -> InfoSky
    ReleaseType.SILENT -> GoldAmber
    else -> Emerald400
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkSurfaceElevated,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "v${item.versionName} (${item.versionCode})",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimaryDark
          )

          Surface(
            shape = RoundedCornerShape(4.dp),
            color = badgeColor.copy(alpha = 0.2f),
            border = BorderStroke(0.5.dp, badgeColor)
          ) {
            Text(
              text = type.badgeText,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          if (item.status.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = DarkBg,
              border = BorderStroke(0.5.dp, DarkBorder)
            ) {
              Text(
                text = item.status,
                fontSize = 9.5.sp,
                color = TextSecondaryDark,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
              )
            }
          }
        }

        Text(
          text = item.publishedAt,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
          color = TextMutedDark
        )
      }

      if (item.releaseTitle.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = item.releaseTitle,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = Emerald400
        )
      }

      if (item.releaseNotes.isNotBlank()) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = item.releaseNotes,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
          color = TextSecondaryDark
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "By: ${item.publishedBy}",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = TextMutedDark,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        if (type == ReleaseType.RECOMMENDED) {
          Text(
            text = "Force after: ${item.forceAfterDays} days",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            color = InfoSky
          )
        }
      }
    }
  }
}

/**
 * Admin Testing Simulator Dialog:
 * Allows Admin to test all 5 release types and grace period scenarios in development.
 * Explicitly labeled SIMULATION to prevent confusion with real GitHub releases.
 */
@Composable
fun AdminTestingSimulatorSheet(
  onSimulate: (String, Long, ReleaseType, Int, Int, String) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = Icons.Default.HourglassBottom, contentDescription = null, tint = GoldAmber)
        Text(text = "Release Policy Simulator", color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Prominent Warning Banner for Simulation
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = GoldAmber.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, GoldAmber),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = GoldAmber, modifier = Modifier.size(16.dp))
            Text(
              text = "[SIMULATION ONLY] Simulates update policies on-device. Simulated data is labeled SIMULATION and is never treated as a real GitHub production release.",
              fontSize = 10.5.sp,
              color = GoldAmber,
              lineHeight = 14.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        SimulatorScenarioButton(
          title = "1. [SIMULATION] Optional Update (Minor UI)",
          subtitle = "User gets [ Update ] and [ Skip ] without blocking.",
          color = Emerald400,
          onClick = {
            onSimulate("1.0.1", 2L, ReleaseType.OPTIONAL, 0, 15, "• [SIMULATION] Minor UI tweaks\n• [SIMULATION] Text corrections")
          }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SimulatorScenarioButton(
          title = "2. [SIMULATION] Recommended Update (Day 2 of 15)",
          subtitle = "Grace period active (13 days left). User can [ Update Now ] or [ Later ].",
          color = InfoSky,
          onClick = {
            onSimulate("1.1.0", 3L, ReleaseType.RECOMMENDED, 2, 15, "• [SIMULATION] Improved inventory\n• [SIMULATION] Faster billing\n• [SIMULATION] Bug fixes")
          }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SimulatorScenarioButton(
          title = "3. [SIMULATION] 15-Day Policy Expired (Day 16 of 15)",
          subtitle = "Grace period elapsed. Forced update engages — No Skip/Later button!",
          color = SoftRed,
          onClick = {
            onSimulate("1.1.0", 3L, ReleaseType.RECOMMENDED, 16, 15, "• [SIMULATION] Mandatory update\n• [SIMULATION] 15-day grace period expired")
          }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SimulatorScenarioButton(
          title = "4. [SIMULATION] Security-Critical Update",
          subtitle = "Immediate mandatory update required before app access.",
          color = SoftRed,
          onClick = {
            onSimulate("1.2.0", 4L, ReleaseType.CRITICAL, 0, 15, "• [SIMULATION] Security patch & critical database migration")
          }
        )

        Spacer(modifier = Modifier.height(6.dp))

        SimulatorScenarioButton(
          title = "5. [SIMULATION] Silent / Low-Impact Update",
          subtitle = "Discreet notification, background preparation, standard Android flow.",
          color = GoldAmber,
          onClick = {
            onSimulate("1.0.2", 5L, ReleaseType.SILENT, 0, 15, "• [SIMULATION] Non-intrusive telemetry fix")
          }
        )
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(text = "Close", color = TextSecondaryDark)
      }
    },
    containerColor = DarkCard
  )
}

@Composable
private fun SimulatorScenarioButton(
  title: String,
  subtitle: String,
  color: Color,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DarkSurfaceElevated,
    border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = color)
      Text(text = subtitle, fontSize = 10.5.sp, color = TextSecondaryDark)
    }
  }
}
