# Source handoff checks

28 October2026. DX340 Android13/API33, build eng.iBasso.20260812.181952; Mango4.1.37/code240; Apple Music6.5.3/code1599; companion0.1.0 debug. Existing user-confirmed AMP15 phone output configuration. No output-volume/gain changes.

## Implementation

SET_PLAYING(true), native/UI PLAY_ITEM and PLAY_QUEUE_ITEM share a bounded handoff preparation. The adapter validates target/revision and authorization before pausing a supported competing source once, waits for the same source session to report paused, then revalidates before target dispatch. Native item selection re-reads its parent after handoff. Authorization loss, changed source/session, unsupported pause, unknown state and timeout fail closed. There is no automatic replay or rollback. Redundant desired-state commands are no-ops, with capabilities rechecked before actual mutation.

## Verified software checks

scripts/build-android.ps1 passed: APKs, lint and27 JVM tests (11 protocol+3 cache+4 UI model+9 new handoff). Handoff tests use synthetic sources and a fake clock/scheduler; they cover pause observation, redundant completion, unsupported pause, timeout without retry, pre/during-request authorization loss, target change, source replacement, unknown/multiple active sources and a new competitor during the wait. They are not physical Bluetooth evidence. Log: artifacts/handoff-build-output-final.txt.

## Verified physical source-state checks

The actual companion SourceRegistry was exercised through its debug-only privileged DUMP-protected receiver. This bypasses BLE and controller approval intentionally for source isolation. scripts/probe-source-handoff.ps1 checks report freshness, operation/source match and both observed source states; it restores the original active source. It makes deliberate playback changes and must not be described as read-only.

| Run | Result | Evidence |
|---|---|---|
| Screen on,2 cycles | VERIFIED8 SET_PLAYING(true) requests: Mango/Mango/Apple/Apple repeated twice; each target playing and other paused | artifacts/handoff-20261028-205318/summary.json; passed=true, restoreError=null |
| Screen asleep,1 cycle | VERIFIED4 requests in both directions, repeated desired states; target playing and other paused | artifacts/handoff-20261028-205345/summary.json; passed=true, restoreError=null |
| Restoration | VERIFIED original Apple source restored after each run; screen woke afterward | Summary/source snapshots; no lock configuration changed |
| Audible transition/overlap | NOT TESTED | Session states are not acoustic measurements or user listening confirmation |
| Phone BLE source switching | NOT TESTED | No separate controller phone available |
| Long sleep/30min pause,20/100 commands | NOT TESTED | Short local samples do not pass T03/T05/T09 in full |
| Native/UI item and queue handoff | IMPLEMENTED; further physical cases pending | Above physical runs cover desired-state handoff, not every selection path |

Reproduction after starting the current debug companion:

```powershell
./scripts/probe-source-handoff.ps1 -Serial d -Cycles 2
```

Use d only when ADB identifies the same DX340. Both existing source sessions and exactly one playing source are required. The script never enables capabilities from its result or raises volume. It restores source selection/play state, not elapsed position; time naturally advances during testing.
