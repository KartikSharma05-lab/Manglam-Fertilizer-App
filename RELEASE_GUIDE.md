# Manglam Fertilizer — Production Release & In-App Update Guide

This guide details the complete, end-to-end production release lifecycle for the **Manglam Fertilizer** Android application (`com.manglamfertilizer.app`), covering everything from local source modification and cryptographic signing to GitHub Actions automated releases, in-app client detection, verification, authorized installation, and post-install confirmation.

---

## 1. Production Signing Policy (MANDATORY)

> [!IMPORTANT]
> **Production Signing is Strict & Mandatory**:
> - A production release **MUST NOT** use a debug keystore (`debug.keystore`).
> - If a production keystore is unavailable or unconfigured, the build system **FAILS THE BUILD** with:
>   `"Production release keystore is not configured. Build aborted."`
> - The build pipeline **never** silently falls back to debug signing for production release artifacts.

---

## 2. Release Key Permanence & Continuity

> [!CAUTION]
> **CRITICAL KEY CONTINUITY MANDATE**:
> Android's Package Manager strictly verifies cryptographic signing certificates when installing updates.
> The **exact same production signing key** must be used for:
> - **v1.0.0** (Baseline Production)
> - **v1.0.1**
> - **v1.0.2**
> - **v1.1.0**
> - **All future releases**
>
> If the signing key is changed or regenerated, users will encounter `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and will be unable to install updates without uninstalling the app first (which loses local data).
>
> **Keep your production `.jks` or `.keystore` file securely backed up and NEVER lose or overwrite it.**

---

## 3. End-to-End 20-Step Production Release & Update Lifecycle

The complete update infrastructure operates across the following 20 discrete stages:

```
[ DEVELOPER & SOURCE CODE ]
 1. Make code changes and verify app stability.
 2. Increase versionName in app/build.gradle.kts (e.g. "1.0.0" -> "1.0.1").
 3. Increase versionCode in app/build.gradle.kts (e.g. 1 -> 2, must be strictly increasing).
 4. Build and run unit tests locally (gradle :app:testDebugUnitTest).

[ PIPELINE EXECUTION & CONFIGURATION ]
 5. Run one-click release process (build-release.sh / .bat / .ps1) OR push a Git tag ('v1.0.1') for GitHub Actions.
 6. Configure release type: OPTIONAL, RECOMMENDED, SILENT, FORCED, or SECURITY_CRITICAL.
 7. Configure bilingual release notes (English and Hindi changelogs).
 8. Configure force period (e.g. 15 days grace period for RECOMMENDED updates).
 9. Configure minimum supported version code (e.g. 1).

[ CI/CD AUTOMATION & ARTIFACT GENERATION ]
10. GitHub Actions builds signed production APK (rejects debug certificates).
11. GitHub Actions generates SHA-256 checksum on signed APK.
12. GitHub Actions creates update.json, release-metadata.json, and SHA256SUMS.txt.
13. GitHub Release is published atomically with all 4 release artifacts.

[ CLIENT APP UPDATE CYCLE ]
14. Existing users detect update via https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/latest/download/update.json.
15. Users receive a daily throttled notification (or in-app alert dialog).
16. Client downloads APK with atomic temp-file streaming.
17. 8-Point security verification runs (SHA-256, package identity, signature continuity, monotonic version check).
18. Android system package installer performs user-authorized installation.
19. New version starts on device.
20. App verifies installedVersionCode >= pendingInstallVersionCode, logs UPDATE_COMPLETED, clears cache, and resets update state.
```

---

## 4. One-Click Build Commands

### For Linux / macOS (Bash):
```bash
chmod +x build-release.sh
./build-release.sh
```
*With custom arguments*:
```bash
./build-release.sh "1.0.1" 2 "RECOMMENDED" "Manglam Fertilizer v1.0.1" "Changelog notes" "हिंदी नोट्स" 15 1
```

### For Windows (Batch):
```cmd
build-release.bat
```
*With custom arguments*:
```cmd
build-release.bat 1.0.1 2 RECOMMENDED 15
```

### For Windows (PowerShell):
```powershell
.\build-release.ps1
```
*With custom parameters*:
```powershell
.\build-release.ps1 -VersionName "1.0.1" -VersionCode 2 -ReleaseType RECOMMENDED -ForceAfterDays 15
```

---

## 5. Production Keystore Configuration

### Option A: Local File via `keystore.properties` (Recommended for Local Dev)
1. Create `keystore.properties` in the project root:
   ```properties
   storeFile=my-upload-key.jks
   storePassword=YourKeystorePassword
   keyAlias=upload
   keyPassword=YourKeyPassword
   ```
2. Note: `keystore.properties` and all `*.jks` / `*.keystore` files are strictly excluded by `.gitignore`.

### Option B: Local Environment Variables
Set the following environment variables before building:
- `KEYSTORE_PATH` — Full path to your `.jks` file
- `STORE_PASSWORD` — Keystore password
- `KEY_ALIAS` — Key alias
- `KEY_PASSWORD` — Key password

### Option C: GitHub Actions CI/CD Secrets
Configure encrypted secrets in GitHub (`Settings -> Secrets and variables -> Actions`):
- `KEYSTORE_BASE64` — Base64-encoded release key: `base64 -w 0 my-upload-key.jks`
- `KEYSTORE_PASSWORD` — Keystore password
- `KEY_ALIAS` — Key alias
- `KEY_PASSWORD` — Key password

---

## 6. Distribution Artifacts in `release/`

When the build completes, four production artifacts are generated in `release/`:

| Artifact | Description |
|---|---|
| `release/ManglamFertilizer-v<version>.apk` | Signed, production-ready release APK file. |
| `release/update.json` | Manifest consumed by the client app update engine. |
| `release/SHA256SUMS.txt` | Cryptographic SHA-256 hash file for integrity verification. |
| `release/release-metadata.json` | Machine-readable build and release metadata. |

---

## 7. How to Publish a GitHub Release

1. Increment `versionCode` and `versionName` in `app/build.gradle.kts` (e.g. `versionCode = 2`, `versionName = "1.0.1"`).
2. Run `./build-release.sh` (or `build-release.bat` / `build-release.ps1`).
3. Commit and push a tag:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```
4. On GitHub, create a Release for tag `v1.0.1` and upload all 4 artifacts:
   - `ManglamFertilizer-v1.0.1.apk`
   - `update.json`
   - `SHA256SUMS.txt`
   - `release-metadata.json`
5. All installed client applications will automatically detect the update from:
   `https://github.com/KartikSharma05-lab/Manglam-Fertilizer-App/releases/latest/download/update.json`

