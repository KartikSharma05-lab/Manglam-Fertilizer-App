package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.UserRole
import com.manglamfertilizer.app.data.repository.AuthRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Test Suite for MANGALAM FERTILIZER Firestore Security Rules & Data Integrity
 *
 * Verifies rule logic according to system specifications:
 * 1. Unauthenticated read/write -> DENIED
 * 2. Authenticated Staff valid operations -> ALLOWED
 * 3. Unauthorized Staff operations (delete product, delete invoice, tamper createdAt, impersonation) -> DENIED
 * 4. Admin valid operations -> ALLOWED
 * 5. Audit log modification by normal client -> DENIED
 * 6. Audit log deletion by normal client / admin -> DENIED
 */
class FirestoreSecurityRulesUnitTest {

  data class AuthContext(
    val isAuthenticated: Boolean,
    val email: String? = null,
    val uid: String? = null
  ) {
    val role: UserRole
      get() = if (!isAuthenticated || email == null) UserRole.STAFF else AuthRepository.determineRole(email)

    val isAdmin: Boolean
      get() = isAuthenticated && role == UserRole.ADMIN

    val isStaff: Boolean
      get() = isAuthenticated && !isAdmin
  }

  // Model Firestore security evaluation engine matching firestore.rules
  object FirestoreSecurityEngine {

    fun canRead(auth: AuthContext, collection: String): Boolean {
      if (!auth.isAuthenticated) return false
      return when (collection) {
        "products", "categories", "invoices", "customers", "credit_records", "users", "settings", "inventory_history" -> true
        "auditLogs", "audit_logs", "auditCleanupRuns", "audit_cleanup_runs", "backups" -> auth.isAdmin
        else -> false
      }
    }

    fun canCreateProduct(
      auth: AuthContext,
      name: String,
      sellingPrice: Double,
      stockQuantity: Double
    ): Boolean {
      if (!auth.isAuthenticated) return false
      if (auth.isAdmin) return true
      return auth.isStaff && name.isNotBlank() && sellingPrice >= 0 && stockQuantity >= 0
    }

    fun canUpdateProduct(
      auth: AuthContext,
      name: String,
      sellingPrice: Double,
      stockQuantity: Double,
      originalCreatedAt: Long,
      newCreatedAt: Long,
      originalCreatedBy: String,
      newCreatedBy: String
    ): Boolean {
      if (!auth.isAuthenticated) return false
      if (auth.isAdmin) return true
      return auth.isStaff &&
          name.isNotBlank() &&
          sellingPrice >= 0 &&
          stockQuantity >= 0 &&
          originalCreatedAt == newCreatedAt &&
          originalCreatedBy == newCreatedBy
    }

    fun canDeleteProduct(auth: AuthContext): Boolean {
      if (!auth.isAuthenticated) return false
      return auth.isAdmin
    }

    fun canCreateInvoice(
      auth: AuthContext,
      amount: Double,
      invoiceUserEmail: String,
      invoiceUserId: String
    ): Boolean {
      if (!auth.isAuthenticated) return false
      if (auth.isAdmin) return true
      return auth.isStaff &&
          amount >= 0 &&
          (invoiceUserEmail.equals(auth.email, ignoreCase = true) || invoiceUserId == auth.uid)
    }

    fun canUpdateOrDeleteInvoice(auth: AuthContext): Boolean {
      if (!auth.isAuthenticated) return false
      return auth.isAdmin
    }

    fun canCreateAuditLog(
      auth: AuthContext,
      action: String,
      logUserEmail: String,
      logUserId: String
    ): Boolean {
      if (!auth.isAuthenticated) return false
      if (action.isBlank()) return false
      if (auth.isAdmin) return true
      return auth.isStaff &&
          (logUserEmail.equals(auth.email, ignoreCase = true) || logUserId == auth.uid)
    }

    fun canUpdateAuditLog(auth: AuthContext): Boolean {
      // Audit logs are append-only; update is strictly false for everyone
      return false
    }

    fun canDeleteAuditLog(auth: AuthContext): Boolean {
      // Audit logs are immutable; deletion by client is strictly false
      return false
    }

    fun canWriteSettings(auth: AuthContext): Boolean {
      if (!auth.isAuthenticated) return false
      return auth.isAdmin
    }

    fun canWriteBackups(auth: AuthContext): Boolean {
      if (!auth.isAuthenticated) return false
      return auth.isAdmin
    }
  }

  // =========================================================================
  // 1. UNAUTHENTICATED ACCESS TESTS (DENIED)
  // =========================================================================

