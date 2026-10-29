---
name: DX340 Remote
description: Shared phone controls with adaptive system appearance for a Bluetooth music companion.
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
  phone-body: "16px 20px 32px"
  phone-action-outer: "4px 0"
  phone-action-icon-gap: "8px"
  phone-playback-gap: "12px"
  phone-section-before: "24px"
  phone-label-before: "8px"
  phone-label-after: "8px"
  android-label-before: "12dp"
  android-label-after: "8dp"
  apple-playback-gap: "20pt"
components:
  phone-action:
    height: "48 logical px minimum"
  phone-action-icon:
    size: "22 logical px"
  phone-artwork:
    height: "220 logical px"
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

The user selected native system appearance, then one shared Android/iPhone interface on 28 October 2026. Flutter/Dart now owns the phone screens, using stock Material controls on Android and Cupertino controls on iPhone. Light/dark appearance, system typography and familiar control states provide the visual identity. No custom screen palette, typeface or approved comp was introduced.

The shared phone implementation uses Flutter 3.41.4/Dart 3.11.1 above Kotlin Android Bluetooth/foreground-service and Swift CoreBluetooth/restoration code. The DX340 companion and source adapters remain Kotlin; the later Watch client remains native SwiftUI. Existing Kotlin and SwiftUI phone apps are retained as parity references. Their tokens below are explicitly scoped to those references and the companion, rather than applied to Flutter.

Both finish reviewers returned ship for their supplied disconnected captures only: Android dark/default-font screens and the six iPhone simulator states described below. This records the implemented system and those narrow verdicts. Whole-product, physical phone, connection stability and accessibility acceptance remain open.

The optional Mango folders setting and local test surface extend the companion's existing system appearance. A separate reviewer returned ship only for nine DX340 dark/default-font states. That host review does not change the shared phone direction or broaden either earlier phone verdict.

**Key Characteristics:**

- One maintained phone presentation with platform-adaptive system controls.
- Large labeled playback actions with authorization and capability-dependent states.
- Player, Library, Queue and Setup as stable bottom destinations.
- Scrolling connection, recovery and source explanations that follow actual state.

## Colors

Shared phone screen colors come from Flutter's stock Material 3 light/dark themes and Cupertino brightness/theme roles. ThemeMode.system follows the device setting. Errors use Material colorScheme.error on Android and resolved Cupertino systemRed on iPhone. These are system-owned roles; screenshot samples are not an authored palette.

The frontmatter colors describe existing Android launcher vector artwork only. The companion, native Android reference and shared Android phone reuse that mark. They are not application surface or text tokens. The retained Kotlin screens use Theme.DeviceDefault.DayNight. The retained SwiftUI reference uses system foregrounds, secondary metadata and bar material for its library now-playing inset.

**The System Color Rule.** Let platform-adaptive theme roles supply screen colors and control states; keep the launcher palette scoped to the launcher.

## Typography

The shared phone does not specify a custom font family or fixed font-size scale. It uses the Material text theme for headlineSmall track titles, titleLarge section headings, bodyLarge labels and bodyMedium connection/subtitle text. Cupertino buttons and navigation use their stock styles. Material icon glyphs are also used in the Cupertino shell; do not describe them as SF Symbols.

Reading content follows the text scaler and wraps inside scrolling content. Headings expose header semantics; status exposes a live region; artwork and browsing/play icons have semantic labels. The iPhone fixed-height tab bar uses MediaQuery.withNoTextScaling, matching the chrome behavior normally provided by CupertinoTabScaffold, while reading content continues to scale. Its final largest-text screenshot shows only the initial viewport.

Frontmatter typography keys prefixed android- record retained Kotlin host/reference TextView roles, not the shared Flutter type scale. Native Button labels keep platform typography. The SwiftUI reference retains title2 bold track text, footnote wrapping status, subheadline now-playing text and secondary nonempty metadata. Do not translate these semantic styles into invented font measurements.

## Layout

All phone dimensions in the frontmatter's phone- entries are Flutter logical pixels. The body token is CSS-order top/horizontal/bottom, extracted from EdgeInsets.fromLTRB(20, 16, 20, 32). Each screen uses a ListView under its navigation title with connection status, optional error/pending text and destination content. System safe areas and bottom navigation keep chrome separate from scrolling content.

