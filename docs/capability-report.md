# Source and BLE capability findings

28 October 2026. Physical DX340 Android13/API33, firmware fingerprint `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. Companion0.1.0 debug; Mango4.1.37/code240; Apple Music6.5.3/code1599. User reports AMP15 phone out.

| Area | Result | Evidence / limit |
|---|---|---|
| GATT hosting / advertising | VERIFIED API callbacks | Service created, advertising succeeds. No physical central/security pass |
| Android remote installation/open | VERIFIED on DX340 only | artifacts/android-remote-launch.json/png. Separate-controller radio NOT TESTED |
| iPhone compile/open | BLOCKED here | Windows has no xcrun/Xcode; project source exists. Mac action required |
| Mango native browsing | VERIFIED sampled tree | Own companion identity; five root categories,17 albums,11 artists,3 genres sampled. No folder/playlist category exposed in sampled root |
| Mango selected-item playback | VERIFIED one user-confirmed audible selection | Selected “777 …” through Mango; later confirmation snapshot had advanced. See test-results/2026-10-28-mango-selection.md; full T08 incomplete |
| Mango explicit transports | VERIFIED desired-state adapter sample | Raw repeated play was non-idempotent. Adapter checks observed state and dispatches once; pause/pause/play/play passed on “Curtain Call”. artifacts/mango-observed-state-probe.json. No acoustic/radio claim |
| Mango seek/volume | NOT TESTED / disabled | Action mask560 omits seek; output-volume mapping unknown |
| Apple native MediaBrowser | FAILED tested connection | Ordinary client onConnectionFailed |
| Apple Media3 browse | DENIED tested root | Connected, getLibraryRoot returned -4, no root. No identity spoofing |
| Apple offline outside-queue selection | VERIFIED foreground research + user-confirmed audio | “Paradise Paradise”, absent from prior101 queue; Wi-Fi0/default network none. ADB semantic navigation, not service/BLE acceptance |
| Apple optional own UI adapter | VERIFIED sampled root/album/playlist/song reads and one observed selection | Actual bound companion AccessibilityService, debug DUMP-protected in-process probe, “Kuchibirukara Romantica” observed playing. Scroll/Back/stale-ID checks passed; Downloaded Playlists→AAA yielded downloaded rows. No BLE, fresh outside-queue or new human audio pass claimed |
| Apple UI screen off/unlock | VERIFIED sleep rejection; REQUIRED limit | Actual service returned UI_SCREEN_UNLOCK_REQUIRED/canBrowse=false while playing=true. Secure-lock test pending. User accepted this optional mode |
| Source handoff | IMPLEMENTED, NOT fully verified | UI selection validates target, pauses known competing session, observes pause before click. Both-source overlap/audio acceptance outstanding |
| Resume / cold launch / volume | NOT SUPPORTED beta | Capabilities false; no fabricated commands |
| Sleep / range / battery / walking | NOT TESTED | Builds and source state observations do not establish radio/audio continuity |

The actual opt-in companion accessibility service has been enabled through Android Settings under the user's accepted mode. It restricts semantic reads/actions to Apple Music, uses downloaded badges and snapshot-scoped IDs, and rejects unverified app versions. No credentials/private downloads/coordinates/global gestures are used. The separate read-only UI research APK remains uninstalled.

Development ADB permission grants and upgrades do not establish a passed first-install wizard. No root, firmware changes, protected-download extraction, app impersonation, replacement playback engine or network fallback occurred.
