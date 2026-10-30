"""Software-only conformance and diagnostic failure tests; no radio, bond or playback."""
import asyncio
import json
from pathlib import Path
import struct
import unittest

from windows_ble_protocol import (Assembler, MAX_MESSAGE, ReadOnlySession, decode_object,
                                  frames, require_authenticated_bond, run_read_only_checks, diagnostic_failure)


class DiagnosticFailureTests(unittest.TestCase):
    def test_domain_codes_preserved_but_os_paths_and_messages_omitted(self):
        self.assertEqual(diagnostic_failure(ValueError('AUTHENTICATED_OS_BOND_REQUIRED'))['errorCode'], 'AUTHENTICATED_OS_BOND_REQUIRED')
        failure = diagnostic_failure(FileNotFoundError(2, 'private device identity', 'private path'))
        self.assertEqual(failure, {'errorType': 'FileNotFoundError', 'errorCode': 'DIAGNOSTIC_FAILED', 'errno': 2})
        self.assertNotIn('private', json.dumps(failure))

    def test_numeric_gatt_error_does_not_leak_message_or_accept_arbitrary_values(self):
        failure = diagnostic_failure(RuntimeError('private device address'), 5)
        self.assertEqual(failure['gattProtocolError'], 5)
        self.assertNotIn('private', json.dumps(failure))
        for invalid in (-1, 256, True, '5'):
            self.assertNotIn('gattProtocolError', diagnostic_failure(RuntimeError('private'), invalid))


