# Source-session and capability recovery

Scope: Kotlin companion on DX340 Android13/API33, Mango4.1.37/code240 and Apple Music6.5.3/code1599. AMP15 phone out remains user reported. This report separates synthetic callback schedules, a mixed component check and physical phone acceptance.

## Callback ownership

Source inspection found that an old MediaController's delayed onSessionDestroyed callback could remove the replacement controller stored under the same package. The previous implementation keyed cleanup only by package. A per-session callback lease now protects playback/metadata/queue callbacks and retirement. Closing the registry invalidates all leases; browser callbacks also ignore a closed registry. Current-session destruction clears its native item proofs as well as items.

**VERIFIED in synthetic tests:** three SourceCallbackGuard tests cover delayed old callbacks/destruction after replacement, independent ownership for the two sources, duplicate retirement and queued callbacks after registry shutdown. These do not force a real Mango session replacement or prove Bluetooth reconnection. scripts/build-android.ps1 passed builds/lint and40 JVM tests; artifacts/source-recovery-final-build.txt.

## Capability revisions

Changing native Mango selection verification advances sourceEpoch only when native selection capability actually changes. Both existing native phone models clear library/queue/navigation data on an epoch change. The host retains same-session parent proofs so the local test can refresh its current folder; every native selection still re-reads and validates source metadata before dispatch. Repeating the same capability state does not churn the epoch.

**VERIFIED mixed component test:** actual native Mango browser/session, simulated UI readiness and simulated verification changes in isolated temporary preferences. Production verification and its epoch are not modified. The check passed:

- Native capability absent: unknown native request rejected with UNSUPPORTED_CAPABILITY;25 native rows disabled.
- Simulated verification enabled: new epoch, old revision rejected with STALE_LIBRARY, current folder refresh returns playable rows.
- Simulated verification revoked: another epoch,25 refreshed rows disabled.
- Repeated revocation leaves the epoch unchanged; zero UI adapter calls and no playback dispatch.

The check now lives in debug-only MangoCapabilityCheck and is callable through the existing DUMP-protected debug receiver. Its worker has an8s deadline and marshals SourceRegistry calls to the main thread. This avoids the instrumentation process restart. The legacy instrumentation entry point still runs the same check and retains its documented restart limitation.

Command, after starting the matching debug companion:

```powershell
& $adb -s d shell am broadcast -a org.dx340.remote.host.DEBUG_UI_PROBE -n org.dx340.remote.host/.DebugUiProbeReceiver --es source com.ibasso.music --es operation CHECK_MANGO_CAPABILITIES
& $adb -s d shell run-as org.dx340.remote.host cat files/mango-capability-probe.json
```

Inspect passed=true; a successful broadcast is not a passed check. Raw files: source-recovery-capability-check.json, source-recovery-process.json, source-recovery-bindings-{before,after}.txt and source-recovery-production-state.json in ignored artifacts/. The actual check retained the same companion PID, both accessibility bindings and no crashed services. Production epoch/capabilities stayed unchanged; Mango reported playing before and after. Temporary preference files were absent afterward. This is no audio-quality or radio claim.

This check used companion SHA256dee7c96b0a04f2f926cd014a90ef22bcb255ba8c494c64cd80d6ea53a37f4ad6, before the later debug-only volume probe addition. No phone UI/application code changed in this checkpoint. Real session replacement, pending phone commands during capability changes, process/range recovery and the full source/phone matrix remain NOT TESTED.
