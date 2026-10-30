# Build, installation and first use

Latest30 October: original-key companion **6c390636fa8fb64c86f1153d93b0d459f4d623b3eea725bd97848fad7cfeb85a** and shared Android phone **14d08e149209467566ffc769829074acc7f70f8c9e6b9e1dc262e16478f804ee**, sourcead44610, are installed on DX340 for source/launch checks. The phone APK has no separate-phone BLE acceptance. Upgrade both native bridges to receive slower bulk artwork: framing permits10s idle/60s total progress and GET_ARTWORK alone gets75s for a reply; other control deadlines remain12s. Earlier clients can discard large replies at10s. Existing signing, permissions and Mac/iPhone setup steps below still apply. [Failure, fix and current scopes](test-results/2026-10-30-artwork-progress.md).

Current30 October installed companion7aee0f7eb54345d213c0969471f78ca0c252e3f1e61ae14cfe553569ee72bab6/source7b50383 retains the original signer. Debug PLAY_ITEM uses a unique reportTag and an asynchronous30s receipt; the updated Mango/Apple listening helpers poll that receipt without resending playback. Use current helpers, retain failed/unknown attempts and never infer audible success from FINISHED. Physical paused later-page preparation passes12.709s, so responsiveness and actual final-path audio remain pending. See [the final paused report](test-results/2026-10-30-mango-refresh-final-paused.md). Shared phone23c536e7... is unchanged and has no physical phone BLE acceptance.

This is a diagnostic beta, not an accepted full remote system. Mango stays the local playback engine. Product Android manifests request no INTERNET or Wi-Fi permissions. Flutter debug/profile tooling adds INTERNET for development; use the release-mode development package for everyday offline trials. Build/download/signing setup may use the development computer's internet; everyday app transport is BLE only.

## Shared Android/iPhone interface

The user selected Flutter shared phone UI with native Kotlin/Swift Bluetooth/lifecycle code. Keep the companion installed on DX340. Install the new controller on a separate phone for actual radio acceptance; installing it on DX340 proves launch only.

Windows: install pinned Flutter3.41.4, JDK21 and SDK36; run `./scripts/build-phone.ps1`. It runs analysis/tests and builds `artifacts/dx340-phone-0.2.0-development.apk` in release mode, signed with the existing development key outside Git. Use `adb -s <phone-serial> install -r artifacts/dx340-phone-0.2.0-development.apk`. Application ID is retained as org.dx340.remote.controller; build2 upgrades reference build1 without uninstalling. Keys, raw logs and packages remain ignored.

Mac: install the pinned Flutter3.41.4 SDK and Xcode, run `flutter pub get --enforce-lockfile` in phone/, then open phone/ios/Runner.xcworkspace. Choose your signing team and connected iPhone; build/run Runner. Bundle ID remains org.dx340.remote.ios. Record exact Mac/Xcode/iPhone model/OS/build. The reported iOS27.2 requires a compatible Xcode; hosted simulator evidence does not establish that compatibility or device signing. To verify a simulator, run `sh scripts/build-phone-ios.sh <UDID>` from the root.

Use Setup → allow Bluetooth/Nearby Devices → Find DX340. Open pairing on the companion; connect and accept both system prompts; approve the pending controller on DX340; retry authorization. Test Player/Library/Queue with Wi-Fi and controller cellular data off. Switching source does not start playback. Optional assisted source browsing needs DX340 awake/unlocked. Continue the separate-phone sleep/range/revocation matrix in compatibility-matrix.md; no framework promises physical stability.

Native Mango metadata pages may be reused briefly while browsing. Use Library refresh to request fresh source data; a selection always revalidates with Mango before playback. The optional screen-assisted modes still require live DX340 screen-on/unlocked validation.

While connected, Library and Queue retain a playback bar above the four destinations. Tap its track information to open Player; its separate Play/Pause control follows source capabilities and pending-command state. It disappears on disconnection. At large text the compact title truncates visually while its accessible label and full Player retain the metadata. Rendering and software checks do not replace physical phone Bluetooth/interaction acceptance.

The older native client instructions below remain as reference build/recovery documentation. Do not overwrite build2 with build1 as a routine update; Android version downgrade and signing mismatch require deliberate handling.

## Windows build

