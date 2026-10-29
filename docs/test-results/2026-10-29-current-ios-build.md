# Current recovery checkpoint iOS build/launch

29 October2026, Hong Kong. Source6e838f5 on codex/source-recovery-checkpoint includes the newer elapsed, queue pagination and source/controller lifetime changes. Hosted macOS26.6.2/25G83, Xcode26.6/17F113; unsigned iPhone simulator only. No personal signing account or physical phone was used.

- **VERIFIED workflow:** shared Flutter run40840955016 SUCCESS. Analysis,32 Dart tests, Runner.app build/install/launch, six disconnected light/dark/large-text capture readiness checks and10 matcher regressions passed.
- **VERIFIED workflow:** native reference run40840955084 SUCCESS. Three Swift protocol tests, application build/install/launch, six disconnected capture readiness checks and10 matcher regressions passed.
- The matcher accepts only exact control text or observed bullet/middle-dot prefixes for Volume up. Navigation destinations still require exact title and tab evidence, and all four tabs must be present. Arbitrary prefixes, missing tabs and splash-only frames are rejected. This fixes the separate old volume branch's observed OCR condition without claiming its complete fixture workflow was rerun here.
- **NOT TESTED:** physical signing/installation, real iPhone CoreBluetooth, background/range/security/audio, connected populated library/queue UI and user interaction. Captures/readiness are rendering evidence only; whole UI acceptance is not inferred.

Artifacts12883137487/12883600333 were downloaded under ignored artifacts/resume-current-flutter-ios/ and resume-current-native-ios/. Current Android-only named-advertising change7c13007 does not alter this Swift/Dart application source. Native/Flutter unit totals describe different suites; the10 capture helper cases are separate.
