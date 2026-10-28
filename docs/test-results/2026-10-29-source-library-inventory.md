# Source switching and offline Apple browsing inventory

29 October 2026. **VERIFIED source-local reads only. No new playback, audible selection or phone BLE acceptance.**

Device d: DX340 Android13/API33, build eng.iBasso.20260812.181952; Mango4.1.37/code240 and Apple Music6.5.3/code1599. Companion installed hash0898571b827f59335e2711eb23a10ea33e171a3c148c7770e6ca0451bbc3f828; shared phone09aaae78... unchanged. Wi-Fi0/default network:none. Both sessions were observed paused (playbackState2), volume35. No product code/APK/install, permissions, download/playlist management, music files or signing configuration changed in this continuation.

## Source switching

New scripts/verify-source-library-switch.ps1 uses tagged own-adapter probes and records failures rather than hiding them with retries. Each round requests Apple Downloaded root, Mango native root, Mango assisted storage choice and Internal Storage. The default starts with the other source foreground; -StartFromHome returns to Android Home before the two foreground requests. Apps are not manually launched between rounds. No coordinate, playback or volume command is used.

| Starting condition | Rounds | Reads | Failed reads | Apple root probe range | Mango storage probe range |
|---|---:|---:|---:|---|---|
| Alternating source windows |10|40|0|466–1,626ms|1,080–1,287ms|
| Android Home before foreground requests |10|40|0|417–670ms|1,051–1,453ms|

Apple returned Downloaded/four categories; Mango returned native root/six rows, assisted choice/two rows and Internal Storage/nine rows. These durations cover debug receiver/source dispatch; ADB round-trip timings are separately recorded. This is not command/phone/radio latency. The preceding occasional UI_FOREGROUND_REQUIRED /Apple UI_ACTION_FAILED failures did **not reproduce in these20 rounds**. They are not declared fixed or impossible. Cold-process startup, long sleep, secure lock and real controller recovery remain separate tests.

Commands executed:

```powershell
./scripts/verify-source-library-switch.ps1 -Serial d -Adb (Get-Command adb).Source -Rounds 10
./scripts/verify-source-library-switch.ps1 -Serial d -Adb (Get-Command adb).Source -Rounds 10 -StartFromHome
```

Raw runs: artifacts/source-library-switch/20261028-231212-1c17c275 and20261028-231323-81c7da66, with samples.json/summary.json and each tagged raw report. Copies: source-library-switch-before.txt/source-library-home-before.txt. Those output names mean pre-existing production code was tested; no application fix/build followed them.

## Real Apple browsing and pending listening matrix

The actual optional Apple adapter navigated Downloaded → Albums, opened five distinct album labels, scrolled their track pages, returned to root and navigated two existing playlists. The reusable inventory script sampled66 playable downloaded album-row labels and20 playlist-row labels, with80 distinct title/artist labels across the sampled pages. These are sampled accessible labels, not complete collection sizes or stable global track identities. No play, shuffle, download, share, edit or New Playlist action was used.

An initial scratch assertion stopped because the Favourite Songs header includes two trailing nonbreaking spaces. Inspection showed the correct playlist and downloaded rows. The harness now trims only the expected display-header comparison; application action IDs, fingerprints and revision validation remain unchanged. This was a harness mismatch, not a source-navigation failure.

```powershell
./scripts/verify-apple-browse-inventory.ps1 -Serial d -Adb (Get-Command adb).Source
./scripts/prepare-apple-listening-plan.ps1 -Serial d -Adb (Get-Command adb).Source
```

The first script repeats the five-album/two-playlist metadata inventory with bounded scrolling and fresh opaque tokens. It saves only source metadata under ignored artifacts/apple-browse-inventory/<run>/. The second reads the complete current101-item public queue with revisions, then prepares20 distinct observed title/artist candidates: three from each album, three from one playlist and two from the other. Seventeen candidate titles were absent from that full queue at preparation time. It sends no playback operation.

Generated artifacts/apple-listening-plan.json and apple-listening-plan-summary.json have20 entries, all NOT TESTED with no audible confirmation. Re-browse/revalidate current source, window, IDs, revisions, actual selected metadata and outside-queue status immediately before each eventual selection. Source state and human listening confirmation must be recorded separately; no global identity or playback success follows from these labels. The user's current listening availability question is pending; elapsed time is not a listening response.

Raw reusable inventory: artifacts/apple-browse-inventory/20261028-231920-d0fbfca6, including collections.json, summary.json and listening-queue-{0,50,100}.json. Earlier scratch inventory/snapshots and inspected album/playlist screenshots are in its parent directory. They remain excluded from the portable Git bundle. Copy the optional listening-plan JSON separately if needed; it contains metadata only, no audio/private downloads/signing keys.

## Final state and verification limits

All three new scripts require both known sessions observed paused and stop without sending playback controls otherwise. The strengthened observation guard was also applied to159 saved raw probe reports; all passed. Fresh listening-plan preparation passed after this guard. Raw audit: artifacts/source-library-paused-audit.json. Final source state confirms both paused, target restored to Mango; both optional services bound with no binding/crashed entries. The inspected final Mango capture shows volume35 and paused playback. Evidence: source-library-final-state.json, source-library-final-services.txt and source-library-final.png.

Product APKs are unchanged. The preceding native build/lint79 tests and Flutter analysis32 tests/release/lint retain their scope; no redundant rebuild was run for development scripts. Swift still needs a Mac build. Original20-selection/format/SD/rescan/output/listening matrices, source Playlist recovery above the proposed3second target, physical Android/iPhone BLE/security/background/range/install/battery/walk and later Watch acceptance remain incomplete. This report does not pass T07/T08 or any full phone stage.
