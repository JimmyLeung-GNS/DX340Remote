# Compatibility and acceptance matrix

Reconciled 1 November 2026. Full goal **ACTIVE/incomplete**. Current installed Companion `61153223…` / source `9fb503a` and shared Pixel `95d29be6…` / source `e40b822` are distinct from older listening/build cohorts. See [status](status.md), [interrupted-library evidence](test-results/2026-11-01-interrupted-library.md) and the [structured acceptance ledger](test-results/2026-11-01-acceptance-reconciliation.json).

## Current platform evidence

| Platform | Verified scope | Remaining gate |
|---|---|---|
| DX340 Android13/API33 | Companion0.1.0/code1 original signer, installed SHA61153223…;241 JVM/native builds/lint plus release diagnostic contract. Actual Pixel/PC subsets linked below. | Full source/audio/lifecycle acceptance; one debug-injected physical timeout recovers automatically; natural failure/full matrix open. |
| Pixel9a Android16/API36 CP1A.260405.005 | Shared0.2.0/code2 SHA95d29be6… installed;82Flutter/analyze/release/lint; physical interrupted browse/reconnect and fresh SD browse. | Three current611/95 source-state selections/row geometry verified; hearing pending,100 controls/source and reliability/offline matrix open. |
| Shared iPhone, proposed iOS17+ | Exacte40 job41231890444 SUCCESS;82Flutter/12guards/seven builds/UIKit report/export/restoration, archive verified. | Signed owner-iPhone install, BLE/audio/recovery; actual model/OS/team unknown. |
| Native iPhone reference | Exacte40 job41231890453 SUCCESS;21Swift/12guards/build/launch/archive verified. Both Apple jobs closed. | Retained reference; physical parity not verified. |
| Native Android reference | Current native build/lint succeeds; retained reference app. | Same-DX340 UI/compile results are not a separate-phone radio pass. |
| Other DAPs/phones and reported iOS27.2 | Not qualified on hardware. iPhone Air/iOS26.5 hosted simulators are the recorded Apple targets. | Re-inventory each actual device/OS/source/version; no universal compatibility claim. |
| Owner Mac / Watch | Current Windows11/JBR21/pinned Flutter available; Xcode tool returns spawn xcrun ENOENT. | Owner confirms only Windows/Android available now; physical Mac/Xcode/signing/iPhone pending; Watch implementation gated on stable phones. |

## P0–P9 stages

| Stage | Status | Evidence and remaining work |
|---|---|---|
| P0 | Partial | DX340/Pixel/Windows/signing inventoried; owner Mac/iPhone model/build/team and Watch remain unknown. |
| P1 | Partial | Actual Android/PC BLE and local authorization subsets verified; physical iPhone/offline/sleep gate incomplete. |
| P2 | Partial | Versioned source adapters and many physical subsets exist; full both-phone/source action/audio matrix incomplete. |
| P3 | Implemented; partial validation | Host protocol/routing/authorization/storage guards implemented; one physical debug-injected missed-callback timeout/recreation passes; natural callback faults/full matrix open. |
| P4 | Implemented; partial validation | Shared Pixel95 installed; saved setup/browsing/interruption recovery verified.100 controls/source, full audio/reliability remain open. |
| P5 | Implemented; simulator verified | Shared/native iPhone build and simulator/export checks pass. Owner signing/install/BLE/audio not tested. |
| P6 | Implemented; partial validation | Mango native metadata plus optional awake/unlocked folders/playlists; current navigation verified, current20-file audio matrix incomplete. |
| P7 | Implemented; partial validation | Optional awake/unlocked Apple Downloaded UI adapter; older offline outside-queue hearing is scoped, current20/both-phone matrix incomplete. |
| P8 | Partial | Original-signer updates and fault subsets pass; page latency, calls/range/sleep/battery/walking/output/store gates remain open. |
| P9 | Not started | Native Watch remains gated on stable phones; no Watch hardware evidence. |

## T01–T18 acceptance

