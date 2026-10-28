# Confirmed-indication queue and completion audit

29 October 2026, Asia/Shanghai. Follow-up to local checkpoint99d56b7. Application changes are local; no new package was installed or published.

## Change

The DX340 GATT server previously waited indefinitely for onNotificationSent. If Android omitted that callback, the busy flag held all later replies. The host now uses the tested IndicationQueue to submit one confirmed frame at a time and schedule a10-second per-frame deadline. Missing completion, callback error, immediate submit failure or queue overflow closes the queue and clears pending control/background frames. High-priority control replies may preempt artwork between confirmed frames.

The native adapter clears its logical peer, subscription, reassembly and app authentication through the disconnect callback before asking Android to cancel a failed connection. Explicit unsubscribe/disconnect and service stop also clear the queue and its timer; stop ignores later service/connection callbacks. A fresh subscription/authorization starts with an empty queue. No source playback operation is retried, paused or resumed by this recovery path.

## Verification

| Status | Case | Evidence / scope |
|---|---|---|
| VERIFIED | Native Android builds, instrumentation APK compilation, lint and64 JVM tests | artifacts/indication-watchdog-final-android-build.txt and indication-watchdog-jvm-tests.json; zero failures/errors |
| VERIFIED |7 new confirmed-queue tests | Actual shared production queue helper; serial submission, missing callback, stale scheduled deadline after close/reopen, callback/immediate failure, priority order, bounded overflow and500 consecutive acknowledged frames with no active fixture deadlines |
| VERIFIED | Clear-before-failure ordering in queue helper | Failure callback observes zero pending frames and cannot enqueue new work until explicitly reopened |
| VERIFIED | Shared Android phone analysis/22 tests/release build/release lint | artifacts/indication-watchdog-final-phone-build.txt and indication-watchdog-flutter-{analyze,tests}.txt; shared native module compiles in both Android project roots |
| VERIFIED | Current installed/new host signing mismatch | artifacts/indication-watchdog-signing-preflight.json/txt. Generated certificateed3ffa7b... differs from installed24f48aa3...; no install attempted |
| FAILED environment probe | Connected Apple tool cannot list simulators | XcodeBuildMCP list_sims returned spawn xcrun ENOENT; artifacts/indication-watchdog-apple-environment.txt. Current Swift/iOS changes remain uncompiled here |
| BLOCKED | New DX340 package runtime verification | Original key remains on another Windows PC; readable local path/transfer pending. Only DX340 is connected, not a separate phone |
| NOT TESTED | Actual indications, callback ordering, disconnect/reconnect, subscription-disable and security | No physical BLE central is available. Helper tests do not prove Android radio integration or actual MITM properties |
| NOT TESTED | Device sleep/wall-clock timeout, battery, latency and audible outcome | Handler scheduling can be delayed by deep sleep; no timing guarantee is inferred from the simulated clock |

Android's completion callback identifies the device but carries no frame ID. The software tests prove stale scheduled timeout isolation, not that a delayed native callback from the same peer after a reconnect can always be distinguished. That callback-ordering case still requires physical tests. The10-second deadline uses Handler uptime scheduling; sleep/main-thread delays can extend wall-clock time. No permanent wake lock or alarm was added.

## Current packages and next action

Companion SHA256: `7e2bd68847573ef43e3252adc268d3d14759c327eb6a1603166522516421bce7`. Shared-phone SHA256: `e9a5be9a8f65546c9df6d210bb82d32d9605adf5da03f12cc96a866711052c71`. These still use this PC's default development key and supersede earlier generated-package hash entries. Installed baseline apps/data/settings remain unchanged.

The same signing/hardware blockers have been revalidated over successive continuations. Remaining acceptance work needs external state: copy the original debug.keystore from the original Windows PC, then verify its identity/rebuild/update; provide actual Android/iPhone hardware and a usable Mac/Xcode for the respective gates. Current software checks are complete for this change. More simulated checks cannot establish the remaining radio, source-selection, sleep, walking, battery or audio results. See completion-audit.md and compatibility-matrix.md; the full system remains incomplete.

After a matching-key update, exercise missing/error completions, unsubscribe during transfer, host stop with an in-flight frame, low-capacity ATT transfers and reconnect to the same peer. Confirm authorization is cleared, old frames/controls are not replayed, fresh state recovers and music remains independent. Record actual callbacks, timings and human listening separately. Keep existing standard signing keys and source apps intact.

Primary contracts checked: [BluetoothGattServerCallback completion serialization](https://developer.android.com/reference/android/bluetooth/BluetoothGattServerCallback#onNotificationSent(android.bluetooth.BluetoothDevice,%20int)), [Android Handler scheduling](https://developer.android.com/reference/android/os/Handler#postDelayed(java.lang.Runnable,%20long)). These API contracts are not physical acceptance evidence.
