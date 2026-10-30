# Library target movement and connection investigation

31 October 2026, Asia/Hong_Kong. Application correction source **42b68abe0a1bb6e988a5f700daa4d8e3100b12b4**. Full goal ACTIVE/incomplete.

## Report and reproduced defect

The owner reported a successful first album/song selection, then Library rows suddenly moving upward under a finger, an unintended second-row selection, later selections visible on DX340 without playback, and eventual connection loss. The exact source/path and original command sequence are not yet confirmed. This report is evidence of an owner-observed failure, not a completed playback or reliability test.

A deterministic Flutter regression reproduced changing status/pending text moving the first song from y260 to y452 on Android, and y251.7 to y443.7 on Cupertino: **192 logical pixels** in each configuration. The text returned upward when the temporary message disappeared. This proves a presentation defect consistent with the reported jump; it does not establish the cause of failed source playback or disconnect.

## Correction and software checks — VERIFIED in stated scopes

- Library and Queue now put live status/error/pending text in a separate, independently scrollable viewport above the music list. Its height follows three scaled body lines plus24 logical pixels, capped at35% of available height. Messages remain complete and scrollable; wrapping and pending transitions cannot reflow the song list. Player and Setup retain their existing presentation.
- Row widget identity includes source scope, rendered revision and item ID. A held tap is canceled when its page is replaced. Library selection captures the displayed revision/scope and checks current playable row membership before sending; it cannot attach an old row to a newer page's revision. Existing authorization/capability/mutation and native source/epoch checks remain.
- Pinned Flutter3.41.4/revisionff37bef603469fb030f2b72995ab929ccfc227f0 analysis passes; **51 Flutter tests pass**. Five new tests cover Android/Cupertino geometry, status/error/pending transitions, status changes during a held first-song tap, page replacement during a held tap, and model rejection across revision/page/source-epoch changes. Existing negative assisted-mode tests retain valid page identities so they still exercise their intended capability guards.
- Actual Android release packaging passes. Initial lint and one ordinary retry failed on generated Windows path escaping/cached lint evidence. After normalizing only ignored local.properties, forced lint passed using `:app:lintRelease --rerun-tasks -x :app:compileFlutterBuildRelease`; the excluded Flutter compilation had already succeeded for the unchanged production source. No lint baseline or rule suppression was added. The first regression run failed as expected; a subsequent test harness cleanup error was corrected before the final51-test pass.

## Installed package and rendering — VERIFIED in stated scopes

On Windows11 Pro10.0.26300/DESKTOP-T9ICUE4, both USB devices were freshly inventoried: DX340 Android13/API33 and Pixel9a Android16. The existing external original development key was available. Installed baseline hashes initially matched companion58314b84… and phone2dce4f99…. The retained native reference clients remain in source.

The Pixel was upgraded **in place**, with signature comparison before installation. New ordinary shared phone0.2.0/code2:

`208a797e778b6af181341bc7b9d860670d996fb105eadd00101aa239536c17bc`

The installed APK was pulled and its complete bytes reverified. Original certificate24f48aa3… matches. No uninstall/data reset or signer replacement. Companion remains:

`58314b84cd45447fd866c1ea9a48922c39edd9584f14100c47cd3cb8c91ee97e`

Four actual Pixel disconnected Library captures (light/dark, font1.0/2.0) were inspected. A separate finish reviewer returned **ship only for those four captures**. This does not cover connected Library/Queue, long combined error/pending scrolling, actual TalkBack/VoiceOver, iPhone rendering, BLE/audio or full UX-01. Font1.0/night mode no were restored. No synthetic app was installed for these captures.

