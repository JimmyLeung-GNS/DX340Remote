# Pending task UX-01: app UI and logo design

Requested31 October2026 during the device-switch handoff. **IN PROGRESS — connection/setup clarity selected by the owner.** Original logos/icons and the broader setup/Companion redesign remain pending.

Latestf5f5a03: source-scoped page scroll restoration fixes the second inherited-offset defect;53 Flutter/162 JVM and hosted Swift12/shared53 builds pass. Current Pixel765… has two owner-audible assisted-folder selections, stable pending/tab-return bounds and saved-grant reconnect; earlier Intro is208… evidence. Seven-image follow-up review is scoped to supplied Pixel/simulator renders. Bounded native setup cleanup is implemented; unapproved current-Pixel/iPhone expiration and full accessibility remain untested. Stale Back after playback still requires fresh-root recovery; improve that guidance and approval visibility next. [Current evidence](test-results/2026-10-31-mango-phone-selection.md); no full UX-01/icon completion.

The owner then reported Library rows jumping under a finger, failed subsequent playback and a lost connection. Prioritize that defect before the broader layout work. Source42b68ab isolates changing Library/Queue status from row geometry, keys row identity and rejects stale selections;51 Flutter tests and Android release/lint pass. Updated ordinary Pixel app208a797e… is installed with the original signer. Four disconnected Pixel light/dark/default/large captures have a narrow ship review. Connection/local approval and original playback failures remain separate unfinished physical work. See [incident and correction](test-results/2026-10-31-library-target-stability.md). No full UX-01 or actual iPhone acceptance follows from this fix.

Improve the DX340 Companion and shared Android/iPhone Remote interface and create a coherent, original app-icon family. Preserve the user's existing choice: one shared Flutter phone interface with native system appearance, Kotlin/Swift Bluetooth underneath, and Kotlin companion/source adapters. Keep Mango playback and Apple-managed downloads.

Deliverables:

- Review the actual current setup, approval expiry/retry, Player, Library, Queue, source switching, pending actions, disconnected/error/invalidated states and the companion's long scrolling screen. Improve hierarchy, navigation, readable metadata, touch targets and recovery guidance.
- Produce consistent light/dark designs using native platform conventions. Validate large text, contrast, TalkBack/VoiceOver and supported/unsupported controls. Keep Bluetooth connection state separate from playback state; preserve safe volume gating, source/epoch checks and explicit controller handoff.
- Create original Remote and Companion icons with a recognizable family and clearly different roles. Supply editable source masters, Android adaptive/monochrome assets and iOS AppIcon assets. Prepare a Watch variant when that target is implemented; the phone-stability gate for Watch remains intact.
- Implement and review the designs on Android and iPhone, using real device states where available and explicitly labelled fixtures otherwise. Record screenshots/builds and limits of physical verification. Retain native reference clients until shared parity is verified.

Do not add Wi-Fi/cloud dependencies, hide screen-on/unlock requirements for optional browsing, enable unverified actions or replace playback engines as part of the redesign. Keep protocol and lifecycle behavior intact while changing presentation.

Acceptance: an agreed design direction, complete source/platform icon assets, both phone builds plus companion build, inspected state/accessibility coverage, and no regression of verified Bluetooth/source behavior. Physical iPhone, audio and reliability requirements remain separate evidence gates.

Read development-handoff.md/status.md and the latest dated test report before starting. Use current app versions and user decisions; do not restart old framework/style questions already answered.
