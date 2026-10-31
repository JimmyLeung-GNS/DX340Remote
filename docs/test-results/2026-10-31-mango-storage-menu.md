# Mango storage menu correction and current audio checks —31 October2026

**Software regression and mounted navigation VERIFIED; original no-card timeout NOT RETESTED. Two Mango selections owner-audible; Apple selection FAILED.** [Versions, receipts, preservation checks and evidence hashes](2026-10-31-mango-storage-menu.json).

Companion source `8f970acba210ce84fffce7f51160fd2e448f2d85`, installed APK `66b21e1c86b1ba8d36453091825c6ff09b67a5c8b44136146fe3856496caf456`; unchanged Pixel APK `d75540a514bbc484cc1f84462c50b1f9f62b7bd4b15ce4c8ae019c58655acc02`/source80d4edc. Original signing certificate matches before in-place upgrade; pulled installed host bytes match. DX340 Android13/API33, Mango4.1.37/code240, Apple Music6.5.3/code1599; Pixel9a Android16/API36, shared phone0.2.0/code2.

## Parser correction and limits

The retained unmounted baseline screenshot shows Mango's drawer offering Internal Storage without an SD Card option. The parser previously required both option IDs to recognize any open drawer. It now recognizes either marker, preventing cached folder rows behind a partial drawer from being treated as the active page. Selection separately requires exactly one requested option, enabled and clickable, with exactly one expected descendant label. Missing, ambiguous, disabled, nonclickable, mislabeled and unknown/network options are rejected. No ancestor fallback is used for a storage option. Existing window, version, authorization, deadline and SD-availability guards remain.

Five new JUnit cases cover the real redacted mounted drawer and explicitly synthetic partial/invalid variants. Two cases fail against the old parser; all five pass after the change. The no-SD variant is a subtree removal matching the baseline screenshot, **not a captured no-card tree**. The original debug export omitted enabled flags; tests explicitly exercise disabled variants. Native host/reference/probe assembly and lint pass with193 JVM tests:55 protocol,3 controller,7 cache,128 source. Phone/Dart/Swift code is unchanged; no new hosted Apple job was started.

The earlier cached-SD/Internal timeout trace does retain a final73-node raw snapshot. It has **neither** storage option marker. Its repeated `menu=false` observations and unchanged SD page do not prove the partial-drawer correction resolves that entire physical failure. Keep that failure open. No new unmount/remount or physical card removal occurred here, following the separately recorded Apple storage-event crash.

## Physical navigation and listening

After normal upgrade/local Enable phone control, the Pixel reconnects using its saved approval. Actual phone BLE Internal → SD → Internal browsing succeeds with terminal4766/4023/3986ms. These exceed the proposed3s page target and do not establish p95 latency. Card remains mounted throughout.

After fresh owner listening readiness, the phone browses Apple Downloaded → Albums → AAA 2nd Anniversary Live - 5th Attack 070922 - Nippon Budokan, then selects **Kuchibirukara Romantica** once. A separate complete public MediaSession queue observation confirms it is absent from the previous101 items. The phone PLAY_ITEM returns dispatched439ms, then GET_STATE returns Apple's unchanged NONE0/A・RA・SHI metadata. The actual Apple UI shows **You're Offline — Error code:3**. This is a **failed source playback test despite a successful protocol result**. The dialog is saved and dismissed locally once. No retry, network change, download extraction, source-app reset/update or vendor contact occurs. Download availability and the relationship to the prior storage crash remain unresolved.

The phone next selects Mango **03. 風のたより.wav**. Public source state3/expected metadata/headset output37 and explicit owner hearing confirm that track. The old page then expires after selection as designed. A test helper's attempted Back to parent stops because that action is disabled; no tap is issued. The operator follows the visible fresh Browse source library guidance and navigates Internal/Music/Tayori to the different **magic** folder while WAV playback remains active. Selecting **03. ゴースト.flac** produces state3/expected metadata and a second explicit owner audible confirmation. Playback requests are single sends,4537/4446ms terminal; neither is an acoustic timing or bit-perfect proof. Mango is then paused through the phone, observed506ms.

## Receipts and final state

The same phone model exports111 →146 records: all111 earlier request rows unchanged, zero evictions,35 new applied RESULTs. Operations: HELLO1, AUTHORIZE1, BROWSE24, SELECT_SOURCE2, PLAY_ITEM3, GET_STATE3, SET_PLAYING1. No volume command. All three playback receipts say dispatched; the Apple failure is preserved separately from the two audible Mango successes. Terminal-result success alone is not a playback claim.

Four saved Companion preference hashes, both exposed bond identity sets and own permission grant/flag rows, settings, approvals and optional browsing modes remain unchanged. Host foreground service/both Accessibility bindings are healthy. Phone normal Disconnect clears foreground/started work; help collapsed and targetMango. Final MangoPAUSED2/AppleNONE0, volume37/headset(4), proof false; both USB devices present and original SD volume mounted. AppleNONE0 is not a paused-playback result.

Full goal remains ACTIVE/incomplete. Continue the original no-card regression, Apple crash/offline compatibility, complete source/playlist/format/output/reliability acceptance and physical owner iPhone/Mac signing/Watch gates. Fresh30-minute readiness remains unanswered; no long hold was started. Earlier audible results retain their original build scopes. No pause/device-switch request.
