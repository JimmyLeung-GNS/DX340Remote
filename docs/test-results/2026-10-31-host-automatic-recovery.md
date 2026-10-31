# Enabled Companion recovers after process loss —31 October2026

**VERIFIED paused Android desk subset; full goal ACTIVE/incomplete.** [Exact versions, APK hashes, timings, preservation and raw evidence manifest](2026-10-31-host-automatic-recovery.json). Installed Companion644799cc…/source81b3f6b replaces00e5. Pixel4318/source5f2de32 is unchanged. Both in-place Companion updates match the original certificate24f48aa3…; final installed APK was pulled and compared byte-exact with the candidate. DX340 Android13/Companion0.1.0 code1, Mango4.1.37/code240 and Apple6.5.3/code1599; Pixel9a Android16/shared0.2.0 code2.

## Change and failed first attempt

Previously the enabled HostService returned START_NOT_STICKY, requiring local reopening and Enable phone control after process loss. Sourceb15edf6 returns START_STICKY only after normal initialization. Explicit STOP and missing Bluetooth permissions still stop and return non-sticky. A null restart restores availability with a new host epoch and fresh authorization; it does not redeliver PAIR/PROBE or a playback command. Pairing remains closed until a deliberate local action. No boot receiver, alarm, permission expansion, playback engine or wire-format change was added.

Android documents this system-managed lifecycle for explicitly started/stopped services and notes that sticky foreground restarts are distinct from new background foreground-service starts. This mechanism does not guarantee survival of force-stop, reboot or every vendor's power policy. [Service START_STICKY](https://developer.android.com/reference/android/app/Service#START_STICKY)

The first physical sticky-only APK9d41e898… **fails phone recovery**: Companion automatically restarts in Settings background, but HELLO takes1794ms and AUTHORIZE times out12010ms. Host approval is not proof the phone received authorization. Its new server attaches to an existing connection before advertising; the phone reports MTU517 but the new host log contains no MTU callback. The working diagnosis is a retained default20-byte reply capacity after server recreation, consistent with slow fragmentation; that old capacity was not directly logged, so this is not an OS-wide causal claim. The failed report and native logs remain intact.

The first driver also encountered a stale foreground-service record before a replacement PID existed and aborted on expected PID absence. The kill was not repeated for that attempt. The corrected driver accepts absent PID while observing the same restart. The final physical case below uses a separate, fully recorded attempt.

Source81b3f6b adds bounded capacity learning after a valid non-prepared protocol write. Its received size is evidence that this payload fits the shared ATT link; capacity grows only to the greater of known capacity and observed9–512-byte frame size, with a minimum20. Known larger capacity is retained. The normal MTU callback remains in use; no assumed517-byte MTU, new transport message or authorization bypass is introduced. [Android MTU request](https://developer.android.com/reference/android/bluetooth/BluetoothGatt#requestMtu(int)), [server callback](https://developer.android.com/reference/android/bluetooth/BluetoothGattServerCallback#onMtuChanged(android.bluetooth.BluetoothDevice,%20int)).

Two new JVM regression tests fail with retained20 and pass with observed-write learning: a6000-byte authorization-like reply frames/reassembles below the successful incoming write bound, and small/invalid/oversized observations preserve bounds. Full native Android build/reference/probe assembly/lint passes with180 JVM tests (55 protocol,3 controller,7 cache,115 source adapters), zero failures/errors. Flutter/Swift sources and the installed phone are unchanged; prior72 Flutter/19 Swift and closed5f Apple archives retain their scope. No new Apple workflow is needed or running for these Android-only paths.

## Automatic recovery on the fixed host

Both source sessions are paused at37/headset(4). Pixel has a live saved-approval connection. The driver resolves and verifies actual DX340 Settings foreground, then verifies the unique Companion PID12608 and exact process command line before one own-UID SIGKILL. It makes **zero DX340 input/launch actions after that kill during recovery**, and does not tap phone Connect.

Android recreates Companion as PID12800 with foreground service observed1765ms after the kill command, including shell/observation overhead. Settings stays foreground. Diagnostic events explicitly record the null-intent SERVICE_RECOVERED path and received-write capacities180 then220. Host epoch changes; phone PID27585 remains unchanged. The phone automatically reaches Connected/Ready after a4310ms Connecting→Connected journal interval. The host's own clock measures966ms advertising→approved connection. These clocks are separate, and none of these spans is GATT ACK/p95/range acceptance.

Decoded recovered HELLO170ms and AUTHORIZE1707ms are applied RESULTs. A subsequent explicit Refresh player returns applied GET_STATE791ms. This is one paused desk process-loss case with no local recovery handling, not full T12 or supported bag/screen-off/range behavior. No playback mutation was in flight; interrupted-mutation no-replay remains open.

## Explicit Stop remains respected

After normal phone Disconnect, Companion's actual native Advanced → **Stop Bluetooth remote** removes HostService. Ten one-second-spaced observations show no HostService before another own process-loss test. The stopped Companion process12800 is terminated; Android recreates its bound-service process13001, but fifteen further one-second-spaced observations show HostService stays stopped. This is a bounded stop-preservation case, not indefinite observation or a physical notification Stop test.

Companion is then deliberately reopened/enabled to restore the initial enabled state. A deliberate saved phone Connect succeeds with HELLO93ms/authorization977ms, and Refresh player returns GET_STATE503ms. No new pairing or approval is needed.

## Final state and evidence limits

Final export contains35 requests in the same phone model: all23 prior process-report records unchanged, four retained sticky-only trial requests including its authorization timeout, and eight fixed-build applied RESULTs (three HELLO, three AUTHORIZE, two GET_STATE). No fixed-build error/timeout, playback or volume command; zero evictions. Do not describe the entire35-row history as error-free: it retains earlier cancellation errors and the failed sticky-only timeout.

Four saved Companion preferences have identical before/after byte hashes. Both devices' observed radio/Wi-Fi/mobile-data/font/timeout settings, own runtime grant/flag rows and exposed bonded identities/types match (one phone row, four DX rows, not four unique devices). Pixel/JIMMYAH grants and browsingON are retained. Both Accessibility services are actually bound. Both direct media sessions finish paused2 at37/headset(4), volume proof false. Companion is enabled/foreground; normal phone Disconnect clears foreground/started work, help is collapsed and both USB devices are present.

Raw packages/logs/UI/native dumps remain under ignored `artifacts/host-sticky-20261031/`; tracked manifest contains hashes and sanitized findings. No source playback, volume, mobile-data change, root, firmware, bond reset or controller revocation was performed. No observer, build, emulator, Apple job, audio test or long hold remains running. Fresh30-minute readiness and earlier envision hearing remain unanswered; no new audio confirmation is inferred. Older b805/00e5 audible results are not promoted to this new host.

Full T01–T18/current two-phone source/albums/playlists/formats/SD/output/commands/latency/sleep/range/calls/battery/walk/ownership, signed owner-iPhone/Mac and later Watch remain unfinished. This correction removes the demonstrated local-handling requirement for one process-loss case; wider reliability still needs its own proof.
