package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.manglamfertilizer.app.data.model.GreetingInfo
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.model.UserRole
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
  context: Context,
  private val auditRepository: AuditRepository = AuditRepository.getInstance(context)
) {
  private val tag = "AuthRepository"
  private val repoScope = CoroutineScope(Dispatchers.IO)

  companion object {
    val ADMIN_EMAILS = AdminAuthUtils.getAdminEmails()

    fun determineRole(email: String?): UserRole {
      return if (AdminAuthUtils.isAdmin(email)) {
        UserRole.ADMIN
      } else {
        UserRole.STAFF
      }
    }
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences("manglam_auth_session", Context.MODE_PRIVATE)

  private val firebaseAuth: FirebaseAuth? get() = com.manglamfertilizer.app.data.util.FirestoreProvider.auth
  private val firestore: FirebaseFirestore? get() = com.manglamfertilizer.app.data.util.FirestoreProvider.get()

  private val _currentUser = MutableStateFlow<User?>(null)
  val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

  // Track whether the current app session has passed local biometric/device lock unlock
  private val _isSessionUnlocked = MutableStateFlow(true)
  val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

  init {
    checkCurrentUser()
  }

  fun checkCurrentUser() {
    val fbUser = firebaseAuth?.currentUser
    val emailStr = fbUser?.email?.trim()
    if (fbUser != null && !emailStr.isNullOrBlank()) {
      val normalizedEmail = emailStr.lowercase(Locale.ROOT)
      val emailKey = normalizedEmail.replace(".", "_")
      val role = determineRole(normalizedEmail)
      val userId = fbUser.uid
      
      // Load account-specific preferred greeting name if saved
      val savedGreetingName = prefs.getString("greeting_name_$userId", null)
        ?: prefs.getString("greeting_name_$emailKey", null)
        ?: fbUser.displayName?.trim()?.takeIf { it.isNotBlank() }
        ?: ""

      val isCompleted = isDisplayNameSetupCompleted(userId) || savedGreetingName.isNotBlank()
      if (isCompleted) {
        prefs.edit()
          .putBoolean("display_name_setup_completed_$userId", true)
          .putBoolean("display_name_setup_completed_$emailKey", true)
          .putBoolean("profile_completed_$userId", true)
          .putBoolean("profile_completed_$emailKey", true)
          .apply()
      }

      val user = User(
        id = userId,
        name = savedGreetingName,
        email = normalizedEmail,
        role = role,
        active = true,
        phoneNumber = prefs.getString("user_phone_$userId", fbUser.phoneNumber ?: "") ?: "",
        branchName = prefs.getString("user_branch_$userId", "Main Branch") ?: "Main Branch",
        lastLogin = System.currentTimeMillis()
      )
      _currentUser.value = user

      // Check if device unlock is required on app launch
      val isDeviceLockEnabled = isDeviceUnlockEnabled(user.id)
      _isSessionUnlocked.value = !isDeviceLockEnabled

      // Background sync from Firestore to restore cloud profile if local is missing
      repoScope.launch {
        try {
          val db = firestore
          if (db != null) {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
              val cloudName = doc.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
              val cloudCompleted = doc.getBoolean("profileCompleted") == true
              if (cloudName != null) {
                prefs.edit()
                  .putString("greeting_name_$userId", cloudName)
                  .putString("greeting_name_$emailKey", cloudName)
                  .putBoolean("display_name_setup_completed_$userId", true)
                  .putBoolean("display_name_setup_completed_$emailKey", true)
                  .putBoolean("profile_completed_$userId", true)
                  .putBoolean("profile_completed_$emailKey", true)
                  .apply()
                _currentUser.value?.let { curr ->
                  if (curr.id == userId && curr.name != cloudName) {
                    _currentUser.value = curr.copy(name = cloudName)
                  }
                }
              } else if (cloudCompleted) {
                prefs.edit()
                  .putBoolean("display_name_setup_completed_$userId", true)
                  .putBoolean("display_name_setup_completed_$emailKey", true)
                  .putBoolean("profile_completed_$userId", true)
                  .putBoolean("profile_completed_$emailKey", true)
                  .apply()
              }
            }
          }
        } catch (e: Exception) {
          Log.w(tag, "Optional profile cloud sync skipped: ${e.message}")
        }
      }
    } else {
      _currentUser.value = null
      _isSessionUnlocked.value = true
    }
  }

  fun isDisplayNameSetupCompleted(userId: String): Boolean {
    val emailKey = _currentUser.value?.email?.replace(".", "_")
    if (prefs.getBoolean("display_name_setup_completed_$userId", false)) return true
    if (prefs.getBoolean("profile_completed_$userId", false)) return true
    if (emailKey != null) {
      if (prefs.getBoolean("display_name_setup_completed_$emailKey", false)) return true
      if (prefs.getBoolean("profile_completed_$emailKey", false)) return true
      if (!prefs.getString("greeting_name_$emailKey", null).isNullOrBlank()) return true
    }
    val savedName = prefs.getString("greeting_name_$userId", null)
    if (!savedName.isNullOrBlank()) {
      prefs.edit().putBoolean("display_name_setup_completed_$userId", true).apply()
      return true
    }
    val currentUserVal = _currentUser.value
    if (currentUserVal != null && currentUserVal.id == userId && currentUserVal.name.isNotBlank()) {
      prefs.edit().putBoolean("display_name_setup_completed_$userId", true).apply()
      return true
    }
    val fbUser = firebaseAuth?.currentUser
    val fbDisplayName = fbUser?.displayName?.trim()
    if (fbUser != null && fbUser.uid == userId && !fbDisplayName.isNullOrBlank()) {
      prefs.edit()
        .putString("greeting_name_$userId", fbDisplayName)
        .putBoolean("display_name_setup_completed_$userId", true)
        .putBoolean("profile_completed_$userId", true)
        .apply()
      return true
    }
    return false
  }

  fun setDisplayNameSetupCompleted(userId: String, completed: Boolean) {
    val emailKey = _currentUser.value?.email?.replace(".", "_")
    val editor = prefs.edit()
      .putBoolean("display_name_setup_completed_$userId", completed)
      .putBoolean("profile_completed_$userId", completed)
    if (emailKey != null) {
      editor.putBoolean("display_name_setup_completed_$emailKey", completed)
        .putBoolean("profile_completed_$emailKey", completed)
    }
    editor.apply()
  }

  fun getFirebaseDisplayName(): String? {
    return firebaseAuth?.currentUser?.displayName?.trim()?.takeIf { it.isNotBlank() }
  }

  fun updatePreferredGreetingName(userId: String, name: String) {
    val cleanName = name.trim()
    val emailKey = _currentUser.value?.email?.replace(".", "_")
    val editor = prefs.edit()
      .putString("greeting_name_$userId", cleanName)
      .putBoolean("display_name_setup_completed_$userId", true)
      .putBoolean("profile_completed_$userId", true)
    if (emailKey != null) {
      editor.putString("greeting_name_$emailKey", cleanName)
        .putBoolean("display_name_setup_completed_$emailKey", true)
        .putBoolean("profile_completed_$emailKey", true)
    }
    editor.apply()

    val oldName = _currentUser.value?.name ?: ""
    _currentUser.value?.let { current ->
      if (current.id == userId) {
        _currentUser.value = current.copy(name = cleanName)
        val roleStr = current.role.name
        auditRepository.logProfileNameChanged(
          oldName = oldName,
          newName = cleanName,
          userEmail = current.email,
          userRole = roleStr,
          userId = userId
        )
      }
    }

    // Background Firebase Profile update and Firestore persistent cloud sync
    repoScope.launch {
      try {
        val fbUser = firebaseAuth?.currentUser
        if (fbUser != null && fbUser.uid == userId) {
          val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(cleanName)
            .build()
          fbUser.updateProfile(profileUpdates).await()
        }
      } catch (e: Exception) {
        Log.w(tag, "Optional updateProfile omitted: ${e.message}")
      }

      try {
        val db = firestore
        if (db != null) {
          val map = mapOf(
            "displayName" to cleanName,
            "profileCompleted" to true,
            "updatedAt" to System.currentTimeMillis()
          )
          db.collection("users").document(userId).set(map, SetOptions.merge()).await()
        }
      } catch (e: Exception) {
        Log.w(tag, "Optional firestore name update omitted: ${e.message}")
      }
    }
  }

  fun isDeviceUnlockEnabled(userId: String): Boolean {
    return prefs.getBoolean("device_unlock_enabled_$userId", false)
  }

  fun setDeviceUnlockEnabled(userId: String, enabled: Boolean) {
    prefs.edit().putBoolean("device_unlock_enabled_$userId", enabled).apply()
  }

  fun hasPromptedDeviceUnlock(userId: String): Boolean {
    return prefs.getBoolean("device_unlock_prompted_$userId", false)
  }

  fun setPromptedDeviceUnlock(userId: String, prompted: Boolean) {
    prefs.edit().putBoolean("device_unlock_prompted_$userId", prompted).apply()
  }

  fun unlockSession() {
    _isSessionUnlocked.value = true
  }

  fun lockSession() {
    val user = _currentUser.value
    if (user != null && isDeviceUnlockEnabled(user.id)) {
      _isSessionUnlocked.value = false
    }
  }

  suspend fun login(email: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
    val trimmedEmail = email.trim()
    val normalizedEmail = trimmedEmail.lowercase(Locale.ROOT)
    Log.d(tag, "[AUTH_DIAGNOSTIC] AUTH_START: Starting login for $normalizedEmail")

    if (trimmedEmail.isBlank()) {
      return@withContext Result.failure(Exception("Please enter your registered email."))
    }
    if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
      return@withContext Result.failure(Exception("Please enter a valid email address."))
    }
    if (pass.isBlank()) {
      return@withContext Result.failure(Exception("Please enter your password."))
    }

    val auth = firebaseAuth
    if (auth == null) {
      Log.e(tag, "[AUTH_DIAGNOSTIC] AUTH_FAILED: FirebaseAuth instance is null")
      return@withContext Result.failure(Exception("Firebase Authentication is initializing. Please check your internet connection."))
    }

    val authResult = try {
      auth.signInWithEmailAndPassword(trimmedEmail, pass).await()
    } catch (e: FirebaseAuthInvalidUserException) {
      Log.w(tag, "[AUTH_DIAGNOSTIC] AUTH_FAILED: User not found (${e.errorCode})")
      return@withContext Result.failure(Exception("Invalid email or password."))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Log.w(tag, "[AUTH_DIAGNOSTIC] AUTH_FAILED: Invalid credentials (${e.errorCode}): ${e.message}")
      return@withContext Result.failure(Exception("Invalid email or password."))
    } catch (e: FirebaseAuthException) {
      Log.w(tag, "[AUTH_DIAGNOSTIC] AUTH_FAILED: Firebase auth error code=${e.errorCode}: ${e.message}")
      return@withContext Result.failure(Exception(mapFirebaseErrorCode(e.errorCode, e.message)))
    } catch (e: Exception) {
      Log.e(tag, "[AUTH_DIAGNOSTIC] AUTH_FAILED: General auth error: ${e.message}", e)
      val msg = when {
        e.message?.contains("network", ignoreCase = true) == true || e.message?.contains("unreachable", ignoreCase = true) == true ->
          "Please check your internet connection."
        e.message?.contains("disabled", ignoreCase = true) == true ->
          "This account is disabled. Please contact the administrator."
        e.message?.contains("credential", ignoreCase = true) == true ||
        e.message?.contains("password", ignoreCase = true) == true ||
        e.message?.contains("expired", ignoreCase = true) == true ||
        e.message?.contains("malformed", ignoreCase = true) == true ||
        e.message?.contains("incorrect", ignoreCase = true) == true ->
          "Invalid email or password."
        else ->
          "Authentication failed. Please verify your credentials."
      }
      return@withContext Result.failure(Exception(msg))
    }

    val fbUser = authResult.user
    if (fbUser == null || fbUser.uid.isBlank()) {
      Log.e(tag, "[AUTH_DIAGNOSTIC] AUTH_FAILED: FirebaseUser or UID is null")
      auth.signOut()
      return@withContext Result.failure(Exception("Authentication failed. User identity not found."))
    }

    val userEmail = (fbUser.email ?: normalizedEmail).trim().lowercase(Locale.ROOT)
    val role = determineRole(userEmail)
    val userId = fbUser.uid

    Log.d(tag, "[AUTH_DIAGNOSTIC] AUTH_SUCCESS: Firebase Authentication succeeded for uid=$userId")
    Log.d(tag, "[AUTH_DIAGNOSTIC] ROLE_DETERMINED: Email '$userEmail' mapped to Role: ${role.name}")

    val emailKey = userEmail.replace(".", "_")

    // Retrieve account-specific saved greeting name if configured previously
    var savedGreetingName = prefs.getString("greeting_name_$userId", null)
      ?: prefs.getString("greeting_name_$emailKey", null)
      ?: fbUser.displayName?.trim()?.takeIf { it.isNotBlank() }

    // Fetch from Firestore users/$userId to restore across fresh installs or different devices
    try {
      val db = firestore
      if (db != null) {
        val userDoc = db.collection("users").document(userId).get().await()
        if (userDoc.exists()) {
          val cloudName = userDoc.getString("displayName")?.trim()?.takeIf { it.isNotBlank() }
          val cloudCompleted = userDoc.getBoolean("profileCompleted") == true
          if (cloudName != null) {
            savedGreetingName = cloudName
            prefs.edit()
              .putString("greeting_name_$userId", cloudName)
              .putString("greeting_name_$emailKey", cloudName)
              .putBoolean("display_name_setup_completed_$userId", true)
              .putBoolean("display_name_setup_completed_$emailKey", true)
              .putBoolean("profile_completed_$userId", true)
              .putBoolean("profile_completed_$emailKey", true)
              .apply()
          } else if (cloudCompleted) {
            prefs.edit()
              .putBoolean("display_name_setup_completed_$userId", true)
              .putBoolean("display_name_setup_completed_$emailKey", true)
              .putBoolean("profile_completed_$userId", true)
              .putBoolean("profile_completed_$emailKey", true)
              .apply()
          }
        }
      }
    } catch (e: Exception) {
      Log.w(tag, "Optional firestore profile fetch skipped: ${e.message}")
    }

    val finalDisplayName = savedGreetingName?.trim()?.takeIf { it.isNotBlank() } ?: ""
    if (finalDisplayName.isNotBlank()) {
      prefs.edit()
        .putString("greeting_name_$userId", finalDisplayName)
        .putString("greeting_name_$emailKey", finalDisplayName)
        .putBoolean("display_name_setup_completed_$userId", true)
        .putBoolean("display_name_setup_completed_$emailKey", true)
        .putBoolean("profile_completed_$userId", true)
        .putBoolean("profile_completed_$emailKey", true)
        .apply()
    }

    val user = User(
      id = userId,
      name = finalDisplayName,
      email = userEmail,
      role = role,
      active = true,
      phoneNumber = prefs.getString("user_phone_$userId", fbUser.phoneNumber ?: "") ?: "",
      branchName = prefs.getString("user_branch_$userId", "Main Branch") ?: "Main Branch",
      createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      lastLogin = System.currentTimeMillis()
    )

    // Persist current session pointer
    prefs.edit()
      .putString("active_session_uid", userId)
      .putString("active_session_email", userEmail)
      .apply()

    _currentUser.value = user
    _isSessionUnlocked.value = true // Fresh login is immediately unlocked

    // Log authoritative audit event for USER_LOGIN
    auditRepository.logUserLogin(
      email = user.email,
      role = user.role.name,
      userId = user.id
    )

    // Non-blocking optional sync of login timestamp (fails silently without impeding login)
    try {
      val db = firestore
      if (db != null) {
        val loginData = mutableMapOf<String, Any>(
          "email" to userEmail,
          "role" to role.name,
          "active" to true,
          "lastLogin" to System.currentTimeMillis(),
          "updatedAt" to System.currentTimeMillis()
        )
        if (finalDisplayName.isNotBlank()) {
          loginData["displayName"] = finalDisplayName
          loginData["profileCompleted"] = true
        }
        db.collection("users").document(userId).set(loginData, SetOptions.merge())
      }
    } catch (e: Exception) {
      Log.w(tag, "Optional profile timestamp sync omitted: ${e.message}")
    }

    Log.i(tag, "[AUTH_DIAGNOSTIC] NAVIGATION_TO_HOME: Login complete for ${user.email} (${user.role.name})")
    Result.success(user)
  }

  suspend fun sendPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
    try {
      val trimmed = email.trim()
      if (trimmed.isBlank()) {
        return@withContext Result.failure(Exception("Please enter your registered email."))
      }
      if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
        return@withContext Result.failure(Exception("Please enter a valid email address."))
      }

      val auth = firebaseAuth
        ?: return@withContext Result.failure(Exception("Firebase service unavailable. Please check your connection."))

      auth.sendPasswordResetEmail(trimmed).await()
      Result.success("Password reset instructions have been sent to $trimmed. Please check your inbox.")
    } catch (e: FirebaseAuthInvalidUserException) {
      Result.failure(Exception("No account found with this email address."))
    } catch (e: FirebaseAuthException) {
      Result.failure(Exception(mapFirebaseErrorCode(e.errorCode)))
    } catch (e: Exception) {
      val msg = when {
        e.message?.contains("network", ignoreCase = true) == true -> "Please check your internet connection."
        else -> "Unable to send password reset email. Please verify the email and try again."
      }
      Result.failure(Exception(msg))
    }
  }

  fun logout() {
    val loggedInUser = _currentUser.value
    if (loggedInUser != null) {
      auditRepository.logUserLogout(
        email = loggedInUser.email,
        role = loggedInUser.role.name,
        userId = loggedInUser.id
      )
    }

    try {
      firebaseAuth?.signOut()
    } catch (e: Exception) {
      Log.w(tag, "SignOut warning: ${e.message}")
    }
    // Remove active session pointer but preserve user preferences per account (greeting_name_$userId, etc.)
    prefs.edit()
      .remove("active_session_uid")
      .remove("active_session_email")
      .apply()
    _currentUser.value = null
    _isSessionUnlocked.value = false
  }

  private fun mapFirebaseErrorCode(errorCode: String, message: String? = null): String {
    val msg = message?.lowercase(Locale.ROOT) ?: ""
    if (msg.contains("credential") || msg.contains("password") || msg.contains("expired") || msg.contains("malformed") || msg.contains("incorrect")) {
      return "Invalid email or password."
    }
    return when (errorCode) {
      "ERROR_INVALID_EMAIL", "invalid-email" -> "Please enter a valid email address."
      "ERROR_WRONG_PASSWORD", "wrong-password", "invalid-credential", "INVALID_LOGIN_CREDENTIALS" -> "Invalid email or password."
      "ERROR_USER_NOT_FOUND", "user-not-found" -> "Invalid email or password."
      "ERROR_USER_DISABLED", "user-disabled" -> "This account is disabled. Please contact the administrator."
      "ERROR_TOO_MANY_REQUESTS", "too-many-requests" -> "Too many failed attempts. Please try again later."
      "ERROR_NETWORK_REQUEST_FAILED", "network-request-failed" -> "Please check your internet connection."
      else -> "Authentication failed. Please check your credentials."
    }
  }

  fun getGreetingInfo(): GreetingInfo {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val (greeting, iconName) = when (hour) {
      in 5..11 -> Pair("Good Morning", "wb_sunny")
      in 12..16 -> Pair("Good Afternoon", "wb_twilight")
      in 17..20 -> Pair("Good Evening", "nights_stay")
      else -> Pair("Good Night", "bedtime")
    }
    return GreetingInfo(
      greeting = greeting,
      iconName = iconName
    )
  }
}
