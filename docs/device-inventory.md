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
| Mac/Xcode/signing | BLOCKED: user has access to a Mac; this session has no access or Apple toolchain | Windows environment inspection |
| Watch | NOT TESTED: model/watchOS unknown; phone stability gate not met | Plan P9 |
| Output | USER REPORTED AMP15 phone out; correct Mango and Apple selected-track audio confirmed | Gain/volume mapping/audio quality NOT MEASURED |
| Collection | PARTIAL: Mango All Music returned466 items; total files/SD layout/format counts unknown | Native browser sample, not full storage inventory |
| Flutter toolchain | VERIFIED installed3.41.4 stable/Dart3.11.1 | artifacts/flutter-version.txt, flutter-doctor.txt; Android license warning; no migration selected |

Bluetooth MAC and unrelated notifications are omitted from tracked reports. Raw ADB dumps stay under ignored artifacts/. No root, firmware changes, Wi-Fi setup or source replacement performed.
