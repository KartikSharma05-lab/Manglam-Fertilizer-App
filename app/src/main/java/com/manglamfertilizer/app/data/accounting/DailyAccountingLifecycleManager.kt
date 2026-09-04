package com.manglamfertilizer.app.data.accounting

import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.PaymentMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Filter mode for Daily Accounts period selection.
 */
enum class AccountingPeriodMode {
  TODAY_ACTIVE,   // Current business day with 12:00 PM noon cutoff
  YESTERDAY,      // Previous business day / yesterday
  THIS_MONTH,     // Entire current calendar month
  CUSTOM_DATE,    // User selected specific date
  CUSTOM_RANGE,   // User selected date range (e.g. 01 Sep – 30 Sep)
  ALL_RECORDS     // Complete historical archive
}

data class DailyAccountsSummary(
  val totalSales: Double,
  val totalCash: Double,
  val totalOnline: Double,
  val totalDue: Double,
  val invoiceCount: Int,
  val invoices: List<Invoice>,
  val periodLabel: String
)

/**
 * Authoritative Business Day & Accounting Lifecycle Manager.
 * 
 * Rules:
 * 1. Business Day cutoff is 12:00 PM noon local time.
 * 2. Invoices are deduplicated by unique ID (idempotency guarantee).
 * 3. Historical records are NEVER deleted on daily rollover.
 * 4. Supports Today (Active), Yesterday, Monthly, Custom Date, and Date Range queries.
 */
object DailyAccountingLifecycleManager {

  /**
   * Calculates the exact start and end timestamps (in milliseconds) for a business day
   * with a 12:00 PM (noon) cutoff in the local timezone.
   * 
   * Example:
   * - At 09:00 AM on Sept 2: Active cycle started at Sept 1 12:00:00.000 PM and ends at Sept 2 11:59:59.999 AM.
   * - At 01:00 PM on Sept 2: Active cycle started at Sept 2 12:00:00.000 PM and ends at Sept 3 11:59:59.999 AM.
   */
  fun getBusinessDayRange(
    referenceMillis: Long = System.currentTimeMillis(),
    cutoffHour: Int = 12,
    timeZone: TimeZone = TimeZone.getDefault()
  ): Pair<Long, Long> {
    val cal = Calendar.getInstance(timeZone).apply {
      timeInMillis = referenceMillis
    }
    val currentHour = cal.get(Calendar.HOUR_OF_DAY)

    val startCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = referenceMillis
      if (currentHour < cutoffHour) {
        // Roll back to previous calendar day at cutoff hour
        add(Calendar.DAY_OF_MONTH, -1)
      }
      set(Calendar.HOUR_OF_DAY, cutoffHour)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val endCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = startCal.timeInMillis
      add(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MILLISECOND, -1)
    }

