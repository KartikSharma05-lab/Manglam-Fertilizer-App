package com.manglamfertilizer.app.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class ReleaseType(val displayName: String, val badgeText: String, val description: String) {
  @Json(name = "OPTIONAL")
  OPTIONAL("Optional Update", "Optional", "Low priority / small UI adjustments. User can skip freely."),

  @Json(name = "RECOMMENDED")
  RECOMMENDED("Recommended Update", "Recommended", "Normal feature update with 15-day grace period reminders."),

  @Json(name = "FORCED")
  FORCED("Force Update", "Mandatory", "Mandatory update. Blocks app access until installed."),

  @Json(name = "SILENT")
  SILENT("Silent / Low-Impact Update", "Silent / Low-Impact", "Non-intrusive update. Background prep, standard Android authorization."),

  @Json(name = "CRITICAL")
  CRITICAL("Security-Critical Update", "Security Critical", "Immediate security/critical fix. Mandatory immediate update.");

  companion object {
    fun fromString(type: String?): ReleaseType {
      return when (type?.trim()?.uppercase()) {
        "FORCED" -> FORCED
        "CRITICAL", "SECURITY_CRITICAL", "SECURITY-CRITICAL" -> CRITICAL
        "RECOMMENDED" -> RECOMMENDED
        "SILENT", "LOW_IMPACT", "LOW-IMPACT", "SILENT_UPDATE" -> SILENT
        else -> OPTIONAL
      }
    }
  }
}

@JsonClass(generateAdapter = true)
data class UpdateManifest(
  @Json(name = "packageName")
  val packageName: String = "com.manglamfertilizer.app",
  @Json(name = "versionName")
  val versionName: String = "1.0.0",
  @Json(name = "versionCode")
  val versionCode: Long = 1L,
  @Json(name = "releaseType")
  val releaseType: String = "OPTIONAL",
  @Json(name = "releaseTitle")
  val releaseTitle: String = "",
  @Json(name = "releaseNotes")
  val releaseNotes: String = "",
  @Json(name = "releaseNotesHindi")
  val releaseNotesHindi: String? = null,
  @Json(name = "apkUrl")
  val apkUrl: String = "",
  @Json(name = "sha256")
  val sha256: String = "",
  @Json(name = "publishedAt")
  val publishedAt: String = "",
  @Json(name = "publishedBy")
  val publishedBy: String? = "admin.manglamferilizer@gmail.com",
  @Json(name = "minimumSupportedVersion")
  val minimumSupportedVersion: String? = "1.0.0",
  @Json(name = "minimumSupportedVersionCode")
  val minimumSupportedVersionCode: Long? = 1L,
  @Json(name = "forceAfterDays")
  val forceAfterDays: Int? = 15,
  @Json(name = "signingCertificateSha256")
  val signingCertificateSha256: String? = null
) {
  val type: ReleaseType
    get() = ReleaseType.fromString(releaseType)

  val isExplicitlyForced: Boolean
    get() = type == ReleaseType.FORCED || type == ReleaseType.CRITICAL

  val isSilent: Boolean
    get() = type == ReleaseType.SILENT

  val isOptional: Boolean
    get() = type == ReleaseType.OPTIONAL || type == ReleaseType.SILENT
}

@JsonClass(generateAdapter = true)
data class ReleaseHistoryItem(
  @Json(name = "versionName")
  val versionName: String,
  @Json(name = "versionCode")
  val versionCode: Long,
  @Json(name = "releaseType")
  val releaseType: String,
  @Json(name = "releaseTitle")
  val releaseTitle: String,
  @Json(name = "releaseNotes")
  val releaseNotes: String,
  @Json(name = "publishedAt")
  val publishedAt: String,
  @Json(name = "publishedBy")
  val publishedBy: String,
  @Json(name = "forceAfterDays")
  val forceAfterDays: Int = 15,
  @Json(name = "status")
  val status: String = "Active", // "Active", "Archived", "Draft"
  @Json(name = "apkUrl")
  val apkUrl: String = ""
) {
  val type: ReleaseType
    get() = ReleaseType.fromString(releaseType)
}
