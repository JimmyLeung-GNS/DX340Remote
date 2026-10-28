# Shared phone interface migration

28 October 2026. User choice: shared phone interface/UI and stable use/connection. Flutter is the shared phone UI; Kotlin remains the DX340 host and Android BLE/service; Swift remains iPhone CoreBluetooth/restoration. Native system appearance is retained through Material Android and Cupertino iPhone chrome.

## Implementation and verification order

1. Retain native checkpoint ce6d8b3 and both reference clients. Extract Android RemoteModel/RemoteService into controller-core, consumed by both the reference app and Flutter.
2. Add own MethodChannel/EventChannel bridge v1, sharing native connection implementations rather than introducing another BLE plugin. No Bluetooth packets or request IDs originate in Dart. No network fallback.
3. Build one set of Player/Library/Queue/Setup screens. Preserve full paged browsing, UI-assisted back/scroll IDs, queue selection, source selection, artwork, authorization and disabled unsupported actions.
4. Run Dart state/widget tests for unauthorized cached data, repeated mutations, source/epoch navigation, UI-assisted IDs, disconnect/no replay, malformed bridge data and large-text recovery. These are simulated tests only.
5. Build release-mode Android development-signed APK, inspect merged permissions, install/launch on available DX340. Build/launch the actual Flutter iPhone app on the hosted Mac simulator. Neither proves real phone BLE.
6. On separate Android/iPhone hardware, verify pairing/local approval, Wi-Fi/cellular off, source browsing/selection, background/sleep, range loss, radio toggle, revocation and upgrade with the existing signing identity. Run the acceptance matrix before calling the phone migration stable. Keep reference apps until parity is demonstrated.

## Connection ownership

Android binds the native service when the UI opens and starts its connected-device foreground service only after explicit Connect. Activity/Flutter view recreation must not own or replay playback commands. Disconnect cancels the connection and clears authorization immediately; source playback continues independently. iPhone AppDelegate retains the native CoreBluetooth object, including restoration initialization during process launch; Flutter scene lifecycle handles presentation. Background entitlement is bluetooth-central. These are implemented mechanisms, not physical background acceptance.

Native code retains framing, bounded queues, request IDs, deduplication, source/host epochs, observed replies and bounded reconnect. Reconnection observes current state and never automatically retries playback. Read-only artwork does not disable transport controls; pending playback mutations do. The host still independently validates approval, source scope, capabilities and stale item revisions.

## Local bridge v1

`org.dx340.remote/commands`: snapshot, permissions, scan, connect(id), disconnect, authorize, resume, browse(parent,page), send(type,args). send accepts only the product's current command allowlist. Debug captureTab selects a real disconnected screen for CI; it introduces no simulated source or connection.

`org.dx340.remote/events`: JSON strings with bridgeVersion=1, status, authorized, mutationPending, state, sources, library, queue, queueRevision, devices and bounded artworkBase64. Additive queuePage contains offset/total/nullable nextOffset, and queueLoading prevents selecting rows while a new window loads. Older bridge snapshots without these fields expose no invented continuation. Native observers publish current state on reattachment; no retained Dart mutation queue exists. Invalid/incompatible data disables controls. Queue reads use the ordinary send method with GET_QUEUE args.offset and the current revision for continuation; refreshing begins at0 without a revision.

29 October continuation: optional `stateSampleId` changes only when native code accepts a host state; `stateAgeMs` measures age on that phone's monotonic clock. Disk caches have no sample identity or age. Status/library/artwork events reuse the sample identity and cannot reset the elapsed clock. Flutter advances only known positive source speed/position/duration, labels the time estimated, clamps to track duration and stops its one-second text timer on hidden Player tab, track end, connection loss or backgrounding. On return to the foreground, the existing native resume method requests fresh state; the old sample stays frozen with a refresh hint. No periodic BLE read, subscription frequency change or playback command is added. Native reference screens retain snapshot presentation.

Flutter pin: 3.41.4, revision ff37bef603469fb030f2b72995ab929ccfc227f0; Dart3.11.1. App IDs remain org.dx340.remote.controller / org.dx340.remote.ios; phone build0.2.0+2 supersedes reference Android build1. Android release-mode beta uses the existing development key outside Git; it is not production signing. iPhone simulator builds are unsigned. Physical iPhone builds require the user's Mac/Xcode/team.

Primary documentation: [Flutter platform channels](https://docs.flutter.dev/platform-integration/platform-channels), [Flutter UIScene lifecycle](https://docs.flutter.dev/release/breaking-changes/uiscenedelegate). Native background execution limits still apply; a framework choice cannot guarantee an uninterrupted radio connection.

Latest readiness guard: cached assisted actions use the actual UI path's readiness (Apple canBrowse under ui-assisted mode; hybrid Mango canBrowseFolders), independently of native canBrowse/canPlayItem. Kotlin/Swift discard assisted pages/pending browse locations when that readiness changes, retaining native pages and queue windows. Flutter also guards open/back/scroll/play and disables stale assisted rows, while native-root refresh remains available for Mango. Android/iPhone-layout fixtures pass; actual phone BLE event delivery and current Swift compilation remain unverified.
