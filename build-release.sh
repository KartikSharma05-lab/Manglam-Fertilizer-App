#!/usr/bin/env bash
# ==============================================================================
# Manglam Fertilizer - One-Click Production Release APK Build Pipeline (POSIX)
# ==============================================================================

set -euo pipefail

echo "=============================================================================="
echo "      MANGLAM FERTILIZER - ONE-CLICK RELEASE APK BUILD PIPELINE               "
echo "=============================================================================="
echo "[INFO] Starting release build pipeline at $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# ------------------------------------------------------------------------------
# STEP 1: VALIDATE PROJECT INTEGRITY & STRUCTURE (DO NOT DAMAGE SOURCE)
# ------------------------------------------------------------------------------
echo "[STEP 1/18] Validating project integrity and critical configuration..."

for required_path in "app" "app/build.gradle.kts" "app/src/main/AndroidManifest.xml" "gradle/libs.versions.toml" "settings.gradle.kts"; do
    if [ ! -e "$required_path" ]; then
        echo "[ERROR] Required path '$required_path' is missing! Aborting build to protect integrity." >&2
        exit 1
    fi
done

echo "[OK] Project structure validated successfully."
echo ""

# ------------------------------------------------------------------------------
# STEP 2: EXTRACT & READ VERSION CONFIGURATION
# ------------------------------------------------------------------------------
echo "[STEP 2/18] Extracting version information..."

DETECTED_VERSION_NAME=$(grep -oE 'versionName[[:space:]]*=[[:space:]]*"[^"]+"' app/build.gradle.kts | head -n1 | sed -E 's/.*"([^"]+)".*/\1/' || echo "1.0.0")
DETECTED_VERSION_CODE=$(grep -oE 'versionCode[[:space:]]*=[[:space:]]*[0-9]+' app/build.gradle.kts | head -n1 | sed -E 's/.*=[[:space:]]*([0-9]+).*/\1/' || echo "1")
PACKAGE_NAME="com.manglamfertilizer.app"

VERSION_NAME="${1:-$DETECTED_VERSION_NAME}"
VERSION_CODE="${2:-$DETECTED_VERSION_CODE}"
RELEASE_TYPE="${3:-RECOMMENDED}"
RELEASE_TITLE="${4:-Manglam Fertilizer v${VERSION_NAME} Release}"
RELEASE_NOTES="${5:-• Production release update\n• Inventory, billing, and performance optimizations\n• Stability and offline sync improvements}"
RELEASE_NOTES_HINDI="${6:-• नया अपडेट\n• स्टॉक व बिलिंग में सुधार\n• बेहतर परफॉरमेंस व स्थिरता}"
FORCE_AFTER_DAYS="${7:-15}"
MIN_SUPPORTED_VERSION_CODE="${8:-1}"

# ------------------------------------------------------------------------------
# STEP 3: VALIDATE VERSION
# ------------------------------------------------------------------------------
echo "[STEP 3/18] Validating version parameters..."

if [ -z "$VERSION_NAME" ]; then
    echo "[ERROR] Invalid Version Name: cannot be empty." >&2
    exit 1
fi

if [ -z "$VERSION_CODE" ] || [ "$VERSION_CODE" -le 0 ]; then
    echo "[ERROR] Invalid Version Code '$VERSION_CODE'. Must be an integer > 0." >&2
    exit 1
fi

echo "[INFO] Target Version Name : $VERSION_NAME"
echo "[INFO] Target Version Code : $VERSION_CODE"
echo "[INFO] Target Release Type : $RELEASE_TYPE"
echo "[OK] Version parameters validated."
echo ""

# ------------------------------------------------------------------------------
# STEP 4: VALIDATE APPLICATION / PACKAGE IDENTIFIER
# ------------------------------------------------------------------------------
echo "[STEP 4/18] Validating target package name..."

APP_ID_CONFIG=$(grep -oE 'applicationId[[:space:]]*=[[:space:]]*"[^"]+"' app/build.gradle.kts | head -n1 | sed -E 's/.*"([^"]+)".*/\1/' || echo "")

