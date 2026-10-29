# Synthetic metadata component check, 29 October 2026

Historical component benchmark scope below is unchanged. A later production NativeBrowsePageCache now uses LibraryIndex inside SourceRegistry; its implementation and separate DX340 results are in 2026-10-29-native-page-cache.md. The component timing below is not a production-host timing result.

**VERIFIED at Windows JVM component scope.** No Android device or music app participates in this check. The full source/phone performance gate remains open. This adds the implementation plan's previously missing metadata fixture; the preceding test used10,000 integer IDs only.

Environment: Windows11, Android Studio JBR21.0.7, existing Gradle8.14/Kotlin toolchain. The product APKs are unchanged from the pending-Accessibility report (companion8ab23122..., shared phone09aaae78...). Native aggregate results now contain77 passing tests: preceding74 plus three new metadata correctness tests. The targeted library-cache suite and benchmark were run after the final fixture edit. Preceding Android build/lint and Flutter analysis32 tests/release/lint retain their recorded scope; they were not unnecessarily repeated for test-only changes. Current Swift still needs a Mac build.

## Reproduce

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
./scripts/verify-library-metadata.ps1
```

The script runs :library-cache:test and :library-cache:metadataBenchmark. Outputs are ignored artifacts/library-metadata/build.txt, summary.json and synthetic-tracks.json. The deterministic10,000-record fixture SHA256 is410cd390327271fb167e15167f83f700d760238eaafdefc079a6ee8639336141. It contains invented opaque IDs, duplicate filenames, multilingual/escaped/long titles, sparse WAV artist tags and FLAC metadata. It opens no source files or playback services.

Tests verify all400 pages of25 records retain exactly10,000 ordered unique IDs, each encoded envelope fits65,536 bytes, terminal pagination, Unicode JSON round trips, real protocol framing/reassembly at20/185/512-byte capacities on sampled pages, LRU recency/eviction with200-page capacity, source/revision separation, invalidation and rejection of replaced/missing selection identities despite unchanged display titles. Invalid51-row cache writes reject.

## Measured component results

The generated source is already in memory. Uncached assembly uses a new LibraryIndex, takes the first page, updates the real RevisionGuard, copies into the cache, renders the fixture JSON envelope and parses it. Cached assembly reads an existing page and renders/parses it. First invocation is recorded before10 warmup iterations; each warmed distribution has50 samples. Fixture generation, walking the collection and framing are measured separately.

| Measurement | First invocation | Warm median | Warm p95 | Warm maximum |
|---|---:|---:|---:|---:|
| Cached components |0.748ms|0.1099ms|0.146ms|0.183499ms|
| Uncached components |7.607101ms|0.1371ms|0.255499ms|1.7963ms|
| Frame/reassemble, capacity20 |not separately sampled|0.1693ms|0.2955ms|0.4472ms|
| Frame/reassemble, capacity185 |not separately sampled|0.0134ms|0.0182ms|0.0462ms|

Fixture generation28.9351ms; walk/render/decode all400 pages34.2605ms; maximum encoded page5,014 bytes. Scoped first/warmed component timings meet the proposed500ms cached/3second uncached budgets. This is a local smoke benchmark, not a cross-device performance guarantee or a production page-latency pass. Raw samples and environment are in summary.json; JVM startup/JIT effects and host load can change results.

## Explicit limits and next check

LibraryIndex is a standalone component and is **not wired into SourceRegistry**. The production source adapter uses RevisionGuard and current source-owned item/proof maps; the fixture's JSON renderer is test code using the documented fields/limits. This check must not be described as benchmarking an active host page cache. No disk-cache restoration, actual MediaBrowser callback, Flutter rendering, DX340 CPU, source scan, artwork, BLE scheduling/radio, playback or audio was measured.

Actual Mango exposes500 unique song IDs in the preceding source inventory. Its observed Playlist-to-folder recovery4.57–5.12seconds still exceeds the proposed3second uncached target. Actual source latency and cached/uncached page arrival/rendering must be measured separately over each real phone, with collection size and scan time recorded. Source scan/storage/rescan/SD, the full selection/listening matrix, phone reliability/security and current iOS compilation remain incomplete. This result fills only the synthetic component fixture/check gap.
