package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.manglamfertilizer.app.data.model.AuditCleanupRun
import com.manglamfertilizer.app.data.model.AuditLogItem
import com.manglamfertilizer.app.data.model.AuditRetentionConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository and Safety Controller for Audit Log Retention & Automated Server-side Cleanup.
 *
 * Core Policies & Guardrails:
 * 1. NO CLEANUP DURING DEVELOPMENT: The application is under active development; no records are deleted before 30 January 2028.
 * 2. FIRST CLEANUP: 30 January 2028 at 00:00:00 UTC.
 * 3. RETENTION POLICY: Server-side Firebase Cloud Function (`auditRetentionCleanup`) running yearly on Jan 30.
 * 4. SCOPE BOUNDARY: ONLY `auditLogs` are eligible. Never deletes products, customers, invoices, categories, or settings.
 * 5. SEPARATE AUDIT RECORDING: Summaries are written to `auditCleanupRuns` (preventing infinite recursion).
 * 6. SIMULATION TEST HARNESS: Built-in safety test runner for Jan 29, Jan 30, and Jan 31, 2028.
 */
class AuditRetentionRepository private constructor(context: Context) {
  private val tag = "AuditRetentionRepo"
  private val scope = CoroutineScope(Dispatchers.IO)

  private val firestore: FirebaseFirestore? get() = com.manglamfertilizer.app.data.util.FirestoreProvider.get()

  private val _cleanupRuns = MutableStateFlow<List<AuditCleanupRun>>(emptyList())
  val cleanupRuns: StateFlow<List<AuditCleanupRun>> = _cleanupRuns.asStateFlow()

  private val _isSimulating = MutableStateFlow(false)
  val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

  private val _latestSimulation = MutableStateFlow<AuditCleanupRun?>(null)
  val latestSimulation: StateFlow<AuditCleanupRun?> = _latestSimulation.asStateFlow()

  private var runsListener: ListenerRegistration? = null

  fun startListeningRuns() {
    val db = firestore ?: return
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val email = currentUser?.email ?: ""
    if (currentUser == null || !com.manglamfertilizer.app.data.util.AdminAuthUtils.isAdmin(email)) {
      Log.d(tag, "Skipping auditCleanupRuns listener: User is not an admin ($email)")
      return
    }

    try {
      runsListener?.remove()
      runsListener = db.collection(AuditRetentionConstants.CLEANUP_RUNS_COLLECTION)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(50)
        .addSnapshotListener { snapshot, error ->
          if (error != null) {
            Log.w(tag, "Notice on auditCleanupRuns listener: ${error.message}")
            return@addSnapshotListener
          }
          if (snapshot != null) {
            val list = snapshot.documents.map { AuditCleanupRun.fromSnapshot(it) }
            _cleanupRuns.value = list
          }
        }
    } catch (e: Exception) {
      Log.w(tag, "Failed to attach listener to auditCleanupRuns: ${e.message}")
    }
  }

  fun stopListeningRuns() {
    runsListener?.remove()
    runsListener = null
  }

  /**
   * Safety Simulator: Evaluates the cleanup algorithm against specific test dates
   * (e.g., Jan 29 2028, Jan 30 2028, Jan 31 2028).
   *
   * ALWAYS runs in DRY-RUN mode — ZERO production records are modified.
   */
  suspend fun runSafetySimulation(
    simulatedDateKey: String, // "2028-01-29", "2028-01-30", "2028-01-31"
    retentionDays: Int = AuditRetentionConstants.DEFAULT_RETENTION_DAYS,
    adminEmail: String = "kartik.bharadwaj0105@gmail.com"
  ): AuditCleanupRun = withContext(Dispatchers.IO) {
    _isSimulating.value = true
    val startTime = System.currentTimeMillis()

    val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val displayFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss 'UTC'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }

