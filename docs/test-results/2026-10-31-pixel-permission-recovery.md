# Pixel Nearby Devices loss and recovery

**VERIFIED for this single physical Android permission-loss case**,31 October2026. Full T12 and the complete project remain incomplete. Device/package hashes and permission flags are in the adjacent sanitized JSON.

Pixel9a Android16/API36 CP1A.260405.005, shared0.2.0/code2 **1a592c56…**, connected to DX340 Android13/API33 eng.iBasso.20260812.181952, Companion0.1.0/code1 **491dbbff…** using its retained approved identity. Both music sources started paused at output37/headset(4), verification disabled. No playback or volume action was sent during this test.

1. Fresh ordinary Setup/authorized UI and connected foreground service were observed. In the actual Android app-info permission UI, Nearby Devices was temporarily changed from Allow to Don't allow. Both BLUETOOTH_SCAN and BLUETOOTH_CONNECT became denied; notification permission stayed granted.
2. ActivityManager explicitly records killing the controller process for `permissions revoked` at07:12:25.486. No foreground service remained after denial. This is an observed OS permission-loss termination, not an inferred app crash or suspension.
3. Reopening the ordinary app showed Setup **Allow Bluetooth access** and **Open app settings**. Player retained explicitly stale cached metadata; Play/Pause/Previous/Next were disabled. No connection foreground state was started merely by reopening the denied app.
4. The app's Open app settings route reached its real Android App info. Permissions → Nearby devices → Allow restored both grants. Their exact runtime flags match baseline; notification grant/flags were unchanged. The Settings child stack was still on top after a launcher intent, so the harness returned using observed Back navigation rather than clearing app data/tasks.
5. One Connect to DX340 reached protected saved-grant authorization without another enrollment/pairing/approval window. Open Player/Refresh player completed the read route; normal Disconnect returned to remembered-player Connect and removed foreground state (`startRequested=false` while UI-bound).

All four normalized host preferences match preflight, including both controller grants, optional modes and source verification. Both OS bonded-device inventories match, both sources remain paused2 at37/headset(4), and radio/font/theme settings are unchanged. Both Nearby grants/flags are restored, as is the original notification permission. Existing Pixel/JIMMYAH approval and bonds remain intact. No new source/audio, controller-revocation or physical iPhone result follows.

Raw evidence is under `artifacts/pixel-permission-loss-20261031/` and uniquely named `artifacts/connection-flow-20261031/permission-*` files. The adjacent JSON preserves package/device identity, grant/flag transitions, preservation assertions and raw package/service/event hashes. No code/APK change was needed, and unrelated software tests were not repeated for this UI-driven physical check.

This does not pass radio toggles, host restart, phone call, range/sleep/walking/battery, unapproved/revoked-controller tests or the separate physical iPhone cases. The controlled30-minute repeat still needs its fresh owner-availability answer; no observer is running.
