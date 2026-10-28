# Mango assisted folders: local adapter checkpoint

Device: USB DX340 `d`, Android13/API33, DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240, Apple Music6.5.3/code1599. AMP15 phone out is user reported. Companion debug build0.1.0 on branch codex/mango-folders. These are actual DX340 source-adapter checks, **not BLE phone tests**.

| Result | Case | Evidence / limitation |
|---|---|---|
| VERIFIED | Own service browses Internal Storage/Music/Tayori/first live folder | Current row names match source screenshots and separate snapshots; nine WAV rows returned |
| VERIFIED | Active page excludes cached ancestors | Actual sibling drawing orders1/2,1/2/3 and4/1/2/3; selected unique highest, independent of node position |
| VERIFIED | Selected WAV source state | Request02. 白日夢.wav; Mango 白日夢 playing, Apple paused; dispatch/observed metadata only |
| VERIFIED | Selected FLAC source state | Request02. 魔法.flac in tayori LIVE TOUR magic2025; Mango 魔法/album magic playing, Apple paused; artifacts/mango-ui-flac-selected.json |
| VERIFIED | Offline precondition | Wi-Fi0, Active default network none immediately before selection |
| VERIFIED, user reported | Audible WAV match | User answered “Yes, the correct song is audible” for02. 白日夢.wav through AMP15. Delayed reply, not an instantaneous metadata/acoustic measurement; artifacts/mango-ui-wav-audible-confirmation.json |
| VERIFIED, user reported | Audible FLAC match | User separately answered “Yes, the correct song is audible” for02. 魔法.flac through AMP15, with Wi-Fi0/no default network before selection. Delayed reply, not an instantaneous metadata/acoustic measurement; artifacts/mango-ui-flac-audible-confirmation.json |
| FAILED | Initial Music browse returned cached root rows | Fixed-page assumption removed before file selection; not counted as success |
| FAILED | Early root recovery / animation race | UI_ACTION_TIMEOUT / changed child index; bounded settled navigation being corrected and retested |
| VERIFIED | Corrected deep-folder root recovery | Fresh home/storage choice after Internal Storage/Music/Tayori/live-file page returned Internal Storage root; mango-ui-deep-recovery-final-root.json. Earlier failures remain above |
| VERIFIED | Old file token rejected | Previous file snapshot after root recovery returned STALE_LIBRARY, without dispatch; mango-ui-expired-file-rejection.json |
| VERIFIED | Screen-off capability separation | Assisted root returned UI_SCREEN_UNLOCK_REQUIRED, canBrowseFolders false; native root still returned its six entries and source kept playing. Device then woken without changing its lock configuration |
| VERIFIED | Mode disabled leaves native browsing | Native root returned only five native categories, assisted request denied, playback remained playing. Mode restored afterward; separate Accessibility permission retained |
| VERIFIED | Local test foreground restoration | Actual test screen opened Mango for Internal Storage and Music reads, then returned with correct rows. No phone involved; final copy/capture review pending |
| VERIFIED | Cross-storage navigation | Own service returned SD Card root/nine visible directories from the internal folder context; mango-ui-sd-root.json |
| FAILED, corrected | Empty SD Music | Initial list-only parser timed out on Mango's explicit empty_view/no data state; corrected parser excludes cached ancestor rows. Actual local UI SD→Music now shows no local items and Back recovery; empty-sd-music.png |
| VERIFIED, scoped review | Companion/local test presentation | Independent reviewer returned ship for9 actual DX340 dark/default-font captures, including empty folder. No light/large-font/screen-reader/result/play-callback/phone-BLE claim |

Software checks: final scripts/build-android.ps1 passed build/lint and37 JVM tests (13 protocol/reply,3 cache,4 Apple model,8 Mango model,9 handoff). Shared phone analysis/10 Dart tests/release APK/lint and DX340 launch passed. iOS Flutter run40723554411 and native reference40723554458 succeeded at7921d75; Flutter6 disconnected images inspected. These are separate from the actual source-adapter cases above.

## Mixed component capability check

**VERIFIED component policy; simulated UI readiness, actual native session.** SourceProbeInstrumentation mode `mango-capability-boundary` constructs SourceRegistry against the real Mango browser/session but an explicitly simulated ready UI adapter and isolated empty verification preferences. It does not clear or fabricate the user's production verification. An unknown native ID makes the test non-mutating even if the capability guard regresses.

Result: canPlayItem=true, canPlayNativeItem=false, request rejected with UNSUPPORTED_CAPABILITY, all25 sampled native rows playable=false, native diagnostic-test rows retained, zero UI adapter calls. Temporary preferences were deleted. Report: artifacts/mango-capability-probe.json; runner log mango-capability-instrumentation.txt, passed=true / INSTRUMENTATION_CODE:-1. This is not an AccessibilityService, audio or phone-BLE acceptance result. The extended Dart test also confirms that nativeTestable does not authorize phone playback.

Command after installing the matching companion/instrumentation APKs:

```powershell
& $adb -s d shell am instrument -w -e mode mango-capability-boundary org.dx340.remote.host.test/org.dx340.remote.host.SourceProbeInstrumentation
```

## Instrumentation interruption and recovery

**FAILED automatic rebinding in this development interruption; manual recovery VERIFIED.** After instrumentation finished/stopped the companion process, Android listed both enabled accessibility services under Crashed services, with no Bound services. Product browse returned UI_PERMISSION_REQUIRED; native Mango playback continued. Merely reopening the companion did not restore these bindings. This does not prove ordinary OS suspension or actual phone reconnection behavior.

Through Android Settings → Accessibility, each already-authorized browsing service was turned off, Stop confirmed, turned on, and Allow confirmed. Both then appeared bound with Crashed services empty. Apple own-service Downloaded root returned Playlists/Artists(disabled)/Albums/Songs; Mango still played and Apple remained paused. Mango's own-service home also returned both storage choices and canBrowseFolders became true. No source engine, lock configuration or volume changed. Evidence: accessibility-after-instrumentation-{enabled,bound}.txt, accessibility-recovered-bound.txt, apple-after-service-recovery-root.json and mango-final-restored-after-recovery.json. The accepted optional services remain enabled; companion is foreground with Mango selected.

Current companion APK SHA256 f2837d9e63b984f5994791be7b57dfe026e9871022319e38c3e3e76d5fb6de1e. The nine-state visual review used earlier capture APK0153c5740b227bb882341be716672ced480f40c3239473f046a95fd020eca4ac; later capability guards did not change the captured appearance. Fresh-unverified-install UI interaction remains untested.
| NOT TESTED | Full FLAC/WAV, SD removal/rescan/duplicates | Snapshot-scoped identity does not prove long-term native library identity |
| NOT TESTED | Either phone over BLE / background / security | Only DX340 connected locally; simulator/software tests are separate |

Raw source/UI details remain in ignored artifacts/. Public implementation documents contain no file contents or protected downloads. See mango-ui-experiment.md for artifact names and version-specific semantic restrictions.
