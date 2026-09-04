package com.manglamfertilizer.app.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manglamfertilizer.app.data.model.ReleaseType
import com.manglamfertilizer.app.data.repository.AppUpdateRepository
import com.manglamfertilizer.app.util.UpdateNotificationHelper

/**
 * Background WorkManager worker for performing periodic, battery-safe update checks
 * and issuing throttled daily reminder notifications when new versions or security patches are pending.
 */
class UpdateCheckWorker(
  private val context: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

  companion object {
    private const val TAG = "UpdateCheckWorker"
    const val WORK_NAME_PERIODIC = "manglam_periodic_update_check"
    const val WORK_NAME_ONE_TIME = "manglam_one_time_update_check"
  }

  override suspend fun doWork(): Result {
    Log.d(TAG, "UpdateCheckWorker started background check...")

    // 1. Network Connectivity Guard
    if (!isNetworkAvailable(context)) {
      Log.d(TAG, "Network unavailable during update check. Gracefully retrying later.")
      return Result.retry()
    }

    val repository = AppUpdateRepository.getInstance(context)

    return try {
      val result = repository.checkForUpdate(isManual = false)

      if (result.isSuccess) {
        val info = result.getOrNull()

        if (info != null && info.hasUpdate && info.manifest != null) {
          val manifest = info.manifest
          val isSilent = manifest.type == ReleaseType.SILENT && !info.isForced

          if (isSilent) {
            Log.d(TAG, "Silent release detected (v${manifest.versionName}). Skipping intrusive notification.")
          } else {
            // Check Daily Throttle: at most ONE notification per calendar day
            if (repository.canShowDailyReminder(manifest.versionCode)) {
              UpdateNotificationHelper.showUpdateNotification(
                context = context,
                manifest = manifest,
                isForced = info.isForced,
                isSecurityCritical = info.isSecurityCritical
              )
              repository.recordDailyReminderShown(manifest.versionCode)
              Log.d(TAG, "Daily update reminder notification posted for v${manifest.versionName}")
            } else {
              Log.d(TAG, "Daily update reminder already shown today for v${manifest.versionCode}. Throttled.")
            }
          }
        } else {
          // App is up to date or has no active update — cancel any lingering update notification
          UpdateNotificationHelper.cancelUpdateNotification(context)
          Log.d(TAG, "App is up to date (no pending update). Cleared notifications.")
        }

        Result.success()
      } else {
        val error = result.exceptionOrNull()
        Log.w(TAG, "Background update check failed: ${error?.message}")
        // Do not generate false notifications on failure. Retry with backoff.
        Result.retry()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during UpdateCheckWorker execution", e)
      Result.retry()
    }
  }

  private fun isNetworkAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
