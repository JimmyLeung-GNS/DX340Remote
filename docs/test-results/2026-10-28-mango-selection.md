# Mango selected-item test — 28 October 2026

**VERIFIED: one user-confirmed local selected-track experiment. Full T08 is not passed.**

Device: DX340, Android 13/API 33, `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`. Installed Mango `com.ibasso.music` 4.1.37/240; companion 0.1.0 development candidate. Output: AMP15 phone out, user reported. Gain/listening level and file encoding not independently inventoried; companion did not adjust volume.

The user browsed the companion's ordinary Mango tree, selected a playable All Music item and confirmed the public playFromMediaId experiment. Saved requested item: `__ALL_MUSIC__5`, “777 (feat. Awich, CHICO CARLITO & Paledusk) 5”, ONE OK ROCK. Earlier ADB observed Mango as the media-button session playing “777 (feat. Awich, CHICO CARLITO & Paledusk)”, active queue item 6. Browse and queue indices need not match; no equivalence is inferred from those numbers.

When asked whether the selected song played correctly through Mango, the user answered **“Yes, the correct song is audible.”** The agent then located and pressed the app's confirmation control to save that human report. The agent does not claim to have heard the audio.

Timing limit: the later saved confirmation snapshot reported “CHEERS”. Time elapsed between dispatch, the earlier “777” observation and confirmation. The later snapshot is not proof of an instantaneous requested-title match. Retain the earlier session observation and explicit user answer as the evidence chain.

Mango remains the playback engine. This demonstrates one actual selected-item path despite absent ACTION_PLAY_FROM_MEDIA_ID flags. It does not establish offline conditions, FLAC/WAV coverage, folders/playlists, 20 selections, either phone's radio path, source switching, volume mapping, bit-perfect output or uninterrupted audio on host restart.

Raw ignored evidence: artifacts/after-local-play-attempt.txt, after-audible-confirmation.txt, mango-playback-tests.jsonl and ui-before-record.xml. Installed candidate hash is in status.md.
