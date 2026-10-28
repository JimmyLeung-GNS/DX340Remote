# Windows PC diagnostic central

This development tool exercises the DX340 host over a real PC Bluetooth radio. It is not a product remote or a substitute for Android/iPhone acceptance. Tested PC/driver and precise verification scope: test-results/2026-10-29-windows-ble.md. The everyday control path remains native phone Bluetooth, with existing DX340 apps playing the audio.

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

After a verified authenticated bond, a separate protocol diagnostic still needs to subscribe, exchange HELLO, verify LOCAL_APPROVAL_REQUIRED before companion approval, approve the identified pending controller visibly, authorize and read state. It must disconnect before another controller test. Bonding, application approval and a live authorized session are distinct. That exchange is not yet verified; do not treat pair status alone as completing P1 or T15. Physical phones must complete their own runbook afterward.

Primary references: [Bleak scanner](https://bleak.readthedocs.io/en/stable/api/scanner.html), [Bleak Windows backend](https://bleak.readthedocs.io/en/latest/backends/windows.html), [Windows BLE device/uncached discovery](https://learn.microsoft.com/en-us/uwp/api/windows.devices.bluetooth.bluetoothledevice.frombluetoothaddressasync), [Windows pairing/protection levels](https://learn.microsoft.com/en-us/uwp/api/windows.devices.enumeration.deviceinformationpairing.pairasync). Windows objects/service enumeration and a requested protection level are not by themselves proof of authenticated application traffic.
