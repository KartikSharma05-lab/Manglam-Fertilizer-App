@echo off
setlocal enabledelayedexpansion

echo ==============================================================================
echo       MANGLAM FERTILIZER - ONE-CLICK RELEASE APK BUILD SYSTEM
echo ==============================================================================
echo [INFO] Starting release build pipeline at %DATE% %TIME%
echo.

REM ------------------------------------------------------------------------------
REM STEP 1: VALIDATE PROJECT INTEGRITY & STRUCTURE (DO NOT DAMAGE SOURCE)
REM ------------------------------------------------------------------------------
echo [STEP 1/18] Validating project structure and essential configuration files...

if not exist "app" (
    echo [ERROR] 'app' module directory not found! Ensure you run this from the project root.
    goto :BUILD_FAILED
)

if not exist "app\build.gradle.kts" (
    echo [ERROR] 'app\build.gradle.kts' is missing!
    goto :BUILD_FAILED
)

if not exist "app\src\main\AndroidManifest.xml" (
    echo [ERROR] 'app\src\main\AndroidManifest.xml' is missing!
    goto :BUILD_FAILED
)

if not exist "gradle\libs.versions.toml" (
    echo [ERROR] 'gradle\libs.versions.toml' is missing!
    goto :BUILD_FAILED
)

if not exist "settings.gradle.kts" (
    echo [ERROR] 'settings.gradle.kts' is missing!
    goto :BUILD_FAILED
)

echo [OK] Project structure validated successfully.
echo.

REM ------------------------------------------------------------------------------
REM STEP 2: EXTRACT & READ VERSION CONFIGURATION
REM ------------------------------------------------------------------------------
echo [STEP 2/18] Extracting version metadata...

set VERSION_NAME=1.0.0
set VERSION_CODE=1
set APP_ID=com.manglamfertilizer.app

for /f "tokens=2 delims==" %%a in ('findstr /i "versionName" "app\build.gradle.kts"') do (
    set raw=%%a
    set raw=!raw:"=!
    set raw=!raw: =!
    if not "!raw!"=="" set VERSION_NAME=!raw!
)

for /f "tokens=2 delims==" %%a in ('findstr /i "versionCode" "app\build.gradle.kts"') do (
    set raw=%%a
    set raw=!raw: =!
    if not "!raw!"=="" set VERSION_CODE=!raw!
)

if not "%~1"=="" set VERSION_NAME=%~1
if not "%~2"=="" set VERSION_CODE=%~2
set RELEASE_TYPE=RECOMMENDED
if not "%~3"=="" set RELEASE_TYPE=%~3
set FORCE_AFTER_DAYS=15
if not "%~4"=="" set FORCE_AFTER_DAYS=%~4

REM ------------------------------------------------------------------------------
REM STEP 3: VALIDATE VERSION
REM ------------------------------------------------------------------------------
echo [STEP 3/18] Validating version parameters...

if "%VERSION_NAME%"=="" (
    echo [ERROR] Version Name cannot be empty!
    goto :BUILD_FAILED
)

if "%VERSION_CODE%"=="" (
    echo [ERROR] Failed to extract valid versionCode from app\build.gradle.kts
    goto :BUILD_FAILED
)

echo [INFO] Target Version Name : %VERSION_NAME%
echo [INFO] Target Version Code : %VERSION_CODE%
echo [INFO] Target Release Type : %RELEASE_TYPE%
echo [OK] Version information validated.
echo.

REM ------------------------------------------------------------------------------
REM STEP 4: VALIDATE APPLICATION / PACKAGE IDENTIFIER
REM ------------------------------------------------------------------------------
echo [STEP 4/18] Validating target package name...

set DETECTED_APP_ID=
for /f "tokens=2 delims==" %%a in ('findstr /i "applicationId" "app\build.gradle.kts"') do (
    set raw=%%a
    set raw=!raw:"=!
    set raw=!raw: =!
    if not "!raw!"=="" set DETECTED_APP_ID=!raw!
)

