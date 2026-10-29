# Device inventory — updated 29 October 2026

Resumed host JIMMYAH uses Microsoft JDK21.0.9 and SDK/pinned Flutter3.41.4 under C:\Users\JimmyLeung\AppData\Local. Earlier JBR/Users-user paths below describe the preceding environment; windows-continuation.md has current exact paths. Windows native pairing reports ENCRYPTION_AND_AUTHENTICATION; four earlier approved protocol reads/fresh post-revocation denial passed, while later ATT1/approval/INFO failures remain unresolved. Companion approval currently absent, OS bond retained. Only DX340 d is connected. AMP15 phone-out Mango and Apple37→36→37 local steps were each user-confirmed audible in92b5f9c2...; current395be729... restart clears memory proof. Both corrected iOS workflows pass at8d73a48; physical iPhone/signing remain unavailable. HiBy currently stopped after authorized isolation; both intended players paused37 and both browsing services enabled/bound.

| Component | Result | Evidence |
|---|---|---|
| Development host | VERIFIED current: Windows11 Pro10.0.26300, Android Studio JBR21.0.7 selected per process; Android SDK available | Current Windows/SDK inspection; windows-ble/windows-version.json and preceding build reports |
| PC Bluetooth diagnostic central | VERIFIED: Intel Wireless Bluetooth, driver24.20.0.3, radio already On; WinRT BLE/central support | Real discovery/GATT/unpaired-denial checks in test-results/2026-10-29-windows-ble.md. Authenticated bond still unverified; not a phone substitute |
| Android toolchain | VERIFIED: Gradle 8.14, AGP 8.11.1, Kotlin 2.1.10, compile/target SDK 36, minimum SDK 31 | Project configuration; Android builds |
| USB target | VERIFIED: iBasso DX340, Android 13 / API 33 | `adb -s d shell getprop` |
| Exact system build | VERIFIED: `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys` | getprop ro.build.display.id |
| Security patch | VERIFIED reported: 2022-10-05 | getprop ro.build.version.security_patch |
| Firmware marketing version | NOT TESTED: reported 1.09.851 not independently confirmed | User input; exact Android build above |
| Mango | VERIFIED installed: `com.ibasso.music`, 4.1.37 / code 240 | dumpsys package; companion probe |
| Apple Music | VERIFIED installed: `com.apple.android.music`, 6.5.3 / code 1599 | dumpsys package; companion probe |
| BLE feature | VERIFIED reported by PackageManager; advertiser/GATT callbacks plus real Windows over-air discovery, uncached service enumeration and unpaired attribute denial | test-results/2026-10-29-windows-ble.md.5 awake/5 USB-powered screen-off discovery rounds; no authenticated exchange/phone pass |
| iPhone | NOT TESTED: iOS 27.2 user reported; model/build unknown; unavailable to session | User confirmed only DX340 is connected |
| Android controller | NOT TESTED: model/OS unknown; unavailable | User confirmation |
| User's Mac/Xcode/signing | BLOCKED: no access; exact macOS/Xcode/signing account unknown | Windows environment inspection; physical iPhone deployment still requires this |
| Development-only hosted Mac | VERIFIED macOS26.6.2/build25G83, Xcode26.6/build17F113; iPhone Air simulator/iOS26.5 | GitHub CI run40691819981, downloaded logs and native screenshot. No real iPhone/radio claim |
| Watch | NOT TESTED: model/watchOS unknown; phone stability gate not met | Plan P9 |
| Output | USER REPORTED AMP15 phone out; correct Mango and Apple selected-track audio confirmed | Gain/volume mapping/audio quality NOT MEASURED |
| Collection | PARTIAL: current source-exposed Mango All Music500 unique IDs; Apple metadata sampled5 albums/2 playlists; physical files/SD layout/full formats unknown | original-key-upgrade.md and source-library-inventory.md test reports. Metadata/native IDs are not full physical storage or audio acceptance |
| Flutter toolchain | VERIFIED3.41.4 stable/Dart3.11.1, revision ff37bef603469fb030f2b72995ab929ccfc227f0 | User selected shared phone UI; release Android build/lint/9 Dart tests and iPhone simulator build/launch passed. License first-run warning was an earlier inventory result, not a failed final build |

Bluetooth MAC and unrelated notifications are omitted from tracked reports. Raw ADB dumps stay under ignored artifacts/. No root, firmware changes, Wi-Fi setup or source replacement performed.

The early29 October rebuild initially failed signing preflight because this PC's default key differed (test-results/2026-10-29-elapsed-position.md). That historical gate is resolved: the user supplied the original key, it was verified outside Git and both apps were upgraded in place. Current companion40114ff8.../shared phone09aaae78... use that identity; default key/global environment stayed unchanged. Current package/navigation evidence is in test-results/2026-10-29-mango-prefetch.md; preceding controller-management evidence is in test-results/2026-10-29-controller-management.md. The checkout uses process-local JBR21.0.7 and pinned Flutter3.41.4 under work/flutter; global Flutter remains3.41.7. Only DX340 is connected over USB. This PC's newly verified BLE radio provides a real diagnostic central, but no physical Android/iPhone or current Mac build is available. Windows authenticated pairing consent and listening availability are pending user steps; no new audio result is inferred.
