package com.manglamfertilizer.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.ReleaseHistoryItem
import com.manglamfertilizer.app.data.model.ReleaseType
import com.manglamfertilizer.app.data.model.UpdateEngineState
import com.manglamfertilizer.app.data.model.UpdateManifest
import com.manglamfertilizer.app.data.model.VerificationResult
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.util.ApkVerifier
import com.manglamfertilizer.app.util.UpdateNotificationHelper
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

data class GitHubStatusInfo(
  val isConnected: Boolean = true,
  val repository: String = "KartikSharma05-lab/Manglam-Fertilizer-App",
  val branch: String = "main",
  val manifestEndpoint: String = "",
  val lastCheckedTime: Long = 0L,
  val responseCode: Int = 200,
  val message: String = "Connected to GitHub Release Channel"
) {
  val isGreen: Boolean get() = isConnected && responseCode in 200..299
  val isYellow: Boolean get() = isConnected && (responseCode == 404 || responseCode in 300..399)
  val isRed: Boolean get() = !isConnected || responseCode >= 400
}

class AppUpdateRepository(private val context: Context) {
  private val auditRepository = AuditRepository.getInstance(context)

  companion object {
    private const val TAG = "AppUpdateRepository"
    private const val PREFS_NAME = "manglam_update_prefs"
    private const val KEY_CUSTOM_MANIFEST_URL = "custom_manifest_url"
    private const val KEY_LAST_CHECK_TIME = "last_check_timestamp"
    private const val KEY_RELEASE_HISTORY = "release_history_json"
    private const val KEY_LOCAL_PUBLISHED_MANIFEST = "local_published_manifest_json"
    private const val KEY_CACHED_REMOTE_MANIFEST = "cached_remote_manifest_json"
    private const val KEY_LAST_TRUSTED_NETWORK_TIME = "last_trusted_network_time"
    private const val KEY_LAST_TRUSTED_TIME_ELAPSED = "last_trusted_time_elapsed"
    private const val KEY_PENDING_INSTALL_VERSION_CODE = "pending_install_version_code"
    private const val KEY_PENDING_INSTALL_VERSION_NAME = "pending_install_version_name"

    // Default GitHub Releases Update Manifest endpoint
    const val DEFAULT_MANIFEST_URL =
      "https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/latest/download/update.json"

    @Volatile
    private var instance: AppUpdateRepository? = null

    fun getInstance(context: Context): AppUpdateRepository {
      return instance ?: synchronized(this) {
        instance ?: AppUpdateRepository(context.applicationContext).also { instance = it }
      }
    }
  }

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
  private val manifestAdapter = moshi.adapter(UpdateManifest::class.java)
  private val historyListAdapter = moshi.adapter<List<ReleaseHistoryItem>>(
    Types.newParameterizedType(List::class.java, ReleaseHistoryItem::class.java)
  )

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

  private var activeDownloadCall: Call? = null
  private var activeDownloadJob: Job? = null

  private val _updateInfo = MutableStateFlow(
    AppUpdateInfo(
      installedVersionName = getInstalledVersionName(),
      installedVersionCode = getInstalledVersionCode(),
      installedPackageName = context.packageName
    )
  )
  val updateInfo: StateFlow<AppUpdateInfo> = _updateInfo.asStateFlow()

  private val _releaseHistory = MutableStateFlow<List<ReleaseHistoryItem>>(emptyList())
  val releaseHistory: StateFlow<List<ReleaseHistoryItem>> = _releaseHistory.asStateFlow()

  private val _githubStatus = MutableStateFlow(
    GitHubStatusInfo(
      manifestEndpoint = getManifestUrl(),
      lastCheckedTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    )
  )
  val githubStatus: StateFlow<GitHubStatusInfo> = _githubStatus.asStateFlow()

  init {
    checkPostInstallSuccess()
    loadReleaseHistory()
    checkCachedApk()
  }

  /**
   * Post-Installation Verification:
   * Verifies if installedVersionCode >= pendingInstallVersionCode.
   * If yes, logs UPDATE_COMPLETED, clears cached APKs, cancels notification, and resets state.
   */
  fun checkPostInstallSuccess() {
    val pendingVersionCode = prefs.getLong(KEY_PENDING_INSTALL_VERSION_CODE, 0L)
    val pendingVersionName = prefs.getString(KEY_PENDING_INSTALL_VERSION_NAME, "") ?: ""
    val installedVersionCode = getInstalledVersionCode()
    val installedVersionName = getInstalledVersionName()

    if (pendingVersionCode > 0L) {
      if (installedVersionCode >= pendingVersionCode) {
        Log.i(TAG, "Post-install verification passed: installed $installedVersionCode (v$installedVersionName) >= expected $pendingVersionCode")
        auditRepository.logUpdateCompleted(
          version = installedVersionName,
          versionCode = installedVersionCode,
          userEmail = "",
          userRole = "SYSTEM"
        )
        prefs.edit()
          .remove(KEY_PENDING_INSTALL_VERSION_CODE)
          .remove(KEY_PENDING_INSTALL_VERSION_NAME)
          .apply()

        clearUpdateCache()
        UpdateNotificationHelper.cancelUpdateNotification(context)

        _updateInfo.update {
          it.copy(
            state = UpdateEngineState.UPDATE_COMPLETED,
            statusMessage = "Successfully updated to v$installedVersionName."
          )
        }
      }
    }
  }

  fun getManifestUrl(): String {
    return prefs.getString(KEY_CUSTOM_MANIFEST_URL, DEFAULT_MANIFEST_URL) ?: DEFAULT_MANIFEST_URL
  }