Verified here: PowerShell 7.6.5, Microsoft JDK 21.0.9, SDK platform 36, Gradle 8.14 (wrapper plus distribution SHA-256), AGP 8.11.1, Kotlin 2.1.10. Android minimum is API 31. The DX340 is API 33.

Create ignored `android/local.properties` with your SDK path, for example:

```properties
sdk.dir=C:/Users/JimmyLeung/AppData/Local/Android/Sdk
```

From the repository root:

```powershell
./scripts/build-android.ps1
```

The script builds both apps, the read-only source instrumentation APK and optional UI-research APK, runs protocol/cache tests and native lint, then copies APKs and SHA256SUMS.txt into artifacts/. It sets a process-local JAVA_TOOL_OPTIONS workaround for this Windows host's Java loopback/Unix-domain selector failure. It does not change system Java settings. Full logs are artifacts/android-build.txt and build-script-output.txt.

APKs use the development computer's standard Android debug signing key outside Git (`~/.android/debug.keystore`). Retain that identity for development updates. These are development-signed packages, not production signing or store releases. No signing keys are committed. A different signing identity needs a deliberate migration; uninstalling loses local approval/evidence/cache state.

Before upgrading from another development computer, run `./scripts/verify-android-signing.ps1 -Serial <serial> -Apk artifacts/dx340-phone-0.2.0-development.apk`. The shared-phone launch script runs this check automatically before installation. It verifies the candidate's package/signature and compares signing-certificate digests with the installed base APK, keeping a local report in artifacts/android-upgrade-signing.json. A mismatch stops the update. Restore the original development key securely outside Git; do not uninstall/reset the installed app to bypass the check. This conservative development-key comparison does not implement signing-key rotation.

To use an original standard Android debug keystore on another computer, keep its file outside the repository and set `DX340_DEBUG_KEYSTORE` for these builds. The shared Gradle script applies this path to the companion, reference controller, research/instrumentation packages and shared Flutter Android release/debug builds. It keeps Android's standard debug alias/password settings. A missing or relative path fails; it does not silently use another key. This does not alter the computer's default `~/.android/debug.keystore` or global environment.

```powershell
$previousDx340Key = $env:DX340_DEBUG_KEYSTORE
try {
    $env:DX340_DEBUG_KEYSTORE = 'C:\Users\user\.android\dx340-original-debug.keystore'
    ./scripts/build-android.ps1
    ./scripts/build-phone.ps1
} finally {
    $env:DX340_DEBUG_KEYSTORE = $previousDx340Key
}
# Check the actual APKs against the installed identity before any update.
./scripts/verify-android-signing.ps1 -Serial d -Apk artifacts/dx340-companion-0.1.0-debug.apk -Package org.dx340.remote.host
./scripts/verify-android-signing.ps1 -Serial d -Apk artifacts/dx340-phone-0.2.0-development.apk
```

These signing commands still need the JDK/SDK/pinned Flutter environment described above. Custom production signing is outside this standard debug-key override.

29 October2026: original key transfer is resolved on this PC. C:\Users\user\.android\dx340-original-debug.keystore was verified against installed certificate24f48aa3... and used for successful in-place companion/shared-phone upgrades. Keep selecting it with the process-local override; the PC's different default debug.keystore remains unchanged. The requested C:\Users\user.android directory was not writable. Current build environment uses Android Studio JBR21.0.7, SDK at C:\Users\user\AppData\Local\Android\Sdk and pinned Flutter in work/flutter; the earlier Windows toolchain paragraph is historical. See test-results/2026-10-29-original-key-upgrade.md for installed hashes and preserved-data checks.

## DX340 companion

```powershell
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $adb devices -l
& $adb -s d install -r artifacts/dx340-companion-0.1.0-debug.apk
& $adb -s d shell am start -n org.dx340.remote.host/.MainActivity
```

Use `d` only when `adb devices -l` still identifies it as this DX340. No firmware update, root or source-app replacement is required. Use Android mode, not Pure Mango OS.

