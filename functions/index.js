/**
 * Firebase Cloud Functions Entry Point
 * Mangalam Fertilizer Management System
 */

const functions = require('firebase-functions');
const admin = require('firebase-admin');

if (!admin.apps.length) {
  admin.initializeApp();
}

const { executeAuditRetentionCleanup } = require('./auditRetentionPolicy');

/**
 * 1. SCHEDULED YEARLY AUDIT CLEANUP FUNCTION
 * Schedule: Runs once every year on January 30 at 02:00 UTC (0 2 30 1 *).
 * 
 * Safety:
 * - Before January 30, 2028: Returns immediately without deleting anything.
 * - After January 30, 2028: Deletes ONLY auditLogs older than retention cutoff in batches <= 400.
 */
exports.auditRetentionCleanup = functions.pubsub
  .schedule('0 2 30 1 *')
  .timeZone('UTC')
  .onRun(async (context) => {
    console.log('[CloudFunction] Starting scheduled audit retention cleanup job...');
    const result = await executeAuditRetentionCleanup({
      dryRun: false,
      triggeredBy: 'CLOUD_FUNCTION_SCHEDULER'
    });
    console.log('[CloudFunction] Scheduled cleanup completed:', result);
    return result;
  });

/**
 * 2. ADMIN SAFETY TEST / SIMULATION CALLABLE FUNCTION
 * Allows authenticated administrators to simulate cleanup runs for specific dates:
 * - "2028-01-29" (Simulate pre-first-run date -> tripwire returns 0 deleted)
 * - "2028-01-30" (Simulate first run date -> evaluates expired logs)
 * - "2028-01-31" (Simulate post-first-run date -> evaluates expired logs)
 * 
 * Always defaults to dryRun: true for maximum safety.
 */
exports.simulateAuditCleanup = functions.https.onCall(async (data, context) => {
  // Verify authenticated admin caller
  const adminEmails = [
    'kartik.bharadwaj0105@gmail.com',
    'admin.manglamferilizer@gmail.com'
  ];

  if (!context.auth || !context.auth.token || !context.auth.token.email) {
    throw new functions.https.HttpsError('unauthenticated', 'Admin authentication required.');
  }

  const callerEmail = context.auth.token.email.toLowerCase();
  if (!adminEmails.includes(callerEmail)) {
    throw new functions.https.HttpsError('permission-denied', 'Only authorized administrators can run simulations.');
  }

  const simulatedDateKey = data.simulatedDate || '2028-01-30';
  let simulatedTimestamp = null;

  if (simulatedDateKey === '2028-01-29') {
    simulatedTimestamp = 1832733600000; // 2028-01-29 02:00:00 UTC
  } else if (simulatedDateKey === '2028-01-30') {
    simulatedTimestamp = 1832820000000; // 2028-01-30 02:00:00 UTC
  } else if (simulatedDateKey === '2028-01-31') {
    simulatedTimestamp = 1832906400000; // 2028-01-31 02:00:00 UTC
  } else {
    simulatedTimestamp = new Date(simulatedDateKey).getTime();
  }

  const isDryRun = data.dryRun !== false; // defaults to true for safety

  const result = await executeAuditRetentionCleanup({
    dryRun: isDryRun,
    simulatedTimestamp,
    simulatedDate: simulatedDateKey,
    triggeredBy: `ADMIN_SIMULATION_TEST (${callerEmail})`
  });

  return result;
});

/**
 * 3. SERVER-SIDE AUDIT LOGGING TRIGGERS FOR SECURITY-CRITICAL OPERATIONS
 * Guarantees immutable server-trusted audit generation even if client app crashes.
 */

// Server audit on product deletion (Critical Inventory Event)
exports.onProductDeleted = functions.firestore
  .document('products/{productId}')
  .onDelete(async (snap, context) => {
    const deletedData = snap.data() || {};
    const productId = context.params.productId;
    const db = admin.firestore();

    const logId = `srv_log_prod_del_${Date.now()}_${productId.slice(-6)}`;
    await db.collection('auditLogs').doc(logId).set({
      logId,
      userId: deletedData.updatedBy || deletedData.createdBy || 'server_system',
      userEmail: deletedData.updatedBy || deletedData.createdBy || 'admin@manglamfertilizer.com',
      userRole: 'ADMIN',
      action: 'PRODUCT_DELETED_SERVER_VERIFIED',
      entityType: 'Product',
      entityId: productId,
      description: `[Server Verified] Product '${deletedData.name || productId}' was permanently deleted.`,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      clientTimestamp: Date.now(),
      deviceInstallationId: 'cloud_functions_engine',
      metadata: {
        productName: String(deletedData.name || ''),
        company: String(deletedData.company || ''),
        lastStock: String(deletedData.stockQuantity || '0'),
        sellingPrice: String(deletedData.sellingPrice || '0')
      }
    });
  });

// Server audit on invoice deletion (Critical Financial Event)
exports.onInvoiceDeleted = functions.firestore
  .document('invoices/{invoiceId}')
  .onDelete(async (snap, context) => {
    const deletedData = snap.data() || {};
    const invoiceId = context.params.invoiceId;
    const db = admin.firestore();

    const logId = `srv_log_inv_del_${Date.now()}_${invoiceId.slice(-6)}`;
    await db.collection('auditLogs').doc(logId).set({
      logId,
      userId: deletedData.createdBy || 'server_system',
      userEmail: deletedData.userEmail || deletedData.createdBy || 'admin@manglamfertilizer.com',
      userRole: 'ADMIN',
      action: 'INVOICE_DELETED_SERVER_VERIFIED',
      entityType: 'Invoice',
      entityId: invoiceId,
      description: `[Server Verified] Invoice #${deletedData.invoiceNumber || invoiceId} was deleted (Amount: ₹${deletedData.grandTotal || deletedData.amount || 0}).`,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      clientTimestamp: Date.now(),
      deviceInstallationId: 'cloud_functions_engine',
      metadata: {
        invoiceNumber: String(deletedData.invoiceNumber || ''),
        customerName: String(deletedData.customerName || ''),
        amount: String(deletedData.grandTotal || deletedData.amount || '0')
      }
    });
  });

// Server audit on settings modification (Critical System Configuration Event)
exports.onSettingsUpdated = functions.firestore
  .document('settings/{settingId}')
  .onWrite(async (change, context) => {
    const settingId = context.params.settingId;
    const db = admin.firestore();
    const afterData = change.after.exists ? change.after.data() : null;

    const logId = `srv_log_setting_${Date.now()}_${settingId.slice(-6)}`;
    await db.collection('auditLogs').doc(logId).set({
      logId,
      userId: afterData?.updatedBy || 'admin_user',
      userEmail: afterData?.updatedBy || 'admin.manglamferilizer@gmail.com',
      userRole: 'ADMIN',
      action: change.after.exists ? 'SETTING_UPDATED_SERVER_VERIFIED' : 'SETTING_DELETED_SERVER_VERIFIED',
      entityType: 'Settings',
      entityId: settingId,
      description: `[Server Verified] System configuration '${settingId}' was updated.`,
      timestamp: admin.firestore.FieldValue.serverTimestamp(),
      clientTimestamp: Date.now(),
      deviceInstallationId: 'cloud_functions_engine',
      metadata: {
        settingId: String(settingId),
        operation: change.after.exists ? 'UPDATE' : 'DELETE'
      }
    });
  });

