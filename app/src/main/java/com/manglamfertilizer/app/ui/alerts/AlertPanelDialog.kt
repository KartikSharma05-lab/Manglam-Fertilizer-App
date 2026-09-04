package com.manglamfertilizer.app.ui.alerts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.AlertType
import com.manglamfertilizer.app.data.model.ExpiryPriority
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.data.util.AlertEngine
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.GoldAmberLight
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import com.manglamfertilizer.app.ui.theme.WarningAmber

/**
 * Model representing a compact, prioritized alert card.
 */
data class AlertCardDisplayModel(
  val productId: String,
  val productName: String,
  val product: Product?,
  val nameColor: Color,
  val showStar: Boolean,
  val alertIcon: ImageVector,
  val iconTint: Color,
  val iconBgColor: Color,
  val reasonText: String,
  val reasonColor: Color,
  val priorityRank: Int
)

@Composable
fun AlertPanelDialog(
  alerts: List<StockAlert>,
  products: List<Product>,
  onDismiss: () -> Unit,
  onSelectProduct: (Product) -> Unit,
  onNavigateToInventory: () -> Unit,
  modifier: Modifier = Modifier
) {
  // Resolve compact prioritized display models for all active alerts
  val alertCards = remember(alerts, products) {
    val productMap = products.associateBy { it.id }
    val grouped = alerts.groupBy { it.productId }
    grouped.map { (prodId, prodAlerts) ->
      val prod = productMap[prodId]
      val prodName = prod?.name ?: prodAlerts.firstOrNull()?.productName ?: "Unknown Product"
      resolveAlertCardDisplay(
        productId = prodId,
        productName = prodName,
        product = prod,
        alerts = prodAlerts
      )
    }.sortedWith(
      compareBy<AlertCardDisplayModel> { it.priorityRank }
        .thenBy { it.productName.lowercase() }
    )
  }

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
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
          .fillMaxWidth()
          .fillMaxHeight(0.92f)
          .testTag("alert_panel_dialog")
      ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // 1. DIALOG HEADER (Alerts & Notifications + Active Count + Close Button)
        Surface(
          color = DarkSurfaceElevated,
          border = BorderStroke(0.dp, Color.Transparent),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = Color(0xFF3B0C0C),
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = SoftRed,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }

              Column {
                Text(
                  text = "Alerts & Notifications",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = TextPrimaryDark
                )
                Text(
                  text = "${alerts.size} active alert(s) requiring attention",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                  color = TextSecondaryDark
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier.testTag("alert_panel_close_btn")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondaryDark
              )
            }
          }
        }

        // Header separator
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DarkBorder)
        )

        // 2. COMPACT ALERTS LIST
        if (alertCards.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(42.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "No Active Alerts",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = TextPrimaryDark
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "All products are in stock and within normal shelf life.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMutedDark
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)
          ) {
            items(alertCards, key = { it.productId }) { item ->
              CompactAlertCard(
                item = item,
                onClick = {
                  val targetProd = item.product ?: products.find { it.id == item.productId }
                  if (targetProd != null) {
                    onSelectProduct(targetProd)
                  } else {
                    onNavigateToInventory()
                  }
                }
              )
            }
          }
        }
      }
    }
  }
}
}

/**
 * Compact alert card that shows ONLY what product and why it is alerted.
 */
@Composable
private fun CompactAlertCard(
  item: AlertCardDisplayModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
      .testTag("alert_card_${item.productId}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Alert Status Icon Badge
      Surface(
        shape = CircleShape,
        color = item.iconBgColor,
        modifier = Modifier.size(34.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = item.alertIcon,
            contentDescription = null,
            tint = item.iconTint,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      // Product Name & Alert Reason
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Center
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (item.showStar) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Expired warning",
              tint = GoldAmberLight,
              modifier = Modifier.size(15.dp)
            )
          }
          Text(
            text = item.productName,
            style = MaterialTheme.typography.titleSmall.copy(
              fontSize = 13.5.sp,
              fontWeight = FontWeight.Bold,
              lineHeight = 17.sp
            ),
            color = item.nameColor,
            modifier = Modifier
              .weight(1f, fill = false)
              .testTag("alert_product_name_${item.productId}")
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = item.reasonText,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
          ),
          color = item.reasonColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      // Small subtle chevron indicator
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
        contentDescription = "View Details",
        tint = TextMutedDark.copy(alpha = 0.4f),
        modifier = Modifier.size(12.dp)
      )
    }
  }
}

/**
 * Evaluates priority and styling for a product's combined alert conditions:
 * Priority Order:
 * 1. EXPIRED -> RED + ⭐ STAR
 * 2. EXPIRY WITHIN 1 MONTH -> RED
 * 3. EXPIRY WITHIN 3 MONTHS -> YELLOW
 * 4. EXPIRY WITHIN 6 MONTHS -> YELLOW
 * 5. LOW STOCK / OUT OF STOCK -> WHITE
 */
