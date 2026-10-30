# Guided connection —31 October2026

Scope: implement the owner's simpler connection mechanism and UI request on the existing Flutter phones/native bridges and Kotlin Companion. Preserve Bluetooth-only control, local approval, remembered-controller access and source playback. Full project remains ACTIVE/incomplete.

## Final Android status correction

After the initial ee98c10 checkpoint, actual captures showed that AUTHORIZE completed on the phone while the Companion still displayed Ready: the host had not notified its local UI when control became authenticated. Added a bounded connection-status record/observer notification immediately after the existing authorization grant; no authorization rules or source commands changed. Native APK/lint rebuild PASS (protocol/cache/adapter code unchanged from166 tests). Corrected Companion **b0e1f582cfc1f1edfdf5ca98630c359ba8a3dad8f0a664e4a849332119d57301** was original-signer upgraded in place and re-pulled byte-identical; phone **e309eccf…** unchanged.

Saved-player reconnect on the corrected host is VERIFIED: fresh Pixel UI says Connected to DX340 and the simultaneously observed Companion says Controller connected. Normal Disconnect returns the Companion to Ready/advertising and the phone to remembered Connect. Final-corrected preflight comparison again preserves all4 host preferences, two approvals/bonds, both sources paused2, output37/prooffalse, both optional modes and bound AccessibilityON, original fonts/themes/radios. No playback/volume command or new listening proof. Private exact captures: `host-verified-connected.png`, `phone-verified-connected-clean.png`, `host-verified-ready.png`, `phone-verified-idle.png`. Earlier10b… and misleadingly named intermediate captures below are historical and excluded from final corrected-host appearance evidence.

Hosted jobs at ee98c1033a7be29f394eb16f3fbcca738b1645bf: [native Apple41130610830](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41130610830), [shared iPhone41130610891](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41130610891). Last observed in progress; the later Android-only status correction does not alter their Apple/shared application code.

The follow-up also fixes iOS unsupported-Bluetooth presentation (failed/unavailable instead of indefinite Preparing), marks connected restoration as Connecting, and retains the failure phase after a setup-expiry disconnect callback. These are unverified native Apple changes until the follow-up hosted jobs finish. Their push supersedes the initial ee98c10 jobs; do not cite a superseded run as evidence for the new Swift code. Android/shared Dart binaries are unaffected by these Swift corrections.

## Implemented

- Phone Setup has a phase-specific next action: Connect, progress/Cancel, approve on player, choose among discovered players, explicit controller handoff, connected/Open Player/Disconnect, and permission/radio recovery. Permissions, discovery and connection no longer require three manual buttons. Connection help is secondary. Setup scroll resets on a phase change; Library/Queue geometry handling is retained.
- Native clients remember a player only after confirmed authorization. A known approved player reconnects directly; otherwise a10-second custom-service-filtered scan chooses a unique first player. Multiple players and substitution for an absent saved player require explicit choice. Per-attempt callbacks/generations discard canceled discovery results.
- LOCAL_APPROVAL_REQUIRED starts only bounded AUTHORIZE observation every1.5s, respecting the existing60s setup deadline and in-flight request guard. Cancellation/loss/timeout/success clears it. Source commands are never replayed. Another controller's ownership still requires the deliberate existing proposal-bound action.
- Companion uses Set up first phone or Enable phone control when stopped, and Ready/tap Connect plus Set up a new phone when running. First-phone setup starts the service and opens the local60s window in one action. Stop is under Advanced. Approval controls remain identity/bond/window/epoch guarded. Permission success resumes only the explicitly requested pending start action.

## Windows checks

Host DESKTOP-T9ICUE4, Windows11 Pro10.0.26300; Flutter3.41.4/Dart3.11.1, Android SDK36, Android Studio JBR. `scripts/build-android.ps1`: PASS including166 JVM tests (protocol48/cache7/adapters111), native APKs and lint. `scripts/build-phone.ps1`: PASS,63 Flutter tests, analyze, ordinary release-mode development APK and Android lint. Additional debug-only Setup capture fixture analyzes cleanly. New shared fixtures cover8 discovery decisions and10 retry-gate steps; Flutter tests include duplicate Connect, denial, late permission after Cancel/bridge failure, deliberate change-player and both platform layouts at320px/text2.

