# Device-local unplugged-test preparation — 31 October 2026

**VERIFIED short preparation checks; actual unplugged30-minute hold and USB-detachment survival NOT TESTED. Full goal ACTIVE/incomplete.** [Exact script digests, versions, sample clocks, outcomes and raw evidence hashes](2026-10-31-device-recorder.json). [Procedure](../unplugged-observation.md).

The [two failed USB-powered holds](2026-10-31-display-off-hold.md) remain failures. The new tool records locally so a later agreed test can remove the DX USB connection. No application is rebuilt or installed: original-signer Pixelce47b14c/source25ed62a and Companion82ad322f/sourcefa60705 remain, with the same DX340/Pixel/source versions documented in the JSON.

The launcher creates one unique owned scratch directory, normalizes/upload-checks Android shell syntax, detaches a read-only recorder and retains an exact manifest. Status verifies the script process rather than trusting a stale marker. Collection requires a terminal result, checks a strict file inventory/size bounds and produces hashes without deciding T05. Stop verifies the owned PID/path; the native recorder verifies its logger's output descriptor before signalling it. No screen, source, Bluetooth or configuration action exists in the recorder. Arming requires three consecutive unpowered/displayOFF readings five seconds apart and times out after120seconds. Clock/sample gaps and deadline behavior remain explicit.

Fourteen Python recorder regressions cover path/PID boundaries, conflicting receipts, live/unconfirmed jobs, completion races, file/size limits, destination preservation and stop ownership. Five phone-observer regressions cover actual foreground Remote rejection, Settings background, unknown awake activity, screen-off activity absence and process/service loss. A physical negative Pixel preflight rejects the currently disconnected/foreground Remote after one sample0.625s; it is not a background hold pass.

| Device-local case | Actual duration / samples | Outcome |
| --- | --- | --- |
| Initial awake recorder | 12.62s /5 | Snapshot timer completes, all displayON; empty power log does not establish event capture |
| First controlled sleep diagnostic | 11.31s /4 | ON→OFF snapshots captured, but direct-file power log remains empty; logger trial FAILED |
| Corrected stdout logger | 10.75s /4 | Actual sleep/OFF plus subsequent USB-power wake/ON events captured; not an OFF hold pass |
| Final candidate controlled sleep | 10.95s /4 | Actual sleep/OFF events and OFF samples captured; owned processes later confirmed retired |
| Final candidate unplugged arming/stop | No started hold /0 | Remains ARMING on USB; verified stop produces STOPPED/OPERATOR_STOP and both owned processes retire |

The initial empty logs are preserved with their exact prototype scripts/digests, not relabeled as working event capture. A bounded stdout log stream fixes the observed loss. It has a1024-line limit; an early logger exit fails the recorder. The corrected intermediate capture additionally records another WAKE_REASON_PLUGGED_IN/PLUGGED:false event while USB remains attached. This supports changing the observation route, without diagnosing cable/port/OS/battery causation.

The final shell SHA256 is `107e08b392852358bc90143c39acb94f45fc42ddce279d4730d99c4da5f29f9e`. Both final-candidate checks use those bytes. Detachment from the launching ADB command is observed; actual physical USB-removal survival is still unverified. Shell scheduling may pause during suspend; COMPLETED/deadline reached is only recorder completion, not continuous display/Bluetooth/audio proof. Unexpected process loss remains incomplete and requires inspection of the same owned job and logger; never launch a replacement from a stale marker or a polling timeout.

Final capture retains all four host preference hashes, global settings, process IDs, own grant/flag sets and exposed bond identities/types. Host foreground/both Accessibility bindings, both browsing modesON, both source sessions paused2 at37/headset(4), volume prooffalse, normal disconnected phone/targetMango and mounted SD remain. The recorder sends no playback/volume command and does not open/change Apple downloads, mount storage, access private files, change firmware/power policy or enable a network. Native diagnostic scratch files remain in their unique directories, with no owned recorder/logger left live.

A fresh unplugged availability question is pending; no long hold has started. When ready, prepare the ordinary phone session, launch one arming recorder, obtain the actual unplug/power-off reply, then run the separate31-minute phone observer. Verify actual phone state/control/hearing before reconnecting DX USB and collect that exact manifest afterward. The prior80 Flutter/199 JVM/19 Swift and closed25ed simulator archive retain their previous scopes. Full T01–T18/physical owner-iPhone/Mac signing/source/format/output/reliability/security/Watch acceptance remains open.
