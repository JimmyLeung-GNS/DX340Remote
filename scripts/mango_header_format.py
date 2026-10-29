"""Bounded container metadata parser, not a decoder or output-quality test."""
import struct


def text_tags(data: bytes):
    tags = {}
    if data[:4] in (b"RIFF", b"RF64"):
        offset = 12
        while offset + 8 <= len(data):
            name, size = data[offset:offset+4], struct.unpack_from("<I", data, offset + 4)[0]
            if name.lower() == b"id3 ":
                start = offset + 8
                if data[start:start+3] != b"ID3" or start + 10 > len(data):
                    break
                version, flags = data[start+3], data[start+5]
                if version not in (3, 4) or flags & ~0x10:
                    break  # Do not guess through unsynchronization/extended headers.
                pos = start + 10
                while pos + 10 <= min(offset + 8 + size, len(data)):
                    frame, encoded_size = data[pos:pos+4], data[pos+4:pos+8]
                    if version == 4 and any(value & 128 for value in encoded_size):
                        break
                    length = int.from_bytes(encoded_size, "big") if version == 3 else sum(value << (7 * (3-i)) for i, value in enumerate(encoded_size))
                    if not length or pos + 10 + length > min(offset + 8 + size, len(data)):
                        break
                    key = {b"TIT2": "titleTag", b"TPE1": "artistTag", b"TALB": "albumTag"}.get(frame)
                    if key and data[pos+8:pos+10] == b"\0\0":
                        payload = data[pos+10:pos+10+length]
                        codec = {0: "latin1", 1: "utf-16", 2: "utf-16-be", 3: "utf-8"}.get(payload[0])
                        if codec:
                            try:
                                tags[key] = payload[1:].decode(codec).rstrip("\0")
                            except UnicodeError:
                                pass
                    pos += 10 + length
                break
            if name == b"data":
                break
            offset += 8 + size + (size & 1)
    elif data[:4] == b"fLaC":
        offset = 4
        while offset + 4 <= len(data):
            kind, size = data[offset] & 127, int.from_bytes(data[offset+1:offset+4], "big")
            if kind == 4:
                pos = offset + 4
                if pos + 4 > len(data):
                    break
                vendor_length = struct.unpack_from("<I", data, pos)[0]
                pos += 4 + vendor_length
                if pos + 4 > min(offset + 4 + size, len(data)):
                    break
                count = struct.unpack_from("<I", data, pos)[0]; pos += 4
                for _ in range(min(count, 50)):
                    if pos + 4 > min(offset + 4 + size, len(data)):
                        break
                    length = struct.unpack_from("<I", data, pos)[0]; pos += 4
                    if pos + length > min(offset + 4 + size, len(data)):
                        break
                    try:
                        field = data[pos:pos+length].decode("utf-8")
                        key, _, value = field.partition("=")
                        name = {"TITLE": "titleTag", "ARTIST": "artistTag", "ALBUM": "albumTag"}.get(key.upper())
                        if name:
                            tags[name] = value
                    except UnicodeError:
                        pass
                    pos += length
                break
            offset += 4 + size
    return tags


def inspect_header(data: bytes, expected_format: str):
    if not 12 <= len(data) <= 256:
        raise ValueError("Bounded public header unavailable; do not infer format from filename")
    result = {"sampleRateHz": None, "bitsPerSample": None, "validBitsPerSample": None, "channels": None}
    if data[:4] == b"fLaC" and len(data) >= 42 and data[4] & 127 == 0 and int.from_bytes(data[5:8], "big") == 34:
        result["format"] = "FLAC"
        stream = int.from_bytes(data[18:26], "big")
        rate, channels, bits = stream >> 44, ((stream >> 41) & 7) + 1, ((stream >> 36) & 31) + 1
        if not rate or not 4 <= bits <= 32:
            raise ValueError("Invalid FLAC STREAMINFO sample rate or bit depth")
        result.update(sampleRateHz=rate, channels=channels, bitsPerSample=bits, validBitsPerSample=bits)
    elif data[:4] in (b"RIFF", b"RF64") and data[8:12] == b"WAVE":
        result["format"] = "WAV"
        offset = 12
        while offset + 8 <= len(data):
            size = struct.unpack_from("<I", data, offset + 4)[0]
            if data[offset:offset+4] == b"fmt ":
                if size < 16 or offset + 24 > len(data):
                    raise ValueError("Incomplete WAVE format chunk in bounded header")
                codec, channels, rate, _, _, bits = struct.unpack_from("<HHIIHH", data, offset + 8)
                if not rate or not channels:
                    raise ValueError("Invalid WAVE sample rate or channel count")
                result.update(sampleRateHz=rate, channels=channels, bitsPerSample=bits, waveFormatTag=codec)
                if codec in (1, 3):
                    result["validBitsPerSample"] = bits
                elif codec == 65534 and size >= 40 and offset + 48 <= len(data):
                    extension_size, valid_bits = struct.unpack_from("<HH", data, offset + 24)
                    if extension_size < 22 or valid_bits > bits:
                        raise ValueError("Invalid WAVEFORMATEXTENSIBLE precision")
                    result["validBitsPerSample"] = valid_bits or None
                break
            offset += 8 + size + (size & 1)
    else:
        raise ValueError("Expected FLAC STREAMINFO or RIFF/RF64 WAVE header not observed")
    if result["format"] != expected_format:
        raise ValueError("Actual public header disagrees with observed filename extension")
    result.update(text_tags(data))
    return result
