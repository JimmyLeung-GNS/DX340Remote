# Development handoff

Updated 29 October 2026, Asia/Shanghai. Work in `C:\GitHub\DX340Remote`.

Current resume: user asks for help copying the key to `C:\Users\user.android\dx340-original-debug.keystore` (literal requested path). That file and the earlier `.android` destination are absent. User confirms the key remains only on the other Windows PC. No readable USB transfer is connected. Current PC's default key was freshly checked and still has the nonmatching ed3ffa7b... certificate. Wait for the user to copy the original file onto USB and connect it here, or provide an accessible source; then perform the destination copy, certificate comparison, project-only signing rebuild and in-place update. Do not substitute this PC's default key. Goal has resumed; use a fresh blocked audit for this resumed run.

This file was absent when the user requested continuation. It is reconstructed from the checked-in `AGENTS.md`, implementation plan, status and commit400ec34, plus the work recorded below. It does not claim to recover an older missing file's contents.

## Read first

1. `AGENTS.md`, `docs/implementation-plan.md`, `docs/status.md`.
2. `docs/completion-audit.md` and `docs/test-results/2026-10-29-indication-watchdog.md` for the current blocker audit and transport work; the signing/browse/elapsed reports document preceding changes.
3. `docs/phone-reliability-runbook.md`, `docs/compatibility-matrix.md`, and the separate Mango/Apple experiment reports before physical acceptance work.

## Saved implementation

The canonical phone interface is Flutter/Dart over native Kotlin BLE/service and Swift CoreBluetooth/restoration. Host and source adapters stay Kotlin. Native phone reference apps are retained. BLE carries controls/metadata only; existing DX340 Mango and Apple Music apps play the music. Optional screen-on/unlocked Apple and Mango assisted browsing were accepted in prior work. Replacing Mango, root, firmware changes and download extraction remain excluded.

Checkpoint400ec34 contains source-session callback ownership/capability guards and guarded downward-only debug volume probes. Phone volume remains disabled. Prior local Mango WAV/FLAC and Apple downloaded outside-queue selections have user-reported audible confirmations; those are not phone Bluetooth acceptance. Full format/library/SD/playlist, reliability and audio matrices remain incomplete.

This continuation adds host position normalization and shared-phone elapsed estimates with native sample identity/age, lifecycle freeze/fresh-resume guards and no periodic BLE polling. See the test report and current working tree. New Swift changes require a fresh Mac build; earlier iOS CI results do not verify this source revision. No publication is performed by this continuation.

Latest local verification: native build/lint64 JVM tests, Flutter analysis22 tests/release APK/release lint passed. Companion and phone packages are generated in artifacts but **not installed**. Both installed apps' certificate24f48aa3... differs from this checkout's ed3ffa7b... development key. The verified upgrade preflight stops before installation. Restore the original key securely outside Git before rebuilding/upgrading; preserve installed data. Current raw package hashes are in the indication-watchdog report; full public certificate fingerprints are in the preceding elapsed report. Source is saved on local branch `codex/source-recovery-checkpoint`; use `git log -1` for the current checkpoint. It has not been pushed or published.

The host now has a tested confirmed-frame queue with per-indication completion deadlines, bounded failure cleanup and control/artwork priority. Native queue failures clear logical peer/authentication before requesting OS disconnect, without replaying controls. Software scheduling tests do not prove real BLE callback ordering, sleep timing or same-peer reconnect behavior. The connected Apple tool was also checked directly and failed to list simulators with xcrun ENOENT; a Mac/iOS build remains unavailable here.

The preceding continuation guards native browse-request lifetime before cache mutation, carries authorization through play-item revalidation, invalidates requests/callback ownership/source epochs on detected notification-access loss, retries session observer registration after grant, and adds explicit refresh/recovery UI. Pure helper tests cover ordering/failure cases; actual device permission delivery and recovery are not tested.

