package com.manglamfertilizer.app.ui.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.util.DuplicateCustomerMatch
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DuplicateCustomerWarningDialog(
  enteredName: String,
  enteredPhone: String,
  enteredVillage: String,
  enteredAddress: String,
  match: DuplicateCustomerMatch,
  onUseExisting: (Customer) -> Unit,
  onUpdateAndUse: (Customer) -> Unit,
  onAddAsNew: () -> Unit,
  onDismiss: () -> Unit
) {
  val existing = match.existingCustomer
  val currencyFormat = remember {
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.8f)),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 640.dp)
        .imePadding()
        .padding(8.dp)
        .testTag("duplicate_customer_warning_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
              shape = CircleShape,
              color = GoldAmber.copy(alpha = 0.15f),
              modifier = Modifier.size(32.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = null,
                  tint = GoldAmber,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Possible Duplicate Farmer",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
              Text(
                text = match.matchReason,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldAmber
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Dismiss",
              tint = TextMutedDark,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        HorizontalDivider(color = DarkBorder, thickness = 1.dp)

        // Comparison Section
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Existing Record Card
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f)),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(Emerald400, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "EXISTING IN SYSTEM",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Emerald400
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = existing.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
              if (existing.phoneNumber.isNotBlank()) {
                Text(
                  text = "📞 ${existing.phoneNumber}",
                  fontSize = 11.sp,
                  color = TextSecondaryDark
                )
              }
              if (existing.village.isNotBlank()) {
                Text(
                  text = "📍 ${existing.village}",
                  fontSize = 11.sp,
                  color = TextMutedDark
                )
              }
              Spacer(modifier = Modifier.height(6.dp))
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(6.dp)) {
                  Text(
                    text = "Current Due: ${currencyFormat.format(existing.totalDue)}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (existing.totalDue > 0) GoldAmber else Emerald400
                  )
                  Text(
                    text = "Total Sales: ${currencyFormat.format(existing.totalPurchases)}",
                    fontSize = 10.sp,
                    color = TextMutedDark
                  )
                }
              }
            }
          }

          // Entered Data Card
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, InfoSky.copy(alpha = 0.5f)),
            modifier = Modifier.weight(1f)
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .background(InfoSky, CircleShape)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "ENTERED JUST NOW",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = InfoSky
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = enteredName.ifBlank { "—" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
              if (enteredPhone.isNotBlank()) {
                Text(
                  text = "📞 $enteredPhone",
                  fontSize = 11.sp,
                  color = TextSecondaryDark
                )
              }
              if (enteredVillage.isNotBlank()) {
                Text(
                  text = "📍 $enteredVillage",
                  fontSize = 11.sp,
                  color = TextMutedDark
                )
              }
              if (enteredAddress.isNotBlank()) {
                Text(
                  text = "🏠 $enteredAddress",
                  fontSize = 10.5.sp,
                  color = TextMutedDark
                )
              }
            }
          }
        }

        Text(
          text = "To avoid duplicate records and fragmented outstanding dues, choose how you want to proceed:",
          fontSize = 11.sp,
          color = TextSecondaryDark
        )

        // Action 1: Use Existing Farmer (Recommended)
        Button(
          onClick = { onUseExisting(existing) },
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("use_existing_customer_button")
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Use Existing Farmer (Recommended)",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
          )
        }

        // Action 2: Update Existing Profile & Use
        OutlinedButton(
          onClick = {
            val updatedCust = existing.copy(
              phoneNumber = enteredPhone.trim().ifBlank { existing.phoneNumber },
              village = enteredVillage.trim().ifBlank { existing.village },
              address = enteredAddress.trim().ifBlank { existing.address }
            )
            onUpdateAndUse(updatedCust)
          },
          border = BorderStroke(1.dp, Emerald400),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("update_existing_customer_button")
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Update Existing & Use",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Emerald400
          )
        }

        // Action 3: Add as New (Separate Person)
        OutlinedButton(
          onClick = onAddAsNew,
          border = BorderStroke(1.dp, DarkBorder),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("add_as_new_customer_button")
        ) {
          Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TextSecondaryDark, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Add As Separate Farmer",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondaryDark
          )
        }

        // Action 4: Cancel / Edit
        TextButton(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Cancel & Edit Details",
            fontSize = 11.5.sp,
            color = TextMutedDark
          )
        }
      }
    }
  }
}
