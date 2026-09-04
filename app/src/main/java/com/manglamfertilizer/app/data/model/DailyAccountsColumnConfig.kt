package com.manglamfertilizer.app.data.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration model for customizable table columns in the Daily Accounts ledger.
 * Mandatory accounting fields are protected so vital financial data cannot be permanently removed.
 */
data class DailyAccountsColumnConfig(
  val id: String,
  val title: String,
  val isVisible: Boolean = true,
  val isCustom: Boolean = false,
  val isMandatory: Boolean = false,
  val order: Int = 0,
  val dataType: String = "Text",
  val defaultWidthDp: Int = 90
) {
  val width: Dp get() = defaultWidthDp.dp

  companion object {
    const val COL_SNO = "sNo"
    const val COL_FARMER_NAME = "farmerName"
    const val COL_PHONE = "phone"
    const val COL_VILLAGE = "village"
    const val COL_INVOICE_NUM = "invoiceNumber"
    const val COL_PRODUCT = "product"
    const val COL_QTY = "qty"
    const val COL_TOTAL = "total"
    const val COL_CASH = "cash"
    const val COL_ONLINE = "online"
    const val COL_DUE = "due"
    const val COL_PAYMENT_MODE = "paymentMode"
    const val COL_DATE = "date"
    const val COL_DISCOUNT = "discount"
    const val COL_GST = "gst"

    val MANDATORY_COLUMN_IDS = setOf(
      COL_SNO,
      COL_FARMER_NAME,
      COL_PRODUCT,
      COL_QTY,
      COL_TOTAL,
      COL_CASH,
      COL_ONLINE,
      COL_DUE
    )

    val DEFAULT_COLUMNS = listOf(
      DailyAccountsColumnConfig(
        id = COL_SNO,
        title = "S.No.",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 0,
        dataType = "Number",
        defaultWidthDp = 48
      ),
      DailyAccountsColumnConfig(
        id = COL_FARMER_NAME,
        title = "Farmer Name",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 1,
        dataType = "Text",
        defaultWidthDp = 145
      ),
      DailyAccountsColumnConfig(
        id = COL_PRODUCT,
        title = "Product",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 2,
        dataType = "Text",
        defaultWidthDp = 155
      ),
      DailyAccountsColumnConfig(
        id = COL_QTY,
        title = "Qty",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 3,
        dataType = "Number",
        defaultWidthDp = 75
      ),
      DailyAccountsColumnConfig(
        id = COL_TOTAL,
        title = "Total",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 4,
        dataType = "Currency",
        defaultWidthDp = 95
      ),
      DailyAccountsColumnConfig(
        id = COL_CASH,
        title = "Cash",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 5,
        dataType = "Currency",
        defaultWidthDp = 90
      ),
      DailyAccountsColumnConfig(
        id = COL_ONLINE,
        title = "Online",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 6,
        dataType = "Currency",
        defaultWidthDp = 90
      ),
      DailyAccountsColumnConfig(
        id = COL_DUE,
        title = "Due",
        isVisible = true,
        isCustom = false,
        isMandatory = true,
        order = 7,
        dataType = "Currency",
        defaultWidthDp = 90
      ),
      DailyAccountsColumnConfig(
        id = COL_PHONE,
        title = "Phone Number",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 8,
        dataType = "Text",
        defaultWidthDp = 110
      ),
      DailyAccountsColumnConfig(
        id = COL_VILLAGE,
        title = "Village",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 9,
        dataType = "Text",
        defaultWidthDp = 110
      ),
      DailyAccountsColumnConfig(
        id = COL_INVOICE_NUM,
        title = "Bill / Inv #",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 10,
        dataType = "Text",
        defaultWidthDp = 120
      ),
      DailyAccountsColumnConfig(
        id = COL_PAYMENT_MODE,
        title = "Payment Mode",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 11,
        dataType = "Text",
        defaultWidthDp = 100
      ),
      DailyAccountsColumnConfig(
        id = COL_DATE,
        title = "Time / Date",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 12,
        dataType = "Date",
        defaultWidthDp = 120
      ),
      DailyAccountsColumnConfig(
        id = COL_DISCOUNT,
        title = "Discount",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 13,
        dataType = "Currency",
        defaultWidthDp = 85
      ),
      DailyAccountsColumnConfig(
        id = COL_GST,
        title = "GST",
        isVisible = false,
        isCustom = false,
        isMandatory = false,
        order = 14,
        dataType = "Currency",
        defaultWidthDp = 80
      )
    )
  }
}