| Test | Status | Current scoped finding | Required remaining proof |
|---|---|---|---|
| T01 | Partial | Current95 read-only BLE succeeds with both systems reporting no active/default networks in four snapshots each; Settings Wi-Fi off, SIMs Add a network, SIM property ABSENT,NOT_READY. Stored Pixel mobile_data1 remains, so strict cellular-off and physical iPhone are open. [Current subset](test-results/2026-11-01-no-active-network-read.md). | Both phones connect with Wi-Fi and cellular off. |
| T02 | Partial (older binary) | Ten earlier1a592/491 desk saved-connect cycles; current95 saved reconnect passes. No bag-distance discovery cohort. [Evidence](test-results/2026-10-31-saved-player-reconnect.md). | At least9/10 discovery/connect trials within10s at normal bag distance, each phone. |
| T03 | Not passed | Older owner-audible controls retain their source hashes; current95 interrupted browsing is not100 playback controls. [Evidence](test-results/2026-10-31-current-phone-source-audio.md). | 100 deliberate commands per source/phone, at least99percent confirmed, no duplicate skip/wrong-source command. |
| T04 | Not passed | Earlier4ee read-only ACK/reply p95 Mango160/469ms, Apple168/494ms are scoped. Current95 fresh SD result7777ms misses3s page target; no full control/state/acoustic pass. [Evidence](test-results/2026-11-01-write-ack-timing.md).  Current95 isolated GET_STATE ATT94ms/RESULT322ms is one read, not a p95/control/audio pass. [Current read](test-results/2026-11-01-no-active-network-read.md). | ACK p95 below500ms, observed local state below1.5s; cache pages below500ms/uncached below3s measured separately. |
| T05 | Not passed | Earlier1802.562s USB-powered Awake/background subset; two USB-power-wake sleep holds failed. Job5024 failedARMING_TIMEOUT with zero hold samples; parser fixed, no repeat underway. [Evidence](test-results/2026-11-01-unplugged-arming.json). | Actual DX340 sleep/30minute pause followed by phone resume with no DX340 handling. |
| T06 | Not tested | No physical ten-cycle range cohort on either phone. [Evidence](test-results/2026-11-01-interrupted-library.md). | Ten out-of-range returns; reconnect within15s after phone foreground/host available without stale replay. |
| T07 | Partial (older audio) | Older source-local20 cases retain24attempts/4rejections/mixed builds; later b805/00e5 audible outside101 queue and ce47/82ad outside21 queue remain separate. Current611/95 downloaded-album selection outside captured101 queue reaches expected PLAYING3; hearing pending. [Current pair](test-results/2026-11-01-current-pair-source-selection.md). [Evidence](test-results/2026-10-31-current-sd-audio.md). | 20 correct offline downloaded selections across5albums/2playlists, outside prior queue included, each phone. |
| T08 | Partial (older audio) | b805/00e5 physical Pixel WAV/FLAC owner hearing (2/20 in that scope) and earlier source-local cases retain caveats.Current611/95 two selected WAV/FLAC files reach expected PLAYING3, sampled rows stable; hearing pending. [Current pair](test-results/2026-11-01-current-pair-source-selection.md). [Evidence](test-results/2026-10-31-current-phone-source-audio.md). | At least20 correct FLAC/WAV selections across albums/folders/exposed playlists through Mango, each phone. |
| T09 | Partial | Older source-switch/state/audio subsets retain exact hashes; current611/95 exclusive source snapshots and deliberate pauses pass, hearing pending/no full acoustic matrix. [Current pair](test-results/2026-11-01-current-pair-source-selection.md). [Evidence](test-results/2026-10-31-phone-ui-source-scope.md). | Alternate source control during playback; only selected source changes, no unwanted overlap. |
| T10 | Partial (older audio) | Older37→36→37 owner-confirmed adapter steps on AMP15 phone out. Current volume proof false and remote volume disabled; output/range/wheel/phone/reconnect coverage incomplete. [Evidence](test-results/2026-10-29-resumed-volume.md). | Bounded low-level volume steps match wheel and actual output, no reconnect jump. |
| T11 | Partial; failures retained | Real OS cycles/add-remove stale-folder cases retain scope. Current mounted root/nested Internal navigation passes; original no-card Internal8412ms timeout and Apple MEDIA_MOUNTED crash remain open. [Evidence](test-results/2026-11-01-mounted-storage-navigation.md). | Safe add/remove/rescan/physical SD handling with stale IDs rejected/refreshed and original files preserved. |
| T12 | Partial | Current95 pending browse loss clears spinner; saved reconnect does not replay. Earlier radio/permission/process recovery scopes retained; new host debug-injected timeout auto-recovery verified without replay; natural faults/calls/full sleep/two-phone matrix incomplete. [Evidence](test-results/2026-11-01-interrupted-library.md). | Calls/radio/permission/background/restart produce honest status and bounded recovery with clear required action. |
| T13 | Not physically tested | Six two-hour entries/matched procedure and12 software checks prepared; zero measured battery trials. [Evidence](test-results/2026-10-31-battery-preparation.md). | Three matched2hour off/on pairs; proposed extra drain no more than5percentage points/2hours. |
| T14 | Not tested | No physical one-hour bag walk on either phone. [Evidence](test-results/2026-11-01-interrupted-library.md). | One hour in actual bag per phone; supported controls reliable without unexpected playback interruption. |
| T15 | Partial | Prior unapproved/revoked denial subsets retained. Current desktop added through normal identified approval; Pixel/JIMMYAH and OS bonds preserved. No existing PC revocation repeated. Full each-phone/live timing incomplete. [Evidence](test-results/2026-11-01-framing-bounds.md). | No metadata/control before approval; revocation enforced, preserving instructed grants during development. |
| T16 | Partial | PC65537-byte and60s slow-drip bounds, earlier malformed/quota/idle subset, current95 pending-browse loss/no replay verified. One debug-injected successful indication callback omission reaches the real deadline and recovers, with no old read replay. Duplicate playback mutations, input ATT write faults and natural vendor failures remain open. [Evidence](test-results/2026-11-01-framing-bounds.md). | Duplicate/delayed/truncated/oversized/transfer-loss faults cause no crash, duplicate mutation or unbounded allocation. |
| T17 | Partial | Original-signer host611/phone95 verified in place, grants/settings retained. Earlier enabled/stopped package-restoration scopes retained. Final fresh install/production signing/owner-iPhone not tested. [Evidence](test-results/2026-11-01-interrupted-library.md). | Fresh install and upgrades preserve intended pairing/state with valid installation/signing. |
| T18 | Not implemented/tested | No Watch target/hardware acceptance; stable-phone gate remains unmet. [Evidence](test-results/2026-11-01-interrupted-library.md). | Later supported Watch commands tested on actual hardware, wrist-down/out-of-range, no internet. |

Three current pair source-state selections/row-geometry checks, with all hearing answers still pending, are recorded in [current source selection](test-results/2026-11-01-current-pair-source-selection.md).

One controlled hardware timeout case is detailed in [injected indication recovery](test-results/2026-11-01-injected-indication-recovery.md). It supplements P3/T12/T16/T17 without marking any complete acceptance pass.

## Evidence boundaries

Original Mango and Apple Music remain the playback engines; Apple-managed downloads retain the owner-selected SD Card destination. A destination/size label is not proof that every download is intact. Optional source UI browsing requires the accepted awake/unlocked mode. No root, source identity impersonation or private-download extraction.

Older hearing, queue, storage, volume and reliability results keep their exact recorded binaries/conditions. They are not promoted to95/dc or physical iPhone. Unit/widget tests, CI builds, metadata, ATT receipt, decoded reply, observed player state and owner hearing prove different things. No T01–T18 row is a full both-phone acceptance pass.

The [previous matrix snapshot](compatibility-matrix.bade60d-archive.md) is preserved byte-for-byte from committed sourcebade60d (canonical SHA in the ledger); its old Current labels are historical. Existing dated reports remain authoritative for those historical findings.
