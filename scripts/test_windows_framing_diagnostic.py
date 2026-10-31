"""Synthetic radio-driver orchestration tests, not physical host acceptance."""
import unittest
import struct

from test_windows_ble_protocol import Peer
from windows_ble_protocol import Assembler, run_read_only_checks
from windows_framing_diagnostic import run_framing_fault_checks, run_framing_bounds_checks


class FramingDiagnosticTests(unittest.IsolatedAsyncioTestCase):
    async def ready(self):
        peer = Peer(approved=True)
        clock = [0]
        peer.incoming = Assembler(lambda: clock[0])
        normal = peer.handler
        def handler(request):
            reply = normal(request)
            state = reply['data'].get('state')
            if state is not None:
                state['volume'] = {'current': 37, 'verified': False}
            return reply
        peer.handler = handler
        async def raw(frame):
            try:
                result = peer.incoming.accept(frame)
            except ValueError:
                return 13
            self.assertIsNone(result, 'No raw fault frame may complete a JSON message')
            return 0
        async def delay(seconds):
            self.assertEqual(seconds, 11)
            clock[0] += seconds
        await run_read_only_checks(peer.session, 'authorized-state')
        return peer, raw, delay

    async def test_rejection_quota_idle_expiry_and_authorized_recovery_use_same_assembler(self):
        peer, raw, delay = await self.ready()
        checks = []
        result = await run_framing_fault_checks(peer.session, raw, checks, enabled=True, delay=delay)
        self.assertEqual(result['faultWrites'], 9)
        self.assertEqual(result['sameSessionRecoveries'], 4)
        self.assertEqual([r['observedAttStatus'] for r in result['probes']], [13, 13, 0, 13, 0, 0, 0, 0, 13])
        self.assertEqual(len(checks), 4)
        self.assertEqual({r['type'] for r in peer.requests}, {'HELLO', 'AUTHORIZE', 'GET_STATE'})
        self.assertEqual(len(peer.incoming.partials), 0)
        self.assertTrue(peer.session.authorized)
        self.assertNotIn('雪', str(result))

    async def test_explicit_mode_and_authorization_required_before_any_probe(self):
        peer, raw, delay = await self.ready()
        baseline = len(peer.requests)
        with self.assertRaisesRegex(ValueError, 'AUTHORIZED_FRAMING_MODE_REQUIRED'):
            await run_framing_fault_checks(peer.session, raw, [], delay=delay)
        peer.session.authorized = False
        with self.assertRaisesRegex(ValueError, 'AUTHORIZED_FRAMING_MODE_REQUIRED'):
            await run_framing_fault_checks(peer.session, raw, [], enabled=True, delay=delay)
        self.assertEqual(len(peer.requests), baseline)

    async def test_unexpected_acceptance_or_att_error_stops_without_more_faults(self):
        for status in (0, 5, True, '13'):
            peer, _, delay = await self.ready()
            calls = []
            async def wrong(frame):
                calls.append(frame)
                return status
            progress = {}
            with self.assertRaisesRegex(ValueError, 'FRAMING_ATT_STATUS_NOT_VERIFIED'):
                await run_framing_fault_checks(peer.session, wrong, [], enabled=True, delay=delay, progress=progress)
            self.assertEqual(len(calls), 1)
            self.assertEqual(progress['status'], 'FAILED_OR_INTERRUPTED')
            self.assertEqual(progress['probes'][0]['case'], 'bad-magic')
            self.assertEqual(progress['probes'][0]['observedAttStatus'], status)

    async def test_source_playing_or_changed_identity_cannot_pass(self):
        for field, value in [('playing', True), ('volume', {'current': 36}), ('sourceEpoch', 'changed')]:
            peer, raw, delay = await self.ready()
            normal = peer.handler
            count = [0]
            def handler(request):
                reply = normal(request)
                if request['type'] == 'GET_STATE':
                    count[0] += 1
                    if count[0] == (1 if field == 'playing' else 2):
                        reply['data']['state'][field] = value
                return reply
            peer.handler = handler
            with self.assertRaises(ValueError):
                await run_framing_fault_checks(peer.session, raw, [], enabled=True, delay=delay)


