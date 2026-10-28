# Mango public file identity and external-open investigation

28 October2026; DX340 Android13/API33, build eng.iBasso.20260812.181952; Mango4.1.37/code240; companion0.1.0 debug. Read-only probes through the ordinary companion MediaBrowser client plus Android package intent resolution. No playback command, file read, firmware change, vendor contact or alternate player launch.

| Probe | Result | Evidence |
|---|---|---|
| Returned MediaDescription for __ALL_MUSIC__1 | VERIFIED read: title “+Matter 1”, artist ONE OK ROCK, mediaUri=null, extras empty | artifacts/mango-identity-__ALL_MUSIC__1.json |
| Returned MediaDescription for __ALL_MUSIC__2 | VERIFIED read: title “+Matter 2”, same artist, mediaUri=null, extras empty | artifacts/mango-identity-__ALL_MUSIC__2.json |
| Returned MediaDescription for __ALL_MUSIC__5 | VERIFIED read: title “777 …5”, ONE OK ROCK, mediaUri=null, extras empty | artifacts/mango-identity-__ALL_MUSIC__5.json |
| VIEW intent resolution | FAILED to find an advertised Mango route for tested cases | audio/flac, audio/x-flac, audio/wav, audio/x-wav with file/content schemes; query-only nonexistent diagnostic URIs. artifacts/mango-view-resolver-probe.json |
| Mango manifest filter report | VERIFIED no advertised ACTION_VIEW filter in this installed package dump | artifacts/mango-package-filter-probe.txt; no explicit unadvertised Activity intent was launched |
| Native browser folder/playlist categories | NOT EXPOSED in sampled root | Root still Now Playing/All Music/Album/Artist/Genre; artifacts/mango-identity-root.json |
| Stable identity after rescan/SD or duplicate-file reorder | NOT VERIFIED | Current guards compare source ID/flags/title/artist/URI and page snapshots. These three descriptions do not expose a stable file URI. Display index and matching labels cannot prove underlying file identity if indistinguishable files reorder |

This does not invalidate the earlier audible selected-track test or observed current-library selection; it limits the claims about formats, folders and stale IDs. No filename extension/MIME/file format can be inferred from these sampled labels. Full FLAC/WAV/SD acceptance remains incomplete.

Preserve Mango and its supported native browsing. Further options require evidence: a documented stable Mango file/playlist interface, or a separately accepted optional Mango UI-assisted folder mode with screen-on/unlock and app-update tradeoffs. The Apple Music UI decision does not silently authorize adopting Mango UI browsing. A replacement audio engine remains a separate explicit decision. See mango-interface-question.md for a prepared question the user may send; no vendor has been contacted.
