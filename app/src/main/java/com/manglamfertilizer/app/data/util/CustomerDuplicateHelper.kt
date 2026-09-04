package com.manglamfertilizer.app.data.util

import com.manglamfertilizer.app.data.model.Customer

data class DuplicateCustomerMatch(
  val existingCustomer: Customer,
  val matchReason: String,
  val confidenceScore: Int
)

object CustomerDuplicateHelper {

  /**
   * Normalizes a string by lowercasing, removing extra whitespace and special characters.
   */
  fun normalize(str: String): String {
    return str.lowercase().replace(Regex("[^a-z0-9]"), "").trim()
  }

  /**
   * Normalizes a phone number to standard 10 digits.
   */
  fun normalizePhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
  }

  /**
   * Checks if input customer data matches any existing customer.
   * Compares Name, Phone, Village, and Address according to business rules.
   */
  fun findDuplicates(
    existingCustomers: List<Customer>,
    name: String,
    phone: String = "",
    village: String = "",
    address: String = "",
    excludeCustomerId: String? = null
  ): List<DuplicateCustomerMatch> {
    val trimmedName = name.trim()
    if (trimmedName.isBlank() || trimmedName.equals("Walk-in Farmer", ignoreCase = true)) {
      return emptyList()
    }

    val normName = normalize(trimmedName)
    val normPhone = normalizePhone(phone)
    val normVillage = normalize(village)
    val normAddress = normalize(address)

    val matches = mutableListOf<DuplicateCustomerMatch>()

    for (cust in existingCustomers) {
      if (!excludeCustomerId.isNullOrBlank() && cust.id == excludeCustomerId) {
        continue
      }

      val custNormName = normalize(cust.name)
      val custNormPhone = normalizePhone(cust.phoneNumber)
      val custNormVillage = normalize(cust.village)
      val custNormAddress = normalize(cust.address)

      val isExactPhoneMatch = normPhone.isNotBlank() && custNormPhone.isNotBlank() && normPhone == custNormPhone
      val isExactNameMatch = normName.isNotBlank() && custNormName == normName
      val isNameSimilar = normName.isNotBlank() && (custNormName.contains(normName) || normName.contains(custNormName))
      val isVillageMatch = normVillage.isNotBlank() && custNormVillage.isNotBlank() && normVillage == custNormVillage
      val isAddressMatch = normAddress.isNotBlank() && custNormAddress.isNotBlank() && normAddress == custNormAddress

      when {
        // High confidence: Matching phone AND matching name
        isExactPhoneMatch && isExactNameMatch -> {
          matches.add(
            DuplicateCustomerMatch(
              existingCustomer = cust,
              matchReason = "Identical Name & Mobile Number (${cust.phoneNumber})",
              confidenceScore = 100
            )
          )
        }
        // High confidence: Matching phone only (Phone is primary unique identifier in India)
        isExactPhoneMatch -> {
          matches.add(
            DuplicateCustomerMatch(
              existingCustomer = cust,
              matchReason = "Same Mobile Number registered for '${cust.name}'",
              confidenceScore = 90
            )
          )
        }
        // Medium confidence: Exact name AND matching village / address
        isExactNameMatch && (isVillageMatch || isAddressMatch) -> {
          val loc = if (isVillageMatch) "Village: ${cust.village}" else "Address: ${cust.address}"
          matches.add(
            DuplicateCustomerMatch(
              existingCustomer = cust,
              matchReason = "Matching Name & Location ($loc)",
              confidenceScore = 85
            )
          )
        }
        // Medium confidence: Similar name AND matching village
        isNameSimilar && isVillageMatch -> {
          matches.add(
            DuplicateCustomerMatch(
              existingCustomer = cust,
              matchReason = "Similar Name in same Village (${cust.village})",
              confidenceScore = 70
            )
          )
        }
        // Notice: Exact name match without conflicting phone
        isExactNameMatch && (normPhone.isBlank() || custNormPhone.isBlank()) -> {
          matches.add(
            DuplicateCustomerMatch(
              existingCustomer = cust,
              matchReason = "Exact Name Match (${cust.name})",
              confidenceScore = 60
            )
          )
        }
      }
    }

    return matches.sortedByDescending { it.confidenceScore }
  }
}
