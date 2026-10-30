# Mac/iPhone test commands

Keep Windows development on the new PC; the Mac supplies Xcode/signing/iPhone execution. Import the same checkpoint on the Mac with the included Python verifier and a new clone destination. Use a compatible installed Xcode and the pinned Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0. The runner inventories tools; it does not install SDKs, sign in, change system settings or buy/enroll services.

## 1. Inventory before choosing a device

```sh
cd /your/verified/DX340Remote
python3 scripts/mac-iphone-tests.py preflight
```

Read the new artifacts/mac-iphone-tests/<run>/summary.json and command logs. They record actual Mac/Xcode/SDK/Swift/Flutter versions, available iPhone simulators and devicectl physical-device metadata when available. Missing OS/build/Developer Mode information stays unknown. The reported iOS27.2 is not treated as a verified build. Compare actual hardware/software with [Apple's Xcode requirements](https://developer.apple.com/xcode/system-requirements/); the selected physical build is the final compatibility check.

If pinned Flutter is missing, provision it inside this clone without changing a global SDK:

```sh
git clone --depth 1 --branch 3.41.4 https://github.com/flutter/flutter.git work/flutter
test "$(git -C work/flutter rev-parse HEAD)" = ff37bef603469fb030f2b72995ab929ccfc227f0
export PATH="$PWD/work/flutter/bin:$PATH"
python3 scripts/mac-iphone-tests.py preflight
```

If work/flutter already exists, verify it; do not overwrite it. The current shared project has no Podfile/native Flutter plugin dependency, so CocoaPods is not made an artificial preflight blocker. Install/resolve it if a future plugin actually requires it. Full Xcode setup and iPhone trust/Developer Mode/signing steps are documented by [Flutter](https://docs.flutter.dev/platform-integration/ios/setup). Account passwords, trust/Developer Mode and keychain prompts stay with the owner.

## 2. Simulator verification

Choose a real available iPhone simulator UDID from preflight; none is chosen automatically:

```sh
python3 scripts/mac-iphone-tests.py simulator --simulator YOUR_SIMULATOR_UDID
```

This creates a detached, committed-source worktree with isolated evidence, then runs the existing native Swift tests/build/captures followed by shared Flutter analysis/tests/build/readiness captures. Both apps use the same bundle ID, so the shared ordinary app runs last; capture fixtures restore that target on exit. Inspect captured PNGs; matcher success is not a new visual ship review. Simulator results never establish Bluetooth, audio, signing or phone acceptance. Repeated runs get new output folders and preserve previous results; no generated worktree/evidence is deleted automatically.

## 3. Signed ordinary iPhone app

Connect and unlock the iPhone on the Mac; handle its Trust/Developer Mode prompts. Choose the exact device identifier from preflight and your actual Apple signing team ID. The project/default app ID is org.dx340.remote.ios:

```sh
python3 scripts/mac-iphone-tests.py device-build --device YOUR_DEVICE_ID --team YOUR_TEAM_ID
```

This analyzes/tests the shared app, builds lib/main.dart for iphoneos, then signs the Release Runner with the selected team. It does not install, export private keys or enable automatic provisioning/account/device registration. If first signing fails for missing profiles, open **that run's recorded source worktree** phone/ios/Runner.xcworkspace in Xcode, select the team/device and resolve signing interactively once. Then rerun the command. This keeps machine-specific signing edits out of the primary source checkout. If Xcode reports the default bundle ID unavailable, use your own explicitly chosen ID with --bundle-id and retain that identity for future upgrades.

Successful output is labelled SIGNED_ORDINARY_DEVICE_BUILD_PASSED. Signature/team, actual iphoneos platform, app identity and ordinary Flutter target are checked. A simulator or synthetic capture app is rejected before installation.

## 4. Explicit installation and launch

Use the successful build run directory from step3:

```sh
python3 scripts/mac-iphone-tests.py device-install --device YOUR_DEVICE_ID --run /absolute/path/to/artifacts/mac-iphone-tests/BUILD_RUN
```

The source, app path, selected device, signature/team, bundle ID and ordinary target are rechecked. Install/launch uses Apple's [Xcode command-line tools](https://developer.apple.com/documentation/xcode/xcode-command-line-tool-reference). Success means the commands succeeded; inspect the iPhone screen and handle Bluetooth permission manually before calling the app usable.

## 5. Physical acceptance, recorded separately

Run phone-reliability-runbook.md and controller-handoff-runbook.md. Record iPhone model/exact OS/build, Mac/Xcode, source commit, app ID/team, DX340/app versions and selected companion APK hash. Keep the PC's saved approval for development; disconnect its live diagnostic link during first iPhone enrollment/session on the installed single-controller host. Initial setup may use internet; actual remote tests require DX340/iPhone Wi-Fi off and iPhone cellular data off.

Start with authorization/current metadata and a deliberate play/pause at the owner's chosen level. Record actual source state separately from human audio. Then complete T01–T18: discovery/control/latency,30minute sleep/range recovery,20 Apple outside-queue downloads/5 albums/2 playlists, Mango native/folder selection/SD/rescan, bounded verified volume, interruptions/security/faults/install, matched battery/walking and later Watch. Do not mark unexecuted rows passed. Optional assisted browsing still requires DX340 awake/unlocked. Mango native Album metadata635 versus All Music500 is a source-category finding, not playback proof. Use ordinary targets for real BLE; screenshot fixtures have no real control path.

Full physical tests may need a second Android controller and a newer companion candidate for two-peer handoff. Preserve/verify the existing installed baseline before any upgrade; candidate audio evidence must be earned separately. No Android private key is needed for Mac/iPhone signing, and no Windows PC relocation beyond the user's chosen move is required.
