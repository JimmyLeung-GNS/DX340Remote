#!/usr/bin/env python3
"""Compare operator-recorded T13 trials; never turn sample data into physical acceptance."""
import argparse
from collections import defaultdict
from datetime import datetime
import hashlib
import json
from pathlib import Path
import re
from statistics import mean

ID = re.compile(r'[A-Za-z0-9_-]{1,64}\Z')
HASH = re.compile(r'[a-fA-F0-9]{64}\Z')
CONFIG = set('playerModel playerOsBuild companionApkSha256 sourcePackage sourceVersion playbackSetSha256 outputProfile volume gain eq screenPolicy networkPolicy controllerModel controllerOsBuild controllerApkSha256'.split())
FLAGS = set('unpluggedThroughoutAttested configurationUnchangedAttested noKnownPlaybackInterruptionAttested clockStableAttested'.split())
RUN_KEYS = set('id pair mode status configuration startedAt endedAt startPercent endPercent pluggedAtStart pluggedAtEnd sourcePlayingAtStart sourcePlayingAtEnd serviceState controllerState conditions evidence'.split())


class TrialError(ValueError):
    pass


def require(value, message):
    if not value:
        raise TrialError(message)


def timestamp(value):
    require(isinstance(value, str), 'A recorded trial needs both endpoint timestamps')
    try:
        result = datetime.fromisoformat(value.replace('Z', '+00:00'))
    except ValueError:
        raise TrialError('Invalid endpoint timestamp') from None
    require(result.utcoffset() is not None, 'Endpoint timestamps must include a timezone')
    return result


def evidence_receipts(receipts, base):
    """Verify only explicitly named files under the caller's evidence directory."""
    require(type(receipts) is dict and set(receipts) == {'start', 'end'}, 'Both endpoint evidence receipts are required')
    paths = []
    for receipt in receipts.values():
        require(type(receipt) is dict and set(receipt) == {'path', 'sha256'}, 'Invalid endpoint evidence receipt')
        name, digest = receipt['path'], receipt['sha256']
        require(isinstance(name, str) and '\\' not in name and ':' not in name, 'Evidence needs a relative slash-separated path')
        parts = Path(name)
        require(not parts.is_absolute() and '..' not in parts.parts, 'Evidence path must stay inside its directory')
        target = (base / parts).resolve()
        require(target.is_relative_to(base.resolve()) and target.is_file(), 'Endpoint evidence file is missing or outside its directory')
        require(isinstance(digest, str) and HASH.fullmatch(digest), 'Invalid endpoint evidence digest')
        require(0 < target.stat().st_size <= 25_000_000, 'Endpoint evidence must be nonempty and at most 25 MB')
        require(hashlib.sha256(target.read_bytes()).hexdigest() == digest.lower(), 'Endpoint evidence digest mismatch')
        paths.append(target)
    require(paths[0] != paths[1], 'Start and end must be distinct endpoint evidence files')
    return paths


