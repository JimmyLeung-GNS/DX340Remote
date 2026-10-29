"""Read-only two-sided OS bond check using the USB DX340 and current Windows radio.

Does not scan, pair/unpair, open GATT, approve a controller or control playback.
A mutual classic bond is separate from protected BLE/application authorization.
"""
import argparse
import asyncio
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess

from winrt.windows.devices.bluetooth import BluetoothAdapter, BluetoothDevice, BluetoothLEDevice
from winrt.windows.devices.enumeration import DeviceInformation


async def inspect(args):
    command = [args.adb, '-s', args.serial, 'shell']
    model = subprocess.run(command + ['getprop', 'ro.product.model'], capture_output=True,
                           text=True, encoding='utf-8', check=True, timeout=10).stdout.strip()
    if model != 'DX340':
        raise ValueError('USB_DX340_REQUIRED')
    dump = subprocess.run(command + ['dumpsys', 'bluetooth_manager'], capture_output=True,
                          text=True, encoding='utf-8', check=True, timeout=15).stdout
    address = re.search(r'^  address: ([0-9A-F:]{17})$', dump, re.MULTILINE | re.IGNORECASE)
    if address is None:
        raise ValueError('USB_BLUETOOTH_ADDRESS_UNAVAILABLE')
    adapter = await asyncio.wait_for(BluetoothAdapter.get_default_async(), 10)
    if adapter is None:
        raise ValueError('WINDOWS_BLUETOOTH_ADAPTER_REQUIRED')
    local = ':'.join(f'{adapter.bluetooth_address:012X}'[i:i+2] for i in range(0, 12, 2))
    bonded = re.search(r'^  Bonded devices:\s*\n(.*?)(?=\n[^ ]|\Z)', dump, re.MULTILINE | re.DOTALL)
    matching = []
    if bonded:
        for line in bonded.group(1).splitlines():
            entry = re.match(r'^    ([0-9A-F:]{17}) \[.*?\] (.*)$', line, re.IGNORECASE)
            if entry and entry.group(1).upper() == local:
                matching.append(entry.group(2))
    result = {'createdUtc': datetime.now(timezone.utc).isoformat(),
              'scope': 'Read-only two-sided OS inventory; not BLE protocol, controller approval or phone acceptance',
              'androidBondMatchesCurrentWindowsRadio': bool(matching),
              'androidSavedNamesForCurrentRadio': matching, 'windows': {}}
    remote = int(address.group(1).replace(':', ''), 16)
    for label, kind in [('classic', BluetoothDevice), ('bleAtPublicAddress', BluetoothLEDevice)]:
        native = None
        try:
            native = await asyncio.wait_for(kind.from_bluetooth_address_async(remote), 10)
            if native is None:
                result['windows'][label] = {'available': False}
                continue
            fresh = await asyncio.wait_for(DeviceInformation.create_from_id_async(native.device_information.id), 10)
            result['windows'][label] = {'available': True, 'name': fresh.name,
                                       'paired': fresh.pairing.is_paired,
                                       'protection': fresh.pairing.protection_level.name}
        except Exception as error:
            result['windows'][label] = {'verified': False, 'errorType': type(error).__name__}
        finally:
            if native is not None:
                native.close()
    classic = result['windows']['classic']
    result['mutualClassicBondReported'] = bool(matching) and classic.get('paired', False)
    result['windowsClassicAuthenticatedProtectionReported'] = classic.get('protection') == 'ENCRYPTION_AND_AUTHENTICATION'
    result['bleIdentityCaveat'] = 'An unavailable BLE object at the public address does not exclude a randomized BLE identity. Discover the custom service separately before GATT checks.'
    return result


async def main(args):
    try:
        report = await inspect(args)
    except Exception as error:
        known = {'USB_DX340_REQUIRED', 'USB_BLUETOOTH_ADDRESS_UNAVAILABLE', 'WINDOWS_BLUETOOTH_ADAPTER_REQUIRED'}
        report = {'mutualClassicBondReported': False, 'errorType': type(error).__name__,
                  'errorCode': str(error) if isinstance(error, ValueError) and str(error) in known else 'INVENTORY_NOT_VERIFIED'}
    out = Path(__file__).resolve().parents[1] / 'artifacts' / 'windows-ble'
    out.mkdir(parents=True, exist_ok=True)
    (out / f'{args.label}.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
    return 0 if report.get('mutualClassicBondReported') and report.get('windowsClassicAuthenticatedProtectionReported') else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--serial', default='d')
    parser.add_argument('--adb', default='adb')
    parser.add_argument('--label', default='pairing-state')
    options = parser.parse_args()
    if not re.fullmatch(r'[a-zA-Z0-9_-]{1,48}', options.label):
        parser.error('Use a bounded alphanumeric/hyphen/underscore label')
    raise SystemExit(asyncio.run(main(options)))
