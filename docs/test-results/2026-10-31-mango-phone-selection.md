# Mango phone selection, scroll recovery and bounded setup

31 October2026, Asia/Hong_Kong. Latest application source **f5f5a03b04ea46d1631328e44097a4caf9512215**. Full goal ACTIVE/incomplete. This follows [the initial Library incident](2026-10-31-library-target-stability.md), preserving its failures and older package scope.

## Current packages and verified software

| Component | Source/package and result |
|---|---|
| Installed DX340 companion | Unchanged **58314b84cd45447fd866c1ea9a48922c39edd9584f14100c47cd3cb8c91ee97e**, sourcecf3bccc; Android13/API33, Mango4.1.37/code240, Apple6.5.3/code1599 |
| Installed Pixel9a controller | New ordinary shared0.2.0/code2 **76554f3fb07fa6abfab9180d5b017cc007bdb5036a70c63206a3780239436295**, sourcef5f5a03; Android16/API36. Original signer24f48aa3… compared before in-place update; installed bytes pulled/reverified. No uninstall/data reset |
| Previous controller | **208a797e…**, source42b68ab. The first Intro listening confirmation below belongs to this previous package |
| Android | Native assembly/lint and **162 aggregate JVM tests**: protocol44/cache7/adapters111, zero failures/errors. Shared analysis/**53 Flutter tests**/release/lint pass. Newly built companion/reference/probe outputs were **not installed**; do not attach installed583… audio evidence to those candidates |
| Native Apple | [41111847167](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41111847167) SUCCESS atf5f5a03: **12 Swift tests**, native build/launch/captures and12 capture-matcher checks. Artifact13003553632 SHA256**b9f47df123312dd28e5ec7100fa9358673dbf9324c9da67ca9ada90dd3b3f962** |
| Shared Apple | [41111847393](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41111847393) SUCCESS atf5f5a03: **53 Flutter tests**, analysis, simulator builds/captures and ordinary-app restoration. Artifact13005500187 SHA256**86146d94dcc46eebda371afc310b4e3734deb831001202866f48cc2c512df076** |

Both Apple ZIPs were downloaded, SHA256-verified, safely extracted and their test/build logs inspected. Hosted macOS26.6.2/build25G83, Xcode26.6/build17F113; iPhone simulator evidence only. No owner-iPhone signing/install, reported iOS27.2 compatibility or physical Apple BLE/audio pass. The earlier shared job41107018018 also completed SUCCESS at42b68ab/51 tests; artifact13002927005 SHA256673b4d4f6aaad4c2a5af47e829fc5a583779f0c34beeb2cc0d42e83027011989 was verified separately.

## Two distinct presentation corrections

The earlier42b68ab correction isolates changing status/error/pending messages from song geometry and binds a tap to the displayed row/revision/source. The owner then identified the failing path as **Mango folders or playlists**.

During actual folder testing on208…, returning from Player showed the first row at a different position and hid the album header. Tests then reproduced a related cause: a new long album could inherit **330 logical pixels** of an earlier folder's offset when a reply arrived before the loading frame rendered. f5f5a03 gives Library scroll storage its source, revision, requested/returned parent, page and loading context; Queue storage includes source, revision and window. Status/error/busy values are excluded from list identity. Two new Android/Cupertino tests require the fresh album at offset0 and preserve its position across Player → Library. Earlier failing test output is retained.

On the installed765… Pixel, `01. OPENING VTR.flac` remained at Android UIAutomator bounds **[53,1457][1028,1648]** before and after Player → Library. Selection-one/selection-two first rows retained the same physical bounds before versus pending; `02. CALL.flac` retained **[53,1648][1028,1840]**. Controls became disabled while pending. These are observed UI bounds plus separate software held-pointer tests, not a physical manual-finger/accessibility certification.

A follow-up reviewer returned **ship only for seven supplied renders**: two actual connected Pixel light/default images (assisted invalidation guidance and24-item native Queue), plus five hosted iPhone images (labelled synthetic Library light/large-dark/Queue, ordinary Library and restored Player). No new visual world/tokens. Independent long-message/list scrolling, complete large-text reachability and TalkBack/VoiceOver remain open. Static images do not prove selection/audio.

