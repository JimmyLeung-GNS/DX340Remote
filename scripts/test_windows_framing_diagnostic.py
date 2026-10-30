"""Synthetic radio-driver orchestration tests, not physical host acceptance."""
import unittest

from test_windows_ble_protocol import Peer
from windows_ble_protocol import Assembler, run_read_only_checks
from windows_framing_diagnostic import run_framing_fault_checks


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


if __name__ == '__main__':
    unittest.main()
