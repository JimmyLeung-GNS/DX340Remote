# Implementation status

Updated 28 October 2026 (Hong Kong). **Goal active; full system incomplete.**

## Current checkpoint

Both native platforms now have build/launch evidence. The Kotlin companion and Android remote build and open on the physical DX340. The SwiftUI iPhone app builds and opens in an iPhone Air/iOS 26.5 simulator on GitHub's standard Mac runner (macOS 26.6.2, Xcode 26.6). Android-on-DX340 launch is not a separate-phone BLE test; simulator launch is not physical iPhone installation, signing or Bluetooth proof. Only DX340 is connected locally.

Android build/lint and 29 JVM tests passed (11 protocol, 2 reply authorization, 3 cache, 4 Apple UI model, 9 source handoff). The final APK-only/lint rebuild after the Bluetooth-permission guard passed and was installed; actual source state remained readable. Three shared Swift protocol tests, unsigned iOS build and native launch/captures passed in CI run 40699505189. An independent reviewer scored three iOS fixes resolved: large-text overlap, blank disconnected metadata/Source section and disconnected Library/Queue recovery. This is a scoped rendering verdict, not whole-product or interaction acceptance. DESIGN.md and its sidecar are updated.

Mango 4.1.37/code 240 accepts our ordinary MediaBrowser client and exposes All Music/Album/Artist/Genre. One selected track was user-confirmed audible through Mango on AMP15 phone out. The desired-state adapter handles Mango's observed toggle-like raw play behavior. Short actual awake/asleep switching samples and native/UI item selections observed the intended source playing and the other paused. Acoustic overlap, full format/selection, phone BLE and long-sleep acceptance remain open. Seek and actual output-volume control stay disabled.

Apple Music 6.5.3/code 1599 denied the tested direct browse interfaces. The user accepted optional screen-on/unlocked browsing. Its actual companion AccessibilityService selected downloaded One Night Animal from the AAA playlist while Wi-Fi was 0/default network none; the song was absent from the complete previous 20-item queue. The requested title was observed playing and Mango paused. The user then answered **“Yes, the correct song is audible.”** This is delayed user-reported confirmation: by the later snapshot playback had advanced to another AAA track, so no instantaneous metadata match is claimed. It is separate from the earlier ADB Paradise Paradise research. Full T07 and both phone BLE paths remain incomplete.

## Current Mango folder work

Three sampled native Mango descriptions expose no media URI or extra identity fields. The installed package has no advertised ACTION_VIEW route in the tested FLAC/WAV cases and no folder/playlist category in the sampled native root. Stable file identity after duplicate-file reorder/rescan/SD changes is unverified; do not infer format or identity from numeric display suffixes.

After these findings, the user explicitly answered **“Allow optional Mango UI mode.”** Investigation/implementation is authorized with screen-on/unlock and app-update limitations, preserving Mango and native controls. Initial actual UI research reached Mango's folder view and observed FLAC filenames, breadcrumbs and internal/SD storage menu entries. Production Mango AccessibilityService integration is **not yet implemented**; ADB research does not prove it. Details and safety constraints: mango-ui-experiment.md and test-results/2026-10-28-mango-identity.md. No replacement engine or vendor contact was authorized/performed.

The next implementation should add optional Mango folder browsing while retaining native album/song browsing. Exclude Local Network, destructive controls and settings. Isolate active folder content: Mango retains hidden ancestor page trees. Use semantic actions, current window/breadcrumb/file-label validation, snapshot-scoped IDs and the shared observed source handoff. Keep research taps separate from own-service and BLE results.

## Environment and stages

DX340 Android 13/API 33, build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. Firmware marketing version 1.09.851 and iPhone iOS 27.2 remain user-reported; actual iPhone model/build, personal Mac/Xcode and separate Android phone inventory remain unknown. AMP15 phone output/audio confirmations are user reports; gain, audio quality and output-volume mapping are unmeasured.

Windows PowerShell 7.6.5/JDK 21.0.9/SDK 36; pinned Gradle 8.14, AGP 8.11.1, Kotlin 2.1.10. Debug signing key remains outside Git. The Java selector workaround is process-local. Flutter 3.41.4/Dart 3.11.1 is installed, but no Flutter migration or global configuration change occurred. Native Kotlin/Swift remains canonical. Development-only public GitHub Mac CI uses no signing credentials or paid runner and is absent from the everyday BLE path.

The user asked for a Kotlin/Flutter comparison; docs/framework-comparison.md records current primary documentation and the recommendation. Flutter fits a single shared phone UI; Kotlin Multiplatform is a separate code-sharing option; current native Kotlin/Swift fits the selected system appearance and native BLE work. No migration decision has been made. DX340 host/source work is independent of the phone UI framework.

