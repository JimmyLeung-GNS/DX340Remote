# Serialized service rediscovery on the physical Android pair

**VERIFIED:** a real Service Changed event during discovery now drains the old callback, rediscovers on the same GATT client and restores saved authorization. A later explicit state read succeeds. **Failures retained:** initial stalled connections were not repaired by this change or an own-server restart alone; one recorded Pixel Bluetooth cleanup was needed before the working-transport trial. This is a scoped rediscovery regression pass, not resolution of all stalled-link recovery. [Structured result](2026-11-01-service-change-recovery.json).

## Installed software and checks

Both packages were built from `731aa64424fa65c771b7ce0f4a3db7a590ead662` and installed in place with the original signer:

| Endpoint | Installed package | Runtime |
|---|---|---|
| DX340 | Companion0.1.0/code1, SHA256 `e1a024293c70e334de1e45c31176be6c19e4555fe97391e84f56b4515148b1f8` | Android13/API33 `eng.iBasso.20260812.181952`, PID21079 |
| Pixel9a | Remote0.2.0/code2, SHA256 `dac6016c1223f1f2edcc431d95fd108daaa1b2e8d5139fd2ae72d9393092f84f` | Android16/API36 `CP1A.260405.005`, PID15533 |

Native assembly/lint and268 aggregate JVM tests pass (183 debug +85 JVM), including eight new procedure-sequencing cases and the seven [failed-response dispatch](2026-11-01-input-response-guard.md) cases added immediately beforehand. Pinned shared Android analysis/release assembly/lint pass. Dart is unchanged;82Flutter/22Swift/18 analyser retain prior scopes and were not rerun. The20 host release tests/build/lint were run for sourcef1's input guard, separately from this Android-client change. No new Apple path/job or old-job polling. Original-signer comparisons and final installed hashes match; no uninstall/data reset.

## Change

[Android documents](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onServiceChanged(android.bluetooth.BluetoothGatt)) that database invalidation calls for rediscovery. The old phone immediately disconnected, abandoning an active discovery callback. The new gate serializes discovery, MTU, subscription and writes. Service Changed clears authorization, pending frames, correlations and old handles immediately; a busy native operation drains before one fresh discovery. Repeated changes coalesce, unrelated callbacks cannot advance setup, old commands are not replayed. The same link reuses its known MTU; setup has a generation-bound30s deadline that repeated invalidations do not extend. Both Android reference and shared phone use this native core. No wire/permission/Dart/iOS change.

## Failed attempts and environment cleanup

The earlier14b/daa update failure remains in the [input-guard report](2026-11-01-input-response-guard.md): six GATT resources/181562ms, old Service Changed handling abandons discovery, zero protocol writes. The new731 pair also initially stalls after an expired host bootstrap, with **no Service Changed callback** in that attempt. Its30s observation fails; ongoing native retries are then explicitly canceled. A separate visible Companion Stop/Enable cycle also fails its30s observation, and its remaining retries are explicitly canceled. These observation timeouts are not mislabeled as terminal retry exhaustion or erased by later success.

Before any of those new failures, a strict ADVERTISING-only scratch guard refused stale BOOTSTRAP_TIMEOUT status without tapping Connect. That guard was corrected to check saved-enabled state; the Companion screen itself said it was ready. No source/volume or new pairing action occurred. An initial post-install capture found the phone process not yet launched; a fresh post-launch capture was used. The empty-report formatter raised only after saving/collapsing a valid zero-row report; the saved report was retained and the scratch formatter corrected. One reused baseline filename is disclosed in driver notes; original attempt status/epoch remains in receipts/polls/logs.

One separate [cleanup record](2026-11-01-ble-environment-cleanup.json) switches **Pixel Bluetooth** off/on and verifies original global1/actualON restored. The off sample is BLE_TURNING_OFF, not proof of a fully RF-off interval. No Wi-Fi/cellular/Nearby/other OS-service setting, grant, bond or source is changed. A saved connection then succeeds, followed by explicit GET_STATE submission4/ACK142/result489ms. This is manual environment cleanup, **not automatic recovery or proof that the prior73–82s stalls are fixed**. Same-peer OS/Google connections also exist in working operation; their presence alone is not a proven cause.

## Real Service Changed trial on the working transport

With the phone authorized, one further visible Companion Stop/Enable pair creates an actual service-database change. The service is deliberately off for about28s while the visible Enable action is prepared. Three new GATT resources are created during that outage; intermediate failures while the Wire service is absent are retained. No phone Connect, new approval/pairing or radio change is sent during this trial.

Two real Service Changed events occur. The first invalidates an established connection and rediscovers; the missing Wire service during the intentional outage correctly causes bounded retry. The successful second event arrives during a reconnect's active discovery:

| Event | Pixel log seconds |
|---|---:|
| Discovery starts |1793485784.075|
| Service Changed received |1793485784.168|
| Old discovery completes |1793485784.241|
| Fresh discovery starts |1793485784.251|
| Fresh discovery completes |1793485784.254|
| HELLO submitted |1793485784.368|
| AUTHORIZE submitted |1793485784.545|

There is no close, disconnect request or replacement GATT resource between that second event and fresh authorization. Host advertising→approved is1054ms; the PC observer sees approval1907ms after Enable, which is a scoped observation and not UI/acoustic/p95 timing. The phone visibly returns to Ready automatically. Final explicit GET_STATE submission4/ACK134/result477ms succeeds. Pending playback mutation and invalidation during actual MTU/CCC/write phases retain software-only coverage.

Native model `665863d7-8c97-4990-b2e1-9a71afb2a423` grows0→0→3→6. All preceding rows remain equal/zero evictions; six applied results are HELLO/AUTH/GET_STATE twice. No playback/source/volume command or replay. The old a71a45b6 twelve-row history is preserved separately through the deliberate phone upgrade.

## Final state and remaining scope

Final06:32:53 HKT: both sources PAUSED2, exact positions/preferences/settings preserved, volume37/headset(4)/proof false. Both original-signer upgrades intentionally change PIDs; post-upgrade PIDs remain21079/15533 through the interventions. Own-service restarts change host epochs; final `2159b3e5-2f0d-408c-badf-379c8548416c`. Saved-enabled XML is restored byte-for-byte after both Stop/Enable pairs. Three grants/bonds and both optional modes/Accessibility bindings remain; card `public:179,1` mounted `FBB9-E8FE`, no Apple destination/mount change. Phone normal Disconnect/help collapsed/foreground cleared; host main/service foreground, Advanced collapsed, saved enabled/advertising.

Ignored evidence:371 hashed files under `artifacts/service-change-recovery-20261101/`, plus six cleanup files; digests in JSON. Prior126 topology/147 input-guard/239 cleanup-order/337 held-input evidence remains frozen. All builds/observers finished. Full source/control/audio/format/output/offline/sleep/range/calls/battery/walking/owner-iPhone/Mac signing/Watch remains unfinished. Next: actual Android source/control acceptance using direct observed playback without listening prompts. Avoid repeating unchanged withheld-response faults on this pair; any further fault work needs a new discriminating question and an explicit cleanup plan.