class FramingTests(unittest.TestCase):
    def test_large_reply_may_progress_for_more_than_ten_seconds(self):
        now = [0]
        a = Assembler(lambda: now[0])
        data = bytes(i % 251 for i in range(44 * 1024))
        sequence = frames(data, 512, 9)
        for index, frame in enumerate(sequence):
            now[0] = index * 0.5
            value = a.accept(frame)
            self.assertEqual(value, data if index == len(sequence) - 1 else None)
        self.assertGreater(now[0], 10)
        self.assertLess(now[0], 60)

    def test_other_messages_cannot_extend_an_idle_partial(self):
        now = [0]
        a = Assembler(lambda: now[0]); sequence = frames(b'x' * 40, 20, 8)
        a.accept(sequence[0]); now[0] = 9.999
        self.assertEqual(a.accept(frames(b'x', 20, 9)[0]), b'x')
        now[0] = 10
        with self.assertRaises(ValueError):
            a.accept(sequence[1])

    def test_total_lifetime_and_clock_rollback_fail_despite_progress(self):
        now = [0]
        a = Assembler(lambda: now[0]); sequence = frames(b'x' * 120, 20, 7)
        for index in range(7):
            now[0] = index * 9
            self.assertIsNone(a.accept(sequence[index]))
        now[0] = 60
        with self.assertRaises(ValueError):
            a.accept(sequence[7])
        a.accept(sequence[0]); now[0] = 59.999
        with self.assertRaises(ValueError):
            a.accept(sequence[1])

    def test_shared_kotlin_swift_fixtures(self):
        fixture = Path(__file__).resolve().parents[1] / 'protocol/fixtures/framing.json'
        for case in json.loads(fixture.read_text(encoding='utf-8')):
            with self.subTest(serial=case['id']):
                encoded = frames(case['text'].encode('utf-8'), case['capacity'], case['id'])
                self.assertEqual([item.hex() for item in encoded], case['hex'])
                assembler = Assembler()
                results = [assembler.accept(item) for item in encoded]
                self.assertTrue(all(item is None for item in results[:-1]))
                self.assertEqual(results[-1].decode('utf-8'), case['text'])

    def test_interleaving_expiry_and_four_message_bound(self):
        now = [0]
        assembler = Assembler(lambda: now[0])
        sequences = [frames(b'abcdefgh', 10, serial) for serial in range(5)]
        for sequence in sequences[:4]:
            self.assertIsNone(assembler.accept(sequence[0]))
        with self.assertRaises(ValueError):
            assembler.accept(sequences[4][0])
        now[0] = 10
        with self.assertRaises(ValueError):
            assembler.accept(sequences[0][1])
        self.assertIsNone(assembler.accept(sequences[4][0]))
        for frame in sequences[4][1:]:
            result = assembler.accept(frame)
        self.assertEqual(result, b'abcdefgh')

    def test_order_duplicate_and_count_change_drop_partial(self):
        sequence = frames(b'abcdefgh', 10, 7)
        for bad in (sequence[0], sequence[2], struct.pack('>BBHHH', 0x44, 1, 7, 1, 5) + b'cd'):
            assembler = Assembler()
            assembler.accept(sequence[0])
            with self.assertRaises(ValueError):
                assembler.accept(bad)
            self.assertEqual(assembler.partials, {})
        with self.assertRaises(ValueError):
            Assembler().accept(sequence[1])

    def test_frame_and_payload_bounds(self):
        valid = frames(b'x' * MAX_MESSAGE, 512, 65535)
        assembler = Assembler()
        for frame in valid:
            result = assembler.accept(frame)
        self.assertEqual(len(result), MAX_MESSAGE)
        for value in (b'', b'x' * (MAX_MESSAGE + 1)):
            with self.assertRaises(ValueError):
                frames(value, 20, 0)
        with self.assertRaises(ValueError):
            frames(b'x' * MAX_MESSAGE, 9, 0)
        for value in (b'', b'x' * 8, b'x' * 513, struct.pack('>BBHHH', 0x44, 1, 0, 0, 0) + b'x',
                      struct.pack('>BBHHH', 0, 1, 0, 0, 1) + b'x'):
            with self.assertRaises(ValueError):
                Assembler().accept(value)
        overlong = frames(b'x' * MAX_MESSAGE, 512, 9)
        count = len(overlong) + 1
        assembler = Assembler()
        for index, frame in enumerate(overlong):
            assembler.accept(struct.pack('>BBHHH', 0x44, 1, 9, index, count) + frame[8:])
        with self.assertRaises(ValueError):
            assembler.accept(struct.pack('>BBHHH', 0x44, 1, 9, count - 1, count) + b'x')

    def test_disconnect_clear_cannot_finish_old_partial(self):
        sequence = frames(b'abcdefgh', 10, 0)
        assembler = Assembler()
        assembler.accept(sequence[0])
        assembler.clear()
        with self.assertRaises(ValueError):
            assembler.accept(sequence[1])

    def test_json_unicode_quotes_duplicates_depth_and_nonfinite(self):
        self.assertEqual(decode_object('{"title":"雪\\\"{}"}'.encode()), {'title': '雪"{}'})
        for value in (b'{"v":1,"v":2}', b'{"x":NaN}', b'{"x":Infinity}', b'[]', b'{"x":"\xff"}',
                      b'{"x":' + b'[' * 32 + b'0' + b']' * 32 + b'}'):
            with self.assertRaises((ValueError, UnicodeError)):
                decode_object(value)

    def test_bond_guard_rejects_unpaired_unknown_and_weaker_protection(self):
        for paired, protection in ((False, 'ENCRYPTION_AND_AUTHENTICATION'), (False, 'NONE'),
                                   (True, 'ENCRYPTION'), (True, 'DEFAULT'), (True, 'UNKNOWN')):
            with self.assertRaisesRegex(ValueError, 'AUTHENTICATED_OS_BOND_REQUIRED'):
                require_authenticated_bond(paired, protection)
        require_authenticated_bond(True, 'ENCRYPTION_AND_AUTHENTICATION')


class Peer:
    """Deterministic fake peer for diagnostic correctness only, never host acceptance."""
    def __init__(self, approved=False, timeout=0.2):
        self.approved = approved
        self.requests = []
        self.incoming = Assembler()
        self.handler = self.normal
        self.serial = 0
        self.session = ReadOnlySession(self.write, timeout, allow_authorized_metadata=approved)

    async def write(self, frame):
        if len(frame) > 20:
            raise AssertionError('Diagnostic must use conservative20-byte writes')
        message = self.incoming.accept(frame)
        if message is None:
            return
        request = decode_object(message)
        self.requests.append(request)
        reply = self.handler(request)
        if reply is not None:
            self.emit(reply)

    def emit(self, reply):
        for frame in frames(json.dumps(reply, ensure_ascii=False, separators=(',', ':')).encode(), 20, self.serial):
            self.session.feed(frame)
        self.serial += 1

    def normal(self, request):
        operation = request['type']
        kind = 'RESULT'
        state = {'source': 'com.ibasso.music', 'sourceEpoch': 'source-epoch', 'playing': False, 'title': '雪解け'}
        if operation == 'HELLO':
            kind, data = 'HELLO', {'maxMessage': MAX_MESSAGE, 'pairingSecurity': 'physical verification pending'}
        elif not self.approved:
            kind, data = 'ERROR', {'code': 'LOCAL_APPROVAL_REQUIRED' if operation == 'AUTHORIZE' else 'AUTHORIZATION_DENIED'}
        elif operation == 'AUTHORIZE':
            data = {'authorized': True, 'state': state, 'sources': [{'id': state['source']}]}
        else:
            data = {'state': state}
        return {'v': 1, 'id': request['id'], 'hostEpoch': 'host-epoch', 'type': kind, 'data': data}