class BoundsDiagnosticTests(unittest.IsolatedAsyncioTestCase):
    async def ready(self, *, wrong_status=None, delay_offset=0, callback_delay=0):
        peer = Peer(approved=True)
        now = [0.0]
        peer.incoming = Assembler(lambda: now[0])
        calls = []
        async def raw(frame):
            calls.append(frame)
            try:
                value = peer.incoming.accept(frame)
                self.assertIsNone(value, 'Fault bytes must never complete a JSON message')
                status = 0
            except ValueError:
                status = 13
            now[0] += callback_delay(frame) if callable(callback_delay) else callback_delay
            return wrong_status if wrong_status is not None else status
        async def delay(seconds):
            self.assertEqual(seconds, 9)
            now[0] += seconds + delay_offset
        await run_read_only_checks(peer.session, 'authorized-state')
        return peer, raw, delay, now, calls

    async def test_oversized_and_progressing_expiry_recover_same_authorized_session(self):
        peer, raw, delay, now, calls = await self.ready()
        checks = []
        result = await run_framing_bounds_checks(peer.session, raw, checks, enabled=True,
                                                capacity=512, delay=delay, clock=lambda: now[0])
        self.assertEqual(result['oversizedFrames'], 131)
        self.assertEqual(result['faultWrites'], 140)
        self.assertEqual(result['sameSessionRecoveries'], 2)
        self.assertEqual(result['slowDripMinimumAgeMs'], 63000)
        self.assertEqual(result['slowDripMaximumProgressGapMs'], 9000)
        self.assertEqual(len(checks), 2)
        self.assertTrue(all(x['passed'] for x in checks))
        self.assertEqual(sum(len(x) - 8 for x in calls[:131]), 65537)
        self.assertTrue(all(x[8:] == b'x' * (len(x) - 8) for x in calls))
        # A host with no size/age bounds would still have an incomplete,
        # invalid JSON message: never deliver a full command as a probe.
        for frame in calls:
            _, _, _, index, count = struct.unpack('>BBHHH', frame[:8])
            self.assertLess(index, count - 1)
        self.assertEqual({x['type'] for x in peer.requests}, {'HELLO', 'AUTHORIZE', 'GET_STATE'})
        self.assertEqual(len(peer.incoming.partials), 0)

    async def test_no_probe_without_optin_auth_metadata_capacity_and_paused_source(self):
        for change in ('optin', 'auth', 'metadata', 'capacity', 'small', 'playing'):
            peer, raw, delay, now, calls = await self.ready()
            if change == 'auth': peer.session.authorized = False
            if change == 'metadata': peer.session.allow_authorized_metadata = False
            if change == 'playing':
                normal = peer.handler
                def handler(request):
                    reply = normal(request)
                    if 'state' in reply['data']: reply['data']['state']['playing'] = True
                    return reply
                peer.handler = handler
            with self.subTest(change=change), self.assertRaises(ValueError):
                await run_framing_bounds_checks(peer.session, raw, [], enabled=change != 'optin',
                    capacity=513 if change == 'capacity' else 20 if change == 'small' else 512,
                    delay=delay, clock=lambda: now[0])
            self.assertEqual(calls, [])

    async def test_wrong_first_ack_stops_immediately_and_preserves_failed_probe(self):
        for status in (13, True, '0'):
            peer, raw, delay, now, calls = await self.ready(wrong_status=status)
            progress = {}
            with self.subTest(status=status), self.assertRaisesRegex(ValueError, 'FRAMING_ATT_STATUS_NOT_VERIFIED'):
                await run_framing_bounds_checks(peer.session, raw, [], enabled=True, capacity=512,
                                                delay=delay, clock=lambda: now[0], progress=progress)
            self.assertEqual(len(calls), 1)
            self.assertEqual(progress['probes'][0]['observedAttStatus'], status)
            self.assertEqual(progress['status'], 'FAILED_OR_INTERRUPTED')

    async def test_idle_expiry_is_not_mistaken_for_absolute_lifetime(self):
        peer, raw, delay, now, calls = await self.ready(delay_offset=1.1)
        with self.assertRaisesRegex(ValueError, 'FRAMING_ATT_STATUS_NOT_VERIFIED'):
            await run_framing_bounds_checks(peer.session, raw, [], enabled=True, capacity=512,
                                            delay=delay, clock=lambda: now[0])
        self.assertEqual(len(calls), 134)  # stop at the first premature expired fragment

    async def test_callback_delay_cannot_hide_an_idle_gap(self):
        peer, raw, delay, now, calls = await self.ready(
            callback_delay=lambda frame: 0.6 if struct.unpack('>H', frame[2:4])[0] == 0xf080 else 0)
        # Add callback delay only to the drip frames. Host gaps stay9.6s, but the
        # conservative sender upper bound10.2s cannot prove sub10s progress.
        progress = {}
        with self.assertRaisesRegex(ValueError, 'SLOW_DRIP_TIMING_NOT_VERIFIED'):
            await run_framing_bounds_checks(peer.session, raw, [], enabled=True, capacity=512,
                                            delay=delay, clock=lambda: now[0], progress=progress)
        self.assertGreater(progress['slowDripMaximumProgressGapMs'], 10000)
        self.assertEqual(progress['status'], 'FAILED_OR_INTERRUPTED')

    async def test_changed_source_cannot_pass_recovery(self):
        peer, raw, delay, now, calls = await self.ready()
        normal = peer.handler
        reads = [0]
        def handler(request):
            reply = normal(request)
            if request['type'] == 'GET_STATE':
                reads[0] += 1
                if reads[0] == 2: reply['data']['state']['sourceEpoch'] = 'changed'
            return reply
        peer.handler = handler
        with self.assertRaisesRegex(ValueError, 'FRAMING_RECOVERY_STATE_CHANGED'):
            await run_framing_bounds_checks(peer.session, raw, [], enabled=True, capacity=512,
                                            delay=delay, clock=lambda: now[0])
        self.assertEqual(len(calls), 132)


if __name__ == '__main__':
    unittest.main()
