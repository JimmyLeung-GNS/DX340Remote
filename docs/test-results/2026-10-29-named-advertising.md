# Named BLE discovery on the resumed Windows host

29 October2026, Hong Kong. Actual DX340 Android13/API33, Mango4.1.37/code240, Apple6.5.3/code1599; Windows11/build26300, Intel Bluetooth radio, Python3.14/Bleak3.0.2. This is a development PC central, not Android/iPhone acceptance.

## Finding and change

The original host advertised only the service UUID. Fresh Windows observations had no scanner/advertisement local name. The script-requested native authenticated pairing returned FAILED/protectionNONE, including a second attempt with a freshly verified60second companion pairing window. No bond, weaker fallback or companion approval was created. Absence of a name explains why the suggested label was not reliable; it does not establish the cause of PairAsync failure.

The host now keeps its original service UUID in the advertisement and includes the existing Android Bluetooth adapter name in a separate scan response. It does not rename the adapter or alter encrypted-MITM characteristic/descriptor permissions, local approval, authorization leases or playback routing. The scan-response name is included only when its UTF-8 payload fits the29-byte name budget; a missing/oversized name retains UUID-based advertising rather than breaking discovery. Device display names remain non-authoritative identities.

Primary API: [Android BluetoothLeAdvertiser scan-response overload](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeAdvertiser). Windows request semantics: [DeviceInformationPairing.PairAsync](https://learn.microsoft.com/en-us/uwp/api/windows.devices.enumeration.deviceinformationpairing.pairasync). API availability is separate from actual observations below.

## Evidence

| Result | Case | Scope |
|---|---|---|
| VERIFIED software | Full Android build/lint86 JVM tests | resume-named-advertising-build.txt,45seconds; no new wrapper-only test |
| VERIFIED signing/update | Original certificate matches installed host; install-r succeeds | Installed prior40114ff8...; candidate f0812c2f349e84cad7ff3769d408dc5a6baf821769778754b8f9e90ceb7a368e |
| VERIFIED state retention | All four companion preference file hashes unchanged | apple-ui, controllers, mango-ui, source-verification; no app-data reset |
| VERIFIED real radio | Fresh Windows scan has one matching service, local and scanner name equal DX340 | resumed-named-advertisement.json; this is actual over-air metadata, not a mock |
| VERIFIED unchanged local output state | Public music level37, musicActive=false after update | Both source playback remained paused; no playback/volume command sent |
| VERIFIED service state | Advertising callback active, Accessibility crashed set empty | Fresh diagnostic/binding read; no source recovery guarantee inferred |
| NOT VERIFIED | Authenticated OS bond/local application approval | Latest Windows observation remains paired=false/protectionNONE; operator's Settings pairing flow pending |
| NOT TESTED | Either physical phone / full security/background/range/audio matrix | PC discovery and compilation are insufficient |

The user was told to choose **DX340** in native Windows Bluetooth Settings, reopen the companion pairing window and compare matching system codes. Neither this code nor the diagnostic script accepts a consent dialog. Product phone volume is still disabled on the current recovery branch; the separate older volume branch remains preserved.

Ignored artifacts/: resume-native-pairing-{output,retry-output}.txt, windows-ble/resumed-first-native-pairing-result.json, resumed-named-advertisement.json, resume-advertising-preferences-{before,after}.txt, resume-advertising-state-{before,after}.json, android-upgrade-signing.json and resume-named-advertising-build.txt. Raw addresses and unrelated notifications are not included in this tracked report.
