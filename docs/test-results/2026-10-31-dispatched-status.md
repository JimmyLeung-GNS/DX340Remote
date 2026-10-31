# Dispatched playback status —31 October2026

Full goal **ACTIVE/incomplete**. App source `5f2de32ca58952faf401d0d558755972d4e160ba`; observer tooling `b356f5f31ed84f0355182a149b0d3e17c6d865d7`. [Build identities, timings and evidence manifest](2026-10-31-dispatched-status.json).

## Correction

Both native phone bridges previously left “waiting for observed playback” visible even after a dispatched selection's follow-up read completed and later source metadata changed. The native journals correctly retained dispatched outcomes; the status wording was misleading.

Kotlin and Swift now show **Playback request sent.** A matched current-host GET_STATE RESULT with a state payload changes that message to **Player state refreshed.** only while the current status is still the dispatch message. Error and library-transition notices are not replaced. The separate observed outcome continues to show **Playback state confirmed by source.** A refreshed state does not assert that the earlier command succeeded or became audible. No retry, playback replay, transport, request budget, source routing, journal outcome or wire-format change was added.

## Physical check

**VERIFIED/message and source-state subset:** original-signer Pixel4318e6ad…/shared0.2.0 code2 is installed in place and re-pulled byte-identical; DX340 Companion00e5e50b… remains unchanged. Pixel9a Android16/API36 CP1A.260405.005; DX340 Android13 eng.iBasso.20260812.181952, Mango4.1.37/code240, Apple6.5.3/code1599. Original certificate24f48aa3566dcf7b65774ee12585191a8a0df9eeff9cc49e9e72c22bacca6e8d retained.

Saved approval/cached metadata survive. Fresh phone Local folders navigation to Tayori's magic-tour folder selects `01. envision.flac` exactly once. Actual Mango reports envision/tayori playing, Apple remains paused. The phone shows **Player state refreshed.**; the old waiting message is absent. All four visible row rectangles remain identical before/pending/terminal. Pixel Pause then shows the distinct source-confirmed message. Audio confirmation was requested retrospectively and is **pending**, not inferred; the three earlier b805 listening confirmations retain their earlier APK scope.

An actual exported journal has11 requests, zero evictions: HELLO, AUTHORIZE,6 BROWSE,1 PLAY_ITEM,1 GET_STATE and1 SET_PLAYING. Selection terminal3311ms remains **dispatched**; follow-up state353ms has no observed command outcome; pause485ms is **observed**. All are applied results. This verifies that the new presentation does not relabel the dispatched command in diagnostics. Timings are not GATT ACK/acoustic measurements or T04 acceptance.

Final16:38 direct state: both sources paused2, volume37/headset(4)/proof false. All four saved host preferences compare byte-hash unchanged; Pixel/JIMMYAH grants and browsingON retained. Phone normal Disconnect clears foreground/started ownership, Setup help collapsed. Both USB devices remain available. No new volume command or owner settings change.

## Checks and next steps

Native/reference build/lint and178 JVM checks PASS (53 protocol/3 controller JSON/7 cache/115 adapters). Shared Android analyze/release/lint PASS; no unchanged Dart tests were rerun locally for this native presentation correction. Existing routing/unknown-host/duplicate/privacy checks retain their scopes; no tests merely mirror new strings. Native Apple41184412035 is SUCCESS,19 Swift/12 capture guards/build/install/launch, artifact13035817739/archive SHA256 `3f256433f0b015d946255be3284b62f87977177a030a1efc2f62ef4476462436` verified. Shared41184412049 at5f2de32 is also SUCCESS:72 Flutter tests/analysis,12 capture guards, ordinary and fixture builds, actual native UIKit report-sheet probe and ordinary-app restoration. Artifact13036224709/archive SHA256 `152e319cbdc459464d75ce508a24a9977772092b5e6a66decfab63a995b49b2e` verified. Both jobs are terminal; no CI polling is needed. Prior3f4e862 jobs remain closed.

The USB observer now optionally requires `-ExpectedDxWakefulness Asleep` or Dozing and records the requirement in every sample/summary. Default Any preserves the old awake case; it cannot become a screen-off pass. PowerShell syntax passes. A live read-only negative preflight correctly rejects an actually Awake DX340 with required Asleep after one sample/2.578seconds. No30-minute screen-off hold started; fresh owner readiness is pending. See [reliability runbook](../phone-reliability-runbook.md).

Next: both Apple jobs are closed/archived. Perform the separately agreed screen-off hold if the owner becomes available, and continue the full current-build/source/offline/format/playlist/SD/output/command/latency/interruption/battery/walking/ownership matrix. Actual owner-iPhone/Mac signing and Watch gates remain open; Watch follows stable phones. No user pause or device-switch request occurred.

A later current4318 permission-loss/recovery subset is separately [verified here](2026-10-31-4318-permission-recovery.md); its new model session is not pooled into this pre-revocation11-request export.
