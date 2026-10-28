# DX340 Bluetooth Remote

Development beta: Kotlin DX340 companion, Kotlin Android remote and SwiftUI iPhone remote sharing BLE protocol v1. Music continues through the installed Mango Player or Apple Music on DX340. Everyday control has no Wi-Fi/cloud path.

Android APK builds, lint and JVM tests pass on Windows. Both Android apps have opened on the physical DX340. The iOS project exists but has **not been compiled or opened**: this session has no Mac/Xcode. Separate Android/iPhone Bluetooth acceptance and Watch remain incomplete. See [current status](docs/status.md) and [platform launch verification](docs/cross-platform-verification.md).

## Build and launch checks

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

The second build command installs/launches the app in the selected simulator and captures evidence. It is prepared for Mac execution and has not run here. Open `apple/DXRemote.xcodeproj` to sign and run on a physical iPhone. Simulator launch cannot establish BLE compatibility.

## Source support so far

- Mango exposes a native library; one selected-track audio test was confirmed by the user on AMP15 phone output. Desired-state pause/pause/play/play was observed working through the adapter. Full FLAC/WAV, folders/playlists and reliability acceptance remain open.
- Apple Music denied the tested direct browse interfaces. The user accepted optional screen-on/unlocked UI-assisted browsing. The actual companion service has returned downloaded albums/songs and selected a song with matching observed playing state. This does not pass phone/BLE or the full offline selection matrix. The mode is pinned to tested Apple Music 6.5.3/code1599.
- Output volume stays disabled until verified against the actual amplifier output.

Use [installation instructions](docs/installation.md), [capability findings](docs/capability-report.md), [acceptance matrix](docs/compatibility-matrix.md) and [protocol specification](protocol/specification.md). Physical, simulated and source-only results are recorded separately. Project constraints are in AGENTS.md and docs/implementation-plan.md.
