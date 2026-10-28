# Notification access revoked during assisted browsing

29 October2026, Asia/Shanghai. **VERIFIED for actual source-local permission loss/recovery. Controller approval/revocation over BLE is not covered.**

DX340 ADB d, Android13/API33, build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240; Apple6.5.3/code1599. Installed companion application source remains checkpoint2824a6b; this continuation adds a repeatable test and evidence, with no application-source change, rebuild or installation.

## Actual checks

```powershell
./scripts/verify-pending-session-access.ps1 -Serial d
```

Requires the running matching debug companion, both optional browsing services enabled/bound, both music apps open with paused sessions, and the companion's SessionListener initially granted notification access. If those conditions are absent the script stops rather than granting new permissions or starting music. It wakes the DX340/dismisses its existing nonsecure keyguard; it does not change lock settings or bypass credentials.

The script records the complete original listener set and host PID. It prepares a fresh Apple Songs or Mango Internal Storage navigation token, starts the real own-adapter browse and records STARTED before revoking only org.dx340.remote.host/org.dx340.remote.host.SessionListener after250ms. It requires the pending request to stop, the source epoch to change, all can* capabilities to be false and metadata/returned items to be cleared. A separate new browse while access is absent must return PERMISSION_REVOKED. No UI mode/Accessibility grant is revoked and no playback/volume/file/playlist command is sent.

The own listener is restored in an inner finally and again in the outer safety finally. The script polls for the exact original component set instead of assuming the settings update is synchronous. It never writes the complete settings value or grants/removes another component. Source observation must recover automatically without a host restart or explicit refresh call. Old canceled navigation tokens reject; a new root/page and ordinary native queue read must recover. Final target is Mango, both sources paused and original listener set restored.

| Source | Pending result | Measured probe time | New request while revoked | Old token after regrant | Fresh assisted page | Native queue read |
|---|---|---|---|---|---|---|
| Apple | AUTHORIZATION_DENIED |1087ms|PERMISSION_REVOKED|STALE_LIBRARY|Songs,6 rows|50 rows in first window|
| Mango | AUTHORIZATION_DENIED |675ms|PERMISSION_REVOKED|STALE_LIBRARY|Internal Storage,9 rows|28 rows|

VERIFIED in both cases: source metadata cleared, all action capabilities false, source epoch invalidated, no library rows returned while revoked, exact original listener component set restored, fresh source observation/browsing/queue access recovered, host PID unchanged. AUTHORIZATION_DENIED is the running adapter's boolean scope guard after source access was lost; it does not prove a revoked-controller Bluetooth path. Timings include the ADB trigger/settings/report round trip and are not BLE latency measurements. The script verifies paused sessions before/after; separate privileged media_session snapshots while revoked were inspected and both target sources reported PlaybackState2(paused).

The first exploratory Apple run also stopped correctly, in617ms, but its harness compared settings immediately after allow_listener returned and reported restoration failure before Android's setting update completed. A subsequent fresh read confirmed the original grants restored and source access recovered. The final script includes bounded setting/source-recovery polling and both complete cases passed. This preliminary harness failure is not counted as a permission-restoration defect in the app.

Evidence under ignored artifacts/:

- pending-session-access/summary.json, listeners-before.json and listeners-after.json;
- per-source before-revocation/interrupted/new-request-denied/restored-state/old-token/fresh-page/native-queue-recovered reports;
- per-source media-session-revoked.txt and independently checked revoked-playback-check.json;
- 99-final-state.json, accessibility-after.txt, wifi.txt and connectivity.txt;
- exploratory pending-access-probe/ reports, retained with the stated harness limitation.

Both optional Accessibility services remained bound with no binding/crashed services. Wi-Fi0/default network:none. No playback or volume change was sent; no companion/source app was stopped or reinstalled. Other notification listeners, controller preferences, optional source modes and the default signing key were untouched.

## Build and remaining scope

Application source and installed APKs are unchanged from the preceding interruption checkpoint: companion SHA2566a15d94df8e9ca66ed3f8828bab176e8fe32e9aa4b30f3ace8e565292426443d and shared phone SHA256897f32fdb04cdfbe7c9033b754aa02aeed7dc5af21ff66e8ba39d9299e12eaa1. Previous native build/lint74 JVM tests and Flutter analysis28 tests/release/lint retain their original scope; they were not rerun for this test-only continuation.

NOT TESTED: loss of the separate Accessibility grant, actual controller authorization revocation/radio disconnection during pending work, long sleep/secure lock, complete source selection/format/SD/playlist/audio/reliability matrices and current iPhone build/signing/BLE. These checks close the source-session permission-loss part of T12 locally; they do not complete T12 or T15 on either phone. Goal active and incomplete.