fun resolveAlertCardDisplay(
  productId: String,
  productName: String,
  product: Product?,
  alerts: List<StockAlert>
): AlertCardDisplayModel {
  val startOfToday = AlertEngine.getStartOfToday()

  // Evaluate conditions
  val isExpired = alerts.any { it.alertType == AlertType.EXPIRED } ||
      (product?.expiryDate?.let { it > 0 && it < startOfToday } == true)

  val expiryEval = product?.expiryDate?.let { AlertEngine.evaluateExpiry(it) }

  val is1MonthExpiry = !isExpired && (
      alerts.any { it.alertType == AlertType.NEAR_EXPIRY && it.expiryPriority == ExpiryPriority.HIGH } ||
          (expiryEval?.isNearExpiry == true && expiryEval.priority == ExpiryPriority.HIGH)
      )

  val is3MonthsExpiry = !isExpired && !is1MonthExpiry && (
      alerts.any { it.alertType == AlertType.NEAR_EXPIRY && it.expiryPriority == ExpiryPriority.MEDIUM } ||
          (expiryEval?.isNearExpiry == true && expiryEval.priority == ExpiryPriority.MEDIUM)
      )

  val is6MonthsExpiry = !isExpired && !is1MonthExpiry && !is3MonthsExpiry && (
      alerts.any { it.alertType == AlertType.NEAR_EXPIRY && (it.expiryPriority == ExpiryPriority.NORMAL || it.expiryPriority == null) } ||
          (expiryEval?.isNearExpiry == true && (expiryEval.priority == ExpiryPriority.NORMAL || expiryEval.priority == null))
      )

  val isOutOfStock = alerts.any { it.alertType == AlertType.OUT_OF_STOCK } ||
      (product?.stockQuantity?.let { it <= 0 } == true)

  val isLowStock = alerts.any { it.alertType == AlertType.LOW_STOCK } || isOutOfStock ||
      (product?.let { it.stockQuantity <= it.minStockAlert } == true)

  return when {
    // 1. EXPIRED -> RED + ⭐ STAR
    isExpired -> {
      val reason = if (isOutOfStock) {
        "Expired • Out of Stock"
      } else if (isLowStock) {
        "Expired • Low Stock"
      } else {
        "Expired"
      }
      AlertCardDisplayModel(
        productId = productId,
        productName = productName,
        product = product,
        nameColor = SoftRed,
        showStar = true,
        alertIcon = Icons.Default.EventBusy,
        iconTint = SoftRed,
        iconBgColor = Color(0xFF3B0C0C),
        reasonText = reason,
        reasonColor = SoftRed,
        priorityRank = 1
      )
    }

    // 2. EXPIRY WITHIN 1 MONTH -> RED
    is1MonthExpiry -> {
      val reason = if (isOutOfStock) {
        "Expiring within 1 month • Out of Stock"
      } else if (isLowStock) {
        "Expiring within 1 month • Low Stock"
      } else {
        "Expiring within 1 month"
      }
      AlertCardDisplayModel(
        productId = productId,
        productName = productName,
        product = product,
        nameColor = SoftRed,
        showStar = false,
        alertIcon = Icons.Default.Warning,
        iconTint = SoftRed,
        iconBgColor = Color(0xFF3B0C0C),
        reasonText = reason,
        reasonColor = SoftRed,
        priorityRank = 2
      )
    }

    // 3. EXPIRY WITHIN 3 MONTHS -> YELLOW
    is3MonthsExpiry -> {
      val reason = if (isOutOfStock) {
        "Near Expiry — ≤ 3 months • Out of Stock"
      } else if (isLowStock) {
        "Near Expiry — ≤ 3 months • Low Stock"
      } else {
        "Near Expiry — ≤ 3 months"
      }
      AlertCardDisplayModel(
        productId = productId,
        productName = productName,
        product = product,
        nameColor = GoldAmberLight,
        showStar = false,
        alertIcon = Icons.Default.EventBusy,
        iconTint = GoldAmberLight,
        iconBgColor = Color(0xFF3B2A06),
        reasonText = reason,
        reasonColor = GoldAmberLight,
        priorityRank = 3
      )
    }

    // 4. EXPIRY WITHIN 6 MONTHS -> YELLOW
    is6MonthsExpiry -> {
      val reason = if (isOutOfStock) {
        "Near Expiry — ≤ 6 months • Out of Stock"
      } else if (isLowStock) {
        "Near Expiry — ≤ 6 months • Low Stock"
      } else {
        "Near Expiry — ≤ 6 months"
      }
      AlertCardDisplayModel(
        productId = productId,
        productName = productName,
        product = product,
        nameColor = GoldAmberLight,
        showStar = false,
        alertIcon = Icons.Default.EventBusy,
        iconTint = GoldAmberLight,
        iconBgColor = Color(0xFF3B2A06),
        reasonText = reason,
        reasonColor = GoldAmberLight,
        priorityRank = 4
      )
    }

    // 5. OUT OF STOCK -> WHITE
    isOutOfStock -> {
      AlertCardDisplayModel(
        productId = productId,
        productName = productName,
        product = product,
        nameColor = Color.White,
        showStar = false,
        alertIcon = Icons.Default.Warning,
        iconTint = SoftRed,
        iconBgColor = Color(0xFF3B0C0C),
        reasonText = "Out of Stock",
        reasonColor = SoftRed,
        priorityRank = 5
      )
    }

    // 6. LOW STOCK -> WHITE
    else -> {
      AlertCardDisplayModel(
        productId = productId,
        productName = productName,
        product = product,
        nameColor = Color.White,
        showStar = false,
        alertIcon = Icons.Default.Warning,
        iconTint = WarningAmber,
        iconBgColor = Color(0xFF3B2A06),
        reasonText = "Low Stock",
        reasonColor = WarningAmber,
        priorityRank = 6
      )
    }
  }
}

