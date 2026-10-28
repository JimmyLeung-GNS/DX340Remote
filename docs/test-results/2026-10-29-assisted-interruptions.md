# Pending assisted-browse interruptions

29 October2026, Asia/Shanghai. **VERIFIED for the actual source-local cases below; phone BLE and controller revocation are not covered.**

DX340 ADB d, Android13/API33, build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240 and Apple6.5.3/code1599 remained installed and paused. No playback, volume, music-file, playlist-editing or permission change was sent. Wi-Fi0/default network:none. The test wakes/dismisses the existing nonsecure keyguard afterward; it does not change lock settings or bypass credentials. Secure-PIN lock behavior remains untested.

## Failure and correction

FAILED before correction: while Apple Downloaded → Songs was loading, input keyevent223 after250ms put the device into its screen-off/dozing state. The own adapter waited6.253seconds and returned UI_UNEXPECTED_SCREEN with no rows. The Apple waitChanged loop caught every exception, retried until its6second deadline and replaced the actual readiness error with a generic screen error. Evidence: artifacts/ui-interrupt-before/summary.json,02-apple-sleep-during-browse.json and02-power-asleep.txt.

The Apple adapter now rereads only specific transitional-window/context errors until its deadline. Terminal errors such as UI_SCREEN_UNLOCK_REQUIRED retain their exact actionable code and finish immediately. This does not replay the browse click or any playback action. Both assisted adapters now clear their previous action frame on failed/canceled browsing before invoking completion, so a canceled window's navigation token cannot be reused after waking or returning to that source. Busy state is released and fresh browsing can recover.

DebugUiProbeReceiver, already debug-only and DUMP-protected, adds optional alphanumeric reportTag (1–48 characters) for per-request private report files and STARTED/FINISHED phases. Tags cannot supply arbitrary paths. Untagged clients retain the existing apple-ui-live-test.json destination. The debug BROWSE probe can schedule a validated source change after100–1000ms only while its request is unfinished; the report records when/from/to. This exercises the real SourceRegistry/adapter predicate on this device, but its trigger is in-process rather than a BLE command. A completed browse suppresses the scheduled change. No product protocol or controller authorization behavior was changed by this diagnostic feature.

## Repeatable actual-device script

```powershell
./scripts/verify-assisted-interruptions.ps1 -Serial d
```

Requires the running matching debug companion, both enabled/bound optional browsing services and both music apps paused. It prepares fresh Apple Songs and Mango Internal Storage navigation tokens. A screen-off test starts a background ADB broadcast, captures STARTED from that unique report before injecting keyevent223, then checks the finished report and observed device power state. Source-change tests schedule the real host source selector after200ms and require a recorded change while unfinished. Every canceled navigation token must reject as STALE_LIBRARY; a fresh root/page must recover without playback. Finally it wakes the DX340 and restores the control target to Mango.

The initial script assertions expected mWakefulness=Asleep and then immediate HAL noninteractive state. This firmware reports Dozing while its display/HAL transition is still pending. Those harness assertions were corrected; they were not adapter failures. The passing script records Asleep/Dozing power state and the adapter's exact wake/unlock result. It does not claim stable deep sleep or long-pause acceptance. Timings below include the shell trigger/report round trip and are not BLE ACK/source latency measurements.

| Source | Pending interruption | Observed error | Probe elapsed | Old token | Fresh recovery |
|---|---|---|---|---|---|
| Apple | Physical screen-off after STARTED | UI_SCREEN_UNLOCK_REQUIRED |815ms|STALE_LIBRARY|Songs,6 rows|
| Apple | In-process source change to Mango | AUTHORIZATION_DENIED |539ms|STALE_LIBRARY|Songs,6 rows|
| Mango | Physical screen-off after STARTED | UI_SCREEN_UNLOCK_REQUIRED |1272ms|STALE_LIBRARY|Internal Storage,9 rows|
| Mango | In-process source change to Apple | AUTHORIZATION_DENIED |598ms|STALE_LIBRARY|Internal Storage,9 rows|

VERIFIED: all four cases passed with both sources paused throughout. AUTHORIZATION_DENIED here is the adapter's boolean request-scope guard after source selection changed; it is **not a tested revoked-controller BLE authorization result**. The stale-token checks are canceled browse/navigation tokens, not new audible PLAY_ITEM tests. Evidence: artifacts/assisted-interruptions/summary.json, per-case before-sleep/power/interrupted/old-token/fresh-page reports and99-final-state.json. No synthetic music source participated.

## Build, installation and continuity

VERIFIED: scripts/build-android.ps1 passed native APKs, instrumentation compilation, lint and all74 JVM tests. The four additional meaningful interruption checks above are actual-device script checks, not extra JVM fixtures. Log: artifacts/assisted-interruptions-final-android-build.txt. Original external key was selected through process-local DX340_DEBUG_KEYSTORE; signing preflight matched installed certificate24f48aa3..., and adb install -r succeeded. Four companion preference files stayed byte-identical; both optional services are bound with no binding/crashed entries. Installed APKs were pulled and matched generated bytes:

| Artifact | SHA256 |
|---|---|
| Current companion0.1.0 debug |6a15d94df8e9ca66ed3f8828bab176e8fe32e9aa4b30f3ace8e565292426443d|
| Unchanged shared phone0.2.0+2 development release |897f32fdb04cdfbe7c9033b754aa02aeed7dc5af21ff66e8ba39d9299e12eaa1|

Shared phone/native controller/Swift source did not change this turn. The preceding Flutter analysis/28 tests/release/lint results retain their scope; a current iOS build still needs Mac/Xcode. Upgrade/hash/preferences/services/network evidence uses the assisted-interruptions prefix under artifacts/. Default signing key/global settings were not changed.

NOT TESTED: pending Accessibility permission revocation, actual controller approval/revocation over BLE, radio disconnect with queued replies, secure lock/long sleep, real two-phone source changes and fresh iOS compilation. Existing full source/format/playlist/SD/volume/audio/reliability/Watch gates remain open. Goal active and incomplete.
