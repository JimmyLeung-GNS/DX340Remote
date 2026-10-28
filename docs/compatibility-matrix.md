# Compatibility and acceptance matrix

29 October 2026. VERIFIED means only the stated observation. No complete T01–T18 acceptance run has passed.

Real Windows central now verifies host discovery/GATT layout and four unauthenticated attribute denials;5 awake/5 screen-off Dozing scans pass. Authenticated pairing failed before a code callback and needs native operator consent; no bond/approval established. Exact scope: test-results/2026-10-29-windows-ble.md. This does not pass either phone or full security acceptance.

Latest source-only evidence:20 source-switch rounds/80 reads pass, including Home-origin launches; earlier failures did not reproduce. Actual Apple inventory browses5 albums/2 playlists and prepares20 NOT TESTED listening candidates. No new playback or audible result is claimed. See test-results/2026-10-29-source-library-inventory.md; original phone/source acceptance remains incomplete.

Latest local software checkpoint: Android build/lint and79 JVM tests pass after the Mango settling correction. Current companion0898571b... is installed in place with original key/preferences preserved; the shared phone remains09aaae78... with preceding Flutter analysis32 tests/release/lint. Source-local Playlist recovery improves but still fails the proposed3second target; root/nested recovery and four pending interruption regressions pass (test-results/2026-10-29-mango-folder-timing.md). The10,000-record metadata component benchmark passes on Windows and is not host-cache/source/phone latency proof (test-results/2026-10-29-synthetic-metadata.md). Current Swift has not compiled here; the connected Apple tool fails with xcrun ENOENT. Earlier results retain their historical scope. Full-scope audit: completion-audit.md; preceding Accessibility/readiness evidence: test-results/2026-10-29-pending-accessibility.md; session access: test-results/2026-10-29-pending-session-access.md; earlier interruptions: test-results/2026-10-29-assisted-interruptions.md; queue evidence: test-results/2026-10-29-queue-pagination.md; preceding audio/key evidence: test-results/2026-10-29-original-key-upgrade.md.

| Platform / combination | Build | Open | Physical BLE controller |
|---|---|---|---|
| DX340 host, Android13/API33, eng.iBasso.20260812.181952 | VERIFIED latest Windows Gradle build/lint | VERIFIED current package installed/opened; advertising active | VERIFIED Windows discovery/GATT enumeration/unpaired attribute denial; authenticated exchange/phone centrals NOT TESTED |
| Shared Flutter Android remote0.2.0+2 | VERIFIED latest analysis/32 Dart tests/release APK/release lint | VERIFIED current DX340 disconnected cold-launch/capture only | NOT TESTED; same-device launch is not phone BLE |
| Shared Flutter iPhone remote, iOS17+ proposed | Historical Flutter3.41.4/Xcode26.6 unsigned simulator build/10 Dart tests, CI40723554411 at7921d75. Current Swift/iOS build BLOCKED here | Earlier six disconnected captures inspected; new elapsed/recovery changes NOT LAUNCHED on iOS | NOT TESTED; physical signing/installation open |
| Android remote0.1.0 on same DX340 | VERIFIED Windows Gradle | VERIFIED screenshot/process | NOT TESTED; same-device UI is not phone radio proof |
| Separate Android phone, API31+ proposed | APK available | NOT TESTED, device unavailable | NOT TESTED |
| Native iPhone remote, iOS17+ proposed | VERIFIED Xcode26.6 unsigned simulator build, Swift3 tests | VERIFIED4 disconnected screens plus Player dark/largest-text captures on iPhone Air/iOS26.5;3 scoped UI fixes scored resolved | NOT TESTED |
| User-reported iOS27.2 iPhone | Exact hardware/build unknown | NOT TESTED | NOT TESTED |
| Watch | NOT IMPLEMENTED; stable-phone gate unmet | NOT TESTED | NOT TESTED |

| Acceptance | Current result / evidence still required |
|---|---|
| T01 Offline connection | NOT TESTED with phones; Apple foreground source research had no active network |
| T02 Discovery | NOT TESTED; need ten trials per phone |
| T03 100 controls/source/phone | NOT TESTED; local source probes are smaller and have no BLE |
| T04 Latency | NOT MEASURED; distinguish ACK from observed state |
| T05 Screen off /30min pause | NOT TESTED for BLE; optional Apple/Mango browsing requires on/unlocked. Mango assisted rejection/native browse separation sampled locally |
| T06 Ten range recoveries | NOT TESTED |
| T07 Apple20 tracks/5 albums/2 playlists | PARTIAL: actual own-service downloaded playlist selection outside complete20-item queue observed playing offline; user-confirmed audible with delayed-snapshot caveat. Preceding parser build also selected A・RA・SHI outside complete101-item queue with user-confirmed audio offline. Full20/5/2 and phone matrix NOT TESTED |
| T08 Mango20 FLAC/WAV/folders/playlists | PARTIAL native audible selection plus accepted optional own-service nested-folder WAV selection user-confirmed audible. Native root lacks folder/playlist categories; Preceding parser build also selected03. 風のたより.wav and03. ゴースト.flac with user-confirmed audio; full20/format/playlist/phone matrix open |
| T09 Source switching | PARTIAL:12 local desired-state requests across awake/asleep tests and native/UI selected-item handoff observed correct target playing/other paused. No phone BLE or acoustic-overlap acceptance |
| T10 Actual output volume | PARTIAL: wheel36→35 and app API35→34 matched API/Mango display and user-reported quieter AMP15 output. Upward/boundaries/Apple/other-output/phone/reconnect NOT TESTED; remote volume remains disabled |
| T11 Rescan/SD/stale IDs | Old Mango UI file tokens rejected locally; SD root and explicit empty Music folder read. Current empty Playlist screen observed; folder recovery from it passed3 times after a4/4 timeout baseline. Physical removal/rescan/duplicate cases NOT TESTED |
| T12 Interruptions | Request/callback/session-access recovery and indication-deadline helpers pass software tests, alongside earlier activity/capability checks. Actual DX340 notification revoke/restore/epoch invalidation/fresh-root recovery passed without restarting host. Four local Apple/Mango pending screen-off/in-process-source-change cancellation, old-token rejection and fresh-page recovery cases passed. Two pending notification-access loss/regrant cases also passed: source data/capabilities cleared and fresh browsing/queues recovered without host restart. Two actual Accessibility revoke/rebind cases passed, retaining native/session access and rejecting old UI tokens. Actual controller revocation, session replacement/phone/radio/background matrix NOT TESTED |
| T13 Matched battery runs | NOT TESTED |
| T14 Walking | NOT TESTED |
| T15 Authorization/revocation | Windows unpaired INFO/CCC/RX/subscription all denied0x05; no bond. Current-approval reply guard/pending-frame cleanup tested in software. Authenticated MITM, bonded-but-unapproved, approval and revoked-controller behavior remain NOT TESTED physically |
| T16 Protocol faults | JVM framing/dedup/routing/revision and7 new confirmed-queue timeout/failure/priority tests pass; physical transfer faults and delayed same-peer callbacks NOT TESTED |
| T17 Installation/upgrade | Earlier DX340 debug upgrades/open verified. Current host/shared-phone matching-key upgrades VERIFIED on DX340, four companion preference files unchanged and installed bytes checked; separate-phone upgrade/fresh permissions/credentials matrix NOT TESTED |
| T18 Watch | NOT TESTED / implementation pending phone stability |

Software tests use synthetic fixtures. Actual source probes run on DX340 with its installed apps; reports identify dispatch, observed state or user-confirmed audio. Neither category substitutes for phone Bluetooth acceptance.
