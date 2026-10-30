"""Planning guards only: no connected device, listening or source support claim."""
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('native_plan', Path(__file__).with_name('prepare-mango-native-listening-plan.py'))
plan = importlib.util.module_from_spec(spec)
spec.loader.exec_module(plan)
summary_spec = importlib.util.spec_from_file_location('native_summary', Path(__file__).with_name('summarize-mango-native-matrix.py'))
summary_module = importlib.util.module_from_spec(summary_spec)
summary_spec.loader.exec_module(summary_module)


def fixtures():
    albums = []
    for number, size in ((1, 23), (2, 20), (3, 33), (5, 24), (10, 31)):
        albums.append({'id': f'__ARTIST_GENRE_ALBUM__0_0_{number}', 'title': f'Album {number}',
                       'sourceReportedTracks': size,
                       'items': [{'id': f'__MUSIC__0_0_{number}_{i}', 'title': f'Track {i}',
                                  'subtitle': 'Artist', 'playable': True, 'browsable': False}
                                 for i in range(1, size + 1)]})
    music = {'complete': True, 'items': [{'id': f'__ALL_MUSIC__{i}', 'title': f'All {i}',
                                         'subtitle': 'Artist', 'playable': True, 'browsable': False}
                                        for i in range(1, 501)]}
    return albums, music


class NativePlanGuards(unittest.TestCase):
    def test_current_contract_first_last_and_later_windows_without_file_claim(self):
        rows = plan.build_plan(*fixtures())
        self.assertEqual(len(rows), 20)
        self.assertEqual([rows[i]['ordinal'] for i in (0, 1)], [1, 500])
        self.assertEqual(rows[1]['page'], 19)
        self.assertTrue(any(row['page'] == 1 and row['parent'] != '__ALL_MUSIC__' for row in rows))
        self.assertTrue(all(row['status'] == 'NOT TESTED' and row['format'].startswith('UNKNOWN') for row in rows))
        self.assertEqual(len({(row['parent'], row['id']) for row in rows}), 20)

    def test_only_exact_source_appended_ordinal_is_removed(self):
        self.assertEqual(plan.canonical_title({'id': '__MUSIC__0_0_1_5', 'title': 'Intermission 1 5'}), ('Intermission 1', 5))
        self.assertEqual(plan.canonical_title({'id': '__ALL_MUSIC__7', 'title': '777 7'}), ('777', 7))
        for title in ('Track', 'Track 8', 'Track 07'):
            with self.assertRaises(ValueError):
                plan.canonical_title({'id': '__ALL_MUSIC__7', 'title': title})

    def test_incomplete_inventory_count_and_missing_album_are_refused(self):
        albums, music = fixtures()
        music['complete'] = False
        with self.assertRaises(ValueError): plan.build_plan(albums, music)
        albums, music = fixtures()
        albums[0]['sourceReportedTracks'] += 1
        with self.assertRaises(ValueError): plan.build_plan(albums, music)
        albums, music = fixtures()
        with self.assertRaises(ValueError): plan.build_plan(albums[1:], music)

    def test_unplayable_and_assisted_rows_cannot_be_native_candidates(self):
        for mutation in ({'playable': False}, {'uiAssisted': True}, {'browsable': True}, {'id': 'mango-ui:item'}):
            albums, music = fixtures()
            music['items'][0].update(mutation)
            with self.assertRaises(ValueError): plan.build_plan(albums, music)


class NativeSummaryGuards(unittest.TestCase):
    def setUp(self):
        self.plan = {'selections': plan.build_plan(*fixtures())}

    def record(self, sequence=1):
        return {'kind': 'actual-offline-native-Mango-production-selection-no-BLE', 'sequence': sequence,
                'candidate': self.plan['selections'][sequence - 1], 'status': 'USER_CONFIRMED_AUDIBLE',
                'humanResult': 'Correct', 'humanAudio': 'Synthetic test answer',
                'observedCorrectPlayingState': True, 'installedCompanionSha256': 'c' * 64}

    def test_no_human_answer_or_unmatched_candidate_cannot_count_as_a_confirmation(self):
        no_answer = self.record()
        no_answer['humanAudio'] = None
        unknown = self.record()
        unknown.update(sequence=999, candidate=None)
        summary = summary_module.summarize(self.plan, [no_answer, unknown])
        self.assertEqual(summary['confirmedNativeLocations'], 0)
        self.assertEqual(summary['totalAttempts'], 1)
        self.assertFalse(summary['allPlannedLocationsConfirmedOnOneBuild'])

    def test_repeated_confirmation_needs_manual_audit(self):
        with self.assertRaises(ValueError):
            summary_module.summarize(self.plan, [self.record(), self.record()])

    def test_twenty_results_with_missing_or_mixed_build_are_not_a_single_build_run(self):
        records = [self.record(i) for i in range(1, 21)]
        self.assertTrue(summary_module.summarize(self.plan, records)['allPlannedLocationsConfirmedOnOneBuild'])
        records[0]['installedCompanionSha256'] = None
        self.assertFalse(summary_module.summarize(self.plan, records)['allPlannedLocationsConfirmedOnOneBuild'])
        records[0]['installedCompanionSha256'] = 'a' * 64
        self.assertFalse(summary_module.summarize(self.plan, records)['allPlannedLocationsConfirmedOnOneBuild'])


if __name__ == '__main__':
    unittest.main()
