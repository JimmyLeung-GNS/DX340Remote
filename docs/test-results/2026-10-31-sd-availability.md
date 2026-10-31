# Unavailable SD choices and retained lifecycle failures —31 October2026

**Capability behavior VERIFIED in the stated cases; full storage/source preservation FAILED/incomplete.** [Versions, receipts, hashes and preservation](2026-10-31-sd-availability.json), [separate Apple crash/recovery evidence](2026-10-31-apple-storage-crash.md). Current original-signer Companiond1668fed/source3ad1ed3 and unchanged Pixeld75540a5/source80d4edc were pulled and hash-matched.

## Reproduced problem and change

With old hostf009, Android reported the single SD volume unmounted. The phone cleared its old rows but still offered an enabled SD Card choice. One request then returned UI_ACTION_TIMEOUT after10423ms, with generic screen guidance. The exact original volume was restored in finally.

The companion now uses [getStorageVolumes](https://developer.android.com/reference/android/os/storage/StorageManager#getStorageVolumes()) and each volume's [public state](https://developer.android.com/reference/android/os/storage/StorageVolume#getState()) to decide whether SD browsing may be attempted. With no readable removable volume, or an unavailable/oversized snapshot, the SD choice is nonbrowsable/nonplayable and explains that SD storage is unavailable. Internal Storage is not disabled by this policy. mounted and mounted_ro permit the existing guarded attempt. A readable removable volume might be USB rather than Mango's SD card; this is not a positive card-identity proof. The existing version/window/context checks remain necessary.

Cached SD actions are rechecked before validation/navigation and return the existing STALE_LIBRARY recovery code if storage is unavailable. No wire field/error, phone code, automatic mount, file access, playback action or new permission was added. The installed APK declares only Bluetooth/notifications/foreground-service permissions, with no Internet or broad storage/media permission.

## Physical cases and failures

There were three actual OS unmount/remount cycles in this investigation: old-host baseline, new-host cached-SD/foreground-start case, and new-host already-Internal case. These are **not physical removals**. Each restored the same original public volume; no scan was confirmed. The first fixed-case I/O preflight stopped before unmounting. Its sample values were not saved, so the exact failed busy condition is unproven; the later preflight recorded three stable counters/zero in-flight I/O before proceeding. This was a test guard, not an automatic approval rejection.

On d166, mounted SD is enabled. After unmount, old actions clear before a debug probe and the new home shows SD disabled with its hint. Two explicit taps on that disabled row keep the page unchanged; the exported request count contains no extra SD browse. The normal-font light Pixel screenshot was inspected; it is not a full dark/large-text/iPhone UI review.

**FAILED cached-SD start:** after foregrounding Mango's cached SD screen, choosing Internal Storage times out8412ms terminal (8245ms source trace). The trace retains an unchanged SD root and no successful storage choice. The policy leaves Internal enabled, but that navigation case is not verified usable. After remount and local cancellation of Mango's Scan SD card? prompt, fresh Internal browsing recovers4516ms. The failure remains open and is not hidden by the next case.

**VERIFIED already-Internal start:** with Mango already showing Internal Storage, another unmount leaves its Internal browsing usable (1341ms result), while SD remains disabled. A separate own-companion source-local probe obtains a current disabled SD ID/revision with stable source epoch, then receives STALE_LIBRARY in44ms. This is backend adapter evidence, not a phone BLE fault-injection pass. After remount/local scan-prompt cancellation, the phone re-enables SD and actually browses its folders4249ms. That still exceeds the proposed3s page target.

**FAILED source preservation:** Apple Music crashed during this investigation and disappeared from active media sessions. See the [dedicated crash/recovery report](2026-10-31-apple-storage-crash.md). A previous collector's assumption that both sessions always exist failed; the final collector records observed states explicitly. Mango finishes PAUSED2, Apple NONE0 after read-only recovery, volume37/headset(4), proof false. Apple NONE is not paused-playback or audio proof.

## Builds, receipts and final state

Native Android assembly/reference/probe/lint and188 JVM checks pass:55 protocol/3 controller/7 cache/123 source, including five new volume-state cases. The phone/Dart/Swift bridge is unchanged; prior80d hosted Apple jobs are closed/archive-verified with76 Flutter/19 Swift/12 capture guards, simulator only. No new Apple run was needed or polled for this Android-only source change.

The same native phone model exports78 →89 →105 →111 records with all prior request rows unchanged and zero evictions. Old-host additions:11 requests,10 results/one unavailable-SD timeout. New-host storage cases:16 requests,15 results/one cached-SD/Internal timeout. Apple recovery:6 requests,5 results/one source-scope cancellation. All new terminal records are applied, including errors; none is relabeled as a successful action. Only HELLO/AUTHORIZE/BROWSE/SELECT_SOURCE occur; no play, pause, skip or volume command.

All four saved Companion preference hashes, settings, own permission grant/flag rows and exposed bond identities are unchanged. Pixel/JIMMYAH approvals and both optional browsing modes remain. Host foreground service/both bindings are healthy, target restoredMango; normal phone Disconnect clears foreground/started work and help is collapsed. Both USB devices remain present, the card is mounted, and the one empty All Music-context fixture is absent. No source app was reinstalled, force-stopped or data-cleared. All three native scan prompts were canceled locally; no automatic dialog dismissal was added.

Keep T11/T12/source-audio and physical owner-iPhone/Mac/Watch acceptance open. Next investigate the cached-SD/Internal failure and Apple storage-event compatibility; current-build audio/long-hold tests need the pending fresh owner readiness. No user pause/device switch.
