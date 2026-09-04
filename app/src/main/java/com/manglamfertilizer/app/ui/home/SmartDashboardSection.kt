package com.manglamfertilizer.app.ui.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.DashboardMetrics
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
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SmartDashboardSection(
  metrics: DashboardMetrics,
  modifier: Modifier = Modifier
) {
  val strings = LocalStrings.current
  val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 0
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .testTag("smart_dashboard_section")
  ) {
    Text(
      text = strings.businessSummary.uppercase(),
      style = MaterialTheme.typography.titleSmall.copy(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp
      ),
      color = TextSecondaryDark
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Row 1: Today's Sales & Customer Dues
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val hasSales = metrics.todaySales > 0
      DashboardMetricCard(
        title = strings.todaySales,
        value = currencyFormat.format(metrics.todaySales),
        supportingText = if (hasSales) "Revenue" else "No sales yet",
        icon = Icons.Default.CurrencyRupee,
        iconTint = Emerald400,
        containerTint = Emerald900,
        modifier = Modifier
          .weight(1f)
          .testTag("metric_today_sales")
      )

      val hasDues = metrics.totalCustomerDues > 0
      DashboardMetricCard(
        title = strings.customerDues,
        value = currencyFormat.format(metrics.totalCustomerDues),
        supportingText = if (hasDues) "Pending balance" else "Clear",
        icon = Icons.Default.AccountBalanceWallet,
        iconTint = GoldAmber,
        containerTint = Color(0xFF3B2A06),
        modifier = Modifier
          .weight(1f)
          .testTag("metric_customer_dues")
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Row 2: Low Stock Items & Near Expiry Items
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      val lowStockCount = metrics.lowStockCount
      DashboardMetricCard(
        title = strings.lowStock,
        value = "$lowStockCount Products",
        supportingText = if (lowStockCount == 0) "All good" else "Reorder needed",
        icon = Icons.Default.Warning,
        iconTint = if (lowStockCount > 0) WarningAmber else TextSecondaryDark,
        containerTint = if (lowStockCount > 0) Color(0xFF3B2A06) else DarkCard,
        modifier = Modifier
          .weight(1f)
          .testTag("metric_low_stock_count")
      )

      val nearExpiryCount = metrics.nearExpiryCount
      DashboardMetricCard(
        title = strings.nearExpiry,
        value = "$nearExpiryCount Products",
        supportingText = if (nearExpiryCount == 0) "All good" else "Expiring soon",
        icon = Icons.Default.EventBusy,
        iconTint = if (nearExpiryCount > 0) SoftRed else TextSecondaryDark,
        containerTint = if (nearExpiryCount > 0) Color(0xFF3B0C0C) else DarkCard,
        modifier = Modifier
          .weight(1f)
          .testTag("metric_expiring_soon_count")
      )
    }
  }
}

@Composable
private fun DashboardMetricCard(
  title: String,
  value: String,
  supportingText: String,
  icon: ImageVector,
  iconTint: Color,
  containerTint: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
      // Header: Title + Icon
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
          ),
          color = TextSecondaryDark,
          maxLines = 1,
          modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(4.dp))

        Surface(
          shape = CircleShape,
          color = containerTint,
          modifier = Modifier.size(28.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(15.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      // Main Value
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(
          fontSize = 16.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.3).sp
        ),
        color = TextPrimaryDark,
        maxLines = 1
      )

      Spacer(modifier = Modifier.height(2.dp))

      // Supporting Status Text
      Text(
        text = supportingText,
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.sp,
          fontWeight = FontWeight.Normal
        ),
        color = TextMutedDark,
        maxLines = 1
      )
    }
  }
}
