package com.manglamfertilizer.app.data.util

import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.model.UserRole

/**
 * Authoritative security utility for Admin vs Staff verification.
 * As strictly mandated:
 * - ONLY these two email addresses are Admin:
 *   1) kartik.bharadwaj0105@gmail.com
 *   2) admin.manglamferilizer@gmail.com (and admin.manglamfertilizer@gmail.com)
 * - Firebase UID is NOT used to determine Admin status.
 * - Any other account is treated as STAFF.
 */
object AdminAuthUtils {
  private val ADMIN_EMAILS = setOf(
    "kartik.bharadwaj0105@gmail.com",
    "admin.manglamferilizer@gmail.com",
    "admin.manglamfertilizer@gmail.com"
  )

  fun getAdminEmails(): Set<String> = ADMIN_EMAILS

  fun resolveAdminEmail(providedEmail: String? = null, fallbackName: String? = null): String {
    val candidate = when {
      !providedEmail.isNullOrBlank() -> providedEmail.trim()
      fallbackName != null && fallbackName.contains("@") -> fallbackName.trim()
      else -> com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email?.trim() ?: ""
    }
    return candidate
  }

  fun isAdmin(email: String?): Boolean {
    if (email.isNullOrBlank()) return false
    return ADMIN_EMAILS.contains(email.trim().lowercase())
  }

  fun isAdmin(user: User?): Boolean {
    if (user == null) return false
    return isAdmin(user.email)
  }

  fun getEffectiveRole(user: User?): UserRole {
    return if (isAdmin(user)) UserRole.ADMIN else UserRole.STAFF
  }
}
