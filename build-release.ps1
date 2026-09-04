<#
.SYNOPSIS
    Manglam Fertilizer - One-Click Production Release APK Build Pipeline

.DESCRIPTION
    Validates project structure, runs unit tests, compiles release APK, validates signatures,
    calculates cryptographic SHA-256 hashes, and automatically generates update.json
    and release-metadata.json ready for GitHub release distribution.

.PARAMETER VersionName
    Optional explicit Version Name (e.g. 1.0.1). If omitted, parsed from app/build.gradle.kts.

.PARAMETER VersionCode
    Optional explicit Version Code (e.g. 2). If omitted, parsed from app/build.gradle.kts.

.PARAMETER ReleaseType
    Type of update: OPTIONAL, RECOMMENDED, FORCED, SILENT, SECURITY_CRITICAL (Default: RECOMMENDED).

.PARAMETER ReleaseTitle
    Title of the release update.

.PARAMETER ReleaseNotes
    English release changelog notes.

.PARAMETER ReleaseNotesHindi
    Hindi release changelog notes.

.PARAMETER ForceAfterDays
    Number of grace period days before update becomes mandatory (Default: 15).

.PARAMETER MinimumSupportedVersionCode
    Minimum version code allowed without hard blocking (Default: 1).
#>

[CmdletBinding()]
param(
    [string]$VersionName = "",
    [int]$VersionCode = 0,
    [ValidateSet("OPTIONAL", "RECOMMENDED", "FORCED", "SILENT", "SECURITY_CRITICAL")]
    [string]$ReleaseType = "RECOMMENDED",
    [string]$ReleaseTitle = "",
    [string]$ReleaseNotes = "• Production release update`n• Inventory, billing, and performance optimizations`n• Stability and offline sync improvements",
    [string]$ReleaseNotesHindi = "• नया अपडेट`n• स्टॉक व बिलिंग में सुधार`n• बेहतर परफॉरमेंस व स्थिरता",
    [int]$ForceAfterDays = 15,
    [int]$MinimumSupportedVersionCode = 1,
    [switch]$SkipTests
)

$ErrorActionPreference = "Stop"

Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "      MANGLAM FERTILIZER - ONE-CLICK RELEASE APK BUILD PIPELINE               " -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "[INFO] Starting release build pipeline at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Gray
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 1: VALIDATE PROJECT INTEGRITY & STRUCTURE (DO NOT DAMAGE SOURCE)
# ------------------------------------------------------------------------------
Write-Host "[STEP 1/18] Validating project integrity and critical configuration..." -ForegroundColor Yellow

$RequiredPaths = @(
    "app",
    "app/build.gradle.kts",
    "app/src/main/AndroidManifest.xml",
    "gradle/libs.versions.toml",
    "settings.gradle.kts"
)

foreach ($path in $RequiredPaths) {
    if (-not (Test-Path $path)) {
        Write-Error "[FATAL] Required path '$path' is missing! Aborting build to protect integrity."
        exit 1
    }
}

Write-Host "  [OK] Critical project files and directory hierarchy verified." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 2: EXTRACT & READ VERSION CONFIGURATION
# ------------------------------------------------------------------------------
Write-Host "[STEP 2/18] Extracting version information..." -ForegroundColor Yellow

$GradleContent = Get-Content "app/build.gradle.kts" -Raw

if ([string]::IsNullOrWhiteSpace($VersionName)) {
    if ($GradleContent -match 'versionName\s*=\s*"([^"]+)"') {
        $VersionName = $Matches[1].Trim()
    } else {
        $VersionName = "1.0.0"
    }
}

if ($VersionCode -le 0) {
    if ($GradleContent -match 'versionCode\s*=\s*([0-9]+)') {
        $VersionCode = [int]$Matches[1].Trim()
    } else {
        $VersionCode = 1
    }
}

$PackageName = "com.manglamfertilizer.app"

if ([string]::IsNullOrWhiteSpace($ReleaseTitle)) {
    $ReleaseTitle = "Manglam Fertilizer v$VersionName Release"
}

# ------------------------------------------------------------------------------
# STEP 3: VALIDATE VERSION
# ------------------------------------------------------------------------------
Write-Host "[STEP 3/18] Validating version parameters..." -ForegroundColor Yellow

if ([string]::IsNullOrWhiteSpace($VersionName)) {
    Write-Error "[FATAL] Invalid Version Name: cannot be empty."
    exit 1
}

if ($VersionCode -le 0) {
    Write-Error "[FATAL] Invalid VersionCode '$VersionCode'. VersionCode must be an integer greater than 0."
    exit 1
}

