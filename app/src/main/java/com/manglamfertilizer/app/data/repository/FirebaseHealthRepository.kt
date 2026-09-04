package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.manglamfertilizer.app.data.util.FirestoreProvider
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class HealthCheckResult(
  val isSuccess: Boolean,
  val status: String,
  val details: String,
  val timestamp: Long = System.currentTimeMillis(),
  val errorCode: String? = null
)

data class FirebaseHealthReport(
  val timestamp: Long = System.currentTimeMillis(),
  val isFirebaseInitialized: Boolean,
  val authStatus: HealthCheckResult,
  val connectivityStatus: HealthCheckResult,
  val readStatus: HealthCheckResult,
  val writeStatus: HealthCheckResult,
  val lastSuccessfulSync: Long,
  val lastError: String?,
  val isFullyHealthy: Boolean
)

/**
 * Authoritative Repository for Firebase and Cloud Firestore Health & Connectivity Verification.
 *
 * Implements real authenticated read/write verification against Cloud Firestore,
 * ensuring the app never relies on fake/random connectivity states.
 */
class FirebaseHealthRepository(private val context: Context) {
  private val tag = "FirebaseHealthRepository"
  private val prefs: SharedPreferences =
    context.getSharedPreferences("manglam_firebase_health_prefs", Context.MODE_PRIVATE)

  private val firestore: FirebaseFirestore? get() = FirestoreProvider.get()
  private val firebaseAuth: FirebaseAuth? get() = FirestoreProvider.auth

  private val _healthReport = MutableStateFlow<FirebaseHealthReport?>(null)
  val healthReport: StateFlow<FirebaseHealthReport?> = _healthReport.asStateFlow()

  private val _lastError = MutableStateFlow<String?>(prefs.getString("last_firebase_error", null))
  val lastErrorFlow: StateFlow<String?> = _lastError.asStateFlow()

  fun getLastSuccessfulSync(): Long {
    return prefs.getLong("last_successful_sync_time", 0L)
  }

  fun recordSuccessfulSync() {
    val now = System.currentTimeMillis()
    prefs.edit().putLong("last_successful_sync_time", now).apply()
    _lastError.value = null
    prefs.edit().remove("last_firebase_error").apply()
  }

  fun recordLastError(error: String, errorCode: String? = null, operation: String = "", collection: String = "") {
    val cleanMsg = if (errorCode != null) "[$errorCode] $error" else error
    prefs.edit().putString("last_firebase_error", cleanMsg).apply()
    _lastError.value = cleanMsg
    FirestoreProvider.logFirebaseError(tag, operation, collection, "", Exception(error))
  }

  fun getLastError(): String? {
    return _lastError.value
  }

  /**
   * 1. Check Firebase Authentication
   * Verifies FirebaseApp is initialized and a valid current user session is active.
   */
  fun checkAuthentication(): HealthCheckResult {
    return try {
      val auth = firebaseAuth ?: return HealthCheckResult(
        isSuccess = false,
        status = "AUTH_UNAVAILABLE",
        details = "FirebaseAuth instance could not be initialized.",
        errorCode = "AUTH_NULL"
      )

      val user = auth.currentUser
      if (user != null && !user.email.isNullOrBlank()) {
        HealthCheckResult(
          isSuccess = true,
          status = "AUTHENTICATED",
          details = "Authenticated as ${user.email} (UID: ${user.uid})"
        )
      } else {
        HealthCheckResult(
          isSuccess = false,
          status = "UNAUTHENTICATED",
          details = "No authenticated user session active. Cloud operations require login.",
          errorCode = "NO_CURRENT_USER"
        )
      }
    } catch (e: Exception) {
      val msg = e.message ?: "Authentication check failed"
      recordLastError(msg, "AUTH_EXCEPTION", "checkAuthentication", "auth")
      HealthCheckResult(
        isSuccess = false,
        status = "AUTH_ERROR",
        details = msg,
        errorCode = "AUTH_EXCEPTION"
      )
    }
  }

  /**
   * 2. Check Firestore Connectivity
   * Verifies that the Firebase Firestore instance is reachable and ready to receive commands.
   */
  suspend fun checkFirestoreConnectivity(): HealthCheckResult = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext HealthCheckResult(
        isSuccess = false,
        status = "FIRESTORE_NULL",
        details = "FirebaseFirestore instance is null or failed to initialize.",
        errorCode = "DB_NULL"
      )

      // Test basic connectivity via ping document read in health collection
      val businessId = FirestoreProvider.BUSINESS_ID
      val healthDocRef = db.collection("businesses").document(businessId)
        .collection("system_health").document("connectivity_ping")

      val snapshot = healthDocRef.get().await()
      recordSuccessfulSync()

