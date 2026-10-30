"""Fixed incomplete/invalid frames only; never constructs a playback command."""
import asyncio
import struct
import time


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