Player actions normally occupy two equal-width columns with separate Play/Pause and Previous/Next rows. They stack when the available control area is below 340 logical pixels wide or the text scaler's scale(1) exceeds 1.4. This is a content-width rule, not a device-width breakpoint. The minimum action height, icon size/gap, row gap and artwork height are the authored phone tokens above. Artwork uses BoxFit.contain. Empty artist/album fields and an empty Source section are omitted.

Shared labels use the before/after spacing tokens, with the section-before token for headings. Each action has the outer padding token; Android OutlinedButton additionally wraps its content in vertical padding of 12 logical pixels. Cupertino library rows use vertical padding of 16 logical pixels and a 16 logical-pixel icon gap. Material ListTile uses zero content padding and minVerticalPadding of 12 logical pixels; rows have a one-logical-pixel divider. These are component source values, not a new universal spacing scale.

The companion remains one scrolling Kotlin setup surface. Its 24px root padding and the native Android reference's raw-pixel root insets are retained implementation debt, not phone layout standards. The native reference uses a four-button row above scrolling content and artwork capped at 240dp. The SwiftUI reference uses NavigationStack/TabView and List/Section, scrollable wrapping status, a library now-playing inset and artwork capped at 240pt; its playback gap/target tokens remain reference-only. Those prior layouts are not the current shared phone shell.

No physical small-phone, landscape, Android large-font or scrolling-reachability acceptance follows from these source values or screenshots.

## Elevation & Depth

The shared phone uses stock Material and Cupertino surface, navigation, button and divider treatments. There is no authored shadow, elevation or motion scale. The companion and native references retain platform button/list/bar treatments. Do not infer reusable shadow values from pixels. Reduced-motion behavior has not been exercised on target controller hardware.

## Shapes

Buttons, navigation indicators, chevrons and platform surfaces inherit framework shapes. No custom corner-radius scale or card family exists. Exact rendered radii and disabled/focus colors remain framework-owned; do not freeze them from these captures.

## Components

### Playback actions

Labeled and explicit. Shared actions use CupertinoButton on iPhone and OutlinedButton on Android, with flexible centered text, optional icons and the minimum source target recorded above. Null callbacks express disabled states. Play and Pause remain distinct desired-state actions. Native authorization, capability and pending-mutation state govern availability; cached titles or lists never authorize playback.

Seeking appears only when the source advertises it; authorized unsupported states explain the limitation. Output volume follows the bounded, locally verified behavior below. The elapsed time is a source snapshot. A successful write does not establish audible playback.

### Bounded output volume

The shared Player uses the existing heading, label and adaptive action components. When the source advertises adjustable volume and supplies verified state, it shows the current level, output label and confirmed range, followed by distinct Volume down and Volume up actions. Each direction disables at its range boundary; authorization and pending-mutation guards also govern availability. Otherwise, the section shows the source's reason or directs the user to verify the source and output in the DX340 companion. Reading text wraps and scrolls; the existing four destinations, stock typography, minimum action targets and Material/Cupertino states remain unchanged. No new visual tokens or assets are introduced.

The companion's local VolumeCheckActivity uses scrolling TextView, EditText and Button controls under its existing system theme. It names the source and output configuration, separates the one-step decrease, human listening confirmation, explicit return and return confirmation, and disables source/output edits during a check. Clear verification / cancel check, Refresh output status and Back to companion remain separate actions. Readback asks the user to confirm only what they heard. The session-only verification and instruction to recheck after changing amp, output, headphones or gain stay visible; cancellation does not promise automatic volume restoration. The retained native phone references still lack volume buttons.

### Library and queue rows

Capability-led lists distinguish browsable folders from playable selections, with a folder/music icon and a chevron for folders. Cupertino rows use buttons; Android rows use ListTile. Nonempty subtitles wrap within the row. Library loading uses an adaptive progress indicator. Parent navigation, native pagination and assisted Show more are distinct actions; queue and library selections use source IDs and revisions.

Disconnected Library and Queue provide connection guidance and Open Setup. Supported native browsing and optional assisted browsing remain separate modes with explicit limitations; full browsing is not reduced to queue skipping. Connected phone content rendering remains outside the finish reviewers' screenshot scope. The optional Mango folder service now has local adapter evidence; both phone BLE routes and full folder acceptance remain open.

### Persistent playback while browsing

Library and Queue show a compact playback bar only while authorized and when the current title is nonempty. It reserves its own space between the scrolling content and the existing four destinations; it does not overlay the list. Player, Setup and disconnected states have no browsing bar. The bar inherits the Material surface/divider roles or resolved Cupertino system background/separator, with stock metadata and playback buttons. No new visual world, assets or tokens are introduced.

