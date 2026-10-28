---
name: DX340 Remote
description: Native system controls for a Bluetooth music companion.
colors:
  launcher-shell: "#263238"
  launcher-mark: "#FFFFFF"
typography:
  android-app-title:
    fontFamily: "sans"
    fontSize: "28sp"
  android-track-title:
    fontFamily: "sans"
    fontSize: "26sp"
  android-section-title:
    fontFamily: "sans"
    fontSize: "24sp"
  android-body:
    fontFamily: "sans"
    fontSize: "18sp"
  android-status:
    fontFamily: "sans"
    fontSize: "16sp"
spacing:
  android-label-before: "12dp"
  android-label-after: "8dp"
  apple-playback-gap: "20pt"
components:
  android-action:
    size: "48dp minimum height"
  android-destination:
    height: "56dp"
  apple-playback:
    size: "44pt minimum width and height"
---

# Design System: DX340 Remote

## Overview

**Creative North Star: "Native system interface"**

The user selected native system appearance on 28 October 2026. Use platform typography, colors, navigation and control states. The current interface is a functional diagnostic beta with large playback actions and explicit capability explanations. It has no separate screen palette or custom display typeface.

This document records current Kotlin Views and SwiftUI source, Android captures and the corrected six-image Apple simulator matrix. It is a handoff for continuing native work, not whole-product visual acceptance. No Figma, web, comp or Flutter migration was selected. Android navigation polish remains open. Apple builds and opens on the hosted Mac; review resolved three scoped disconnected-state fixes, with broader visual and interaction acceptance still open.

**Key Characteristics:**

- Native typography and appearance that follow system settings.
- Large playback targets with capability-dependent disabled states.
- Player, Library, Queue and Setup destinations.
- Setup and diagnostic detail separated from everyday playback.

## Colors

Screen colors come from the operating system theme. Android host and controller inherit `Theme.DeviceDefault.DayNight`; SwiftUI uses default foregrounds, `.secondary` metadata and a `.bar` library now-playing background. Connection status uses a native list section. Do not freeze screenshot colors into an application palette. Apple light and dark disconnected screens are captured in the simulator; contrast across physical target phones has not been verified.

The frontmatter colors describe only the shared Android launcher artwork: a dark shell and white screen/radio mark from both `res/drawable/ic_dx340.xml` files. They are not button or background tokens. No secondary, tertiary, error or accent palette is defined by the app.

**The System Color Rule.** Let native theme roles supply screen colors and enabled, disabled, pressed and focus states; keep the launcher palette scoped to the launcher.

## Typography

Android explicitly selects the platform `sans` family. Frontmatter records observed `TextView.textSize` roles in scalable `sp`; it does not assign those values to platform button labels. The Mango test title also uses the track-title size. Headings set the accessibility heading trait. Status is selectable on the host and a polite accessibility live region on the remote.

Apple uses the system font and native text styles: track title `.title2` with `.bold()`, default list body text, `.footnote` connection status and `.subheadline` library now-playing text. Artist, album and explanatory metadata use secondary foreground emphasis. Empty artist and album strings are omitted. Preserve semantic styles rather than translating them into fixed point sizes. The largest-text dark Player simulator capture shows wrapping connection status in scrolling content; physical Dynamic Type, VoiceOver and large Android font settings still require review.

## Layout

Android uses vertical `LinearLayout` containers and scrolling content. The remote keeps its title, connection status and four equal-width destination buttons above a scrolling body. The host is one scrolling setup surface. Action buttons use the minimum height in frontmatter; destination buttons use its fixed height. Library and queue show now-playing text below their body content. Track artwork preserves aspect ratio with a 240dp maximum height.

Apple uses one `NavigationStack` per `TabView` destination and native `List`/`Section` layout. Every destination places `ConnectionStatusSection` inside its scrolling list; its footnote text uses `.fixedSize(horizontal: false, vertical: true)` to wrap vertically. Connection status is not a root safe-area inset. This prevents the prior largest-text overlap while retaining native scrolling. The library alone has a separate now-playing inset. Playback actions use the minimum frames in frontmatter; artwork scales to fit within 240pt height. Playback spacing is recorded in frontmatter. Other list margins and default `.padding()` remain system-owned. The large-text screenshot is an initial viewport, not proof of scrolling or every control's reachability.

Current Android root margins are raw pixel values: host content uses 24px on each edge; remote insets add 20px horizontally and 12px vertically, after an initial 20px padding. These are implementation debt, not a density-independent spacing scale. System-bar insets are handled, but density, landscape, small-screen and large-text layouts remain unaccepted. There are no authored breakpoints or responsive grids.

## Elevation & Depth

No custom shadow or elevation vocabulary exists. Android native buttons provide their theme treatment; Apple list sections and bar materials provide grouping. Retain these platform treatments. No bespoke animation duration or easing is defined. Native motion should respect system reduced-motion preferences; that behavior has not been exercised on target Apple hardware.

## Shapes

