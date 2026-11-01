# Updated Companion: 100 playback controls per source

**Verified current Android basic-control subset:** the Pixel sent 100 deliberate commands to Mango and 100 to Apple Music: 20 Next, 40 Play and 40 Pause per source. All 200 matched the expected public track/queue/state; no duplicate skip or wrong-source change was observed. Each source had a separate setup Play and cleanup Pause outside its 100 commands. Library browsing remains covered by the separate current [Mango](2026-11-01-queue-capability-mango-matrix.md) and [Apple Music](2026-11-01-queue-capability-apple-matrix.md) selection cohorts.

Original-signer Companion 0.1.0/code 1 `92b9972d83b3c9e67f1a24189b7ed1a06ee6a5c57c2061d9c134607eec3e1482`, source `84da612`; shared Remote 0.2.0/code 2 `9f085bc22e8665b25dc886ee22cb6f2a977c002baa57c6f057c59fc2a243116e`, source `07c2029`. DX340 Android 13/API 33 `eng.iBasso.20260812.181952`; Pixel 9a Android 16/API 36 `CP1A.260405.005`; Mango 4.1.37/code 240 and Apple Music 6.5.3/code 1599. Hashes/PIDs matched before and after. No app code/build/install/CI change; prior 272 native/core, 90 Flutter, 22 Swift and 18 analyser checks retain their scopes.

| Source | Confirmed | Native ACK p95/max | Terminal reply p95/max | Tap to public state upper bound p95/max |
|---|---:|---:|---:|---:|
| mango | 100/100 | 159/172 ms | 598/609 ms | 547/562 ms |
| apple | 100/100 | 166/174 ms | 563/601 ms | 547/1047 ms |

These nearest-rank distributions cover the actual 100 commands per source. Native timing begins at send registration, includes encoding/queue/callback handling and measures ATT completion separately from terminal replies. Public-state timing begins immediately before the single ADB phone tap and ends at the first matching DX340 MediaSession dump; it includes command delivery/polling overhead. This supports the Android control ACK/state subset of T04. It does not measure rendered-phone UI or acoustic latency. NEXT terminal replies are dispatched; public state is checked independently. No fresh source UI-tree SNAPSHOT broadcast or profiler ran during the control cohorts.

| Automatic reads after Next | Samples | ACK p95/max | Reply p95/max |
|---|---:|---:|---:|
| mango | 20 | 95/97 ms | 438/485 ms |
| apple | 20 | 192/194 ms | 779/1023 ms |

Each native cohort has 122 applied error-free results: 20 NEXT, 82 SET_PLAYING (including setup/cleanup) and 20 automatic GET_STATE. There were no artwork requests, unexpected connection transitions, authorization requests, queue playback or volume commands inside either control cohort. Earlier Apple 775 ms read-ACK failure remains preserved. Different tracks, warmed artwork cache and observation methods prevent a causal performance claim; mixed library-page and physical iPhone targets remain open.

Mango used fixed queue IDs 4–23. Public 42-byte FLAC STREAMINFO headers supplied durations for its 28 files, including the 24.984 s intermission. Every complete five-command cycle plus its first public base position remained below the corresponding duration. Apple reused activeQueueId 0 while shifting its window: 21 consecutive exact versioned titles were distinct, all 20 successors matched in order, and the final three-title queue prefix independently matches 20 advances. All 20 current-title duration labels were available in the phone UI; the same conservative wall-time bound excludes natural endings. These checks distinguish deliberate Next results from automatic track progression.

The other source remained paused with unchanged public metadata/cursor before and after every command. Both devices had no active default network and no ADB reverse before/after each source cohort; this is not a continuous network trace or strict cellular-off proof. Stored Pixel mobile_data=1 keeps T01 open. Unmuted music 37/headset(4), volume proof false, four preference hashes, settings, exposed bond lists and both optional bindings were retained. Headphone audibility and quality were not independently tested; routine listening prompts were waived.

Current setup capabilities advertise Previous on both sources and Seek on Apple Music. Mango Seek, Shuffle/Repeat and remote volume are not advertised; Mango’s actual UI reports seeking unavailable. This cohort does not test Previous or Apple Seek.

During setup, an ADB shell header read expanded an embedded LF to CRLF (42 bytes became 43). The exact shell/exec-out hex comparison is retained. Reading all 28 headers with exec-out fixed the driver step. The one setup selection and Pause were retained; no playback input was replayed and the failure preceded both 100-command cohorts.

Native model `47719f25-1cfd-4c64-a9b3-c4aada9d5e00` has all 996 cumulative rows conserved across overlapping historical/current exports, with unchanged shared rows. Five current exports bridge the 256-row retention limit; historical rows support conservation only, not current latency statistics. Final ordinary Disconnect is recorded separately.

Final 2026-11-01T11:12:25.449654+08:00: both sources PAUSED2/37/headset(4)/proof false. Host PID 28758/epoch 71344a90 and phone PID 1355 retained, phone disconnected/help collapsed/target Mango, host enabled/advertising. SD `FBB9-E8FE` remains mounted; no storage setting/mount/reset/private-download action occurred. Earlier Apple own-UI SD Card destination scope remains separate from whole-library integrity. All control/export workers are terminal. No unplugged hold recorder or phone observer started.

Post-cohort power cleanup is a separate frozen case: Pixel battery 9%, Android AC powered true/USB powered false/status 4 (not charging). Home and Sleep were sent after normal Disconnect; observed wakefulness is Dozing. This reduces display load without changing charging settings. Charging effectiveness remains unverified. This is not a background/sleep/battery acceptance run.

[Structured evidence](2026-11-01-queue-capability-controls.json) indexes 1289 frozen files under ignored `artifacts/queue-capability-controls-20261101`, manifest `2d26f9eeefd71f7d4d9e770fbec7d8f009d368438dbe0fb9c0a2b6066fa21e6f`. The separate eight-file power cleanup has manifest `c72a4d95a81c246663cb5452141c045084bb33ee37ca2def28360cddc1845813`. Do not rerun helpers against either frozen root. Prior [e1/9f controls](2026-11-01-ordinary-controls.md) and all older failures retain their exact binary/method scopes.

Full goal remains **active/incomplete**. Next: Previous on both/Apple Seek, supported-action/source-switch/lifecycle coverage, rendered-UI/cache latency, strict cellular-off, owner-ready unplugged hold, interrupted-link stalls, original no-card Internal timeout/Apple MEDIA_MOUNTED crash, physical iPhone/signing/range/calls/security/output/battery/walk and Watch.
