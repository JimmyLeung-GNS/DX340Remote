# Development handoff — 29 October 2026

**Superseding workspace notice:** continue in `C:\GitHub\DX340Remote`, branch `codex/source-recovery-checkpoint` (b3511ec and later), on the current Windows PC JIMMYAH. This old checkout/branch is preserved history. The primary checkout includes newer cancellation, queue pagination, elapsed state and authorization lifetime guards, real authenticated Windows diagnostic evidence, current iOS baseline builds and the selectively ported volume implementation. Read its latest status and dated reports before any device action. Do not reuse old34–35 proof to enable a new build; fresh37→36→37 listening is being recorded there. Full physical-phone/Watch acceptance remains incomplete.

**Resume notice:** the user subsequently requested continuing development on29 October. The goal is active again; read the newest continuation in `docs/status.md` before using the historical pause checkpoint below.

**Implementation paused at the user's request so development can move to another computer. The full system is incomplete.** This document is the latest checkpoint; older dated sections in status.md are historical evidence, not the current feature summary. Resume only when the user asks to continue.

## Open the right source

Public repository: https://github.com/JimmyLeung-GNS/DX340Remote

**Continue branch `codex/output-volume`, not the older default `main`.** Main was400ec34 at this handoff. The volume implementation is7b52021, capture-readiness change8b5b7c4, interrupted-observation tests/capture settling299e668, followed by this handoff commit. No PR or merge was created for this branch; phone presentation review is unfinished.

On the other computer:

```sh
git clone --branch codex/output-volume https://github.com/JimmyLeung-GNS/DX340Remote.git
cd DX340Remote
git status --short
git log -5 --oneline
```

For an existing checkout, inspect/stash or commit its own changes before switching; do not overwrite them. Fetch origin and explicitly select the branch. Repository access is public; pushing requires the user's own GitHub authentication on that computer. The old Windows GCM credentials do not travel with Git.

Read in order: **AGENTS.md, this file, CODEX_GOAL_PROMPT.txt, docs/implementation-plan.md, docs/status.md**. Then read the relevant protocol, installation and test reports. Preserve the accumulated work; do not restart P0 or replace the architecture.

## Decisions already made — do not ask again

- The user selected **one shared Flutter/Dart phone UI**, with native Kotlin Bluetooth/foreground-service code on Android and Swift CoreBluetooth/restoration on iPhone. DX340 host/adapters remain Kotlin; Watch later remains SwiftUI. AGENTS.md reflects this decision and supersedes the original prompt's separate-native-phone wording. Native phone references are retained until physical parity is verified.
- Native system appearance: Material on Android, Cupertino on iPhone. Four shared destinations: Player, Library, Queue, Setup.
- Everyday control is Bluetooth-only. No Wi-Fi/hotspot/cloud fallback; music stays in installed Mango or Apple Music on DX340. No root, firmware modification, identity impersonation, private-download extraction or replacement engine.
- Preserve Mango Player. The user separately approved optional **screen-on/unlocked Apple Music browsing** and **screen-on/unlocked Mango folders** through our separate version-pinned AccessibilityServices. Updates need revalidation. These are accepted optional modes, not screen-off browsing.
- Public GitHub repository, commits and pushes with the user's account were explicitly authorized. No vendor contact, store release, paid service, signing-key publication or new chat was authorized.
- Only DX340 was available locally. Separate Android phone, physical iPhone, personal Mac/signing and Watch are unavailable to this session. User-reported iPhone iOS27.2 model/build remains unknown.

## What exists

- `android/host-app`: real DX340 companion/GATT host, local approval, notification-session access, diagnostic/browse/volume-check screens and separate Apple/Mango UI services.
- `android/source-adapters`: source sessions/browser/native selection, UI-adapter contracts, observed source handoff, callback ownership guards and public output-volume coordinator. Transport and source adapters remain separate.
- `android/core-protocol`, `core-ble`, `controller-core`, `library-cache`: framing, epochs, bounded duplicate ledger, authorization, native phone service/recovery and cache guards.
- `phone/`: canonical Flutter Android/iPhone UI with native bridges. Dart does presentation/navigation; native layers own BLE/auth/epochs/request IDs/recovery. Android FGS survives Activity recreation; Swift restoration and30s setup deadline are implemented. Mechanisms/builds are not radio stability acceptance.
- `android/remote-app` and `apple/DXRemote`: retained native phone references. They have no new volume buttons yet.
- Shared protocol v1: `protocol/specification.md`, schemas/fixtures. All command mutations are deliberate; reconnect does not replay playback or volume.

