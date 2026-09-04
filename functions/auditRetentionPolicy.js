/**
 * Mangalam Fertilizer - Automated Audit Retention & Cleanup Policy Engine
 * 
 * CRITICAL SAFETY RULES:
 * 1. NO CLEANUP DURING DEVELOPMENT: No records are deleted prior to 30 January 2028.
 * 2. FIRST CLEANUP: 30 January 2028 (00:00:00 UTC).
 * 3. RETENTION PERIOD: Configurable (default 365 days / 1 year). Only audit logs older than (executionDate - retentionDays) are eligible.
 * 4. SCOPE RESTRICTION: ONLY `auditLogs` / `audit_logs` are deleted. NEVER products, customers, invoices, users, or settings.
 * 5. CONTROLLED BATCHING: Uses batches of <= 400 documents to avoid exceeding Firestore's 500-limit per transaction.
 * 6. AUDIT SUMMARY: Records run metadata to `auditCleanupRuns` collection (preventing infinite logging loops).
 */

const admin = require('firebase-admin');

// Mandatory first allowed cleanup timestamp (2028-01-30 00:00:00 UTC)
const FIRST_ALLOWED_CLEANUP_TIMESTAMP = 1832803200000;
const FIRST_ALLOWED_CLEANUP_ISO = '2028-01-30T00:00:00.000Z';
const DEFAULT_RETENTION_DAYS = 365;
const DEFAULT_BATCH_SIZE = 400;

/**
 * Executes the scheduled audit log retention cleanup process.
 * 
 * @param {Object} options
 * @param {boolean} [options.dryRun=false] - If true, evaluates records without deleting.
 * @param {number} [options.simulatedTimestamp=null] - For testing simulations.
 * @param {string} [options.triggeredBy='CLOUD_FUNCTION_SCHEDULER']
 * @returns {Promise<Object>} Summary of the cleanup run.
 */
