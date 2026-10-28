# Android and iOS launch verification

28 October 2026. User requested explicit verification that the remote can open on both platforms, and a revised setup/implementation plan if needed.

| Item | Result |
|---|---|
| Android companion | VERIFIED installed and opened on DX340 Android 13; physical source experiments separately recorded |
| Android remote | VERIFIED installed and opened on the same available DX340 as an Android launch test; process and focused Activity observed, screenshot artifacts/android-remote-open-check.png. This is not a separate-controller BLE test |
| iOS remote | SOURCE EXISTS at apple/DXRemote.xcodeproj; NOT COMPILED, SIGNED OR OPENED on an iPhone |
| Mac tooling | BLOCKED in this Windows session; XcodeBuildMCP list_sims returned spawn xcrun ENOENT. User has Mac access, but only DX340 is connected here |
| Flutter | VERIFIED installed: Flutter 3.41.4 stable, Dart 3.11.1; artifacts/flutter-version.txt and flutter-doctor.txt |

The existing architecture uses separate native Android and iPhone apps sharing BLE protocol v1. An APK does not run on iOS. A Flutter app also produces separate Android/iOS packages, but shares its Dart application code. Flutter does not remove the Mac/Xcode/signing or physical Bluetooth test requirements. [Official Flutter iOS build requirements](https://docs.flutter.dev/deployment/ios).

## Verification/setup plan

1. Preserve the working Kotlin DX340 companion, source adapters, optional accepted Apple Music UI-assisted mode and versioned BLE protocol.
2. Resolve the user's phone-client preference: retain native Kotlin/Swift clients, or make a shared Flutter phone project canonical. The choice is pending; do not discard working code or silently migrate the host.
3. If native is retained: run scripts/build-apple.sh on the Mac; repair actual Swift/Xcode errors, then sign/install on the user's iPhone. Continue Android and iOS controller tests against the actual DX340.
4. If Flutter is selected: create one Android/iOS phone project, reuse native Kotlin/CoreBluetooth transport via platform channels, implement common protocol/state/browsing UI and supported-action handling, and retain old clients as reference rather than deleting them. Pin the installed toolchain/dependencies and add Dart envelope/reconnect/capability tests.
5. Build/test the selected Android remote here and verify launch on the available Android hardware, clearly labelling DX340-only launch checks. Build/sign the selected iOS remote on Mac; a scaffold or Windows analysis is not an iOS launch pass.
6. With both phones available, rerun actual BLE pairing/security, Wi-Fi/cellular-off connection, browsing, source selection, reconnection and background tests. Neither framework choice supplies that evidence automatically.

No cloud build service, remote repository, paid service or publication is introduced by this plan. Apple signing remains a local Mac/user-account step. Bluetooth-only everyday operation and Mango playback remain requirements under either client framework.

## Repeatable launch checks added

`./scripts/verify-android-launch.ps1 -Serial <serial>` installs the remote, checks Activity launch and process, records device inventory and captures the UI. Executed successfully on serial d (DX340), and the screenshot was inspected: Player, Library, Queue and Setup are visible; disconnected playback controls are disabled. Evidence: artifacts/android-remote-launch.{json,txt,png}. This is an Android launch pass only.

On Mac, run `sh scripts/build-apple.sh`, then `xcrun simctl list devices available`, then `sh scripts/build-apple.sh <iPhone-simulator-UDID>`. The script runs Swift tests, builds, installs, launches and captures the chosen simulator. Logs and screenshot go to artifacts/. This new Mac launch path is prepared but NOT EXECUTED here. Then open apple/DXRemote.xcodeproj, select signing team and actual iPhone, run, and record model/OS/build plus any errors. Use docs/installation.md for the physical BLE steps.

Continue the native implementation while the optional framework preference remains unanswered, following the original Kotlin/Swift requirement. Missing Mac verification alone is not evidence that native iOS is incompatible, so it does not justify discarding the working native clients.
