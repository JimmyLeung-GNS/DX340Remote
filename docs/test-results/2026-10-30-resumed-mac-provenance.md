# Resumed Windows inventory and staged Mac build provenance

30 October2026, Asia/Hong_Kong. Goal resumed; full P0–P9/T01–T18 scope remains incomplete.

## Source and actual environment

The historical Documents checkout was clean on codex/output-volume/ce48684 and was left untouched. The actual active checkout C:\GitHub\DX340Remote was clean at5fe74d6; its status/HEAD were saved before fetching. A normal fast-forward preserved both histories and reached the requested c4fb607e8838cd1866662d7ec1243570b11ee015 checkpoint. No reset/stash/force-push or existing-file deletion was used.

This execution host is **JIMMYAH**, Windows11 build26300/PowerShell7.6.5, rather than the exporting DESKTOP-T9ICUE4 described in the transferred notes. Fresh inventory finds only USB DX340 d; no local xcodebuild. Microsoft JDK21.0.9 and Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0/Dart3.11.1 are present in the previous JimmyLeung paths. Android SDK exists under AppData/Local/Android/Sdk; ANDROID_HOME was absent and is supplied only to build processes. The existing external debug.keystore public certificate matches24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d; no key was copied into Git or a report.

**VERIFIED read-only bond inventory:** verify-windows-pairing-state.py with explicit adb/serial reports that the current Windows radio matches Android's saved JIMMYAH identity and Windows reports paired=true/ENCRYPTION_AND_AUTHENTICATION. BLE lookup at the public address remains unavailable, with the randomized-identity caveat preserved. No pairing/unpairing, app approval/revocation, GATT connection or playback operation was performed. This is not proof of a saved companion grant. The keep-PC-access preference remains in force; owner restoration can be requested only when a live identified approval is needed. Other PC identities/bonds were not altered.

## New implementation

The staged Mac runner now binds a successful signed ordinary-device build to a deterministic receipt of the final app bundle and the real build worktree commit/diff/status. App files, executable flags, directories and internal symlink targets contribute to the receipt; unreadable entries, broken/cyclic links and links outside the bundle fail closed. The report records source changes produced during the build rather than silently equating them to a pristine commit.

Before installation, the runner still validates physical iphoneos metadata, ordinary target, path, source checkpoint and signing team. It additionally compares both receipts, then checks them again after read-only signing inspection and before the device mutation. A different/re-signed app with the same team/bundle ID, post-build source edits, or legacy report without binding data cannot inherit the earlier build result. A fresh build is required. These receipts protect build provenance; they are not a distribution trust mechanism or physical-test result.

Added a standard public-repository Mac preparation workflow: run all17 Python guards without skips on macOS, install the pinned Flutter SDK, perform actual Mac preflight, choose a CI simulator from that inventory, and execute the native/shared simulator pipeline through the staged runner's detached worktree. It contains no signing credentials, provisioning registration, physical device target or everyday network-control path. Hosted workflow execution/results are pending at this implementation checkpoint.

## Current verification

- **VERIFIED on Windows:** existing5 Mac-runner guards and4 checkpoint-integrity checks pass. New runner suite discovers13 tests:11 pass,2 symlink tests are explicitly skipped because Windows denies symlink creation (WinError1314). Those2 cases require the hosted Mac job; they are not counted as passed here. Cases cover app replacement/addition/removal, empty directories, root-independent receipts, changed source, wrong commit, missing receipt, unreadable directories and rejection before any install/launch dispatch.
- **VERIFIED non-Mac refusal:** `python scripts/mac-iphone-tests.py preflight` returns NEEDS_MACOS_XCODE/exit2, executes only Git inventory and records a summary. This is expected and is not a Mac build.
- **VERIFIED local native build:** scripts/build-android.ps1 with process-local original signing/JDK/SDK settings completes build/lint and120 JVM tests. New build candidates are separate from installed/audio-evidence packages. No APK has been installed by this continuation.
- Shared phone current-code analysis/tests/release/lint are running; inspect the actual process/result before updating this row. Actual Mac simulator orchestration, signed physical iPhone build/install, both-phone BLE/source/reliability, PC grant restoration, two-controller handoff and Watch remain **NOT TESTED** by these local checks.

Local evidence: artifacts/resume-checkpoint-pre-fetch.json, resumed-c4fb607-native-build.txt, resumed-mac-receipt-final-tests.txt, resume-signing-public-certificate.txt, windows-ble/resumed-jimmyah-readonly.json and mac-iphone-tests/20261030-080747-0faa2762/. These ignored local files do not replace the tracked reports or future physical evidence.
