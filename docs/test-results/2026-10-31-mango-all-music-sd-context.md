# SD freshness from Mango All Music —31 October2026

**VERIFIED two read-only Android cases.** [Exact versions and evidence hashes](2026-10-31-mango-all-music-sd-context.json). This uses hostf009/source2303807 and phoned755/source80d4edc before the later d166 availability change. No result is transferred to that later APK.

Starting with the existing SD list cached, the operator navigates Mango to All Music using its observed drawer. One uniquely named empty directory is then created at the SD root. Actual Pixel saved-grant Connect → Library → Local folders → SD sees the new leading directory. It is removed only after proving it is empty; the original root name set matches afterward.

Mango is again taken to All Music, then fresh actual phone browsing to SD omits the removed directory. The immediate native-page assertion ran during its transition and failed; PowerShell continued the following read-only phone sequence. That driver failure is retained. The actual browse's completed adapter trace records all-music-menu-click, proving the settled starting route; it is not inferred from that failed assertion.

Both SD results are applied replies,4462/3945ms terminal. Both exceed the proposed3s page target and are not GATT ACK/p95 or audio proof. The first SD-cache defect did not reproduce from this starting page. No additional navigation patch was made for this case. No track, scan or volume command was issued. One owned empty fixture was removed; no existing music file was edited. These eight requests are part of the11 new baseline records documented in the [following storage investigation](2026-10-31-sd-availability.md), not an independent model or extra control count.
