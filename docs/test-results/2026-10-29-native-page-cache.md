# Native Mango metadata page cache

29 October2026, Hong Kong. **VERIFIED at native software and actual DX340 source-local read-only scope.** This continuation follows2c119fe. It closes the missing production cache wiring for native browser pages; phone BLE, full Mango20/format/SD/listening, source UI navigation latency and persisted host indexing remain separate work.

Device d: iBasso DX340 Android13/API33, build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys; Mango4.1.37/code240, Apple6.5.3/code1599. AMP15 phone out remains user-reported. Both source sessions stayed paused at37. Wi-Fi0/no active default network. No playback, volume, source file, playlist, network, permission or firmware change was sent by the new diagnostic. HiBy remains stopped from the preceding authorized isolation test; companion PC approval absent/OS bond retained.

## Implementation and contract

SourceRegistry now uses NativeBrowsePageCache over the existing LibraryIndex. At most200 pages of25 lightweight rows are retained in memory. Rows contain bounded display text, opaque source IDs, flags and a SHA256 identity fingerprint; no artwork, audio or protected download contents are copied into this cache. Keys include source, source epoch, package/build version, parent and page. Entries expire at10 seconds using Android's monotonic clock. Cursors and row ordering are preserved, including empty terminal pages.

Only a non-root native request carrying the current revision can reuse a page. Current authorization, permission, source epoch, connected browser and browsable-parent checks run before lookup. Every cached row must still have the matching live item/proof map entry; cache never recreates evicted playback proof. Playability is rendered from current native capabilities. Root refresh, source switching, session replacement/destruction, permission loss, host shutdown, browser failure/suspension and observed identity changes invalidate the relevant pages. A page may display metadata up to10 seconds old; it is presentation, not evidence of a current selectable file.

Both native PLAY_ITEM validation reads still pass a null expected revision and therefore query the source afresh, before and after source handoff. The ten-second playback validation deadline, fingerprint/epoch checks and final dispatch guards remain. Duplicate source IDs within one page are rejected as SOURCE_ID_AMBIGUOUS rather than issuing ambiguous rows. Apple/Mango Accessibility browsing bypasses the cache entirely and continues validating current window, page and action identities. Protocol v1 adds optional cached/cacheAgeMs native response fields; existing clients ignore them. Root refresh remains the existing user refresh action.

The public Android subscription callbacks can deliver loaded or updated children. Our bounded per-request subscriptions unsubscribe on completion; this cache does not claim to monitor every later source-library change. [Android MediaBrowser callbacks](https://developer.android.com/reference/android/media/browse/MediaBrowser.SubscriptionCallback), [MediaBrowser subscription lifecycle](https://developer.android.com/reference/android/media/browse/MediaBrowser).

## Build, tests and update

VERIFIED scripts/build-android.ps1: all native debug APKs, instrumentation compilation, lint and108 JVM tests pass (including five new production-cache tests). The new tests cover cache bypass for fresh validation, expired/backwards clock, missing identity proofs, source/epoch/package/parent/page isolation, invalidation, immutable input snapshot/cursors/empty pages,10,000 metadata rows with200-page LRU capacity and metadata bounds/duplicate IDs. The first build failed a new test's incorrect LRU eviction expectation; correcting that expectation produced the passing final run. Logs: artifacts/native-page-cache-build.txt and native-page-cache-build-final.txt. This was a test expectation correction, not a hidden device success retry.

Original signing certificate matched before adb install -r. Installed bytes match **04846d60bc4e65065028d510286b56cba91577840a994d351146c55dff418fa2**. Four companion preference hashes remain identical; both optional Accessibility services are enabled/bound with no binding/crashed entries. Evidence: native-page-cache-signing.txt, native-page-cache-prefs.json and native-page-cache-accessibility.txt under artifacts/. Host restart clears memory-only volume proof as designed; remote volume remains disabled pending a fresh local check.

Shared phone code/package is unchanged: c24be4c application source, Android APKb12f83e4145660bf4129482820e893b329a3c8aacfcafe75fc90520a5cfb1d20, preceding Flutter42 analysis/test/release/lint and hosted iOS40857287517. Native Swift source is unchanged from its passing reference workflow40850675500. No new phone UI or iOS build claim follows from this Android-host change. Earlier20 Apple audio confirmations remain scoped to their recorded mixed builds, not this newly installed host.

The isolated scripts/verify-library-metadata.ps1 check also passed after correcting its output disclaimer to distinguish LibraryIndex component timing from the newly wired production cache. It still walks10,000 synthetic tracks/400 pages; first cached component1.0629ms, first uncached14.2434ms, maximum encoded page5,014 bytes. The standard artifacts/library-metadata/ output now contains this latest run; a copy is preserved in artifacts/native-page-cache/library-metadata-rerun/. Earlier documented numbers remain historical. This benchmark is not an actual DX340/phone/cache-wire timing result.

## Actual read-only checks

scripts/verify-native-browse-cache.ps1 passed on the installed DX340. Reports are in artifacts/native-page-cache/20261029-111054-0f8fd700/; top-level transcript native-page-cache-physical.txt. Native All Music pages contain25 rows, with identical metadata/order/cursors between cold and cached responses. Timings use the debug receiver's monotonic elapsedMs, excluding the outer ADB report read; these are individual samples, not p95 or real phone latency.

| Read | Cache used | elapsedMs | Observed behavior |
|---|---|---:|---|
| All Music page0 first read |No|190|25 rows, next1|
| Page0 repeated |Yes|85|Same25 rows/cursor, cache age404ms|
| Page1 first read |No|202|25 rows, next2|
| Page1 repeated |Yes|71|Same25 rows/cursor, age402ms|
| Page0 without expected revision |No|159|Fresh source read using playback-validation argument style; no PLAY_ITEM sent|
| Page0 repeated after fresh read |Yes|64|Age397ms|
| Page0 after root refresh |No|157|Root refresh dropped cached pages|
| Page0 after Apple/Mango target switch |No|110|Source switching dropped cached pages without starting either source|
| Page0 after10.1-second wait |No|198|Expired page fetched anew|

Four current-build source-local pending-interruption regressions also passed: Apple sleep UI_SCREEN_UNLOCK_REQUIRED1044ms, Apple source-change AUTHORIZATION_DENIED693ms, Mango sleep1106ms and source-change556ms. Every old token returned STALE_LIBRARY; fresh Songs6/Internal Storage9 pages recovered, both sources paused. Transcript native-page-cache-assisted-regressions.txt; copied reports native-page-cache-assisted-reports/. Preceding toolbar-build interruption reports were preserved separately in apple20-final-interruption-reports/ before the fixed-output helper ran again. These are adapter/cancellation checks, not live controller revocation or phone radio tests.

Final read-only GET_STATE confirms selected Mango, both sessions paused37 and volume verified=false. Wi-Fi remains0/no active default network. The companion MainActivity was reopened after the navigation checks. Evidence: native-page-cache-final-state.json and native-page-cache-final-companion-launch.txt under artifacts/.

## Remaining verification

This cache does not solve the slower optional Mango folder navigator, create a persistent filesystem index, scan SD cards or expose unsupported playlists. Observe populated source playlists before adding their adapter. Full20 FLAC/WAV selections through Mango, actual file/SD/rescan changes, real phone cached-page/render latency, playback revalidation over BLE and audio/quality/background/range/battery/walking remain open. A cached list must never be substituted for those results. Continue the physical phone runbook when separate devices/Mac/signing are available.
