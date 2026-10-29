# GitHub checkpoint resumed on DESKTOP-T9ICUE4

29 October 2026, Windows continuation in `C:\GitHub\DX340Remote`, branch `codex/source-recovery-checkpoint`. Full objective remains incomplete and active. This report separates fresh local checks from preserved earlier playback and hosted Apple evidence.

## Source and environment

**VERIFIED:** fetched GitHub and fast-forwarded the clean checkout from04b04aa to096601a2cce7b9af16e90664b28c3bae1f131ccd. Read AGENTS.md, CODEX_GOAL_PROMPT.txt, development-handoff.md, implementation-plan.md and status.md before continuation. The actual current host is DESKTOP-T9ICUE4, Windows11 build26300, PowerShell7.6.5; JIMMYAH results in earlier reports belong to the preceding host. No second Windows PC is required.

Current tools: Android Studio JBR21.0.7, SDK under `C:\Users\user\AppData\Local\Android\Sdk`, pinned Flutter3.41.4/Dart3.11.1 under `work/flutter` (revisionff37bef603469fb030f2b72995ab929ccfc227f0), Python3.11.5/Bleak3.0.2 under `work/ble-diagnostic`. Global Java/Flutter/default signing key were not changed. The original external key at `C:\Users\user\.android\dx340-original-debug.keystore` is available; both candidate signing preflights match certificate24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d. No secret is in Git.

