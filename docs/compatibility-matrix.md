# Compatibility and acceptance matrix

28 October 2026. VERIFIED means only the stated observation. No complete T01–T18 acceptance run has passed.

| Platform / combination | Build | Open | Physical BLE controller |
|---|---|---|---|
| DX340 host, Android13/API33, eng.iBasso.20260812.181952 | VERIFIED Windows Gradle | VERIFIED on DX340 | Advertising callbacks only; central NOT TESTED |
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
| T05 Screen off /30min pause | NOT TESTED for BLE; optional Apple browsing requires on/unlocked |
| T06 Ten range recoveries | NOT TESTED |
| T07 Apple20 tracks/5 albums/2 playlists | PARTIAL: actual own-service downloaded playlist selection outside complete20-item queue observed playing offline; user-confirmed audible with delayed-snapshot caveat. Full20/5/2 and phone matrix NOT TESTED |
| T08 Mango20 FLAC/WAV/folders/playlists | PARTIAL one audible selection; root lacks folder/playlist categories. No format matrix passed |
| T09 Source switching | PARTIAL:12 local desired-state requests across awake/asleep tests and native/UI selected-item handoff observed correct target playing/other paused. No phone BLE or acoustic-overlap acceptance |
| T10 Actual output volume | NOT TESTED; remote volume disabled |
| T11 Rescan/SD/stale IDs | Software guards tested; physical media-change cases NOT TESTED |
| T12 Interruptions | Partial Activity-recreation fixture test; real phone/radio/background matrix NOT TESTED |
| T13 Matched battery runs | NOT TESTED |
| T14 Walking | NOT TESTED |
| T15 Authorization/revocation | Current-approval reply guard tested in software and pending frames cleared on disconnect; real unapproved/revoked central NOT TESTED |
| T16 Protocol faults | JVM framing/dedup/routing/revision tests pass; physical transfer faults NOT TESTED |
| T17 Installation/upgrade | DX340 debug upgrades/open verified; fresh permissions/credentials matrix NOT TESTED |
| T18 Watch | NOT TESTED / implementation pending phone stability |

Software tests use synthetic fixtures. Actual source probes run on DX340 with its installed apps; reports identify dispatch, observed state or user-confirmed audio. Neither category substitutes for phone Bluetooth acceptance.
