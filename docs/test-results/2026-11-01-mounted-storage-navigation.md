# Mounted-card Mango storage navigation — 1 November 2026

**Verified browsing subset; original no-card failure stays open.** [Structured evidence](2026-11-01-mounted-storage-navigation.json) records the physical Pixel requests, source-local traces, preservation checks and150 raw-file hashes.

Unchanged installed binaries: original-signer Companion `dc53629d…`/source `1e3c551` on DX340 Android13/API33 `eng.iBasso.20260812.181952`, PID4334; shared Remote `4eea5c15…`/source `a5b3fc0` on Pixel9a Android16/API36 `CP1A.260405.005`, PID17394. Mango4.1.37/code240 and Apple Music6.5.3/code1599 remain paused at37 through headset(4). The same SD public volume stays mounted throughout. This session changes no application code or installed package.

The Pixel made13 successful BROWSE requests after its saved HELLO/AUTHORIZE connection. Every current request has an applied RESULT and a separate ATT write receipt. All31 entries from the preceding export are unchanged; the report grows to47 with zero evictions. One additional GET_STATE predates the current connection and belongs to the previous session between exports. It is preserved separately rather than counted as a new request in this navigation run.

| Selected page/path | ATT write receipt | Matched RESULT |
|---|---:|---:|
| Internal Storage |149ms|3,467ms|
| SD Card |125ms|3,979ms|
| Internal from cached SD root, after foregrounding Companion |156ms|4,802ms|
| SD Card again |136ms|4,237ms|
| SD Card / Music |135ms|2,387ms|
| Internal from nested SD Card / Music, after foregrounding Companion |143ms|6,628ms|

In the two foreground-start cases, the phone first chose the virtual source root/storage choices without changing Mango's cached page; Companion was then foregrounded on DX340 before the Internal selection. Read-only source-local SNAPSHOT receipts capture guarded navigation: foreground launch → SD root/menu → one storage click → Internal finish at4,331ms; and foreground launch → SD Card / Music → SD root/menu → one storage click → Internal finish at5,861ms. These are source-side durations, distinct from the phone's matched reply timings. Intermediate snapshots remain labelled in-progress; the final nested snapshot contains the terminal finished step.

The SD Music folder returned an empty page, consistent with a read-only listing of that public folder. No file was selected or played. An attempted phone scroll on this short empty page was refused by the driver before input because it had no scroll surface; this was not a source failure or a replay. Three same-target source-local SNAPSHOT receipts are additional diagnostics, not extra BLE commands.

Both foreground paths and several other pages exceed the proposed three-second uncached-page target. This test does not close performance acceptance. The earlier no-card cached-SD/Internal8,412ms timeout is still unresolved because the card was never unmounted here. Preserve the earlier Apple MEDIA_MOUNTED crash and do not infer its repair from mounted browsing.

Final checks preserve all four preference hashes, recorded global settings, three controller grants, source positions, host/phone PIDs, both optional modes/bindings and mounted SD. Both sources stay paused2 at37/headset(4). Pixel uses ordinary Disconnect with connection help collapsed and foreground service cleared; Companion's main screen is restored and host service remains foreground. The Mango control target is retained. No scan, mount cycle, network/power-policy change, source reset or playback/volume command occurred.

Current-build hearing, selected-file audio, full T08/T11, strict offlineT01 (Pixel cellular flag1),30-minute background, range/calls/battery/walking, owner-iPhone/Mac signing and Watch remain unfinished. The previous unplugged job remains terminalFAILED/ARMING_TIMEOUT with zero hold samples; fresh availability is pending and no observer/build/CI is live. Prior successful Apple jobs stay closed and simulator-only.
