import importlib.util
import json
from pathlib import Path
import tempfile
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('recorder', Path(__file__).parents[1] / 'device-hold-recorder.py')
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)


class RecorderTests(unittest.TestCase):
    def setUp(self):
        self.manifest = {'serial': 'd', 'remoteRoot': '/data/local/tmp/dx340-hold-' + 'a' * 32, 'pid': 123}

    def test_manifest_rejects_unowned_or_traversal_paths(self):
        for root in ['/data/local/tmp', self.manifest['remoteRoot'] + '/..', '/sdcard/Music', '/data/local/tmp/dx340-hold-' + 'g' * 32]:
            with self.subTest(root=root), self.assertRaises(ValueError):
                m.validate_manifest(dict(self.manifest, remoteRoot=root))

    def test_pid_and_serial_validation(self):
        for changes in [{'pid': 1}, {'pid': True}, {'pid': '123'}, {'serial': 'd; reboot'}]:
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                m.validate_manifest(dict(self.manifest, **changes))

    def test_stop_does_not_signal_reused_pid(self):
        with patch.object(m, 'adb', side_effect=[b'RUNNING', b'other-process\0']) as adb:
            with self.assertRaisesRegex(ValueError, 'no signal sent'): m.stop(self.manifest)
            self.assertEqual(adb.call_count, 2)

    def test_stop_verifies_exact_script_before_signal(self):
        command = b'\0'.join(x.encode() for x in ['sh', self.manifest['remoteRoot'] + '/record.sh', self.manifest['remoteRoot'], '10', '2', '0'])
        with patch.object(m, 'adb', side_effect=[b'RUNNING', command, b'']) as adb:
            self.assertIn('verified owned recorder', m.stop(self.manifest))
            self.assertEqual(adb.call_args.args, ('d', 'shell', 'kill', '-TERM', '123'))

    def test_terminal_stop_sends_no_signal(self):
        with patch.object(m, 'adb', return_value=b'COMPLETED') as adb:
            self.assertIn('no signal', m.stop(self.manifest)); self.assertEqual(adb.call_count, 1)

    def test_live_collection_requires_same_job_to_finish(self):
        with patch.object(m, 'adb', return_value=b'RUNNING') as adb:
            with self.assertRaisesRegex(ValueError, 'not terminal'): m.collect(self.manifest, Path('unused'))
            self.assertEqual(adb.call_count, 1)

    def test_unknown_file_inventory_is_not_copied(self):
        with patch.object(m, 'adb', side_effect=[b'FAILED', b'../../other\n']) as adb:
            with self.assertRaisesRegex(ValueError, 'nothing copied'): m.collect(self.manifest, Path('unused'))
            self.assertEqual(adb.call_count, 2)

    def test_conflicting_terminal_state_rejected(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(m, 'adb', side_effect=[b'FAILED', b'summary.tsv\n', b'17', b'status\tCOMPLETED\n']):
            with self.assertRaisesRegex(ValueError, 'disagrees'): m.collect(self.manifest, Path(tmp))

    def test_valid_archive_keeps_acceptance_separate(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(m, 'adb', side_effect=[b'COMPLETED', b'summary.tsv\n', b'17', b'status\tCOMPLETED\n']):
            v = m.collect(self.manifest, Path(tmp))
            self.assertEqual(v['state'], 'COMPLETED'); self.assertIn('NOT ADJUDICATED', v['acceptance'])
            self.assertEqual(v['files'][0]['sha256'], m.hashlib.sha256((Path(tmp) / 'summary.tsv').read_bytes()).hexdigest())

    def test_oversized_file_rejected_before_read(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(m, 'adb', side_effect=[b'FAILED', b'summary.tsv\n', str(8*1024*1024+1).encode()]) as adb:
            with self.assertRaisesRegex(ValueError, 'file not read'): m.collect(self.manifest, Path(tmp))
            self.assertEqual(adb.call_count, 3)

    def test_stale_running_marker_is_not_a_live_handle(self):
        with patch.object(m, 'adb', side_effect=[b'RUNNING', subprocess.CalledProcessError(1, 'adb'), b'RUNNING']):
            self.assertEqual(m.inspect(self.manifest)['state'], 'INCOMPLETE_PROCESS_UNCONFIRMED')

    def test_completion_race_is_read_again(self):
        with patch.object(m, 'adb', side_effect=[b'RUNNING', subprocess.CalledProcessError(1, 'adb'), b'COMPLETED']):
            self.assertEqual(m.inspect(self.manifest)['state'], 'COMPLETED')

    def test_limits_fail_before_device_access(self):
        for duration, interval in [(9, 1), (3601, 30), (1800, 1), (1800, 0), (1800, 61)]:
            with self.subTest(duration=duration, interval=interval), patch.object(m, 'adb') as adb:
                with self.assertRaises(ValueError): m.start('d', Path('unused'), duration, interval, False)
                adb.assert_not_called()

    def test_existing_destination_is_not_overwritten(self):
        with tempfile.TemporaryDirectory() as tmp, patch.object(m, 'adb') as adb:
            with self.assertRaises(FileExistsError): m.start('d', Path(tmp), 10, 2, False)
            adb.assert_not_called()

    def test_empty_pid_read_recovers_same_launch(self):
        command = b'\0'.join(x.encode() for x in ['sh', self.manifest['remoteRoot'] + '/record.sh', self.manifest['remoteRoot'], '10', '2', '0'])
        fixed = type('FixedUUID', (), {'hex': 'a' * 32})()
        replies = [b'', b'', b'', b'123\n', b'', b'123\n', b'ARMING', b'ARMING', command]
        with tempfile.TemporaryDirectory() as tmp, patch.object(m.uuid, 'uuid4', return_value=fixed), patch.object(m, 'adb', side_effect=replies) as adb, patch.object(m.time, 'sleep') as sleep:
            out = Path(tmp) / 'case'
            result = m.start('d', out, 10, 2, False)
            self.assertEqual(result['state'], 'ARMING')
            self.assertTrue(result['ownedProcessLive'])
            self.assertEqual(json.loads((out / 'manifest.json').read_text())['pid'], 123)
            self.assertEqual(sum('nohup sh ' in str(call.args) for call in adb.call_args_list), 1)
            self.assertEqual(sum(call.args == ('d', 'shell', 'cat', self.manifest['remoteRoot'] + '/pid') for call in adb.call_args_list), 2)
            sleep.assert_called_once_with(.2)

    def test_persistent_empty_pid_preserves_manifest_without_relaunch(self):
        fixed = type('FixedUUID', (), {'hex': 'a' * 32})()
        def reply(*args):
            return b'123\n' if 'nohup sh ' in str(args) else b''
        with tempfile.TemporaryDirectory() as tmp, patch.object(m.uuid, 'uuid4', return_value=fixed), patch.object(m, 'adb', side_effect=reply) as adb, patch.object(m.time, 'sleep') as sleep:
            out = Path(tmp) / 'case'
            with self.assertRaisesRegex(RuntimeError, 'Existing launch not confirmed'):
                m.start('d', out, 10, 2, False)
            self.assertEqual(json.loads((out / 'manifest.json').read_text())['pid'], 123)
            self.assertEqual(sum('nohup sh ' in str(call.args) for call in adb.call_args_list), 1)
            self.assertEqual(sleep.call_count, 15)

    def test_malformed_pid_fails_closed_without_relaunch(self):
        replies = [b'', b'', b'', b'123\n', b'not-a-pid']
        with tempfile.TemporaryDirectory() as tmp, patch.object(m, 'adb', side_effect=replies) as adb:
            out = Path(tmp) / 'case'
            with self.assertRaisesRegex(RuntimeError, 'Malformed recorder PID'):
                m.start('d', out, 10, 2, False)
            self.assertEqual(json.loads((out / 'manifest.json').read_text())['pid'], 123)
            self.assertEqual(sum('nohup sh ' in str(call.args) for call in adb.call_args_list), 1)


if __name__ == '__main__': unittest.main()
