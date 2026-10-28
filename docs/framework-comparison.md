# Phone framework choice

28 October 2026. Following the comparison, the user explicitly selected a shared phone interface/UI with stable use and connection. Flutter is now the chosen phone presentation stack. The DX340 host stays Kotlin; Android Bluetooth/service and Swift CoreBluetooth/restoration stay native. The existing native phone apps remain reference builds during migration.

| Option | Shared code | Fit and tradeoff for DX340 Remote |
|---|---|---|
| Current Kotlin Android + SwiftUI iOS | Protocol specification/fixtures; separate client implementations | Direct native Bluetooth/lifecycle integration and actual system UI; two phone UIs to maintain. Both platform builds now have evidence |
| Kotlin Multiplatform, native UI | Shared business logic; Kotlin Android UI and SwiftUI iOS UI | Can gradually share protocol/state/cache logic while keeping system UI. Requires a new multiplatform module; existing Android/JVM APIs are not automatically portable |
| Kotlin + Compose Multiplatform | Shared Kotlin logic and UI where appropriate | Android/iOS shared UI is a supported option; platform-specific Bluetooth/lifecycle integration and platform appearance work remain |
| Flutter phone clients | Shared Dart UI and application logic | A direct fit if one shared phone interface is the priority. Bluetooth may use a suitable plugin or native Kotlin/Swift platform channels; capabilities/background/restoration must be verified, not assumed |

Selected implementation: `phone/` contains shared Dart screens and presentation state. Android reuses `android/controller-core` (extracted from the native remote); iOS compiles the same `apple/BLEClient/RemoteConnection.swift` and protocol framing as the native reference app. Own versioned platform channels connect presentation to those native components. Material Android and Cupertino iPhone chrome follow the user's system-appearance preference. No third-party Bluetooth plugin or network fallback is introduced.

This is an engineering recommendation, not a measured claim that one framework has better physical Bluetooth reliability. Neither choice removes iOS signing, real phone Bluetooth tests or Mango/Apple Music integration work on DX340. The current iOS simulator build passes, so a framework rewrite is not required simply to open the app on iOS. A future Watch target remains separate work under the plan.

Primary references checked:

- [Kotlin Multiplatform overview](https://kotlinlang.org/docs/multiplatform/kmp-overview.html): Android/iOS code sharing, native UI or Compose options and incremental adoption.
- [Kotlin Multiplatform](https://kotlinlang.org/multiplatform/): Compose Multiplatform stable support for Android/iOS/desktop.
- [Flutter platform channels](https://docs.flutter.dev/platform-integration/platform-channels): integration with platform-specific code.
- [Flutter platform adaptations](https://docs.flutter.dev/ui/adaptive-responsive/platform-adaptations): automatic behaviors versus explicit Material/Cupertino design choices.
- [Flutter iOS deployment](https://docs.flutter.dev/deployment/ios): Mac/Xcode and signing requirements.
