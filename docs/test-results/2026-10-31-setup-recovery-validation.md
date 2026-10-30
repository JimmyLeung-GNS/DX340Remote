# Setup recovery and background validation —31 October2026

Full project ACTIVE/incomplete. This follows [guided connection](2026-10-31-guided-connection.md), preserving its exact binary/audio scopes. Current installed original-signer Pixel **4449ee74a3545f29e5cb261aeb4f60159be23521ffc145eaf0f4a09bf5b6db84**, Companion **b0e1f582… unchanged**. No playback or volume command is added by this stage.

## Apple software evidence

At source **cde1aa8aadbae3eafc14353d500e0a7012cef1d8**, both hosted jobs completed **SUCCESS**:

- [Native41131663595](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41131663595):16 Swift tests, zero failures; Xcode build, simulator installation/launch and native captures. Artifact13014600104, archive SHA256 **bf787421ca95bcde6d24440619a3f09f21d45dd5168aba95cdcd9fe465da00c6**.
- [Shared41131662763](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41131662763):63 Flutter tests, analysis, ordinary/synthetic iPhone builds and captures, and successful final ordinary-target rebuild/install/launch. Artifact13014530735, archive SHA256 **292db69ad4217196e2fa9fd12f78867d60cdad9da80055e3dc58149521cd0af4**.

Archives were downloaded with existing Git credentials only when needed, digest checked and safely extracted under `artifacts/connection-flow-20261031/ci-<run>/`. Actual logs and the restored ordinary screenshot were inspected. Hosted macOS26.6.2/build25G83, Xcode26.6/build17F113; selected iPhone Air/iOS26.5 simulator. No physical iPhone/signing/BLE/audio pass follows. The later Setup contrast correction needs its own shared-app build/capture confirmation; it does not invalidate unchanged native Swift evidence.

## Simulated presentation and contrast correction

Android emulator36.1.9, existing Medium_Phone_API_36.1 system image, buildBP41.250822.007, headless serialemulator-5580. First `-gpu software` launch failed OpenGLES initialization; the reported `swiftshader_indirect` fallback booted with2 cores/2048MB, without changing physical devices or the saved AVD configuration. An unrelated Digital Wellbeing ANR overlaid the first capture attempt; fresh UI bounds identified its Close app action. That overlay frame is excluded. The completed capture batch verifies native font/night configuration and PNG integrity, then restores emulator font1/light settings.

Ten Android synthetic Setup captures cover idle, approval, choosing, connected, permission, Bluetooth-off, failed, plus font2 dark idle/approval and scrolled visible Cancel. The actual emulator scroll reached Cancel at `[53,1659][1028,1819]`. These debug fixtures cannot send Bluetooth commands. Five cde1aa8 iOS synthetic captures cover idle/approval/choosing/connected light and initial largest-Dynamic-Type approval dark.

The first15-frame cross-platform review returned **fix** for one material finding: iOS Setup's normal17pt systemBlue action text and filled-button label/background have **4.017:1** light contrast, below4.5:1. No new Android presentation correction was requested. The largest iOS screenshot only establishes its initial viewport; actual simulator scrolling/Cancel and physical VoiceOver remain unverified.

The correction retains Cupertino controls and semantic system-blue variants, scoped to Setup. Light actions use the system's stronger blue with white filled labels; dark actions use adaptive blue with black filled labels. Other destinations retain their incumbent appearance. Four new widget cases verify both action/background contrast pairs in light/dark and normal/high-contrast environments, then verify the Player tint is unchanged. The iOS large-layout case now uses text scale3.2; Android remains font2. **67 Flutter tests/analyze/ordinary Android release/lint PASS**. At this implementation step iOS confirmation was pending; the later c8ff4b7 confirmation below closes F1 without changing the old cde1aa8 archive.

Correction source **c8ff4b7b9fd8472e09bf13707ba61068cf1ab56a**; [shared confirmation41138111275](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41138111275) is **IN PROGRESS**. Inspect its terminal status/archive and score only the existing material finding after the new captures arrive; no additional polishing loop is requested.

## Ordinary Android emulator recovery

The ordinary release-mode development package was installed separately from the no-BLE fixture, with matching original signer. Actual system Nearby Devices denial leaves controls unauthorized and does not start a foreground service; the public app-details route opens the correct application's settings and returns to Setup. With Nearby Devices granted and virtual Bluetooth off, the public Bluetooth-settings route opens Connected devices and the app presents Turn on Bluetooth. These are emulator software/UI results, not physical radio acceptance.

Two defects were reproduced and corrected:

1. Bluetooth-off Connect started a foreground service even though no connection could begin. The bridge now checks the native model's actual Bluetooth readiness before starting it; manual device connection also rejects radio-off before service start.
2. Cancel called stopService while the Activity remained bound. The old service still reported `isForeground=true` with `startRequested=false`. A service-owned stopConnection now clears the BLE session, explicitly removes foreground state/notification with STOP_FOREGROUND_REMOVE, then calls stopSelf. Shared and retained native-reference callers use that path. A bound service may remain available for the UI; that is distinct from an active foreground connection.

Final ordinary emulator APK **4449ee74a3545f29e5cb261aeb4f60159be23521ffc145eaf0f4a09bf5b6db84** verifies radio-off Connect with foreground start count0, then two real scan/Cancel cycles. Each scan starts the foreground service; each Cancel leaves no foreground flag and `startRequested=false`, with the bound process preserved for the next attempt. Virtual Bluetooth was restored to its originalON state. An initial launcher retry resumed the existing Settings child task; a fresh NEW_TASK|CLEAR_TASK launch corrected the harness state before verified cases. No stale-bounds click was used.