Only DX340 `d` is connected: Android13/API33, exact build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`; Mango4.1.37/code240 and Apple Music6.5.3/code1599. No separate Android controller, signed physical iPhone or Watch is available here. No local Mac/Xcode execution occurred.

## Fresh software checks and package separation

**VERIFIED:** process-local tool/key selection followed by serial `scripts/build-android.ps1` and `scripts/build-phone.ps1`: native build/lint/instrumentation compilation and120 JVM tests across21 suites, zero failures/errors; Flutter analysis46 tests/release APK/lint. Windows protocol software suite passes23 tests. Original-key signing preflight passes for both candidates. Logs are under ignored `artifacts/resumed-096601a/`: native-build.txt/native-test-summary.json, phone-build.txt, windows-protocol-tests.txt and both signing-preflight JSON/text pairs.

| Role | SHA256 | Actual scope |
|---|---|---|
| Fresh local companion candidate |5e1d6e0cf5dc592812d341f0d54caa3ec9c34755ed6bc894945b8760a4a1838c| Built from096601a; **not installed** |
| Fresh local ordinary shared phone |2d42728f4f2112552e38ec6030c52b578957f2b0ddc9afffc615f9f5ef0cf78c| Built from096601a; **not installed** |
| Retained installed companion |04846d60bc4e65065028d510286b56cba91577840a994d351146c55dff418fa2| Fresh initial/final pulls match; earlier Mango listening evidence remains scoped to this host APK |
| Retained installed shared phone |23c536e7890ddc58a84552fcd3bd4be4eac578722daa8553c3a94d7bf932328d| Fresh initial/final pulls match; DX340 launch/rendering only, no separate-phone BLE proof |

No APK was installed during this resumption. The old local manifest and40114ff8/09aaae78 package outputs were archived as previous-local-* under the resumption artifacts before builds. The regenerated manifest must distinguish installed baselines from fresh candidates.

Earlier hosted Apple workflows40881374344/40881374524 succeeded at04b04aa (native Swift5 tests/shared Flutter46, unsigned simulator builds/captures). These are preserved **historical hosted results**, not fresh local Mac/signing/iPhone evidence. Their downloaded artifact directories are absent on this PC. See controller-handoff.md for that evidence scope.

## Fresh Windows BLE results

This PC is a development central, not a phone acceptance substitute. All diagnostics allow only HELLO/AUTHORIZE/GET_STATE, send zero playback/volume/source-change commands and do not pair or approve automatically. Raw reports are `artifacts/windows-ble/<label>.json`; failures are retained.

| Label | Result | Evidence/limit |
|---|---|---|
| resume096-desktop-preflight | **BLOCKED** | Unpaired/protectionNONE; refused before explicit GATT operations, zero frames |
| resume096-desktop-unapproved | **FAILED** | Windows reported authenticated bond after user pairing; DX340 listed only JIMMYAH; protected INFO denied ATT0x05, zero frames |
| resume096-desktop-unapproved-retry | **FAILED** | User confirmed both native prompts; DX340 then listed this PC and JIMMYAH; protected INFO timed out, zero frames |
| resume096-desktop-window-open | **VERIFIED, single session only** | Explicit host60second pairing window; protected INFO/indications, correlated HELLO, AUTHORIZE LOCAL_APPROVAL_REQUIRED and GET_STATE AUTHORIZATION_DENIED; MTU517,29 framing writes, successful disconnect cleanup |
| resume096-desktop-approved | **FAILED** | Three-second scan did not find exactly one companion; no GATT/frames |
| resume096-desktop-approved-retry | **FAILED** | Ten-second scan/INFO succeeded but protected indication subscription failed ATT0x01; zero frames and successful cleanup |

The successful denial session used installed baseline04846d60, **not the fresh handoff candidate**. It proves neither authorized metadata nor playback, two centrals, handoff, stable bonding or live revocation. Subsequent Android dumps again lost this PC's bond while Windows reported authenticated pairing; repeated BONDING/BONDED/NONE events remain unresolved. No causal claim about HiBy, radio driver or firmware is made.

Targeted WinRT cleanup removed only this PC's discovered DX340 test pairing: UNPAIRED, fresh Windows pairedAfter=false (`targeted-bond-cleanup.json`). JIMMYAH's Android bond was preserved. Windows Settings still displayed a cached DX340/PAN row; PAN was not joined. Last Android postflight lists JIMMYAH only. A new native pairing flow is prepared; the operator question remains pending. Elapsed time is not confirmation.

The user authorized direct routine desktop/DX340 testing. Existing scrcpy3.3.3 was used over USB with `--no-audio` to mirror the companion, open its60second pairing window and place it beside Settings. Input/focus sharing was explained. The computer-use helper could not target the ShellExperienceHost Add-device picker: its Bluetooth click was rejected as belonging to a different process; the single activate/reobserve retry also failed. No targeting bypass was attempted. The computer-use skill requires manual authentication/security dialogs, so matching-code confirmation remains with the operator. No local controller approval was accepted. The USB mirror is development tooling, not an everyday control dependency.

## Final preservation checks

**VERIFIED:** fresh own-adapter GET_STATE observes both intended players paused (state2), Mango “The Beginning”/ONE OK ROCK and Apple “BAD LOVE - AAA DOME TOUR 15th ANNIVERSARY - thanx AAA lot (Live)”; volume37, output-volume proof false/canAdjustVolume=false. Wi-Fi0/no default network, HiBy stopped=true. No listening test or new audio-quality claim.

All four real preference contents match the initial normalized digests. Initial capture hashed UTF8 LF-joined shell lines without a final LF; the first postflight mistakenly compared those to raw-device byte hashes and reported four mismatches. That incomparable capture is retained as *initial-incomparable-digests.json. A like-for-like normalized comparison matches all four; raw-device digests are separately recorded in preferences-after.json. Do not call the initial normalized comparison an independent raw-byte-before proof.

Final inspection found Mango Accessibility disabled despite initial canBrowseFolders=true. The cause was not established. Its own previously enabled component was restored through a targeted settings update preserving the current Apple component; final dumps confirm both enabled/bound and no crashed services, and fresh source state again reports canBrowseFolders=true. Before/after service lists and final accessibility dump are retained. App preference digests remain unchanged.

Evidence: postflight-state.json, postflight-summary.json, preferences-before/after.json, installed-after.json, services-before/after-targeted-restoration.txt and *-postflight.txt under `artifacts/resumed-096601a/`. Final installed APK pulls match both baseline hashes above.

## Next actions and open acceptance

1. Operator completes native Bluetooth → DX340 matching-code confirmation on both devices. Refresh both Windows protection and DX340 bond inventory before treating pairing as successful; do not reset unrelated bonds.
2. Open the companion's60second pairing window, then run `work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-protocol.py await-approval --seconds 10 --label <unique-label>`. Only when a live identified pending request is visible should the operator approve locally. The hold lasts45seconds. Preserve failures; do not infer a tap or restore a revoked approval automatically.
3. A stable authorized PC session can permit read-only state/duplicate-read/revocation diagnostics. It still does not prove a physical phone path or candidate two-peer behavior. Candidate installs require the documented signature/backup/restoration sequence and their own physical evidence.
4. Separate Android/iPhone hardware and supported Mac/signing are required for both-phone offline/background/source tests, two-controller handoff and audible continuity. Retain earlier20 Apple confirmations across mixed builds/24 attempts and20 Mango confirmations/21 attempts at their recorded scope. Clean final-build T07/T08, verified volume on current session, latency/range/battery/walking/source breadth and Watch remain open.

Local source/docs checkpoint and bundle can be refreshed without publishing. Keep the goal active; no full-system completion claim.
