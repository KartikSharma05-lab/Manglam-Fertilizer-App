package com.manglamfertilizer.app.ui.inventory

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

enum class ProductStatusFilter(val label: String) {
  ALL("All"),
  IN_STOCK("In Stock"),
  LOW_STOCK("Low Stock"),
  NEAR_EXPIRY("Near Expiry"),
  EXPIRED("Expired")
}

// Backward compatibility alias
typealias InventorySummaryFilter = ProductStatusFilter

@Composable
fun InventorySummaryCards(
  totalProductsCount: Int,
  inStockCount: Int,
  lowStockCount: Int,
  nearExpiryCount: Int,
  expiredCount: Int,
  selectedFilter: ProductStatusFilter,
  onSelectFilter: (ProductStatusFilter) -> Unit,
  modifier: Modifier = Modifier
) {
  val totalExpiryCount = nearExpiryCount + expiredCount
  val isExpiryFilterActive = selectedFilter == ProductStatusFilter.NEAR_EXPIRY || selectedFilter == ProductStatusFilter.EXPIRED

  // Compact, professional horizontal summary cards (PRODUCTS, LOW STOCK, EXPIRY)
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp)
      .testTag("inventory_summary_cards"),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // 1. Products Card
    CompactSummaryCard(
      title = "Products",
      count = totalProductsCount.toString(),
      isSelected = selectedFilter == ProductStatusFilter.ALL,
      accentColor = Emerald400,
      activeBgColor = Emerald900.copy(alpha = 0.6f),
      onClick = { onSelectFilter(ProductStatusFilter.ALL) },
      modifier = Modifier.weight(1f),
      testTag = "summary_card_products"
    )

    // 2. Low Stock Card
    CompactSummaryCard(
      title = "Low Stock",
      count = lowStockCount.toString(),
      isSelected = selectedFilter == ProductStatusFilter.LOW_STOCK,
      accentColor = if (lowStockCount > 0) SoftRed else Emerald400,
      activeBgColor = SoftRed.copy(alpha = 0.2f),
      onClick = {
        if (selectedFilter == ProductStatusFilter.LOW_STOCK) {
          onSelectFilter(ProductStatusFilter.ALL)
        } else {
          onSelectFilter(ProductStatusFilter.LOW_STOCK)
        }
      },
      modifier = Modifier.weight(1f),
      testTag = "summary_card_low_stock"
    )

    // 3. Expiry Card (Near Expiry + Expired combined)
    CompactSummaryCard(
      title = "Expiry",
      count = totalExpiryCount.toString(),
      isSelected = isExpiryFilterActive,
      accentColor = if (expiredCount > 0) SoftRed else if (nearExpiryCount > 0) GoldAmber else Emerald400,
      activeBgColor = if (expiredCount > 0) SoftRed.copy(alpha = 0.2f) else GoldAmber.copy(alpha = 0.2f),
      onClick = {
        if (isExpiryFilterActive) {
          onSelectFilter(ProductStatusFilter.ALL)
        } else {
          onSelectFilter(ProductStatusFilter.NEAR_EXPIRY)
        }
      },
      modifier = Modifier.weight(1f),
      testTag = "summary_card_expiry"
    )
  }
}

@Composable
private fun CompactSummaryCard(
  title: String,
  count: String,
  isSelected: Boolean,
  accentColor: Color,
  activeBgColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String
) {
  val bgColor = if (isSelected) activeBgColor else DarkCard
  val borderColor = if (isSelected) accentColor else DarkBorder

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .height(38.dp)
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
          fontSize = 11.5.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        ),
        color = if (isSelected) TextPrimaryDark else TextSecondaryDark
      )

      Text(
        text = count,
        style = MaterialTheme.typography.titleMedium.copy(
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        ),
        color = accentColor
      )
    }
  }
}
