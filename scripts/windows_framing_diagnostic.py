"""Fixed incomplete/invalid frames only; never constructs a playback command."""
import asyncio
import struct
import time


async def run_framing_bounds_checks(session, write_fault, checks, *, capacity, enabled=False,
                                    delay=asyncio.sleep, clock=time.monotonic, progress=None):
    """Explicit PC-only 65537-byte/60s probes, containing no JSON or playback."""
    if not enabled or not session.allow_authorized_metadata or not session.authorized:
        raise ValueError('AUTHORIZED_FRAMING_MODE_REQUIRED')
    if type(capacity) is not int or not 9 <= capacity <= 512:
        raise ValueError('INVALID_FRAME_CAPACITY')
    width = capacity - 8
    count = (65537 + width - 1) // width
    if count > 512:
        raise ValueError('BOUNDED_TRANSFER_CAPACITY_REQUIRED')
    progress = {} if progress is None else progress
    progress.update(status='FAILED_OR_INTERRUPTED', payloadBytes=65537, frameCapacity=capacity,
                    oversizedFrames=count, probes=[], sameSessionRecoveries=0)
    before = await session.exchange('GET_STATE')
    state = before['data'].get('state')
    if before['type'] != 'RESULT' or not isinstance(state, dict) or state.get('playing') is not False:
        raise ValueError('PAUSED_SOURCE_REQUIRED')

    async def probe(label, serial, index, declared, size, expected):
        # Declared count always leaves a missing final fragment. Even a broken
        # host cannot turn these x bytes into an executable JSON command.
        frame = struct.pack('>BBHHH', 0x44, 1, serial, index, declared) + b'x' * size
        row = {'case': label, 'index': index, 'wireBytes': len(frame),
               'expectedAttStatus': expected, 'observedAttStatus': None,
               'writeStarted': clock(), 'writeAcknowledged': None}
        progress['probes'].append(row)
        status = await write_fault(frame)
        row.update(writeAcknowledged=clock(), observedAttStatus=status)
        if row['writeStarted'] < 0 or row['writeAcknowledged'] < row['writeStarted']:
            raise ValueError('FRAMING_TIMING_NOT_VERIFIED')
        if type(status) is not int or status != expected:
            raise ValueError('FRAMING_ATT_STATUS_NOT_VERIFIED')
        return row

    async def recover():
        reply = await session.exchange('GET_STATE')
        current = reply['data'].get('state')
        if (reply['type'] != 'RESULT' or not isinstance(current, dict) or
                any(current.get(field) != state.get(field) for field in
                    ('source', 'sourceEpoch', 'playing', 'title', 'artist', 'volume'))):
            raise ValueError('FRAMING_RECOVERY_STATE_CHANGED')
        progress['sameSessionRecoveries'] += 1

    def uninterrupted(rows):
        # Host processing occurs between sender start and its ATT callback.
        # These are conservative upper bounds for every host progress gap.
        return all(0 <= b['writeStarted'] - a['writeAcknowledged'] and
                   b['writeAcknowledged'] - a['writeStarted'] < 10
                   for a, b in zip(rows, rows[1:]))

    oversize = []
    remaining = 65537
    for index in range(count):
        size = min(width, remaining)
        oversize.append(await probe('oversized-incomplete', 0xf070, index, count + 1,
                                    size, 13 if index == count - 1 else 0))
        remaining -= size
    if not uninterrupted(oversize) or oversize[-1]['writeAcknowledged'] - oversize[0]['writeStarted'] >= 60:
        raise ValueError('OVERSIZED_TIMING_NOT_VERIFIED')
    await probe('oversized-slot-released', 0xf070, 0, 2, 1, 0)
    await recover()
    checks.append({'check': '65537-byte incomplete message rejected at limit; released slot and same-session unchanged read', 'passed': True})

    drip = [await probe('slow-drip-start', 0xf080, 0, 32, 1, 0)]
    for index in range(1, 8):
        await delay(9)
        drip.append(await probe('slow-drip-progress', 0xf080, index, 32, 1, 13 if index == 7 else 0))
    lower_age = drip[-1]['writeStarted'] - drip[0]['writeAcknowledged']
    upper_gaps = [b['writeAcknowledged'] - a['writeStarted'] for a, b in zip(drip, drip[1:])]
    progress.update(slowDripMinimumAgeMs=round(lower_age * 1000, 3),
                    slowDripMaximumProgressGapMs=round(max(upper_gaps) * 1000, 3))
    if not uninterrupted(drip) or lower_age < 60:
        raise ValueError('SLOW_DRIP_TIMING_NOT_VERIFIED')
    await recover()
    checks.append({'check': 'incomplete message expires after60s despite progress gaps below10s; same-session unchanged read', 'passed': True})
    progress.update(status='PASS_WITHIN_PC_BOUNDS_SUBSET', faultWrites=len(progress['probes']),
                    scope='PC framing bounds only; no heap measurement, playback, phone faults or full T16 acceptance')
    return progress


