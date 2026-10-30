# First physical Android-phone Bluetooth session

30 October2026, JIMMYAH Windows development host. **PARTIAL phone acceptance; full goal ACTIVE/incomplete.** This real Pixel-to-DX340 BLE result is separate from earlier PC-central, DX340-only remote launch, local source-adapter and iOS simulator evidence. [Sanitized record](2026-10-30-pixel-first-bluetooth.json).

## Exact combination

| Component | Observed scope |
|---|---|
| Controller | Google Pixel9a, Android16/API36, build CP1A.260405.005, security patch2026-04-05 |
| Phone application | Shared Flutter/native Kotlin org.dx340.remote.controller0.2.0/code2, source587ea78; installed/pulled SHA256 **111a29f854a8038613c3818ace5948020f8516842b7b57ba5df22e6baeaa0c6d** |
| DX340 | Android13/API33, DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys |
| Companion | Unchanged sourcead44610; installed/pulled SHA256 **6c390636fa8fb64c86f1153d93b0d459f4d623b3eea725bd97848fad7cfeb85a** |
| Source apps | Mango4.1.37/code240; Apple Music6.5.3/code1599 |
| Signing | Original public certificate SHA25624f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d; both installed APKs/signers compared |
| Output | Owner-confirmed AMP15 phone out; STREAM_MUSIC/headset37 before/after; no volume commands |

The owner unlocked the Pixel and granted Nearby Devices/notifications. Package inspection confirms BLUETOOTH_SCAN, BLUETOOTH_CONNECT and POST_NOTIFICATIONS granted; native connected-device foreground service observed active. This is a fresh separate-phone install, not an upgrade/background-duration pass.

## Physical checks

| Check | Result and limits |
|---|---|
| Protected BLE/enrollment | VERIFIED actual service discovery/MTU517, protected encrypted-MITM CCC and HELLO/AUTHORIZE path. LOCAL_APPROVAL_REQUIRED observed before owner approval, then Connected to DX340/live data. No security requirement weakened. Exact manual OS-code comparison was not captured; no full production-security claim |
| Metadata/artwork/native browsing | VERIFIED Pixel displays live Mango title/artist/album/decoded artwork, native root/Album/first live-album track rows and queue range1–33 of33. Not every queue item/cache/latency acceptance |
| Selected-track audio | VERIFIED **one deliberate Pixel native-album PLAY_ITEM**,23:00:35+08:00: 白日夢2/tayori from1st Live “春を待つ”. Pixel changes title/Pause; independent DX340 snapshot reports Mango state3/active item2/title白日夢 and Apple state2. Owner: **“Yes, the correct song is audible.”** No replay/retry. Native album route, not a new qualified-file/folder matrix |
| Disconnect/saved grant | VERIFIED one deliberate disconnect while playing: independent DX340 snapshot still reports same Mango title playing/Apple paused. Fresh saved-grant reconnect needs no new owner approval and shows same title/estimated elapsed2:26; no new Play. Not acoustic dropout/ten range/long-sleep proof |
| Phone Pause | VERIFIED one Pixel Pause; “Playback state confirmed by source.” DX340 independently reports Mango state2/position150242ms and Apple state2. Phone freezes elapsed2:30 despite Mango's paused snapshot advertising speed1.0 |
| Paused source changes | VERIFIED Pixel selects Apple then Mango; old library/queue rows clear before fresh reads. Both players stay paused. No playing-source/acoustic-overlap acceptance |
| Apple queue | VERIFIED displayed1–50,51–100,101–101 of101; final row One Night Animal; Start of queue returns1–50. Not every-item comparison/stale-revision fault/later-page playback |
| Apple assisted library | VERIFIED Downloaded → Albums → AAA2nd Anniversary Live → track rows Kuchibirukara Romantica/Paradise Paradise. DX340 awake/unlocked. **No Apple phone selection/audio result** |
| Mango assisted library | VERIFIED storage picker → Internal Storage/Music/AAA/2016 Fuji-Q folder → visible FLAC filenames. Existing Playlists route settles to original empty list. No fixture created/no assisted file played |
| Cleanup | VERIFIED both source sessions paused37, Mango control target, volume verified=false/canAdjustVolume=false. Pixel disconnected; host advertises, Pixel and JIMMYAH approvals retained, no pending request. Original phone network settings restored after disconnect |

## Offline boundary and failed setup attempts

During successful reads/selected-track experiment, Wi-Fi reads0 on both endpoints and both connectivity dumps report **Active default network:none**. Pixel SIM state ABSENT,NOT_READY. `svc data disable` completed but legacy unscoped `mobile_data` remained1. No active IP network was available; the plan's literal controller-cellular-data-off Settings requirement is **not verified**, so T01 remains partial. After final Disconnect, `svc wifi enable`/`svc data enable` restored intended original settings1/1/1 for Wi-Fi/mobile-data/Bluetooth. DX340 Wi-Fi remains off. No Wi-Fi/cloud fallback introduced.

Initial OS pairing notifications expired; one Pixel notification used unexpected label TOBY, which was not trusted as identity. Owner reported missing/expired prompts. Standard Bluetooth Settings plus temporary120-second DX340 discoverability, explicitly allowed by the owner, later yielded saved/bonded records. No exact manual matching-code confirmation was captured.

Two companion-approval requests expired before the owner could tap. Merely reopening pairing/retrying the expired link did not recover the row; explicit Disconnect/fresh Find/Connect did. Intermediate reconnections also stalled in service discovery. Retain these as failed/incomplete setup attempts, not a clean enrollment or nine-of-ten result. Phone setup text could remain stale after approval expiry: a UX follow-up, not proof of a live request.

Successful owner sequence on both screens: **DX340 Open pairing60s → Pixel Find/Connect → DX340 approve identified Pixel → Pixel Retry authorization**. A later assistant-operated Retry visibly confirmed Connected to DX340. Assistant never tapped owner approval/revocation or accepted OS Pair. Existing PC approvals/bonds preserved; no revocation test.

## Signing helper and verification

Initial fresh install stopped before installation because Android16 `pm path` returned exit1/empty for the absent app. `scripts/verify-android-signing.ps1` now accepts absence only after a successful empty package-manager listing, verifies candidate package/signature first and writes a fresh new-install receipt. Existing signing comparison remains; no uninstall/reset/key migration.

Actual fresh Pixel install succeeds; post-install pulled bytes match111a29f8…/original signer. Unchanged companion pulled bytes match6c390636…/original signer. Both installed/candidate comparisons pass. Separate negative checks reject unavailable ADB target/wrong candidate package; PowerShell syntax parsing passes. No application code/build changed; prior151 JVM/46 Dart/Swift9/CI results retain their source/package scope.

Private raw evidence: ignored `artifacts/pixel9a-first-phone/` inventory/install/signing receipts, UI/action XML/screenshots, GATT logs, source-session/audio/connectivity snapshots, cleanup receipt. Unique serials/addresses/unrelated notifications are excluded from tracked evidence. Final local GET_STATE corroborates cleanup only; it did not perform the phone selection.

Next: unambiguous cellular-off state, measured Android discovery/controls and full phone native/folder/playlist/Apple outside-queue selections with owner listening; then background/lock/range/restart/security/faults/output/upgrades. Preserve Pixel/JIMMYAH grants; do not repeat PC revocation. Physical iPhone/signing, SD/rescan, battery/walking, two-controller handoff and Watch remain open. Storage-volume invalidation was inspected/planned before Pixel arrived but **not implemented**.
