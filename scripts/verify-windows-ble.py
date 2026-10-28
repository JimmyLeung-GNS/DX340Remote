"""Development-only Windows radio checks. No pairing, approval or playback operation."""
import argparse
import asyncio
from datetime import datetime, timezone
import importlib.metadata
import json
from pathlib import Path
import platform
import re
import struct
import time

from bleak import BleakClient, BleakScanner
from bleak.exc import BleakGATTProtocolError
from winrt.windows.devices.bluetooth import BluetoothAdapter, BluetoothLEDevice

SERVICE = 'e7e01000-4a76-4b53-9c8f-c7a12b00d340'
INFO = 'e7e01001-4a76-4b53-9c8f-c7a12b00d340'
RX = 'e7e01002-4a76-4b53-9c8f-c7a12b00d340'
TX = 'e7e01003-4a76-4b53-9c8f-c7a12b00d340'
OUT = Path(__file__).resolve().parents[1] / 'artifacts' / 'windows-ble'

def safe_error(error, address=''):
    text = f'{type(error).__name__}: {error}'
    if address:
        for value in (address, address.replace(':', ''), address.replace(':', '-')):
            text = re.sub(re.escape(value), '<target>', text, flags=re.IGNORECASE)
    return re.sub(r'(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}', '<address>', text)

async def scan(rounds, seconds):
    samples = []
    for index in range(rounds):
        started = time.monotonic()
        seen = []
        addresses = set()
        def detected(device, advertisement):
            if SERVICE in advertisement.service_uuids:
                addresses.add(device.address)
                seen.append((time.monotonic(), advertisement.rssi))
        async with BleakScanner(detection_callback=detected, service_uuids=[SERVICE]):
            await asyncio.sleep(seconds)
        samples.append({'round': index + 1, 'matchingDevices': len(addresses), 'receivedEvents': len(seen),
                        'firstSeenMs': round((seen[0][0] - started) * 1000) if seen else None,
                        'rssiMin': min(value[1] for value in seen) if seen else None,
                        'rssiMax': max(value[1] for value in seen) if seen else None})
    return {'samples': samples, 'detectedRounds': sum(bool(row['receivedEvents']) for row in samples),
            'allDetected': all(row['receivedEvents'] for row in samples)}

async def probe(seconds):
    devices = await BleakScanner.discover(timeout=seconds, service_uuids=[SERVICE], return_adv=True)
    if len(devices) != 1:
        raise RuntimeError(f'Expected one matching companion advertisement, found {len(devices)}')
    device = next(iter(devices.values()))[0]
    async def pairing_state():
        native = await BluetoothLEDevice.from_bluetooth_address_async(int(device.address.replace(':', ''), 16))
        if native is None:
            raise RuntimeError('WinRT target unavailable')
        try:
            return native.device_information.pairing.is_paired
        finally:
            native.close()
    paired_before = await pairing_state()
    if paired_before:
        raise RuntimeError('Unpaired probe requires an unpaired target; existing bonds are never removed')
    client = BleakClient(device, services=[SERVICE], pair=False, timeout=12, winrt={'use_cached_services': False})
    result = {'pairedBefore': paired_before, 'checks': []}
    try:
        await asyncio.wait_for(client.connect(), timeout=15)
        result['connected'] = client.is_connected
        service = client.services.get_service(SERVICE)
        if service is None or {char.uuid for char in service.characteristics} != {INFO, RX, TX}:
            raise RuntimeError('Expected companion GATT layout not found')
        result['characteristics'] = [{'uuid': char.uuid, 'properties': char.properties} for char in service.characteristics]
        tx = client.services.get_characteristic(TX)
        ccc = next(item for item in tx.descriptors if item.uuid.startswith('00002902-'))
        async def check(label, operation):
            try:
                value = await asyncio.wait_for(operation(), timeout=10)
                result['checks'].append({'attribute': label, 'deniedWithInsufficientAuthentication': False,
                                         'unexpectedSuccess': True, 'returnedBytes': len(value) if value is not None else 0})
            except Exception as error:
                expected = isinstance(error, BleakGATTProtocolError) and int(error.code) == 5
                result['checks'].append({'attribute': label, 'deniedWithInsufficientAuthentication': expected,
                                         'gattError': int(error.code) if isinstance(error, BleakGATTProtocolError) else None,
                                         'error': safe_error(error, device.address)})
        await check('INFO uncached read', lambda: client.read_gatt_char(INFO, use_cached=False))
        await check('CCC uncached read', lambda: client.read_gatt_descriptor(ccc.handle, use_cached=False))
        # One20-byte first fragment cannot dispatch a complete message if unexpectedly accepted.
        fragment = struct.pack('>BBHHH', 0x44, 1, 7, 0, 2) + b'{"v":1,"id":'
        await check('RX first-fragment write', lambda: client.write_gatt_char(RX, fragment, response=True))
        await check('CCC enable indications', lambda: client.start_notify(TX, lambda sender, data: None))
        result['pairedAfter'] = await pairing_state()
        result['allDeniedWithoutBond'] = all(item['deniedWithInsufficientAuthentication'] for item in result['checks']) and not result['pairedAfter']
    finally:
        await asyncio.wait_for(client.disconnect(), timeout=5)
        result['clientCleanupCompleted'] = True
    return result

async def main(args):
    OUT.mkdir(parents=True, exist_ok=True)
    report = {'kind': f'Windows-BLE-{args.mode}-no-pairing-no-playback', 'createdUtc': datetime.now(timezone.utc).isoformat(),
              'python': platform.python_version(), 'windows': platform.platform(), 'bleak': importlib.metadata.version('bleak'),
              'service': SERVICE, 'requestedRounds': args.rounds, 'scanWindowSeconds': args.seconds}
    try:
        adapter = await BluetoothAdapter.get_default_async()
        if adapter is None:
            raise RuntimeError('No WinRT Bluetooth adapter')
        radio = await adapter.get_radio_async()
        report['adapter'] = {'lowEnergySupported': adapter.is_low_energy_supported,
                             'centralRoleSupported': adapter.is_central_role_supported, 'radioState': radio.state.name}
        if not adapter.is_low_energy_supported or not adapter.is_central_role_supported or radio.state.name != 'ON':
            raise RuntimeError('Enabled BLE central required; no radio setting was changed')
        report.update(await scan(args.rounds, args.seconds) if args.mode == 'scan' else await probe(args.seconds))
    except Exception as error:
        report['error'] = safe_error(error)
    (OUT / f'{args.label}.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
    return 0 if not report.get('error') and report.get('allDetected', report.get('allDeniedWithoutBond', False)) else 1

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['scan', 'unpaired-probe'])
    parser.add_argument('--rounds', type=int, default=5)
    parser.add_argument('--seconds', type=float, default=3)
    parser.add_argument('--label', default='diagnostic')
    options = parser.parse_args()
    if not 1 <= options.rounds <= 30 or not 1 <= options.seconds <= 15 or not re.fullmatch(r'[a-zA-Z0-9_-]{1,48}', options.label):
        parser.error('Bounded rounds1..30, seconds1..15 and an alphanumeric/hyphen/underscore label are required')
    raise SystemExit(asyncio.run(main(options)))