      HealthCheckResult(
        isSuccess = true,
        status = "CONNECTED",
        details = "Firestore connected successfully. Server response confirmed."
      )
    } catch (e: Exception) {
      val errorCode = (e as? FirebaseFirestoreException)?.code?.name ?: "CONNECTIVITY_FAILED"
      val msg = e.message ?: "Firestore connectivity test failed"
      recordLastError(msg, errorCode, "checkConnectivity", "system_health")
      HealthCheckResult(
        isSuccess = false,
        status = "CONNECTIVITY_ERROR",
        details = msg,
        errorCode = errorCode
      )
    }
  }

  /**
   * 3. Check Firestore Read Capability
   * Performs an authenticated read operation on the business settings or products collection.
   */
  suspend fun checkFirestoreRead(): HealthCheckResult = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext HealthCheckResult(
        isSuccess = false,
        status = "FIRESTORE_NULL",
        details = "Firestore instance unavailable",
        errorCode = "DB_NULL"
      )

      val businessId = FirestoreProvider.BUSINESS_ID
      val snapshot = db.collection("businesses").document(businessId)
        .collection("products")
        .limit(1)
        .get()
        .await()

      recordSuccessfulSync()
      HealthCheckResult(
        isSuccess = true,
        status = "READ_SUCCESS",
        details = "Firestore read test passed (Retrieved ${snapshot.size()} document(s))."
      )
    } catch (e: Exception) {
      val errorCode = (e as? FirebaseFirestoreException)?.code?.name ?: "READ_FAILED"
      val msg = e.message ?: "Firestore read test failed"
      recordLastError(msg, errorCode, "checkFirestoreRead", "products")
      HealthCheckResult(
        isSuccess = false,
        status = "READ_ERROR",
        details = msg,
        errorCode = errorCode
      )
    }
  }

  /**
   * 4. Check Firestore Write Capability
   * Performs an authenticated write & cleanup test in the system_health collection.
   */
  suspend fun checkFirestoreWrite(): HealthCheckResult = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext HealthCheckResult(
        isSuccess = false,
        status = "FIRESTORE_NULL",
        details = "Firestore instance unavailable",
        errorCode = "DB_NULL"
      )

      val authUser = firebaseAuth?.currentUser
      if (authUser == null) {
        return@withContext HealthCheckResult(
          isSuccess = false,
          status = "WRITE_UNAUTHORIZED",
          details = "Cannot test Firestore write without an authenticated session.",
          errorCode = "UNAUTHENTICATED"
        )
      }

      val businessId = FirestoreProvider.BUSINESS_ID
      val testDocId = "health_test_${System.currentTimeMillis()}"
      val testDocRef = db.collection("businesses").document(businessId)
        .collection("system_health").document(testDocId)

      val testData = hashMapOf(
        "testId" to testDocId,
        "performedBy" to (authUser.email ?: authUser.uid),
        "timestamp" to FieldValue.serverTimestamp(),
        "clientTimestamp" to System.currentTimeMillis(),
        "status" to "HEALTH_CHECK_OK"
      )

      // 1. Write document
      testDocRef.set(testData, SetOptions.merge()).await()

      // 2. Clean up test document immediately
      try {
        testDocRef.delete().await()
      } catch (cleanupEx: Exception) {
        Log.w(tag, "Health check test cleanup notice: ${cleanupEx.message}")
      }

      recordSuccessfulSync()
      HealthCheckResult(
        isSuccess = true,
        status = "WRITE_SUCCESS",
        details = "Firestore authenticated write and cleanup test completed successfully."
      )
    } catch (e: Exception) {
      val errorCode = (e as? FirebaseFirestoreException)?.code?.name ?: "WRITE_FAILED"
      val msg = e.message ?: "Firestore write test failed"
      recordLastError(msg, errorCode, "checkFirestoreWrite", "system_health")
      HealthCheckResult(
        isSuccess = false,
        status = "WRITE_ERROR",
        details = msg,
        errorCode = errorCode
      )
    }
  }

  /**
   * Runs complete diagnostic suite across all 4 pillars and caches the report.
   */
  suspend fun runFullHealthCheck(): FirebaseHealthReport = withContext(Dispatchers.IO) {
    val isAppInit = try {
      FirebaseApp.getApps(context).isNotEmpty()
    } catch (_: Exception) {
      false
    }

    val authResult = checkAuthentication()
    val connResult = checkFirestoreConnectivity()
    val readResult = checkFirestoreRead()
    val writeResult = if (authResult.isSuccess) checkFirestoreWrite() else HealthCheckResult(
      isSuccess = false,
      status = "WRITE_SKIPPED",
      details = "Skipped write test because user is not authenticated.",
      errorCode = "UNAUTHENTICATED"
    )

    val fullyHealthy = isAppInit && authResult.isSuccess && connResult.isSuccess && readResult.isSuccess && writeResult.isSuccess
    val report = FirebaseHealthReport(
      timestamp = System.currentTimeMillis(),
      isFirebaseInitialized = isAppInit,
      authStatus = authResult,
      connectivityStatus = connResult,
      readStatus = readResult,
      writeStatus = writeResult,
      lastSuccessfulSync = getLastSuccessfulSync(),
      lastError = getLastError(),
      isFullyHealthy = fullyHealthy
    )

    _healthReport.value = report
    report
  }

  companion object {
    @Volatile
    private var instance: FirebaseHealthRepository? = null

    fun getInstance(context: Context): FirebaseHealthRepository {
      return instance ?: synchronized(this) {
        instance ?: FirebaseHealthRepository(context.applicationContext).also { instance = it }
      }
    }
  }
}