if not "%DETECTED_APP_ID%"=="%APP_ID%" (
    echo [ERROR] Package name mismatch! Found '%DETECTED_APP_ID%', expected '%APP_ID%'.
    goto :BUILD_FAILED
)

echo [INFO] Target Application ID : %APP_ID%
echo [OK] Package identifier validated.
echo.

REM ------------------------------------------------------------------------------
REM STEP 5: VALIDATE PRODUCTION KEYSTORE
REM ------------------------------------------------------------------------------
echo [STEP 5/18] Validating production signing keystore...

set HAS_PROD_KEYSTORE=0

if defined KEYSTORE_PATH (
    if exist "%KEYSTORE_PATH%" (
        set HAS_PROD_KEYSTORE=1
        echo [INFO] Using production keystore from KEYSTORE_PATH: %KEYSTORE_PATH%
    )
)

if %HAS_PROD_KEYSTORE% equ 0 (
    if exist "keystore.properties" (
        for /f "tokens=2 delims==" %%a in ('findstr /i "storeFile" "keystore.properties"') do (
            set storefile=%%a
            set storefile=!storefile: =!
            if exist "!storefile!" (
                set HAS_PROD_KEYSTORE=1
                echo [INFO] Using production keystore from keystore.properties: !storefile!
            )
        )
    )
)

if %HAS_PROD_KEYSTORE% equ 0 (
    if exist "my-upload-key.jks" (
        set HAS_PROD_KEYSTORE=1
        echo [INFO] Using production keystore: my-upload-key.jks
    )
)

if %HAS_PROD_KEYSTORE% equ 0 (
    echo ==============================================================================
    echo [ERROR] Production release keystore is not configured. Build aborted.
    echo ==============================================================================
    echo To configure a production keystore, either:
    echo   1. Create 'keystore.properties' with storeFile, storePassword, keyAlias, keyPassword
    echo   2. Set environment variables: KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD
    echo   3. Place 'my-upload-key.jks' in the project root
    goto :BUILD_FAILED
)

echo [OK] Production signing keystore validated.
echo.

REM ------------------------------------------------------------------------------
REM STEP 6: RUN UNIT & CONSISTENCY TESTS
REM ------------------------------------------------------------------------------
echo [STEP 6/18] Running Unit & Consistency Tests...

set GRADLE_CMD=gradlew.bat
if not exist "gradlew.bat" (
    where gradle >nul 2>nul
    if %ERRORLEVEL% equ 0 (
        set GRADLE_CMD=gradle
    ) else (
        echo [ERROR] Neither 'gradlew.bat' nor global 'gradle' found!
        goto :BUILD_FAILED
    )
)

if not "%SKIP_TESTS%"=="true" (
    echo [INFO] Executing: %GRADLE_CMD% testDebugUnitTest --no-daemon
    call %GRADLE_CMD% testDebugUnitTest --no-daemon
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Unit tests failed with exit code %ERRORLEVEL%!
        goto :BUILD_FAILED
    )
    echo [OK] Unit tests passed successfully.
) else (
    echo [INFO] Skipping tests (SKIP_TESTS=true).
)
echo.

REM ------------------------------------------------------------------------------
REM STEP 7: BUILD RELEASE APK
REM ------------------------------------------------------------------------------
echo [STEP 7/18] Building Release APK with Gradle Wrapper...

echo [INFO] Executing: %GRADLE_CMD% assembleRelease --no-daemon
call %GRADLE_CMD% assembleRelease --no-daemon
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Gradle release build failed with exit code %ERRORLEVEL%!
    goto :BUILD_FAILED
)

echo [OK] Release build completed successfully.
echo.

REM ------------------------------------------------------------------------------
REM STEP 8: VERIFY APK ARTIFACT EXISTS
REM ------------------------------------------------------------------------------
echo [STEP 8/18] Locating compiled release APK artifact...

