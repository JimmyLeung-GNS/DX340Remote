# Compatibility and acceptance matrix

29 October 2026. VERIFIED means only the stated observation. No complete T01–T18 acceptance run has passed.

Latest local software checkpoint: Android builds/lint and74 aggregate JVM tests; Flutter analysis/28 tests/release APK/release lint pass. Original matching key restored; companion and shared phone upgraded in place on DX340, preserving companion preferences. Current Swift changes have not compiled here; the connected Apple tool fails with xcrun ENOENT. Earlier launch/CI/source results below retain their historical scope. Full-scope audit: completion-audit.md; current pending session-access evidence: test-results/2026-10-29-pending-session-access.md; installed-build/interruption evidence: test-results/2026-10-29-assisted-interruptions.md; preceding Mango recovery: test-results/2026-10-29-mango-playlist-recovery.md; queue evidence: test-results/2026-10-29-queue-pagination.md; preceding audio/key evidence: test-results/2026-10-29-original-key-upgrade.md.

| Platform / combination | Build | Open | Physical BLE controller |
|---|---|---|---|
| DX340 host, Android13/API33, eng.iBasso.20260812.181952 | VERIFIED latest Windows Gradle build/lint | VERIFIED current package installed/opened; advertising active | Historical advertising callbacks only; central NOT TESTED |
| Shared Flutter Android remote0.2.0+2 | VERIFIED latest analysis/28 Dart tests/release APK/release lint | VERIFIED current DX340 disconnected cold-launch/capture only | NOT TESTED; same-device launch is not phone BLE |
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
| T12 Interruptions | Request/callback/session-access recovery and indication-deadline helpers pass software tests, alongside earlier activity/capability checks. Actual DX340 notification revoke/restore/epoch invalidation/fresh-root recovery passed without restarting host. Four local Apple/Mango pending screen-off/in-process-source-change cancellation, old-token rejection and fresh-page recovery cases passed. Two pending notification-access loss/regrant cases also passed: source data/capabilities cleared and fresh browsing/queues recovered without host restart. Actual Accessibility/controller revocation, session replacement/phone/radio/background matrix NOT TESTED |
| T13 Matched battery runs | NOT TESTED |
| T14 Walking | NOT TESTED |
| T15 Authorization/revocation | Current-approval reply guard tested in software and pending frames cleared on disconnect; real unapproved/revoked central NOT TESTED |
| T16 Protocol faults | JVM framing/dedup/routing/revision and7 new confirmed-queue timeout/failure/priority tests pass; physical transfer faults and delayed same-peer callbacks NOT TESTED |
| T17 Installation/upgrade | Earlier DX340 debug upgrades/open verified. Current host/shared-phone matching-key upgrades VERIFIED on DX340, four companion preference files unchanged and installed bytes checked; separate-phone upgrade/fresh permissions/credentials matrix NOT TESTED |
| T18 Watch | NOT TESTED / implementation pending phone stability |

Software tests use synthetic fixtures. Actual source probes run on DX340 with its installed apps; reports identify dispatch, observed state or user-confirmed audio. Neither category substitutes for phone Bluetooth acceptance.
