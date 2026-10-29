# DX340 Bluetooth Remote

Development beta: Kotlin DX340 companion and a shared Flutter Android/iPhone interface over native Kotlin/Swift Bluetooth components, sharing BLE protocol v1. Native reference clients are retained during physical parity verification. Music continues through the installed Mango Player or Apple Music on DX340. Everyday control has no Wi-Fi/cloud path.

Current checkpoint8d73a48: Android build/lint102 JVM tests; shared Flutter analysis36 tests/release/lint; Windows read-only diagnostic23 software checks pass. Installed companion395be729.../phone7db636c5.... Both corrected iOS simulator workflows pass: [shared Flutter](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40850675516), [native Swift reference](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40850675500). Local Mango and Apple37→36→37 steps were separately confirmed audible on AMP15 phone out; proof clears on host restart. Windows authenticated read sessions passed earlier, but intermittent failures/live revocation and physical phone BLE remain unresolved. Full browsing/audio/reliability/Watch acceptance remains incomplete. See [current status](docs/status.md), [volume report](docs/test-results/2026-10-29-resumed-volume.md) and [completion audit](docs/completion-audit.md).

Development continues on this Windows PC in `C:\GitHub\DX340Remote`, branch `codex/source-recovery-checkpoint`. Start with [Windows resume steps](docs/windows-continuation.md) and [development handoff](docs/development-handoff.md). The local Git bundle/manifest are a recovery checkpoint. A second Windows PC is not required; the separate Mac guide covers iPhone-specific build/validation work.

## Build and launch checks

Shared phone UI (Flutter 3.41.4): `./scripts/build-phone.ps1` on Windows, or `sh scripts/build-phone-ios.sh <iPhone-simulator-UDID>` on Mac. Migration and physical acceptance steps: [shared phone migration](docs/shared-phone-migration.md). The commands below build the companion and native reference clients.

```powershell
./scripts/build-android.ps1
./scripts/verify-android-launch.ps1 -Serial <adb-device-serial>
```

Packages and evidence are generated in ignored `artifacts/`. Launching the remote on DX340 checks Android installation/UI only; a separate phone is required for controller Bluetooth testing.

For the shared iPhone app, follow [Mac/iPhone validation](docs/mac-continuation.md) and use `phone/ios/Runner.xcworkspace` for physical signing. The commands below exercise the retained native Apple reference client on a Mac with Xcode:

```sh
sh scripts/build-apple.sh
xcrun simctl list devices available
sh scripts/build-apple.sh <iPhone-simulator-UDID>
```

The second build command installs/launches the native reference app in the selected simulator and captures evidence. The [Apple CI workflow](docs/continuous-verification.md) documents its unsigned verification. `apple/DXRemote.xcodeproj` remains the native reference project; the shared phone app is the canonical interface. Simulator launch cannot establish BLE compatibility.

## Source support so far

- Mango exposes a native library with a user-confirmed selected-track test. Its separately accepted optional screen-on/unlocked folder service has now selected a WAV and a FLAC file, each user-confirmed audible through Mango on AMP15 phone out. Native albums/songs remain available. Full20-track/SD/rescan/playlist/phone and reliability acceptance remain open; [folder test evidence](docs/test-results/2026-10-29-mango-folders.md) separates actual service, simulated component and phone results.
- Apple Music denied the tested direct browse interfaces. The user accepted optional screen-on/unlocked UI-assisted browsing. The actual companion service has returned downloaded albums/songs and selected a song with matching observed playing state. This does not pass phone/BLE or the full offline selection matrix. The mode is pinned to tested Apple Music 6.5.3/code1599.
- Output volume stays disabled until verified against the actual amplifier output.

Use [installation instructions](docs/installation.md), [capability findings](docs/capability-report.md), [acceptance matrix](docs/compatibility-matrix.md) and [protocol specification](protocol/specification.md). Physical, simulated and source-only results are recorded separately. Project constraints are in AGENTS.md and docs/implementation-plan.md.
