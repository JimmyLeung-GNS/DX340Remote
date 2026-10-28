# DX340 Remote shared phone app

Flutter 3.41.4 / Dart 3.11.1. Shared Player, Library, Queue and Setup UI; native Kotlin Android Bluetooth/service and Swift CoreBluetooth/restoration underneath. No third-party BLE plugin or runtime network path.

From repository root on Windows:

```powershell
./scripts/build-phone.ps1
```

On Mac with the pinned Flutter SDK and Xcode:

```sh
sh scripts/build-phone-ios.sh <iPhone-simulator-UDID>
```

For an actual iPhone, run `flutter pub get --enforce-lockfile` here, open `ios/Runner.xcworkspace`, choose a signing team and connected iPhone, then build/run. Record real hardware/OS/build and run the Bluetooth acceptance steps in ../docs/installation.md. A simulator cannot verify the BLE radio.

Android output: ../artifacts/dx340-phone-0.2.0-development.apk (release-mode Flutter, existing development signing identity; not production signing). The controller application ID is preserved so install -r updates the native reference app; do not run the older versionCode1 over build2 as a routine rollback.

Reference clients remain in ../android/remote-app and ../apple/DXRemote. See ../docs/shared-phone-migration.md for parity requirements and bridge v1. Source-only tests and synthetic/widget tests do not establish phone Bluetooth or audio quality.
