# First-write diagnostics and real-stack recovery —1 November2026

**VERIFIED implementation and scoped recovery; input-write interruption target NOT MET. Full goal remains incomplete.** [Structured result](2026-11-01-first-write-submission.json). This case separates an implemented observation, a failed attempt to target a specific fault phase, an unsuppressed host timeout and Apple simulator evidence.

Source `d06a481b4eb533b42d80331203891f8f62a342ab` adds optional format1 `firstWriteSubmittedAfterMs` to the bounded Kotlin/Swift native journal. Android records it after the first frame's platform API returns success; CoreBluetooth records invocation because its API has no immediate acceptance return. It is separate from the successful **last-frame** callback, protocol ACCEPTED, decoded result and hearing. [Contract and primary API sources](../connection-reports.md). FIFO submission observations never advance the queue or retry a command. Duplicate, abandoned, evicted and regressed-clock cases retain first/unknown timings without reviving operations. No playback, authorization or BLE wire command changes.

Native/ref/probe builds/lint and **242 JVM tests** pass:58 protocol,3 controller,7 cache,146 source-adapter,28 host. Shared Android analyze/**82 Flutter tests**/release build/lint pass. **18 Python analyser checks** pass. Shared fixtures now contain8 FIFO and16 journal cases, exercised by Kotlin/Swift; fixture cases are not added again to aggregate test counts. Actual older95 journal exports deduplicate to60 rows with zero inferred first-submission samples. New field merge/ordering checks reject conflicts, submission after ACK or local termination, and invalid duration types.

The original signing certificate was compared with the installed Pixel package before update. Candidate and actual installed SHA256 are `a65ac6a9660223532a23f0eaf828dc62282759d1e66e04dba66fb10a2704622a`, shared Remote0.2.0/code2, Pixel9a Android16/API36 `CP1A.260405.005`, new PID4427. Installed Companion remains **61153223b7a37d5ed3ac293455478d4199cf4fed4b2f4282fb21e5f03915a215**/source9fb503a, Companion0.1.0/code1 on DX340 Android13/API33 `eng.iBasso.20260812.181952`, PID10544. The newly built native host candidate was **not installed**. The update intentionally starts new model `a6d211e5-d892-4ae6-a416-ebe05ff24768`; the previous60-row0baf310c journal remains in its prior report, not merged across APKs or claimed persisted through process replacement.

## Actual phase targeting

Preflight system logs expose connection events but no characteristic-write trace rows. SDK36's local reference uses a disabled verbose write-log branch; this is not a statement that every vendor build is identical. The new current phone first demonstrates HELLO4/72/116ms, AUTHORIZE6/139/925ms and an explicit GET_STATE4/169/468ms submission/ACK/result.

A single-use on-device watcher waits for a unique arm marker and the next actual WRITE_SUBMITTED:GET_STATE request ID, then requests Bluetooth disable and restores the original On choice. It does not clear logs. A25second no-trigger bound and exit/signal restoration guard prevent an indefinite armed job or a deliberately retained Off setting. Initial phase-file reads precede creation; readiness waits on the same PID, without launching another job. The actual job/root is recorded in the result JSON, terminal FINISHED/collected; watcher6068 and logger6109 no longer exist.

The selected request records **submission3ms, successful final write ACK144ms, local loss363ms, terminalApplied=false**. Therefore the write finished before loss: **this was a reply-wait interruption, not proof of an input ATT write still pending**. Do not relabel the target as passed. Global Bluetooth0 was sampled while Bluetooth Manager reported **BLE_TURNING_ON**; this is not proof that every RF/background BLE function was Off. The script restored global1 and actual State ON. Wi-Fi/cellular and other settings were untouched. A subsequent direct timing race was not retried.

