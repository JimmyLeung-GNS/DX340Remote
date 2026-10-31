"""Synthetic analyser tests, never physical Bluetooth or timing evidence."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

SPEC = importlib.util.spec_from_file_location('reports', Path(__file__).resolve().parents[1] / 'summarize-connection-reports.py')
reports = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(reports)


def report(rows=None, session='session-A'):
    return {'formatVersion': 1, 'clock': 'monotonic_ms_relative_to_model', 'capacity': 256,
            'evictedRequests': 0, 'evictedConnections': 0, 'requests': rows or [], 'connections': [],
            'modelInstanceId': session, 'platform': 'android', 'authorized': False,
            'connectionPhase': 'idle', 'privacy': 'fixture', 'timingScope': 'fixture',
            'app': {'id': 'fixture.app', 'versionName': 'fixture', 'versionCode': 1, 'baseApkSha256': 'a' * 64},
            'os': {'version': 'fixture', 'api': 36, 'model': 'fixture'}}


def row(id='request-A', **changes):
    value = {'id': id, 'operation': 'PLAY_ITEM', 'source': 'mango', 'startedMs': 10,
             'accepted': False, 'acceptedAfterMs': None, 'finishedAfterMs': None,
             'result': 'pending', 'terminalApplied': False, 'hostOutcome': 'none', 'error': None}
    value.update(changes)
    return value


class ReportsTest(unittest.TestCase):
    def test_empty_and_missing_durations_never_imply_pass_or_zero(self):
        value = reports.summarize([report()])
        self.assertEqual(value['uniqueRequests'], 0)
        self.assertEqual(value['groups'], [])
        self.assertEqual(value['physicalT04'], 'NOT_ADJUDICATED')
        value = reports.summarize([report([row(result='result', terminalApplied=True)])])
        self.assertEqual(value['groups'][0]['appliedResult'], {'samples': 0, 'p50Ms': None, 'p95Ms': None, 'maxMs': None})

    def test_overlapping_exports_merge_in_either_order_without_double_counting(self):
        pending = row(accepted=True, acceptedAfterMs=20)
        complete = row(accepted=True, acceptedAfterMs=20, finishedAfterMs=80, result='result', terminalApplied=True, hostOutcome='observed')
        a, b = report([pending]), report([complete])
        before = copy.deepcopy([a, b])
        for inputs in ([a, b, a, b], [b, a, b, a]):
            value = reports.summarize(inputs)
            self.assertEqual(value['uniqueRequests'], 1)
            group = value['groups'][0]
            self.assertEqual(group['acceptedReceipt']['p95Ms'], 20)
            self.assertEqual(group['hostObservedAppliedResult']['samples'], 1)
        self.assertEqual([a, b], before)

    def test_applied_observed_dispatched_error_and_loss_are_separate(self):
        rows = [row('observed', result='result', finishedAfterMs=100, terminalApplied=True, hostOutcome='observed'),
                row('dispatched', result='result', finishedAfterMs=200, terminalApplied=True, hostOutcome='dispatched'),
                row('ignored', result='result', finishedAfterMs=300, hostOutcome='observed'),
                row('error', result='error', finishedAfterMs=400, terminalApplied=True, error='SOURCE_BUSY'),
                row('lost', result='connection_lost', finishedAfterMs=500, accepted=True, acceptedAfterMs=50)]
        group = reports.summarize([report(rows)])['groups'][0]
        self.assertEqual(group['terminalReply']['samples'], 4)
        self.assertEqual(group['appliedResult']['samples'], 2)
        self.assertEqual(group['hostObservedAppliedResult']['samples'], 1)
        self.assertEqual(group['acceptedReceipt']['samples'], 1)
        self.assertEqual(group['outcomes'], {'connection_lost': 1, 'error': 1, 'result': 3})

    def test_conflicting_receipts_and_identities_are_rejected(self):
        base = row(accepted=True, acceptedAfterMs=20, result='result', finishedAfterMs=80)
        for key, value in [('operation', 'NEXT'), ('source', 'apple'), ('startedMs', 11),
                           ('acceptedAfterMs', 21), ('finishedAfterMs', 81), ('hostOutcome', 'observed')]:
            changed = dict(base, **{key: value})
            with self.subTest(key=key), self.assertRaises(reports.ReportError):
                reports.summarize([report([base]), report([changed])])
        with self.assertRaises(reports.ReportError):
            reports.summarize([report([row(result='result', finishedAfterMs=80)]), report([row(accepted=True, acceptedAfterMs=20)])])

    def test_sessions_require_explicit_combination_builds_remain_separate(self):
        a, b = report([row()]), report([row()], 'session-B')
        with self.assertRaises(reports.ReportError):
            reports.summarize([a, b])
        value = reports.summarize([a, b], allow_multiple_sessions=True)
        self.assertEqual(value['uniqueRequests'], 2)
        b['app']['baseApkSha256'] = 'b' * 64
        with self.assertRaises(reports.ReportError):
            reports.summarize([a, b], allow_multiple_sessions=True)

    def test_eviction_counts_take_session_maxima_not_sum_of_exports(self):
        a, b = report(), report()
        a['evictedRequests'], b['evictedRequests'] = 100, 120
        a['evictedConnections'], b['evictedConnections'] = 4, 5
        value = reports.summarize([a, b, a])
        self.assertEqual(value['evictedRequestsAtLeast'], 120)
        self.assertEqual(value['evictedConnectionsAtLeast'], 5)

    def test_nearest_rank_and_clock_regression_do_not_manufacture_samples(self):
        values = [row(str(i), result='result', terminalApplied=True, finishedAfterMs=i) for i in range(1, 21)]
        values.append(row('bad-clock', startedMs=None, result='result', terminalApplied=True, finishedAfterMs=0))
        group = reports.summarize([report(values)])['groups'][0]
        self.assertEqual(group['appliedResult'], {'samples': 20, 'p50Ms': 10, 'p95Ms': 19, 'maxMs': 20})
        for invalid in [-1, True, 1.5]:
            with self.subTest(value=invalid), self.assertRaises(reports.ReportError):
                reports.summarize([report([row(finishedAfterMs=invalid)])])

    def test_bounded_read_duplicate_keys_and_unexpected_metadata_are_rejected(self):
        value = report()
        value['musicTitle'] = 'must never be summarized'
        with self.assertRaises(reports.ReportError):
            reports.summarize([value])
        with self.assertRaises(reports.ReportError):
            reports.summarize([report([row(source='private-title')])])
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'report.json'
            path.write_text('{"formatVersion":1,"formatVersion":1}')
            with self.assertRaises(reports.ReportError):
                reports.read_report(path)
            path.write_bytes(b' ' * 262145)
            with self.assertRaises(reports.ReportError):
                reports.read_report(path)
            path.write_text(json.dumps(report()))
            self.assertEqual(reports.read_report(path), report())


if __name__ == '__main__':
    unittest.main()