## Hardware/source evidence already obtained

Actual DX340 serial `d`, Android13/API33, system build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. Mango4.1.37/code240 (`com.ibasso.music`); Apple Music6.5.3/code1599 (`com.apple.android.music`). AMP15 phone out is user reported. Marketing firmware1.09.851 was reported but not independently confirmed.

- Host advertising/GATT creation callbacks pass. **No separate physical central, bonding/MITM/security/background/range/walking/battery acceptance has passed.** Running both APKs on DX340 is not a two-device BLE test.
- Native Mango library accepted our ordinary app identity. A native selected track was user-confirmed audible. Native root lacks sampled folder/playlist categories; sampled item metadata lacks stable public file URIs. Do not assume numeric IDs survive rescan/duplicate reorder/SD changes.
- Optional own Mango service selected nested `02. 白日夢.wav` and `02. 魔法.flac`; each correct song was separately user-confirmed audible, Mango playing/Apple paused and Wi-Fi0/no default network. Active cached page identification uses unique maximum sibling drawingOrder; fixed first/last rules failed. Empty SD Music has a semantic no-data case. Full20-format/playlist/rescan/SD-removal/phone matrix remains open.
- Direct Apple browsing denied the tested ordinary MediaBrowser/Media3 roots. Accepted own UI adapter selected downloaded One Night Animal from AAA playlist outside the complete prior20-item queue offline. User confirmed correct audible track; delayed reply/snapshot caveat is documented. Earlier Paradise Paradise was ADB foreground research, not our service or BLE. Full20-track/5-album/2-playlist/phone acceptance is open.
- Short local source handoffs observed intended source playing/other paused awake/asleep. No acoustic-overlap or full long-pause/cold-start claim. Seek/resume/cold launch/shuffle/repeat/search remain unsupported where capabilities say so.
- Instrumentation can restart the companion and leave enabled AccessibilityServices crashed/unbound. Local Android Settings off/Stop/on/Allow recovered them. In-process DUMP-protected debug checks avoid that restart. Do not promise automatic process-death recovery.

Read `docs/test-results/2026-10-29-mango-folders.md`, `2026-10-29-source-recovery.md`, earlier selection/handoff reports and the compatibility matrix before extending those areas.

## Current volume implementation and exact verification

`OutputVolume.kt` is a main-thread coordinator shared by local calibration and BLE ADJUST_VOLUME. Public AudioManager STREAM_MUSIC only; one absolute level per write,500ms readback, no automatic retry/restoration. Local calibration requires a labelled output, one step down, explicit human quieter confirmation, explicit return to the starting level and human louder confirmation. **Only that exact two-level band is enabled**, not the whole hardware0..100 range.

Proof is memory-only, bound to source session/epoch, package version, firmware and output inventory. Stopping the companion or audio-device callbacks clears it. Android cannot identify every amp/gain/headphone change; UI explicitly requires rechecking those changes. Only selected source may be playing, with no other active media session reported. Remote commands require delta integer±1, expectedVolume integer, current profileId, current source/host epochs and continued authorization. Stale values/profiles/bounds reject; native clients serialize mutations and never replay after reconnect.

**Actual local results, separate from phones:**

1. Wheel36→35: user said quieter; API/Mango35.
2. Debug public API35→34: user said quieter. Debug return34→35: user said audibly louder. No capability was enabled by these debug probes.
3. New production Mango local calibration35→34→35: user separately answered **“Yes, this new step is quieter”** and **“Yes, the return is louder”**. Human confirmations were then tapped; state exposed verified34–35.
4. Apple selected/playing with Mango paused initially exposed verified=false, proving Mango's proof did not enable Apple. Its separate production local calibration35→34→35 received **“Yes, Apple Music is quieter”** and **“Yes, Apple Music is louder again”**. Confirmation then exposed a different profile, verified34–35, label AMP15 phone out. Wi-Fi reported0. This is local source/audio evidence, not phone BLE.

At pause, fresh state reported **Apple Music playing, volume35, verified34–35** in the existing companion service session. Mango was paused. Proofs will expire when that service/device restarts; this is expected. No extrema or other-output acceptance passed. Human reports are not dB/bit-perfect measurements.

Installed companion used for both calibration sequences: SHA256 `51cf92b5c1850525a3803237b1c67aa0d986e8bd2d8b450c7890f9ba03cc1e94`.

