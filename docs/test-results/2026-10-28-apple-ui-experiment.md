# Apple Music foreground UI experiment

28 October 2026. DX340 Android 13/API 33, build eng.iBasso.20260812.181952. Installed Apple Music 6.5.3/code 1599. AMP15 phone out, user reported.

**VERIFIED one foreground UI/offline/outside-prior-queue selection, with human audible confirmation. Not a companion AccessibilityService or BLE pass; full T07 remains open.**

1. Native MediaBrowser failed and Media3 getLibraryRoot returned -4. No identity spoofing or private download access.
2. Read-only ADB UI inspection exposed Library → Downloaded. The app stated “Only showing music on this device.” Albums and song labels had usable semantic/resource identifiers; song rows included “Downloaded Song” badges.
3. Target-UID public MediaSession queue inspection recorded 101 existing Apple queue items and confirmed candidate “Paradise Paradise” was absent. Current item was “This Can’t Be Us” by ONE OK ROCK.
4. Wi-Fi setting was 0 and dumpsys connectivity reported `Active default network: none` before selection. No network was enabled for the test.
5. Mango was paused through its specifically named session and observed paused before selecting the Apple song. No volume change.
6. Semantic UI selection chose “Paradise Paradise” by AAA under downloaded album “AAA 2nd Anniversary Live - 5th Attack 070922 - Nippon Budokan”. Apple session subsequently reported that title/artist playing, with a new 20-item queue; Mango remained paused.
7. The user answered **“Yes, Apple Music plays it audibly.”** No agent hearing or audio-quality measurement is claimed.
8. During screen sleep/lock transition, hierarchy inspection exposed only com.android.systemui (28 nodes), not the Apple library. Window policy reported a non-secure keyguard. This is not proof of a working unattended wake/unlock route. No lock settings were changed.
9. Apple was paused and observed paused after the experiment. Mango's paused session was no longer in the active-session monitor list; resuming through Mango's visible player restored its reported playing state. Automated source handoff/resumption remains separate implementation work.

**Decision:** after these findings and disclosure that screen-on/unlocked operation and app updates change reliability, the user explicitly chose “Allow an optional screen-on/unlocked Apple Music mode.” Implement the opt-in adapter; do not silently describe it as screen-off support. This accepts the optional workflow, not a declaration that full browsing or phone interoperability is complete.

Raw evidence under ignored artifacts/: apple-ui-window.xml, apple-ui-downloaded.xml, apple-ui-albums.xml, apple-ui-album-tracks.xml, apple-queue-before-ui.json, apple-offline-connectivity.txt, apple-ui-selected-session.txt, apple-ui-selection-result.png, apple-screen-off.xml, apple-screen-off-policy.txt, apple-paused-after-experiment.txt and after-apple-experiment-restored.txt.
