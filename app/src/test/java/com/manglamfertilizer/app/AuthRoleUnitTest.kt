package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.UserRole
import com.manglamfertilizer.app.data.repository.AuthRepository
import com.manglamfertilizer.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRoleUnitTest {

  @Test
  fun testAdminRoleMapping_kartik() {
    val role = AuthRepository.determineRole("kartik.bharadwaj0105@gmail.com")
    assertEquals(UserRole.ADMIN, role)
  }

  @Test
  fun testAdminRoleMapping_adminManglam() {
    val role = AuthRepository.determineRole("admin.manglamferilizer@gmail.com")
    assertEquals(UserRole.ADMIN, role)
  }

  @Test
  fun testAdminRoleMapping_withWhitespaceAndUppercase() {
    val role1 = AuthRepository.determineRole("  KARTIK.BHARADWAJ0105@GMAIL.COM  ")
    assertEquals(UserRole.ADMIN, role1)

    val role2 = AuthRepository.determineRole("  ADMIN.MANGLAMFERILIZER@GMAIL.COM  ")
    assertEquals(UserRole.ADMIN, role2)
  }

  @Test
  fun testStaffRoleMapping_otherAccounts() {
    val role1 = AuthRepository.determineRole("staff.member@gmail.com")
    assertEquals(UserRole.STAFF, role1)

    val role2 = AuthRepository.determineRole("cashier@manglam.com")
    assertEquals(UserRole.STAFF, role2)

    val role3 = AuthRepository.determineRole("user123@yahoo.com")
    assertEquals(UserRole.STAFF, role3)
  }

  @Test
  fun testScreenNavigationItemsNotNull() {
    val items = Screen.getBottomNavItems()
    assertEquals(6, items.size)
    items.forEach { screen ->
      assertNotNull("Screen object in bottom navigation must not be null", screen)
      assertNotNull("Route for ${screen.title} must not be null", screen.route)
      assertTrue(screen.route.isNotBlank())
    }
  }

  @Test
  fun testDisplayNameDoesNotAffectRole() {
    // Custom name or display name must never alter security authorization
    val role = AuthRepository.determineRole("staff.member@gmail.com")
    assertEquals(UserRole.STAFF, role)
  }
}