  fun setManifestUrl(url: String, userEmail: String = "", userRole: String = "ADMIN", userId: String = "") {
    val oldUrl = getManifestUrl()
    val cleanUrl = url.trim()
    if (oldUrl != cleanUrl) {
      prefs.edit().putString(KEY_CUSTOM_MANIFEST_URL, cleanUrl).apply()
      _githubStatus.update { it.copy(manifestEndpoint = cleanUrl) }
      auditRepository.logUpdateConfigurationChanged(
        oldUrl = oldUrl,
        newUrl = cleanUrl,
        userEmail = userEmail,
        userRole = userRole,
        userId = userId
      )
    }
  }

  fun resetManifestUrl() {
    prefs.edit().remove(KEY_CUSTOM_MANIFEST_URL).apply()
    _githubStatus.update { it.copy(manifestEndpoint = DEFAULT_MANIFEST_URL) }
  }

  fun getInstalledVersionName(): String {
    return try {
      val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
      pInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
      "1.0.0"
    }
  }

  fun getInstalledVersionCode(): Long {
    return try {
      val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pInfo.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        pInfo.versionCode.toLong()
      }
    } catch (e: Exception) {
      1L
    }
  }

  private fun getUpdatesDir(): File {
    val dir = File(context.cacheDir, "updates")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  private fun getApkTargetFile(versionCode: Long): File {
    return File(getUpdatesDir(), "ManglamFertilizer-v${versionCode}.apk")
  }

  // --- Network-Derived Time & Anti Clock-Manipulation ---

  /**
   * Updates trusted reference time from HTTP Date header or network timestamp.
   */
  private fun updateTrustedNetworkTime(networkTimeMillis: Long) {
    if (networkTimeMillis > 0L) {
      val elapsedRealtime = android.os.SystemClock.elapsedRealtime()
      prefs.edit()
        .putLong(KEY_LAST_TRUSTED_NETWORK_TIME, networkTimeMillis)
        .putLong(KEY_LAST_TRUSTED_TIME_ELAPSED, elapsedRealtime)
        .apply()
    }
  }

  /**
   * Returns estimated current time using monotonic elapsedRealtime offset against
   * the last verified network time, preventing device clock tampering.
   */
  fun getCurrentTrustedTimeMillis(): Long {
    val lastNetworkTime = prefs.getLong(KEY_LAST_TRUSTED_NETWORK_TIME, 0L)
    val lastElapsed = prefs.getLong(KEY_LAST_TRUSTED_TIME_ELAPSED, 0L)
    val nowElapsed = android.os.SystemClock.elapsedRealtime()

    return if (lastNetworkTime > 0L && lastElapsed > 0L && nowElapsed >= lastElapsed) {
      val delta = nowElapsed - lastElapsed
      lastNetworkTime + delta
    } else {
      System.currentTimeMillis()
    }
  }

  /**
   * Parses publishedAt timestamp string into epoch milliseconds.
   * Supports ISO 8601 (2026-08-28T02:00:00Z, 2026-08-28T02:00:00+05:30),
   * Standard Date (yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm 'IST', yyyy-MM-dd),
   * Natural Date Strings (e.g., "26 August 2026", "26 Aug 2026", "26-08-2026").
   */
  fun parsePublishedAtToMillis(publishedAt: String): Long {
    if (publishedAt.isBlank()) return 0L
    val clean = publishedAt.trim()

    val formats = listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ss",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd HH:mm 'IST'",
      "yyyy-MM-dd HH:mm",
      "yyyy-MM-dd",
      "dd MMMM yyyy HH:mm:ss",
      "dd MMMM yyyy HH:mm 'IST'",
      "dd MMMM yyyy HH:mm",
      "dd MMMM yyyy",
      "d MMMM yyyy",
      "dd MMM yyyy HH:mm:ss",
      "dd MMM yyyy HH:mm 'IST'",
      "dd MMM yyyy HH:mm",
      "dd MMM yyyy",
      "d MMM yyyy",
      "dd-MM-yyyy HH:mm:ss",
      "dd-MM-yyyy HH:mm",
      "dd-MM-yyyy",
      "dd/MM/yyyy"
    )

    for (pattern in formats) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
        if (pattern.endsWith("'Z'")) {
          sdf.timeZone = TimeZone.getTimeZone("UTC")
        } else if (pattern.contains("'IST'")) {
          sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        val date = sdf.parse(clean)
        if (date != null) {
          return date.time
        }
      } catch (_: Exception) {
        // Try next format
      }
    }
    return 0L
  }

  /**
   * Authoritative Release Age Calculator:
   * Calculates elapsed calendar days between manifest.publishedAt and the current trusted time
   * using consistent Indian Standard Time (Asia/Kolkata, UTC+05:30) calendar day boundaries.
   *
   * Example:
   * Published: 26 August 2026
   * 26 Aug -> Day 0
   * 27 Aug -> Day 1
   * ...
   * 09 Sep -> Day 14
   * 10 Sep -> Day 15 (Grace period expired -> Forced Update Required)
   */
  fun calculateCalendarDaysSincePublication(publishedAtStr: String, currentTrustedMillis: Long): Int {
    val pubMillis = parsePublishedAtToMillis(publishedAtStr)
    if (pubMillis <= 0L) return 0

    val tz = TimeZone.getTimeZone("Asia/Kolkata")
    val pubCal = Calendar.getInstance(tz).apply { timeInMillis = pubMillis }
    val nowCal = Calendar.getInstance(tz).apply { timeInMillis = currentTrustedMillis }

    // Normalize both calendar dates to midnight (00:00:00.000) in Asia/Kolkata
    pubCal.set(Calendar.HOUR_OF_DAY, 0)
    pubCal.set(Calendar.MINUTE, 0)
    pubCal.set(Calendar.SECOND, 0)
    pubCal.set(Calendar.MILLISECOND, 0)

    nowCal.set(Calendar.HOUR_OF_DAY, 0)
    nowCal.set(Calendar.MINUTE, 0)
    nowCal.set(Calendar.SECOND, 0)
    nowCal.set(Calendar.MILLISECOND, 0)

    val diffMillis = nowCal.timeInMillis - pubCal.timeInMillis
    val days = (diffMillis / (1000L * 60 * 60 * 24L)).toInt()
    return days.coerceAtLeast(0)
  }

  /**
   * Calculates the grace period publication baseline timestamp.
   * Uses manifest.publishedAt as authoritative source, with fallback to persistent first-seen.
   */
  private fun getEffectivePublicationTimestamp(manifest: UpdateManifest): Long {
    val parsedPublishedAt = parsePublishedAtToMillis(manifest.publishedAt)
    val key = "update_first_seen_v_${manifest.versionCode}"
    var storedTimestamp = prefs.getLong(key, 0L)

    if (parsedPublishedAt > 0L) {
      if (storedTimestamp != parsedPublishedAt) {
        prefs.edit().putLong(key, parsedPublishedAt).apply()
      }
      return parsedPublishedAt
    }

    if (storedTimestamp <= 0L) {
      storedTimestamp = getCurrentTrustedTimeMillis()
      prefs.edit().putLong(key, storedTimestamp).apply()
    }
    return storedTimestamp
  }

  // --- Skip / Later Dismissal & Daily Reminder Logic ---

  /**
   * Checks if the update reminder was already dismissed for today.
   */
  fun isDismissedForToday(versionCode: Long): Boolean {
    val key = "update_dismissed_v_$versionCode"
    val lastDismissed = prefs.getLong(key, 0L)
    if (lastDismissed <= 0L) return false
    val now = getCurrentTrustedTimeMillis()
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return sdf.format(Date(now)) == sdf.format(Date(lastDismissed))
  }

  /**
   * Records user choosing Skip / Later.
   * Persisted in SharedPreferences so it survives app restarts, device reboots, and logout/login.
   */
  fun recordDismissed(versionCode: Long) {
    val key = "update_dismissed_v_$versionCode"
    val now = getCurrentTrustedTimeMillis()
    prefs.edit().putLong(key, now).apply()
    _updateInfo.update { it.copy(isDismissedForToday = true) }
  }

  /**
   * Checks whether a daily notification/reminder can be shown today.
   * Limits reminders to at most once per day per version.
   */
  fun canShowDailyReminder(versionCode: Long): Boolean {
    val key = "update_last_reminder_v_$versionCode"
    val lastReminder = prefs.getLong(key, 0L)
    if (lastReminder <= 0L) return true
    val now = getCurrentTrustedTimeMillis()
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return sdf.format(Date(now)) != sdf.format(Date(lastReminder))
  }

  /**
   * Marks that the daily reminder notification has been displayed today.
   */
  fun recordDailyReminderShown(versionCode: Long) {
    val key = "update_last_reminder_v_$versionCode"
    val now = getCurrentTrustedTimeMillis()
    prefs.edit().putLong(key, now).apply()
  }

  // --- Manifest Validation ---

  /**
   * Strictly validates update manifest attributes.
   * Returns VerificationResult.Success or VerificationResult.Failed with descriptive reason.
   */
  fun validateManifest(manifest: UpdateManifest, installedVersionCode: Long): VerificationResult {
    val expectedPackage = context.packageName

    // 1. Package Name
    if (manifest.packageName.isBlank()) {
      return VerificationResult.Failed("Malformed manifest: Missing package name.")
    }
    if (manifest.packageName != expectedPackage) {
      return VerificationResult.Failed(
        "Manifest package name (${manifest.packageName}) does not match installed app ID ($expectedPackage)."
      )
    }

    // 2. Version Name
    if (manifest.versionName.isBlank()) {
      return VerificationResult.Failed("Malformed manifest: Missing versionName.")
    }

    // 3. Version Code
    if (manifest.versionCode <= 0L) {
      return VerificationResult.Failed("Malformed manifest: Invalid versionCode (${manifest.versionCode}).")
    }

    // 4. Release Type
    val typeStr = manifest.releaseType.trim().uppercase()
    val validTypes = setOf("OPTIONAL", "RECOMMENDED", "FORCED", "SILENT", "CRITICAL", "SECURITY_CRITICAL", "SECURITY-CRITICAL", "LOW_IMPACT")
    if (typeStr.isBlank() || typeStr !in validTypes) {
      return VerificationResult.Failed("Malformed manifest: Unknown release type '${manifest.releaseType}'.")
    }

    // 5. APK Download URL
    if (manifest.apkUrl.isBlank() || (!manifest.apkUrl.startsWith("http://") && !manifest.apkUrl.startsWith("https://"))) {
      return VerificationResult.Failed("Malformed manifest: Invalid APK URL '${manifest.apkUrl}'.")
    }

    // 6. SHA-256 Checksum
    val sha = manifest.sha256.trim()
    if (sha.isNotBlank() && (sha.length != 64 || !sha.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' })) {
      return VerificationResult.Failed("Malformed manifest: Invalid SHA-256 checksum format.")
    }

    return VerificationResult.Success
  }

  // --- Cached APK and Local Manifest Handlers ---

  /**
   * Checks if an APK was already downloaded and verified on disk.
   */
  private fun checkCachedApk() {
    val currentInfo = _updateInfo.value
    val manifest = currentInfo.manifest ?: getLocalPublishedManifest() ?: getCachedRemoteManifest() ?: return
    val targetFile = getApkTargetFile(manifest.versionCode)

    if (targetFile.exists() && targetFile.length() > 0) {
      val verification = ApkVerifier.verifyApk(
        context = context,
        apkFile = targetFile,
        manifest = manifest,
        installedVersionCode = currentInfo.installedVersionCode
      )
      if (verification is VerificationResult.Success) {
        val pubTimestamp = getEffectivePublicationTimestamp(manifest)
        val now = getCurrentTrustedTimeMillis()
        val daysSincePublished = calculateCalendarDaysSincePublication(manifest.publishedAt, now)
        val forceAfterDays = manifest.forceAfterDays ?: 15
        val isGraceExpired = daysSincePublished >= forceAfterDays

        _updateInfo.update {
          it.copy(
            state = UpdateEngineState.READY_TO_INSTALL,
            manifest = manifest,
            verifiedApkFile = targetFile,
            firstSeenTimestamp = pubTimestamp,
            daysSinceFirstSeen = daysSincePublished,
            isGracePeriodExpired = isGraceExpired,
            statusMessage = "Update v${manifest.versionName} ready to install."
          )
        }
      }
    }
  }

  private fun getLocalPublishedManifest(): UpdateManifest? {
    val json = prefs.getString(KEY_LOCAL_PUBLISHED_MANIFEST, null) ?: return null
    return try {
      manifestAdapter.fromJson(json)
    } catch (e: Exception) {
      null
    }
  }

  private fun getCachedRemoteManifest(): UpdateManifest? {
    val json = prefs.getString(KEY_CACHED_REMOTE_MANIFEST, null) ?: return null
    return try {
      manifestAdapter.fromJson(json)
    } catch (e: Exception) {
      null
    }
  }

  private fun saveCachedRemoteManifest(manifest: UpdateManifest) {
    try {
      val json = manifestAdapter.toJson(manifest)
      prefs.edit().putString(KEY_CACHED_REMOTE_MANIFEST, json).apply()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to cache remote manifest", e)
    }
  }

  // --- Primary Update Checker ---

  /**
   * Checks GitHub Release manifest or custom endpoint for available updates.
   * Uses versionCode as authoritative comparison, enforces 15-day grace period from publishedAt,
   * handles clock tamper prevention, and validates manifest integrity strictly.
   */
  suspend fun checkForUpdate(
    manifestUrl: String = getManifestUrl(),
    isManual: Boolean = false,
    userEmail: String = "",
    userRole: String = "ADMIN"
  ): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
    val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    val nowMillis = getCurrentTrustedTimeMillis()
    val isRecentlyChecked = (nowMillis - lastCheckTime) < (15 * 60 * 1000L)

    // If automatic check and checked less than 15 mins ago, reuse cached state without network hit
    if (!isManual && isRecentlyChecked && _updateInfo.value.lastCheckedTimestamp > 0L) {
      return@withContext Result.success(_updateInfo.value)
    }

    val installedVersionCode = getInstalledVersionCode()
    val installedVersionName = getInstalledVersionName()
    val installedPackageName = context.packageName

    _updateInfo.update {
      it.copy(
        state = UpdateEngineState.CHECKING,
        isChecking = true,
        installedVersionName = installedVersionName,
        installedVersionCode = installedVersionCode,
        installedPackageName = installedPackageName,
        statusMessage = "Checking for updates...",
        errorMessage = null
      )
    }

    try {
      val localManifest = getLocalPublishedManifest()
      val cachedRemoteManifest = getCachedRemoteManifest()
      var manifestToUse: UpdateManifest? = null
      var httpStatus = 200

      // Priority 1: Check admin-published local manifest if newer
      if (localManifest != null && localManifest.versionCode > installedVersionCode) {
        manifestToUse = localManifest
      } else {
        // Priority 2: Query remote manifest from GitHub Release endpoint
        val request = Request.Builder()
          .url(manifestUrl)
          .header("Cache-Control", "no-cache")
          .header("Accept", "application/json")
          .build()

        val response = httpClient.newCall(request).execute()
        httpStatus = response.code

        // Capture network time from HTTP Date header for anti-tamper clock sync
        val dateHeader = response.header("Date")
        if (!dateHeader.isNullOrBlank()) {
          try {
            val parsedDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).parse(dateHeader)
            if (parsedDate != null) {
              updateTrustedNetworkTime(parsedDate.time)
            }
          } catch (_: Exception) {
            // Ignore date header parse failure
          }
        }

        if (!response.isSuccessful) {
          if (localManifest != null && localManifest.versionCode > installedVersionCode) {
            manifestToUse = localManifest
          } else if (response.code == 404) {
            // 404 means no release manifest published on GitHub yet
            val now = getCurrentTrustedTimeMillis()
            prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()
            _githubStatus.update {
              it.copy(
                isConnected = true,
                lastCheckedTime = now,
                responseCode = 404,
                message = "Connected to GitHub Releases • Up to date"
              )
            }
            UpdateNotificationHelper.cancelUpdateNotification(context)
            val info = _updateInfo.value.copy(
              state = UpdateEngineState.NO_UPDATE,
              isChecking = false,
              lastCheckedTimestamp = now,
              statusMessage = "You are using the latest version (v$installedVersionName).",
              errorMessage = null
            )
            _updateInfo.value = info
            return@withContext Result.success(info)
          } else {
            // If network/server error, fallback to valid cached remote manifest if available
            if (cachedRemoteManifest != null && cachedRemoteManifest.versionCode > installedVersionCode) {
              manifestToUse = cachedRemoteManifest
            } else {
              throw IOException("Failed to fetch update manifest: HTTP ${response.code}")
            }
          }
        } else {
          val responseBody = response.body?.string()
            ?: throw IOException("Empty response from update server")

          val parsedManifest = try {
            manifestAdapter.fromJson(responseBody)
              ?: throw IOException("Failed to parse update manifest JSON")
          } catch (e: Exception) {
            localManifest ?: cachedRemoteManifest ?: throw IOException("Invalid update manifest format: ${e.localizedMessage}")
          }

          // Strict validation on fetched remote manifest
          val validation = validateManifest(parsedManifest, installedVersionCode)
          if (validation is VerificationResult.Failed) {
            Log.e(TAG, "Rejected malformed manifest: ${validation.reason}")
            // Do NOT overwrite good cache with bad manifest; try fallback
            if (cachedRemoteManifest != null && cachedRemoteManifest.versionCode > installedVersionCode) {
              manifestToUse = cachedRemoteManifest
            } else if (localManifest != null && localManifest.versionCode > installedVersionCode) {
              manifestToUse = localManifest
            } else {
              throw SecurityException(validation.reason)
            }
          } else {
            manifestToUse = parsedManifest
            saveCachedRemoteManifest(parsedManifest)
          }
        }
      }

      val manifest = manifestToUse ?: throw IOException("No valid update manifest found")

      // Secondary Validation Check on whatever manifest is chosen
      val chosenValidation = validateManifest(manifest, installedVersionCode)
      if (chosenValidation is VerificationResult.Failed) {
        throw SecurityException(chosenValidation.reason)
      }

      val now = getCurrentTrustedTimeMillis()
      prefs.edit().putLong(KEY_LAST_CHECK_TIME, now).apply()

      _githubStatus.update {
        it.copy(
          isConnected = true,
          lastCheckedTime = now,
          responseCode = httpStatus,
          message = "Connected to GitHub Releases • Latest Manifest Active"
        )
      }

      // Authoritative comparison: Remote Version Code vs Installed Version Code
      val remoteVersionCode = manifest.versionCode
      if (remoteVersionCode <= installedVersionCode) {
        // App is already at or ahead of remote release
        UpdateNotificationHelper.cancelUpdateNotification(context)
        val info = _updateInfo.value.copy(
          state = UpdateEngineState.NO_UPDATE,
          isChecking = false,
          manifest = manifest,
          lastCheckedTimestamp = now,
          statusMessage = "You are using the latest version (v$installedVersionName).",
          errorMessage = null,
          isSimulation = false
        )
        _updateInfo.value = info
        return@withContext Result.success(info)
      }

      // Calculate 15-Day Grace Period from publication timestamp (manifest.publishedAt is authoritative)
      val pubTimestamp = getEffectivePublicationTimestamp(manifest)
      val daysSincePublished = calculateCalendarDaysSincePublication(manifest.publishedAt, now)
      val forceAfterDays = manifest.forceAfterDays ?: 15
      val isGracePeriodExpired = daysSincePublished >= forceAfterDays
      val isDismissedToday = isDismissedForToday(manifest.versionCode)

      // Minimum Supported Version Code Check
      val minSupportedCode = manifest.minimumSupportedVersionCode ?: 0L
      val isBelowMinSupported = minSupportedCode > 0 && installedVersionCode < minSupportedCode

      // Security Critical Check
      val isSecurityCritical = manifest.type == ReleaseType.CRITICAL

      // Check if an APK is already downloaded and verified on disk
      val cachedApk = getApkTargetFile(manifest.versionCode)
      var isAlreadyDownloadedAndVerified = false

      if (cachedApk.exists() && cachedApk.length() > 0) {
        val verification = ApkVerifier.verifyApk(
          context = context,
          apkFile = cachedApk,
          manifest = manifest,
          installedVersionCode = installedVersionCode
        )
        if (verification is VerificationResult.Success) {
          isAlreadyDownloadedAndVerified = true
        }
      }

      // Mandatory / Forced update criteria
      val isEffectivelyForced = manifest.isExplicitlyForced ||
          isSecurityCritical ||
          isBelowMinSupported ||
          isGracePeriodExpired

      val newState = when {
        isAlreadyDownloadedAndVerified -> UpdateEngineState.READY_TO_INSTALL
        isEffectivelyForced -> UpdateEngineState.FORCED_UPDATE_REQUIRED
        else -> UpdateEngineState.UPDATE_AVAILABLE
      }

      val statusMsg = when {
        isAlreadyDownloadedAndVerified -> "Update ready to install."
        isSecurityCritical -> "Security update required immediately (v${manifest.versionName})."
        isBelowMinSupported -> "Security update required (v${manifest.versionName})."
        isGracePeriodExpired -> "Update mandatory (15-day grace period ended)."
        manifest.isExplicitlyForced -> "Mandatory update required (v${manifest.versionName})."
        manifest.isSilent -> "Optional minor update available (v${manifest.versionName})."
        else -> "New update v${manifest.versionName} is available."
      }

      val info = _updateInfo.value.copy(
        state = newState,
        isChecking = false,
        manifest = manifest,
        verifiedApkFile = if (isAlreadyDownloadedAndVerified) cachedApk else null,
        lastCheckedTimestamp = now,
        statusMessage = statusMsg,
        errorMessage = null,
        firstSeenTimestamp = pubTimestamp,
        daysSinceFirstSeen = daysSincePublished,
        isGracePeriodExpired = isGracePeriodExpired,
        isDismissedForToday = isDismissedToday,
        isSimulation = false
      )
      _updateInfo.value = info

      auditRepository.logUpdateChecked(
        manifestVersion = manifest.versionName,
        versionCode = manifest.versionCode,
        isManual = isManual,
        userEmail = userEmail,
        userRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else userRole
      )

      Result.success(info)

    } catch (e: UnknownHostException) {
      Log.w(TAG, "Network unavailable when checking for updates", e)
      updateGitHubStatusError("Network offline. Unable to check for updates.")
      handleCheckError("Unable to check for updates. Please check your internet connection.", isManual)
    } catch (e: SocketTimeoutException) {
      Log.w(TAG, "Update server timeout", e)
      updateGitHubStatusError("Connection timeout with GitHub Releases.")
      handleCheckError("Unable to check for updates. Connection timed out.", isManual)
    } catch (e: Exception) {
      Log.w(TAG, "Update check error: ${e.message}")
      updateGitHubStatusError(e.localizedMessage ?: "Unable to connect to update channel")
      handleCheckError(e.localizedMessage ?: "Unable to check for updates. Please try again later.", isManual)
    }
  }

  private fun updateGitHubStatusError(msg: String) {
    _githubStatus.update {
      it.copy(
        isConnected = false,
        responseCode = 500,
        message = msg
      )
    }
  }

  private fun handleCheckError(message: String, isManual: Boolean): Result<AppUpdateInfo> {
    val previousState = _updateInfo.value.state
    val fallbackState = if (previousState == UpdateEngineState.CHECKING) {
      // If we have an existing update or ready-to-install APK, preserve it
      if (_updateInfo.value.manifest != null && _updateInfo.value.hasUpdate) {
        previousState
      } else {
        UpdateEngineState.NO_UPDATE
      }
    } else {
      previousState
    }

    val info = _updateInfo.value.copy(
      state = fallbackState,
      isChecking = false,
      errorMessage = message,
      statusMessage = message
    )
    _updateInfo.value = info
    return Result.failure(Exception(message))
  }

  // --- Download and Install Engine ---

  /**
   * Downloads the release APK with progress reporting and checksum verification.
   */
  fun startDownload(scope: CoroutineScope) {
    val manifest = _updateInfo.value.manifest ?: return
    if (manifest.apkUrl.isBlank()) {
      _updateInfo.update {
        it.copy(
          state = UpdateEngineState.DOWNLOAD_FAILED,
          errorMessage = "No valid download URL provided in release manifest."
        )
      }
      return
    }

    activeDownloadJob?.cancel()
    activeDownloadJob = scope.launch(Dispatchers.IO) {
      downloadApkInternal(manifest)
    }
  }

  fun cancelDownload() {
    activeDownloadCall?.cancel()
    activeDownloadJob?.cancel()
    _updateInfo.update {
      it.copy(
        state = UpdateEngineState.DOWNLOAD_PAUSED,
        statusMessage = "Download paused."
      )
    }
  }

  private suspend fun downloadApkInternal(manifest: UpdateManifest) = withContext(Dispatchers.IO) {
    val targetFile = getApkTargetFile(manifest.versionCode)
    val tempFile = File(getUpdatesDir(), "update_${manifest.versionCode}.download")

    _updateInfo.update {
      it.copy(
        state = UpdateEngineState.DOWNLOADING,
        downloadProgress = 0f,
        downloadedBytes = 0L,
        totalBytes = 0L,
        statusMessage = "Starting download...",
        errorMessage = null
      )
    }

    auditRepository.logUpdateStarted(
      version = manifest.versionName,
      versionCode = manifest.versionCode,
      userEmail = "",
      userRole = "ADMIN"
    )

    try {
      val request = Request.Builder()
        .url(manifest.apkUrl)
        .build()

      val call = httpClient.newCall(request)
      activeDownloadCall = call
      val response = call.execute()

      if (!response.isSuccessful) {
        throw IOException("Download failed with HTTP ${response.code}")
      }

      val body = response.body ?: throw IOException("Empty response body from download URL")
      val totalLength = body.contentLength()

      if (tempFile.exists()) {
        tempFile.delete()
      }

      var downloadedBytes = 0L
      val buffer = ByteArray(8192)

      body.byteStream().use { input ->
        FileOutputStream(tempFile).use { output ->
          var lastReportTime = 0L

          while (isActive) {
            val bytesRead = input.read(buffer)
            if (bytesRead == -1) break

            output.write(buffer, 0, bytesRead)
            downloadedBytes += bytesRead

            val currentTime = System.currentTimeMillis()
            if (currentTime - lastReportTime > 200 || downloadedBytes == totalLength) {
              lastReportTime = currentTime
              val progress = if (totalLength > 0) {
                (downloadedBytes.toFloat() / totalLength).coerceIn(0f, 1f)
              } else {
                0f
              }

              _updateInfo.update {
                it.copy(
                  downloadProgress = progress,
                  downloadedBytes = downloadedBytes,
                  totalBytes = totalLength,
                  statusMessage = "Downloading update (${(progress * 100).toInt()}%)..."
                )
              }
            }
          }
        }
      }

      if (!isActive) {
        tempFile.delete()
        _updateInfo.update { it.copy(state = UpdateEngineState.DOWNLOAD_PAUSED) }
        return@withContext
      }

      // Rename temp file to target APK
      if (targetFile.exists()) {
        targetFile.delete()
      }
      if (!tempFile.renameTo(targetFile)) {
        throw IOException("Failed to save downloaded APK to destination cache")
      }

      // Verification Step
      _updateInfo.update {
        it.copy(
          state = UpdateEngineState.VERIFYING,
          statusMessage = "Verifying package integrity and signatures..."
        )
      }

      val verification = ApkVerifier.verifyApk(
        context = context,
        apkFile = targetFile,
        manifest = manifest,
        installedVersionCode = getInstalledVersionCode()
      )

      when (verification) {
        is VerificationResult.Success -> {
          _updateInfo.update {
            it.copy(
              state = UpdateEngineState.READY_TO_INSTALL,
              verifiedApkFile = targetFile,
              downloadProgress = 1f,
              statusMessage = "Update ready to install.",
              errorMessage = null
            )
          }
          auditRepository.logUpdateDownloaded(
            version = manifest.versionName,
            fileSize = targetFile.length(),
            userEmail = "",
            userRole = "ADMIN"
          )
        }
        is VerificationResult.Failed -> {
          targetFile.delete()
          _updateInfo.update {
            it.copy(
              state = UpdateEngineState.VERIFICATION_FAILED,
              verifiedApkFile = null,
              errorMessage = verification.reason,
              statusMessage = "Update verification failed."
            )
          }
          auditRepository.logUpdateVerificationFailed(
            version = manifest.versionName,
            reason = verification.reason,
            userEmail = "",
            userRole = "ADMIN"
          )
        }
      }

    } catch (e: CancellationException) {
      tempFile.delete()
      _updateInfo.update {
        it.copy(
          state = UpdateEngineState.DOWNLOAD_PAUSED,
          statusMessage = "Download paused"
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Download error", e)
      tempFile.delete()
      _updateInfo.update {
        it.copy(
          state = UpdateEngineState.DOWNLOAD_FAILED,
          errorMessage = e.localizedMessage ?: "Failed to download update APK.",
          statusMessage = "Download failed."
        )
      }
      auditRepository.logUpdateFailed(
        version = manifest.versionName,
        errorCode = "DOWNLOAD_FAILED",
        errorReason = e.localizedMessage ?: "Download failed",
        userEmail = "",
        userRole = "ADMIN"
      )
    } finally {
      activeDownloadCall = null
    }
  }

  /**
   * Triggers the standard Android system package installer with user authorization.
   */
  fun installUpdate(context: Context): Result<Unit> {
    val currentInfo = _updateInfo.value
    val apkFile = currentInfo.verifiedApkFile

    if (apkFile == null || !apkFile.exists()) {
      return Result.failure(IllegalStateException("No verified update APK file available for installation"))
    }

    return try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
          val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(settingsIntent)
          return Result.failure(
            SecurityException("Please allow permission to install updates from this source.")
          )
        }
      }

      val apkUri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val expectedVersionCode = currentInfo.manifest?.versionCode ?: 0L
      val expectedVersionName = currentInfo.manifest?.versionName ?: ""
      if (expectedVersionCode > 0L) {
        prefs.edit()
          .putLong(KEY_PENDING_INSTALL_VERSION_CODE, expectedVersionCode)
          .putString(KEY_PENDING_INSTALL_VERSION_NAME, expectedVersionName)
          .apply()
      }

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      context.startActivity(installIntent)

      UpdateNotificationHelper.cancelUpdateNotification(context)

      _updateInfo.update {
        it.copy(
          state = UpdateEngineState.INSTALLING,
          statusMessage = "Installing update..."
        )
      }

      auditRepository.logUpdateInstalled(
        version = currentInfo.manifest?.versionName ?: "unknown",
        versionCode = currentInfo.manifest?.versionCode ?: 0L,
        userEmail = "",
        userRole = "ADMIN"
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer", e)
      _updateInfo.update {
        it.copy(
          state = UpdateEngineState.INSTALL_FAILED,
          errorMessage = "Installation failed: ${e.localizedMessage}"
        )
      }
      auditRepository.logUpdateFailed(
        version = currentInfo.manifest?.versionName ?: "unknown",
        errorCode = "INSTALL_FAILED",
        errorReason = e.localizedMessage ?: "Installation failed",
        userEmail = "",
        userRole = "ADMIN"
      )
      Result.failure(e)
    }
  }

  /**
   * Dismisses non-forced update dialog/state for the remainder of today.
   */
  fun dismissUpdate() {
    val current = _updateInfo.value
    val manifest = current.manifest
    if (manifest != null && !current.isForced) {
      recordDismissed(manifest.versionCode)
    }
    _updateInfo.update {
      it.copy(
        errorMessage = null,
        isDismissedForToday = true
      )
    }
  }

  // --- Admin Release Management & History Engine ---

  private fun loadReleaseHistory() {
    val json = prefs.getString(KEY_RELEASE_HISTORY, null)
    val history = if (!json.isNullOrBlank()) {
      try {
        historyListAdapter.fromJson(json) ?: getDefaultSeedHistory()
      } catch (e: Exception) {
        getDefaultSeedHistory()
      }
    } else {
      getDefaultSeedHistory()
    }
    _releaseHistory.value = history
  }

  private fun getDefaultSeedHistory(): List<ReleaseHistoryItem> {
    return listOf(
      ReleaseHistoryItem(
        versionName = "1.0.0",
        versionCode = 1L,
        releaseType = "OPTIONAL",
        releaseTitle = "Production Baseline Release",
        releaseNotes = "• Initial stable production release of ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} Dealer ERP\n• Bilingual Hindi/English billing & inventory management",
        publishedAt = "2026-08-01 09:00 IST",
        publishedBy = "admin.manglamferilizer@gmail.com",
        forceAfterDays = 15,
        status = "Production Release",
        apkUrl = "https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v1.0.0/ManglamFertilizer-v1.0.0.apk"
      )
    )
  }

  private fun saveReleaseHistory(list: List<ReleaseHistoryItem>) {
    _releaseHistory.value = list
    try {
      val json = historyListAdapter.toJson(list)
      prefs.edit().putString(KEY_RELEASE_HISTORY, json).apply()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save release history", e)
    }
  }

  /**
   * Admin Action: Save Release Configuration locally for staging / verification.
   * Note: Production releases are built & published by GitHub Actions CI/CD.
   */
  fun publishRelease(manifest: UpdateManifest, adminEmail: String): Result<Unit> {
    return try {
      val validation = validateManifest(manifest, getInstalledVersionCode())
      if (validation is VerificationResult.Failed) {
        return Result.failure(IllegalArgumentException(validation.reason))
      }

      val manifestJson = manifestAdapter.toJson(manifest)
      prefs.edit().putString(KEY_LOCAL_PUBLISHED_MANIFEST, manifestJson).apply()

      // Record published timestamp
      val pubTimestamp = getEffectivePublicationTimestamp(manifest)

      // Add to release history
      val nowFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm 'IST'", Locale.getDefault()).format(Date())
      val historyItem = ReleaseHistoryItem(
        versionName = manifest.versionName,
        versionCode = manifest.versionCode,
        releaseType = manifest.releaseType,
        releaseTitle = manifest.releaseTitle.ifBlank { "Version ${manifest.versionName}" },
        releaseNotes = manifest.releaseNotes,
        publishedAt = manifest.publishedAt.ifBlank { nowFormatted },
        publishedBy = adminEmail,
        forceAfterDays = manifest.forceAfterDays ?: 15,
        status = "Staged Configuration",
        apkUrl = manifest.apkUrl
      )

      val currentList = _releaseHistory.value.toMutableList()
      currentList.removeAll { it.versionCode == manifest.versionCode }
      currentList.add(0, historyItem)
      saveReleaseHistory(currentList)

      // Refresh update state
      val installedVersionCode = getInstalledVersionCode()
      val isNewer = manifest.versionCode > installedVersionCode
      val minSupportedCode = manifest.minimumSupportedVersionCode ?: 0L
      val isBelowMinSupported = minSupportedCode > 0 && installedVersionCode < minSupportedCode
      val isEffectivelyForced = manifest.isExplicitlyForced || isBelowMinSupported

      val newState = when {
        !isNewer -> UpdateEngineState.NO_UPDATE
        isEffectivelyForced -> UpdateEngineState.FORCED_UPDATE_REQUIRED
        else -> UpdateEngineState.UPDATE_AVAILABLE
      }

      _updateInfo.update {
        it.copy(
          state = newState,
          manifest = manifest,
          firstSeenTimestamp = pubTimestamp,
          daysSinceFirstSeen = 0,
          isGracePeriodExpired = false,
          isDismissedForToday = false,
          isSimulation = false,
          statusMessage = "Release configuration saved locally."
        )
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save release configuration", e)
      Result.failure(e)
    }
  }

  /**
   * Admin Simulation: allows admin to test various release scenarios directly.
   * Explicitly tagged SIMULATION and isolated from real production releases.
   */
  fun simulateRelease(
    versionName: String,
    versionCode: Long,
    releaseType: ReleaseType,
    daysAgo: Int = 0,
    forceAfterDays: Int = 15,
    releaseNotes: String = "• [SIMULATION] Testing update distribution flow\n• [SIMULATION] UI performance enhancements"
  ) {
    val now = getCurrentTrustedTimeMillis()
    val publishedMillis = now - (daysAgo * 86400000L)
    val publishedDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(publishedMillis))

    val manifest = UpdateManifest(
      packageName = context.packageName,
      versionName = versionName,
      versionCode = versionCode,
      releaseType = releaseType.name,
      releaseTitle = "[SIMULATION] ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} v$versionName",
      releaseNotes = releaseNotes,
      publishedAt = publishedDateStr,
      publishedBy = "SIMULATION Test Engine",
      forceAfterDays = forceAfterDays,
      minimumSupportedVersionCode = if (releaseType == ReleaseType.CRITICAL) versionCode else 1L,
      apkUrl = "https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v$versionName/ManglamFertilizer-v$versionName.apk"
    )

    prefs.edit()
      .putString(KEY_LOCAL_PUBLISHED_MANIFEST, manifestAdapter.toJson(manifest))
      .putLong("update_first_seen_v_$versionCode", publishedMillis)
      .apply()

    val installedCode = getInstalledVersionCode()
    val isGraceExpired = daysAgo >= forceAfterDays
    val minSupportedCode = manifest.minimumSupportedVersionCode ?: 0L
    val isBelowMinSupported = minSupportedCode > 0 && installedCode < minSupportedCode
    val isEffectivelyForced = releaseType == ReleaseType.FORCED || releaseType == ReleaseType.CRITICAL || isGraceExpired || isBelowMinSupported

    val newState = when {
      versionCode <= installedCode -> UpdateEngineState.NO_UPDATE
      isEffectivelyForced -> UpdateEngineState.FORCED_UPDATE_REQUIRED
      else -> UpdateEngineState.UPDATE_AVAILABLE
    }

    _updateInfo.update {
      it.copy(
        state = newState,
        manifest = manifest,
        firstSeenTimestamp = publishedMillis,
        daysSinceFirstSeen = daysAgo,
        isGracePeriodExpired = isGraceExpired,
        isDismissedForToday = false,
        isSimulation = true,
        statusMessage = "[SIMULATION] Scenario v$versionName ($releaseType)"
      )
    }
  }

  /**
   * Cleans up local cached APKs.
   */
  fun clearUpdateCache() {
    try {
      getUpdatesDir().listFiles()?.forEach { it.delete() }
      _updateInfo.update {
        it.copy(
          verifiedApkFile = null,
          downloadProgress = 0f
        )
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error clearing update cache", e)
    }
  }
}