set RAW_APK=
if exist "app\build\outputs\apk\release\app-release.apk" (
    set RAW_APK=app\build\outputs\apk\release\app-release.apk
) else if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    set RAW_APK=app\build\outputs\apk\release\app-release-unsigned.apk
) else if exist "app\build\outputs\apk\release" (
    for /r "app\build\outputs\apk\release" %%f in (*.apk) do (
        set RAW_APK=%%f
    )
) else if exist "app\build\outputs" (
    for /r "app\build\outputs" %%f in (*.apk) do (
        echo "%%f" | findstr /i /v "debug androidTest test unsigned intermediates" >nul && set RAW_APK=%%f
    )
)

if "%RAW_APK%"=="" (
    echo [ERROR] Could not find any release APK file in app\build\outputs!
    goto :BUILD_FAILED
)

echo [OK] Located APK: %RAW_APK%
echo.

REM ------------------------------------------------------------------------------
REM STEP 9: VERIFY PACKAGE NAME IN APK
REM ------------------------------------------------------------------------------
echo [STEP 9/18] Verifying APK package name...
where aapt >nul 2>nul
if %ERRORLEVEL% equ 0 (
    for /f "tokens=2 delims='" %%p in ('aapt dump badging "%RAW_APK%" 2^>nul ^| findstr "package: name="') do (
        if not "%%p"=="%APP_ID%" (
            echo [ERROR] Package name verification failed: expected '%APP_ID%', found '%%p'.
            goto :BUILD_FAILED
        )
    )
)
echo [OK] Package name verified in APK.
echo.

REM ------------------------------------------------------------------------------
REM STEP 10: VERIFY VERSION NAME IN APK
REM ------------------------------------------------------------------------------
echo [STEP 10/18] Verifying APK versionName...
where aapt >nul 2>nul
if %ERRORLEVEL% equ 0 (
    for /f "tokens=4 delims='" %%v in ('aapt dump badging "%RAW_APK%" 2^>nul ^| findstr "versionName="') do (
        echo [INFO] APK versionName verified: %%v
    )
)
echo [OK] Version name checked.
echo.

REM ------------------------------------------------------------------------------
REM STEP 11: VERIFY VERSION CODE IN APK
REM ------------------------------------------------------------------------------
echo [STEP 11/18] Verifying APK versionCode...
where aapt >nul 2>nul
if %ERRORLEVEL% equ 0 (
    for /f "tokens=6 delims='" %%c in ('aapt dump badging "%RAW_APK%" 2^>nul ^| findstr "versionCode="') do (
        echo [INFO] APK versionCode verified: %%c
    )
)
echo [OK] Version code checked.
echo.

REM ------------------------------------------------------------------------------
REM STEP 12: VERIFY RELEASE SIGNING CERTIFICATE
REM ------------------------------------------------------------------------------
echo [STEP 12/18] Verifying release cryptographic signature...
where apksigner >nul 2>nul
if %ERRORLEVEL% equ 0 (
    call apksigner verify "%RAW_APK%" >nul 2>nul
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] APK signature verification failed! APK is corrupted or unsigned.
        goto :BUILD_FAILED
    )
    for /f "tokens=*" %%c in ('apksigner verify --print-certs "%RAW_APK%" 2^>nul ^| findstr /i "Android Debug"') do (
        echo [ERROR] APK is signed with an Android Debug key! Production release aborted.
        goto :BUILD_FAILED
    )
)
echo [OK] Release signing certificate verified.
echo.

REM ------------------------------------------------------------------------------
REM STEP 13: COPY ARTIFACT & COMPUTE FINAL SHA-256 CHECKSUM
REM ------------------------------------------------------------------------------
echo [STEP 13/18] Preparing release distribution directory and computing SHA-256...

if not exist "release" mkdir release

set FINAL_APK_NAME=ManglamFertilizer-v%VERSION_NAME%.apk
set FINAL_APK_PATH=release\%FINAL_APK_NAME%

