"""Read-only Windows BLE checks after operator-managed authenticated OS pairing.

No pairing, controller approval/revocation, playback, source change or volume operation.
Run approval-boundary before local approval; await-approval holds a fresh connection for
45seconds by default (--approval-wait30..180) while the operator approves on DX340.
This client wait does not extend a host security deadline. authorized-state checks a saved approval.
authorized-queue additionally reads all pages of the selected paused source's public
queue, cached duplicates and stale revision rejection. It never selects a source.
"""
import argparse
import asyncio
from datetime import datetime, timezone
import importlib.metadata
import json
from pathlib import Path
import platform
import re

from bleak import BleakClient, BleakScanner
from bleak.exc import BleakGATTProtocolError
from winrt.windows.devices.bluetooth import BluetoothAdapter, BluetoothLEDevice
from winrt.windows.devices.enumeration import DeviceInformation
from windows_ble_protocol import (SERVICE, INFO, RX, TX, MAX_MESSAGE, ReadOnlySession,
                                  decode_object, require_authenticated_bond, run_read_only_checks, run_queue_checks, diagnostic_failure)

OUT = Path(__file__).resolve().parents[1] / 'artifacts' / 'windows-ble'


async def inspect_bond(device):
    native = await asyncio.wait_for(BluetoothLEDevice.from_bluetooth_address_async(int(device.address.replace(':', ''), 16)), 10)
    if native is None:
        raise ValueError('TARGET_UNAVAILABLE')
    try:
        fresh = await asyncio.wait_for(DeviceInformation.create_from_id_async(native.device_information.id), 10)
        return {'paired': fresh.pairing.is_paired, 'protection': fresh.pairing.protection_level.name}
    finally:
        native.close()


async def verify(args, report):
    report['phase'] = 'adapter'
    adapter = await BluetoothAdapter.get_default_async()
    if adapter is None or not adapter.is_low_energy_supported or not adapter.is_central_role_supported:
        raise ValueError('BLE_CENTRAL_REQUIRED')
    radio = await adapter.get_radio_async()
    if radio.state.name != 'ON':
        raise ValueError('BLUETOOTH_OFF')
    report['phase'] = 'scan'
    devices = await BleakScanner.discover(timeout=args.seconds, service_uuids=[SERVICE], return_adv=True)
    matching = [device for device, adv in devices.values() if SERVICE in adv.service_uuids]
    if len(matching) != 1:
        raise ValueError('EXACTLY_ONE_COMPANION_REQUIRED')
    device = matching[0]
    report['phase'] = 'bond-preflight'
    report['bondBefore'] = await inspect_bond(device)
    require_authenticated_bond(report['bondBefore']['paired'], report['bondBefore']['protection'])
    session = None
    client = BleakClient(device, services=[SERVICE], pair=False, timeout=12,
                         winrt={'use_cached_services': False},
                         disconnected_callback=lambda _: session.close() if session else None)
    try:
        report['gattConnectionAttempted'] = True
        report['phase'] = 'connect-and-enumerate'
        await asyncio.wait_for(client.connect(), 15)
        report['phase'] = 'validate-layout'
        report['negotiatedAttMtu'] = client.mtu_size
        report['gattLayout'] = [{'uuid': item.uuid, 'handle': item.handle,
                                 'characteristics': [{'uuid': char.uuid, 'handle': char.handle,
                                                      'properties': char.properties}
                                                     for char in item.characteristics]}
                                for item in client.services]
        service = client.services.get_service(SERVICE)
        if service is None or {char.uuid for char in service.characteristics} != {INFO, RX, TX}:
            raise ValueError('UNEXPECTED_GATT_LAYOUT')
        for characteristic, required in ((INFO, 'read'), (RX, 'write'), (TX, 'indicate')):
            if required not in client.services.get_characteristic(characteristic).properties:
                raise ValueError('UNEXPECTED_GATT_PROPERTIES')
        report['phase'] = 'protected-info-read'
        info = decode_object(bytes(await asyncio.wait_for(client.read_gatt_char(INFO, use_cached=False), 10)))
        if type(info.get('v')) is not int or info != {'v': 1, 'security': 'bond-mitm-and-local-approval', 'maxMessage': MAX_MESSAGE}:
            raise ValueError('UNEXPECTED_INFO_OR_METADATA')

        async def write(frame):
            report['phase'] = 'request-write'
            report['operation'] = session.operation
            await client.write_gatt_char(RX, frame, response=True)
            report['phase'] = 'reply-await'

        session = ReadOnlySession(write, allow_authorized_metadata=args.mode != 'approval-boundary',
                                  allow_queue_reads=args.mode == 'authorized-queue')
        report['phase'] = 'protected-indication-subscription'
        await asyncio.wait_for(client.start_notify(TX, lambda _, data: session.feed(bytes(data)), force_indicate=True), 10)
        report['checks'].append({'check': 'protected INFO read and indication subscription', 'passed': True})
        report['phase'] = 'read-only-protocol'
        def approval_requested():
            print(f'AUTHORIZE now requires local approval. Review the identified pending request on DX340; waiting up to {args.approval_wait} seconds. No approval is performed by this script.', flush=True)
        await run_read_only_checks(session, 'authorized-state' if args.mode in {'await-disconnect', 'authorized-queue'} else args.mode,
                                   report['checks'], approval_wait=args.approval_wait, approval_requested=approval_requested)
        if args.mode == 'authorized-queue':
            report['queueSummary'] = await run_queue_checks(session, report['checks'])
        if args.mode == 'await-disconnect':
            report['phase'] = 'await-operator-disconnect'
            print(f'Authorized read checks passed. Waiting up to{args.operator_wait}seconds for your local Revoke action on DX340; this script changes no approvals or playback.', flush=True)
            try:
                async with asyncio.timeout(args.operator_wait):
                    while client.is_connected:
                        await asyncio.sleep(0.1)
            except TimeoutError:
                raise ValueError('OPERATOR_DISCONNECT_NOT_OBSERVED') from None
            report['disconnectObserved'] = True
            report['checks'].append({'check': 'connection closed during operator action; revocation requires host/reconnect evidence', 'passed': True})
        report['phase'] = 'bond-postflight'
        report['bondAfter'] = await inspect_bond(device)
        require_authenticated_bond(report['bondAfter']['paired'], report['bondAfter']['protection'])
        report['allChecksPassed'] = True
        report['phase'] = 'complete'
    finally:
        if session:
            report['framesWritten'] = session.frames_written
            report['framesReceived'] = session.frames_received
            report['unsolicitedStatesIgnored'] = session.unsolicited_states
            session.close('DIAGNOSTIC_FINISHED')
        try:
            await asyncio.wait_for(client.disconnect(), 5)
            report['clientCleanupCompleted'] = not client.is_connected
            if client.is_connected:
                raise ValueError('DISCONNECT_NOT_VERIFIED')
        except Exception:
            report['clientCleanupCompleted'] = False
            raise ValueError('DISCONNECT_NOT_VERIFIED') from None