Native/reference APK rebuilds and lint PASS; protocol/cache/source-adapter code remains unchanged from166 prior passing JVM tests. These changes do not prove a physical radio toggle or fresh-controller enrollment. [Android foreground-service stopping](https://developer.android.com/develop/background-work/services/fgs/stop-fgs), [Service API](https://developer.android.com/reference/android/app/Service).

## Physical background attempt

The owner explicitly agreed to leave both devices/headphone controls untouched for30 minutes. On installed Pixel **e309eccf…** / Companion **b0e1f582…**, a fresh saved-player protected connection was observed before Home/Sleep and the read-only observer began at05:44 HKT. Both devices initially reported Dozing, then USB stay-awake behavior reported Awake. No power/radio settings were changed.

**ABORTED; no30-minute background pass.**27 samples retained both processes/foreground services, both sources paused2, output37/headset(4); last successful sample elapsed **853.066seconds**. A mid-run ActivityTaskManager read showed Remote MainActivity in the foreground. Home/Sleep had not kept it background; setup-complete/provisioned were1 and lock-task modeNONE, but the exact return cause remains unconfirmed. A read-only GET_STATE was logged after foreground return. The operator stopped the observer with Ctrl-C; no final summary was emitted, so a separate `run-review.json` preserves the result alongside all original samples. Raw run: `artifacts/android-background-hold/20261031-054414-a052d/`.

The observer now saves phone power/activity snapshots and host events, records the current top component, and fails if the remote is foreground or background state cannot be established while awake. A fresh optional stop-file lets a device-switch/owner stop write a clean incomplete summary instead of requiring Ctrl-C. A stop-file probe verifies zero samples, operatorStoppedtrue and observationCompletefalse; it is a cancellation check, not a physical duration pass.

Android Settings was explicitly foregrounded and verified instead of relying on Home. A fresh30-minute availability question is pending; do not start the controlled repeat from elapsed time or the earlier answer. No observer is currently running. Do not turn this aborted mixed foreground/background attempt into GateA/T05 acceptance.

## Physical read/upgrade/cleanup

On the old e309eccf… phone build, the ordinary app returned authorized from Settings and a manual Refresh player completed a fresh read before normal Disconnect. This is a post-return read, not a continuous30-minute background pass. The old Pixel service then reproduced the same cleanup defect: `isForeground=true` despite `startRequested=false`, while Setup was still bound.

The final4449ee74… phone APK was compared against the installed signer, upgraded with `adb install -r`, re-pulled and verified byte-identical to the candidate. A fresh remembered-player Connect completed protected existing-grant authorization on the unchangedb0e1f582… Companion. Open Player/Refresh player read metadata; normal Disconnect now leaves no foreground flag and `startRequested=false` while the UI remains bound. This verifies the actual Pixel cleanup correction, independently of the emulator cases. Only HELLO/AUTHORIZE/state observation is involved; no new playback/audio confirmation.

Afterward, all4 host preferences (controllers, mango-ui, apple-ui, source-verification) equal the preflight, both source sessions remain paused2 and output remains37/headset(4) with proof disabled. Both optional browsing modes remainON; existing approvals/bonds are retained. Radios and fonts/themes are unchanged. Final Pixel is disconnected; Companion remains available. Current phone0.2.0/code2/Pixel9a Android16 CP1A.260405.005; Companion0.1.0/code1/DX340 Android13 eng.iBasso20260812; Mango4.1.37/code240, Apple Music6.5.3/code1599. Detailed sanitized fields are in the adjacent JSON, raw private evidence under `artifacts/connection-flow-20261031/`.

## Remaining

Actual iPhone and screen readers, a correctly controlled30-minute repeat, unplugged/screen-off/range/walking/battery, full source/format/audio/SD acceptance, fresh approval and multiple players, original silent/disconnect cause, remaining icon platform checks, production signing/store review and Watch. Keep the full objective intact.

## Hosted corrected Setup confirmation

Shared run41138111275 at c8ff4b7b9fd8472e09bf13707ba61068cf1ab56a finished **SUCCESS**. Artifact13018038764 archive SHA256 `5f199039aba6ea116076bf159dc0475e828aae1482cc3fbe48f1e29bfe2a1aa5` was downloaded, verified and safely extracted under `artifacts/connection-flow-20261031/ci-41138111275/`; the older cde1aa8 archive remains unchanged. Actual logs show analysis,67 Flutter tests, ordinary/isolated fixture builds/captures and ordinary-target restoration build/install/launch passed. The restored frame shows Bluetooth unavailable on the simulator, with no fake connected fixture state.12 capture-readiness matcher regressions pass. Host is macOS26.6.2/25G83, Xcode26.6/17F113, iPhoneAir/iOS26.5 simulator.

All four corrected light Setup frames (idle, approval, choosing, connected) contain actual RGB0,64,221 action pixels against white:7.56089235:1 in either foreground/background direction. The large dark approval frame remains an initial viewport only; no new actual iOS scroll/Cancel or screen-reader acceptance follows. Pixel sample records/hashes are in `artifacts/connection-flow-20261031/contrast-rendered-pixels.json`. The one F1-only reviewer verdict is **SHIP**: F1 resolved and no correction-introduced regression in these five views (`artifacts/connection-flow-20261031/setup-contrast-verdict.md`). This closes only the contrast finding; physical iPhone, full UX-01 and background acceptance remain separate.
