package com.manglamfertilizer.app.worker

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages WorkManager background scheduling for battery-safe, non-intrusive update checks.
 */
object UpdateWorkScheduler {
  private const val TAG = "UpdateWorkScheduler"

  /**
   * Schedules periodic background update checks using AndroidX WorkManager.
   * Runs every 12-24 hours when network is connected and battery is not low.
   */
  fun schedulePeriodicUpdateCheck(context: Context) {
    try {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

      val periodicWorkRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
        repeatInterval = 12,
        repeatIntervalTimeUnit = TimeUnit.HOURS,
        flexTimeInterval = 2,
        flexTimeIntervalUnit = TimeUnit.HOURS
      )
        .setConstraints(constraints)
        .setBackoffCriteria(
          BackoffPolicy.EXPONENTIAL,
          30,
          TimeUnit.MINUTES
        )
        .build()

      WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UpdateCheckWorker.WORK_NAME_PERIODIC,
        ExistingPeriodicWorkPolicy.KEEP,
        periodicWorkRequest
      )

      Log.d(TAG, "Periodic update check work enqueued successfully (12h cycle).")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to schedule periodic update check", e)
    }
  }

  /**
   * Triggers an immediate one-time background update check if network is available.
   */
  fun triggerImmediateCheck(context: Context) {
    try {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val oneTimeRequest = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
        .setConstraints(constraints)
        .build()

      WorkManager.getInstance(context).enqueueUniqueWork(
        UpdateCheckWorker.WORK_NAME_ONE_TIME,
        ExistingWorkPolicy.REPLACE,
        oneTimeRequest
      )

      Log.d(TAG, "Immediate update check work enqueued.")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to enqueue immediate update check", e)
    }
  }

  /**
   * Cancels all scheduled update check work.
   */
  fun cancelAll(context: Context) {
    try {
      WorkManager.getInstance(context).cancelUniqueWork(UpdateCheckWorker.WORK_NAME_PERIODIC)
      WorkManager.getInstance(context).cancelUniqueWork(UpdateCheckWorker.WORK_NAME_ONE_TIME)
      Log.d(TAG, "All background update work cancelled.")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to cancel update work", e)
    }
  }
}