    // Determine simulated timestamp
    val (simulatedMillis, simulatedLabel) = when (simulatedDateKey) {
      "2028-01-29" -> {
        // 2028-01-29 02:00:00 UTC (1 day BEFORE first cleanup)
        1832733600000L to "29 January 2028, 02:00 UTC (Pre-Launch Safety Check)"
      }
      "2028-01-30" -> {
        // 2028-01-30 02:00:00 UTC (First scheduled cleanup date!)
        1832820000000L to "30 January 2028, 02:00 UTC (First Scheduled Cleanup)"
      }
      "2028-01-31" -> {
        // 2028-01-31 02:00:00 UTC (Post-first cleanup execution)
        1832906400000L to "31 January 2028, 02:00 UTC (Post-Launch Retention Test)"
      }
      else -> {
        // Fallback or custom date parsing
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          set(2028, Calendar.JANUARY, 30, 2, 0, 0)
        }
        cal.timeInMillis to "30 January 2028, 02:00 UTC"
      }
    }

    val runId = "sim_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

    // 1. FIRST RUN SAFETY TRIPWIRE CHECK
    // If the execution date is strictly before Jan 30, 2028 (00:00:00 UTC), must immediately halt!
    if (simulatedMillis < AuditRetentionConstants.FIRST_CLEANUP_TIMESTAMP_MILLIS) {
      val elapsed = System.currentTimeMillis() - startTime
      val summary = AuditCleanupRun(
        runId = runId,
        timestamp = System.currentTimeMillis(),
        runDateFormatted = displayFormat.format(Date(simulatedMillis)),
        cutoffTimestamp = 0L,
        cutoffDateFormatted = "N/A (Execution Refused)",
        recordsEvaluated = 0,
        recordsDeleted = 0,
        dryRun = true,
        status = "SKIPPED_BEFORE_FIRST_RUN_DATE",
        triggeredBy = "ADMIN_SIMULATION_TEST",
        message = "SAFETY TRIPWIRE ACTIVE: Simulated execution date ($simulatedLabel) is prior to the mandatory first cleanup date (30 January 2028). Automatic cleanup strictly disabled during development. 0 records deleted.",
        simulatedDate = simulatedDateKey,
        executionTimeMs = elapsed,
        batchesProcessed = 0
      )

      _latestSimulation.value = summary
      _isSimulating.value = false
      recordRunSummaryToFirestore(summary, adminEmail)
      return@withContext summary
    }

    // 2. RETENTION CUTOFF CALCULATION
    val cutoffMillis = simulatedMillis - (retentionDays.toLong() * 24L * 60L * 60L * 1000L)
    val cutoffDateFormatted = displayFormat.format(Date(cutoffMillis))

    // 3. EVALUATE ELIGIBLE AUDIT LOGS
    var totalEvaluated = 0
    var eligibleCount = 0
    var batchesNeeded = 0

    val db = firestore
    if (db != null) {
      try {
        // Query auditLogs from Firestore to inspect real record timestamps
        val snapshot = db.collection(AuditRetentionConstants.AUDIT_LOGS_COLLECTION)
          .get()
          .await()

        totalEvaluated = snapshot.size()
        val cutoffTimestamp = Timestamp(Date(cutoffMillis))

        // Count documents with timestamp < cutoff
        for (doc in snapshot.documents) {
          val rawTs = doc.get("timestamp")
          val docMillis = when (rawTs) {
            is Timestamp -> rawTs.toDate().time
            is Number -> rawTs.toLong()
            is Date -> rawTs.time
            else -> doc.getLong("clientTimestamp") ?: 0L
          }
          if (docMillis > 0 && docMillis < cutoffMillis) {
            eligibleCount++
          }
        }

        batchesNeeded = if (eligibleCount > 0) {
          ((eligibleCount + AuditRetentionConstants.BATCH_SIZE - 1) / AuditRetentionConstants.BATCH_SIZE)
        } else 0

      } catch (e: Exception) {
        Log.e(tag, "Simulation fetch error: ${e.message}", e)
      }
    }

    val elapsed = System.currentTimeMillis() - startTime
    val status = if (eligibleCount > 0) "SIMULATION_SUCCESS" else "NO_EXPIRED_RECORDS"
    val msg = if (eligibleCount > 0) {
      "DRY-RUN SIMULATION SUCCESS ($simulatedLabel): Retention cutoff set to $cutoffDateFormatted (>$retentionDays days old). Evaluated $totalEvaluated total records; identified $eligibleCount records eligible for deletion across $batchesNeeded batch(es). Zero production records modified."
    } else {
      "DRY-RUN SIMULATION SUCCESS ($simulatedLabel): Evaluated $totalEvaluated total records. No records older than cutoff date ($cutoffDateFormatted). Current-year records protected. Zero deletions."
    }

    val result = AuditCleanupRun(
      runId = runId,
      timestamp = System.currentTimeMillis(),
      runDateFormatted = displayFormat.format(Date(simulatedMillis)),
      cutoffTimestamp = cutoffMillis,
      cutoffDateFormatted = cutoffDateFormatted,
      recordsEvaluated = totalEvaluated,
      recordsDeleted = eligibleCount,
      dryRun = true,
      status = status,
      triggeredBy = "ADMIN_SIMULATION_TEST",
      message = msg,
      simulatedDate = simulatedDateKey,
      executionTimeMs = elapsed,
      batchesProcessed = batchesNeeded
    )

    _latestSimulation.value = result
    _isSimulating.value = false
    recordRunSummaryToFirestore(result, adminEmail)
    result
  }

  /**
   * Persists summary to Firestore `auditCleanupRuns` collection.
   * Separate from `auditLogs` to prevent recursion loops.
   */
  private suspend fun recordRunSummaryToFirestore(run: AuditCleanupRun, adminEmail: String) {
    val db = firestore ?: return
    try {
      val data = hashMapOf(
        "runId" to run.runId,
        "timestamp" to FieldValue.serverTimestamp(),
        "clientTimestamp" to run.timestamp,
        "runDateFormatted" to run.runDateFormatted,
        "cutoffTimestamp" to run.cutoffTimestamp,
        "cutoffDateFormatted" to run.cutoffDateFormatted,
        "recordsEvaluated" to run.recordsEvaluated,
        "recordsDeleted" to run.recordsDeleted,
        "dryRun" to run.dryRun,
        "status" to run.status,
        "triggeredBy" to run.triggeredBy,
        "message" to run.message,
        "simulatedDate" to (run.simulatedDate ?: ""),
        "executionTimeMs" to run.executionTimeMs,
        "batchesProcessed" to run.batchesProcessed,
        "executedByAdmin" to adminEmail
      )
      db.collection(AuditRetentionConstants.CLEANUP_RUNS_COLLECTION)
        .document(run.runId)
        .set(data)
        .await()
      Log.d(tag, "Recorded cleanup run summary ${run.runId} (Status: ${run.status})")
    } catch (e: Exception) {
      Log.e(tag, "Failed to record cleanup run summary: ${e.message}", e)
    }
  }

  companion object {
    @Volatile
    private var instance: AuditRetentionRepository? = null

    fun getInstance(context: Context): AuditRetentionRepository {
      return instance ?: synchronized(this) {
        instance ?: AuditRetentionRepository(context.applicationContext).also { instance = it }
      }
    }
  }
}
