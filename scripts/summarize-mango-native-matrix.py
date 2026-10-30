"""Summarize recorded native attempts; never creates listening confirmations."""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path


def summarize(plan, attempts):
    candidates = {row['sequence']: row for row in plan['selections']}
    accepted = []
    for attempt in attempts:
        expected = candidates.get(attempt.get('sequence'))
        if expected is None or attempt.get('kind') != 'actual-offline-native-Mango-production-selection-no-BLE' or attempt.get('candidate') != expected:
            continue
        accepted.append(attempt)
    confirmed = [r for r in accepted if r['status'] == 'USER_CONFIRMED_AUDIBLE' and
                 r.get('humanResult') == 'Correct' and r.get('humanAudio') and r.get('observedCorrectPlayingState')]
    per_sequence = {}
    for result in confirmed:
        per_sequence.setdefault(result['sequence'], []).append(result)
    if any(len(rows) > 1 for rows in per_sequence.values()):
        raise ValueError('Repeated confirmations for a sequence require manual audit, not double counting')
    hashes = sorted({r.get('installedCompanionSha256') for r in confirmed if r.get('installedCompanionSha256')})
    entries = []
    for sequence, candidate in sorted(candidates.items()):
        passed = per_sequence.get(sequence, [])
        entries.append({'sequence': sequence, 'parent': candidate['parent'], 'page': candidate['page'],
                        'ordinal': candidate['ordinal'], 'title': candidate['expectedTitle'],
                        'artist': candidate['expectedArtist'], 'albumLabel': candidate['albumLabel'],
                        'humanConfirmed': bool(passed), 'userQuote': passed[0]['humanAudio'] if passed else None,
                        'confirmedUtc': passed[0].get('confirmedUtc') if passed else None,
                        'attemptCount': sum(r['sequence'] == sequence for r in accepted),
                        'confirmationSnapshotMatchesOriginalTitle':
                            passed[0].get('confirmationObservedState', {}).get('title') == candidate['expectedTitle'] if passed else None,
                        'pauseObserved': passed[0].get('pausedAfterConfirmation') if passed else None})
    pending = [r['sequence'] for r in entries if not r['humanConfirmed']]
    return {'kind': 'native-Mango-boundary-matrix-summary', 'createdUtc': datetime.now(timezone.utc).isoformat(),
            'plannedNativeLocations': len(candidates), 'confirmedNativeLocations': len(per_sequence),
            'totalAttempts': len(accepted), 'failedAttempts': sum(r['status'].startswith('FAILED') for r in accepted),
            'pendingSequences': pending, 'confirmedCompanionHashes': hashes,
            'allPlannedLocationsConfirmedOnOneBuild': not pending and len(hashes) == 1 and
                all(r.get('installedCompanionSha256') == hashes[0] for r in confirmed),
            'selections': entries,
            'scope': 'Native positions including intros/endings; no20-distinct-song/unique-file/format/phone/BLE/audio-quality acceptance. Confirmation snapshots can follow natural track advance.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', type=Path, required=True)
    parser.add_argument('--attempt-root', type=Path, default=Path('artifacts/mango-native-matrix'))
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    if not args.output.resolve().is_relative_to((repo / 'artifacts').resolve()):
        raise SystemExit('Write summaries under artifacts; tracked publication is separately reviewed')
    plan = json.loads(args.plan.read_text(encoding='utf-8-sig'))
    attempts = [json.loads(p.read_text(encoding='utf-8-sig')) for p in sorted(args.attempt_root.glob('*/summary.json'))]
    summary = summarize(plan, attempts)
    args.output.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(f"Confirmed {summary['confirmedNativeLocations']}/{summary['plannedNativeLocations']}; attempts {summary['totalAttempts']}; failures {summary['failedAttempts']}")


if __name__ == '__main__':
    main()
