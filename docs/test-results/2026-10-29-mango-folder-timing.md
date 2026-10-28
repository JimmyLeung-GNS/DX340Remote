# Mango folder settling and source-local timing

29 October 2026. **VERIFIED for the source-local navigation below. Playlist recovery still misses the proposed three-second page target; phone latency is NOT TESTED.**

Device d: DX340, Android13/API33, build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240; Apple Music6.5.3/code1599, freshly checked. Both sessions stayed paused; volume remained35. Wi-Fi0/default network:none. No playback, playlist editing, permission change, source-file access or new listening test was used.

## Reproduction and correction

A fresh recovery from the observed empty Playlist screen took4,612ms including ADB dispatch/report retrieval. A diagnostic build then measured4,369ms inside the debug receiver /4,621ms over ADB. Its bounded trace showed changing menu layouts, source-tree reads up to450ms, and two serial settling waits on the same final folder fingerprint. The diagnostic build was an intermediate measurement, not the final installed package.

The final folder previously waited for300ms of navigation stability, then started a separate450ms settling interval. It now uses one complete450ms interval for the final semantic folder snapshot in the same active window. Intermediate menus still wait300ms; window or fingerprint changes restart the interval, as does a backwards test clock. Authorization/readiness/deadline checks and live semantic action validation remain in place. The eight-second navigation deadline is unchanged.

UiSnapshotStability has two tests covering matching fingerprints in different windows, changed/revisited fingerprints and a backwards clock. The public [active-window root API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getRootInActiveWindow()) supplies the current window tree; timing alone never authorizes a node action. No prefetch flag, coordinate navigation or Android permission setting was added to the application.

Diagnostics retain at most64 in-memory steps per browse, including tree duration, node count, phase and stable age. DebugUiProbeReceiver adds monotonic elapsedMs. That timer includes receiver/source dispatch before the report is assembled; it is not an isolated tree/adapter benchmark. The ADB timer additionally includes broadcast/report retrieval. Neither measures BLE or phone rendering.

## Actual results on the final build

| Starting state | Samples | Debug-receiver duration | ADB round trip | Result |
|---|---:|---|---|---|
| Observed empty Playlist |3 main repeats|3,829 /3,875 /4,119ms|4,115 /4,175 /4,376ms|Internal Storage,9 rows each|
| Observed empty Playlist, reusable script |1|3,540ms|3,858ms|Internal Storage,9 rows|
| Internal Storage root |3 main repeats|534 /741 /713ms|798 /1,052 /999ms|Same root,9 rows each|
| Internal Storage root, reusable script |1|698ms|972ms|Same root,9 rows|
| Internal Storage / Music |1|2,164ms|2,452ms|Returned to storage root,9 rows|

Playlist recovery improves over the reproduced baseline but still exceeds three seconds. The result remains **FAILED against that proposed target**; no replacement target or complete-library/phone performance pass is claimed. Source animations/layout changes and tree retrieval still consume time. The observed empty Playlist screen does not verify populated playlists or arbitrary playlist selection.

Research-only starting-state setup used ADB taps derived from freshly inspected screenshots and checked semantic layouts before each tap. It never selected New Playlist or Local Network. Those coordinates live only in ignored scratch work. The tracked scripts/verify-mango-folder-timing.ps1 uses the existing semantic adapter and requires an already prepared starting screen; it sends no coordinate input. Both start checks were exercised:

```powershell
./scripts/verify-mango-folder-timing.ps1 -Serial d -Adb (Get-Command adb).Source
# Manually prepare Mango's Playlist screen, then:
./scripts/verify-mango-folder-timing.ps1 -Serial d -Adb (Get-Command adb).Source -ExpectedStart Playlist
```

The fresh four-case assisted-interruption regression passes: Apple screen-off921ms and Mango1,214ms return UI_SCREEN_UNLOCK_REQUIRED; in-process source changes636/686ms return AUTHORIZATION_DENIED. All four reject old tokens with STALE_LIBRARY and recover fresh Songs6 /Internal Storage9 rows. These are source-local checks, not controller revocation/radio tests. Separate notification/Accessibility revoke tests were not unnecessarily repeated for this settling change.

## Build, installation and artifacts

VERIFIED: scripts/build-android.ps1 completed native APKs/instrumentation compilation, lint and79 aggregate JVM tests. Preceding Flutter analysis32 tests/release/lint retain their scope; Flutter and Swift sources are unchanged this turn. Current Swift compilation still needs a Mac.

Original-key preflight matched the installed certificate; install-r succeeded, all four companion preference files stayed byte-identical and both Accessibility services rebound with no binding/crashed entries. Pulled installed bytes exactly match the final companion:

`0898571b827f59335e2711eb23a10ea33e171a3c148c7770e6ca0451bbc3f828`

Shared phone APK remains09aaae7836a98a77c3ce52c870873dc39191dde846e286947d18892875a963a2. No uninstall/data reset/default-key/global-environment change occurred. Final target Mango; both source sessions paused.

Evidence: artifacts/mango-navigation-timing/{baseline-summary,diagnostic1-summary,after-summary,root-nested-summary,installed-host,prefs-before,prefs-after,final-state}.json, diagnostic/final-build.txt, preflight/install/version/network/service captures and inspected baseline/menu/Playlist screenshots. Interruption reports are under artifacts/assisted-interruptions/ with copied output interruption-check.txt. Unique reusable-script runs are under artifacts/mango-folder-timing/. Raw summaries use probeElapsedMs to avoid implying pure adapter time.

Full20-file/format/SD/rescan/playlist and Apple selection matrices, actual phone BLE/security/background/range, iOS signing/build, volume/audio/battery/walking acceptance and Watch remain incomplete. See development-handoff.md and completion-audit.md; no full-stage completion follows from this improvement.
