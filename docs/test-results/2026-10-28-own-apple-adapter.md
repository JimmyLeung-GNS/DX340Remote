# Own Apple Music UI adapter — physical source checks

28 October2026. DX340 Android13/API33, build eng.iBasso.20260812.181952. Apple Music6.5.3/code1599, companion0.1.0 debug. User previously accepted optional screen-on/unlocked mode. Actual companion AccessibilityService was enabled through Android Settings. These checks use its real public AccessibilityNodeInfo actions through SourceRegistry, with a debug-only receiver requiring privileged android.permission.DUMP. **No phone or BLE command path was involved.**

Example developer probe (after starting debug companion):

```powershell
& $adb -s d shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation BROWSE
& $adb -s d shell run-as org.dx340.remote.host cat files/apple-ui-live-test.json
```

Child BROWSE supplies --es parent with the returned item/back/scroll token and --es revision with its exact current revision; scroll also supplies --ei page. PLAY_ITEM supplies --es item/--es revision. Never replay a mutation automatically. The probe is a development route, absent from release; it does not prove Bluetooth authorization/security.

| Check | Result / evidence |
|---|---|
| Root→Downloaded→Albums→album | VERIFIED actual service returned albums then downloaded-badge song rows; artifacts/apple-ui-verify-albums.json and verify-tracks.json |
| Song selection | VERIFIED dispatched through own adapter and observed “Kuchibirukara Romantica” playing at position1266ms; artifacts/apple-ui-live-play.json and sessions-after-play.txt. No new human audio confirmation or fresh outside-queue proof |
| Forward scroll | VERIFIED returned12 visible song rows after layout settled; artifacts/apple-ui-verify-scroll.json |
| Stale prior revision | VERIFIED STALE_LIBRARY; no selection/navigation issued, artifacts/apple-ui-verify-stale.json |
| Back after scroll | VERIFIED returned Albums, artifacts/apple-ui-verify-back.json |
| Screen sleep | VERIFIED UI_SCREEN_UNLOCK_REQUIRED and canBrowse=false while source playing=true; artifacts/apple-ui-verify-screen-off.json. Woke/restored non-secure keyguard afterward; secure lock not tested |
| Downloaded Playlists→AAA | VERIFIED five sampled playlist names, then three downloaded-badge song rows; artifacts/apple-ui-verify-playlists-final.json and verify-playlist-tracks.json. No playlist selection audio acceptance |
| Artists | DISABLED pending verified semantic model |
| Complete T07 | NOT TESTED:20 downloaded selections/five albums/two playlists through both phone BLE clients remain required |

Initial attempts failed because the real service merged the Library tab differently from UIAutomator, activity transitions changed window snapshots, collapsed headers changed IDs, and scroll initially published a transitional page. Fixes prefer the observed merged tab, bounded root retry, known collapsed header and450ms stable semantic snapshot before returning a changed page. Playlist rows use observed direct clickable ViewGroup children with matching text/description under the verified list; a JVM fixture excludes the out-of-list New Playlist action. Earlier failed probe artifacts are retained.

Software verification: scripts/build-android.ps1 passed after these changes: host/remote/research APKs, instrumentation APK, app lint,11 protocol+3 cache+4 semantic-model tests=18 passing. These tests are synthetic fixtures. They do not establish radio, audio quality, screen-off transport or full source acceptance.

No volume/gain, lock configuration, download management, firmware, credentials, private app files or playback engine were changed. Apple Music playback remained independent of the companion's upgrades; observed playing state is not acoustic continuity proof.
