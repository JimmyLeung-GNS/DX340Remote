# Explicit approved-controller handoff

29 October2026, Windows continuation, branch `codex/source-recovery-checkpoint`.

**IMPLEMENTED BETA; physical two-controller behavior NOT TESTED.** The installed playback-evidence companion remains04846d60bc4e65065028d510286b56cba91577840a994d351146c55dff418fa2 (source8f9af73). The new candidate has not been installed on DX340. Twenty Mango audible confirmations and earlier Apple results do not verify this candidate or either phone Bluetooth path.

## Behavior and bounds

The host admits at most two transports with one controlling lease; a second must already be bonded and locally approved. A busy approved controller receives an opaque ownership revision without player metadata. The shared phone Setup action **Take control on this phone** consumes a host-bound native proposal only after a deliberate tap. REQUEST_CONTROL compares host epoch/revision, grants a new lease, drops old unsent replies and sends a fixed control-loss notice. Updated displaced clients disconnect and suppress autoreconnect. Normal authorization never automatically takes over. Ownership changes send no source/playback/volume command.

The common per-controller ledger prevents repeated grant IDs from retaking control after displacement/disconnect. Unique mutation gate leases prevent an old callback with the same request ID from releasing a new operation. Independent per-link reassembly/MTU/serials and a global indication serializer prevent peers sharing message state. Bootstrap expires after60seconds; displaced legacy links after10seconds. All are main-thread scheduled limits, not measured radio deadlines. A missing OS indication callback now closes the whole GATT server; the companion tells the owner to stop/start remote control rather than submitting into an uncertain operation. Repeated ambiguous CONNECTED events fail closed. Actual callback timing/host limits remain open.

Additive protocol/schema fields and native reference app actions are documented. Generic Flutter source-operation allowlists are unchanged: only the dedicated native method can consume the ownership proposal. No controller chooses its own wire revision in Dart. Old host/client pairs need disconnect-first operation.

## Verification so far

- **VERIFIED software:** `scripts/build-android.ps1` builds companion, native Android reference, research/probe APKs and lint;120 JVM tests pass (preceding108 plus5 handoff/6 peer-indication/1 pending-source cancellation cases). Logs `artifacts/handoff-native-final-build.txt`. Component command first failed for missing ANDROID_HOME before compilation; corrected environment passed (`handoff-component-tests-sdk-fixed.txt`).
- **FAILED then corrected:** Flutter analysis first rejected missing braces in the debug fixture. First large-text widget checks used ensureVisible before a lazy row existed; the action was moved ahead of setup instructions and tests now scroll to it. The next run exposed a missing test-platform reset before Flutter invariants; cleanup corrected. Failure logs remain in `handoff-phone-analysis-initial-failure.txt`, `handoff-phone-test-initial-failure.txt`, `handoff-flutter-tests-corrected.txt`. Final build/test/rendering results follow in the checkpoint update.
- **NOT TESTED hardware:** two central links, actual ownership/revocation/late-callback timing, continuity, phone reconnection, bonded identity resolution, iPhone signing/device installation and Watch. Existing source-local audio evidence stays separate.

Toolchain: pinned Gradle8.14/AGP8.11.1/Kotlin2.1.10/SDK36/JDK21.0.9 and Flutter3.41.4/Dart3.11.1; process-local Android SDK/signing/Windows selector settings follow windows-continuation.md. Original debug key remains outside Git.

Current primary API sequencing reference: [Android onNotificationSent](https://developer.android.com/reference/android/bluetooth/BluetoothGattServerCallback#onNotificationSent(android.bluetooth.BluetoothDevice,int)) requires completion before another submission; the candidate serializes globally and treats a missing completion as server failure. This is an implementation rule, not proof of DX340 multi-central behavior.

Exact physical next steps: [controller-handoff-runbook.md](../controller-handoff-runbook.md). Complete both phone offline/background/source tests before considering Watch.
