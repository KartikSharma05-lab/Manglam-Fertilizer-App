package com.manglamfertilizer.app

import com.manglamfertilizer.app.data.model.AppUpdateInfo
import com.manglamfertilizer.app.data.model.ReleaseType
import com.manglamfertilizer.app.data.model.UpdateEngineState
import com.manglamfertilizer.app.data.model.UpdateManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppUpdateEngineUnitTest {

  @Test
  fun testDefaultManifestPackageNameAndBaseline() {
    val manifest = UpdateManifest()
    assertEquals("com.manglamfertilizer.app", manifest.packageName)
    assertEquals("1.0.0", manifest.versionName)
    assertEquals(1L, manifest.versionCode)
    assertEquals(15, manifest.forceAfterDays)
  }

  @Test
  fun testReleaseTypeParsing() {
    assertEquals(ReleaseType.OPTIONAL, ReleaseType.fromString("OPTIONAL"))
    assertEquals(ReleaseType.RECOMMENDED, ReleaseType.fromString("RECOMMENDED"))
    assertEquals(ReleaseType.FORCED, ReleaseType.fromString("FORCED"))
    assertEquals(ReleaseType.SILENT, ReleaseType.fromString("SILENT"))
    assertEquals(ReleaseType.CRITICAL, ReleaseType.fromString("SECURITY_CRITICAL"))
    assertEquals(ReleaseType.CRITICAL, ReleaseType.fromString("CRITICAL"))
    assertEquals(ReleaseType.OPTIONAL, ReleaseType.fromString("UNKNOWN_TYPE"))
  }

  @Test
  fun testForcedUpdateEvaluation_ExplicitForced() {
    val forcedManifest = UpdateManifest(
      versionName = "1.0.1",
      versionCode = 2L,
      releaseType = "FORCED"
    )
    val info = AppUpdateInfo(
      state = UpdateEngineState.UPDATE_AVAILABLE,
      installedVersionCode = 1L,
      manifest = forcedManifest
    )
    assertTrue(info.isForced)
  }

  @Test
  fun testForcedUpdateEvaluation_SecurityCritical() {
    val criticalManifest = UpdateManifest(
      versionName = "1.0.1",
      versionCode = 2L,
      releaseType = "SECURITY_CRITICAL"
    )
    val info = AppUpdateInfo(
      state = UpdateEngineState.UPDATE_AVAILABLE,
      installedVersionCode = 1L,
      manifest = criticalManifest
    )
    assertTrue(info.isForced)
  }

  @Test
  fun testForcedUpdateEvaluation_GracePeriodExpiration() {
    val recommendedManifest = UpdateManifest(
      versionName = "1.0.1",
      versionCode = 2L,
      releaseType = "RECOMMENDED",
      forceAfterDays = 15
    )
    val nonExpiredInfo = AppUpdateInfo(
      state = UpdateEngineState.UPDATE_AVAILABLE,
      installedVersionCode = 1L,
      manifest = recommendedManifest,
      daysSinceFirstSeen = 10,
      isGracePeriodExpired = false
    )
    assertFalse(nonExpiredInfo.isForced)
    assertEquals(5, nonExpiredInfo.remainingGraceDays)

    val expiredInfo = AppUpdateInfo(
      state = UpdateEngineState.UPDATE_AVAILABLE,
      installedVersionCode = 1L,
      manifest = recommendedManifest,
      daysSinceFirstSeen = 16,
      isGracePeriodExpired = true
    )
    assertTrue(expiredInfo.isForced)
    assertEquals(0, expiredInfo.remainingGraceDays)
  }

  @Test
  fun testForcedUpdateEvaluation_MinimumSupportedVersionViolation() {
    val manifest = UpdateManifest(
      versionName = "1.0.5",
      versionCode = 5L,
      releaseType = "RECOMMENDED",
      minimumSupportedVersionCode = 3L
    )
    val outdatedInfo = AppUpdateInfo(
      state = UpdateEngineState.UPDATE_AVAILABLE,
      installedVersionCode = 2L,
      manifest = manifest
    )
    assertTrue(outdatedInfo.isForced)

    val supportedInfo = AppUpdateInfo(
      state = UpdateEngineState.UPDATE_AVAILABLE,
      installedVersionCode = 3L,
      manifest = manifest
    )
    assertFalse(supportedInfo.isForced)
  }
}