if [ "$APP_ID_CONFIG" != "$PACKAGE_NAME" ]; then
    echo "[ERROR] Invalid application ID: found '$APP_ID_CONFIG', expected '$PACKAGE_NAME'." >&2
    exit 1
fi

echo "[INFO] Application ID: $PACKAGE_NAME"
echo "[OK] Package identifier validated."
echo ""

# ------------------------------------------------------------------------------
# STEP 5: VALIDATE PRODUCTION KEYSTORE
# ------------------------------------------------------------------------------
echo "[STEP 5/18] Validating production signing keystore..."

PROD_KEYSTORE_FOUND="false"

if [ -n "${KEYSTORE_PATH:-}" ] && [ -f "$KEYSTORE_PATH" ]; then
    PROD_KEYSTORE_FOUND="true"
    echo "[INFO] Using production keystore from KEYSTORE_PATH: $KEYSTORE_PATH"
elif [ -f "keystore.properties" ]; then
    STORE_FILE_PROP=$(grep -E '^[[:space:]]*storeFile[[:space:]]*=' keystore.properties | cut -d'=' -f2- | tr -d ' \r' || true)
    if [ -n "$STORE_FILE_PROP" ] && [ -f "$STORE_FILE_PROP" ]; then
        PROD_KEYSTORE_FOUND="true"
        echo "[INFO] Using production keystore from keystore.properties: $STORE_FILE_PROP"
    fi
elif [ -f "my-upload-key.jks" ]; then
    PROD_KEYSTORE_FOUND="true"
    echo "[INFO] Using production keystore: my-upload-key.jks"
fi

if [ "$PROD_KEYSTORE_FOUND" != "true" ]; then
    echo "==============================================================================" >&2
    echo "[ERROR] Production release keystore is not configured. Build aborted." >&2
    echo "==============================================================================" >&2
    echo "To configure a production keystore, either:" >&2
    echo "  1. Create 'keystore.properties' with storeFile, storePassword, keyAlias, keyPassword" >&2
    echo "  2. Set environment variables: KEYSTORE_PATH, STORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD" >&2
    echo "  3. Place 'my-upload-key.jks' in the project root" >&2
    exit 1
fi

echo "[OK] Production keystore validated."
echo ""

# ------------------------------------------------------------------------------
# STEP 6: RUN UNIT & CONSISTENCY TESTS
# ------------------------------------------------------------------------------
echo "[STEP 6/18] Running Unit & Consistency Tests..."

GRADLE_CMD="./gradlew"
if [ ! -x "./gradlew" ]; then
    if command -v gradle >/dev/null 2>&1; then
        GRADLE_CMD="gradle"
    else
        chmod +x ./gradlew 2>/dev/null || true
    fi
fi

if [ "${SKIP_TESTS:-false}" != "true" ]; then
    echo "[INFO] Executing: $GRADLE_CMD testDebugUnitTest --no-daemon"
    $GRADLE_CMD testDebugUnitTest --no-daemon
    echo "[OK] Unit tests passed successfully."
else
    echo "[INFO] Skipping tests (SKIP_TESTS=true)."
fi
echo ""

# ------------------------------------------------------------------------------
# STEP 7: BUILD RELEASE APK
# ------------------------------------------------------------------------------
echo "[STEP 7/18] Compiling Release APK with Gradle..."

echo "[INFO] Executing: $GRADLE_CMD assembleRelease --no-daemon"
$GRADLE_CMD assembleRelease --no-daemon

echo "[OK] Release APK assembled successfully."
echo ""

# ------------------------------------------------------------------------------
# STEP 8: VERIFY APK ARTIFACT EXISTS
# ------------------------------------------------------------------------------
echo "[STEP 8/18] Locating compiled APK build output..."

CANDIDATES=()
while IFS= read -r apk_file; do
    [ -n "$apk_file" ] && CANDIDATES+=("$apk_file")
done < <(find app/build/outputs -type f -name "*.apk" 2>/dev/null \
    | grep -v -i -E "debug|androidTest|test|unsigned|intermediates" \
    | sort -u || true)

