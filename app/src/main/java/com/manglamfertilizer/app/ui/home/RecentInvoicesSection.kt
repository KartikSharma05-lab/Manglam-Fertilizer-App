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
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Icon
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
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentInvoicesSection(
  invoices: List<Invoice>,
  onViewAllInvoices: () -> Unit,
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
      .testTag("recent_invoices_section")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = strings.recentInvoices.uppercase(),
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.1.sp
        ),
        color = TextSecondaryDark
      )

      if (invoices.isNotEmpty()) {
        Text(
          text = strings.viewAll,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Emerald400
          ),
          modifier = Modifier
            .clickable { onViewAllInvoices() }
            .padding(vertical = 2.dp, horizontal = 4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (invoices.isEmpty()) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("recent_invoices_empty_state")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Surface(
            shape = CircleShape,
            color = DarkSurfaceElevated,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(20.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = strings.noBillsFound,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimaryDark
          )

          Spacer(modifier = Modifier.height(1.dp))

          Text(
            text = "Today's created bills will appear here",
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 11.5.sp
            ),
            color = TextSecondaryDark
          )
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        invoices.take(5).forEach { invoice ->
          InvoiceRowItem(
            invoice = invoice,
            currencyFormat = currencyFormat,
            onClick = onViewAllInvoices
          )
        }
      }
    }
  }
}

@Composable
private fun InvoiceRowItem(
  invoice: Invoice,
  currencyFormat: NumberFormat,
  onClick: () -> Unit
) {
  val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .testTag("invoice_row_${invoice.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Surface(
          shape = CircleShape,
          color = Emerald900,
          modifier = Modifier.size(34.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Receipt,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(17.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = invoice.customerName,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 13.5.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimaryDark,
            maxLines = 1
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = invoice.invoiceNumber,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp
              ),
              color = TextMutedDark
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "• $timeStr",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp
              ),
              color = TextMutedDark
            )
          }
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = currencyFormat.format(invoice.grandTotal),
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
          ),
          color = Emerald400
        )

        val modeLabel = when (invoice.paymentMode) {
          PaymentMode.CASH -> "Cash"
          PaymentMode.UPI -> "UPI"
          PaymentMode.CARD -> "Card"
          PaymentMode.CHEQUE -> "Cheque"
          PaymentMode.CREDIT -> "Due"
          PaymentMode.SPLIT -> "Split"
          PaymentMode.OTHER -> "Other"
        }

        Text(
          text = modeLabel,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp
          ),
          color = if (invoice.remainingDue > 0) GoldAmber else TextSecondaryDark
        )
      }
    }
  }
}
