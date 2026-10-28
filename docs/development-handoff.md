# Development handoff

Updated 29 October 2026, Asia/Shanghai. Work in `C:\GitHub\DX340Remote`.

Current resume: **original key transfer/signing gate resolved**. The user supplied a ZIP; only debug.keystore was extracted to C:\Users\user\.android\dx340-original-debug.keystore (the literal C:\Users\user.android destination was not writable). Its certificate24f48aa3... matches installed apps. Both apps were rebuilt with process-local DX340_DEBUG_KEYSTORE and upgraded in place, preserving all four companion preferences and the default PC key. Goal active, full system incomplete. Read test-results/2026-10-29-original-key-upgrade.md for key transfer, silent source/recovery checks and preceding listening samples. Do not repeat the request for a key or use the different default key.

Latest checkpoint adds full queue pagination after discovering101 actual Apple queue entries behind the product's50-item cap. Read test-results/2026-10-29-queue-pagination.md for **current installed hashes** and evidence; the original-key report records the preceding build/audio samples. Current native build/lint72 tests and Flutter analysis28 tests/release APK/lint pass. Both current packages upgraded in place. Actual50+50+1 queue windows match the complete public queue, stale/missing revisions reject and fresh-start recovers. Both sources paused; no new listening test. Shared UI/native references carry pagination, display revision and loading guards; Swift changes need a Mac build. scripts/verify-queue-pagination.ps1 is the repeatable source-local read check; phone-reliability-runbook.md step11 describes missing phone acceptance.

This file was absent when the user requested continuation. It is reconstructed from the checked-in `AGENTS.md`, implementation plan, status and commit400ec34, plus the work recorded below. It does not claim to recover an older missing file's contents.

## Read first

1. `AGENTS.md`, `docs/implementation-plan.md`, `docs/status.md`.
2. `docs/completion-audit.md` and `docs/test-results/2026-10-29-original-key-upgrade.md` for current evidence; the indication/signing/browse/elapsed reports document preceding changes.
3. `docs/phone-reliability-runbook.md`, `docs/compatibility-matrix.md`, and the separate Mango/Apple experiment reports before physical acceptance work.

## Saved implementation

The canonical phone interface is Flutter/Dart over native Kotlin BLE/service and Swift CoreBluetooth/restoration. Host and source adapters stay Kotlin. Native phone reference apps are retained. BLE carries controls/metadata only; existing DX340 Mango and Apple Music apps play the music. Optional screen-on/unlocked Apple and Mango assisted browsing were accepted in prior work. Replacing Mango, root, firmware changes and download extraction remain excluded.

Checkpoint400ec34 contains source-session callback ownership/capability guards and guarded downward-only debug volume probes. Phone volume remains disabled. Prior local Mango WAV/FLAC and Apple downloaded outside-queue selections have user-reported audible confirmations; those are not phone Bluetooth acceptance. Full format/library/SD/playlist, reliability and audio matrices remain incomplete.

This continuation adds host position normalization and shared-phone elapsed estimates with native sample identity/age, lifecycle freeze/fresh-resume guards and no periodic BLE polling. See the test report and current working tree. New Swift changes require a fresh Mac build; earlier iOS CI results do not verify this source revision. No publication is performed by this continuation.

Preceding parser verification: native build/lint67 JVM tests; Flutter analysis22 tests/release APK/release lint passed. Those packages were installed on DX340 with matching original-key preflight and pulled-APK verification; they are superseded by the queue build above. No separate phone has been tested. Host code recognizes observed Apple Songs download badges and retains full row identity beyond displayed title truncation. Actual6 selectable Songs rows/stale-token rejection passed. Source is saved on local branch codex/source-recovery-checkpoint; use git log -1 for the checkpoint. It has not been pushed or published.

The host now has a tested confirmed-frame queue with per-indication completion deadlines, bounded failure cleanup and control/artwork priority. Native queue failures clear logical peer/authentication before requesting OS disconnect, without replaying controls. Software scheduling tests do not prove real BLE callback ordering, sleep timing or same-peer reconnect behavior. The connected Apple tool was also checked directly and failed to list simulators with xcrun ENOENT; a Mac/iOS build remains unavailable here.

