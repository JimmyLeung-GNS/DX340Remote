# iOS build and simulator launch

28 October 2026. Standard GitHub-hosted Mac runner for the authorized public repository. macOS 26.6.2/build 25G83, Xcode 26.6/build 17F113, iPhone Air simulator/iOS 26.5. App minimum deployment target remains iOS 17; the reported physical iOS 27.2 device is not verified by this test.

| Evidence | Result |
|---|---|
| Shared Swift protocol tests | VERIFIED: 3 tests, 0 failures, shared Kotlin/Swift framing fixtures included |
| Native app build | VERIFIED unsigned simulator BUILD SUCCEEDED |
| Native launch | VERIFIED simctl returned PIDs and real native screens were captured |
| Final capture run | SUCCESS: [40699505189](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40699505189), source 412b87a; app UI unchanged from 77f828c |
| Capture set | Player light, Library light, Queue light, Setup light, Player dark, largest Dynamic Type Player dark; artifacts/apple-ci-40699505189/ |
| Independent UI verdict | ship, limited to 3 scored corrections: largest-text overlap resolved; empty metadata/orphan Source removed; disconnected Library/Queue recovery leads to Open Setup |
| Physical iPhone signing/open/BLE | NOT TESTED; no signed iPhone package or physical iPhone connection |
| Connected screens, real tab taps, scroll interaction, VoiceOver | NOT TESTED by these captures |

The captures select actual native disconnected screens via Debug-only launch arguments; they do not inject a simulated source or connection. Capture readiness uses native Vision text recognition to wait for the expected heading and all four tab labels. It is an evidence gate, not a navigation or Bluetooth test. Normal light/content size are reset before capture, and large-text Player is freshly launched after the size change.

Earlier results remain scoped accurately:

- Run 40691819981/source 53d26ec built and opened Player, but its bottom status banner covered tab labels and the compiler reported actor-conformance warnings.
- Run 40693857668/source a177a1c fixed normal-size tab overlap and compiler warnings, but Library/Queue/Setup captures still showed Player because the generated Debug configuration omitted DEBUG. Those named-destination captures are invalid.
- Run 40695585066/source b2318ec produced the correct destinations but revealed persistent largest-text overlap. The reviewer requested three material fixes.
- Run 40696984022/source 77f828c built the fixes, but its standard dark capture was a blank launch frame. The reviewer required recapture rather than accepting it.
- Capture-helper run 40699026514/source f4668eb was superseded/cancelled after checking Vision's documented default text-height threshold, which would omit small native tab labels. The corrected helper explicitly uses 0.005 and the final run succeeded.

Hosted simulator boot logs reported Data Migration Failed even though app installation/launch subsequently succeeded. The final readiness log also waited out a system Apple Intelligence notification. Preserve these runner caveats; no general simulator-health or physical-device reliability claim follows.

CoreBluetooth delegate conformance now uses @preconcurrency with the explicitly configured main queue; Xcode's actor-crossing warnings are gone in the verified build. Remaining warnings about destination architectures and missing AppIntents metadata are recorded; no unused framework was added to suppress them.

Actual evidence: xcode-version.txt, macos-version.txt, swift-tests.txt, apple-build.txt, apple-simulator-launch.txt, apple-capture-readiness.txt and six PNGs under artifacts/apple-ci-40699505189/. Temporary GitHub artifacts have a 3-day retention; local copies are retained outside Git. The UI verdict is recorded in DESIGN.md and its sidecar without whole-product approval.
