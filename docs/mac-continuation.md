# Move this checkpoint to the next test machine

The local branch codex/source-recovery-checkpoint has not been pushed or published. The generated source bundle artifacts/dx340-checkpoint.bundle contains that branch and reachable tracked history, including the current handoff. It excludes ignored APKs/logs/work tools and signing keys. artifacts/checkpoint-manifest.json records the exact source commit, bundle hash and APK hashes. Verify the manifest against the actual transferred files; do not assume older reports describe the latest packages.

## Mac source/build

Copy the bundle and manifest securely to the Mac. Replace the example path below with its actual location:

```sh
shasum -a 256 /path/to/dx340-checkpoint.bundle
git clone --branch codex/source-recovery-checkpoint /path/to/dx340-checkpoint.bundle DX340Remote
cd DX340Remote
git bundle verify /path/to/dx340-checkpoint.bundle
git rev-parse HEAD
git status --short
xcodebuild -version
xcrun simctl list devices available
```

Confirm HEAD and the bundle SHA256 match checkpoint-manifest.json. Read AGENTS.md, docs/development-handoff.md and docs/status.md. Use the project pin Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0, with a compatible installed Xcode. To provision the pin inside this checkout without changing a global Flutter installation:

```sh
mkdir -p work
git clone --depth 1 --branch 3.41.4 https://github.com/flutter/flutter.git work/flutter
test "$(git -C work/flutter rev-parse HEAD)" = ff37bef603469fb030f2b72995ab929ccfc227f0 || exit 1
export PATH="$PWD/work/flutter/bin:$PATH"
flutter --version
```

If work/flutter already exists, verify its revision instead of overwriting it. Record the actual Mac/Xcode/iPhone model/OS/build/team; none is inferred from earlier hosted CI. Select a real available iPhone simulator UDID from the listing:

```sh
sh scripts/build-phone-ios.sh '<available-iPhone-simulator-UDID>'
```

This must compile the current Swift bridges/client/reference changes, install/launch the shared UI and inspect its captures. Old CI results do not verify this revision. Fix actual compiler/runtime errors in place; retain the native apple/ reference client.

For physical iPhone installation, open phone/ios/Runner.xcworkspace in Xcode, choose the user's signing team/device and run Runner. Account/credential prompts are completed by the user; do not place credentials in chat or Git. Follow phone-reliability-runbook.md offline against the actual DX340. Simulator/rendering success does not prove Bluetooth, signing, background/range, authorization or audio. Record failures and actual commands/results under docs/test-results.

## Separate Android phone

Transfer artifacts/dx340-phone-0.2.0-development.apk and its manifest/hash to the test PC/phone. The companion is already installed on DX340. Identify the **separate phone's** ADB serial:

```powershell
adb devices -l
$phoneSerial = '<separate-phone-serial>'
./scripts/verify-android-signing.ps1 -Serial $phoneSerial -Apk artifacts/dx340-phone-0.2.0-development.apk
adb -s $phoneSerial install -r artifacts/dx340-phone-0.2.0-development.apk
adb -s $phoneSerial shell am start -W -n org.dx340.remote.controller/org.dx340.remote.dx340_phone.MainActivity
```

Do not select DX340 serial d as proof of this phone gate. Do not uninstall/reset an existing app to bypass a signer mismatch. The original Android debug keystore stays outside Git and is needed for later Android updates; it is intentionally absent from the source bundle. Existing signed APKs can be transferred without transferring the private key.

Complete Setup/Nearby Devices/notification prompts on the phone, start the companion, open its pairing window, accept authenticated system bonding, approve the new controller on DX340 and retry authorization. Then run the full runbook with Wi-Fi/cellular off. Prior source-local reads/audible samples do not pass the phone matrix.

## Acceptance still required

The full P0–P9/T01–T18 scope remains in implementation-plan.md/completion-audit.md. In particular: real BLE on both phones,30-minute sleep/range/security/fault tests,100 controls/source/phone, current Mac/iPhone builds/signing, source20-track/format matrices, Apple5 albums/2 playlists, Mango populated-playlist/SD/rescan proof, measured volume/audio/battery/walk and complete installation/upgrade acceptance. Mango's observed Playlist list is empty; no playlist was created. Do not invent collection or audio evidence. Approved-controller handoff needs measured connection-limit behavior before selecting/validating its mechanism. Watch follows stable phone acceptance.

The plan's separate10,000-track synthetic metadata/performance fixture also remains unverified and can be checked independently; it must not be counted as phone or actual-source latency proof.