The metadata button opens the full Player and exposes the complete title and nonempty artist in its semantic label. Visual title and artist each use one line with ellipsis; above a text scale of 1.4, the artist is omitted visually while remaining in semantics. A separate 48×48-logical-pixel action shows Pause while playing and Play while paused, sending an explicit desired playing state. Authorization, the corresponding canPause/canPlay capability and pending-mutation state govern availability. These source and widget-test properties do not establish physical touch or screen-reader acceptance.

### Navigation

Four stable labels: Player, Library, Queue and Setup. Android uses Material NavigationBar with selectedIndex and system selection treatment. iPhone uses CupertinoNavigationBar and CupertinoTabBar with currentIndex. The destination index is restorable. Open Setup changes it directly. Debug capture routing selects actual disconnected screens without fabricating source data.

Android development taps and the recovery route were observed through actual screenshots on DX340. iPhone capture routing and OCR readiness establish rendered destinations only; native finger taps, scrolling and recovery-action interaction have not been verified there. Synthetic Dart recovery tests are separate software evidence.

### Setup, status and diagnostics

Setup explains that music stays on DX340 and provides access, discovery, connect, approval retry and disconnect actions. Optional assisted browsing explains its awake/unlocked requirement. Connection status, error and pending-action explanations scroll with content; no fixed connection banner covers large text. Error roles are platform adaptive. Technical companion diagnostics stay in its setup flow.

For an already approved phone with a current native busy proposal, Setup places **Take control on this phone** ahead of new-phone instructions, followed by the explanation that taking control disconnects the other approved controller while music stays on DX340. It reuses the shared adaptive action and wrapping label components, without a modal, new visual tokens or assets. Authorization, pending work and consume-once native availability govern the action; sending the request hides it, while pending or control-lost status remains in the scrolling live region. A new native proposal is required before offering the action again. This describes presentation and guards, not verified physical transfer or playback continuity.

The phone owns presentation; native code owns BLE lifecycle, request IDs, epochs and recovery. The UI must preserve capability differences and interruption explanations when transport state changes.

### Optional Mango companion test

The host setting and test use existing Kotlin TextView, Button, ScrollView and AlertDialog controls under Theme.DeviceDefault.DayNight. They inherit the companion typography and minimum action target above; no palette, shape, material or spacing tokens are added. The raw-pixel root padding remains scoped implementation debt.

The setting explicitly names optional Mango folders and explains Accessibility access, the awake/unlocked requirement and revalidation after Mango updates. The local test distinguishes DX340 source-interface checks from phone Bluetooth control, keeps the existing listening volume, and presents the current folder path and limitations in scrolling text. Assisted storage, folder and file labels preserve their source case. Rows without browsing or supported playback are disabled, with one explicit local diagnostic exception: a nativeTestable Mango row may open the separately confirmed public-API experiment even when playable is false. This flag does not authorize ordinary remote selection. Native category labels retain stock button casing. SourceRegistry independently gates native selection at validation, handoff and dispatch; native playable rows require advertised support or stored version-specific verification.

Back, Open Mango Player and Retry library remain available alongside the folder rows. Empty folders explain that no local items are shown and direct the user to Back. Selecting a playable file first opens a confirmation dialog naming the file, the optional UI route and the listening check, with Cancel and Test selected file actions. The native play-by-ID experiment retains its separate explanation. A completed request then offers distinct audible-correct and wrong/silent/failed result actions; request success alone never records audible success. Pending-result and UI play-callback screens are implemented source behavior outside the nine-state visual review.

### Assets and provenance

Android reuses ic_dx340.xml; five generated Flutter Android launcher PNGs were removed. The iPhone AppIcon and transparent launch placeholders remain provisional Flutter-template assets. phone/ASSET_ORIGINS.md and third_party/FLUTTER_LICENSE identify them. All 18 shipping PNGs embed their origins; the recorded embed-prompt scan reported 18 rasters and zero missing origins. No new raster illustration or generated artwork was introduced. Launcher branding is unfinished.

### Evidence and acceptance limits

