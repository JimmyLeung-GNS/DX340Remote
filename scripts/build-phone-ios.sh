#!/bin/sh
set -eu
if [ "$(uname -s)" != Darwin ]; then
    printf '%s\n' 'Flutter iOS verification requires macOS and Xcode.' >&2; exit 2
fi
repo=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
simulator=${1:?Pass an available iPhone simulator UDID}
cd "$repo"
mkdir -p artifacts work
flutter --version > artifacts/flutter-bootstrap.txt 2>&1
flutter --version --machine > artifacts/flutter-ios-toolchain.json
python3 - <<'PY'
import json
assert json.load(open('artifacts/flutter-ios-toolchain.json'))['frameworkVersion'] == '3.41.4'
PY
xcodebuild -version > artifacts/flutter-xcode-version.txt
sw_vers > artifacts/flutter-macos-version.txt
cd phone
flutter pub get --enforce-lockfile
flutter analyze > ../artifacts/flutter-ios-analyze.txt 2>&1
flutter test --reporter expanded > ../artifacts/flutter-ios-tests.txt 2>&1
flutter build ios --simulator --debug --no-codesign --target lib/main.dart > ../artifacts/flutter-ios-build.txt 2>&1
cd "$repo"
swiftc scripts/check-apple-capture.swift -o work/check-flutter-capture
work/check-flutter-capture --self-test > artifacts/flutter-capture-matcher-tests.txt
xcrun simctl boot "$simulator" 2>/dev/null || true
xcrun simctl bootstatus "$simulator" -b > artifacts/flutter-simulator-boot.txt 2>&1
xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app
capture() {
    heading=$1
    path=$2
    attempt=0
    while [ "$attempt" -lt 15 ]; do
        xcrun simctl io "$simulator" screenshot "$path"
        if work/check-flutter-capture "$path" "$heading" >> artifacts/flutter-capture-readiness.txt 2>&1; then
            # Launch animation can still fade a readable first frame. Keep the
            # final image only after a second ready frame one second later.
            sleep 1
            xcrun simctl io "$simulator" screenshot "$path"
            if work/check-flutter-capture "$path" "$heading" >> artifacts/flutter-capture-readiness.txt 2>&1; then return; fi
        fi
        attempt=$((attempt + 1)); sleep 1
    done
    return 1
}
xcrun simctl ui "$simulator" appearance light

xcrun simctl ui "$simulator" content_size large
for tab in player library queue setup; do
    xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
    xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=$tab" >> artifacts/flutter-simulator-launch.txt 2>&1
    heading=$tab; if [ "$tab" = player ]; then heading=DX340; fi
    capture "$heading" "artifacts/flutter-$tab-light.png"
done
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" appearance dark
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=player >> artifacts/flutter-simulator-launch.txt 2>&1
capture DX340 artifacts/flutter-player-dark.png
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" content_size accessibility-extra-extra-extra-large
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=player >> artifacts/flutter-simulator-launch.txt 2>&1
capture DX340 artifacts/flutter-player-large-dark.png
xcrun simctl ui "$simulator" content_size large
xcrun simctl ui "$simulator" appearance light

# The fixture is a separate debug target. Always restore the ordinary target/app,
# including when capture verification fails, so Xcode never inherits a fake remote.
restore_ordinary_phone() {
    fixture_status=$?
    trap - EXIT
    cd "$repo/phone"
    if ! flutter build ios --simulator --debug --no-codesign --target lib/main.dart > ../artifacts/flutter-ios-restore-build.txt 2>&1; then exit 1; fi
    cd "$repo"
    xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
    if ! xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app; then exit 1; fi
    xcrun simctl ui "$simulator" content_size large
    xcrun simctl ui "$simulator" appearance light
    if ! xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=player >> artifacts/flutter-restored-launch.txt 2>&1; then exit 1; fi
    if [ "$fixture_status" -eq 0 ]; then
        if ! capture DX340 artifacts/flutter-restored-player-light.png; then exit 1; fi
    fi
    exit "$fixture_status"
}
trap restore_ordinary_phone EXIT
cd "$repo/phone"
flutter build ios --simulator --debug --no-codesign --target test_support/volume_capture.dart > ../artifacts/flutter-ios-volume-fixture-build.txt 2>&1
cd "$repo"
xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app
for mode in volume-high volume-low; do
    xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
    xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=$mode" >> artifacts/flutter-volume-fixture-launch.txt 2>&1
    capture 'Volume up' "artifacts/flutter-synthetic-$mode-light.png"
done
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" appearance dark
xcrun simctl ui "$simulator" content_size accessibility-extra-extra-extra-large
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=volume-high >> artifacts/flutter-volume-fixture-launch.txt 2>&1
capture 'Volume up' artifacts/flutter-synthetic-volume-large-dark.png

# Persistent browsing controls use a distinct synthetic target, also covered by
# the ordinary-app EXIT restoration above. No fixture ever sends BLE commands.
cd "$repo/phone"
flutter build ios --simulator --debug --no-codesign --target test_support/browsing_capture.dart > ../artifacts/flutter-ios-browsing-fixture-build.txt 2>&1
cd "$repo"
xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app
xcrun simctl ui "$simulator" content_size large
xcrun simctl ui "$simulator" appearance light
for destination in library queue; do
    xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
    xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=browsing-$destination" >> artifacts/flutter-browsing-fixture-launch.txt 2>&1
    if [ "$destination" = library ]; then heading=Library; else heading=Queue; fi
    capture "$heading" "artifacts/flutter-synthetic-browsing-$destination-light.png"
