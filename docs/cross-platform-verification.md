# Android and iOS launch verification

28 October 2026. The user chose a shared Android/iPhone interface after the Kotlin/Flutter comparison. Flutter now supplies one phone UI, with native Kotlin Android Bluetooth/service and Swift CoreBluetooth/restoration retained. The Kotlin DX340 companion and installed source apps are unchanged by this choice.

| Item | Evidence and limits |
|---|---|
| Shared Flutter Android phone app0.2.0+2 | VERIFIED Windows release-mode development-signed build, analysis,9 state/widget tests and release lint. Installed/opens on DX340 Android13; Player/Library/Queue/Setup and recovery captures inspected. This is not a separate-phone BLE test |
| Shared Flutter iPhone app | VERIFIED unsigned build and simulator launch on hosted macOS26.6.2/Xcode26.6, iPhone Air/iOS26.5 at b1fdf4c/24d0484. Corrected final six-image capture matrix passed in CI40713390473 at5f31f5d. Physical signing/installation NOT TESTED |
| Native reference clients | Retained. Native Android build/lint/29JVM tests pass after controller-core extraction. SwiftUI reference CI40710376253 succeeded with3 shared Swift protocol tests, build/launch/captures |
| Physical phones | Only DX340 USB connected. Separate Android controller and actual iPhone/Mac/signing inventory are unavailable. BLE/stability acceptance remains NOT TESTED |
| Toolchain | Pinned Flutter3.41.4/Dart3.11.1, Gradle8.14+checksum, AGP8.11.1, Kotlin2.1.10, JDK21, Android SDK36; phone minimum API31 / iOS17 proposed |

Flutter produces separate Android and iOS packages from shared Dart UI. An APK cannot run on iOS. Flutter still needs Mac/Xcode/signing for physical iPhone deployment. [Official iOS requirements](https://docs.flutter.dev/deployment/ios).

## Repeatable checks and actual phone steps

1. Windows: `./scripts/build-phone.ps1` runs dependency-lock verification, analysis, meaningful state/widget tests, release-mode APK build and Android release lint. It keeps generated Windows properties normalization/Java workaround local to development. Output artifacts/dx340-phone-0.2.0-development.apk uses the existing development key outside Git.
2. `./scripts/verify-phone-launch.ps1 -Serial <serial>` installs/opens, checks process/inventory and captures the shared Player. DX340 currently yields null UIAutomator roots for both Flutter and native companion; automation failure is recorded separately from Activity/process/screenshots. TalkBack is NOT TESTED. Actual tab and OpenSetup development taps were separately observed from screenshots.
3. Mac: install pinned Flutter3.41.4 and suitable Xcode, run `flutter pub get --enforce-lockfile` in phone/, open phone/ios/Runner.xcworkspace, select signing team/actual iPhone and build/run. Record exact hardware/OS/build. For a simulator, run `sh scripts/build-phone-ios.sh <iPhone-UDID>` from the root. The public Mac CI performs unsigned simulator checks only.
4. Both physical phones: follow installation.md and phone-reliability-runbook.md for authenticated pairing/local approval, Wi-Fi/cellular-off control, full browsing/selection, sleep/background, range loss, radio toggles, revocation and upgrade. No source-only, compile, screenshot or simulated result passes this matrix.

Full browsing, Mango preservation and Bluetooth-only everyday operation remain requirements. Optional Mango folder integration remains independent host work, authorized but not implemented by the phone migration. Shared-phone test history: test-results/2026-10-28-shared-phone.md. Architecture/parity plan: shared-phone-migration.md.
