import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('checkpoint', Path(__file__).with_name('verify-device-checkpoint.py'))
checkpoint = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checkpoint)


class CheckpointIntegrity(unittest.TestCase):
    def fixture(self, root):
        bundle = root / 'dx340-checkpoint.bundle'
        bundle.write_bytes(b'explicit integrity fixture, not a Git bundle')
        return {'formatVersion': 1, 'sourceBranch': 'codex/source-recovery-checkpoint', 'sourceCommit': 'a' * 40,
                'files': [{'file': bundle.name, 'sha256': hashlib.sha256(bundle.read_bytes()).hexdigest()}]}

    def test_valid_hash_inventory(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            manifest = self.fixture(root)
            (root / 'checkpoint.json').write_text(json.dumps(manifest))
            self.assertEqual(checkpoint.verify(root)['sourceCommit'], 'a' * 40)

    def test_corrupt_source_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            manifest = self.fixture(root)
            (root / 'checkpoint.json').write_text(json.dumps(manifest))
            (root / 'dx340-checkpoint.bundle').write_bytes(b'corrupted')
            with self.assertRaisesRegex(ValueError, 'SHA256 mismatch'):
                checkpoint.verify(root)

    def test_path_traversal_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp) / 'kit'
            root.mkdir()
            manifest = self.fixture(root)
            manifest['files'][0]['file'] = '../outside'
            (root / 'checkpoint.json').write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, 'Invalid'):
                checkpoint.verify(root)

    def test_duplicate_file_entries_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            manifest = self.fixture(root)
            manifest['files'] *= 2
            (root / 'checkpoint.json').write_text(json.dumps(manifest))
            with self.assertRaisesRegex(ValueError, 'duplicate'):
                checkpoint.verify(root)


if __name__ == '__main__':
    unittest.main()