The user confirms the original build was on another available Windows PC but does not know the key path. A narrow check of this PC's standard Android signing locations found only its existing nonmatching default key. On the original PC, open `%USERPROFILE%\.android` in File Explorer and copy debug.keystore, preserving the original. Suggested destination on this PC is `C:\Users\user\.android\dx340-original-debug.keystore`; keep this PC's existing debug.keystore unchanged. Transfer/certificate validation remain pending. The shared android/development-signing.gradle supports process-local DX340_DEBUG_KEYSTORE for all Android builds. Real alternate-key packaging, phone release selection, missing-path rejection and default-key restoration builds are verified. Use installation.md's try/finally environment override once the original file is accessible; validate actual certificates before updating.

## Local environment and reproducible commands

The connected ADB target `d` is the DX340, Android13/API33, build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. It is not a separate Android phone. No physical iPhone, personal Mac or signing team is accessible here.

The user explicitly confirmed DX340 only for now during this continuation. Read-only dumps show Mango and Apple both paused; no playback/volume test was performed. Installed apps and companion services were preserved.

This checkout started without the old ignored work/artifacts/local.properties. Historical raw evidence paths in status belong to the earlier environment and are not presumed present here. Global Flutter resolves to3.41.7; Java was absent from PATH. Official Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0 is cloned into ignored `work/flutter`. Use the installed Android Studio JBR21.0.7 locally; do not change global Flutter/Java settings.

```powershell
Set-Location C:\GitHub\DX340Remote
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\user\AppData\Local\Android\Sdk'
$env:PATH = "$PWD\work\flutter\bin;$env:PATH"
./scripts/build-android.ps1
./scripts/build-phone.ps1
```

If `work/flutter` is missing, clone the official3.41.4 tag and verify the full revision before building. The Android-only project needs ignored `android/local.properties` containing `sdk.dir=C:/Users/user/AppData/Local/Android/Sdk`. Flutter generates its own ignored phone local properties. Signing keys stay outside Git. Keep prior installed APKs/signers for upgrade comparison; never uninstall or reset app data merely to bypass a signing mismatch.

On Mac, install the same Flutter revision and run `sh scripts/build-phone-ios.sh <available-iPhone-simulator-UDID>` from the repository root. Physical iPhone deployment uses `phone/ios/Runner.xcworkspace`, a compatible Xcode and the user's signing team. Preserve native `apple/` as reference.

## Resume next

1. Obtain the user-provided readable path for the available original development key. Use DX340_DEBUG_KEYSTORE for project-only signing, rebuild and run both signing preflights before any update. Validate the new Swift bridge/native sample timing on Mac, then both physical phones over BLE. Verify pause/seek/track/source changes, stale artwork/status events, lock/background/resume, range loss and disconnection with Wi-Fi/cellular off. A synthetic widget test or DX340-only app launch does not pass this gate.
2. Complete the Mango and Apple selection/format/library-change matrices. Separate source-reported state, actual user listening and phone BLE evidence. Observe and validate playlist screens before adding an assisted playlist adapter. Stable native Mango duplicate-file identity after rescan/SD changes remains unverified.
3. Physically verify the new browse/access-recovery guards using the report's exact cases; continue in-flight UI permission/revocation/cancellation and measured recovery. Reinstalling/instrumenting the companion previously left optional Accessibility services enabled but unbound; do not assume this session-access work repairs that. Use existing in-process debug checks where appropriate and document manual recovery.
4. Volume: prior wheel/API downward steps only. Upward/output/Apple/phone/reconnect acceptance remains open. Do not enable phone volume from a reported AudioManager value alone.
5. Finish the phone reliability/security/battery/walking/audio/install matrix before Watch. Watch inventory and stable-phone gate remain open.

The earlier goal run was blocked after the key/hardware gate persisted across more than three continuations. The user has now resumed it to obtain help with the transfer, and the fresh current check is recorded at the top. The full objective remains incomplete. Revalidate the source/key and continue the exact acceptance steps above once transfer or hardware access changes. Do not mark completion from compile/simulator/proxy evidence.
