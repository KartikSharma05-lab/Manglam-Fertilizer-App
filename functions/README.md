# Mangalam Fertilizer - Firebase Cloud Functions
## Automated Audit Log Retention & Yearly Cleanup Policy

### Overview
This package contains the automated server-side retention cleanup for Mangalam Fertilizer Firestore audit collections.

### Policy Rules & Safety Constraints
1. **No Cleanup During Development**: Strictly forbidden to delete records before **30 January 2028**.
2. **First Cleanup Date**: **30 January 2028 (00:00:00 UTC)**.
3. **Execution Cadence**: Runs once every year on **January 30** at `02:00 UTC` via Google Cloud Scheduler (`0 2 30 1 *`).
4. **Scope Restriction**: Deletes **ONLY** records from the `auditLogs` (and `audit_logs`) collection. Core business collections (`products`, `customers`, `invoices`, `categories`, `users`, `settings`) are completely untouched.
5. **Retention Window**: Configurable in `/settings/auditRetentionConfig` (default: **365 days / 1 year**).
6. **Controlled Batches**: Batches are strictly limited to `400 documents` per commit to respect Firestore transaction limits.
7. **Dedicated Summaries**: Writes execution logs to `auditCleanupRuns` rather than `auditLogs`, preventing infinite logging feedback loops.

### Deployment Instructions
```bash
# Install Firebase CLI if needed
npm install -g firebase-tools

# Login to Firebase
firebase login

# Deploy Cloud Functions
firebase deploy --only functions
```

### Safety Simulation Modes (Available in App & Cloud Function)
- **2028-01-29**: Tests tripwire guardrail prior to first launch (returns 0 deleted with `SKIPPED_BEFORE_FIRST_RUN_DATE`).
- **2028-01-30**: Tests first active run date, applying 365-day cutoff to expired records in dry-run mode.
- **2028-01-31**: Tests post-launch retention evaluation in dry-run mode.