done
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" appearance dark
xcrun simctl ui "$simulator" content_size accessibility-extra-extra-extra-large
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=browsing-library >> artifacts/flutter-browsing-fixture-launch.txt 2>&1
capture Library artifacts/flutter-synthetic-browsing-large-dark.png

# Expired assisted page keeps its rows in place while recovery is independently scrollable.
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=browsing-expired >> artifacts/flutter-browsing-fixture-launch.txt 2>&1
capture Library artifacts/flutter-synthetic-browsing-expired-large-dark.png
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" content_size large
xcrun simctl ui "$simulator" appearance light
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=browsing-expired >> artifacts/flutter-browsing-fixture-launch.txt 2>&1
capture Library artifacts/flutter-synthetic-browsing-expired-light.png

# Explicit ownership action, pending and control-loss presentation. The native
# proposal/lease is tested separately; these labeled fixtures have no BLE path.
cd "$repo/phone"
flutter build ios --simulator --debug --no-codesign --target test_support/handoff_capture.dart > ../artifacts/flutter-ios-handoff-fixture-build.txt 2>&1
cd "$repo"
xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app
xcrun simctl ui "$simulator" content_size large
xcrun simctl ui "$simulator" appearance light
for mode in ready pending lost; do
    xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
    xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=handoff-$mode" >> artifacts/flutter-handoff-fixture-launch.txt 2>&1
    capture Setup "artifacts/flutter-synthetic-handoff-$mode-light.png"
done
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" appearance dark
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=handoff-ready >> artifacts/flutter-handoff-fixture-launch.txt 2>&1
capture Setup artifacts/flutter-synthetic-handoff-ready-dark.png
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" content_size accessibility-extra-extra-extra-large
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=handoff-ready >> artifacts/flutter-handoff-fixture-launch.txt 2>&1
capture Setup artifacts/flutter-synthetic-handoff-large-dark.png

# Guided setup states are synthetic UI only. The EXIT trap restores the ordinary
# native bridge target even if this capture stage fails.
cd "$repo/phone"
flutter build ios --simulator --debug --no-codesign --target test_support/setup_capture.dart > ../artifacts/flutter-ios-setup-fixture-build.txt 2>&1
cd "$repo"
xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app
xcrun simctl ui "$simulator" content_size large
xcrun simctl ui "$simulator" appearance light
for mode in idle approval choosing connected; do
    xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
    xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=setup-$mode" >> artifacts/flutter-setup-fixture-launch.txt 2>&1
    capture Setup "artifacts/flutter-synthetic-setup-$mode-light.png"
done
xcrun simctl terminate "$simulator" org.dx340.remote.ios
xcrun simctl ui "$simulator" appearance dark
xcrun simctl ui "$simulator" content_size accessibility-extra-extra-extra-large
xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=setup-approval >> artifacts/flutter-setup-fixture-launch.txt 2>&1
capture Setup artifacts/flutter-synthetic-setup-approval-large-dark.png

# Real native export SDK probe, separate from UI/source fixtures. The existing
# EXIT trap restores ordinary main.dart even if this probe fails. No BLE/audio.
cd "$repo/phone"
flutter build ios --simulator --debug --no-codesign --target test_support/report_export_probe.dart > ../artifacts/flutter-ios-report-probe-build.txt 2>&1
cd "$repo"
xcrun simctl terminate "$simulator" org.dx340.remote.ios 2>/dev/null || true
xcrun simctl install "$simulator" phone/build/ios/iphonesimulator/Runner.app
xcrun simctl ui "$simulator" content_size large
xcrun simctl ui "$simulator" appearance light
probe_token="report-$(uuidgen)"
xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=$probe_token" > artifacts/flutter-report-probe-launch.txt 2>&1
probe_container=$(xcrun simctl get_app_container "$simulator" org.dx340.remote.ios data)
python3 - "$probe_container" "$probe_token" <<'PYPROBE'
import json, pathlib, sys, time
container, token = pathlib.Path(sys.argv[1]), sys.argv[2]
marker = container / 'tmp' / ('dx340-' + token + '.json')
for _ in range(60):
    if marker.is_file():
        try:
            value = json.loads(marker.read_text())
            break
        except json.JSONDecodeError:
            pass
    time.sleep(.5)
else:
    raise SystemExit('Native export probe did not complete; ordinary-app restoration still runs')
assert value['token'] == token and value['status'] == 'presented', value
reports = []
for path in (container / 'tmp' / 'dx340-connection-reports').glob('connection-*.json'):
    report = json.loads(path.read_text())
    if report.get('modelInstanceId') == value['modelInstanceId']:
        reports.append((path, report))
assert len(reports) == 1, 'Expected exactly the current native model export'
path, report = reports[0]
assert report['formatVersion'] == 1 and report['platform'] == 'ios'
assert report['authorized'] is False and report['requests'] == [], 'Probe must not create BLE/timing evidence'
assert set(report) == {'formatVersion','clock','capacity','evictedRequests','evictedConnections','requests','connections','platform','authorized','modelInstanceId','connectionPhase','timingScope','privacy','app','os'}
assert all(set(row) == {'atMs','phase'} for row in report['connections'])
pathlib.Path('artifacts/flutter-native-report-export.json').write_bytes(path.read_bytes())
pathlib.Path('artifacts/flutter-report-probe-result.json').write_text(json.dumps(value, indent=2))
PYPROBE
xcrun simctl io "$simulator" screenshot artifacts/flutter-native-report-share-sheet.png >/dev/null
