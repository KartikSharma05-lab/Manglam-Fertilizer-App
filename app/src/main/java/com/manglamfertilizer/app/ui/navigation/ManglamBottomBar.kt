package com.manglamfertilizer.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun ManglamBottomBar(
  currentRoute: String,
  onNavigate: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val strings = LocalStrings.current
  val navItems = remember { Screen.getBottomNavItems() }
  val topBorderColor = DarkBorder

  NavigationBar(
    modifier = modifier
      .fillMaxWidth()
      .drawBehind {
        // Subtle top border line
        drawLine(
          color = topBorderColor,
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1.dp.toPx()
        )
      }
      .testTag("bottom_nav_bar"),
    containerColor = DarkSurface,
    tonalElevation = 8.dp,
    windowInsets = NavigationBarDefaults.windowInsets
  ) {
    navItems.forEach { screen ->
      val isSelected = currentRoute == screen.route
      val localizedTitle = when (screen) {
        Screen.Home -> strings.navHome
        Screen.Billing -> strings.navBilling
        Screen.Inventory -> strings.navInventory
        Screen.Customers -> strings.navCustomers
        Screen.Reports -> strings.navReports
        Screen.Settings -> strings.navSettings
        else -> screen.title
      }

      NavigationBarItem(
        icon = {
          Icon(
            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
            contentDescription = localizedTitle,
            modifier = Modifier.size(22.dp)
          )
        },
        label = {
          Text(
            text = localizedTitle,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
          )
        },
        selected = isSelected,
        onClick = { onNavigate(screen.route) },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = Emerald400,
          selectedTextColor = Emerald400,
          indicatorColor = Emerald900,
          unselectedIconColor = TextSecondaryDark,
          unselectedTextColor = TextSecondaryDark
        ),
        modifier = Modifier.testTag("nav_tab_${screen.route}")
      )
    }
  }
}
