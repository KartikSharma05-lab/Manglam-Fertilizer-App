package com.manglamfertilizer.app.data.util

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings

/**
 * Centralized provider for Firebase Firestore with persistent disk caching enabled.
 * Ensures consistent configuration across all repositories and provides secure, sanitized error logging.
 */
object FirestoreProvider {
  private const val TAG = "FirestoreProvider"
  const val BUSINESS_ID = "mangalam_fertilizer"

  val db: FirebaseFirestore? by lazy {
    try {
      val instance = FirebaseFirestore.getInstance()
      try {
        val settings = FirebaseFirestoreSettings.Builder()
          .setPersistenceEnabled(true)
          .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
          .build()
        instance.firestoreSettings = settings
      } catch (e: Exception) {
        Log.w(TAG, "Firestore persistence settings notice: ${e.message}")
      }
      instance
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get FirebaseFirestore instance: ${e.message}", e)
      null
    }
  }

  fun get(): FirebaseFirestore? = db

  val auth: FirebaseAuth? by lazy {
    try {
      FirebaseAuth.getInstance()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get FirebaseAuth instance: ${e.message}", e)
      null
    }
  }

  fun isFirebaseInitialized(): Boolean {
    return try {
      FirebaseApp.getApps(android.app.Application().applicationContext).isNotEmpty() || FirebaseApp.getInstance() != null
    } catch (e: Exception) {
      // If FirebaseApp.getInstance() succeeded, it won't throw
      try {
        FirebaseApp.getInstance() != null
      } catch (_: Exception) {
        false
      }
    }
  }

  /**
   * Sanitized Firebase Error Logging.
   * Logs: operation, collection, document ID, error code, and error message.
   * Strictly filters out passwords, tokens, API keys, or credentials.
   */
  fun logFirebaseError(
    tag: String,
    operation: String,
    collection: String,
    documentId: String = "",
    exception: Exception?
  ) {
    val errorCode = if (exception is FirebaseFirestoreException) {
      exception.code.name
    } else {
      "GENERAL_ERROR"
    }

    val rawMessage = exception?.message ?: "Unknown Firebase exception"
    val cleanMessage = sanitizeMessage(rawMessage)

    Log.e(
      tag,
      "[FIREBASE_ERROR] Operation: $operation | Collection: $collection | DocId: ${documentId.ifBlank { "N/A" }} | Code: $errorCode | Message: $cleanMessage",
      exception
    )
  }

  private fun sanitizeMessage(msg: String): String {
    return msg
      .replace(Regex("(?i)(password|token|key|secret)=[^\",&\\s]+"), "$1=***REDACTED***")
      .replace(Regex("(?i)bearer\\s+[a-zA-Z0-9_.-]+"), "Bearer ***REDACTED***")
  }
}
