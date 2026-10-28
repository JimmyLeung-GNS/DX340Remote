#!/bin/sh
set -eu
if [ "$(uname -s)" != Darwin ]; then
    printf '%s\n' 'iOS verification requires macOS with Xcode. No iOS build was performed.' >&2
    exit 2
fi
simulator=${1:-}
if [ "$#" -gt 1 ]; then
    printf '%s\n' 'Usage: sh scripts/build-apple.sh [iPhone-simulator-UDID]' >&2
    exit 2
fi
repo=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$repo"
mkdir -p artifacts
xcodebuild -version > artifacts/xcode-version.txt
sw_vers > artifacts/macos-version.txt
swift test --package-path apple --scratch-path artifacts/swift-build > artifacts/swift-tests.txt 2>&1
destination='generic/platform=iOS Simulator'
if [ -n "$simulator" ]; then destination="platform=iOS Simulator,id=$simulator"; fi
xcodebuild -project apple/DXRemote.xcodeproj -scheme DXRemote -destination "$destination" -derivedDataPath artifacts/apple-simulator CODE_SIGNING_ALLOWED=NO build > artifacts/apple-build.txt 2>&1
if [ -n "$simulator" ]; then
    # boot fails if already booted; bootstatus below is the authority.
    xcrun simctl boot "$simulator" 2>/dev/null || true
    xcrun simctl bootstatus "$simulator" -b > artifacts/apple-simulator-boot.txt 2>&1
    xcrun simctl install "$simulator" artifacts/apple-simulator/Build/Products/Debug-iphonesimulator/DXRemote.app
    xcrun simctl launch "$simulator" org.dx340.remote.ios > artifacts/apple-simulator-launch.txt 2>&1
    sleep 2
    xcrun simctl spawn "$simulator" launchctl list > artifacts/apple-simulator-processes.txt
    xcrun simctl io "$simulator" screenshot artifacts/apple-simulator-launch.png
    for tab in library queue setup; do
        xcrun simctl terminate "$simulator" org.dx340.remote.ios
        xcrun simctl launch "$simulator" org.dx340.remote.ios "--capture-tab=$tab" >> artifacts/apple-simulator-launch.txt 2>&1
        sleep 1
        xcrun simctl io "$simulator" screenshot "artifacts/apple-$tab-light.png"
    done
    xcrun simctl terminate "$simulator" org.dx340.remote.ios
    xcrun simctl launch "$simulator" org.dx340.remote.ios --capture-tab=player >> artifacts/apple-simulator-launch.txt 2>&1
    xcrun simctl ui "$simulator" appearance dark
    sleep 1
    xcrun simctl io "$simulator" screenshot artifacts/apple-player-dark.png
    xcrun simctl ui "$simulator" content_size accessibility-extra-extra-extra-large
    sleep 1
    xcrun simctl io "$simulator" screenshot artifacts/apple-player-large-dark.png
    xcrun simctl ui "$simulator" content_size large
    xcrun simctl ui "$simulator" appearance light
    printf '%s\n' 'Simulator launch command succeeded; inspect the screenshot and app. Physical iPhone and Bluetooth acceptance remain untested.'
else
    printf '%s\n' 'Simulator compilation completed. To install/launch, rerun with an available iPhone simulator UDID from: xcrun simctl list devices available'
fi
