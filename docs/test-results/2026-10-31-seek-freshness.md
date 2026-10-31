# Fresh player time for relative seeking — 31 October 2026

**VERIFIED software regression fix and bounded Pixel/DX340 paused-seek cases. Full goal ACTIVE/incomplete.** [Versions, receipts and raw evidence hashes](2026-10-31-seek-freshness.json).

The installed original-signer phone is `ce47b14caa3b78c6e482387a971bc3efc80fdc7539845a7cb01b5c55daa614f2`, source `25ed62a39ee0ce7bd4d33d83a9596b6cb22ffcf0`. Its pulled APK matches the candidate. Companion remains installed `82ad322f6f804c224603dde6668cb2f34c1a7de83b974edbfc4dec5245c721a5`, source fa60705; a later uninstalled host candidate must not be substituted for those bytes. Pixel9a Android16/API36 CP1A.260405.005, Remote0.2.0/code2; DX340 Android13/API33 eng.iBasso.20260812.181952, Companion0.1.0/code1, Mango4.1.37/code240, Apple Music6.5.3/code1599.

## Fix and checks

After foreground resume the elapsed clock correctly froze its old sample, but Back10seconds could still derive a seek from that stale time. Both platform widget lifecycle regressions fail against the old code. The control now requires an authorized, foreground, fresh player sample with known position/duration and consistent paused state or projectable playing state. Activation checks that predicate again, protecting callbacks held before lifecycle changes. A deliberate seek consumes its sample before sending one existing absolute SEEK; the existing post-command/foreground state read supplies a new sample. The helper clamps the target to the known range. There is no playback retry, new polling, wire change or native bridge/host change.

Both Android/iPhone-style widget regressions cover disabled stale resume, held callbacks, replayed old samples and fresh-state recovery. Three model regressions cover incomplete/unprojectable state, paused zero-clamping and sample consumption, authorization and playing/background transitions. An initial fixture type error while inserting a null sample age is retained; the fixture now explicitly uses a dynamic-value map. Targeted15 and full80 Flutter tests, analyze, Android release and lint pass. Prior199 JVM/19 Swift checks retain their previous source scope; they are not rerun/relabelled as a native change.

## Actual phone/source cases

All four deliberate selections below are **paused seeks**, with no PLAY_ITEM, play/pause, next/previous or volume command. Mango remains paused at78991ms with the same Ghost/tayori metadata. Apple remains paused on Kuchibirukara Romantica/AAA. Its position intentionally changes by40seconds across the cases; position preservation is not claimed.

| Phone and condition | Apple position before → after | Result scope |
| --- | --- | --- |
| Previous277d phone, awake | 183568 → 173568ms | One deliberate seek; Mango UI correctly says seeking unavailable |
| Currentce47 phone, awake | 173568 → 163568ms | One deliberate seek, source and other-player state verified |
| Currentce47 after Home/foreground, initial sleep attempt | 163568 → 153568ms | Same phone PID; one seek between Dozing endpoints, initial power sample still transitional |
| Currentce47, explicit display OFF | 153568 → 143568ms | OFF preflight, ten OFF samples, OFF postflight; unchanged last-wake timestamp |

The first sleep driver's original assessment was too strong: its PowerManager sample had wakefulness-changing/HAL-interactive state and lacked a display-OFF preflight. Raw evidence and the corrected assessment are retained. The separate stricter case reads the actual internal display from `dumpsys display`, records ten OFF samples over a9.235second observed interval, sees Dozing throughout and no wake-time change. It restores ON/Awake and confirms keyguard not showing. This is not deep sleep, a30minute hold, bag/range/walk, battery, audio or full T05 acceptance. The physical foreground stale-race window itself is too short for UIAutomator observation; negative race coverage is software evidence, not a physical race capture.

The first new-phone driver stopped safely when cold-launch artwork placed source controls offscreen. A fresh scroll exposed the button before the single source change; no blind command or playback replay was sent. No DX340 UIAutomator was used.

## Receipts, SD and preservation

Previous277d model20→26 retains all20 earlier records unchanged. Its six new results are HELLO, AUTHORIZE, two SELECT_SOURCE, SEEK134ms and GET_STATE. Newce47 model has11 applied results: HELLO, AUTHORIZE, two SELECT_SOURCE, three SEEK141/204/159ms and four GET_STATE. No errors or evictions. These are native send-to-decoded-reply timings, not GATT ACK, acoustic latency or p95. Four requests do not complete the100-command/source/phone matrix.

Read-only Apple settings and its Downloads chooser again verify **SD Card selected**, Device Storage unselected,237GB Downloaded Music and272GB free. No storage option was changed; ordinary Back closes the chooser/settings. Download Pins stays at the previously observed Off setting. A destination and size label do not establish integrity of every private download. The earlier Device Storage→SD/Pins transition cause, source MEDIA_MOUNTED crash and original no-card/Internal navigation failure remain unresolved. No mount cycle, source reset, private extraction or network activation occurred.

Four own host preference hashes, runtime permission grant/flag sets, exposed bond identities/types and global settings match preflight. Pixel/JIMMYAH grants and both optional browsing modes ON remain. Host stays foreground with both Accessibility bindings. Final phone uses normal Disconnect, clears foreground/started ownership, collapses help and restores target Mango. Both source sessions are paused2 at unchanged37/headset(4), volume proof false. Both USB devices and mounted FBB9-E8FE are present. Pixel mobile_data remains1/noSIM; strict mobile-data-off acceptance remains unproven.

## CI and remaining work

Shared iPhone workflow [41208873642](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41208873642) is terminal **SUCCESS** at exact source25ed62a. Artifact13046470518 ZIP SHA256 `042e6c280d06cd5dda2c10e6bc1633f15f67a174ddd05d3769e78af3430cd09f` matches GitHub's digest and is safely extracted. Inspection confirms80 Flutter tests/analyze,12 capture guards, seven simulator builds, the native UIKit report sheet with matching exported model, and final ordinary rebuild/launch. The restored frame was manually inspected: Bluetooth unavailable/no track, with ordinary controls. Hosted macOS26.6.2/25G83, Xcode26.6/17F113, iPhone Air/iOS26.5. Native Apple paths did not change. This run is closed; no polling remains. Hosted simulator evidence cannot prove owner signing, physical iPhone Bluetooth or hearing.

Earlier Apple offline selection/hearing and immediate full-queue proof remain separate from these paused cases. Continue fresh owner listening/source selections, full albums/playlists/formats/output, strict offline, commands/latency, long background/range/calls/battery/walk, ownership/security, physical owner-iPhone/Mac signing and later Watch. No long hold has started and no completion/device-switch claim is made.