1. Tap **Grant Bluetooth permissions** and accept Nearby Devices. Allow notifications for the ongoing remote-service notification.
2. Tap **Enable playback session access** and enable the companion in the device's notification-listener settings. Read the Android prompt. Initial development setup used ADB to grant these permissions; that is not evidence of a passed first-install wizard. If this firmware blocks sideloaded restricted settings, record its actual screen before choosing its recovery path rather than assuming a universal menu sequence.
3. Tap **Start Bluetooth remote**. **Show / hide technical diagnostics** exposes GATT/advertiser errors and source probes. If Bluetooth is off or an advertiser fails, stop the remote, correct the condition, and start again.
4. With no controller phone, use **Test Mango library on this DX340**. Browse All Music/Album/Artist/Genre, choose a playable track, and confirm **Test selected track**. This deliberately tests public playFromMediaId even though Mango's current action flags omit it.
5. Open Mango with **Open Mango Player**, inspect which song is selected and listen using the reported AMP15 phone output at your usual low level. Return with system Back/Recents and record **Correct selected track is audible** or **Wrong track, silent or failed**. If another app is playing, pause it first. The companion never raises volume for this test.
6. A successful recorded test enables play-by-ID for this exact Mango app version/code and firmware fingerprint. An update or a recorded failure invalidates that verification. This is one local integration result, not T08's 20-track acceptance or a BLE-phone pass.

The latest source preserves pending tests across Activity recreation, puts result controls above the list, and provides Open Mango/Retry on errors. The first installed local-test build put those controls below its list; scroll to its bottom if using that preserved candidate. Its APK is saved as artifacts/dx340-companion-installed-local-test.apk. Do not replace an in-progress test until recording its result; updating can restart the companion while source playback remains independent.

Use **Export diagnostics** to save an internal capability/event snapshot through Android's document picker. Debug evidence can also be retrieved:

```powershell
& $adb -s d shell run-as org.dx340.remote.host cat files/diagnostic.json > artifacts/dx340-diagnostic.json
& $adb -s d shell run-as org.dx340.remote.host cat files/mango-playback-tests.jsonl > artifacts/mango-playback-tests.jsonl
```

Only run the second command after recording a result. A file's absence is not a failed audio test.

## Android phone remote

Use a separate physical phone, API 31 or newer. Do not install on DX340 and count its UI as a controller radio test.

```powershell
& $adb devices -l
& $adb -s <phone-serial> install -r artifacts/dx340-remote-0.1.0-debug.apk
```

1. Open DX340 Remote → **Setup**, grant Nearby Devices/notification access, then **Find DX340**.
2. On DX340, **Open pairing for 60 seconds**. On the phone select the discovered DX340 and accept the OS authenticated pairing prompts on both devices.
3. On DX340 review the requesting identity under **Controller access**, then tap **Approve** for that named controller. On the phone tap **Retry authorization after approval**. If the pairing window expired, reopen it first. A changed request must be reviewed again.
4. Player shows individually supported controls. Library browses the selected source; Queue displays only a source-provided queue. Disabled controls are not implemented by pretending a BLE write succeeded. Actual volume remains disabled pending output verification.
5. The connection foreground service survives Activity recreation. Range recovery makes bounded retries and discards unsent commands; reconnection observes state without changing playback. Actual background/radio behavior remains untested on a separate phone.
6. Tap **Disconnect** before switching controllers. The current beta accepts one controlling peer; automatic handoff is still outstanding. In DX340's **Controller access**, **Revoke access for…** removes only that identity's approval and disconnects its active session, retaining other approvals and OS bonds. Management also works while the remote service is stopped. **Revoke all controller approvals** removes all application approvals and closes pairing. These actions leave source playback running; actual multi-controller revocation/reapproval still requires physical acceptance.

## Mac and iPhone

The committed `apple/DXRemote.xcodeproj` is generated deterministically by `python3 scripts/generate-xcode.py`. Python generation is verified on Windows; hosted Mac CI has now compiled and opened the native app on an iPhone Air/iOS26.5 simulator using Xcode26.6. This is not physical iPhone deployment or Bluetooth acceptance. An equivalent XcodeGen project.yml is provided but is not required to open the committed project. `apple/Package.swift` tests the shared Swift framing implementation without a phone.

On the user's Mac, first record macOS and Xcode versions. Choose an installed Xcode that actually supports the reported iOS 27.2 build; no such compatibility is assumed from Windows.

```sh
sw_vers
xcodebuild -version
sh scripts/build-apple.sh
```

That script runs Swift protocol tests and a generic iOS Simulator compile, writing artifacts/. To also install and open the app in an available iPhone simulator:

```sh
xcrun simctl list devices available
sh scripts/build-apple.sh <iPhone-simulator-UDID>
```