**Verified, scoped controller-handoff Android rendering:** at04b04aa, artifacts/handoff-android-review.md returns ship for six actual1080×2160 DX340 Android13/API33 captures under artifacts/handoff-captures/: synthetic-handoff-dark.png, synthetic-handoff-light.png, synthetic-handoff-large-dark.png, synthetic-handoff-pending.png, synthetic-handoff-lost.png and ordinary-app-restored.png. Ready states expose the complete takeover action, including the initial large-text viewport; pending/lost states omit it. Provenance records separate debug fixture e38f629b... and restored ordinary release23c536e7..., font1.0/night yes restored with no cleanup errors. The OS floating shortcut's covered patch is excluded. No native detector ran. This verdict covers only those renders, not finger scrolling, TalkBack, separate controller-phone interaction, two-controller BLE, audio continuity or whole-product acceptance. phone/test_support/handoff_capture.dart is explicitly labeled synthetic and rejects control actions; the ordinary app restoration is separate. Incumbent tokens and sidecar remain unchanged.

**Verified, scoped controller-handoff iPhone rendering:** shared workflow40881374524 succeeds at04b04aa (46 tests/builds/launches/readiness/ordinary target restoration). artifacts/handoff-ios-review.md returns ship for six1260×2736 iPhone Air/iOS26.5 hosted simulator captures under artifacts/handoff-flutter-ios-40881374524/: flutter-synthetic-handoff-ready-light.png, flutter-synthetic-handoff-ready-dark.png, flutter-synthetic-handoff-large-dark.png, flutter-synthetic-handoff-pending-light.png, flutter-synthetic-handoff-lost-light.png and flutter-restored-player-light.png. Cupertino treatment/four destinations remain consistent; largest Dynamic Type keeps the complete action visible, with normal scrolling continuation below. This is rendering evidence only, not physical interaction, VoiceOver, BLE/ownership or audio continuity. No detector or raster assets were introduced.

The prior reviewed shared UI capture source is phone/lib/main.dart and phone/lib/remote.dart at revision 5f31f5d. The direction and quality bar are in docs/shared-phone-ui-brief.md. Host/native reference sources remain android/host-app, android/remote-app and apple/DXRemote/RemoteView.swift. The historical phone reviewer records below remain scoped to their supplied captures.

**Verified, scoped browsing-bar rendering at c24be4c:** artifacts/browsing-android-review.md returns ship only for the unobscured lower playback bar/navigation and ordinary Player bar absence in four 1080×2160 DX340 Android 13/API 33 captures: .impeccable/review/browsing-android-synthetic-library-dark.png, browsing-android-synthetic-queue-light.png, browsing-android-synthetic-library-large-dark.png and browsing-android-ordinary-app-restored.png. The Android SystemUI Accessibility move tooltip covers upper list content; that content and obscured recovery controls are excluded from approval. The ordinary restored capture is Player, not disconnected Library/Queue. Other absence states rely on source/widget checks.

artifacts/browsing-ios-review.md independently returns ship for four valid 1260×2736 iPhone Air/iOS 26.5 simulator captures under artifacts/browsing-ios/: flutter-synthetic-browsing-library-light.png, flutter-synthetic-browsing-queue-light.png, flutter-synthetic-browsing-large-dark.png and flutter-restored-player-light.png. The large-text capture is an initial viewport. This verdict covers the supplied rendered views only, with Xcode 26.6/macOS 26.6.2, rather than physical iPhone interaction. No HTML/CSS detector ran for either native surface.

**Verified, separate browsing software and restoration checks:** Flutter iOS CI 40857287517 at c24be4c succeeded with analysis, 42 tests, ordinary/volume/browsing/restoration builds, installs and launches, capture readiness and 12 matcher regression cases. Artifact 12890946500 is downloaded under artifacts/browsing-ios/. Android release build/lint and the 42 Flutter tests passed; native Android host checks passed 103 tests independently. The ordinary Android app was restored with font scale 1/night mode yes, and the Accessibility shortcut position was restored within one pixel. These are software/development-restoration results, not physical controller-phone acceptance.

**Browsing synthetic and physical boundaries:** phone/test_support/browsing_capture.dart is a separate, explicitly labeled debug presentation fixture that cannot send BLE commands; phone/lib/main.dart contains the ordinary browsingPlaybackBar. The fixture's fabricated current-track/library/queue state establishes presentation only. Physical phone BLE, finger interaction, TalkBack/VoiceOver, scrolling reachability and audio acceptance remain not tested. The separate ongoing Apple source listening matrix is source-local evidence and does not contribute to these UI verdicts. Earlier disconnected and volume reviews below retain their original scope.

