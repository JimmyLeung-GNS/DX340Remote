# Storage-volume invalidation

30 October2026. Implementation source **cf3bccc11567c44371d044701a0043b700d5a1d6**. Full goal ACTIVE/incomplete. [Sanitized evidence](2026-10-30-storage-invalidation.json).

## Change

The companion now registers the public [StorageManager volume callback](https://developer.android.com/reference/android/os/storage/StorageManager#registerStorageVolumeCallback(java.util.concurrent.Executor,%20android.os.storage.StorageManager.StorageVolumeCallback)) on the main executor and unregisters it on close. A bounded64-volume tracker deduplicates known identical states, treats anonymous secondary events conservatively, reconciles the startup registration gap and retains event order. Android supplies an event-time snapshot, so the callback consumes that snapshot rather than replacing it with the latest mounted state; otherwise a fast unmount/remount could be missed. [Android callback contract](https://developer.android.com/reference/android/os/storage/StorageManager.StorageVolumeCallback#onStateChanged(android.os.storage.StorageVolume)).

A boundary retires both source epochs before cancelling requests, clears native items/proofs/page caches/revision baselines/root probes, invalidates both assisted UI frames and Apple's downloaded-context flag, cancels pending browsing and resets volume verification. Late native callbacks cannot repopulate the old context. Media sessions, playback, controller grants and the BLE transport are retained. No filesystem scan, storage permission, mount/unmount, file modification or player replacement is added. Diagnostics contain status/counts, not volume UUIDs/paths. If monitor setup fails, arbitrary library/queue selection is disabled with an explanation; that failure branch has not been physically injected.

## Built and installed combination

| Component | Evidence |
|---|---|
| DX340 | Android13/API33, DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys; Mango4.1.37/code240, Apple6.5.3/code1599 |
| New companion | Original-key in-place upgrade; sourcecf3bccc; installed/pulled SHA256 **58314b84cd45447fd866c1ea9a48922c39edd9584f14100c47cd3cb8c91ee97e** |
| Preserved baseline | Previous companion6c390636… saved under artifacts/storage-boundary-upgrade/previous-companion-6c.apk; earlier audio results remain on their exact packages |
| Controller | Google Pixel9a Android16/API36 CP1A.260405.005; unchanged shared0.2.0/code2/source587ea78 APK **111a29f854a8038613c3818ace5948020f8516842b7b57ba5df22e6baeaa0c6d** |
| Development signer | Public certificate SHA25624f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d; pre/post signing and installed bytes verified; key stays outside Git |

## Separate verification scopes

| Scope | Result |
|---|---|
| Software | VERIFIED159 aggregate JVM tests, zero failures/errors, host assembly/lint. Seven new volume-model cases plus one revision-baseline case cover rapid same-UUID unmount/remount, startup gap, replacement identity, unknown UUIDs, bounded eviction, oversized snapshots and shutdown callbacks. These are simulated events |
| Actual Android registration | VERIFIED diagnostic ACTIVE,2 retained public volumes, callbackEvents0. Both Accessibility services bound/no crashed services; host foreground. This proves setup returned successfully, **not actual SD-event delivery** |
| Actual source adapters, synthetic boundary | VERIFIED scripts/verify-storage-invalidation.ps1:35 paused source requests. Held native item proof/page, both actual UI item tokens and old Apple queue continuation reject. Both UI frames/downloaded context clear; fresh native and assisted roots recover. Pending native/Mango-UI/Apple-UI browsing each cancels with STALE_LIBRARY and fresh recovery passes. No BLE in this35-request scope and no SD event/playback/volume/file/grant change |
| Physical Pixel BLE, synthetic boundary | VERIFIED saved-grant connection to583… without new approval; native album rows visible, then removed after synthetic source-epoch retirement pushed over BLE; fresh root reload succeeds. Separate23-item Mango queue populated, cleared after another boundary, then refresh returns1–23 of23. UI XML and before/after screenshots inspected. This proves phone invalidation/recovery, **not physical SD removal** |
| Preservation | VERIFIED four companion preference digests unchanged across original-key upgrade/checks, including Pixel/JIMMYAH approvals; no pairing/revocation. Both source sessions remain paused37, Mango target, volume proof false. Phone explicitly disconnected and its Wi-Fi setting restored1; mobile-data/Bluetooth remain1/1. No playback/volume command or new audio confirmation |

Commands at the committed source:

```powershell
# Supply this PC's real JDK/SDK and original key through process-local variables.
cd android
./gradlew.bat --no-daemon :host-app:assembleDebug :host-app:lintDebug :core-protocol:test :library-cache:test :source-adapters:testDebugUnitTest
cd ..
./scripts/verify-android-signing.ps1 -Serial d -Package org.dx340.remote.host -Apk android/host-app/build/outputs/apk/debug/host-app-debug.apk
# After preserving the installed baseline, install -r, open companion and Start remote.
./scripts/verify-storage-invalidation.ps1 -Serial d
```

Final diagnostic: ACTIVE, callbackEvents0, libraryInvalidations6 (four source-only synthetic boundaries plus two phone-observed synthetic boundaries),2 retained volumes, both UI frames absent/not busy, Apple offlineContext=false. The distinction between6 invalidations and0 real callback events must survive the handoff.

Raw ignored evidence: artifacts/storage-boundary-{committed-build.txt,test-command.txt,upgrade/,pixel/} and artifacts/storage-boundary-probes/storage-20261030-233739-c60ef/. The phone's Wi-Fi was temporarily off/no active default network, but the earlier legacy cellular-setting ambiguity remains; do not promote this to full T01. The first Pixel **audible** selection belongs to companion6c…/phone111… in [its own report](2026-10-30-pixel-first-bluetooth.md), not this new583… build.

## Remaining physical proof and UI follow-up

Actual SD removal/reinsertion, source rescan and file changes remain **NOT TESTED** for this implementation. With owner participation, pause both sources, confirm which removable card is being tested, retain current native/folder/queue references, safely eject using Android Settings, then have the owner remove/reinsert the card. Observe actual callbackEvents/source epochs/phone rows, reject held references, re-browse and confirm original files unchanged. Do not send protected fake OS broadcasts or use root/firmware changes as substitutes. Separate source-rescan tests are needed even if mount events pass.

The physical Pixel safely removes old rows, but its existing status still says “Library loaded”/“Queue loaded” afterward. Correct that refresh guidance across native phone models and verify shared UI behavior; it is not a completed presentation review. Full phone format/Apple outside-queue audio,30-minute/background/range/latency, signed owner iPhone, volume/output, battery/walking, two-controller handoff and Watch remain open.