  @Test
  fun testUnauthenticated_ReadProductsDenied() {
    val unauth = AuthContext(isAuthenticated = false)
    assertFalse(FirestoreSecurityEngine.canRead(unauth, "products"))
    assertFalse(FirestoreSecurityEngine.canRead(unauth, "invoices"))
    assertFalse(FirestoreSecurityEngine.canRead(unauth, "auditLogs"))
    assertFalse(FirestoreSecurityEngine.canRead(unauth, "settings"))
  }

  @Test
  fun testUnauthenticated_WriteProductsDenied() {
    val unauth = AuthContext(isAuthenticated = false)
    assertFalse(FirestoreSecurityEngine.canCreateProduct(unauth, "Urea 50kg", 266.0, 100.0))
    assertFalse(FirestoreSecurityEngine.canDeleteProduct(unauth))
  }

  @Test
  fun testUnauthenticated_InvoiceOperationsDenied() {
    val unauth = AuthContext(isAuthenticated = false)
    assertFalse(FirestoreSecurityEngine.canCreateInvoice(unauth, 5000.0, "staff@manglam.com", "uid123"))
    assertFalse(FirestoreSecurityEngine.canUpdateOrDeleteInvoice(unauth))
  }

  // =========================================================================
  // 2. AUTHENTICATED STAFF VALID OPERATIONS (ALLOWED)
  // =========================================================================