if [ ${#CANDIDATES[@]} -eq 0 ] && [ -d "app/build/outputs/apk/release" ]; then
    while IFS= read -r apk_file; do
        [ -n "$apk_file" ] && CANDIDATES+=("$apk_file")
    done < <(find app/build/outputs/apk/release -type f -name "*.apk" 2>/dev/null \
        | grep -v -i -E "debug|androidTest|test|intermediates" \
        | sort -u || true)
fi

if [ ${#CANDIDATES[@]} -eq 0 ]; then
    echo "[ERROR] No release APK artifact found in app/build/outputs! Build failed." >&2
    if [ -d "app/build/outputs" ]; then
        echo "[DIAGNOSTIC] Current build outputs:" >&2
        ls -laR app/build/outputs >&2 || true
    fi
    exit 1
fi

if [ ${#CANDIDATES[@]} -gt 1 ]; then
    echo "[ERROR] Multiple release APK candidates found! Ambiguous build output:" >&2
    for c in "${CANDIDATES[@]}"; do
        echo "  Candidate: $c" >&2
    done
    exit 1
fi

RAW_APK="${CANDIDATES[0]}"

echo "[OK] Located Source APK: $RAW_APK"
echo ""

# ------------------------------------------------------------------------------
# STEP 9: VERIFY PACKAGE NAME IN COMPILED APK
# ------------------------------------------------------------------------------
echo "[STEP 9/18] Verifying APK package name..."

if command -v aapt >/dev/null 2>&1; then
    APK_PKG=$(aapt dump badging "$RAW_APK" 2>/dev/null | grep -oE "package: name='[^']+'" | cut -d"'" -f2 || true)
    if [ -n "$APK_PKG" ] && [ "$APK_PKG" != "$PACKAGE_NAME" ]; then
        echo "[ERROR] APK Package verification failed! Expected '$PACKAGE_NAME', found '$APK_PKG'." >&2
        exit 1
    fi
fi
echo "[OK] Package name verified in APK."
echo ""

# ------------------------------------------------------------------------------
# STEP 10: VERIFY VERSION NAME IN COMPILED APK
# ------------------------------------------------------------------------------
echo "[STEP 10/18] Verifying APK versionName..."

if command -v aapt >/dev/null 2>&1; then
    APK_VNAME=$(aapt dump badging "$RAW_APK" 2>/dev/null | grep -oE "versionName='[^']+'" | cut -d"'" -f2 || true)
    if [ -n "$APK_VNAME" ] && [ "$APK_VNAME" != "$VERSION_NAME" ]; then
        echo "[WARN] APK versionName '$APK_VNAME' differs from target '$VERSION_NAME'."
    fi
fi
echo "[OK] Version name checked."
echo ""

# ------------------------------------------------------------------------------
# STEP 11: VERIFY VERSION CODE IN COMPILED APK
# ------------------------------------------------------------------------------
echo "[STEP 11/18] Verifying APK versionCode..."

if command -v aapt >/dev/null 2>&1; then
    APK_VCODE=$(aapt dump badging "$RAW_APK" 2>/dev/null | grep -oE "versionCode='[0-9]+'" | grep -oE '[0-9]+' || true)
    if [ -n "$APK_VCODE" ] && [ "$APK_VCODE" -le 0 ]; then
        echo "[ERROR] Invalid APK versionCode: '$APK_VCODE'." >&2
        exit 1
    fi
fi
echo "[OK] Version code checked."
echo ""

# ------------------------------------------------------------------------------
# STEP 12: VERIFY RELEASE SIGNING CERTIFICATE
# ------------------------------------------------------------------------------
echo "[STEP 12/18] Verifying release cryptographic signature..."

if command -v apksigner >/dev/null 2>&1; then
    if ! apksigner verify "$RAW_APK" >/dev/null 2>&1; then
        echo "[ERROR] APK signature verification failed! APK is corrupted or unsigned." >&2
        exit 1
    fi
    CERT_INFO=$(apksigner verify --print-certs "$RAW_APK" 2>/dev/null || true)
    if echo "$CERT_INFO" | grep -qi "Android Debug"; then
        echo "[ERROR] APK is signed with an Android Debug key! Production release aborted." >&2
        exit 1
    fi
elif command -v jarsigner >/dev/null 2>&1; then
    if ! jarsigner -verify "$RAW_APK" >/dev/null 2>&1; then
        echo "[ERROR] APK jarsigner verification failed!" >&2
        exit 1
    fi
fi
echo "[OK] Release signing certificate verified."
echo ""

# ------------------------------------------------------------------------------
# STEP 13: COPY ARTIFACT & COMPUTE FINAL SHA-256 CHECKSUM
# ------------------------------------------------------------------------------
echo "[STEP 13/18] Preparing release directory and calculating SHA-256 hash..."

mkdir -p release

FINAL_APK_NAME="ManglamFertilizer-v${VERSION_NAME}.apk"
FINAL_APK_PATH="release/${FINAL_APK_NAME}"

cp -f "$RAW_APK" "$FINAL_APK_PATH"

if command -v sha256sum >/dev/null 2>&1; then
    SHA256_HASH=$(sha256sum "$FINAL_APK_PATH" | awk '{print $1}')
elif command -v shasum >/dev/null 2>&1; then
    SHA256_HASH=$(shasum -a 256 "$FINAL_APK_PATH" | awk '{print $1}')
else
    SHA256_HASH=$(openssl dgst -sha256 "$FINAL_APK_PATH" | awk '{print $NF}')
fi

APK_SIZE_BYTES=$(wc -c < "$FINAL_APK_PATH" | tr -d ' ')

echo "[INFO] Release Artifact : $FINAL_APK_PATH"
echo "[INFO] APK Size (Bytes) : $APK_SIZE_BYTES"
echo "[INFO] APK SHA-256      : $SHA256_HASH"
echo ""

# ------------------------------------------------------------------------------
# STEP 14: GENERATE UPDATE.JSON (SAFE JSON ENCODING)
# ------------------------------------------------------------------------------
echo "[STEP 14/18] Generating release manifest (update.json)..."

TODAY_STR=$(date '+%Y-%m-%d')
APK_URL="https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/download/v${VERSION_NAME}/${FINAL_APK_NAME}"

if command -v python3 >/dev/null 2>&1; then
    python3 -c '
import json, sys

data = {
    "packageName": sys.argv[1],
    "versionName": sys.argv[2],
    "versionCode": int(sys.argv[3]),
    "releaseType": sys.argv[4],
    "releaseTitle": sys.argv[5],
    "releaseNotes": sys.argv[6],
    "releaseNotesHindi": sys.argv[7],
    "apkUrl": sys.argv[8],
    "sha256": sys.argv[9],
    "publishedBy": sys.argv[10],
    "publishedAt": sys.argv[11],
    "forceAfterDays": int(sys.argv[12]),
    "minimumSupportedVersion": sys.argv[13],
    "minimumSupportedVersionCode": int(sys.argv[14])
}

with open("release/update.json", "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)
' "$PACKAGE_NAME" "$VERSION_NAME" "$VERSION_CODE" "$RELEASE_TYPE" "$RELEASE_TITLE" "$RELEASE_NOTES" "$RELEASE_NOTES_HINDI" "$APK_URL" "$SHA256_HASH" "admin.manglamferilizer@gmail.com" "$TODAY_STR" "$FORCE_AFTER_DAYS" "1.0.0" "$MIN_SUPPORTED_VERSION_CODE"
else
    cat <<EOF > release/update.json
{
  "packageName": "${PACKAGE_NAME}",
  "versionName": "${VERSION_NAME}",
  "versionCode": ${VERSION_CODE},
  "releaseType": "${RELEASE_TYPE}",
  "releaseTitle": "${RELEASE_TITLE}",
  "releaseNotes": "${RELEASE_NOTES}",
  "releaseNotesHindi": "${RELEASE_NOTES_HINDI}",
  "apkUrl": "${APK_URL}",
  "sha256": "${SHA256_HASH}",
  "publishedBy": "admin.manglamferilizer@gmail.com",
  "publishedAt": "${TODAY_STR}",
  "forceAfterDays": ${FORCE_AFTER_DAYS},
  "minimumSupportedVersion": "1.0.0",
  "minimumSupportedVersionCode": ${MIN_SUPPORTED_VERSION_CODE}
}
EOF
fi

echo "[OK] Successfully generated release/update.json"
echo ""

# ------------------------------------------------------------------------------
# STEP 15: GENERATE RELEASE-METADATA.JSON
# ------------------------------------------------------------------------------
echo "[STEP 15/18] Generating release metadata (release-metadata.json)..."

BUILD_TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')

if command -v python3 >/dev/null 2>&1; then
    python3 -c '
import json, sys

meta = {
    "buildTimestamp": sys.argv[1],
    "packageName": sys.argv[2],
    "versionName": sys.argv[3],
    "versionCode": int(sys.argv[4]),
    "releaseType": sys.argv[5],
    "apkFilename": sys.argv[6],
    "apkSizeBytes": int(sys.argv[7]),
    "sha256": sys.argv[8],
    "distributionUrl": sys.argv[9],
    "githubReleaseTag": sys.argv[10]
}

with open("release/release-metadata.json", "w", encoding="utf-8") as f:
    json.dump(meta, f, indent=2, ensure_ascii=False)
' "$BUILD_TIMESTAMP" "$PACKAGE_NAME" "$VERSION_NAME" "$VERSION_CODE" "$RELEASE_TYPE" "$FINAL_APK_NAME" "$APK_SIZE_BYTES" "$SHA256_HASH" "$APK_URL" "v${VERSION_NAME}"
else
    cat <<EOF > release/release-metadata.json
{
  "buildTimestamp": "${BUILD_TIMESTAMP}",
  "packageName": "${PACKAGE_NAME}",
  "versionName": "${VERSION_NAME}",
  "versionCode": ${VERSION_CODE},
  "releaseType": "${RELEASE_TYPE}",
  "apkFilename": "${FINAL_APK_NAME}",
  "apkSizeBytes": ${APK_SIZE_BYTES},
  "sha256": "${SHA256_HASH}",
  "distributionUrl": "${APK_URL}",
  "githubReleaseTag": "v${VERSION_NAME}"
}
EOF
fi

echo "[OK] Successfully generated release/release-metadata.json"
echo ""

# ------------------------------------------------------------------------------
# STEP 16: GENERATE SHA256SUMS.TXT
# ------------------------------------------------------------------------------
echo "[STEP 16/18] Writing checksum file (SHA256SUMS.txt)..."

echo "$SHA256_HASH  $FINAL_APK_NAME" > release/SHA256SUMS.txt

echo "[OK] Checksum saved to release/SHA256SUMS.txt"
echo ""

# ------------------------------------------------------------------------------
# STEP 17: STAGE & VERIFY ALL RELEASE ARTIFACTS
# ------------------------------------------------------------------------------
echo "[STEP 17/18] Verifying all release artifacts in release/..."

for artifact in "release/${FINAL_APK_NAME}" "release/update.json" "release/SHA256SUMS.txt" "release/release-metadata.json"; do
    if [ ! -f "$artifact" ]; then
        echo "[ERROR] Missing release artifact: $artifact" >&2
        exit 1
    fi
done

echo "[OK] All 4 distribution artifacts staged and verified."
echo ""

# ------------------------------------------------------------------------------
# STEP 18: SUMMARY & PUBLISH INSTRUCTIONS
# ------------------------------------------------------------------------------
echo "=============================================================================="
echo "      [STEP 18/18] BUILD & VERIFICATION SUCCESSFUL - READY FOR RELEASE!        "
echo "=============================================================================="
echo ""
echo "Release Artifacts:"
echo "  1. APK File    : ${FINAL_APK_PATH}"
echo "  2. Update JSON : release/update.json"
echo "  3. SHA-256 Sum : release/SHA256SUMS.txt"
echo "  4. Metadata    : release/release-metadata.json"
echo ""
echo "Publish Instructions:"
echo "  1. Create a GitHub Release tag: 'v${VERSION_NAME}'"
echo "  2. Upload '${FINAL_APK_NAME}' and 'update.json' as Release Assets"
echo "  3. Client apps will automatically detect and verify the update via GitHub Releases."
echo ""

