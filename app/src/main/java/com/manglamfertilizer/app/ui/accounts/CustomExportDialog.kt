package com.manglamfertilizer.app.ui.accounts

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.manglamfertilizer.app.data.accounting.AccountingPeriodMode
import com.manglamfertilizer.app.data.accounting.DailyAccountingLifecycleManager
import com.manglamfertilizer.app.data.accounting.DailyAccountsExporter
import com.manglamfertilizer.app.data.accounting.ExportFormat
import com.manglamfertilizer.app.data.model.Invoice
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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

/**
 * Custom Data Export Dialog for Daily Accounts & Reports.
 * 
 * Allows user to:
 * 1. Select Custom Date Range (Start Date & End Date or Presets: Today, Yesterday, This Month, All Time)
 * 2. Select Export Format (PDF, Excel .xlsx, CSV .csv)
 * 3. Review live dataset size & accounting totals before exporting
 * 4. Generate & share file asynchronously off main thread
 */
@Composable
fun CustomExportDialog(
  invoices: List<Invoice>,
  initialMode: AccountingPeriodMode = AccountingPeriodMode.THIS_MONTH,
  initialStartMillis: Long? = null,
  initialEndMillis: Long? = null,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val numberFormat = remember { DecimalFormat("#,##,###.##") }
  val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }

  var selectedMode by remember { mutableStateOf(initialMode) }
  var startMillis by remember { mutableStateOf(initialStartMillis) }
  var endMillis by remember { mutableStateOf(initialEndMillis) }
  var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
  var isExporting by remember { mutableStateOf(false) }

  // Fallback defaults for custom range if unset
  val calNow = Calendar.getInstance()
  val defaultStartMillis = remember {
    Calendar.getInstance().apply {
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
    }.timeInMillis
  }
  val defaultEndMillis = remember {
    Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
    }.timeInMillis
  }

  val effectiveStartMillis = startMillis ?: defaultStartMillis
  val effectiveEndMillis = endMillis ?: defaultEndMillis

  // Calculate matching invoices and aggregates
  val accountingSummary = remember(invoices, selectedMode, startMillis, endMillis) {
    DailyAccountingLifecycleManager.filterAndAggregate(
      invoices = invoices,
      mode = selectedMode,
      customStartMillis = if (selectedMode == AccountingPeriodMode.CUSTOM_RANGE || selectedMode == AccountingPeriodMode.CUSTOM_DATE) effectiveStartMillis else null,
      customEndMillis = if (selectedMode == AccountingPeriodMode.CUSTOM_RANGE) effectiveEndMillis else null,
      timeZone = TimeZone.getDefault()
    )
  }

  Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DarkSurface),
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 680.dp)
        .imePadding()
        .padding(4.dp)
        .testTag("custom_export_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(18.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // 1. Dialog Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Download,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = "Export Daily Accounts",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
              Text(
                text = "Custom date range & format selection",
                fontSize = 11.sp,
                color = TextSecondaryDark
              )
            }
          }

          if (!isExporting) {
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextMutedDark,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        HorizontalDivider(color = DarkBorder)

        // 2. Period Selection Section
        Text(
          text = "1. SELECT PERIOD OR DATE RANGE",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = Emerald400
        )

        // Preset Period Chips
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          PresetChip(
            label = "Today",
            isSelected = selectedMode == AccountingPeriodMode.TODAY_ACTIVE,
            onClick = {
              selectedMode = AccountingPeriodMode.TODAY_ACTIVE
              startMillis = null
              endMillis = null
            },
            modifier = Modifier.weight(1f)
          )
          PresetChip(
            label = "This Month",
            isSelected = selectedMode == AccountingPeriodMode.THIS_MONTH,
            onClick = {
              selectedMode = AccountingPeriodMode.THIS_MONTH
              startMillis = null
              endMillis = null
            },
            modifier = Modifier.weight(1f)
          )
          PresetChip(
            label = "Custom Range",
            isSelected = selectedMode == AccountingPeriodMode.CUSTOM_RANGE,
            onClick = {
              selectedMode = AccountingPeriodMode.CUSTOM_RANGE
              if (startMillis == null) startMillis = defaultStartMillis
              if (endMillis == null) endMillis = defaultEndMillis
            },
            modifier = Modifier.weight(1f)
          )
          PresetChip(
            label = "All Time",
            isSelected = selectedMode == AccountingPeriodMode.ALL_RECORDS,
            onClick = {
              selectedMode = AccountingPeriodMode.ALL_RECORDS
              startMillis = null
              endMillis = null
            },
            modifier = Modifier.weight(1f)
          )
        }

        // Custom Date Range Pickers (Start Date & End Date)
        if (selectedMode == AccountingPeriodMode.CUSTOM_RANGE) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Start Date Picker Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .clickable {
                    val calStart = Calendar.getInstance().apply { timeInMillis = effectiveStartMillis }
                    DatePickerDialog(
                      context,
                      { _, y, m, d ->
                        val newCal = Calendar.getInstance().apply {
                          set(Calendar.YEAR, y)
                          set(Calendar.MONTH, m)
                          set(Calendar.DAY_OF_MONTH, d)
                          set(Calendar.HOUR_OF_DAY, 0)
                          set(Calendar.MINUTE, 0)
                          set(Calendar.SECOND, 0)
                        }
                        startMillis = newCal.timeInMillis
                        if (effectiveEndMillis < newCal.timeInMillis) {
                          endMillis = newCal.apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                          }.timeInMillis
                        }
                      },
                      calStart.get(Calendar.YEAR),
                      calStart.get(Calendar.MONTH),
                      calStart.get(Calendar.DAY_OF_MONTH)
                    ).apply { setTitle("Select Start Date") }.show()
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("From Date:", fontSize = 12.sp, color = TextSecondaryDark)
                }
                Text(
                  text = dateFormat.format(Date(effectiveStartMillis)),
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimaryDark
                )
              }

              HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

              // End Date Picker Row
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(6.dp))
                  .clickable {
                    val calEnd = Calendar.getInstance().apply { timeInMillis = effectiveEndMillis }
                    DatePickerDialog(
                      context,
                      { _, y, m, d ->
                        val newCal = Calendar.getInstance().apply {
                          set(Calendar.YEAR, y)
                          set(Calendar.MONTH, m)
                          set(Calendar.DAY_OF_MONTH, d)
                          set(Calendar.HOUR_OF_DAY, 23)
                          set(Calendar.MINUTE, 59)
                          set(Calendar.SECOND, 59)
                        }
                        endMillis = newCal.timeInMillis
                      },
                      calEnd.get(Calendar.YEAR),
                      calEnd.get(Calendar.MONTH),
                      calEnd.get(Calendar.DAY_OF_MONTH)
                    ).apply { setTitle("Select End Date") }.show()
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.DateRange, contentDescription = null, tint = InfoSky, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("To Date:", fontSize = 12.sp, color = TextSecondaryDark)
                }
                Text(
                  text = dateFormat.format(Date(effectiveEndMillis)),
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimaryDark
                )
              }
            }
          }
        }

        // 3. Export Format Selection
        Text(
          text = "2. CHOOSE EXPORT FORMAT",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp,
          color = Emerald400
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FormatOptionCard(
            format = ExportFormat.PDF,
            icon = Icons.Default.Description,
            isSelected = selectedFormat == ExportFormat.PDF,
            onClick = { selectedFormat = ExportFormat.PDF },
            modifier = Modifier.weight(1f)
          )
          FormatOptionCard(
            format = ExportFormat.XLSX,
            icon = Icons.Default.TableChart,
            isSelected = selectedFormat == ExportFormat.XLSX,
            onClick = { selectedFormat = ExportFormat.XLSX },
            modifier = Modifier.weight(1f)
          )
          FormatOptionCard(
            format = ExportFormat.CSV,
            icon = Icons.Default.Description,
            isSelected = selectedFormat == ExportFormat.CSV,
            onClick = { selectedFormat = ExportFormat.CSV },
            modifier = Modifier.weight(1f)
          )
        }

        // 4. Data Summary & Verification Preview
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Records to Export:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondaryDark
              )
              Text(
                text = "${accountingSummary.invoiceCount} Bills (${accountingSummary.periodLabel})",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column {
                Text("Total Sales", fontSize = 10.sp, color = TextMutedDark)
                Text("₹${numberFormat.format(accountingSummary.totalSales)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
              }
              Column {
                Text("Cash", fontSize = 10.sp, color = TextMutedDark)
                Text("₹${numberFormat.format(accountingSummary.totalCash)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald400)
              }
              Column {
                Text("Online", fontSize = 10.sp, color = TextMutedDark)
                Text("₹${numberFormat.format(accountingSummary.totalOnline)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = InfoSky)
              }
              Column {
                Text("Due", fontSize = 10.sp, color = TextMutedDark)
                Text("₹${numberFormat.format(accountingSummary.totalDue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (accountingSummary.totalDue > 0) GoldAmber else Emerald400)
              }
            }
          }
        }

        // 5. Export Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            enabled = !isExporting,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Cancel", color = TextSecondaryDark)
          }

          Button(
            onClick = {
              if (accountingSummary.invoices.isEmpty()) {
                Toast.makeText(context, "No accounting records found in this range to export", Toast.LENGTH_SHORT).show()
                return@Button
              }

              isExporting = true
              coroutineScope.launch {
                try {
                  val result = DailyAccountsExporter.exportData(
                    context = context,
                    invoices = accountingSummary.invoices,
                    periodLabel = accountingSummary.periodLabel,
                    format = selectedFormat
                  )
                  DailyAccountsExporter.shareExportFile(
                    context = context,
                    exportResult = result,
                    periodLabel = accountingSummary.periodLabel
                  )
                  Toast.makeText(context, "Export generated successfully: ${result.file.name}", Toast.LENGTH_LONG).show()
                  onDismiss()
                } catch (e: Exception) {
                  e.printStackTrace()
                  Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                  isExporting = false
                }
              }
            },
            enabled = !isExporting,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkSurface),
            modifier = Modifier
              .weight(1.4f)
              .testTag("confirm_export_button")
          ) {
            if (isExporting) {
              CircularProgressIndicator(color = DarkSurface, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Exporting...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            } else {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export & Share", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PresetChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bgColor = if (isSelected) Emerald400 else DarkCard
  val contentColor = if (isSelected) Color(0xFF02231B) else TextSecondaryDark
  val borderColor = if (isSelected) Emerald400 else DarkBorder

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
  ) {
    Box(
      modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = contentColor,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun FormatOptionCard(
  format: ExportFormat,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor = if (isSelected) Emerald400 else DarkBorder
  val bgColor = if (isSelected) DarkSurfaceElevated else DarkCard

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = bgColor,
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .clickable { onClick() }
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) Emerald400 else TextMutedDark,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = format.name,
        fontSize = 12.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        color = if (isSelected) TextPrimaryDark else TextSecondaryDark
      )
      Text(
        text = ".${format.extension}",
        fontSize = 9.5.sp,
        color = if (isSelected) Emerald400 else TextMutedDark
      )
    }
  }
}
