# Read-only queue guidance and action labels —31 October2026

**IMPLEMENTED; Android subset VERIFIED, full goal ACTIVE/incomplete.** [Exact APK/source versions, receipts, preservation and evidence hashes](2026-10-31-read-only-queue.json). Phonef9b899ef…/source2cf7c74 replaces4318; original-signer in-place install and pulled installed APK byte equality verified. Companion644799cc/source81b3f6b is unchanged. Pixel9a Android16/shared0.2.0 code2, DX340 Android13/Companion0.1.0 code1, Mango4.1.37/code240 and Apple6.5.3/code1599.

## Correction and software checks

Actual Mango queue rows are informational: canReadQueue=true, canPlayQueueItem=false. Their old Play accessibility label implied an unavailable selection. The shared UI now shows **Read-only queue. Choose tracks in Library.** If a source cannot browse, it instead says **Choose tracks on your player.** Informational queue rows announce **Queue track**; unplayable library entries announce **Unavailable**. Supported selection still announces Play. No capability, routing, transport, source adapter or wire behavior changed.

Guidance lives in the existing fixed status viewport, outside the song list. It cannot insert/remove a list row when capability/status changes. Its own scroll key resets on guidance changes. Four new widget cases cover Android/iOS styles with and without browsing, disabled-row taps emitting no command, supported selection still sending exactly one queue command, and unchanged row position across capability changes. The actual pre-fix run fails missing-guidance expectations. Post-fix harness cleanup errors (duplicate Remote disposal and late SemanticsHandle disposal) were corrected; failed logs remain. Final four targeted checks and full **76 Flutter tests/analyze/Android release/lint PASS**. Unchanged native Android180 JVM and Swift19 results retain their earlier scopes.

## Physical Pixel checks

The upgraded phone remembers its approved player, reconnects without pairing/new approval and reads Mango's28-item queue. The new explanation is visible and sampled rows announce Queue track while remaining disabled/nonclickable. Eight visible default-font row bounds match the older4318 captures exactly; this is sampled geometry evidence, not continuous no-flicker or rapid-tap proof.

System font scale temporarily changes1.0→2.0. The actual own activity configuration confirms2.0; the explanation and first disabled row remain visible, and the screenshot was inspected. The finally block restores1.0. No general dark/large/error/accessibility ship review or physical iPhone result follows from this one case.

Using phone source buttons, Apple queue reports1–50 of101 with selectable Play rows and no read-only hint. Its Downloaded root's unverified Artists category is correctly Unavailable/disabled. No track is selected. Mango is then restored; fresh queue returns1–28 of28 with the explanation and disabled Queue track rows.

One automation lookup initially sought the source button before scrolling it into view; the safe guard issued no tap/selection. Subsequent readonly-apple-* captures are still Mango and include one extra read-only GET_QUEUE. Only the explicitly asserted actual-apple-* captures establish Apple behavior. Those failed/mislabeled driver observations are preserved rather than counted as Apple proof. The later sequence stops on failed actions and scrolls only from fresh UI observations.

The new native model exports **nine applied RESULT requests**, zero evictions: HELLO, AUTHORIZE,4 GET_QUEUE (including the extra Mango refresh),2 SELECT_SOURCE and1 Apple BROWSE. Authorization877ms; queue reads360/338/552/339ms; source switches428/401ms; Apple browse2462ms. No playback or volume command, no terminal error/timeout. This model is separate from the [previous51-row export](2026-10-31-phone-ui-source-scope.md); do not merge them or reuse older audio as this APK's audio.

## CI and final preservation

[Shared iPhone job41192248278](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41192248278) is terminal SUCCESS at exact source2cf7c74. Artifact13039228009 ZIP SHA256 `d4c036b93b342ea5ce78ad3fadac9b1537260f14094a3a05b7f8ac27bcc0af72` is verified and safely extracted. Archive inspection confirms76 Flutter tests/analyze,12 capture guards, ordinary/Setup/browsing/handoff/volume/report-probe simulator builds and final ordinary rebuild/relaunch. The actual native UIKit export-sheet result matches its export model; the restored ordinary frame was inspected and shows Bluetooth unavailable/no track, not a connected fixture. Hosted macOS26.6.2/25G83, Xcode26.6/17F113, iPhone Air/iOS26.5 simulator. This is not owner signing, physical iPhone Bluetooth or audio. The run is closed; no further polling. Prior5f Apple jobs remain closed with their own scopes.

Four saved Companion preferences retain byte-identical hashes. Both devices' original settings, own permission grant/flag rows and exposed bond identities/types are preserved. Font scale is1.0 again; browsing modesON and Pixel/JIMMYAH approvals remain. Both direct source sessions finish paused2 at37/headset(4), Companion foreground with both Accessibility bindings, targetMango. Normal phone Disconnect clears foreground/started work and help is collapsed. Both USB devices remain present. No local build/emulator/observer/long hold/audio test remains active.

Automatic approval review blocked optional removal of two empty scratch directories; they remain. This does not block source, testing or checkpoint work, and no alternative deletion method was attempted.

Fresh listening availability for this build and a new30-minute hold remain unanswered; the earlier envision hearing remains unconfirmed. Full current two-phone/source/albums/playlists/formats/SD/output/100-command/latency/sleep/call/range/battery/walk/ownership, physical owner-iPhone/Mac signing and Watch requirements remain open. Current fixed-row UI/decoded requests are not audio, T04, full T09/T12/T16, force-stop/reboot or bag acceptance.