| Stage | State |
|---|---|
| P0 | PARTIAL: Windows/DX340 and hosted Mac inventoried; physical phone/Mac/signing inventory open |
| P1 | PARTIAL: host advertising/GATT callbacks verified, both client platforms build/open; real central/security/30-minute gate NOT TESTED |
| P2 | PARTIAL: native Mango and accepted optional Apple route sampled; source/format/volume/restart matrix incomplete |
| P3 | IMPLEMENTED BETA: bounded framing/dedup/epochs, bond+local approval, routing, shared handoff, reply revocation guards, library/queue/artwork; real security/recovery open |
| P4 | IMPLEMENTED BETA: native Android client; launch on DX340 verified, separate-phone/radio and navigation polish open |
| P5 | SIMULATOR VERIFIED: Swift3 tests/build/launch, scoped disconnected UI fixes; physical iPhone signing/BLE/interaction open |
| P6 | IN PROGRESS: one Mango audible selection, native identity limitations sampled; optional Mango folder UI now authorized and under investigation |
| P7 | PARTIAL: own-service offline outside-queue sample observed and user-confirmed audible; full20-track/5-album/2-playlist/phone matrix open |
| P8 | PARTIAL: builds/checks/docs/matrix; physical reliability, battery, installation and audio acceptance open |
| P9 | NOT STARTED: stable-phone gate unmet; Watch inventory unknown |

## Commands and evidence

- scripts/build-android.ps1: latest full build/lint/29 tests passed; artifacts/revocation-build-output.txt. Subsequent -WithoutTests APK/lint rebuild after permission check passed; artifacts/revocation-final-build-output.txt. APK hashes: artifacts/SHA256SUMS.txt. Source tests and simulated tests remain separate.
- scripts/verify-android-launch.ps1 -Serial d passed; Activity Status:ok, process and inspected artifacts/android-remote-launch.png. Only a launch check on DX340.
- CI https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40699505189 completed SUCCESS at source412b87a; app UI unchanged from77f828c. Swift3 tests/BUILD SUCCEEDED/simctl launch/6 validated native captures. Local copies: artifacts/apple-ci-40699505189/. See test-results/2026-10-28-ios-simulator.md for prior failed/invalid captures and simulator data-migration caveat. Capture OCR verifies image readiness, not navigation or BLE.
- scripts/probe-source-handoff.ps1 -Serial d -Cycles 2: 8 awake desired-state requests passed; another4 asleep requests passed. Original Apple source restored. Native Mango and UI Apple selected-item handoffs then observed correct target playing/other paused. Reports: test-results/2026-10-28-source-handoff.md and selection-handoff.md.
- Own Apple UI selection: artifacts/handoff-apple-selected.json, complete prior queue and offline precondition files. User audio answer and delayed snapshot: artifacts/apple-own-adapter-audible-confirmation.json. Full20-track and acoustic-quality acceptance are not passed.
- Mango public identity/intent probes: artifacts/mango-identity-*.json, mango-view-resolver-probe.json and package dump. No file contents read or alternate app launched. Prepared mango-interface-question.md has not been sent to a vendor.
- Initial accepted Mango UI research: artifacts/mango-ui-{initial,library,storage-root,storage-picker} captures. Research navigation exposed cached hidden ancestors and a Local Network menu entry; production must fail closed around these.
- Earlier Mango confirmation remains documented with its delayed snapshot caveat in test-results/2026-10-28-mango-selection.md. Physical Activity recreation used a labelled synthetic fixture, not playback/audio proof.

## Remaining actions

1. Implement/test the now-authorized optional Mango folder adapter without reducing native browsing, then verify actual service selection of known FLAC/WAV files through Mango. Keep screen-on/unlock limits explicit. Add UI-backed playlists only after observing and validating their screens.
2. Run full T07/T08 selection/format/library-change matrices. Source-state, user listening and phone Bluetooth evidence must be recorded separately. Stable duplicate-file/rescan/SD identity is still unresolved by native Mango metadata.
3. On the personal Mac, open apple/DXRemote.xcodeproj, choose signing team and actual iPhone, build/run, and record exact hardware/OS/build. Ensure Xcode supports the reported device. Follow installation.md for physical pairing, with Wi-Fi/cellular off.
4. Connect a separate Android phone (API31+ proposed), install/launch and test actual pairing, local approval, browsing, sleep/range and reconnection. No separate phone is currently available to this session.
5. Finish live elapsed-position presentation without continuous BLE progress traffic; current clients display source snapshots. Complete source/permission/revocation interruption cases, including cancellation of in-flight UI navigation, and measured recovery.
6. Verify low-level output volume/wheel behavior with human listening at safe levels before enabling volume. Complete compatibility-matrix.md/T01–T18 security, battery/walking/audio/install tests, then Watch after phone stability.

Single-controller beta still lacks automatic controller handoff. Request ledger is bounded at2048/controller/host epoch. Unsupported search/shuffle/repeat/verified volume/resume/cold launch remain explicit. No bit-perfect, production-security, latency or complete-system claim is made.

## Authorizations and public source

The user explicitly requested a public GitHub repository and pushing commits: https://github.com/JimmyLeung-GNS/DX340Remote. Current source-handoff work is on codex/source-handoff pending checkpoint consolidation into main. Signing keys, packages, raw device logs and scratch files remain excluded. Public source availability does not change verification limits. No root, firmware modification, protected-download extraction, identity spoofing, replacement engine, vendor contact or store release occurred.
