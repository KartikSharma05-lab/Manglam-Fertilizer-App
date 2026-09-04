package com.manglamfertilizer.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
  val route: String,
  val title: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
  object Billing : Screen("billing", "Bills", Icons.Filled.Receipt, Icons.Outlined.Receipt)
  object Inventory : Screen("inventory", "Inventory", Icons.Filled.Inventory2, Icons.Outlined.Inventory2)
  object Customers : Screen("customers", "Customers", Icons.Filled.People, Icons.Outlined.People)
  object Reports : Screen("reports", "Reports", Icons.Filled.Assessment, Icons.Outlined.Assessment)
  object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)

  // Secondary standalone quick action routes (not in bottom bar)
  object DailyAccounts : Screen("daily_accounts", "Daily Accounts", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
  object AIAdvisor : Screen("ai_advisor", "AI Advisor", Icons.Filled.Home, Icons.Outlined.Home)
  object VoiceAI : Screen("voice_ai", "Voice AI", Icons.Filled.Home, Icons.Outlined.Home)

  companion object {
    fun getBottomNavItems(): List<Screen> = listOf(
      Home,
      Billing,
      Inventory,
      Customers,
      Reports,
      Settings
    )
  }
}