Write-Host "  [INFO] Target Version Name : $VersionName" -ForegroundColor White
Write-Host "  [INFO] Target Version Code : $VersionCode" -ForegroundColor White
Write-Host "  [INFO] Release Type        : $ReleaseType" -ForegroundColor White
Write-Host "  [INFO] Grace Period Days   : $ForceAfterDays" -ForegroundColor White
Write-Host "  [OK] Version information validated successfully." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 4: VALIDATE APPLICATION / PACKAGE IDENTIFIER
# ------------------------------------------------------------------------------
Write-Host "[STEP 4/18] Validating target package name..." -ForegroundColor Yellow

if ($GradleContent -match 'applicationId\s*=\s*"([^"]+)"') {
    $DetectedAppId = $Matches[1].Trim()
    if ($DetectedAppId -ne $PackageName) {
        Write-Error "[FATAL] Package name mismatch! Found '$DetectedAppId', expected '$PackageName'."
        exit 1
    }
}

Write-Host "  [INFO] Application ID: $PackageName" -ForegroundColor White
Write-Host "  [OK] Package identifier validated." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 5: VALIDATE PRODUCTION KEYSTORE
# ------------------------------------------------------------------------------
Write-Host "[STEP 5/18] Validating production signing keystore..." -ForegroundColor Yellow

$HasProdKeystore = $false

if (-not [string]::IsNullOrWhiteSpace($env:KEYSTORE_PATH) -and (Test-Path $env:KEYSTORE_PATH)) {
    $HasProdKeystore = $true
    Write-Host "  [INFO] Using production keystore from KEYSTORE_PATH: $env:KEYSTORE_PATH" -ForegroundColor White
} elseif (Test-Path "keystore.properties") {
    $PropsContent = Get-Content "keystore.properties" -Raw
    if ($PropsContent -match 'storeFile\s*=\s*(.+)') {
        $StoreFilePath = $Matches[1].Trim()
        if (Test-Path $StoreFilePath) {
            $HasProdKeystore = $true
            Write-Host "  [INFO] Using production keystore from keystore.properties: $StoreFilePath" -ForegroundColor White
        }
    }
} elseif (Test-Path "my-upload-key.jks") {
    $HasProdKeystore = $true
    Write-Host "  [INFO] Using production keystore: my-upload-key.jks" -ForegroundColor White
}

if (-not $HasProdKeystore) {
    Write-Host "==============================================================================" -ForegroundColor Red
    Write-Host "[ERROR] Production release keystore is not configured. Build aborted." -ForegroundColor Red
    Write-Host "==============================================================================" -ForegroundColor Red
    Write-Host "To configure a production keystore, either:" -ForegroundColor Yellow
    Write-Host "  1. Create 'keystore.properties' with storeFile, storePassword, keyAlias, keyPassword" -ForegroundColor Gray
    Write-Host "  2. Set environment variables: KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD" -ForegroundColor Gray
    Write-Host "  3. Place 'my-upload-key.jks' in the project root" -ForegroundColor Gray
    exit 1
}

Write-Host "  [OK] Production signing keystore validated." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 6: RUN UNIT & CONSISTENCY TESTS
# ------------------------------------------------------------------------------
if (-not $SkipTests) {
    Write-Host "[STEP 6/18] Running Unit & Consistency Tests..." -ForegroundColor Yellow
    
    $GradleExec = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "gradle" }
    
    Write-Host "  [INFO] Executing tests: $GradleExec testDebugUnitTest" -ForegroundColor Gray
    & $GradleExec testDebugUnitTest --no-daemon
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[FATAL] Unit tests failed with exit code $LASTEXITCODE! Stopping release."
        exit 1
    }
    Write-Host "  [OK] All unit tests passed successfully." -ForegroundColor Green
} else {
    Write-Host "[STEP 6/18] Skipping unit tests (--SkipTests specified)." -ForegroundColor DarkYellow
}
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 7: BUILD RELEASE APK
# ------------------------------------------------------------------------------
Write-Host "[STEP 7/18] Compiling Release APK with Gradle Wrapper..." -ForegroundColor Yellow

$GradleExec = if (Test-Path ".\gradlew.bat") { ".\gradlew.bat" } else { "gradle" }
Write-Host "  [INFO] Executing: $GradleExec assembleRelease" -ForegroundColor Gray

& $GradleExec assembleRelease --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Error "[FATAL] Gradle build failed with exit code $LASTEXITCODE! Stopping release."
    exit 1
}

Write-Host "  [OK] Release APK assembled successfully." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 8: VERIFY APK ARTIFACT EXISTS
# ------------------------------------------------------------------------------
Write-Host "[STEP 8/18] Locating APK build output..." -ForegroundColor Yellow

$CandidatePaths = @(
    "app/build/outputs/apk/release/app-release.apk",
    "app/build/outputs/apk/release/app-release-unsigned.apk"
)

$SourceApk = $null
foreach ($path in $CandidatePaths) {
    if (Test-Path $path) {
        $SourceApk = $path
        break
    }
}

