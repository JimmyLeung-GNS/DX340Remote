"""Synthetic arithmetic/provenance guards only; no physical battery measurements."""
import copy
from datetime import datetime, timedelta, timezone
import hashlib
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('battery', Path(__file__).resolve().parents[1] / 'summarize-battery-trials.py')
battery = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(battery)


class BatteryTrialsTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)

    def trial(self, index, mode, start=80, end=75, **changes):
        start_time = datetime(2026, 1, 1, tzinfo=timezone.utc) + timedelta(hours=index * 3)
        evidence = {}
        for endpoint in ('start', 'end'):
            name = f'{index}-{endpoint}.txt'
            data = f'SYNTHETIC FIXTURE ONLY: {index} {endpoint}'.encode()
            (self.base / name).write_bytes(data)
            evidence[endpoint] = {'path': name, 'sha256': hashlib.sha256(data).hexdigest()}
        configuration = {k: 'fixture' for k in battery.CONFIG}
        for key in ('companionApkSha256', 'controllerApkSha256', 'playbackSetSha256'):
            configuration[key] = 'a' * 64
        configuration['networkPolicy'] = 'wifi_off_cellular_off_bluetooth_on'
        value = {'id': f'trial-{index}', 'pair': f'pair-{index // 2}', 'mode': mode, 'status': 'recorded',
                 'configuration': configuration, 'startedAt': start_time.isoformat(),
                 'endedAt': (start_time + timedelta(hours=2)).isoformat(), 'startPercent': start, 'endPercent': end,
                 'pluggedAtStart': False, 'pluggedAtEnd': False, 'sourcePlayingAtStart': True, 'sourcePlayingAtEnd': True,
                 'serviceState': 'stopped' if mode == 'off' else 'enabled',
                 'controllerState': 'disconnected' if mode == 'off' else 'connected',
                 'conditions': {k: True for k in battery.FLAGS}, 'evidence': evidence}
        value.update(changes)
        return value

    def summarize(self, runs):
        return battery.summarize({'formatVersion': 1, 'runs': runs}, self.base)

    def test_six_distinct_matched_fixtures_compare_pp_without_a_physical_pass(self):
        runs = [self.trial(i, 'off' if i % 2 == 0 else 'on', end=75 if i % 2 == 0 else 72) for i in range(6)]
        before = copy.deepcopy(runs)
        result = self.summarize(runs)
        self.assertTrue(result['threeMatchedPairsRecorded'])
        self.assertEqual(result['comparablePairs'], 3)
        self.assertEqual(result['meanAdditionalDrainPercentagePoints'], 3)
        self.assertEqual(result['physicalT13'], 'NOT_ADJUDICATED')
        self.assertEqual(runs, before)

    def test_template_is_untested_not_a_zero_drain_success(self):
        template = self.trial(0, 'off', status='not_tested', startedAt=None, endedAt=None, startPercent=None, endPercent=None)
        result = self.summarize([template])
        self.assertEqual(result['recordedAttempts'], 0)
        self.assertEqual(result['eligibleTrials'], 0)
        self.assertIsNone(result['meanAdditionalDrainPercentagePoints'])
        self.assertFalse(result['threeMatchedPairsRecorded'])
        self.assertEqual(self.summarize([])['comparablePairs'], 0)

    def test_short_powered_interrupted_or_wrong_mode_trials_remain_excluded(self):
        for changes in [dict(endedAt='2026-01-01T00:30:00+00:00'), dict(pluggedAtEnd=True),
                        dict(sourcePlayingAtStart=False), dict(controllerState='connected'), dict(endPercent=81),
                        dict(conditions={k: False for k in battery.FLAGS})]:
            with self.subTest(changes=changes):
                result = self.summarize([self.trial(0, 'off', **changes), self.trial(1, 'on')])
                self.assertEqual(result['comparablePairs'], 0)
                self.assertFalse(result['runs'][0]['eligible'])
                self.assertIsNone(result['meanAdditionalDrainPercentagePoints'])

    def test_mismatched_build_output_start_band_and_duration_do_not_form_a_pair(self):
        for key in ('companionApkSha256', 'volume', 'outputProfile', 'sourceVersion'):
            a, b = self.trial(0, 'off'), self.trial(1, 'on')
            b['configuration'][key] = 'b' * 64 if key.endswith('Sha256') else 'different'
            with self.subTest(key=key):
                self.assertEqual(self.summarize([a, b])['comparablePairs'], 0)
        self.assertEqual(self.summarize([self.trial(0, 'off'), self.trial(1, 'on', start=74)])['comparablePairs'], 0)
        a, b = self.trial(0, 'off'), self.trial(1, 'on')
        b['endedAt'] = (datetime.fromisoformat(b['endedAt']) + timedelta(seconds=31)).isoformat()
        self.assertEqual(self.summarize([a, b])['comparablePairs'], 0)

    def test_overlapping_trials_across_pairs_are_not_repetitions(self):
        a, b = self.trial(0, 'off'), self.trial(1, 'on')
        c = self.trial(2, 'off', startedAt=a['startedAt'], endedAt=a['endedAt'])
        result = self.summarize([a, b, c])
        self.assertEqual(result['comparablePairs'], 0)
        self.assertIn('overlapping_trial_on_same_player', result['runs'][0]['reasons'])

    def test_repeated_files_copied_bytes_tampered_evidence_and_path_escape_rejected(self):
        for kind in ('same-path', 'copied-bytes', 'bad-hash', 'escape', 'empty'):
            a, b = self.trial(0, 'off'), self.trial(1, 'on')
            if kind == 'same-path':
                b['evidence']['start'] = dict(a['evidence']['start'])
            elif kind == 'copied-bytes':
                source, dest = a['evidence']['start'], b['evidence']['start']
                (self.base / dest['path']).write_bytes((self.base / source['path']).read_bytes())
                dest['sha256'] = source['sha256']
            elif kind == 'bad-hash':
                b['evidence']['start']['sha256'] = 'b' * 64
            elif kind == 'escape':
                b['evidence']['start']['path'] = '../outside.txt'
            else:
                receipt = b['evidence']['start']
                (self.base / receipt['path']).write_bytes(b'')
                receipt['sha256'] = hashlib.sha256(b'').hexdigest()
            with self.subTest(kind=kind), self.assertRaises(battery.TrialError):
                self.summarize([a, b])

    def test_duplicate_attempts_modes_and_untyped_levels_are_rejected(self):
        a, b = self.trial(0, 'off'), self.trial(1, 'off')
        for values in ([a, a], [a, b], [self.trial(0, 'off', startPercent=True)]):
            with self.assertRaises(battery.TrialError):
                self.summarize(values)
        with self.assertRaises(battery.TrialError):
            self.summarize([self.trial(0, 'off', startedAt='2026-01-01T00:00:00')])

    def test_mixed_setups_across_three_pairs_do_not_create_a_pooled_result(self):
        runs = [self.trial(i, 'off' if i % 2 == 0 else 'on') for i in range(6)]
        for run in runs[4:]:
            run['configuration']['outputProfile'] = 'another-output'
        result = self.summarize(runs)
        self.assertEqual(result['comparablePairs'], 3)
        self.assertFalse(result['threeMatchedPairsRecorded'])
        self.assertIsNone(result['meanAdditionalDrainPercentagePoints'])

    def test_unknown_configuration_and_non_ble_only_profile_are_not_accepted(self):
        value = self.trial(0, 'off')
        value['configuration']['gain'] = 'unknown'
        with self.assertRaises(battery.TrialError):
            self.summarize([value])
        value = self.trial(0, 'off')
        value['configuration']['networkPolicy'] = 'wifi_on'
        result = self.summarize([value])
        self.assertFalse(result['runs'][0]['eligible'])

    def test_on_then_off_order_and_reported_excess_remain_visible(self):
        result = self.summarize([self.trial(0, 'on', end=68), self.trial(1, 'off')])
        self.assertEqual(result['pairs'][0]['order'], ['on', 'off'])
        self.assertEqual(result['meanAdditionalDrainPercentagePoints'], 7)
        self.assertFalse(result['pairs'][0]['withinProposedFivePointTarget'])
        self.assertEqual(result['physicalT13'], 'NOT_ADJUDICATED')

    def test_powershell_uppercase_digests_match_same_build_and_actual_bytes(self):
        a, b = self.trial(0, 'off'), self.trial(1, 'on')
        for key in ('companionApkSha256', 'controllerApkSha256', 'playbackSetSha256'):
            b['configuration'][key] = b['configuration'][key].upper()
        for receipt in b['evidence'].values():
            receipt['sha256'] = receipt['sha256'].upper()
        self.assertEqual(self.summarize([a, b])['comparablePairs'], 1)

    def test_cli_rejects_duplicate_json_keys_without_a_summary(self):
        path = self.base / 'ambiguous.json'
        path.write_text('{"formatVersion":1,"runs":[],"runs":[]}', encoding='utf-8')
        result = subprocess.run([sys.executable, SPEC.origin, str(path)], capture_output=True, text=True, check=False)
        self.assertEqual(result.returncode, 2)
        self.assertEqual(result.stdout, '')
        self.assertIn('Duplicate JSON key', result.stderr)


if __name__ == '__main__':
    unittest.main()
