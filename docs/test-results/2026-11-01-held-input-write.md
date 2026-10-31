# Pending input-write retirement and slow fallback recovery

**VERIFIED:** an actually unacknowledged input write retires without dispatch/replay; the debug host fallback logically retires its held application link and requests disconnection after 5,002ms. This is not immediate physical ACL teardown proof. **FAILED:** the fallback observation did not reach Ready within its 23-second window. **VERIFIED later:** the same attempt eventually reauthorizes automatically, 81,526ms after logical retirement. That recovery delay remains unresolved. [Structured result](2026-11-01-held-input-write.json). Full acceptance remains incomplete.

## Installed pair and implementation

DX340 Android13/API33 `eng.iBasso.20260812.181952`, Companion0.1.0/code1, source `5f4f1f24a673f778d7b403203e35f3e48edf4b8a`, installed SHA256 `3a32689ac5805cf58758286f27ee1c1b2abeec16f01b8de70b5ac9cfe8307b2c`, PID16085. Pixel9a Android16/API36 `CP1A.260405.005`, Remote0.2.0/code2, unchanged source `d06a481b4eb533b42d80331203891f8f62a342ab`, installed SHA256 `a65ac6a9660223532a23f0eaf828dc62282759d1e66e04dba66fb10a2704622a`, PID4427. Mango4.1.37/code240 and Apple Music6.5.3/code1599 remain installed. The original signing certificate matches before the in-place Companion upgrade; final installed bytes match the candidate. No uninstall or app-data reset.

The DUMP-protected debug receiver arms only a unique report tag against the exact current host epoch, an idle approved controller and both paused sources. A valid encrypted, subscribed, reassembled input frame is discarded before its ATT response and before message dispatch. It is never staged for later dispatch. The one-shot arm expires after20s; an accepted hold closes its exact server/link within5s. Peer loss, service stop and authorization reset clear the arm/hold. Independent per-hold identity prevents canceled timer callbacks from closing a later link. Existing callback omission and input-response hold modes exclude one another. Release behavior is a no-op; the merged release manifest has no `DebugUiProbeReceiver`.