class SessionTests(unittest.IsolatedAsyncioTestCase):
    async def test_unapproved_boundary_checks_both_authorize_and_state(self):
        peer = Peer()
        checks = await run_read_only_checks(peer.session, 'approval-boundary')
        self.assertTrue(all(check['passed'] for check in checks))
        self.assertEqual([item['type'] for item in peer.requests], ['HELLO', 'AUTHORIZE', 'GET_STATE'])
        self.assertTrue(all(item.get('hostEpoch') == 'host-epoch' for item in peer.requests[1:]))

    async def test_authorized_read_and_exact_duplicate_use_same_request(self):
        peer = Peer(approved=True)
        checks = await run_read_only_checks(peer.session, 'authorized-state')
        self.assertEqual(len(checks), 4)
        self.assertEqual(peer.requests[-1], peer.requests[-2])
        self.assertEqual([item['type'] for item in peer.requests], ['HELLO', 'AUTHORIZE', 'GET_STATE', 'GET_STATE'])

    async def test_wrong_mode_and_playback_operations_never_write(self):
        peer = Peer()
        for operation in ('NEXT', 'PREVIOUS', 'SET_PLAYING', 'SEEK', 'PLAY_ITEM', 'PLAY_QUEUE_ITEM',
                          'ADJUST_VOLUME', 'SELECT_SOURCE', 'SUBSCRIBE_STATE'):
            with self.assertRaisesRegex(ValueError, 'READ_ONLY_OPERATION_REQUIRED'):
                await peer.session.exchange(operation)
        with self.assertRaises(ValueError):
            await run_read_only_checks(peer.session, 'playback')
        with self.assertRaises(ValueError):
            await peer.session.exchange('GET_STATE')
        self.assertEqual(peer.requests, [])

    async def test_unexpected_authorization_or_metadata_fails_boundary(self):
        for leak in ('authorization', 'error-data', 'envelope-data', 'unsolicited-state'):
            peer = Peer()
            def handler(request):
                reply = peer.normal(request)
                if request['type'] == 'AUTHORIZE':
                    if leak == 'authorization':
                        reply.update(type='RESULT', data={'authorized': True, 'state': {}, 'sources': []})
                    elif leak == 'error-data':
                        reply['data']['state'] = {'title': 'not permitted'}
                    elif leak == 'envelope-data':
                        reply['state'] = {'title': 'not permitted'}
                    else:
                        peer.emit({'v': 1, 'id': '', 'hostEpoch': 'host-epoch', 'type': 'STATE_CHANGED', 'data': {'title': 'not permitted'}})
                return reply
            peer.handler = handler
            progress = []
            with self.assertRaises(ValueError):
                await run_read_only_checks(peer.session, 'approval-boundary', progress)
            self.assertTrue(peer.session.closed)
            self.assertEqual(progress, [{'check': 'correlated HELLO with host epoch', 'passed': True}])
            self.assertNotIn('GET_STATE', [item['type'] for item in peer.requests])

    async def test_mismatched_id_epoch_and_version_cannot_complete_request(self):
        for field, value in (('id', 'late-request'), ('hostEpoch', 'different-host'), ('v', True)):
            peer = Peer()
            await peer.session.exchange('HELLO')
            def handler(request):
                reply = peer.normal(request)
                reply[field] = value
                return reply
            peer.handler = handler
            with self.assertRaises(ValueError):
                await peer.session.exchange('AUTHORIZE')
            self.assertTrue(peer.session.closed)

    async def test_accepted_does_not_satisfy_request(self):
        peer = Peer(timeout=0.01)
        await peer.session.exchange('HELLO')
        peer.handler = lambda request: {'v': 1, 'id': request['id'], 'hostEpoch': 'host-epoch', 'type': 'ACCEPTED', 'data': {}}
        with self.assertRaises(TimeoutError):
            await peer.session.exchange('AUTHORIZE')
        self.assertTrue(peer.session.closed)

    async def test_disconnect_and_timeout_invalidate_no_automatic_retry(self):
        for disconnect in (False, True):
            peer = Peer(timeout=0.01)
            if disconnect:
                def handler(request):
                    peer.session.close()
                peer.handler = handler
            else:
                peer.handler = lambda request: None
            with self.assertRaises((TimeoutError, ValueError)):
                await peer.session.exchange('HELLO')
            with self.assertRaises(ValueError):
                await peer.session.exchange('HELLO')
            self.assertEqual(len(peer.requests), 1)

    async def test_changed_duplicate_read_result_fails_diagnostic(self):
        peer = Peer(approved=True)
        counter = [0]
        def handler(request):
            reply = peer.normal(request)
            if request['type'] == 'GET_STATE':
                counter[0] += 1
                reply['data']['state']['position'] = counter[0]
            return reply
        peer.handler = handler
        with self.assertRaisesRegex(ValueError, 'READ_DUPLICATE_RESULT_CHANGED'):
            await run_read_only_checks(peer.session, 'authorized-state')

    async def test_write_timeout_is_bounded_and_does_not_retry(self):
        calls = []
        async def blocked_write(frame):
            calls.append(frame)
            await asyncio.Event().wait()
        session = ReadOnlySession(blocked_write, timeout=0.01)
        with self.assertRaises(TimeoutError):
            await session.exchange('HELLO')
        self.assertEqual(len(calls), 1)
        self.assertTrue(session.closed)

    async def test_authorized_unsolicited_state_cannot_complete_read(self):
        peer = Peer(approved=True)
        def handler(request):
            if request['type'] == 'GET_STATE':
                peer.emit({'v': 1, 'id': '', 'hostEpoch': 'host-epoch', 'type': 'STATE_CHANGED', 'data': {'state': {'title': 'event'}}})
            return peer.normal(request)
        peer.handler = handler
        await run_read_only_checks(peer.session, 'authorized-state')
        self.assertEqual(peer.session.unsolicited_states, 2)

    async def test_wait_mode_observes_later_local_approval_without_mutation(self):
        peer = Peer()
        peer.session.allow_authorized_metadata = True
        authorizations = [0]
        def handler(request):
            if request['type'] == 'AUTHORIZE':
                authorizations[0] += 1
                # Simulates the operator's local action, not a controller command.
                peer.approved = authorizations[0] >= 2
            return peer.normal(request)
        peer.handler = handler
        checks = await run_read_only_checks(peer.session, 'await-approval', approval_poll_interval=0.001)
        self.assertEqual(authorizations[0], 2)
        self.assertTrue(any(check['check'] == 'local approval initially required' for check in checks))
        self.assertEqual({request['type'] for request in peer.requests}, {'HELLO', 'AUTHORIZE', 'GET_STATE'})

    async def test_wait_mode_expires_without_approving_or_reading_metadata(self):
        peer = Peer()
        peer.session.allow_authorized_metadata = True
        with self.assertRaisesRegex(ValueError, 'LOCAL_APPROVAL_NOT_OBSERVED'):
            await run_read_only_checks(peer.session, 'await-approval', approval_wait=0.01, approval_poll_interval=0.02)
        self.assertFalse(peer.approved)
        self.assertEqual([request['type'] for request in peer.requests], ['HELLO', 'AUTHORIZE'])

    async def test_request_timeout_is_not_reported_as_operator_deadline(self):
        peer = Peer(timeout=0.01)
        peer.session.allow_authorized_metadata = True
        authorization_count = [0]
        def handler(request):
            if request['type'] == 'AUTHORIZE':
                authorization_count[0] += 1
                if authorization_count[0] == 2:
                    return None  # No reply to the poll, before the operator deadline.
            return peer.normal(request)
        peer.handler = handler
        notices = []
        with self.assertRaisesRegex(ValueError, 'APPROVAL_REQUEST_TIMEOUT'):
            await run_read_only_checks(peer.session, 'await-approval', approval_wait=1,
                                       approval_poll_interval=0.001, approval_requested=lambda: notices.append(True))
        self.assertEqual(notices, [True])
        self.assertFalse(peer.approved)

    async def test_no_pending_approval_notice_for_already_approved_peer(self):
        peer = Peer(approved=True)
        notices = []
        await run_read_only_checks(peer.session, 'await-approval', approval_requested=lambda: notices.append(True))
        self.assertEqual(notices, [])


if __name__ == '__main__':
    unittest.main()
