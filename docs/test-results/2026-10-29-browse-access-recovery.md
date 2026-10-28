# Browse request lifetime and session-access recovery

29 October 2026, Asia/Shanghai. Local working tree based on400ec34, including the preceding elapsed-position continuation. Full system acceptance remains incomplete.

## Changes

Native paged MediaBrowser callbacks previously checked completion/source scope only when replying. A late callback could still update revision and item/proof caches after timeout, revocation or a source change. SourceRequest now checks the request lifetime and source/authorization/permission/browser context before cache mutation, rechecks context at completion and cleans up exactly once. Shutdown and detected permission loss cancel pending native pages. Native play-item revalidation now carries the controller's actual authorization predicate through both browse passes.

SourceSessionAccess separates public session discovery/observer registration from Android object ownership. It retries registration after an initial permission denial, handles a grant lost between discovery and registration, unregisters on detected loss and refuses to restart after shutdown. SourceRegistry cancels pending pages, invalidates callback leases, clears controller/item/proof data and advances source epochs when access is lost. Restoring access attaches fresh controllers; an old callback cannot become current merely because permission returned.

Detected missing notification access now disables advertised source capabilities, including saved Mango verification and ready assisted adapters. The shared phone Player/Library explains the recovery action. Explicit GET_STATE/SUBSCRIBE_STATE refreshes session discovery/registration without restarting the companion or invoking playback. No new periodic poll, playback engine or network path is introduced. Actual Android permission delivery and recovery timing remain unverified on hardware.

## Verification

| Status | Check | Evidence / scope |
|---|---|---|
| VERIFIED | Native Android packages, instrumentation APK compilation and lint | scripts/build-android.ps1; final artifacts/browse-access-final-build.txt, BUILD SUCCESSFUL |
| VERIFIED | 57 JVM tests, zero failures/errors | artifacts/browse-access-jvm-tests.json and Android JUnit XML; includes6 new request-lifetime tests,5 session-access tests and1 callback-lease recovery test |
| VERIFIED | Late/duplicate callback behavior | Shared SourceRequest helper tests cover timeout, revoked authorization/permission, stale source/browser, completion recheck, shutdown and cleanup reentrancy before cache work |
| VERIFIED | Software permission-recovery ordering | Shared SourceSessionAccess tests simulate Android read/register SecurityExceptions, startup recovery and loss followed by grant before old callbacks; no stale request/cache mutation or callback-lease resurrection |
| VERIFIED | Shared Flutter analysis,22 tests, release APK and release lint | artifacts/browse-access-{flutter-analyze,flutter-tests,phone-build}.txt; adds Android/iPhone-layout recovery-copy/disabled-control tests to prior20 tests |
| VERIFIED | Release phone permissions inspected | artifacts/browse-access-phone-permissions.txt; Bluetooth/lifecycle permissions only, no Internet/Wi-Fi permissions |
| VERIFIED | Both generated Android packages fail the upgrade signer check | artifacts/browse-access-{host,phone}-signing.json/txt; both installed apps use24f48aa3..., both new packages useed3ffa7b...; no install/uninstall/reset attempted |
| BLOCKED | Actual new companion/phone installation and runtime | Original development signing identity is unavailable in the current environment; installed companion PID15847 remains unchanged |
| NOT TESTED | Actual DX340 notification-access revoke/grant, browse interruption and session recovery | No device permission, Accessibility setting, source playback or volume was changed. Shared helper tests and compile results do not prove the Android integration or firmware behavior |
| NOT TESTED | Current iOS build, physical phone BLE, audible selection and long-run recovery | Mac/iPhone/separate Android phone unavailable; user confirmed DX340 only |

The Android host GET_STATE refresh branch is compile/lint verified. Its complete BLE dispatch and physical permission-recovery path have not been exercised. The phone widget tests supply explicit capability/state fixtures and do not prove that the actual host emits those values on hardware.

## Current generated packages

| Package | SHA256 |
|---|---|
| Companion, not installed | fdc2b7b05e5312470d283a28d32552d98bb483dd1f1a663a4eaf28df67ef063b |
| Shared phone, not installed | e9a5be9a8f65546c9df6d210bb82d32d9605adf5da03f12cc96a866711052c71 |

This supersedes the elapsed-position report for current generated-package hashes and test totals. Its installed baseline hashes/certificate observations remain unchanged. Toolchain remains isolated Flutter3.41.4/Dart3.11.1, process-local Android Studio JBR21.0.7, SDK36/build-tools36.1.0. Source is saved as a local checkpoint on `codex/source-recovery-checkpoint`; no publication occurred.

## Physical continuation

Restore the original signing identity securely outside Git and rebuild before updating. Preserve installed app data. On the actual DX340, test starting the companion before notification access is granted; grant access, request fresh state and verify capabilities/metadata recover. Revoke access during native browse and during selected-item revalidation, restore it before a delayed callback, and verify old rows/revisions cannot act or repopulate caches. Repeat with a source change, session replacement and companion stop while browsing. Record exact versions, source state and audible outcome separately. Then repeat through each real phone's BLE path.

Keep the separate Accessibility-service recovery limitation visible; this code does not claim to repair an enabled-but-unbound Accessibility service. Do not toggle permissions automatically for acceptance tests.

Public API contracts checked: [MediaSessionManager getActiveSessions and observer registration](https://developer.android.com/reference/android/media/session/MediaSessionManager), [NotificationListenerService lifecycle](https://developer.android.com/reference/android/service/notification/NotificationListenerService). Uses Android13-compatible APIs; no newer package-specific active-session API is used.