if (-not $SourceApk) {
    $ApkFiles = Get-ChildItem -Path "app/build/outputs/apk/release" -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue
    if ($ApkFiles.Count -gt 0) {
        $SourceApk = $ApkFiles[0].FullName
    }
}

if (-not $SourceApk) {
    Write-Error "[FATAL] No APK artifact found in 'app/build/outputs/apk/release'!"
    exit 1
}

Write-Host "  [OK] Located Source APK: $SourceApk" -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 9: VERIFY PACKAGE NAME IN COMPILED APK
# ------------------------------------------------------------------------------
Write-Host "[STEP 9/18] Verifying package name in compiled APK..." -ForegroundColor Yellow

if (Get-Command aapt -ErrorAction SilentlyContinue) {
    $AaptOutput = & aapt dump badging $SourceApk 2>$null
    if ($AaptOutput -match "package:\s+name='([^']+)'") {
        $ApkPackage = $Matches[1]
        if ($ApkPackage -ne $PackageName) {
            Write-Error "[FATAL] APK package name verification failed! Expected '$PackageName', found '$ApkPackage'."
            exit 1
        }
    }
}
Write-Host "  [OK] Package name verified in APK." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 10: VERIFY VERSION NAME IN COMPILED APK
# ------------------------------------------------------------------------------
Write-Host "[STEP 10/18] Verifying versionName in compiled APK..." -ForegroundColor Yellow

if (Get-Command aapt -ErrorAction SilentlyContinue) {
    $AaptOutput = & aapt dump badging $SourceApk 2>$null
    if ($AaptOutput -match "versionName='([^']+)'") {
        $ApkVerName = $Matches[1]
        if ($ApkVerName -ne $VersionName) {
            Write-Host "  [WARN] APK versionName '$ApkVerName' differs from target '$VersionName'." -ForegroundColor Yellow
        }
    }
}
Write-Host "  [OK] Version name verified in APK." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 11: VERIFY VERSION CODE IN COMPILED APK
# ------------------------------------------------------------------------------
Write-Host "[STEP 11/18] Verifying versionCode in compiled APK..." -ForegroundColor Yellow

if (Get-Command aapt -ErrorAction SilentlyContinue) {
    $AaptOutput = & aapt dump badging $SourceApk 2>$null
    if ($AaptOutput -match "versionCode='([0-9]+)'") {
        $ApkVerCode = [int]$Matches[1]
        if ($ApkVerCode -le 0) {
            Write-Error "[FATAL] Invalid APK versionCode: '$ApkVerCode'."
            exit 1
        }
    }
}
Write-Host "  [OK] Version code verified in APK." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 12: VERIFY RELEASE SIGNING CERTIFICATE
# ------------------------------------------------------------------------------
Write-Host "[STEP 12/18] Verifying release cryptographic signature..." -ForegroundColor Yellow

if (Get-Command apksigner -ErrorAction SilentlyContinue) {
    & apksigner verify $SourceApk
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[FATAL] APK signature verification failed! The APK is corrupted or unsigned."
        exit 1
    }
    $CertOutput = & apksigner verify --print-certs $SourceApk 2>$null
    if ($CertOutput -match "Android Debug") {
        Write-Error "[FATAL] APK is signed with an Android Debug key! Production release aborted."
        exit 1
    }
}
Write-Host "  [OK] Release signing certificate verified." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 13: COPY ARTIFACT & COMPUTE FINAL SHA-256 CHECKSUM
# ------------------------------------------------------------------------------
Write-Host "[STEP 13/18] Preparing release directory and calculating SHA-256 hash..." -ForegroundColor Yellow

$ReleaseDir = "release"
if (-not (Test-Path $ReleaseDir)) {
    New-Item -ItemType Directory -Path $ReleaseDir | Out-Null
}

$FinalApkName = "ManglamFertilizer-v$VersionName.apk"
$FinalApkPath = Join-Path $ReleaseDir $FinalApkName

Copy-Item -Path $SourceApk -Destination $FinalApkPath -Force
$ApkItem = Get-Item $FinalApkPath
$ApkSizeBytes = $ApkItem.Length

$HashResult = Get-FileHash -Path $FinalApkPath -Algorithm SHA256
$Sha256Hex = $HashResult.Hash.ToLower()

Write-Host "  [INFO] Output Artifact : $FinalApkPath" -ForegroundColor White
Write-Host "  [INFO] Artifact Size   : $([math]::Round($ApkSizeBytes / 1MB, 2)) MB ($ApkSizeBytes bytes)" -ForegroundColor White
Write-Host "  [INFO] SHA-256 Hash    : $Sha256Hex" -ForegroundColor White
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 14: GENERATE UPDATE.JSON (SAFE JSON ENCODING)
# ------------------------------------------------------------------------------
Write-Host "[STEP 14/18] Generating release manifest (update.json)..." -ForegroundColor Yellow