async def main(args):
    report = {'kind': 'Windows-BLE-read-only-protocol-diagnostic', 'mode': args.mode,
              'createdUtc': datetime.now(timezone.utc).isoformat(), 'windows': platform.platform(),
              'python': platform.python_version(), 'bleak': importlib.metadata.version('bleak'),
              'gattConnectionAttempted': False, 'framesWritten': 0, 'allChecksPassed': False,
              'checks': [],
              'playbackCommandsSent': 0, 'pairingRequested': False,
              'approvalWaitSeconds': args.approval_wait if args.mode == 'await-approval' else None,
              'scope': 'PC development diagnostic; not physical phone or audio acceptance'}
    try:
        hold = args.operator_wait if args.mode == 'await-disconnect' else args.approval_wait if args.mode == 'await-approval' else 0
        await asyncio.wait_for(verify(args, report), (180 if args.mode == 'authorized-queue' else 90) + hold)
    except Exception as error:
        # Record fixed local codes/types only; OS messages may contain device identities.
        report['allChecksPassed'] = False
        report.update(diagnostic_failure(error, int(error.code) if isinstance(error, BleakGATTProtocolError) else None))
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f'{args.label}.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report, indent=2))
    return 0 if report['allChecksPassed'] and report.get('clientCleanupCompleted') else 1


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=['approval-boundary', 'await-approval', 'authorized-state', 'authorized-queue', 'await-disconnect'])
    parser.add_argument('--seconds', type=float, default=5)
    parser.add_argument('--label', default='protocol-check')
    parser.add_argument('--operator-wait', type=int, default=120, help='Bounded30..180second hold for await-disconnect only')
    parser.add_argument('--approval-wait', type=int, default=45, help='Bounded30..180second client hold for await-approval only; host deadlines unchanged')
    options = parser.parse_args()
    if not 1 <= options.seconds <= 15 or not 30 <= options.operator_wait <= 180 or not 30 <= options.approval_wait <= 180 or not re.fullmatch(r'[a-zA-Z0-9_-]{1,48}', options.label):
        parser.error('Bounded scan1..15seconds, operator/approval waits30..180seconds and an alphanumeric/hyphen/underscore label are required')
    raise SystemExit(asyncio.run(main(options)))
