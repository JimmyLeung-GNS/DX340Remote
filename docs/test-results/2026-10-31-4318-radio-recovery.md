# Current4318 background Bluetooth-off recovery —31 October2026

**VERIFIED/subset; full goal ACTIVE/incomplete.** [Versions, hashes, exact evidence and restoration manifest](2026-10-31-4318-radio-recovery.json). Original-signer Pixel4318/source5f2de32 and unchanged Companion00e5/source7fbdec8; the earlier04/6b radio results retain their original APK scopes.

## Actual sequence

Both sources start paused at37/headset(4). Pixel Bluetooth1 is recorded before the test, along with both devices' settings, Bluetooth dumps and the phone's permission states. The ordinary phone reconnects with saved approval; its foreground service is verified active. Public Android Settings is resolved/launched and independently verified as the foreground activity, leaving Remote in the background.

Only the Pixel radio is disabled through `cmd bluetooth_manager disable`; `wait-for-state:STATE_OFF` and actual setting0 confirm it. Foreground/started connection work clears while Settings remains foreground. The command-to-cleanup observation is672ms, including shell/state-wait/observation overhead, not exact callback or radio latency. Reopening Remote while off shows **Turn on Bluetooth** and Settings guidance, without restarting foreground work. Its process ID remains unchanged.

The finally block restores the initially enabled radio and waits for STATE_ON/setting1. UI returns to **Connect your player** with its remembered approval; no foreground work runs while idle. One deliberate saved-player Connect and Refresh player then succeed without pairing or new local approval.

The actual export shares its native model with the previous13-request pending-source report; all13 old rows compare unchanged. Five new requests are applied RESULTs: before-off HELLO142ms/authorization838ms, after-on HELLO86ms/authorization899ms/GET_STATE436ms.18 total/zero evictions. No source playback/volume command or new error occurs in the radio case. These are decoded reply durations, not GATT frame ACK or acoustic/T04 measurements. The deliberate reconnect is not an automatic range-recovery result; no playback mutation was in flight, so interrupted-mutation no-replay remains open.

## Restoration and limits

Normal Disconnect clears phone foreground/started state and Setup help is collapsed. Both direct source sessions remain paused2/output37/headset(4), volume proof false. Four saved host preferences compare byte-hash unchanged, retaining Pixel/JIMMYAH approvals and optional browsingON. Host remains foreground with both Accessibility services bound. Original radio/Wi-Fi/mobile-data/font/timeout settings and all3 phone permission grant/flag sets match before.

Phone exposed bonded identities/types and DX340 exposed bonded rows match. A first parser expected a heading without Android16's device count and did not recognize it; corrected extraction is saved separately. Live ACL/encryption status differs between the initially disconnected and subsequently connected phone dumps. Those differences are retained; they are not lost bonds or a new pairing/encryption-grade audit. No factory reset, unpairing or approval revocation was performed.

No product code, package, signing material or source engine changed. No new build/CI/emulator/observer/listening test was needed;178 JVM/72 Flutter/19 Swift and the closed5f Apple archives keep their documented scope. Both USB devices remain available. Fresh30-minute readiness and earlier retrospective envision hearing questions remain unanswered; no sleep hold or new audio pass is inferred.

This adds one current-build Android radio interruption/recovery subset. Calls, in-flight mutations, host-process interruption on current4318, range/sleep, full two-phone/source/format/playlist/SD/output/100-command/latency/battery/walk/ownership, physical owner-iPhone/Mac signing and later Watch remain unfinished.
