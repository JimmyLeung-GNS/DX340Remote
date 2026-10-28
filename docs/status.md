# Implementation status

Updated28 October2026 (Hong Kong). **Goal active; full system incomplete.**

## Current checkpoint

Kotlin DX340 companion and native Android remote build and open on the only connected device, DX340 Android13. Android launch has been repeated with scripts/verify-android-launch.ps1: Activity Status:ok, process present, screenshot inspected. Installing the remote on DX340 is not separate-phone Bluetooth verification. Native SwiftUI/CoreBluetooth source and deterministic Xcode project exist, but **iOS has not compiled, signed or opened**. Windows has no xcrun; Mac/iPhone/separate Android controller/Watch are unavailable here.

The user's latest platform question is recorded in cross-platform-verification.md. Native Kotlin/Swift remains canonical per the original requirements while optional Flutter preference is unanswered. Flutter3.41.4/Dart3.11.1 is installed; Flutter would still require Mac/Xcode/signing and real iPhone BLE tests. No Flutter migration, global Flutter configuration change or cloud build occurred. Public GitHub publication was subsequently authorized by the user, as recorded below. Repeatable Android and Mac simulator launch scripts are now prepared; only Android has run.

Mango's native browser exposes All Music/Album/Artist/Genre. The user confirmed one companion-selected track audibly through Mango on AMP15 phone out. Raw repeated play proved toggle-like; the exact-version desired-state adapter then passed pause/pause/play/play with unchanged track. Seek/output volume remain disabled. Full20-file FLAC/WAV/folder/playlist acceptance is incomplete; sampled root has no folder/playlist category.

Apple Music6.5.3/code1599 denied native browser and Media3 root(-4). Earlier ADB foreground research selected downloaded “Paradise Paradise” outside the prior101-item queue with Wi-Fi0/no active network, and the user confirmed audio. The user accepted **optional screen-on/unlocked Apple Music browsing**. Its real companion AccessibilityService is now implemented and enabled through Settings, version-pinned, downloaded-badge-only, bounded and snapshot-scoped. Own-service root→Albums→song and one selected “Kuchibirukara Romantica” observed playing succeeded. Scroll, Back, stale-ID rejection and screen-off fail-closed checks passed after fixing transitional layout timing. This is actual DX340 service evidence through a privileged debug probe, **not phone/BLE acceptance, a fresh outside-queue proof or a new human audio confirmation**. Downloaded Playlists→AAA now returns playlist names and downloaded song rows; Artists remains disabled. No root/private download extraction/source replacement.

## Environment and stages

DX340 build: `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`; Mango4.1.37/code240; Apple Music6.5.3/code1599. Marketing firmware1.09.851 and iOS27.2 are user-reported; iPhone model/build, Mac/Xcode and separate Android phone are unknown. AMP15 phone out/audio confirmations are user reports; no gain/quality/volume mapping measurement.

Windows PowerShell7.6.5/JDK21.0.9/SDK36; pinned Gradle8.14, AGP8.11.1, Kotlin2.1.10. Development debug signing key stays outside Git. A process-local Java selector workaround is confined to the build script.

| Stage | State |
|---|---|
| P0 | PARTIAL: Windows/DX340 inventory verified; other hardware absent |
| P1 | PARTIAL: host GATT/advertising callbacks verified; no central/security/30min pass |
| P2 | PARTIAL: native Mango and accepted optional Apple route investigated; source/volume/format matrix open |
| P3 | IMPLEMENTED BETA: framing/bounds/epochs/dedup/mutation gate, bond+local approval, routing/library/queue/artwork; physical security/recovery unverified |
| P4 | IMPLEMENTED BETA: Android discovery/auth/control/browsing/cache/reconnect service; launch verified on DX340 only |
| P5 | SOURCE IMPLEMENTED: SwiftUI/CoreBluetooth/restoration/cache; Mac build and iPhone required |
| P6 | PARTIAL: one audible Mango selection; full format/folder/playlist/rescan/SD acceptance open |
| P7 | IMPLEMENTED EXPERIMENT: own guarded Apple UI adapter selected observed track; complete playlist/offline20-track/phone acceptance open |
| P8 | PARTIAL: builds/tests/instructions/matrix; physical reliability and Android navigation polish/iOS visual QA open |
| P9 | NOT STARTED: stable-phone gate unmet; Watch hardware unknown |

## Commands and evidence

