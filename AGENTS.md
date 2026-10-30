# Project instructions

## Scope

Read `docs/implementation-plan.md` and `docs/status.md` before implementing. The full system is a DX340 BLE companion, iPhone remote, Android remote and later Watch extension. Begin with P0 through P2, then continue through the remaining stages. Do not stop at a plan, skeleton or mock UI when implementation is requested.

Keep Wi-Fi, hotspots and cloud services out of the everyday control path. Music plays on DX340 through its existing source app. Preserve the Android Mango Player unless the user explicitly accepts a concrete alternative. Apple Music downloads remain managed by the installed Apple Music app. No root, firmware modifications, private-download extraction or impersonation of another application's identity.

## Engineering

The user selected a shared phone interface on 28 October 2026. Use Flutter/Dart for the shared Android/iPhone UI, retaining Kotlin Android Bluetooth/foreground-service code and Swift CoreBluetooth/restoration code underneath it. Keep the DX340 companion and source adapters in Kotlin; the later Watch client remains native SwiftUI. Retain the native phone reference apps until Flutter parity is verified. Share a versioned BLE protocol. Follow applicable skills and current primary documentation. Prefer native lifecycle and Bluetooth APIs. Maintain the source adapters separately from transport and UI. Implement only supported actions; report capability differences explicitly.

Use meaningful tests for framing, duplicate commands, routing, stale IDs, reconnection and capability handling. Run actual build commands where supported. A successful mock, BLE write or compile does not prove source playback, audio quality or physical Bluetooth support.

## Evidence and continuity

On 30 October 2026 the user explicitly chose to switch development to **another Windows PC**, superseding the 29 October stay-on-this-PC instruction. The exporting checkout is `C:\GitHub\DX340Remote`; verify the portable checkpoint and inventory the next PC's actual workspace/toolchain rather than assuming the same paths. Read `docs/device-continuation.md` for transfer/resume and `docs/windows-continuation.md` for the prior local build workflow. Mac/Xcode remains the separate iPhone build/validation host.

Update `docs/status.md` with completed work, commands/results, unresolved facts and next actions. Store device inventory, capability findings and physical test results under `docs/`. Label each result as verified, failed, blocked or not tested, identifying device, OS, app versions and evidence.

Do not reduce full browsing to play/pause or queue skipping. Full Apple Music browsing requires selecting a downloaded song outside the current queue while offline. Full Mango integration requires selecting a local file that actually plays through Mango. Accessibility-based browsing and replacement playback engines are separate decisions with stated tradeoffs.

When hardware, signing or a Mac is unavailable, complete independent implementation and checks, prepare exact user-run steps, and record what needs physical verification. Keep unverified scope visible and follow the environment's goal lifecycle rules. Do not claim completion from proxy evidence.

## Boundaries

Work in this repository. Use `work/` for scratch files and `artifacts/` for generated packages and logs. Keep secrets and signing keys out of Git. Do not publish, buy services, contact vendors, create another chat or alter the user's device configuration beyond authorized development actions. These actions require direct user authorization. Routine local edits, builds and tests are within the implementation scope.
