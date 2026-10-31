# Current pair source selection —1 November2026

**VERIFIED: source-state, BLE receipt and sampled row-geometry subsets. Human listening confirmations remain PENDING.** [Structured result](2026-11-01-current-pair-source-selection.json). Full acceptance remains incomplete; older explicit hearing answers retain their original APK scopes.

DX340 Android13/API33 `eng.iBasso.20260812.181952`, Companion0.1.0/code1 SHA256 `61153223b7a37d5ed3ac293455478d4199cf4fed4b2f4282fb21e5f03915a215`, source9fb503a/PID10544. Pixel9a Android16/API36 `CP1A.260405.005`, shared Remote0.2.0/code2 SHA256 `95d29be69f09e90b978a6907e46d2ec9952b5a0676ccd0a7da0d36fa54d06bca`, sourcee40b822/PID25153. Sources remain Mango4.1.37/code240 and Apple Music6.5.3/code1599. No app rebuild or update occurs within this listening batch; the preceding same-signer host update and injected timeout have [separate evidence](2026-11-01-injected-indication-recovery.md). No fault is armed during these selections.

The owner explicitly answered **Yes, I can listen now** before this batch. Each selection below used the real Pixel UI over BLE, once; no debug adapter dispatched playback. Three separate expected-song questions were presented while their corresponding source reported playing. None has an answer at this checkpoint. Source metadata/state3 alone does not establish audible or correct audio.

| Selection | Actual observed source result | Listening |
|---|---|---|
| Apple Downloaded →Albums →AAA 2nd Anniversary Live - 5th Attack 070922 - Nippon Budokan →Kuchibirukara Romantica | Expected AAA song PLAYING3, Mango PAUSED2. Candidate absent from the captured complete previous101-item public queue. Apple paused deliberately afterward. | Pending |
| Mango Internal Storage/Music/Tayori/1st Live folder →03. 風のたより.wav | 風のたより/tayori PLAYING3, Apple PAUSED2. | Pending |
| Fresh-root browse during WAV playback →different magic tour folder →03. ゴースト.flac | Immediate pre-selection media dump confirms WAV PLAYING3; afterward ゴースト/tayori PLAYING3, Apple PAUSED2. Mango paused deliberately afterward. | Pending |

An existing album helper combined navigation with its one phone Play tap. That selection occurred before the intended separate play step; it was disclosed and not sent again. The previous101 queue was captured before album navigation, and the native journal confirms only one Apple PLAY_ITEM in this batch. Connectivity was captured **during** Apple playback, with no active default network on either endpoint; an immediate pre-play connectivity capture was not made. Both Wi-Fi flags remain0 and Pixel mobile_data1 remains unchanged. This is a sampled no-active-network source-state subset, not continuous network proof, strict cellular-off T01 or a hearing pass. Apple download settings/card mounting were not changed.

The Music and Tayori driver lookups each stopped without a tap because the desired folder was below the viewport. Fresh inspection and explicit scrolling exposed both. Actual Internal/Music browsing succeeded; these are driver offscreen-search failures, not new source browse failures. No failed playback, duplicate selection or lost BLE connection occurs in this batch.

Three WAV rows and four FLAC rows compare exactly before/pending/final, including the partly visible fourth FLAC row. The third row was selected in each case. Selected-page actions retire/disable while geometry remains unchanged; a fresh root enables navigation to the next folder. This is a default-font sampled regression, not exhaustive touch/scroll or a new first-row playback acceptance cohort. No fresh file-header/output-quality comparison was performed; older48kHz/24-bit header observations keep their earlier scope.

Native model `0baf310c-e141-4cfb-95f4-c16c8b7d459b` retains all19 preceding rows exactly and grows to48, zero evictions. The29 new requests are HELLO1, AUTHORIZE1, SELECT_SOURCE2, BROWSE17, PLAY_ITEM3, GET_STATE3 and SET_PLAYING2. All have applied RESULTs with no error. No NEXT/PREVIOUS/SEEK/ADJUST_VOLUME/queue-play command was sent. Three selected-item receipts remain **dispatched**, separate from subsequent observed states and hearing:

| Selection | Last ATT write callback | ACCEPTED | Dispatched terminal |
|---|---:|---:|---:|
| Apple album song |83ms|90ms|1,505ms|
| Mango WAV |90ms|116ms|5,351ms|
| Mango FLAC |79ms|85ms|4,933ms|

These software timings are not acoustic latency, observed-state timing or T04 p95 proof. Seventeen successful browses and three selections do not close20-per-source/phone, five-album/two-playlist,100-control or full fault/reliability acceptance.

Final04:11:50 HKT snapshots preserve all four preference hashes/global settings, PIDs10544/25153, three approved controllers and OS bonds, both optional browsing modes and Accessibility bindings, mounted FBB9-E8FE card, volume37/headset(4)/proof false. Both sources PAUSED2; source titles/positions intentionally changed by these authorized selections. Pixel normal Disconnect/help collapse clears foreground ownership; Companion main restored/host foreground. No network, power-policy, storage, private-download, file, grant or volume-setting action occurred.

Raw evidence: `artifacts/current-pair-audio-20261101/`,223 SHA256-manifest entries, fresh phone UI, source media snapshots, public queue, connectivity, native report and analyser. Consume late owner answers against their exact Apple/WAV/FLAC questions, retaining pending/negative answers honestly; do not substitute historical listening answers or replay a song to fill a missing result. Physical iPhone remains unavailable now by owner confirmation. Original failed holds/arming5024, no-card Internal timeout and Apple MEDIA_MOUNTED crash remain separate unresolved results.
