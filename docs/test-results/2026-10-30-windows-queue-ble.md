# Actual Windows BLE queue windows

30 October2026, Asia/Hong_Kong. **VERIFIED PC metadata scope; full system incomplete.**

## Environment and packages

Current JIMMYAH/Windows11 build26300, Python3.14/Bleak3.0.2; USB DX340 d, Android13/API33/build DX340-userdebug13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys. Source apps remain Mango4.1.37/code240 and Apple Music6.5.3/code1599. Installed listening companion04846d60bc4e65065028d510286b56cba91577840a994d351146c55dff418fa2 and ordinary phone23c536e7890ddc58a84552fcd3bd4be4eac578722daa8553c3a94d7bf932328d were not upgraded or replaced. New diagnostic source is based on130513a; product Kotlin/Swift/Dart code is unchanged.

Only DX340 remains USB-connected and no local Xcode exists. The prior turn completed staged hosted Mac41026103457; no live build or separate physical iPhone/Android controller is inferred. JIMMYAH's actual authenticated OS bond and owner-approved saved companion grant are retained. No PC revocation test was run.

## Implementation and software checks

`verify-windows-protocol.py authorized-queue` adds an explicitly enabled read-only queue mode to the existing diagnostic. Default modes retain the HELLO/AUTHORIZE/GET_STATE allowlist; only this mode can send GET_QUEUE, and only after saved authorization with validated source/epoch/offset/revision fields. It cannot send playback, volume, source-selection, pairing or approval/revocation commands. It uses conservative20-byte request frames and protected indications with authenticated bond checks before/after.

The diagnostic walks nextOffset windows, requires stable revision/total, bounded complete rows, unique IDs, exact cached first-page duplicates, stale/missing-required-revision denial without metadata, fresh first-page recovery and unchanged selected source/epoch/title/artist/paused state. It refuses incomplete coverage at200 pages/10000 items or the180-second overall deadline. Titles/IDs/revisions stay in memory; stored reports contain fixed checks, counts, source package, byte sizes and timing only.

**VERIFIED software:** `C:/Python314/python.exe -m unittest discover -s scripts -p 'test_windows*.py' -v` passes all30 cases (23 existing plus7 queue cases). Synthetic tests cover fragmented101-row and empty/byte-limited windows, explicit opt-in/authorization and mutation rejection before writes, malformed sizes/types/continuations/revisions, duplicate IDs, lying stale acceptance, changed duplicate replies, active/replaced source rejection and refusal at the page-walk limit. These are diagnostic-correctness tests, not radio acceptance. No unrelated Android/Flutter rebuild was required by these Python/documentation-only changes; preceding native120/Flutter46 results retain their source scope.

## Actual Bluetooth execution

Both executions use a fresh saved-grant connection, protected INFO/indications/HELLO/AUTHORIZE, state/duplicate-state checks and final disconnect cleanup. No local approval was required again. Both OS bond checks report ENCRYPTION_AND_AUTHENTICATION; negotiated ATT MTU517.

| Selected source | Host-reported complete queue | BLE windows | Framing writes / received callbacks | Outcome |
|---|---:|---|---|---|
| Mango |38 unique queue IDs | offset0:38 rows,1497-byte reply,6298.718ms |163 /37 | All checks pass; single-window result does not establish positive continuation |
| Apple Music |101 unique queue IDs | offset0:50 rows/4533bytes/6900.970ms; offset50:50/2560/5938.222ms; offset100:1/298/4138.672ms |219 /62 | All checks pass, including actual positive revision-bound continuation |

Commands use `./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-protocol.py authorized-queue --label jimmyah-queue-pagination` and a separate `--label jimmyah-apple-queue-pagination` run. Zero playback/volume/pairing commands in either run; no automatic retries/reconnect or service restart. Received callback counts include unsolicited state frames (two messages ignored per run), not just page replies. The recorded times include conservative diagnostic request framing and reply delivery; they are not ACK/control latency, phone performance, a repeated benchmark or a passed performance target.

Mango was initially selected and paused. Before the second connection, the signature-protected local debug GET_STATE setup selected **paused Apple Music as the companion control target**; it did not start Apple or pause Mango. After terminal Bluetooth success, the same local GET_STATE setup restored Mango. These local setup mutations are distinct from the Bluetooth diagnostic's read-only operations. Both setup reports verify both intended players paused. No source-selection command was sent over Bluetooth.

These walks verify completeness relative to the host's current queue total and revision. They do not independently compare every received title with a fresh full public-source baseline, select a queue item, browse arbitrary downloaded/local files or verify audio. Existing source-local101-entry public-baseline matching is separately recorded in2026-10-29-queue-pagination.md. Do not label this T07/T08 or either-phone acceptance.

## Preservation and durable evidence

Fresh restored own-adapter state is Mango selected, both intended sources paused, volume37, prooffalse; Wi-Fi0/no active default network. All four shared-preference digests match the before/after checks (and the preceding approved-state digests): optional source modes/source verification/controller approval remain unchanged. The PC is disconnected with its grant saved; no other identity/bond was changed. No APK installation, source audio command or user listening confirmation occurred.

Sanitized terminal reports are tracked in [Mango BLE queue evidence](evidence/2026-10-30-mango-queue-ble.json) and [Apple BLE queue evidence](evidence/2026-10-30-apple-queue-ble.json). They contain no radio addresses or music labels. Ignored raw setup/postflight/software logs remain under artifacts/windows-queue-* and artifacts/windows-ble/jimmyah*-queue-pagination.json; they are not automatically transferred in a Git clone. Prior setup/INFO failures in2026-10-30-resumed-mac-provenance.md remain unresolved; two successful queue sessions do not establish reliability.

## Remaining proof

Use ordinary signed iPhone and separate Android phone builds for actual BLE enrollment, offline control/browsing/selection, sleep/range/latency, verified volume, interruptions, rescan/SD, battery/walking and two-central handoff. Keep PC approval and disconnect its live link before initial phone testing. Complete final-build Apple and Mango selection matrices with human audio confirmation separately; Watch follows stable phones. mac-iphone-quickstart.md, phone-reliability-runbook.md and controller-handoff-runbook.md provide the exact entry points. Full P0–P9/T01–T18 requirements remain incomplete.
