#!/usr/bin/env python3
"""Bounded shell diagnostics survive ADB disconnect; never adjudicate T05/audio."""
import argparse
import datetime
import hashlib
import json
from pathlib import Path
import re
import subprocess
import time
import uuid

ROOT = re.compile(r'/data/local/tmp/dx340-hold-[a-f0-9]{32}\Z')
FILES = re.compile(r'(?:record\.sh|state|pid|logger-pid|logger\.txt|launcher\.txt|summary\.tsv|started-uptime|error\.txt|arming\.tsv|arming-(?:power|display)\.txt|\d{3}-(?:clock|power|display|media|audio|service|pid)\.txt|power-events\.log(?:\.[12])?)\Z')
TERMINAL = {'COMPLETED', 'FAILED', 'STOPPED'}


def adb(serial, *args):
    return subprocess.check_output(['adb', '-s', serial, *args], timeout=30)


def validate_manifest(value):
    if not isinstance(value, dict) or not ROOT.fullmatch(value.get('remoteRoot', '')):
        raise ValueError('Not an owned recorder directory')
    if not re.fullmatch(r'[A-Za-z0-9_.:-]{1,128}', value.get('serial', '')):
        raise ValueError('Invalid target serial')
    if type(value.get('pid')) is not int or value['pid'] <= 1:
        raise ValueError('Invalid recorder PID')
    return value


def state(manifest):
    validate_manifest(manifest)
    root, serial = manifest['remoteRoot'], manifest['serial']
    return adb(serial, 'shell', 'cat', root + '/state').decode().strip()


def inspect(manifest):
    current = state(manifest)
    if current in TERMINAL:
        return {'state': current, 'ownedProcessLive': None}
    try:
        command = adb(manifest['serial'], 'exec-out', 'cat', f"/proc/{manifest['pid']}/cmdline").replace(b'\0', b' ').decode()
        matches = command.startswith('sh ') and manifest['remoteRoot'] + '/record.sh ' in command
    except subprocess.CalledProcessError:
        matches = False
    if not matches:
        latest = state(manifest)
        if latest in TERMINAL:
            return {'state': latest, 'ownedProcessLive': None}
        return {'state': 'INCOMPLETE_PROCESS_UNCONFIRMED', 'lastWrittenState': latest, 'ownedProcessLive': False}
    return {'state': current, 'ownedProcessLive': True}


def start(serial, out, duration, interval, unplugged):
    if not 10 <= duration <= 3600 or not 1 <= interval <= 60 or duration // interval + 3 > 256:
        raise ValueError('Require10..3600seconds,1..60second intervals and at most256samples')
    out.mkdir(parents=True, exist_ok=False)
    root = '/data/local/tmp/dx340-hold-' + uuid.uuid4().hex
    script = Path(__file__).with_suffix('.sh')
    # Normalize repository checkout CRLF before Android sh parses the script.
    payload = script.read_text(encoding='utf-8').replace('\r\n', '\n').encode()
    local = out / 'record.sh'; local.write_bytes(payload)
    adb(serial, 'shell', 'mkdir', '-m', '700', root)
    adb(serial, 'push', str(local), root + '/record.sh')
    adb(serial, 'shell', 'sh', '-n', root + '/record.sh')
    command = ('nohup sh ' + root + '/record.sh ' + root + f' {duration} {interval} {int(unplugged)}'
               + ' >' + root + '/launcher.txt 2>&1 </dev/null & echo $!')
    launched = adb(serial, 'shell', command).decode().strip()
    if not re.fullmatch(r'\d+', launched):
        raise RuntimeError('Launch outcome unknown; inspect directory, never start a replacement blindly: ' + root)
    value = {'serial': serial, 'remoteRoot': root, 'pid': int(launched), 'durationSeconds': duration,
             'sampleSeconds': interval, 'requireUnpluggedDisplayOff': unplugged,
             'scriptSha256': hashlib.sha256(payload).hexdigest(),
             'launchedAt': datetime.datetime.now().astimezone().isoformat(),
             'scope': 'Read-only shell recorder; no wake lock, source/BLE/screen/configuration action. Sampling may pause during suspend. Not battery/radio/audio acceptance.'}
    (out / 'manifest.json').write_text(json.dumps(value, indent=2) + '\n', encoding='utf-8')
    # A launch PID is not evidence of successful arming. Inspect this exact job.
    for _ in range(15):
        try:
            pid = int(adb(serial, 'shell', 'cat', root + '/pid').decode().strip())
            if pid != value['pid']:
                raise RuntimeError('Unexpected recorder PID; inspect existing job')
            current = state(value)
            if current in {'ARMING', 'RUNNING'} | TERMINAL:
                return dict(value, **inspect(value))
        except subprocess.CalledProcessError:
            pass
        time.sleep(.2)
    raise RuntimeError('Existing launch not confirmed; retain manifest and inspect, do not restart')


