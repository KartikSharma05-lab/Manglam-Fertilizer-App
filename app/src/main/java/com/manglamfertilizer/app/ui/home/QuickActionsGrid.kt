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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.PurpleAccent
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

data class QuickActionItem(
  val id: String,
  val title: String,
  val icon: ImageVector,
  val tint: Color,
  val containerColor: Color
)

@Composable
fun QuickActionsGrid(
  onActionClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val strings = LocalStrings.current

  val actions = listOf(
    QuickActionItem("billing", strings.navBilling, Icons.Default.Receipt, Emerald400, Emerald900),
    QuickActionItem("inventory", strings.navInventory, Icons.Default.Inventory2, GoldAmber, Color(0xFF3B2A06)),
    QuickActionItem("customers", strings.navCustomers, Icons.Default.People, InfoSky, Color(0xFF0C2B42)),
    QuickActionItem("barcode", strings.barcode, Icons.Default.QrCodeScanner, Emerald400, Emerald900),
    QuickActionItem("ai_advisor", strings.krishiAdvisor, Icons.Default.AutoAwesome, PurpleAccent, Color(0xFF2E1065)),
    QuickActionItem("voice_ai", strings.voiceAssistant, Icons.Default.Mic, Color(0xFF22D3EE), Color(0xFF083344)),
    QuickActionItem("daily_accounts", strings.dailyAccounts, Icons.Default.AccountBalanceWallet, Color(0xFF34D399), Color(0xFF064E3B)),
    QuickActionItem("alerts", strings.stockAlerts, Icons.Default.NotificationsActive, SoftRed, Color(0xFF3B0C0C))
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .testTag("quick_actions_grid")
  ) {
    Text(
      text = strings.quickActions.uppercase(),
      style = MaterialTheme.typography.titleSmall.copy(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp
      ),
      color = TextSecondaryDark
    )

    Spacer(modifier = Modifier.height(8.dp))

    // 4 Columns × 2 Rows layout
    actions.chunked(4).forEachIndexed { index, rowItems ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = if (index == 0) 8.dp else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rowItems.forEach { item ->
          QuickActionButton(
            item = item,
            onClick = { onActionClick(item.id) },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

@Composable
private fun QuickActionButton(
  item: QuickActionItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("quick_action_${item.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 10.dp, horizontal = 4.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Surface(
        shape = CircleShape,
        color = item.containerColor,
        modifier = Modifier.size(36.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = item.tint,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = item.title,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 11.sp
        ),
        color = TextPrimaryDark,
        textAlign = TextAlign.Center,
        maxLines = 1
      )
    }
  }
}
