# Device inventory — 28 October 2026

| Component | Result | Evidence |
|---|---|---|
| Development host | VERIFIED: Windows, PowerShell 7.6.5, Microsoft JDK 21.0.9, Android Studio/SDK available | `Get-Command`, SDK inspection; artifacts/gradle-bootstrap.txt |
| Android toolchain | VERIFIED: Gradle 8.14, AGP 8.11.1, Kotlin 2.1.10, compile/target SDK 36, minimum SDK 31 | Project configuration; Android builds |
| USB target | VERIFIED: iBasso DX340, Android 13 / API 33 | `adb -s d shell getprop` |
| Exact system build | VERIFIED: `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys` | getprop ro.build.display.id |
| Security patch | VERIFIED reported: 2022-10-05 | getprop ro.build.version.security_patch |
| Firmware marketing version | NOT TESTED: reported 1.09.851 not independently confirmed | User input; exact Android build above |
| Mango | VERIFIED installed: `com.ibasso.music`, 4.1.37 / code 240 | dumpsys package; companion probe |
| Apple Music | VERIFIED installed: `com.apple.android.music`, 6.5.3 / code 1599 | dumpsys package; companion probe |
| BLE feature | VERIFIED reported by PackageManager; advertiser and GATT start callback succeed | artifacts/host-advertising.txt; companion diagnostic |
| iPhone | NOT TESTED: iOS 27.2 user reported; model/build unknown; unavailable to session | User confirmed only DX340 is connected |
| Android controller | NOT TESTED: model/OS unknown; unavailable | User confirmation |
| User's Mac/Xcode/signing | BLOCKED: no access; exact macOS/Xcode/signing account unknown | Windows environment inspection; physical iPhone deployment still requires this |
| Development-only hosted Mac | VERIFIED macOS26.6.2/build25G83, Xcode26.6/build17F113; iPhone Air simulator/iOS26.5 | GitHub CI run40691819981, downloaded logs and native screenshot. No real iPhone/radio claim |
| Watch | NOT TESTED: model/watchOS unknown; phone stability gate not met | Plan P9 |
| Output | USER REPORTED AMP15 phone out; correct Mango and Apple selected-track audio confirmed | Gain/volume mapping/audio quality NOT MEASURED |
| Collection | PARTIAL: Mango All Music returned466 items; total files/SD layout/format counts unknown | Native browser sample, not full storage inventory |
| Flutter toolchain | VERIFIED3.41.4 stable/Dart3.11.1, revision ff37bef603469fb030f2b72995ab929ccfc227f0 | User selected shared phone UI; release Android build/lint/9 Dart tests and iPhone simulator build/launch passed. License first-run warning was an earlier inventory result, not a failed final build |

Bluetooth MAC and unrelated notifications are omitted from tracked reports. Raw ADB dumps stay under ignored artifacts/. No root, firmware changes, Wi-Fi setup or source replacement performed.

29 October continuation: current DX340 build and installed Mango4.1.37/code240/Apple6.5.3/code1599 were rechecked read-only. User confirms DX340 only for now. The local checkout uses Android Studio JBR21.0.7 selected per process and an isolated pinned Flutter3.41.4 under work/flutter; global Flutter remains3.41.7. Installed package hashes/development certificate differ from the old checkpoint's environment. New packages built successfully but are not installed because the signing preflight rejects the mismatch; see test-results/2026-10-29-elapsed-position.md. No new physical phone, audio or timing acceptance is inferred.
