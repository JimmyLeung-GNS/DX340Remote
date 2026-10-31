# GATT topology during the discovery stall

**VERIFIED:** Companion application connection count drops to0 while Pixel reports a replacement controller client and four same-peer OS/Google server connections during two unfinished discoveries. This distinguishes application cancellation from complete shared-link teardown. **Root cause and fast recovery remain unresolved.** [Structured evidence](2026-11-01-gatt-stall-topology.json).

Unchanged installed pair: Companion3a32689a/source5f4f1f2, DX340 Android13/API33 `eng.iBasso.20260812.181952`, PID16085; Pixel Remote daa9f5e5/source84e56ac, Android16/API36 `CP1A.260405.005`, PID12008. Companion0.1.0/code1, Remote0.2.0/code2, Mango4.1.37/code240 and Apple Music6.5.3/code1599. Exact hashes are in the structured report. No app update/build/CI or device-setting action during this physical case.

One unique read-only held-input fault `topology-055604`; no repeated fault/manual reconnect. Device dumps/logs are bracketed by each device's epoch seconds/uptime plus PC read duration. The two endpoint reads overlap; collections take938–1078ms and are not simultaneous RF measurements. PC since-tap samples do not substitute for native request or acoustic timing.

| Sample | PC since tap | DX server connections | Pixel controller registration / connections | Pixel same-peer OS/Google server connections |
|---|---:|---:|---:|---:|
| Normal connected |before|1|68 /1|4|
| Write held |265ms|1|68 /1|4|
| Host cancellation requested |5578ms|0|68 /1|4|
| First discovery unfinished |20281ms|0|69 /1|4|
| Second discovery unfinished |45015ms|0|70 /1|4|
| Second discovery still unfinished |65312ms|0|70 /1|4|
| Eventual saved approval |79015ms|1|71 /1|4|

Registration IDs change, so the persistent client count does not prove an old Java GATT client leak. Logs place the20/45/65s samples between discovery start and completion. The same extra servers exist during normal operation and successful recovery, so their presence alone is not a sufficient explanation. Do not blame or disable OS/Nearby services, assume physical ACL state from app callbacks, or claim an identified blocked ATT transaction.

Host held→logical retirement/public cancel request5005ms; →saved approval73410ms after that request. Request `c561a960-81e1-4257-a306-47c2a02bab9f` has submission4ms/noACK/loss10022ms/notapplied. Journal8→12 preserves all eight prior rows/zero evictions, fresh recovery only HELLO/AUTHORIZE, then explicit GET_STATE `0e23248a-c635-4984-9850-85325da9a09a` succeeds at submission3/ACK145/result444ms. No playback/source/volume command, replay, radio or network change. This is still slow recovery on unchanged software, not a speed fix or full T12/T16 pass.

Final06:00:45 HKT: both PAUSED2/volume37/headset(4)/proof false, source positions/preferences/settings/PIDs/grants/bonds/modesON/bindings/SD retained, normal phone Disconnect/help collapsed/foreground cleared; host enabled/foreground service/advertising. Raw evidence `artifacts/gatt-stall-topology-20261101/`:126 hashes, digest in JSON. Earlier frozen cases unchanged. Next independent code finding: [failed input-response submission handling](2026-11-01-input-response-guard.md); avoid repeating this unchanged fault without a new discriminating question.
