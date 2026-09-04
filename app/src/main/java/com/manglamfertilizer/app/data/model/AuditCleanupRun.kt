package com.manglamfertilizer.app.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Summary record generated whenever the server-side audit retention policy executes
 * or when an admin runs a safety simulation in test mode.
 *
 * Stored in dedicated collection `auditCleanupRuns` to strictly avoid infinite audit logging loops.
 */
@Keep
data class AuditCleanupRun(
  val runId: String = "",
  val timestamp: Long = System.currentTimeMillis(),
  val runDateFormatted: String = "",
  val cutoffTimestamp: Long = 0L,
  val cutoffDateFormatted: String = "",
  val recordsEvaluated: Int = 0,
  val recordsDeleted: Int = 0,
  val dryRun: Boolean = true,
  val status: String = "SUCCESS", // SUCCESS, SKIPPED_BEFORE_FIRST_RUN_DATE, NO_EXPIRED_RECORDS, SIMULATION_SUCCESS, ERROR
  val triggeredBy: String = "CLOUD_FUNCTION_SCHEDULER", // CLOUD_FUNCTION_SCHEDULER, ADMIN_SIMULATION_TEST, DEV_HARNESS
  val message: String = "",
  val simulatedDate: String? = null,
  val executionTimeMs: Long = 0L,
  val batchesProcessed: Int = 0
) {
  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): AuditCleanupRun {
      val rawTs = doc.get("timestamp")
      val ts = when (rawTs) {
        is Timestamp -> rawTs.toDate().time
        is Number -> rawTs.toLong()
        is Date -> rawTs.time
        else -> 0L
      }
      val rawCutoff = doc.get("cutoffTimestamp")
      val cutoffTs = when (rawCutoff) {
        is Timestamp -> rawCutoff.toDate().time
        is Number -> rawCutoff.toLong()
        is Date -> rawCutoff.time
        else -> 0L
      }

      return AuditCleanupRun(
        runId = doc.getString("runId") ?: doc.id,
        timestamp = ts,
        runDateFormatted = doc.getString("runDateFormatted") ?: "",
        cutoffTimestamp = cutoffTs,
        cutoffDateFormatted = doc.getString("cutoffDateFormatted") ?: "",
        recordsEvaluated = (doc.getLong("recordsEvaluated") ?: 0L).toInt(),
        recordsDeleted = (doc.getLong("recordsDeleted") ?: 0L).toInt(),
        dryRun = doc.getBoolean("dryRun") ?: true,
        status = doc.getString("status") ?: "SUCCESS",
        triggeredBy = doc.getString("triggeredBy") ?: "CLOUD_FUNCTION_SCHEDULER",
        message = doc.getString("message") ?: "",
        simulatedDate = doc.getString("simulatedDate"),
        executionTimeMs = doc.getLong("executionTimeMs") ?: 0L,
        batchesProcessed = (doc.getLong("batchesProcessed") ?: 0L).toInt()
      )
    }
  }
}

/**
 * Immutable system constants and configurable retention policy guidelines for MANGALAM FERTILIZER.
 */
object AuditRetentionConstants {
  // CRITICAL RULE: First scheduled cleanup is January 30, 2028 (00:00:00 UTC).
  // Under development: NO CLEANUP BEFORE THIS DATE.
  const val FIRST_CLEANUP_DATE_STR = "2028-01-30T00:00:00Z"
  const val FIRST_CLEANUP_TIMESTAMP_MILLIS = 1832803200000L // 2028-01-30 00:00:00 UTC

  // Default Retention Period: 365 Days (1 Year). Older records than (Execution Date - Retention Days) are eligible.
  const val DEFAULT_RETENTION_DAYS = 365

  // Batch Size: Up to 400 documents per batch commit (safely within Firestore 500 max limit).
  const val BATCH_SIZE = 400

  // Server collection names
  const val AUDIT_LOGS_COLLECTION = "audit_logs"
  const val CLEANUP_RUNS_COLLECTION = "auditCleanupRuns"
  const val SETTINGS_COLLECTION = "settings"
  const val RETENTION_CONFIG_DOC = "auditRetentionConfig"

  // Cloud schedule: Runs once every year around January 30 at 02:00 UTC
  const val CRON_SCHEDULE = "0 2 30 1 *"
  const val TIMEZONE = "UTC"
}
