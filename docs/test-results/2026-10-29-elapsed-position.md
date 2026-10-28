# Elapsed-position continuation and upgrade preflight

29 October 2026, Asia/Shanghai. Source baseline400ec34 plus the current local working-tree changes. Full project goal remains active and incomplete.

Superseded for current generated packages and final test totals by 2026-10-29-browse-access-recovery.md. The hashes below identify the earlier elapsed-position checkpoint; installed baseline identity and physical limits remain applicable.

## Implementation

The host now normalizes public PlaybackState position using speed and the source's monotonic update time at snapshot creation. Unknown positions remain-1; non-playing, invalid-clock, unknown-duration and invalid-rate cases do not advance. Estimates clamp at track duration. Optional protocol-v1 playbackSpeed keeps old clients compatible.

Both native phone bridges attach a sample UUID and age measured on that phone's monotonic clock. Disk cache loading does not create a live sample. Status, library and artwork events reuse the sample and cannot restart the local clock. Flutter presents a labelled estimate with a one-second text timer only on the visible Player. It freezes immediately on connection loss/backgrounding, waits for a fresh host sample after foreground return, and stops ticking at duration without inventing the next track. The existing foreground resume requests current state; elapsed display adds no periodic BLE requests. The Back10seconds action, where source capabilities permit it, uses the current displayed position rather than the old snapshot.

## Results

| Status | Check | Evidence / limits |
|---|---|---|
| VERIFIED | Native Android build, instrumentation APK compilation and lint | scripts/build-android.ps1, artifacts/progress-android-build.txt; host, native controller and UI research packages generated |
| VERIFIED | 45 JVM tests, zero failures/errors | artifacts/progress-jvm-tests.json; includes5 new position tests for age/speed, non-advancing state, unknown clocks/position, invalid rates and duration/overflow bounds |
| VERIFIED | Pinned Flutter analysis and20 tests | artifacts/progress-flutter-analyze.txt and progress-flutter-tests.txt;8 new position tests plus2 lifecycle widget tests in Android/iPhone layouts, alongside10 existing tests |
| VERIFIED | Android release-mode APK and release lint | scripts/build-phone.ps1; artifacts/progress-phone-build.txt, flutter-android-build.txt and flutter-android-lint.txt |
| VERIFIED | Generated phone APK permissions inspected | artifacts/progress-phone-permissions.txt; no INTERNET, ACCESS_WIFI_STATE or CHANGE_WIFI_STATE permissions |
| VERIFIED | Read-only source inventory on DX340 | Android13/API33/build eng.iBasso.20260812.181952; Mango4.1.37/code240 and Apple Music6.5.3/code1599 still installed. Raw media-session dump reports both paused; Mango reports speed1 despite paused state, which must not animate. This is input inspection, not execution of the new host code |
| VERIFIED | Signing check positive case | Comparing the pulled installed phone APK with that same installed identity passes; artifacts/progress-signing-same.json/txt |
| VERIFIED | Signing mismatch stops before installation | Actual verify-phone-launch invocation rejects the new APK; artifacts/progress-signing-mismatch.json and progress-signing-blocked-launch.txt. No install/uninstall/reset occurs; companion PID15847 and controller lastUpdateTime remain unchanged |
| BLOCKED | In-place Android runtime upgrade/launch of this build | Original installed development signing key is unavailable in this checkout; newly built APK uses a different certificate |
| NOT TESTED | New Swift sample-age code compiled/launched on Mac | Windows environment has no Xcode. Prior simulator/CI passes predate these Swift edits |
| NOT TESTED | Physical phone/BLE timing, seek, track/source transitions and recovery | User confirms only DX340 available. Widget/JVM results do not establish radio behavior, audible playback, elapsed accuracy or latency |

Initial analysis caught a redundant numeric type check; corrected before final verification. Lifecycle widget tests exposed background frames stopping before parent rebuild; the timer now listens to model changes directly and cancels synchronously. Final20 tests pass. Earlier failed widget output is retained separately as artifacts/progress-flutter-tests-early-failure.txt; use the final report filenames above.

## Packages and signing

| Package / baseline | SHA256 |
|---|---|
| Newly built companion, not installed | 268518779e418399685682a3f4b41dff63df80d9c2957a1259d0381c31f8189b |
| Newly built shared phone, not installed | f13564c5b267eddc4048111575bafc846cce60dd7979f2951b7ec354acbc9c29 |
| Installed companion pulled read-only | 51cf92b5c1850525a3803237b1c67aa0d986e8bd2d8b450c7890f9ba03cc1e94 |
| Installed shared phone pulled read-only | 4c1937dbe4554008e085478d85f49b1ab5d11ea07bb699e22f3adebd91f94405 |

The currently installed phone certificate SHA256 is `24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d`; the new development package certificate is `ed3ffa7b199dc1ecf01046dec0d3f88d4ca58915503adc1ab2bcec31c2cb1a64`. Certificate fingerprints are public identity data, not signing keys. Restore the original key securely outside Git and rebuild to preserve approval/cache. Do not uninstall to bypass this mismatch. The script conservatively compares certificate sets and does not implement signing-key rotation.

Installed APK hashes differ from the earlier environment's status entries. This report supersedes those entries for installed-package identity in this session; it does not assign a source commit to an APK based on versionName alone. Prior raw logs/artifacts were absent in this checkout and were not regenerated as historical proof.

## Environment and next checks

Official Flutter3.41.4/full revisionff37bef603469fb030f2b72995ab929ccfc227f0 and Dart3.11.1 were restored in ignored work/flutter. Global Flutter3.41.7 was preserved. Builds used process-local JAVA_HOME pointing to Android Studio JBR21.0.7 and the existing SDK36/build-tools36.1.0. No global toolchain, playback, volume, accessibility setting or network configuration was changed. No publication occurred.

After signing is restored, install matching host/phone builds and follow phone-reliability-runbook.md on both actual controllers. Check pause with a nonzero reported speed, resume, seek, track/source/epoch changes, cached artwork/status events, track end, disconnected cached time and lock/background/foreground recovery. Compare displayed estimates with source state separately from listening evidence. Run a fresh Mac build before assigning any iOS runtime result.

Primary API references checked for implementation: [Android PlaybackState](https://developer.android.com/reference/android/media/session/PlaybackState), [Dart Stopwatch](https://api.dart.dev/dart-core/Stopwatch-class.html), [Flutter lifecycle observer](https://api.flutter.dev/flutter/widgets/WidgetsBindingObserver/didChangeAppLifecycleState.html). API documentation does not pass device acceptance.
