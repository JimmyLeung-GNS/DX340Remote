# Bounded volume controls

DX340 serial d, Android13/API33, eng.iBasso.20260812.181952. Mango4.1.37/code240; Apple Music6.5.3/code1599 installed. AMP15 phone out is user reported. Bluetooth controller hardware is unavailable; **no phone volume command has been physically tested**.

## Pause update

Development paused for device transfer. The latest facts and resume order are in **../development-handoff.md**. Apple Music subsequently completed its own production35→34→35 local calibration, with separate human replies “Yes, Apple Music is quieter” and “Yes, Apple Music is louder again”. Its distinct proof enabled34–35; fresh final state was Apple playing/volume35, Mango paused. This is local source/audio evidence, not phone BLE. Final Android suite passed50 JVM tests (10 volume cases);14 Dart tests/release/lint passed. A final label-restoration correction passed APK/lint rebuild but was not installed. The canonical phone release was restored over the debug fixture. Source/run/package details and the known OCR readiness failure are in the handoff. Older NOT TESTED Apple rows below describe the initial Mango checkpoint.

## Verified local Mango calibration

The preceding debug return34→35 read back35 and the user replied “Yes, the return to 35 is audibly louder”. That diagnostic enabled no capability. The installed production adapter was then checked separately through its local screen:

| Result | Case | Evidence / limit |
|---|---|---|
| VERIFIED local API/UI | Initial35, source Mango playing, Apple paused | Fresh source state; native local screen; no phone involved |
| VERIFIED local API + user reported | Test one step quieter35→34 | Public STREAM_MUSIC write/readback; user answered “Yes, this new step is quieter” |
| VERIFIED explicit transition | Human confirmation permits return action | UI changed from heard-down to Return to starting volume; no automatic restoration |
| VERIFIED local API + user reported | Explicit return34→35 | User answered “Yes, the return is louder” |
| VERIFIED local capability | Human return confirmation enables34–35 | Current35, hardware0..100, verified=true, range34..35, AMP15 phone out, new opaque profileId |
| NOT TESTED | Apple Music / other output configurations / hardware extrema | Mango proof does not enable another source |
| NOT TESTED | Phone BLE/reconnect/background/walking | A local proof/compile/fixture is not radio acceptance |

The assistant tapped the local confirmation buttons only after each human answer. Replies are human reports, not calibrated acoustic/dB measurements or proof of bit-perfect audio. No gain/EQ/route/engine was changed. Device left at35 after this sequence. The profile expires on service stop or audio-device changes; app/session/firmware/output inventory must match. Android cannot identify every amp/gain/headphone change, so explicit rechecking is required after those changes.

## Software and presentation checks

- VERIFIED build: scripts/build-android.ps1, build/lint/48 JVM tests. Eight new coordinator tests cover human-confirmation gating, malformed/stale values, authorization/context/bounds, context changes between phases, delayed readback failure without compensation, concurrent local/remote serialization, CommandLedger duplicates and reset/closed/paused readiness. Synthetic tests do not establish audio or Bluetooth behavior.
- VERIFIED build: scripts/build-phone.ps1, analysis/14 Dart tests/release APK/release lint. New tests cover band/profile/current-level guards, double taps/reconnect non-replay, and Android/Cupertino boundary controls at2.5 text scale. Initial compilation/lint/type/test-cleanup issues were fixed; a subsequent overlapping Gradle process caused a file lock. Both processes finished before the passing serialized build.
- VERIFIED scoped review: fresh independent Impeccable reviewer returned **ship** for six actual DX340 dark/default-font local-screen captures only. Light/large-font/TalkBack and physical phone acceptance are open.
- IN PROGRESS: updated iOS actual application build/launch and separate synthetic volume rendering. The fixture entry point phone/test_support/volume_capture.dart is debug-only, labelled synthetic, and cannot send Bluetooth actions. Production packages use lib/main.dart.

Host APK SHA25651cf92b5c1850525a3803237b1c67aa0d986e8bd2d8b450c7890f9ba03cc1e94; shared phone release4c1937dbe4554008e085478d85f49b1ab5d11ea07bb699e22f3adebd91f94405. Ignored artifacts: output-volume-build.txt, output-volume-phone-build.txt, volume-production-precondition.json, volume-calibration-{down,return,verified}-state.json and volume-check-{initial,ready,down,return-ready,return,verified}.png. Earlier diagnostic files remain historical evidence in volume-observation.md.

## Physical next steps

Play each intended source on the intended output at a normal low level, run the local two-step check and confirm only what was heard. On a separate authorized phone with Wi-Fi/cellular off, refresh Player, observe level/output/range, step down/up once, and compare public readback, player display and actual audio. Change volume with the hardware wheel before a phone tap: stale expected values must be rejected; refresh and issue a new deliberate command. Disconnect during a pending step, reconnect and verify no replay. Stop/restart the companion: the proof must be absent. Recheck amp/gain/output changes explicitly. Hardware extrema and broader listening bands need separate evidence; never infer full T10 from the34–35 pair.
