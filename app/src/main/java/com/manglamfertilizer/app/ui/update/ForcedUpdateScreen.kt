package com.manglamfertilizer.app.ui.update

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.UpdateEngineState
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.DecimalFormat

@Composable
fun ForcedUpdateScreen(
  updateInfo: AppUpdateInfo,
  onDownloadClick: () -> Unit,
  onInstallClick: (Context) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val strings = LocalStrings.current
  val manifest = updateInfo.manifest

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .padding(24.dp)
      .testTag("forced_update_screen"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Large Warning Badge
      Surface(
        shape = CircleShape,
        color = Color(0xFF3B0C0C),
        border = BorderStroke(2.dp, SoftRed),
        modifier = Modifier.size(80.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = SoftRed,
            modifier = Modifier.size(40.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = strings.forcedUpdateTitle,
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp
        ),
        color = TextPrimaryDark,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Version ${manifest?.versionName ?: "1.0.0"} is now mandatory",
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 14.sp
        ),
        color = SoftRed,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = strings.forcedUpdateDesc,
        style = MaterialTheme.typography.bodyMedium.copy(
          lineHeight = 20.sp,
          fontSize = 13.5.sp
        ),
        color = TextSecondaryDark,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
      )

      Spacer(modifier = Modifier.height(20.dp))

      // What's New Card
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Text(
            text = strings.whatsNew.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp,
              fontSize = 11.sp
            ),
            color = TextSecondaryDark
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = manifest?.releaseNotes?.ifBlank { "Important security and functionality enhancements." }
              ?: "Important security and functionality enhancements.",
            style = MaterialTheme.typography.bodySmall.copy(
              lineHeight = 18.sp,
              fontSize = 13.sp
            ),
            color = TextPrimaryDark
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Progress & Status Indicators
      when (updateInfo.state) {
        UpdateEngineState.DOWNLOADING -> {
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = strings.downloadingUpdate,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
              )
              Text(
                text = "${(updateInfo.downloadProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = Emerald400
              )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
              progress = { updateInfo.downloadProgress },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = Emerald400,
              trackColor = DarkBorder
            )
            Spacer(modifier = Modifier.height(4.dp))
            val df = DecimalFormat("#.##")
            val downloadedMb = df.format(updateInfo.downloadedBytes.toDouble() / (1024 * 1024))
            val totalMb = df.format(updateInfo.totalBytes.toDouble() / (1024 * 1024))
            Text(
              text = "$downloadedMb MB / $totalMb MB",
              style = MaterialTheme.typography.labelSmall,
              color = TextMutedDark
            )
          }
        }

        UpdateEngineState.VERIFYING -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = GoldAmber
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Verifying package signatures & integrity...",
              style = MaterialTheme.typography.bodySmall,
              color = GoldAmber
            )
          }
        }

        UpdateEngineState.VERIFICATION_FAILED, UpdateEngineState.DOWNLOAD_FAILED, UpdateEngineState.INSTALL_FAILED -> {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF3B0C0C),
            border = BorderStroke(1.dp, SoftRed),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = SoftRed,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = updateInfo.errorMessage ?: strings.verificationFailed,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = SoftRed
              )
            }
          }
        }

        else -> {}
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Action Button
      if (updateInfo.state == UpdateEngineState.READY_TO_INSTALL) {
        Button(
          onClick = { onInstallClick(context) },
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Emerald500,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("forced_update_install_button")
        ) {
          Icon(
            imageVector = Icons.Default.InstallMobile,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = strings.installUpdate,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      } else if (updateInfo.state != UpdateEngineState.DOWNLOADING && updateInfo.state != UpdateEngineState.VERIFYING && updateInfo.state != UpdateEngineState.INSTALLING) {
        Button(
          onClick = onDownloadClick,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = SoftRed,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("forced_update_download_button")
        ) {
          Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (updateInfo.state == UpdateEngineState.VERIFICATION_FAILED || updateInfo.state == UpdateEngineState.DOWNLOAD_FAILED) {
              "Retry Update Download"
            } else {
              strings.downloadUpdate
            },
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
          )
        }
      }
    }
  }
}
