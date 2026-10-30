"""Synthetic diagnostic regressions; not DX340, radio, image decoding or audio evidence."""
import base64
import copy
import hashlib
import unittest

from test_windows_ble_protocol import Peer
from windows_ble_protocol import run_read_only_checks
from windows_artwork_diagnostic import MAX_ARTWORK, run_artwork_checks, verify_artwork


def reply_for(content):
    key = hashlib.sha256(content).hexdigest()
    return key, {'type': 'RESULT', 'data': {'key': key, 'mime': 'image/jpeg',
                                        'base64': base64.b64encode(content).decode('ascii')}}


class ArtworkContentTests(unittest.TestCase):
    def test_exact_bound_and_hash(self):
        for length in (4, MAX_ARTWORK):
            content = b'\xff\xd8' + b'x' * (length - 4) + b'\xff\xd9'
            key, reply = reply_for(content)
            self.assertEqual(verify_artwork(reply, key), content)

    def test_corrupt_encoding_bounds_mime_identity_and_digest_rejected(self):
        key, valid = reply_for(b'\xff\xd8synthetic\xff\xd9')
        for field, value in [('key', '0' * 64), ('mime', 'text/html'), ('base64', '!!!!'),
                             ('base64', valid['data']['base64'] + '='), ('base64', 'A' * 70000),
                             ('base64', True), ('extra', 'untrusted')]:
            with self.subTest(field=field, value=str(value)[:20]):
                bad = copy.deepcopy(valid)
                bad['data'][field] = value
                with self.assertRaises(ValueError):
                    verify_artwork(bad, key)
        for content in (b'', b'not JPEG', b'\xff\xd8' + b'x' * MAX_ARTWORK + b'\xff\xd9'):
            wrong_key, bad = reply_for(content)
            with self.assertRaises(ValueError):
                verify_artwork(bad, wrong_key)
        bad = copy.deepcopy(valid)
        bad['data']['base64'] = base64.b64encode(b'\xff\xd8changed\xff\xd9').decode()
        with self.assertRaisesRegex(ValueError, 'CONTENT_NOT_VERIFIED'):
            verify_artwork(bad, key)


class ArtworkSessionTests(unittest.IsolatedAsyncioTestCase):
    def peer(self):
        peer = Peer(approved=True)
        peer.session.allow_artwork_reads = True
        key, art = reply_for(b'\xff\xd8' + b'x' * 2048 + b'\xff\xd9')
        def handler(request):
            reply = peer.normal(request)
            state = reply['data'].get('state')
            if state is not None:
                state.update(artworkKey=key, volume={'current': 37, 'verified': False})
            if request['type'] == 'GET_ARTWORK':
                if request['args']['key'] == key:
                    reply['data'] = copy.deepcopy(art['data'])
                else:
                    reply.update(type='ERROR', data={'code': 'EXPIRED_ITEM'})
            return reply
        peer.handler = handler
        return peer, key

    async def test_fragmented_success_duplicate_expired_and_fresh_recovery(self):
        peer, _ = self.peer()
        await run_read_only_checks(peer.session, 'authorized-state')
        checks = []
        result = await run_artwork_checks(peer.session, checks)
        self.assertEqual(result['decodedBytes'], 2052)
        self.assertGreater(result['receivedFrames'], 1)
        self.assertEqual(len(checks), 4)
        requests = [r for r in peer.requests if r['type'] == 'GET_ARTWORK']
        self.assertEqual(len(requests), 4)
        self.assertEqual(requests[0], requests[1])
        self.assertTrue(all(r['sourceEpoch'] == 'source-epoch' for r in requests))
        self.assertEqual({r['type'] for r in peer.requests}, {'HELLO', 'AUTHORIZE', 'GET_STATE', 'GET_ARTWORK'})
        self.assertNotIn('雪', str(result))

    async def test_opt_in_authorization_and_schema_gates_before_writes(self):
        peer, key = self.peer()
        with self.assertRaisesRegex(ValueError, 'AUTHORIZED_ARTWORK_MODE_REQUIRED'):
            await peer.session.exchange('GET_ARTWORK', args={'key': key})
        await run_read_only_checks(peer.session, 'authorized-state')
        baseline = len(peer.requests)
        peer.session.allow_artwork_reads = False
        with self.assertRaisesRegex(ValueError, 'AUTHORIZED_ARTWORK_MODE_REQUIRED'):
            await peer.session.exchange('GET_ARTWORK', args={'key': key})
        peer.session.allow_artwork_reads = True
        for args in ({}, {'key': None}, {'key': '../private'}, {'key': key.upper()}, {'key': key, 'playing': True}):
            with self.assertRaisesRegex(ValueError, 'INVALID_ARTWORK_READ'):
                await peer.session.exchange('GET_ARTWORK', args=args, source='com.ibasso.music', source_epoch='source-epoch')
        for operation in ('SET_PLAYING', 'SELECT_SOURCE', 'ADJUST_VOLUME', 'PLAY_ITEM', 'BROWSE'):
            with self.assertRaisesRegex(ValueError, 'READ_ONLY_OPERATION_REQUIRED'):
                await peer.session.exchange(operation)
        self.assertEqual(len(peer.requests), baseline)

    async def test_missing_current_image_and_active_source_stop_before_image_request(self):
        for field, value, error in [('playing', True, 'PAUSED_SOURCE_REQUIRED'), ('artworkKey', None, 'CURRENT_ARTWORK_REQUIRED')]:
            peer, _ = self.peer()
            normal = peer.handler
            def handler(request):
                reply = normal(request)
                if request['type'] == 'GET_STATE':
                    reply['data']['state'][field] = value
                return reply
            peer.handler = handler
            await run_read_only_checks(peer.session, 'authorized-state')
            with self.assertRaisesRegex(ValueError, error):
                await run_artwork_checks(peer.session, [])
            self.assertFalse(any(r['type'] == 'GET_ARTWORK' for r in peer.requests))

    async def test_wrong_expired_key_response_fails_without_recovery_request(self):
        peer, key = self.peer()
        normal = peer.handler
        def handler(request):
            reply = normal(request)
            if request['type'] == 'GET_ARTWORK' and request['args']['key'] != key:
                reply['data'] = {'code': 'STALE_SOURCE'}
            return reply
        peer.handler = handler
        await run_read_only_checks(peer.session, 'authorized-state')
        with self.assertRaisesRegex(ValueError, 'EXPIRED_KEY_NOT_REJECTED'):
            await run_artwork_checks(peer.session, [])
        self.assertEqual(sum(r['type'] == 'GET_ARTWORK' for r in peer.requests), 3)

    async def test_postflight_volume_or_identity_change_is_not_a_pass(self):
        for field, value in [('sourceEpoch', 'changed'), ('volume', {'current': 36}), ('title', 'changed')]:
            peer, _ = self.peer()
            normal = peer.handler
            def handler(request):
                reply = normal(request)
                if request['type'] == 'GET_STATE' and sum(r['type'] == 'GET_ARTWORK' for r in peer.requests) == 4:
                    reply['data']['state'][field] = value
                return reply
            peer.handler = handler
            await run_read_only_checks(peer.session, 'authorized-state')
            with self.assertRaisesRegex(ValueError, 'SOURCE_STATE_CHANGED'):
                await run_artwork_checks(peer.session, [])


if __name__ == '__main__':
    unittest.main()
