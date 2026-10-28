# Original-key upgrades and DX340 source checks

29 October 2026, Asia/Shanghai. **VERIFIED within the scopes below; full project incomplete.**

Device: ADB d, DX340 Android13/API33, build DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango com.ibasso.music4.1.37/code240; Apple com.apple.android.music6.5.3/code1599. Shared phone0.2.0+2 installed on DX340 only. A separate Android phone, physical iPhone, usable Mac/Xcode and Watch are unavailable. Headphones were connected for the three listening samples; current output/amp details were not reconfirmed.

## Signing and upgrades

The user supplied C:\Users\user\Downloads\dx340-windows-debug-signing-2026-10-29.zip. Inventory contained debug.keystore and README.txt; only the exact key entry was extracted. No archive instructions were executed. C:\Users\user.android was not writable, so the private key is at **C:\Users\user\.android\dx340-original-debug.keystore**, outside Git. The ZIP and key are not repository artifacts.

Public certificate SHA256: **24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d**, matching both installed apps. Existing C:\Users\user\.android\debug.keystore remains unchanged with its different ed3ffa7b... certificate. No user/global signing override was set. Builds used process-local DX340_DEBUG_KEYSTORE and standard debug alias/password settings. Original-key transfer is resolved.

Commands, from the repository root with process-local JBR21.0.7, SDK36 and pinned Flutter3.41.4:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = 'C:\Users\user\AppData\Local\Android\Sdk'
$env:PATH = "$PWD\work\flutter\bin;$env:PATH"
$previousDx340Key = $env:DX340_DEBUG_KEYSTORE
try {
    $env:DX340_DEBUG_KEYSTORE = 'C:\Users\user\.android\dx340-original-debug.keystore'
    ./scripts/build-android.ps1
    ./scripts/build-phone.ps1
} finally { $env:DX340_DEBUG_KEYSTORE = $previousDx340Key }
./scripts/verify-android-signing.ps1 -Serial d -Apk artifacts/dx340-companion-0.1.0-debug.apk -Package org.dx340.remote.host
./scripts/verify-android-signing.ps1 -Serial d -Apk artifacts/dx340-phone-0.2.0-development.apk
adb -s d install -r artifacts/dx340-companion-0.1.0-debug.apk
adb -s d install -r artifacts/dx340-phone-0.2.0-development.apk
```

Both preflights and installations passed. No uninstall/data reset occurred. Companion controllers.xml/source-verification.xml/apple-ui.xml/mango-ui.xml stayed byte-for-byte unchanged across upgrades and silent tests. Both optional Accessibility services rebound; binding/crashed lists were empty. Host advertised GATT successfully. Installed APKs were pulled and their bytes matched generated files:

| Current installed artifact | SHA256 |
|---|---|
| Companion0.1.0 debug, final parser/debug-queue build | cf0623f6b98620716678cadc0cea40d0192875e4fd148fec4ac7767aa062d5f0 |
| Shared phone0.2.0+2 development release | bfdba843bfcd82caa4ec40c5000eeccb2050389b6eb4aae820450327e849fcd0 |

Evidence under ignored artifacts/: restored-original-key-verification.json, original-key-default-isolation.json, original-key-{host,phone}-preflight.json, original-key-final-host-preflight.json, original-key-final-preference-check.json, original-key-final-installed-host-verification.json and original-key-installed-phone-verification.json. Earlier companion hashes in this turn are superseded by the final row above.

## Software and launch checks

VERIFIED: native Android build/lint/67 aggregate JVM tests (three new Apple parser tests); Flutter analysis/22 tests/release APK/release lint with original key. Logs: original-key-final-android-build.txt and original-key-phone-build.txt. Native reference/research/instrumentation APKs compiled; source checks below used the debug receiver rather than instrumentation.

Phone launch on the same DX340 is only a rendering check. A launch while dozing/locked produced a black frame; waking alone did not repair that instance. A fresh cold launch while already awake/unlocked rendered the disconnected Player correctly, visually inspected in original-key-phone-fresh.png. No renderer fix was made. Real-phone locked launch/lifecycle remains NOT TESTED. Current Swift code has not compiled here: the Apple tool failed with spawn xcrun ENOENT. Earlier iOS CI pertains to older source.

## Actual silent source and access checks

All tests in this section kept both music sources paused and volume unchanged. They call actual adapters through the debug-only DUMP-protected receiver, without a BLE central or simulated source. In-process capability tests use isolated verification preferences and simulated UI readiness; they are explicitly component evidence.

| Case | Result |
|---|---|
| Paused Mango timing | VERIFIED: position214493 unchanged over2seconds; public raw paused speed1 normalized to0 |
| Native Mango root/page/IDs | VERIFIED:6 root rows,25-row page, repeat IDs/revision stable, invalid revision rejected |
| Full source-exposed All Music enumeration | VERIFIED:500 unique IDs,20 nonempty pages of25 plus empty terminal page. This is not a physical file count or proof Mango exposes every file |
| Native category ID separation | VERIFIED: Album18/Artist11/Genre3 rows, no ID collisions against the500 songs |
| Isolated capability check | VERIFIED component: real native source, simulated ready UI, native denial/disabled rows/no UI playback dispatch; production verification preferences unchanged |
| Assisted Mango browsing | VERIFIED:2 storage choices,9 Internal Storage rows; old UI token rejected as STALE_LIBRARY |
| Device sleep | VERIFIED: assisted browse UI_SCREEN_UNLOCK_REQUIRED/canBrowseFoldersfalse; native browse still25 rows/canBrowsetrue |
| Notification-access loss | VERIFIED: revoked only own SessionListener using cmd notification disallow_listener; all capabilities disabled, access error reported, epoch invalidated, browse PERMISSION_REVOKED |
| Access recovery | VERIFIED: original listener set restored exactly in finally; source/listener registration recovered without host restart, old revision STALE_LIBRARY, old uncached ID EXPIRED_ITEM, fresh-root browse25 rows. Host PID unchanged during this test |
| Apple category first pages | VERIFIED: Downloaded root4 categories; Albums2 rows, Playlists5, Songs6. These are first-page counts, not totals |

Summaries: artifacts/original-key-probes/{summary,assisted-summary,permission-summary,library-summary}.json. The library summary records the original Apple Songs failure before the fix below; do not read its playableRows0 as the current result. A first scratch permission test incorrectly expected an old uncached ID to work under a new epoch; actual EXPIRED_ITEM was safe behavior. The corrected test explicitly checks rejection and fetches a fresh root, then passes.

## Apple Songs parser correction

FAILED before correction: all6 Downloaded → Songs rows were disabled. Actual own-service snapshot showed same-row nested action_button with exact description Downloaded Song; the existing parser recognized only track_action_button, used by other observed track layouts.

The parser accepts both observed resource IDs only with the exact semantic badge inside the same clickable row. It does not infer a download from the surrounding screen, another row, generic controls, unknown IDs or near-match descriptions. Revision identity includes full bounded row text beyond the128-character title/64-character subtitle display limits. Tests cover both badge IDs, cloud-row isolation, misleading labels/IDs and long-title identity changes.

VERIFIED after installing the updated host:6 Songs rows selectable, zero browsable song rows, old pre-upgrade token rejected STALE_LIBRARY. Evidence: original-key-apple-song-snapshot.json, original-key-probes/apple-parser-summary.json and probes30–33. Initial root navigation during an unsettled Apple screen failed UI_ACTION_FAILED, then an explicit retry succeeded; a Mango foreground-transition attempt likewise failed UI_FOREGROUND_REQUIRED before explicit retry. These failures remain reliability follow-ups; no playback retry was added.

A debug-only CHECK_APPLE_QUEUE_BASELINE operation reads the complete public Apple MediaSession queue with a10000-item bound, records count/candidate presence and full rows. The product queue response is intentionally bounded at50; it cannot establish absence from a larger queue. This debug read ports the earlier instrumentation check into the existing DUMP-protected receiver, avoiding the device's historical instrumentation/Accessibility disruption. It neither selects nor plays a track.

For a fresh read on a matching debug build with playback-session access, run the following and verify that the returned operation/time/source are fresh. This changes the control target to Apple without starting playback; restore Mango with a GET_STATE probe afterward if that was the prior target.

```powershell
adb -s d shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation CHECK_APPLE_QUEUE_BASELINE --es source com.apple.android.music --es candidate A・RA・SHI
adb -s d shell run-as org.dx340.remote.host cat files/apple-ui-live-test.json
adb -s d shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es operation GET_STATE --es source com.ibasso.music
```

## Listening samples on this installed build

The user confirmed headphones connected and readiness for a few tests. Wi-Fi0 and Active default network:none were recorded; no source/volume/firmware change was used to obtain playback. Existing Apple Music and Mango remained the playback engines. Each row below was selected by its fresh own-adapter token, observed playing with the other source paused, and separately user-confirmed **Yes, the correct song is audible**. Follow-up snapshots still showed the requested title playing when each confirmation was recorded. These are source-local audible results, not BLE/quality/long-duration acceptance.

| Source / selection | Evidence |
|---|---|
| Apple Downloaded → Songs → A・RA・SHI, ARASHI | Absent from complete101-item previous queue; observed correct title/artist playing, Mango paused. User-confirmed audible. probes37–42 and apple-audio-summary.json |
| Mango Internal Storage / Music / Tayori / first-live folder →03. 風のたより.wav | Observed 風のたより/tayori playing, Apple paused. User-confirmed audible. probes43–52 and50-mango-audio-WAV-summary.json |
| Mango same artist / magic-tour folder →03. ゴースト.flac | Observed ゴースト/tayori playing, Apple paused. User-confirmed audible. probes53–58 and56-mango-audio-FLAC-summary.json |

Final state: both sources paused; Mango selected as control target. No volume command was sent. Offline evidence: original-key-listen-{wifi,connectivity}.txt and original-key-final-{wifi,connectivity}.txt.

## Remaining acceptance

NOT TESTED: separate Android/iPhone BLE/authentication/revocation, background/range/30-minute connection, current iOS compilation/signing, full20-track/source/format matrices,5 albums/2 Apple playlists, Mango playlist behavior, physical SD/rescan/duplicate identity, measured latency/battery/walk/audio quality and complete fresh-install/upgrade matrix. Upward/output/phone volume remains disabled. Optional assisted browsing still needs screen-on/unlocked and exact source-version revalidation. Watch remains gated on stable phone acceptance. Goal remains active and incomplete; key transfer is no longer a blocker.
