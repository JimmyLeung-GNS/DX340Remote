# Current phone source selection and listening —31 October2026

**VERIFIED/subsets; full goal ACTIVE/incomplete.** Installed original-signer Pixel APK b805e4ce…/source3f4e862 and DX340 Companion00e5e50b…/source7fbdec8. These are new current-build results, separate from the older66f CALL/OPENING VTR and source-local Intro evidence. [Full hashes, versions and evidence manifest](2026-10-31-current-phone-source-audio.json).

The owner explicitly confirmed fresh listening availability for a few current-build selections. All three selections below came from the real Pixel UI over BLE; none used the debug adapter to initiate playback. No volume command was sent.

| Source | Deliberate phone selection | Actual source and owner evidence |
|---|---|---|
| Apple Music | Downloaded →Songs →A・RA・SHI | Before selection, own debug receiver reads the complete public101-item Apple queue without playback: candidate absent. One phone PLAY_ITEM; actual Apple session reports A・RA・SHI/ARASHI playing and Mango paused. Owner confirms expected song audible. Apple then paused from phone. |
| Mango WAV | Local folders →Internal Storage →Music →Tayori →1st Live folder →03. 風のたより.wav | One phone PLAY_ITEM; actual Mango session reports 風のたより/tayori playing and Apple paused. Owner confirms expected track audible. |
| Mango FLAC | Fresh-root browse during WAV playback →different magic tour folder →03. ゴースト.flac | One phone PLAY_ITEM; actual Mango session reports ゴースト/tayori playing, Apple stays paused. Owner confirms expected track audible. Mango then paused from phone. |

Both endpoint connectivity dumps show **no active default network**. Wi-Fi remains off on both; the Pixel has the earlier recorded no-SIM configuration/mobile-data setting1. This is an actual no-network/no-SIM subset, not the separate strict SIM-installed cellular-off gate. No cloud, Wi-Fi control path, new player engine or Apple download extraction was used.

Bounded public local-file headers identify both Mango files as stereo48kHz/24-bit. WAV is RIFF/WAVE extensible with PCM subformat,24 valid/container bits/channel mask3; FLAC STREAMINFO has48kHz/2 channels/24 bits. Final header captures are4096 bytes and42 bytes respectively. Field interpretations follow [Microsoft WAVEFORMATEXTENSIBLE](https://learn.microsoft.com/en-us/windows/win32/api/mmreg/ns-mmreg-waveformatextensible) and [RFC9639 section8.2](https://www.rfc-editor.org/rfc/rfc9639.html#section-8.2). These metadata observations and audible playback do not establish DAC/output bit depth, bit-perfect playback or comparative audio quality.

## Actual decoded receipts

The current native model survives from the prior6-request b805 report. New export has34 requests, zero evictions; all6 old rows compare unchanged. The **28 new requests** are1 HELLO,1 AUTHORIZE,2 SELECT_SOURCE,14 BROWSE,3 PLAY_ITEM,3 GET_STATE,2 GET_ARTWORK and2 SET_PLAYING. Every new request has an applied RESULT, no error/timeout/loss. Existing source categories remain separate: apple/mango; no metadata is added to the native journal. Raw exports remain unchanged under ignored artifacts.

| Selection | Matched ACCEPTED | Dispatched terminal |
|---|---:|---:|
| Apple A・RA・SHI |135ms|229ms|
| Mango WAV |149ms|3521ms|
| Mango FLAC |159ms|3529ms|

These are native send-registration to decoded replies, **not GATT frame ACKs or acoustic timing**. Both Mango terminal results remain dispatched, not an observed-state timestamp. Source-state snapshots and explicit listening are separate. Pauses return observed: Apple599ms, Mango495ms. The analyser retains NOT_ADJUDICATED for T04; three selected tracks are not a p95 acceptance population.

For the WAV and FLAC pages, all sampled visible row rectangles compare unchanged before/pending/terminal; pending and consumed song actions disable while fresh-root browsing becomes available again. No disconnect or wrong-row selection occurred in this small sequence. This is sampled geometry evidence, not a continuous no-flicker assertion. The status can still say sent/waiting while later metadata shows the selected playing track; this remains an open UI outcome-message issue rather than a claimed failure or completion of command observation.

## Final state and remaining work

At16:18 HKT, both direct source sessions are paused2; stream volume37/headset(4), volume proof false. Four saved Companion preferences remain byte-hash identical to the earlier baseline, retaining Pixel/JIMMYAH grants and browsingON. Companion stays foreground, both Accessibility services bound. Phone normal Disconnect clears foreground/started ownership; Setup help is collapsed, approved player remembered. Both devices remain available over USB; font1.0, Wi-Fi0/Bluetooth1 and30-minute screen timeout retained.

No product code, package, signing key or user settings changed in this test turn. No new build/Apple CI job was required;178 JVM/72 Flutter/19 Swift and both closed3f4e862 Apple archives retain their original scopes. No observer, emulator or long-run test is active.

Current Android T07 is1/20 and T08 is2/20 in this clean package scope; full required albums/playlists, other formats/SD/output,100 commands/source, responsiveness, screen sleep, range/calls, battery/walk/ownership, actual owner-iPhone/Mac signing and later Watch remain unfinished. Next: fix truthful post-dispatch status without inventing observed completion or replaying playback, then continue the full current-package matrix. Do not transfer these results to any future APK automatically.