Native history3→4→7 retains every preceding row exactly, zero evictions. Post-On metadata is cached and controls disabled. The four-row export contains only the new retired read after its three-row baseline, proving no automatic request replay in that window. One later ordinary saved Connect adds only fresh HELLO/AUTHORIZE; a deliberate new GET_STATE then succeeds at4/160/556ms submission/ACK/result. The abandoned request remains lost/not applied with its original ACK, rather than being overwritten by the fresh result. No playback/source/volume command is sent.

The remaining input-write test needs a separate, bounded debug host ATT-response hold/discard: one current authorized idle link, paused-source/exact-epoch/DUMP-only arming, no staged message dispatch, release no-op and unconditional clearing on loss/stop. A fallback link close must be bounded and generation guarded. Observe actual loss relative to any fallback before adjudicating its scope. Preserve this missed window; do not race or replay playback to obtain a passing result.

## Unsuppressed host recovery

The deliberate client-radio interruption also leaves an outgoing host indication unresolved. Host events show disconnect, then **BLE_REPLY_TIMEOUT9,890ms later**, followed by automatic fresh advertising **693ms after timeout**. The latest debug callback arm is1,655,617ms old at that timeout, beyond its20second lifetime; no callback suppression is armed/ignored in this interval. Thus this is an **un-injected indication deadline following intentional controller-radio interruption**, separate from the earlier deliberately ignored callbacks. It is not a spontaneous vendor-failure claim or a diagnosis of a specific stack defect.

Host PID/epoch remain unchanged; no local restart, new pairing window or approval is used. The phone stays disconnected after its radio returns and subsequently uses ordinary saved Connect. This supplies real-stack automatic transport recreation evidence while leaving full T12/T16, radio/permission races, duplicate mutations, range/calls/sleep and both-phone acceptance open.

## Exact-source Apple jobs

Both d06a481 jobs are **terminal SUCCESS, archives verified and closed**; do not poll/rerun them. Hosted macOS26.6.2/25G83, Xcode26.6/17F113; boot GUIDs correlate to iPhone Air/iOS26.5 simulator inventories. Owner Mac/iPhone/signing remain unavailable now.

| Job | Verified artifact | Scope |
|---|---|---|
| Native41239849340 |13057022917, SHA256 `abbfc8bfe3baf4027f66cd45827e6573a54cc78d6599c48e9f7730021f660e8d`|22 Swift tests,12 capture guards, native Xcode build/launch|
| Shared41239849305 |13056948663, SHA256 `fe1d840255a93f6d7be156bdc17eac2c4d5d8869465c395afda0c6669ced65a2`|82 Flutter/analyze,12 guards, seven simulator builds, native UIKit report sheet and ordinary restoration inspected; format1 export matches model ID/zero requests|

These are software/simulator/export results, not a physical iPhone submission callback, BLE/audio test or owner signing inventory. Earlier e40 jobs stay closed in their own scope.

Final04:53:56 HKT snapshots preserve all four preference hashes, global settings, permission grant/flag sets, host PID/epoch, paused source positions, three approved controllers/bonds, both browsing modes/Accessibility bindings and mounted FBB9-E8FE card. Phone PID intentionally changed only on upgrade and remains4427 through the fault. Both sources PAUSED2, volume37/headset(4)/proof false; phone normal Disconnect/help collapse clears foreground ownership; host main/foreground service retained. No source playback, volume, storage, download setting, key or bond action occurred. The three older611/95 Apple/WAV/FLAC hearing questions remain pending and do not certify the newa65 build. Previous failed holds/arming5024, original no-card Internal timeout and Apple MEDIA_MOUNTED crash remain separate, unresolved evidence.

Raw case: `artifacts/first-write-submission-20261101/`,308 SHA256-manifest entries including exact test XML, source-version builds, actual installed hashes/signer comparison, job and radio samples, native exports, host/client events, scripts, both CI archives/metadata/captures and model-matched report. Full goal ACTIVE/incomplete; strict cellular-off T01, current-build audio and the30minute unplugged test remain unproved.
