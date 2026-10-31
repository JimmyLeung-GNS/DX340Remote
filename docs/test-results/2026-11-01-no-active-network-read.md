# Read-only BLE with no active networks — 1 November 2026

**Verified sampled Android subset; strict cellular-off T01 and physical iPhone remain open.** [Structured evidence](2026-11-01-no-active-network-read.json) includes the three new correlated requests, preservation checks and87 raw-file hashes. Binaries remain original-signer shared Pixel `95d29be6…`/sourcee40b822 and Companion `dc53629d…`/source1e3c551: Pixel9a Android16/API36 CP1A.260405.005 and DX340 Android13/API33 eng.iBasso.20260812.181952. Mango4.1.37/code240 and Apple Music6.5.3/code1599 remain paused.

Both devices report **Active default network: none** and an empty Current Networks section in each of four snapshots: preflight, connected phase, after the deliberate state read, and postflight. These are eight sampled observations, not continuous packet capture. Wi-Fi stays0 on both, DX340 mobile_data0 and Pixel mobile_data1; Bluetooth stays1. Both ADB reverse lists are empty. No radio/network setting is changed and no playback/source/volume command is issued.

The Pixel reports `gsm.sim.state=ABSENT,NOT_READY`. Its actual Network and Internet screen shows **SIMs — Add a network**; the Internet page's sole checkable Use Wi-Fi switch is false. The direct network-operator Settings route returned to Remote before inspection, so no SIM-panel proof is claimed from that attempt. Read-only Settings navigation was then observed through its normal visible Internet row. No toggle or Add-network action was pressed. The installed release phone package does not list `android.permission.INTERNET`; debug/profile source manifests declare it separately. Android documents [INTERNET as the socket permission](https://developer.android.com/reference/android/Manifest.permission#INTERNET) and [default-network availability separately](https://developer.android.com/reference/android/net/ConnectivityManager#getActiveNetwork()). These facts support the observed BLE interpretation; they do not turn stored mobile_data1 into a verified off setting or inventory a ready carrier subscription.

| New actual Pixel request | ATT write receipt | Matched result |
|---|---:|---:|
| HELLO |187ms|191ms|
| AUTHORIZE using saved grant |187ms|532ms|
| One deliberate GET_STATE from Refresh player |94ms|322ms|

All three RESULTs are applied with no new error. The same0baf310c journal grows10→13, preserving all prior rows exactly, including the earlier retired interrupted browse, with zero evictions. The first driver search could not see Refresh player below the artwork and sent no command; one normal scroll exposed the action, then it was pressed once. This read-state sample does not establish100 controls, playback, hearing, T04 p95 or current Apple browse acceptance.

Final03:36:22 snapshots preserve all four host preference hashes, grants, settings, PIDs4334/25153, source positions and paused2/2 at37/headset(4), with the same SD volume mounted. Normal phone Disconnect clears foreground ownership and help is collapsed; host service remains foreground. Display/power were captured only after the read; no claim follows that it ran during sleep, and this is not T05 or the pending30-minute hold. No package, source data, storage, volume, power-policy or network configuration change occurred.

Full T01–T18/two-phone/source/audio/reliability/signing/Watch work remains incomplete. The earlier unplugged recorder stays terminalfailed with zero hold samples; fresh availability and owner Mac/iPhone access are unanswered. Both Apple CI jobs remain closed and are not rerun for this physical read-only check.
