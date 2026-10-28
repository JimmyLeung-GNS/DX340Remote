# AMP15 volume observation and downward API probe

Device: DX340 Android13/API33, build eng.iBasso.20260812.181952, Mango4.1.37/code240 playing local music. AMP15 phone out is user reported. This is a local companion/source test, **not phone Bluetooth volume acceptance**. No gain, EQ, route or source engine was changed by the test.

| Result | Case | Evidence / limit |
|---|---|---|
| VERIFIED read | Public AudioManager baseline | STREAM_MUSIC min0/max100/current36, fixedVolume=false, musicActive=true; fresh companion diagnostic |
| VERIFIED user reported | Physical wheel down one click | User answered “Turned down one click; sound is quieter.” Fresh API read35 and Mango volume_value_view35 |
| VERIFIED API/display | Companion-controlled downward step | Public setStreamVolume requested35→34; after500ms API observed34 and Mango displayed34; playback remained reported playing |
| VERIFIED user reported | App-controlled step sounded quieter | User separately answered “Yes, the app-controlled step is quieter.” Delayed reply; no calibrated dB or instantaneous A/B measurement |
| VERIFIED rejection | Repeat with old expected volume | STALE_VOLUME, display remained34; no second downward step |
| VERIFIED rejection | Old source epoch | STALE_SOURCE, display remained34 |
| NOT TESTED | Upward control / minimum / maximum / other outputs | No increase, mute/minimum or maximum listening test was performed |
| NOT TESTED | Apple Music / phones / reconnect | No Apple volume action, BLE command or phone volume UI acceptance |

The added PROBE_VOLUME_DOWN operation is debug-only and protected by privileged DUMP permission. It requires a matching current sourceEpoch, explicit matching expectedVolume, playing source, active music and non-fixed volume above the minimum. It sets an absolute value exactly one lower and reports the subsequent API read. It does not enable canAdjustVolume or change any persistent verification flag. The product's phone volume controls remain disabled pending implementation and remaining verification.

Public API reference and installed SDK36 source were checked: [AudioManager setStreamVolume](https://developer.android.com/reference/android/media/AudioManager#setStreamVolume(int,%20int,%20int)). API availability is separate from the actual-device and listening results above. The diagnostic build/lint passed; the preceding source-recovery full build passed40 JVM tests. No unit test or SDK call is treated as acoustic proof.

Ignored artifacts/: volume-wheel-baseline.json, volume-wheel-after.json, volume-wheel-mango-ui.json, volume-api-downward.json, volume-api-mango-ui.json, volume-api-listening-confirmation.json, volume-api-{duplicate,stale-source}-rejection.json, volume-api-after-rejections-ui.json and volume-final-reported-state.json. Device was left at34 after the downward probe. Current companion APK SHA2563cd67d3f001264d96f83d6fec8b85cb67952a7be0517de53b9c115f9c8d32cf0.
