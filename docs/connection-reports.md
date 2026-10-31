# Connection reports

The shared phone app has a deliberate troubleshooting action in **Setup → Connection help**: **Save connection report** on Android or **Share connection report** on iPhone. This does not connect, authorize, retry, change playback or adjust volume. The regular UI snapshot does not include the journal, so collecting it adds no library/queue rebuild notifications.

History belongs to the current native model instance and is cleared by process restart. Each native model creates a random instance identifier, reused by its exports for safe correlation; it is not a device, Bluetooth or controller identity. The default limit is 256 requests and 256 connection transitions. Eviction counters expose missing earlier history.

Format version 1 contains allowlisted operation/source/error/connection categories, request IDs and relative monotonic milliseconds. Unknown categories become `OTHER`/`other`. Music metadata, command arguments, item IDs, paths, artwork, Bluetooth identities, host epochs and credentials are excluded. App/OS versions and generic device model identify the software environment. Android additionally hashes its own installed base APK, outside the BLE callback thread. No source storage is scanned.

Connection entries record native setup-phase assignments; the root `connectionPhase` separately captures current derived permission/radio/authorization readiness. This is not continuous radio observation. Unsolicited state events are not matched request receipts and do not create latency samples. In particular, a dispatched-only action followed by a later state event does not acquire an inferred observed-state duration in this journal.

Each request records `startedMs`, `accepted`, nullable `acceptedAfterMs`, nullable `finishedAfterMs`, `result`, `terminalApplied`, `hostOutcome` and nullable `error`. A duplicate/late reply cannot overwrite its first receipt. Unknown or changed-host replies are rejected before journaling. Clock regression produces null, never a fabricated duration. Timings start at native send registration, before encoding/queued writes, and end at a matched decoded reply after reassembly. They are **not** a GATT frame acknowledgement, UI-tap measurement or acoustic measurement.

`ACCEPTED` means the host accepted the request. A terminal receipt can still be ignored by context/payload guards; `terminalApplied` becomes true only after the native client processes it. `hostOutcome=dispatched` is distinct from `observed`. Neither proves audible playback. Timeout, send failure, connection loss and session reset finalize unfinished entries without resending operations.

Android creates a unique app-owned JSON file under `Download/DX340Remote` with MediaStore, publishing only after a successful bounded write. It requests no additional storage permission. This follows [Android shared-media guidance](https://developer.android.com/training/data-storage/shared/media). iPhone prepares at most three own temporary JSON files and opens the native [activity controller](https://developer.apple.com/documentation/uikit/uiactivityviewcontroller/), anchored for iPad. The notice says to choose a destination; presentation is not a claim that the owner saved or shared a file. No destination or recipient is chosen automatically.

The report is at most 256 KiB. Native export is single-flight; a failure permits another deliberate attempt. The shared UI keeps report progress/errors separate from connection/source status.

## Verification and limits

The Kotlin and Swift journals consume the same six fixtures for duplicate/late replies, privacy categories, lost accepted mutations, bounded eviction, clock regression and duplicate registration. Sustained-operation tests cover bounded memory; Android JSON tests check explicit nulls survive serialization. Flutter tests check explicit invocation, single-flight, failure/retry, disposal and the collapsed-help action on both platform styles.

The iOS CI developer-only `report_export_probe.dart` uses the real native bridge and UIKit with no BLE/source fixture, correlates a unique launch marker and native model instance, checks an empty request history and the strict report keys, then captures the share sheet. The existing EXIT trap rebuilds/reinstalls/relaunches ordinary `main.dart`, including after failure. This probe is never imported by the production entrypoint.

Builds, simulator exports and fixture timings do not pass T04. Physical exports from the actual phones, decoded ACCEPTED versus terminal/applied outcomes, source-state observation and listening remain separate required evidence. Do not relabel old physical results as results for a newly built candidate.

## Summarizing exports

Run `python scripts/summarize-connection-reports.py <first.json> <second.json>` with explicit export paths. The standard-library tool rejects mixed build/platform/OS environments, conflicting request identities/receipts, duplicate JSON keys and oversized/unexpected report fields. Separate model sessions require the explicit `--allow-multiple-sessions` option; request IDs are deduplicated with their model instance, so process restarts do not collide. Repeated eviction counters use the maximum per session rather than being added on every export.

The output groups operations by source and separates accepted-receipt, terminal-reply, applied-result and host-observed applied-result distributions. Timeouts/losses/errors remain outcome counts. Percentiles use nearest rank; missing or invalid-clock samples do not become zeros. Empty reports retain zero samples and null percentiles. Every summary says `physicalT04=NOT_ADJUDICATED`: these reports alone cannot establish controlled local-track availability, GATT acknowledgement timing or audible output. Overlapping exports do not inflate the sample count.

Run the analyser's synthetic checks with `python -m unittest discover -s scripts/tests -p test_connection_reports.py -v`. Keep raw exports and generated summaries under ignored `artifacts/`, with sanitized device/build/test conditions in `docs/test-results/`.
