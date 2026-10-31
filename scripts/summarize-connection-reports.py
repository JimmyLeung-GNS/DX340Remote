#!/usr/bin/env python3
"""Summarize bounded phone reports. Never adjudicates physical T04 acceptance."""
import argparse
from collections import Counter
import json
import math
from pathlib import Path
import re

ROOT_KEYS = set('formatVersion clock capacity evictedRequests evictedConnections requests connections modelInstanceId app os platform authorized connectionPhase timingScope privacy'.split())
ROW_KEYS = set('id operation source startedMs accepted acceptedAfterMs finishedAfterMs result terminalApplied hostOutcome error'.split())
WRITE_ACK = 'writeAcknowledgedAfterMs'
ENDINGS = {'result', 'error', 'other_reply', 'timeout', 'send_failed', 'connection_lost', 'session_reset', 'other'}
SAFE = re.compile(r'[A-Za-z0-9_-]{1,64}\Z')
OPERATIONS = set('HELLO AUTHORIZE REQUEST_CONTROL GET_STATE GET_QUEUE GET_ARTWORK GET_CAPABILITIES LIST_SOURCES SUBSCRIBE_STATE SET_PLAYING NEXT PREVIOUS SEEK SELECT_SOURCE PLAY_ITEM PLAY_QUEUE_ITEM ADJUST_VOLUME BROWSE SEARCH SET_SHUFFLE SET_REPEAT OTHER'.split())
SOURCES = {'mango', 'apple', 'none', '', 'other'}


