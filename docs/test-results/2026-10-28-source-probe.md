# Physical source probe — 28 October 2026

DX340, Android 13/API 33, `eng.iBasso.20260812.181952`. Companion identity `org.dx340.remote.host`; Mango 4.1.37/240; Apple Music 6.5.3/1599. No impersonation.

**VERIFIED reads:** native MediaBrowser accepted by Mango's exported services.MusicService. Root `/`: Now Playing, All Music, Album, Artist, Genre. Read-only traversal reached 17 albums, 11 artists, three genres and playable tracks (pages of 25). This is not audio or full folders/playlists proof. Sampled session mask 560, metadata and 28-item queue were readable. Reported max/current volume 0 is not actual amplifier volume.

**VERIFIED host callback only:** GATT service created and advertising callback succeeded; registered service visible in dumpsys bluetooth_manager. No physical central/security/offline pass.

**FAILED/denied Apple browse:** native MediaBrowser connection failed to exported player.MediaPlaybackService. Media3 1.8.0 connected, but getLibraryRoot returned -4 without a root. The [official SessionError reference](https://developer.android.com/reference/androidx/media3/session/SessionError) defines -4 as permission denied. This does not establish that every legitimate integration is impossible.

Reproduce with `adb -s d shell am instrument -w org.dx340.remote.host.test/org.dx340.remote.host.SourceProbeInstrumentation`, then retrieve files/source-probe.json. It sends no playback commands. Android RESULT_OK (-1) means the reporting runner completed, not that individual gates passed.

Raw ignored evidence: artifacts/source-probe-tree.json, physical-source-probe.txt, initial-media-sessions.txt, probe-media-sessions.txt, dx340-probe-01.json, host-advertising.txt. No private download directory was read; no accessibility service enabled.

NOT TESTED: phone BLE/security, Apple offline arbitrary selection, source startup/resumption, explicit transports, output volume/audio quality, background/battery/walking and library/SD changes.
