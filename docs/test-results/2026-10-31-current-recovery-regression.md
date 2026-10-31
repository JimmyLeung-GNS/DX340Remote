# Current-build Android interruption checks

**VERIFIED for three scoped physical interruption cases**,31 October2026. Full goal ACTIVE/incomplete. This does not pass full T12, range, screen-off or acoustic acceptance.

Code source `53229582b5d4ad04a1c9ad739b38575c2f4aa9ed`; Pixel9a Android16/API36 CP1A.260405.005/shared0.2.0 code2 APK `6b7e379da44edc9fdaad1d80f19599ca346f848d3f3126639ea541f906f059a4`; DX340 Android13/API33 eng.iBasso.20260812.181952/Companion0.1.0 code1 APK `491dbbffabda8f7a20d64a2f591caa486427e036ba9e3821d831ccf6eff5934a`. Both installed APKs were re-pulled afterward and byte hashes match. Original signing identity retained. Mango4.1.37/code240 and Apple Music6.5.3/code1599 remain the playback apps. Earlier1a permission and04 radio reports retain their original package scopes; this report supplies actual current6b regressions.

## Nearby Devices denial and recovery

With an authorized saved-player session, Android App info → Permissions → Nearby devices → Don't allow denied both BLUETOOTH_SCAN and BLUETOOTH_CONNECT. Notification permission stayed granted. ActivityManager records killing the controller at09:02:50.701 for **permissions revoked**, not an app crash; its foreground service was absent. Reopening ordinary Remote showed Allow Bluetooth access/Open app settings. Cached Player metadata was explicitly stale; Play/Pause/Previous/Next were disabled, and reopening did not start foreground work.

Remote's Open app settings route reached its actual App info. Permissions → Nearby devices → Allow restored the original grant values **and exact runtime flags**, leaving notifications unchanged. Three observed Back actions returned from the Settings child stack; no data/task clearing was used. One deliberate Connect reauthorized with the saved grant, without pairing/enrollment/approval. The protected state-request UI remained authorized beyond its full reply budget without an observed timeout, and normal Disconnect cleared foreground/started state.

## Bluetooth-off while Remote is in the background

After another saved-grant connection (native host encrypted subscription/approval observed), Pixel Settings was explicitly foreground. Its actual Use Bluetooth switch was turned off. Bluetooth_on0 was observed; foreground/started state cleared while Settings remained top and the controller PID stayed unchanged. The driver restored Bluetooth to1 in finally. Foreground work stayed absent and ordinary Setup returned to remembered-player Connect, without automatic reconnect or stale Connected state. One deliberate Connect, protected state-request UI and normal Disconnect cleanup then passed.

This is a physical radio-toggle case, not range recovery. The public radio receiver and service lifecycle introduced in the04 predecessor remain effective on current6b after its new service-invalidation/reply-routing code.

## Companion process loss and controlled recovery

After an authorized phone connection, the driver read/verified the Companion PID and used run-as for that same debuggable application UID to send SIGKILL to its own process. It did not force-stop the package or clear app data. The phone left Connected for its bounded retry path. The first non-waiting host launch capture showed Launcher; a fresh am start -W confirmed Companion Main before the visible Enable phone control action was tapped.

Companion uses START_NOT_STICKY, so this case required **reopening Companion and enabling phone control locally**. It is not unattended host self-restart or handling-free bag recovery. The restarted host has a different PID and host epoch; the phone PID stayed unchanged. The phone then rediscovered/authorized automatically, without a phone-side Connect tap or another enrollment/approval. New-process native events record advertising at09:10:18.120 and approved-controller connection at09:10:39.323: **21.203seconds on the same host clock**. One preauthorization connection closed before the next encrypted subscription/approval; that intermediate attempt is retained, not described as a clean first attempt. This span does not pass the separate15-second range-recovery target or measure transport ACK latency.

A subsequent protected state-request UI check and normal Disconnect passed. As in the other two cases, the driver waited beyond the12-second reply budget (about24.5seconds including UI collection), observed Connected to player/enabled controls with no timeout/error, then verified foreground/started cleanup. No decoded terminal packet or p95 ACK/state latency was captured. No playback mutation was in flight; physical no-replay under an interrupted mutation remains NOT TESTED.

## Preservation and next work

All4 normalized host preferences, Pixel/JIMMYAH approvals, exposed bonded identity/type inventories, radio/font/theme settings and all three phone runtime grant/flag states match preflight. Both optional browsing modes remainON and both Accessibility services are actually bound after the host process restart. Final sources report paused2/output37/headset(4), volume proof disabled. Companion is enabled and Pixel normally disconnected with foreground/started state removed. No playback/volume command was sent; no new audio result. No observer, emulator or build remains running.

No product code changed. Existing168 JVM/67 Flutter/17 Swift and Android/Apple build results retain their source53229582 scope; unrelated checks were not repeated for these physical-only cases. Raw logs, private package pulls, UI trees and diagnostics are under `artifacts/current-recovery-regression-20261031/`; unique current-permission/current-radio/current-process/process-loss files are also under `artifacts/connection-flow-20261031/`. The adjacent sanitized JSON binds all outcomes, binaries, restoration checks and raw hashes. Old evidence was not overwritten.

One independent clarity issue remains: Android's connection-timeout text still asks the user to open pairing even when an already approved player only needs Companion re-enabled. Correct that hint without changing approval or transport rules. Calls, in-flight operations, range, full T12, screen-off T05, unplugged battery/walking, physical iPhone/signing, full source/audio/format/SD/output/handoff and later Watch remain open.