async def run_framing_fault_checks(session, write_fault, checks, *, enabled=False, delay=asyncio.sleep, progress=None):
    if not enabled or not session.allow_authorized_metadata or not session.authorized:
        raise ValueError('AUTHORIZED_FRAMING_MODE_REQUIRED')
    progress = {} if progress is None else progress
    progress['status'] = 'FAILED_OR_INTERRUPTED'
    probes = progress.setdefault('probes', [])
    before = await session.exchange('GET_STATE')
    state = before['data'].get('state')
    if before['type'] != 'RESULT' or not isinstance(state, dict) or state.get('playing') is not False:
        raise ValueError('PAUSED_SOURCE_REQUIRED')
    recoveries = 0

    async def probe(label, serial, index, count, expected, magic=0x44):
        # Payload pieces contain only x: no possible completion is valid JSON.
        # Accepted sequences stay incomplete; index1 is tested only without index0.
        frame = struct.pack('>BBHHH', magic, 1, serial, index, count) + b'x'
        observation = {'case': label, 'wireBytes': len(frame), 'expectedAttStatus': expected, 'observedAttStatus': None}
        probes.append(observation)
        status = await write_fault(frame)
        observation['observedAttStatus'] = status
        if type(status) is not int or status != expected:
            raise ValueError('FRAMING_ATT_STATUS_NOT_VERIFIED')

    async def recover():
        nonlocal recoveries
        reply = await session.exchange('GET_STATE')
        current = reply['data'].get('state')
        if (reply['type'] != 'RESULT' or not isinstance(current, dict) or
                any(current.get(field) != state.get(field) for field in
                    ('source', 'sourceEpoch', 'playing', 'title', 'artist', 'volume'))):
            raise ValueError('FRAMING_RECOVERY_STATE_CHANGED')
        recoveries += 1
        progress['sameSessionRecoveries'] = recoveries

    await probe('bad-magic', 0xf000, 0, 2, 13, magic=0)
    await recover()
    checks.append({'check': 'invalid header rejected with ATT0x0D; same-session state read recovers', 'passed': True})
    await probe('out-of-order-first', 0xf001, 1, 2, 13)
    await recover()
    checks.append({'check': 'out-of-order first fragment rejected; same-session state read recovers', 'passed': True})
    await probe('duplicate-incomplete-start', 0xf002, 0, 2, 0)
    await probe('duplicate-incomplete-rejected', 0xf002, 0, 2, 13)
    await recover()
    checks.append({'check': 'duplicate incomplete fragment rejected and partial released; state read recovers', 'passed': True})
    for index in range(4):
        await probe('partial-slot-' + str(index + 1), 0xf010 + index, 0, 2, 0)
    await probe('fifth-partial-rejected', 0xf014, 0, 2, 13)
    started = time.monotonic()
    await delay(11)  # After the final partial, beyond the host's10s idle limit.
    idle_ms = round((time.monotonic() - started) * 1000, 3)
    await recover()
    checks.append({'check': 'four incomplete slots bounded; fifth rejected; idle expiry restores same-session reads', 'passed': True})
    progress.update(status='PASS_WITHIN_PC_FRAMING_SUBSET', faultWrites=len(probes),
                    sameSessionRecoveries=recoveries, idleWaitRequestedMs=11000, idleWaitObservedMs=idle_ms,
                    scope='PC BLE framing subset only; no playback, full oversized/slow-drip, physical phone or full T16 acceptance')
    return progress
