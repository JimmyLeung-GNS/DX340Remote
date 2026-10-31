#!/usr/bin/env python3
"""Phone-only USB observer for a separately recorded, unplugged DAP hold."""
import argparse
import datetime
import json
from pathlib import Path
import re
import subprocess
import time

PACKAGE = 'org.dx340.remote.controller'


def condition(service, activities, power, initial_pid, current_pid):
    wake = re.search(r'mWakefulness=(\w+)', power)
    resumed = re.search(r'^\s*(?:topResumedActivity|mResumedActivity)=(.+)$', activities, re.M)
    top = re.search(r'u\d+\s+([A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+)', resumed[1]) if resumed else None
    top = top[1] if top else ''
    wake = wake[1] if wake else 'Unknown'
    background = not top.startswith(PACKAGE + '/') if top else wake in ('Asleep', 'Dozing')
    return {'phoneWakefulness': wake, 'phoneTopActivity': top, 'phoneAppBackground': background,
            'phoneForegroundService': 'isForeground=true' in service,
            'phoneProcessUnchanged': bool(initial_pid) and current_pid == initial_pid}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--serial', required=True); p.add_argument('--out', type=Path, required=True)
    p.add_argument('--duration', type=int, default=1860); p.add_argument('--interval', type=int, default=30)
    a = p.parse_args()
    if not 1 <= a.duration <= 3600 or not 15 <= a.interval <= 60:
        p.error('Require1..3600seconds and15..60second intervals')
    a.out.mkdir(parents=True, exist_ok=False)
    def adb(*args): return subprocess.check_output(['adb', '-s', a.serial, *args], timeout=25).decode('utf-8')
    began = time.monotonic(); samples = []; failure = None; stopped = False
    try:
        initial = adb('shell', 'pidof', PACKAGE).strip()
        while True:
            if (a.out / 'stop').exists():
                stopped = True; failure = 'Operator stopped; no completed hold'; break
            raw = {
                'service': adb('shell', 'dumpsys', 'activity', 'services', PACKAGE),
                'activities': adb('shell', 'dumpsys', 'activity', 'activities'),
                'power': adb('shell', 'dumpsys', 'power'),
            }
            current = adb('shell', 'pidof', PACKAGE).strip()
            for name, text in raw.items():
                (a.out / f'{len(samples):03d}-{name}.txt').write_text(text, encoding='utf-8')
            row = dict(condition(raw['service'], raw['activities'], raw['power'], initial, current),
                       at=datetime.datetime.now().astimezone().isoformat(), elapsedSeconds=round(time.monotonic() - began, 3))
            samples.append(row)
            with (a.out / 'samples.jsonl').open('a', encoding='utf-8') as out: out.write(json.dumps(row) + '\n')
            print(json.dumps(row), flush=True)
            if not all(row[x] for x in ['phoneAppBackground', 'phoneForegroundService', 'phoneProcessUnchanged']):
                raise RuntimeError('Phone background/process/service condition failed; preserve this attempt')
            remaining = a.duration - (time.monotonic() - began)
            if remaining <= 0: break
            time.sleep(min(a.interval, remaining))
    except BaseException as exc:
        failure = type(exc).__name__ + ': ' + str(exc)
        raise
    finally:
        v = {'kind': 'Phone-only USB-powered background observer; no DAP reads/commands',
             'requestedSeconds': a.duration, 'elapsedSeconds': round(time.monotonic() - began, 3),
             'samples': len(samples), 'failure': failure, 'operatorStopped': stopped,
             'observationComplete': failure is None and not stopped and time.monotonic() - began >= a.duration,
             'limits': 'Not continuous BLE, DAP display/source state, audio, unplugged phone, battery or full T05 evidence. Collect DAP recorder and test foreground control/hearing separately.'}
        (a.out / 'summary.json').write_text(json.dumps(v, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__': main()
