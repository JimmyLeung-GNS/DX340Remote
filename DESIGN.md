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
  apple-status-padding: "8pt"
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

This document records current Kotlin Views and SwiftUI source, plus the supplied Android captures. It is a handoff for continuing native work, not whole-product visual acceptance. No Figma, web, comp or Flutter migration was selected. Android navigation polish remains open; Apple compilation and visual verification remain unapproved.

**Key Characteristics:**

- Native typography and appearance that follow system settings.
- Large playback targets with capability-dependent disabled states.
- Player, Library, Queue and Setup destinations.
- Setup and diagnostic detail separated from everyday playback.

## Colors

Screen colors come from the operating system theme. Android host and controller inherit `Theme.DeviceDefault.DayNight`; SwiftUI uses default foregrounds, `.secondary` metadata and `.bar` status backgrounds. Do not freeze the dark DX340 screenshot colors into an application palette. Light appearance and contrast across target phones have not been visually verified.

The frontmatter colors describe only the shared Android launcher artwork: a dark shell and white screen/radio mark from both `res/drawable/ic_dx340.xml` files. They are not button or background tokens. No secondary, tertiary, error or accent palette is defined by the app.

**The System Color Rule.** Let native theme roles supply screen colors and enabled, disabled, pressed and focus states; keep the launcher palette scoped to the launcher.

## Typography

Android explicitly selects the platform `sans` family. Frontmatter records observed `TextView.textSize` roles in scalable `sp`; it does not assign those values to platform button labels. The Mango test title also uses the track-title size. Headings set the accessibility heading trait. Status is selectable on the host and a polite accessibility live region on the remote.

Apple uses the system font and native text styles: track title `.title2` with `.bold()`, default list body text, `.footnote` connection status and `.subheadline` library now-playing text. Artist, album and explanatory metadata use secondary foreground emphasis. Preserve semantic styles rather than translating them into fixed point sizes. Dynamic Type and large Android font settings still require device review.

## Layout

Android uses vertical `LinearLayout` containers and scrolling content. The remote keeps its title, connection status and four equal-width destination buttons above a scrolling body. The host is one scrolling setup surface. Action buttons use the minimum height in frontmatter; destination buttons use its fixed height. Library and queue show now-playing text below their body content. Track artwork preserves aspect ratio with a 240dp maximum height.

Apple uses one `NavigationStack` per `TabView` destination, native `List`/`Section` layout, and safe-area status bars. The library has a separate now-playing inset. Playback actions use the minimum frames in frontmatter; artwork scales to fit within 240pt height. Playback spacing and connection-status padding are recorded in frontmatter. Other list margins and default `.padding()` remain system-owned.

Current Android root margins are raw pixel values: host content uses 24px on each edge; remote insets add 20px horizontally and 12px vertically, after an initial 20px padding. These are implementation debt, not a density-independent spacing scale. System-bar insets are handled, but density, landscape, small-screen and large-text layouts remain unaccepted. There are no authored breakpoints or responsive grids.

## Elevation & Depth

No custom shadow or elevation vocabulary exists. Android native buttons provide their theme treatment; Apple list sections and bar materials provide grouping. Retain these platform treatments. No bespoke animation duration or easing is defined. Native motion should respect system reduced-motion preferences; that behavior has not been exercised on target Apple hardware.

## Shapes

Controls inherit platform corner shapes and native focus/pressed styling. The app defines no reusable radius token, custom card family or decorative clipping system. Do not infer an exact corner radius from screenshots because device theme and density determine the rendered result.

## Components

### Playback actions

Android currently presents separate full-width text actions for Play, Pause, Previous and Next. SwiftUI presents labeled SF Symbol buttons in one borderless row. Both gate supported actions using authorization and source capabilities. Preserve independent Play and Pause semantics. The seek action is visible but disabled when unsupported; volume is explanatory text pending physical verification. Minimum target dimensions describe source intent, not completed accessibility acceptance.

### Library and queue rows

Android uses native buttons; Apple uses native buttons and folder `NavigationLink` rows. Folder navigation, playable selections and pagination remain distinct actions. Capability explanations appear near unavailable browsing/queue controls. Apple adds a native library loading indicator. Keep source-scoped stale-result behavior intact when changing presentation; scoped lifecycle and stale-result fixes were source-reviewed, not physically accepted.

### Navigation

The four destination names are stable. Apple uses native tabs with system icons and navigation titles. Android currently uses four ordinary text buttons in one row, with no selected-state semantics or active destination treatment. This is open polish debt; do not copy it as an approved navigation standard. Screen recreation/navigation restoration and large-text fit still need device review.

### Setup, status and diagnostics

Setup explains that playback stays on DX340 and guides discovery, pairing and controller authorization. The companion exposes native action buttons for permissions, local tests, approval, optional Apple Music browsing and diagnostics. Technical diagnostics are toggled on the host. Status text describes actual connection/source state; sending a command is not proof of audible playback. No custom input field, chip, tooltip or card component exists.

### Evidence and pending review

Source references: `android/host-app/src/main/kotlin/org/dx340/remote/host/MainActivity.kt`, `android/remote-app/src/main/kotlin/org/dx340/remote/controller/MainActivity.kt`, both Android `res/values/styles.xml`, and `apple/DXRemote/RemoteView.swift`.

Observed Android captures: `artifacts/android-remote-launch.png` shows disconnected Player with disabled controls; `artifacts/ui-host-final.png` shows the companion setup screen. Both show the DX340 dark native theme. The host capture predates optional Apple Music controls now present in source, so its filename does not establish current visual acceptance. These are not separate controller-phone or iPhone captures. DX340 inventory records Android 13, Mango 4.1.37 and Apple Music 6.5.3; app-build/capture correspondence and full accessibility checks remain unresolved. No Apple screenshot, compiled Apple app or physical iPhone verification is available in this handoff. See `docs/status.md` for the broader acceptance record. No browser approximation is provided as a substitute for native rendering.

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
- **Don't** claim Apple UI acceptance, Bluetooth interoperability or audible playback from source or screenshots.
