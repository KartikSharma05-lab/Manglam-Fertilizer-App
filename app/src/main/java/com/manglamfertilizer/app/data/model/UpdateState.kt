package com.manglamfertilizer.app.data.model

import java.io.File

/**
 * Explicit update engine states as mandated by the custom GitHub In-App Update Engine architecture.
 */
enum class UpdateEngineState {
  NO_UPDATE,
  CHECKING,
  UPDATE_AVAILABLE,
  DOWNLOADING,
  DOWNLOAD_PAUSED,
  DOWNLOAD_FAILED,
  VERIFYING,
  VERIFICATION_FAILED,
  READY_TO_INSTALL,
  INSTALLING,
  INSTALL_FAILED,
  UPDATE_COMPLETED,
  FORCED_UPDATE_REQUIRED
}

sealed class VerificationResult {
  data object Success : VerificationResult()
  data class Failed(val reason: String, val details: String? = null) : VerificationResult()
}

data class AppUpdateInfo(
  val state: UpdateEngineState = UpdateEngineState.NO_UPDATE,
  val installedVersionName: String = "1.0.0",
  val installedVersionCode: Long = 1L,
  val installedPackageName: String = "",
  val manifest: UpdateManifest? = null,
  val downloadProgress: Float = 0f, // 0.0 to 1.0
  val downloadedBytes: Long = 0L,
  val totalBytes: Long = 0L,
  val verifiedApkFile: File? = null,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
  val lastCheckedTimestamp: Long = 0L,
  val isChecking: Boolean = false,
  val firstSeenTimestamp: Long = 0L,
  val daysSinceFirstSeen: Int = 0,
  val isGracePeriodExpired: Boolean = false,
  val isDismissedForToday: Boolean = false,
  val isSimulation: Boolean = false
) {
  val hasUpdate: Boolean
    get() = state == UpdateEngineState.UPDATE_AVAILABLE ||
        state == UpdateEngineState.DOWNLOADING ||
        state == UpdateEngineState.DOWNLOAD_PAUSED ||
        state == UpdateEngineState.VERIFYING ||
        state == UpdateEngineState.READY_TO_INSTALL ||
        state == UpdateEngineState.FORCED_UPDATE_REQUIRED

  val isReadyToInstall: Boolean
    get() = state == UpdateEngineState.READY_TO_INSTALL && verifiedApkFile?.exists() == true

  val releaseType: ReleaseType
    get() = manifest?.type ?: ReleaseType.OPTIONAL

  /**
   * Evaluates if update is strictly forced / non-dismissible:
   * 1. Explicit FORCED or CRITICAL release type.
   * 2. Current version is strictly below minimumSupportedVersionCode.
   * 3. 15-day (or configured forceAfterDays) grace period has expired.
   */
  val isForced: Boolean
    get() {
      if (state == UpdateEngineState.FORCED_UPDATE_REQUIRED) return true
      val m = manifest ?: return false
      if (m.isExplicitlyForced) return true
      val minCode = m.minimumSupportedVersionCode ?: 0L
      if (minCode > 0 && installedVersionCode < minCode) return true
      if (isGracePeriodExpired) return true
      return false
    }

  val isSilent: Boolean
    get() = manifest?.isSilent == true

  val isSecurityCritical: Boolean
    get() = manifest?.type == ReleaseType.CRITICAL

  val isOptional: Boolean
    get() = !isForced && (manifest?.isOptional == true)

  val gracePeriodDays: Int
    get() = manifest?.forceAfterDays ?: 15

  val remainingGraceDays: Int
    get() = (gracePeriodDays - daysSinceFirstSeen).coerceAtLeast(0)

  /**
   * User-facing Update Status mapping strictly supporting all required states:
   * Up to date, Update available, Downloading, Verifying, Ready to install,
   * Installing, Update required, Download failed, Verification failed, No internet, GitHub unavailable.
   */
  val displayStatus: String
    get() {
      if (errorMessage != null) {
        val err = errorMessage.lowercase()
        if (err.contains("internet") || err.contains("network") || err.contains("offline") || err.contains("unknownhost")) {
          return "No internet"
        }
        if (err.contains("github") || err.contains("timeout") || err.contains("server") || err.contains("connect")) {
          return "GitHub unavailable"
        }
      }
      return when (state) {
        UpdateEngineState.NO_UPDATE, UpdateEngineState.UPDATE_COMPLETED -> "Up to date"
        UpdateEngineState.CHECKING -> "Checking for updates..."
        UpdateEngineState.UPDATE_AVAILABLE -> "Update available"
        UpdateEngineState.DOWNLOADING, UpdateEngineState.DOWNLOAD_PAUSED -> "Downloading"
        UpdateEngineState.VERIFYING -> "Verifying"
        UpdateEngineState.READY_TO_INSTALL -> "Ready to install"
        UpdateEngineState.INSTALLING -> "Installing"
        UpdateEngineState.FORCED_UPDATE_REQUIRED -> "Update required"
        UpdateEngineState.DOWNLOAD_FAILED -> "Download failed"
        UpdateEngineState.VERIFICATION_FAILED -> "Verification failed"
        UpdateEngineState.INSTALL_FAILED -> "Install failed"
      }
    }
}
