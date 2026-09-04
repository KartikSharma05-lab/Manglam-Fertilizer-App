package com.manglamfertilizer.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.CloudSyncState
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark

@Composable
fun HomeHeader(
  cloudSyncState: CloudSyncState = CloudSyncState.CONNECTED_SYNCED,
  syncStatusText: String = "Synced",
  unreadAlertsCount: Int = 0,
  onNotificationsClick: () -> Unit,
  onCloudClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("home_header"),
    color = DarkSurface,
    tonalElevation = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Logo + MANGALAM FERTILIZER + Inventory & Billing
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Manglam leaf logo container
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Emerald900,
            modifier = Modifier
              .size(36.dp)
              .border(1.dp, Emerald400, RoundedCornerShape(10.dp))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = "Manglam Logo",
                tint = Emerald400,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column(
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "MANGALAM FERTILIZER",
              style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
              ),
              color = TextPrimaryDark
            )

            Spacer(modifier = Modifier.height(1.dp))

            Text(
              text = "Inventory & Billing",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
              ),
              color = GoldAmber
            )
          }
        }

        // Right: Cloud Sync Status Icon + Notification Bell
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Cloud Status Indicator (Icon only - No 'Online' text)
          val (cloudIcon, cloudTint, cloudBg) = when (cloudSyncState) {
            CloudSyncState.CONNECTED_SYNCED -> Triple(Icons.Default.CloudDone, Emerald400, Emerald900)
            CloudSyncState.SYNCING_OR_WEAK -> Triple(Icons.Default.CloudSync, GoldAmber, Color(0xFF3B2A06))
            CloudSyncState.OFFLINE_OR_ERROR -> Triple(Icons.Default.CloudOff, SoftRed, Color(0xFF3B0C0C))
          }

          Surface(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .then(
                if (onCloudClick != null) Modifier.clickable { onCloudClick() } else Modifier
              )
              .testTag("cloud_sync_indicator"),
            color = DarkSurfaceElevated,
            shape = CircleShape,
            border = BorderStroke(1.dp, DarkBorder)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = cloudIcon,
                contentDescription = "Cloud Status: $syncStatusText",
                tint = cloudTint,
                modifier = Modifier.size(19.dp)
              )
            }
          }

          // Notification Bell
          Surface(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .clickable { onNotificationsClick() }
              .testTag("notifications_button"),
            color = DarkSurfaceElevated,
            shape = CircleShape,
            border = BorderStroke(1.dp, DarkBorder)
          ) {
            Box(contentAlignment = Alignment.Center) {
              BadgedBox(
                badge = {
                  if (unreadAlertsCount > 0) {
                    Badge(
                      containerColor = SoftRed,
                      contentColor = TextPrimaryDark,
                      modifier = Modifier.testTag("notification_badge")
                    ) {
                      Text(
                        text = if (unreadAlertsCount > 9) "9+" else unreadAlertsCount.toString(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                  }
                }
              ) {
                Icon(
                  imageVector = Icons.Default.Notifications,
                  contentDescription = "Notifications",
                  tint = TextPrimaryDark,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