class ReportError(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise ReportError(message)


def milliseconds(value):
    return value is None or (type(value) is int and 0 <= value <= 2**63 - 1)


def validate(report):
    require(type(report) is dict and set(report) == ROOT_KEYS, 'Unexpected report fields')
    require(type(report['formatVersion']) is int and report['formatVersion'] == 1, 'Unsupported format version')
    require(report['clock'] == 'monotonic_ms_relative_to_model', 'Unsupported clock')
    require(type(report['capacity']) is int and 1 <= report['capacity'] <= 512, 'Invalid history capacity')
    require(report['platform'] in ('android', 'ios'), 'Unsupported platform')
    require(isinstance(report['modelInstanceId'], str) and SAFE.fullmatch(report['modelInstanceId']), 'Invalid model instance ID')
    require(type(report['authorized']) is bool, 'Invalid authorization observation')
    for key in ('evictedRequests', 'evictedConnections'):
        require(type(report[key]) is int and report[key] >= 0, 'Invalid eviction counter')
    app = report['app']
    keys = {'id', 'versionName', 'versionCode'} | ({'baseApkSha256'} if report['platform'] == 'android' else set())
    require(type(app) is dict and set(app) == keys, 'Unexpected app fields')
    require(all(isinstance(app[k], str) for k in ('id', 'versionName')), 'Invalid app identity')
    require(type(app['versionCode']) in (str, int), 'Invalid app build')
    if report['platform'] == 'android':
        require(isinstance(app['baseApkSha256'], str) and re.fullmatch('[a-f0-9]{64}', app['baseApkSha256']), 'Invalid APK digest')
    os_keys = {'version', 'model'} | ({'api'} if report['platform'] == 'android' else set())
    require(type(report['os']) is dict and set(report['os']) == os_keys, 'Unexpected OS fields')
    require(type(report['requests']) is list and len(report['requests']) <= report['capacity'], 'Request bound exceeded')
    require(type(report['connections']) is list and len(report['connections']) <= report['capacity'], 'Connection bound exceeded')
    seen = set()
    for row in report['requests']:
        require(type(row) is dict and ROW_KEYS <= set(row) <= ROW_KEYS | {WRITE_ACK}, 'Unexpected request fields')
        require(isinstance(row['id'], str) and SAFE.fullmatch(row['id']) and row['id'] not in seen, 'Invalid or duplicate request ID')
        seen.add(row['id'])
        require(all(isinstance(row[k], str) for k in ('operation', 'source', 'result', 'hostOutcome')), 'Invalid request categories')
        require(row['operation'] in OPERATIONS and row['source'] in SOURCES, 'Unknown operation/source category')
        require(all(milliseconds(row[k]) for k in ('startedMs', 'acceptedAfterMs', 'finishedAfterMs')), 'Invalid duration')
        require(milliseconds(row.get(WRITE_ACK)), 'Invalid write-acknowledgement duration')
        require(type(row['accepted']) is bool and type(row['terminalApplied']) is bool, 'Invalid receipt flags')
        require(row['accepted'] or row['acceptedAfterMs'] is None, 'Timing without ACCEPTED receipt')
        require(row['result'] in ENDINGS | {'pending'}, 'Unknown terminal category')
        require(row['hostOutcome'] in ('none', 'dispatched', 'observed'), 'Unknown host outcome')
        require(row['error'] is None or isinstance(row['error'], str), 'Invalid error category')
        if row['result'] == 'pending':
            require(row['finishedAfterMs'] is None, 'Pending request has a terminal time')
        if row['result'] not in ('result', 'error', 'other_reply'):
            require(not row['terminalApplied'] and row['hostOutcome'] == 'none', 'Non-reply cannot be applied or observed')
        require((row['error'] is not None) == (row['result'] == 'error'), 'Error category disagrees with terminal receipt')
        a, b = row['acceptedAfterMs'], row['finishedAfterMs']
        require(a is None or b is None or a <= b, 'ACCEPTED is later than terminal receipt')
        ack = row.get(WRITE_ACK)
        if row['result'] not in ('pending', 'result', 'error', 'other_reply'):
            require(ack is None or b is None or ack <= b, 'Write acknowledgement appeared after local termination')
    for row in report['connections']:
        require(type(row) is dict and set(row) == {'atMs', 'phase'} and milliseconds(row['atMs']) and isinstance(row['phase'], str), 'Invalid connection transition')


def merge(first, second):
    for key in ('id', 'operation', 'source', 'startedMs'):
        require(first[key] == second[key], 'Conflicting immutable request identity')
    if first['accepted'] and second['accepted']:
        require(first['acceptedAfterMs'] == second['acceptedAfterMs'], 'Conflicting first ACCEPTED timing')
    a, b = first.get(WRITE_ACK), second.get(WRITE_ACK)
    require(a is None or b is None or a == b, 'Conflicting first write acknowledgement timing')
    terminals = [r for r in (first, second) if r['result'] != 'pending']
    if len(terminals) == 2:
        for key in ('result', 'finishedAfterMs', 'hostOutcome', 'error', 'accepted', 'acceptedAfterMs'):
            require(first[key] == second[key], 'Conflicting terminal receipt')
    final = dict(terminals[0] if terminals else first)
    accepted = next((r for r in (first, second) if r['accepted']), None)
    if terminals:
        require(accepted is None or final['accepted'], 'ACCEPTED appeared after a finalized receipt')
    elif accepted:
        final.update(accepted=True, acceptedAfterMs=accepted['acceptedAfterMs'])
    final['terminalApplied'] = any(r['terminalApplied'] for r in (first, second))
    if WRITE_ACK in first or WRITE_ACK in second:
        final[WRITE_ACK] = a if a is not None else b
    validate_row_ack = final.get(WRITE_ACK)
    if final['result'] not in ('pending', 'result', 'error', 'other_reply'):
        require(validate_row_ack is None or final['finishedAfterMs'] is None or validate_row_ack <= final['finishedAfterMs'],
                'Merged write acknowledgement appeared after local termination')
    return final


def distribution(values):
    values = sorted(values)
    if not values:
        return {'samples': 0, 'p50Ms': None, 'p95Ms': None, 'maxMs': None}
    # Nearest-rank percentiles, explicitly reported; no interpolation or zero fill.
    return {'samples': len(values), 'p50Ms': values[math.ceil(.50 * len(values)) - 1],
            'p95Ms': values[math.ceil(.95 * len(values)) - 1], 'maxMs': values[-1]}


def summarize(reports, allow_multiple_sessions=False):
    require(bool(reports), 'No reports supplied')
    rows, environments, sessions, evictions = {}, set(), set(), {}
    for report in reports:
        validate(report)
        environment = json.dumps({k: report[k] for k in ('platform', 'app', 'os')}, sort_keys=True)
        environments.add(environment)
        session = report['modelInstanceId']
        sessions.add(session)
        current = (report['evictedRequests'], report['evictedConnections'])
        previous = evictions.get(session, (0, 0))
        evictions[session] = tuple(max(a, b) for a, b in zip(previous, current))
        for row in report['requests']:
            key = session, row['id']
            rows[key] = merge(rows[key], row) if key in rows else dict(row)
    require(len(environments) == 1, 'Mixed builds/platforms/OS environments require separate summaries')
    require(allow_multiple_sessions or len(sessions) == 1, 'Multiple model sessions require --allow-multiple-sessions')
    environment = json.loads(next(iter(environments)))
    require(environment['platform'] != 'ios' or len(sessions) == 1,
            'iOS reports have no binary digest: summarize each model session separately and retain external build provenance')
    groups = {}
    for row in rows.values():
        groups.setdefault((row['operation'], row['source']), []).append(row)
    output = []
    for (operation, source), items in sorted(groups.items()):
        timed = [r for r in items if r['startedMs'] is not None]
        applied = [r for r in timed if r['result'] == 'result' and r['terminalApplied']]
        def times(values, key):
            return distribution([r[key] for r in values if r.get(key) is not None])
        output.append({'operation': operation, 'source': source, 'requests': len(items),
                       'outcomes': dict(sorted(Counter(r['result'] for r in items).items())),
                       'attWriteAcknowledged': times(timed, WRITE_ACK),
                       'acceptedReceipt': times([r for r in timed if r['accepted']], 'acceptedAfterMs'),
                       'terminalReply': times([r for r in timed if r['result'] in ('result', 'error', 'other_reply')], 'finishedAfterMs'),
                       'appliedResult': times(applied, 'finishedAfterMs'),
                       'hostObservedAppliedResult': times([r for r in applied if r['hostOutcome'] == 'observed'], 'finishedAfterMs')})
    return {'formatVersion': 1, 'inputReports': len(reports), 'modelSessions': len(sessions),
            'environment': environment, 'uniqueRequests': len(rows),
            'evictedRequestsAtLeast': sum(v[0] for v in evictions.values()),
            'evictedConnectionsAtLeast': sum(v[1] for v in evictions.values()),
            'percentiles': 'nearest_rank', 'groups': output,
            'physicalT04': 'NOT_ADJUDICATED',
            'scope': 'Separate native registration-to-last-ATT-write-response, protocol ACCEPTED and decoded-reply times. Includes encoding/queue/callback delay, plus reply reassembly. Legacy missing write receipts stay unknown. No UI-tap/acoustic or audible proof. Empty/missing samples never pass acceptance.'}


def read_report(path):
    with Path(path).open('rb') as stream:
        data = stream.read(262145)
    require(len(data) <= 262144, 'Input exceeds bounded phone report size')
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            require(key not in result, 'Duplicate JSON key')
            result[key] = value
        return result
    return json.loads(data, object_pairs_hook=unique_pairs)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reports', nargs='+', help='Explicit paths to phone JSON exports')
    parser.add_argument('--allow-multiple-sessions', action='store_true', help='Combine Android model instances with the same APK digest/OS environment; iOS stays one session per summary')
    args = parser.parse_args()
    try:
        result = summarize([read_report(p) for p in args.reports], args.allow_multiple_sessions)
    except (ReportError, OSError, json.JSONDecodeError, UnicodeDecodeError, TypeError) as error:
        parser.exit(2, 'Cannot summarize reports: ' + str(error) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