**Verified, scoped volume rendering:** application checkpoint b3511ec, with no product UI change in the later 8d73a48 backend/capture-matcher correction. artifacts/resumed-volume-android-review.md returns ship for eight supplied 1080×2160 views on DX340 Android 13/API 33: three synthetic phone boundary/large-text views, the ordinary restored phone view, and four local companion check states through the return-listening prompt. The synthetic large-text view is scrolled. artifacts/resumed-volume-ios-review.md returns ship for five supplied 1260×2736 iPhone Air/iOS 26.5 simulator views: synthetic high/light, low/light and large/dark, plus ordinary Player light and large/dark. The synthetic large view is scrolled; the ordinary large view is the initial viewport. Captures are under artifacts/volume-captures/, artifacts/resume-volume-*.png and artifacts/resume-volume-flutter-ios/ as enumerated in the reports. These verdicts accept appearance only; the completed local verification state was not part of the Android review. No HTML/CSS detector ran for this native UI.

**Synthetic evidence boundary:** phone/test_support/volume_capture.dart is a separate debug-only fixture that rejects commands. Its 34–35 labels and both platforms' synthetic captures are presentation evidence, independent of the historical 34–35 listening results and the fresh local 37→36→37 check. They establish neither volume writes nor Bluetooth operation.

**Verified separately, local volume listening and software checks:** docs/test-results/2026-10-29-resumed-volume.md records DX340 Android 13/API 33, Mango 4.1.37/code 240 and Apple Music 6.5.3/code 1599. On AMP15 phone out/headphones, the user separately confirmed the quieter 37→36 step and louder explicit return to 37 for each source; each local source/output context enabled only 36–37. Both sources finished paused at 37. These checks are local adapter/listener evidence, with tracks advancing between delayed answers, not an instantaneous loudness comparison or audio-quality result. Fresh Android build/lint/102 JVM tests and Flutter analysis/36 tests/release APK/lint are separate software checks. No physical controller phone was available.

**Failed, preceding volume iOS automation:** shared Flutter CI 40848442083 at b3511ec compiled and launched, but its large synthetic fixture readiness gate failed when OCR read the icon and label as `» volume up`. The label is visible in the reviewed image. Ordinary-target restore build/install/launch succeeded; that restoration and the five-state visual verdict did not make that workflow successful. Native Swift reference CI 40848442450 at b3511ec succeeded separately with three protocol tests, build/launch/captures and ten matcher cases.

**Verified, corrected volume iOS automation:** Flutter CI 40850675516 and native Swift reference CI 40850675500 at 8d73a48 succeeded after the matcher correction. Artifacts 12888375329 and 12887965219 are downloaded under artifacts/resume-volume-flutter-ios-final/ and artifacts/resume-volume-native-ios-final/. Flutter analysis and 36 tests, ordinary/fixture/restore builds, installs and launches, readiness for six ordinary, three synthetic and one restored capture, and 12 matcher cases passed. Native Swift passed three protocol tests, build/launch/captures and 12 matcher cases. Environment: iPhone Air/iOS 26.5, Xcode 26.6 (17F113), macOS 26.6.2 (25G83). Phone UI code is unchanged; the five corresponding final key images were inspected as valid, without a new independent finish review. The existing reviews remain limited to their supplied b3511ec images. Successful automation establishes neither physical iPhone nor connected phone volume acceptance, and the synthetic captures remain presentation evidence only.

**Verified, preceding shared phone checks:** at 7921d75, CI 40723554411 succeeded with analysis, ten Dart tests, an iOS simulator build and six disconnected captures; artifact 12831164026. Android analysis, ten Dart tests, release APK, release lint and launch on DX340 also passed; APK SHA256 8a38cf21a859deba032e3fd850b60ba57445f6f60349361a5ab6cebb03e87aba. These checks preserve the shared phone direction. They do not create a new visual-review verdict or establish connected phone operation.

**Verified, scoped Mango host evidence:** companion debug 0.1.0, reviewed capture APK SHA256 0153c5740b227bb882341be716672ced480f40c3239473f046a95fd020eca4ac, on actual DX340 Android 13/API 33 with Mango 4.1.37/code 240. This hash identifies the historical capture build; a later capability-gating correction is separate. The independent reviewer confirmed ship remains limited to the unchanged nine 1080×2160 dark/default-font captures in .impeccable/review/mango/: host.png, native-root.png, storage-choices.png, internal-root.png, music.png, tayori.png, files.png, file-dialog.png and empty-sd-music.png. Music is a declared scrolled viewport. Local UI navigation and confirmation/cancel were observed. The captured device already held native selection verification, so the correction leaves these appearances unchanged; a fresh unverified install is visually not tested. The verdict excludes light mode, large fonts, screen readers, pending-result/UI-play-callback screens, phone BLE, audio quality and full-matrix acceptance.