def evaluate(run):
    require(type(run) is dict and set(run) == RUN_KEYS, 'Unexpected trial fields')
    require(all(isinstance(run[k], str) and ID.fullmatch(run[k]) for k in ('id', 'pair')), 'Invalid trial/pair identifier')
    require(run['mode'] in ('off', 'on') and run['status'] in ('not_tested', 'recorded'), 'Invalid trial mode/status')
    require(type(run['configuration']) is dict and set(run['configuration']) == CONFIG, 'Unexpected configuration fields')
    require(type(run['conditions']) is dict and set(run['conditions']) == FLAGS, 'Unexpected condition fields')
    summary = {'id': run['id'], 'pair': run['pair'], 'mode': run['mode'], 'eligible': False, 'reasons': []}
    if run['status'] == 'not_tested':
        require(all(run[k] is None for k in ('startedAt', 'endedAt', 'startPercent', 'endPercent')), 'Untested trial cannot contain measured endpoints')
        summary['reasons'] = ['not_tested']
        return summary
    config = run['configuration']
    require(all(isinstance(v, str) and 1 <= len(v) <= 160 for v in config.values()), 'Record the complete configuration for every actual trial')
    require(all(v.casefold().strip() not in {'unknown', 'unverified', 'pending', 'todo'} for v in config.values()), 'Unverified configuration labels cannot establish a matched trial')
    require(all(HASH.fullmatch(config[k]) for k in ('companionApkSha256', 'controllerApkSha256', 'playbackSetSha256')), 'Build and playback-set identities need SHA256 digests')
    require(all(type(run[k]) is int and 0 <= run[k] <= 100 for k in ('startPercent', 'endPercent')), 'Battery endpoint levels must be integer percentages')
    require(all(type(run[k]) is bool for k in ('pluggedAtStart', 'pluggedAtEnd', 'sourcePlayingAtStart', 'sourcePlayingAtEnd')), 'Record endpoint power/playback observations')
    require(all(type(value) is bool for value in run['conditions'].values()), 'Record each operator attestation explicitly')
    require(run['serviceState'] in ('stopped', 'enabled', 'unknown') and run['controllerState'] in ('disconnected', 'connected', 'unknown'), 'Invalid observed service/controller state')
    elapsed = (timestamp(run['endedAt']) - timestamp(run['startedAt'])).total_seconds()
    summary.update(durationSeconds=elapsed, dischargePercentagePoints=run['startPercent'] - run['endPercent'])
    reasons = summary['reasons']
    if not 7200 <= elapsed <= 7260:
        reasons.append('duration_outside_7200_to_7260_seconds')
    if run['pluggedAtStart'] or run['pluggedAtEnd']:
        reasons.append('externally_powered_endpoint')
    if not all(run['conditions'].values()):
        reasons.append('unconfirmed_run_conditions')
    if config['networkPolicy'] != 'wifi_off_cellular_off_bluetooth_on':
        reasons.append('not_the_declared_bluetooth_only_network_profile')
    if not run['sourcePlayingAtStart'] or not run['sourcePlayingAtEnd']:
        reasons.append('source_not_playing_at_both_endpoints')
    expected = ('stopped', 'disconnected') if run['mode'] == 'off' else ('enabled', 'connected')
    if (run['serviceState'], run['controllerState']) != expected:
        reasons.append('service_or_controller_state_does_not_match_trial_mode')
    if run['endPercent'] > run['startPercent']:
        reasons.append('battery_level_increased')
    summary['eligible'] = not reasons
    return summary


def canonical_configuration(run):
    return {k: v.lower() if k.endswith('Sha256') else v for k, v in run['configuration'].items()}


