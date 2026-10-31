# Phone source changes clear Library and Queue —31 October2026

**VERIFIED paused Android UI subset.** [Exact versions, hashes, receipts and raw evidence](2026-10-31-phone-ui-source-scope.json). This case uses Pixel4318/source5f2de32 and Companion644799cc/source81b3f6b before the later f9 UI update. It is not promoted to that later phone APK.

The ordinary Pixel reconnects with its saved approval. All source changes use actual **Player → Control Apple Music / Control Mango Player** buttons. No source-local debug switch is used. The switches follow completed pages; this is not a pending-request interruption.

Mango Local folders → Internal Storage renders actual folder rows. Switching to Apple clears them and shows **Library changed. Browse again or refresh Queue.** Fresh Apple Downloaded root → Songs loads selectable rows. Switching back clears those rows and fresh Mango root browsing succeeds. Sampled old item rows are absent after both changes; no old opaque token was deliberately replayed over the wire.

Mango Refresh queue reports1–28 of28 (selection unsupported). After switching to Apple, those rows clear before refresh. Apple reports1–50 of101. The phone scrolls to **Next queue page**, which loads51–100 of101 and resets to that page's visible header. Switching from this second page back to Mango clears the old rows and page context; fresh Mango queue starts1–28 of28. The final one-item Apple page was not requested. This is not a full collection/title audit or queue playback test.

The export has51 requests in the same model as the prior35-row host-recovery export; all35 previous request records compare unchanged. **16 new requests are applied RESULTs:** HELLO, AUTHORIZE,6 BROWSE,4 SELECT_SOURCE and4 GET_QUEUE; zero evictions. Source switches take422/452/401/405ms; queue reads381/571/625/347ms. These are decoded reply durations, not GATT ACK/p95/T04 acceptance. SELECT_SOURCE journal source denotes the source before that request, not its target. No PLAY_ITEM, PLAY_QUEUE_ITEM, transport playback or volume command was sent.

Final state: both direct sessions paused2 at37/headset(4), same host/phone processes, four host preferences and settings unchanged, both browsing services bound, Companion foreground. Phone normal Disconnect clears foreground/started work and help is collapsed. Both USB devices remain available. No new build/listening/observer/long hold in this physical subset.

One clarity defect was observed: the eight visible Mango queue rows are correctly disabled/nonclickable but announce **Play**. The subsequent [read-only queue correction](2026-10-31-read-only-queue.md) addresses that wording on a different phone APK. Preserve these older raw captures as the baseline.

This advances actual phone UI context/pagination coverage. Continuous flicker, rapid taps, in-flight source switching, old-token wire replay, actual playback/audio/no-overlap and full T09/T12/T16/current two-phone/source/format/SD/output/reliability/signing/Watch remain unfinished.