Controls inherit platform corner shapes and native focus/pressed styling. The app defines no reusable radius token, custom card family or decorative clipping system. Do not infer an exact corner radius from screenshots because device theme and density determine the rendered result.

## Components

### Playback actions

Android currently presents separate full-width text actions for Play, Pause, Previous and Next. SwiftUI presents labeled SF Symbol buttons in one borderless row. Both gate supported actions using authorization and source capabilities. Preserve independent Play and Pause semantics. The seek action is visible but disabled when unsupported; volume is explanatory text pending physical verification. Disconnected Apple Player omits empty metadata and the Source section when no sources exist. Minimum target dimensions describe source intent, not completed accessibility acceptance.

### Library and queue rows

Android uses native buttons; Apple uses native buttons and folder `NavigationLink` rows. Folder navigation, playable selections and pagination remain distinct actions. Capability explanations appear near unavailable browsing/queue controls. Disconnected Apple Library and Queue explain that a connection in Setup is needed and provide an `Open Setup` button that changes the selected tab. Authorized sources without browsing or queue capabilities retain source-specific explanations. Apple adds a native library loading indicator. Keep source-scoped stale-result behavior intact when changing presentation; scoped lifecycle and stale-result fixes were source-reviewed, not physically accepted.

### Navigation

The four destination names are stable. Apple uses native tabs with system icons and navigation titles, selected through a typed destination binding. A DEBUG-only `--capture-tab=` launch argument selects actual native destinations for screenshots; it does not simulate source data or a connection. Capture routing and OCR readiness checks do not prove tab taps or `Open Setup` interaction. Android currently uses four ordinary text buttons in one row, with no selected-state semantics or active destination treatment. This is open polish debt; do not copy it as an approved navigation standard. Screen recreation/navigation restoration and physical large-text fit still need review.

### Setup, status and diagnostics

Setup explains that playback stays on DX340 and guides discovery, pairing and controller authorization. The companion exposes native action buttons for permissions, local tests, approval, optional Apple Music browsing and diagnostics. Technical diagnostics are toggled on the host. Apple connection status is a shared scrollable native section with the updates-frequently accessibility trait; it remains visible in disconnected screens without occupying a fixed root inset. Status text describes actual connection/source state; sending a command is not proof of audible playback. No custom input field, chip, tooltip or card component exists.

### Evidence and pending review

Source references: `android/host-app/src/main/kotlin/org/dx340/remote/host/MainActivity.kt`, `android/remote-app/src/main/kotlin/org/dx340/remote/controller/MainActivity.kt`, both Android `res/values/styles.xml`, and `apple/DXRemote/RemoteView.swift`.

Observed Android captures: `artifacts/android-remote-launch.png` shows disconnected Player with disabled controls; `artifacts/ui-host-final.png` shows the companion setup screen. Both show the DX340 dark native theme. The host capture predates optional Apple Music controls now present in source, so its filename does not establish current visual acceptance. These are not separate controller-phone or iPhone captures. DX340 inventory records Android 13, Mango 4.1.37 and Apple Music 6.5.3; Android app-build/capture correspondence and full accessibility checks remain unresolved.

Verified hosted Apple build evidence: Xcode 26.6 (17F113), macOS 26.6.2 (25G83), iPhone Air simulator running iOS 26.5; app build and launch succeeded and all three Swift protocol tests passed. App UI revision `77f828c` and capture-readiness revision `412b87a` produced the valid6 image matrix under `artifacts/apple-ci-40699505189/`: `apple-simulator-launch.png`, `apple-library-light.png`, `apple-queue-light.png`, `apple-setup-light.png`, `apple-player-dark.png` and `apple-player-large-dark.png`. Build, launch, version, Swift-test and capture-readiness logs sit alongside them. OCR readiness gates heading/tab visibility only; it is not interaction evidence.

Reviewer disposition `ship:3` resolves only three scoped corrections: scrollable connection status avoids the prior largest-text overlap; disconnected Player omits blank metadata and orphan Source; disconnected Library/Queue provide correct recovery copy and `Open Setup`. No whole-product visual approval, tab/button tap testing, VoiceOver, connected library/queue data, physical iPhone or Bluetooth acceptance follows from that disposition or this capture matrix. See `docs/status.md` for the broader acceptance record. No browser approximation is provided as a substitute for native rendering.

## Do's and Don'ts

### Do:

- **Do** use native system typography, colors and control feedback.
- **Do** preserve large playback targets, scalable text and screen-reader labels.
- **Do** explain unsupported capabilities and show actual connection state.
- **Do** keep Player, Library, Queue and Setup distinct and diagnostics in setup flows.
- **Do** verify current builds on target devices in light, dark and large-text settings before acceptance.

### Don't:

- **Don't** promote launcher colors into a fixed screen palette.
- **Don't** treat the Android four-button navigation row or raw-pixel margins as approved final patterns.
- **Don't** replace native components with a different UI stack without a selected migration.
- **Don't** expand the three scoped Apple review fixes into whole-product, interaction, accessibility, Bluetooth or audible-playback acceptance.
