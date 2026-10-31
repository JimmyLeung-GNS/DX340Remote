# Controlled Android background observation

**VERIFIED for this scoped USB-powered, awake, paused background observation**,31 October2026. Full goal ACTIVE/incomplete; this is not the complete screen-off T05 or continuous-radio acceptance.

Pixel9a Android16/API36 CP1A.260405.005/shared0.2.0 code2, APK `6b7e379da44edc9fdaad1d80f19599ca346f848d3f3126639ea541f906f059a4`, original signer. DX340 Android13/API33 eng.iBasso.20260812.181952/Companion0.1.0 code1 remains `491dbbffabda8f7a20d64a2f591caa486427e036ba9e3821d831ccf6eff5934a`. Code source53229582; Mango4.1.37/code240, Apple Music6.5.3/code1599. Earlier audio results retain their original binaries.

The owner confirmed fresh30-minute availability. After saved-grant connection, Android Settings was explicitly foreground and its actual top activity verified. The existing read-only observer ran31 October08:26:59–08:57:01 HKT:

```powershell
./scripts/observe-android-background.ps1 -PhoneSerial 53061JEBF22737 -DxSerial d -Adb (Get-Command adb).Source -DurationSeconds 1800 -SampleSeconds 30 -StopFile C:/GitHub/DX340Remote/work/background-20261031-final.stop
```

The stop flag was not created. The observer's terminal summary reports **1802.562 seconds,56 samples, observationComplete=true, no failure**. Sample timestamps span1800.026 seconds; the largest sampled gap is33.406 seconds, including observation work. Every sample retains the original phone/host process and both foreground services, actual Settings top activity, both source states paused2 and output37/headset(4). Both devices report Awake throughout. Compared with the first native event snapshots, the union of later snapshots has zero new nonempty phone-GATT/host event lines, no observed disconnect callback and no queued control operation. These are bounded sampled observations, not proof of unbroken radio delivery between samples.

After the observer terminated, the ordinary Remote was foregrounded. Setup remained authorized. Open Player → Refresh player queued a protected GET_STATE. After24.641 seconds through UI collection/waiting (beyond the12-second reply budget), Player still showed Connected to player, enabled authorized controls and no reply-timeout/error. The phone PID was unchanged. This verifies the UI/read recovery route with no observed timeout; it does not capture a decoded terminal packet or measure transport ACK latency. Normal Setup → Disconnect then removed foreground state and left startRequested=false while UI-bound. No playback or volume command was sent.

All four normalized host preferences, exposed bond identity/type inventories, and radio/font/theme settings match preflight. Pixel/JIMMYAH approvals remain. Both browsing modes stayON; both Accessibility services are actually bound. Final source sessions remain paused2/output37/headset(4), volume proof disabled. Companion remains enabled, its Advanced controls were collapsed and the screen returned to the top. No observer, emulator or local build remains running.

Raw evidence is `artifacts/android-background-hold/20261031-082658-75a8b/`; fresh UI files use `background-final-*` under `artifacts/connection-flow-20261031/`. The collector's original summary deliberately retains its pre-return NOT TESTED field; the separate post-return-read.json records the subsequent action rather than overwriting that original observation. The adjacent sanitized JSON binds package/source identities, all sample checks, limits, preservation and raw hashes. Earlier interrupted/incomplete background runs are preserved separately and are not relabeled as passes.

Physical screen-off pause/resume without DX340 handling, unplugged battery/walking/range, in-flight controls, phone calls, current6b permission/radio regressions, full source/format/SD/security, physical iPhone signing/Bluetooth and Watch remain open. See [phone acceptance](../phone-reliability-runbook.md) and [compatibility matrix](../compatibility-matrix.md).