One small fix is included in the final handoff source: reopening the volume screen now repopulates the pending check's output label from its stored context. The old installed screen showed an empty disabled field after reopening, although the check/proof retained the correct label. This fix passed Android APK/lint rebuild (`output-volume-label-build.txt`,40s); **it was not installed or physically rechecked before the user paused**. Latest locally built companion SHA256 `91442ef1635a7df771d94bac863818c3aedbfbbf49dd3b133fcfd2d31889044f`. Upgrading it will stop the old service and expire the current proofs; do not claim they persist.

The temporary synthetic phone capture APK was removed by reinstalling the canonical Flutter phone release at pause: SHA256 `4c1937dbe4554008e085478d85f49b1ab5d11ea07bb699e22f3adebd91f94405`, version0.2.0+2. Font scale restored1.0, night mode yes (original values). The synthetic fixture never sent Bluetooth actions.

## Builds, reviews and unfinished CI issue

- VERIFIED Windows Android build/lint/**50 JVM tests**:40 prior +10 new volume cases. `output-volume-final-build.txt`27s; subsequent label-only APK/lint rebuild passed40s. Tests include duplicate/new-ID stale command behavior, concurrent requests, changed source/output/value, human-confirmation stages, delayed readback failure, lost authorization and no compensation/retry. These are software results.
- VERIFIED shared phone analysis/**14 Dart tests**/release APK/release lint: `output-volume-phone-build.txt`. Canonical code built successfully. Two new model tests cover band/profile/value guards and double-tap/reconnect non-replay; two platform widget cases cover boundary states at2.5 text scale. Fixed earlier compilation/lint/test-cleanup issues; an overlapping Gradle process caused a file lock before the passing serialized build. Do not run two builds of the same project concurrently.
- A fresh Impeccable reviewer returned **ship only for six actual DX340 dark/default-font local volume-check captures**. Phone volume rendering review and final documenter handoff are **unfinished**. Existing DESIGN.md/.impeccable/design.json were not updated for volume; do not claim whole UI approval. The pending-label restoration change was not physically captured.
- iOS run [40739134141](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40739134141) at8b5b7c4 **FAILED its synthetic capture readiness gate**. Analysis/14 Dart tests, canonical Runner.app build/launch/six disconnected captures, and separate fixture Runner.app build succeeded. Downloaded evidence artifact12839271261. Failure: Vision recognized `• volume up`, while `scripts/check-apple-capture.swift` requires exact `volume up`. This is a known OCR matcher issue, not proof of a failed app build. **Not fixed before pause.** Handle the icon prefix narrowly; do not weaken readiness to a splash/tab-only match or call this workflow passed.
- Latest shared iOS run [40740304213](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40740304213), source299e668, was **in progress** at pause. It has improved fixture first-frame/whole-button scrolling but the same unresolved OCR matcher. Inspect actual result/artifacts after resume.
- Native reference run [40739133956](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/40739133956), source8b5b7c4, was **in progress** at pause. Inspect actual result. Two earlier7b52021 runs were cancelled by subsequent pushes, not passed.
- Older native/Flutter simulator checks are recorded in status.md. Hosted Mac: macOS26.6.2/25G83, Xcode26.6/17F113, iPhone Air/iOS26.5 simulator. No physical iPhone signing/BLE proof.

`phone/test_support/volume_capture.dart` is a separate **debug-only synthetic entry point**, never production `lib/main.dart`. It cannot send BLE commands. Native fixture captures may show a fake35 while the actual DX340 status bar shows34; that is intentional simulation, not a connected-phone observation. Android captures under artifacts/volume-phone-synthetic-*.png include earlier helper versions. Final helper was rebuilt but its latest first-frame fix was not recaptured before pause. Do not treat those as final reviewed UI evidence.

## Toolchains and transferable evidence

