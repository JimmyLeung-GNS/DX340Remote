# Apple Music UI-assisted branch — original research and current adoption

Original separate read-only APK: NOT TESTED/uninstalled. Current companion adapter: accepted by the user and physically sampled; see installation.md and test-results/2026-10-28-own-apple-adapter.md. Native MediaBrowser failed; Media3 connected as our own package but getLibraryRoot returned -4 (permission denied) on Apple Music 6.5.3 / DX340 Android 13. Root access denial prevents claiming ordinary direct browsing. See docs/test-results/2026-10-28-source-probe.md.

A separate optional **read-only** research APK (`org.dx340.remote.probe`) provides an accessibility probe. It is not bundled into the companion and is not installed or enabled by default. It does not click, navigate remotely, enter credentials, read private downloads or expose its snapshots over BLE. This separate read-only service remains uninstalled. After the user accepted optional screen-on/unlocked operation, the real companion click adapter was implemented and its AccessibilityService enabled through Settings. Its evidence and limitations are recorded separately; the historical research instructions below are not required for product setup.

Tradeoffs before any adoption: it requires the screen on and unlocked to inspect a visible Apple Music window; app updates and localization can change the accessible tree; missing labels may make arbitrary selection impossible; it does not satisfy the in-bag screen-off workflow by itself. Any production click adapter would need a versioned library-screen allowlist, snapshot-scoped IDs, fail-closed navigation and user acceptance of the changed workflow. Replacement playback is not part of this experiment.

## Exact optional user-run experiment

1. Install the separate `artifacts/dx340-ui-research-0.1.0-debug.apk` on DX340 only if conducting this experiment. In Android Accessibility settings, choose **DX340 Apple Music UI research (read only)**, read its description and enable it only for this experiment. Do not enter Apple ID credentials during capture.
2. Open installed Apple Music. Navigate manually to Library → Downloaded, then albums, playlists and songs. Keep the screen unlocked. No app identity impersonation is involved.
3. After each relevant screen, export the latest internal snapshot from the development computer:

   ```powershell
   & "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" -s d shell run-as org.dx340.remote.probe cat files/apple-ui-probe.json > artifacts/apple-ui-downloaded.json
   ```

4. Preserve separate snapshots for downloaded albums, an album's song list, downloaded playlists, and a selected song outside the previous queue. The snapshot records structured accessible labels only. Inspect it for stable labels and IDs; capturing labels is not a playback test.
5. Disable the research service after capture. Record the exact Apple Music version, language, display state and whether each required control is exposed. Screen-off reports are blocked deliberately.
6. Only if the tree supports reliable semantic navigation, implement a separate reviewed click experiment. Then verify 20 downloaded tracks across five albums and two playlists with Wi-Fi/cellular off and actual audio. ADB tapping or selecting through Apple Music manually does not establish remote UI-assisted browsing.

If accessible structure is insufficient or screen-on/unlock requirements are unacceptable, full offline Apple Music browsing remains unresolved. Keep supported session controls available without claiming the original complete scope.
