"""Bounded, read-only protocol diagnostic. No product transport or playback engine."""
import asyncio
import json
import re
import struct
import time
import uuid

SERVICE = 'e7e01000-4a76-4b53-9c8f-c7a12b00d340'
INFO = 'e7e01001-4a76-4b53-9c8f-c7a12b00d340'
RX = 'e7e01002-4a76-4b53-9c8f-c7a12b00d340'
TX = 'e7e01003-4a76-4b53-9c8f-c7a12b00d340'
MAX_MESSAGE = 65536


def diagnostic_failure(error, gatt_code=None):
    """Fixed domain codes and numeric OS details only; never exception messages/paths."""
    code = str(error) if isinstance(error, ValueError) and re.fullmatch(r'[A-Z_]{3,80}', str(error)) else 'DIAGNOSTIC_FAILED'
    result = {'errorType': type(error).__name__, 'errorCode': code}
    for key in ('winerror', 'hresult', 'errno'):
        value = getattr(error, key, None)
        if isinstance(value, int) and not isinstance(value, bool):
            result[key] = value
    if type(gatt_code) is int and 0 <= gatt_code <= 255:
        result['gattProtocolError'] = gatt_code
    return result


def frames(data, capacity, serial):
    if not data or len(data) > MAX_MESSAGE or not 9 <= capacity <= 512 or not 0 <= serial <= 65535:
        raise ValueError('MALFORMED_FRAME')
    width = capacity - 8
    count = (len(data) + width - 1) // width
    if count > 65535:
        raise ValueError('TOO_MANY_CHUNKS')
    return [struct.pack('>BBHHH', 0x44, 1, serial, index, count) + data[index * width:(index + 1) * width]
            for index in range(count)]


class Assembler:
    def __init__(self, clock=time.monotonic):
        self.clock = clock
        self.partials = {}

    def clear(self):
        self.partials.clear()

    def accept(self, frame):
        now = self.clock()
        self.partials = {key: value for key, value in self.partials.items() if now - value[0] < 10}
        if not 9 <= len(frame) <= 512:
            raise ValueError('MALFORMED_FRAME')
        magic, version, serial, index, count = struct.unpack('>BBHHH', frame[:8])
        if (magic, version) != (0x44, 1):
            raise ValueError('INCOMPATIBLE_PROTOCOL')
        if count == 0 or index >= count:
            raise ValueError('MALFORMED_FRAME')
        if serial not in self.partials:
            if index != 0 or len(self.partials) >= 4:
                raise ValueError('OUT_OF_ORDER')
            self.partials[serial] = [now, count, 0, bytearray()]
        partial = self.partials[serial]
        if partial[1] != count or partial[2] != index or len(partial[3]) + len(frame) - 8 > MAX_MESSAGE:
            del self.partials[serial]
            raise ValueError('MALFORMED_FRAME')
        partial[2] += 1
        partial[3].extend(frame[8:])
        if partial[2] != count:
            return None
        del self.partials[serial]
        return bytes(partial[3])


