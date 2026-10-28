# Mango folder recovery from the Playlist screen

29 October2026, Asia/Shanghai. **VERIFIED for the actual source-local navigation described below. Full playlist browsing and phone BLE remain unverified.**

Device d: DX340 Android13/API33, build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240; Apple6.5.3/code1599. Both source sessions stayed paused. No playback, volume, permission, firmware, music-file or playlist-editing action was used. Wi-Fi0/default network:none.

## Playlist inventory and reproduced failure

Read-only research opened Mango's Music menu and Playlist screen using ADB taps based on observed screenshots. The own AccessibilityService snapshot exposes entry_title_tv=Playlist, playlist_view and the separate New Playlist control. The list has zero children and the visible screen has no saved playlist rows. This is the current observed list, not a universal claim that Mango cannot support playlists. No playlist was created or modified, and New Playlist was not clicked. Local Network was not entered.

Research captures: artifacts/mango-playlist-research-{initial,menu,selector,list}.png and matching *-snapshot.json; after-loop-snapshot.json also records the failed navigator's state. ADB coordinates were research-only, not added to application navigation.

FAILED before the fix: four Apple-root → Mango-folder attempts each timed out with UI_ACTION_TIMEOUT after8.50–8.63seconds while Mango remained on Playlist. Native roots and assisted storage-choice pages succeeded; actual Internal Storage navigation did not. The navigator knew the player Library button and folder menu paths, but the Playlist screen exposes neither player button nor folder_detail_content. Its menu arrow was never selected. Trace showed menu=false/storageClicked=false with an exhausted8second deadline. Apple roots succeeded in all four rounds; the earlier Apple transient root failure was not reproduced here.

Before evidence: artifacts/ui-navigation-before/summary.json and individual fresh debug reports. These failures are retained as failures, rather than counted as a passing retry.

## Implemented correction

MangoUiModel now recognizes the observed Playlist layout only to return to local-folder navigation. Recognition requires one exact Playlist header, one playlist_view, their expected shared page/navbar ancestry and one enabled/clickable nav_bar_back_iv in that navbar. Ambiguous/cached/unrelated layouts fail closed. It returns only that menu arrow; playlist creation, rows, menus, deletion and network controls are not exposed. Folder parsing still rejects this screen as a file list.

The existing settled-navigation signature now includes the header and playlist container. After a stable observation, the service uses its existing menu action once, then selects the requested local storage through the existing semantic/authorization/window/version checks. The8second deadline, screen-on/unlocked requirements, cancellation and playback routing remain in force. No gesture or coordinate is used by the production adapter.

Two new parser tests cover recognition versus Settings/folders/destructive controls, disabled arrows, duplicate headers/lists and unrelated page ancestry. Existing folder/empty/cached-page tests still pass.

## Current verification and install

VERIFIED: scripts/build-android.ps1 completed native APKs, instrumentation compilation, lint and74 aggregate JVM tests with the original external key selected using process-local DX340_DEBUG_KEYSTORE. Log: artifacts/mango-playlist-recovery-android-build.txt. Shared Flutter/native controller/Swift sources did not change in this correction; their preceding Windows analysis/28 Flutter tests/release/lint results retain that scope. A current iOS compile still requires a Mac.

Signing preflight matched installed original certificate24f48aa3...; adb install -r succeeded. Four companion preference files stayed byte-identical. Both optional Accessibility services rebound with no binding/crashed entries. The installed host APK was pulled and exactly matched the built artifact:

**Companion SHA256dfdcaf1bee7ad23339f5b7dd546ceabe408c9cbda6964530e0b335691abc5b9a.**

Shared phone remains installed at queue checkpoint SHA256897f32fdb04cdfbe7c9033b754aa02aeed7dc5af21ff66e8ba39d9299e12eaa1. No uninstall, data reset, default-key or global-environment change occurred. Preflight/install/hash/preferences/services/network reports use the mango-playlist-recovery prefix under artifacts/.

VERIFIED after the fix: four source-navigation rounds all returned Internal Storage with9 rows. The first began from the retained Playlist screen and took5.12seconds including the ADB probe/report round trip; subsequent rounds began from folders and took1.74–2.22seconds. Apple Downloaded roots also succeeded in all four. Evidence: artifacts/ui-navigation-after/summary.json.

Two additional explicit research setups reopened the verified empty Playlist screen before each own-adapter folder request. Both returned Internal Storage/9 rows, in4.80 and4.57seconds. Reports: artifacts/mango-playlist-recovery-repeats/summary.json and individual fresh snapshots/results. Both source sessions remained paused and control target Mango. These are three actual Playlist-to-storage recoveries in total, not six identical starting-state tests. The three folder-start rounds are recorded separately.

The Playlist recovery times exceed the plan's proposed three-second first-uncached-page target; performance acceptance is **not passed**. This correction proves bounded recovery from the observed page, not general Mango navigation or phone responsiveness. The earlier occasional post-launch UI_FOREGROUND_REQUIRED and Apple UI_ACTION_FAILED remain separate reliability follow-ups.

## Next evidence needed

No existing Mango playlist was available for row/track selection research. Observe a real populated playlist before adding an adapter; editing/creating playlists is outside the initial browsing scope. Full source20-file/format/SD/rescan matrices and both phone BLE paths remain open. Recheck this navigation from Playlist, player, nested folders, lock/background and source changes over each phone, including in-flight revocation/cancellation. Do not infer radio, audio or full playlist support from these source-local reads. Goal active and incomplete.
