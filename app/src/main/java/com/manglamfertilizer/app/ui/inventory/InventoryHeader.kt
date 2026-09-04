package com.manglamfertilizer.app.ui.inventory

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun InventoryHeader(
  onOpenHistory: () -> Unit,
  onOpenScanner: () -> Unit,
  onOpenExport: () -> Unit,
  onOpenConfig: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = DarkSurface,
    modifier = modifier
      .fillMaxWidth()
      .testTag("inventory_header")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
      // 2-Line Header Title: "Inventory" / "Management" (Crisp, modern, naturally positioned)
      Column {
        Text(
          text = "Inventory",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            lineHeight = 24.sp
          ),
          color = TextPrimaryDark
        )
        Text(
          text = "Management",
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            lineHeight = 20.sp
          ),
          color = Emerald400
        )
      }

      // Compact Action Icons (History, Scanner, Export, Config)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HeaderActionButton(
          icon = Icons.Default.History,
          contentDescription = "Inventory History",
          testTag = "inventory_history_btn",
          onClick = onOpenHistory
        )

        HeaderActionButton(
          icon = Icons.Default.QrCodeScanner,
          contentDescription = "Dual Product Scanner",
          testTag = "inventory_scanner_btn",
          onClick = onOpenScanner
        )

        HeaderActionButton(
          icon = Icons.Default.FileDownload,
          contentDescription = "Download / Export",
          testTag = "inventory_export_btn",
          onClick = onOpenExport
        )

        HeaderActionButton(
          icon = Icons.Default.Tune,
          contentDescription = "Inventory Configuration",
          testTag = "inventory_config_btn",
          onClick = onOpenConfig
        )
      }
    }
    }
  }
}

@Composable
private fun HeaderActionButton(
  icon: ImageVector,
  contentDescription: String,
  testTag: String,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .size(36.dp)
      .testTag(testTag)
  ) {
    IconButton(
      onClick = onClick,
      modifier = Modifier.size(36.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = TextSecondaryDark,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
