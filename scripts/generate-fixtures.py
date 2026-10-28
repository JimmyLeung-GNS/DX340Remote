"""Deterministic v1 UTF-8 wire fixtures, consumed by Kotlin and Swift tests."""
import json
import pathlib
import struct

root = pathlib.Path(__file__).resolve().parents[1]
fixtures = []
for text, capacity, message_id in [('{"v":1,"id":"a","type":"HELLO"}', 20, 7), ('{"title":"雪解け"}', 20, 65535), ('{}', 244, 0)]:
    payload = text.encode('utf-8')
    width = capacity - 8
    count = (len(payload) + width - 1) // width
    fixtures.append(dict(text=text, capacity=capacity, id=message_id, hex=[(struct.pack('>BBHHH', 0x44, 1, message_id, i, count) + payload[i*width:(i+1)*width]).hex() for i in range(count)]))
destination = root / 'protocol/fixtures/framing.json'
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(json.dumps(fixtures, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
