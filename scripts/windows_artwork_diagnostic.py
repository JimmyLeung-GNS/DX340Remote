"""Selected paused source's public artwork; no playback, source change or file paths."""
import base64
import binascii
import hashlib
import re
import time
import uuid

MAX_ARTWORK = 44 * 1024


def verify_artwork(reply, key):
    data = reply.get('data')
    if (reply.get('type') != 'RESULT' or not isinstance(data, dict) or
            set(data) != {'key', 'mime', 'base64'} or data.get('key') != key or
            data.get('mime') != 'image/jpeg' or not isinstance(data.get('base64'), str) or
            not 4 <= len(data['base64']) <= 4 * ((MAX_ARTWORK + 2) // 3)):
        raise ValueError('ARTWORK_REPLY_NOT_VERIFIED')
    try:
        content = base64.b64decode(data['base64'], validate=True)
    except (ValueError, binascii.Error):
        raise ValueError('ARTWORK_ENCODING_NOT_VERIFIED') from None
    if (not 4 <= len(content) <= MAX_ARTWORK or
            base64.b64encode(content).decode('ascii') != data['base64'] or
            not content.startswith(b'\xff\xd8') or not content.endswith(b'\xff\xd9') or
            hashlib.sha256(content).hexdigest() != key):
        raise ValueError('ARTWORK_CONTENT_NOT_VERIFIED')
    return content


async def run_artwork_checks(session, checks):
    before = await session.exchange('GET_STATE')
    state = before['data'].get('state')
    if before['type'] != 'RESULT' or not isinstance(state, dict) or state.get('playing') is not False:
        raise ValueError('PAUSED_SOURCE_REQUIRED')
    key = state.get('artworkKey')
    if not isinstance(key, str) or not re.fullmatch(r'[a-f0-9]{64}', key):
        raise ValueError('CURRENT_ARTWORK_REQUIRED')
    source, epoch = state.get('source'), state.get('sourceEpoch')
    async def read(candidate, request_id=None):
        return await session.exchange('GET_ARTWORK', request_id, args={'key': candidate}, source=source, source_epoch=epoch)
    request_id = str(uuid.uuid4())
    started = time.monotonic()
    frames_before = session.frames_received
    original = await read(key, request_id)
    content = verify_artwork(original, key)
    metrics = {'decodedBytes': len(content), 'sha256': key, 'replyBytes': session.last_reply_bytes,
               'receivedFrames': session.frames_received - frames_before,
               'elapsedMs': round((time.monotonic() - started) * 1000, 3)}
    checks.append({'check': 'bounded public JPEG/base64 markers and SHA256 after complete reassembly', 'passed': True})
    if await read(key, request_id) != original:
        raise ValueError('ARTWORK_DUPLICATE_RESULT_CHANGED')
    checks.append({'check': 'duplicate artwork ID returns identical cached result', 'passed': True})
    absent_key = hashlib.sha256(('diagnostic-absent-' + str(uuid.uuid4())).encode()).hexdigest()
    rejected = await read(absent_key)
    if rejected['type'] != 'ERROR' or rejected['data'] != {'code': 'EXPIRED_ITEM'}:
        raise ValueError('ARTWORK_EXPIRED_KEY_NOT_REJECTED')
    checks.append({'check': 'non-current artwork key rejected without image data', 'passed': True})
    refreshed = await read(key)
    if verify_artwork(refreshed, key) != content:
        raise ValueError('ARTWORK_FRESH_READ_CHANGED')
    after = await session.exchange('GET_STATE')
    current = after['data'].get('state')
    if (after['type'] != 'RESULT' or not isinstance(current, dict) or
            any(current.get(field) != state.get(field) for field in
                ('source', 'sourceEpoch', 'playing', 'title', 'artist', 'artworkKey', 'volume'))):
        raise ValueError('SOURCE_STATE_CHANGED_DURING_ARTWORK_READ')
    checks.append({'check': 'fresh artwork recovery and selected paused state/volume retained', 'passed': True})
    return dict(source=source, **metrics,
                scope='PC BLE public artwork integrity; JPEG markers are not full image decoding, phone caching or control-priority latency acceptance')
