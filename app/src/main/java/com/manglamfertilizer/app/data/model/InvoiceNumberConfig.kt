package com.manglamfertilizer.app.data.model

/**
 * Authoritative Admin-Controlled Invoice Number Configuration.
 * Stored in Firestore at: businesses/{businessId}/settings/invoiceNumber
 */
data class InvoiceNumberConfig(
  val startingNumber: Long = 2026001L,
  val nextInvoiceNumber: Long = 2026001L,
  val lastIssuedNumber: Long? = null,
  val prefix: String = "",
  val suffix: String = "",
  val enabled: Boolean = true,
  val updatedAt: Long = System.currentTimeMillis(),
  val updatedBy: String = "Admin"
) {
  /**
   * Formats a sequential number according to the configuration.
   */
  fun formatNumber(number: Long): String {
    return if (prefix.isNotBlank() || suffix.isNotBlank()) {
      "$prefix$number$suffix"
    } else {
      number.toString()
    }
  }

  /**
   * Returns a display representation of the format pattern (e.g. prefix#suffix).
   */
  fun formatPattern(): String {
    return if (prefix.isNotBlank() || suffix.isNotBlank()) {
      "${prefix}#${suffix}"
    } else {
      "Sequential (#)"
    }
  }

  /**
   * Returns the formatted string representation of the current next invoice number.
   */
  fun formattedNextNumber(): String = formatNumber(nextInvoiceNumber)

  /**
   * Returns the formatted string representation of the last issued invoice number, if any.
   */
  fun formattedLastIssuedNumber(): String = lastIssuedNumber?.let { formatNumber(it) } ?: "None"
}

sealed class InvoiceNumberValidationResult {
  object Valid : InvoiceNumberValidationResult()
  data class Warning(val message: String) : InvoiceNumberValidationResult()
  data class Error(val message: String) : InvoiceNumberValidationResult()
}