The preceding continuation guards browse-request lifetime, carries authorization through play-item revalidation and invalidates requests/callbacks/epochs on access loss. **Actual DX340 notification-access revoke/restore now passed**: capabilities disabled, epochs changed, source access recovered without process restart, stale revision/uncached IDs rejected and fresh-root25-row browse recovered. Notification settings were restored exactly. In-flight UI/authorization delivery and phone radio recovery remain unverified.

The original key is now at C:\Users\user\.android\dx340-original-debug.keystore. The shared android/development-signing.gradle supports process-local DX340_DEBUG_KEYSTORE for every Android build; keep using it in this checkout. Global/default key settings stay unchanged. Use installation.md's try/finally override and signing preflight before each update. Keys and the user's ZIP stay outside Git.

## Local environment and reproducible commands

The connected ADB target `d` is the DX340, Android13/API33, build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. It is not a separate Android phone. No physical iPhone, personal Mac or signing team is accessible here.

The user confirmed DX340 only for now. Silent upgrade/recovery tests kept both sources paused and volume unchanged. Three later source-local listening samples passed: Apple A・RA・SHI outside complete101-item previous queue, Mango03. 風のたより.wav and03. ゴースト.flac. Each requested title remained observed playing at the user confirmation; all three were confirmed audible. Final state both sources paused, control target Mango, volume unchanged. These are not phone BLE or audio-quality acceptance.

This checkout started without the old ignored work/artifacts/local.properties. Historical raw evidence paths in status belong to the earlier environment and are not presumed present here. Global Flutter resolves to3.41.7; Java was absent from PATH. Official Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0 is cloned into ignored `work/flutter`. Use the installed Android Studio JBR21.0.7 locally; do not change global Flutter/Java settings.

```powershell
Set-Location C:\GitHub\DX340Remote
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\user\AppData\Local\Android\Sdk'
$env:PATH = "$PWD\work\flutter\bin;$env:PATH"
$env:DX340_DEBUG_KEYSTORE = 'C:\Users\user\.android\dx340-original-debug.keystore'
./scripts/build-android.ps1
./scripts/build-phone.ps1
```

If `work/flutter` is missing, clone the official3.41.4 tag and verify the full revision before building. The Android-only project needs ignored `android/local.properties` containing `sdk.dir=C:/Users/user/AppData/Local/Android/Sdk`. Flutter generates its own ignored phone local properties. Signing keys stay outside Git. Keep prior installed APKs/signers for upgrade comparison; never uninstall or reset app data merely to bypass a signing mismatch.

On Mac, install the same Flutter revision and run `sh scripts/build-phone-ios.sh <available-iPhone-simulator-UDID>` from the repository root. Physical iPhone deployment uses `phone/ios/Runner.xcworkspace`, a compatible Xcode and the user's signing team. Preserve native `apple/` as reference.

## Resume next

1. Key/signing/in-place upgrade is resolved and three current-build audible samples passed. Continue the remaining source-selection/format/album/playlist matrix on DX340, then validate the Swift changes on Mac and both physical phones over BLE when available. Use the preserved original-key process override and preflights for updates. Verify pause/seek/track/source changes, stale artwork/status, lock/background/resume/range/disconnect offline. A widget test or DX340-only launch does not pass the phone gate.
2. Complete the Mango and Apple selection/format/library-change matrices. Separate source-reported state, actual user listening and phone BLE evidence. Observe and validate playlist screens before adding an assisted playlist adapter. Stable native Mango duplicate-file identity after rescan/SD changes remains unverified.
3. Notification loss/regrant and old IDs were exercised on DX340; continue in-flight UI permission/revocation/cancellation and measured recovery. The upgrade rebound both optional Accessibility services, but older instrumentation left them unbound. Prefer the DUMP-protected debug receiver over UIAutomator/instrumentation for source checks here. Initial Apple root navigation during an unsettled screen failed once and explicit retry succeeded; investigate measured settling/recovery without replaying playback.
4. Volume: prior wheel/API downward steps only. Upward/output/Apple/phone/reconnect acceptance remains open. Do not enable phone volume from a reported AudioManager value alone.
5. Finish the phone reliability/security/battery/walking/audio/install matrix before Watch. Watch inventory and stable-phone gate remain open.

Earlier key-transfer blocks are historical and now resolved by the user-supplied ZIP. The goal is active; the full objective remains incomplete. Separate phones/Mac and full source/audio/reliability acceptance remain external gates. Do not mark completion from compile/simulator/proxy evidence.