def collect(manifest, out):
    current = state(manifest)
    if current not in TERMINAL:
        raise ValueError('Recorder is not terminal: ' + current)
    root, serial = manifest['remoteRoot'], manifest['serial']
    names = adb(serial, 'shell', 'ls', '-1', root).decode().splitlines()
    if not names or len(names) > 1820 or not all(FILES.fullmatch(x) for x in names):
        raise ValueError('Unexpected recorder file inventory; nothing copied')
    out.mkdir(parents=True, exist_ok=True)
    hashes = []
    total = 0
    for name in names:
        size = int(adb(serial, 'shell', 'stat', '-c', '%s', root + '/' + name).decode().strip())
        total += size
        if size < 0 or size > 8 * 1024 * 1024 or total > 64 * 1024 * 1024:
            raise ValueError('Unexpected diagnostic size; file not read')
        data = adb(serial, 'exec-out', 'cat', root + '/' + name)
        if len(data) != size:
            raise ValueError('Terminal diagnostic size changed')
        (out / name).write_bytes(data)
        hashes.append({'file': name, 'sha256': hashlib.sha256(data).hexdigest()})
    summary = dict(row.split('\t', 1) for row in (out / 'summary.tsv').read_text().splitlines())
    if summary['status'] != current:
        raise ValueError('Terminal summary disagrees with state')
    result = {'state': current, 'summary': summary, 'manifest': manifest, 'files': hashes,
              'acceptance': 'NOT ADJUDICATED: inspect display/source/output/services, sample gaps and filtered power events; verify phone BLE and hearing separately.'}
    (out / 'collection.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    return result


def stop(manifest):
    if state(manifest) in TERMINAL:
        return 'Already terminal; no signal sent'
    root, pid, serial = manifest['remoteRoot'], manifest['pid'], manifest['serial']
    command = adb(serial, 'exec-out', 'cat', f'/proc/{pid}/cmdline').replace(b'\0', b' ').decode()
    if root + '/record.sh ' not in command or not command.startswith('sh '):
        raise ValueError('PID no longer identifies the owned recorder; no signal sent')
    adb(serial, 'shell', 'kill', '-TERM', str(pid))
    return 'Stop signal sent to verified owned recorder; inspect same job until terminal'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    p = sub.add_parser('start'); p.add_argument('--serial', required=True); p.add_argument('--out', type=Path, required=True)
    p.add_argument('--duration', type=int, default=1800); p.add_argument('--interval', type=int, default=30)
    p.add_argument('--require-unplugged-off', action='store_true')
    for name in ['status', 'collect', 'stop']:
        p = sub.add_parser(name); p.add_argument('manifest', type=Path)
        if name == 'collect': p.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    if args.command == 'start':
        value = start(args.serial, args.out, args.duration, args.interval, args.require_unplugged_off)
    else:
        manifest = validate_manifest(json.loads(args.manifest.read_text(encoding='utf-8')))
        value = inspect(manifest) if args.command == 'status' else stop(manifest) if args.command == 'stop' else collect(manifest, args.out)
    print(json.dumps(value, indent=2))


if __name__ == '__main__':
    main()
