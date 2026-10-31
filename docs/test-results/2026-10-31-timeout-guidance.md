# Android timeout guidance

**VERIFIED for this targeted Android timeout/recovery change**,31 October2026. Full goal ACTIVE/incomplete.

The already approved Pixel previously ended a stopped-Companion retry with “open pairing on DX340 and retry.” That suggested another enrollment window even though the required action was to restore Companion phone control. Source `fb26152fa4ab79122bfc0f51981526c0f173b313` changes only the Android connection-timeout text:

> Connection timed out. Check phone control is enabled in Companion on your player, then tap Connect again.

Existing30-second attempt timers, bounded retry, saved-player identity, authenticated subscription/local approval, protocol v1 and no-replay behavior are unchanged. Encrypted-pairing errors and first-time enrollment remain separate. No Dart/Swift/source/asset/layout change.

## Packages and checks

Pixel9a Android16/API36 CP1A.260405.005/shared0.2.0 code2 was upgraded in place from6b7e379d… to **`0cf86c9cfead4b27b3b2c1dcf2135b98fbe78fb992bc755491a1549d79b2fdc0`**. Candidate and installed certificates match the original `24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d`. Final installed bytes were re-pulled and match. Companion remains **`491dbbffabda8f7a20d64a2f591caa486427e036ba9e3821d831ccf6eff5934a`**, DX340 Android13/API33 eng.iBasso.20260812.181952/0.1.0 code1; its final installed bytes also match. Last recorded source versions are Mango4.1.37/code240 and Apple Music6.5.3/code1599; a final package-version refresh was unavailable after the subsequent USB disappearance.

Shared Android analysis/release/lint and native-reference assembly/lint pass. Both compiled client APKs contain the replacement literal and no old timeout literal. No new unit test mirrors a wording change. Prior168 JVM/67 Flutter/17 Swift and Apple build results keep their exact recorded source scope; unchanged suites were not repeated. Apple source/workflow paths did not change, so no new Apple job was started. The retained native-reference APK was built, not installed over the shared controller.

## Actual timeout and prescribed recovery

The new ordinary Pixel connected using its saved approval. The driver used Companion's normal Advanced → Stop Bluetooth remote action, preserving music-app state. Pixel stayed in bounded retry with foreground ownership. It reached the corrected terminal message at an observed128.656seconds (UI polling included); foreground/started state cleared and its PID was unchanged. This stopped-host negative-case duration is not discovery/ACK/range latency.

The actual error was inspected in one batch at original font scale1.0 and temporary2.0. Full text, Connect to DX340 and Connection help remain visible without clipping or scrolling. Original font scale1.0 was restored in finally. This is a scoped light/portrait Pixel copy check, not a new whole-product, iPhone, theme, landscape or screen-reader verdict. No further cosmetic edits or rebuilds followed this inspection.

Companion was re-enabled through its visible Enable phone control button; then the phone's Connect to DX340 action restored protected saved authorization without new pairing/enrollment/approval. The state-request UI remained authorized with enabled controls and no timeout/error beyond its12-second reply budget (24.657seconds including UI collection/waiting). No decoded terminal packet or p95 ACK/state timing was captured. Normal Disconnect then removed phone foreground/started state. Companion stays enabled, Advanced collapsed/top restored.

## Preservation and remaining acceptance

All4 normalized host preferences, Pixel/JIMMYAH approvals, exposed bond identity/type inventories and radio/font/theme settings match preflight. Phone runtime grants/flags match the last verified6b pre-upgrade inventory. Both optional browsing modes remainON and both Accessibility services are bound. Final source sessions report paused2/output37/headset(4), volume proof disabled. No playback/volume command or new audio test occurred. No observer, emulator or build remains running.

The earlier56-sample background observation and permission/radio/process-loss matrix remain explicitly scoped to6b, not relabeled as new0cf results. Full calls/in-flight/range/sleep/battery/walking/iPhone/signing/source/format/SD/output/handoff and Watch acceptance remain open. Owner availability for Apple Music listening was requested separately; no answer has arrived and no listening test started. Phone operation receipts/timing still need stronger observability for the latency gate; no-timeout UI checks do not replace that evidence.

Raw evidence is under `artifacts/timeout-guidance-20261031/` and unique timeout-guidance files in `artifacts/connection-flow-20261031/`. Previous default build files were copied into before-build-default-files before rebuilding; prior reports/raw evidence were not overwritten. The adjacent JSON binds source/packages, compiled strings, signing, checks, restoration and raw hashes. The [connection guide](../connection-guide.md) already describes the corrected approved-phone recovery path.

After all tests, the final package pulls, preservation checks and restoration inputs completed, both devices disappeared from ADB. A fresh09:36 inventory was empty. Last verified09:33 phone state is normally disconnected/foreground removed, original font1.0 restored, host enabled, source sessions paused37 and all preservation assertions passed. No active observer/test was interrupted. The absence prevents further physical/source-version checks; it does not establish a radio/audio outcome. Re-inventory on return.
