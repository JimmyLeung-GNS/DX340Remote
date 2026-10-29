# Mango prefetch comparison and retained change

29 October 2026. **VERIFIED source-local navigation improvement in this sample; FAILED against the proposed three-second uncached-page target. Phone/BLE latency and new audible selections are NOT TESTED.**

DX340 d: Android13/API33, build eng.iBasso.20260812.181952; Mango4.1.37/code240, Apple Music6.5.3/code1599. Both sources stayed paused, volume35, Wi-Fi0/default network:none. No source file, playlist, permission, approval or bond was changed.

## Controlled comparison

The current controller-management build reproduced slow recovery from Mango's observed empty Playlist screen. Each request used fresh native/assisted-home/storage tokens and returned Internal Storage with nine rows. Timing uses the existing debug receiver elapsedMs, including source dispatch; ADB includes broadcast/report retrieval. Neither timer measures isolated tree time, BLE delivery or phone rendering.

| APK / strategy | Playlist probe durations, ms | Observation |
|---|---|---|
| Existing e8a21bd4... before experiment |4200 /4196 /4100|All nine-row results;11 tree reads each|
| Root-only uninterrupted hybrid prefetch, experimental5081fb54... |4200 /3727 /3590|8–9 tree reads; not retained separately|
| Root and child uninterrupted hybrid prefetch, retained40114ff8... |3880 /3704 /3952|8–9 tree reads; all below six baseline samples|
| Existing e8a21bd4... restored after experiments |4504 /4518 /4255|All nine-row results;11 tree reads each|

The six baseline samples average4295.5ms; the three retained-strategy samples average3845.3ms, about450ms/10.5% lower in this small sequential comparison. This is not a p95 or a randomized performance result. Root-only and root-plus-child averages are similar; these samples do not establish that the latter is uniquely optimal. Total observed tree time did not consistently improve: baseline2089–2437ms versus retained2211–2285ms. Fewer observations/scheduled waits account for much of the request-time difference. Do not describe this as a general tree-throughput improvement.

All Playlist samples still exceed three seconds. Root refresh was747ms on the initial baseline and856/769ms on the retained build; no root-refresh improvement is claimed. Retained nested Music browsing returned nine rows in2195ms and a fresh home/storage request recovered nine root rows in2172ms. The Playlist list remains empty; this does not prove populated-playlist selection or the complete library matrix.

Research setup used ADB taps from freshly inspected root/menu screenshots, with semantic layout checks before each menu/Playlist tap. No playback, New Playlist or Local Network action was selected. Coordinates remain in ignored work/measure-mango-prefetch.ps1. The tracked scripts/verify-mango-folder-timing.ps1 measures an already prepared screen through the actual semantic adapter without coordinate input.

## Retained implementation and checks

Mango tree snapshots use Android33's public getRootInActiveWindow(strategy) and getChild(index,strategy) with hybrid descendant prefetch plus FLAG_PREFETCH_UNINTERRUPTIBLE. Android31/32 retain the preceding calls; that fallback is compiled, not physically tested here. Android documents the [active-root overload](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getRootInActiveWindow(int)) and [bounded prefetch behavior](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#getChild(int,%20int)), including that the uninterrupted flag prevents another node/action request interrupting a batch. This is not an atomic snapshot or a timing guarantee.

The800-node/depth/child bounds, package/version/awake/unlocked checks,450ms final-folder and300ms intermediate stability, eight-second deadline and live semantic action revalidation remain. Prefetch is used for reading the tree; perform still reacquires and refreshes the live target and rechecks authorization before dispatch.

VERIFIED: scripts/build-android.ps1 completed APK/instrumentation compilation, lint and86 aggregate JVM tests with zero failures. Actual four-case interruption regression passes: Apple/Mango screen-off return UI_SCREEN_UNLOCK_REQUIRED, source changes return AUTHORIZATION_DENIED, old tokens return STALE_LIBRARY, and fresh Songs6/Internal Storage9 pages recover. Both sources remain paused. Existing controller tests retain their preceding scope; authenticated controller revocation is still unverified.

## Installed package and continuity

The retained original-key APK was installed, the prior package restored for the comparison, then the retained APK reinstalled after the comparison. Signing preflights and every install-r pass. Final pulled installed bytes exactly match:

`40114ff8c14dc000909fcc4e3543d2f42265a3916bf7b0eb008fc469fde62b87`

Four real companion preference files remained byte-identical across the experiment/restoration/reinstall. Both optional Accessibility services remain bound with no binding/crashed entries. Shared phone stays09aaae7836a98a77c3ce52c870873dc39191dde846e286947d18892875a963a2 with preceding Flutter32 analysis/test/release/lint scope; no Flutter/Swift change. Current Swift needs a Mac build.

Evidence: ignored artifacts/mango-prefetch/ contains before/experiment/restored-baseline timing and tree traces, comparison.json, experimental/child build and signing/install logs, final-root*.json, nested-summary.json, final-interruptions.txt, final-installed-host.json/APK, prefs-before/after.json and final-state/service/audio/network captures. Reusable timing runs also retain uniquely tagged raw reports under artifacts/mango-folder-timing/. No key is stored in Git or these reports.

Next: actual phone page timing and full source/listening/security/reliability acceptance remain open. The latency target still fails; this scoped change does not complete P6/P8. Windows native pairing/listening availability and separate phones/Mac remain pending. Keep the goal active and the complete scope visible.
