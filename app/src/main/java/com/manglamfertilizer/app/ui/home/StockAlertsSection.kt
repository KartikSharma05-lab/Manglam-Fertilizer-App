package com.manglamfertilizer.app.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.AlertType
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import com.manglamfertilizer.app.ui.theme.WarningAmber

@Composable
fun StockAlertsSection(
  alerts: List<StockAlert>,
  onViewInventory: () -> Unit,
  onOpenAlertsPanel: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val strings = LocalStrings.current

  val nearExpiryCount = remember(alerts) {
    alerts.count { it.alertType == AlertType.NEAR_EXPIRY }
  }
  val lowStockCount = remember(alerts) {
    alerts.count { it.alertType == AlertType.LOW_STOCK || it.alertType == AlertType.OUT_OF_STOCK }
  }
  val expiredCount = remember(alerts) {
    alerts.count { it.alertType == AlertType.EXPIRED }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .testTag("stock_alerts_section")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = null,
          tint = if (alerts.isNotEmpty()) GoldAmber else TextSecondaryDark,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = strings.stockAlerts.uppercase(),
          style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
          ),
          color = TextSecondaryDark
        )
      }

      if (alerts.isNotEmpty()) {
        Text(
          text = "View All (${alerts.size})",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald400
          ),
          modifier = Modifier
            .clickable { onOpenAlertsPanel?.invoke() ?: onViewInventory() }
            .padding(vertical = 2.dp, horizontal = 4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (alerts.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("stock_alerts_empty_state")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = Emerald900,
            modifier = Modifier.size(34.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(18.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "0 Products",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 13.5.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryDark
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "• All good",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                ),
                color = Emerald400
              )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
              text = "No low stock or expiring batches found.",
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp
              ),
              color = TextMutedDark
            )
          }
        }
      }
    } else {
      // Alert Summary Capsule Banner
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .clickable { onOpenAlertsPanel?.invoke() ?: onViewInventory() }
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 7.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = buildString {
              val parts = mutableListOf<String>()
              if (nearExpiryCount > 0) parts.add("$nearExpiryCount near expiry")
              if (lowStockCount > 0) parts.add("$lowStockCount low stock")
              if (expiredCount > 0) parts.add("$expiredCount expired")
              append(parts.joinToString(" • "))
            },
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 11.5.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimaryDark
          )

          Text(
            text = "Details ›",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = GoldAmber
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Top Alert Cards
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        alerts.take(4).forEach { alert ->
          StockAlertCard(
            alert = alert,
            onClick = { onOpenAlertsPanel?.invoke() ?: onViewInventory() }
          )
        }
      }
    }
  }
}

private data class AlertCardStyle(
  val badgeText: String,
  val badgeBg: Color,
  val badgeTextColor: Color,
  val icon: ImageVector
)

@Composable
private fun StockAlertCard(
  alert: StockAlert,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isExpiry = alert.alertType == AlertType.NEAR_EXPIRY || alert.alertType == AlertType.EXPIRED

  val style = when (alert.alertType) {
    AlertType.EXPIRED -> AlertCardStyle(
      badgeText = "EXPIRED",
      badgeBg = Color(0xFF3B0C0C),
      badgeTextColor = SoftRed,
      icon = Icons.Default.EventBusy
    )
    AlertType.NEAR_EXPIRY -> {
      when (alert.expiryPriority) {
        ExpiryPriority.HIGH -> AlertCardStyle(
          badgeText = "HIGH PRIORITY (≤1 MO)",
          badgeBg = Color(0xFF3B0C0C),
          badgeTextColor = SoftRed,
          icon = Icons.Default.EventBusy
        )
        ExpiryPriority.MEDIUM -> AlertCardStyle(
          badgeText = "MEDIUM PRIORITY (≤3 MOS)",
          badgeBg = Color(0xFF3B2A06),
          badgeTextColor = GoldAmber,
          icon = Icons.Default.EventBusy
        )
        ExpiryPriority.NORMAL, null -> AlertCardStyle(
          badgeText = "EXPIRY ALERT (≤6 MOS)",
          badgeBg = Color(0xFF0C2B42),
          badgeTextColor = InfoSky,
          icon = Icons.Default.EventBusy
        )
      }
    }
    AlertType.OUT_OF_STOCK -> AlertCardStyle(
      badgeText = "OUT OF STOCK",
      badgeBg = Color(0xFF3B0C0C),
      badgeTextColor = SoftRed,
      icon = Icons.Default.Warning
    )
    AlertType.LOW_STOCK -> AlertCardStyle(
      badgeText = "LOW STOCK",
      badgeBg = Color(0xFF3B2A06),
      badgeTextColor = WarningAmber,
      icon = Icons.Default.Warning
    )
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("stock_alert_item_${alert.productId}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = style.badgeBg,
            modifier = Modifier.size(30.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.badgeTextColor,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          Column {
            Text(
              text = alert.productName,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = TextPrimaryDark,
              maxLines = 1
            )

            val subLine = buildString {
              if (alert.company.isNotBlank()) append(alert.company)
              if (alert.category.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(alert.category)
              }
              if (alert.rackLocation.isNotBlank()) {
                if (isNotEmpty()) append(" • Rack: ") else append("Rack: ")
                append(alert.rackLocation)
              }
            }
            if (subLine.isNotBlank()) {
              Text(
                text = subLine,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = TextSecondaryDark,
                maxLines = 1
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(4.dp),
          color = style.badgeBg,
          border = BorderStroke(1.dp, style.badgeTextColor.copy(alpha = 0.5f))
        ) {
          Text(
            text = style.badgeText,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = style.badgeTextColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Status text: Expiry details or Stock details
      val statusText = if (isExpiry) {
        alert.remainingTimeText.ifBlank { alert.statusMessage }
      } else {
        "Current: ${alert.currentStock.toInt()} ${alert.unit.name} • Min Alert: ${alert.minStock.toInt()} ${alert.unit.name}"
      }

      Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall.copy(
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium
        ),
        color = if (alert.alertType == AlertType.EXPIRED || alert.alertType == AlertType.OUT_OF_STOCK) SoftRed else GoldAmber,
        maxLines = 1
      )
    }
  }
}