    return Pair(startCal.timeInMillis, endCal.timeInMillis)
  }

  /**
   * Calculates standard calendar day range (00:00:00.000 to 23:59:59.999).
   */
  fun getCalendarDayRange(
    dateMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault()
  ): Pair<Long, Long> {
    val startCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = dateMillis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = startCal.timeInMillis
      add(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MILLISECOND, -1)
    }
    return Pair(startCal.timeInMillis, endCal.timeInMillis)
  }

  /**
   * Calculates calendar month range from 1st of month 00:00:00.000 to last millisecond of month.
   */
  fun getMonthRange(
    referenceMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
  ): Pair<Long, Long> {
    val startCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = referenceMillis
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance(timeZone).apply {
      timeInMillis = startCal.timeInMillis
      add(Calendar.MONTH, 1)
      add(Calendar.MILLISECOND, -1)
    }
    return Pair(startCal.timeInMillis, endCal.timeInMillis)
  }

  /**
   * Filter and aggregate invoices for a given accounting period mode and optional custom date/range.
   */
  fun filterAndAggregate(
    invoices: List<Invoice>,
    mode: AccountingPeriodMode,
    customStartMillis: Long? = null,
    customEndMillis: Long? = null,
    searchQuery: String = "",
    timeZone: TimeZone = TimeZone.getDefault()
  ): DailyAccountsSummary {
    // 1. Deduplicate by unique invoice ID
    val deduplicatedInvoices = invoices.distinctBy { it.id }

    // 2. Determine time range boundaries
    val (rangeStart, rangeEnd, label) = when (mode) {
      AccountingPeriodMode.TODAY_ACTIVE -> {
        val (start, end) = getBusinessDayRange(System.currentTimeMillis(), cutoffHour = 12, timeZone = timeZone)
        val fmt = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
        Triple(start, end, "Today Active (${fmt.format(Date(start))} – ${fmt.format(Date(end))})")
      }
      AccountingPeriodMode.YESTERDAY -> {
        val nowCal = Calendar.getInstance(timeZone).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val (start, end) = getBusinessDayRange(nowCal.timeInMillis, cutoffHour = 12, timeZone = timeZone)
        val fmt = SimpleDateFormat("dd MMM", Locale.getDefault())
        Triple(start, end, "Yesterday (${fmt.format(Date(start))})")
      }
      AccountingPeriodMode.THIS_MONTH -> {
        val (start, end) = getMonthRange(System.currentTimeMillis(), timeZone = timeZone)
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        Triple(start, end, "Month of ${fmt.format(Date(start))}")
      }
      AccountingPeriodMode.CUSTOM_DATE -> {
        val targetMillis = customStartMillis ?: System.currentTimeMillis()
        val (start, end) = getCalendarDayRange(targetMillis, timeZone = timeZone)
        val fmt = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        Triple(start, end, "Date: ${fmt.format(Date(start))}")
      }
      AccountingPeriodMode.CUSTOM_RANGE -> {
        val start = customStartMillis ?: System.currentTimeMillis()
        val end = customEndMillis ?: (start + 86400000L)
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        Triple(start, end, "${fmt.format(Date(start))} – ${fmt.format(Date(end))}")
      }
      AccountingPeriodMode.ALL_RECORDS -> {
        Triple(0L, Long.MAX_VALUE, "All Historical Records")
      }
    }

    // 3. Filter invoices strictly within the target range
    val rangeInvoices = if (mode == AccountingPeriodMode.ALL_RECORDS) {
      deduplicatedInvoices
    } else {
      deduplicatedInvoices.filter { it.timestamp in rangeStart..rangeEnd }
    }

    // 4. Apply text search query across farmer name, product name, bill number, phone, village
    val query = searchQuery.trim().lowercase()
    val filteredInvoices = if (query.isBlank()) {
      rangeInvoices
    } else {
      rangeInvoices.filter { inv ->
        inv.customerName.lowercase().contains(query) ||
            inv.invoiceNumber.lowercase().contains(query) ||
            inv.customerPhone.contains(query) ||
            inv.customerVillage.lowercase().contains(query) ||
            inv.items.any { item -> item.productName.lowercase().contains(query) }
      }
    }.sortedByDescending { it.timestamp }

    // 5. Calculate authoritative accounting sums
    val totalSales = filteredInvoices.sumOf { it.grandTotal }
    val totalCash = filteredInvoices.sumOf { inv ->
      if (inv.paymentMode == PaymentMode.CASH) inv.amountPaid else 0.0
    }
    val totalOnline = filteredInvoices.sumOf { inv ->
      if (inv.paymentMode != PaymentMode.CASH && inv.paymentMode != PaymentMode.CREDIT) {
        inv.amountPaid
      } else 0.0
    }
    val totalDue = filteredInvoices.sumOf { inv ->
      inv.remainingDue.coerceAtLeast(0.0)
    }

    return DailyAccountsSummary(
      totalSales = totalSales,
      totalCash = totalCash,
      totalOnline = totalOnline,
      totalDue = totalDue,
      invoiceCount = filteredInvoices.size,
      invoices = filteredInvoices,
      periodLabel = label
    )
  }
}