This deliberately violates the normal response requirement only inside the bounded debug harness. Normal writes retain their existing ACK/dispatch path. Android documents the response obligation and public server cancellation API: [write-request callback](https://developer.android.com/reference/android/bluetooth/BluetoothGattServerCallback#onCharacteristicWriteRequest(android.bluetooth.BluetoothDevice,int,android.bluetooth.BluetoothGattCharacteristic,boolean,boolean,int,byte%5B%5D)), [GATT server](https://developer.android.com/reference/android/bluetooth/BluetoothGattServer).

## A: physical radio interruption during the pending write

The single-use Pixel watcher waits for both the native first-submission marker and a host-held marker relayed by the PC, so it cannot cut merely on queue registration. Request `6f296c3e-60ce-4061-af93-a4db87645ea6` records first submission4ms, **no ACK**, loss767ms and `terminalApplied=false`. The host records hold→disconnect863ms; no five-second fallback occurs in this case. Thus this attempt reaches the requested pending-input phase, unlike the [earlier missed-window case](2026-11-01-first-write-submission.md), which retains its ACK144ms/loss363ms and **NOT_MET** classification.

The one-shot job `472f384fe0b54766bffcaaad8694b721` is FINISHED/collected. Recorded watcher9227/logger9238 have no live process entries. Original Bluetooth-on state is restored (global1/actualON). The off snapshot has global0 but Manager `BLE_TURNING_ON`; this does not establish a fully RF-off interval. Two early missing-phase-file reads are retained in driver evidence; the same job was observed rather than relaunched. Saved Connect afterward sends only fresh HELLO/AUTHORIZE; a distinct GET_STATE returns submission3/ACK131/result430ms.

## B: bounded fallback, then unexpectedly slow automatic recovery

No radio toggle is made in B. Request `874cc889-5148-4414-916b-a39982080c20` records first submission4ms, **no ACK**, `connection_lost` after10,028ms and `terminalApplied=false`. Host clock events show:

| Event | Host timestamp | Relative to held / closed |
|---|---|---|
| Input held/discarded |1793481943785|held+0ms|
| Bounded link close |1793481948787|held+5,002ms|
| Later host connection |1793482029374|close+80,587ms|
| Saved controller approved |1793482030313|close+81,526ms|

`fallback-driver-result.json` retains its **INSPECT_EXISTING_CASE / observation failure** unchanged. Read-only inspection of that same attempt later finds Ready; no extra injection, manual Connect, host restart, pairing window or new approval was used after its Refresh tap. The client trace shows a10s write deadline, two30s service-discovery stalls with late callbacks after close, one status133, then successful HELLO/AUTHORIZE. These are observations, not a proven vendor/platform root cause. Never subtract timestamps across the two device clocks.

After eventual recovery, one deliberate new GET_STATE (`7219ca92-4568-4220-ab57-5cc2485ef30f`) succeeds at submission4/ACK131/result379ms. This proves fresh read recovery; it does not erase the failed short observation or establish fast reconnect, T06, full T12 or T16 acceptance. Next investigation: the two stalled discovery attempts and cleanup of an abandoned ATT procedure using supported public APIs, without blind replay or private GATT refresh.

## Journal, builds and preservation

Native model `a6d211e5-d892-4ae6-a416-ebe05ff24768` grows9→10→13→16→17. Every preceding row is unchanged, zero evictions. The eight new rows are two retired GET_STATEs, four fresh HELLO/AUTHORIZEs and two explicit successful GET_STATEs. No playback/source/volume command was sent. Neither interrupted read is applied or replayed.

`scripts/build-android.ps1` passes native assembly/lint and248 aggregate JVM tests (183 Android debug +65 plain JVM; zero failures/errors), including six new held-write cases and nine retained callback-fault cases. Host release assembly/lint and20 release tests also pass; release counts are a separate variant scope. Test XML and the merged release manifest are copied into raw evidence. The unchanged phone retains prior d06 82Flutter/22Swift/18 analyser scopes. Both exactd06 Apple jobs remain closed SUCCESS/archive-verified simulator evidence; this host-only change starts no new Apple job.

Final05:35:43 HKT: both sessions PAUSED2, volume37/headset(4), volume proof false, source states/positions exactly preserved. All four preference hashes, settings and PIDs match the pre-radio capture; saved grants/bonds are retained, both optional modes and Accessibility bindings remain on. Phone uses ordinary Disconnect/help collapsed, foreground service cleared. Host saved enabled/foreground service/advertising; the source UI may remain foreground. SD volume `public:179,1` stays mounted as `FBB9-E8FE`; no storage location or mount action. Both Wi-Fi0, Pixel mobile_data1 preserved; strict cellular-off T01 stays open.

Raw evidence: ignored `artifacts/held-input-write-20261101/`,337 files hashed in `sha256-manifest.json`; digest in the structured report. Earlier frozen reports/raw evidence are unchanged. Original no-card Mango Internal timeout, Apple MEDIA_MOUNTED crash, earlier failed holds and full source/format/output/sleep/range/calls/battery/walking/owner-iPhone/Watch gates remain open.

## Subsequent scope clarification

The [callback-ordered cleanup follow-up](2026-11-01-gatt-retirement.md) confirms that the host fallback marker measures application-link retirement plus a public disconnect request, not immediate physical ACL teardown. Its new phone still takes81,580ms to saved approval versus81,526ms here. Preserve the original failure and timing; neither case proves fast recovery.

## Routine playback checks

On1 November the owner requested direct screen-based playback checks without a listening question after each selection. Future checks inspect the actual track, playback state/indicator and advancing position. A Play action icon alone can represent a paused player. Record observed playback separately from headphone audibility/quality. The three earlier unanswered questions are no longer a development dependency; their hearing evidence remains **NOT TESTED**, with no retrospective audible-pass claim.