Inspect artifacts/apple-simulator-launch.png and apple-simulator-launch.txt. These commands have run on the standard GitHub Mac runner; they cannot run locally in this Windows session. Compilation/launch success would not establish physical BLE support. On failure, preserve artifacts/apple-build.txt and swift-tests.txt for repair. The script exits without a build on non-macOS hosts.

Open `apple/DXRemote.xcodeproj` in Xcode. Select the DXRemote scheme, choose your signing team and connected iPhone, and run. Record the iPhone model and exact Settings → General → About OS/build. Complete development trust/Developer Mode only as required by that device/Xcode. No TestFlight/App Store upload is performed by these scripts.

In the iPhone app, use Setup → Find DX340, then the same pairing/local-approval flow above. The app declares bluetooth-central background mode and implements restoration. Force-quit, radio-off and OS suspension recovery require physical tests. No silent audio or network fallback is used to keep it alive.

## Repeat source probes

The default read-only instrumentation uses the companion's ordinary package identity, not Android Auto's identity. Running instrumentation restarts the companion process and interrupts its BLE connection, so first finish any pending local test and disconnect controllers. The default command below sends no playback commands. Additional explicit mango-transports/mango-observed-state modes DO issue playback commands and are documented in their test reports.

```powershell
& $adb -s d install -r artifacts/dx340-source-probe-0.1.0-debug.apk
& $adb -s d shell am instrument -w org.dx340.remote.host.test/org.dx340.remote.host.SourceProbeInstrumentation
& $adb -s d shell run-as org.dx340.remote.host cat files/source-probe.json > artifacts/source-probe-tree.json
```

Restart the companion afterward. The runner records native browser roots/pages and a Media3 Apple Music root attempt. `INSTRUMENTATION_CODE: -1` is Android RESULT_OK for completion of the reporting runner; it is not an assertion that source gates passed. Inspect each result.

On this DX340, finishing instrumentation also left the enabled optional browsing services listed as crashed/unbound. If assisted browsing reports missing access afterward, open Android Settings → Accessibility, select each DX340 browsing service, turn it off/confirm Stop, then turn it on/review Allow. Return to the companion and refresh. This manual recovery was verified; restarting the companion alone did not restore those bindings. A normal phone UI or source-only compile does not establish automatic process-death recovery.

For the native/assisted capability-boundary component test, prefer the debug receiver's `CHECK_MANGO_CAPABILITIES` operation documented in test-results/2026-10-29-source-recovery.md. It uses actual native Mango with simulated readiness/verification in temporary isolated preferences and sends no playback command. The actual run kept the same process and both service bindings. It requires privileged DUMP access, is absent from release and does not test phone Bluetooth. The older instrumentation mode still restarts the target process.

## Local output-volume verification

The shared Flutter phone Player has volume buttons, enabled only for a locally confirmed pair of levels in the current companion session. The older native reference phone screens do not yet expose these buttons. Phone Bluetooth operation remains a separate acceptance test.

1. Connect your usual headphones/output and choose a comfortable low level with the DX340 wheel. Play the intended source in Mango or Apple Music; leave the other source paused.
2. In DX340 Companion, start Bluetooth remote, then tap **Verify output volume on this DX340**. Choose the matching source. Source selection alone does not start music.
3. Enter the amp/output configuration, for example **AMP15 phone out**. Tap **Test one step quieter**. Confirm **I heard the quieter step** only if you heard it.
4. Tap **Return to starting volume**, then **I heard the return · enable this range** only if you heard that return. Only the starting level and one step below are enabled; this does not verify the entire hardware range.
5. On an authorized phone, refresh Player. At either boundary only the inward step is enabled. Restarting/stopping the companion, losing source-session access or an output-device callback clears the check. A wheel value outside the confirmed band disables it. Recheck separately for the other source and after changing amp/output/headphones/gain, since Android cannot identify every physical configuration.

**Clear verification / cancel check** leaves the current volume unchanged. Interrupted commands do not automatically retry or restore volume. Use the DX340 wheel when manual adjustment is needed. Each human observation and API readback must be recorded separately; successful BLE delivery is not an acoustic result.

## Optional Apple Music browsing

