# Windows read-only protocol diagnostic preparation

29 October2026. **VERIFIED19 software tests and live unpaired refusal. Authenticated protocol exchange, companion approval and physical revocation are NOT TESTED.**

The previous Windows discovery/GATT/0x05 report remains separate. This checkpoint prepares the next actual protocol test without turning a Windows PC into a substitute for either product phone. No APK, key, approval, bond, permission or radio setting changed. No music selection or listening check occurred.

## Resulting diagnostic

scripts/verify-windows-protocol.py uses the existing isolated Python3.11.5/Bleak3.0.2 environment and pinned WinRT packages. It requires exactly one custom-service advertisement and a fresh Windows paired/protection report showing ENCRYPTION_AND_AUTHENTICATION before an explicit Bleak GATT connection or characteristic operation. It does not call a pairing API or change a bond. Unknown/weaker protection fails closed.

Its wire allowlist contains only HELLO, AUTHORIZE and GET_STATE. It uses explicit write-with-response with conservative20-byte frames and an indication subscription. Uncached INFO/service layout/properties are checked. The pure Python framing layer is diagnostic code with the shared Kotlin/Swift fixture bytes; it does not replace any product Kotlin/Swift/Flutter transport.

Three distinct modes are prepared:

- approval-boundary: HELLO, AUTHORIZE returning LOCAL_APPROVAL_REQUIRED, then GET_STATE returning AUTHORIZATION_DENIED. Extra preauthorization metadata is a failure. No approval is performed or removed.
- await-approval: holds a fresh connection, observes initial approval denial when present and polls AUTHORIZE every two seconds for up to45seconds while the operator approves the identified request locally. The host clears pending approval on disconnect, so a separate held connection is necessary after the denial run. Successful authorization is followed by a state read and exact duplicate read ID/result check.
- authorized-state: uses an already saved companion approval on a new connection, reads state and checks the duplicate read result. It cannot create the missing approval.

The receiver bounds messages to64KiB, four partials,512-byte incoming frames and ten-second partial expiry. UTF-8, depth/duplicate JSON keys, request correlation, host epoch, reply envelope and authorization-boundary data are checked. Unexpected/stale replies or disconnect invalidate the diagnostic session. Per-request deadlines include writes; no automatic reconnection occurs. Only the explicit approval-wait mode polls authorization; playback operations are absent. Unsolicited state cannot satisfy a request and is not retained. Reports preserve completed checks on later failure but omit addresses/music labels/raw OS error strings. Overall failure/cleanup is explicit.

This does not prove host authorization from an OS enum. The next actual authenticated run must successfully perform protected GATT access, correlated protocol exchange, local denial/approval and state checks. A duplicate read comparison does not prove exactly-once playback mutation. Pending-read revocation, another real approval, callback ordering and both phone paths remain separate acceptance work.

## Verification

VERIFIED software:19 unittest cases cover all three shared framing fixtures including split Japanese UTF-8; four-partial/ten-second expiry/interleaving; maximum/oversized/truncated/bad-count/duplicate/out-of-order frames and disconnect clearing; invalid UTF-8/deep/duplicate/nonfinite JSON; unpaired/unknown/weaker bond guards; both approval/state paths; later local approval and absent-approval expiry; forbidden playback/source/volume operations; preauthorization metadata; stale IDs/host epochs/version; ACCEPTED versus final reply; blocked writes/timeouts/disconnect without replay; changed duplicate read results; unsolicited-state isolation. Fake peers test the diagnostic's assertions, not the real companion/radio. All19 pass with zero failures; py_compile and CLI help pass.

```powershell
./work/ble-diagnostic/Scripts/python.exe -m unittest discover -s scripts -p test_windows_ble_protocol.py -v
./work/ble-diagnostic/Scripts/python.exe scripts/verify-windows-protocol.py approval-boundary --seconds 5 --label protocol-unpaired-preflight
```

VERIFIED live preflight on this Windows11 Pro26300/Intel Bluetooth PC: one DX340 service candidate was found; fresh Windows pairing=false and protection=NONE. The diagnostic exited1 with AUTHENTICATED_OS_BOND_REQUIRED before an explicit GATT connection/characteristic request; framesWritten0/pairingRequested=false/playbackCommandsSent0. This is a verified refusal, not a successful authenticated protocol run. Native operator pairing availability remains pending; no consent was automated.

DX340 remains the sole USB target d, Android13/API33 build eng.iBasso.20260812.181952, Mango4.1.37/code240 and Apple6.5.3/code1599. Companion40114ff8.../shared phone09aaae78... unchanged from the prior checkpoint; native86/Flutter32 build/test scopes remain preceding evidence. No Android rebuild or duplicate source regression was needed for standalone diagnostic scripts. Current Swift still needs a Mac build.

Evidence: artifacts/windows-protocol/unit-tests.txt, help.txt, physical-preflight-output.json and artifacts/windows-ble/protocol-unpaired-preflight.json. Exact operator sequence is in windows-ble-diagnostics.md. Reports/logs/dependency environments stay ignored; generated Python bytecode is also ignored.

Primary contracts: [Bleak writes/subscriptions and disconnect behavior](https://bleak.readthedocs.io/en/stable/api/client.html), [Windows reported pairing protection](https://learn.microsoft.com/en-us/uwp/api/windows.devices.enumeration.deviceinformationpairing.protectionlevel?view=winrt-28000), [authenticated protection enum](https://learn.microsoft.com/en-us/uwp/api/windows.devices.enumeration.devicepairingprotectionlevel?view=winrt-26100). The pinned Windows backend was also inspected for force_indicate support. These APIs and software checks do not fill missing native consent or phone/audio/security/reliability acceptance. Goal active; full system incomplete.
