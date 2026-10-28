"""Request native authenticated pairing with DX340. A present operator handles OS dialogs.

Run only when the operator is ready and the companion's60second pairing window is open.
This script never accepts a consent/code dialog, falls back to weaker protection, changes
radio settings, removes existing bonds, approves a controller or sends playback commands.
Successful Windows pairing is separate from companion approval and phone acceptance.
"""
import asyncio
from datetime import datetime, timezone
import json
from pathlib import Path

from bleak import BleakScanner
from winrt.windows.devices.bluetooth import BluetoothLEDevice
from winrt.windows.devices.enumeration import DeviceInformation, DevicePairingProtectionLevel

SERVICE = 'e7e01000-4a76-4b53-9c8f-c7a12b00d340'
OUT = Path(__file__).resolve().parents[1] / 'artifacts' / 'windows-ble'

async def main():
    report = {'kind': 'native-Windows-pairing-request-operator-consent-required',
              'createdUtc': datetime.now(timezone.utc).isoformat(), 'requestedProtection': 'ENCRYPTION_AND_AUTHENTICATION'}
    native = None
    try:
        devices = await BleakScanner.discover(timeout=5, service_uuids=[SERVICE], return_adv=True)
        if len(devices) != 1:
            raise RuntimeError(f'Expected one matching companion advertisement, found {len(devices)}')
        device = next(iter(devices.values()))[0]
        native = await BluetoothLEDevice.from_bluetooth_address_async(int(device.address.replace(':', ''), 16))
        if native is None:
            raise RuntimeError('Target unavailable through WinRT')
        info = await DeviceInformation.create_from_id_async(native.device_information.id)
        report['pairedBefore'] = info.pairing.is_paired
        if info.pairing.is_paired:
            report['pairingStatus'] = 'ALREADY_PAIRED_NO_CHANGE'
        else:
            print('Complete the native Windows and DX340 pairing dialogs; compare any codes. No dialog is accepted by this script.', flush=True)
            result = await asyncio.wait_for(info.pairing.pair_async(DevicePairingProtectionLevel.ENCRYPTION_AND_AUTHENTICATION), timeout=75)
            report['pairingStatus'] = result.status.name
            report['reportedProtectionUsed'] = result.protection_level_used.name
        fresh = await DeviceInformation.create_from_id_async(native.device_information.id)
        report['pairedAfter'] = fresh.pairing.is_paired
        report['freshProtectionLevel'] = fresh.pairing.protection_level.name
        report['authenticatedBondReported'] = fresh.pairing.is_paired and fresh.pairing.protection_level == DevicePairingProtectionLevel.ENCRYPTION_AND_AUTHENTICATION
    except Exception as error:
        # Device addresses and system device IDs are deliberately excluded from reports.
        report['errorType'] = type(error).__name__
        report['outcome'] = 'Not verified; inspect native dialogs and current OS bond state before retrying'
    finally:
        if native is not None:
            native.close()
        OUT.mkdir(parents=True, exist_ok=True)
        (OUT / 'native-pairing-result.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
        print(json.dumps(report, indent=2), flush=True)
    return 0 if report.get('authenticatedBondReported') else 1

if __name__ == '__main__':
    raise SystemExit(asyncio.run(main()))