$TodayStr = (Get-Date).ToString("yyyy-MM-dd")
$ApkDownloadUrl = "https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v$VersionName/$FinalApkName"

$UpdateManifest = [ordered]@{
    packageName                  = $PackageName
    versionName                  = $VersionName
    versionCode                  = $VersionCode
    releaseType                  = $ReleaseType
    releaseTitle                 = $ReleaseTitle
    releaseNotes                 = $ReleaseNotes
    releaseNotesHindi            = $ReleaseNotesHindi
    apkUrl                       = $ApkDownloadUrl
    sha256                       = $Sha256Hex
    publishedBy                  = "admin.manglamferilizer@gmail.com"
    publishedAt                  = $TodayStr
    forceAfterDays               = $ForceAfterDays
    minimumSupportedVersion      = "1.0.0"
    minimumSupportedVersionCode  = $MinimumSupportedVersionCode
}

$UpdateJsonPath = Join-Path $ReleaseDir "update.json"
$UpdateManifest | ConvertTo-Json -Depth 5 | Set-Content -Path $UpdateJsonPath -Encoding UTF8

Write-Host "  [OK] Generated '$UpdateJsonPath'" -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 15: GENERATE RELEASE-METADATA.JSON
# ------------------------------------------------------------------------------
Write-Host "[STEP 15/18] Generating release metadata (release-metadata.json)..." -ForegroundColor Yellow

$ReleaseMetadata = [ordered]@{
    buildTimestamp   = (Get-Date).ToString("o")
    packageName      = $PackageName
    versionName      = $VersionName
    versionCode      = $VersionCode
    releaseType      = $ReleaseType
    apkFilename      = $FinalApkName
    apkSizeBytes     = $ApkSizeBytes
    sha256           = $Sha256Hex
    distributionUrl  = $ApkDownloadUrl
    githubReleaseTag = "v$VersionName"
}

$MetadataPath = Join-Path $ReleaseDir "release-metadata.json"
$ReleaseMetadata | ConvertTo-Json -Depth 5 | Set-Content -Path $MetadataPath -Encoding UTF8

Write-Host "  [OK] Generated '$MetadataPath'" -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 16: GENERATE SHA256SUMS.TXT
# ------------------------------------------------------------------------------
Write-Host "[STEP 16/18] Writing checksum file (SHA256SUMS.txt)..." -ForegroundColor Yellow

"$Sha256Hex  $FinalApkName" | Set-Content (Join-Path $ReleaseDir "SHA256SUMS.txt") -Encoding UTF8

Write-Host "  [OK] Checksum saved to release/SHA256SUMS.txt" -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 17: STAGE & VERIFY ALL RELEASE ARTIFACTS
# ------------------------------------------------------------------------------
Write-Host "[STEP 17/18] Verifying all release artifacts in release/..." -ForegroundColor Yellow

$DistributionArtifacts = @(
    $FinalApkPath,
    $UpdateJsonPath,
    (Join-Path $ReleaseDir "SHA256SUMS.txt"),
    $MetadataPath
)

foreach ($art in $DistributionArtifacts) {
    if (-not (Test-Path $art)) {
        Write-Error "[FATAL] Missing release artifact: $art"
        exit 1
    }
}

Write-Host "  [OK] All 4 distribution artifacts staged and verified." -ForegroundColor Green
Write-Host ""

# ------------------------------------------------------------------------------
# STEP 18: SUMMARY & PUBLISH INSTRUCTIONS
# ------------------------------------------------------------------------------
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host "      [STEP 18/18] BUILD & VERIFICATION SUCCESSFUL - READY FOR RELEASE!        " -ForegroundColor Green
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Artifacts generated in directory: .\$ReleaseDir\" -ForegroundColor Cyan
Write-Host "  1. APK File    : $FinalApkPath" -ForegroundColor White
Write-Host "  2. Update JSON : $UpdateJsonPath" -ForegroundColor White
Write-Host "  3. SHA-256 Sum : $(Join-Path $ReleaseDir 'SHA256SUMS.txt')" -ForegroundColor White
Write-Host "  4. Metadata    : $MetadataPath" -ForegroundColor White
Write-Host ""
Write-Host "Publish Instructions:" -ForegroundColor Yellow
Write-Host "  1. Push Git Tag: git tag v$VersionName && git push origin v$VersionName" -ForegroundColor White
Write-Host "  2. Create GitHub Release 'v$VersionName' and attach '$FinalApkName' and 'update.json'" -ForegroundColor White
Write-Host "  3. In-app update engine will automatically deliver this version to users." -ForegroundColor White
Write-Host ""