If Apple Music finishes loading rows or changes its visible list, the remote may report that the library changed. Refresh the library and select again. The companion refuses the old snapshot rather than selecting a row whose identity changed. Toolbar collapse alone is handled for unchanged song rows; this does not remove the screen-on/unlocked requirement.

The user accepted this screen-on/unlocked mode. Apple Music must be6.5.3/code1599; an update disables the unverified adapter. Its direct browser is denied in the tested interfaces. This mode navigates the installed app's visible Downloaded library; it does not extract downloads.

1. In DX340 Companion, tap **Enable optional Apple Music browsing**. In the opened Accessibility settings, choose the companion's Apple Music browsing service and review/allow Android's prompt. This service is already enabled on the currently connected development DX340.
2. Start Bluetooth remote, leave DX340 screen on/unlocked and open Apple Music. The phone Library screen can request the Downloaded root and navigate albums, playlists and songs. Only source-labelled downloaded songs are enabled for selection. Artists is currently disabled pending verification.
3. Back/More navigate the source's actual UI. Screen/layout changes can invalidate IDs; use root refresh when told the page changed. A locked/asleep device disables browsing and reports that it must be unlocked. Ordinary session controls are reported separately.
4. Tap **Disable optional Apple Music browsing** to turn off product access. You can also disable the service in Android Accessibility settings. Keep Mango as the local-file player.

Actual companion-service album/song reads, scroll, stale-ID and screen-off checks are recorded under test-results/. Complete playlist/song-count and phone BLE acceptance are still required. App updates may break semantic navigation; the mode must then be revalidated. The separate read-only research APK remains uninstalled and is not required for this mode.

For development only, `am start -W --activity-clear-top -n org.dx340.remote.host/.MainActivity --ez developmentProbe true` recreates the debug companion Activity and starts probes; this avoids merely bringing an old Settings screen atop its task. The debug-only UI probe receiver requires Android's privileged DUMP permission, is absent from release, and is not an everyday control path. See the test report for its exact command and evidence limits.

## Optional Mango folders and existing playlists

This is the separately accepted screen-on/unlocked mode for Mango4.1.37/code240. Native All Music/Album/Artist/Genre remain available. The own companion service has browsed nested folders and selected a WAV track that the user confirmed audible; the full format/SD/phone Bluetooth matrix remains open.

1. In DX340 Companion, tap **Enable optional Mango browsing**. In Accessibility settings, enable **DX340 Mango browsing** and review Android's permission prompt. On the current development DX340, the optional mode and this service are already enabled. Older builds called these “Mango folders” / “DX340 Mango folder browsing”; the service component is unchanged.
2. Keep DX340 awake and unlocked. In the connected phone's Library, choose **Local folders · assisted**, then **Internal Storage** or **SD Card**. Navigate folders and choose a playable FLAC/WAV row. Back/More follow the source's visible folder view. No Local Network option is exposed.
3. To try the local test without a phone, choose **Test Mango library on this DX340** → **Local folders · assisted**. The test briefly opens Mango for each assisted operation and returns to the test screen. Select a file, confirm **Test selected file**, listen, then record the audible result. Local results do not verify phone Bluetooth or native play-by-ID.
4. For an existing playlist, choose **Playlists · assisted**, then the named playlist and a track. The remote cannot create, edit or delete playlists. Two local playlist selections were audibly verified on d2c24a52.... Newer068c39ec... verifies all30 positions across six pages and one audible last-page selection after a retained metadata delay. Multiple/duplicate playlists, responsiveness and phone Bluetooth remain pending; see `test-results/2026-10-30-mango-playlist-pagination.md` and the preceding two-track report.
5. If the screen changes or an item expires, dismiss any Mango popup, then use **Retry library** / root refresh. If the device sleeps, wake/unlock it first. Turn the mode off with **Disable optional Mango browsing**; the separate Android Accessibility permission can also be revoked. An app update requires adapter revalidation.

This mode uses Mango's semantic UI, not a replacement audio engine. It does not enable seeking or output-volume control; those remain separately capability gated.

Folder playback now leaves/reopens the folder to reject cached filenames after external changes, then revalidates the held viewport and selected row. This can take several seconds: one later-page preparation measured12.551s and misses the response target. The new path has validation-only device checks; its current audio/phone/rescan/SD acceptance is pending. See `test-results/2026-10-30-mango-folder-file-changes.md`. Native album controls retain their separate path.
