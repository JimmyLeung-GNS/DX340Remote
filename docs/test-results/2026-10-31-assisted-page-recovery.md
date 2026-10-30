# Mango assisted page recovery after selection

31 October 2026, Hong Kong. Full goal ACTIVE/incomplete. Follows [the physical selection report](2026-10-31-mango-phone-selection.md); its three listening confirmations and offline subset retain their package scopes.

## Cause and change

**Verified in source:** installed MangoAccessibilityService.play performs the validated source action and sets `last = null`. The old action IDs, including Back, can therefore fail STALE_LIBRARY after a successful selection. The client previously kept that page actionable. This explains a deterministic stale-page path; it does not prove the cause of the owner's original silent-playback/disconnect sequence or error133.

Kotlin and Swift now bind a Mango assisted PLAY_ITEM to its request ID, actual source package, source epoch and displayed revision. On terminal reply, timeout/send failure or connection loss, only that matching page is marked invalidated. ACCEPTED is not terminal. A newer page, source or epoch is preserved, unrelated replies cannot consume the selection, and duplicate/late completions cannot re-retire another page. Admission requires `com.ibasso.music`, a `mango-ui:` item, assisted context and equal nonempty requested/displayed revisions. Native Mango albums and Apple selection retain their existing behavior.

The shared phone retains the expired rows and scroll position, disables their song/folder/Back/page actions and shows **Page expired after selection. Tap Browse source library.** in the independent status viewport. Fresh root browsing, source switching, Player and Queue controls remain available according to capabilities. A fresh page removes the marker. No playback retry, automatic source navigation, host security relaxation or protocol wire change is added. Retained native reference clients also expose the recovery and enforce the native stale-page guard.

## Checks

- **Verified:** native Android assembly/lint and164 aggregate JVM tests (protocol46/cache7/adapters111), zero failures/errors. Two new JVM tests consume11 shared lifecycle and7 eligibility cases; Swift equivalents are implemented but require the new Apple CI result.
- **Verified:** full shared analysis and56 Flutter tests, including expired-page action rejection/fresh-root recovery and held-pointer/geometry checks under Android and Cupertino configurations.
- **Failed, retained:** initial test assertions counted startup snapshot/capture reads, compared a record containing a map by identity, and failed to clear the platform override before the test invariant. Corrected fixture bookkeeping passes; these are not product regressions.
- **Failed, retained:** a release rebuild reached R8 but generated classes.dex was locked by another process. A subsequent exclusive read confirmed the lock had cleared; no destructive cleanup or process kill was used. Retry/build/package outcome is recorded in the follow-up below.
- **Verified:** shortened-copy Android release retry and lint pass after the generated-file lock cleared.
- **Pending:** confirmation captures, physical Mango page-expiry/fresh-root recovery, and current-source native/shared Apple CI. No new audio acceptance is claimed yet.

Private logs/captures/packages are under ignored `artifacts/assisted-page-recovery-20261031/` and `artifacts/browsing-captures/`. The first capture round is explicitly synthetic, contains no BLE actions and was restored to the ordinary307971ce… package. Its long recovery sentence clipped in the large initial status viewport; one copy-only shortening is being confirmed. The fixture predates the final native eligibility correction, but has the same UI behavior and cannot send Bluetooth controls. It is not native protocol/runtime evidence.

## Owner preference and actual settings

The owner asked why Mango browsing had been restored off and whether it could remain enabled. The agent explained the prior test-only permission and the awake/unlocked, Accessibility and app-update limitations; ongoing enabled browsing is now the recorded preference. This supersedes the previous test-only cleanup default. **Verified:** Mango app mode true and Mango Accessibility enabled through visible Companion/Android Settings; Apple service remains enabled. The service configuration filters Mango's package. Controller approvals and bonds are preserved. Before any new playback, both actual source sessions were paused2 at output37; no volume command was sent.

Keeping optional browsing enabled does not make screen-off source navigation supported or remove pairing/local approval. The full physical Android/iPhone/source/format/SD/reliability/background/signing/Watch matrix remains unfinished. Both previous background attempts remain INCOMPLETE.
