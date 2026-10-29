# Native Mango album inventory and All Music discrepancy

29 October2026, DESKTOP-T9ICUE4 / branch `codex/source-recovery-checkpoint`, after local checkpointc4ab197. **VERIFIED metadata only; native selected-album playback and full T08 remain NOT TESTED.**

Device: DX340 `d`, Android13/API33, build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. Mango4.1.37/code240, Apple Music6.5.3/code1599. Installed companion04846d60bc4e65065028d510286b56cba91577840a994d351146c55dff418fa2 and ordinary shared phone23c536e7890ddc58a84552fcd3bd4be4eac578722daa8553c3a94d7bf932328d remain unchanged. Earlier20 Mango listening confirmations belong to this companion's assisted-folder path; they do not establish native Album playback or phone BLE.

## Actual source-local metadata

New read-only `scripts/verify-mango-native-inventory.ps1` walks bounded native Album and All Music pages through the installed companion. It requires Wi-Fi0/no default network, both intended sessions paused, expected volume37, one source revision, correct parent/page/continuation, unique row IDs within each parent and matching source-reported album counts. It sends BROWSE/GET_STATE only, never changes permissions or playback, and stops at the existing400-request diagnostic bound. An over-bound walk cannot be called complete.

The final run passes63 source-local read requests: **22 albums /635 album entries**, each album's returned count equals Mango's subtitle count. Three albums span more than two25-row pages (76,70 and56 entries). Native root categories are Now Playing, All Music, Album, Artist and Genre; no native folder or playlist category is exposed by that root. Artist/Genre descendants were not exhaustively walked. These are source metadata entries, not635 proven unique files or audible tracks.

**Observed discrepancy:** All Music returns500 entries in20 full25-row pages, followed by an empty page20. Do not equate this category with the complete observed album collection. No135-file identity/missing-file conclusion follows from different category totals. The existing native Album path reaches all635 returned album entries; source-local assisted folders remain separately available. No fallback player or invented source IDs were introduced.

Final run: `artifacts/mango-native-inventory/20261029-154851-f6a83dab/` (albums.json, all-music.json, summary.json and every raw tagged probe). Command:

```powershell
./scripts/verify-mango-native-inventory.ps1 -Serial d -Adb (Get-Command adb).Source
```

## Direct public-source discrimination

New Android test-only `MangoNativeCountsProbe`/runner mode `mango-native-counts` connects a platform MediaBrowser directly using the legitimate installed companion identity. It bypasses SourceRegistry, its page slicing and its cache. Browser construction/subscription/unsubscription/disconnection stay on the main thread, with bounded connection/child waits and10000-row maximum. No MediaController playback operation is present.

The actual raw callbacks report:

| Parent / request | Raw response |
|---|---|
| Native root, no paging options |5 categories |
| All Music, no paging options |500 entries, IDs1–500 |
| All Music, page0/size25 |25 entries, IDs1–25 |
| All Music, page19/size25 |25 entries, IDs476–500 |
| All Music, page20/size25 |0 entries |
| All Music, page0/size1000 |500 entries, IDs1–500 |
| Album, no paging options |22 album rows, reported aggregate635 |
| Largest album, no paging options |76 entries, matching source report76 |

This establishes that the observed500-entry All Music response is already present at Mango's public browser boundary, rather than caused by our SourceRegistry pagination/cache. It does not prove why Mango exposes different category coverage or establish a universal limit for every database/version. The probe uses documented [MediaBrowser subscription/paging options](https://developer.android.com/reference/android/media/browse/MediaBrowser) and [MediaBrowserService child callbacks](https://developer.android.com/reference/android/service/media/MediaBrowserService), without assuming an API alone proves device behavior.

Build command: process-local JBR21.0.7/SDK/original key, `android/gradlew.bat -p android :host-app:assembleDebugAndroidTest` **PASSED**. Test APK83bf632ea7e6dfd4dfb3e58b5abef22f39ce10c6313c2f364726897cb25cb51e was temporarily installed after matching its installed-test certificate. An initial preflight mistakenly used the host package name and correctly refused the test APK before installation; corrected `.host.test` preflight passed. `adb -s d shell am instrument -w -r -e mode mango-native-counts org.dx340.remote.host.test/org.dx340.remote.host.SourceProbeInstrumentation` completed with INSTRUMENTATION_CODE -1 and report completed=true. Raw JSON/build/signing/install/result logs are in `artifacts/resumed-096601a/` as mango-native-counts.json and native-counts-*.

This adds physical diagnostic evidence, not another JVM/Flutter unit-test count. Production app/protocol code is unchanged, so preceding native120/Flutter46 build evidence retains its scope. No new candidate production package was installed.

## Retained harness failures and restoration

The first inventory run (`20261029-154123-97fc6481`) failed an assumed Album/All Music equality. Inspection also found its Measure-Object aggregation over in-memory ordered dictionaries produced null rather than a sum. Saved album rows independently total635. The next run (`20261029-154657-57539214`) properly recorded the category discrepancy but still had the null aggregate. Both reports remain as failed/insufficient harness evidence. Explicit numeric accumulation fixes the summary; the final fresh63-request run reports635 and keeps albumAllMusicCountsEqual=false. No source data or failed run was overwritten to make it pass.

The original test APKee2f570c2065d6b54870360b34c27ee8d61495cd029ea11c6ddadacd1418a335 was backed up, restored with install-r and freshly pulled/hash-matched. Instrumentation restarts the target host process: the first immediate own-UID foreground-service restart was rejected as background. Waiting for MainActivity with `am start -W` and retrying the service start succeeded; final dumpsys shows HostService isForeground=true. Both own Accessibility services were enabled but marked crashed after instrumentation; a targeted rebind preserved unrelated components and the original complete service list. Final both enabled/bound/no crashes and canBrowseFolders=true. These restoration failures are not product acceptance passes.

Final evidence under `artifacts/native-album-postflight/`: both players paused2 at37, volume proof false, Wi-Fi0/default network none, HiBy stopped=true. Four raw preference hashes match immediately before/after instrumentation; final normalized contents also match the preceding resumption. Both installed production APK hashes remain unchanged. Test-restoration.json verifies old test bytes exactly. The new production candidates5e1d6e0c.../2d42728f... remain uninstalled.

## Remaining acceptance

Native Album selection still requires matching actual source state and a human audible confirmation, then full both-phone T08 coverage. This read-only inventory does not pass playlists, SD/rescan, codec/file uniqueness, audio quality, phone BLE or latency acceptance. Do not remove the earlier20 assisted-folder confirmations or reclassify them as native Album results.

Fresh radio check `artifacts/windows-ble/resume-c4ab-bond-audit.json` did not discover exactly one service candidate in5seconds, before GATT/frames. Android's fresh bond dump lists JIMMYAH only. No new native pairing answer or local controller approval was received; the manual pairing request remains pending. Both-phone/two-controller/Watch gates are unchanged. Full objective remains incomplete and active.
