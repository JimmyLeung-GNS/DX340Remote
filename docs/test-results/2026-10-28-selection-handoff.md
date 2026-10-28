# Native and UI selected-item handoff

28 October2026; physical DX340 Android13/API33, eng.iBasso.20260812.181952; Mango4.1.37/code240, Apple Music6.5.3/code1599, companion0.1.0 debug. Source branch codex/source-handoff. The privileged debug receiver invoked the same actual SourceRegistry selection paths used by the BLE dispatcher. No phone/radio/approval path was tested.

| Test | Result | Evidence |
|---|---|---|
| Native Mango PLAY_ITEM while Apple playing | VERIFIED source-state observation | Browsed All Music and selected __ALL_MUSIC__5 (“777 …5” in Mango's browse label). Observed Mango title “777 (feat. Awich, CHICO CARLITO & Paledusk)” playing and Apple paused. artifacts/handoff-native-{root,items,play}.json. Display index suffix is not part of session metadata |
| Apple own-service downloaded PLAY_ITEM while Mango playing | VERIFIED source-state observation | Navigated Downloaded→Playlists→AAA, selected “One Night Animal - AAA DOME TOUR 15th ANNIVERSARY - thanx AAA lot (Live)”; observed exact title playing, Mango paused. artifacts/handoff-apple-{root,playlists,playlist-tracks,selected}.json |
| Candidate outside previous queue | VERIFIED for this observed selection | Previous GET_QUEUE returned20 items, below the50-item response cap, so it was complete. Candidate exact title absent. artifacts/handoff-apple-queue-before.json and outside-queue-precondition.json |
| Offline precondition | VERIFIED at test | Wi-Fi setting0 and dumpsys connectivity “Active default network: none”; no network setting changed. artifacts/handoff-apple-offline-{wifi,connectivity}.txt |
| Correct audible track through AMP15 | PENDING human confirmation for this own-service selection | User was asked to listen. Earlier audible Paradise Paradise research and Mango confirmation are separate results |
| Full acceptance | NOT TESTED | Still requires20 tracks/five albums/two playlists, both controller BLE paths, audible behavior and remaining matrix |

The Apple selection used semantic node actions from the companion's bound opt-in AccessibilityService. It did not use ADB coordinates/taps, queue skipping, private download extraction or another playback engine. ADB only invoked the privileged debug test entry point and read reports. Mango used its public MediaBrowser/playFromMediaId path and exact-version recorded capability. Native parent identity and UI snapshot revision were revalidated after handoff.

Source pause observations and matching metadata are evidence of adapter behavior, not acoustic silence/quality or end-to-end Bluetooth operation. The human confirmation, if obtained, must be recorded as user-reported audio and kept separate from simulated coordinator tests.
