# Mango breadth-first prefetch experiment

29 October2026. **VERIFIED negative performance result; experimental change REVERTED. The proposed three-second source-page target still FAILS. No new playback/audio or phone BLE acceptance.**

DX340 d: Android13/API33 build eng.iBasso.20260812.181952, Mango4.1.37/code240 and Apple6.5.3/code1599. Both sources were observed paused before/after the checks. Final volume35, Wi-Fi0/default network:none, both optional Accessibility services bound with no binding/crashed entries.

## Hypothesis and observed results

The current adapter walks its tree breadth first. Android offers [distinct hybrid and breadth-first descendant prefetch strategies](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST); it forbids combining those descendant strategies. A one-constant experimental build replaced hybrid with breadth-first while retaining uninterrupted prefetch, all tree/action bounds, live validation and300/450ms settling intervals. No guard, timeout, permission or source behavior was relaxed.

Fresh observed Playlist-to-Internal-Storage requests used the existing semantic adapter and new tokens/revisions. Every result had nine rows, and both sources stayed paused. Probe durations include debug-receiver/source dispatch; they are not isolated tree, BLE or phone timings.

| Strategy / sequence | Probe durations, ms | ADB round trips, ms |
|---|---|---|
| Existing hybrid40114ff8..., before |3622 /3787 /3922|3922 /4090 /4183|
| Breadth-first experiment61644d1c... |3812 /4098 /4342|4071 /4349 /4593|
| Existing hybrid40114ff8..., restored |3824 /3710 /3665|4086 /3988 /3954|

The six hybrid samples average3755ms; three breadth-first samples average4084ms, about329ms/8.8% slower. These are small sequential samples, not randomized/p95 results. Breadth-first tree-read totals2327–2480ms also fail to show the hoped-for improvement against initial hybrid2124–2265ms. The matching traversal hypothesis is not supported by these measurements, so the change is not retained. All nine samples exceed three seconds. Do not replace the target or infer actual-phone latency from these observations.

Setup reused the research-only observed menu/Playlist coordinates with current semantic layout checks; it never selected playback, New Playlist or Local Network. The tracked verify-mango-folder-timing.ps1 still measures the actual adapter without coordinate navigation. Unique raw tagged runs remain under artifacts/mango-folder-timing/; the research wrapper is ignored work/measure-mango-prefetch.ps1.

## Build and restoration

The experimental scripts/build-android.ps1 build/lint/instrumentation compilation passed in22seconds; native aggregate remains86 JVM tests. The comparison exercised the real source tree. The final product source is byte-for-byte unchanged in Git from054fa12, using hybrid prefetch. Its preceding native86/Flutter32 and19 diagnostic test scopes retain their limits; no new iOS/phone/audio result is inferred.

Original-key signing preflights and both install-r operations pass. The preceding40114ff8... APK was restored, and fresh Android sha256sum matches the actual artifact:

`40114ff8c14dc000909fcc4e3543d2f42265a3916bf7b0eb008fc469fde62b87`

All four real preferences stayed byte-identical across the experiment/restoration. Shared phone remains09aaae7836a98a77c3ce52c870873dc39191dde846e286947d18892875a963a2. Final source target Mango; both sources paused. APK checksums were refreshed after restoring the original package. Product code/settings/bonds/source files remain unchanged; only this evidence is saved in the new source checkpoint.

Evidence: ignored artifacts/mango-breadth-prefetch/{comparison,prefs-before,prefs-after,restored-verification,start-state,final-state}.json, copied labelled timing/tree snapshots, experimental/restoration APKs and signing/install/build/service/audio/network logs. Final restored recoveries are the observed product check; no duplicate full regression was run after restoring exactly the preceding tested source/APK.

Full completion is still unproven. See completion-audit.md and test-results/2026-10-29-continuation-resources.md for the current native-consent/listening/phone/Mac gates. This negative experiment does not complete P6/P8. Authenticated physical acceptance remains necessary.
