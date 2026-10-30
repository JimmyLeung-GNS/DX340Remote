# Mac build and additional test-device workflow

For a Mac destination, first read device-switch-handoff.md and development-handoff.md, then inventory the actual Mac/Xcode/iPhone and signing access. The last verified development host was Windows; its paths do not apply to a new Mac. This guide covers Mac builds, physical iPhone validation and additional test devices. Development is paused until the user resumes it.

The active branch `codex/source-recovery-checkpoint` is published in the authorized public repository [JimmyLeung-GNS/DX340Remote](https://github.com/JimmyLeung-GNS/DX340Remote). Clone/fetch that branch and verify the handoff commit before changing code. A separately transferred `artifacts/dx340-checkpoint.bundle` is an offline alternative; it contains tracked history, excluding ignored APKs/logs/work tools and signing keys. Its local checkpoint-manifest.json records the source/bundle/package hashes. Verify transferred files against that manifest; these ignored artifacts are not included in a GitHub clone.

## Mac source/build

For a new checkout, use the published branch:

```sh
git clone --branch codex/source-recovery-checkpoint https://github.com/JimmyLeung-GNS/DX340Remote.git DX340Remote
cd DX340Remote
git rev-parse HEAD
git status --short
```

Compare HEAD with the outgoing handoff prompt. If the checkout already exists, inspect and preserve local changes before fetching/updating it.

Offline alternative: copy the bundle and manifest securely to the Mac. Replace the example path below with its actual location and use a new destination directory:

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

Current companion40114ff8.../86-test build and source-local prefetch comparison are recorded in test-results/2026-10-29-mango-prefetch.md. Preceding isolated controller-management checks are recorded in test-results/2026-10-29-controller-management.md. Per-controller revocation and authorization-lifetime guards are implemented; authenticated physical revocation/reapproval remains unverified. The preceding same-device recovery and unresolved Playlist timing are recorded in test-results/2026-10-29-mango-folder-timing.md. Source-local Playlist recovery still exceeds3seconds after the settling improvement; measure actual arrival/rendering over each phone separately. The manifest identifies the current package independently of older report hashes.

The full P0–P9/T01–T18 scope remains in implementation-plan.md/completion-audit.md. In particular: real BLE on both phones,30-minute sleep/range/security/fault tests,100 controls/source/phone, current Mac/iPhone builds/signing, source20-track/format matrices, Apple5 albums/2 playlists, Mango populated-playlist/SD/rescan proof, measured volume/audio/battery/walk and complete installation/upgrade acceptance. Mango's observed Playlist list is empty; no playlist was created. Do not invent collection or audio evidence. Approved-controller handoff needs measured connection-limit behavior before selecting/validating its mechanism. Watch follows stable phone acceptance.

The plan's separate10,000-track synthetic metadata component fixture now passes on Windows; see test-results/2026-10-29-synthetic-metadata.md. LibraryIndex is standalone, not wired into the source adapter. Repeat its component check if useful, but actual cached/uncached page latency must still be measured over each phone/source path; the synthetic timings cannot fill that gate.

Source-only navigation/inventory scripts and their scope are documented in test-results/2026-10-29-source-library-inventory.md. The generated artifacts/apple-listening-plan.json is excluded from the Git bundle; optionally transfer it separately for the pending20-selection matrix, or regenerate on the DX340 test PC. It contains candidate metadata, not audio or verified playback. Revalidate fresh IDs/revisions/source/offline/queue state for actual selections; current listening availability must be established first.

This Windows PC also has a real diagnostic BLE central: see windows-ble-diagnostics.md, test-results/2026-10-29-windows-protocol-preparation.md and test-results/2026-10-29-windows-ble.md. Host advertising/GATT enumeration and unpaired denials are verified; authenticated pairing needs the operator's native consent and is not verified. Read-only approval/state diagnostics now have19 software checks and a live unpaired refusal, not authenticated protocol proof. The isolated Python environment/raw addresses/logs are excluded from the bundle; reproducible dependency pins/scripts are tracked. Windows radio scripts require Windows; pure test_windows_ble_protocol.py conformance checks use only Python3.11+ and can run on the Mac. This path is development evidence only and does not replace either phone test.
