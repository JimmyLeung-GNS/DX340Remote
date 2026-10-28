# Android and iOS launch verification

28 October 2026. User requested explicit verification that the remote can open on both platforms, and a revised setup/implementation plan if needed.

| Item | Result |
|---|---|
| Android companion | VERIFIED installed and opened on DX340 Android 13; physical source experiments separately recorded |
| Android remote | VERIFIED installed and opened on the same available DX340 as an Android launch test; process and focused Activity observed, screenshot artifacts/android-remote-open-check.png. This is not a separate-controller BLE test |
| iOS remote | VERIFIED unsigned simulator build and native screen captures, final CI40699505189 at412b87a. NOT SIGNED/OPENED on a physical iPhone |
| Mac tooling | Local Windows xcrun unavailable. Standard GitHub Mac runner now verified: macOS26.6.2/Xcode26.6, iPhone Air/iOS26.5 simulator. User's Mac/signing/actual iPhone remain unavailable |
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

On Mac, run `sh scripts/build-apple.sh`, then `xcrun simctl list devices available`, then `sh scripts/build-apple.sh <iPhone-simulator-UDID>`. The script runs Swift tests, builds, installs, launches and captures the chosen simulator. Logs and screenshot go to artifacts/. The first hosted run completed successfully: [CI40691819981](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40691819981), source53d26ec, Swift3 tests, BUILD SUCCEEDED, launch PID8195 and inspected native Player screenshot. Its bottom status banner obscured tab labels; a correction and expanded native-screen captures are under verification in run40693857668. Then open apple/DXRemote.xcodeproj, select signing team and actual iPhone, run, and record model/OS/build plus any errors. Use docs/installation.md for physical BLE steps.

Continue the native implementation while the optional framework preference remains unanswered, following the original Kotlin/Swift requirement. Missing Mac verification alone is not evidence that native iOS is incompatible, so it does not justify discarding the working native clients.

Final update: [CI40699505189](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40699505189) passed after correcting actual UI defects and capture timing. All4 native disconnected screens, Player dark and largest-text dark were captured. The reviewer scored the3 listed fixes resolved; see test-results/2026-10-28-ios-simulator.md for exact scope and prior invalid evidence. Native Android and iOS now both have build/launch evidence, with physical-device scope kept explicit. A Flutter rewrite is not needed to establish these two platform builds.
