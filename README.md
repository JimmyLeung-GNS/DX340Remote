# DX340 Bluetooth Remote

**Continuing on another computer:** read [the development handoff](docs/development-handoff.md) and check out `codex/output-volume`. Development was paused by the user on29 October2026; the full system is incomplete.

Development beta: Kotlin DX340 companion and a shared Flutter Android/iPhone interface over native Kotlin/Swift Bluetooth components, sharing BLE protocol v1. Native reference clients are retained during physical parity verification. Music continues through the installed Mango Player or Apple Music on DX340. Everyday control has no Wi-Fi/cloud path.

The shared Flutter phone app **builds and opens on Android and in an iPhone simulator**: Android release build/lint,10 Dart tests and DX340 launch pass; [iPhone CI40729419337](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40729419337) passes analysis/10 tests/unsigned build/launch and6 disconnected capture-readiness checks on iPhone Air/iOS26.5, Xcode26.6. Native Android build/lint/40 JVM tests and Swift3 protocol tests also pass. These results do not verify separate-phone Bluetooth stability or physical iPhone installation. Full browsing acceptance, volume and Watch remain incomplete. See [current status](docs/status.md) and [platform verification](docs/cross-platform-verification.md).

## Build and launch checks

Shared phone UI (Flutter 3.41.4): `./scripts/build-phone.ps1` on Windows, or `sh scripts/build-phone-ios.sh <iPhone-simulator-UDID>` on Mac. Migration and physical acceptance steps: [shared phone migration](docs/shared-phone-migration.md). The commands below build the companion and native reference clients.

```powershell
./scripts/build-android.ps1
./scripts/verify-android-launch.ps1 -Serial <adb-device-serial>
```

Packages and evidence are generated in ignored `artifacts/`. Launching the remote on DX340 checks Android installation/UI only; a separate phone is required for controller Bluetooth testing.

On Mac with Xcode:

```sh
sh scripts/build-apple.sh
xcrun simctl list devices available
sh scripts/build-apple.sh <iPhone-simulator-UDID>
```

The second build command installs/launches the app in the selected simulator and captures evidence. The [Apple CI workflow](docs/continuous-verification.md) also runs this unsigned verification on a standard public-repository Mac runner. Open `apple/DXRemote.xcodeproj` to sign and run on a physical iPhone. Simulator launch cannot establish BLE compatibility.

## Source support so far

- Mango exposes a native library with a user-confirmed selected-track test. Its separately accepted optional screen-on/unlocked folder service has now selected a WAV and a FLAC file, each user-confirmed audible through Mango on AMP15 phone out. Native albums/songs remain available. Full20-track/SD/rescan/playlist/phone and reliability acceptance remain open; [folder test evidence](docs/test-results/2026-10-29-mango-folders.md) separates actual service, simulated component and phone results.
- Apple Music denied the tested direct browse interfaces. The user accepted optional screen-on/unlocked UI-assisted browsing. The actual companion service has returned downloaded albums/songs and selected a song with matching observed playing state. This does not pass phone/BLE or the full offline selection matrix. The mode is pinned to tested Apple Music 6.5.3/code1599.
- Output volume stays disabled until verified against the actual amplifier output.

Use [installation instructions](docs/installation.md), [capability findings](docs/capability-report.md), [acceptance matrix](docs/compatibility-matrix.md) and [protocol specification](protocol/specification.md). Physical, simulated and source-only results are recorded separately. Project constraints are in AGENTS.md and docs/implementation-plan.md.
