package com.manglamfertilizer.app.data.util

import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.model.InvoiceNumberValidationResult

/**
 * Pure domain logic for Admin Invoice Number configuration,
 * sequence validation, duplicate detection, and concurrency protection.
 */
object InvoiceNumberManager {

  const val DEFAULT_STARTING_NUMBER = 2026001L

  /**
   * Extracts the numeric part from an invoice number string.
   * Handles pure numbers ("2026001"), prefixed numbers ("INV-2026001"), or dashed numbers ("2026-001").
   */
  fun extractNumericValue(invoiceNumberStr: String?): Long? {
    if (invoiceNumberStr.isNullOrBlank()) return null
    // Try direct long parsing first
    invoiceNumberStr.toLongOrNull()?.let { return it }

    // If it contains letters/dashes, extract trailing numeric sequence
    val digitsOnly = invoiceNumberStr.filter { it.isDigit() }
    return digitsOnly.toLongOrNull()
  }

  /**
   * Finds the highest numeric invoice number among all existing invoices in the database.
   */
  fun findHighestIssuedInvoiceNumber(invoices: List<Invoice>): Long? {
    return invoices.mapNotNull { extractNumericValue(it.invoiceNumber) }.maxOrNull()
  }

  /**
   * Checks if a numeric invoice number is already assigned to an existing invoice.
   */
  fun isNumberAlreadyAssigned(numberToCheck: Long, invoices: List<Invoice>): Boolean {
    val numberStr = numberToCheck.toString()
    return invoices.any { inv ->
      inv.invoiceNumber.trim() == numberStr || extractNumericValue(inv.invoiceNumber) == numberToCheck
    }
  }

  /**
   * Computes safe initial configuration based on existing invoices if no config exists.
   */
  fun computeSafeInitialConfig(existingInvoices: List<Invoice>): InvoiceNumberConfig {
    val maxExisting = findHighestIssuedInvoiceNumber(existingInvoices)
    return if (maxExisting != null && maxExisting >= DEFAULT_STARTING_NUMBER) {
      InvoiceNumberConfig(
        startingNumber = DEFAULT_STARTING_NUMBER,
        nextInvoiceNumber = maxExisting + 1,
        lastIssuedNumber = maxExisting,
        enabled = true
      )
    } else if (maxExisting != null && maxExisting > 0) {
      InvoiceNumberConfig(
        startingNumber = maxExisting + 1,
        nextInvoiceNumber = maxExisting + 1,
        lastIssuedNumber = maxExisting,
        enabled = true
      )
    } else {
      InvoiceNumberConfig(
        startingNumber = DEFAULT_STARTING_NUMBER,
        nextInvoiceNumber = DEFAULT_STARTING_NUMBER,
        lastIssuedNumber = null,
        enabled = true
      )
    }
  }

  /**
   * Validates a newly proposed starting invoice number (Initial Setup).
   */
  fun validateStartingNumber(
    startingNumberInput: String,
    existingInvoices: List<Invoice>
  ): InvoiceNumberValidationResult {
    val clean = startingNumberInput.trim()
    if (clean.isBlank()) {
      return InvoiceNumberValidationResult.Error("Starting invoice number cannot be empty.")
    }
    if (clean.contains(".")) {
      return InvoiceNumberValidationResult.Error("Decimal values are not allowed. Please enter a whole integer.")
    }
    val parsed = clean.toLongOrNull()
      ?: return InvoiceNumberValidationResult.Error("Invalid number. Please enter numeric digits (e.g. 2026001).")

    if (parsed <= 0) {
      return InvoiceNumberValidationResult.Error("Starting invoice number must be greater than 0.")
    }

    if (isNumberAlreadyAssigned(parsed, existingInvoices)) {
      return InvoiceNumberValidationResult.Error(
        "Invoice number $parsed is already assigned to an existing invoice. Please choose a higher number."
      )
    }

    val maxExisting = findHighestIssuedInvoiceNumber(existingInvoices)
    if (maxExisting != null && parsed <= maxExisting) {
      return InvoiceNumberValidationResult.Error(
        "Existing invoices already reach up to $maxExisting. Starting number must be greater than $maxExisting to prevent duplicates."
      )
    }

    return InvoiceNumberValidationResult.Valid
  }

  /**
   * Validates an Admin override for the next invoice number.
   */
  fun validateNextNumberOverride(
    nextNumberInput: String,
    currentConfig: InvoiceNumberConfig,
    existingInvoices: List<Invoice>
  ): InvoiceNumberValidationResult {
    val clean = nextNumberInput.trim()
    if (clean.isBlank()) {
      return InvoiceNumberValidationResult.Error("Next invoice number cannot be empty.")
    }
    if (clean.contains(".")) {
      return InvoiceNumberValidationResult.Error("Decimal values are not allowed.")
    }
    val parsed = clean.toLongOrNull()
      ?: return InvoiceNumberValidationResult.Error("Invalid number format. Please enter numeric digits.")

    if (parsed <= 0) {
      return InvoiceNumberValidationResult.Error("Next invoice number must be greater than 0.")
    }

    if (isNumberAlreadyAssigned(parsed, existingInvoices)) {
      return InvoiceNumberValidationResult.Error(
        "Invoice #$parsed already exists in the records! Setting this number will cause duplicate invoices."
      )
    }

    val maxExisting = findHighestIssuedInvoiceNumber(existingInvoices)
    if (maxExisting != null && parsed <= maxExisting) {
      return InvoiceNumberValidationResult.Error(
        "Conflict: Highest existing invoice number is $maxExisting. New number must be at least ${maxExisting + 1} to prevent duplicate collision."
      )
    }

    if (currentConfig.lastIssuedNumber != null && parsed < currentConfig.lastIssuedNumber) {
      return InvoiceNumberValidationResult.Error(
        "Cannot set next number ($parsed) lower than the last issued number (${currentConfig.lastIssuedNumber})."
      )
    }

    if (parsed > (currentConfig.nextInvoiceNumber + 1000)) {
      return InvoiceNumberValidationResult.Warning(
        "Notice: Setting next number to $parsed will create a large sequence gap from current ${currentConfig.nextInvoiceNumber}."
      )
    }

    return InvoiceNumberValidationResult.Valid
  }
}
