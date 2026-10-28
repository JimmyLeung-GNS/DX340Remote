# DX340 Bluetooth Remote

Development beta: Kotlin DX340 companion and a shared Flutter Android/iPhone interface over native Kotlin/Swift Bluetooth components, sharing BLE protocol v1. The user selected this phone UI migration on 28 October 2026; verification is in progress and native reference clients are retained. Music continues through the installed Mango Player or Apple Music on DX340. Everyday control has no Wi-Fi/cloud path.

Android APK builds, lint and JVM tests pass on Windows. Both Android apps have opened on the physical DX340. The native iOS app has now **compiled and opened in an iPhone Air/iOS26.5 simulator** on GitHub's standard Mac runner (Xcode26.6); three Swift protocol tests passed. This does not verify installation on the user's iPhone. Separate Android/iPhone Bluetooth acceptance and Watch remain incomplete. See [current status](docs/status.md) and [platform launch verification](docs/cross-platform-verification.md).

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

- Mango exposes a native library; one selected-track audio test was confirmed by the user on AMP15 phone output. Desired-state pause/pause/play/play was observed working through the adapter. Full FLAC/WAV, folders/playlists and reliability acceptance remain open.
- Apple Music denied the tested direct browse interfaces. The user accepted optional screen-on/unlocked UI-assisted browsing. The actual companion service has returned downloaded albums/songs and selected a song with matching observed playing state. This does not pass phone/BLE or the full offline selection matrix. The mode is pinned to tested Apple Music 6.5.3/code1599.
- Output volume stays disabled until verified against the actual amplifier output.

Use [installation instructions](docs/installation.md), [capability findings](docs/capability-report.md), [acceptance matrix](docs/compatibility-matrix.md) and [protocol specification](protocol/specification.md). Physical, simulated and source-only results are recorded separately. Project constraints are in AGENTS.md and docs/implementation-plan.md.