**Verified separately, user-reported audible adapter results:** the companion debug adapter selected 02. 白日夢.wav through Mango, and separately selected 02. 魔法.flac from the Tayori LIVE TOUR “magic” folder. For the FLAC, Mango reported 魔法/album magic playing and Apple Music paused, with Wi-Fi 0/no default network before selection. The user separately confirmed each correct song audible through AMP15 phone out. Both delayed listening replies and source-state observations are separate from the local UI navigation/confirmation observations and the nine-state visual verdict. They do not prove the test screen's play callback, native play-by-ID, phone Bluetooth, audio quality or the full format/library matrix. See docs/mango-ui-experiment.md and docs/test-results/2026-10-29-mango-folders.md.

**Verified, scoped Android evidence:** APK 0.2.0+2, SHA256 5bef92a12d4d530ff95b5907eaae13deacf332ca7f7233e3740e2c1a0ab4ad60, installed on the actual DX340 Android 13/API 33. artifacts/flutter-android-{player,library,queue,setup,recovery}-dark.png are five 1080×2160 disconnected default-font captures. artifacts/flutter-android-capture-provenance.txt records source, package and observed development destination/Open Setup taps. The Android finish reviewer returned ship with no material fixes for only those states. This is not a separate controller phone or BLE test. UIAutomator returned null roots for both Flutter and the native companion; that is neither a screen-reader pass nor a demonstrated screen-reader failure. Analysis, nine Dart tests, release APK and release lint passed in artifacts/flutter-build-final-ui.txt.

**Verified, scoped iPhone simulator evidence:** CI 40713390473 succeeded at 5f31f5d; artifact 12828870069. Environment: iPhone Air/iOS 26.5, Xcode 26.6 (17F113), macOS 26.6.2 (25G83), Flutter 3.41.4/Dart 3.11.1. Analysis, nine Dart tests, unsigned Runner.app build/install, six launches and two-ready-frame OCR gates passed. artifacts/flutter-ios-40713390473/ contains flutter-player-light.png, flutter-library-light.png, flutter-queue-light.png, flutter-setup-light.png, flutter-player-dark.png and flutter-player-large-dark.png plus build/test/toolchain/readiness logs. The iPhone finish reviewer returned ship with no material fixes for only these six disconnected states. The largest-text image is an initial viewport; scrolling, VoiceOver and native finger interaction remain unverified. Earlier 24d0484 label collision/faded Setup captures are superseded by this final evidence.

**Verified reference checks:** Kotlin host/native Android reference build, lint and 29 JVM tests passed after controller-core extraction (artifacts/shared-controller-native-build.txt). Native Swift reference CI 40710376253 passed three protocol tests, build, launch and captures after timeout/stale-callback guards. Historical native screenshots under artifacts/apple-ci-40699505189/ and artifacts/android-remote-launch.png remain reference evidence; artifacts/ui-host-final.png predates optional Apple controls. These checks do not broaden shared UI acceptance.

**Not tested / open:** signing and install on a physical iPhone; separate Android controller hardware; connected library/queue rendering on either phone; target-phone TalkBack/VoiceOver, contrast, scrolling and large-font accessibility; BLE interoperability/security/background/range/reconnection, source audio, walking and battery acceptance. No web detector ran for this adaptive mobile UI. Native references remain until Flutter parity is physically verified. Full-system work is active and incomplete; source browsing and volume acceptance remain governed by docs/status.md.

## Do's and Don'ts

### Do:

- **Do** use stock Material/Cupertino appearance and system theme roles for the shared phone.
- **Do** preserve scalable reading content, semantic labels and the authored minimum action target.
- **Do** keep Player, Library, Queue and Setup stable and connection guidance scrollable.
- **Do** let authorization, source capability and pending state decide available actions.
- **Do** retain companion/native-reference tokens in their stated scope until parity is verified.
- **Do** identify device, source revision and capture/interaction limits when accepting UI work.

### Don't:

- **Don't** turn launcher artwork colors or screenshot samples into a screen palette.
- **Don't** copy the retained Kotlin reference's raw-pixel insets or four-button row into shared phone standards.
- **Don't** invent a custom typeface, radius, shadow scale or browser approximation of native rendering.
- **Don't** treat disconnected screenshots, synthetic tests or builds as physical accessibility, Bluetooth, source-audio or whole-product acceptance.
- **Don't** describe provisional iPhone template icons as finished launcher branding.
