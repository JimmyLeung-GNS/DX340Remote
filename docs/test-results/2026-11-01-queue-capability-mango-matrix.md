# Updated Companion: 20 Mango folder selections

**Verified current Android subset:** the Pixel selected 20 distinct public local files through Mango’s assisted Library over BLE, across four folders. All expected titles/artists reached reported PLAYING with advancing public position. Current 12-byte public file headers identify 17 FLAC and three WAV containers. Every selected title differs from the immediate pre-tap track. These are Library PLAY_ITEM actions; no queue skipping substitutes for selection.

DX340 Android 13/API 33 `eng.iBasso.20260812.181952`, Companion 0.1.0/code 1 SHA256 `92b9972d83b3c9e67f1a24189b7ed1a06ee6a5c57c2061d9c134607eec3e1482` / source `84da612`; Pixel 9a Android 16/API 36 `CP1A.260405.005`, Remote 0.2.0/code 2 `9f085bc22e8665b25dc886ee22cb6f2a977c002baa57c6f057c59fc2a243116e` / source `07c2029`. Mango 4.1.37/code 240; Apple Music 6.5.3/code 1599. Installed hashes match before/after. No app code, build, install or Apple CI change occurred; earlier 272 native/core, 90 Flutter, 22 Swift and 18 analyser checks retain their recorded scopes.

The prior matrix supplied navigation candidates only. Each current enabled phone folder/file row was observed before input, with a fresh root-to-folder path for every selection. After a single playback tap, the driver waited six quiet seconds (minimum 6.109 s) before collecting public MediaSession/uptime pairs 1.5 s apart, the existing normally written host diagnostic file and screenshots. No fresh source UI snapshot broadcast or profiler ran in that timing window. Positions are estimates from public base position, speed/update time and uptime, not acoustic or decoded-frame proof. First observation is intentionally delayed; this does not measure the 1.5 s observed-state/UI target.

Apple Music remained paused with unchanged metadata/cursor for each selection. Before and after every case both endpoints reported no active default network and no ADB reverse. Unmuted music volume 37/headset(4) and volume proof false were retained. Pixel’s stored mobile_data=1 leaves strict T01 open. Hearing questions were waived; headphone audibility and quality remain untested.

| Native operation | Samples | Submission p95 | ATT ACK p95/max | ACCEPTED p95 | Terminal reply p95 |
|---|---:|---:|---:|---:|---:|
| BROWSE | 122 | 7 ms | 155/160 ms | 163 ms | 2704 ms |
| PLAY_ITEM | 20 | 6 ms | 155/157 ms | 158 ms | 3818 ms |
| GET_STATE | 20 | 14 ms | 125/261 ms | none | 399 ms |

Nearest-rank native percentiles start at send registration and include encoding, queues and callbacks. PLAY_ITEM terminal results mean dispatched, not proof of audible playback. Mixed BROWSE pages do not establish separately classified cached/uncached first-page latency. Changed host/phone and observation method versus the older Mango cohort prevent a causal A/B claim; the earlier Apple ACK failure remains preserved. Full T04 remains open.

Selected-row top bounds matched before/final in 20/20 cases; complete rectangles in 20/20 and fixed Browse toolbar in 20/20. No pending-state capture was taken inside the quiet window; older pending-frame evidence retains its original binary/method scope.

Native model `47719f25-1cfd-4c64-a9b3-c4aada9d5e00` grew from 576 to 739 cumulative requests. The 163 new applied results are {"BROWSE": 122, "PLAY_ITEM": 20, "GET_STATE": 20, "SET_PLAYING": 1}. Exactly 20 PLAY_ITEM and 20 automatic GET_STATE rows match this cohort, with one separate final Pause. No auth, skip, queue playback, seek, volume or source-selection request occurs after baseline; the only new connection transition is deliberate final idle. Saved reconnection HELLO/AUTH precede baseline and remain separately recorded. Historical overlapping archives conserve all 739 rows without mutation; merged history is not a current latency group.

An initialization script first read a UTF-8 document using Windows’ default encoding and stopped after read-only preflight/hash capture, before any connection or playback tap. The error is retained; explicit UTF-8 preparation reused the original preflight without overwriting it. No playback input was replayed.

Final 2026-11-01T10:50:46.942779+08:00: both sources PAUSED2 at 37/headset(4)/proof false; host/phone PIDs, four preference hashes, settings, exposed bond lists and both optional bindings match preflight. Phone ordinary Disconnect/help collapsed/target Mango; host service remains enabled/advertising. SD `FBB9-E8FE` remains mounted. No storage setting/mount/reset/private-download action occurred; earlier Apple own-UI SD Card destination verification retains its scope, without whole-library integrity proof. All matrix/export processes are terminal; no hold recorder or phone observer started.

[Structured evidence](2026-11-01-queue-capability-mango-matrix.json) indexes 976 frozen raw files under ignored `artifacts/queue-capability-mango-matrix-20261101`, manifest SHA256 `e9d2bcba35c3d17f62901b61069d17c33d08edbe15067d4f0ed59693954381ef`. Do not rerun helpers against this root. Prior [Mango matrix](2026-11-01-observed-mango-matrix.md), [current Apple matrix](2026-11-01-queue-capability-apple-matrix.md) and all failures retain their distinct scopes.

The current post-selection playlist page was also inspected: it returned no items; no playlist/editing fixtures were created. Full goal remains **active/incomplete**. This selection cohort contains folders, not playlists. Current 100 controls/source, advertised actions, observed-state/UI latency, interrupted-link stalls, strict cellular-off, fresh owner-ready unplugged hold, no-card Internal timeout/Apple MEDIA_MOUNTED crash, physical iPhone/signing, range/calls/security/output/battery/walk and Watch remain open.