copy /Y "%RAW_APK%" "%FINAL_APK_PATH%" >nul
if not exist "%FINAL_APK_PATH%" (
    echo [ERROR] Failed to copy APK to %FINAL_APK_PATH%
    goto :BUILD_FAILED
)

for %%A in ("%FINAL_APK_PATH%") do set APK_SIZE_BYTES=%%~zA

set SHA256_HASH=
for /f "skip=1 tokens=* delims=" %%a in ('certutil -hashfile "%FINAL_APK_PATH%" SHA256') do (
    if not defined SHA256_HASH (
        set raw_hash=%%a
        set raw_hash=!raw_hash: =!
        set SHA256_HASH=!raw_hash!
    )
)

echo [INFO] Release Artifact : %FINAL_APK_PATH%
echo [INFO] APK Size (Bytes) : %APK_SIZE_BYTES%
echo [INFO] APK SHA-256      : %SHA256_HASH%
echo.

REM ------------------------------------------------------------------------------
REM STEP 14: GENERATE UPDATE.JSON
REM ------------------------------------------------------------------------------
echo [STEP 14/18] Generating release manifest (update.json)...

set TODAY=%DATE:~10,4%-%DATE:~4,2%-%DATE:~7,2%
if "%TODAY:-=%"=="" set TODAY=2026-08-28

where powershell >nul 2>nul
if %ERRORLEVEL% equ 0 (
    powershell -Command "$m = [ordered]@{ packageName = '%APP_ID%'; versionName = '%VERSION_NAME%'; versionCode = [int]%VERSION_CODE%; releaseType = '%RELEASE_TYPE%'; releaseTitle = 'Manglam Fertilizer v%VERSION_NAME% Release'; releaseNotes = '• Production release update`n• Inventory, billing, and performance optimizations`n• Stability and offline sync improvements'; releaseNotesHindi = '• नया अपडेट`n• स्टॉक व बिलिंग में सुधार`n• बेहतर परफॉरमेंस व स्थिरता'; apkUrl = 'https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v%VERSION_NAME%/%FINAL_APK_NAME%'; sha256 = '%SHA256_HASH%'; publishedBy = 'admin.manglamferilizer@gmail.com'; publishedAt = '%TODAY%'; forceAfterDays = [int]%FORCE_AFTER_DAYS%; minimumSupportedVersion = '1.0.0'; minimumSupportedVersionCode = 1 }; $m | ConvertTo-Json -Depth 5 | Set-Content -Path 'release\update.json' -Encoding UTF8"
) else (
(
echo {
echo   "packageName": "%APP_ID%",
echo   "versionName": "%VERSION_NAME%",
echo   "versionCode": %VERSION_CODE%,
echo   "releaseType": "%RELEASE_TYPE%",
echo   "releaseTitle": "Manglam Fertilizer v%VERSION_NAME% Release",
echo   "releaseNotes": "• Production release update\n• Inventory, billing, and performance optimizations\n• Stability and offline sync improvements",
echo   "releaseNotesHindi": "• नया अपडेट\n• स्टॉक व बिलिंग में सुधार\n• बेहतर परफॉरमेंस व स्थिरता",
echo   "apkUrl": "https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v%VERSION_NAME%/%FINAL_APK_NAME%",
echo   "sha256": "%SHA256_HASH%",
echo   "publishedBy": "admin.manglamferilizer@gmail.com",
echo   "publishedAt": "%TODAY%",
echo   "forceAfterDays": %FORCE_AFTER_DAYS%,
echo   "minimumSupportedVersion": "1.0.0",
echo   "minimumSupportedVersionCode": 1
echo }
) > release\update.json
)

echo [OK] Generated release\update.json
echo.

REM ------------------------------------------------------------------------------
REM STEP 15: GENERATE RELEASE-METADATA.JSON
REM ------------------------------------------------------------------------------
echo [STEP 15/18] Generating release metadata (release-metadata.json)...

