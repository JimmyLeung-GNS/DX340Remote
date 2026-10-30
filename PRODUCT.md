# Product
<!-- impeccable:product-schema 1 -->

## Platform
adaptive

## Stack
User selected a shared phone interface: Flutter/Dart screens for Android and iPhone, Kotlin Android Bluetooth/foreground service and Swift CoreBluetooth/restoration bridges. DX340 host/source adapters stay Kotlin. Watch stays native SwiftUI. Native phone apps remain parity references.

## Users
The owner controls a DX340 kept in a bag from an iPhone or Android phone.

## Product Purpose
Bluetooth-only control and full browsing while installed Apple Music and Mango remain the playback engines on DX340. Watch controls follow stable phone operation.

## Operating Context
Current31 October2026 Windows session has DX340 Android13 and Pixel9a Android16 connected over USB. Public GitHub Mac CI can build/run iOS simulators; no physical iPhone, Mac or Watch is attached here. Inventory is session-specific and must be checked again after a device switch. Physical acceptance is distinct from compilation and simulated tests.

## Capabilities and Constraints
Follow AGENTS.md and docs/implementation-plan.md. No everyday network dependency, root, firmware changes or private Apple downloads. Preserve Mango. Real source capabilities must be measured. User confirmed native system appearance and simpler connection/setup. One Connect guides permission, discovery/remembered-player connection and local approval; another player or controller takeover remains deliberate. Optional Mango/Apple screen browsing stays enabled at the owner's request and requires the documented awake/unlocked source context.

Wider DAP support is a future direction, not a universal compatibility claim. Current Android minimum is API31; the measured DAP is DX340 and current source adapters target Mango/Apple Music. Personal Google Play distribution is a feasibility direction; account creation, payment and uploads are separate owner actions. Keep the original development signing identity for current device upgrades.

## Evidence on Hand
docs/device-inventory.md, docs/capability-report.md and dated docs/test-results reports track measured evidence. Actual Pixel-to-DX340 protected connection, saved-grant reads/Disconnect cleanup and separately scoped owner-audible source selections are recorded with their exact package hashes. Do not transfer older audio proof to a later package. Full radio interoperability, physical iPhone,30-minute background/range/battery, audio-quality and whole-product acceptance remain incomplete. docs/status.md is the current checkpoint; builds and simulator/export previews prove only their declared scopes.

## Accessibility & Inclusion
Large native playback targets, scalable text, screen-reader labels and explicit disabled/error states.
