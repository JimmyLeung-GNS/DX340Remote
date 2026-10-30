#!/usr/bin/env python3
"""Mac-only inventory/build/install runner. Physical BLE/audio acceptance stays explicit."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import platform
import plistlib
import re
import signal
import stat
import shutil
import subprocess
import sys
import time
import uuid

REPO = Path(__file__).resolve().parents[1]
FLUTTER_VERSION = '3.41.4'
FLUTTER_REVISION = 'ff37bef603469fb030f2b72995ab929ccfc227f0'
BUNDLE = 'org.dx340.remote.ios'


def device_inventory(raw):
    result = []
    for item in raw.get('result', {}).get('devices', []):
        hardware = item.get('hardwareProperties', {})
        properties = item.get('deviceProperties', {})
        connection = item.get('connectionProperties', {})
        result.append({'identifier': item.get('identifier'), 'udid': hardware.get('udid'),
                       'model': hardware.get('marketingName') or hardware.get('productType'),
                       'name': properties.get('name'), 'osVersion': properties.get('osVersionNumber'),
                       'osBuild': properties.get('osBuildUpdate'),
                       'developerMode': properties.get('developerModeStatus'),
                       'pairingState': connection.get('pairingState'),
                       'transport': connection.get('transportType')})
    return result


def ordinary_target(config, phone):
    values = [line.split('=', 1)[1].strip() for line in config.read_text().splitlines()
              if line.startswith('FLUTTER_TARGET=')]
    expected = (phone / 'lib/main.dart').resolve()
    if not values or any((phone / value).resolve() != expected for value in values):
        raise ValueError('Refusing a missing/synthetic Flutter target; rebuild lib/main.dart.')


def validate_device_app(app, expected_bundle):
    with (app / 'Info.plist').open('rb') as file:
        info = plistlib.load(file)
    if info.get('CFBundleIdentifier') != expected_bundle:
        raise ValueError('App bundle identifier differs from the selected shared app.')
    if info.get('DTPlatformName') != 'iphoneos' or 'iPhoneOS' not in info.get('CFBundleSupportedPlatforms', []):
        raise ValueError('A simulator app cannot be installed as a physical-iPhone test build.')
    return {key: info.get(key) for key in ('CFBundleIdentifier', 'CFBundleShortVersionString', 'CFBundleVersion', 'DTPlatformVersion')}


def app_contents_receipt(app):
    """Bind the final signed bundle's bytes and links, independently of its path.

    This records build provenance; it does not replace codesign or device tests.
    Never follow a framework symlink outside the recorded bundle.
    """
    root = app.resolve(strict=True)
    if not root.is_dir():
        raise ValueError('Recorded app is not a directory.')
    entries = []
    total_bytes = 0
    def unreadable(error):
        raise error
    for folder, directories, files in os.walk(root, followlinks=False, onerror=unreadable):
        for name in sorted(directories + files):
            path = Path(folder) / name
            relative = path.relative_to(root).as_posix()
            mode = path.lstat().st_mode
            if stat.S_ISLNK(mode):
                try:
                    destination = path.resolve(strict=True)
                except (OSError, RuntimeError) as error:
                    raise ValueError('Broken or cyclic app symlink: ' + relative) from error
                if not destination.is_relative_to(root):
                    raise ValueError('App symlink escapes its bundle: ' + relative)
                entries.append({'path': relative, 'type': 'link', 'target': os.readlink(path)})
            elif stat.S_ISDIR(mode):
                entries.append({'path': relative, 'type': 'directory'})
            elif stat.S_ISREG(mode):
                digest = hashlib.sha256()
                size = 0
                with path.open('rb') as file:
                    for chunk in iter(lambda: file.read(1024 * 1024), b''):
                        digest.update(chunk)
                        size += len(chunk)
                total_bytes += size
                entries.append({'path': relative, 'type': 'file', 'size': size,
                                'sha256': digest.hexdigest(), 'executable': bool(mode & 0o111)})
            else:
                raise ValueError('Unsupported app bundle entry: ' + relative)
    encoded = json.dumps(sorted(entries, key=lambda entry: entry['path']),
                         sort_keys=True, separators=(',', ':'), ensure_ascii=True).encode('utf-8')
    return {'formatVersion': 1, 'contentTreeSha256': hashlib.sha256(encoded).hexdigest(),
            'entries': len(entries), 'bytes': total_bytes}


def worktree_receipt(tree, expected_commit):
    """Keep generated/signing edits observable without publishing their contents."""
    def git(*args):
        return subprocess.check_output(['git', '-C', str(tree), *args], timeout=30)
    root = Path(os.fsdecode(git('rev-parse', '--show-toplevel')).strip()).resolve()
    commit = git('rev-parse', 'HEAD').decode('ascii').strip()
    if root != tree.resolve() or commit != expected_commit:
        raise ValueError('Recorded source worktree/commit changed; create a fresh device build.')
    patch = git('diff', '--no-ext-diff', '--no-textconv', '--binary', 'HEAD', '--')
    status = git('status', '--porcelain', '--untracked-files=normal')
    return {'formatVersion': 1, 'sourceCommit': commit,
            'trackedDiffSha256': hashlib.sha256(patch).hexdigest(),
            'statusSha256': hashlib.sha256(status).hexdigest(), 'hasSourceChanges': bool(status)}


def verify_install_receipt(app, tree, prior):
    if not isinstance(prior.get('signedAppContents'), dict) or not isinstance(prior.get('buildWorktree'), dict):
        raise ValueError('Build receipt has no app/source binding; rebuild with the current runner.')
    if worktree_receipt(tree, prior['sourceCommit']) != prior['buildWorktree']:
        raise ValueError('Source worktree changed after signing; create a fresh device build.')
    if app_contents_receipt(app) != prior['signedAppContents']:
        raise ValueError('Signed app contents changed after the recorded build; rebuild before installing.')


class Session:
    def __init__(self, mode, output_root=None):
        self.out = (output_root or REPO / 'artifacts/mac-iphone-tests') / (datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8])
        self.out.mkdir(parents=True)
        self.env = os.environ.copy()
        self.report = {'mode': mode, 'createdUtc': datetime.now(timezone.utc).isoformat(),
                       'host': platform.system(), 'commands': [], 'physicalBle': 'NOT TESTED',
                       'audio': 'NOT TESTED', 'pcApproval': 'Preserve; no revoke/pair operation in this runner'}
        self.tree = None

    def call(self, label, argv, timeout=60, required=True, cwd=None, include_stderr=False):
        print(label + ': logs in ' + str(self.out), flush=True)
        try:
            with (self.out / (label + '.txt')).open('w', encoding='utf-8') as stdout, (self.out / (label + '-stderr.txt')).open('w', encoding='utf-8') as stderr:
                child = subprocess.Popen([str(arg) for arg in argv], cwd=cwd or REPO, env=self.env,
                                         stdout=stdout, stderr=stderr, start_new_session=platform.system() == 'Darwin')
                self.report['currentCommand'] = {'label': label, 'pid': child.pid}
                (self.out / 'summary.json').write_text(json.dumps(self.report, indent=2))
                deadline = time.monotonic() + timeout
                while True:
                    try:
                        child.wait(timeout=max(0.001, min(30, deadline - time.monotonic())))
                        break
                    except subprocess.TimeoutExpired:
                        if time.monotonic() < deadline:
                            print(label + ' still running, PID ' + str(child.pid), flush=True)
                            continue
                        if platform.system() == 'Darwin':
                            os.killpg(child.pid, signal.SIGTERM)
                        else:
                            child.terminate()
                        try:
                            child.wait(timeout=5)
                        except subprocess.TimeoutExpired:
                            if platform.system() == 'Darwin':
                                os.killpg(child.pid, signal.SIGKILL)
                            else:
                                child.kill()
                            child.wait(timeout=5)
                        self.report['lastTimedOutCommand'] = {'label': label, 'pid': child.pid, 'processTerminated': child.poll() is not None}
                        self.report.pop('currentCommand', None)
                        self.report['commands'].append({'label': label, 'exitCode': child.returncode, 'timedOut': True})
                        raise RuntimeError(label + ' exceeded its command deadline; test child terminated')
                code = child.returncode
            output = (self.out / (label + '.txt')).read_text(encoding='utf-8', errors='replace')
            if include_stderr:
                output += (self.out / (label + '-stderr.txt')).read_text(encoding='utf-8', errors='replace')
        except OSError as error:
            code, output = -1, type(error).__name__ + ': ' + str(error)
            (self.out / (label + '-stderr.txt')).write_text(output, encoding='utf-8')
        self.report['commands'].append({'label': label, 'argv': [str(arg) for arg in argv], 'exitCode': code})
        self.report.pop('currentCommand', None)
        if code and required:
            raise RuntimeError(label + ' failed; inspect ' + str(self.out / (label + '.txt')))
        return code, output

    def inventory(self):
        self.report['python'] = platform.python_version()
        self.report['sourceCommit'] = self.call('source-commit', ['git', 'rev-parse', 'HEAD'])[1].strip()
        self.report['sourceStatus'] = self.call('source-status', ['git', 'status', '--porcelain'])[1]
        if platform.system() != 'Darwin':
            self.report['status'] = 'NEEDS_MACOS_XCODE'
            return False
        self.call('macos-version', ['sw_vers'])
        self.call('developer-directory', ['xcode-select', '-p'])
        self.call('xcode-version', ['xcodebuild', '-version'])
        self.call('sdk-inventory', ['xcodebuild', '-showsdks'])
        self.call('swift-version', ['swift', '--version'])
        _, sims = self.call('simulators', ['xcrun', 'simctl', 'list', 'devices', 'available', '--json'])
        self.report['simulators'] = [{'runtime': runtime, 'name': item['name'], 'udid': item['udid'], 'state': item['state']}
                                     for runtime, items in json.loads(sims)['devices'].items() if '.iOS-' in runtime
                                     for item in items if item.get('isAvailable') and item['name'].startswith('iPhone')]
        devices_path = self.out / 'devices.json'
        code, _ = self.call('devices', ['xcrun', 'devicectl', 'list', 'devices', '--json-output', devices_path], required=False)
        self.report['devices'] = device_inventory(json.loads(devices_path.read_text())) if code == 0 and devices_path.exists() else []
        sdk = REPO / 'work/flutter/bin/flutter'
        flutter = str(sdk) if sdk.is_file() else shutil.which('flutter')
        if not flutter:
            self.report['status'] = 'NEEDS_PINNED_FLUTTER'
            return False
        self.env['PATH'] = str(Path(flutter).parent) + os.pathsep + self.env.get('PATH', '')
        # A fresh pinned checkout can put Dart download progress on stdout even
        # for --machine. Finish that wrapper bootstrap before strict JSON input.
        self.call('flutter-bootstrap', [flutter, '--version'], timeout=600)
        _, version = self.call('flutter-version', [flutter, '--version', '--machine'], timeout=180)
        self.report['flutter'] = json.loads(version)
        if self.report['flutter'].get('frameworkVersion') != FLUTTER_VERSION or self.report['flutter'].get('frameworkRevision') != FLUTTER_REVISION:
            self.report['status'] = 'FLUTTER_PIN_MISMATCH'
            return False
        self.report['flutterExecutable'] = flutter
        self.report['cocoaPodsRequiredByCurrentProject'] = (REPO / 'phone/ios/Podfile').exists()
        if self.report['cocoaPodsRequiredByCurrentProject']:
            self.call('cocoapods-version', ['pod', '--version'])
        self.report['status'] = 'TOOLCHAIN_INVENTORIED'
        return True

    def snapshot(self):
        if self.report.get('sourceStatus'):
            raise ValueError('Commit or stash source edits before testing a reproducible snapshot; no files were reset.')
        work = REPO / 'work' / ('mac-test-' + self.out.name)
        work.parent.mkdir(exist_ok=True)
        self.call('source-worktree', ['git', '-c', 'core.autocrlf=false', 'worktree', 'add', '--detach', work, self.report['sourceCommit']])
        evidence = self.out / 'evidence'
        evidence.mkdir()
        (work / 'artifacts').symlink_to(evidence, target_is_directory=True)
        self.tree = work
        self.report['sourceWorktree'] = str(work)
        self.report['evidenceDirectory'] = str(evidence)

    def save(self):
        (self.out / 'summary.json').write_text(json.dumps(self.report, indent=2), encoding='utf-8')
        print('Evidence: ' + str(self.out), flush=True)


def run(args):
    session = Session(args.mode)
    try:
        if not session.inventory():
            return 2
        if args.mode == 'preflight':
            return 0
        if args.mode == 'simulator':
            if args.simulator.upper() not in {item['udid'].upper() for item in session.report['simulators']}:
                raise ValueError('Select an actual available iPhone simulator UDID from preflight; none is chosen automatically.')
            session.snapshot()
            # Both targets share a bundle identifier. Run the reference first and restore shared ordinary app last.
            session.call('native-simulator', ['sh', 'scripts/build-apple.sh', args.simulator], timeout=1800, cwd=session.tree)
            session.call('shared-simulator', ['sh', 'scripts/build-phone-ios.sh', args.simulator], timeout=2400, cwd=session.tree)
            session.report['status'] = 'SIMULATOR_BUILD_LAUNCH_MATCHERS_PASSED'
            session.report['visualReview'] = 'PENDING; inspect the captured PNGs'
        elif args.mode == 'device-build':
            matches = [item for item in session.report['devices'] if args.device.lower() in {str(item.get('identifier')).lower(), str(item.get('udid')).lower()}]
            if len(matches) != 1:
                raise ValueError('Selected physical device is not uniquely present in devicectl inventory.')
            session.report.update({'selectedDevice': matches[0], 'requestedDevice': args.device, 'team': args.team, 'bundleId': args.bundle_id})
            destination_device = matches[0].get('udid') or args.device
            session.snapshot()
            phone = session.tree / 'phone'
            flutter = session.report['flutterExecutable']
            session.call('pub-get', [flutter, 'pub', 'get', '--enforce-lockfile'], timeout=600, cwd=phone)
            session.call('analysis', [flutter, 'analyze'], timeout=600, cwd=phone)
            session.call('flutter-tests', [flutter, 'test', '--reporter', 'expanded'], timeout=1200, cwd=phone)
            session.call('ordinary-unsigned-device-build', [flutter, 'build', 'ios', '--release', '--no-codesign', '--target', 'lib/main.dart'], timeout=1800, cwd=phone)
            ordinary_target(phone / 'ios/Flutter/Generated.xcconfig', phone)
            derived = session.out / 'signed'
            session.call('signed-device-build', ['xcodebuild', '-workspace', phone / 'ios/Runner.xcworkspace', '-scheme', 'Runner', '-configuration', 'Release', '-sdk', 'iphoneos', '-destination', 'id=' + destination_device, '-derivedDataPath', derived, 'CODE_SIGN_STYLE=Automatic', 'DEVELOPMENT_TEAM=' + args.team, 'PRODUCT_BUNDLE_IDENTIFIER=' + args.bundle_id, 'FLUTTER_TARGET=lib/main.dart', 'build'], timeout=1800)
            app = derived / 'Build/Products/Release-iphoneos/Runner.app'
            ordinary_target(phone / 'ios/Flutter/Generated.xcconfig', phone)
            session.report['app'] = validate_device_app(app, args.bundle_id)
            session.call('signature-verification', ['codesign', '--verify', '--deep', '--strict', app])
            _, signature = session.call('signature-details', ['codesign', '-dv', '--verbose=4', app], include_stderr=True)
            if ('TeamIdentifier=' + args.team) not in signature.splitlines():
                raise ValueError('Signed app team does not match the explicitly selected team.')
            session.report['buildWorktree'] = worktree_receipt(session.tree, session.report['sourceCommit'])
            session.report['signedAppContents'] = app_contents_receipt(app)
            session.report.update({'appPath': str(app), 'status': 'SIGNED_ORDINARY_DEVICE_BUILD_PASSED', 'installation': 'NOT REQUESTED'})
        elif args.mode == 'device-install':
            prior_path = Path(args.run).resolve()
            prior = json.loads((prior_path / 'summary.json').read_text())
            if prior.get('status') != 'SIGNED_ORDINARY_DEVICE_BUILD_PASSED' or prior.get('requestedDevice') != args.device:
                raise ValueError('Use a successful signed ordinary build for the same explicitly selected device.')
            app = Path(prior['appPath']).resolve()
            if not app.is_relative_to(prior_path) or prior.get('sourceCommit') != session.report['sourceCommit']:
                raise ValueError('App path/source does not match this recorded build.')
            tree = Path(prior['sourceWorktree']).resolve()
            ordinary_target(tree / 'phone/ios/Flutter/Generated.xcconfig', tree / 'phone')
            session.report['app'] = validate_device_app(app, prior['bundleId'])
            verify_install_receipt(app, tree, prior)
            session.call('signature-verification', ['codesign', '--verify', '--deep', '--strict', app])
            _, signature = session.call('signature-details', ['codesign', '-dv', '--verbose=4', app], include_stderr=True)
            if ('TeamIdentifier=' + prior['team']) not in signature.splitlines():
                raise ValueError('Install app team differs from its recorded signed build.')
            # Recheck after read-only signing commands, immediately before mutation.
            verify_install_receipt(app, tree, prior)
            session.call('install-ordinary-device-app', ['xcrun', 'devicectl', 'device', 'install', 'app', '--device', args.device, app], timeout=180)
            session.call('launch-ordinary-device-app', ['xcrun', 'devicectl', 'device', 'process', 'launch', '--device', args.device, prior['bundleId']], timeout=90)
            session.report.update({'status': 'INSTALL_LAUNCH_COMMANDS_SUCCEEDED', 'installedBuild': str(prior_path), 'onScreenReview': 'PENDING; inspect app/permission prompts on the iPhone'})
        return 0
    except Exception as error:
        session.report.update({'status': 'FAILED', 'error': str(error)})
        print(str(error), file=sys.stderr)
        return 1
    finally:
        session.save()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    modes = parser.add_subparsers(dest='mode', required=True)
    modes.add_parser('preflight')
    sim = modes.add_parser('simulator')
    sim.add_argument('--simulator', required=True)
    device = modes.add_parser('device-build')
    device.add_argument('--device', required=True)
    device.add_argument('--team', required=True)
    device.add_argument('--bundle-id', default=BUNDLE)
    install = modes.add_parser('device-install')
    install.add_argument('--device', required=True)
    install.add_argument('--run', required=True)
    arguments = parser.parse_args()
    if arguments.mode == 'device-build' and (not re.fullmatch(r'[A-Z0-9]{10}', arguments.team) or not re.fullmatch(r'[A-Za-z][A-Za-z0-9]*(?:\.[A-Za-z0-9-]+)+', arguments.bundle_id)):
        parser.error('Use an explicit 10-character team ID and a valid owned bundle identifier.')
    raise SystemExit(run(arguments))