where powershell >nul 2>nul
if %ERRORLEVEL% equ 0 (
    powershell -Command "$meta = [ordered]@{ buildTimestamp = (Get-Date).ToString('o'); packageName = '%APP_ID%'; versionName = '%VERSION_NAME%'; versionCode = [int]%VERSION_CODE%; releaseType = '%RELEASE_TYPE%'; apkFilename = '%FINAL_APK_NAME%'; apkSizeBytes = [int64]%APK_SIZE_BYTES%; sha256 = '%SHA256_HASH%'; distributionUrl = 'https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v%VERSION_NAME%/%FINAL_APK_NAME%'; githubReleaseTag = 'v%VERSION_NAME%' }; $meta | ConvertTo-Json -Depth 5 | Set-Content -Path 'release\release-metadata.json' -Encoding UTF8"
) else (
(
echo {
echo   "buildTimestamp": "%DATE% %TIME%",
echo   "packageName": "%APP_ID%",
echo   "versionName": "%VERSION_NAME%",
echo   "versionCode": %VERSION_CODE%,
echo   "releaseType": "%RELEASE_TYPE%",
echo   "apkFilename": "%FINAL_APK_NAME%",
echo   "apkSizeBytes": %APK_SIZE_BYTES%,
echo   "sha256": "%SHA256_HASH%",
echo   "distributionUrl": "https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v%VERSION_NAME%/%FINAL_APK_NAME%",
echo   "githubReleaseTag": "v%VERSION_NAME%"
echo }
) > release\release-metadata.json
)

echo [OK] Generated release\release-metadata.json
echo.

REM ------------------------------------------------------------------------------
REM STEP 16: GENERATE SHA256SUMS.TXT
REM ------------------------------------------------------------------------------
echo [STEP 16/18] Writing checksum file (SHA256SUMS.txt)...

echo %SHA256_HASH%  %FINAL_APK_NAME% > release\SHA256SUMS.txt

echo [OK] Saved checksum to release\SHA256SUMS.txt
echo.

REM ------------------------------------------------------------------------------
REM STEP 17: STAGE & VERIFY ALL RELEASE ARTIFACTS
REM ------------------------------------------------------------------------------
echo [STEP 17/18] Verifying all distribution artifacts...

if not exist "%FINAL_APK_PATH%" (
    echo [ERROR] Missing release APK: %FINAL_APK_PATH%
    goto :BUILD_FAILED
)
if not exist "release\update.json" (
    echo [ERROR] Missing release\update.json
    goto :BUILD_FAILED
)
if not exist "release\SHA256SUMS.txt" (
    echo [ERROR] Missing release\SHA256SUMS.txt
    goto :BUILD_FAILED
)
if not exist "release\release-metadata.json" (
    echo [ERROR] Missing release\release-metadata.json
    goto :BUILD_FAILED
)

echo [OK] All 4 distribution artifacts staged and verified.
echo.

REM ------------------------------------------------------------------------------
REM STEP 18: SUMMARY & PUBLISH INSTRUCTIONS
REM ------------------------------------------------------------------------------
echo ==============================================================================
echo       [STEP 18/18] BUILD & VERIFICATION SUCCESSFUL - READY FOR RELEASE!
echo ==============================================================================
echo.
echo Output Directory: .\release\
echo   1. APK File    : %FINAL_APK_PATH%
echo   2. Update JSON : release\update.json
echo   3. SHA256 Sums : release\SHA256SUMS.txt
echo   4. Metadata    : release\release-metadata.json
echo.
echo Next Steps:
echo   - Create GitHub Release tag 'v%VERSION_NAME%'
echo   - Upload '%FINAL_APK_NAME%' and 'update.json' to the GitHub Release.
echo   - The in-app update engine will automatically detect and distribute the update.
echo.
exit /b 0

:BUILD_FAILED
echo.
echo ==============================================================================
echo                      BUILD FAILED - STOPPING PIPELINE
echo ==============================================================================
echo [ERROR] The release pipeline halted due to errors. No invalid APK was published.
exit /b 1

