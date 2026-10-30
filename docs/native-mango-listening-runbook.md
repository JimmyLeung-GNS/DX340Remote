# Native Mango listening and boundary checks

Use the original-key companion with the pinned native-ID correction. Current verified build is7129c307.../source0164a76, Mango4.1.37/code240, DX340 Android13. Obtain the owner's current listening availability/output/comfortable volume first. Keep Apple paused. This runbook checks the source-local production adapter; it does not operate a phone over Bluetooth.

1. With both sources paused/offline, run `scripts/verify-mango-native-inventory.ps1`. Require its terminal VERIFIED_SOURCE_LOCAL_NATIVE_METADATA_ONLY summary; preserve a failed/truncated inventory separately. Metadata rows do not prove file format or playback.
2. Generate a new plan from that actual inventory:

```powershell
python scripts/prepare-mango-native-listening-plan.py artifacts/mango-native-inventory/YOUR_RUN --output artifacts/YOUR_NEW_NATIVE_PLAN.json
```

The prepared20 **native row locations** include the first/last exposed All Music rows and first/last/later-page rows across five observed albums. Some boundary clips are intros/endings, not20 distinct qualified songs/files. All Music's observed500 limit is not the whole collection. Source-appended ordinals are removed only for expected metadata comparisons; original browser IDs remain the requested identities. Do not invent rows or infer a FLAC/WAV/container/unique-file result.

3. Run exactly one deliberate selection, using the actual expected volume and installed companion hash:

```powershell
./scripts/check-mango-native-selection.ps1 -PlanPath artifacts/YOUR_NEW_NATIVE_PLAN.json -Sequence 1
```

The helper requires both sources paused, Wi-Fi off/no default network, verified native capability, matching installed bytes and a fresh native root/parent/page/title/artist. It sends one PLAY_ITEM, then bounded read-only polls require two settled matching playing states. It does not retry playback. A failed dispatched attempt is paused; preserve its failure and any pause failure. Inspect artifacts/latest-native-mango-attempt.json and the actual per-attempt summary; a saved marker is not a live process or an audio result.

4. Ask the owner whether the intended song/clip is audible. Only after an actual answer, record it and pause:

```powershell
./scripts/record-mango-native-confirmation.ps1 -AttemptDirectory artifacts/mango-native-matrix/YOUR_ATTEMPT -Result Correct -UserQuote 'THE ACTUAL ANSWER'
```

Use Incorrect for a reported failure. The answer is persisted before cleanup; a pause failure cannot erase it. The helper rejects an already recorded or non-pending result. It also captures source state at confirmation, which may be later than the original observed state: short intros/endings can advance naturally before the reply. Keep that timing caveat visible. Do not fill a human answer from a source-state observation, timeout or elapsed time.

Repeat with the next sequence only after the previous clip is paused and its actual outcome saved. Record all attempts, not just successes; independently count native locations, title/artist/album labels and failures without asserting unique physical files. Keep source-local, human, simulated and phone evidence separate. Full final-build20-song/format/playlist/SD/rescan, phone/offline/reliability and Watch acceptance remains required beyond this boundary matrix.
