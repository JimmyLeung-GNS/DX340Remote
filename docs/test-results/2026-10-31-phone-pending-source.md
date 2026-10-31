# Current phone pending source changes —31 October2026

**VERIFIED/subsets; full goal ACTIVE/incomplete.** Original-signer installed Pixel4318e6ad…/source5f2de32 and unchanged Companion00e5e50b…/source7fbdec8. [Exact versions, hashes, timings and evidence manifest](2026-10-31-phone-pending-source.json). Prior permission recovery and the older audio cases retain their own scopes.

## Method and actual results

Both sources start paused at37/headset(4), optional browsingON. The ordinary Pixel UI sends each browse over its real saved-grant BLE connection. A parallel read-only log watcher waits for one new QUEUED:BROWSE from the current app process, then waits at least300ms before invoking the existing debug-only/DUMP-protected own Companion receiver with GET_STATE and the other source. That receiver selects the requested source before reading it. No product code or APK changes were needed.

This is a real phone request with an explicit **source-local interruption trigger**, not a Pixel source-button sequence or a wholly source-local browse. Only the phone uses UIAutomator; DX340 evidence uses the own debug adapter/public service dumps. No DX340 UIAutomator, playback, volume, radio, permission or approval operation was sent.

| Pending phone browse | Companion source change | Accepted reply | Minimum queued-to-injection | Terminal error |
|---|---|---:|---:|---:|
| Mango Local folders →Internal Storage |Mango→Apple|142ms|360ms|1170ms/AUTHORIZATION_DENIED|
| Apple Downloaded →Songs |Apple→Mango|94ms|344ms|1512ms/AUTHORIZATION_DENIED|

Both accepted replies precede the trigger's conservative minimum delay, and both terminal errors follow it. These were pending accepted requests, not completed-before-trigger attempts. The error is the existing adapter's boolean request-scope cancellation after source selection changes, consistent with the [older source-local report](2026-10-29-assisted-interruptions.md); **it does not mean controller approval was revoked**. The phone remains authorized/connected.

After each change, Library reports **Library changed. Browse again or refresh Queue.** and no old Browse/Play item rows remain. A fresh Apple Downloaded root loads successfully1592ms. After the reverse change, fresh Mango root/folders/Internal Storage loads successfully1305ms and shows its own folder rows. No stale source data is republished after those observations. Deliberate replay of an old opaque token over the wire was not performed.

## Receipts and preservation

The actual export belongs to the same native model as the prior3-request permission-recovery export; all3 earlier rows compare unchanged. The new sequence adds10 requests:1 HELLO,1 AUTHORIZE,8 BROWSE. Eight applied RESULTs and the2 expected applied cancellation ERRORs are retained;13 total, zero evictions. There is no PLAY_ITEM/SET_PLAYING/skip/volume command. The analyser keeps T04 NOT_ADJUDICATED; these timings are decoded replies, not GATT frame ACK or acoustic measurements.

Final17:07 direct state: both source sessions paused2, volume37/headset(4), four saved preferences byte-hash unchanged. Pixel/JIMMYAH grants and browsingON retained; Companion foreground/both Accessibility bindings healthy. Control target restored to Mango; phone normal Disconnect clears foreground/started work and Setup help is collapsed. No new package, signer or user settings change. No build, CI job, emulator, long-hold observer or listening test started in this turn.

These two cases advance current Android pending-browse/source-context coverage; they do not pass all T12/T16 or either platform's full reliability matrix. Phone-UI switching, stale-token replay, radio/calls/range/screen sleep, full source/offline/format/playlist/SD/output/command/latency/battery/walk/ownership and physical owner-iPhone/Mac signing remain open; Watch stays gated on stable phones. The fresh30-minute hold and earlier retrospective envision hearing questions remain unanswered; no hold or audio pass is inferred.
