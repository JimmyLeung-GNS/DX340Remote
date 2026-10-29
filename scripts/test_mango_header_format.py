import struct
import unittest
from mango_header_format import inspect_header, text_tags


class MangoHeaderTest(unittest.TestCase):
    def test_rfc9639_example_one_header(self):
        # RFC9639 D.1.1, first42 bytes only, no example audio frame.
        data = bytes.fromhex("664c6143800000221000100000000f00000f0ac442f0000000013e84b41807dc690307586a3dad1a2e0f")
        facts = inspect_header(data, "FLAC")
        self.assertEqual((44100, 16, 2), (facts["sampleRateHz"], facts["bitsPerSample"], facts["channels"]))

    def wave(self, payload, leading=b""):
        return b"RIFF" + struct.pack("<I", 4+len(leading)+8+len(payload)) + b"WAVE" + leading + b"fmt " + struct.pack("<I", len(payload)) + payload

    def test_pcm_wave_with_odd_sized_unknown_chunk(self):
        fmt = struct.pack("<HHIIHH", 1, 2, 96000, 576000, 6, 24)
        facts = inspect_header(self.wave(fmt, b"JUNK\x01\0\0\0x\0"), "WAV")
        self.assertEqual((96000, 24, 2), (facts["sampleRateHz"], facts["validBitsPerSample"], facts["channels"]))

    def test_extensible_container_width_is_separate_from_valid_precision(self):
        fmt = struct.pack("<HHIIHHHHI", 65534, 2, 48000, 384000, 8, 32, 22, 24, 3) + bytes.fromhex("0100000000001000800000aa00389b71")
        facts = inspect_header(self.wave(fmt), "WAV")
        self.assertEqual(32, facts["bitsPerSample"])
        self.assertEqual(24, facts["validBitsPerSample"])
        self.assertEqual(48000, facts["sampleRateHz"])

    def test_malformed_truncated_mislabeled_and_overbound_headers_reject(self):
        pcm = self.wave(struct.pack("<HHIIHH", 1, 2, 44100, 176400, 4, 16))
        for data, expected in ((b"fLaC", "FLAC"), (pcm, "FLAC"), (pcm[:28], "WAV"), (b"x" * 257, "WAV"),
                               (b"fLaC\x80\0\0\x22" + bytes(34), "FLAC")):
            with self.subTest(data=data[:12], expected=expected), self.assertRaises(ValueError):
                inspect_header(data, expected)

    def test_wave_container_without_format_in_prefix_does_not_invent_audio_parameters(self):
        facts = inspect_header(b"RIFF\0\0\0\0WAVEJUNK\xff\xff\xff\xff", "WAV")
        self.assertEqual("WAV", facts["format"])
        self.assertIsNone(facts["sampleRateHz"])
        self.assertIsNone(facts["bitsPerSample"])

    def test_complete_id3_title_is_not_guessed_from_filename_or_partial_cover_frame(self):
        for version, codec, payload in ((4, 3, "Delusion:All".encode()), (3, 1, "白昼夢".encode("utf-16"))):
            text = bytes([codec]) + payload + (b"\0\0" if codec == 1 else b"\0")
            frame = b"TIT2" + len(text).to_bytes(4, "big") + b"\0\0" + text
            tag = b"ID3" + bytes([version, 0, 0]) + b"\0\0\0\x40" + frame + b"APIC\0\0\x7f\x7f\0\0"
            wave = b"RIFF\0\0\0\0WAVEid3 " + len(tag).to_bytes(4, "little") + tag
            self.assertEqual("Delusion:All" if version == 4 else "白昼夢", text_tags(wave)["titleTag"])
        self.assertNotIn("titleTag", text_tags(wave[:-12]))

    def test_vorbis_comment_title_within_bounded_prefix(self):
        comment = b"TITLE=Starting"
        payload = struct.pack("<I", 4) + b"test" + struct.pack("<II", 1, len(comment)) + comment
        prefix = b"fLaC\0\0\0\x22" + bytes(34) + b"\x84" + len(payload).to_bytes(3, "big") + payload
        self.assertEqual("Starting", text_tags(prefix)["titleTag"])
        self.assertNotIn("titleTag", text_tags(prefix[:-1]))

    def test_id3_frame_outside_declared_tag_size_is_not_a_title(self):
        frame = b"TIT2\0\0\0\x05\0\0\x03fake"
        tag = b"ID3\x04\0\0\0\0\0\0" + frame
        wave = b"RIFF\0\0\0\0WAVEid3 " + len(tag).to_bytes(4, "little") + tag
        self.assertNotIn("titleTag", text_tags(wave))


if __name__ == "__main__":
    unittest.main()
