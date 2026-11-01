# Current Pixel Apple Music offline selection matrix

**VERIFIED:** 20 distinct tracks selected through the actual Pixel Library UI over BLE, across **five downloaded albums and two playlists**. Each reaches the expected source track with PLAYING and advancing reported position, while Mango remains paused. This passes the current Android observed-playback subset of T07. Full both-phone, audio-quality and project acceptance remain incomplete.

Installed original-signer Remote0.2.0/code2 SHA256 `9f085bc22e8665b25dc886ee22cb6f2a977c002baa57c6f057c59fc2a243116e`, source `07c2029503ebb929f03c0a3cc9b421c29ef2455d`; unchanged Companion0.1.0/code1 `e1a024293c70e334de1e45c31176be6c19e4555fe97391e84f56b4515148b1f8`, source731aa64. Pixel9a Android16/API36 `CP1A.260405.005`; DX340 Android13/API33 `eng.iBasso.20260812.181952`; Apple Music6.5.3/code1599, Mango4.1.37/code240. The iPhone-only tint correction and its build evidence are [separate](2026-11-01-library-contrast.md).

| Current collection, revalidated through phone browsing | Selections |
|---|---:|
| AAA 2nd Anniversary Live - 5th Attack 070922 - Nippon Budokan | 3 |
| AAA 3rd Anniversary Live 080922-080923 Nihonbudoukan | 3 |
| AAA 10th Anniversary Best | 3 |
| AAA 15th Anniversary All Time Best -thanx AAA lot- | 3 |
| AAA a-nation2018 SET LIST | 3 |
| Favourite Songs playlist | 3 |
| AAA playlist | 2 |

Old inventory supplied navigation candidates only. Each case explicitly opened a fresh collection, observed its current heading and enabled song row, and sent one playback tap. Matching normalizes Unicode NFKC, case and whitespace, preserving punctuation/version suffixes. All15 album cases additionally match the requested album;17 cases have an independent AAA artist expectation. Favourite Songs artists were observed rather than independently predicted: Hitsujibungaku, Misekai and Liao JunTao. The playlist live-version suffixes remain part of the expected titles. [Structured results](2026-11-01-apple-selection-matrix.json) contain every selection, observed artist/album, position delta and native playback receipt.

The first selection, Kuchibirukara Romantica, is absent from the **full captured101-item** immediately preceding queue. Each case compares its expected title against the complete captured public queue, independently of the probe's candidate-membership flag. These are Library PLAY_ITEM requests, not queue skipping or PLAY_QUEUE_ITEM. Before and after every selection, **both endpoints** report no active default network and no ADB reverse; Wi-Fi remains0. Stored Pixel mobile_data1 is retained, so strict cellular-offT01 and physical iPhone remain open. No Apple download extraction, storage setting, mount, reset or replacement playback engine was used. Apple SD destination retains its earlier own-UI verification scope; mounted storage alone does not prove whole-library integrity.

All20 selected row tops and the fixed Browse toolbar stay unchanged across the sampled before/follow-up/final states. Nineteen selected rectangles match exactly. In case18, the partly clipped 谁 (Live) row keeps top1843 while its visible bottom changes1983→1980 with now-playing metadata. Do not turn that into a claim of identical rectangles or continuous-frame capture. All20 audio dumps show unmuted music volume37/headset(4); source screenshots and public state agree. Advancing SourceRegistry positions are estimates from PlaybackState position/speed/update-time. **Audibility and audio quality are NOT TESTED**; routine listening prompts were waived.

Before case10's playback tap, an unquoted parenthesis in a read-only diagnostic intent caused an Android shell syntax error. The worker stopped,14 read-only preflight files were copied, and POSIX argument quoting was corrected. The first nine completed selections were skipped when resuming; no playback tap was repeated. This driver failure is separate from source playback failure. The older frozen611/95 full101-item queue was also read-only rechecked: Kuchibirukara Romantica is absent there, so that historical absence remains supported without changing its evidence.

Native model `47719f25-1cfd-4c64-a9b3-c4aada9d5e00` grows **0→2→129**. The current two-row saved HELLO/AUTH baseline remains unchanged, zero evictions. The127 new applied results are78BROWSE, **exactly20PLAY_ITEM**,20GET_STATE,7 automatic GET_ARTWORK, one final Pause and one explicit return to Mango control. No new auth, skip, queue-play or volume command appears. The earlier5d/37-row pre-upgrade preparation is a separate model/build scope; no playback was attempted in that preparation.

For20 PLAY_ITEM requests, nearest-rank p95 is first submission4ms, ATT write response167ms, ACCEPTED169ms, dispatched RESULT2679ms. For20 automatic GET_STATE requests, ATT p95 is775ms and reply p95 is1166ms; there is no ACCEPTED sample for those reads. These are native stage timings under this observed selection workload, not UI-tap-to-audio timing. The read ACK sample misses the500ms target, and full T04 remains **not passed**. Investigate with a discriminating ordinary-control/read test; do not infer a cause or erase the result.

Final **08:14:41 HKT**: both sources PAUSED2 at37/headset(4), volume proof false; unchanged hostPID21079/epoch2159b3e5 and phonePID1355. Mango cursor/metadata, four preference hashes, settings, grants, host bonds, both Accessibility bindings and SD mountFBB9-E8FE are retained. Phone normal Disconnect/help collapse and targetMango complete; host foreground service stays enabled/advertising. All matrix/export/build workers are terminal. Raw `artifacts/apple-selection-matrix-20261101/` is frozen with **832 files**, manifest SHA256 `33e5464b5c65a430232899f6976c7a7a1c60dfec872c14fb5c4f82375fb0e000`. Do not mutate or replay it.

Next: current Android100 playback commands/source and isolated timing checks, then remaining full T01–T18/physical owner-iPhone/signing/Watch. Prior interrupted-link stalls, no-card Internal timeout and Apple MEDIA_MOUNTED crash remain open. Previous Mango20 and owner-audible results retain their original binary scopes.
