# Distribution and wider player compatibility

Owner direction31 October2026: make connection simpler now, aim to support more DAPs next, and consider Google Play for personal installation/updates. These are implementation directions and a distribution feasibility question; no store account creation, payment or upload is authorized by that question.

## Google Play for personal use

Verified against official Google guidance31 October2026. Internal testing can distribute through Play to an email list including the owner's Google account, without a public production launch; testers normally use an opt-in link. It supports up to100 testers. The12-testers/14-continuous-days rule for personal accounts created after13 November2023 is a production-access requirement, not an internal-track prerequisite. A Play Console account has aUS$25 one-time registration fee and identity/device verification requirements. The actual owner's account status has not been inspected.

References: [internal testing](https://support.google.com/googleplay/android-developer/answer/9845334?hl=en), [personal-account production requirements](https://support.google.com/googleplay/android-developer/answer/14151465?hl=en), [developer-account setup](https://support.google.com/googleplay/android-developer/answer/6112435?hl=en).

Current packages are development builds. The shared phone's release variant explicitly uses the existing external debug signing identity; no production-signed App Bundle has been built. Before upload, prepare a durable release/upload key outside Git, Play App Signing, version codes and the required listing/declarations. Plan migration from the installed development signature before changing signing identities; never uninstall the owner's app or lose controller/source preferences to bypass a mismatch. A successful APK build does not establish Play eligibility. [Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756?hl=en)

Phone Remote and DAP Companion are separate applications. The Companion's optional source-screen browsing uses AccessibilityService and needs the applicable declaration, disclosure/consent and policy assessment. Personal use does not establish approval or a policy exemption. No claim that the app is a disability accessibility tool is intended. The Remote does not request that source-app access. [AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en)

The DX340's installed Play Store package was observed; device certification, account availability and Play installation of these apps have not been tested. Direct signed APK development updates remain available while store preparation is pending. Everyday controls remain Bluetooth-only, regardless of installation channel.

## Player compatibility

The new setup path discovers the project's Companion service rather than matching the DX340 product name. The last approved player's native OS identity is remembered; a different player requires an explicit choice. Source capabilities remain reported separately from connection readiness. These changes are a foundation for wider support, not proof that other DAPs work.

The current Android companion and remote require Android12/API31 or newer. DX340 Android13 is the physically tested DAP; no other make/model has been verified. Older Android DAPs need a scoped compatibility investigation, and closed/non-Android operating systems need their own supported integration. Do not promise "most DAPs" or lower minSdk without testing permission, BLE hosting and lifecycle differences.

For each future player profile record model/OS/firmware, installability, BLE peripheral support, background/sleep, output routing/volume, storage and each installed source app's transport/browse/play-selected-item evidence. Keep vendor/source adapters separate from transport and shared phone UI. Preserve existing engines and report unsupported capabilities; no firmware/root/private download extraction or identity spoofing.