def summarize(document, base):
    require(type(document) is dict and set(document) == {'formatVersion', 'runs'}, 'Unexpected trial document fields')
    require(type(document['formatVersion']) is int and document['formatVersion'] == 1, 'Unsupported trial format')
    runs = document['runs']
    require(type(runs) is list and len(runs) <= 60, 'At most 60 explicit trial attempts are supported')
    records, groups, evidence_owners, evidence_digests = [], defaultdict(dict), set(), set()
    for run in runs:
        result = evaluate(run)
        require(not any(r['id'] == result['id'] for r in records), 'Duplicate trial ID; retries need separate attempt IDs and pairs')
        require(result['mode'] not in groups[result['pair']], 'Duplicate mode in a pair; preserve failed attempts in separate pairs')
        if run['status'] == 'recorded':
            for path in evidence_receipts(run['evidence'], base):
                require(path not in evidence_owners, 'An endpoint evidence file cannot represent multiple trials')
                evidence_owners.add(path)
            for receipt in run['evidence'].values():
                digest = receipt['sha256'].lower()
                require(digest not in evidence_digests, 'Repeated endpoint evidence bytes cannot represent distinct observations')
                evidence_digests.add(digest)
        records.append(result)
        groups[result['pair']][result['mode']] = (run, result)
    actual = [(r, result) for r, result in zip(runs, records) if r['status'] == 'recorded']
    for index, (first, first_result) in enumerate(actual):
        for second, second_result in actual[index + 1:]:
            if timestamp(first['startedAt']) < timestamp(second['endedAt']) and timestamp(second['startedAt']) < timestamp(first['endedAt']):
                for result in (first_result, second_result):
                    result['eligible'] = False
                    if 'overlapping_trial_on_same_player' not in result['reasons']:
                        result['reasons'].append('overlapping_trial_on_same_player')
    pairs = []
    for name, modes in sorted(groups.items()):
        pair = {'pair': name, 'comparable': False, 'reasons': []}
        if set(modes) != {'off', 'on'}:
            pair['reasons'] = ['missing_off_or_on_trial']
        else:
            off, off_result = modes['off']; on, on_result = modes['on']
            if not off_result['eligible'] or not on_result['eligible']:
                pair['reasons'].append('ineligible_or_untested_trial')
            else:
                if canonical_configuration(off) != canonical_configuration(on):
                    pair['reasons'].append('configuration_mismatch')
                if abs(off['startPercent'] - on['startPercent']) > 5:
                    pair['reasons'].append('starting_levels_more_than_5_percentage_points_apart')
                if abs(off_result['durationSeconds'] - on_result['durationSeconds']) > 30:
                    pair['reasons'].append('durations_more_than_30_seconds_apart')
                pair['comparable'] = not pair['reasons']
                if pair['comparable']:
                    extra = on_result['dischargePercentagePoints'] - off_result['dischargePercentagePoints']
                    pair.update(offDischargePercentagePoints=off_result['dischargePercentagePoints'],
                                onDischargePercentagePoints=on_result['dischargePercentagePoints'],
                                additionalDrainPercentagePoints=extra, withinProposedFivePointTarget=extra <= 5,
                                order=['off', 'on'] if timestamp(off['startedAt']) < timestamp(on['startedAt']) else ['on', 'off'])
        pairs.append(pair)
    comparable = [p for p in pairs if p['comparable']]
    # Matched repetitions must use the same recorded setup, including build hashes.
    configurations = {json.dumps(canonical_configuration(groups[p['pair']]['off'][0]), sort_keys=True) for p in comparable}
    return {'formatVersion': 1, 'physicalT13': 'NOT_ADJUDICATED', 'recordedAttempts': sum(r['status'] == 'recorded' for r in runs),
            'eligibleTrials': sum(r['eligible'] for r in records), 'comparablePairs': len(comparable),
            'threeMatchedPairsRecorded': len(comparable) >= 3 and len(configurations) == 1,
            'singleConfigurationAcrossComparablePairs': len(configurations) == 1,
            'meanAdditionalDrainPercentagePoints': mean(p['additionalDrainPercentagePoints'] for p in comparable) if comparable and len(configurations) == 1 else None,
            'runs': records, 'pairs': pairs,
            'scope': 'Arithmetic on reported physical endpoints/attestations and hash-checked evidence files; their contents are not authenticated or interpreted. No short-run extrapolation, USB-powered acceptance or physical T13 pass. Integer battery levels have coarse resolution; inspect all trials, interruptions, conditions and raw evidence.'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', help='Explicit operator-recorded JSON under artifacts')
    args = parser.parse_args()
    try:
        path = Path(args.report)
        with path.open('rb') as stream:
            data = stream.read(1_000_001)
        require(len(data) <= 1_000_000, 'Trial report exceeds 1 MB')
        def unique_pairs(items):
            result = {}
            for key, value in items:
                require(key not in result, 'Duplicate JSON key')
                result[key] = value
            return result
        result = summarize(json.loads(data, object_pairs_hook=unique_pairs), path.parent)
    except (TrialError, OSError, ValueError, TypeError) as error:
        parser.exit(2, 'Cannot summarize battery trials: ' + str(error) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
