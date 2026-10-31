# Bounded phone request reports

31 October2026. Full goal **ACTIVE/incomplete**. App-code source `956e5bf936e71ede95b14f09ec19e2be1e6fe06a`; analyser source `50c080d89672664684aeec6a233563f74f830b30`. This adds observations and deliberate local export, without new transport, authorization, retry, source-selection or playback behavior. See [format, privacy and interpretation](../connection-reports.md).

## VERIFIED — local implementation and builds

Kotlin/Swift native journals retain up to256 requests and256 phase assignments, sanitize categories and omit music/argument/Bluetooth/credential fields. First ACCEPTED and terminal receipts are immutable; changed-host/unknown replies are rejected before journaling. Terminal receipt and actual native application of the reply are separate. Null durations survive JSON export. History is excluded from normal Flutter snapshots and introduces no additional library/queue observer notification.

On Windows11 DESKTOP-T9ICUE4, pinned Flutter3.41.4/Dart3.11.1, Android Studio JBR and SDK36.1 build tools:

- `scripts/build-android.ps1`: native Companion/reference/probe builds and lint PASS; **174 JVM tests** PASS:53 protocol,3 controller JSON,7 cache,111 adapters.
- `scripts/build-phone.ps1`: analysis, **72 Flutter tests**, release APK and lint PASS.
- `python -m unittest discover -s scripts/tests -p test_connection_reports.py -v`: **8 synthetic analyser tests** PASS. Duplicate exports, conflicting immutable/terminal values, mixed environments/sessions, evictions, missing/invalid clock values and bounded/duplicate-key JSON inputs are covered.

All three Android candidates retain the original development certificate SHA256 `24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d`. Candidate hashes:

| Candidate | SHA256 | Installation scope |
| --- | --- | --- |
| Shared phone0.2.0/code2 | `66f12aaffe3aed081fd478cf2ca1c446b7c4e3429f0064ca5360b251a5a63ae8` | Android emulator only |
| Companion0.1.0/code1 | `6faf6fd7d20bb67292317c57c256efcecca43edd97b64bf21aa935b52685b16e` | Built, not installed |
| Native reference0.1.0/code1 | `7f679f198ae4e28fa7cc264672f393713453a80ac139710f4aed7877fe670ea0` | Built, not installed |

Earlier intermediate candidates/logs remain under the same artifact directory with distinct names. The current generic artifact paths contain the latest candidates, not necessarily the versions on physical devices. Failed initial test-harness runs are retained: Kotlin comparison through `JSONObject(Map)` omitted nulls; Flutter cleanup double-disposed its model and reset the platform override too late. Those checks were corrected, not treated as product passes. Actual Android serialization has three explicit-null/privacy/receipt regression tests.

## VERIFIED — actual Android SDK export in an emulator

Headless `emulator-5560`, AVD Medium_Phone_API_36.1, Android16/API36 `BP41.250822.007`, generic `sdk_gphone64_x86_64`; ordinary release target, no synthetic source/BLE fixture. Installation and launch succeeded. Fresh UI-tree bounds drove Setup → Connection help → Save connection report. No Windows foreground input was shared.

MediaStore created one app-owned `application/json` row at `Download/DX340Remote/connection-29bb144a-9093-48ae-83a4-671bd648953a.json`, `is_pending=0`. The960-byte exported file passed the strict root/app/transition key checks; its `baseApkSha256` matches the candidate. It has one idle phase, no requests and `authorized=false`. The analyser returns zero requests, no timing groups and `physicalT04=NOT_ADJUDICATED`, rather than a latency pass.

Before/after native service dumps retain PID4149 and `startForegroundCount=0`, with a bound service only. The client operation log is empty. Saving did not connect, authorize or queue any source/Bluetooth request. Flutter behavioral tests separately verify single-flight, native failure/retry, empty-result handling, disposal and both platform help controls.

One bounded Android light/portrait inspection covered the normal report action, actual saved notice and200% text. Action/helper/long filename wrap without horizontal overflow; scrolling reaches all copy. Measured report-action glyph/background contrast is **6.12299851:1**, RGB103/80/164 over254/247/255. This is a scoped new-action review, not full UX-01, dark mode, iOS or screen-reader acceptance. Font1.0/nightno restored. Only the exact created MediaStore row600 was deleted after pulling evidence; then the emulator was shut down. A subsequent ADB inventory is empty. No physical device setting was changed.

## NOT TESTED — current physical and Apple scope

At this report's initial checkpoint, hosted native run41159819250 and shared run41159819268 are **in progress** at app source956e5bf. Their terminal outcomes, archive digests and UIKit export probe must be checked before citing current Apple compilation/export. Earlier source532 simulator archives retain their own scope.

Pixel and DX340 disappeared from USB after the earlier completed timeout check. This is not evidence of a BLE failure. Last physically installed/re-pulled versions remain Pixel `0cf86c9cfead4b27b3b2c1dcf2135b98fbe78fb992bc755491a1549d79b2fdc0` and Companion `491dbbffabda8f7a20d64a2f591caa486427e036ba9e3821d831ccf6eff5934a`, with the preservation/final paused37 state in [timeout guidance](2026-10-31-timeout-guidance.md). The journal candidate has no physical Bluetooth/source/audio/background result. The [completed1802.562-second awake/paused check](2026-10-31-controlled-background.md) remains tied to Pixel6b/source532 and does not transfer to either0cf or66f.

Owner Apple-listening availability remains unanswered; no audio test starts from silence. Physical iPhone signing/Bluetooth, full T04/T05, calls/in-flight/range/sleep/battery/walking, source/format/SD/output/handoff/Watch remain open. Preserve existing approvals/bonds and optional browsingON; do not repeat PC revocation. Re-inventory hardware before physical action.

Raw packages/logs/reports/screenshots are under ignored `artifacts/request-journal-20261031/`; `local-evidence-sha256.json` binds15 local artifacts and `candidate-packages.json` binds package roles. The adjacent sanitized JSON records statuses and limits. No store upload, production signing key or recipient selection occurred.
