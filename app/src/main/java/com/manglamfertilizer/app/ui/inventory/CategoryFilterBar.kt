package com.manglamfertilizer.app.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.CategoryItem
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun CategoryFilterBar(
  categories: List<CategoryItem>,
  products: List<Product> = emptyList(),
  selectedCategory: String?, // null means "All"
  onSelectCategory: (String?) -> Unit,
  modifier: Modifier = Modifier
) {
  LazyRow(
    modifier = modifier
      .fillMaxWidth()
      .testTag("category_filter_bar"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Permanent "All" chip with dynamic count
    item {
      val allCount = products.size
      val allLabel = if (allCount > 0) "All ($allCount)" else "All"
      CategoryChip(
        label = allLabel,
        isSelected = selectedCategory == null,
        onClick = { onSelectCategory(null) },
        testTag = "category_chip_all"
      )
    }

    // 2. Dynamic user-created categories (NO hardcoded categories, only real categories from DB)
    items(categories.distinctBy { it.id }, key = { it.id }) { cat ->
      val count = products.count { it.category.equals(cat.name, ignoreCase = true) }
      val label = if (count > 0) "${cat.name} ($count)" else cat.name
      CategoryChip(
        label = label,
        isSelected = selectedCategory.equals(cat.name, ignoreCase = true),
        onClick = {
          if (selectedCategory.equals(cat.name, ignoreCase = true)) {
            onSelectCategory(null)
          } else {
            onSelectCategory(cat.name)
          }
        },
        testTag = "category_chip_${cat.name.lowercase().replace(" ", "_")}"
      )
    }
  }
}

@Composable
private fun CategoryChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  val backgroundColor = if (isSelected) Emerald900.copy(alpha = 0.8f) else DarkCard
  val borderColor = if (isSelected) Emerald400 else DarkBorder
  val textColor = if (isSelected) Emerald400 else TextSecondaryDark

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = backgroundColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .height(32.dp)
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 14.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
          fontSize = 12.5.sp,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        ),
        color = textColor
      )
    }
  }
}
