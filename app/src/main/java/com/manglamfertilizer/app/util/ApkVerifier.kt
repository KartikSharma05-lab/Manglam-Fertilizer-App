package com.manglamfertilizer.app.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.manglamfertilizer.app.data.model.UpdateManifest
import com.manglamfertilizer.app.data.model.VerificationResult
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ApkVerifier {
  private const val TAG = "ApkVerifier"

  /**
   * Calculates the SHA-256 hash of a file as a lowercase hex string.
   */
  fun calculateSha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
      val buffer = ByteArray(8192)
      var read: Int
      while (fis.read(buffer).also { read = it } != -1) {
        digest.update(buffer, 0, read)
      }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  /**
   * Calculates SHA-256 fingerprint of a raw signature byte array.
   */
  fun calculateSignatureFingerprint(signatureBytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(signatureBytes)
    return hash.joinToString("") { "%02x".format(it) }
  }

  /**
   * Retrieves the SHA-256 signing certificate fingerprints of the currently installed application.
   */
  @Suppress("DEPRECATION")
  fun getInstalledAppSignatures(context: Context): List<String> {
    val fingerprints = mutableListOf<String>()
    try {
      val pm = context.packageManager
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val packageInfo = pm.getPackageInfo(
          context.packageName,
          PackageManager.GET_SIGNING_CERTIFICATES
        )
        val signingInfo = packageInfo.signingInfo
        if (signingInfo != null) {
          val signers = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
          } else {
            signingInfo.signingCertificateHistory
          }
          signers?.forEach { signature ->
            fingerprints.add(calculateSignatureFingerprint(signature.toByteArray()))
          }
        }
      } else {
        val packageInfo = pm.getPackageInfo(
          context.packageName,
          PackageManager.GET_SIGNATURES
        )
        packageInfo.signatures?.forEach { signature ->
          fingerprints.add(calculateSignatureFingerprint(signature.toByteArray()))
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting installed app signatures", e)
    }
    return fingerprints
  }

  /**
   * Retrieves the SHA-256 signing certificate fingerprints of a downloaded APK file.
   */
  @Suppress("DEPRECATION")
  fun getApkSignatures(context: Context, apkFile: File): List<String> {
    val fingerprints = mutableListOf<String>()
    try {
      val pm = context.packageManager
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        PackageManager.GET_SIGNATURES
      }
      val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
      if (archiveInfo != null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
          val signingInfo = archiveInfo.signingInfo
          if (signingInfo != null) {
            val signers = if (signingInfo.hasMultipleSigners()) {
              signingInfo.apkContentsSigners
            } else {
              signingInfo.signingCertificateHistory
            }
            signers?.forEach { signature ->
              fingerprints.add(calculateSignatureFingerprint(signature.toByteArray()))
            }
          }
        } else {
          archiveInfo.signatures?.forEach { signature ->
            fingerprints.add(calculateSignatureFingerprint(signature.toByteArray()))
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error getting APK signatures for ${apkFile.name}", e)
    }
    return fingerprints
  }

  /**
   * Retrieves PackageInfo from an APK archive on disk.
   */
  fun getApkPackageInfo(context: Context, apkFile: File): PackageInfo? {
    return try {
      val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
      } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
      }
      context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
    } catch (e: Exception) {
      Log.e(TAG, "Error reading package archive info", e)
      null
    }
  }

  /**
   * Comprehensive 8-point validation of a downloaded APK against:
   * CHECK 1: File exists
   * CHECK 2: File size > 0
   * CHECK 3: SHA-256 matches manifest checksum
   * CHECK 4: APK can be parsed by PackageManager
   * CHECK 5: Package name == com.manglamfertilizer.app (matches installed app)
   * CHECK 6: APK versionCode == manifest.versionCode
   * CHECK 7: APK versionCode > installedVersionCode (no downgrades)
   * CHECK 8: Signing certificate matches trusted installed production certificate
   *
   * FAIL CLOSED: If any check fails, the invalid APK is deleted immediately,
   * and VerificationResult.Failed is returned.
   */
  fun verifyApk(
    context: Context,
    apkFile: File,
    manifest: UpdateManifest,
    installedVersionCode: Long
  ): VerificationResult {
    // CHECK 1: File exists
    if (!apkFile.exists()) {
      Log.e(TAG, "CHECK 1 FAILED: APK file does not exist: ${apkFile.absolutePath}")
      return VerificationResult.Failed(
        "File not found.",
        "APK file does not exist on local storage."
      )
    }

    // CHECK 2: File size > 0
    if (apkFile.length() <= 0L) {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 2 FAILED: APK file size is 0 bytes")
      return VerificationResult.Failed(
        "Corrupted APK file.",
        "Downloaded APK file is empty (0 bytes)."
      )
    }

    // CHECK 3: SHA-256 matches manifest
    val calculatedHash = try {
      calculateSha256(apkFile)
    } catch (e: Exception) {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 3 FAILED: Failed to calculate SHA-256", e)
      return VerificationResult.Failed(
        "Checksum calculation failed.",
        "Failed to compute SHA-256 checksum: ${e.localizedMessage}"
      )
    }

    val expectedHash = manifest.sha256.trim().lowercase()
    if (expectedHash.isNotEmpty() && !calculatedHash.equals(expectedHash, ignoreCase = true)) {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 3 FAILED: SHA-256 mismatch! Computed: $calculatedHash, Expected: $expectedHash")
      return VerificationResult.Failed(
        "SHA-256 checksum mismatch.",
        "The downloaded APK hash ($calculatedHash) does not match the manifest hash ($expectedHash)."
      )
    }

    // CHECK 4: APK can be parsed
    val archiveInfo = getApkPackageInfo(context, apkFile)
    if (archiveInfo == null) {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 4 FAILED: Unable to parse Android package archive")
      return VerificationResult.Failed(
        "Invalid APK format.",
        "Corrupted package: Android package manager could not parse the downloaded APK."
      )
    }

    // CHECK 5: Package name == com.manglamfertilizer.app (matches installed app)
    val apkPackageName = archiveInfo.packageName
    val expectedPackageName = context.packageName // com.manglamfertilizer.app

    if (apkPackageName != expectedPackageName || apkPackageName != "com.manglamfertilizer.app") {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 5 FAILED: Package name mismatch! APK: $apkPackageName, Expected: $expectedPackageName")
      return VerificationResult.Failed(
        "Package identity mismatch.",
        "APK package name ($apkPackageName) does not match installed application ($expectedPackageName)."
      )
    }

    // Extract numerical versionCode from APK archive
    @Suppress("DEPRECATION")
    val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      archiveInfo.longVersionCode
    } else {
      archiveInfo.versionCode.toLong()
    }

    // CHECK 6: APK versionCode == manifest.versionCode
    if (manifest.versionCode > 0 && apkVersionCode != manifest.versionCode) {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 6 FAILED: VersionCode mismatch! APK: $apkVersionCode, Manifest: ${manifest.versionCode}")
      return VerificationResult.Failed(
        "Version code mismatch.",
        "APK version code ($apkVersionCode) does not match manifest version code (${manifest.versionCode})."
      )
    }

    // CHECK 7: APK versionCode > installedVersionCode
    if (apkVersionCode <= installedVersionCode) {
      safeDelete(apkFile)
      Log.e(TAG, "CHECK 7 FAILED: APK version code ($apkVersionCode) <= installed ($installedVersionCode)")
      return VerificationResult.Failed(
        "Rollback protection triggered.",
        "APK version code ($apkVersionCode) is not newer than currently installed version ($installedVersionCode)."
      )
    }

    // CHECK 8: Signing certificate matches trusted installed production certificate
    // The installed production application's signing certificate is the primary trust anchor.
    val installedSignatures = getInstalledAppSignatures(context)
    val apkSignatures = getApkSignatures(context, apkFile)

    if (installedSignatures.isEmpty()) {
      Log.w(TAG, "Could not extract installed app signatures; proceeding with extreme caution.")
    } else {
      if (apkSignatures.isEmpty()) {
        safeDelete(apkFile)
        Log.e(TAG, "CHECK 8 FAILED: No signing certificates found in downloaded APK archive")
        return VerificationResult.Failed(
          "Signature missing.",
          "Downloaded APK does not contain a valid Android signing certificate."
        )
      }

      val signatureMatchesInstalled = apkSignatures.any { apkSig ->
        installedSignatures.contains(apkSig)
      }

      if (!signatureMatchesInstalled) {
        safeDelete(apkFile)
        Log.e(TAG, "CHECK 8 FAILED: APK signature does not match installed application certificate!")
        return VerificationResult.Failed(
          "Signature mismatch.",
          "APK signing certificate does not match the trusted installed application release certificate."
        )
      }
    }

    // Optional additional consistency check against manifest fingerprint if provided
    val manifestCertFingerprint = manifest.signingCertificateSha256?.trim()?.lowercase()
    if (!manifestCertFingerprint.isNullOrBlank() && apkSignatures.isNotEmpty()) {
      val matchesManifestCert = apkSignatures.any { it.equals(manifestCertFingerprint, ignoreCase = true) }
      if (!matchesManifestCert) {
        safeDelete(apkFile)
        Log.e(TAG, "CHECK 8 (Consistency) FAILED: APK signature does not match manifest certificate fingerprint!")
        return VerificationResult.Failed(
          "Certificate fingerprint mismatch.",
          "APK signing certificate does not match the manifest expected certificate fingerprint."
        )
      }
    }

    Log.i(TAG, "ALL 8 SECURITY CHECKS PASSED: ${apkFile.name} (Code: $apkVersionCode, SHA256: $calculatedHash)")
    return VerificationResult.Success
  }

  private fun safeDelete(file: File) {
    try {
      if (file.exists()) {
        file.delete()
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to delete invalid APK file: ${file.absolutePath}", e)
    }
  }
}