def decode_object(data):
    text = data.decode('utf-8', errors='strict')
    stack = []
    quoted = escaped = False
    for char in text:
        if quoted:
            if escaped:
                escaped = False
            elif char == '\\':
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
        elif char in '{[':
            stack.append(char)
            if len(stack) > 32:
                raise ValueError('MALFORMED_JSON')
        elif char in '}]':
            if not stack or stack.pop() != ('{' if char == '}' else '['):
                raise ValueError('MALFORMED_JSON')
    if quoted or stack or len(data) > MAX_MESSAGE:
        raise ValueError('MALFORMED_JSON')

    def object_pairs(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError('DUPLICATE_JSON_KEY')
            result[key] = value
        return result

    def invalid_constant(value):
        raise ValueError('MALFORMED_JSON')

    result = json.loads(text, object_pairs_hook=object_pairs, parse_constant=invalid_constant)
    if not isinstance(result, dict):
        raise ValueError('MALFORMED_JSON')
    return result


def require_authenticated_bond(paired, protection):
    if not paired or protection != 'ENCRYPTION_AND_AUTHENTICATION':
        raise ValueError('AUTHENTICATED_OS_BOND_REQUIRED')


class ReadOnlySession:
    """One request at a time,20-byte writes, no automatic retry or reconnect."""
    ALLOWED = frozenset({'HELLO', 'AUTHORIZE', 'GET_STATE'})

    def __init__(self, write, timeout=12, allow_authorized_metadata=False, allow_queue_reads=False, allow_artwork_reads=False):
        self.write = write
        self.timeout = timeout
        self.assembler = Assembler()
        self.host_epoch = None
        self.serial = 0
        self.pending = None
        self.request_id = None
        self.closed = False
        self.allow_authorized_metadata = allow_authorized_metadata
        self.allow_queue_reads = allow_queue_reads
        self.allow_artwork_reads = allow_artwork_reads
        self.authorized = False
        self.operation = None
        self.unsolicited_states = 0
        self.frames_written = 0
        self.frames_received = 0
        self.last_reply_bytes = 0

    def close(self, code='DISCONNECTED'):
        self.closed = True
        self.authorized = False
        self.assembler.clear()
        if self.pending is not None and not self.pending.done():
            self.pending.set_exception(ValueError(code))

    def feed(self, frame):
        if self.closed:
            return
        self.frames_received += 1
        try:
            data = self.assembler.accept(frame)
            if data is None:
                return
            reply = decode_object(data)
            epoch = reply.get('hostEpoch')
            if set(reply) != {'v', 'id', 'hostEpoch', 'type', 'data'} or type(reply.get('v')) is not int or reply['v'] != 1 or not isinstance(epoch, str) or not 1 <= len(epoch) <= 128 or not isinstance(reply.get('data'), dict):
                raise ValueError('MALFORMED_REPLY')
            if self.host_epoch is not None and epoch != self.host_epoch:
                raise ValueError('STALE_HOST')
            if reply.get('type') == 'STATE_CHANGED' and reply.get('id') == '' and self.host_epoch is not None:
                if not self.authorized:
                    raise ValueError('UNEXPECTED_PREAUTH_METADATA')
                self.unsolicited_states += 1
                return  # Do not retain unsolicited metadata or let it satisfy a request.
            if self.pending is None or self.pending.done() or reply.get('id') != self.request_id:
                raise ValueError('UNEXPECTED_REPLY')
            if reply.get('type') == 'ACCEPTED':
                if reply['data'] not in ({}, {'outcome': 'pending'}):
                    raise ValueError('UNEXPECTED_ACCEPTED_DATA')
                return
            if reply.get('type') not in {'HELLO', 'RESULT', 'ERROR'}:
                raise ValueError('MALFORMED_REPLY')
            if reply['type'] == 'ERROR' and (set(reply['data']) != {'code'} or not isinstance(reply['data']['code'], str) or not re.fullmatch(r'[A-Z_]{3,64}', reply['data']['code'])):
                raise ValueError('UNEXPECTED_ERROR_OR_METADATA')
            if reply['type'] == 'HELLO' and self.operation != 'HELLO':
                raise ValueError('UNEXPECTED_HELLO')
            if reply['type'] == 'RESULT' and not self.authorized:
                if self.operation != 'AUTHORIZE' or not self.allow_authorized_metadata or reply['data'].get('authorized') is not True or not isinstance(reply['data'].get('state'), dict) or not isinstance(reply['data'].get('sources'), list):
                    raise ValueError('UNEXPECTED_PREAUTH_METADATA')
                self.authorized = True
            self.last_reply_bytes = len(data)
            self.pending.set_result(reply)
        except (ValueError, UnicodeError, RecursionError):
            self.close('INVALID_OR_STALE_REPLY')

    async def exchange(self, operation, request_id=None, *, args=None, source=None, source_epoch=None):
        if operation == 'GET_ARTWORK':
            if not self.allow_artwork_reads or not self.allow_authorized_metadata or not self.authorized:
                raise ValueError('AUTHORIZED_ARTWORK_MODE_REQUIRED')
            if (not isinstance(args, dict) or set(args) != {'key'} or
                    not isinstance(args['key'], str) or not re.fullmatch(r'[a-f0-9]{64}', args['key']) or
                    source not in {'com.ibasso.music', 'com.apple.android.music'} or
                    not isinstance(source_epoch, str) or not 1 <= len(source_epoch) <= 128):
                raise ValueError('INVALID_ARTWORK_READ')
        elif operation == 'GET_QUEUE':
            if not self.allow_queue_reads or not self.allow_authorized_metadata or not self.authorized:
                raise ValueError('AUTHORIZED_QUEUE_MODE_REQUIRED')
            if (not isinstance(args, dict) or set(args) - {'offset', 'revision'} or
                    type(args.get('offset', 0)) is not int or not 0 <= args.get('offset', 0) <= 10000 or
                    ('revision' in args and (not isinstance(args['revision'], str) or not 1 <= len(args['revision']) <= 128)) or
                    source not in {'com.ibasso.music', 'com.apple.android.music'} or
                    not isinstance(source_epoch, str) or not 1 <= len(source_epoch) <= 128):
                raise ValueError('INVALID_QUEUE_READ')
        elif operation not in self.ALLOWED:
            raise ValueError('READ_ONLY_OPERATION_REQUIRED')
        elif args is not None or source is not None or source_epoch is not None:
            raise ValueError('UNEXPECTED_READ_ARGUMENTS')
        if self.closed or self.pending is not None:
            raise ValueError('SESSION_UNAVAILABLE')
        if (operation == 'HELLO') != (self.host_epoch is None):
            raise ValueError('HELLO_REQUIRED_OR_ALREADY_DONE')
        request_id = request_id or str(uuid.uuid4())
        if not re.fullmatch(r'[A-Za-z0-9-]{1,64}', request_id):
            raise ValueError('INVALID_REQUEST_ID')
        request = {'v': 1, 'id': request_id, 'type': operation, 'args': args if operation in {'GET_QUEUE', 'GET_ARTWORK'} else {}}
        if operation in {'GET_QUEUE', 'GET_ARTWORK'}:
            request.update(source=source, sourceEpoch=source_epoch)
        if self.host_epoch is not None:
            request['hostEpoch'] = self.host_epoch
        data = json.dumps(request, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
        self.pending = asyncio.get_running_loop().create_future()
        self.request_id = request_id
        self.operation = operation
        try:
            async with asyncio.timeout(self.timeout):
                outgoing = frames(data, 20, self.serial)
                self.serial = (self.serial + 1) & 65535
                for frame in outgoing:
                    if self.closed:
                        raise ValueError('SESSION_UNAVAILABLE')
                    await self.write(frame)
                    self.frames_written += 1
                reply = await self.pending
            if operation == 'HELLO':
                if reply['type'] != 'HELLO' or type(reply['data'].get('maxMessage')) is not int or reply['data']['maxMessage'] != MAX_MESSAGE or set(reply['data']) - {'maxMessage', 'pairingSecurity'}:
                    raise ValueError('UNEXPECTED_HELLO_OR_METADATA')
                self.host_epoch = reply['hostEpoch']
            return reply
        except BaseException:
            self.close('REQUEST_FAILED')
            raise
        finally:
            if not self.pending.done():
                self.pending.cancel()
            elif not self.pending.cancelled():
                self.pending.exception()  # Consume callback failure even if a write failed first.
            self.pending = None
            self.request_id = None
            self.operation = None


async def run_read_only_checks(session, mode, checks=None, approval_wait=45, approval_poll_interval=2, approval_requested=None):
    if mode not in {'approval-boundary', 'await-approval', 'authorized-state'}:
        raise ValueError('INVALID_MODE')
    checks = [] if checks is None else checks
    await session.exchange('HELLO')
    checks.append({'check': 'correlated HELLO with host epoch', 'passed': True})
    authorization = await session.exchange('AUTHORIZE')
    if mode == 'await-approval' and authorization['type'] == 'ERROR':
        if authorization['data'] != {'code': 'LOCAL_APPROVAL_REQUIRED'}:
            raise ValueError('UNEXPECTED_APPROVAL_ERROR')
        checks.append({'check': 'local approval initially required', 'passed': True})
        if approval_requested is not None:
            approval_requested()
        approval_deadline = asyncio.timeout(approval_wait)
        try:
            async with approval_deadline:
                while authorization['type'] == 'ERROR':
                    await asyncio.sleep(approval_poll_interval)
                    authorization = await session.exchange('AUTHORIZE')
                    if authorization['type'] == 'ERROR' and authorization['data'] != {'code': 'LOCAL_APPROVAL_REQUIRED'}:
                        raise ValueError('UNEXPECTED_APPROVAL_ERROR')
        except TimeoutError:
            raise ValueError('LOCAL_APPROVAL_NOT_OBSERVED' if approval_deadline.expired() else 'APPROVAL_REQUEST_TIMEOUT') from None
    if mode == 'approval-boundary':
        if authorization['type'] != 'ERROR' or authorization['data'] != {'code': 'LOCAL_APPROVAL_REQUIRED'}:
            raise ValueError('EXPECTED_LOCAL_APPROVAL_DENIAL_NO_METADATA')
        checks.append({'check': 'AUTHORIZE denies before local approval', 'passed': True})
        state = await session.exchange('GET_STATE')
        if state['type'] != 'ERROR' or state['data'] != {'code': 'AUTHORIZATION_DENIED'}:
            raise ValueError('EXPECTED_UNAUTHORIZED_STATE_DENIAL_NO_METADATA')
        checks.append({'check': 'GET_STATE denies before authorization', 'passed': True})
    else:
        if authorization['type'] != 'RESULT' or authorization['data'].get('authorized') is not True or not isinstance(authorization['data'].get('state'), dict) or not isinstance(authorization['data'].get('sources'), list):
            raise ValueError('LOCAL_APPROVAL_OR_AUTHORIZATION_NOT_VERIFIED')
        checks.append({'check': 'AUTHORIZE succeeds after local approval', 'passed': True})
        request_id = str(uuid.uuid4())
        state = await session.exchange('GET_STATE', request_id)
        current = state['data'].get('state')
        if state['type'] != 'RESULT' or not isinstance(current, dict) or not isinstance(current.get('source'), str) or not current['source'] or not isinstance(current.get('sourceEpoch'), str) or not current['sourceEpoch'] or type(current.get('playing')) is not bool:
            raise ValueError('STATE_NOT_VERIFIED')
        checks.append({'check': 'GET_STATE returns current source identity/state', 'passed': True})
        duplicate = await session.exchange('GET_STATE', request_id)
        if duplicate != state:
            raise ValueError('READ_DUPLICATE_RESULT_CHANGED')
        checks.append({'check': 'duplicate read ID returns identical cached result', 'passed': True})
    return checks


async def run_queue_checks(session, checks):
    """Read the selected paused source only. Keep titles/IDs/revisions in memory.

    The diagnostic caps its walk at200 pages and10000 items, refusing incomplete
    coverage instead of truncating it. Session/outer timeouts still apply.
    """
    before = await session.exchange('GET_STATE')
    state = before['data'].get('state')
    if before['type'] != 'RESULT' or not isinstance(state, dict) or state.get('playing') is not False:
        raise ValueError('PAUSED_SOURCE_REQUIRED')
    source, epoch = state.get('source'), state.get('sourceEpoch')
    rows, pages, revision, total, offset = [], [], None, None, 0
    first_request_id = str(uuid.uuid4())
    first = None

    async def read(arguments, request_id=None):
        return await session.exchange('GET_QUEUE', request_id, args=arguments, source=source, source_epoch=epoch)

    for _ in range(200):
        arguments = {'offset': offset}
        if revision is not None:
            arguments['revision'] = revision
        started = time.monotonic()
        reply = await read(arguments, first_request_id if offset == 0 else None)
        elapsed_ms = round((time.monotonic() - started) * 1000, 3)
        page = reply['data']
        if reply['type'] != 'RESULT' or set(page) != {'revision', 'offset', 'total', 'items', 'nextOffset'}:
            raise ValueError('QUEUE_PAGE_NOT_VERIFIED')
        if len(json.dumps(page, ensure_ascii=False, separators=(',', ':')).encode('utf-8')) > 49152:
            raise ValueError('QUEUE_PAGE_NOT_VERIFIED')
        if (not isinstance(page['revision'], str) or not 1 <= len(page['revision']) <= 128 or
                type(page['offset']) is not int or page['offset'] != offset or
                type(page['total']) is not int or not 0 <= page['total'] <= 10000 or
                (revision is not None and (page['revision'] != revision or page['total'] != total)) or
                not isinstance(page['items'], list) or len(page['items']) > 50):
            raise ValueError('QUEUE_PAGE_NOT_VERIFIED')
        revision, total = page['revision'], page['total']
        for item in page['items']:
            if (not isinstance(item, dict) or set(item) != {'id', 'title'} or
                    not isinstance(item['id'], str) or not item['id'] or not isinstance(item['title'], str)):
                raise ValueError('QUEUE_ITEM_NOT_VERIFIED')
        stop = offset + len(page['items'])
        expected_next = stop if stop < total else None
        if (stop > total or (not page['items'] and stop < total) or
                page['nextOffset'] != expected_next or
                (page['nextOffset'] is not None and type(page['nextOffset']) is not int)):
            raise ValueError('QUEUE_CONTINUATION_NOT_VERIFIED')
        rows.extend(page['items'])
        pages.append({'offset': offset, 'rows': len(page['items']), 'replyBytes': session.last_reply_bytes,
                      'elapsedMs': elapsed_ms})
        if first is None:
            first = reply
        if expected_next is None:
            break
        offset = expected_next
    else:
        raise ValueError('DIAGNOSTIC_QUEUE_PAGE_LIMIT')
    if len(rows) != total or len({item['id'] for item in rows}) != total:
        raise ValueError('QUEUE_COVERAGE_NOT_VERIFIED')
    checks.append({'check': 'complete selected-source queue with stable revision and unique IDs', 'passed': True})
    if await read({'offset': 0}, first_request_id) != first:
        raise ValueError('QUEUE_DUPLICATE_RESULT_CHANGED')
    checks.append({'check': 'duplicate first-page ID returns identical cached result', 'passed': True})
    negative_cases = [{'offset': 0, 'revision': 'diagnostic-stale-' + str(uuid.uuid4())}]
    if total:
        negative_cases.append({'offset': 1})
    for arguments in negative_cases:
        rejected = await read(arguments)
        if rejected['type'] != 'ERROR' or rejected['data'] != {'code': 'STALE_LIBRARY'}:
            raise ValueError('QUEUE_STALE_REVISION_NOT_REJECTED')
    checks.append({'check': 'stale or missing required queue revision rejects without metadata', 'passed': True,
                   'cases': len(negative_cases)})
    refreshed = await read({'offset': 0})
    if refreshed['type'] != 'RESULT' or refreshed['data'] != first['data']:
        raise ValueError('QUEUE_FRESH_READ_CHANGED')
    after = await session.exchange('GET_STATE')
    current = after['data'].get('state')
    if after['type'] != 'RESULT' or not isinstance(current, dict) or any(current.get(key) != state.get(key) for key in ('source', 'sourceEpoch', 'playing', 'title', 'artist')):
        raise ValueError('SOURCE_STATE_CHANGED_DURING_QUEUE_READ')
    checks.append({'check': 'fresh first-page recovery and selected paused source state retained', 'passed': True})
    return {'source': source, 'total': total, 'pages': pages,
            'scope': 'PC BLE queue metadata; page timings are not phone control latency or playback evidence'}
