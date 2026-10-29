# Windows PC diagnostic central

This development tool exercises the DX340 host over a real PC Bluetooth radio. It is not a product remote or a substitute for Android/iPhone acceptance. Tested PC/driver and precise verification scope: test-results/2026-10-29-windows-ble.md. The everyday control path remains native phone Bluetooth, with existing DX340 apps playing the audio.

Resumed discovery update: the host now includes the existing Android Bluetooth name in a separate scan response when it fits. This DX340 is observed as **DX340**, not DX340 Remote. Name is for finding the device only; authenticated bond and identified local approval still decide access. The initial programmatic native request failed without a bond; a present operator is using normal Windows Bluetooth Settings. See test-results/2026-10-29-named-advertising.md for build/over-air/update evidence and remaining limits.

## Bootstrap and unpaired checks

From the repository root, use Python3.11 on this Windows11 PC. If the ignored environment already exists, use it rather than overwriting a global installation:

```powershell
python -m venv work/ble-diagnostic
./work/ble-diagnostic/Scripts/python.exe -m pip install -r scripts/windows-ble-requirements.txt
./work/ble-diagnostic/Scripts/python.exe -m pip check
./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-ble.py scan --rounds 5 --seconds 3 --label awake-discovery
./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-ble.py unpaired-probe --seconds 3 --label unpaired-attributes
```

The scanner filters the custom service UUID and records fresh watcher events, first-seen times and RSSI without logging addresses. The unpaired probe refuses an already paired target and never removes a bond. It checks uncached service layout and the expected0x05 denials for INFO/CCC/RX/subscription, then disconnects. Reports stay under artifacts/windows-ble/. An unexpected success/error is retained as failure, not counted as authentication proof.

For a screen-off sample, put DX340's screen off normally, record dumpsys power before/after, run a separately labelled scan and restore wake state. Keep USB-powered Dozing, actual asleep/deep idle, connected-state survival and physical phone results separate. Short discovery scans do not prove background command delivery.

## Native authenticated pairing, operator required

The operator must be present for Windows and DX340 system consent. Open the companion on DX340 and press Open pairing for60 seconds. Then run:

```powershell
./work/ble-diagnostic/Scripts/python.exe scripts/request-windows-pairing.py
```

The script selects exactly one matching service advertisement, requests native encryption with authentication and leaves all native dialogs to the operator. Compare the actual pairing codes on both devices when offered and reject a mismatch or unrelated target. It reports Windows' resulting bond/protection properties under artifacts/windows-ble/native-pairing-result.json. This script is prepared/syntax checked; its successful pairing has not been verified here. The preceding custom request failed without a code callback. No Just Works/encryption-only fallback is implemented.

If a bond already exists, it is retained and inspected. If the result is unknown or times out, inspect both OS bond states before another request. Do not uninstall/reset apps, delete other bonds, edit permissions or weaken host protection to obtain a passing test.

## Read-only protocol checks after pairing

The prepared scripts/verify-windows-protocol.py refuses an unpaired/unknown/weaker Windows bond before any explicit GATT connect/characteristic operation. It never requests pairing, approves/revokes a controller, changes source or sends playback/volume commands. Nineteen software conformance/failure checks pass; the actual authenticated exchange is NOT TESTED. A live preflight on29 October stopped with AUTHENTICATED_OS_BOND_REQUIRED, paired=false/protection=NONE, zero protocol frames.

After native pairing is verified, reopen the companion's60second pairing window while this PC has no companion approval. Run:

```powershell
./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-protocol.py approval-boundary --label before-local-approval
```

Expect protected INFO/indication access and HELLO, followed by LOCAL_APPROVAL_REQUIRED for AUTHORIZE and AUTHORIZATION_DENIED for GET_STATE, without metadata. Do not approve during this denial run. Existing approved PCs must not be counted as unapproved; the script will fail rather than remove their approval. Disconnect cleanup runs when the command exits. A successful attribute operation alone is not a passing protocol check.

Disconnect clears the host's pending request. To approve this PC, reopen the60second pairing window and use a fresh held connection:

```powershell
./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-protocol.py await-approval --label local-approval-and-state
```

Review the identified pending request under Controller access on DX340 and tap Approve there. This mode polls AUTHORIZE every two seconds for up to45seconds; it performs no approval itself. After success it reads state and repeats the identical GET_STATE request ID to check the cached read result. It never repeats a playback action. The whole run is bounded and disconnects before another test. If no approval is observed, it fails; do not infer acceptance from a timeout. Then a fresh connection can check the saved approval:

```powershell
./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-protocol.py authorized-state --label saved-approval-state
```

Reports exclude Bluetooth addresses and music labels; fixed check results/error codes remain under artifacts/windows-ble/. They include explicit failure/cleanup status. OS bond/protection reporting, companion approval and live authorization are distinct; the tool checks all three but has not completed these physical steps here. Native authenticated pairing, revocation during a pending request, same-peer callback ordering, another real controller's retention and the complete phone/security/reliability matrix remain open. Physical phones must complete their own runbook.

Software-only checks can be repeated without a radio or Windows dependencies using Python3.11+:

```powershell
./work/ble-diagnostic/Scripts/python.exe -m unittest discover -s scripts -p test_windows_ble_protocol.py -v
```

See test-results/2026-10-29-windows-protocol-preparation.md for scope and evidence. Bonding/consent must be completed by the present operator; neither tool accepts native dialogs.

Primary references: [Bleak scanner](https://bleak.readthedocs.io/en/stable/api/scanner.html), [Bleak Windows backend](https://bleak.readthedocs.io/en/latest/backends/windows.html), [Windows BLE device/uncached discovery](https://learn.microsoft.com/en-us/uwp/api/windows.devices.bluetooth.bluetoothledevice.frombluetoothaddressasync), [Windows pairing/protection levels](https://learn.microsoft.com/en-us/uwp/api/windows.devices.enumeration.deviceinformationpairing.pairasync). Windows objects/service enumeration and a requested protection level are not by themselves proof of authenticated application traffic.
