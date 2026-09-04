package com.manglamfertilizer.app.ui.update

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.ReleaseType
import com.manglamfertilizer.app.data.model.UpdateEngineState
import com.manglamfertilizer.app.ui.localization.LocalStrings
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
import java.text.DecimalFormat

@Composable
fun UpdateDialog(
  updateInfo: AppUpdateInfo,
  onDownloadClick: () -> Unit,
  onCancelDownload: () -> Unit,
  onInstallClick: (Context) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val strings = LocalStrings.current
  val manifest = updateInfo.manifest
  val isForced = updateInfo.isForced
  val isOptional = updateInfo.isOptional
  val releaseType = updateInfo.releaseType

  val dialogTitle = when {
    isForced -> "Update Required"
    isOptional -> "An optional update is available."
    else -> "New Update Available"
  }

  Dialog(
    onDismissRequest = {
      if (!isForced && updateInfo.state != UpdateEngineState.DOWNLOADING) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = !isForced && updateInfo.state != UpdateEngineState.DOWNLOADING,
      dismissOnClickOutside = !isForced && updateInfo.state != UpdateEngineState.DOWNLOADING,
      usePlatformDefaultWidth = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, if (isForced) SoftRed else DarkBorder),
        modifier = modifier
          .fillMaxWidth()
          .testTag("update_dialog")
      ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header Row: Icon + Title + Close Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(
              shape = CircleShape,
              color = when {
                isForced -> Color(0xFF3B0C0C)
                updateInfo.state == UpdateEngineState.READY_TO_INSTALL -> Emerald900
                else -> Color(0xFF0C2B42)
              },
              modifier = Modifier.size(44.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = when {
                    isForced -> Icons.Default.Warning
                    updateInfo.state == UpdateEngineState.READY_TO_INSTALL -> Icons.Default.CheckCircle
                    updateInfo.state == UpdateEngineState.DOWNLOADING -> Icons.Default.CloudDownload
                    updateInfo.state == UpdateEngineState.VERIFYING -> Icons.Default.Security
                    updateInfo.state == UpdateEngineState.VERIFICATION_FAILED -> Icons.Default.ErrorOutline
                    else -> Icons.Default.NewReleases
                  },
                  contentDescription = null,
                  tint = when {
                    isForced -> SoftRed
                    updateInfo.state == UpdateEngineState.READY_TO_INSTALL -> Emerald400
                    updateInfo.state == UpdateEngineState.DOWNLOADING -> InfoSky
                    updateInfo.state == UpdateEngineState.VERIFYING -> GoldAmber
                    updateInfo.state == UpdateEngineState.VERIFICATION_FAILED -> SoftRed
                    else -> Emerald400
                  },
                  modifier = Modifier.size(24.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = dialogTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                  fontSize = 17.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = if (isForced) SoftRed else TextPrimaryDark
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Version: ${manifest?.versionName ?: "1.0.1"}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 13.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = Emerald400
              )
            }
          }

          if (!isForced && updateInfo.state != UpdateEngineState.DOWNLOADING) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .size(32.dp)
                .testTag("update_dialog_close_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = strings.close,
                tint = TextSecondaryDark,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Version Comparison & Release Type Pills
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = DarkSurfaceElevated,
            border = BorderStroke(1.dp, DarkBorder)
          ) {
            Text(
              text = "Current: v${updateInfo.installedVersionName}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              ),
              color = TextSecondaryDark,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          val releaseTypeBadgeColor = when (releaseType) {
            ReleaseType.CRITICAL, ReleaseType.FORCED -> SoftRed
            ReleaseType.RECOMMENDED -> InfoSky
            ReleaseType.SILENT -> GoldAmber
            else -> Emerald400
          }
          val releaseTypeBg = when (releaseType) {
            ReleaseType.CRITICAL, ReleaseType.FORCED -> Color(0xFF3B0C0C)
            ReleaseType.RECOMMENDED -> Color(0xFF0C2B42)
            ReleaseType.SILENT -> Color(0xFF3B2A06)
            else -> Emerald900
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = releaseTypeBg,
            border = BorderStroke(1.dp, releaseTypeBadgeColor)
          ) {
            Text(
              text = releaseType.badgeText.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = releaseTypeBadgeColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        // Grace Period Reminder for Recommended Updates (15-Day Policy)
        if (!isForced && releaseType == ReleaseType.RECOMMENDED) {
          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = InfoSky.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = InfoSky,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Grace period: ${updateInfo.remainingGraceDays} days remaining before required",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = InfoSky
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Release Title & Changelog
        if (!manifest?.releaseTitle.isNullOrBlank()) {
          Text(
            text = manifest?.releaseTitle ?: "",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextPrimaryDark
          )
          Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
          text = "WHAT'S NEW:",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
          ),
          color = TextSecondaryDark
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Scrollable Release Notes Container
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp, max = 150.dp)
        ) {
          val scrollState = rememberScrollState()
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .verticalScroll(scrollState)
              .padding(10.dp)
          ) {
            Text(
              text = manifest?.releaseNotes?.ifBlank { "• Improved inventory\n• Faster billing\n• UI improvements\n• Bug fixes" }
                ?: "• Improved inventory\n• Faster billing\n• UI improvements\n• Bug fixes",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 18.sp
              ),
              color = TextPrimaryDark
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic State Progress & Messages
        when (updateInfo.state) {
          UpdateEngineState.DOWNLOADING -> {
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = strings.downloadingUpdate,
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                  ),
                  color = TextSecondaryDark
                )
                Text(
                  text = "${(updateInfo.downloadProgress * 100).toInt()}%",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = Emerald400
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              LinearProgressIndicator(
                progress = { updateInfo.downloadProgress },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = Emerald400,
                trackColor = DarkBorder
              )
              Spacer(modifier = Modifier.height(4.dp))
              val df = DecimalFormat("#.##")
              val downloadedMb = df.format(updateInfo.downloadedBytes.toDouble() / (1024 * 1024))
              val totalMb = df.format(updateInfo.totalBytes.toDouble() / (1024 * 1024))
              Text(
                text = "$downloadedMb MB / $totalMb MB",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  color = TextMutedDark
                )
              )
            }
          }

          UpdateEngineState.VERIFYING -> {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = GoldAmber
              )
              Text(
                text = "Verifying cryptographic signatures and package integrity...",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 12.sp,
                  color = GoldAmber
                )
              )
            }
          }

          UpdateEngineState.READY_TO_INSTALL -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Emerald900,
              border = BorderStroke(1.dp, Emerald400),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = "Update verified and ready to install.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald400
                  )
                )
              }
            }
          }

          UpdateEngineState.VERIFICATION_FAILED, UpdateEngineState.DOWNLOAD_FAILED -> {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF3B0C0C),
              border = BorderStroke(1.dp, SoftRed),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = SoftRed,
                  modifier = Modifier.size(18.dp)
                )
                Text(
                  text = updateInfo.errorMessage ?: "Update operation failed.",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = SoftRed
                  )
                )
              }
            }
          }

          else -> {
            // Idle info
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = DarkSurfaceElevated,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = "Secure In-App distribution channel via GitHub Releases",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.5.sp,
                    color = TextSecondaryDark
                  )
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Skip / Later button is shown ONLY if NOT forced and not downloading
          if (!isForced && updateInfo.state != UpdateEngineState.DOWNLOADING && updateInfo.state != UpdateEngineState.VERIFYING) {
            OutlinedButton(
              onClick = onDismiss,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, DarkBorder),
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondaryDark
              ),
              modifier = Modifier.testTag("update_dialog_cancel_button")
            ) {
              Text(
                text = if (isOptional) "Skip" else "Later",
                fontSize = 13.sp
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
          }

          when (updateInfo.state) {
            UpdateEngineState.DOWNLOADING -> {
              OutlinedButton(
                onClick = onCancelDownload,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SoftRed),
                colors = ButtonDefaults.outlinedButtonColors(
                  contentColor = SoftRed
                ),
                modifier = Modifier.testTag("update_dialog_pause_button")
              ) {
                Text(text = strings.cancel, fontSize = 13.sp)
              }
            }

            UpdateEngineState.READY_TO_INSTALL -> {
              Button(
                onClick = { onInstallClick(context) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = Emerald500,
                  contentColor = Color.White
                ),
                modifier = Modifier.testTag("update_dialog_install_button")
              ) {
                Icon(
                  imageVector = Icons.Default.InstallMobile,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Install Now",
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp
                )
              }
            }

            UpdateEngineState.VERIFYING, UpdateEngineState.INSTALLING -> {
              // Active transition indicator
            }

            else -> {
              val updateButtonLabel = when {
                updateInfo.state == UpdateEngineState.VERIFICATION_FAILED || updateInfo.state == UpdateEngineState.DOWNLOAD_FAILED -> "Retry Download"
                isOptional -> "Update"
                else -> "Update Now"
              }

              Button(
                onClick = onDownloadClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isForced) SoftRed else Emerald500,
                  contentColor = Color.White
                ),
                modifier = Modifier.testTag("update_dialog_download_button")
              ) {
                Icon(
                  imageVector = Icons.Default.CloudDownload,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = updateButtonLabel,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.5.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
}
