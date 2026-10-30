# Pending task UX-01: app UI and logo design

Requested31 October2026 during the device-switch handoff. **PENDING — design and implementation have not started.**

Improve the DX340 Companion and shared Android/iPhone Remote interface and create a coherent, original app-icon family. Preserve the user's existing choice: one shared Flutter phone interface with native system appearance, Kotlin/Swift Bluetooth underneath, and Kotlin companion/source adapters. Keep Mango playback and Apple-managed downloads.

Deliverables:

- Review the actual current setup, approval expiry/retry, Player, Library, Queue, source switching, pending actions, disconnected/error/invalidated states and the companion's long scrolling screen. Improve hierarchy, navigation, readable metadata, touch targets and recovery guidance.
- Produce consistent light/dark designs using native platform conventions. Validate large text, contrast, TalkBack/VoiceOver and supported/unsupported controls. Keep Bluetooth connection state separate from playback state; preserve safe volume gating, source/epoch checks and explicit controller handoff.
- Create original Remote and Companion icons with a recognizable family and clearly different roles. Supply editable source masters, Android adaptive/monochrome assets and iOS AppIcon assets. Prepare a Watch variant when that target is implemented; the phone-stability gate for Watch remains intact.
- Implement and review the designs on Android and iPhone, using real device states where available and explicitly labelled fixtures otherwise. Record screenshots/builds and limits of physical verification. Retain native reference clients until shared parity is verified.

Do not add Wi-Fi/cloud dependencies, hide screen-on/unlock requirements for optional browsing, enable unverified actions or replace playback engines as part of the redesign. Keep protocol and lifecycle behavior intact while changing presentation.

Acceptance: an agreed design direction, complete source/platform icon assets, both phone builds plus companion build, inspected state/accessibility coverage, and no regression of verified Bluetooth/source behavior. Physical iPhone, audio and reliability requirements remain separate evidence gates.

Read development-handoff.md/status.md and the latest dated test report before starting. Use current app versions and user decisions; do not restart old framework/style questions already answered.
