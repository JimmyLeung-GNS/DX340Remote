# Accessibility grant loss and item capability boundaries

29 October2026, Asia/Shanghai. **VERIFIED for the actual DX340 adapter checks and software tests below; physical phone BLE/current iOS compilation remain unverified.**

Device d: DX340 Android13/API33, build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240; Apple6.5.3/code1599. Both sources stayed paused. No playback, volume, music-file, playlist-editing or notification-listener grant change was sent.

## Actual finding and correction

An Apple pending Songs browse stopped correctly with UI_PERMISSION_REQUIRED when its already-granted Accessibility service was removed. However, the returned capabilities still advertised canPlayNativeItem/canPlayItem despite the denied native browser and unavailable assisted service. Its generic MediaSession play-by-ID action bit was being treated as an available native library path.

Native selection now requires both a connected browser (where IDs are actually obtained/revalidated) and the existing advertised/verified selection support. UI readiness can enable assisted selection independently. Apple item selection therefore becomes unavailable while its UI service is absent, while ordinary MediaSession controls/queue access remain available. Mango's ready/verified native path remains available while only folder assistance is absent. The optional Local folders root row is explicitly disabled while that adapter is unavailable.

The shared phone also gates cached assisted rows/back/scroll/play requests on the assisted path's current readiness, so Mango's native capabilities cannot keep old UI rows actionable. Native Kotlin/Swift clients discard assisted pages/pending browse locations when that readiness changes, while retaining native pages and queues. Retained reference views apply the same availability guard. Swift navigation scope stays tied to source/epoch/mode, rather than changing every time a hybrid folder page opens. These client mechanisms/software checks do not prove delivery over real BLE.

## Physical grant test

```powershell
./scripts/verify-pending-accessibility.ps1 -Serial d
# If needed, select a known working ADB executable explicitly:
./scripts/verify-pending-accessibility.ps1 -Serial d -Adb (Get-Command adb).Source
```

The script requires both companion services already granted and bound, global Accessibility enabled and both music sessions paused. It records the original set, starts a real assisted browse, verifies STARTED, then removes only that source's component from enabled_accessibility_services after250ms. The other service stays enabled and bound; accessibility_enabled and notification access are unchanged. An inner finally restores that component; the outer safety finally restores all originally granted companion components before checking the complete original set. Unknown/empty/unsafe settings are rejected. No new service is granted and no production application enables Accessibility automatically.

Android documents system-managed binding, onServiceConnected and onUnbind in its [AccessibilityService lifecycle](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). The development test follows the enabled-component setting observed by the [Android13 accessibility manager](https://android.googlesource.com/platform/frameworks/base/+/android-13.0.0_r1/services/accessibility/java/com/android/server/accessibility/AccessibilityManagerService.java); the OEM device results below are the authority for this firmware. No UIAutomator/instrumentation was used.

| Source | Pending result | Final repeat elapsed | New assisted request while absent | Native queue while absent | Old token after rebind | Fresh page |
|---|---|---|---|---|---|---|
| Apple | UI_PERMISSION_REQUIRED |565ms|UI_PERMISSION_REQUIRED|50 first-window rows|STALE_LIBRARY|Songs,6 rows|
| Mango | UI_PERMISSION_REQUIRED |805ms|UI_PERMISSION_REQUIRED|28 rows|STALE_LIBRARY|Internal Storage,9 rows|

VERIFIED: both original service grants restored, both rebound without a host restart, no crashed/binding services, global Accessibility flag and notification setting unchanged, ordinary session capabilities preserved, metadata retained and both sources paused. Apple's unavailable item flags are now false. Mango's native root still reads while its UI is absent, with the assisted folder row disabled. Timings include the ADB/settings/report round trip; they are not BLE latency. The first complete corrected run also passed at543/793ms.

One subsequent repeat hit a Windows ADB daemon connection failure after the Apple case. Fresh checks confirmed both grants restored and both services bound. SDK and scrcpy ADB both reported36.0.0-13206524; no version-mismatch or app/firmware cause is inferred. A complete isolated repeat through the verified working executable then passed both cases. Evidence is retained; the failed repeat is not counted as an app failure or passing complete run.

Raw evidence: artifacts/pending-accessibility/summary.json and per-case STARTED/interrupted/revoked/restored/old-token/fresh-page/queue/root reports, services-before/after.json and dumpsys captures. The pre-fix capability snapshot is retained under pending-accessibility-probe/. Repeat output is pending-accessibility-isolated-repeat.txt. No new audible selection was tested.

## Software, packages and continuity

VERIFIED: native build/lint74 JVM tests; Flutter analysis32 tests/release APK/release lint. Four new Flutter tests cover stale assisted actions/native escape and native album access, plus disabled cached UI rows with native refresh on Android/iPhone layouts. Existing UI fixtures were corrected to declare the readiness they represent. An initial analyzer braces failure was fixed before the passing build. Logs: artifacts/ui-readiness-final-android-build.txt and ui-readiness-phone-build-verified.txt.

The existing in-process Mango capability regression also passed with25 native rows/25 revoked rows checked, UNSUPPORTED_CAPABILITY for unverified native selection, old revision rejection and zero UI calls. It uses real native Mango with simulated UI readiness/isolated verification preferences and is explicitly component evidence, not another physical permission test. Report: ui-readiness-mango-component.json.

Original-key signing preflights and in-place upgrades passed; installed bytes matched generated APKs. Four companion preference files remain unchanged. Current installed hashes:

| Artifact | SHA256 |
|---|---|
| Companion0.1.0 debug |8ab231222145f0cd2addef286e2bbeecdb963caf8de943a9b5a99860f46b2694|
| Shared phone0.2.0+2 development release |09aaae7836a98a77c3ce52c870873dc39191dde846e286947d18892875a963a2|

Fresh awake/unlocked cold launch of the phone on DX340 rendered its disconnected Player correctly; ui-readiness-phone-player.png was inspected. Same-device launch is not a phone BLE test. Current Swift changes remain uncompiled here and require a Mac. No default key/global environment was changed; keys remain outside Git.

The original source/format/SD/playlist/audio/volume, physical Android/iPhone BLE/security/background/range, current iOS signing/build and Watch gates remain open. Automatic approved-controller handoff also needs actual host/controller connection-limit evidence. This checkpoint does not complete the system.
