# Source and BLE capability findings

28 October 2026. Physical DX340 Android13/API33, firmware fingerprint `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. Companion0.1.0 debug; Mango4.1.37/code240; Apple Music6.5.3/code1599. User reports AMP15 phone out.

| Area | Result | Evidence / limit |
|---|---|---|
| GATT hosting / advertising | VERIFIED API callbacks | Service created, advertising succeeds. No physical central/security pass |
| Android remote installation/open | VERIFIED on DX340 only | artifacts/android-remote-launch.json/png. Separate-controller radio NOT TESTED |
| iOS compile/open | VERIFIED simulator only | Final CI40699505189: Xcode26.6, Swift3 tests passed, iPhone Air/iOS26.5 native screens captured. Reviewer scored3 disconnected UI fixes resolved. Physical iPhone signing/launch/BLE still NOT TESTED |
| Mango native browsing | VERIFIED sampled tree | Own companion identity; five root categories,17 albums,11 artists,3 genres sampled. No folder/playlist category exposed in sampled root |
| Mango selected-item playback | VERIFIED one user-confirmed audible selection | Selected “777 …” through Mango; later confirmation snapshot had advanced. See test-results/2026-10-28-mango-selection.md; full T08 incomplete |
| Mango explicit transports | VERIFIED desired-state adapter sample | Raw repeated play was non-idempotent. Adapter checks observed state and dispatches once; pause/pause/play/play passed on “Curtain Call”. artifacts/mango-observed-state-probe.json. No acoustic/radio claim |
| Mango seek/volume | Seek disabled; volume PARTIAL local downward observation, phone controls disabled | Action mask560 omits seek. Physical wheel36→35 and public API35→34 matched Mango display and were separately user-reported quieter on AMP15. No upward/full-boundary/other-output/Apple/phone volume acceptance; test-results/2026-10-29-volume-observation.md |
| Apple native MediaBrowser | FAILED tested connection | Ordinary client onConnectionFailed |
| Apple Media3 browse | DENIED tested root | Connected, getLibraryRoot returned -4, no root. No identity spoofing |
| Apple offline outside-queue selection | VERIFIED foreground research + user-confirmed audio | “Paradise Paradise”, absent from prior101 queue; Wi-Fi0/default network none. ADB semantic navigation, not service/BLE acceptance |
| Apple optional own UI adapter | VERIFIED sampled navigation and one own-service offline/outside-queue user-confirmed audible selection | One Night Animal selected from downloaded AAA playlist, absent from complete prior20-item queue, observed playing/Mango paused offline. User said correct song audible; delayed confirmation snapshot had advanced. Scroll/Back/stale-ID samples passed. Full20/5/2 and phone BLE remain open |
| Mango optional UI mode | VERIFIED local own-service nested-folder WAV selection/user-confirmed audio; full matrix open | User accepted screen-on/unlocked mode. Own separate service selected02. 白日夢.wav, observed Mango 白日夢 playing/Apple paused offline; user confirmed correct audible song through AMP15. Actual cached-page drawing order, deep-root recovery, stale selection and sleep rejection sampled. Native browsing/controls retained; phone BLE, FLAC/SD/full20 remain open |
| Apple UI screen off/unlock | VERIFIED sleep rejection; REQUIRED limit | Actual service returned UI_SCREEN_UNLOCK_REQUIRED/canBrowse=false while playing=true. Secure-lock test pending. User accepted this optional mode |
| Source handoff | VERIFIED short local source-state samples; full acceptance pending | Shared desired-state/native-item/UI-item/queue preparation validates authorization/target before pause, then waits/revalidates.12 desired-state requests awake/asleep and native/UI item selections observed target playing/other paused. Queue path, phone BLE and acoustic-overlap acceptance outstanding |
| Resume / cold launch / volume | NOT SUPPORTED beta | Capabilities false; no fabricated commands |
| Sleep / range / battery / walking | NOT TESTED | Builds and source state observations do not establish radio/audio continuity |

The two separate opt-in companion accessibility services have been enabled through Android Settings under the user's accepted modes. Apple uses its Downloaded semantic badges; Mango uses local-storage folder semantics and active sibling drawing order. Both restrict reads/actions to their own source package, bind snapshot-scoped IDs and reject unverified app versions. No credentials/private downloads/coordinates/global gestures are used in these adapters. The separate read-only UI research APK remains uninstalled.

Development ADB permission grants and upgrades do not establish a passed first-install wizard. No root, firmware changes, protected-download extraction, app impersonation, replacement playback engine or network fallback occurred.