- scripts/build-android.ps1 passed after the final adapter changes (1m23s): host/remote/source-test/optional research APKs, app lint and18 JVM tests(11 protocol/3 cache/4 UI model). Logs: artifacts/build-script-output.txt and android-build.txt; APK hashes in SHA256SUMS.txt. These are software checks, not radio/audio acceptance.
- scripts/verify-android-launch.ps1 -Serial d passed; artifacts/android-remote-launch.json/txt/png inspected. Disconnected Player/Library/Queue/Setup visible, playback actions disabled.
- scripts/build-apple.sh now supports an explicit simulator UDID to build/install/launch/capture after Swift tests. **Prepared, not executed**. Mac instructions are installation.md; no iOS launch claim.
- Own AccessibilityService probe: debug-only receiver requires privileged android.permission.DUMP; calls actual SourceRegistry/service in the host process. artifacts/apple-ui-live-play.json shows requested track playing; apple-ui-verify-{albums,tracks,scroll,stale,back,screen-off}.json capture actual-service observations. On sleep it returned UI_SCREEN_UNLOCK_REQUIRED, canBrowse=false, while source still reported playing. Secure-lock and phone tests remain separate.
- Initial own-service attempts failed foreground/merged-tab semantics, then scroll layout settling. Fixes followed observed public accessibility trees; final album/scroll/back/stale/sleep sequence passed. Earlier failed results are retained, not silently replaced by simulated success.
- Mango evidence: artifacts/mango-playback-tests.jsonl, mango-observed-state-probe.json; test-results/2026-10-28-mango-selection.md. User said “Yes, the correct song is audible.” Agent recorded that report in the app. Delayed confirmation snapshot had advanced to CHEERS; it is not an instantaneous requested-title match.
- Apple research: test-results/2026-10-28-apple-ui-experiment.md; user said “Yes, Apple Music plays it audibly.” That earlier ADB research is distinct from own-service observed state.
- Physical Activity recreation test preserved a labelled synthetic pending-result fixture, with no playback dispatched; artifacts/ui-recreation-probe.json. No acoustic claim.
- Native UI reviewer source-scored scoped lifecycle/stale-response fixes resolved. Native system design documented in DESIGN.md/.impeccable/design.json. No whole-product visual approval; host screenshot predates optional Apple controls, and iOS is uncompiled.

## Remaining facts and exact next actions

1. On Mac, copy this source repository, run `sh scripts/build-apple.sh`; list simulators with `xcrun simctl list devices available`; run `sh scripts/build-apple.sh <iPhone-simulator-UDID>`. Preserve generated build/test/launch logs and screenshot. Repair actual compiler/runtime failures before claiming iOS support.
2. Open apple/DXRemote.xcodeproj, choose signing team and connected iPhone, run, and record hardware/exact OS/build. Then use installation.md pairing steps against DX340, with Wi-Fi/cellular off. No publication is necessary.
3. Connect a separate Android phone(API31+ proposed), run the Android launch script with its serial, then actual pairing/local approval, offline controls, full browsing and reconnect/sleep tests. Only DX340 is currently connected.
4. Own-service Downloaded Playlists→AAA now returns playlist names and downloaded song rows; see test-results/2026-10-28-own-apple-adapter.md. Finish20 tracks/five albums/two playlists, outside-queue offline own-service and phone tests; verify source handoff/secure-lock behavior and update-version fail-closed. Keep UI mode limitations visible.
5. Finish Mango20 FLAC/WAV, folder/playlist decision based on exposed interfaces, SD/rescan, low-level actual output-volume checks and audio continuity. Do not switch Mango engines without an explicit concrete decision.
6. Execute compatibility-matrix.md/T01–T18 physical reliability/battery/walking/security, then Watch after phone stability. No numerical performance/audio-quality or production-security claim yet.

Single-controller beta has no automatic handoff; request ledger is bounded2048 per controller/host epoch. Library/queue scopes invalidate stale results; real SD and repeated metadata cases are not passed. Artwork uses bounded public metadata caches; actual BLE latency unknown. Search/shuffle/repeat/verified volume/resume/cold launch are unsupported. Full scope remains visible rather than reducing browsing to queue skipping.

## Public source repository

On28 October2026 the user explicitly requested committing and pushing this implementation to a public GitHub repository. Created https://github.com/JimmyLeung-GNS/DX340Remote using the existing local GitHub sign-in; source commits are published on main. Build artifacts, raw device logs, scratch files and signing material remain excluded. Public availability does not change the beta verification limits above.
