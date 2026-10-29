# DX340 Bluetooth Remote

Development beta: Kotlin DX340 companion and a shared Flutter Android/iPhone interface over native Kotlin/Swift Bluetooth components, sharing BLE protocol v1. Native reference clients are retained during physical parity verification. Music continues through the installed Mango Player or Apple Music on DX340. Everyday control has no Wi-Fi/cloud path.

Current controller checkpoint04b04aa adds explicit handoff between already approved controllers. Native build/lint120 tests, Flutter analysis46 tests/release/lint and hosted native/shared iOS builds/launches pass (Swift5/shared46). One Windows approval-denial smoke test passes on the candidate, then the earlier listening companion is restored; physical two-phone handoff remains **NOT TESTED**. Installed companion04846d60... and shared phone23c536e7... have separate evidence scopes. Read the [handoff test report](docs/test-results/2026-10-29-controller-handoff.md) and [physical runbook](docs/controller-handoff-runbook.md). Full system/phone reliability/Watch acceptance remains incomplete.

The earlier Mango listening baseline passes108 native tests/build/lint and adds bounded native Mango page caching, verified with read-only DX340 probes. Its recorded package pair was companion04846d60.../phoneb12f83e4.... Shared phone source c24be4c retains42 Flutter tests/release/lint and [iPhone simulator CI40857287517](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40857287517). Earlier20 offline Apple audio confirmations span mixed builds/four rejected attempts; they are source-local evidence. Twenty Mango file selections across five album folders (11 WAV/9 FLAC) are also user-confirmed audible, including two later-page files;21 attempts retain one diagnostic title mismatch. These are local adapter results. Physical phone BLE, native playlists/SD/wider formats/reliability and Watch remain incomplete. See [current status](docs/status.md), [cache report](docs/test-results/2026-10-29-native-page-cache.md) and [Apple listening matrix](docs/test-results/2026-10-29-apple-listening-matrix.md) and [Mango listening matrix](docs/test-results/2026-10-29-mango-listening-matrix.md).

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
