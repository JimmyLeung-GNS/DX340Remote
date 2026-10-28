# Mango assisted folders: local adapter checkpoint

Device: USB DX340 `d`, Android13/API33, DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Mango4.1.37/code240, Apple Music6.5.3/code1599. AMP15 phone out is user reported. Companion debug build0.1.0 on branch codex/mango-folders. These are actual DX340 source-adapter checks, **not BLE phone tests**.

| Result | Case | Evidence / limitation |
|---|---|---|
| VERIFIED | Own service browses Internal Storage/Music/Tayori/first live folder | Current row names match source screenshots and separate snapshots; nine WAV rows returned |
| VERIFIED | Active page excludes cached ancestors | Actual sibling drawing orders1/2,1/2/3 and4/1/2/3; selected unique highest, independent of node position |
| VERIFIED | Selected WAV source state | Request02. 白日夢.wav; Mango 白日夢 playing, Apple paused; dispatch/observed metadata only |
| VERIFIED | Offline precondition | Wi-Fi0, Active default network none immediately before selection |
| VERIFIED, user reported | Audible WAV match | User answered “Yes, the correct song is audible” for02. 白日夢.wav through AMP15. Delayed reply, not an instantaneous metadata/acoustic measurement; artifacts/mango-ui-wav-audible-confirmation.json |
| FAILED | Initial Music browse returned cached root rows | Fixed-page assumption removed before file selection; not counted as success |
| FAILED | Early root recovery / animation race | UI_ACTION_TIMEOUT / changed child index; bounded settled navigation being corrected and retested |
| NOT TESTED | Full FLAC/WAV, SD removal/rescan/duplicates | Snapshot-scoped identity does not prove long-term native library identity |
| NOT TESTED | Either phone over BLE / background / security | Only DX340 connected locally; simulator/software tests are separate |

Raw source/UI details remain in ignored artifacts/. Public implementation documents contain no file contents or protected downloads. See mango-ui-experiment.md for artifact names and version-specific semantic restrictions.
