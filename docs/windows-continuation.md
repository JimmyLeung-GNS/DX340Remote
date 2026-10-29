# Continue development on this Windows PC

Confirmed by the user on 29 October 2026: keep developing in `C:\GitHub\DX340Remote` on the current Windows PC. The working branch is `codex/source-recovery-checkpoint`. Continue this checkout rather than creating a second clone or assuming a move to another Windows PC.

## Resume the existing checkout

Use PowerShell 7 (7.6.5 was verified here). Read AGENTS.md, development-handoff.md, implementation-plan.md and status.md. Check the local branch/checkpoint:

```powershell
Set-Location C:\GitHub\DX340Remote
git status --short
git branch --show-current
git log -1 --oneline
adb devices -l
```

Use Git for the latest source commit and dated reports for package provenance. Current source8d73a48 passes102 native/36 Flutter/23 diagnostic software tests. Installed companion395be729... and phone7db636c5.... Fresh Mango and Apple37→36→37 local steps were separately confirmed audible on AMP15 phone out in92b5f9c2...; the later host restart clears memory-only proof. Corrected Flutter40850675516/native40850675500 iOS workflows pass at8d73a48. Live PC revocation still lacks a passing hold; later polling/INFO attempts failed. HiBy remains stopped after its authorized isolation test and playing-state relaunch. Read status.md and the resumed-volume/mango-foreground-startup/authenticated-windows/current-ios-build reports. Local bundle/manifest may lag; do not infer installed packages from HEAD alone.

## Existing toolchain and signing

Current host is JIMMYAH. The earlier `C:\Users\user`/Android Studio JBR paths described a previous environment; use the verified paths below on this PC:

| Purpose | Local path/version |
|---|---|
| Java | `C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot`, JDK21.0.9 |
| Android SDK | `C:\Users\JimmyLeung\AppData\Local\Android\Sdk` |
| Pinned Flutter | `C:\Users\JimmyLeung\AppData\Local\Flutter`, 3.41.4/revision `ff37bef603469fb030f2b72995ab929ccfc227f0` |
| Original development key | `C:\Users\JimmyLeung\.android\debug.keystore`, outside Git; verify certificate before installs |
| Windows BLE diagnostic Python | `C:\GitHub\DX340Remote\work\ble-diagnostic\Scripts\python.exe` |

Select the pinned tools and original key in this PowerShell process. The scripts below build/test locally; they do not install onto a device. Keep the existing ignored work/artifacts/local.properties files.

```powershell
Set-Location C:\GitHub\DX340Remote
$savedJavaForDx340 = $env:JAVA_HOME
$savedSdkForDx340 = $env:ANDROID_HOME
$savedPathForDx340 = $env:PATH
$savedKeyForDx340 = $env:DX340_DEBUG_KEYSTORE
$savedJavaOptionsForDx340 = $env:JAVA_TOOL_OPTIONS
$savedErrorActionForDx340 = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Stop'
    $env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-21.0.9.10-hotspot'
    $env:ANDROID_HOME = 'C:\Users\JimmyLeung\AppData\Local\Android\Sdk'
    $env:PATH = "C:\Users\JimmyLeung\AppData\Local\Flutter\bin;$env:PATH"
    $env:DX340_DEBUG_KEYSTORE = 'C:\Users\JimmyLeung\.android\debug.keystore'
    ./scripts/build-android.ps1
    ./scripts/build-phone.ps1
} finally {
    $env:JAVA_HOME = $savedJavaForDx340
    $env:ANDROID_HOME = $savedSdkForDx340
    $env:PATH = $savedPathForDx340
    $env:DX340_DEBUG_KEYSTORE = $savedKeyForDx340
    $env:JAVA_TOOL_OPTIONS = $savedJavaOptionsForDx340
    $ErrorActionPreference = $savedErrorActionForDx340
}
```

Use installation.md's matching-certificate preflight before any later install-r. Preserve app data and existing keys. The original key-transfer problem is resolved; use the path above for future updates.

## Local checkpoint and remaining physical steps

The branch contains the committed source/docs. Ignored artifacts/dx340-checkpoint.bundle and checkpoint-manifest.json are refreshed together and verified by a separate local clone. They are a recovery copy for this PC; no development relocation is required. APKs, logs, tools and the signing key are separate from the Git bundle. Rebuild and verify the manifest after code/package changes rather than treating an old package as current source.

Only DX340 d is currently available over USB. Windows native pairing succeeded with ENCRYPTION_AND_AUTHENTICATION; four fresh approved state/duplicate-read sessions passed, then the user revoked approval and a fresh denial passed. The OS bond is retained; companion approval is currently removed. Intermittent setup/ATT failures and immediate live-revocation timing remain unresolved. The operator is available for listening; the fresh volume check is in progress. Separate Android/iPhone acceptance and the three-second Mango page target remain unmet. See test-results/2026-10-29-authenticated-windows.md and windows-ble-diagnostics.md.

Mac/Xcode is required later to compile/sign/validate the iPhone code. mac-continuation.md documents that platform-specific work while this Windows checkout remains the main development workspace. Full project scope/remaining evidence is in completion-audit.md and phone-reliability-runbook.md.
