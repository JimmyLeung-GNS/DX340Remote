# Phone refresh guidance and background observation

31 October2026. Application source **5b8175d0f33b88f105eb81591dcd9df3c824b98c**; extended read-only observer source **6385c520f7fc49292fe0866b2e41929aab4f4b41**. Full goal ACTIVE/incomplete.

## Verified implementation and builds

Android and Swift native models now share12 transition fixtures for library/queue invalidation. A changed source, source epoch or browse mode clears both contexts and displays **“Library changed. Browse again or refresh Queue.”** Assisted-readiness changes clear only affected assisted pages, preserving native Mango pages/queues. Initial authorization retains its connection message; error replies retain their specific recovery text. The change adds no automatic browsing, playback replay or wire fields.

The shared phone release APK is **2dce4f99b502d31529dc944e504f441313ae46e05f7903c6ef40d673768150e0**, source5b8175d,0.2.0/code2. It upgraded the actual Pixel9a Android16/API36 CP1A.260405.005 in place, preserving original signer24f48aa3…. Installed bytes/signature were pulled and verified. The companion remains **58314b84cd45447fd866c1ea9a48922c39edd9584f14100c47cd3cb8c91ee97e**, sourcecf3bccc, on DX340 Android13/API33/Mango4.1.37/code240/Apple6.5.3/code1599. The unchanged111… controller and earlier6c… companion audio evidence retain their original scope.

| Check | Result |
|---|---|
| Android software | VERIFIED controller assembly/lint and160 aggregate protocol/library/source JVM tests, zero failures/errors;12 shared transition cases in one new JVM test |
| Shared Android | VERIFIED pinned Flutter analysis/46 tests/release/lint; final native logging compile/package/lint after the documented lock cleanup. No new Dart UI code |
| Physical Pixel | VERIFIED saved-grant reconnect/metadata; actual native album rows clear after an explicitly synthetic storage boundary, display the new guidance, and queue clearing/reload returns1–23 of23. Initial authorization still displays Connected to DX340. Inspected light/default-font screenshot only; no full accessibility/visual review |
| Assisted transition | Software fixtures pass. Physical assisted-message check NOT TESTED: Mango optional UI preference was observed off and the expected folder row was absent. Automation stopped at that missing action. Current setting retained; Apple optional mode remains on. The source of that preference change is unconfirmed |
| Native Apple CI | VERIFIED [41087921312](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41087921312), source5b8175d:Swift10 tests/build/launch/captures and12 capture-matcher cases. Artifact12992609012 SHA256d6fa604cf11585e9135528a89362e459ba25eecdaf4b90f0322e2218847e5a73 |
| Shared Apple CI | VERIFIED [41087921694](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41087921694), source5b8175d:Flutter46/analyze/builds/captures/ordinary-app restoration. Artifact12993161393 SHA2561d39f596037cad9655ab4c7c4d5cc7714586a6aeac3ca816117e379e176492b8. ZIP hashes verified/extracted safely; restored ordinary screenshot inspected |

Both hosted runs used macOS26.6.2/build25G83, Xcode26.6/build17F113 and selected iPhone Air/iOS26.5 simulator. They do not prove signing, reported iOS27.2 compatibility or physical iPhone Bluetooth/audio.

## Failed attempts and corrections

Final shared packaging initially failed twice because Windows held generated classes.dex during R8 minification. Read-only Restart Manager inspection identified an idle Gradle daemon; its daemon log bound it to this workspace's phone/android build. Only that identified idle daemon was stopped. The targeted release/lint rebuild then passed. No app data, SDK or unrelated process was reset. [Microsoft resource-holder API](https://learn.microsoft.com/en-us/windows/win32/api/restartmanager/nf-restartmanager-rmgetlist).

The first post-upgrade launch incorrectly targeted the retained native-reference Activity and returned Activity-not-found. The shared component **org.dx340.remote.controller/org.dx340.remote.dx340_phone.MainActivity** then returned Status:ok and actual Flutter semantics. This was a launch-command error, not a verified application crash.

The first background attempt on companion583…/phone111… stopped after **347.428seconds/12 samples**, before30minutes. Both processes/foreground services remained observed, but Mango changed from paused to playing; DX340 had also woken. The operator answered **“I’m not sure”** about controls being used. Cause remains unconfirmed; no application-defect or clean30-minute claim. A single local desired-state Pause restored both sources paused at37, with Mango reporting風のたより. That recovery pause is separate from the read-only observer, which sends no BLE/playback/volume commands. It is not a new audio confirmation.

## Repeat observation — INCOMPLETE at device-switch handoff

The owner agreed to leave devices/headphone controls untouched. A new run started **00:32:37 HKT** on583…/2dce…, with the Pixel app put in the background and DX340 sent Sleep. The observer uses public ADB diagnostics for app/service presence, source state, output route/level and bounded logs; it sends no Bluetooth commands. Android client logging records queued command **types only**, without IDs, metadata, arguments or credentials.

The run terminated with a device shell-read failure after **1779.414seconds/56 successful samples**, below the1800-second minimum. Last successful sample: **01:01:46 HKT, elapsed1749.339seconds**. All56 successful samples retained both processes/foreground services, both sources paused and the same output37/headset(4). The bounded client logs contain pre-run connection/read entries; no new entries were observed during the sampled interval. This is sampled evidence, not continuous radio proof. **No30-minute pass or fresh-after-return Bluetooth read is verified.**

DX340 woke during the repeat, with mStayOn=true observed; exact wake cause remains unconfirmed. USB power and stay-awake settings exclude continuous screen-off/deep sleep, unplugged behavior, battery/walking or full Gate A/T05 acceptance.

At01:05 HKT both devices were absent from `adb devices -l`. Therefore the planned foreground read, explicit Pixel Disconnect and Wi-Fi restoration **could not be performed**. Do not assume USB removal disconnected Bluetooth. Last observed state was both sources paused37/proof false, Mango selected, Apple UI mode on/Mango UI mode off and retained Pixel/JIMMYAH approvals. Pixel Wi-Fi was0 (original1); mobile_data/Bluetooth last1/1. These are last observations, not reverified final device state. On resume inspect both devices, use ordinary Setup → Disconnect if still connected, and restore Pixel Wi-Fi to its original enabled state when appropriate. Do not revoke grants or silently re-enable Mango UI mode.

Raw private evidence: artifacts/browse-guidance-{phone/,native-final-build.txt,shared-build.txt,shared-final-build.txt,shared-final-retry.txt,shared-after-lock-cleanup.txt,ci-native/,ci-shared/}; artifacts/android-background-hold/20261031-000133-e1ff0/; unexpected-playback/; and repeat20261031-003237-8ce35/. Unique serials/addresses/raw logs stay outside Git.

Next: inventory the destination host/devices and complete the cleanup above. A new owner-ready30-minute run needs fresh preconditions and a post-return read; do not extend this failed run or treat elapsed time as consent. Actual SD/rescan and full Android/iPhone/signing/audio/reliability/volume/handoff/Watch gates remain open. Add **pending UX-01** for improved Companion/shared phone UI and original app logos/platform icons; see [design brief](../ui-branding-plan.md). No design work started. Sanitized structured evidence is in the adjacent JSON; source and these reports are tracked, raw logs/APKs/keys are not.
