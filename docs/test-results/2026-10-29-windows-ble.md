# Real Windows central and DX340 BLE host checks

29 October 2026. **VERIFIED real over-air discovery, uncached GATT enumeration and unpaired attribute denial. Authenticated exchange and both phone paths remain NOT VERIFIED.**

Windows PC: Microsoft Windows11 Pro10.0.26300, Intel Wireless Bluetooth, driver24.20.0.3 (18 December2025), radio already On. WinRT reports BLE/central support. An isolated work/ble-diagnostic environment uses Python3.11.5, Bleak3.0.2 and the versions in scripts/windows-ble-requirements.txt. Global Python/Flutter/Java and radio settings were not changed.

DX340 d: Android13/API33, build eng.iBasso.20260812.181952; Mango4.1.37/code240, Apple Music6.5.3/code1599. Existing companion0898571b... and phone09aaae78... unchanged. Music stayed paused at volume35. DX340 Wi-Fi0/default network:none; the PC's other network configuration was left as found. No Internet request is part of the BLE diagnostic; package installation used the development PC's existing connection.

## Over-air results

The PC received the companion's advertised service e7e01000-4a76-4b53-9c8f-c7a12b00d340. Its local name was absent in the scan response; discovery used the service UUID. The unpaired PC then reported a live GATT connection and enumerated the custom service uncached: INFO/read, RX/write and TX/indicate with its CCC descriptor. This improves the previous advertising-callback-only evidence. Public service enumeration is not authorization to read source metadata or control playback.

| Scan state | Fresh watcher rounds | Duration per round | Detected | First advertisement times |
|---|---:|---:|---:|---|
| DX340 awake |5|3seconds|5/5|1,406 /422 /407 /781 /1,000ms|
| DX340 screen off |5|3seconds|5/5|1,703 /1,390 /297 /828 /1,859ms|

Each watcher saw2–9 matching advertisement events. Observed RSSI ranged-84 to-69dBm. The target was USB-powered in the existing workspace; neither bag distance nor range was measured. Both screen-off power snapshots reported mWakefulness=Dozing and mIsPowered=true. This is a short screen-off/dozing advertising check, **not deep idle, a30-minute connection, phone discovery or one-hour walking acceptance**. The test restored wake/unlock afterward.

## Unpaired security checks

scripts/verify-windows-ble.py unpaired-probe passes on the real PC/DX340 link. Windows reported unpaired before and after. All four operations returned GATT protocol error0x05, Insufficient Authentication:

- INFO uncached read.
- CCC uncached read.
- RX write with response, containing only the first20-byte fragment of an incomplete message.
- CCC enable-indications write.

The RX probe cannot complete or dispatch a logical command even if unexpectedly accepted. No playback or source metadata request was sent. Client disconnect/cleanup completed. This proves the observed unauthenticated attribute boundary on this pair of devices. It does not prove MITM negotiation, bonded-but-unapproved behavior, application authorization, revocation, reconnection or production security.

## Authenticated pairing attempt and operator step

After the companion's visible60second pairing-window button was activated, a development WinRT custom pairing request explicitly required ENCRYPTION_AND_AUTHENTICATION and a ConfirmPinMatch ceremony. It failed with status FAILED, protection NONE, no comparison-code callback and no resulting Windows bond. Android logged a pairing request and a pairing notification, followed by BOND_BONDING →BOND_NONE after roughly30seconds. No weaker fallback was attempted. The notification/native-consent behavior is a follow-up; the cause of the failed authenticated attempt is not established by these observations alone.

Windows documents that [custom pairing involves native system consent on desktop](https://learn.microsoft.com/en-us/windows/apps/develop/devices-sensors/pair-devices). The current computer-use guidance prohibits automating user authentication dialogs, so native pairing consent is handed to the present operator. The user's availability question is pending. No native Windows consent was accepted and no companion controller was approved.

scripts/request-windows-pairing.py now prepares a basic native request targeting the sole advertisement with our service UUID and requiring ENCRYPTION_AND_AUTHENTICATION. It never accepts dialogs, removes old bonds or falls back to weaker protection. **Syntax checked only; its native pairing success has NOT been tested.** Run it only when the operator is ready to handle both devices' prompts and the companion pairing window is open. See windows-ble-diagnostics.md. Do not automatically repeat an unknown/timeout result; inspect current native state first.

## Evidence and continuity

Raw ignored artifacts/windows-ble/: scan.json, awake-discovery.json, screen-off-discovery.json, unpaired-attributes.json, pair-result.json, host-pairing-window.json, filtered pairing notification/logs, Windows/driver/dependency inventory, power snapshots and final source/services/preferences captures. Bluetooth addresses/device identifiers remain in ignored private files only. Reports omit them.

Final state: both sessions observed paused, target Mango; four companion preference files byte-identical to the preceding baseline, both optional Accessibility services bound with no binding/crashed entries. Windows remains unpaired; no new companion approval exists. Product APKs unchanged; preceding native build/lint79 tests and Flutter32/release/lint retain their scope. pip check and syntax checks for both Python diagnostics pass. No redundant product build was run.

The source/format/listening matrices, Mango Playlist recovery above3seconds, Mac/current iOS signing/build, real Android/iPhone BLE/authentication/background/range/latency/battery/walk/install and Watch gates remain open. A Windows diagnostic does not replace either phone acceptance.
