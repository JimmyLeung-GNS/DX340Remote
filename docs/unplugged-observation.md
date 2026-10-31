# Device-local recorder for unplugged screen-off tests

This is development instrumentation, not an everyday-control dependency or a T05 pass. It prepares a different route after the [two failed USB-powered holds](test-results/2026-10-31-display-off-hold.md). Preserve installed applications, grants, source engines and Apple-managed SD downloads. No root, Wi-Fi, power-setting change or app replacement is needed.

## Verified preparation and limits

`scripts/device-hold-recorder.py` uploads a read-only shell recorder into a unique mode700 `/data/local/tmp/dx340-hold-<uuid>` directory. It detaches from the launching ADB command, saves a manifest with the exact PID/script digest and offers status/collect/stop for that same job. A stale RUNNING file without a matching live process becomes INCOMPLETE_PROCESS_UNCONFIRMED; never restart merely because USB/polling is unavailable. Stop checks the exact owned script before signalling its PID. The recorder checks its logger's PID/command/output descriptor before signalling that child.

The recorder sends no playback/Bluetooth/screen/configuration command, holds no wake lock and writes only its own diagnostic files. It records public power/display, media sessions, music output, Companion service/process and narrowly filtered system power/display logs. Default duration1800seconds, interval30; configuration is capped at3600seconds/256samples. Collection accepts only known filenames, at most8MiB per file/64MiB total and records SHA256 hashes. Files remain in their own scratch directory after collection; no source files or unrelated device directories are deleted.

The clock is `/proc/uptime`, which includes suspended time. [Linux proc_uptime(5)](https://man7.org/linux/man-pages/man5/proc_uptime.5.html) A process may stop receiving CPU during suspend: sampling gaps remain evidence gaps. It checks the deadline when next scheduled, rather than judging a later USB reconnection as an earlier failure. A deadline reached/COMPLETED recorder is **not** acceptance of continuous display-off, Bluetooth, audio, battery or T05. Inspect all samples, clock gaps, wake events and separate phone/control/hearing evidence. A long-suspended job may only write its terminal summary when the CPU resumes.

First direct-file captures ended with empty power logs despite OFF samples. Those failed logger trials are retained. The corrected bounded stdout stream captures actual PowerGroup/DisplayDeviceRepository events in controlled tests. This is consistent with the stdout flush behavior in [AOSP logcat](https://android.googlesource.com/platform/system/logging/+/c47b22f3dffa9325b054e172dba737712afc771f/logcat/logcat.cpp); vendor internals were not traced. The stream has a1024-line cap, and an early logger exit fails the observation. It never clears system logs. Diagnostic output is not copied into Git; retain hashes/conditions under docs/test-results.

## Fresh physical sequence

1 November preparation correction: the DX340 repeats `mIsPowered` in the main Power Manager and battery-saver sections. The recorder now reads only one valid main-section value and fails closed on missing/ambiguous data. Actual Android-shell parsing of saved/synthetic text and three software tests pass; this is not physical unplugged arming proof. The [fresh failed arming attempt](test-results/2026-11-01-unplugged-arming.json) keeps its original script digest and zero hold samples. Do not reuse that terminal job. Read the [framing/recorder checkpoint](test-results/2026-11-01-framing-bounds.md) before the next repeat.

1. Obtain fresh owner availability for about31minutes, allowing arming time around the30-minute hold. Inventory both devices, exact installed binaries, source/output states and settings. Keep both sources paused at the agreed unchanged volume. Prepare the ordinary approved phone connection and source target, then foreground Android Settings on the phone. These setup actions are separate from the read-only recorder.
2. With both USB endpoints still connected, start exactly one new manifest:

   ```powershell
   python scripts/device-hold-recorder.py start --serial d --out artifacts/unplugged-case/device --duration 1800 --interval 30 --require-unplugged-off
   ```

   The initial state is ARMING. The deadline does not start until **three consecutive** observations, five seconds apart, report unpowered and actual internal/default display OFF. A brief charging fluctuation is insufficient. Arming expires after120seconds. An expired/failed job is retained; a repeat needs a new explicit availability agreement and case directory.
3. Ask the owner to unplug **only DX340 USB**, keep Pixel USB attached and both devices powered, and press the DX power button once if its display lights after unplugging. Confirm the display is off. Do not enable Wi-Fi or change charging/power policy. User confirmation and the PC's USB inventory establish physical cable handling; Android's power flag alone cannot prove it. Record the owner's completion time. After USB removal the device job cannot be polled directly; do not call its old manifest a verified live process.
4. Start the separate phone-only observer immediately after the completion reply. Its default31-minute window conservatively includes the device arming interval:

   ```powershell
   python scripts/observe-android-phone-background.py --serial 53061JEBF22737 --out artifacts/unplugged-case/phone --duration 1860 --interval 30
   ```

   This observer reads only Pixel process/service/power/activity snapshots. It never queries the DX340 or sends Bluetooth/source commands. Record its actual running tool/process handle. A polling timeout is not terminal. Create `<phone-output>/stop` to stop it early; retain its incomplete summary. It does not prove the DAP recorder survived USB removal or that Bluetooth stayed continuously available.
5. After the agreed interval, foreground the ordinary Pixel without touching/reconnecting DX340. Record the fresh state read and, if agreed, one deliberate supported native session control. Verify source metadata/state and hearing separately; do not use screen-assisted browsing while the DX display is off. Keep volume unchanged. A delayed/disconnected result must not trigger playback replay. Only afterward reconnect USB and collect the same device job:

   ```powershell
   python scripts/device-hold-recorder.py status artifacts/unplugged-case/device/manifest.json
   python scripts/device-hold-recorder.py collect artifacts/unplugged-case/device/manifest.json --out artifacts/unplugged-case/device/raw
   ```

6. Inspect the terminal summary, initial arming evidence, every sample, gaps, logger lifetime/cap and all power/display events within the actual hold interval. Distinguish the expected unplugging/reconnection wake events from any earlier wake. Confirm clocks/durations, source/output, process identities, settings/grants/bonds and final ordinary Disconnect/source pause. Restore original awake state only after the post-hold no-DX-handling test. Failed or incomplete evidence remains failed/incomplete.

Software checks: `python -m unittest discover -s scripts/tests -p test_device_hold_recorder.py -v`, `python -m unittest discover -s scripts/tests -p test_phone_background_observer.py -v` and `python -m unittest discover -s scripts/tests -p test_hold_power_parser.py -v`. Short plugged-in capture/stop/parser/negative-preflight checks do not prove real USB-detachment survival,30-minute sleep, unplugged reliability or any physical iPhone result.
