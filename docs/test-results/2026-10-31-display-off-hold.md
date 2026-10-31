# Display-off background attempts and USB power wakes — 31 October 2026

**FAILED both30-minute holds; fresh post-failure reads VERIFIED. Full goal ACTIVE/incomplete.** [Versions, samples, receipts and raw evidence hashes](2026-10-31-display-off-hold.json).

Installed apps are unchanged: original-signer Pixel Remote `ce47b14caa3b78c6e482387a971bc3efc80fdc7539845a7cb01b5c55daa614f2`/source25ed62a,0.2.0/code2 on Pixel9a Android16/API36 CP1A.260405.005; Companion `82ad322f6f804c224603dde6668cb2f34c1a7de83b974edbfc4dec5245c721a5`/sourcefa60705,0.1.0/code1 on DX340 Android13/API33 eng.iBasso.20260812.181952. Mango4.1.37/code240 and Apple Music6.5.3/code1599 remain installed. This turn changes observation tooling, not either installed app.

## Actual display guard

The existing observer could require Asleep/Dozing but did not inspect the actual display. New optional `-ExpectedDxDisplayState OFF` saves `dumpsys display` at every sample and requires exactly one actual internal/default DisplayDeviceInfo record with state OFF. External panels, requested/logical settings, absent/ambiguous records and an ambient DOZE display do not satisfy the guard. Nine software parser cases pass. Default Any preserves the old observation scope; wakefulness remains independently recorded/required. Source88319ad records this tooling; the first run used the same guard bytes before their commit.

Both attempts use the actual Pixel saved-grant connection, Apple target with both sources paused, Android Settings foreground and a verified DX display-OFF preflight. Neither observer issues BLE/source/radio commands. Read-only USB process/service, source/output, power/display/activity and filtered log snapshots remain distinct from continuous radio or audio observation.

| Attempt | Observer interval | Samples | Terminal condition |
| --- | --- | --- | --- |
| First,22:40 HKT | 135.179seconds | 5 | Four OFF/Dozing samples, then ON/Awake; guard terminates |
| Fresh owner-approved repeat,22:46 HKT | 34.282seconds | 2 | OFF/Dozing then ON/Awake; guard terminates |

All samples retain both process IDs, both foreground services, background phone state, both sources paused2 and37/headset(4). These facts do not rescue the failed display condition. Neither attempt reaches30minutes, and neither is a T05/GateA/D pass. The terminated unified-exec handles2350 and19076 must not be restarted or treated as live waits. No third hold is started.

## Wake evidence and uncertainty

Contemporaneous DX system logs show ACTION_POWER_DISCONNECTED, followed by **WAKE_REASON_PLUGGED_IN / android.server.power:PLUGGED:false**, actual internal display ON and ACTION_POWER_CONNECTED. The wake times are22:42:16.692 and22:46:41.150 HKT; the observer detects ON later at its next sample. Do not equate sample detection time with the exact wake time.

Windows' enabled Microsoft-Windows-Kernel-PnP/Configuration log has no matching events22:39–22:49; the query result/error category is retained. The Pixel/DX ADB transport IDs remain3/4 at the observed endpoints. This does not establish continuous USB data availability or identify a physical cause. The Android power events identify the wake trigger; they do not prove a bad cable/port, Windows suspend behavior, battery-controller defect, owner action or Companion causation. No power setting, firmware, source setting, storage option or app data is changed to force a passing result.

## Recovery and cleanup

After each terminated hold, foregrounding the ordinary Pixel produces a fresh applied Apple GET_STATE,443ms and307ms respectively. These are decoded-reply timings after short failed attempts, not30-minute recovery, GATT ACK, p95 or hearing proof. Normal target-Mango restoration/report export/Disconnect/help collapse follows. No source playback or volume mutation is sent.

The same ce47 native model retains all earlier20 records, then25, then30; every earlier row remains unchanged. Each stage adds HELLO, AUTHORIZE, two SELECT_SOURCE and GET_STATE, all applied RESULTs with no errors/evictions. This journal and endpoint checks do not establish uninterrupted radio throughout either attempt.

Final checks preserve four own host preference hashes, global settings, own runtime grant/flag sets, exposed bond identities/types and both process IDs. Pixel/JIMMYAH grants and both optional browsing modes ON remain; host foreground/both Accessibility bindings remain. Both sources are paused2 at37/headset(4), volume proof false; phone normally disconnected/help collapsed/target Mango, both USB endpoints present and SD FBB9-E8FE mounted. Apple SD preference and the previous owner-audible A・RA・SHI result retain the preceding report's scope; there is no new hearing test or source-storage action here.

Next: prepare and validate a bounded device-local read-only recorder before an unplugged screen-off follow-up, avoiding USB-power transitions during the hold. Record its sampling/clock/process limits, then obtain fresh physical readiness and perform post-hold phone control/hearing separately. Do not reuse either ended hold's availability agreement. Current80 Flutter/199 JVM/19 Swift and the closed25ed shared iPhone archive retain their prior scopes; no app build is needed for this tooling change. Full source/playlist/format/output/strict-offline/commands/latency/background/range/calls/battery/walk/ownership/security, physical owner-iPhone/Mac signing and Watch acceptance remain open.
