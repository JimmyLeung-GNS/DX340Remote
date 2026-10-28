# Flutter phone migration checkpoint

28 October 2026. IN PROGRESS, not physical acceptance. The user selected a shared phone UI with native Bluetooth/lifecycle underneath. Actual Android package and iPhone simulator checks are running. Do not infer phone radio reliability from these builds.

Initial failed checks: first widget test lacked the foundation import, and record/map comparisons incorrectly relied on map identity; corrected. The large-text widget probe initially looked for lazy ListView content before scrolling; corrected to scroll to the real control. These were test failures, not device BLE results. Final commands/results will be appended after execution.

## Migration verification checkpoint

- VERIFIED: Flutter3.41.4 analysis clean and9 Dart state/widget tests passed on Windows; both Material/Cupertino variants include disconnected recovery, large-text scrolling and disabled playback. Native Windows build/lint and29 JVM tests still pass after controller-core extraction (artifacts/shared-controller-native-build.txt).
- VERIFIED build: Android release-mode development-signed APK compiles; certificate SHA256 matches prior development identity24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d. Merged permissions include BLE/notifications/connected-device foreground service, no Internet/Wi-Fi.
- VERIFIED runtime scope: physical DX340 Android13/API33 opens the shared Player/Library/Queue/Setup screens. Observed navigation and Library OpenSetup recovery by development taps against screenshots. No separate-phone BLE connection was made. Raw captures in artifacts/flutter-android-*.png.
- FAILED automation: UIAutomator currently returns null root for both this Flutter remote and the unchanged Kotlin companion. A forced Dart semantics experiment did not fix it and was removed. Screen-reader accessibility is NOT TESTED. Launch script now records this separately from Activity/process/screenshot evidence.
- FAILED first Android release lint: device-name permission check and Flutter-generated Windows property colon escaping. Added explicit permission/SecurityException handling and process-local generated-property normalization. Subsequent release lint passed; current final UI build/lint being rerun.
- VERIFIED iOS compilation/launch at b1fdf4c and24d0484: hosted macOS26.6.2/Xcode26.6, iPhone Air/iOS26.5 simulator. Nine Dart tests pass there; unsigned Flutter Runner.app builds and launches. Run40710376259 was cancelled during captures when superseded. Run40711487461 failed the final largest-text capture gate: actual oversized tab labels collided. The fix matches CupertinoTabScaffold's native fixed-height tab text behavior while leaving content scaling intact; regression test added. Corrected final capture run pending. First Setup screenshot caught launch fade; capture now requires a second ready frame after1second.

Physical iPhone signing, separate Android controller, radio/offline controls, pairing/security, background/walk/battery and source selection via either phone remain NOT TESTED. These results do not pass stability or whole-product acceptance. Follow phone-reliability-runbook.md and the existing acceptance matrix.