## Approval recovery and client setup deadline

The old local-approval attempt expired before its owner action; its requesting row disappeared. A fresh identified request was prepared without accepting the approval on the owner's behalf. The DX340 subsequently recorded **CONTROLLER_APPROVED** and two saved approvals; the Pixel's ordinary Retry authorization then displayed Connected to DX340. JIMMYAH was preserved. After the765… upgrade, a **saved-grant Find/Connect succeeded without another pairing window or approval**. Reads/selections/Pause remained usable beyond seven minutes, including a later native Queue read. No revoke/unpair test.

Kotlin and Swift now bound an unfinished setup to60seconds after the protected subscription. Successful authorization cancels that client timer. Clearing a connection or starting another invalidates old timer tickets. Foreground entry, outgoing requests and incoming replies recheck elapsed time, so a delayed task does not leave stale approval guidance after suspension. LOCAL_APPROVAL_REQUIRED pauses automatic reconnect while waiting for the owner's action; successful authorization restores normal reconnect policy. Timeout closes the unfinished link, clears pending work and explains a fresh Find/Connect; saved grants/bonds and source playback are unaffected by that client cleanup.

This is a **client recovery bound**, not an extension or measurement of the host's pairing/bootstrap window. Shared six-scenario fixtures cover boundary/cancel/new-attempt/suspension/clock-rollback/late-timer behavior, with additional integer-limit tests in Kotlin/Swift. The scheduler uses monotonic elapsed checks: Android [Handler callbacks can be delayed by sleep](https://developer.android.com/reference/android/os/Handler), while Apple's [ContinuousClock advances during sleep](https://developer.apple.com/documentation/swift/continuousclock). No wake lock or network polling was added.

Software and hosted compilation pass; the approved Pixel session supplies a positive non-expiration sample. **Physical expiration of an unapproved765… Pixel session and actual iPhone setup timeout remain NOT TESTED**. Do not revoke the retained Pixel/JIMMYAH grants merely to manufacture that case.

## Scoped physical folder listening

The owner authorized enabling optional Mango browsing **for this test**. Before it, app mode and Mango Accessibility were off; both were observed enabled/bound for browsing. Apple mode/access remained on. Selections used the ordinary Pixel Library over BLE and the existing Mango engine on DX340, with actual DX340 awake/unlocked and output37/headset(4).

| Deliberate selection | Controller scope | Observed result |
|---|---|---|
| Gold Symphony: `01. Intro -GOLD SYMPHONY-.flac` | Previous208…/42b68ab | Matching Intro metadata/playing and owner-confirmed expected audio; ordinary phone Pause afterward |
| WAY OF GLORY: `01. OPENING VTR.flac` | Current765…/f5f5a03 | Matching OPENING VTR/AAA playing and owner-confirmed expected audio; row/tab-return geometry stable |
| Gold Symphony: `02. CALL.flac` after browsing again during active playback | Current765…/f5f5a03 | Before command Mango was playing MAGIC; one CALL selection reached matching CALL/AAA playing and owner-confirmed expected audio; phone mini-player Pause afterward |

Each selection used **one Play tap, no replay/retry of playback**. Three audible confirmations span **two phone builds**, with only two on765…. All are FLAC-named files across two album folders, not a full qualified-format/20-track T08 result. Playlists, WAV, SD/rescan and physical iPhone selection were not exercised here.

The first playing samples for Intro/OPENING still reported the previous title; later source observations matched the selected tracks before their audio questions. The CALL receipt sampled previous MAGIC metadata through elapsed10.922s and matching CALL at12.000s, measured from the diagnostic script start **including UI capture overhead**, not ATT acknowledgement or clean source latency. Keep the metadata lag visible; no p95 target is passed. MAGIC was observed before the final selection without an agent Next command; the reason for that intervening advance was not independently measured.

## Retained failure and limits

- **Back to parent failed once with STALE_LIBRARY after playback changed.** The phone cleared the old page and displayed refresh guidance. Explicit Browse source library recovered a fresh root/folder route while source playback remained active; the later CALL selection passed. Post-play assisted navigation needs clearer recovery and broader testing. No claim that every browse request passed.
- One UIAutomator transition returned null root. Its old XML was not accepted as current evidence; subsequent input used unique dump filenames and required a successful fresh result. No product crash was inferred from that automation failure.
- Automatic approval review rejected a combined command including a proposed Pixel mobile-data disable, reporting only **“blocked by policy.”** None of that command ran. No alternate radio-changing route was used. Pixel Wi-Fi0/mobile_data1/Bluetooth1 at this test's preflight remained unchanged. **This is not strict T01/offline Settings acceptance**; the older Wi-Fi1 cleanup was verified in the preceding continuation, and0 was the freshly observed setting for this new test.
- The original owner's full failed-click/disconnect sequence was not reconstructed from retained logs. The controlled selections above worked, with the one stale navigation failure retained. They do not establish rapid-tap robustness,100-command reliability, background/range/battery/walking behavior or the cause of the earlier133 error.

## Separate no-SIM offline follow-up — VERIFIED in stated scope

After the owner reported mobile data off, the generic global mobile_data flag still read1. The initial flag-based preflight failed; discovery alone ran, with no Play/Pause. Read-only investigation found **no active/available/selectable subscription**, default/active data subscription IDs−1 and SIM states ABSENT/NOT_READY. Pixel connectivity had **no active default network**. Both devices' native Wi-Fi status reported **Wifi is disabled**. The generic flag was therefore not accepted as proof of an active cellular path or of a user-toggle change. Android's [data-enabled API is subscription-scoped](https://developer.android.com/reference/android/telephony/TelephonyManager#isDataEnabled()).

In that verified no-network/no-SIM configuration, a fresh saved-grant Pixel connection succeeded. One ordinary phone Play resumed **CALL**: actual Mango state3/CALL, Apple state2. One ordinary Pause returned Mango to state2, Apple remained2. Afterward both devices reported no active default network, both Wi-Fi settings stayed0, and the Pixel still had no available/selectable subscription or active data subscription. The phone displayed source-confirmed playback results. It was then disconnected normally. No new human audio confirmation was requested for this short sequence.

This passes a **physical Android no-SIM/offline connection and basic-control subset**, not the SIM-installed cellular-toggle variant, either-phone full T01/Gate A, latency or background/reliability matrix. It does not retrospectively change the network scope of the three folder listening samples. Raw evidence is separately under ignored `artifacts/android-offline-20261031/`. No automated radio-changing action was used after the review rejection; generic mobile_data remains1, with no active data subscription.

## Verified cleanup and next work

Final: both players **paused2**, music **37/headset(4)**; ordinary Pixel **Disconnect verified**, including after the offline follow-up. Installed companion583…/phone765… bytes rechecked. **Two approvals retained (Pixel/JIMMYAH)** and controller preferences equal to the pre-upgrade restored-grant snapshot. No bond removal/revocation. Optional Mango app mode and its Accessibility service restored **off**, Apple mode/service remain **on**. Pixel network flags equal this test's preflight0/1/1; active cellular subscriptions remain absent. Exported volume proof remains **false**.

Restoring Mango mode off while still connected also completed the previously missing physical assisted-invalidation check: the phone cleared its assisted page and displayed **“Assisted browsing changed. Browse again; wake/unlock DX340 and check browsing access if needed.”** It retained the live controller session and subsequently read **Queue items1–24 of24** through native Mango capabilities. No Queue selection was attempted; those rows remained correctly disabled where the source lacks selection capability.

Next: improve post-play assisted recovery and Companion/Setup approval visibility under UX-01; original logos/platform icons remain pending. Continue the full Android/source/reliability matrix and owner-Mac/iPhone signing/physical tests, then Watch only after the phone-stability gate. Both earlier background observations remain **INCOMPLETE**, including1779.414seconds; this USB-powered screen-on session does not replace them.

Private raw evidence: ignored `artifacts/mango-phone-selection-20261031/` (three one-tap receipts, source snapshots/owner-confirmation captures, UI bounds, failed/successful tests, build/signing logs, verified CI archives, cleanup-summary.json). Addresses, serials, private preferences, APKs and signing material stay outside Git. Adjacent JSON is sanitized.
