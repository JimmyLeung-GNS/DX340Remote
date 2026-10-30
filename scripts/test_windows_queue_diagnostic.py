"""Synthetic peer tests for the queue diagnostic, never radio/source acceptance."""
import unittest

from test_windows_ble_protocol import Peer
from windows_ble_protocol import run_read_only_checks, run_queue_checks


class QueueDiagnosticTests(unittest.IsolatedAsyncioTestCase):
    def peer(self, total=101, width=50):
        peer = Peer(approved=True)
        peer.session.allow_queue_reads = True
        rows = [{'id': str(index), 'title': '雪・"曲"' * 20 + str(index)} for index in range(total)]

        def handler(request):
            reply = peer.normal(request)
            if request['type'] != 'GET_QUEUE':
                return reply
            args = request['args']
            offset, expected = args.get('offset', 0), args.get('revision')
            if (offset > 0 and expected is None) or (expected is not None and expected != 'queue-revision'):
                reply.update(type='ERROR', data={'code': 'STALE_LIBRARY'})
            else:
                page_rows = rows[offset:offset + width]
                stop = offset + len(page_rows)
                reply['data'] = {'revision': 'queue-revision', 'offset': offset, 'total': len(rows),
                                 'items': page_rows, 'nextOffset': stop if stop < len(rows) else None}
            return reply
        peer.handler = handler
        return peer

    async def ready(self, peer):
        await run_read_only_checks(peer.session, 'authorized-state')

    async def test_full_fragmented_queue_walk_duplicate_and_rejection_recovery(self):
        peer = self.peer()
        await self.ready(peer)
        checks = []
        summary = await run_queue_checks(peer.session, checks)
        self.assertEqual(summary['total'], 101)
        self.assertEqual([page['rows'] for page in summary['pages']], [50, 50, 1])
        self.assertGreater(summary['pages'][0]['replyBytes'], 512)
        self.assertEqual(len(checks), 4)
        queue_requests = [r for r in peer.requests if r['type'] == 'GET_QUEUE']
        self.assertEqual(queue_requests[0], queue_requests[3])
        self.assertTrue(all(r['source'] == 'com.ibasso.music' and r['sourceEpoch'] == 'source-epoch' for r in queue_requests))
        self.assertEqual({r['type'] for r in peer.requests}, {'HELLO', 'AUTHORIZE', 'GET_STATE', 'GET_QUEUE'})
        self.assertNotIn('雪', str(summary))

    async def test_empty_and_byte_limited_windows_are_complete_without_invented_rows(self):
        for total, width, counts in ((0, 50, [0]), (5, 2, [2, 2, 1])):
            with self.subTest(total=total):
                peer = self.peer(total, width)
                await self.ready(peer)
                summary = await run_queue_checks(peer.session, [])
                self.assertEqual(summary['total'], total)
                self.assertEqual([p['rows'] for p in summary['pages']], counts)

    async def test_queue_is_explicitly_opt_in_authorized_and_schema_bounded_before_writes(self):
        peer = self.peer()
        peer.session.allow_queue_reads = False
        with self.assertRaisesRegex(ValueError, 'AUTHORIZED_QUEUE_MODE_REQUIRED'):
            await peer.session.exchange('GET_QUEUE', args={}, source='com.ibasso.music', source_epoch='source-epoch')
        peer.session.allow_queue_reads = True
        with self.assertRaisesRegex(ValueError, 'AUTHORIZED_QUEUE_MODE_REQUIRED'):
            await peer.session.exchange('GET_QUEUE', args={}, source='com.ibasso.music', source_epoch='source-epoch')
        await self.ready(peer)
        baseline = len(peer.requests)
        for args in ({'offset': True}, {'offset': -1}, {'offset': 10001}, {'revision': None},
                     {'revision': ''}, {'revision': 'x' * 129}, {'playing': True}):
            with self.assertRaisesRegex(ValueError, 'INVALID_QUEUE_READ'):
                await peer.session.exchange('GET_QUEUE', args=args, source='com.ibasso.music', source_epoch='source-epoch')
        for source, epoch in (('other', 'source-epoch'), ('com.ibasso.music', ''), ('com.ibasso.music', True)):
            with self.assertRaisesRegex(ValueError, 'INVALID_QUEUE_READ'):
                await peer.session.exchange('GET_QUEUE', args={}, source=source, source_epoch=epoch)
        for operation in ('SET_PLAYING', 'PLAY_QUEUE_ITEM', 'SELECT_SOURCE', 'ADJUST_VOLUME', 'BROWSE'):
            with self.assertRaisesRegex(ValueError, 'READ_ONLY_OPERATION_REQUIRED'):
                await peer.session.exchange(operation, args={'playing': True})
        with self.assertRaisesRegex(ValueError, 'UNEXPECTED_READ_ARGUMENTS'):
            await peer.session.exchange('GET_STATE', args={'offset': 0})
        self.assertEqual(len(peer.requests), baseline)

    async def test_bad_shapes_progress_revision_and_unique_ids_fail_instead_of_truncating(self):
        cases = [
            ('offset', True), ('total', 10001), ('nextOffset', 0), ('nextOffset', True),
            ('items', []), ('items', [{'id': 'same', 'title': 'x'}] * 50),
            ('items', [{'id': str(i), 'title': 'x'} for i in range(51)]),
            ('items', [{'id': '0', 'title': 'x' * 50000}]),
        ]
        for field, bad_value in cases:
            with self.subTest(field=field, value=str(bad_value)[:30]):
                peer = self.peer()
                normal = peer.handler
                def handler(request):
                    reply = normal(request)
                    if request['type'] == 'GET_QUEUE' and reply['type'] == 'RESULT':
                        reply['data'][field] = bad_value
                    return reply
                peer.handler = handler
                await self.ready(peer)
                with self.assertRaises(ValueError):
                    await run_queue_checks(peer.session, [])
        peer = self.peer()
        normal = peer.handler
        def changed_revision(request):
            reply = normal(request)
            if request['type'] == 'GET_QUEUE' and request['args'].get('offset') == 50:
                reply['data']['revision'] = 'new-revision'
            return reply
        peer.handler = changed_revision
        await self.ready(peer)
        with self.assertRaisesRegex(ValueError, 'QUEUE_PAGE_NOT_VERIFIED'):
            await run_queue_checks(peer.session, [])

    async def test_lying_stale_acceptance_and_changed_duplicate_are_rejected(self):
        for fault in ('duplicate', 'stale'):
            peer = self.peer()
            normal = peer.handler
            reads = [0]
            def handler(request):
                reply = normal(request)
                if request['type'] == 'GET_QUEUE':
                    reads[0] += 1
                    if fault == 'duplicate' and reads[0] == 4:
                        reply['data']['revision'] = 'changed-cached-reply'
                    elif fault == 'stale' and reply['type'] == 'ERROR':
                        reply.update(type='RESULT', data={'items': []})
                return reply
            peer.handler = handler
            await self.ready(peer)
            expected = 'QUEUE_DUPLICATE_RESULT_CHANGED' if fault == 'duplicate' else 'QUEUE_STALE_REVISION_NOT_REJECTED'
            with self.assertRaisesRegex(ValueError, expected):
                await run_queue_checks(peer.session, [])

    async def test_source_change_or_playing_state_cannot_be_reported_unchanged(self):
        for initial_playing in (True, False):
            peer = self.peer(0)
            normal = peer.handler
            state_reads = [0]
            def handler(request):
                reply = normal(request)
                if request['type'] == 'GET_STATE':
                    state_reads[0] += 1
                    if initial_playing:
                        reply['data']['state']['playing'] = True
                    elif state_reads[0] == 4:
                        reply['data']['state']['sourceEpoch'] = 'replaced-source'
                return reply
            peer.handler = handler
            await self.ready(peer)
            expected = 'PAUSED_SOURCE_REQUIRED' if initial_playing else 'SOURCE_STATE_CHANGED_DURING_QUEUE_READ'
            with self.assertRaisesRegex(ValueError, expected):
                await run_queue_checks(peer.session, [])

    async def test_diagnostic_page_limit_refuses_an_incomplete_walk(self):
        peer = self.peer(201, 1)
        await self.ready(peer)
        checks = []
        with self.assertRaisesRegex(ValueError, 'DIAGNOSTIC_QUEUE_PAGE_LIMIT'):
            await run_queue_checks(peer.session, checks)
        self.assertEqual(sum(r['type'] == 'GET_QUEUE' for r in peer.requests), 200)
        self.assertEqual(checks, [])


if __name__ == '__main__':
    unittest.main()
