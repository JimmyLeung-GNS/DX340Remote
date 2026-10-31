# Connection reports

The shared phone app has a deliberate troubleshooting action in **Setup → Connection help**: **Save connection report** on Android or **Share connection report** on iPhone. This does not connect, authorize, retry, change playback or adjust volume. The regular UI snapshot does not include the journal, so collecting it adds no library/queue rebuild notifications.

History belongs to the current native model instance and is cleared by process restart. Each report uses a new random model instance identifier for safe correlation across multiple exports; it is not a device, Bluetooth or controller identity. The default limit is 256 requests and 256 connection transitions. Eviction counters expose missing earlier history.

Format version 1 contains allowlisted operation/source/error/connection categories, request IDs and relative monotonic milliseconds. Unknown categories become `OTHER`/`other`. Music metadata, command arguments, item IDs, paths, artwork, Bluetooth identities, host epochs and credentials are excluded. App/OS versions and generic device model identify the software environment. Android additionally hashes its own installed base APK, outside the BLE callback thread. No source storage is scanned.

Each request records `startedMs`, `accepted`, nullable `acceptedAfterMs`, nullable `finishedAfterMs`, `result`, `terminalApplied`, `hostOutcome` and nullable `error`. A duplicate/late reply cannot overwrite its first receipt. Unknown or changed-host replies are rejected before journaling. Clock regression produces null, never a fabricated duration. Timings start at native send registration, before encoding/queued writes, and end at a matched decoded reply after reassembly. They are **not** a GATT frame acknowledgement, UI-tap measurement or acoustic measurement.

`ACCEPTED` means the host accepted the request. A terminal receipt can still be ignored by context/payload guards; `terminalApplied` becomes true only after the native client processes it. `hostOutcome=dispatched` is distinct from `observed`. Neither proves audible playback. Timeout, send failure, connection loss and session reset finalize unfinished entries without resending operations.

Android creates a unique app-owned JSON file under `Download/DX340Remote` with MediaStore, publishing only after a successful bounded write. It requests no additional storage permission. This follows [Android shared-media guidance](https://developer.android.com/training/data-storage/shared/media). iPhone prepares at most three own temporary JSON files and opens the native [activity controller](https://developer.apple.com/documentation/uikit/uiactivityviewcontroller/), anchored for iPad. The notice says to choose a destination; presentation is not a claim that the owner saved or shared a file. No destination or recipient is chosen automatically.

The report is at most 256 KiB. Native export is single-flight; a failure permits another deliberate attempt. The shared UI keeps report progress/errors separate from connection/source status.

## Verification and limits

The Kotlin and Swift journals consume the same six fixtures for duplicate/late replies, privacy categories, lost accepted mutations, bounded eviction, clock regression and duplicate registration. Sustained-operation tests cover bounded memory; Android JSON tests check explicit nulls survive serialization. Flutter tests check explicit invocation, single-flight, failure/retry, disposal and the collapsed-help action on both platform styles.

The iOS CI developer-only `report_export_probe.dart` uses the real native bridge and UIKit with no BLE/source fixture, correlates a unique launch marker and native model instance, checks an empty request history and the strict report keys, then captures the share sheet. The existing EXIT trap rebuilds/reinstalls/relaunches ordinary `main.dart`, including after failure. This probe is never imported by the production entrypoint.

Builds, simulator exports and fixture timings do not pass T04. Physical exports from the actual phones, decoded ACCEPTED versus terminal/applied outcomes, source-state observation and listening remain separate required evidence. Do not relabel old physical results as results for a newly built candidate.
