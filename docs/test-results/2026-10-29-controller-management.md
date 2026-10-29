# Individual controller revocation and authorization lifetime

29 October 2026. **VERIFIED software, isolated Android storage/UI and unpaired radio checks. Actual authenticated controller revocation remains NOT TESTED.**

DX340 d: Android13/API33, build eng.iBasso.20260812.181952; Mango4.1.37/code240, Apple Music6.5.3/code1599. Windows central inventory remains in windows-ble.md. Both source sessions stayed paused at volume35. No real approval or OS bond was added/removed. No new audible selection was tested.

## Resulting behavior

The companion now lists saved approved controllers under Controller access, with a recognizable OS name/address suffix and a revoke action for each identity. Duplicate labels include the full local address to disambiguate them. Bluetooth permission/radio unavailability is displayed as unknown status rather than a missing bond. The list remains usable when the host service is stopped. Revoking one identity removes only its application approval, invalidates its current authorization and requests that peer's disconnect; unrelated approvals remain. OS bonds stay managed by Android. Revoke-all also closes the pairing window and disconnects a pending/current peer.

Pending approval identifies the requesting controller. Its button is enabled only with the known bond, an open pairing window and capacity. The callback binds the displayed identity/host instance; changed requests must be reviewed again. Friendly names are labels, never authentication identities.

Startup cleanup now skips an unknown bond snapshot when the adapter is off/unavailable or its lookup fails. An empty result from a disabled radio must not remove all saved approvals. A known enabled-adapter snapshot can still prune approvals for missing bonds. Actual Bluetooth-off recovery with approved physical peers is not tested here.

## Authorization/reconnection correction

The old asynchronous guards compared only current device/approval values. Those values can become true again after revoking/disconnecting and then reauthorizing the same address. The host now captures an opaque in-process authorization lease for each request. Disconnect, selected revocation, revoke-all and service destruction invalidate the old lease. Reauthorizing the same address creates a different lease; delayed callbacks cannot regain permission or deliver old replies into that session. Repeated AUTHORIZE on the same uninterrupted session remains idempotent.

Mutation ownership also rejects a second entry using an already owned request ID; duplicates are handled by the request ledger first. A different controller/session cannot borrow the gate by choosing the same ID. Ledgers remain retained across revocation/reapproval within the host epoch, preserving command deduplication. No protocol field/version was added. Native same-peer callback ordering and actual radio interruptions still require physical acceptance.

## Verification

- VERIFIED: scripts/build-android.ps1 completed native APKs/instrumentation compilation, lint and86 aggregate JVM tests. Seven additions cover authorization lifetime/identity, same-ID mutation ownership and SourceRequest refusing a late page after same-identity reauthorization.
- VERIFIED component: scripts/verify-controller-management.ps1 uses a debug DUMP-protected receiver to exercise the actual Android ControllerApprovalStore with synthetic identities in an isolated preference file. Target revocation left the other seven approvals unchanged on disk. Eight-controller capacity, repeat approval, invalid/unknown identity handling, unknown bond snapshot, known-snapshot pruning and revoke-all preserving unrelated fixture values pass. The fixture is deleted; production controllers.xml remains unchanged. This does not create bonds or call real peer revocation.
- VERIFIED scoped UI: the real production panel was rendered in a DUMP-protected debug preview with an isolated store and a null live-host provider. Observed taps changed2 →1 (second identity preserved) →0. Final-package captures final-preview-{two,one,empty}.png were inspected. The actual companion's zero-approval screen was also inspected. Pending real approval and revoking a live service peer remain unverified.
- One earlier preview-cleanup assertion ran200ms after Back and observed its file during Activity teardown. Fresh state showed MainActivity resumed and no fixture left; the final bounded check observed deletion within789ms. This was a harness timing assumption, not a persistent fixture or an application crash. No preview fixture remains.
- VERIFIED real Windows radio regression on the final build: uncached service enumeration succeeds; unpaired INFO/CCC/RX/subscription all deny0x05 Insufficient Authentication, with no bond and completed disconnect cleanup. This tests the unauthenticated attribute boundary, not the new authenticated lease/revocation path.

The existing controllers.xml Boolean format is retained. Android documents that [SharedPreferences.apply updates memory immediately and persists asynchronously](https://developer.android.com/reference/android/content/SharedPreferences.Editor#apply()). The component check uses a commit barrier and reads its own isolated XML to verify normal disk persistence. Power-loss/storage-failure durability is not proved. [Bond lookup requires Bluetooth access](https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#getBondedDevices()); failed/offline lookup is not treated as an authoritative empty list.

## Packages and continuity

Original-certificate preflight and install-r passed. The installed companion was pulled and exactly matches the final artifact:

`e8a21bd467efc65c0f20fc593ace8b3cd1f3ef81ee8b2dcf732156a493afe98b`

Shared phone remains09aaae7836a98a77c3ce52c870873dc39191dde846e286947d18892875a963a2 with preceding Flutter analysis32 tests/release/lint scope; no shared UI/bridge/Swift change was made. Current Swift still needs a Mac build. Both optional Accessibility services remain bound, no binding/crashed entries; four real preference files stayed byte-identical. No key/global environment, firmware, source files, download management or playback engine changed.

Evidence: ignored artifacts/controller-management/final-build.txt, final-component-output.txt/component-check.json, final-preview-*.png/final-preview-check.json, preview-one-preferences.xml, installed-host.json, preflight/install/pull, prefs-before/after.json and final-state/services captures. Final real radio report: artifacts/windows-ble/controller-build-unpaired.json. Earlier preview captures belong to an intermediate build with the same UI source and are not substituted for the final captures.

Next physical proof: use two genuinely paired/approved controllers; revoke only one during a pending read/control, verify its disconnect/denial, unrelated controller retention and stale callback rejection after deliberate reapproval. Verify stopped-host and Bluetooth-off cases separately. Current Windows native pairing and listening availability are still pending user steps. Full phone/Mac/source/format/audio/latency/battery/walk/Watch scope remains incomplete; this checkpoint does not pass complete T15 or P3.
