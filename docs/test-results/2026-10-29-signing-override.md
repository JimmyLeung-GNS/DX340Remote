# Project-scoped development signing

29 October 2026, Asia/Shanghai. Follow-up to local checkpoint f376699. The user confirms the original key/computer is available. Its readable local path on this PC is still pending, so the original identity has not yet been validated or used.

## Implementation

The common android/development-signing.gradle reads process-local DX340_DEBUG_KEYSTORE. All three native Android app modules and the shared Flutter Android app apply it. A supplied value must be an absolute existing file; standard Android debug alias/password settings remain in place. The Flutter development release uses the same debug signing configuration. An unset override preserves normal Android signing behavior. No global key, environment setting, app identifier or installed data is replaced.

The first attempted Kotlin script plugin did not receive the Android plugin's compile classpath; it was replaced with a small common Gradle Groovy script. Final native and Flutter builds pass. This is build configuration only; application behavior is unchanged from the preceding checkpoint.

## Verification

| Status | Check | Evidence / scope |
|---|---|---|
| VERIFIED | Default signing configuration | artifacts/signing-override-default.txt; existing default key/certificate ed3ffa7b... retained |
| VERIFIED | Actual companion APK signed with a selected alternate key | Isolated disposable standard debug key; real Gradle assembleDebug; artifacts/signing-override-fixture-build.txt and signing-override-certificate-check.json show the fixture certificate differs from the default |
| VERIFIED | Shared phone release/debug override | artifacts/signing-override-phone-fixture.txt; release-specific block independently checked in signing-override-phone-release-block.txt and identifies the fixture key/certificate |
| VERIFIED | Missing key fails instead of silently falling back | artifacts/signing-override-missing.txt; deliberate nonexistent path rejected during configuration |
| VERIFIED | Default-key isolation and cleanup | artifacts/signing-override-isolation.json; original default keystore hash unchanged, no user-level DX340_DEBUG_KEYSTORE variable; disposable fixture keystore/APK removed from ignored work after verification |
| VERIFIED | Final native build/lint and57 JVM tests | scripts/build-android.ps1 without override; artifacts/signing-override-android-build.txt, BUILD SUCCESSFUL |
| VERIFIED | Final shared-phone analysis/22 tests/release APK/release lint | scripts/build-phone.ps1 without override; artifacts/signing-override-phone-build.txt and standard Flutter logs |
| VERIFIED | Final packages still blocked from updating installed apps | Read-only signing preflights, artifacts/signing-override-{host,phone}-preflight.json/txt. Both still use the PC's ed3ffa7b... key and differ from installed24f48aa3... |
| BLOCKED | Original-key build and in-place update | User must provide a readable local file path for the original keystore; no new package was installed |
| NOT TESTED | Actual device runtime, current iOS build and phone BLE | Original-key update and physical phone/Mac gates remain open |

Current generated companion SHA256: `1d1870cfb62fb863bdbe93f81f56a993909c34144c2ea6b14faac26463d0c7e5`. Shared-phone SHA256 remains `e9a5be9a8f65546c9df6d210bb82d32d9605adf5da03f12cc96a866711052c71`. These are default-PC-key packages, not original-key packages. Earlier generated companion hashes are historical; installed APK baselines are unchanged. The disposable test APK was never installed or copied to a distributable artifact filename.

## Next action

Use the original keystore at a user-provided path outside Git. Set DX340_DEBUG_KEYSTORE only for the build process, rebuild both apps and compare their actual signing certificates with the installed24f48aa3... identity using verify-android-signing.ps1. Update only after a match. Preserve the installed baseline APKs, permissions, source settings and app data; do not reset/uninstall to solve a mismatch. Exact PowerShell steps are in installation.md. No publication or global device/toolchain configuration change occurred.

Primary references: [Android debug signing](https://developer.android.com/studio/publish/app-signing#debug-mode), [Gradle environment-variable provider](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.provider/-provider-factory/environment-variable.html). Public certificate fingerprints are not private key material; no original key contents are included in tracked files.