An initial native compile failed on a public cross-module nullable property smart cast; using a local value corrected it. Initial widget checks caught an old Find label expectation, phase scroll inheritance and a test platform-override cleanup issue; final63 pass. These failed attempts remain private logs, not physical failures.

New Swift code/captures are **NOT YET VERIFIED** until the new hosted Apple jobs finish. Prior31047c22 native14/shared56 success applies only to that earlier source.

## Physical Android observations

DX340 seriald Android13/API33, firmware eng.iBasso20260812, Mango4.1.37/code240, Apple Music6.5.3/code1599; Pixel9a serial53061JEBF22737 Android16/API36. Existing original development signer SHA25624f48aa3… matched installed/candidate APKs before `adb install -r`; no uninstall/data reset/key substitution.

- Installed Companion **10b13583082540c54c2a26d85233c72af7d0ab17c38bc53b32eaa8850b10a901**, phone **e309eccf9bdee05c77006609230fa0ffd1d816e2df85d6cac82036e5d6ddf35b**. Re-pulled installed APKs are byte-identical to candidates.
- Service was stopped by in-place upgrade. Owner-facing Enable phone control was tapped on the observed Companion screen; it resumed service without opening a new enrollment window because approvals already exist. A direct shell start was rejected because HostService is intentionally not exported; no export change/bypass was made.
- **VERIFIED**: phone with no new remembered-player entry → one Connect → bounded discovery → protected subscription → existing host authorization → Connected to DX340. No manual Find/device-choice/Retry, new approval or pairing-window action was needed. Open Player showed current metadata; Refresh player performed GET_STATE only.
- **VERIFIED**: normal Disconnect, cold force-stop/relaunch, remembered Connect to DX340, successful reconnect and normal Disconnect. The immediate screenshot named `phone-remembered-connected.png` was taken during Connecting; the next current enabled Disconnect action establishes later connected state. It is not a final connected screenshot or latency measurement.
- **VERIFIED**: explicit Connection help → Choose another player begins discovery; Cancel returns to idle, and a later observation past the10s discovery limit remains idle. No delayed connect was observed.
- **VERIFIED**: all four normalized host preference XMLs (controllers, mango-ui, apple-ui, source-verification) equal preflight. Existing two controller approvals and OS bonds retained. Mango and Apple optional browsing remainON per owner preference. Both source sessions report paused2, output37, API musicActivefalse and volume prooffalse unchanged; no playback/volume commands sent in this scope. Both optional Accessibility services are enabled and bound, none crashed. Final host is advertising after normal phone Disconnect; canceled discovery did not reopen a connection.
- Radios unchanged: DX340 Wi-Fi0/mobile0/Bluetooth1; Pixel Wi-Fi0/mobile_data1/Bluetooth1. Prior no-SIM evidence is separate; this is not a cellular-toggle-off test. Original font1/nightyes onDX and font1/light onPixel unchanged.

Private evidence under `artifacts/connection-flow-20261031/`: build/test/lint/signing records, fresh Pixel UI XML, pulled PNGs, preflight/final preferences and dumpsys, host diagnostic. DX340 UIAutomator was avoided because of its Accessibility suppression effect; host input used an observed native screen. No Windows foreground input used.

## Not tested / remaining

New unapproved-phone physical pairing and automatic approval completion; canceled connection during live authorization; multiple actual DAP discovery; physical iPhone; TalkBack/VoiceOver; background/range/reliability; fresh audio/source/format/SD acceptance; original silent-playback/disconnect/133 cause; icon deliverables; release signing/store upload; Watch. Do not transfer earlier source listening evidence to these new binaries. See [distribution and player compatibility](../distribution-and-player-compatibility.md) for the owner's wider-DAP/Play intentions and their limits.
