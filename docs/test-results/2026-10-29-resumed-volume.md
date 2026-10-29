# Fresh bounded-volume integration

29 October 2026, JIMMYAH / Windows 11 build26300. This port preserves the newer source-recovery branch's authorization leases, cancellation, queue pagination and elapsed-state handling. Historical 34–35 listening results on codex/output-volume do not enable this build.

## Verified software scope

- Android build/lint and99 JVM tests pass (`artifacts/resume-volume-native-final-build.txt`), including13 volume coordinator cases. Flutter analysis/36 tests/release APK/lint pass (`artifacts/resume-volume-phone-build.txt`). These are software checks, not acoustic or phone Bluetooth proof.
- Public STREAM_MUSIC adapter requires active selected-source playback, fresh source/output context, a local label, a one-step decrease and explicit human confirmations of decrease/return. Only that pair of levels can be enabled. Proof is memory-only and cleared on restart/output callback/access loss. A pending new check hides previous proof. No automatic volume replay, write retry or compensation is sent.
- BLE mutation uses the captured authorization lease and existing deduplication/mutation gate. Shared Flutter Player adds capability/range-gated buttons; native reference clients accept the operation but still lack volume buttons.
- The separate debug-only synthetic phone capture target cannot send Bluetooth commands. Capture helpers restore the ordinary app/target and display settings. Captures and hosted iOS verification for this port are pending.

## Current physical check

DX340 d, Android13/API33, build `DX340-userdebug 13 TKQ1.221013.002 eng.iBasso.20260812.181952 release-keys`; Mango4.1.37/code240, Apple6.5.3/code1599. User confirms AMP15 phone out/headphones and37 comfortable. Companion SHA256 `92b5f9c2899a2c17ec615e3e44c3f94633c064e4620a3ad3152a38cecd3ecda5` installed in place with matching original signer and all four preferences byte-identical. Both sources initially paused at37; PC approval remains revoked.

Local companion screen opened through its visible button. An attempted own-UID activity command failed with a package/UID SecurityException; no exported-activity workaround was added. Mango resumed through the existing source adapter, Apple remained paused. The new production volume screen lowered37→36; API/state and inspected screenshot agree. Human acoustic confirmation and explicit return are pending. `artifacts/resume-volume-{before,playing,down}.json` and corresponding PNGs record this scope. No fresh volume proof or phone BLE success is claimed.

Next: obtain actual listening confirmation, then explicitly return only to37 and confirm separately. Repeat for Apple independently. Verify current iOS compile/rendering and native screenshots with scoped independent review. Real phone volume/reconnect, full output/gain mapping, quality, reliability and both-phone acceptance remain NOT TESTED.