async function executeAuditRetentionCleanup(options = {}) {
  const db = admin.firestore();
  const startTime = Date.now();

  const executionTimestamp = options.simulatedTimestamp || Date.now();
  const isDryRun = options.dryRun === true;
  const triggeredBy = options.triggeredBy || 'CLOUD_FUNCTION_SCHEDULER';
  const runId = `cleanup_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`;

  const executionDate = new Date(executionTimestamp);
  const executionDateFormatted = executionDate.toISOString().replace('T', ' ').substring(0, 19) + ' UTC';

  console.log(`[AuditCleanup] Starting run ${runId} (Triggered by: ${triggeredBy}, Exec Date: ${executionDateFormatted}, DryRun: ${isDryRun})`);

  // ----------------------------------------------------
  // 1. FIRST RUN SAFETY TRIPWIRE
  // ----------------------------------------------------
  if (executionTimestamp < FIRST_ALLOWED_CLEANUP_TIMESTAMP) {
    const elapsed = Date.now() - startTime;
    const summary = {
      runId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      clientTimestamp: Date.now(),
      runDateFormatted: executionDateFormatted,
      cutoffTimestamp: 0,
      cutoffDateFormatted: 'N/A (Execution Blocked)',
      recordsEvaluated: 0,
      recordsDeleted: 0,
      dryRun: isDryRun,
      status: 'SKIPPED_BEFORE_FIRST_RUN_DATE',
      triggeredBy,
      message: `SAFETY TRIPWIRE ACTIVE: Execution date (${executionDateFormatted}) is prior to the first scheduled cleanup date (30 January 2028). Automatic cleanup is strictly disabled during the development phase. 0 records deleted.`,
      simulatedDate: options.simulatedDate || null,
      executionTimeMs: elapsed,
      batchesProcessed: 0
    };

    console.warn(`[AuditCleanup] ${summary.message}`);
    await recordCleanupRunSummary(db, summary);
    return summary;
  }

  // ----------------------------------------------------
  // 2. READ CONFIGURATION & CALCULATE CUTOFF
  // ----------------------------------------------------
  let retentionDays = DEFAULT_RETENTION_DAYS;
  let batchSize = DEFAULT_BATCH_SIZE;

  try {
    const configDoc = await db.collection('settings').doc('auditRetentionConfig').get();
    if (configDoc.exists) {
      const data = configDoc.data();
      if (data.retentionDays && Number.isInteger(data.retentionDays) && data.retentionDays > 0) {
        retentionDays = data.retentionDays;
      }
      if (data.batchSize && Number.isInteger(data.batchSize) && data.batchSize > 0 && data.batchSize <= 450) {
        batchSize = data.batchSize;
      }
    }
  } catch (err) {
    console.warn('[AuditCleanup] Could not read auditRetentionConfig doc, using defaults', err.message);
  }

  const cutoffMillis = executionTimestamp - (retentionDays * 24 * 60 * 60 * 1000);
  const cutoffDate = new Date(cutoffMillis);
  const cutoffDateFormatted = cutoffDate.toISOString().replace('T', ' ').substring(0, 19) + ' UTC';
  const cutoffTimestamp = admin.firestore.Timestamp.fromDate(cutoffDate);

  console.log(`[AuditCleanup] Retention Period: ${retentionDays} days. Cutoff Date: ${cutoffDateFormatted}`);

  // ----------------------------------------------------
  // 3. QUERY EXPIRED AUDIT RECORDS (ONLY from auditLogs / audit_logs)
  // ----------------------------------------------------
  const collectionsToInspect = ['auditLogs', 'audit_logs'];
  let totalEvaluated = 0;
  let totalDeleted = 0;
  let totalBatches = 0;

  try {
    for (const collectionName of collectionsToInspect) {
      let hasMore = true;

      while (hasMore) {
        // Query records where timestamp < cutoffTimestamp
        const querySnapshot = await db.collection(collectionName)
          .where('timestamp', '<', cutoffTimestamp)
          .limit(batchSize)
          .get();

        if (querySnapshot.empty) {
          hasMore = false;
          break;
        }

        const countInBatch = querySnapshot.size;
        totalEvaluated += countInBatch;

        if (isDryRun) {
          // Dry-run mode: count without deleting
          totalDeleted += countInBatch;
          totalBatches++;
          // For dry-run, stop after first batch or calculate total without looping indefinitely
          hasMore = false;
        } else {
          // Controlled Batch Deletion
          const batch = db.batch();
          querySnapshot.docs.forEach(doc => {
            batch.delete(doc.ref);
          });

          await batch.commit();
          totalDeleted += countInBatch;
          totalBatches++;
          console.log(`[AuditCleanup] Successfully deleted batch #${totalBatches} (${countInBatch} docs) from ${collectionName}`);

          // If less than batch size returned, we are done with this collection
          if (countInBatch < batchSize) {
            hasMore = false;
          }
        }
      }
    }

    const elapsed = Date.now() - startTime;
    const status = totalDeleted > 0 ? (isDryRun ? 'SIMULATION_SUCCESS' : 'SUCCESS') : 'NO_EXPIRED_RECORDS';
    const message = isDryRun
      ? `DRY-RUN SIMULATION COMPLETED: ${totalEvaluated} records evaluated; ${totalDeleted} records eligible for retention deletion across ${totalBatches} batch(es). Zero production records altered.`
      : `CLEANUP SUCCESSFUL: Safely deleted ${totalDeleted} expired audit records older than ${cutoffDateFormatted} across ${totalBatches} batch(es).`;

    const summary = {
      runId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      clientTimestamp: Date.now(),
      runDateFormatted: executionDateFormatted,
      cutoffTimestamp: cutoffMillis,
      cutoffDateFormatted,
      recordsEvaluated: totalEvaluated,
      recordsDeleted: isDryRun ? 0 : totalDeleted,
      dryRun: isDryRun,
      status,
      triggeredBy,
      message,
      simulatedDate: options.simulatedDate || null,
      executionTimeMs: elapsed,
      batchesProcessed: totalBatches
    };

    await recordCleanupRunSummary(db, summary);
    console.log(`[AuditCleanup] Run ${runId} finished with status: ${status}. Deleted: ${summary.recordsDeleted}`);
    return summary;

  } catch (error) {
    const elapsed = Date.now() - startTime;
    console.error(`[AuditCleanup] Error during execution of run ${runId}:`, error);

    const errorSummary = {
      runId,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      clientTimestamp: Date.now(),
      runDateFormatted: executionDateFormatted,
      cutoffTimestamp: cutoffMillis,
      cutoffDateFormatted,
      recordsEvaluated: totalEvaluated,
      recordsDeleted: totalDeleted,
      dryRun: isDryRun,
      status: 'ERROR',
      triggeredBy,
      message: `CLEANUP ERROR: ${error.message}`,
      simulatedDate: options.simulatedDate || null,
      executionTimeMs: elapsed,
      batchesProcessed: totalBatches
    };

    await recordCleanupRunSummary(db, errorSummary);
    throw error;
  }
}

/**
 * Saves summary to dedicated `auditCleanupRuns` collection.
 */
async function recordCleanupRunSummary(db, summary) {
  try {
    await db.collection('auditCleanupRuns').doc(summary.runId).set(summary);
  } catch (err) {
    console.error('[AuditCleanup] Failed to persist cleanup run summary:', err);
  }
}

module.exports = {
  FIRST_ALLOWED_CLEANUP_TIMESTAMP,
  FIRST_ALLOWED_CLEANUP_ISO,
  DEFAULT_RETENTION_DAYS,
  DEFAULT_BATCH_SIZE,
  executeAuditRetentionCleanup
};