  @Test
  fun testStaff_ValidReadOperationsAllowed() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    assertTrue(FirestoreSecurityEngine.canRead(staff, "products"))
    assertTrue(FirestoreSecurityEngine.canRead(staff, "categories"))
    assertTrue(FirestoreSecurityEngine.canRead(staff, "invoices"))
    assertTrue(FirestoreSecurityEngine.canRead(staff, "customers"))
  }

  @Test
  fun testStaff_ValidProductCreateAndEditAllowed() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    val canCreate = FirestoreSecurityEngine.canCreateProduct(
      auth = staff,
      name = "DAP 50kg IFFCO",
      sellingPrice = 1350.0,
      stockQuantity = 50.0
    )
    assertTrue(canCreate)

    val canUpdate = FirestoreSecurityEngine.canUpdateProduct(
      auth = staff,
      name = "DAP 50kg IFFCO",
      sellingPrice = 1350.0,
      stockQuantity = 60.0,
      originalCreatedAt = 1700000000000L,
      newCreatedAt = 1700000000000L,
      originalCreatedBy = "sales.staff@manglam.com",
      newCreatedBy = "sales.staff@manglam.com"
    )
    assertTrue(canUpdate)
  }

  @Test
  fun testStaff_ValidInvoiceAndAuditCreationAllowed() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    val canCreateInvoice = FirestoreSecurityEngine.canCreateInvoice(
      auth = staff,
      amount = 2500.0,
      invoiceUserEmail = "sales.staff@manglam.com",
      invoiceUserId = "staff_uid_01"
    )
    assertTrue(canCreateInvoice)

    val canCreateAudit = FirestoreSecurityEngine.canCreateAuditLog(
      auth = staff,
      action = "INVOICE_CREATED",
      logUserEmail = "sales.staff@manglam.com",
      logUserId = "staff_uid_01"
    )
    assertTrue(canCreateAudit)
  }

  // =========================================================================
  // 3. UNAUTHORIZED STAFF OPERATIONS (DENIED)
  // =========================================================================

  @Test
  fun testStaff_ProductDeletionDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    assertFalse("Staff must NOT be permitted to delete products", FirestoreSecurityEngine.canDeleteProduct(staff))
  }

  @Test
  fun testStaff_InvoiceModificationOrDeletionDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    assertFalse("Staff must NOT update or delete invoices", FirestoreSecurityEngine.canUpdateOrDeleteInvoice(staff))
  }

  @Test
  fun testStaff_TamperingCreatedAtOrAuthorDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    val tamperedDate = FirestoreSecurityEngine.canUpdateProduct(
      auth = staff,
      name = "DAP 50kg IFFCO",
      sellingPrice = 1350.0,
      stockQuantity = 60.0,
      originalCreatedAt = 1700000000000L,
      newCreatedAt = 1799999999999L, // Tampered date
      originalCreatedBy = "admin@manglam.com",
      newCreatedBy = "admin@manglam.com"
    )
    assertFalse("Staff cannot tamper with createdAt timestamp", tamperedDate)

    val tamperedAuthor = FirestoreSecurityEngine.canUpdateProduct(
      auth = staff,
      name = "DAP 50kg IFFCO",
      sellingPrice = 1350.0,
      stockQuantity = 60.0,
      originalCreatedAt = 1700000000000L,
      newCreatedAt = 1700000000000L,
      originalCreatedBy = "admin@manglam.com",
      newCreatedBy = "sales.staff@manglam.com" // Tampered author
    )
    assertFalse("Staff cannot tamper with createdBy author metadata", tamperedAuthor)
  }

  @Test
  fun testStaff_IdentitySpoofingDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    val spoofedInvoice = FirestoreSecurityEngine.canCreateInvoice(
      auth = staff,
      amount = 2500.0,
      invoiceUserEmail = "kartik.bharadwaj0105@gmail.com", // Spoofing Admin
      invoiceUserId = "admin_uid_99"
    )
    assertFalse("Staff cannot forge another user's email on invoices", spoofedInvoice)

    val spoofedAudit = FirestoreSecurityEngine.canCreateAuditLog(
      auth = staff,
      action = "PRODUCT_DELETED",
      logUserEmail = "kartik.bharadwaj0105@gmail.com", // Spoofing Admin
      logUserId = "admin_uid_99"
    )
    assertFalse("Staff cannot forge another user's email on audit logs", spoofedAudit)
  }

  @Test
  fun testStaff_ReadingAuditLogsOrWritingSettingsDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    assertFalse(FirestoreSecurityEngine.canRead(staff, "auditLogs"))
    assertFalse(FirestoreSecurityEngine.canRead(staff, "audit_logs"))
    assertFalse(FirestoreSecurityEngine.canWriteSettings(staff))
    assertFalse(FirestoreSecurityEngine.canWriteBackups(staff))
  }

  // =========================================================================
  // 4. ADMIN VALID OPERATIONS (ALLOWED)
  // =========================================================================

  @Test
  fun testAdmin_KartikAuthorizedOperationsAllowed() {
    val admin = AuthContext(isAuthenticated = true, email = "kartik.bharadwaj0105@gmail.com", uid = "admin_kartik_01")
    assertTrue(admin.isAdmin)
    assertTrue(FirestoreSecurityEngine.canRead(admin, "auditLogs"))
    assertTrue(FirestoreSecurityEngine.canRead(admin, "backups"))
    assertTrue(FirestoreSecurityEngine.canCreateProduct(admin, "Zinc Sulfate", 450.0, 20.0))
    assertTrue(FirestoreSecurityEngine.canDeleteProduct(admin))
    assertTrue(FirestoreSecurityEngine.canUpdateOrDeleteInvoice(admin))
    assertTrue(FirestoreSecurityEngine.canWriteSettings(admin))
    assertTrue(FirestoreSecurityEngine.canWriteBackups(admin))
  }

  @Test
  fun testAdmin_ManglamAdminAuthorizedOperationsAllowed() {
    val admin = AuthContext(isAuthenticated = true, email = "admin.manglamferilizer@gmail.com", uid = "admin_manglam_02")
    assertTrue(admin.isAdmin)
    assertTrue(FirestoreSecurityEngine.canDeleteProduct(admin))
    assertTrue(FirestoreSecurityEngine.canUpdateOrDeleteInvoice(admin))
    assertTrue(FirestoreSecurityEngine.canWriteSettings(admin))
  }

  // =========================================================================
  // 5. AUDIT LOG IMMUTABILITY & DELETION RESTRICTION (DENIED)
  // =========================================================================

  @Test
  fun testAuditLogs_ClientModificationStrictlyDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    val admin = AuthContext(isAuthenticated = true, email = "kartik.bharadwaj0105@gmail.com", uid = "admin_kartik_01")

    assertFalse("Normal client update to auditLogs must be strictly DENIED", FirestoreSecurityEngine.canUpdateAuditLog(staff))
    assertFalse("Admin update to auditLogs must be strictly DENIED", FirestoreSecurityEngine.canUpdateAuditLog(admin))
  }

  @Test
  fun testAuditLogs_ClientDeletionStrictlyDenied() {
    val staff = AuthContext(isAuthenticated = true, email = "sales.staff@manglam.com", uid = "staff_uid_01")
    val admin = AuthContext(isAuthenticated = true, email = "kartik.bharadwaj0105@gmail.com", uid = "admin_kartik_01")

    assertFalse("Normal client delete to auditLogs must be strictly DENIED", FirestoreSecurityEngine.canDeleteAuditLog(staff))
    assertFalse("Admin delete to auditLogs must be strictly DENIED", FirestoreSecurityEngine.canDeleteAuditLog(admin))
  }
}
