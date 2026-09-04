package com.manglamfertilizer.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.manglamfertilizer.app.MainActivity
import com.manglamfertilizer.app.R
import com.manglamfertilizer.app.data.model.UpdateManifest

object UpdateNotificationHelper {
  private const val TAG = "UpdateNotification"
  const val CHANNEL_ID = "manglam_app_updates_channel"
  const val CHANNEL_NAME = "App Updates"
  const val NOTIFICATION_ID = 9001

  const val EXTRA_OPEN_UPDATE = "EXTRA_OPEN_UPDATE"
  const val EXTRA_UPDATE_FORCED = "EXTRA_UPDATE_FORCED"
  const val EXTRA_VERSION_CODE = "EXTRA_VERSION_CODE"

  /**
   * Initializes the Android Notification Channel for App Updates.
   */
  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Notifications for Manglam Fertilizer application updates and security patches"
        setShowBadge(true)
        enableVibration(true)
      }

      notificationManager.createNotificationChannel(channel)
    }
  }

  /**
   * Displays the update notification if permission is granted and requirements are met.
   * Handles Android 13+ runtime POST_NOTIFICATIONS safely without failing if denied.
   */
  fun showUpdateNotification(
    context: Context,
    manifest: UpdateManifest,
    isForced: Boolean,
    isSecurityCritical: Boolean = false
  ) {
    createNotificationChannel(context)

    // Check Android 13+ (API 33) notification permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!hasPermission) {
        Log.d(TAG, "POST_NOTIFICATIONS permission not granted. Skipping system tray notification.")
        return
      }
    }

    try {
      val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_OPEN_UPDATE, true)
        putExtra(EXTRA_UPDATE_FORCED, isForced || isSecurityCritical)
        putExtra(EXTRA_VERSION_CODE, manifest.versionCode)
      }

      val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

      val title = if (isForced || isSecurityCritical) {
        "Manglam Fertilizer Update Required"
      } else {
        "Manglam Fertilizer Update Available"
      }

      val body = if (isForced || isSecurityCritical) {
        "Please update to continue using the app."
      } else {
        "Version ${manifest.versionName} is available. Tap to update."
      }

      val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_notify_sync)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(
          if (isForced || isSecurityCritical) NotificationCompat.PRIORITY_HIGH
          else NotificationCompat.PRIORITY_DEFAULT
        )
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .build()

      NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
      Log.d(TAG, "Update notification posted successfully for version v${manifest.versionName}")
    } catch (e: SecurityException) {
      Log.w(TAG, "SecurityException while posting notification (Permission denied): ${e.message}")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to post update notification", e)
    }
  }

  /**
   * Cancels any pending or active update notification from the system tray.
   */
  fun cancelUpdateNotification(context: Context) {
    try {
      NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
      Log.d(TAG, "Update notification cancelled.")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to cancel update notification: ${e.message}")
    }
  }
}