- Flutter3.41.4, revision `ff37bef603469fb030f2b72995ab929ccfc227f0`, Dart3.11.1; `phone/pubspec.lock` committed.
- JDK21; Gradle8.14/checksum, AGP8.11.1, Kotlin2.1.10, SDK36, minimum31. Android scripts set a Windows-only Java selector workaround; do not copy it blindly to macOS/Linux. Gradle/Flutter downloads need development internet; everyday remote control does not.
- Windows commands: `./scripts/build-android.ps1`, `./scripts/build-phone.ps1`. Mac: `sh scripts/build-phone-ios.sh <simulator-UDID>` and native reference `sh scripts/build-apple.sh <UDID>`. Physical phone needs signing and supported Xcode/device versions.
- Old machine ADB: `C:/Users/JimmyLeung/AppData/Local/Android/Sdk/platform-tools/adb.exe`, serial d. New machine must rediscover path/serial and authorize USB; do not hardcode old machine paths. Companion service is non-exported; shell `am startservice` is denied. Debug MainActivity developmentProbe starts it; see installation.md.
- `artifacts/` and `work/` are ignored and **will not arrive in a Git clone**. Tracked docs summarize evidence. Raw APKs/logs/images/JSON remain on the original computer. Copy a private handoff evidence bundle if needed, or download retained Actions artifacts before expiration. The latest failed iOS artifact was downloaded to `artifacts/volume-ios-40739134141/`; runner artifact retention is3days. No claim that local evidence was uploaded to Git.
- A private local bundle was created at `artifacts/dx340-handoff-evidence-2026-10-29.zip` (about31MB). It contains the latest built companion APK, canonical phone APK, selected build logs/source-state JSON/local screenshots, downloaded failed-iOS evidence and a copy of this handoff. It contains no signing keys or credentials and is not committed/uploaded. Copy it separately if raw evidence/packages are needed on the other computer; the repository version of this handoff remains authoritative.
- Development signing keys and GitHub tokens are not in Git and were not copied. New machine's default debug key may differ and cause INSTALL_FAILED_UPDATE_INCOMPATIBLE. **Do not auto-uninstall the installed companion or silently change signing identity.** Securely transfer the existing development key with user involvement, or obtain a deliberate fresh-install decision recognizing loss of local grants/verification. Apple signing team remains user-specific and unconfigured.
- Subsequent user-authorized signing transfer: the existing Windows debug.keystore was copied to ignored `artifacts/windows-signing-transfer-2026-10-29/debug.keystore` and packaged with instructions as `artifacts/dx340-windows-debug-signing-2026-10-29.zip`. These private files are **not on GitHub**. Copy the key securely to the new Windows user's `%USERPROFILE%\.android\debug.keystore`, backing up any existing key first. Its public certificate SHA256 is `24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d`, verified equal to both existing companion and canonical phone APK signers. DX340 was disconnected at this check; installed certificates were not re-read. Standard development alias/password instructions are inside the private archive. GitHub/Apple credentials are separate and not transferred.
- `work/github-request.ps1` is an ignored helper using the old machine's in-memory GCM credential, scoped to this repo. `gh` was unavailable. Connected GitHub MCP was the wrong account (`tobytai`), so it was not used for writes. Authenticate the correct user on the new machine; do not expose tokens.

## Resume in this order

1. Read instructions/handoff, inspect branch/dirty state and available devices/toolchains. Check the two pending CI results once; preserve diagnostics. Fix the known OCR icon-prefix gate and rerun supported iOS checks. Do not conflate build success with workflow/capture success.
2. Finish volume presentation verification: latest Android fixture/native iOS captures, independent scoped finish reviews and documenter per the applicable Impeccable skill. Preserve system appearance. Verify the pending-label restoration when installing the matching build. Keep simulation/local source/physical phone evidence separate.
3. Continue volume physical gates on matching outputs, including deliberate stale wheel/profile rejection and no replay across reconnect; local proofs expire on service restart. Current34–35 pair does not establish hardware extrema or full T10.
4. When actual phones are available, follow phone-reliability-runbook.md and compatibility-matrix.md: real bond/local approval, offline browse/select, source control, lock/background, radio/range recovery, revocation,100-command latency/security/battery/walking/install tests. Record iPhone model/exact OS build and Android model/OS. Simulator and same-DX340 client launch do not fill these rows.
5. Continue full Apple/Mango browsing/selection/format/playlist/rescan/SD and lifecycle matrices; retain accepted optional modes and Mango engine. Native duplicate/rescan identity remains unresolved. Finish elapsed-position presentation without continuous BLE progress traffic. Keep unsupported features explicit.
6. Watch comes after stable physical phone acceptance. Do not mark the complete goal achieved until all required app/hardware/browsing/Watch evidence exists. Public source availability alone is not completion.

Suggested new-computer Codex prompt:

> Continue this existing DX340Remote project from branch codex/output-volume. Read AGENTS.md, docs/development-handoff.md, CODEX_GOAL_PROMPT.txt, docs/implementation-plan.md and docs/status.md first. Resume from the saved checkpoint, preserve the shared Flutter phone UI/native BLE architecture and accepted Mango/Apple modes, and address the listed unfinished checks. Keep actual hardware evidence separate from synthetic tests. Do not repeat settled preferences or claim physical features work from builds alone.