The shared iOS simulator workflow [41107018018](https://github.com/JimmyLeung-GNS/DX340Remote/actions/runs/41107018018) was observed **in progress** at42b68ab. Do not infer success or physical iPhone/signing support from that observation; inspect its terminal result.

## Separate connection investigation — PARTIAL

On arrival the Pixel's actual Library screen showed **“Encrypted pairing failed (133). Open pairing on DX340 and retry.”** Both source sessions were observed paused, music output37/headset(4). Optional Mango mode remained off; Apple mode on. Relevant app command logs for the original failure were not retained in the available logcat capture.

The companion's persisted event report contains OS bond removal/associated approval revocation at01:15:41 and01:15:57. Its export is a historical event snapshot, not a fresh source-state read. Current preference inspection found **one saved approval, JIMMYAH**; the Pixel's prior approval was absent before any repair action here. Pixel Bluetooth history separately contains a GATT response timeout at01:51:10. These facts do not establish why the owner's original playback attempt failed, or whether manual pairing changes contributed. No causal claim against a source adapter is made.

The Pixel's ordinary **Setup → Disconnect** was executed and its disconnected status verified. Its unfinished handoff Wi-Fi cleanup was completed: original enabled setting restored from0 to1. No offline acceptance follows from subsequent network-enabled diagnostics.

After the update, one fresh Find/Connect with a visible companion pairing window reached **ENCRYPTED_SUBSCRIPTION**, HELLO/authorization and explicit **LOCAL_APPROVAL_REQUIRED**, rather than another133 failure. The companion identified a pending controller; the Pixel displayed the matching local-approval recovery instruction. That attempt then reached the host's bounded **BOOTSTRAP_TIMEOUT** without approval; no authorized state/browse/playback pass on208…. The requesting row was no longer available. The Pixel still showed the earlier approval guidance in the observed screen; timeout guidance/closure propagation needs further checking. Ordinary Pixel Disconnect was executed again and its disconnected status verified. A fresh Find/Connect is required before owner approval/retry, which remains **PENDING**. Existing OS bonds were not removed, and no controller revocation test was run. All four companion preference XML structures remained equal to this turn's preflight after the upgrade/reconnect request; controller preferences remained equal at final cleanup. Restoring a missing approval must be recorded separately from that equality check. Final both sources were freshly observed paused, Pixel Wi-Fi1.

## Next actions and evidence boundaries

1. Finish identified Pixel local approval and Retry authorization; inspect actual state. Preserve JIMMYAH and other bonds. A successful protected bootstrap is not an authorized full session.
2. Confirm the owner's failing source/browse path. Reproduce one deliberate selection at a time with captured status, source state and GATT events; do not replay an unknown-outcome action or queue multiple trial selections. Verify the moving-row correction on connected Library/Queue and independently diagnose playback/connection failures. Human audio confirmation remains separate.
3. Inspect terminal iOS CI/captures, long-message scroll access and accessibility. Complete UX-01 setup hierarchy, Companion presentation and original icon assets; the owner chose **connection and setup clarity** as the first focus. The larger redesign/icon task is not complete.
4. Both earlier background observations remain INCOMPLETE, including the1779.414-second run. No new30-minute, iPhone, SD/rescan, audio-quality, volume, walking/battery or Watch acceptance is supplied here.

Private raw evidence is under ignored `artifacts/library-selection-20261031/`: original device logs/diagnostics/bluetooth dumps and APKs, expected failing geometry, final software/build/lint/signing logs, installed-byte/preference comparison, four screenshots and workflow handle. Addresses, serials, signing material and raw private logs are not published. The adjacent JSON contains the sanitized summary.

## Terminal follow-up

Job41107018018 subsequently completed SUCCESS at42b68ab with51 Flutter tests/build/captures/ordinary restoration. Artifact13002927005 SHA256673b4d4f6aaad4c2a5af47e829fc5a583779f0c34beeb2cc0d42e83027011989 was downloaded and verified. Pixel approval was later restored by its owner. The newer installed765…/f5f5a03 correction, separate three listening scopes, no-SIM offline subset and final cleanup are in [the next report](2026-10-31-mango-phone-selection.md); this report's initial pending/failed observations remain historical evidence.
